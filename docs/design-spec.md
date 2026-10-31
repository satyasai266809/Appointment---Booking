# Design Specification: Conflict-Safe Appointment Booking

## 1. Scope and service boundary

In scope: booking and cancelling provider appointments with a no-overlap guarantee.
Out of scope: UI, authentication, notifications, recurring availability, calendar integration,
deployment, external database, time zones other than UTC.

```
caller -> AppointmentController (HTTP mapping)
              -> AppointmentService (validation, invariant, locking)
              -> AppointmentRepository (port) -> InMemoryAppointmentRepository
```

| Type | Responsibility |
|------|----------------|
| `AppointmentController` | Maps REST requests and service results to HTTP requests, response bodies, and status codes |
| `AppointmentService` | Validates input, enforces the invariant, serialises check-then-write |
| `AppointmentRepository` | Persistence port: `save`, `findById`, `findActiveByProvider`, `findAll` |
| `InMemoryAppointmentRepository` | Map-backed implementation; not thread-safe by itself |
| `Appointment` | Immutable record: id, providerId, start, end, state (`ACTIVE`/`CANCELLED`); overlap rule |
| `BookingResult` / `CancelResult` | Outcome status plus message; no exceptions for expected rejections |

## 2. Data and API design

Appointment: `id` (UUID, server-generated), `providerId`, `start`/`end` (`Instant`, UTC), `state`.

```
POST   /api/appointments                 -> 201 BOOKED | 409 CONFLICT | 400 INVALID_INPUT
DELETE /api/appointments/{appointmentId} -> 200 CANCELLED | 404 NOT_FOUND | 409 ALREADY_CANCELLED | 400 INVALID_INPUT
```

The controller delegates to `AppointmentService`; it does not implement booking rules. Booking responses contain status, appointment (when booked), and message. Cancellation responses contain status and message.

Validation (before touching the store): non-blank provider id; both timestamps non-blank, end in `Z`,
parseable as `Instant`; `end > start`. Any failure returns `INVALID_INPUT` with no state change.

## 3. Booking invariant

For each provider, no two `ACTIVE` appointments have overlapping intervals. Intervals are half-open
`[start, end)`; overlap is `a.start < b.end && b.start < a.end`. Touching endpoints are not an overlap.
Different providers are independent. `CANCELLED` appointments are excluded from the check, so
cancelling immediately frees the time.

## 4. Concurrency approach

The invariant spans a read (find overlaps) and a write (save). Without mutual exclusion two requests can
both read "free" and both insert. `AppointmentService` holds one `ReentrantLock` around every
repository read/write in `book` and `cancel`, making check-then-write atomic. The repository is
therefore only ever accessed under the lock.

Tradeoffs considered:
- Per-provider locks: more throughput, more code (lock map lifecycle); not needed for this slice.
- Atomic `saveIfNoOverlap` on the repository: pushes domain logic into persistence; with a real
  database the equivalent is an exclusion constraint, noted as follow-up.
- `ConcurrentHashMap` alone: insufficient, it does not make check-then-insert atomic.

## 5. Persistence

In-memory only. Cancelled records are retained as `CANCELLED` so cancel can report
`ALREADY_CANCELLED` versus `NOT_FOUND`. The repository port allows a database-backed implementation,
which would need to enforce the invariant itself (transaction plus exclusion constraint).

## 6. Key assumptions

UTC only with mandatory `Z` suffix (offsets rejected rather than converted, to keep one unambiguous
format); positive duration only; past times allowed; ids server-generated. See README.

## 7. Testing strategy

JUnit 5 tests cover the service rules and HTTP adapter. Service coverage includes successful booking; five overlap shapes (identical, overlap start, overlap end, contained, containing); adjacency on both sides; different providers; cancel then rebook; cancel missing and twice; missing identifiers; invalid timestamps and non-positive durations with the store verified unchanged; and two concurrency tests (32 threads on one slot yield exactly one winner; 40 staggered overlapping requests leave no overlapping active pair). MockMvc tests verify HTTP status mapping and booking, conflict, adjacency, cancellation, rebooking, invalid input, and missing/repeated cancellation. Mutation check: with the lock disabled the concurrency tests fail (see AI record).
