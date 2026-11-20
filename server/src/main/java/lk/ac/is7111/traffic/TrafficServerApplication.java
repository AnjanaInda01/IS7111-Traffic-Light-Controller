package lk.ac.is7111.traffic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TrafficServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(TrafficServerApplication.class, args);
    }
}
