package com.archivalia.book.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.archivalia.book.entity.Book;

public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findByActiveTrue();

    List<Book> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrCategoryContainingIgnoreCase(
            String title,
            String author,
            String category
    );
}