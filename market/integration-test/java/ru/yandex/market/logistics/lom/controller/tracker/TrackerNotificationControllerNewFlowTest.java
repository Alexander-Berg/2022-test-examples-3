package ru.yandex.market.logistics.lom.controller.tracker;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.converter.tracker.SegmentStatusConverter;
import ru.yandex.market.logistics.lom.dto.queue.LomSegmentCheckpoint;
import ru.yandex.market.logistics.lom.entity.WaybillSegmentStatusHistory;
import ru.yandex.market.logistics.lom.entity.embedded.OrderHistoryEventAuthor;
import ru.yandex.market.logistics.lom.entity.enums.PartnerType;
import ru.yandex.market.logistics.lom.entity.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.jobs.model.ProcessSegmentCheckpointsPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.repository.WaybillSegmentRepository;
import ru.yandex.market.logistics.lom.repository.WaybillSegmentStatusHistoryRepository;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.lom.utils.TestUtils.validationErrorsJsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DatabaseSetup({
    "/billing/before/billing_service_products.xml",
    "/controller/tracker/before/setup.xml",
})
@DisplayName("Обработка чекпоинтов трекера: новый флоу")
class TrackerNotificationControllerNewFlowTest extends AbstractTrackerNotificationControllerTest {
    private static final Instant FIXED_TIME = Instant.parse("2019-06-12T00:00:00Z");
    private static final EnumSet<OrderDeliveryCheckpointStatus>
        GET_DELIVERY_DATE_PARAM_ENABLED_PARAM_REQUIRED = EnumSet.of(
        OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_SHOP,
        OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_RECIPIENT,
        OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_DELIVERY
    );

    @Autowired
    private SegmentStatusConverter segmentStatusConverter;

    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private WaybillSegmentStatusHistoryRepository waybillSegmentStatusHistoryRepository;
    @Autowired
    private WaybillSegmentRepository waybillSegmentRepository;

