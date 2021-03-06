package com.pronet.signup;
import com.pronet.exceptions.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@Component("signUpController")
@RequestMapping("/api/v1")
public class SignUpController {

    SignUpService signUpService;

    @Autowired
    public SignUpController(SignUpService signUpService) {
        this.signUpService = signUpService;
    }

    @RequestMapping(value = "/signup", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public void signUpUser(@Valid @RequestBody SignUp model, BindingResult result) {
        {
            if (model.getUser_name() == null || model.getUser_name().trim().equals(""))
                throw new BadRequestException("Require Name");

            if (result.hasErrors()) {
                throw new BadRequestException("Error in Request Body");
            }

            signUpService.signUpUser(model);

        }
    }
}
