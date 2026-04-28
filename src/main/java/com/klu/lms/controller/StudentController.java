package com.klu.lms.controller;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.klu.lms.dto.FileResponse;
import com.klu.lms.model.Assignment;
import com.klu.lms.model.Course;
import com.klu.lms.model.Material;
import com.klu.lms.model.Submission;
import com.klu.lms.repository.AssignmentRepository;
import com.klu.lms.repository.CourseRepository;
import com.klu.lms.repository.EnrollmentRepository;
import com.klu.lms.model.User;
import com.klu.lms.repository.UserRepository;
import com.klu.lms.service.CourseService;
import com.klu.lms.service.MaterialService;
import com.klu.lms.service.SubmissionService;

@RestController
@RequestMapping("/student")
public class StudentController {

    private final CourseService courseService;
    private final MaterialService materialService;
    private final SubmissionService submissionService;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;

    public StudentController(
        CourseService courseService,
        MaterialService materialService,
        SubmissionService submissionService,
        CourseRepository courseRepository,
        EnrollmentRepository enrollmentRepository,
        AssignmentRepository assignmentRepository,
        UserRepository userRepository
    ) {
        this.courseService = courseService;
        this.materialService = materialService;
        this.submissionService = submissionService;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "Student dashboard API working";
    }

    @GetMapping("/courses")
    public List<Course> myCourses(Principal principal) {
        return courseService.getStudentCourses(principal.getName());
    }

    @GetMapping("/catalog")
    public List<Course> allCourses() {
        return courseService.getAllCourses();
    }

    @PostMapping("/courses/{courseId}/enroll")
    public ResponseEntity<String> enroll(@PathVariable Long courseId, Principal principal) {
        courseService.enrollStudent(courseId, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body("Enrollment successful");
    }

    @GetMapping("/courses/{courseId}/materials")
    public List<Material> getCourseMaterials(@PathVariable Long courseId, Principal principal) {
        return materialService.getStudentMaterialsForCourse(courseId, principal.getName());
    }

    @GetMapping("/resources")
    public List<Material> getResourceLibrary() {
        return materialService.getLibraryMaterials();
    }

    @GetMapping("/materials/{materialId}/download")
    public ResponseEntity<Resource> downloadMaterial(@PathVariable Long materialId, Principal principal) throws IOException {
        Material material = materialService.getMaterial(materialId);
        Resource resource = materialService.downloadMaterialForStudent(materialId, principal.getName());
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + material.getOriginalFileName() + "\"")
            .contentType(MediaType.parseMediaType(material.getContentType()))
            .body(resource);
    }

    @PostMapping(value = "/assignments/{assignmentId}/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileResponse> submitAssignment(
        @PathVariable Long assignmentId,
        @RequestParam("file") MultipartFile file,
        Principal principal
    ) throws IOException {
        Submission submission = submissionService.submitAssignment(assignmentId, file, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new FileResponse(submission.getOriginalFileName(), "Assignment submitted successfully"));
    }

    @GetMapping("/submissions")
    public List<Submission> mySubmissions(Principal principal) {
        return submissionService.getStudentSubmissions(principal.getName());
    }

    @GetMapping("/assignments")
    public List<Assignment> myAssignments(Principal principal) {
        User student = userRepository.findByEmail(principal.getName())
            .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        return enrollmentRepository.findByStudent(student).stream()
            .flatMap(enrollment -> assignmentRepository.findByCourse(enrollment.getCourse()).stream())
            .toList();
    }
}
