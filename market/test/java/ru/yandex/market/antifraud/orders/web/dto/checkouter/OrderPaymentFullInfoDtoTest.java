package ru.yandex.market.antifraud.orders.web.dto.checkouter;

import org.junit.Test;

import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 21.01.2020
 */
public class OrderPaymentFullInfoDtoTest {
    @Test
    public void deserializationTest() {
        OrderPaymentFullInfoDto orderPaymentFullInfoDto1 = new OrderPaymentFullInfoDto(OrderPaymentType.PREPAID);
        OrderPaymentFullInfoDto orderPaymentFullInfoDto2 = new OrderPaymentFullInfoDto(OrderPaymentType.UNKNOWN);

        String json1 = "{\"orderPaymentType\":\"PREPAID\"}";
        String json2 = "{\"orderPaymentType\":\"null\"}";
        assertEquals(orderPaymentFullInfoDto1, AntifraudJsonUtil.fromJson(json1, OrderPaymentFullInfoDto.class));
        assertEquals(orderPaymentFullInfoDto2, AntifraudJsonUtil.fromJson(json2, OrderPaymentFullInfoDto.class));
    }
}
