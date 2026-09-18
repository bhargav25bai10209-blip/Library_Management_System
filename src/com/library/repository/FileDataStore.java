package com.library.repository;

import com.library.model.*;
import com.library.util.JsonHelper;
import com.library.util.PasswordHasher;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Storage engine handling atomic file persistence and seed data initialization in pure Java.
 */
public class FileDataStore {

    private static final String DATA_DIR = "data";
    private static final String BOOKS_FILE = DATA_DIR + File.separator + "books.json";
    private static final String USERS_FILE = DATA_DIR + File.separator + "users.json";
    private static final String TRANSACTIONS_FILE = DATA_DIR + File.separator + "transactions.json";
    private static final String RESERVATIONS_FILE = DATA_DIR + File.separator + "reservations.json";
    private static final String RECEIPTS_FILE = DATA_DIR + File.separator + "receipts.json";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private static FileDataStore instance;

    private FileDataStore() {
        initDirectory();
    }

    public static synchronized FileDataStore getInstance() {
        if (instance == null) {
            instance = new FileDataStore();
        }
        return instance;
    }

    private void initDirectory() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private synchronized void atomicWrite(String filePath, String content) {
        try {
            Path target = Path.of(filePath);
            Path temp = Path.of(filePath + ".tmp");
            Files.writeString(temp, content, StandardCharsets.UTF_8);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            // Fallback for filesystems that do not support ATOMIC_MOVE
            try {
                Files.writeString(Path.of(filePath), content, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                System.err.println("Critical Error writing to " + filePath + ": " + ex.getMessage());
            }
        }
    }

    private synchronized String readString(String filePath) {
        try {
            Path path = Path.of(filePath);
            if (!Files.exists(path)) return null;
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Error reading " + filePath + ": " + e.getMessage());
            return null;
        }
    }

    // ================= BOOK STORAGE =================

    public List<Book> loadBooks() {
        String json = readString(BOOKS_FILE);
        if (json == null || json.trim().isEmpty()) {
            List<Book> seeds = createSeedBooks();
            saveBooks(seeds);
            return seeds;
        }
        List<Map<String, String>> records = JsonHelper.parseJsonArray(json);
        List<Book> books = new ArrayList<>();
        for (Map<String, String> m : records) {
            try {
                Book b = new Book();
                b.setIsbn(m.get("isbn"));
                b.setTitle(m.get("title"));
                b.setAuthor(m.get("author"));
                b.setCategory(BookCategory.fromString(m.get("category")));
                b.setPublicationYear(Integer.parseInt(m.getOrDefault("publicationYear", "2020")));
                b.setTotalCopies(Integer.parseInt(m.getOrDefault("totalCopies", "1")));
                b.setAvailableCopies(Integer.parseInt(m.getOrDefault("availableCopies", "1")));
                b.setRackLocation(m.get("rackLocation"));
                if (m.containsKey("dateAdded")) {
                    b.setDateAdded(LocalDateTime.parse(m.get("dateAdded"), DATE_TIME_FORMATTER));
                }
                books.add(b);
            } catch (Exception e) {
                System.err.println("Skipping malformed book record: " + e.getMessage());
            }
        }
        return books;
    }

    public void saveBooks(List<Book> books) {
        List<Map<String, String>> list = new ArrayList<>();
        for (Book b : books) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("isbn", b.getIsbn());
            m.put("title", b.getTitle());
            m.put("author", b.getAuthor());
            m.put("category", b.getCategory().name());
            m.put("publicationYear", String.valueOf(b.getPublicationYear()));
            m.put("totalCopies", String.valueOf(b.getTotalCopies()));
            m.put("availableCopies", String.valueOf(b.getAvailableCopies()));
            m.put("rackLocation", b.getRackLocation());
            m.put("dateAdded", b.getDateAdded() != null ? b.getDateAdded().format(DATE_TIME_FORMATTER) : LocalDateTime.now().format(DATE_TIME_FORMATTER));
            list.add(m);
        }
        atomicWrite(BOOKS_FILE, JsonHelper.toJsonArray(list));
    }

    // ================= USER STORAGE =================

