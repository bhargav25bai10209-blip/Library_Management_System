package com.library.service;

import com.library.exception.BookNotFoundException;
import com.library.exception.DuplicateEntityException;
import com.library.model.Book;
import com.library.model.BookCategory;
import com.library.observer.LibraryEvent;
import com.library.observer.NotificationService;
import com.library.repository.BookRepository;
import com.library.util.InputValidator;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing book catalog operations, searches, and inventory updates.
 */
public class BookService {

    private final BookRepository bookRepository;
    private final NotificationService notificationService;

    public BookService(BookRepository bookRepository, NotificationService notificationService) {
        this.bookRepository = bookRepository;
        this.notificationService = notificationService;
    }

    public synchronized Book addBook(String isbn, String title, String author, BookCategory category,
                                     int publicationYear, int copies, String rackLocation)
            throws DuplicateEntityException, IllegalArgumentException {
        if (!InputValidator.isValidIsbn(isbn)) {
            throw new IllegalArgumentException("Invalid ISBN format. Provide valid 10 or 13-digit ISBN.");
        }
        if (!InputValidator.isNotEmpty(title)) {
            throw new IllegalArgumentException("Book title cannot be blank.");
        }
        if (!InputValidator.isNotEmpty(author)) {
            throw new IllegalArgumentException("Author name cannot be blank.");
        }
        if (!InputValidator.isPositiveInt(copies)) {
            throw new IllegalArgumentException("Copies must be greater than zero.");
        }
        if (publicationYear < 1450 || publicationYear > 2030) {
            throw new IllegalArgumentException("Invalid publication year: " + publicationYear);
        }

        if (bookRepository.findByIsbn(isbn).isPresent()) {
            throw new DuplicateEntityException("A book with ISBN '" + isbn + "' already exists.");
        }

        Book book = new Book(isbn, title, author, category, publicationYear, copies, rackLocation);
        bookRepository.save(book);

        notificationService.publish(new LibraryEvent(
                LibraryEvent.EventType.BOOK_ADDED,
                "New book cataloged: '" + title + "' by " + author,
                null,
                book
        ));

        return book;
    }

    public synchronized Book updateBook(String isbn, String title, String author, BookCategory category,
                                        int publicationYear, int totalCopies, String rackLocation)
            throws BookNotFoundException, IllegalArgumentException {
        Book existing = getBook(isbn);

        if (!InputValidator.isNotEmpty(title)) {
            throw new IllegalArgumentException("Book title cannot be blank.");
        }
        if (!InputValidator.isNotEmpty(author)) {
            throw new IllegalArgumentException("Author name cannot be blank.");
        }
        if (totalCopies < 0) {
            throw new IllegalArgumentException("Total copies cannot be negative.");
        }

        int copiesDifference = totalCopies - existing.getTotalCopies();
        int newAvailable = existing.getAvailableCopies() + copiesDifference;
        if (newAvailable < 0) {
            throw new IllegalArgumentException("Cannot reduce total copies below currently borrowed copies.");
        }

        existing.setTitle(title);
        existing.setAuthor(author);
        existing.setCategory(category);
        existing.setPublicationYear(publicationYear);
        existing.setTotalCopies(totalCopies);
        existing.setAvailableCopies(newAvailable);
        existing.setRackLocation(rackLocation);

        bookRepository.save(existing);
        return existing;
    }

    public synchronized void deleteBook(String isbn) throws BookNotFoundException, IllegalStateException {
        Book existing = getBook(isbn);
        if (existing.getAvailableCopies() < existing.getTotalCopies()) {
            throw new IllegalStateException("Cannot delete book while copies are currently issued to members.");
        }
        bookRepository.delete(isbn);
    }

    public Book getBook(String isbn) throws BookNotFoundException {
        return bookRepository.findByIsbn(isbn)
                .orElseThrow(() -> new BookNotFoundException("Book not found for ISBN: " + isbn));
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public List<Book> search(String query) {
        return bookRepository.search(query);
    }

    public List<Book> filterByCategory(BookCategory category) {
        return bookRepository.filterByCategory(category);
    }

    public List<Book> getAvailableBooks() {
        return bookRepository.findAvailableBooks();
    }

    public List<Book> getBooksSortedByTitle() {
        return bookRepository.findAll().stream()
                .sorted(Comparator.comparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public List<Book> getBooksSortedByYear() {
        return bookRepository.findAll().stream()
                .sorted(Comparator.comparing(Book::getPublicationYear).reversed())
                .collect(Collectors.toList());
    }
}
