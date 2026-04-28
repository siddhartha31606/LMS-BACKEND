package com.klu.lms.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.klu.lms.dto.AssignmentRequest;
import com.klu.lms.dto.GradeRequest;
import com.klu.lms.model.Assignment;
import com.klu.lms.model.Course;
import com.klu.lms.model.Enrollment;
import com.klu.lms.model.Role;
import com.klu.lms.model.Submission;
import com.klu.lms.model.SubmissionStatus;
import com.klu.lms.model.User;
import com.klu.lms.repository.AssignmentRepository;
import com.klu.lms.repository.CourseRepository;
import com.klu.lms.repository.EnrollmentRepository;
import com.klu.lms.repository.SubmissionRepository;
import com.klu.lms.repository.UserRepository;

@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final NotificationService notificationService;

    public AssignmentService(
        AssignmentRepository assignmentRepository,
        CourseRepository courseRepository,
        SubmissionRepository submissionRepository,
        UserRepository userRepository,
        EnrollmentRepository enrollmentRepository,
        NotificationService notificationService
    ) {
        this.assignmentRepository = assignmentRepository;
        this.courseRepository = courseRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.notificationService = notificationService;
    }

    public Assignment createAssignment(AssignmentRequest request, String instructorEmail) {
        Course course = courseRepository.findById(request.getCourseId())
            .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        if (course.getInstructor() == null || !course.getInstructor().getEmail().equalsIgnoreCase(instructorEmail)) {
            throw new IllegalArgumentException("You can only create assignments for your own courses");
        }

        Assignment assignment = new Assignment();
        assignment.setCourse(course);
        assignment.setTitle(request.getTitle().trim());
        assignment.setDeadline(request.getDeadline());
        Assignment savedAssignment = assignmentRepository.save(assignment);

        List<Enrollment> enrollments = enrollmentRepository.findByCourse(course);
        for (Enrollment enrollment : enrollments) {
            notificationService.sendNotification(
                enrollment.getStudent().getEmail(),
                "New assignment posted",
                "A new assignment \"" + savedAssignment.getTitle() + "\" was created for " + course.getTitle() + "."
            );
        }
        return savedAssignment;
    }

    public List<Assignment> getAssignmentsForCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        return assignmentRepository.findByCourse(course);
    }

    public List<Submission> getInstructorSubmissions(String instructorEmail) {
        User instructor = userRepository.findByEmail(instructorEmail)
            .filter(user -> user.getRole() == Role.INSTRUCTOR)
            .orElseThrow(() -> new IllegalArgumentException("Instructor not found"));

        return courseRepository.findByInstructor(instructor).stream()
            .flatMap(course -> assignmentRepository.findByCourse(course).stream())
            .flatMap(assignment -> submissionRepository.findByAssignment(assignment).stream())
            .toList();
    }

    public Submission gradeSubmission(Long submissionId, GradeRequest request, String instructorEmail) {
        Submission submission = submissionRepository.findById(submissionId)
            .orElseThrow(() -> new IllegalArgumentException("Submission not found"));

        Course course = submission.getAssignment().getCourse();
        if (course.getInstructor() == null || !course.getInstructor().getEmail().equalsIgnoreCase(instructorEmail)) {
            throw new IllegalArgumentException("You can only grade submissions for your own courses");
        }

        submission.setGrade(request.getGrade());
        submission.setFeedback(request.getFeedback());
        submission.setStatus(SubmissionStatus.GRADED);
        submission.setGradedAt(LocalDateTime.now());
        Submission savedSubmission = submissionRepository.save(submission);

        notificationService.sendNotification(
            submission.getStudent().getEmail(),
            "Assignment graded",
            "Your assignment \"" + submission.getAssignment().getTitle() + "\" has been graded."
        );

        return savedSubmission;
    }
}
