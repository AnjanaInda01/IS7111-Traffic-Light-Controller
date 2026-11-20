package lk.ac.is7111.traffic.service;

import org.springframework.http.HttpStatus;

public class TrafficException extends RuntimeException {
    private final HttpStatus status;
    public TrafficException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
    public HttpStatus status() { return status; }
}
