package ru.yandex.market.antifraud.orders.web.dto.checkouter;

import org.junit.Test;

import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;

import static org.junit.Assert.assertEquals;

public class OrderBuyerDeviceIdRequestDtoTest {

    @Test
    public void deserializationTest() {
        OrderBuyerDeviceIdRequestDto expected = OrderBuyerDeviceIdRequestDto.builder()
                .iosDeviceId("iphone 1337X PRO PLUS")
                .build();
        String json1 = "{\"ios_device_id\":\"iphone 1337X PRO PLUS\"}";
        String json2 = "{\"iosDeviceId\":\"iphone 1337X PRO PLUS\"}";
        assertEquals(expected, AntifraudJsonUtil.fromJson(json1, OrderBuyerDeviceIdRequestDto.class));
        assertEquals(expected, AntifraudJsonUtil.fromJson(json2, OrderBuyerDeviceIdRequestDto.class));
    }
}
