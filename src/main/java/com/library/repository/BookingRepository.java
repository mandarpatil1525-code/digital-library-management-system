package com.library.repository;

import com.library.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserIdOrderByBookingDateDesc(Long userId);
    Optional<Booking> findByUserIdAndBookIdAndStatus(Long userId, Long bookId, String status);
    long countByBookIdAndStatus(Long bookId, String status);
    List<Booking> findByBookId(Long bookId);
}
