package ru.yandex.market.logistics.lrm.controler;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.lrm.LrmTest;
import ru.yandex.market.logistics.lrm.converter.EnumConverter;
import ru.yandex.market.logistics.lrm.model.entity.enums.LogisticPointType;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnBoxStatus;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnSegmentStatus;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnStatus;
import ru.yandex.market.logistics.lrm.model.entity.enums.ShipmentDestinationType;
import ru.yandex.market.logistics.lrm.model.entity.enums.ShipmentRecipientType;

@DisplayName("Конвертация внутренних enum-ов в/из API")
@ParametersAreNonnullByDefault
class ReturnEnumsTest extends LrmTest {

    private final EnumConverter enumConverter = new EnumConverter();

    @DisplayName("ReturnStatus to API")
    @ParameterizedTest
    @EnumSource(ReturnStatus.class)
    void returnStatusToApi(ReturnStatus type) {
        softly.assertThat(
                enumConverter.convert(type, ru.yandex.market.logistics.lrm.api.model.ReturnStatus.class)
            )
            .isNotNull();
    }

    @DisplayName("ReturnBoxStatus to API")
    @ParameterizedTest
    @EnumSource(ReturnBoxStatus.class)
    void returnBoxStatusToApi(ReturnBoxStatus type) {
        softly.assertThat(
                enumConverter.convert(type, ru.yandex.market.logistics.lrm.api.model.ReturnBoxStatus.class)
            )
            .isNotNull();
    }

    @DisplayName("ReturnSegmentStatus to API")
    @ParameterizedTest
    @EnumSource(ReturnSegmentStatus.class)
    void returnSegmentStatusToApi(ReturnSegmentStatus type) {
        softly.assertThat(
                enumConverter.convert(type, ru.yandex.market.logistics.lrm.api.model.ReturnSegmentStatus.class)
            )
            .isNotNull();
    }

    @DisplayName("ReturnStatus from API")
    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lrm.api.model.ReturnStatus.class)
    void returnStatusFromApi(ru.yandex.market.logistics.lrm.api.model.ReturnStatus type) {
        softly.assertThat(
                enumConverter.convert(type, ReturnStatus.class)
            )
            .isNotNull();
    }

    @DisplayName("ReturnBoxStatus from API")
    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lrm.api.model.ReturnBoxStatus.class)
    void returnBoxStatusFromApi(ru.yandex.market.logistics.lrm.api.model.ReturnBoxStatus type) {
        softly.assertThat(
                enumConverter.convert(type, ReturnBoxStatus.class)
            )
            .isNotNull();
    }

    @DisplayName("ReturnSegmentStatus from API")
    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lrm.api.model.ReturnSegmentStatus.class)
    void returnSegmentStatusFromApi(ru.yandex.market.logistics.lrm.api.model.ReturnSegmentStatus type) {
        softly.assertThat(
                enumConverter.convert(type, ReturnSegmentStatus.class)
            )
            .isNotNull();
    }

    @DisplayName("LogisticPointType to API")
    @ParameterizedTest
    @EnumSource(LogisticPointType.class)
    void logisticPointTypeToApi(LogisticPointType type) {
        softly.assertThat(
                enumConverter.convert(type, ru.yandex.market.logistics.lrm.api.model.LogisticPointType.class)
            )
            .isNotNull();
    }

    @DisplayName("ShipmentDestinationType to API")
    @ParameterizedTest
    @EnumSource(ShipmentDestinationType.class)
    void shipmentDestinationTypeToApi(ShipmentDestinationType type) {
        softly.assertThat(
                enumConverter.convert(type, ru.yandex.market.logistics.lrm.api.model.ShipmentDestinationType.class)
            )
            .isNotNull();
    }

    @DisplayName("ShipmentRecipientType to API")
    @ParameterizedTest
    @EnumSource(ShipmentRecipientType.class)
    void shipmentRecipientTypeToApi(ShipmentRecipientType type) {
        softly.assertThat(
                enumConverter.convert(type, ru.yandex.market.logistics.lrm.api.model.ShipmentRecipientType.class)
            )
            .isNotNull();
    }
}
