package ru.yandex.market.delivery.tracker.service.tracking;

import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.delivery.tracker.domain.converter.EnumConverter;
import ru.yandex.market.delivery.tracker.domain.enums.InboundDeliveryCheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.enums.InboundOldDeliveryCheckpointStatus;

import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;

@DisplayName("Конвертация енамов чекпоинтов")
class CheckpointConverterTest {

    @RegisterExtension
    final JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();

    @DisplayName("Конвертация кодов статусов в InboundOldDeliveryCheckpointStatus")
    @ParameterizedTest
    @EnumSource(
        value = ru.yandex.market.logistic.gateway.common.model.fulfillment.StatusCode.class,
        names = "UNKNOWN",
        mode = EXCLUDE
    )
    void mapToOldInboundDeliveryCheckpointStatus(
        ru.yandex.market.logistic.gateway.common.model.fulfillment.StatusCode statusCode
    ) {
        softly.assertThat(EnumConverter.convert(statusCode, InboundOldDeliveryCheckpointStatus.class))
            .isNotNull()
            .isNotEqualTo(InboundOldDeliveryCheckpointStatus.UNKNOWN);
    }

    @DisplayName("Конвертация кодов статусов в InboundDeliveryCheckpointStatus")
    @ParameterizedTest
    @EnumSource(
        value = ru.yandex.market.logistic.gateway.common.model.common.StatusCode.class,
        names = "UNKNOWN",
        mode = EXCLUDE
    )
    void mapToOldInboundDeliveryCheckpointStatus(
        ru.yandex.market.logistic.gateway.common.model.common.StatusCode statusCode
    ) {
        softly.assertThat(EnumConverter.convert(statusCode, InboundDeliveryCheckpointStatus.class))
            .isNotNull()
            .isNotEqualTo(InboundDeliveryCheckpointStatus.UNKNOWN);
    }
}
