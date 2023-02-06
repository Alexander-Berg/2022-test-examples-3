package ru.yandex.market.logistics.lom.converter.tracker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.entity.enums.SegmentStatus;

import static org.assertj.core.api.Assertions.assertThat;

public class SegmentStatusConverterTest {

    private final SegmentStatusConverter converter = new SegmentStatusConverter();

    @ParameterizedTest
    @EnumSource(value = OrderDeliveryCheckpointStatus.class, names = "UNKNOWN", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Конвертация статусов от трекера в статусы сегментов вейбилла")
    void convertCheckpointStatus(OrderDeliveryCheckpointStatus checkpointStatus) {
        assertThat(converter.convertToSegmentStatus(checkpointStatus)).isNotEqualTo(SegmentStatus.UNKNOWN);
    }

    @Test
    @DisplayName("Конвертация неизвестного статуса")
    void convertUnknownStatus() {
        assertThat(converter.convertToSegmentStatus(OrderDeliveryCheckpointStatus.UNKNOWN))
            .isEqualTo(SegmentStatus.UNKNOWN);
    }

}
