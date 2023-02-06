package ru.yandex.market.delivery.mdbapp.integration.converter;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.ReturnDeliveryStatus;

import static org.assertj.core.api.Java6Assertions.assertThat;

class EnumConverterTest {

    private final EnumConverter enumConverter = new EnumConverter();

    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(ReturnDeliveryStatus.class)
    public void convertReturnDeliveryStatus(ReturnDeliveryStatus returnDeliveryStatus) {
        assertThat(
            enumConverter.convert(
                returnDeliveryStatus,
                ru.yandex.market.checkout.checkouter.returns.ReturnDeliveryStatus.class
            )
        )
            .isNotNull();
    }

}
