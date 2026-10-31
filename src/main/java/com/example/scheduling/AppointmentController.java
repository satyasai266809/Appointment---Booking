package com.example.scheduling;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService service;

    public AppointmentController(AppointmentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> book(@RequestBody BookingRequest request) {
        BookingResult result = service.book(request.providerId(), request.startUtc(), request.endUtc());
        BookingResponse response = new BookingResponse(result.status(), result.appointment(), result.message());

        HttpStatus status = switch (result.status()) {
            case BOOKED -> HttpStatus.CREATED;
            case CONFLICT -> HttpStatus.CONFLICT;
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(response);
    }

    @DeleteMapping("/{appointmentId}")
    public ResponseEntity<CancellationResponse> cancel(@PathVariable("appointmentId") String appointmentId) {
        CancelResult result = service.cancel(appointmentId);
        CancellationResponse response = new CancellationResponse(result.status(), result.message());

        HttpStatus status = switch (result.status()) {
            case CANCELLED -> HttpStatus.OK;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ALREADY_CANCELLED -> HttpStatus.CONFLICT;
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(response);
    }

    public record BookingRequest(String providerId, String startUtc, String endUtc) { }

    public record BookingResponse(BookingResult.Status status, Appointment appointment, String message) { }

    public record CancellationResponse(CancelResult.Status status, String message) { }
}