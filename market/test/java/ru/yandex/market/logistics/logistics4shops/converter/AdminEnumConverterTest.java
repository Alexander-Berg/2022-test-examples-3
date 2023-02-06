package ru.yandex.market.logistics.logistics4shops.converter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.logistics4shops.AbstractTest;
import ru.yandex.market.logistics.logistics4shops.admin.enums.AdminExcludeOrderFromShipmentRequestStatus;
import ru.yandex.market.logistics.logistics4shops.admin.enums.AdminOrderCheckpointStatus;
import ru.yandex.market.logistics.logistics4shops.admin.enums.AdminPartnerType;
import ru.yandex.market.logistics.logistics4shops.model.entity.enums.ExcludeOrderFromShipmentRequestStatus;
import ru.yandex.market.logistics.logistics4shops.model.entity.enums.OrderCheckpointStatus;
import ru.yandex.market.logistics.logistics4shops.model.enums.PartnerType;

@DisplayName("Конвертация перечислений между внутренним представлением и внешним")
public class AdminEnumConverterTest extends AbstractTest {

    private final EnumConverter enumConverter = new EnumConverter();

    @DisplayName("Конвертация внутреннего представления OrderCheckpointStatus во внешнее")
    @ParameterizedTest
    @EnumSource(OrderCheckpointStatus.class)
    void internalToExternalOrderCheckpointStatus(OrderCheckpointStatus status) {
        softly.assertThat(enumConverter.convert(status, AdminOrderCheckpointStatus.class)).isNotNull();
    }

    @DisplayName("Конвертация внешнего представления AdminOrderCheckpointStatus во внутреннее")
    @ParameterizedTest
    @EnumSource(AdminOrderCheckpointStatus.class)
    void externalToInternalOrderCheckpointStatus(AdminOrderCheckpointStatus status) {
        softly.assertThat(enumConverter.convert(status, OrderCheckpointStatus.class)).isNotNull();
    }

    @DisplayName("Конвертация внутреннего представления ExcludeOrderFromShipmentRequestStatus во внешнее")
    @ParameterizedTest
    @EnumSource(ExcludeOrderFromShipmentRequestStatus.class)
    void internalToExternalExcludeOrderFromShipmentRequestStatus(ExcludeOrderFromShipmentRequestStatus status) {
        softly.assertThat(enumConverter.convert(status, AdminExcludeOrderFromShipmentRequestStatus.class)).isNotNull();
    }

    @DisplayName("Конвертация внутреннего представления AdminExcludeOrderFromShipmentRequestStatus во внешнее")
    @ParameterizedTest
    @EnumSource(AdminExcludeOrderFromShipmentRequestStatus.class)
    void externalToInternalExcludeOrderFromShipmentRequestStatus(AdminExcludeOrderFromShipmentRequestStatus status) {
        softly.assertThat(enumConverter.convert(status, ExcludeOrderFromShipmentRequestStatus.class)).isNotNull();
    }

    @DisplayName("Конвертация внутреннего представления PartnerType во внешнее")
    @ParameterizedTest
    @EnumSource(PartnerType.class)
    void internalToExternalPartnerType(PartnerType type) {
        softly.assertThat(enumConverter.convert(type, AdminPartnerType.class)).isNotNull();
    }

    @DisplayName("Конвертация внешнего представления AdminPartnerType во внутреннее")
    @ParameterizedTest
    @EnumSource(AdminPartnerType.class)
    void externalToInternalPartnerType(AdminPartnerType type) {
        softly.assertThat(enumConverter.convert(type, PartnerType.class)).isNotNull();
    }
}
