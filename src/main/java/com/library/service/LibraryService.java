package com.library.service;

import com.library.model.*;
import com.library.repository.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class LibraryService {

    public static final int LOAN_DAYS = 14;
    public static final int MAX_BOOKS_PER_USER = 3;

    private final UserRepository users;
    private final BookRepository books;
    private final IssueRepository issues;
    private final BookingRepository bookings;
    private final ContactMessageRepository messages;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public LibraryService(UserRepository users, BookRepository books, IssueRepository issues,
                          BookingRepository bookings, ContactMessageRepository messages) {
        this.users = users; this.books = books; this.issues = issues;
        this.bookings = bookings; this.messages = messages;
    }

    // ---------- Accounts ----------
    public User register(String name, String email, String password) {
        if (name == null || name.isBlank() || email == null || email.isBlank() || password == null || password.length() < 6)
            throw new IllegalArgumentException("Fill all fields; password needs at least 6 characters.");
        if (users.findByEmailIgnoreCase(email.trim()).isPresent())
            throw new IllegalArgumentException("An account with this email already exists.");
        User u = new User();
        u.setName(name.trim()); u.setEmail(email.trim().toLowerCase());
        u.setPassword(encoder.encode(password)); u.setRole("USER");
        u.setActive(true); u.setCreatedAt(LocalDateTime.now());
        return users.save(u);
    }

    public User createAdmin(String name, String email, String password) {
        User u = new User();
        u.setName(name); u.setEmail(email); u.setPassword(encoder.encode(password));
        u.setRole("ADMIN"); u.setActive(true); u.setCreatedAt(LocalDateTime.now());
        return users.save(u);
    }

    public User login(String email, String password) {
        User u = users.findByEmailIgnoreCase(email == null ? "" : email.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));
        if (!encoder.matches(password, u.getPassword()))
            throw new IllegalArgumentException("Invalid email or password.");
        if (!u.isActive())
            throw new IllegalArgumentException("Your account has been disabled. Contact the librarian.");
        return u;
    }

    public List<User> allMembers() {
        return users.findAll().stream().filter(u -> "USER".equals(u.getRole())).collect(Collectors.toList());
    }

    public void toggleMember(Long id) {
        User u = users.findById(id).orElseThrow();
        if ("USER".equals(u.getRole())) { u.setActive(!u.isActive()); users.save(u); }
    }

    // ---------- Books ----------
    public List<Book> catalogue(String q, String category) {
        List<Book> list = (q == null || q.isBlank()) ? books.findAll()
                : books.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(q.trim(), q.trim());
        if (category != null && !category.isBlank())
            list = list.stream().filter(b -> category.equalsIgnoreCase(b.getCategory())).collect(Collectors.toList());
        list.sort(Comparator.comparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    public List<String> categories() { return books.findCategories(); }
    public Book getBook(Long id) { return books.findById(id).orElseThrow(); }

    public void saveBook(Long id, String title, String author, String isbn, String category, int quantity) {
        if (title.isBlank() || author.isBlank() || isbn.isBlank() || category.isBlank() || quantity < 0)
            throw new IllegalArgumentException("All fields are required and quantity cannot be negative.");
        Book b;
        if (id == null) {
            b = new Book(); b.setAvailable(quantity);
        } else {
            b = books.findById(id).orElseThrow();
            int activeOut = b.getQuantity() - b.getAvailable();
            if (quantity < activeOut)
                throw new IllegalArgumentException("Quantity cannot be below the " + activeOut + " copies currently issued.");
            b.setAvailable(quantity - activeOut);
        }
        b.setTitle(title.trim()); b.setAuthor(author.trim()); b.setIsbn(isbn.trim());
        b.setCategory(category.trim()); b.setQuantity(quantity);
        books.save(b);
    }

    public void deleteBook(Long id) {
        if (issues.existsByBookId(id))
            throw new IllegalArgumentException("This book has issue history and cannot be deleted. Set its quantity to 0 instead.");
        bookings.deleteAll(bookings.findByBookId(id));
        books.deleteById(id);
    }

    // ---------- Issue / Return / Booking ----------
    public void issueBook(Long userId, Long bookId) {
        User u = users.findById(userId).orElseThrow();
        Book b = books.findById(bookId).orElseThrow();
        if (b.getAvailable() <= 0)
            throw new IllegalArgumentException("No copies available. You can reserve this book instead.");
        if (issues.findByUserIdAndBookIdAndReturnDateIsNull(userId, bookId).isPresent())
            throw new IllegalArgumentException("You already have this book issued.");
        if (issues.countByUserIdAndReturnDateIsNull(userId) >= MAX_BOOKS_PER_USER)
            throw new IllegalArgumentException("You can hold at most " + MAX_BOOKS_PER_USER + " books at a time.");
        if (unpaidFineTotal(userId) > 0)
            throw new IllegalArgumentException("Clear your unpaid fine of ₹" + unpaidFineTotal(userId) + " before issuing new books.");

        Optional<Booking> mine = bookings.findByUserIdAndBookIdAndStatus(userId, bookId, "ACTIVE");
        long others = bookings.countByBookIdAndStatus(bookId, "ACTIVE") - (mine.isPresent() ? 1 : 0);
        if (mine.isEmpty() && b.getAvailable() <= others)
            throw new IllegalArgumentException("The remaining copies are held for members who reserved this book.");

        Issue i = new Issue();
        i.setUser(u); i.setBook(b);
        i.setIssueDate(LocalDate.now()); i.setDueDate(LocalDate.now().plusDays(LOAN_DAYS));
        issues.save(i);
        b.setAvailable(b.getAvailable() - 1);
        books.save(b);
        mine.ifPresent(bk -> { bk.setStatus("FULFILLED"); bookings.save(bk); });
    }

    /** Returns the fine charged (0 if on time). */
    public int returnBook(Long issueId, Long userId) {
        Issue i = issues.findById(issueId).orElseThrow();
        if (!i.getUser().getId().equals(userId) || i.getReturnDate() != null)
            throw new IllegalArgumentException("Invalid return request.");
        i.setReturnDate(LocalDate.now());
        int fine = i.overdueDays() * Issue.FINE_PER_DAY;
        i.setFine(fine);
        i.setFinePaid(fine == 0);
        issues.save(i);
        Book b = i.getBook();
        b.setAvailable(b.getAvailable() + 1);
        books.save(b);
        return fine;
    }

    public void reserveBook(Long userId, Long bookId) {
        User u = users.findById(userId).orElseThrow();
        Book b = books.findById(bookId).orElseThrow();
        if (b.getAvailable() > 0)
            throw new IllegalArgumentException("Copies are available right now - just issue the book.");
        if (issues.findByUserIdAndBookIdAndReturnDateIsNull(userId, bookId).isPresent())
            throw new IllegalArgumentException("You already have this book issued.");
        if (bookings.findByUserIdAndBookIdAndStatus(userId, bookId, "ACTIVE").isPresent())
            throw new IllegalArgumentException("You have already reserved this book.");
        Booking bk = new Booking();
        bk.setUser(u); bk.setBook(b); bk.setBookingDate(LocalDate.now()); bk.setStatus("ACTIVE");
        bookings.save(bk);
    }

    public void cancelBooking(Long bookingId, Long userId) {
        Booking bk = bookings.findById(bookingId).orElseThrow();
        if (bk.getUser().getId().equals(userId) && "ACTIVE".equals(bk.getStatus())) {
            bk.setStatus("CANCELLED"); bookings.save(bk);
        }
    }

    public List<Issue> issuesOf(Long userId) { return issues.findByUserIdOrderByIssueDateDesc(userId); }
    public List<Booking> bookingsOf(Long userId) { return bookings.findByUserIdOrderByBookingDateDesc(userId); }
    public List<Issue> currentlyIssued() { return issues.findByReturnDateIsNullOrderByDueDateAsc(); }

    // ---------- Fines ----------
    public int unpaidFineTotal(Long userId) {
        return issues.findByUserIdOrderByIssueDateDesc(userId).stream()
                .filter(i -> i.getReturnDate() != null && i.getFine() > 0 && !i.isFinePaid())
                .mapToInt(Issue::getFine).sum();
    }

    public List<Issue> allFines() {
        return issues.findAll().stream().filter(i -> i.getCurrentFine() > 0)
                .sorted(Comparator.comparing(Issue::getDueDate).reversed()).collect(Collectors.toList());
    }

    public void markFinePaid(Long issueId) {
        Issue i = issues.findById(issueId).orElseThrow();
        if (i.getReturnDate() == null)
            throw new IllegalArgumentException("The book is not returned yet; the fine is still accruing.");
        i.setFinePaid(true); issues.save(i);
    }

    public int pendingFineTotal() {
        return issues.findAll().stream().filter(i -> i.getReturnDate() != null && i.getFine() > 0 && !i.isFinePaid())
                .mapToInt(Issue::getFine).sum();
    }

    // ---------- Contact ----------
    public void saveMessage(String name, String email, String subject, String text) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Message cannot be empty.");
        ContactMessage m = new ContactMessage();
        m.setName(name); m.setEmail(email); m.setSubject(subject == null ? "" : subject.trim());
        m.setMessage(text.trim()); m.setCreatedAt(LocalDateTime.now());
        messages.save(m);
    }
    public List<ContactMessage> allMessages() { return messages.findAllByOrderByCreatedAtDesc(); }
    public void deleteMessage(Long id) { messages.deleteById(id); }

    // ---------- Dashboard ----------
    public Map<String, Object> stats() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("books", books.count());
        s.put("members", users.countByRole("USER"));
        List<Issue> out = currentlyIssued();
        s.put("issued", out.size());
        s.put("overdue", out.stream().filter(Issue::isOverdue).count());
        s.put("pendingFines", pendingFineTotal());
        s.put("messages", messages.count());
        return s;
    }
}