    public List<User> loadUsers() {
        String json = readString(USERS_FILE);
        if (json == null || json.trim().isEmpty()) {
            List<User> seeds = createSeedUsers();
            saveUsers(seeds);
            return seeds;
        }
        List<Map<String, String>> records = JsonHelper.parseJsonArray(json);
        List<User> users = new ArrayList<>();
        for (Map<String, String> m : records) {
            try {
                Role role = Role.valueOf(m.get("role"));
                User user;
                switch (role) {
                    case ADMIN: {
                        Admin a = new Admin();
                        a.setAdminLevel(m.getOrDefault("adminLevel", "SUPER"));
                        user = a;
                        break;
                    }
                    case LIBRARIAN: {
                        Librarian lib = new Librarian();
                        lib.setEmployeeId(m.get("employeeId"));
                        lib.setDeskLocation(m.get("deskLocation"));
                        user = lib;
                        break;
                    }
                    case STUDENT: {
                        StudentMember sm = new StudentMember();
                        sm.setMembershipId(m.get("membershipId"));
                        sm.setDepartment(m.get("department"));
                        sm.setAcademicYear(Integer.parseInt(m.getOrDefault("academicYear", "2")));
                        sm.setOutstandingFines(Double.parseDouble(m.getOrDefault("outstandingFines", "0.0")));
                        sm.setActiveBorrowsCount(Integer.parseInt(m.getOrDefault("activeBorrowsCount", "0")));
                        user = sm;
                        break;
                    }
                    case FACULTY: {
                        FacultyMember fm = new FacultyMember();
                        fm.setMembershipId(m.get("membershipId"));
                        fm.setDepartment(m.get("department"));
                        fm.setDesignation(m.get("designation"));
                        fm.setOutstandingFines(Double.parseDouble(m.getOrDefault("outstandingFines", "0.0")));
                        fm.setActiveBorrowsCount(Integer.parseInt(m.getOrDefault("activeBorrowsCount", "0")));
                        user = fm;
                        break;
                    }
                    default:
                        continue;
                }

                user.setUserId(m.get("userId"));
                user.setName(m.get("name"));
                user.setEmail(m.get("email"));
                user.setPhone(m.get("phone"));
                user.setPasswordHash(m.get("passwordHash"));
                user.setSalt(m.get("salt"));
                user.setActive(Boolean.parseBoolean(m.getOrDefault("active", "true")));
                if (m.containsKey("registeredAt")) {
                    user.setRegisteredAt(LocalDateTime.parse(m.get("registeredAt"), DATE_TIME_FORMATTER));
                }
                users.add(user);
            } catch (Exception e) {
                System.err.println("Skipping malformed user record: " + e.getMessage());
            }
        }
        return users;
    }

