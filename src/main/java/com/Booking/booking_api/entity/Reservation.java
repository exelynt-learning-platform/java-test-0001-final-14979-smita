package com.Booking.booking_api.entity;

import com.Booking.booking_api.enums.ReservationStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "reservations",
        indexes = {
                @Index(
                        name = "idx_reservation_user",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_reservation_resource",
                        columnList = "resource_id"
                ),
                @Index(
                        name = "idx_reservation_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_reservation_start_time",
                        columnList = "start_time"
                ),
                @Index(
                        name = "idx_reservation_end_time",
                        columnList = "end_time"
                )
        }
)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_reservation_user"
            )
    )
    private User user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "resource_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_reservation_resource"
            )
    )
    private Resource resource;

    @Column(nullable = false)
    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @Column(nullable = false)
    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    @Column(
            nullable = false,
            precision = 12,
            scale = 2
    )
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Price must be zero or greater")
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status =
            ReservationStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = ReservationStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();
    }

    public Reservation() {
    }

    public Reservation(
            User user,
            Resource resource,
            LocalDateTime startTime,
            LocalDateTime endTime,
            BigDecimal price
    ) {

        this.user = user;
        this.resource = resource;
        this.startTime = startTime;
        this.endTime = endTime;
        this.price = price;
        this.status = ReservationStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Resource getResource() {
        return resource;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}