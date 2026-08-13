package com.spendly.service;

import com.spendly.model.User;
import com.spendly.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class LoginServiceTest {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";

    @Autowired private LoginService loginService;
    @Autowired private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void authenticateWithValidCredentialsReturnsUser() {
        userRepository.insert("Test User", "login-svc-test@example.com", passwordEncoder.encode("password123"));

        User user = loginService.authenticate("login-svc-test@example.com", "password123");

        assertThat(user.email()).isEqualTo("login-svc-test@example.com");
    }

    @Test
    void authenticateWithWrongPasswordThrowsInvalidCredentialsException() {
        userRepository.insert("Test User", "wrong-pw@example.com", passwordEncoder.encode("password123"));

        assertThatThrownBy(() -> loginService.authenticate("wrong-pw@example.com", "wrongpass"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage(INVALID_CREDENTIALS_MESSAGE);
    }

    @Test
    void authenticateWithUnknownEmailThrowsInvalidCredentialsException() {
        assertThatThrownBy(() -> loginService.authenticate("no-such-user@example.com", "password123"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage(INVALID_CREDENTIALS_MESSAGE);
    }

    @Test
    void authenticateNormalizesEmailCaseAndWhitespace() {
        userRepository.insert("Test User", "normalize-me@example.com", passwordEncoder.encode("password123"));

        User user = loginService.authenticate("  Normalize-Me@Example.com  ", "password123");

        assertThat(user.email()).isEqualTo("normalize-me@example.com");
    }
}