    public void saveUsers(List<User> users) {
        List<Map<String, String>> list = new ArrayList<>();
        for (User u : users) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("userId", u.getUserId());
            m.put("name", u.getName());
            m.put("email", u.getEmail());
            m.put("phone", u.getPhone());
            m.put("passwordHash", u.getPasswordHash());
            m.put("salt", u.getSalt());
            m.put("role", u.getRole().name());
            m.put("active", String.valueOf(u.isActive()));
            m.put("registeredAt", u.getRegisteredAt().format(DATE_TIME_FORMATTER));

            if (u instanceof Admin a) {
                m.put("adminLevel", a.getAdminLevel());
            } else if (u instanceof Librarian lib) {
                m.put("employeeId", lib.getEmployeeId());
                m.put("deskLocation", lib.getDeskLocation());
            } else if (u instanceof StudentMember sm) {
                m.put("membershipId", sm.getMembershipId());
                m.put("department", sm.getDepartment());
                m.put("academicYear", String.valueOf(sm.getAcademicYear()));
                m.put("outstandingFines", String.valueOf(sm.getOutstandingFines()));
                m.put("activeBorrowsCount", String.valueOf(sm.getActiveBorrowsCount()));
            } else if (u instanceof FacultyMember fm) {
                m.put("membershipId", fm.getMembershipId());
                m.put("department", fm.getDepartment());
                m.put("designation", fm.getDesignation());
                m.put("outstandingFines", String.valueOf(fm.getOutstandingFines()));
                m.put("activeBorrowsCount", String.valueOf(fm.getActiveBorrowsCount()));
            }
            list.add(m);
        }
        atomicWrite(USERS_FILE, JsonHelper.toJsonArray(list));
    }

    // ================= TRANSACTIONS STORAGE =================

    public List<Transaction> loadTransactions() {
        String json = readString(TRANSACTIONS_FILE);
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<Map<String, String>> records = JsonHelper.parseJsonArray(json);
        List<Transaction> txs = new ArrayList<>();
        for (Map<String, String> m : records) {
            try {
                Transaction t = new Transaction();
                t.setTransactionId(m.get("transactionId"));
                t.setBookIsbn(m.get("bookIsbn"));
                t.setBookTitle(m.get("bookTitle"));
                t.setMemberId(m.get("memberId"));
                t.setMemberName(m.get("memberName"));
                t.setIssueDate(LocalDate.parse(m.get("issueDate"), DATE_FORMATTER));
                t.setDueDate(LocalDate.parse(m.get("dueDate"), DATE_FORMATTER));
                if (m.containsKey("returnDate") && m.get("returnDate") != null) {
                    t.setReturnDate(LocalDate.parse(m.get("returnDate"), DATE_FORMATTER));
                }
                t.setFineAmount(Double.parseDouble(m.getOrDefault("fineAmount", "0.0")));
                t.setFinePaid(Boolean.parseBoolean(m.getOrDefault("finePaid", "false")));
                t.setStatus(TransactionStatus.valueOf(m.get("status")));
                t.setRenewalCount(Integer.parseInt(m.getOrDefault("renewalCount", "0")));
                txs.add(t);
            } catch (Exception e) {
                System.err.println("Skipping malformed transaction: " + e.getMessage());
            }
        }
        return txs;
    }

    public void saveTransactions(List<Transaction> txs) {
        List<Map<String, String>> list = new ArrayList<>();
        for (Transaction t : txs) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("transactionId", t.getTransactionId());
            m.put("bookIsbn", t.getBookIsbn());
            m.put("bookTitle", t.getBookTitle());
            m.put("memberId", t.getMemberId());
            m.put("memberName", t.getMemberName());
            m.put("issueDate", t.getIssueDate().format(DATE_FORMATTER));
            m.put("dueDate", t.getDueDate().format(DATE_FORMATTER));
            m.put("returnDate", t.getReturnDate() != null ? t.getReturnDate().format(DATE_FORMATTER) : null);
            m.put("fineAmount", String.valueOf(t.getFineAmount()));
            m.put("finePaid", String.valueOf(t.isFinePaid()));
            m.put("status", t.getStatus().name());
            m.put("renewalCount", String.valueOf(t.getRenewalCount()));
            list.add(m);
        }
        atomicWrite(TRANSACTIONS_FILE, JsonHelper.toJsonArray(list));
    }

    // ================= RESERVATIONS STORAGE =================

    public List<Reservation> loadReservations() {
        String json = readString(RESERVATIONS_FILE);
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<Map<String, String>> records = JsonHelper.parseJsonArray(json);
        List<Reservation> list = new ArrayList<>();
        for (Map<String, String> m : records) {
            try {
                Reservation r = new Reservation();
                r.setReservationId(m.get("reservationId"));
                r.setBookIsbn(m.get("bookIsbn"));
                r.setBookTitle(m.get("bookTitle"));
                r.setMemberId(m.get("memberId"));
                r.setMemberName(m.get("memberName"));
                r.setPriority(Integer.parseInt(m.getOrDefault("priority", "2")));
                r.setRequestDate(LocalDateTime.parse(m.get("requestDate"), DATE_TIME_FORMATTER));
                r.setStatus(ReservationStatus.valueOf(m.get("status")));
                if (m.containsKey("notificationDate") && m.get("notificationDate") != null) {
                    r.setNotificationDate(LocalDateTime.parse(m.get("notificationDate"), DATE_TIME_FORMATTER));
                }
                list.add(r);
            } catch (Exception e) {
                System.err.println("Skipping malformed reservation: " + e.getMessage());
            }
        }
        return list;
    }

    public void saveReservations(List<Reservation> reservations) {
        List<Map<String, String>> list = new ArrayList<>();
        for (Reservation r : reservations) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("reservationId", r.getReservationId());
            m.put("bookIsbn", r.getBookIsbn());
            m.put("bookTitle", r.getBookTitle());
            m.put("memberId", r.getMemberId());
            m.put("memberName", r.getMemberName());
            m.put("priority", String.valueOf(r.getPriority()));
            m.put("requestDate", r.getRequestDate().format(DATE_TIME_FORMATTER));
            m.put("status", r.getStatus().name());
            m.put("notificationDate", r.getNotificationDate() != null ? r.getNotificationDate().format(DATE_TIME_FORMATTER) : null);
            list.add(m);
        }
        atomicWrite(RESERVATIONS_FILE, JsonHelper.toJsonArray(list));
    }

    // ================= RECEIPTS STORAGE =================

    public List<FineReceipt> loadReceipts() {
        String json = readString(RECEIPTS_FILE);
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<Map<String, String>> records = JsonHelper.parseJsonArray(json);
        List<FineReceipt> list = new ArrayList<>();
        for (Map<String, String> m : records) {
            try {
                FineReceipt r = new FineReceipt();
                r.setReceiptId(m.get("receiptId"));
                r.setTransactionId(m.get("transactionId"));
                r.setMemberId(m.get("memberId"));
                r.setMemberName(m.get("memberName"));
                r.setAmountPaid(Double.parseDouble(m.getOrDefault("amountPaid", "0.0")));
                r.setPaymentMethod(m.get("paymentMethod"));
                r.setPaymentDate(LocalDateTime.parse(m.get("paymentDate"), DATE_TIME_FORMATTER));
                list.add(r);
            } catch (Exception e) {
                System.err.println("Skipping malformed receipt: " + e.getMessage());
            }
        }
        return list;
    }

    public void saveReceipts(List<FineReceipt> receipts) {
        List<Map<String, String>> list = new ArrayList<>();
        for (FineReceipt r : receipts) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("receiptId", r.getReceiptId());
            m.put("transactionId", r.getTransactionId());
            m.put("memberId", r.getMemberId());
            m.put("memberName", r.getMemberName());
            m.put("amountPaid", String.valueOf(r.getAmountPaid()));
            m.put("paymentMethod", r.getPaymentMethod());
            m.put("paymentDate", r.getPaymentDate().format(DATE_TIME_FORMATTER));
            list.add(m);
        }
        atomicWrite(RECEIPTS_FILE, JsonHelper.toJsonArray(list));
    }

    // ================= SEED DATA GENERATION =================

    private List<Book> createSeedBooks() {
        List<Book> seeds = new ArrayList<>();
        seeds.add(new Book("978-0134685991", "Effective Java (3rd Edition)", "Joshua Bloch", BookCategory.SOFTWARE_ENGINEERING, 2018, 5, "Rack-CS-101"));
        seeds.add(new Book("978-0132350884", "Clean Code: A Handbook of Agile Software Craftsmanship", "Robert C. Martin", BookCategory.SOFTWARE_ENGINEERING, 2008, 4, "Rack-CS-102"));
        seeds.add(new Book("978-0262033848", "Introduction to Algorithms (CLRS 3rd Ed)", "Thomas H. Cormen", BookCategory.DATA_STRUCTURES, 2009, 6, "Rack-CS-201"));
        seeds.add(new Book("978-0131103627", "The C Programming Language", "Brian W. Kernighan & Dennis M. Ritchie", BookCategory.COMPUTER_SCIENCE, 1988, 3, "Rack-CS-103"));
        seeds.add(new Book("978-0262035613", "Deep Learning", "Ian Goodfellow, Yoshua Bengio, Aaron Courville", BookCategory.ARTIFICIAL_INTELLIGENCE, 2016, 3, "Rack-AI-301"));
        seeds.add(new Book("978-0321356680", "Effective Java", "Joshua Bloch", BookCategory.COMPUTER_SCIENCE, 2008, 2, "Rack-CS-104"));
        seeds.add(new Book("978-0134494166", "Clean Architecture", "Robert C. Martin", BookCategory.SOFTWARE_ENGINEERING, 2017, 4, "Rack-CS-105"));
        seeds.add(new Book("978-0596007126", "Head First Design Patterns", "Eric Freeman & Elisabeth Robson", BookCategory.SOFTWARE_ENGINEERING, 2004, 5, "Rack-CS-106"));
        seeds.add(new Book("978-0132143011", "Computer Systems: A Programmer's Perspective", "Randal E. Bryant & David R. O'Hallaron", BookCategory.COMPUTER_SCIENCE, 2015, 3, "Rack-CS-205"));
        seeds.add(new Book("978-0672324536", "Operating System Concepts", "Abraham Silberschatz & Peter Baer Galvin", BookCategory.COMPUTER_SCIENCE, 2018, 4, "Rack-CS-206"));
        return seeds;
    }

    private List<User> createSeedUsers() {
        List<User> seeds = new ArrayList<>();

        // Admin (default password: "admin123")
        String adminSalt = PasswordHasher.generateSalt();
        String adminHash = PasswordHasher.hashPassword("admin123", adminSalt);
        Admin admin = new Admin("U-ADM-001", "System Administrator", "admin@vityarthi.edu", "9876543210", adminHash, adminSalt, "SUPER");
        seeds.add(admin);

        // Librarian (default password: "lib123")
        String libSalt = PasswordHasher.generateSalt();
        String libHash = PasswordHasher.hashPassword("lib123", libSalt);
        Librarian librarian = new Librarian("U-LIB-001", "Dr. Sarah Jenkins", "librarian@vityarthi.edu", "9876543211", libHash, libSalt, "EMP-404", "Central Desk A");
        seeds.add(librarian);

        // Student Member (default password: "student123")
        String stuSalt = PasswordHasher.generateSalt();
        String stuHash = PasswordHasher.hashPassword("student123", stuSalt);
        StudentMember student = new StudentMember("U-STU-001", "Aarav Sharma", "aarav.sharma@vityarthi.edu", "9876543212", stuHash, stuSalt, "MEM-STU-1001", "Computer Science", 3);
        seeds.add(student);

        // Faculty Member (default password: "faculty123")
        String facSalt = PasswordHasher.generateSalt();
        String facHash = PasswordHasher.hashPassword("faculty123", facSalt);
        FacultyMember faculty = new FacultyMember("U-FAC-001", "Prof. Vikram Reddy", "vikram.reddy@vityarthi.edu", "9876543213", facHash, facSalt, "MEM-FAC-2001", "Computer Science", "Associate Professor");
        seeds.add(faculty);

        return seeds;
    }
}
