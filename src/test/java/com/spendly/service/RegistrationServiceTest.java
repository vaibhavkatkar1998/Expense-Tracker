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
class RegistrationServiceTest {

    @Autowired private RegistrationService registrationService;
    @Autowired private UserRepository userRepository;

    @Test
    void registerCreatesUserWithHashedPassword() {
        User user = registrationService.register("Test User", "svc-test@example.com", "password123");

        assertThat(userRepository.findByEmail("svc-test@example.com")).isPresent();
        assertThat(user.passwordHash()).isNotEqualTo("password123");
        assertThat(new BCryptPasswordEncoder().matches("password123", user.passwordHash())).isTrue();
    }

    @Test
    void registerRejectsDuplicateEmail() {
        registrationService.register("First", "dup@example.com", "password123");
        int countAfterFirst = userRepository.count();

        assertThatThrownBy(() -> registrationService.register("Second", "dup@example.com", "password456"))
                .isInstanceOf(DuplicateEmailException.class);
        assertThat(userRepository.count()).isEqualTo(countAfterFirst);
    }

    @Test
    void registerRejectsBlankName() {
        assertThatThrownBy(() -> registrationService.register("  ", "blank-name@example.com", "password123"))
                .isInstanceOf(RegistrationValidationException.class);
        assertThat(userRepository.findByEmail("blank-name@example.com")).isEmpty();
    }

    @Test
    void registerRejectsMalformedEmail() {
        assertThatThrownBy(() -> registrationService.register("Test User", "not-an-email", "password123"))
                .isInstanceOf(RegistrationValidationException.class);
    }

    @Test
    void registerRejectsShortPassword() {
        assertThatThrownBy(() -> registrationService.register("Test User", "short-pw@example.com", "abc"))
                .isInstanceOf(RegistrationValidationException.class);
        assertThat(userRepository.findByEmail("short-pw@example.com")).isEmpty();
    }

    @Test
    void registerTreatsEmailAsCaseInsensitiveDuplicate() {
        registrationService.register("First", "Foo@Example.com", "password123");

        assertThatThrownBy(() -> registrationService.register("Second", "foo@example.com", "password456"))
                .isInstanceOf(DuplicateEmailException.class);
    }
}
