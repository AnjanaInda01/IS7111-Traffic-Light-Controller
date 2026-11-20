package lk.ac.is7111.traffic.service;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import lk.ac.is7111.traffic.model.Light;
import lk.ac.is7111.traffic.model.Mode;
import lk.ac.is7111.traffic.model.TrafficState;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** All controller state lives here, in memory, protected by this object's monitor. */
@Service
public class TrafficService {
    private static final long SECOND = TimeUnit.SECONDS.toNanos(1);
    private final LongSupplier clock;
    private Light light = Light.RED;
    private Mode mode = Mode.STOPPED;
    private boolean emergency;
    private long deadline;
    private long pausedNanos;

    public TrafficService() { this(System::nanoTime); }
    // A monotonic clock can be injected by deterministic tests without waiting 21 seconds.
    TrafficService(LongSupplier clock) { this.clock = clock; }

    /** One application-wide scheduler, never a new timer per button click. */
    @Scheduled(fixedRate = 1000)
    public synchronized void tick() { advance(clock.getAsLong()); }

    public synchronized TrafficState status() { return snapshot(); }

    public synchronized TrafficState start() {
        requireResetNotNeeded();
        light = Light.RED;
        mode = Mode.AUTOMATIC;
        deadline = clock.getAsLong() + Light.RED.durationSeconds() * SECOND;
        pausedNanos = 0;
        return snapshot();
    }

    public synchronized TrafficState pause() {
        if (mode != Mode.AUTOMATIC) throw conflict("Pause requires an active automatic cycle.");
        long now = clock.getAsLong();
        advance(now);
        pausedNanos = deadline - now;
        mode = Mode.PAUSED;
        return snapshot();
    }

    public synchronized TrafficState resume() {
        requireResetNotNeeded();
        if (mode != Mode.PAUSED) throw conflict("Resume requires a paused automatic cycle.");
        deadline = clock.getAsLong() + pausedNanos;
        pausedNanos = 0;
        mode = Mode.AUTOMATIC;
        return snapshot();
    }

    public synchronized TrafficState stop() {
        // Stop always yields RED/STOPPED/0, but cannot bypass the emergency latch.
        clearTiming(Mode.STOPPED);
        return snapshot();
    }

    public synchronized TrafficState reset() {
        emergency = false;
        clearTiming(Mode.STOPPED);
        return snapshot();
    }

    public synchronized TrafficState emergency() {
        emergency = true;
        clearTiming(Mode.EMERGENCY);
        return snapshot();
    }

    public synchronized TrafficState manual(String value) {
        final Light selected;
        try { selected = Light.valueOf(value.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException | NullPointerException ex) {
            throw new TrafficException(HttpStatus.BAD_REQUEST, "Invalid light. Use RED, YELLOW or GREEN.");
        }
        requireResetNotNeeded();
        if (mode != Mode.STOPPED && mode != Mode.MANUAL) {
            throw conflict("Stop the automatic cycle before choosing a manual light.");
        }
        clearTiming(Mode.MANUAL);
        light = selected;
        return snapshot();
    }

    private void requireResetNotNeeded() {
        if (emergency) throw conflict("Emergency is locked. Press Reset before operating the signal.");
    }

    private TrafficException conflict(String message) {
        return new TrafficException(HttpStatus.CONFLICT, message);
    }

    private void clearTiming(Mode nextMode) {
        light = Light.RED;
        mode = nextMode;
        deadline = 0;
        pausedNanos = 0;
    }

    private void advance(long now) {
        if (mode != Mode.AUTOMATIC) return;
        // Skip complete elapsed cycles after a long scheduler stall, then catch up phases.
        long cycle = 21 * SECOND;
        if (now - deadline >= cycle) deadline += ((now - deadline) / cycle) * cycle;
        while (now - deadline >= 0) {
            light = light.next();
            deadline += light.durationSeconds() * SECOND;
        }
    }

    private TrafficState snapshot() {
        long now = clock.getAsLong();
        advance(now);
        long nanos = mode == Mode.AUTOMATIC ? deadline - now : mode == Mode.PAUSED ? pausedNanos : 0;
        int seconds = (int) ((nanos + SECOND - 1) / SECOND);
        String message = emergency ? "Emergency locked - Reset required" : switch (mode) {
            case AUTOMATIC -> "Normal cycle";
            case PAUSED -> "Cycle paused";
            case MANUAL -> "Manual light selected";
            default -> "Ready to start";
        };
        return new TrafficState(light, mode, seconds, mode == Mode.AUTOMATIC, emergency, message);
    }
}
