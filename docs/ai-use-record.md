# AI Prompt and Use Record

No credentials, secrets or personal data were included in any prompt.
Review notes below describe what actually happened in the AI sessions. The submitter should add any
further personal review observations before submitting.

## Activity 1: Implementation and test generation (GitHub Copilot agent in VS Code)

### Prompt 1
> Assessment brief pasted verbatim: "AI-Assisted Engineering Assessment: Appointment Booking ...
> implement conflict-safe booking and cancellation ... use UTC ... tests ... design spec ... AI prompt
> record ... setup instructions". Preceded by: "we have assessment I will give problem help me with
> solution ok be ready in java".

### AI response summary
Inspected the workspace (empty `Demo.java`, JDK 21, Maven present; no starter project), chose the
in-memory service fallback, and produced: `Appointment`, `AppointmentRepository`,
`InMemoryAppointmentRepository`, `AppointmentService`, `BookingResult`, `CancelResult`, a Maven build,
`AppointmentServiceTest`, README and design spec.

### Suggestion decisions

| Suggestion | Decision | Reason / evidence |
|-----------|----------|-------------------|
| Half-open `[start, end)` interval and `a.start < b.end && b.start < a.end` | Accepted | Directly encodes adjacency rule (criterion 3); covered by the adjacency test |
| Single `ReentrantLock` in the service around check-and-write | Accepted | Simplest correct option for in-memory store; alternatives (per-provider locks, repository-level atomic insert) recorded as tradeoffs in the design spec |
| Result objects instead of exceptions for rejections | Accepted | Criteria require a "clear result" for cancel misses; keeps invalid input side-effect free |
| Use `Instant.parse` alone for timestamp validation | Edited | Review finding: since JDK 12 `Instant.parse` also accepts offsets like `+02:00`. Added an explicit `endsWith("Z")` check, and a test case for `+02:00` |
| Rely on a thread-safe map in the repository instead of a service lock | Rejected | Does not make check-then-insert atomic |
| Delete cancelled appointments instead of marking them | Rejected | Would make "already cancelled" indistinguishable from "missing" (criterion 4) |
| Add a frontend / CLI / extra database | Rejected | Out of scope per brief |

### Files and decisions informed
All files under `src/` and `docs/`, `pom.xml`, `README.md`.

### Verification evidence
- `mvn -q test` : Tests run: 28, Failures: 0, Errors: 0, Skipped: 0 (JDK 21 target, Maven 3.9.16).
- Mutation check on the concurrency design: with `lock.lock()`/`lock.unlock()` commented out in
  `AppointmentService`, `mvn test` failed (1 failure, 1 error, including
  `ConcurrentModificationException` in `concurrentPartiallyOverlappingBookingsNeverOverlap`); with the
  lock restored all 28 tests pass. This shows the concurrency tests detect a missing lock.
- Manual review of overlap boundaries against the invariant (touching endpoints, containment).

### Not verified / candid notes
- Concurrency tests are probabilistic: they cannot prove the absence of races, only detect common ones.
- No persistence-backed implementation exists, so DB-level enforcement is untested.

## Activity 2: Review of the delivered work against the brief (GitHub Copilot agent in VS Code)

### Prompts
> "check project"
>
> Brief pasted again with: "this problem statement is covered fully check once", then "if any gaps are
> fix it and run".

### AI response summary
Built and ran the tests (28 passing), then mapped each acceptance criterion and deliverable to code, tests
and documents. All criteria were met; it reported four minor gaps.

### Suggestion decisions

| Suggestion | Decision | Reason / evidence |
|-----------|----------|-------------------|
| Add `.gitignore` excluding `target/` | Accepted | Build output should not be submitted |
| Make the cancel-invalid-input test assert the store is unchanged | Accepted | Criterion 5 covers cancel as well as book |
| State in README that provider ids are compared exactly | Accepted | Documents behaviour that was implicit |
| Replace the placeholder in this record's header | Accepted | Placeholder was not a review observation |

