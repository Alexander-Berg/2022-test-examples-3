package ru.yandex.market.logistics.management.client.util;

import java.math.BigDecimal;

import javax.annotation.Nonnull;

import lombok.experimental.UtilityClass;

import ru.yandex.market.logistics.management.entity.response.core.Address;

@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
public class DtoFactory {
    @Nonnull
    public static Address getAddressDto() {
        return Address.newBuilder()
            .locationId(12345)
            .settlement("Москва")
            .postCode("555666")
            .latitude(new BigDecimal("100"))
            .longitude(new BigDecimal("200"))
            .street("Октябрьская")
            .house("5")
            .housing("3")
            .building("2")
            .apartment("1")
            .comment("comment")
            .region("region")
            .subRegion("subRegion")
            .addressString("Строка адреса")
            .shortAddressString("Строка адреса")
            .build();
    }
}
