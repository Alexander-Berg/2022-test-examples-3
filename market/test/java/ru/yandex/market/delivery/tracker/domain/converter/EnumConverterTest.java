package ru.yandex.market.delivery.tracker.domain.converter;

import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.delivery.tracker.domain.admin.AdminUnknownCheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.enums.EntityType;
import ru.yandex.market.delivery.tracker.domain.enums.InboundDeliveryCheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.enums.InboundOldDeliveryCheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.enums.MovementDeliveryCheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.enums.OutboundDeliveryCheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.enums.OutboundOldDeliveryCheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.enums.TransferDeliveryCheckpointStatus;

import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;

class EnumConverterTest {

    @RegisterExtension
    final JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();

    @ParameterizedTest
    @EnumSource(value = OrderDeliveryCheckpointStatus.class, mode = EXCLUDE, names = "UNKNOWN")
    void orderToAdminEnum(OrderDeliveryCheckpointStatus value) {
        softly.assertThat(EnumConverter.convertToAdminUnknownCheckpointStatus(value, EntityType.ORDER))
            .isNotNull()
            .isNotEqualTo(AdminUnknownCheckpointStatus.UNKNOWN);
    }

    @ParameterizedTest
    @EnumSource(value = MovementDeliveryCheckpointStatus.class, mode = EXCLUDE, names = "UNKNOWN")
    void movementToAdminEnum(MovementDeliveryCheckpointStatus value) {
        softly.assertThat(EnumConverter.convertToAdminUnknownCheckpointStatus(value, EntityType.MOVEMENT))
            .isNotNull()
            .isNotEqualTo(AdminUnknownCheckpointStatus.UNKNOWN);
    }

    @ParameterizedTest
    @EnumSource(value = InboundDeliveryCheckpointStatus.class, mode = EXCLUDE, names = "UNKNOWN")
    void inboundToAdminEnum(InboundDeliveryCheckpointStatus value) {
        softly.assertThat(EnumConverter.convertToAdminUnknownCheckpointStatus(value, EntityType.INBOUND))
            .isNotNull()
            .isNotEqualTo(AdminUnknownCheckpointStatus.UNKNOWN);
    }

    @ParameterizedTest
    @EnumSource(value = OutboundDeliveryCheckpointStatus.class, mode = EXCLUDE, names = "UNKNOWN")
    void outboundToAdminEnum(OutboundDeliveryCheckpointStatus value) {
        softly.assertThat(EnumConverter.convertToAdminUnknownCheckpointStatus(value, EntityType.OUTBOUND))
            .isNotNull()
            .isNotEqualTo(AdminUnknownCheckpointStatus.UNKNOWN);
    }

    @ParameterizedTest
    @EnumSource(value = TransferDeliveryCheckpointStatus.class, mode = EXCLUDE, names = "UNKNOWN")
    void transferToAdminEnum(TransferDeliveryCheckpointStatus value) {
        softly.assertThat(EnumConverter.convertToAdminUnknownCheckpointStatus(value, EntityType.TRANSFER))
            .isNotNull()
            .isNotEqualTo(AdminUnknownCheckpointStatus.UNKNOWN);
    }

    @ParameterizedTest
    @EnumSource(value = InboundOldDeliveryCheckpointStatus.class, mode = EXCLUDE, names = "UNKNOWN")
    void inboundOldToAdminEnum(InboundOldDeliveryCheckpointStatus value) {
        softly.assertThat(EnumConverter.convertToAdminUnknownCheckpointStatus(value, EntityType.INBOUND_OLD))
            .isNotNull()
            .isNotEqualTo(AdminUnknownCheckpointStatus.UNKNOWN);
    }

    @ParameterizedTest
    @EnumSource(value = OutboundOldDeliveryCheckpointStatus.class, mode = EXCLUDE, names = "UNKNOWN")
    void outboundOldToAdminEnum(OutboundOldDeliveryCheckpointStatus value) {
        softly.assertThat(EnumConverter.convertToAdminUnknownCheckpointStatus(value, EntityType.OUTBOUND_OLD))
            .isNotNull()
            .isNotEqualTo(AdminUnknownCheckpointStatus.UNKNOWN);
    }
}
