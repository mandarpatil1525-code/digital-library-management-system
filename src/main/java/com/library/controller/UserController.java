package com.library.controller;

import com.library.model.User;
import com.library.service.LibraryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/user")
public class UserController {
    private final LibraryService service;
    public UserController(LibraryService service) { this.service = service; }

    private Long uid(HttpSession s) { return ((User) s.getAttribute("user")).getId(); }

    @GetMapping("/catalog")
    public String catalog(@RequestParam(required = false) String q,
                          @RequestParam(required = false) String category, Model model) {
        model.addAttribute("books", service.catalogue(q, category));
        model.addAttribute("categories", service.categories());
        model.addAttribute("q", q);
        model.addAttribute("category", category);
        return "user/catalog";
    }

    @PostMapping("/issue/{bookId}")
    public String issue(@PathVariable Long bookId, HttpSession s, RedirectAttributes ra) {
        try {
            service.issueBook(uid(s), bookId);
            ra.addFlashAttribute("msg", "Book issued. Due in " + LibraryService.LOAN_DAYS + " days.");
            return "redirect:/user/my-books";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("err", e.getMessage());
            return "redirect:/user/catalog";
        }
    }

    @PostMapping("/reserve/{bookId}")
    public String reserve(@PathVariable Long bookId, HttpSession s, RedirectAttributes ra) {
        try {
            service.reserveBook(uid(s), bookId);
            ra.addFlashAttribute("msg", "Book reserved. You can issue it once a copy is returned.");
            return "redirect:/user/my-books";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("err", e.getMessage());
            return "redirect:/user/catalog";
        }
    }

    @GetMapping("/my-books")
    public String myBooks(HttpSession s, Model model) {
        Long id = uid(s);
        model.addAttribute("issues", service.issuesOf(id));
        model.addAttribute("bookings", service.bookingsOf(id));
        model.addAttribute("unpaid", service.unpaidFineTotal(id));
        return "user/my-books";
    }

    @PostMapping("/return/{issueId}")
    public String doReturn(@PathVariable Long issueId, HttpSession s, RedirectAttributes ra) {
        try {
            int fine = service.returnBook(issueId, uid(s));
            if (fine > 0) ra.addFlashAttribute("err", "Book returned late. Fine charged: ₹" + fine + ". Please pay it at the desk.");
            else ra.addFlashAttribute("msg", "Book returned on time. Thank you!");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("err", e.getMessage());
        }
        return "redirect:/user/my-books";
    }

    @PostMapping("/cancel-booking/{id}")
    public String cancel(@PathVariable Long id, HttpSession s, RedirectAttributes ra) {
        service.cancelBooking(id, uid(s));
        ra.addFlashAttribute("msg", "Reservation cancelled.");
        return "redirect:/user/my-books";
    }

    @GetMapping("/contact")
    public String contactPage() { return "user/contact"; }

    @PostMapping("/contact")
    public String contact(@RequestParam String subject, @RequestParam String message,
                          HttpSession s, RedirectAttributes ra) {
        User u = (User) s.getAttribute("user");
        try {
            service.saveMessage(u.getName(), u.getEmail(), subject, message);
            ra.addFlashAttribute("msg", "Your message was sent to the librarian.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("err", e.getMessage());
        }
        return "redirect:/user/contact";
    }
}
