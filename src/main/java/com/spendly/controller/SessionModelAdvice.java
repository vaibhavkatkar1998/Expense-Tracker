package com.spendly.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class SessionModelAdvice {

    @ModelAttribute("sessionUserName")
    public String sessionUserName(HttpSession session) {
        return (String) session.getAttribute("userName");
    }
}
