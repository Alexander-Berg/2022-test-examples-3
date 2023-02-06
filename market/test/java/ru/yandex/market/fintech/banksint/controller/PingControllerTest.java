package ru.yandex.market.fintech.banksint.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.fintech.banksint.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PingControllerTest extends FunctionalTest {

    @Test
    public void pingTest() {
        ResponseEntity<String> response = testRestTemplate.getForEntity("/ping", String.class);
        var expectedResult = "0;OK";
        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals(expectedResult, response.getBody());
    }
}
