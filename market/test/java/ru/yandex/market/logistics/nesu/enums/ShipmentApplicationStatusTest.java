package ru.yandex.market.logistics.nesu.enums;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.model.enums.ShipmentApplicationStatus;
import ru.yandex.market.logistics.nesu.AbstractTest;
import ru.yandex.market.logistics.nesu.api.converter.EnumConverter;

@DisplayName("Конвертация статусов заявки из LOM в статусы заявки NESU")
class ShipmentApplicationStatusTest extends AbstractTest {

    private final EnumConverter enumConverter = new EnumConverter();

    @Test
    @DisplayName("Списки статусов одинаковой длины")
    void statusEnumsEqualSize() {
        softly.assertThat(ShipmentApplicationStatus.values().length)
            .isEqualTo(ShipmentApplicationStatus.values().length);
    }

    @DisplayName("Конвертация статусов ShipmentApplicationStatus")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideEnumArgs")
    void convertShipmentApplicationStatus(
        ShipmentApplicationStatus lomStatus
    ) {
        softly.assertThat(enumConverter.toEnum(
            lomStatus,
            ru.yandex.market.logistics.nesu.api.model.enums.ShipmentApplicationStatus.class)
        ).isNotNull();
    }

    private static Stream<Arguments> provideEnumArgs() {
        return Arrays.stream(ShipmentApplicationStatus.values()).map(Arguments::of);
    }
}
