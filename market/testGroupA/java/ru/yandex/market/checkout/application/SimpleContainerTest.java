package ru.yandex.market.checkout.application;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class SimpleContainerTest extends AbstractContainerTestBase {
    @Test
    public void jettyShouldStart() {
        ResponseEntity<String> forEntity = testRestTemplate.getForEntity("/ping", String.class);

        Assertions.assertNotEquals(HttpStatus.NOT_FOUND, forEntity.getStatusCode());
    }
}
