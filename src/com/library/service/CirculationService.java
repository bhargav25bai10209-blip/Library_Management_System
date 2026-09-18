package com.library.service;

import com.library.exception.*;
import com.library.model.*;
import com.library.observer.LibraryEvent;
import com.library.observer.NotificationService;
import com.library.repository.BookRepository;
import com.library.repository.ReservationRepository;
import com.library.repository.TransactionRepository;
import com.library.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service orchestrating core library circulation: loans, returns, renewals, and reservation queues.
 */
public class CirculationService {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final ReservationRepository reservationRepository;
    private final FineService fineService;
    private final NotificationService notificationService;

    public CirculationService(BookRepository bookRepository,
                              UserRepository userRepository,
                              TransactionRepository transactionRepository,
                              ReservationRepository reservationRepository,
                              FineService fineService,
                              NotificationService notificationService) {
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.reservationRepository = reservationRepository;
        this.fineService = fineService;
        this.notificationService = notificationService;
    }

    /**
     * Issues a book copy to a member with full constraint checks.
     */
    public synchronized Transaction issueBook(String isbn, String memberId)
            throws BookNotFoundException, UserNotFoundException,
                   BookUnavailableException, MaxBorrowLimitException,
                   OutstandingFineException, IllegalStateException {

        Book book = bookRepository.findByIsbn(isbn)
                .orElseThrow(() -> new BookNotFoundException("Book not found with ISBN: " + isbn));

        User user = userRepository.findById(memberId)
                .orElseThrow(() -> new UserNotFoundException("Patron not found with ID: " + memberId));

        if (!(user instanceof Member member)) {
            throw new IllegalArgumentException("User " + memberId + " is a staff member, not a patron.");
        }

        if (!member.isActive()) {
            throw new IllegalStateException("Member account is inactive. Cannot issue books.");
        }

        if (member.getOutstandingFines() > 50.0) {
            throw new OutstandingFineException(String.format(
                    "Patron has ₹%.2f in unpaid fines. Unpaid fines must not exceed ₹50.00 to borrow.",
                    member.getOutstandingFines()
            ));
        }

        if (member.getActiveBorrowsCount() >= member.getMaxBorrowLimit()) {
            throw new MaxBorrowLimitException(String.format(
                    "Borrowing limit reached (%d/%d books currently issued).",
                    member.getActiveBorrowsCount(), member.getMaxBorrowLimit()
            ));
        }

        Optional<Transaction> activeLoan = transactionRepository.findActiveLoan(isbn, memberId);
        if (activeLoan.isPresent()) {
            throw new IllegalStateException("Patron already holds an active copy of this book.");
        }

        if (!book.isAvailable()) {
            throw new BookUnavailableException(
                    "All copies of '" + book.getTitle() + "' are currently issued. You may reserve this book."
            );
        }

        // Decrement book availability and increment member borrow count
        book.decrementCopies();
        bookRepository.save(book);

        member.incrementActiveBorrows();
        userRepository.save(member);

        LocalDate issueDate = LocalDate.now();
        LocalDate dueDate = issueDate.plusDays(member.getDefaultLoanPeriodDays());

        String txId = "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Transaction transaction = new Transaction(txId, book.getIsbn(), book.getTitle(),
                member.getUserId(), member.getName(), issueDate, dueDate);

        transactionRepository.save(transaction);

        // Fulfill any waiting reservation for this member
        Optional<Reservation> reservationOpt = reservationRepository.findPendingByBookAndMember(isbn, memberId);
        if (reservationOpt.isPresent()) {
            Reservation r = reservationOpt.get();
            r.setStatus(ReservationStatus.FULFILLED);
            reservationRepository.save(r);
        }

        notificationService.publish(new LibraryEvent(
                LibraryEvent.EventType.BOOK_ISSUED,
                String.format("Issued '%s'. Due date: %s", book.getTitle(), dueDate),
                member.getUserId(),
                transaction
        ));

