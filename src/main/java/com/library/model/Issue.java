package com.library.model;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "issues")
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private Book book;

    private LocalDate issueDate;

    private LocalDate dueDate;

    private LocalDate returnDate;

    private int fine;

    private boolean finePaid;

    public Issue() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
    public int getFine() { return fine; }
    public void setFine(int fine) { this.fine = fine; }
    public boolean isFinePaid() { return finePaid; }
    public void setFinePaid(boolean finePaid) { this.finePaid = finePaid; }

    public static final int FINE_PER_DAY = 5;

    /** Returned books use the stored fine; books still out accrue fine live. */
    public int getCurrentFine() {
        if (returnDate != null) return fine;
        return overdueDays() * FINE_PER_DAY;
    }
    public int overdueDays() {
        LocalDate end = (returnDate != null) ? returnDate : LocalDate.now();
        long d = java.time.temporal.ChronoUnit.DAYS.between(dueDate, end);
        return (int) Math.max(0, d);
    }
    public boolean isOverdue() { return returnDate == null && LocalDate.now().isAfter(dueDate); }

}
