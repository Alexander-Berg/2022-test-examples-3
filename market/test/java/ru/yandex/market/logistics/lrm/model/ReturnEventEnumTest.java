package ru.yandex.market.logistics.lrm.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.lrm.LrmTest;
import ru.yandex.market.logistics.lrm.converter.EnumConverter;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnBoxStatus;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnEventType;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnReasonType;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnSegmentStatus;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnSource;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnStatus;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnSubreason;
import ru.yandex.market.logistics.lrm.model.entity.enums.ShipmentDestinationType;
import ru.yandex.market.logistics.lrm.model.entity.enums.ShipmentRecipientType;

@DisplayName("Конвертация enum-ов из БД в событие")
class ReturnEventEnumTest extends LrmTest {

    private final EnumConverter enumConverter = new EnumConverter();

    @ParameterizedTest
    @DisplayName("ReturnSource")
    @EnumSource(ReturnSource.class)
    void returnSource(ReturnSource returnSource) {
        softly.assertThat(enumConverter.convert(
            returnSource,
            ru.yandex.market.logistics.lrm.event_model.payload.ReturnSource.class
        )).isNotNull();
    }

    @ParameterizedTest
    @DisplayName("ReturnEventType")
    @EnumSource(ReturnEventType.class)
    void returnEventType(ReturnEventType returnEventType) {
        softly.assertThat(enumConverter.convert(
            returnEventType,
            ru.yandex.market.logistics.lrm.event_model.ReturnEventType.class
        )).isNotNull();
    }

    @ParameterizedTest
    @DisplayName("ReturnReasonType")
    @EnumSource(ReturnReasonType.class)
    void returnReasonType(ReturnReasonType returnReasonType) {
        softly.assertThat(enumConverter.convert(
            returnReasonType,
            ru.yandex.market.logistics.lrm.event_model.payload.ReturnReasonType.class
        )).isNotNull();
    }

    @ParameterizedTest
    @DisplayName("ReturnSubreason")
    @EnumSource(ReturnSubreason.class)
    void returnSubreason(ReturnSubreason returnSubreason) {
        softly.assertThat(enumConverter.convert(
            returnSubreason,
            ru.yandex.market.logistics.lrm.event_model.payload.ReturnSubreason.class
        )).isNotNull();
    }

    @ParameterizedTest
    @DisplayName("ReturnBoxStatus")
    @EnumSource(ReturnBoxStatus.class)
    void returnBoxStatus(ReturnBoxStatus returnBoxStatus) {
        softly.assertThat(enumConverter.convert(
            returnBoxStatus,
            ru.yandex.market.logistics.lrm.event_model.payload.ReturnBoxStatus.class
        )).isNotNull();
    }

    @ParameterizedTest
    @DisplayName("ReturnStatus")
    @EnumSource(ReturnStatus.class)
    void returnStatus(ReturnStatus returnStatus) {
        softly.assertThat(enumConverter.convert(
            returnStatus,
            ru.yandex.market.logistics.lrm.event_model.payload.ReturnStatus.class
        )).isNotNull();
    }

    @ParameterizedTest
    @DisplayName("ReturnSegmentStatus")
    @EnumSource(ReturnSegmentStatus.class)
    void returnSegmentStatus(ReturnSegmentStatus returnSegmentStatus) {
        softly.assertThat(enumConverter.convert(
            returnSegmentStatus,
            ru.yandex.market.logistics.lrm.event_model.payload.enums.ReturnSegmentStatus.class
        )).isNotNull();
    }

    @ParameterizedTest
    @DisplayName("ShipmentDestinationType")
    @EnumSource(ShipmentDestinationType.class)
    void shipmentDestinationType(ShipmentDestinationType shipmentDestinationType) {
        softly.assertThat(enumConverter.convert(
            shipmentDestinationType,
            ru.yandex.market.logistics.lrm.event_model.payload.enums.ShipmentDestinationType.class
        )).isNotNull();
    }

    @ParameterizedTest
    @DisplayName("ShipmentRecipientType")
    @EnumSource(ShipmentRecipientType.class)
    void shipmentRecipientType(ShipmentRecipientType shipmentRecipientType) {
        softly.assertThat(enumConverter.convert(
            shipmentRecipientType,
            ru.yandex.market.logistics.lrm.event_model.payload.enums.ShipmentRecipientType.class
        )).isNotNull();
    }

}
