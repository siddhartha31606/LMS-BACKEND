package com.klu.lms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.klu.lms.model.Course;
import com.klu.lms.model.Enrollment;
import com.klu.lms.model.User;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudent(User student);
    List<Enrollment> findByCourse(Course course);
    Optional<Enrollment> findByStudentAndCourse(User student, Course course);
    void deleteByCourse(Course course);
}
