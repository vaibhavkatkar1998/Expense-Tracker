package com.spendly.controller;

import com.spendly.service.ProfileService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", profileService.getUser());
        model.addAttribute("summary", profileService.getSummary());
        model.addAttribute("initials", profileService.getInitials());
        model.addAttribute("topCategory", profileService.getTopCategory());
        return "profile";
    }
}
