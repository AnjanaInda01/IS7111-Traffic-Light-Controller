package lk.ac.is7111.client.model;

/** Client-side representation of the REST contract, with no transition logic. */
public record TrafficState(Light currentLight, Mode mode, int remainingSeconds,
                           boolean running, boolean emergency, String message) {
    public enum Light { RED, YELLOW, GREEN }
    public enum Mode { STOPPED, AUTOMATIC, PAUSED, MANUAL, EMERGENCY }
}
