package ru.yandex.market.antifraud.orders.logbroker.entities;

import org.junit.Test;

import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dzvyagin
 */
public class CancelOrderRequestTest {


    @Test
    public void deserializationTest() {
        String json = "{\"name\":\"name\",\"entity\":\"entity\",\"key\":\"key\",\"timestamp\":123,\"rule_name\":\"ruleName\"}\n";
        CancelOrderRequest request = AntifraudJsonUtil.fromJson(json, CancelOrderRequest.class);
        assertThat(request).isEqualTo(CancelOrderRequest.builder()
                .name("name")
                .ruleName("ruleName")
                .entity("entity")
                .key("key")
                .timestamp(123L)
                .build());
    }
}
