package lk.ac.is7111.traffic.service;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lk.ac.is7111.traffic.model.Light;
import lk.ac.is7111.traffic.model.Mode;
import lk.ac.is7111.traffic.model.TrafficState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TrafficServiceTest {
    private AtomicLong now;
    private TrafficService service;
    @BeforeEach void setUp() { now = new AtomicLong(); service = new TrafficService(now::get); }
    private void advance(double seconds) { now.addAndGet((long) (seconds * 1_000_000_000L)); service.tick(); }
    private void assertState(Light light, Mode mode, int seconds) {
        TrafficState state = service.status();
        assertEquals(light, state.currentLight());
        assertEquals(mode, state.mode());
        assertEquals(seconds, state.remainingSeconds());
        assertEquals(mode == Mode.AUTOMATIC, state.running());
    }

    @Test void initialState() {
        assertState(Light.RED, Mode.STOPPED, 0);
        assertFalse(service.status().emergency());
    }
    @Test void fullCycleHasExactDurationsAndRepeats() {
        service.start();
        for (int round = 0; round < 2; round++) {
            for (Light light : new Light[]{Light.RED, Light.GREEN, Light.YELLOW}) {
                for (int remaining = light.durationSeconds(); remaining > 0; remaining--) {
                    assertState(light, Mode.AUTOMATIC, remaining);
                    advance(1);
                }
            }
        }
        assertState(Light.RED, Mode.AUTOMATIC, 10);
    }
    @Test void pauseAtFiveThenResumeKeepsTime() {
        service.start(); advance(5); service.pause();
        TrafficState paused = service.status();
        advance(60);
        assertEquals(paused, service.status());
        assertState(Light.RED, Mode.PAUSED, 5);
        service.resume(); advance(1);
        assertState(Light.RED, Mode.AUTOMATIC, 4);
    }
    @Test void pausePreservesFractionalSecond() {
        service.start(); advance(5.4); service.pause(); advance(100);
        service.resume(); advance(0.59);
        assertState(Light.RED, Mode.AUTOMATIC, 5);
        advance(0.01);
        assertState(Light.RED, Mode.AUTOMATIC, 4);
    }
    @Test void emergencyDuringGreenCancelsSwitching() {
        service.start(); advance(10);
        assertState(Light.GREEN, Mode.AUTOMATIC, 8);
        service.emergency(); advance(500);
        assertState(Light.RED, Mode.EMERGENCY, 0);
        assertTrue(service.status().emergency());
        TrafficState before = service.status();
        assertThrows(TrafficException.class, service::start);
        assertThrows(TrafficException.class, service::resume);
        assertThrows(TrafficException.class, () -> service.manual("GREEN"));
        assertEquals(before, service.status());
    }
    @Test void stopCannotBypassEmergencyLockAndResetReleasesIt() {
        service.emergency(); service.stop();
        assertState(Light.RED, Mode.STOPPED, 0);
        assertTrue(service.status().emergency());
        assertThrows(TrafficException.class, service::start);
        assertThrows(TrafficException.class, () -> service.manual("YELLOW"));
        service.reset();
        assertFalse(service.status().emergency());
        service.start();
        assertState(Light.RED, Mode.AUTOMATIC, 10);
    }
    @Test void invalidResumePreservesState() {
        TrafficState before = service.status();
        assertThrows(TrafficException.class, service::resume);
        assertEquals(before, service.status());
    }
    @Test void invalidPausePreservesState() {
        TrafficState before = service.status();
        assertThrows(TrafficException.class, service::pause);
        assertEquals(before, service.status());
    }
    @Test void manualSelectionAndInvalidInput() {
        for (Light light : Light.values()) {
            service.manual(light.name()); advance(50);
            assertState(light, Mode.MANUAL, 0);
        }
        TrafficState before = service.status();
        assertThrows(TrafficException.class, () -> service.manual("BLUE"));
        assertEquals(before, service.status());
        service.reset(); assertState(Light.RED, Mode.STOPPED, 0);
    }
    @Test void manualRejectedWhileAutomaticOrPaused() {
        service.start();
        assertThrows(TrafficException.class, () -> service.manual("GREEN"));
        service.pause();
        TrafficState before = service.status();
        assertThrows(TrafficException.class, () -> service.manual("GREEN"));
        assertEquals(before, service.status());
    }
    @Test void stopAndResetClearTiming() {
        service.start(); advance(12); service.stop(); advance(100);
        assertState(Light.RED, Mode.STOPPED, 0);
        service.manual("GREEN"); service.reset();
        assertState(Light.RED, Mode.STOPPED, 0);
    }
    @Test void startAlwaysBeginsNewRedPhase() {
        service.start(); advance(15); service.start();
        assertState(Light.RED, Mode.AUTOMATIC, 10);
    }
    @Test void delayedSchedulerCatchesUpWithoutExtendingPhases() {
        service.start(); advance(21 * 1000 + 11);
        assertState(Light.GREEN, Mode.AUTOMATIC, 7);
    }
    @Test void simultaneousCommandsAndTicksKeepSnapshotsConsistent() throws Exception {
        var pool = Executors.newFixedThreadPool(8);
        try {
            var tasks = new ArrayList<java.util.concurrent.Callable<Void>>();
            for (int i = 0; i < 200; i++) {
                int command = i % 5;
                tasks.add(() -> {
                    switch (command) {
                        case 0 -> service.start();
                        case 1 -> service.stop();
                        case 2 -> service.reset();
                        case 3 -> service.tick();
                        default -> service.status();
                    }
                    TrafficState s = service.status();
                    assertNotNull(s.currentLight());
                    assertTrue(s.remainingSeconds() >= 0);
                    assertEquals(s.mode() == Mode.AUTOMATIC, s.running());
                    if (s.mode() == Mode.STOPPED) {
                        assertEquals(Light.RED, s.currentLight());
                        assertEquals(0, s.remainingSeconds());
                    }
                    return null;
                });
            }
            for (var result : pool.invokeAll(tasks)) result.get(5, TimeUnit.SECONDS);
        } finally { pool.shutdownNow(); }
    }
}
