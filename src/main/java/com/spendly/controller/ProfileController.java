package com.spendly.controller;

import com.spendly.model.User;
import com.spendly.service.ProfileService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class ProfileController {

    private static final int EXPENSES_PAGE_SIZE = 5;

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        User user = profileService.getUser(userId);
        model.addAttribute("user", user);
        model.addAttribute("initials", profileService.getInitials(user));
        model.addAttribute("stats", profileService.getSummaryStats(userId));
        model.addAttribute("categoryTotals", profileService.getCategoryBreakdown(userId));
        model.addAttribute("expenses", profileService.getRecentExpenses(userId, 1, EXPENSES_PAGE_SIZE));
        return "profile";
    }

    @GetMapping("/profile/expenses")
    public String expenses(@RequestParam(defaultValue = "1") int page, HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        model.addAttribute("expenses", profileService.getRecentExpenses(userId, page, EXPENSES_PAGE_SIZE));
        return "profile :: expenseRows";
    }
}
