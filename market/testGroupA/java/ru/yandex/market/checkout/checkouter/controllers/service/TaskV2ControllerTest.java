package ru.yandex.market.checkout.checkouter.controllers.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

import ru.yandex.market.checkout.application.AbstractContainerTestBase;

public class TaskV2ControllerTest extends AbstractContainerTestBase {
    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    public void converterForTypeExists() {
        var response = testRestTemplate.getForEntity("/tasks/v2/full-info", String.class);
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
