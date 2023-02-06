package ru.yandex.market.logistics.logistics4shops.converter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.logistics4shops.AbstractTest;
import ru.yandex.market.logistics.logistics4shops.model.enums.ExclusionFromShipmentRestrictedReason;
import ru.yandex.market.logistics.logistics4shops.model.enums.OrderExclusionRestrictedReason;

@DisplayName("Тесты на конвертацию причин невозможности исключений товаров из отгрузки")
class ExclusionFromShipmentRestrictedReasonConverterTest extends AbstractTest {
    private final EnumConverter enumConverter = new EnumConverter();

    @DisplayName("Конвертация внутреннего представления ExclusionFromShipmentRestrictedReason во внешнее")
    @ParameterizedTest
    @EnumSource(ExclusionFromShipmentRestrictedReason.class)
    void internalToExternal(ExclusionFromShipmentRestrictedReason reason) {
        softly.assertThat(enumConverter.convert(
            reason,
            ru.yandex.market.logistics.logistics4shops.api.model.ExclusionFromShipmentRestrictedReason.class
        ))
            .isNotNull();
    }

    @DisplayName("Конвертация внешнего представления ExclusionFromShipmentRestrictedReason во внутреннее")
    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.logistics4shops.api.model.ExclusionFromShipmentRestrictedReason.class)
    void externalToInternal(
        ru.yandex.market.logistics.logistics4shops.api.model.ExclusionFromShipmentRestrictedReason reason
    ) {
        softly.assertThat(enumConverter.convert(
            reason,
            ExclusionFromShipmentRestrictedReason.class
        ))
            .isNotNull();
    }

    @DisplayName("Конвертация внутреннего представления OrderExclusionRestrictedReason во внешнее")
    @ParameterizedTest
    @EnumSource(OrderExclusionRestrictedReason.class)
    void internalToExternal(OrderExclusionRestrictedReason reason) {
        softly.assertThat(enumConverter.convert(
            reason,
            ru.yandex.market.logistics.logistics4shops.api.model.OrderExclusionRestrictedReason.class
        ))
            .isNotNull();
    }

    @DisplayName("Конвертация внешнего представления OrderExclusionRestrictedReason во внутреннее")
    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.logistics4shops.api.model.OrderExclusionRestrictedReason.class)
    void externalToInternal(
        ru.yandex.market.logistics.logistics4shops.api.model.OrderExclusionRestrictedReason reason
    ) {
        softly.assertThat(enumConverter.convert(
            reason,
            OrderExclusionRestrictedReason.class
        ))
            .isNotNull();
    }
}
