package com.spendly.repository;

import com.spendly.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired private UserRepository userRepository;
    @Autowired private ExpenseRepository expenseRepository;

    @Test
    void insertAndFindByEmail() {
        long id = userRepository.insert("Test User", "test@example.com", "hashed");
        Optional<User> found = userRepository.findByEmail("test@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(id);
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        assertThat(userRepository.findById(999_999L)).isEmpty();
    }

    @Test
    void duplicateEmailFailsUniqueConstraint() {
        userRepository.insert("A", "dup@example.com", "hash1");
        assertThatThrownBy(() -> userRepository.insert("B", "dup@example.com", "hash2"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void expenseWithInvalidUserIdFailsForeignKey() {
        assertThatThrownBy(() ->
                expenseRepository.insert(999_999L, 10.0, "Food", "2026-08-10", "test")
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}
