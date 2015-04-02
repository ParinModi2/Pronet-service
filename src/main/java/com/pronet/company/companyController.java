package com.pronet.company;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.pronet.BadRequestException;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * Created by varuna on 4/2/15.
 */

@Controller
@RestController
@Component("companyController")
public class companyController {

    @Autowired
    DynamoDB dyDB;

    @Autowired
    DynamoDBMapper mapper;

    @Autowired
    AmazonDynamoDBClient dynamoDBClient;

    @RequestMapping(value = "/companyprofile/{id}", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public JSONObject getCompanyDetails(@PathVariable("id") String id) throws Exception {
        JSONObject json = new JSONObject();
        try {
            CompanyProfile getCompany = mapper.load(CompanyProfile.class, id);
            json.put("page", "C");
            json.put("id", getCompany.getId());
            json.put("name", getCompany.getName());
            if (getCompany.getLogo() == null) {
                json.put("Logo", "/assets/images/companylogo.jpg");
            } else {
                json.put("Logo", getCompany.getLogo());
            }

            if (getCompany.getUrl() == null) {
                json.put("URL", "Company URL");
            } else {
                json.put("URL", getCompany.getUrl());
            }

            if (getCompany.getOverview() == null) {
                json.put("Overview", "Give your company overview");
            } else {
                json.put("Overview", getCompany.getOverview());
            }

            System.out.println(json);
            return json;
        }
        catch(Exception e)
        {
            System.out.println("Company not found");
            throw new BadRequestException("Company not found");

        }
    }
}