package ru.yandex.market.mini.testmjfortank.api;

import java.time.Duration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import ru.yandex.mj.generated.server.api.CheckWithMockApiDelegate;


@Component
public class CheckWithMockApiService implements CheckWithMockApiDelegate {
    private static final String URI = "http://pd2j6iqg7bnxy4oe.sas.yp-c.yandex.net/slow-api";
    private final RestTemplate restTemplate;

    public CheckWithMockApiService() {
        RestTemplateBuilder builder = new RestTemplateBuilder();
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMinutes(1))
                .setReadTimeout(Duration.ofMinutes(1))
                .build();
    }

    @Override
    public ResponseEntity<String> checkWithMockGet() {
        RestTemplateBuilder builder = new RestTemplateBuilder();

        String result = restTemplate.getForObject(URI, String.class);
        return ResponseEntity.ok(result);
    }
}
