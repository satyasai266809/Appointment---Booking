package com.example.scheduling.service;

import com.example.scheduling.model.Appointment;

public record BookingResult(Status status, Appointment appointment, String message) {

    public enum Status { BOOKED, CONFLICT, INVALID_INPUT }

    public boolean isBooked() {
        return status == Status.BOOKED;
    }

    static BookingResult booked(Appointment appointment) {
        return new BookingResult(Status.BOOKED, appointment, "Appointment booked");
    }

    static BookingResult conflict(Appointment existing) {
        return new BookingResult(Status.CONFLICT, null,
                "Provider already has an active appointment (" + existing.id() + ") overlapping that time");
    }

    static BookingResult invalid(String message) {
        return new BookingResult(Status.INVALID_INPUT, null, message);
    }
}