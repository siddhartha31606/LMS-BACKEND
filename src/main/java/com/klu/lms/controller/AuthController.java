package com.klu.lms.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.klu.lms.dto.AuthResponse;
import com.klu.lms.dto.LoginRequest;
import com.klu.lms.dto.MessageResponse;
import com.klu.lms.dto.OtpRequest;
import com.klu.lms.dto.OtpVerificationRequest;
import com.klu.lms.dto.UserProfileResponse;
import com.klu.lms.jwt.JwtUtil;
import com.klu.lms.model.User;
import com.klu.lms.repository.UserRepository;
import com.klu.lms.service.OtpRegistrationService;
import com.klu.lms.service.UserProfileService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String AUTH_COOKIE_NAME = "lms_auth";

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final OtpRegistrationService otpRegistrationService;
    private final UserProfileService userProfileService;
    private final long jwtExpirationMs;

    public AuthController(
        AuthenticationManager authenticationManager,
        JwtUtil jwtUtil,
        UserRepository userRepository,
        OtpRegistrationService otpRegistrationService,
        UserProfileService userProfileService,
        @Value("${app.jwt.expiration-ms}") long jwtExpirationMs
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.otpRegistrationService = otpRegistrationService;
        this.userProfileService = userProfileService;
        this.jwtExpirationMs = jwtExpirationMs;
    }

    @GetMapping("/health")
    public String health() {
        return "Auth API working";
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Validated @RequestBody LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new AuthResponse(null, null, null, null, "Invalid email or password"));
        }

        if (!Boolean.TRUE.equals(user.getActive())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new AuthResponse(null, user.getEmail(), user.getFullName(), user.getRole().name(), "Your account is inactive. Please contact the admin."));
        }

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    normalizedEmail,
                    request.getPassword()
                )
            );
        } catch (DisabledException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new AuthResponse(null, user.getEmail(), user.getFullName(), user.getRole().name(), "Your account is inactive. Please contact the admin."));
        } catch (AuthenticationException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new AuthResponse(null, null, null, null, "Invalid email or password"));
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        AuthResponse response = new AuthResponse(
            null,
            user.getEmail(),
            user.getFullName(),
            user.getRole().name(),
            "Login successful"
        );
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, buildAuthCookie(token).toString())
            .body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(userProfileService.getProfile(principal.getName()));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout() {
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, clearAuthCookie().toString())
            .body(new MessageResponse("Logged out successfully"));
    }

    @PostMapping("/register/request-otp")
    public ResponseEntity<MessageResponse> requestOtp(@Validated @RequestBody OtpRequest request) {
        otpRegistrationService.requestOtp(request);
        return ResponseEntity.ok(new MessageResponse("OTP sent successfully. Please verify it to create your account."));
    }

    @PostMapping("/register/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@Validated @RequestBody OtpVerificationRequest request) {
        AuthResponse response = otpRegistrationService.verifyOtpAndCreateUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .header(HttpHeaders.SET_COOKIE, buildAuthCookie(response.getToken()).toString())
            .body(new AuthResponse(null, response.getEmail(), response.getFullName(), response.getRole(), response.getMessage()));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new AuthResponse(null, null, null, null, "Direct registration is disabled. Please request and verify OTP to create your account."));
    }

    private ResponseCookie buildAuthCookie(String token) {
        return ResponseCookie.from(AUTH_COOKIE_NAME, token)
            .httpOnly(true)
            .secure(false)
            .sameSite("Lax")
            .path("/")
            .maxAge(jwtExpirationMs / 1000)
            .build();
    }

    private ResponseCookie clearAuthCookie() {
        return ResponseCookie.from(AUTH_COOKIE_NAME, "")
            .httpOnly(true)
            .secure(false)
            .sameSite("Lax")
            .path("/")
            .maxAge(0)
            .build();
    }
}
