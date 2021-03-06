package com.pronet.applications;


import com.pronet.exceptions.BadRequestException;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@Component("AppController")
@RequestMapping("/api/v1")
public class AppController {

    AppService appService;

    @Autowired
    public AppController(AppService appService) {
        this.appService = appService;
    }

    //Apply for job
    @RequestMapping(value = "/jobs/apply", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.OK)
    public void jobApp(@RequestBody AppModel model,BindingResult result) throws Exception {

        System.out.println(model.getApp_date() + " " + model.getCompany_id());
        if(result.hasErrors())
        {
            throw new BadRequestException("Request Body Is Missing Required Parameters");
        }
         appService.jobAppAt(model);
    }
    //get all job applications for company
    @RequestMapping(value = "/jobs/apps/{c_id}", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public JSONObject getAllJobs(@PathVariable("c_id") String c_id) throws Exception {
        return appService.getAllJobsAt(c_id);
    }

    //get all job applications for users
    @RequestMapping(value = "/jobs/userapps/{u_id}", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public JSONObject getAllUserApps(@PathVariable("u_id") String u_id) throws Exception {

        return appService.getAllUserAppsAt(u_id);


    }


}
