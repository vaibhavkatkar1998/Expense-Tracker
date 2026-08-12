package com.spendly.controller;

import com.spendly.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoginControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void getLoginRendersForm() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void postLoginWithValidCredentialsSetsSessionAndRedirectsToLoginWithFlash() throws Exception {
        long userId = userRepository.insert("Test User", "login-controller-test@example.com",
                passwordEncoder.encode("password123"));

        mockMvc.perform(post("/login")
                        .param("email", "login-controller-test@example.com")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("success"))
                .andExpect(request().sessionAttribute("userId", userId));
    }

    @Test
    void postLoginWithWrongPasswordReRendersFormWithErrorAndNoSession() throws Exception {
        userRepository.insert("Test User", "wrong-pw-controller@example.com",
                passwordEncoder.encode("password123"));

        mockMvc.perform(post("/login")
                        .param("email", "wrong-pw-controller@example.com")
                        .param("password", "wrongpass"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("error"))
                .andExpect(request().sessionAttributeDoesNotExist("userId"));
    }

    @Test
    void postLoginWithUnknownEmailReRendersFormWithErrorAndNoSession() throws Exception {
        mockMvc.perform(post("/login")
                        .param("email", "no-such-user-controller@example.com")
                        .param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("error"))
                .andExpect(request().sessionAttributeDoesNotExist("userId"));
    }

    @Test
    void getLogoutInvalidatesSessionAndRedirectsToLogin() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);

        mockMvc.perform(get("/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void registerThenLoginWithSameCredentialsSucceeds() throws Exception {
        mockMvc.perform(post("/register")
                        .param("name", "Round Trip User")
                        .param("email", "round-trip@example.com")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        long userId = userRepository.findByEmail("round-trip@example.com").orElseThrow().id();

        mockMvc.perform(post("/login")
                        .param("email", "round-trip@example.com")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(request().sessionAttribute("userId", userId));
    }
}
