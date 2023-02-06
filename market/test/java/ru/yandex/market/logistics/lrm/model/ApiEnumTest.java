package ru.yandex.market.logistics.lrm.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.lrm.LrmTest;
import ru.yandex.market.logistics.lrm.api.model.UnitCountType;
import ru.yandex.market.logistics.lrm.converter.EnumConverter;
import ru.yandex.market.logistics.lrm.model.entity.enums.LogisticPointType;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnBoxStatus;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnReasonType;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnSegmentStatus;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnSource;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnStatus;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnSubreason;
import ru.yandex.market.logistics.lrm.model.entity.enums.ShipmentDestinationType;
import ru.yandex.market.logistics.lrm.model.entity.enums.ShipmentRecipientType;
import ru.yandex.market.logistics.lrm.service.meta.model.FulfilmentReceivedBoxMeta;

@DisplayName("Конвертация enum-ов из OpenAPI и обратно")
class ApiEnumTest extends LrmTest {

    private final EnumConverter enumConverter = new EnumConverter();

    @DisplayName("ReturnSource <- API")
    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lrm.api.model.ReturnSource.class)
    void returnSourceFrom(ru.yandex.market.logistics.lrm.api.model.ReturnSource value) {
        softly.assertThat(enumConverter.convert(value, ReturnSource.class))
            .isNotNull();
    }

    @DisplayName("ReturnSource -> API")
    @ParameterizedTest
    @EnumSource(ReturnSource.class)
    void returnSourceTo(ReturnSource value) {
        softly.assertThat(enumConverter.convert(value, ru.yandex.market.logistics.lrm.api.model.ReturnSource.class))
            .isNotNull();
    }

    @DisplayName("ReturnReasonType <- API")
    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lrm.api.model.ReturnReasonType.class)
    void returnReasonTypeFrom(ru.yandex.market.logistics.lrm.api.model.ReturnReasonType value) {
        softly.assertThat(enumConverter.convert(value, ReturnReasonType.class))
            .isNotNull();
    }

    @DisplayName("ReturnReasonType -> API")
    @ParameterizedTest
    @EnumSource(ReturnReasonType.class)
    void returnReasonTypeTo(ReturnReasonType value) {
        softly.assertThat(enumConverter.convert(value, ru.yandex.market.logistics.lrm.api.model.ReturnReasonType.class))
            .isNotNull();
    }

    @DisplayName("ReturnSubreason <- API")
    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lrm.api.model.ReturnSubreason.class)
    void returnSubreasonFrom(ru.yandex.market.logistics.lrm.api.model.ReturnSubreason value) {
        softly.assertThat(enumConverter.convert(value, ReturnSubreason.class))
            .isNotNull();
    }

    @DisplayName("ReturnSubreason -> API")
    @ParameterizedTest
    @EnumSource(ReturnSubreason.class)
    void returnSubreasonTo(ReturnSubreason value) {
        softly.assertThat(enumConverter.convert(value, ru.yandex.market.logistics.lrm.api.model.ReturnSubreason.class))
            .isNotNull();
    }

    @DisplayName("ReturnSegmentStatus -> API")
    @ParameterizedTest
    @EnumSource(ReturnSegmentStatus.class)
    void returnSegmentStatusTo(ReturnSegmentStatus value) {
        softly.assertThat(enumConverter.convert(
                value,
                ru.yandex.market.logistics.lrm.api.model.ReturnSegmentStatus.class
            ))
            .isNotNull();
    }

    @DisplayName("ReturnBoxStatus -> API")
    @ParameterizedTest
    @EnumSource(ReturnBoxStatus.class)
    void returnBoxStatusTo(ReturnBoxStatus value) {
        softly.assertThat(enumConverter.convert(value, ru.yandex.market.logistics.lrm.api.model.ReturnBoxStatus.class))
            .isNotNull();
    }

    @DisplayName("ReturnStatus -> API")
    @ParameterizedTest
    @EnumSource(ReturnStatus.class)
    void returnStatusTo(ReturnStatus value) {
        softly.assertThat(enumConverter.convert(value, ru.yandex.market.logistics.lrm.api.model.ReturnStatus.class))
            .isNotNull();
    }

    @DisplayName("LogisticPointType -> API")
    @ParameterizedTest
    @EnumSource(LogisticPointType.class)
    void logisticPointTypeTo(LogisticPointType value) {
        softly.assertThat(enumConverter.convert(
                value,
                ru.yandex.market.logistics.lrm.api.model.LogisticPointType.class
            ))
            .isNotNull();
    }

    @DisplayName("ShipmentDestinationType -> API")
    @ParameterizedTest
    @EnumSource(ShipmentDestinationType.class)
    void shipmentDestinationTypeTo(ShipmentDestinationType value) {
        softly.assertThat(enumConverter.convert(
                value,
                ru.yandex.market.logistics.lrm.api.model.ShipmentDestinationType.class
            ))
            .isNotNull();
    }

    @DisplayName("ShipmentRecipientType -> API")
    @ParameterizedTest
    @EnumSource(ShipmentRecipientType.class)
    void shipmentRecipientTypeTo(ShipmentRecipientType value) {
        softly.assertThat(enumConverter.convert(
                value,
                ru.yandex.market.logistics.lrm.api.model.ShipmentRecipientType.class
            ))
            .isNotNull();
    }

    @DisplayName("UnitCountType -> API")
    @ParameterizedTest
    @EnumSource(FulfilmentReceivedBoxMeta.UnitCountType.class)
    void shipmentRecipientTypeTo(FulfilmentReceivedBoxMeta.UnitCountType value) {
        softly.assertThat(enumConverter.convert(value, UnitCountType.class))
            .isNotNull();
    }

}
