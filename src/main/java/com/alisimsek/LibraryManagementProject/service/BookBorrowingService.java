package com.alisimsek.LibraryManagementProject.service;

import com.alisimsek.LibraryManagementProject.dto.request.BookBorrowingRequest;
import com.alisimsek.LibraryManagementProject.dto.request.BookBorrowingUpdateRequest;
import com.alisimsek.LibraryManagementProject.entity.Book;
import com.alisimsek.LibraryManagementProject.entity.BookBorrowing;
import com.alisimsek.LibraryManagementProject.mapper.BookBorrowingMapper;
import com.alisimsek.LibraryManagementProject.repository.BookBorrowingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookBorrowingService {

    private final BookBorrowingRepository bookBorrowingRepository;
    private final BookService bookService;
    private final BookBorrowingMapper bookBorrowingMapper;

    // Tüm ödünç alınan kitapları getir
    public List<BookBorrowing> findAll() {
        return this.bookBorrowingRepository.findAll();
    }

    // ID'ye göre ödünç alınan kitabı getir
    public BookBorrowing getById(Long id) {
        return bookBorrowingRepository.findById(id).orElseThrow(() -> new RuntimeException(id + " id'li Ödünç Alımı Bulunamadı !!!"));
    }

    // Yeni ödünç alımını oluştur
    public BookBorrowing create(BookBorrowingRequest bookBorrowingRequest) {

        // Eğer kitap stoğu 0 veya daha düşükse hata fırlat
        if (bookBorrowingRequest.getBookForBorrowingRequest().getStock() <= 0) {
            throw new RuntimeException("Ödünç almak istediğiniz kitabın stoğu yoktur !!!");
        }

        // Kitap bilgilerini al ve stoğunu azalt
        Book book = bookService.getById(bookBorrowingRequest.getBookForBorrowingRequest().getId());
        book.setStock(book.getStock() - 1);

        // Güncellenmiş kitap bilgilerini kaydet
        Book bookUpdated = bookService.update(bookBorrowingRequest.getBookForBorrowingRequest().getId(), book);

        // Yeni ödünç alımını oluştur ve kaydet
        BookBorrowing bookBorrowing = new BookBorrowing();
        bookBorrowing.setBorrowerName(bookBorrowingRequest.getBorrowerName());
        bookBorrowing.setBorrowerMail(bookBorrowingRequest.getBorrowerMail());
        bookBorrowing.setBorrowingDate(bookBorrowingRequest.getBorrowingDate());
        bookBorrowing.setReturnDate(bookBorrowingRequest.getReturnDate());  // returnDate'i ekleyin
        bookBorrowing.setBook(bookUpdated);

        return this.bookBorrowingRepository.save(bookBorrowing);
    }

    // Ödünç alımını güncelle
    public BookBorrowing update(Long id, BookBorrowingUpdateRequest bookBorrowingUpdateRequest) {

        Optional<BookBorrowing> bookBorrowingFromDb = bookBorrowingRepository.findById(id);
        
        if (bookBorrowingFromDb.isEmpty()) {
            throw new RuntimeException(id + " id'li Ödünç Alımı sistemde bulunamadı !!!");
        }

        BookBorrowing bookBorrowing = bookBorrowingFromDb.get();

        // returnDate güncellenmesi durumunda kitabın stoğunu güncelle
        if (bookBorrowingUpdateRequest.getReturnDate() != null && bookBorrowing.getReturnDate() == null) {
            // Kitabın stoğunu artır
            Book book = bookBorrowing.getBook();
            book.setStock(book.getStock() + 1);

            // Güncellenmiş kitabı kaydet
            bookService.update(book.getId(), book);
        }

        // Güncellenmiş ödünç alımını Map ile kaydet
        bookBorrowingMapper.update(bookBorrowing, bookBorrowingUpdateRequest);

        return bookBorrowingRepository.save(bookBorrowing);
    }

    // Ödünç alınan kitabı sil
    public void deleteById(Long id) {
        Optional<BookBorrowing> bookBorrowingFromDb = bookBorrowingRepository.findById(id);

        if (bookBorrowingFromDb.isPresent()) {
            BookBorrowing bookBorrowing = bookBorrowingFromDb.get();
            if (bookBorrowing.getReturnDate() == null) {
                // Kitap geri verilmediyse stoğunu artır
                Book book = bookBorrowing.getBook();
                book.setStock(book.getStock() + 1);

                // Güncellenmiş kitabı kaydet
                bookService.update(book.getId(), book);
            }
            bookBorrowingRepository.delete(bookBorrowing);
        } else {
            throw new RuntimeException(id + " id'li Ödünç Alımı sistemde bulunamadı !!!");
        }
    }
}
