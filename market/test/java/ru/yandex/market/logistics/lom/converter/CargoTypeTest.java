package ru.yandex.market.logistics.lom.converter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.model.enums.CargoType;

@DisplayName("Конвертация карго-типов")
public class CargoTypeTest extends AbstractTest {

    private final EnumConverter enumConverter = new EnumConverter();

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistic.api.model.delivery.CargoType.class)
    @DisplayName("DS API")
    void delivery(ru.yandex.market.logistic.api.model.delivery.CargoType cargoType) {
        softly.assertThat(enumConverter.convert(cargoType, CargoType.class))
            .isNotNull()
            .extracting(CargoType::getCode)
            .isEqualTo(cargoType.getCode());
    }

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistic.api.model.fulfillment.CargoType.class)
    @DisplayName("FF API")
    void fulfillment(ru.yandex.market.logistic.api.model.fulfillment.CargoType cargoType) {
        softly.assertThat(enumConverter.convert(cargoType, CargoType.class))
            .isNotNull()
            .extracting(CargoType::getCode)
            .isEqualTo(cargoType.getCode());
    }

}
