package com.example.scheduling.service;

import com.example.scheduling.model.Appointment;
import com.example.scheduling.repository.AppointmentRepository;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Invariant: for any provider, no two ACTIVE appointments overlap.
 * Check-then-write is made atomic by a single lock, so concurrent callers cannot both pass the check.
 */
public class AppointmentService {

    private final AppointmentRepository repository;
    private final ReentrantLock lock = new ReentrantLock();

    public AppointmentService(AppointmentRepository repository) {
        this.repository = repository;
    }

    /** Timestamps must be ISO-8601 instants in UTC, e.g. {@code 2030-01-01T09:00:00Z}. */
    public BookingResult book(String providerId, String startUtc, String endUtc) {
        if (isBlank(providerId)) {
            return BookingResult.invalid("providerId is required");
        }
        Instant start;
        Instant end;
        try {
            start = parseUtc(startUtc);
            end = parseUtc(endUtc);
        } catch (IllegalArgumentException e) {
            return BookingResult.invalid(e.getMessage());
        }
        if (!end.isAfter(start)) {
            return BookingResult.invalid("end must be after start");
        }

        lock.lock();
        try {
            Optional<Appointment> clash = repository.findActiveByProvider(providerId).stream()
                    .filter(appointment -> appointment.overlaps(start, end))
                    .findFirst();
            if (clash.isPresent()) {
                return BookingResult.conflict(clash.get());
            }
            Appointment appointment = new Appointment(
                    UUID.randomUUID().toString(), providerId, start, end, Appointment.State.ACTIVE);
            repository.save(appointment);
            return BookingResult.booked(appointment);
        } finally {
            lock.unlock();
        }
    }

    public CancelResult cancel(String appointmentId) {
        if (isBlank(appointmentId)) {
            return CancelResult.of(CancelResult.Status.INVALID_INPUT, "appointmentId is required");
        }
        lock.lock();
        try {
            Optional<Appointment> found = repository.findById(appointmentId);
            if (found.isEmpty()) {
                return CancelResult.of(CancelResult.Status.NOT_FOUND, "No appointment with id " + appointmentId);
            }
            if (!found.get().isActive()) {
                return CancelResult.of(CancelResult.Status.ALREADY_CANCELLED,
                        "Appointment " + appointmentId + " is already cancelled");
            }
            repository.save(found.get().cancelled());
            return CancelResult.of(CancelResult.Status.CANCELLED, "Appointment cancelled");
        } finally {
            lock.unlock();
        }
    }

    private static Instant parseUtc(String value) {
        if (isBlank(value) || !value.endsWith("Z")) {
            throw new IllegalArgumentException("timestamp must be an ISO-8601 UTC instant ending in 'Z'");
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("invalid timestamp");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}