package com.klu.lms.service;

import org.springframework.stereotype.Service;

import com.klu.lms.dto.UserProfileResponse;
import com.klu.lms.dto.UserProfileUpdateRequest;
import com.klu.lms.model.User;
import com.klu.lms.repository.UserRepository;

@Service
public class UserProfileService {

    private final UserRepository userRepository;

    public UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return mapToResponse(user);
    }

    public UserProfileResponse updateProfile(String email, UserProfileUpdateRequest request) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
          user.setFullName(request.getFullName().trim());
        }
        user.setPhoneNumber(normalize(request.getPhoneNumber()));
        user.setDepartment(normalize(request.getDepartment()));
        user.setYearOfStudy(normalize(request.getYearOfStudy()));
        user.setCity(normalize(request.getCity()));
        user.setBio(normalize(request.getBio()));

        return mapToResponse(userRepository.save(user));
    }

    private UserProfileResponse mapToResponse(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setActive(user.getActive());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setDepartment(user.getDepartment());
        response.setYearOfStudy(user.getYearOfStudy());
        response.setCity(user.getCity());
        response.setBio(user.getBio());
        return response;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
