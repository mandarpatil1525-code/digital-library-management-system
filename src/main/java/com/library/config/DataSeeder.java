package com.library.config;

import com.library.repository.BookRepository;
import com.library.repository.UserRepository;
import com.library.service.LibraryService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final BookRepository books;
    private final LibraryService service;

    public DataSeeder(UserRepository users, BookRepository books, LibraryService service) {
        this.users = users; this.books = books; this.service = service;
    }

    @Override
    public void run(String... args) {
        if (users.findByEmailIgnoreCase("admin@library.com").isEmpty())
            service.createAdmin("Librarian", "admin@library.com", "admin123");
        if (books.count() == 0) {
            String[][] seed = {
                {"Clean Code", "Robert C. Martin", "9780132350884", "Programming", "3"},
                {"Effective Java", "Joshua Bloch", "9780134685991", "Programming", "2"},
                {"Head First Java", "Kathy Sierra", "9780596009205", "Programming", "2"},
                {"Introduction to Algorithms", "Thomas H. Cormen", "9780262033848", "Computer Science", "2"},
                {"Wings of Fire", "A. P. J. Abdul Kalam", "9788173711466", "Biography", "4"},
                {"The Discovery of India", "Jawaharlal Nehru", "9780143031031", "History", "2"},
                {"Sapiens", "Yuval Noah Harari", "9780099590088", "History", "1"},
                {"The Alchemist", "Paulo Coelho", "9780062315007", "Fiction", "3"},
                {"Malgudi Days", "R. K. Narayan", "9788185986173", "Fiction", "2"},
                {"A Brief History of Time", "Stephen Hawking", "9780553380163", "Science", "1"},
            };
            for (String[] s : seed)
                service.saveBook(null, s[0], s[1], s[2], s[3], Integer.parseInt(s[4]));
        }
    }
}
