package ru.yandex.market.logistics.lom.converter;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.configuration.properties.TrackerCheckpointProcessingProperties;
import ru.yandex.market.logistics.lom.controller.tracker.dto.DeliveryTrackCheckpoint;
import ru.yandex.market.logistics.lom.dto.queue.LomSegmentCheckpoint;
import ru.yandex.market.logistics.lom.entity.WaybillSegmentStatusHistoryAdditional;
import ru.yandex.market.logistics.lom.entity.enums.SegmentStatus;

class WaybillSegmentStatusHistoryAdditionalConverterTest extends AbstractTest {

    private static final Instant TEST_TIME = Instant.now();
    private static final DeliveryTrackCheckpoint TRACKER_CHECKPOINT_WITH_ADDITIONAL = new DeliveryTrackCheckpoint(
        1L,
        TEST_TIME,
        OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED,
        "test_country",
        "test_city",
        "test_location",
        "test_zipcode"
    );
    private static final DeliveryTrackCheckpoint TRACKER_CHECKPOINT_NO_ADDITIONAL = new DeliveryTrackCheckpoint(
        1L,
        TEST_TIME,
        OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED,
        null,
        null,
        null,
        null
    );
    private static final LomSegmentCheckpoint DBQUEUE_CHECKPOINT_WITH_ADDITIONAL = LomSegmentCheckpoint.builder()
        .trackerCheckpointId(1L)
        .trackerId(2L)
        .segmentStatus(SegmentStatus.CANCELLED)
        .trackerCheckpointStatus("test_status")
        .date(TEST_TIME)
        .country("test_country")
        .city("test_city")
        .location("test_location")
        .zipCode("test_zipcode")
        .build();
    private static final LomSegmentCheckpoint DBQUEUE_CHECKPOINT_NO_ADDITIONAL = LomSegmentCheckpoint.builder()
        .trackerCheckpointId(1L)
        .trackerId(2L)
        .segmentStatus(SegmentStatus.CANCELLED)
        .trackerCheckpointStatus("test_status")
        .date(TEST_TIME)
        .build();
    private static final WaybillSegmentStatusHistoryAdditional DATABASE_ENTITY =
        new WaybillSegmentStatusHistoryAdditional()
            .setCountry("test_country")
            .setCity("test_city")
            .setLocation("test_location")
            .setZipCode("test_zipcode");

    private final WaybillSegmentStatusHistoryAdditionalConverter converter =
        new WaybillSegmentStatusHistoryAdditionalConverter(createProperties(true));

    private final WaybillSegmentStatusHistoryAdditionalConverter converterWithoutAdditional =
        new WaybillSegmentStatusHistoryAdditionalConverter(createProperties(false));

    @Test
    @DisplayName("Конвертирование из модели трекера во внутреннюю модель с включенной записью новых данных")
    void correctForRecipientUpdate_trackerWithAdditionalDataSupport() {
        softly.assertThat(converter.extractAdditionalCheckpointData(TRACKER_CHECKPOINT_WITH_ADDITIONAL).get())
            .isEqualTo(DATABASE_ENTITY);
        softly.assertThat(converter.extractAdditionalCheckpointData(TRACKER_CHECKPOINT_NO_ADDITIONAL).isEmpty())
            .isTrue();
    }

    @Test
    @DisplayName("Конвертирование из модели трекера во внутреннюю модель с выключенной записью новых данных")
    void correctForRecipientUpdate_trackerWithoutAdditionalDataSupport() {
        softly.assertThat(
            converterWithoutAdditional.extractAdditionalCheckpointData(TRACKER_CHECKPOINT_WITH_ADDITIONAL).isEmpty()
        ).isTrue();
        softly.assertThat(
            converterWithoutAdditional.extractAdditionalCheckpointData(TRACKER_CHECKPOINT_NO_ADDITIONAL).isEmpty()
        ).isTrue();
    }

    @Test
    @DisplayName("Конвертирование из модели dbqueue во внутреннюю модель с включенной записью новых данных")
    void correctForRecipientUpdate_dbqueueWithAdditionalDataSupport() {
        softly.assertThat(converter.extractAdditionalCheckpointData(DBQUEUE_CHECKPOINT_WITH_ADDITIONAL).get())
            .isEqualTo(DATABASE_ENTITY);
        softly.assertThat(converter.extractAdditionalCheckpointData(DBQUEUE_CHECKPOINT_NO_ADDITIONAL).isEmpty())
            .isTrue();
    }

    @Test
    @DisplayName("Конвертирование из модели dbqueue во внутреннюю модель с выключенной записью новых данных")
    void correctForRecipientUpdate_dbqueueWithoutAdditionalDataSupport() {
        softly.assertThat(
            converterWithoutAdditional.extractAdditionalCheckpointData(DBQUEUE_CHECKPOINT_WITH_ADDITIONAL).isEmpty()
        ).isTrue();
        softly.assertThat(
            converterWithoutAdditional.extractAdditionalCheckpointData(DBQUEUE_CHECKPOINT_NO_ADDITIONAL).isEmpty()
        ).isTrue();
    }

    private TrackerCheckpointProcessingProperties createProperties(boolean writeInDto) {
        TrackerCheckpointProcessingProperties properties = new TrackerCheckpointProcessingProperties();
        properties.setWriteInDto(writeInDto);
        return properties;
    }
}
