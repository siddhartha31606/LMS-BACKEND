package com.klu.lms.dto;

import jakarta.validation.constraints.Size;

public class UserProfileUpdateRequest {

    @Size(max = 255, message = "Full name is too long")
    private String fullName;

    @Size(max = 25, message = "Phone number is too long")
    private String phoneNumber;

    @Size(max = 255, message = "Department is too long")
    private String department;

    @Size(max = 100, message = "Year of study is too long")
    private String yearOfStudy;

    @Size(max = 255, message = "City is too long")
    private String city;

    @Size(max = 1200, message = "Bio is too long")
    private String bio;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getYearOfStudy() {
        return yearOfStudy;
    }

    public void setYearOfStudy(String yearOfStudy) {
        this.yearOfStudy = yearOfStudy;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
}
