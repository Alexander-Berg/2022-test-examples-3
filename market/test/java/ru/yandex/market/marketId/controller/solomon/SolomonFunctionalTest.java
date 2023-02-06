package ru.yandex.market.marketId.controller.solomon;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.common.test.spring.RestTemplateFactory;
import ru.yandex.market.marketId.FunctionalTest;

public class SolomonFunctionalTest extends FunctionalTest {

    private final static RestTemplate REST_TEMPLATE = RestTemplateFactory.createRestTemplate();

    private ResponseEntity<byte[]> solomonJvmRequest(HttpHeaders headers) {
        return REST_TEMPLATE.exchange(baseUrl() + "/solomon-jvm",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                byte[].class);

    }

    @Test
    @DisplayName("Проверка, что /solomon-jvm отвечает корректно")
    void solomonJvmTest() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        ResponseEntity<byte[]> result = solomonJvmRequest(headers);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
    }
}
