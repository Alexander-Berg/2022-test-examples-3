package ru.yandex.market.logistics.lrm.les;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.les.dto.CarrierType;
import ru.yandex.market.logistics.lrm.LrmTest;
import ru.yandex.market.logistics.lrm.converter.EnumConverter;
import ru.yandex.market.logistics.lrm.model.entity.enums.ShipmentRecipientType;

@DisplayName("Конвертация CarrierType из LES")
class CarrierTypeLesConverterTest extends LrmTest {
    private final EnumConverter enumConverter = new EnumConverter();

    @DisplayName("В ShipmentRecipientType из LRM")
    @ParameterizedTest
    @EnumSource(CarrierType.class)
    void toShipmentRecipientType(CarrierType carrierType) {
        softly.assertThatCode(() -> enumConverter.convert(carrierType, ShipmentRecipientType.class))
            .doesNotThrowAnyException();
    }
}
