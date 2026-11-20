package lk.ac.is7111.client.api;

import java.io.IOException;
import lk.ac.is7111.client.model.TrafficState;

public class ApiException extends IOException {
    private final int statusCode;
    private final TrafficState state;
    public ApiException(int statusCode, String message, TrafficState state) {
        super(message);
        this.statusCode = statusCode;
        this.state = state;
    }
    public int statusCode() { return statusCode; }
    public TrafficState state() { return state; }
}
