package ru.yandex.market.antifraud.orders.test.providers;


import java.util.Collections;

import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderResponseDto;

public class OrderResponseDtoProvider {

    public static OrderResponseDto getEmptyOrderResponse() {
        return new OrderResponseDto(Collections.emptyList());
    }
}