### Files and decisions informed
`.gitignore`, `AppointmentServiceTest.java`, `README.md`, this file.

### Verification evidence
- `mvn test` re-run after the changes (28 tests, 0 failures).

## Activity 3: REST controller addition (GitHub Copilot agent in VS Code)

### Prompt
> "add controller class as well"

After clarification, the user selected a Spring REST controller with HTTP endpoints.

### AI response summary
Added Spring Boot application wiring, an `AppointmentController` with booking and cancellation routes, HTTP status mapping, and MockMvc tests. The controller delegates to `AppointmentService`, keeping the booking invariant in the existing service layer.

### Suggestion decisions

| Suggestion | Decision | Reason / evidence |
|-----------|----------|-------------------|
| Use Spring Boot REST rather than a plain Java facade | Accepted | User selected real HTTP endpoints for the controller |
| Map booking/cancellation outcomes to HTTP status codes | Accepted | Gives callers standard HTTP semantics while preserving existing result statuses in JSON |
| Pass the path-variable name explicitly | Edited | Initial MockMvc run exposed that compiler parameter metadata was unavailable; explicit `@PathVariable("appointmentId")` fixes binding without depending on compiler flags |

### Files and decisions informed
`pom.xml`, `AppointmentBookingApplication.java`, `AppointmentController.java`, `AppointmentControllerTest.java`, README, design spec, this record, and the external end-to-end walkthrough.

### Verification evidence
- Focused MockMvc controller tests passed after the explicit path-variable fix.
- Full `mvn test`: 30 tests run, 0 failures, 0 errors, 0 skipped.

## Activity 4: Layered Spring Boot package structure

### Prompt
> "leave it can you make it simple model and dto and repo and service and controller entire project spring boot strucuree"

### AI response summary
Kept the in-memory persistence choice and reorganized the Spring Boot code into `model`, `dto`, `repository`, `service`, and `controller` packages. The application bootstrap remains in the root package. Extracted HTTP request/response records from the controller into DTO files; booking rules and endpoint behavior remain unchanged.

### Suggestion decisions

| Suggestion | Decision | Reason / evidence |
|-----------|----------|-------------------|
| Use conventional packages by layer | Accepted | Makes each boundary easier to find without introducing extra infrastructure |
| Keep persistence in memory and do not add JPA | Accepted | The user clarified "leave it" after the JPA discussion and requested package structure |
| Keep service results separate from HTTP DTOs | Accepted | Prevents the service outcome types from depending on the controller transport layer |

### Files and decisions informed
Java classes under `src/main/java/com/example/scheduling/`, tests under matching package paths, and this design spec.

### Verification evidence
- `mvn clean test`: 30 tests run, 0 failures, 0 errors, 0 skipped.

## Activity 4: Layered Spring Boot package structure

### Prompt
> "leave it can you make it simple model and dto and repo and service and controller entire project spring boot strucuree"

### AI response summary
Kept the in-memory persistence choice and reorganized the Spring Boot code into `model`, `dto`, `repository`, `service`, and `controller` packages. The application bootstrap remains in the root package. Extracted the HTTP request/response records from the controller into DTO files; booking and cancellation behavior and routes remain unchanged.

### Suggestion decisions

| Suggestion | Decision | Reason / evidence |
|-----------|----------|-------------------|
| Use conventional packages by layer | Accepted | Makes each boundary easier to find without introducing extra infrastructure |
| Keep persistence in memory and do not add JPA | Accepted | User clarified "leave it" after the JPA discussion; this request was for project structure |
| Keep service results separate from HTTP DTOs | Accepted | Avoids making service outcome types depend on the controller transport layer |

### Files and decisions informed
Java classes under `src/main/java/com/example/scheduling/`, service and controller tests under matching test packages, and this design spec.

### Verification evidence
- The first `mvn test` run encountered stale compiled classes from the previous package paths in `target/` and failed test discovery.
- `mvn clean test` removed stale output and passed all 30 tests (0 failures, 0 errors, 0 skipped).
