package com.library.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents a library book entity in the catalog.
 */
public class Book implements Serializable {
    private static final long serialVersionUID = 1L;

    private String isbn;
    private String title;
    private String author;
    private BookCategory category;
    private int publicationYear;
    private int totalCopies;
    private int availableCopies;
    private String rackLocation;
    private LocalDateTime dateAdded;

    public Book() {
        this.dateAdded = LocalDateTime.now();
    }

    public Book(String isbn, String title, String author, BookCategory category,
                int publicationYear, int totalCopies, String rackLocation) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.category = category;
        this.publicationYear = publicationYear;
        this.totalCopies = totalCopies;
        this.availableCopies = totalCopies;
        this.rackLocation = rackLocation;
        this.dateAdded = LocalDateTime.now();
    }

    public Book(String isbn, String title, String author, BookCategory category,
                int publicationYear, int totalCopies, int availableCopies,
                String rackLocation, LocalDateTime dateAdded) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.category = category;
        this.publicationYear = publicationYear;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
        this.rackLocation = rackLocation;
        this.dateAdded = (dateAdded != null) ? dateAdded : LocalDateTime.now();
    }

    public boolean isAvailable() {
        return availableCopies > 0;
    }

    public synchronized boolean decrementCopies() {
        if (availableCopies > 0) {
            availableCopies--;
            return true;
        }
        return false;
    }

    public synchronized void incrementCopies() {
        if (availableCopies < totalCopies) {
            availableCopies++;
        }
    }

    // Getters and Setters
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public BookCategory getCategory() { return category; }
    public void setCategory(BookCategory category) { this.category = category; }

    public int getPublicationYear() { return publicationYear; }
    public void setPublicationYear(int publicationYear) { this.publicationYear = publicationYear; }

    public int getTotalCopies() { return totalCopies; }
    public void setTotalCopies(int totalCopies) { this.totalCopies = totalCopies; }

    public int getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(int availableCopies) { this.availableCopies = availableCopies; }

    public String getRackLocation() { return rackLocation; }
    public void setRackLocation(String rackLocation) { this.rackLocation = rackLocation; }

    public LocalDateTime getDateAdded() { return dateAdded; }
    public void setDateAdded(LocalDateTime dateAdded) { this.dateAdded = dateAdded; }

    @Override
    public String toString() {
        return String.format("[%s] '%s' by %s (%d) - Avail: %d/%d (Rack: %s)",
                isbn, title, author, publicationYear, availableCopies, totalCopies, rackLocation);
    }
}
