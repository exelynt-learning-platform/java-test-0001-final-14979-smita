package com.Booking.booking_api.repository;

import com.Booking.booking_api.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
}