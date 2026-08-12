package com.spendly.controller;

import com.spendly.service.DuplicateEmailException;
import com.spendly.service.RegistrationService;
import com.spendly.service.RegistrationValidationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping("/register")
    public String showRegistrationForm() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String name, @RequestParam String email,
                            @RequestParam String password, Model model,
                            RedirectAttributes redirectAttributes) {
        try {
            registrationService.register(name, email, password);
            redirectAttributes.addFlashAttribute("success", "Registration successful! Please log in.");
            return "redirect:/login";
        } catch (RegistrationValidationException | DuplicateEmailException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            return "register";
        }
    }
}
