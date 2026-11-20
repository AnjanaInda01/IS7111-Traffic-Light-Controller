package lk.ac.is7111.traffic.model;

/** Immutable snapshot: callers cannot mutate the controller's state. */
public record TrafficState(Light currentLight, Mode mode, int remainingSeconds,
                           boolean running, boolean emergency, String message) { }
