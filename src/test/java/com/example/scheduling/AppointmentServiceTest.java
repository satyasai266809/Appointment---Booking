package com.example.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class AppointmentServiceTest {

    private static final String T09 = "2030-01-01T09:00:00Z";
    private static final String T10 = "2030-01-01T10:00:00Z";
    private static final String T11 = "2030-01-01T11:00:00Z";
    private static final String T12 = "2030-01-01T12:00:00Z";

    private InMemoryAppointmentRepository repository;
    private AppointmentService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAppointmentRepository();
        service = new AppointmentService(repository);
    }

    @Test
    void booksValidAppointment() {
        BookingResult result = service.book("dr-a", T09, T10);

        assertEquals(BookingResult.Status.BOOKED, result.status());
        Appointment a = result.appointment();
        assertNotNull(a.id());
        assertEquals("dr-a", a.providerId());
        assertEquals(Instant.parse(T09), a.start());
        assertEquals(Instant.parse(T10), a.end());
        assertTrue(a.isActive());
        assertEquals(1, repository.findAll().size());
    }

    @ParameterizedTest(name = "[{index}] {0}-{1} overlaps 09:00-11:00")
    @CsvSource({
            "2030-01-01T09:00:00Z, 2030-01-01T11:00:00Z",  // identical
            "2030-01-01T08:00:00Z, 2030-01-01T09:30:00Z",  // overlaps start
            "2030-01-01T10:30:00Z, 2030-01-01T12:00:00Z",  // overlaps end
            "2030-01-01T09:30:00Z, 2030-01-01T10:30:00Z",  // contained
            "2030-01-01T08:00:00Z, 2030-01-01T12:00:00Z"   // contains
    })
    void rejectsOverlapForSameProvider(String start, String end) {
        service.book("dr-a", T09, T11);

        BookingResult result = service.book("dr-a", start, end);

        assertEquals(BookingResult.Status.CONFLICT, result.status());
        assertNull(result.appointment());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void allowsAdjacentAppointments() {
        assertTrue(service.book("dr-a", T10, T11).isBooked());

        assertTrue(service.book("dr-a", T09, T10).isBooked());   // ends exactly when the first starts
        assertTrue(service.book("dr-a", T11, T12).isBooked());   // starts exactly when the first ends
        assertEquals(3, repository.findAll().size());
    }

    @Test
    void doesNotConflictAcrossProviders() {
        assertTrue(service.book("dr-a", T09, T10).isBooked());
        assertTrue(service.book("dr-b", T09, T10).isBooked());
    }

    @Test
    void cancelledAppointmentNoLongerBlocksTime() {
        Appointment first = service.book("dr-a", T09, T10).appointment();

        assertEquals(CancelResult.Status.CANCELLED, service.cancel(first.id()).status());

        assertTrue(service.book("dr-a", T09, T10).isBooked());
    }

    @Test
    void cancelMissingAppointmentReturnsNotFound() {
        assertEquals(CancelResult.Status.NOT_FOUND, service.cancel("does-not-exist").status());
    }

    @Test
    void cancelTwiceReturnsAlreadyCancelled() {
        Appointment a = service.book("dr-a", T09, T10).appointment();
        service.cancel(a.id());

        CancelResult second = service.cancel(a.id());

        assertEquals(CancelResult.Status.ALREADY_CANCELLED, second.status());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void cancelRejectsMissingIdentifierWithoutChangingStore(String id) {
        Appointment existing = service.book("dr-a", T09, T10).appointment();

        assertEquals(CancelResult.Status.INVALID_INPUT, service.cancel(id).status());

        assertEquals(List.of(existing), repository.findAll().stream().toList());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void bookRejectsMissingProviderWithoutChangingStore(String providerId) {
        assertInvalidAndUnchanged(service.book(providerId, T09, T10));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"not-a-date", "2030-01-01", "2030-01-01T09:00:00", "2030-01-01T09:00:00+02:00",
            "2030-13-01T09:00:00Z"})
    void bookRejectsInvalidTimestampsWithoutChangingStore(String bad) {
        assertInvalidAndUnchanged(service.book("dr-a", bad, T10));
        assertInvalidAndUnchanged(service.book("dr-a", T09, bad));
    }

    @Test
    void bookRejectsNonPositiveDurationWithoutChangingStore() {
        assertInvalidAndUnchanged(service.book("dr-a", T10, T10));  // zero
        assertInvalidAndUnchanged(service.book("dr-a", T10, T09));  // negative
    }

    @Test
    void invalidInputDoesNotAffectExistingBookings() {
        service.book("dr-a", T09, T10);

        service.book("dr-a", "garbage", T10);

        assertEquals(1, repository.findAll().size());
    }

    @Test
    void concurrentBookingsOfSameSlotYieldExactlyOneWinner() throws Exception {
        int threads = 32;
        List<BookingResult> results = runConcurrently(threads, i -> service.book("dr-a", T09, T10));

        assertEquals(1, results.stream().filter(BookingResult::isBooked).count());
        assertEquals(threads - 1, results.stream()
                .filter(r -> r.status() == BookingResult.Status.CONFLICT).count());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void concurrentPartiallyOverlappingBookingsNeverOverlap() throws Exception {
        // 40 requests of 2h starting every 30 minutes: many mutually overlapping candidates.
        Instant base = Instant.parse(T09);
        runConcurrently(40, i -> service.book("dr-a",
                base.plusSeconds(i * 1800L).toString(),
                base.plusSeconds(i * 1800L + 7200L).toString()));

        List<Appointment> active = repository.findActiveByProvider("dr-a");
        assertTrue(!active.isEmpty());
        for (int i = 0; i < active.size(); i++) {
            for (int j = i + 1; j < active.size(); j++) {
                assertTrue(!active.get(i).overlaps(active.get(j).start(), active.get(j).end()),
                        "overlap between " + active.get(i) + " and " + active.get(j));
            }
        }
    }

    private void assertInvalidAndUnchanged(BookingResult result) {
        assertEquals(BookingResult.Status.INVALID_INPUT, result.status());
        assertNull(result.appointment());
        assertTrue(repository.findAll().isEmpty());
    }

    private interface Task {
        BookingResult run(int index);
    }

    private static List<BookingResult> runConcurrently(int count, Task task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<BookingResult>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                int index = i;
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    return task.run(index);
                }));
            }
            ready.await();
            go.countDown();
            List<BookingResult> results = new ArrayList<>();
            for (Future<BookingResult> f : futures) {
                results.add(f.get(10, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }
}
