package ru.yandex.market.mdm.integration.test.http;

import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 * @author s-ermakov
 */
public class PingTest extends BaseHttpIntegrationTestClass {

    @Test
    public void testPing() {
        String response = restTemplate.getForObject("/ping", String.class);
        // ответы от балансера и по локалхосту отличаются
        // поэтому приводим все к нижнему регистру
        response = response.toLowerCase();
        Assertions.assertThat(response).startsWith("0;ok");
    }
}
