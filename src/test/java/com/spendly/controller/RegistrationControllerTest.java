package com.spendly.controller;

import com.spendly.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RegistrationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;

    @Test
    void getRegisterRendersForm() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void postRegisterWithValidDataCreatesUserAndRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/register")
                        .param("name", "Test User")
                        .param("email", "controller-test@example.com")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("success"));

        assertThat(userRepository.findByEmail("controller-test@example.com")).isPresent();
        String hash = userRepository.findByEmail("controller-test@example.com").get().passwordHash();
        assertThat(new BCryptPasswordEncoder().matches("password123", hash)).isTrue();
    }

    @Test
    void postRegisterWithDuplicateEmailReRendersFormWithError() throws Exception {
        userRepository.insert("Existing", "dup-controller@example.com", "hash");
        int countBefore = userRepository.count();

        mockMvc.perform(post("/register")
                        .param("name", "New User")
                        .param("email", "dup-controller@example.com")
                        .param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));

        assertThat(userRepository.count()).isEqualTo(countBefore);
    }

    @Test
    void postRegisterWithBlankNameReRendersFormWithError() throws Exception {
        mockMvc.perform(post("/register")
                        .param("name", "  ")
                        .param("email", "blank-name-controller@example.com")
                        .param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));

        assertThat(userRepository.findByEmail("blank-name-controller@example.com")).isEmpty();
    }

    @Test
    void postRegisterWithMalformedEmailReRendersFormWithError() throws Exception {
        mockMvc.perform(post("/register")
                        .param("name", "Test User")
                        .param("email", "not-an-email")
                        .param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void postRegisterWithShortPasswordReRendersFormWithError() throws Exception {
        mockMvc.perform(post("/register")
                        .param("name", "Test User")
                        .param("email", "short-pw-controller@example.com")
                        .param("password", "abc"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));

        assertThat(userRepository.findByEmail("short-pw-controller@example.com")).isEmpty();
    }
}
