package lk.ac.is7111.traffic.model;

public enum Light {
    RED(10), GREEN(8), YELLOW(3);

    private final int durationSeconds;
    Light(int durationSeconds) { this.durationSeconds = durationSeconds; }
    public int durationSeconds() { return durationSeconds; }
    public Light next() {
        return switch (this) { case RED -> GREEN; case GREEN -> YELLOW; case YELLOW -> RED; };
    }
}