    @BeforeEach
    void setup() {
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE);
        setCheckpointsProcessingFlow(true);
    }

    @Test
    @DisplayName("В LOM нет информации о TrackerId")
    @ExpectedDatabase(
        value = "/controller/tracker/after/no_business_processes.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void pushTracksUnknownTrackerId() throws Exception {
        notifyTracksWithTVM(
            "controller/tracker/request/push_unknown_tracks.json",
            "controller/tracker/response/push_unknown_tracks_new_flow.json"
        );

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @JpaQueriesCount(11)
    @DisplayName("Получен чекпоинт для неактивного сегмента: таска не создается, чп в историю не записывается")
    @DatabaseSetup("/controller/tracker/before/order_with_inactive_segment.xml")
    @ExpectedDatabase(
        value = "/controller/tracker/before/order_with_inactive_segment.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void skipInactiveSegmentCheckpoint() {
        notifyTracksWithTVM(
            "controller/tracker/request/inactive_segment_tracks.json",
            "controller/tracker/response/inactive_segment_tracks.json"
        );
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @JpaQueriesCount(16)
    @DisplayName(
        "Чекпоинт с trackerId как активного, так и неактивного сегмента: таска создается для активного сегмента"
    )
    @DatabaseSetup("/controller/tracker/before/order_with_active_and_inactive_segment.xml")
    @ExpectedDatabase(
        value = "/controller/tracker/after/order_with_active_and_inactive_segment.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void taskCreatedForActiveSegment() {
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE);

        notifyTracksWithTVM(
            "controller/tracker/request/inactive_segment_tracks.json",
            "controller/tracker/response/active_and_inactive_segment_tracks.json"
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            processSegmentCheckpointsPayloadWithSequenceAndTvm(2L, 101L, 1L)
        );
    }

    @Test
    @DisplayName("Успешно обновить статусы заказов всех треков")
    @ExpectedDatabase(value = "/controller/tracker/after/push_tracks_ok.xml", assertionMode = NON_STRICT_UNORDERED)
    void pushTracksOk() throws Exception {
        notifyTracksWithTVM(
            "controller/tracker/request/push_tracks_ok.json",
            "controller/tracker/response/push_tracks_ok_new_flow.json"
        );

        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload1 =
            processSegmentCheckpointsPayloadWithSequenceAndTvm(
            1L,
            100L,
            1L
        );
        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload2 =
            processSegmentCheckpointsPayloadWithSequenceAndTvm(
            2L,
            101L,
            2L
        );
        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload3 =
            processSegmentCheckpointsPayloadWithSequenceAndTvm(
            4L,
            201L,
            3L
        );
        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload4 =
            processSegmentCheckpointsPayloadWithSequenceAndTvm(
            6L,
            301L,
            4L
        );

        Stream.of(
            segmentCheckpointsPayload1,
            segmentCheckpointsPayload2,
            segmentCheckpointsPayload3,
            segmentCheckpointsPayload4
        ).forEach(
            payload -> {
                queueTaskChecker.assertQueueTaskCreated(QueueType.PROCESS_SEGMENT_CHECKPOINTS, payload);
                processSegmentCheckpointsConsumer.execute(
                    TaskFactory.createTask(QueueType.PROCESS_SEGMENT_CHECKPOINTS, payload)
                );
            }
        );
    }

    @Test
    @DisplayName("Устаревшие чекпоинты от трекера")
    @DatabaseSetup(
        value = "/controller/tracker/before/waybill_segment_status_history.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(value = "/controller/tracker/after/push_expired_ok.xml", assertionMode = NON_STRICT_UNORDERED)
    void expiredCheckpointsHandle() throws Exception {
        notifyTracks(
            "controller/tracker/request/two_checkpoints_prepare.json",
            "controller/tracker/response/push_600_ok.json"
        );

        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
            7L,
            600L
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        );
        processSegmentCheckpointsConsumer.execute(
            TaskFactory.createTask(QueueType.PROCESS_SEGMENT_CHECKPOINTS, segmentCheckpointsPayload)
        );

        assertThat(jdbcTemplate.queryForList("SELECT * FROM order_history_event WHERE order_id = ?", 6L))
            .allMatch(event ->
                event.get("snapshot") != null && event.get("logbroker_id") == null
                    || event.get("snapshot") == null && event.get("logbroker_id").equals(-1L)
            );
    }

    @Test
    @DisplayName("Повторная отправка неповторяемого чекпоинта")
    @DatabaseSetup(
        value = "/controller/tracker/before/order_delivery_delivered.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(value = "/controller/tracker/before/setup.xml", assertionMode = NON_STRICT_UNORDERED)
    void orderDeliveredRepeatedHandler() throws Exception {
        notifyTracks(
            "controller/tracker/request/lo1_order_delivered.json",
            "controller/tracker/response/push_101_ok.json"
        );

        queueTaskChecker.assertNoQueueTasksCreated();
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains("Got no new checkpoints for trackerId 101 for order 1");
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Валидация запроса")
    @MethodSource("validateRequestArguments")
    void validateRequest(String field, String message, String requestPath) throws Exception {
        mockMvc.perform(
                post("/notifyTracks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(extractFileContent(requestPath))
            )
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorsJsonContent(field, message));
    }

    @Nonnull
    private static Stream<Arguments> validateRequestArguments() {
        return Stream.of(
                Triple.of(
                    "tracks",
                    "must not be empty",
                    "empty_tracks.json"
                ),
                Triple.of(
                    "tracks[0].deliveryTrackMeta",
                    "must not be null",
                    "null_delivery_track_meta.json"
                ),
                Triple.of(
                    "tracks[0].deliveryTrackMeta.id",
                    "must not be null",
                    "null_delivery_track_meta_id.json"
                ),
                Triple.of(
                    "tracks[0].deliveryTrackMeta.entityId",
                    "must not be null",
                    "null_delivery_track_meta_entity_id.json"
                ),
                Triple.of(
                    "tracks[0].deliveryTrackMeta.lastUpdatedDate",
                    "must not be null",
                    "null_delivery_track_meta_last_updated.json"
                ),
                Triple.of(
                    "tracks[0].deliveryTrackCheckpoints",
                    "must not be empty",
                    "empty_delivery_track_checkpoints.json"
                ),
                Triple.of(
                    "tracks[0].deliveryTrackCheckpoints[0].checkpointDate",
                    "must not be null",
                    "null_delivery_track_checkpoint_date.json"
                ),
                Triple.of(
                    "tracks[0].deliveryTrackCheckpoints[0].deliveryCheckpointStatus",
                    "must not be null",
                    "null_delivery_track_checkpoint_status.json"
                )
            )
            .map(
                triple -> Arguments.of(
                    triple.getLeft(),
                    triple.getMiddle(),
                    "controller/tracker/request/" + triple.getRight()
                )
            );
    }

    @Test
    @DisplayName("Чекпоинты отмененного заказа")
    @DatabaseSetup(value = "/controller/tracker/before/cancel_order.xml", type = DatabaseOperation.UPDATE)
    @ExpectedDatabase(value = "/controller/tracker/after/push_cancelled_ok.xml", assertionMode = NON_STRICT_UNORDERED)
    void cancelledOrderHandle() throws Exception {
        notifyTracks(
            "controller/tracker/request/lo1_order_delivered.json",
            "controller/tracker/response/push_101_ok.json"
        );

        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
            2L,
            101L
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        );
        processSegmentCheckpointsConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        ));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CREATE_TRUST_BASKET,
            PayloadFactory.createOrderIdPayload(1L, 1, 1).setSequenceId(2L)
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CREATE_BILLING_TRANSACTION_BY_SEGMENT_STATUSES,
            PayloadFactory.createOrderIdSegmentStatusesPayload(
                1L,
                List.of(
                    LomSegmentCheckpoint.builder()
                        .trackerId(101L)
                        .trackerCheckpointId(1L)
                        .segmentStatus(SegmentStatus.OUT)
                        .date(Instant.parse("2019-08-06T13:40:00.00Z"))
                        .trackerCheckpointStatus("DELIVERY_DELIVERED")
                        .build()
                ),
                1,
                2
            ),
            3
        );
    }

    @Test
    @DisplayName("Валидная обработка чекпоинта 114 без даты в истории статусов")
    @DatabaseSetup(
        value = "/controller/tracker/before/order_items_changed_by_partner.xml",
        type = DatabaseOperation.REFRESH
    )
    @DatabaseSetup(
        value = "/controller/tracker/before/order_automatically_removed_items_without_date.xml",
        type = DatabaseOperation.INSERT
    )
    void orderChangedByPartnerRepeatedHandleWithoutDate() throws Exception {
        notifyTracks(
            "controller/tracker/request/lo8_order_changed_by_partner_repeated.json",
            "controller/tracker/response/push_800_ok.json"
        );
    }

    @Test
    @DisplayName("Обработка чекпоинта 50 от партнёра")
    @DatabaseSetup("/controller/tracker/before/setup_cancellation_requests.xml")
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_delivery_delivered_ok.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processDeliveryDeliveredStatus() throws Exception {
        notifyTracks(
            "controller/tracker/request/lo1_order_delivered.json",
            "controller/tracker/response/push_101_ok.json"
        );

        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
            2L,
            101L
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        );
        processSegmentCheckpointsConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        ));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CREATE_TRUST_BASKET,
            PayloadFactory.createOrderIdPayload(1L, 1, 1).setSequenceId(2L)
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Чекпоинты 50/130 не процессятся для сегмента дропоффа (партнёр СД, тип сегмента СЦ)")
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_dropoff_segment_out.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void segmentOutNotProcessedForDropoff(
        @SuppressWarnings("unused") String displayName,
        String requestPath,
        OrderDeliveryCheckpointStatus checkpointStatus
    ) throws Exception {
        notifyTracks(requestPath, "controller/tracker/response/push_901_ok.json");

        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
          15L,
          901L
        );
       queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
           segmentCheckpointsPayload
        );
       processSegmentCheckpointsConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
           segmentCheckpointsPayload
        ));

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.CREATE_TRUST_BASKET);
    }

    @Nonnull
    private static Stream<Arguments> segmentOutNotProcessedForDropoff() {
        return Stream.of(
            Arguments.of(
                "50 чекпоинт",
                "controller/tracker/request/lo9_delivery_delivered_dropoff_segment.json",
                OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED
            ),
            Arguments.of(
                "130 чекпоинт",
                "controller/tracker/request/lo9_sc_transmitted_dropoff_segment.json",
                OrderDeliveryCheckpointStatus.SORTING_CENTER_TRANSMITTED
            )
        );
    }

    @Test
    @DisplayName("Чекпоинт 49 не процессится для сегмента дропоффа (партнёр СД, тип сегмента СЦ)")
    @DatabaseSetup("/controller/tracker/before/setup_cancellation_requests_order_9.xml")
    @ExpectedDatabase(
        value = "/controller/tracker/before/setup_cancellation_requests_order_9.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void deliveryTransmittedToRecipientNotProcessedForDropoff() throws Exception {
        notifyTracks(
            "controller/tracker/request/lo9_delivery_transmitted_to_recipient_dropoff_segment.json",
            "controller/tracker/response/push_901_ok.json"
        );
        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
          15L,
            901L
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        );
        processSegmentCheckpointsConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        ));
    }

    @Test
    @DisplayName("Обработка чекпоинта 49 от партнёра")
    @DatabaseSetup("/controller/tracker/before/setup_cancellation_requests.xml")
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_delivery_transmitted_to_recipient_ok.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void deliveryTransmittedToRecipientStatus() throws Exception {
        notifyTracks(
            "controller/tracker/request/lo1_order_transmitted_to_recipient.json",
            "controller/tracker/response/push_101_ok.json"
        );

        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
            2L,
            101L
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        );
        processSegmentCheckpointsConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        ));
    }

    @Test
    @DisplayName("Обработка чекпоинта 101 от партнёра для отмены заказа на сегменте фф")
    @DatabaseSetup(
        value = "/controller/tracker/before/setup_cancellation_requests_ff_express.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_ff_segment_cancel_retry_new_flow.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void ffSegmentCancellationRetry() throws Exception {
        notifyTracks(
            "controller/tracker/request/lo1_ff_cancel_retry.json",
            "controller/tracker/response/push_100_ok.json"
        );

        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
            1L,
            100L
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        );
        processSegmentCheckpointsConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        ));
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_WAYBILL_SEGMENT_CANCEL,
            PayloadFactory.createSegmentCancellationRequestIdPayload(1, "2", 1, 1)
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Чекпоинты не процессятся для сегмента дропоффа (партнёр СД, тип сегмента СЦ)")
    void checkpointNotProcessedForDropoff(
        @SuppressWarnings("unused") String displayName,
        String requestPath,
        OrderDeliveryCheckpointStatus checkpointStatus,
        QueueType queueType
    ) throws Exception {
        notifyTracks(requestPath, "controller/tracker/response/push_901_ok.json");
        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
            15L,
            901L
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        );
        processSegmentCheckpointsConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        ));
        queueTaskChecker.assertQueueTaskNotCreated(queueType);
    }

    @Nonnull
    private static Stream<Arguments> checkpointNotProcessedForDropoff() {
        return Stream.of(
            Arguments.of(
                "32 чекпоинт",
                "controller/tracker/request/lo9_courier_arrived_to_sender_dropoff_segment.json",
                OrderDeliveryCheckpointStatus.DELIVERY_COURIER_ARRIVED_TO_SENDER,
                QueueType.PROCESS_GET_COURIER
            ),
            Arguments.of(
                "34 чекпоинт",
                "controller/tracker/request/lo9_courier_found_dropoff_segment.json",
                OrderDeliveryCheckpointStatus.DELIVERY_COURIER_FOUND,
                QueueType.PROCESS_GET_COURIER
            ),
            Arguments.of(
                "36 чекпоинт",
                "controller/tracker/request/lo9_courier_not_found_dropoff_segment.json",
                OrderDeliveryCheckpointStatus.DELIVERY_COURIER_NOT_FOUND,
                QueueType.PROCESS_SEGMENT_COURIER_NOT_FOUND
            ),
            Arguments.of(
                "44 чекпоинт",
                "controller/tracker/request/lo9_delivery_date_updated_44_dropoff_segment.json",
                OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_SHOP,
                QueueType.PROCESS_DELIVERY_DATE_UPDATED_BY_DS
            ),
            Arguments.of(
                "46 чекпоинт",
                "controller/tracker/request/lo9_delivery_date_updated_46_dropoff_segment.json",
                OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_RECIPIENT,
                QueueType.PROCESS_DELIVERY_DATE_UPDATED_BY_DS
            ),
            Arguments.of(
                "47 чекпоинт",
                "controller/tracker/request/lo9_delivery_date_updated_47_dropoff_segment.json",
                OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_DELIVERY,
                QueueType.PROCESS_DELIVERY_DATE_UPDATED_BY_DS
            )
        );
    }

    @Test
    @DisplayName("Обработка чекпоинта 36 от партнёра")
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_courier_not_found_new_flow.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processCourierNotFound() throws Exception {
        notifyTracks(
            "controller/tracker/request/lo1_courier_not_found.json",
            "controller/tracker/response/push_101_ok.json"
        );

        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
            2L,
            101L
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        );
        processSegmentCheckpointsConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        ));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_COURIER_NOT_FOUND,
            PayloadFactory.createWaybillSegmentPayload(1, 2, "2", 1, 1)
        );
    }

    @Test
    @DisplayName("Получение чекпоинта 36 от партнёра")
    @ExpectedDatabase(
        value = "/controller/tracker/after/36/36_received.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processCourierNotFoundWithoutProcessing() throws Exception {
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE);
        notifyTracks(
            "controller/tracker/request/lo1_courier_not_found.json",
            "controller/tracker/response/push_101_ok.json"
        );

        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
            2L,
            101L
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        );
    }

    @Test
    @DisplayName("Получение чекпоинтов 31, 32, 48 в перемешанном порядке от партнёра")
    @ExpectedDatabase(
        value = "/controller/tracker/after/31_32_48/31_32_48_one_segment_received.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processCourierUnorderedCheckpoints() throws Exception {
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE);
        notifyTracks(
            "controller/tracker/request/31_32_48/31_32_48_one_segment.json",
            "controller/tracker/response/push_101_ok.json"
        );

        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
            2L,
            101L
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        );
    }

    @Test
    @DisplayName("Получение чекпоинтов от партнёра по нескольким сегментам")
    @ExpectedDatabase(
        value = "/controller/tracker/after/31_120/31_120_received.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void saveUnorderedCheckpointsOnDifferentSegments() throws Exception {
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE);
        notifyTracks(
            "controller/tracker/request/31_120/31_120_different_segments.json",
            "controller/tracker/response/31_120/31_120_ok.json"
        );

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            processSegmentCheckpointsPayloadWithSequence(
                2L,
                101L,
                1L
            )
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            processSegmentCheckpointsPayloadWithSequence(
                1L,
                100L,
                2L
            )
        );
    }

    @Test
    @DisplayName("Чекпоинт 120 от дропшипа")
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_dropship_ready_to_ship_ok.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void dropshipReadyToShipStatusListener() throws Exception {
        notifyTracks(
            "controller/tracker/request/dropship_checkpoint.json",
            "controller/tracker/response/push_700_ok.json"
        );

        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
            9L,
            700L
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        );
        processSegmentCheckpointsConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        ));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_ORDER_READY_TO_SHIP,
            PayloadFactory.createWaybillSegmentPayload(7L, 9L, 1, 1).setSequenceId(2L)
        );
    }

    @Test
    @DisplayName("Чекпоинт 120 от дропшипа по заказу с заявкой на изменение товаров заказа")
    @DatabaseSetup("/controller/tracker/before/order_change_request_dropship.xml")
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_dropship_ready_to_ship_ok.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void dropshipReadyToShipStatusListenerOrderChangedByPartner() throws Exception {
        notifyTracks(
            "controller/tracker/request/dropship_checkpoint.json",
            "controller/tracker/response/push_700_ok.json"
        );

        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
          9L,
            700L
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        );
        processSegmentCheckpointsConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        ));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_ORDER_READY_TO_SHIP,
            PayloadFactory.createWaybillSegmentPayload(7L, 9L, 1, 1).setSequenceId(2L)
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_ORDER_CHANGED_BY_PARTNER_REQUEST,
            PayloadFactory.createWaybillSegmentPayload(7L, 9L, 1, 2).setSequenceId(3L)
        );
    }

    @Test
    @DisplayName("Чекпоинт 120 от сц по заказу с заявкой на изменение товаров заказа")
    @DatabaseSetup("/controller/tracker/before/order_change_request_dropship.xml")
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_dropship_ready_to_ship_ok_sc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void dropshipReadyToShipStatusListenerOrderChangedByPartnerFromDs() throws Exception {
        notifyTracks(
            "controller/tracker/request/dropship_sc_change_request.json",
            "controller/tracker/response/push_701_ok.json"
        );

        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
            10L,
            701L
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        );
        processSegmentCheckpointsConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        ));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_ORDER_CHANGED_BY_PARTNER_REQUEST,
            PayloadFactory.createWaybillSegmentPayload(7L, 9L, 1, 1).setSequenceId(2L)
        );
    }

    @Test
    @DisplayName("Чекпоинт 120 от фулфиллмента по заказу с заявкой на изменение товаров заказа")
    @DatabaseSetup(
        value = "/controller/tracker/before/order_items_changed_by_partner.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_fulfillment_ready_to_ship.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void fulfillmentReadyToShipStatusListenerOrderChangedByPartner() throws Exception {
        notifyTracks(
            "controller/tracker/request/lo8_order_sorting_center_prepared.json",
            "controller/tracker/response/push_800_ok.json"
        );

        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = new ProcessSegmentCheckpointsPayload(
            REQUEST_ID + "/1",
            12L,
            800L,
            new OrderHistoryEventAuthor()
        );

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        );
        processSegmentCheckpointsConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        ));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_ORDER_READY_TO_SHIP,
            PayloadFactory.createWaybillSegmentPayload(8L, 12L, 1, 1).setSequenceId(2L)
        );
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.PROCESS_ORDER_CHANGED_BY_PARTNER_REQUEST);
    }

    @Test
    @DisplayName("Чекпоинт 120 от фулфиллмента по заказу с обработанной заявкой на изменение товаров заказа")
    @DatabaseSetup(
        value = "/controller/tracker/before/order_items_changed_by_partner.xml",
        type = DatabaseOperation.REFRESH
    )
    @DatabaseSetup(
        value = "/controller/tracker/before/order_items_changed_by_partner_processing.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_fulfillment_ready_to_ship.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void fulfillmentReadyToShipStatusListenerOrderChangedByPartnerProcessing() throws Exception {
        notifyTracks(
            "controller/tracker/request/lo8_order_sorting_center_prepared.json",
            "controller/tracker/response/push_800_ok.json"
        );

        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
            12L,
            800L
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        );
        processSegmentCheckpointsConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        ));
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_ORDER_READY_TO_SHIP,
            PayloadFactory.createWaybillSegmentPayload(8L, 12L, 1, 1).setSequenceId(2L)
        );
        queueTaskChecker.assertQueueTaskNotCreated(
            QueueType.PROCESS_ORDER_CHANGED_BY_PARTNER_REQUEST
        );
        assertBillingTransactionQueueTaskCreated(
            SegmentStatus.TRANSIT_PREPARED,
            "SORTING_CENTER_PREPARED",
            3,
            1,
            2
        );
    }

    @Test
    @DisplayName("Чекпоинт 120 от фулфиллмента по заказу с заявкой на изменение товаров заказа")
    @DatabaseSetup(
        value = "/controller/tracker/before/order_items_changed_by_partner_change_request.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_fulfillment_ready_to_ship.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void fulfillmentReadyToShipStatusListenerOrderChangedByPartnerChangeRequest() throws Exception {
        notifyTracks(
            "controller/tracker/request/lo8_order_sorting_center_prepared.json",
            "controller/tracker/response/push_800_ok.json"
        );
        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
            12L,
            800L
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        );
        processSegmentCheckpointsConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        ));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_ORDER_READY_TO_SHIP,
            PayloadFactory.createWaybillSegmentPayload(8L, 12L, 1, 1).setSequenceId(2L)
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_ORDER_CHANGED_BY_PARTNER_REQUEST,
            PayloadFactory.createWaybillSegmentPayload(8L, 12L, 1, 2).setSequenceId(3L)
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CREATE_BILLING_TRANSACTION_BY_SEGMENT_STATUSES,
            PayloadFactory.createOrderIdSegmentStatusesPayload(
                8L,
                List.of(
                    LomSegmentCheckpoint.builder()
                        .trackerId(800L)
                        .trackerCheckpointId(1L)
                        .segmentStatus(SegmentStatus.TRANSIT_PREPARED)
                        .date(Instant.parse("2019-08-06T13:40:00.00Z"))
                        .trackerCheckpointStatus("SORTING_CENTER_PREPARED")
                        .build()
                ),
                1,
                3
            ).setSequenceId(4L)
        );
    }

    @Test
    @DisplayName("Чекпоинт 130 от фулфиллмента по заказу с заявкой на изменение товаров заказа")
    @DatabaseSetup(
        value = "/controller/tracker/before/order_items_changed_by_partner_change_request.xml",
        type = DatabaseOperation.REFRESH
    )
    void fulfillmentOutStatusListenerOrderChangedByPartnerChangeRequest() throws Exception {
        notifyTracks(
            "controller/tracker/request/lo8_order_sorting_center_out.json",
            "controller/tracker/response/push_800_ok.json"
        );

        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
            12L,
            800L
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        );
        processSegmentCheckpointsConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        ));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_ORDER_CHANGED_BY_PARTNER_REQUEST,
            PayloadFactory.createWaybillSegmentPayload(8L, 12L, 1, 1).setSequenceId(2L)
        );
        assertBillingTransactionQueueTaskCreated(
            SegmentStatus.OUT,
            "SORTING_CENTER_TRANSMITTED",
            3,
            1,
            2
        );
    }

    @Test
    @DisplayName("Чекпоинт 120 от фулфиллмента по заказу с обработанной заявкой на изменение товаров заказа")
    @DatabaseSetup(
        value = "/controller/tracker/before/order_items_changed_by_partner_change_request.xml",
        type = DatabaseOperation.REFRESH
    )
    @DatabaseSetup(
        value = "/controller/tracker/before/order_items_changed_by_partner_change_request_processing.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_fulfillment_ready_to_ship.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void fulfillmentReadyToShipStatusListenerOrderChangedByPartnerChangeRequestProcessing() throws Exception {
        OrderHistoryEventAuthor author = new OrderHistoryEventAuthor().setTvmServiceId(222L);
        notifyTracksWithTVM(
            "controller/tracker/request/lo8_order_sorting_center_prepared.json",
            "controller/tracker/response/push_800_ok.json"
        );

        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequenceAndTvm(
            12L,
            800L,
            1L
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        );
        processSegmentCheckpointsConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        ));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_ORDER_READY_TO_SHIP,
            PayloadFactory.createWaybillSegmentPayload(8L, 12L, 1, 1).setSequenceId(2L)
        );
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.PROCESS_ORDER_CHANGED_BY_PARTNER_REQUEST);

        assertBillingTransactionQueueTaskCreatedWithAuthor(
            SegmentStatus.TRANSIT_PREPARED,
            "SORTING_CENTER_PREPARED",
            3,
            author,
            1,
            2
        );
    }

    @Test
    @DisplayName("Чекпоинт 120 от фулфиллмента по заказу без заявки на изменение товаров заказа")
    void fulfillmentReadyToShipStatusListenerOrderNotChangedByPartner() throws Exception {
        notifyTracks(
            "controller/tracker/request/lo8_order_sorting_center_prepared.json",
            "controller/tracker/response/push_800_ok.json"
        );

        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
            12L,
            800L
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        );
        processSegmentCheckpointsConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        ));

        queueTaskChecker.assertQueueTaskNotCreated(
            QueueType.PROCESS_ORDER_CHANGED_BY_PARTNER_REQUEST
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_ORDER_READY_TO_SHIP,
            PayloadFactory.createWaybillSegmentPayload(8L, 12L, 1, 1).setSequenceId(2L)
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CREATE_BILLING_TRANSACTION_BY_SEGMENT_STATUSES,
            PayloadFactory.createOrderIdSegmentStatusesPayload(
                8L,
                List.of(
                    LomSegmentCheckpoint.builder()
                        .trackerId(800L)
                        .trackerCheckpointId(1L)
                        .segmentStatus(SegmentStatus.TRANSIT_PREPARED)
                        .date(Instant.parse("2019-08-06T13:40:00.00Z"))
                        .trackerCheckpointStatus("SORTING_CENTER_PREPARED")
                        .build()
                ),
                1,
                2
            ).setSequenceId(3L)
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("checkpointListenerArguments")
    @DisplayName("Обработка получения чекпоинтов")
    void checkpointListener(
        @SuppressWarnings("unused") String name,
        String notifyTracksRequest,
        String notifyTracksResponse,
        int trackId,
        long waybillSegmentId,
        @Nullable QueueType checkpointProcessorQueue
    ) throws Exception {
        notifyTracks(notifyTracksRequest, notifyTracksResponse);

        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
            waybillSegmentId,
            trackId
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        );
        processSegmentCheckpointsConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        ));

        if (checkpointProcessorQueue != null) {
            queueTaskChecker.assertQueueTaskCreated(
                checkpointProcessorQueue,
                PayloadFactory.createWaybillSegmentPayload(1L, waybillSegmentId, 1, 1).setSequenceId(2L)
            );
        }
    }

    @Nonnull
    private static Stream<Arguments> checkpointListenerArguments() {
        return Stream.of(
            Arguments.of(
                "Чекпоинт 70 от СД",
                "controller/tracker/request/lo1_order_ds_return_arrived.json",
                "controller/tracker/response/push_101_ok.json",
                101,
                2L,
                QueueType.PROCESS_SEGMENT_RETURN_ARRIVED
            ),
            Arguments.of(
                "Чекпоинт 80 от СД",
                "controller/tracker/request/lo1_order_ds_returned.json",
                "controller/tracker/response/push_101_ok.json",
                101,
                2L,
                QueueType.PROCESS_SEGMENT_RETURNED
            ),
            Arguments.of(
                "Чекпоинт 180 от ФФ",
                "controller/tracker/request/lo1_order_ff_returned.json",
                "controller/tracker/response/push_100_ok.json",
                100,
                1L,
                QueueType.PROCESS_SEGMENT_RETURNED
            ),
            Arguments.of(
                "Чекпоинт 181 от ФФ",
                "controller/tracker/request/lo1_order_ff_partially_returned.json",
                "controller/tracker/response/push_100_ok.json",
                100,
                1L,
                QueueType.PROCESS_SEGMENT_RETURNED
            ),
            Arguments.of(
                "Чекпоинт 105 от ФФ",
                "controller/tracker/request/lo1_order_ff_cancelled.json",
                "controller/tracker/response/push_100_ok.json",
                100,
                1L,
                QueueType.PROCESS_SEGMENT_CANCELLED
            ),
            Arguments.of(
                "Чекпоинт 403 от СД",
                "controller/tracker/request/lo1_order_ds_lost.json",
                "controller/tracker/response/push_101_ok.json",
                101,
                2L,
                QueueType.PROCESS_ORDER_LOST
            ),
            Arguments.of(
                "Чекпоинт 410 от СД",
                "controller/tracker/request/lo1_order_ds_cancelled.json",
                "controller/tracker/response/push_101_ok.json",
                101,
                2L,
                null
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("checkpointWithFastCancellationListenerArguments")
    @DisplayName("Обработка получения чекпоинтов")
    void checkpointWithFastCancellationListener(
        @SuppressWarnings("unused") String name,
        String notifyTracksRequest,
        String notifyTracksResponse,
        int trackId,
        long waybillSegmentId,
        @Nullable QueueType checkpointProcessorQueue
    ) throws Exception {
        notifyTracks(notifyTracksRequest, notifyTracksResponse);

        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
            waybillSegmentId,
            trackId
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        );
        processSegmentCheckpointsConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        ));

        queueTaskChecker.assertQueueTaskCreated(
            checkpointProcessorQueue,
            PayloadFactory.createWaybillSegmentPayload(1L, waybillSegmentId, 1, 1).setSequenceId(2L)
        );
    }

    @Nonnull
    private static Stream<Arguments> checkpointWithFastCancellationListenerArguments() {
        return Stream.of(
            Arguments.of(
                "Чекпоинт 60 от СД",
                "controller/tracker/request/lo1_order_ds_return_preparing.json",
                "controller/tracker/response/push_101_ok.json",
                101,
                2L,
                QueueType.PROCESS_SEGMENT_RETURN_PREPARING
            ),
            Arguments.of(
                "Чекпоинт 410 от СД",
                "controller/tracker/request/lo1_order_ds_cancelled.json",
                "controller/tracker/response/push_101_ok.json",
                101,
                2L,
                QueueType.PROCESS_SEGMENT_CANCELLED
            )
        );
    }

    @Test
    @DisplayName("Получение транзитного статуса от СД после возвратного")
    @DatabaseSetup("/controller/tracker/before/setup_returning.xml")
    @ExpectedDatabase(value = "/controller/tracker/after/push_returning.xml", assertionMode = NON_STRICT_UNORDERED)
    void dsTransitCheckpointListener() throws Exception {
        notifyTracks(
            "controller/tracker/request/returning_order_ds.json",
            "controller/tracker/response/returning_order_ds.json"
        );

        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload101 = processSegmentCheckpointsPayloadWithSequence(
            1L,
            101L
        );
        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload102 = processSegmentCheckpointsPayloadWithSequence(
            2L,
            102L,
            2L
        );
        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload103 = processSegmentCheckpointsPayloadWithSequence(
            3L,
            103L,
            3L
        );

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload101
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload102
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload103
        );

        processSegmentCheckpointsConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload101
        ));
        processSegmentCheckpointsConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload102
        ));
        processSegmentCheckpointsConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload103
        ));

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                "level=ERROR\t" +
                    "format=plain\t" +
                    "code=DELIVERY_TRANSPORTATION_AFTER_CANCELLATION\t" +
                    "payload=Delivery transportation (TRANSIT_TRANSMITTED_TO_RECIPIENT) " +
                    "after RETURNING has been detected for order 2\t" +
                    "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/2\t" +
                    "entity_types=order,lom_order,partner\t" +
                    "entity_values=order:null,lom_order:2,partner:1\t" +
                    "extra_keys=orderType,orderStatus,checkpointStatus\t" +
                    "extra_values=DAAS,RETURNING,TRANSIT_TRANSMITTED_TO_RECIPIENT\n"
            );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Получение транзитного статуса от СД после возвратного (проверка по всем статусам)")
    @MethodSource("dsTransitCheckpointListenerScanArguments")
    @DatabaseSetup({
        "/controller/tracker/before/setup_returning.xml",
        "/controller/tracker/before/business_process_state.xml",
    })
    void dsTransitCheckpointListenerScan(OrderDeliveryCheckpointStatus status) {
        saveUnprocessedStatusToWaybillSegmentStatusHistory(status, 2L);

        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
            2L,
            102L,
            2L
        );
        processSegmentCheckpointsConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        ));

        SegmentStatus segmentStatus = segmentStatusConverter.convertToSegmentStatus(status);

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                "level=ERROR\t" +
                    "format=plain\t" +
                    "code=DELIVERY_TRANSPORTATION_AFTER_CANCELLATION\t" +
                    "payload=Delivery transportation (" + segmentStatus + ") " +
                    "after RETURNING has been detected for order 2\t" +
                    "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/2\t" +
                    "entity_types=order,lom_order,partner\t" +
                    "entity_values=order:null,lom_order:2,partner:1\t" +
                    "extra_keys=orderType,orderStatus,checkpointStatus\t" +
                    "extra_values=DAAS,RETURNING," + segmentStatus + "\n"
            );
    }

    @Nonnull
    private static Stream<Arguments> dsTransitCheckpointListenerScanArguments() {
        return Stream.of(
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_TRANSPORTATION_RECIPIENT),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_TRANSMITTED_TO_RECIPIENT),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED)
        );
    }

    @Test
    @DisplayName("Проверка отсутствия логирования при получении статуса отгрузки для заказа в обработке и поздних чп")
    @DatabaseSetup("/controller/tracker/before/setup_returning_shipment.xml")
    void shipmentCheckpointListenerNegative() {
        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
            6L,
            106L,
            2L
        );

        processSegmentCheckpointsConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        ));

        softly.assertThat(backLogCaptor.getResults()).doesNotContain("code=SHIPMENT_AFTER_CANCELLATION");
    }

    @Test
    @DisplayName("Проверка логирования при получении статуса отгрузки для заказа с поздним чп")
    @DatabaseSetup({
        "/controller/tracker/before/setup_returning_shipment.xml",
        "/controller/tracker/before/business_process_state.xml",
    })
    void shipmentCheckpointListenerLate() {
        saveUnprocessedStatusToWaybillSegmentStatusHistory(OrderDeliveryCheckpointStatus.DELIVERY_AT_START, 7L);
        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
            7L,
            107L,
            2L
        );
        processSegmentCheckpointsConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        ));

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                "level=ERROR\t" +
                    "format=plain\t" +
                    "code=SHIPMENT_AFTER_CANCELLATION\t" +
                    "payload=Shipment (IN) after CANCELLED has been detected for order 6\t" +
                    "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/2\t" +
                    "entity_types=order,lom_order,partner\t" +
                    "entity_values=order:null,lom_order:6,partner:5\t" +
                    "extra_keys=orderType,orderStatus,partnerType,checkpointStatus,isAfterCancellation\t" +
                    "extra_values=Unknown,CANCELLED,DELIVERY,IN,false\n"
            );
    }

    @Test
    @DisplayName("Проверка записи фактической даты дедлайна")
    @DatabaseSetup({
        "/controller/tracker/before/setup_waybill_plan_fact.xml",
        "/controller/tracker/before/business_process_state.xml",
    })
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_waybill_plan_fact.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void waybillSegmentFactDateListener() {
        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
            1L,
            101L,
            2L
        );
        processSegmentCheckpointsConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        ));
    }

    @Test
    @DisplayName("Проверка записи фактической даты дедлайна")
    @DatabaseSetup({
        "/controller/tracker/before/setup_waybill_plan_fact_enqueued.xml",
        "/controller/tracker/before/business_process_state.xml",
    })
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_waybill_plan_fact_enqueued.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void waybillSegmentFactDateListenerEnqueued() {
        ProcessSegmentCheckpointsPayload payload = processSegmentCheckpointsPayload(1L, 101L);

        processSegmentCheckpointsConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            payload
        ));
    }

    @Test
    @DisplayName("Проверка записи фактической даты дедлайна с простановкой NOT_ACTUAL")
    @DatabaseSetup({
        "/controller/tracker/before/setup_waybill_plan_fact_with_not_actual.xml",
        "/controller/tracker/before/business_process_state.xml",
    })
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_waybill_plan_fact_with_not_actual.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void waybillSegmentFactDateNotActualListener() {
        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
            2L,
            102L,
            2L
        );
        processSegmentCheckpointsConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        ));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("waybillSegmentFactDateWithoutNotActualListenerArguments")
    @DisplayName("Проверка записи фактической даты дедлайна без простановки NOT_ACTUAL")
    @DatabaseSetup({
        "/controller/tracker/before/setup_waybill_plan_fact_with_not_actual.xml",
        "/controller/tracker/before/business_process_state.xml",
    })
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_waybill_plan_fact_without_not_actual.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void waybillSegmentFactDateWithoutNotActualListener(OrderDeliveryCheckpointStatus status) {
        saveUnprocessedStatusToWaybillSegmentStatusHistory(status, 2L);

        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
            2L,
            102L,
            2L
        );
        processSegmentCheckpointsConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        ));

        if (GET_DELIVERY_DATE_PARAM_ENABLED_PARAM_REQUIRED.contains(status)) {
            verify(lmsClient).getPartner(DS_PARTNER_ID);
        }
    }

    @Nonnull
    private static Stream<Arguments> waybillSegmentFactDateWithoutNotActualListenerArguments() {
        return Stream.of(
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_SHOP),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_RECIPIENT),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_DELIVERY),
            Arguments.of(OrderDeliveryCheckpointStatus.SENDER_SENT),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_LOADED),
            Arguments.of(OrderDeliveryCheckpointStatus.SORTING_CENTER_CREATED),
            Arguments.of(OrderDeliveryCheckpointStatus.RETURN_PREPARING)
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Получение статуса отгрузки после возвратного")
    @MethodSource("shipmentCheckpointListenerScanArguments")
    @DatabaseSetup({
        "/controller/tracker/before/setup_returning_shipment.xml",
        "/controller/tracker/before/business_process_state.xml",
    })
    void shipmentCheckpointListenerScan(
        @SuppressWarnings("unused") String name,
        PartnerType partnerType,
        Long partnerAndOrderId,
        OrderDeliveryCheckpointStatus status,
        String orderType
    ) {
        clock.setFixed(Instant.parse("2019-08-06T13:40:00.00Z"), DateTimeUtils.MOSCOW_ZONE);
        saveUnprocessedStatusToWaybillSegmentStatusHistory(status, partnerAndOrderId);
        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
            partnerAndOrderId,
            100L + partnerAndOrderId,
            2
        );
        processSegmentCheckpointsConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        ));

        SegmentStatus segmentStatus = segmentStatusConverter.convertToSegmentStatus(status);

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                "level=ERROR\t" +
                    "format=plain\t" +
                    "code=SHIPMENT_AFTER_CANCELLATION\t" +
                    "payload=Shipment (" + segmentStatus + ") " +
                    "after RETURNING has been detected for order " + partnerAndOrderId + "\t" +
                    "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/2\t" +
                    "entity_types=order,lom_order,partner\t" +
                    "entity_values=order:null,lom_order:" + partnerAndOrderId + ",partner:" + partnerAndOrderId + "\t" +
                    "extra_keys=orderType,orderStatus,partnerType,checkpointStatus,isAfterCancellation\t" +
                    "extra_values=" + orderType + ",RETURNING," + partnerType + "," + segmentStatus + ",true\n"
            );
    }

    @Nonnull
    private static Stream<Arguments> shipmentCheckpointListenerScanArguments() {
        return Stream.of(
            Arguments.of(
                "ДШ - отгрузка",
                PartnerType.DROPSHIP,
                4L,
                OrderDeliveryCheckpointStatus.SORTING_CENTER_TRANSMITTED,
                "Dropship"
            ),
            Arguments.of(
                "ФФ - отгрузка",
                PartnerType.FULFILLMENT,
                2L,
                OrderDeliveryCheckpointStatus.SORTING_CENTER_TRANSMITTED,
                "Fulfillment"
            ),
            Arguments.of(
                "СЦ - приёмка",
                PartnerType.SORTING_CENTER,
                3L,
                OrderDeliveryCheckpointStatus.SORTING_CENTER_AT_START,
                "Dropship-SC"
            ),
            Arguments.of(
                "СЦ - отгрузка",
                PartnerType.SORTING_CENTER,
                3L,
                OrderDeliveryCheckpointStatus.SORTING_CENTER_TRANSMITTED,
                "Dropship-SC"
            ),
            Arguments.of(
                "СД - приёмка",
                PartnerType.DELIVERY,
                1L,
                OrderDeliveryCheckpointStatus.DELIVERY_AT_START,
                "Unknown"
            ),
            Arguments.of(
                "СД - сортировка",
                PartnerType.DELIVERY,
                1L,
                OrderDeliveryCheckpointStatus.DELIVERY_AT_START_SORT,
                "Unknown"
            )
        );
    }

    @Test
    @DisplayName("Обработка получения 50 чекпоинта по доставленному заказу")
    @DatabaseSetup("/controller/tracker/before/order_delivered_waybill_in_transit_pickup.xml")
    @ExpectedDatabase(
        value = "/controller/tracker/after/order_delivered_50_after_processing.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void getDeliveryDeliveredOnDeliveredOrder() throws Exception {
        notifyTracks(
            "controller/tracker/request/get_50_on_delivered_order.json",
            "controller/tracker/response/push_101_ok.json"
        );

        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
            10012L,
            101L
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        );
        processSegmentCheckpointsConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        ));
    }

    @Test
    @DisplayName("Обработка чекпоинта 50 для заказа Yandex Go")
    @DatabaseSetup("/controller/tracker/before/setup_cancellation_requests_yandex_go.xml")
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_delivery_delivered_ok_yandex_go.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processDeliveryDeliveredStatusYandexGo() throws Exception {
        notifyTracks(
            "controller/tracker/request/lo10_order_delivered.json",
            "controller/tracker/response/push_1003_ok.json"
        );

        ProcessSegmentCheckpointsPayload segmentCheckpointsPayload = processSegmentCheckpointsPayloadWithSequence(
            20L,
            1003L
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        );
        processSegmentCheckpointsConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            segmentCheckpointsPayload
        ));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CREATE_TRUST_BASKET,
            PayloadFactory.createOrderIdPayload(10L, 1, 1).setSequenceId(2L)
        );
    }

    private void saveUnprocessedStatusToWaybillSegmentStatusHistory(
        OrderDeliveryCheckpointStatus status,
        Long waybillSegmentId
    ) {
        transactionTemplate.execute(
            wssh -> {
                waybillSegmentStatusHistoryRepository.save(
                    new WaybillSegmentStatusHistory()
                        .setDate(clock.instant())
                        .setTrackerStatus(status.name())
                        .setStatus(segmentStatusConverter.convertToSegmentStatus(status))
                        .setWaybillSegment(waybillSegmentRepository.getOne(waybillSegmentId))
                        .setUnprocessed(true)
                );
                return true;
            }
        );
    }
}
