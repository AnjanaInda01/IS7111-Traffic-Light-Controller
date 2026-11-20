package lk.ac.is7111.traffic.controller;

import lk.ac.is7111.traffic.model.TrafficState;
import lk.ac.is7111.traffic.service.TrafficException;
import lk.ac.is7111.traffic.service.TrafficService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TrafficExceptionHandler {
    private final TrafficService service;
    public TrafficExceptionHandler(TrafficService service) { this.service = service; }
    public record ErrorResponse(String message, TrafficState state) { }
    @ExceptionHandler(TrafficException.class)
    public ResponseEntity<ErrorResponse> handle(TrafficException ex) {
        return ResponseEntity.status(ex.status()).body(new ErrorResponse(ex.getMessage(), service.status()));
    }
}
