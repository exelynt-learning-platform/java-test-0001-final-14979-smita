package com.Booking.booking_api.exception;

public class ResourceAlreadyBookedException extends RuntimeException {

    public ResourceAlreadyBookedException(String message) {
        super(message);
    }
}