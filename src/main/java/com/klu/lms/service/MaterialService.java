package com.klu.lms.service;

import java.io.IOException;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.klu.lms.model.Course;
import com.klu.lms.model.Enrollment;
import com.klu.lms.model.Material;
import com.klu.lms.model.MaterialCategory;
import com.klu.lms.model.User;
import com.klu.lms.repository.CourseRepository;
import com.klu.lms.repository.EnrollmentRepository;
import com.klu.lms.repository.MaterialRepository;
import com.klu.lms.repository.UserRepository;

@Service
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    public MaterialService(
        MaterialRepository materialRepository,
        CourseRepository courseRepository,
        UserRepository userRepository,
        EnrollmentRepository enrollmentRepository,
        FileStorageService fileStorageService,
        NotificationService notificationService
    ) {
        this.materialRepository = materialRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.fileStorageService = fileStorageService;
        this.notificationService = notificationService;
    }

    public Material uploadMaterial(Long courseId, String title, MultipartFile file, String instructorEmail) throws IOException {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        User instructor = userRepository.findByEmail(instructorEmail)
            .orElseThrow(() -> new IllegalArgumentException("Instructor not found"));

        if (course.getInstructor() == null || !course.getInstructor().getId().equals(instructor.getId())) {
            throw new IllegalArgumentException("You can only upload materials for your own courses");
        }

        FileStorageService.StoredFile storedFile = fileStorageService.store(file, "materials");

        Material material = new Material();
        material.setCourse(course);
        material.setCategory(MaterialCategory.COURSE);
        material.setTitle((title == null || title.isBlank()) ? storedFile.originalFileName() : title.trim());
        material.setUploadedBy(instructor);
        material.setOriginalFileName(storedFile.originalFileName());
        material.setStoredFileName(storedFile.storedFileName());
        material.setContentType(storedFile.contentType());
        material.setFilePath(storedFile.filePath());

        Material savedMaterial = materialRepository.save(material);

        List<Enrollment> enrollments = enrollmentRepository.findByCourse(course);
        for (Enrollment enrollment : enrollments) {
            notificationService.sendNotification(
                enrollment.getStudent().getEmail(),
                "New course material uploaded",
                "A new material \"" + savedMaterial.getTitle() + "\" was uploaded for " + course.getTitle() + "."
            );
        }
        return savedMaterial;
    }

    public Material uploadLibraryMaterial(String title, MultipartFile file, String adminEmail) throws IOException {
        User admin = userRepository.findByEmail(adminEmail)
            .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        FileStorageService.StoredFile storedFile = fileStorageService.store(file, "library");

        Material material = new Material();
        material.setCourse(null);
        material.setCategory(MaterialCategory.LIBRARY);
        material.setTitle((title == null || title.isBlank()) ? storedFile.originalFileName() : title.trim());
        material.setUploadedBy(admin);
        material.setOriginalFileName(storedFile.originalFileName());
        material.setStoredFileName(storedFile.storedFileName());
        material.setContentType(storedFile.contentType());
        material.setFilePath(storedFile.filePath());

        return materialRepository.save(material);
    }

    public List<Material> getMaterialsForCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        return materialRepository.findByCourseAndCategory(course, MaterialCategory.COURSE);
    }

    public Resource downloadMaterialForInstructor(Long materialId, String instructorEmail) throws IOException {
        Material material = getMaterial(materialId);
        User instructor = userRepository.findByEmail(instructorEmail)
            .orElseThrow(() -> new IllegalArgumentException("Instructor not found"));

        if (material.getCourse() == null
            || material.getCourse().getInstructor() == null
            || !material.getCourse().getInstructor().getId().equals(instructor.getId())) {
            throw new IllegalArgumentException("You can only access materials for your own courses");
        }

        return fileStorageService.loadAsResource(material.getFilePath());
    }

    public void deleteMaterialForInstructor(Long materialId, String instructorEmail) {
        Material material = getMaterial(materialId);
        User instructor = userRepository.findByEmail(instructorEmail)
            .orElseThrow(() -> new IllegalArgumentException("Instructor not found"));

        if (material.getCourse() == null
            || material.getCourse().getInstructor() == null
            || !material.getCourse().getInstructor().getId().equals(instructor.getId())) {
            throw new IllegalArgumentException("You can only delete materials for your own courses");
        }

        try {
            fileStorageService.delete(material.getFilePath());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to delete the stored material file", exception);
        }

        materialRepository.delete(material);
    }

    public List<Material> getStudentMaterialsForCourse(Long courseId, String studentEmail) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        User student = userRepository.findByEmail(studentEmail)
            .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        enrollmentRepository.findByStudentAndCourse(student, course)
            .orElseThrow(() -> new IllegalArgumentException("You are not enrolled in this course"));

        return materialRepository.findByCourseAndCategory(course, MaterialCategory.COURSE);
    }

    public List<Material> getLibraryMaterials() {
        return materialRepository.findByCategory(MaterialCategory.LIBRARY);
    }

    public Resource downloadMaterial(Long materialId) throws IOException {
        Material material = materialRepository.findById(materialId)
            .orElseThrow(() -> new IllegalArgumentException("Material not found"));
        return fileStorageService.loadAsResource(material.getFilePath());
    }

    public Resource downloadMaterialForStudent(Long materialId, String studentEmail) throws IOException {
        Material material = getMaterial(materialId);
        User student = userRepository.findByEmail(studentEmail)
            .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        if (material.getCategory() == MaterialCategory.COURSE) {
            enrollmentRepository.findByStudentAndCourse(student, material.getCourse())
                .orElseThrow(() -> new IllegalArgumentException("You are not enrolled in this course"));
        }

        return fileStorageService.loadAsResource(material.getFilePath());
    }

    public Material getMaterial(Long materialId) {
        return materialRepository.findById(materialId)
            .orElseThrow(() -> new IllegalArgumentException("Material not found"));
    }
}
