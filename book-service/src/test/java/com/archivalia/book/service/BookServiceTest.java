package com.archivalia.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.archivalia.book.entity.Book;
import com.archivalia.book.repository.BookRepository;

class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getBookById_shouldReturnBook() {

        Book book = Book.builder()
                .bookId(1L)
                .title("Test Book")
                .author("Test Author")
                .category("General")
                .totalCopies(10)
                .availableCopies(5)
                .finePerDay(20.0)
                .borrowingPeriodDays(7)
                .build();

        when(bookRepository.findById(1L))
                .thenReturn(Optional.of(book));

        Book result = bookService.getBookById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getBookId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Test Book");
        assertThat(result.getAvailableCopies()).isEqualTo(5);

        verify(bookRepository).findById(1L);
    }

    @Test
    void getBookById_shouldRejectMissingBook() {

        when(bookRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getBookById(999L))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void searchBooks_shouldReturnMatchingBooks() {

        Book book = Book.builder()
                .bookId(1L)
                .title("Java Programming")
                .author("James")
                .category("Programming")
                .totalCopies(10)
                .availableCopies(8)
                .finePerDay(20.0)
                .borrowingPeriodDays(7)
                .build();

        when(bookRepository
                .findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrCategoryContainingIgnoreCase(
                        "Java", "Java", "Java"))
                .thenReturn(java.util.List.of(book));

        var result = bookService.searchBooks("Java");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle())
                .isEqualTo("Java Programming");

        verify(bookRepository)
                .findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrCategoryContainingIgnoreCase(
                        "Java", "Java", "Java");
    }
}