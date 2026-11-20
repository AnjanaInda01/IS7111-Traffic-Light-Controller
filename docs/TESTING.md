# Verification record

Verified on 20 November 2026 using Windows 11, Oracle JDK 17.0.12 and Maven 3.9.11.

## Automated results

Both independent Maven projects packaged successfully. **29 tests passed, 0 failures, 0 errors, 0 skipped.** The original Surefire text summaries are included in `test-results/`.

| Test class | Passed | Coverage |
| --- | ---: | --- |
| `TrafficServiceTest` | 14 | Initial state; exact 10/8/3 sequence over two cycles; pause at five; fractional pause; emergency during green; emergency Stop bypass; invalid resume/pause; all manual lights; mode restrictions; stop/reset; fresh Start; delayed scheduler; concurrent operations. |
| `TrafficControllerTest` | 7 | Six-field JSON contract; action endpoints; 400/409 preservation; emergency lock/reset; manual restrictions; 404/405. |
| `TrafficApiTest` | 7 | GET/POST paths; JSON decoding; error state/message; non-JSON error; malformed success; unavailable backend; origin validation. |
| `ClientCoordinatorTest` | 1 | Slow HTTP request leaves EDT responsive; timeout error callback executes on EDT. |

Reproduce with `build-and-test.cmd` or the individual Maven commands in the README. Tests use in-memory state and temporary local HTTP servers; they do not require an already running assignment backend or a database.

The service tests use a fake monotonic clock to verify exact boundaries without sleeping. A separate real-time check was also executed against the packaged Spring Boot server over HTTP.

## Live API checks

Full observations are in `live-api-verification.json`.

| Minimum test | Observed result |
| --- | --- |
| T1 full cycle | RED observed at 0.03s, GREEN at 10.05s, YELLOW at 18.03s and RED again at 21.11s. Sampling adds up to approximately 0.12s of observation latency. |
| T2 pause at five | RED paused at 5 seconds; unchanged after a two-second wait; resumed and showed 4 after 1.1s. |
| T3 emergency during GREEN | Immediately returned RED/EMERGENCY/0; unchanged after two seconds. Start returned 409. Stop then Start/manual also returned 409 until Reset. |
| T4 invalid Resume | Resume from STOPPED returned 409 and preserved state. Invalid manual BLUE returned 400 and also preserved state. |
| Additional manual check | RED, YELLOW and GREEN each returned the selected light in MANUAL with zero countdown. |

## Desktop checks and screenshots

The packaged client and server were also started using `run-client.cmd` and `run-server.cmd`. Actual Swing controls were exercised: Start, Pause, Resume, Stop, manual Green, Emergency, and invalid Resume. The window layout was inspected at its default size and while maximized.

For **T5**, the running backend process was terminated while the desktop stayed open. The GUI displayed DISCONNECTED, an explicit connection error, no active live lamps, `--` countdown and a last-known-state label. Double-clicking its title bar successfully maximized the offline window, confirming that it still accepted UI input. Restarting the backend restored CONNECTED automatically and displayed the initial RED/STOPPED/0 state, confirming both reconnection and memory-only reset.

| Screenshot | Evidence |
| --- | --- |
| [01-initial-stopped.png](screenshots/01-initial-stopped.png) | RED / STOPPED / 00, connected. |
| [02-invalid-resume.png](screenshots/02-invalid-resume.png) | HTTP 409 message with the initial state unchanged. |
| [03-live-automatic.png](screenshots/03-live-automatic.png) | GREEN / AUTOMATIC with a live countdown. |
| [04-paused.png](screenshots/04-paused.png) | Fresh screenshot: RED / PAUSED at 5 seconds. The countdown stayed frozen while the screenshot was captured. |
| [05-manual-green.png](screenshots/05-manual-green.png) | GREEN / MANUAL / 00. |
| [06-emergency-lock.png](screenshots/06-emergency-lock.png) | RED / EMERGENCY / 00 with Reset-required warning. |
| [07-backend-unavailable.png](screenshots/07-backend-unavailable.png) | Connection error, stale label and inactive lamps after actual backend termination. |
| [08-reconnected.png](screenshots/08-reconnected.png) | Automatic reconnection and initial state after backend restart. |

## Repeat the required demonstration

1. Start both applications. Press Start and watch RED for 10 seconds, GREEN for 8, YELLOW for 3, then RED again.
2. Start a new cycle; Pause when the display reaches 5. Wait, confirm it stays at 5, then Resume.
3. During GREEN press Emergency. Confirm immediate RED, zero countdown and disabled Start/manual controls. Confirm Reset releases the lock.
4. Reset, then press Resume. Confirm the HTTP 409 message and unchanged RED/STOPPED/0.
5. Stop the backend with Ctrl+C while the desktop stays open. Confirm the error, resize or move the window, then restart the backend and observe reconnection.

GitHub repository: https://github.com/AnjanaInda01/IS7111-Traffic-Light-Controller. The screenshot report is included in docs/report/. Submission to the course has not been performed.

An additional live screenshot from the IntelliJ-launched client is included as `screenshots/09-intellij-client-live.png`.
