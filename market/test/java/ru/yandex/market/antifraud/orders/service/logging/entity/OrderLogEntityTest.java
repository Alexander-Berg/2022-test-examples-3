package ru.yandex.market.antifraud.orders.service.logging.entity;

import org.junit.Test;

import ru.yandex.market.antifraud.orders.entity.OrderVerdict;
import ru.yandex.market.antifraud.orders.entity.checkouter.OrderLogEntity;
import ru.yandex.market.antifraud.orders.test.providers.OrderRequestProvider;
import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderRequestDto;
import ru.yandex.misc.env.EnvironmentType;

import static org.junit.Assert.assertEquals;

public class OrderLogEntityTest {

    @Test
    public void serializationTest() {
        var orderRequest = OrderRequestProvider.getOrderRequest();
        OrderVerdict verdict = OrderVerdict.EMPTY;
        OrderLogEntity logEntity =
                new OrderLogEntity("req-id", orderRequest, verdict, EnvironmentType.TESTING.toString());
        String json = AntifraudJsonUtil.toJson(logEntity);
        OrderLogEntity parsed = AntifraudJsonUtil.fromJson(json, OrderLogEntity.class);
        assertEquals(logEntity, parsed);
    }

}
