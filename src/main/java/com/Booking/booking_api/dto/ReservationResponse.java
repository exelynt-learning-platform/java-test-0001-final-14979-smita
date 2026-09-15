package com.Booking.booking_api.dto;

import com.Booking.booking_api.entity.Reservation;
import com.Booking.booking_api.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReservationResponse {

    private Long id;
    private Long userId;
    private String username;
    private Long resourceId;
    private String resourceName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal price;
    private ReservationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ReservationResponse() {}

    public static ReservationResponse from(Reservation reservation) {
        ReservationResponse response = new ReservationResponse();
        response.id = reservation.getId();
        response.userId = reservation.getUser().getId();
        response.username = reservation.getUser().getUsername();
        response.resourceId = reservation.getResource().getId();
        response.resourceName = reservation.getResource().getName();
        response.startTime = reservation.getStartTime();
        response.endTime = reservation.getEndTime();
        response.price = reservation.getPrice();
        response.status = reservation.getStatus();
        response.createdAt = reservation.getCreatedAt();
        response.updatedAt = reservation.getUpdatedAt();
        return response;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public Long getResourceId() { return resourceId; }
    public String getResourceName() { return resourceName; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public BigDecimal getPrice() { return price; }
    public ReservationStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
