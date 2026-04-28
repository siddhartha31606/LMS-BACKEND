package com.klu.lms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.klu.lms.model.Assignment;
import com.klu.lms.model.Course;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByCourse(Course course);
    void deleteByCourse(Course course);
}
