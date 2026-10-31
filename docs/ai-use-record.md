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
- `mvn test` re-run after the changes (see final run in the submission notes: 28 tests, 0 failures).
