package com.library.controller;

import com.library.model.User;
import com.library.service.LibraryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    private final LibraryService service;
    public AuthController(LibraryService service) { this.service = service; }

    @GetMapping("/")
    public String home(HttpSession session) {
        User u = (User) session.getAttribute("user");
        if (u == null) return "redirect:/login";
        return "ADMIN".equals(u.getRole()) ? "redirect:/admin/dashboard" : "redirect:/user/catalog";
    }

    @GetMapping("/login")
    public String loginPage() { return "login"; }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password,
                        HttpSession session, Model model) {
        try {
            User u = service.login(email, password);
            session.setAttribute("user", u);
            return "redirect:/";
        } catch (IllegalArgumentException e) {
            model.addAttribute("err", e.getMessage());
            return "login";
        }
    }

    @GetMapping("/register")
    public String registerPage() { return "register"; }

    @PostMapping("/register")
    public String register(@RequestParam String name, @RequestParam String email,
                           @RequestParam String password, Model model, RedirectAttributes ra) {
        try {
            service.register(name, email, password);
            ra.addFlashAttribute("msg", "Registration successful. Please log in.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("err", e.getMessage());
            return "register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
