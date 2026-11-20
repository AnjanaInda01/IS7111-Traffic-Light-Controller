# Requirement mapping and design

## Scenarios

| ID | Implementation | Verification |
| --- | --- | --- |
| S1 | Service starts with RED, STOPPED, zero remaining and no emergency. | Service initial-state and REST contract tests. |
| S2 | Light enum defines 10/8/3 durations and RED -> GREEN -> YELLOW -> RED. | Exact boundary tests over two complete cycles and live cycle verification. |
| S3 | Backend computes ceiling of remaining monotonic duration. UI renders only the returned light and seconds. | Per-second assertions through the full cycle; three visible lamps. |
| S4 | Pause stores remaining nanoseconds; Resume creates a deadline from that duration. | Pause at 5 seconds, long wait, Resume; fractional-second test. |
| S5 | Manual accepted only in STOPPED/MANUAL and without an emergency lock. | All three lights; running/paused rejection tests. |
| S6 | Emergency atomically forces RED, cancels timing and latches a flag. | Green-phase emergency, long wait, restart/manual rejection tests. |
| S7 | Stop clears timing to RED/STOPPED/0; Reset also clears emergency. | Stop/reset tests and emergency bypass tests. |
| S8 | Invalid light -> 400; invalid mode -> 409; unsupported route -> 404; unsupported HTTP method -> 405. | MockMvc and service state-preservation tests. |
| S9 | HTTP executes on a dedicated worker, with two-second timeout; view callbacks run on EDT. | Client outage/slow-server tests and desktop offline verification. |

## Thread safety and timing

`TrafficService` is a singleton Spring service. Every public operation, tick and snapshot is synchronized using the same monitor. State changes and snapshots are atomic. The returned `TrafficState` record is immutable; callers have no mutable reference into the service.

One `@Scheduled(fixedRate = 1000)` task exists for the application lifetime. Start, Pause, Resume and other commands never create timers. `System.nanoTime()` provides monotonic timing, avoiding wall-clock corrections. A phase deadline is retained across scheduler delays, so transitions do not drift by adding processing latency on each tick. Status reads also advance elapsed phases, ensuring the client sees the current phase even between scheduled calls. Large delays skip whole 21-second cycles and then catch up the remaining phase boundaries.

Pause stores the exact nanoseconds until the deadline. The display rounds up to a whole second. Resuming preserves the fractional second as well as the visible countdown. In PAUSED/MANUAL/STOPPED/EMERGENCY, ticks cannot switch lights.

The client has one serial background executor for HTTP polls and commands. A status request is scheduled every second; an unavailable server is bounded by a two-second request timeout. Serial requests prevent an old poll response from replacing a newer command response. Swing components are created and updated only on the Event Dispatch Thread. A pending-action guard prevents repeated button clicks from filling a request queue.

## Storage and architecture

All authoritative controller state is in backend fields. There is no repository layer, database dependency or state serialization. Backend restart constructs a new initial state. Logs, documentation and screenshots are development/submission artifacts; the application does not save traffic state to files.

The two Maven projects can be compiled independently. The server separates REST controller, exception handler, service and model. The client separates HTTP, response model, coordinator, frame and signal painting.

## Explicit behavior choices

- `running=true` only in AUTOMATIC; PAUSED has `running=false`.
- Manual changes may follow other manual changes without another Stop, because no automatic cycle is active.
- Start always begins a fresh RED/10 cycle, including from PAUSED/MANUAL, unless emergency is latched.
- Stop during emergency returns RED/STOPPED/0 but retains the emergency flag; Reset is still mandatory.
- Repeated Stop, Reset and Emergency are safe.
- Light names are accepted case-insensitively, while responses use uppercase names.
- Rejected operations do not change the controller. During an existing automatic cycle, natural elapsed-time progression continues.
- HTTP errors include a human-readable message and a current state snapshot. These snapshots are obtained after command rejection; another concurrent command may legitimately change the state before the error snapshot is read.
