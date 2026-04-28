package com.klu.lms.service;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.klu.lms.dto.AuthResponse;
import com.klu.lms.dto.OtpRequest;
import com.klu.lms.dto.OtpVerificationRequest;
import com.klu.lms.jwt.JwtUtil;
import com.klu.lms.model.PendingRegistration;
import com.klu.lms.model.Role;
import com.klu.lms.model.User;
import com.klu.lms.repository.PendingRegistrationRepository;
import com.klu.lms.repository.UserRepository;

@Service
public class OtpRegistrationService {

    private static final int OTP_VALIDITY_MINUTES = 10;

    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final JwtUtil jwtUtil;

    public OtpRegistrationService(
        PendingRegistrationRepository pendingRegistrationRepository,
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        NotificationService notificationService,
        JwtUtil jwtUtil
    ) {
        this.pendingRegistrationRepository = pendingRegistrationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public void requestOtp(OtpRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        pendingRegistrationRepository.deleteByExpiresAtBefore(LocalDateTime.now());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("An account with this email already exists");
        }

        if (request.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Admin accounts cannot be created through public sign up");
        }

        String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1000000));

        PendingRegistration pending = pendingRegistrationRepository.findByEmail(normalizedEmail)
            .orElseGet(PendingRegistration::new);
        pending.setEmail(normalizedEmail);
        pending.setFullName(request.getFullName().trim());
        pending.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        pending.setRole(request.getRole());
        pending.setOtpCode(otp);
        pending.setCreatedAt(LocalDateTime.now());
        pending.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES));
        pendingRegistrationRepository.save(pending);

        notificationService.sendRequiredNotification(
            normalizedEmail,
            "Your LMS account verification code",
            "Your OTP for LMS account creation is: " + otp + ". It will expire in " + OTP_VALIDITY_MINUTES + " minutes."
        );
    }

    @Transactional
    public AuthResponse verifyOtpAndCreateUser(OtpVerificationRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        PendingRegistration pending = pendingRegistrationRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new IllegalArgumentException("No pending registration found for this email"));

        if (pending.getExpiresAt().isBefore(LocalDateTime.now())) {
            pendingRegistrationRepository.delete(pending);
            throw new IllegalArgumentException("OTP expired. Please request a new OTP");
        }

        if (!pending.getOtpCode().equals(request.getOtp().trim())) {
            throw new IllegalArgumentException("Invalid OTP. Please try again");
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            pendingRegistrationRepository.delete(pending);
            throw new IllegalArgumentException("An account with this email already exists");
        }

        User user = new User();
        user.setFullName(pending.getFullName());
        user.setEmail(pending.getEmail());
        user.setPassword(pending.getPasswordHash());
        user.setRole(pending.getRole());
        user.setActive(true);

        User savedUser = userRepository.save(user);
        pendingRegistrationRepository.delete(pending);

        notificationService.sendNotification(
            savedUser.getEmail(),
            "Welcome to the LMS platform",
            "Hello " + savedUser.getFullName() + ", your account has been verified successfully. You can now sign in to the LMS platform."
        );

        String token = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getRole().name());
        return new AuthResponse(
            token,
            savedUser.getEmail(),
            savedUser.getFullName(),
            savedUser.getRole().name(),
            "Account verified successfully"
        );
    }
}
