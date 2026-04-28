package com.klu.lms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.klu.lms.model.Course;
import com.klu.lms.model.User;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByInstructor(User instructor);
}
