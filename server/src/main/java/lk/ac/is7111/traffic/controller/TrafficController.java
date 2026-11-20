package lk.ac.is7111.traffic.controller;

import lk.ac.is7111.traffic.model.TrafficState;
import lk.ac.is7111.traffic.service.TrafficService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/traffic")
public class TrafficController {
    private final TrafficService service;
    public TrafficController(TrafficService service) { this.service = service; }
    @GetMapping("/status") public TrafficState status() { return service.status(); }
    @PostMapping("/start") public TrafficState start() { return service.start(); }
    @PostMapping("/pause") public TrafficState pause() { return service.pause(); }
    @PostMapping("/resume") public TrafficState resume() { return service.resume(); }
    @PostMapping("/stop") public TrafficState stop() { return service.stop(); }
    @PostMapping("/reset") public TrafficState reset() { return service.reset(); }
    @PostMapping("/emergency") public TrafficState emergency() { return service.emergency(); }
    @PostMapping("/manual/{light}") public TrafficState manual(@PathVariable String light) { return service.manual(light); }
}
