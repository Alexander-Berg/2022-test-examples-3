package ru.yandex.market.logistics.logistics4shops.converter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.logistics4shops.AbstractTest;
import ru.yandex.market.logistics.logistics4shops.model.entity.enums.PossibleOrderChangeMethod;
import ru.yandex.market.logistics.logistics4shops.model.entity.enums.PossibleOrderChangeType;

@DisplayName("Конвертация enum-ов возможных изменений заказа партнёром LMS->L4S")
public class PossibleOrderChangeEnumsConverterTest extends AbstractTest {
    private final EnumConverter enumConverter = new EnumConverter();

    @DisplayName("Конвертация способов внесения изменения LMS->L4S")
    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.management.entity.type.PossibleOrderChangeMethod.class)
    void methodFromExternalToInternal(
        ru.yandex.market.logistics.management.entity.type.PossibleOrderChangeMethod source
    ) {
        softly.assertThat(enumConverter.convert(source, PossibleOrderChangeMethod.class))
            .isNotNull();
    }

    @DisplayName("Конвертация типов изменений LMS->L4S")
    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.management.entity.type.PossibleOrderChangeType.class)
    void typeFromExternalToInternal(ru.yandex.market.logistics.management.entity.type.PossibleOrderChangeType source) {
        softly.assertThat(enumConverter.convert(source, PossibleOrderChangeType.class))
            .isNotNull();
    }
}
