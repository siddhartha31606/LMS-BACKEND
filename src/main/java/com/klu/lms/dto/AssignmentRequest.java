package com.klu.lms.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AssignmentRequest {

    @NotNull(message = "Course id is required")
    private Long courseId;

    @NotBlank(message = "Assignment title is required")
    private String title;

    @NotNull(message = "Deadline is required")
    @FutureOrPresent(message = "Deadline must be today or later")
    private LocalDate deadline;

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }
}
