package ru.yandex.market.logistics.lom.service.tracker.listener.implementation;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.checker.QueueTaskChecker;
import ru.yandex.market.logistics.lom.controller.tracker.dto.DeliveryTrack;
import ru.yandex.market.logistics.lom.controller.tracker.dto.DeliveryTrackCheckpoint;
import ru.yandex.market.logistics.lom.controller.tracker.dto.DeliveryTrackMeta;
import ru.yandex.market.logistics.lom.jobs.processor.ProcessDeliveryTrackerTrackService;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static ru.yandex.market.logistics.lom.jobs.model.QueueType.PROCESS_ORDER_PLACES_CHANGED;

@DisplayName("Обработка 118 чекпоинта: изменение грузомест заказа на ФФ")
@DatabaseSetup("/service/listener/orderPlacesChanged/ff/before/setup.xml")
public class FfOrderPlacesChangedListenerTest extends AbstractCheckpointListenerTest {
    @Autowired
    private ProcessDeliveryTrackerTrackService processDeliveryTrackerTrackService;
    @Autowired
    private QueueTaskChecker queueTaskChecker;

    @Test
    @DisplayName("Задача на обновление грузомест создана")
    @ExpectedDatabase(
        value = "/service/listener/orderPlacesChanged/ff/after/task_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void success() {
        processDeliveryTrackerTrackService.processPayload(
            preparePayload(
                List.of(
                    new DeliveryTrackCheckpoint(
                        1L,
                        CHECKPOINT_INSTANT,
                        OrderDeliveryCheckpointStatus.SORTING_CENTER_PLACES_CHANGED,
                        "test_country",
                        "test_city",
                        "test_location",
                        "test_zip_code"
                    )
                )
            )
        );
    }

    @Test
    @DisplayName("Создана ровно одна задача на обновление грузомест")
    @ExpectedDatabase(
        value = "/service/listener/orderPlacesChanged/ff/after/single_change_places_task_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successMultipleCheckpoints() {
        processDeliveryTrackerTrackService.processPayload(
            preparePayload(
                List.of(
                    new DeliveryTrackCheckpoint(
                        1L,
                        CHECKPOINT_INSTANT,
                        OrderDeliveryCheckpointStatus.SORTING_CENTER_PLACES_CHANGED,
                        "test_country",
                        "test_city",
                        "test_location",
                        "test_zip_code"
                    ),
                    new DeliveryTrackCheckpoint(
                        2L,
                        CHECKPOINT_INSTANT,
                        OrderDeliveryCheckpointStatus.SORTING_CENTER_PLACES_CHANGED,
                        "test_country_2",
                        "test_city_2",
                        "test_location_2",
                        "test_zip_code_2"
                    )
                )
            )
        );
    }

    @Test
    @DisplayName("Задача на обновление грузомест не создана: не ФФ сегмент")
    void notCreatedNotFFSegment() {
        processDeliveryTrackerTrackService.processPayload(
            PayloadFactory.createOrderIdDeliveryTrackPayload(
                1L,
                new DeliveryTrack(
                    new DeliveryTrackMeta(101L, "track-code", "1", CHECKPOINT_INSTANT),
                    List.of(
                        new DeliveryTrackCheckpoint(
                            1L,
                            CHECKPOINT_INSTANT,
                            OrderDeliveryCheckpointStatus.SORTING_CENTER_PLACES_CHANGED,
                            "test_country",
                            "test_city",
                            "test_locaiton",
                            "test_zip_code"
                        )
                    )
                ),
                "1",
                1
            )
        );
        queueTaskChecker.assertQueueTaskNotCreated(PROCESS_ORDER_PLACES_CHANGED);
    }
}
