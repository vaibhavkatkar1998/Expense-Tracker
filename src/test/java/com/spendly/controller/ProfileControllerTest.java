package com.spendly.controller;

import com.spendly.model.ProfileStats;
import com.spendly.repository.ExpenseRepository;
import com.spendly.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProfileControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ExpenseRepository expenseRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private MockHttpSession sessionForNewUser(String email) {
        long userId = userRepository.insert("Test User", email, passwordEncoder.encode("password123"));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", userId);
        return session;
    }

    @Test
    void getProfileWithoutSessionRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void getProfileWithSessionReturnsOkAndProfileView() throws Exception {
        MockHttpSession session = sessionForNewUser("profile-view-test@example.com");

        mockMvc.perform(get("/profile").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"));
    }

    @Test
    void getProfileWithSessionPopulatesModelAttributes() throws Exception {
        MockHttpSession session = sessionForNewUser("profile-model-test@example.com");

        mockMvc.perform(get("/profile").session(session))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("stats"))
                .andExpect(model().attributeExists("categoryTotals"))
                .andExpect(model().attributeExists("expenses"))
                .andExpect(model().attributeExists("initials"));
    }

    @Test
    void getExpensesPageTwoReturnsAdditionalRows() throws Exception {
        MockHttpSession session = sessionForNewUser("profile-expenses-page-test@example.com");
        long userId = (Long) session.getAttribute("userId");

        for (int i = 0; i < 7; i++) {
            expenseRepository.insert(userId, 10.0 + i, "Food", "2026-01-" + String.format("%02d", i + 1),
                    "Expense number " + i);
        }

        mockMvc.perform(get("/profile/expenses").param("page", "2").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("profile :: expenseRows"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Expense number 0")));
    }

    @Test
    void getExpensesWithoutSessionReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/profile/expenses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProfileWithSessionPopulatesSummaryStats() throws Exception {
        MockHttpSession session = sessionForNewUser("profile-stats-test@example.com");
        long userId = (Long) session.getAttribute("userId");

        expenseRepository.insert(userId, 50.0, "Food", "2026-01-01", "Groceries");
        expenseRepository.insert(userId, 30.0, "Food", "2026-01-02", "Dinner");
        expenseRepository.insert(userId, 20.0, "Transport", "2026-01-03", "Bus");

        mockMvc.perform(get("/profile").session(session))
                .andExpect(model().attribute("stats", new ProfileStats(100.0, 3, "Food")));
    }

    @Test
    void getProfileReturnsCategoryTotalsSortedDescending() throws Exception {
        MockHttpSession session = sessionForNewUser("profile-category-breakdown-test@example.com");
        long userId = (Long) session.getAttribute("userId");

        expenseRepository.insert(userId, 50.0, "Food", "2026-01-01", "Groceries");
        expenseRepository.insert(userId, 30.0, "Food", "2026-01-02", "Snacks");
        expenseRepository.insert(userId, 200.0, "Rent", "2026-01-03", "Monthly rent");
        expenseRepository.insert(userId, 15.0, "Transport", "2026-01-04", "Bus fare");

        var result = mockMvc.perform(get("/profile").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("categoryTotals"))
                .andReturn();

        @SuppressWarnings("unchecked")
        java.util.Map<String, Double> categoryTotals =
                (java.util.Map<String, Double>) result.getModelAndView().getModel().get("categoryTotals");

        org.junit.jupiter.api.Assertions.assertEquals(
                java.util.List.of("Rent", "Food", "Transport"),
                java.util.List.copyOf(categoryTotals.keySet()));
        org.junit.jupiter.api.Assertions.assertEquals(200.0, categoryTotals.get("Rent"));
        org.junit.jupiter.api.Assertions.assertEquals(80.0, categoryTotals.get("Food"));
        org.junit.jupiter.api.Assertions.assertEquals(15.0, categoryTotals.get("Transport"));
    }

}
