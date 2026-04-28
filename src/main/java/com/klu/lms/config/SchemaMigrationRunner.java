package com.klu.lms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SchemaMigrationRunner implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaMigrationRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public SchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        LOGGER.info("Ensuring materials.course_id allows NULL values for library resources");
        tryAlterCourseIdNullable("ALTER TABLE materials MODIFY COLUMN course_id BIGINT NULL");
        tryAlterCourseIdNullable("ALTER TABLE materials CHANGE course_id course_id BIGINT NULL");
        LOGGER.info("materials.course_id is ready for nullable library resources");
    }

    private void tryAlterCourseIdNullable(String sql) {
        try {
            jdbcTemplate.execute(sql);
            LOGGER.info("Schema migration executed: {}", sql);
        } catch (Exception ex) {
            LOGGER.warn("Schema migration attempt failed: {}. Reason: {}", sql, ex.getMessage());
        }
    }
}