        return transaction;
    }

    /**
     * Processes book return, calculates any overdue penalties, and triggers reservation queue dispatch.
     */
    public synchronized Transaction returnBook(String isbn, String memberId, LocalDate returnDate)
            throws BookNotFoundException, UserNotFoundException, IllegalStateException {

        LocalDate effectiveReturnDate = (returnDate != null) ? returnDate : LocalDate.now();

        Book book = bookRepository.findByIsbn(isbn)
                .orElseThrow(() -> new BookNotFoundException("Book not found with ISBN: " + isbn));

        User user = userRepository.findById(memberId)
                .orElseThrow(() -> new UserNotFoundException("Patron not found with ID: " + memberId));

        if (!(user instanceof Member member)) {
            throw new IllegalArgumentException("User is not a member.");
        }

        Transaction transaction = transactionRepository.findActiveLoan(isbn, memberId)
                .orElseThrow(() -> new IllegalStateException("No active loan record found for book " + isbn + " and patron " + memberId));

        transaction.setReturnDate(effectiveReturnDate);

        double fine = fineService.calculateFine(transaction, member, effectiveReturnDate);
        transaction.setFineAmount(fine);

        if (fine > 0) {
            transaction.setStatus(TransactionStatus.RETURNED_OVERDUE);
            member.addFine(fine);
        } else {
            transaction.setStatus(TransactionStatus.RETURNED);
        }

        transactionRepository.save(transaction);

        // Reclaim copy
        book.incrementCopies();
        bookRepository.save(book);

        member.decrementActiveBorrows();
        userRepository.save(member);

        // Check if there are patrons waiting in priority reservation queue
        Optional<Reservation> nextInQueue = reservationRepository.peekNextInQueue(isbn);
        if (nextInQueue.isPresent()) {
            Reservation next = nextInQueue.get();
            next.setStatus(ReservationStatus.NOTIFIED);
            next.setNotificationDate(LocalDateTime.now());
            reservationRepository.save(next);

            notificationService.publish(new LibraryEvent(
                    LibraryEvent.EventType.RESERVATION_AVAILABLE,
                    String.format("The book you reserved ('%s') has been returned and is held for you!", book.getTitle()),
                    next.getMemberId(),
                    next
            ));
        }

        notificationService.publish(new LibraryEvent(
                LibraryEvent.EventType.BOOK_RETURNED,
                String.format("Returned '%s'. Overdue fine assessed: ₹%.2f", book.getTitle(), fine),
                member.getUserId(),
                transaction
        ));

        return transaction;
    }

    /**
     * Extends the due date if no other patron is waiting in queue.
     */
    public synchronized Transaction renewBook(String transactionId)
            throws IllegalStateException, LibraryException {

        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new LibraryException("Transaction not found: " + transactionId));

        if (tx.getStatus() != TransactionStatus.ISSUED) {
            throw new IllegalStateException("Cannot renew a book that is already returned.");
        }

        if (tx.getRenewalCount() >= 2) {
            throw new IllegalStateException("Maximum renewal limit (2 times) reached for this loan.");
        }

        // Check if other patrons are waiting for this book
        Optional<Reservation> waiting = reservationRepository.peekNextInQueue(tx.getBookIsbn());
        if (waiting.isPresent()) {
            throw new IllegalStateException("Cannot renew: Another member is currently waiting in reservation queue.");
        }

        User u = userRepository.findById(tx.getMemberId()).orElse(null);
        int renewalDays = (u instanceof Member m) ? m.getDefaultLoanPeriodDays() : 14;

        LocalDate newDueDate = tx.getDueDate().plusDays(renewalDays);
        tx.setDueDate(newDueDate);
        tx.setRenewalCount(tx.getRenewalCount() + 1);

        transactionRepository.save(tx);

        notificationService.publish(new LibraryEvent(
                LibraryEvent.EventType.BOOK_ISSUED,
                String.format("Loan for '%s' renewed. New Due Date: %s", tx.getBookTitle(), newDueDate),
                tx.getMemberId(),
                tx
        ));

        return tx;
    }

    /**
     * Places a hold/reservation for a book currently out of stock.
     */
    public synchronized Reservation reserveBook(String isbn, String memberId)
            throws BookNotFoundException, UserNotFoundException, DuplicateEntityException, IllegalStateException {

        Book book = bookRepository.findByIsbn(isbn)
                .orElseThrow(() -> new BookNotFoundException("Book not found with ISBN: " + isbn));

        User user = userRepository.findById(memberId)
                .orElseThrow(() -> new UserNotFoundException("Patron not found with ID: " + memberId));

        if (!(user instanceof Member member)) {
            throw new IllegalArgumentException("Staff users cannot place reservations.");
        }

        // If copies are already available, encourage direct borrowing
        if (book.isAvailable()) {
            throw new IllegalStateException("Copies of '" + book.getTitle() + "' are currently available on shelf! No need to reserve.");
        }

        if (transactionRepository.findActiveLoan(isbn, memberId).isPresent()) {
            throw new IllegalStateException("You already have an active loan for this book.");
        }

        if (reservationRepository.findPendingByBookAndMember(isbn, memberId).isPresent()) {
            throw new DuplicateEntityException("You already have an active reservation for this book.");
        }

        int priority = (member instanceof FacultyMember) ? 1 : 2;
        String resId = "RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Reservation reservation = new Reservation(resId, isbn, book.getTitle(), member.getUserId(), member.getName(), priority);
        reservationRepository.save(reservation);

        notificationService.publish(new LibraryEvent(
                LibraryEvent.EventType.BOOK_RESERVED,
                String.format("Reservation placed for '%s'. Priority Tier: %s",
                        book.getTitle(), (priority == 1 ? "Faculty (High)" : "Student (Standard)")),
                member.getUserId(),
                reservation
        ));

        return reservation;
    }

    public synchronized void cancelReservation(String reservationId, String memberId) throws LibraryException {
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new LibraryException("Reservation not found: " + reservationId));

        if (!r.getMemberId().equalsIgnoreCase(memberId)) {
            throw new LibraryException("Unauthorized: You do not own this reservation.");
        }

        r.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(r);
    }

    public List<Transaction> getActiveLoansByMember(String memberId) {
        return transactionRepository.findActiveByMemberId(memberId);
    }

    public List<Transaction> getLoanHistoryByMember(String memberId) {
        return transactionRepository.findByMemberId(memberId);
    }

    public List<Transaction> getAllActiveLoans() {
        return transactionRepository.findAll().stream()
                .filter(t -> t.getStatus() == TransactionStatus.ISSUED)
                .toList();
    }

    public List<Transaction> getOverdueLoans() {
        return transactionRepository.findOverdueLoans(LocalDate.now());
    }

    public List<Reservation> getReservationsByMember(String memberId) {
        return reservationRepository.findByMemberId(memberId);
    }

    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }
}
