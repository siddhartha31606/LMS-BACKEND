package com.klu.lms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.klu.lms.model.Course;
import com.klu.lms.model.Material;
import com.klu.lms.model.MaterialCategory;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> findByCourse(Course course);
    List<Material> findByCourseAndCategory(Course course, MaterialCategory category);
    List<Material> findByCategory(MaterialCategory category);
    void deleteByCourse(Course course);
}
