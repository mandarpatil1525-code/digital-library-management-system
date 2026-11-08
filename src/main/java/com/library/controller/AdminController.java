package com.library.controller;

import com.library.model.Book;
import com.library.service.LibraryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final LibraryService service;
    public AdminController(LibraryService service) { this.service = service; }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats", service.stats());
        return "admin/dashboard";
    }

    // ----- Books -----
    @GetMapping("/books")
    public String books(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("books", service.catalogue(q, null));
        model.addAttribute("q", q);
        return "admin/books";
    }

    @GetMapping("/books/new")
    public String newBook(Model model) {
        model.addAttribute("book", new Book());
        return "admin/book-form";
    }

    @GetMapping("/books/edit/{id}")
    public String editBook(@PathVariable Long id, Model model) {
        model.addAttribute("book", service.getBook(id));
        return "admin/book-form";
    }

    @PostMapping("/books/save")
    public String saveBook(@RequestParam(required = false) Long id, @RequestParam String title,
                           @RequestParam String author, @RequestParam String isbn,
                           @RequestParam String category, @RequestParam int quantity,
                           RedirectAttributes ra) {
        try {
            service.saveBook(id, title, author, isbn, category, quantity);
            ra.addFlashAttribute("msg", "Book saved.");
            return "redirect:/admin/books";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("err", e.getMessage());
            return id == null ? "redirect:/admin/books/new" : "redirect:/admin/books/edit/" + id;
        }
    }

    @PostMapping("/books/delete/{id}")
    public String deleteBook(@PathVariable Long id, RedirectAttributes ra) {
        try {
            service.deleteBook(id);
            ra.addFlashAttribute("msg", "Book deleted.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("err", e.getMessage());
        }
        return "redirect:/admin/books";
    }

    // ----- Issued books -----
    @GetMapping("/issued")
    public String issued(Model model) {
        model.addAttribute("issues", service.currentlyIssued());
        return "admin/issued";
    }

    // ----- Members -----
    @GetMapping("/members")
    public String members(Model model) {
        model.addAttribute("members", service.allMembers());
        return "admin/members";
    }

    @PostMapping("/members/toggle/{id}")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        service.toggleMember(id);
        ra.addFlashAttribute("msg", "Member status updated.");
        return "redirect:/admin/members";
    }

    // ----- Fines -----
    @GetMapping("/fines")
    public String fines(Model model) {
        model.addAttribute("fines", service.allFines());
        model.addAttribute("pending", service.pendingFineTotal());
        return "admin/fines";
    }

    @PostMapping("/fines/pay/{id}")
    public String pay(@PathVariable Long id, RedirectAttributes ra) {
        try {
            service.markFinePaid(id);
            ra.addFlashAttribute("msg", "Fine marked as paid.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("err", e.getMessage());
        }
        return "redirect:/admin/fines";
    }

    // ----- Messages -----
    @GetMapping("/messages")
    public String messages(Model model) {
        model.addAttribute("messages", service.allMessages());
        return "admin/messages";
    }

    @PostMapping("/messages/delete/{id}")
    public String deleteMessage(@PathVariable Long id, RedirectAttributes ra) {
        service.deleteMessage(id);
        ra.addFlashAttribute("msg", "Message deleted.");
        return "redirect:/admin/messages";
    }
}
