package com.klu.lms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.klu.lms.model.Assignment;
import com.klu.lms.model.Submission;
import com.klu.lms.model.User;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByStudent(User student);
    List<Submission> findByAssignment(Assignment assignment);
    Optional<Submission> findByAssignmentAndStudent(Assignment assignment, User student);
    void deleteByAssignment(Assignment assignment);
}
