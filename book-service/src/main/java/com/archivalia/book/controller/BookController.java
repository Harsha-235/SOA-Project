package com.archivalia.book.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.archivalia.book.entity.Book;
import com.archivalia.book.entity.FineRateHistory;
import com.archivalia.book.service.BookService;

@RestController
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    // ==========================================
    // ADD BOOK
    // ==========================================

    @PostMapping
    public ResponseEntity<Book> addBook(
            @RequestBody Book book) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(bookService.addBook(book));
    }

    // ==========================================
    // GET ALL BOOKS
    // ==========================================

    @GetMapping
    public ResponseEntity<List<Book>> getAllBooks() {

        return ResponseEntity.ok(
                bookService.getAllBooks()
        );
    }

    // ==========================================
    // GET BOOK BY ID
    // ==========================================

    @GetMapping("/{bookId}")
    public ResponseEntity<Book> getBookById(
            @PathVariable Long bookId) {

        return ResponseEntity.ok(
                bookService.getBookById(bookId)
        );
    }

    // ==========================================
    // UPDATE BOOK
    // ==========================================

    @PutMapping("/{bookId}")
    public ResponseEntity<Book> updateBook(
            @PathVariable Long bookId,
            @RequestBody Book book) {

        return ResponseEntity.ok(
                bookService.updateBook(bookId, book)
        );
    }

    // ==========================================
    // DELETE BOOK
    // ==========================================

    @DeleteMapping("/{bookId}")
    public ResponseEntity<String> deleteBook(
            @PathVariable Long bookId) {

        try {
            bookService.deleteBook(bookId);

            return ResponseEntity.ok(
                    "Book archived successfully"
            );

        } catch (IllegalStateException exception) {

            return ResponseEntity
                    .badRequest()
                    .body(exception.getMessage());
        }
    }

    // ==========================================
    // SEARCH BOOKS
    // ==========================================

    @GetMapping("/search")
    public ResponseEntity<List<Book>> searchBooks(
            @RequestParam String keyword) {

        return ResponseEntity.ok(
                bookService.searchBooks(keyword)
        );
    }

    // ==========================================
    // BORROW BOOK
    // ==========================================

    @PutMapping("/{bookId}/borrow")
    public ResponseEntity<Book> borrowBook(
            @PathVariable Long bookId) {

        return ResponseEntity.ok(
                bookService.borrowBook(bookId)
        );
    }

    // ==========================================
    // RETURN BOOK
    // ==========================================

    @PutMapping("/{bookId}/return")
    public ResponseEntity<Book> returnBook(
            @PathVariable Long bookId) {

        return ResponseEntity.ok(
                bookService.returnBook(bookId)
        );
    }

    // ==========================================
    // GET FINE RATE HISTORY
    // ==========================================

    @GetMapping("/{bookId}/fine-rates")
    public ResponseEntity<List<FineRateHistory>> getFineRateHistory(
            @PathVariable Long bookId) {

        return ResponseEntity.ok(
                bookService.getFineRateHistory(bookId)
        );
    }
}