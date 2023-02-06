package ru.yandex.market.mini.testmjfortank.api;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import ru.yandex.mj.generated.server.api.CheckWithCpuLoadApiDelegate;


@Component
public class CheckWithCpuLoadApiService implements CheckWithCpuLoadApiDelegate {

    private static final Logger logger = LoggerFactory.getLogger(CheckWithCpuLoadApiService.class);
    private static final long TIMEOUT = 40;
    private final Random rng = new Random();

    @Override
    public ResponseEntity<String> checkWithCpuLoadGet() {
        makeLoad();
        return ResponseEntity.ok("All calculations have done");
    }

     private void makeLoad() {
        Instant startTime = Instant.now();
        boolean isStopped = false;
        double store = 1;
        while (!isStopped) {
            double r = rng.nextDouble();
            double v = Math.sin(Math.cos(Math.sin(Math.cos(r))));
            Duration elapsed = Duration.between(startTime, Instant.now());
            isStopped = elapsed.toMillis() >= TIMEOUT;
        }
     }
}
