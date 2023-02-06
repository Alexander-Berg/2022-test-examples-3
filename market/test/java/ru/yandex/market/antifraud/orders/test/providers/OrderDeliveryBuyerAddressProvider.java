package ru.yandex.market.antifraud.orders.test.providers;

import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderDeliveryAddressLanguage;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderDeliveryAddressRecipientPersonRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderDeliveryAddressRequestDto;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 16.06.2020
 */
public class OrderDeliveryBuyerAddressProvider {
    public static OrderDeliveryAddressRequestDto getEmptyOrderDeliveryBuyerAddress() {
        return OrderDeliveryAddressRequestDto.builder().build();
    }

    public static OrderDeliveryAddressRequestDto getOrderDeliveryBuyerAddress() {
        return new OrderDeliveryAddressRequestDto("Россия", "123000", "Москва", null,
                "Льва Толстого", null, "16", null, null, null, "1",
                "123", "4", null, "30.70079620061024,61.71145050985435",
                null, null,
                "Заметка", "+7 999 123456", null, null,
                OrderDeliveryAddressLanguage.RUS, 213L,
                new OrderDeliveryAddressRecipientPersonRequestDto("Иван", null, "Иванов"),
                null, null, null);
    }
}
