package com.spendly.service;

import com.spendly.model.User;
import com.spendly.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class RegistrationService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public RegistrationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(String name, String email, String rawPassword) {
        String trimmedName = name == null ? "" : name.trim();
        if (trimmedName.isEmpty()) {
            throw new RegistrationValidationException("Name is required");
        }

        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new RegistrationValidationException("Enter a valid email address");
        }

        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new RegistrationValidationException("Password must be at least 8 characters");
        }

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new DuplicateEmailException("An account with this email already exists");
        }

        String passwordHash = passwordEncoder.encode(rawPassword);
        long id = userRepository.insert(trimmedName, normalizedEmail, passwordHash);
        return userRepository.findById(id).orElseThrow();
    }
}
