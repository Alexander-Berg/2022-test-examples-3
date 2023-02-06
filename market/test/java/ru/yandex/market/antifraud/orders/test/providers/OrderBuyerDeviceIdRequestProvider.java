package ru.yandex.market.antifraud.orders.test.providers;

import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderBuyerDeviceIdRequestDto;

public abstract class OrderBuyerDeviceIdRequestProvider {

    public static OrderBuyerDeviceIdRequestDto getBuyerDefaultDeviceId() {
        return OrderBuyerDeviceIdRequestDto.builder()
                .androidDeviceId("androidDeviceId")
                .googleServiceId("googleServiceId")
                .androidHardwareSerial("androidHardwareSerial")
                .androidBuildModel("androidBuildModel")
                .androidBuildManufacturer("androidBuildManufacturer")
                .iosDeviceId("iosDeviceId")
                .build();
    }
}
