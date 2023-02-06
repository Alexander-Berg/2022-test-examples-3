package ru.yandex.market.antifraud.orders.test.providers;

import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderBuyerDeviceIdRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderBuyerRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderBuyerUserDeviceRequestDto;

public abstract class OrderBuyerRequestProvider {

    private static final long UID = 359953025L;

    public static OrderBuyerRequestDto getOrderBuyerRequest() {
        return getDefaultBuyerWithCustomId(UID);
    }

    public static OrderBuyerRequestDto getDefaultBuyerWithCustomId(long uid) {
        OrderBuyerDeviceIdRequestDto deviceId = OrderBuyerDeviceIdRequestProvider.getBuyerDefaultDeviceId();
        OrderBuyerUserDeviceRequestDto userDevice = new OrderBuyerUserDeviceRequestDto(deviceId, false);
        return OrderBuyerRequestDto.builder()
            .uid(uid)
            .email("a@b.com")
            .normalizedPhone("71234567891")
            .uuid("100500")
            .yandexuid("6238887791581327997")
            .userDevice(userDevice)
            .ip("127.0.0.1")
            .assessor(false)
            .yandexEmployee(false)
            .build();
    }
}
