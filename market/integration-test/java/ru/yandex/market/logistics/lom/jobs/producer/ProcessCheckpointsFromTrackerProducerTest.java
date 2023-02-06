package ru.yandex.market.logistics.lom.jobs.producer;

import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.controller.tracker.dto.DeliveryTrack;
import ru.yandex.market.logistics.lom.controller.tracker.dto.DeliveryTrackCheckpoint;
import ru.yandex.market.logistics.lom.controller.tracker.dto.DeliveryTrackMeta;
import ru.yandex.market.logistics.lom.entity.embedded.OrderHistoryEventAuthor;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;

@DisplayName("Обработка чекпоимнтов трека от трекера без создания бизнес-процесса")
class ProcessCheckpointsFromTrackerProducerTest extends AbstractContextualTest {

    @Autowired
    private ProcessCheckpointsFromTrackerProducer producer;

    @Test
    @DisplayName("Producer создаёт нужную таску")
    void produceTask() {
        producer.produceTask(1L, deliveryTrack(), new OrderHistoryEventAuthor().setTvmServiceId(123L));
        queueTaskChecker.assertExactlyOneQueueTaskCreated(QueueType.PROCESS_CHECKPOINTS_FROM_TRACKER);
    }

    @Nonnull
    private DeliveryTrack deliveryTrack() {
        return new DeliveryTrack(
            new DeliveryTrackMeta(1L, "track-code", "123", clock.instant()),
            List.of(
                new DeliveryTrackCheckpoint(
                    1L,
                    clock.instant(),
                    OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED,
                    "test_country",
                    "test_city",
                    "test_locaiton",
                    "test_zip_code"
                )
            )
        );
    }
}
