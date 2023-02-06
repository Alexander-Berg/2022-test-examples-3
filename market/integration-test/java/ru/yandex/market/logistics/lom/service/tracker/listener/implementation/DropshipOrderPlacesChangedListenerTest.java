package ru.yandex.market.logistics.lom.service.tracker.listener.implementation;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseOperation;
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

@DisplayName("Обработка 118 чекпоинта: изменение грузомест заказа от Дропшипа")
@DatabaseSetup("/service/listener/orderPlacesChanged/dropship/before/setup.xml")
public class DropshipOrderPlacesChangedListenerTest extends AbstractCheckpointListenerTest {
    @Autowired
    private ProcessDeliveryTrackerTrackService processDeliveryTrackerTrackService;
    @Autowired
    private QueueTaskChecker queueTaskChecker;

    @Test
    @DisplayName("Задача на обновление грузомест создана: получен 120 чекпоинт")
    @DatabaseSetup(
        value = "/service/listener/orderPlacesChanged/dropship/before/120_received.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/service/listener/orderPlacesChanged/dropship/after/task_created.xml",
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
    @DisplayName("Создана ровно одна задача на обновление грузомест: получен 120 чекпоинт")
    @DatabaseSetup(
        value = "/service/listener/orderPlacesChanged/dropship/before/120_received.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/service/listener/orderPlacesChanged/dropship/after/single_change_places_task_created.xml",
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
    @DisplayName("Задача на обновление грузомест не создана: не получен 120 чекпоинт")
    void notCreated120NotReceived() {
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
        queueTaskChecker.assertQueueTaskNotCreated(PROCESS_ORDER_PLACES_CHANGED);
    }

    @Test
    @DisplayName("Задача на обновление грузомест не создана: получен 10/110 чекпоинт")
    @DatabaseSetup(
        value = "/service/listener/orderPlacesChanged/dropship/before/10_received.xml",
        type = DatabaseOperation.INSERT
    )
    void notCreated10Received() {
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
        queueTaskChecker.assertQueueTaskNotCreated(PROCESS_ORDER_PLACES_CHANGED);
    }

    @Test
    @DisplayName("Задача на обновление грузомест не создана: получен 35 чекпоинт")
    @DatabaseSetup(
        value = "/service/listener/orderPlacesChanged/dropship/before/35_received.xml",
        type = DatabaseOperation.INSERT
    )
    void notCreated35Received() {
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
        queueTaskChecker.assertQueueTaskNotCreated(PROCESS_ORDER_PLACES_CHANGED);
    }

    @Test
    @DisplayName("Задача на обновление грузомест не создана: не Дропшип сегмент")
    void notCreatedNotDropshipSegment() {
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
                            "test_location",
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
