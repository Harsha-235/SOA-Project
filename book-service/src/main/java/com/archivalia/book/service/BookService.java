package com.archivalia.book.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.archivalia.book.entity.Book;
import com.archivalia.book.entity.FineRateHistory;
import com.archivalia.book.client.BorrowClient;
import com.archivalia.book.repository.BookRepository;
import com.archivalia.book.repository.FineRateHistoryRepository;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final FineRateHistoryRepository fineRateHistoryRepository;
    private final BorrowClient borrowClient;

    public BookService(
            BookRepository bookRepository,
            FineRateHistoryRepository fineRateHistoryRepository,
            BorrowClient borrowClient) {

        this.bookRepository = bookRepository;
        this.fineRateHistoryRepository = fineRateHistoryRepository;
        this.borrowClient = borrowClient;
    }

    // ==========================================
    // ADD BOOK
    // ==========================================

    public Book addBook(Book book) {

        if (book.getTotalCopies() == null ||
                book.getTotalCopies() < 1) {

            throw new RuntimeException(
                    "Total copies must be at least 1");
        }

        if (book.getFinePerDay() == null ||
                book.getFinePerDay() < 0) {

            throw new RuntimeException(
                    "Fine per day cannot be negative");
        }

        if (book.getBorrowingPeriodDays() == null ||
                book.getBorrowingPeriodDays() < 1) {

            throw new RuntimeException(
                    "Borrowing period must be at least 1 day");
        }

        book.setAvailableCopies(book.getTotalCopies());
        book.setActive(true);

        // Save book first so bookId is generated
        Book savedBook = bookRepository.save(book);

        // ==========================================
        // CREATE INITIAL FINE RATE HISTORY
        // ==========================================

        FineRateHistory initialRate =
                FineRateHistory.builder()
                        .bookId(savedBook.getBookId())
                        .finePerDay(savedBook.getFinePerDay())
                        .effectiveFrom(LocalDate.now())
                        .effectiveTo(null)
                        .build();

        fineRateHistoryRepository.save(initialRate);

        return savedBook;
    }

    // ==========================================
    // GET ALL BOOKS
    // ==========================================

    public List<Book> getAllBooks() {
        return bookRepository.findByActiveTrue();
    }

    // ==========================================
    // GET BOOK BY ID
    // ==========================================

    public Book getBookById(Long bookId) {

        return bookRepository.findById(bookId)
                .orElseThrow(() ->
                        new RuntimeException("Book not found"));
    }

    // ==========================================
    // UPDATE BOOK
    // ==========================================

    public Book updateBook(
            Long bookId,
            Book updatedBook) {

        Book existingBook = getBookById(bookId);

        // Store old fine rate
        Double oldFinePerDay =
                existingBook.getFinePerDay();

        // ==========================================
        // UPDATE BASIC BOOK DETAILS
        // ==========================================

        existingBook.setTitle(updatedBook.getTitle());
        existingBook.setAuthor(updatedBook.getAuthor());
        existingBook.setDescription(updatedBook.getDescription());
        existingBook.setCategory(updatedBook.getCategory());

        // ==========================================
        // VALIDATE FINE PER DAY
        // ==========================================

        if (updatedBook.getFinePerDay() == null ||
                updatedBook.getFinePerDay() < 0) {

            throw new RuntimeException(
                    "Fine per day cannot be negative");
        }

        Double newFinePerDay =
                updatedBook.getFinePerDay();

        // ==========================================
        // UPDATE FINE RATE HISTORY
        // ==========================================

        if (!oldFinePerDay.equals(newFinePerDay)) {

            LocalDate today = LocalDate.now();

            Optional<FineRateHistory> activeRate =
                    fineRateHistoryRepository
                            .findByBookIdAndEffectiveToIsNull(bookId);

            if (activeRate.isPresent()) {

                FineRateHistory currentRate =
                        activeRate.get();

                /*
                 * If the current active rate already started today,
                 * simply change its rate instead of creating another
                 * overlapping record.
                 */
                if (today.equals(currentRate.getEffectiveFrom())) {

                    currentRate.setFinePerDay(newFinePerDay);

                    fineRateHistoryRepository.save(currentRate);

                } else {

                    /*
                     * Close the previous rate yesterday.
                     */
                    currentRate.setEffectiveTo(
                            today.minusDays(1));

                    fineRateHistoryRepository.save(currentRate);

                    /*
                     * Create the new rate from today.
                     */
                    FineRateHistory newRate =
                            FineRateHistory.builder()
                                    .bookId(bookId)
                                    .finePerDay(newFinePerDay)
                                    .effectiveFrom(today)
                                    .effectiveTo(null)
                                    .build();

                    fineRateHistoryRepository.save(newRate);
                }

            } else {

                /*
                 * Safety case:
                 * If there is no active history record,
                 * create a new one starting today.
                 */
                FineRateHistory newRate =
                        FineRateHistory.builder()
                                .bookId(bookId)
                                .finePerDay(newFinePerDay)
                                .effectiveFrom(today)
                                .effectiveTo(null)
                                .build();

                fineRateHistoryRepository.save(newRate);
            }
        }

        // Update current book rate
        existingBook.setFinePerDay(newFinePerDay);

        // ==========================================
        // VALIDATE BORROWING PERIOD
        // ==========================================

        if (updatedBook.getBorrowingPeriodDays() == null ||
                updatedBook.getBorrowingPeriodDays() < 1) {

            throw new RuntimeException(
                    "Borrowing period must be at least 1 day");
        }

        existingBook.setBorrowingPeriodDays(
                updatedBook.getBorrowingPeriodDays());

        // ==========================================
        // UPDATE TOTAL COPIES
        // ==========================================

        if (updatedBook.getTotalCopies() != null) {

            int borrowedCopies =
                    existingBook.getTotalCopies()
                    - existingBook.getAvailableCopies();

            if (updatedBook.getTotalCopies()
                    < borrowedCopies) {

                throw new RuntimeException(
                        "Total copies cannot be less than borrowed copies");
            }

            existingBook.setTotalCopies(
                    updatedBook.getTotalCopies());

            existingBook.setAvailableCopies(
                    updatedBook.getTotalCopies()
                    - borrowedCopies);
        }

        return bookRepository.save(existingBook);
    }

    // ==========================================
    // DELETE BOOK
    // ==========================================

    public void deleteBook(Long bookId) {

        Book book = getBookById(bookId);

        if (Boolean.FALSE.equals(book.getActive())) {
            throw new IllegalStateException(
                    "Book is already archived");
        }

        /*
         * IMPORTANT:
         * Book Service owns book_db and Borrow Service owns borrow_db.
         * We therefore ask Borrow Service whether this book currently
         * has a PENDING or BORROWED record instead of querying borrow_db.
         */
        Boolean hasActiveBorrowing =
                borrowClient.hasActiveBorrowing(bookId);

        if (Boolean.TRUE.equals(hasActiveBorrowing)) {
            throw new IllegalStateException(
                    "Book cannot be deleted while it has an active borrowing request or active loan");
        }

        /*
         * Soft delete:
         * Do NOT physically delete the row. Historical borrow/fine
         * records may still refer to this bookId.
         */
        book.setActive(false);
        bookRepository.save(book);
    }

    // ==========================================
    // SEARCH BOOKS
    // ==========================================

    public List<Book> searchBooks(String keyword) {

        return bookRepository
                .findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrCategoryContainingIgnoreCase(
                        keyword,
                        keyword,
                        keyword
                )
                .stream()
                .filter(book -> !Boolean.FALSE.equals(book.getActive()))
                .toList();
    }

    // ==========================================
    // BORROW BOOK
    // ==========================================

    public Book borrowBook(Long bookId) {

        Book book = getBookById(bookId);

        if (Boolean.FALSE.equals(book.getActive())) {
            throw new RuntimeException(
                    "This book is no longer available in the catalogue");
        }

        if (book.getAvailableCopies() <= 0) {

            throw new RuntimeException(
                    "No copies available");
        }

        book.setAvailableCopies(
                book.getAvailableCopies() - 1
        );

        return bookRepository.save(book);
    }

    // ==========================================
    // RETURN BOOK
    // ==========================================

    public Book returnBook(Long bookId) {

        Book book = getBookById(bookId);

        if (book.getAvailableCopies()
                >= book.getTotalCopies()) {

            throw new RuntimeException(
                    "All copies are already available");
        }

        book.setAvailableCopies(
                book.getAvailableCopies() + 1
        );

        return bookRepository.save(book);
    }

    // ==========================================
    // GET FINE RATE HISTORY
    // ==========================================

    public List<FineRateHistory> getFineRateHistory(Long bookId) {

        // Make sure the book exists
        getBookById(bookId);

        return fineRateHistoryRepository
                .findByBookIdOrderByEffectiveFromAsc(bookId);
    }
}