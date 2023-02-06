package ru.yandex.market.antifraud.orders.entity.checkouter;

import java.time.Instant;
import java.util.Set;

import org.junit.Test;

import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author dzvyagin
 */
public class OrderOfflineLogEntityTest {

    @Test
    public void serializationTest(){
        OrderOfflineLogEntity logEntity = new OrderOfflineLogEntity(
                123L,
                Set.of("rule"),
                "200",
                "DELIVERED",
                "DELIVERED",
                "2020-02-11T00:00:00",
                "multi-order-id",
                "124",
                Set.of(123L),
                1602082782237L,
                Instant.ofEpochMilli(1602082782237L),
                "PRODUCTION"
        );
        String expected = "{\"order_id\":123,\"reason\":[\"rule\"],\"response_status\":\"200\",\"order_status\":\"DELIVERED\",\"order_substatus\":\"DELIVERED\",\"creation_date\":\"2020-02-11T00:00:00\",\"multi_order_id\":\"multi-order-id\",\"buyer_uid\":\"124\",\"orders\":[123],\"entry_timestamp_long\":1602082782237,\"entry_timestamp\":\"2020-10-07T14:59:42.237Z\",\"environment\":\"PRODUCTION\"}";
        assertThat(AntifraudJsonUtil.toJson(logEntity)).isEqualTo(expected);
    }

}
