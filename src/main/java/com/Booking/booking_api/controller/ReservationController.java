package com.Booking.booking_api.controller;

import com.Booking.booking_api.dto.ReservationRequest;
import com.Booking.booking_api.dto.ReservationResponse;
import com.Booking.booking_api.enums.ReservationStatus;
import com.Booking.booking_api.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/reservations")
@SecurityRequirement(name = "Bearer Authentication")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Operation(summary = "View reservations")
    @GetMapping
    public ResponseEntity<Page<ReservationResponse>> getReservations(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(example = "price,desc")
            @RequestParam(required = false) String sort,
            Authentication authentication) {

        boolean admin = isAdmin(authentication);

        return ResponseEntity.ok(reservationService.getReservations(
                authentication.getName(),
                admin,
                status,
                minPrice,
                maxPrice,
                page,
                size,
                sort));
    }

    @Operation(summary = "View one reservation")
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservationById(
            @PathVariable Long id,
            Authentication authentication) {

        return reservationService.getReservationById(
                        id,
                        authentication.getName(),
                        isAdmin(authentication))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create a reservation")
    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody ReservationRequest request,
            Authentication authentication) {

        ReservationResponse created = reservationService.createReservation(
                request,
                authentication.getName(),
                isAdmin(authentication));

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Update a reservation - ADMIN only")
    @PutMapping("/{id}")
    public ResponseEntity<ReservationResponse> updateReservation(
            @PathVariable Long id,
            @Valid @RequestBody ReservationRequest request) {

        return ResponseEntity.ok(
                reservationService.updateReservation(id, request));
    }

    @Operation(summary = "Delete a reservation - ADMIN only")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id) {
        reservationService.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}