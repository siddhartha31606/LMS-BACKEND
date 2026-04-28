package com.klu.lms.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.klu.lms.model.PendingRegistration;

public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, String> {

    Optional<PendingRegistration> findByEmail(String email);

    void deleteByEmail(String email);

    void deleteByExpiresAtBefore(LocalDateTime time);
}
