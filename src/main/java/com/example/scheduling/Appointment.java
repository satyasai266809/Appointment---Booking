package com.example.scheduling;

import java.time.Instant;

/** An appointment occupying the half-open interval [start, end) in UTC. */
public record Appointment(String id, String providerId, Instant start, Instant end, State state) {

    public enum State { ACTIVE, CANCELLED }

    public boolean isActive() {
        return state == State.ACTIVE;
    }

    /** Half-open overlap: appointments that merely touch at an endpoint do not overlap. */
    public boolean overlaps(Instant otherStart, Instant otherEnd) {
        return start.isBefore(otherEnd) && otherStart.isBefore(end);
    }

    public Appointment cancelled() {
        return new Appointment(id, providerId, start, end, State.CANCELLED);
    }
}
