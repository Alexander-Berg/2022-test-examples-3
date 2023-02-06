package ru.yandex.market.mbi.bot;

import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class IntegrationTestClient {

    private final RestTemplate restTemplate;
    private final String url;

    public IntegrationTestClient(RestTemplate restTemplate, String url) {
        this.restTemplate = restTemplate;
        this.url = url;
    }

    public ResponseEntity<String> ping() {
        String uri = UriComponentsBuilder.fromHttpUrl(url)
                .path("/ping")
                .build()
                .toString();
        return restTemplate.getForEntity(uri, String.class);
    }

    public ResponseEntity<String> update(String updateBody) {
        String uri = UriComponentsBuilder.fromHttpUrl(url)
                .path("/test/update")
                .build()
                .toString();
        return restTemplate.postForEntity(uri, new HttpEntity<>(updateBody), String.class);
    }
}
