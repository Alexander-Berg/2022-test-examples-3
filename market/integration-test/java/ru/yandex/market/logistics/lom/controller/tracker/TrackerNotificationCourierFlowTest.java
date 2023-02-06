package ru.yandex.market.logistics.lom.controller.tracker;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.Mockito.when;

@DisplayName("Тесты обработки чекпоинтов, связанных с курьером")
@DatabaseSetup("/controller/tracker/before/setup_express.xml")
class TrackerNotificationCourierFlowTest extends AbstractTrackerNotificationControllerTest {
    @Autowired
    private FeatureProperties featureProperties;

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("getCourierStatuses")
    @DisplayName("Обработка чекпоинтов с информацией о курьере")
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_courier_found.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processGetCourierCheckpointsCourierFound(
        OrderDeliveryCheckpointStatus deliveryCheckpointStatus,
        String requestContentPath
    ) {
        processGetCourierCheckpointsCourierFound(false, deliveryCheckpointStatus, requestContentPath);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("getCourierStatuses")
    @DisplayName("Обработка чекпоинтов с информацией о курьере: новый флоу обработки чекпоинтов")
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_courier_found_new_flow.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processGetCourierCheckpointsCourierFoundNewCheckpointsFlow(
        OrderDeliveryCheckpointStatus deliveryCheckpointStatus,
        String requestContentPath
    ) {
        processGetCourierCheckpointsCourierFound(true, deliveryCheckpointStatus, requestContentPath);
    }

    @SneakyThrows
    private void processGetCourierCheckpointsCourierFound(
        boolean newCheckpointsFlowEnabled,
        OrderDeliveryCheckpointStatus deliveryCheckpointStatus,
        String requestContentPath
    ) {
        setCheckpointsProcessingFlow(newCheckpointsFlowEnabled);
        notifyTracks(requestContentPath, "controller/tracker/response/push_101_ok.json");
        assertCheckpointsTaskCreatedAndRunTask(
            newCheckpointsFlowEnabled,
            processSegmentCheckpointsPayloadWithSequence(2L, 101L),
            getOrderIdDeliveryTrackPayload(1, deliveryCheckpointStatus, 101)
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_GET_COURIER,
            PayloadFactory.createWaybillSegmentIdPayload(2, "2", 1, 1)
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("getCourierStatuses")
    @DisplayName("Обработка чекпоинтов с информацией о курьере при выключенной настройке")
    void processGetCourierCheckpointsDisabled(
        OrderDeliveryCheckpointStatus deliveryCheckpointStatus,
        String requestContentPath
    ) {
        processGetCourierCheckpointsDisabled(false, deliveryCheckpointStatus, requestContentPath);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("getCourierStatuses")
    @DisplayName("Обработка чекпоинтов с информацией о курьере при выключенной настройке: новый флоу обработки чп")
    void processGetCourierCheckpointsDisabledNewCheckpointsFlow(
        OrderDeliveryCheckpointStatus deliveryCheckpointStatus,
        String requestContentPath
    ) {
        processGetCourierCheckpointsDisabled(true, deliveryCheckpointStatus, requestContentPath);
    }

    @SneakyThrows
    private void processGetCourierCheckpointsDisabled(
        boolean newCheckpointsFlowEnabled,
        OrderDeliveryCheckpointStatus deliveryCheckpointStatus,
        String requestContentPath
    ) {
        setCheckpointsProcessingFlow(newCheckpointsFlowEnabled);
        when(featureProperties.isGetCourierOn32Enabled()).thenReturn(false);
        notifyTracks(requestContentPath, "controller/tracker/response/push_101_ok.json");
        assertCheckpointsTaskCreatedAndRunTask(
            newCheckpointsFlowEnabled,
            processSegmentCheckpointsPayloadWithSequence(2L, 101L),
            getOrderIdDeliveryTrackPayload(1, deliveryCheckpointStatus, 101)
        );
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.PROCESS_GET_COURIER);
    }

    @Nonnull
    private static Stream<Arguments> getCourierStatuses() {
        return Stream.of(
            Arguments.of(
                OrderDeliveryCheckpointStatus.DELIVERY_COURIER_FOUND,
                "controller/tracker/request/lo1_courier_found.json"
            ),
            Arguments.of(
                OrderDeliveryCheckpointStatus.DELIVERY_COURIER_ARRIVED_TO_SENDER,
                "controller/tracker/request/lo1_courier_arrived_to_sender.json"
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Чекпоинт 60/70 на сегменте Экспресс с ЭАПП приводит к получению информации о курьере")
    @DatabaseSetup(
        value = "/controller/tracker/before/setup_with_erta.xml",
        type = DatabaseOperation.INSERT
    )
    void processReturnArrivedCheckpointWithErta(
        @SuppressWarnings("unused") String name,
        String requestPath,
        OrderDeliveryCheckpointStatus orderDeliveryCheckpointStatus,
        QueueType queueType,
        boolean getCourierOn60,
        boolean shouldCreateTask
    ) throws Exception {
        processReturnArrivedCheckpointWithErta(
            false,
            requestPath,
            orderDeliveryCheckpointStatus,
            queueType,
            getCourierOn60,
            shouldCreateTask
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("processReturnArrivedCheckpointWithErta")
    @DisplayName(
        "Чекпоинт 60/70 на сегменте Экспресс с ЭАПП приводит к получению информации о курьере: "
            + "новый флоу обработки чекпоинтов"
    )
    @DatabaseSetup(
        value = "/controller/tracker/before/setup_with_erta.xml",
        type = DatabaseOperation.INSERT
    )
    void processReturnArrivedCheckpointWithErtaNewCheckpointsFlow(
        @SuppressWarnings("unused") String name,
        String requestPath,
        OrderDeliveryCheckpointStatus orderDeliveryCheckpointStatus,
        QueueType queueType,
        boolean getCourierOn60,
        boolean shouldCreateTask
    ) throws Exception {
        processReturnArrivedCheckpointWithErta(
            true,
            requestPath,
            orderDeliveryCheckpointStatus,
            queueType,
            getCourierOn60,
            shouldCreateTask
        );
    }

    private void processReturnArrivedCheckpointWithErta(
        boolean newCheckpointsFlowEnabled,
        String requestPath,
        OrderDeliveryCheckpointStatus orderDeliveryCheckpointStatus,
        QueueType queueType,
        boolean getCourierOn60,
        boolean shouldCreateTask
    ) throws Exception {
        when(featureProperties.isGetCourierOn60Enabled()).thenReturn(getCourierOn60);
        setCheckpointsProcessingFlow(newCheckpointsFlowEnabled);
        notifyTracks(
            requestPath,
            "controller/tracker/response/push_101_ok.json"
        );
        assertCheckpointsTaskCreatedAndRunTask(
            newCheckpointsFlowEnabled,
            processSegmentCheckpointsPayloadWithSequence(2L, 101L),
            getOrderIdDeliveryTrackPayload(1, orderDeliveryCheckpointStatus, 101)
        );
        queueTaskChecker.assertQueueTaskCreated(
            queueType,
            PayloadFactory.createWaybillSegmentPayload(1, 2, "2", 1, 1)
        );
        if (shouldCreateTask) {
            queueTaskChecker.assertQueueTaskCreated(
                QueueType.PROCESS_GET_COURIER,
                PayloadFactory.createWaybillSegmentIdPayload(2, "3", 1, 2)
            );
        } else {
            queueTaskChecker.assertQueueTaskNotCreated(QueueType.PROCESS_GET_COURIER);
        }
    }

    @Nonnull
    private static Stream<Arguments> processReturnArrivedCheckpointWithErta() {
        return Stream.of(
            Arguments.of(
                "Чекпоинт 60, проперти получения по 60 включена, таска создается",
                "controller/tracker/request/lo1_order_ds_return_preparing.json",
                OrderDeliveryCheckpointStatus.RETURN_PREPARING,
                QueueType.PROCESS_SEGMENT_RETURN_PREPARING,
                true,
                true
            ),
            Arguments.of(
                "Чекпоинт 60, проперти получения по 60 выключена, таска не создается",
                "controller/tracker/request/lo1_order_ds_return_preparing.json",
                OrderDeliveryCheckpointStatus.RETURN_PREPARING,
                QueueType.PROCESS_SEGMENT_RETURN_PREPARING,
                false,
                false
            ),
            Arguments.of(
                "Чекпоинт 70, проперти получения по 60 включена, таска создается",
                "controller/tracker/request/lo1_order_ds_return_arrived.json",
                OrderDeliveryCheckpointStatus.RETURN_ARRIVED_DELIVERY,
                QueueType.PROCESS_SEGMENT_RETURN_ARRIVED,
                true,
                true
            ),
            Arguments.of(
                "Чекпоинт 70, проперти получения по 60 выключена, таска создается",
                "controller/tracker/request/lo1_order_ds_return_arrived.json",
                OrderDeliveryCheckpointStatus.RETURN_ARRIVED_DELIVERY,
                QueueType.PROCESS_SEGMENT_RETURN_ARRIVED,
                false,
                true
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Чекпоинт 60/70 на сегменте Экспресс без ЭАПП не приводит к получению информации о курьере")
    void processReturnArrivedCheckpoint(
        @SuppressWarnings("unused") String name,
        String requestPath,
        OrderDeliveryCheckpointStatus orderDeliveryCheckpointStatus,
        QueueType queueType,
        boolean getCourierOn60
    ) {
        processReturnArrivedCheckpoint(
            false,
            requestPath,
            orderDeliveryCheckpointStatus,
            queueType,
            getCourierOn60
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("processReturnArrivedCheckpoint")
    @DisplayName(
        "Чекпоинт 60/70 на сегменте Экспресс без ЭАПП не приводит к получению информации о курьере:"
            + "новый флоу обработки чекпоинтов"
    )
    void processReturnArrivedCheckpointNewFlow(
        @SuppressWarnings("unused") String name,
        String requestPath,
        OrderDeliveryCheckpointStatus orderDeliveryCheckpointStatus,
        QueueType queueType,
        boolean getCourierOn60
    ) {
        processReturnArrivedCheckpoint(
            true,
            requestPath,
            orderDeliveryCheckpointStatus,
            queueType,
            getCourierOn60
        );
    }

    @SneakyThrows
    private void processReturnArrivedCheckpoint(
        boolean newCheckpointsFlowEnabled,
        String requestPath,
        OrderDeliveryCheckpointStatus orderDeliveryCheckpointStatus,
        QueueType queueType,
        boolean getCourierOn60
    ) {
        when(featureProperties.isGetCourierOn60Enabled()).thenReturn(getCourierOn60);
        setCheckpointsProcessingFlow(newCheckpointsFlowEnabled);
        notifyTracks(
            requestPath,
            "controller/tracker/response/push_101_ok.json"
        );
        assertCheckpointsTaskCreatedAndRunTask(
            newCheckpointsFlowEnabled,
            processSegmentCheckpointsPayloadWithSequence(2L, 101L),
            getOrderIdDeliveryTrackPayload(1, orderDeliveryCheckpointStatus, 101)
        );
        queueTaskChecker.assertQueueTaskCreated(
            queueType,
            PayloadFactory.createWaybillSegmentPayload(1, 2, "2", 1, 1)
        );
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.PROCESS_GET_COURIER);
    }

    @Nonnull
    private static Stream<Arguments> processReturnArrivedCheckpoint() {
        return Stream.of(
            Arguments.of(
                "Чекпоинт 60, проперти получения по 60 включена, таска не создается",
                "controller/tracker/request/lo1_order_ds_return_preparing.json",
                OrderDeliveryCheckpointStatus.RETURN_PREPARING,
                QueueType.PROCESS_SEGMENT_RETURN_PREPARING,
                true
            ),
            Arguments.of(
                "Чекпоинт 60, проперти получения по 60 выключена, таска не создается",
                "controller/tracker/request/lo1_order_ds_return_preparing.json",
                OrderDeliveryCheckpointStatus.RETURN_PREPARING,
                QueueType.PROCESS_SEGMENT_RETURN_PREPARING,
                false
            ),
            Arguments.of(
                "Чекпоинт 70, проперти получения по 60 включена, таска не создается",
                "controller/tracker/request/lo1_order_ds_return_arrived.json",
                OrderDeliveryCheckpointStatus.RETURN_ARRIVED_DELIVERY,
                QueueType.PROCESS_SEGMENT_RETURN_ARRIVED,
                true
            ),
            Arguments.of(
                "Чекпоинт 70, проперти получения по 60 выключена, таска не создается",
                "controller/tracker/request/lo1_order_ds_return_arrived.json",
                OrderDeliveryCheckpointStatus.RETURN_ARRIVED_DELIVERY,
                QueueType.PROCESS_SEGMENT_RETURN_ARRIVED,
                false
            )
        );
    }
}
