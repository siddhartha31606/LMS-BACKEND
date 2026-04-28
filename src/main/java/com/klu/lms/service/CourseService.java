package com.klu.lms.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.klu.lms.dto.CourseRequest;
import com.klu.lms.model.Course;
import com.klu.lms.model.Enrollment;
import com.klu.lms.model.Role;
import com.klu.lms.model.User;
import com.klu.lms.repository.AssignmentRepository;
import com.klu.lms.repository.CourseRepository;
import com.klu.lms.repository.EnrollmentRepository;
import com.klu.lms.repository.MaterialRepository;
import com.klu.lms.repository.SubmissionRepository;
import com.klu.lms.repository.UserRepository;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final MaterialRepository materialRepository;

    public CourseService(
        CourseRepository courseRepository,
        UserRepository userRepository,
        EnrollmentRepository enrollmentRepository,
        AssignmentRepository assignmentRepository,
        SubmissionRepository submissionRepository,
        MaterialRepository materialRepository
    ) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.materialRepository = materialRepository;
    }

    public Course createCourse(CourseRequest request) {
        Course course = new Course();
        course.setTitle(request.getTitle().trim());
        if (request.getInstructorId() != null) {
            User instructor = userRepository.findById(request.getInstructorId())
                .filter(user -> user.getRole() == Role.INSTRUCTOR)
                .orElseThrow(() -> new IllegalArgumentException("Instructor not found"));
            course.setInstructor(instructor);
        }
        return courseRepository.save(course);
    }

    public List<Course> getAllCourses() {
        List<Course> courses = courseRepository.findAll();
        attachEnrollmentCounts(courses);
        return courses;
    }

    public List<Course> getInstructorCourses(String email) {
        User instructor = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Instructor not found"));
        List<Course> courses = courseRepository.findByInstructor(instructor);
        attachEnrollmentCounts(courses);
        return courses;
    }

    public List<Course> getStudentCourses(String email) {
        User student = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        List<Course> courses = enrollmentRepository.findByStudent(student).stream().map(Enrollment::getCourse).toList();
        attachEnrollmentCounts(courses);
        return courses;
    }

    public Enrollment enrollStudent(Long courseId, String studentEmail) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        User student = userRepository.findByEmail(studentEmail)
            .filter(user -> user.getRole() == Role.STUDENT)
            .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        return enrollmentRepository.findByStudentAndCourse(student, course).orElseGet(() -> {
            Enrollment enrollment = new Enrollment();
            enrollment.setCourse(course);
            enrollment.setStudent(student);
            return enrollmentRepository.save(enrollment);
        });
    }

    public void deleteCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        assignmentRepository.findByCourse(course).forEach(submissionRepository::deleteByAssignment);
        assignmentRepository.deleteByCourse(course);
        materialRepository.deleteByCourse(course);
        enrollmentRepository.deleteByCourse(course);
        courseRepository.delete(course);
    }

    private void attachEnrollmentCounts(List<Course> courses) {
        courses.forEach(course -> course.setEnrollmentCount(enrollmentRepository.findByCourse(course).size()));
    }
}
