package com.klu.lms.controller;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.klu.lms.dto.AssignmentRequest;
import com.klu.lms.dto.FileResponse;
import com.klu.lms.dto.GradeRequest;
import com.klu.lms.model.Assignment;
import com.klu.lms.model.Course;
import com.klu.lms.model.Material;
import com.klu.lms.model.Submission;
import com.klu.lms.repository.AssignmentRepository;
import com.klu.lms.service.AssignmentService;
import com.klu.lms.service.CourseService;
import com.klu.lms.service.MaterialService;
import com.klu.lms.service.SubmissionService;

@RestController
@RequestMapping("/instructor")
public class InstructorController {

    private final CourseService courseService;
    private final MaterialService materialService;
    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;
    private final AssignmentRepository assignmentRepository;

    public InstructorController(
        CourseService courseService,
        MaterialService materialService,
        AssignmentService assignmentService,
        SubmissionService submissionService,
        AssignmentRepository assignmentRepository
    ) {
        this.courseService = courseService;
        this.materialService = materialService;
        this.assignmentService = assignmentService;
        this.submissionService = submissionService;
        this.assignmentRepository = assignmentRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "Instructor dashboard API working";
    }

    @GetMapping("/courses")
    public List<Course> myCourses(Principal principal) {
        return courseService.getInstructorCourses(principal.getName());
    }

    @PostMapping(value = "/materials/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileResponse> uploadMaterial(
        @RequestParam("courseId") Long courseId,
        @RequestParam(value = "title", required = false) String title,
        @RequestParam("file") MultipartFile file,
        Principal principal
    ) throws IOException {
        Material material = materialService.uploadMaterial(courseId, title, file, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new FileResponse(material.getOriginalFileName(), "Material uploaded successfully"));
    }

    @GetMapping("/courses/{courseId}/materials")
    public List<Material> getCourseMaterials(@PathVariable Long courseId) {
        return materialService.getMaterialsForCourse(courseId);
    }

    @GetMapping("/materials/{materialId}/download")
    public ResponseEntity<Resource> downloadMaterial(@PathVariable Long materialId, Principal principal) throws IOException {
        Material material = materialService.getMaterial(materialId);
        Resource resource = materialService.downloadMaterialForInstructor(materialId, principal.getName());
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + material.getOriginalFileName() + "\"")
            .contentType(MediaType.parseMediaType(material.getContentType()))
            .body(resource);
    }

    @DeleteMapping("/materials/{materialId}")
    public ResponseEntity<String> deleteMaterial(@PathVariable Long materialId, Principal principal) {
        materialService.deleteMaterialForInstructor(materialId, principal.getName());
        return ResponseEntity.ok("Material deleted successfully");
    }

    @PutMapping("/materials/{materialId}/delete")
    public ResponseEntity<String> deleteMaterialLegacy(@PathVariable Long materialId, Principal principal) {
        materialService.deleteMaterialForInstructor(materialId, principal.getName());
        return ResponseEntity.ok("Material deleted successfully");
    }

    @PostMapping("/assignments")
    public ResponseEntity<Assignment> createAssignment(@Validated @RequestBody AssignmentRequest request, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(assignmentService.createAssignment(request, principal.getName()));
    }

    @GetMapping("/courses/{courseId}/assignments")
    public List<Assignment> getCourseAssignments(@PathVariable Long courseId) {
        return assignmentService.getAssignmentsForCourse(courseId);
    }

    @GetMapping("/submissions")
    public List<Submission> getSubmissions(Principal principal) {
        return assignmentService.getInstructorSubmissions(principal.getName());
    }

    @PutMapping("/submissions/{submissionId}/grade")
    public Submission gradeSubmission(
        @PathVariable Long submissionId,
        @Validated @RequestBody GradeRequest request,
        Principal principal
    ) {
        return assignmentService.gradeSubmission(submissionId, request, principal.getName());
    }

    @GetMapping("/submissions/{submissionId}/download")
    public ResponseEntity<Resource> downloadSubmission(@PathVariable Long submissionId, Principal principal) throws IOException {
        Submission submission = submissionService.getSubmission(submissionId);
        Resource resource = submissionService.downloadSubmissionForInstructor(submissionId, principal.getName());
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + submission.getOriginalFileName() + "\"")
            .contentType(MediaType.parseMediaType(submission.getContentType()))
            .body(resource);
    }

    @PutMapping("/assignments/{assignmentId}/deadline")
    public Assignment extendDeadline(@PathVariable Long assignmentId, @RequestParam String deadline) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));
        assignment.setDeadline(LocalDate.parse(deadline));
        return assignmentRepository.save(assignment);
    }
}
