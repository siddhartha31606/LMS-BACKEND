package com.klu.lms.controller;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.klu.lms.dto.CourseRequest;
import com.klu.lms.dto.FileResponse;
import com.klu.lms.dto.RegisterRequest;
import com.klu.lms.model.Assignment;
import com.klu.lms.model.Course;
import com.klu.lms.model.Enrollment;
import com.klu.lms.model.Material;
import com.klu.lms.model.Role;
import com.klu.lms.model.Submission;
import com.klu.lms.model.User;
import com.klu.lms.repository.AssignmentRepository;
import com.klu.lms.repository.CourseRepository;
import com.klu.lms.repository.EnrollmentRepository;
import com.klu.lms.repository.SubmissionRepository;
import com.klu.lms.repository.UserRepository;
import com.klu.lms.service.CourseService;
import com.klu.lms.service.MaterialService;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final CourseService courseService;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final MaterialService materialService;

    public AdminController(
        CourseService courseService,
        UserRepository userRepository,
        CourseRepository courseRepository,
        EnrollmentRepository enrollmentRepository,
        AssignmentRepository assignmentRepository,
        SubmissionRepository submissionRepository,
        PasswordEncoder passwordEncoder,
        MaterialService materialService
    ) {
        this.courseService = courseService;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.passwordEncoder = passwordEncoder;
        this.materialService = materialService;
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "Admin dashboard API working";
    }

    // ─── Users ─────────────────────────────────

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @PostMapping("/users")
    public ResponseEntity<User> addUser(@Validated @RequestBody RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("An account with this email already exists");
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setActive(true);
        return ResponseEntity.status(HttpStatus.CREATED).body(userRepository.save(user));
    }

    @PutMapping("/users/{id}/toggle-active")
    public User toggleUserActive(@PathVariable Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setActive(!Boolean.TRUE.equals(user.getActive()));
        return userRepository.save(user);
    }

    @PutMapping("/users/{id}/role")
    public User assignRole(@PathVariable Long id, @RequestParam Role role) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setRole(role);
        return userRepository.save(user);
    }

    // ─── Courses ───────────────────────────────

    @PostMapping("/courses")
    public ResponseEntity<Course> createCourse(@Validated @RequestBody CourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.createCourse(request));
    }

    @GetMapping("/courses")
    public List<Course> getCourses() {
        return courseService.getAllCourses();
    }

    @GetMapping("/library-resources")
    public List<Material> getLibraryResources() {
        return materialService.getLibraryMaterials();
    }

    @PostMapping(value = "/library-resources/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileResponse> uploadLibraryResource(
        @RequestParam(value = "title", required = false) String title,
        @RequestParam("file") MultipartFile file,
        Principal principal
    ) throws IOException {
        Material material = materialService.uploadLibraryMaterial(title, file, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new FileResponse(material.getOriginalFileName(), "Library resource uploaded successfully"));
    }

    @GetMapping("/library-resources/{materialId}/download")
    public ResponseEntity<Resource> downloadLibraryResource(@PathVariable Long materialId) throws IOException {
        Material material = materialService.getMaterial(materialId);
        Resource resource = materialService.downloadMaterial(materialId);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + material.getOriginalFileName() + "\"")
            .contentType(MediaType.parseMediaType(material.getContentType()))
            .body(resource);
    }

    @DeleteMapping("/courses/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/courses/{courseId}/instructor")
    public Course assignInstructor(@PathVariable Long courseId, @RequestParam Long instructorId) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        User instructor = userRepository.findById(instructorId)
            .filter(u -> u.getRole() == Role.INSTRUCTOR)
            .orElseThrow(() -> new IllegalArgumentException("Instructor not found"));
        course.setInstructor(instructor);
        return courseRepository.save(course);
    }

    // ─── Stats ─────────────────────────────────

    @GetMapping("/enrollments")
    public List<Enrollment> getAllEnrollments() {
        return enrollmentRepository.findAll();
    }

    @GetMapping("/assignments")
    public List<Assignment> getAllAssignments() {
        return assignmentRepository.findAll();
    }

    @GetMapping("/submissions")
    public List<Submission> getAllSubmissions() {
        return submissionRepository.findAll();
    }
}
