package com.spendly.controller;

import com.spendly.model.User;
import com.spendly.service.InvalidCredentialsException;
import com.spendly.service.LoginService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LoginController {

    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password,
                         HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        try {
            User user = loginService.authenticate(email, password);
            session.setAttribute("userId", user.id());
            session.setAttribute("userName", user.name());
            redirectAttributes.addFlashAttribute("success", "Logged in successfully!");
            return "redirect:/";
        } catch (InvalidCredentialsException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("email", email);
            return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
