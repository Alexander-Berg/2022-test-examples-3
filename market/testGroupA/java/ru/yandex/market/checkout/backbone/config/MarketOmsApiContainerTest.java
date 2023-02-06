package ru.yandex.market.checkout.backbone.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.checkout.application.AbstractContainerTestBase;

public class MarketOmsApiContainerTest extends AbstractContainerTestBase {
    @Autowired
    RestTemplate marketOmsRestTemplate;

    @Test
    public void restTest() {
        var multiOrder = MarketOmsTestUtils.generateMultiOrderWithAdditionalInfo();

        var response = marketOmsRestTemplate.postForEntity(testRestTemplate.getRootUri() + "/orders/reserve",
                new HttpEntity<>(multiOrder), Object.class);
        // Если 200, то серде прошла успешно
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
