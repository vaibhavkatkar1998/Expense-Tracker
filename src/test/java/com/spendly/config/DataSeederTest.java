package com.spendly.config;

import com.spendly.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DataSeederTest {

    @Autowired private DataSeeder dataSeeder;
    @Autowired private UserRepository userRepository;

    @Test
    void seedsDemoUserAndDoesNotDuplicateOnRerun() {
        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(userRepository.findByEmail("demo@spendly.com")).isPresent();

        dataSeeder.run();

        assertThat(userRepository.count()).isEqualTo(1);
    }
}
