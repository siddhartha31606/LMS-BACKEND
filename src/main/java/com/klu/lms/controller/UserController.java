package com.klu.lms.controller;

import java.security.Principal;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.klu.lms.dto.UserProfileResponse;
import com.klu.lms.dto.UserProfileUpdateRequest;
import com.klu.lms.service.UserProfileService;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserProfileService userProfileService;

    public UserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/profile")
    public UserProfileResponse profile(Principal principal) {
        return userProfileService.getProfile(principal.getName());
    }

    @PutMapping("/profile")
    public UserProfileResponse updateProfile(@Validated @RequestBody UserProfileUpdateRequest request, Principal principal) {
        return userProfileService.updateProfile(principal.getName(), request);
    }
}
