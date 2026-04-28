package com.klu.lms.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.klu.lms.model.Role;
import com.klu.lms.model.User;
import com.klu.lms.repository.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedUser("Admin Guy", "admin.lms@gmail.com", "admin123", Role.ADMIN);
        seedUser("Alice Smith", "alice.smith@gmail.com", "alice123", Role.INSTRUCTOR);
        seedUser("Eve Carter", "eve.carter@gmail.com", "eve123", Role.INSTRUCTOR);
        seedUser("Bob Jones", "bob.jones@gmail.com", "bob123", Role.STUDENT);
    }

    private void seedUser(String fullName, String email, String rawPassword, Role role) {
        if (userRepository.existsByEmail(email)) {
            return;
        }

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setActive(true);
        userRepository.save(user);
    }
}
