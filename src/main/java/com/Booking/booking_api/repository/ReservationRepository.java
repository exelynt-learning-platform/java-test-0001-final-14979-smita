package com.Booking.booking_api.repository;

import com.Booking.booking_api.entity.Reservation;
import com.Booking.booking_api.enums.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByResourceIdAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThan(
            Long resourceId,
            ReservationStatus status,
            LocalDateTime endTime,
            LocalDateTime startTime
    );

    boolean existsByResourceIdAndIdNotAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThan(
            Long resourceId,
            Long reservationId,
            ReservationStatus status,
            LocalDateTime endTime,
            LocalDateTime startTime
    );

    @Query("""
        SELECT r
        FROM Reservation r
        WHERE (:status IS NULL OR r.status = :status)
          AND (:minPrice IS NULL OR r.price >= :minPrice)
          AND (:maxPrice IS NULL OR r.price <= :maxPrice)
        """)
    Page<Reservation> searchReservations(
            @Param("status") ReservationStatus status,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );

    @Query("""
        SELECT r
        FROM Reservation r
        WHERE r.user.username = :username
          AND (:status IS NULL OR r.status = :status)
          AND (:minPrice IS NULL OR r.price >= :minPrice)
          AND (:maxPrice IS NULL OR r.price <= :maxPrice)
        """)
    Page<Reservation> searchUserReservations(
            @Param("username") String username,
            @Param("status") ReservationStatus status,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );
}
