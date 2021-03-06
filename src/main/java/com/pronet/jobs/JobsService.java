package com.pronet.jobs;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.pronet.exceptions.BadRequestException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component("JobsService")
public class JobsService {


    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    DynamoDB dyDB;

    @Autowired
    DynamoDBMapper mapper;

    private RedisTemplate<String, String> redisTemplate;


    @Autowired
    StringRedisTemplate redisClient;

    @Autowired
    AmazonDynamoDBClient client = new AmazonDynamoDBClient();

    @Autowired
    public JobsService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    long score = 0;
    public void saveJobPostAt(JobsModel model) {

        String id = model.getId();
        String description = model.getDescription();
        String jtitle = model.getJtitle();
        String start_date = model.getStart_date();
        String ex_date = model.getEx_date();
        String region = model.getJob_region();
        String job_status = model.getJob_status();
        String skills = model.getSkills();

        Table jobtable = dyDB.getTable("JobPosting");

        String jid = 1 + ((int) (Math.random() * 2000)) + "";
        model.setJid(jid);

        Item jobItem = new Item()
                .withPrimaryKey("jid", jid)
                .withString("id", id)
                .withString("jtitle", jtitle)
                .withString("description", description)
                .withString("skills", skills)
                .withString("job_status", job_status)
                .withString("job_region", region)
                .withString("start_date", start_date)
                .withString("ex_date", ex_date);

        jobtable.putItem(jobItem);

        //Fetch Company Logo URL and Company Name from DynamoDB

        Table company_table = dyDB.getTable("CompanyProfile");
        GetItemSpec spec = new GetItemSpec()
                .withPrimaryKey("id", model.getId())
                .withProjectionExpression("id,logo,user_name")
                .withConsistentRead(true);
        Item item = company_table.getItem(spec);
        //System.out.println(item.toJSONPretty());

        //redis HASH-MAP
        final String keyForHash = String.format("jobs:%s", model.getJid());
        final Map<String, Object> properties = new HashMap<String, Object>();

        properties.put("jobId",jid);
        properties.put("companyId", model.getId());
        properties.put("positionTitle", model.getJtitle());
        properties.put("companyName", item.get("user_name"));//how will i get company name???
        properties.put("companyLogoUrl", item.get("logo"));//from s3 get url
        properties.put("positionLocation", model.getJob_region());
        properties.put("job_status", model.getJob_status());
        properties.put("description",model.getDescription());

        //query: hgetall jobs:11 / 11 is jobID
        redisTemplate.opsForHash().putAll(keyForHash, properties);



        String tag = model.getJtitle().toLowerCase().replace(" ", "_");

        final String keyForSet = String.format("tags:jobs:%s", tag);
        score = redisTemplate.opsForZSet().size(keyForSet);
        //populating tags for search
        //ZRANGE tags:jobs:new_position_for_SE 0 1 WITHSCORES
        redisTemplate.opsForZSet().add(keyForSet, jid, score + 1);

        //redis for region tag
        String tag1 = model.getJob_region().toLowerCase().replace(" ","_");
        final String keyForSet1 = String.format("tags:jobs:%s", tag1);
        //populating tags for search
        //ZRANGE tags:jobs:new_position_for_SE 0 0 WITHSCORES
        redisTemplate.opsForZSet().add(keyForSet1, jid, score+1);

    }

    public JSONObject getJobDetailsAt(String jid, String uid) {

        System.out.println("In get job details");

        JSONObject jsonObject = new JSONObject();

        Table company_table1 = dyDB.getTable("JobPosting");
        GetItemSpec spec1 = new GetItemSpec()
                .withPrimaryKey("jid", jid)
                .withProjectionExpression("id,description,job_region,skills,jtitle,job_status")
                .withConsistentRead(true);
        Item item1 = company_table1.getItem(spec1);


        Table company_table = dyDB.getTable("CompanyProfile");
        GetItemSpec spec = new GetItemSpec()
                .withPrimaryKey("id", item1.get("id"))
                .withProjectionExpression("id,logo,user_name")
                .withConsistentRead(true);
        Item item = company_table.getItem(spec);

        jsonObject.put("jid", jid);
        jsonObject.put("id", item.get("id"));//company ID
        jsonObject.put("jtitle", item1.get("jtitle"));
        jsonObject.put("description", item1.get("description"));
        jsonObject.put("user_name", item.get("user_name"));
        jsonObject.put("logo", item.get("logo"));
        jsonObject.put("skills", item1.get("skills"));
        jsonObject.put("job_region", item1.get("job_region"));
        jsonObject.put("job_status",item1.get("job_status"));

        String sql = "SELECT count(1) FROM job_apps WHERE job_id ='" + jid + "' and user_id ='" +  uid + "'";
        Integer apply = jdbcTemplate.queryForObject(sql, Integer.class);

        if(apply == 0)
            jsonObject.put("apply","Apply");
        else
            jsonObject.put("apply","Applied");

        System.out.println(jsonObject);
        return jsonObject;

    }

    public void deleteJobAt(String jid) throws Exception {

        try {

            Table getJob = dyDB.getTable("JobPosting");
            GetItemSpec spec = new GetItemSpec()
                    .withPrimaryKey("jid", jid)
                    .withProjectionExpression("jtitle")
                    .withConsistentRead(true);
            Item item = getJob.getItem(spec);

            String tableName = "JobPosting";
            final Table jobpostings = dyDB.getTable(tableName);
            jobpostings.deleteItem(new PrimaryKey("jid", jid));

            //delete from Redis

            String jobTitle = item.get("jtitle").toString().toLowerCase().replace(" ", "_");
            final String key = "tags:jobs:" + jobTitle;
            redisClient.opsForZSet().remove(key, jid);
            redisClient.opsForHash().getOperations().delete("jobs:" + jid);
        } catch (Exception e) {

            throw new BadRequestException("Unable to Delete");

        }
    }

    public JSONArray getAllCompanyJobsAt(String c_id) {

        String tableName = "JobPosting";
        JSONArray results = new JSONArray();

        Table table = dyDB.getTable(tableName);
        Map<String, Object> expressionAttributeValues = new HashMap<String, Object>();
        expressionAttributeValues.put(":ID", c_id);
        ItemCollection<ScanOutcome> items = table.scan(
                "id = :ID", //FilterExpression
                "jid,description,jtitle,job_status", //ProjectionExpression
                null,
                expressionAttributeValues);
        Iterator<Item> iterator = items.iterator();
        while (iterator.hasNext()) {
            //System.out.println(iterator.next().toJSONPretty());
            JSONObject jsonObject = new JSONObject();
            Item i = iterator.next();
            jsonObject.put("jid", i.get("jid"));
            jsonObject.put("jtitle", i.get("jtitle"));
            jsonObject.put("description",i.get("description"));
            jsonObject.put("job_status",i.get("job_status"));
            results.add(jsonObject);

        }
        return results;
    }

    public List getAlAppsAt(String j_id) throws Exception{
        try
            {
                String getApps = "SELECT * FROM job_apps WHERE job_id =" + j_id ;
                List results = jdbcTemplate.queryForList(getApps);
                return results;
            }
        catch (Exception e){
            throw new BadRequestException(e.getMessage());
        }
    }

}
