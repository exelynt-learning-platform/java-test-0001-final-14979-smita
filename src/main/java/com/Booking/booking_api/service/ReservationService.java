package com.Booking.booking_api.service;

import com.Booking.booking_api.dto.ReservationRequest;
import com.Booking.booking_api.dto.ReservationResponse;
import com.Booking.booking_api.entity.Reservation;
import com.Booking.booking_api.entity.Resource;
import com.Booking.booking_api.entity.User;
import com.Booking.booking_api.enums.ReservationStatus;
import com.Booking.booking_api.exception.ResourceAlreadyBookedException;
import com.Booking.booking_api.repository.ReservationRepository;
import com.Booking.booking_api.repository.ResourceRepository;
import com.Booking.booking_api.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

@Service
public class ReservationService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "startTime", "endTime", "price",
            "status", "createdAt", "updatedAt"
    );

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;

    public ReservationService(
            ReservationRepository reservationRepository,
            UserRepository userRepository,
            ResourceRepository resourceRepository) {

        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.resourceRepository = resourceRepository;
    }

    // =========================
    // CREATE RESERVATION
    // =========================
    public ReservationResponse createReservation(
            ReservationRequest request,
            String username,
            boolean isAdmin) {

        User user;

        if (isAdmin && request.getUserId() != null) {

            user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "User not found"));
        } else {

            // USER identity comes only from JWT
            user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Authenticated user not found"));
        }

        if (!Boolean.TRUE.equals(user.isEnabled())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "User is disabled");
        }

        Resource resource = findResource(request.getResourceId());

        if (!Boolean.TRUE.equals(resource.isAvailable())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Resource is not available");
        }

        validateTimes(
                request.getStartTime(),
                request.getEndTime());

        validatePrice(request.getPrice());

        ReservationStatus status =
                isAdmin && request.getStatus() != null
                        ? request.getStatus()
                        : ReservationStatus.PENDING;

        checkNoConflict(
                resource.getId(),
                request.getStartTime(),
                request.getEndTime(),
                null);

        Reservation reservation = new Reservation();

        reservation.setUser(user);
        reservation.setResource(resource);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setPrice(request.getPrice());
        reservation.setStatus(status);

        return ReservationResponse.from(
                reservationRepository.save(reservation));
    }

    // =========================
    // GET ALL RESERVATIONS
    // =========================
    @Transactional(readOnly = true)
    public Page<ReservationResponse> getReservations(
            String username,
            boolean isAdmin,
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sort) {

        validatePagination(page, size);

        if (minPrice != null && minPrice.signum() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "minPrice must be zero or greater");
        }

        if (maxPrice != null && maxPrice.signum() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "maxPrice must be zero or greater");
        }

        if (minPrice != null
                && maxPrice != null
                && minPrice.compareTo(maxPrice) > 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "minPrice cannot be greater than maxPrice");
        }

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        buildSort(sort));

        Page<Reservation> reservations;

        if (isAdmin) {

            reservations =
                    reservationRepository.searchReservations(
                            status,
                            minPrice,
                            maxPrice,
                            pageable);

        } else {

            // USER gets only his own reservations
            reservations =
                    reservationRepository.searchUserReservations(
                            username,
                            status,
                            minPrice,
                            maxPrice,
                            pageable);
        }

        return reservations.map(
                ReservationResponse::from);
    }

    // =========================
    // GET RESERVATION BY ID
    // =========================
    @Transactional(readOnly = true)
    public Optional<ReservationResponse> getReservationById(
            Long id,
            String username,
            boolean isAdmin) {

        Optional<Reservation> reservation =
                reservationRepository.findById(id);

        if (reservation.isEmpty()) {
            return Optional.empty();
        }

        Reservation existing = reservation.get();

        // USER can access only his own reservation
        if (!isAdmin
                && !existing.getUser()
                .getUsername()
                .equals(username)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not allowed to access this reservation");
        }

        return Optional.of(
                ReservationResponse.from(existing));
    }

    // =========================
    // UPDATE RESERVATION
    // ADMIN ONLY
    // =========================
    public ReservationResponse updateReservation(
            Long id,
            ReservationRequest request) {

        Reservation existing =
                reservationRepository.findById(id)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Reservation not found"));

        User user = existing.getUser();

        if (request.getUserId() != null) {

            user = userRepository.findById(
                            request.getUserId())
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "User not found"));
        }

        Resource resource =
                findResource(request.getResourceId());

        if (!Boolean.TRUE.equals(resource.isAvailable())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Resource is not available");
        }

        validateTimes(
                request.getStartTime(),
                request.getEndTime());

        validatePrice(request.getPrice());

        ReservationStatus status =
                request.getStatus() == null
                        ? existing.getStatus()
                        : request.getStatus();

        checkNoConflict(
                resource.getId(),
                request.getStartTime(),
                request.getEndTime(),
                existing.getId());

        existing.setUser(user);
        existing.setResource(resource);
        existing.setStartTime(request.getStartTime());
        existing.setEndTime(request.getEndTime());
        existing.setPrice(request.getPrice());
        existing.setStatus(status);

        return ReservationResponse.from(
                reservationRepository.save(existing));
    }

    // =========================
    // DELETE RESERVATION
    // ADMIN ONLY
    // =========================
    public void deleteReservation(Long id) {

        if (!reservationRepository.existsById(id)) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Reservation not found");
        }

        reservationRepository.deleteById(id);
    }

    // =========================
    // FIND RESOURCE
    // =========================
    private Resource findResource(Long resourceId) {

        return resourceRepository.findById(resourceId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Resource not found"));
    }

    // =========================
    // VALIDATE TIME
    // =========================
    private void validateTimes(
            java.time.LocalDateTime startTime,
            java.time.LocalDateTime endTime) {

        if (!startTime.isBefore(endTime)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Start time must be before end time");
        }
    }

    // =========================
    // VALIDATE PRICE
    // =========================
    private void validatePrice(BigDecimal price) {

        if (price == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Price is required");
        }

        if (price.signum() < 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Price must be zero or greater");
        }
    }

    // =========================
    // CHECK DOUBLE BOOKING
    // =========================
    private void checkNoConflict(
            Long resourceId,
            java.time.LocalDateTime startTime,
            java.time.LocalDateTime endTime,
            Long excludedReservationId) {

        boolean alreadyBooked;

        if (excludedReservationId == null) {

            alreadyBooked =
                    reservationRepository
                            .existsByResourceIdAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThan(
                                    resourceId,
                                    ReservationStatus.CANCELLED,
                                    endTime,
                                    startTime);

        } else {

            alreadyBooked =
                    reservationRepository
                            .existsByResourceIdAndIdNotAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThan(
                                    resourceId,
                                    excludedReservationId,
                                    ReservationStatus.CANCELLED,
                                    endTime,
                                    startTime);
        }

        if (alreadyBooked) {

            throw new ResourceAlreadyBookedException(
                    "Resource is already booked for the selected time");
        }
    }

    // =========================
    // SORT
    // =========================
    private Sort buildSort(String sort) {

        if (sort == null || sort.isBlank()) {

            return Sort.by(
                    Sort.Direction.ASC,
                    "id");
        }

        String[] parts = sort.split(",");

        if (parts.length > 2
                || !ALLOWED_SORT_FIELDS.contains(parts[0])) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid sort field. Allowed fields: "
                            + ALLOWED_SORT_FIELDS);
        }

        Sort.Direction direction =
                Sort.Direction.ASC;

        if (parts.length == 2) {

            try {

                direction =
                        Sort.Direction.fromString(
                                parts[1].trim());

            } catch (IllegalArgumentException ex) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Sort direction must be asc or desc");
            }
        }

        return Sort.by(
                direction,
                parts[0]);
    }

    // =========================
    // PAGINATION
    // =========================
    private void validatePagination(
            int page,
            int size) {

        if (page < 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "page must be zero or greater");
        }

        if (size < 1 || size > 100) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "size must be between 1 and 100");
        }
    }
}