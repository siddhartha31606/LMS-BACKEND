package com.klu.lms.service;

import java.io.IOException;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.klu.lms.model.Assignment;
import com.klu.lms.model.Enrollment;
import com.klu.lms.model.Submission;
import com.klu.lms.model.SubmissionStatus;
import com.klu.lms.model.User;
import com.klu.lms.repository.AssignmentRepository;
import com.klu.lms.repository.EnrollmentRepository;
import com.klu.lms.repository.SubmissionRepository;
import com.klu.lms.repository.UserRepository;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    public SubmissionService(
        SubmissionRepository submissionRepository,
        AssignmentRepository assignmentRepository,
        UserRepository userRepository,
        EnrollmentRepository enrollmentRepository,
        FileStorageService fileStorageService,
        NotificationService notificationService
    ) {
        this.submissionRepository = submissionRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.fileStorageService = fileStorageService;
        this.notificationService = notificationService;
    }

    public Submission submitAssignment(Long assignmentId, MultipartFile file, String studentEmail) throws IOException {
        Assignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));
        User student = userRepository.findByEmail(studentEmail)
            .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        Enrollment enrollment = enrollmentRepository.findByStudentAndCourse(student, assignment.getCourse())
            .orElseThrow(() -> new IllegalArgumentException("Student is not enrolled in this course"));

        FileStorageService.StoredFile storedFile = fileStorageService.store(file, "submissions");

        Submission submission = submissionRepository.findByAssignmentAndStudent(assignment, enrollment.getStudent())
            .orElseGet(Submission::new);
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setOriginalFileName(storedFile.originalFileName());
        submission.setStoredFileName(storedFile.storedFileName());
        submission.setContentType(storedFile.contentType());
        submission.setFilePath(storedFile.filePath());
        submission.setStatus(SubmissionStatus.SUBMITTED);
        submission.setGrade(null);
        submission.setFeedback(null);

        Submission savedSubmission = submissionRepository.save(submission);

        if (assignment.getCourse().getInstructor() != null) {
            notificationService.sendNotification(
                assignment.getCourse().getInstructor().getEmail(),
                "Assignment submitted",
                student.getFullName() + " submitted \"" + assignment.getTitle() + "\"."
            );
        }

        notificationService.sendNotification(
            student.getEmail(),
            "Assignment submission received",
            "Your assignment \"" + assignment.getTitle() + "\" was submitted successfully."
        );

        return savedSubmission;
    }

    public List<Submission> getStudentSubmissions(String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
            .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        return submissionRepository.findByStudent(student);
    }

    public Resource downloadSubmission(Long submissionId) throws IOException {
        Submission submission = submissionRepository.findById(submissionId)
            .orElseThrow(() -> new IllegalArgumentException("Submission not found"));
        return fileStorageService.loadAsResource(submission.getFilePath());
    }

    public Resource downloadSubmissionForInstructor(Long submissionId, String instructorEmail) throws IOException {
        Submission submission = getSubmission(submissionId);
        if (submission.getAssignment().getCourse().getInstructor() == null
            || !submission.getAssignment().getCourse().getInstructor().getEmail().equalsIgnoreCase(instructorEmail)) {
            throw new IllegalArgumentException("You can only download submissions for your own courses");
        }
        return fileStorageService.loadAsResource(submission.getFilePath());
    }

    public Submission getSubmission(Long submissionId) {
        return submissionRepository.findById(submissionId)
            .orElseThrow(() -> new IllegalArgumentException("Submission not found"));
    }
}
