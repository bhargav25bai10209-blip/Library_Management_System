package com.library.repository;

import com.library.model.Book;
import com.library.model.BookCategory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Repository for managing book inventory with in-memory cache and persistence syncing.
 */
public class BookRepository {

    private final Map<String, Book> bookMap = new ConcurrentHashMap<>();
    private final FileDataStore fileDataStore;

    public BookRepository(FileDataStore fileDataStore) {
        this.fileDataStore = fileDataStore;
        reload();
    }

    public void reload() {
        bookMap.clear();
        List<Book> books = fileDataStore.loadBooks();
        for (Book b : books) {
            bookMap.put(b.getIsbn().toLowerCase().trim(), b);
        }
    }

    public synchronized void syncToFile() {
        fileDataStore.saveBooks(new ArrayList<>(bookMap.values()));
    }

    public Optional<Book> findByIsbn(String isbn) {
        if (isbn == null) return Optional.empty();
        return Optional.ofNullable(bookMap.get(isbn.toLowerCase().trim()));
    }

    public List<Book> findAll() {
        return new ArrayList<>(bookMap.values());
    }

    public synchronized Book save(Book book) {
        bookMap.put(book.getIsbn().toLowerCase().trim(), book);
        syncToFile();
        return book;
    }

    public synchronized boolean delete(String isbn) {
        if (isbn == null) return false;
        Book removed = bookMap.remove(isbn.toLowerCase().trim());
        if (removed != null) {
            syncToFile();
            return true;
        }
        return false;
    }

    public List<Book> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return findAll();
        }
        String q = query.toLowerCase().trim();
        return bookMap.values().stream()
                .filter(b -> b.getTitle().toLowerCase().contains(q)
                        || b.getAuthor().toLowerCase().contains(q)
                        || b.getIsbn().toLowerCase().contains(q)
                        || b.getCategory().getDisplayName().toLowerCase().contains(q))
                .sorted(Comparator.comparing(Book::getTitle))
                .collect(Collectors.toList());
    }

    public List<Book> filterByCategory(BookCategory category) {
        return bookMap.values().stream()
                .filter(b -> b.getCategory() == category)
                .sorted(Comparator.comparing(Book::getTitle))
                .collect(Collectors.toList());
    }

    public List<Book> findAvailableBooks() {
        return bookMap.values().stream()
                .filter(Book::isAvailable)
                .sorted(Comparator.comparing(Book::getTitle))
                .collect(Collectors.toList());
    }
}
