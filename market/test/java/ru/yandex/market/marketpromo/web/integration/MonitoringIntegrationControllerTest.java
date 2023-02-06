package ru.yandex.market.marketpromo.web.integration;

import org.junit.jupiter.api.Test;

import ru.yandex.market.marketpromo.test.WebTestBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


public class MonitoringIntegrationControllerTest extends WebTestBase {

    @Test
    void shouldReturnOkOnPing() {
        assertThat(restTemplate.getForObject("/ping", String.class), is("0;OK"));
    }
}
