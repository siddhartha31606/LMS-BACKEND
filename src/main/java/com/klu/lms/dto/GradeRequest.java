package com.klu.lms.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class GradeRequest {

    @Min(value = 0, message = "Grade cannot be below 0")
    @Max(value = 100, message = "Grade cannot be above 100")
    private Integer grade;

    private String feedback;

    public Integer getGrade() {
        return grade;
    }

    public void setGrade(Integer grade) {
        this.grade = grade;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
}
