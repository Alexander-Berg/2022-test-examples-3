package ru.yandex.market.logistics.lom.controller.tracker;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.controller.tracker.dto.DeliveryTrackCheckpoint;
import ru.yandex.market.logistics.lom.dto.queue.LomSegmentCheckpoint;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdDeliveryTrackPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.management.entity.request.schedule.LogisticSegmentInboundScheduleFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.DeliveryType;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus.SORTING_CENTER_AUTOMATICALLY_REMOVED_ITEMS;
import static ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus.SORTING_CENTER_CANCELED;
import static ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus.SORTING_CENTER_OUT_OF_STOCK;
import static ru.yandex.market.logistics.lom.entity.enums.SegmentStatus.CANCELLED;
import static ru.yandex.market.logistics.lom.entity.enums.SegmentStatus.TRANSIT_AUTOMATICALLY_REMOVED_ITEMS;
import static ru.yandex.market.logistics.lom.entity.enums.SegmentStatus.TRANSIT_OUT_OF_STOCK;

@DatabaseSetup("/billing/before/billing_service_products.xml")
@DatabaseSetup("/controller/tracker/before/setup.xml")
class TrackerNotificationChangeRequestFlowTest extends AbstractTrackerNotificationControllerTest {
    private static final LogisticSegmentInboundScheduleFilter INBOUND_SCHEDULE_FILTER =
        new LogisticSegmentInboundScheduleFilter()
            .setFromPartnerId(FF_PARTNER_ID)
            .setToPartnerId(9L)
            .setDeliveryType(DeliveryType.COURIER);

    @Override
    @BeforeEach
    void setUp() {
        super.setUp();
        clock.setFixed(Instant.parse("2019-05-24T04:00:00.00Z"), ZoneId.of("Europe/Moscow"));
    }

    @SneakyThrows
    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @ValueSource(booleans = {true, false})
    @DisplayName("Чекпоинт автоматически обновлённого заказа — партнёр может автоматически обновлять товары заказа")
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_order_items_changed_by_partner_change_request_success.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void orderChangedByPartnerHandle(boolean newCheckpointsFlowEnabled) {
        setCheckpointsProcessingFlow(newCheckpointsFlowEnabled);
        mockLmsClientGetFfPartner(List.of(new PartnerExternalParam("AUTO_ITEM_REMOVING_ENABLED", null, "1")));

        notifyTracks(
            "controller/tracker/request/lo8_order_changed_by_partner.json",
            "controller/tracker/response/push_800_ok.json"
        );
        assertCheckpointsTaskCreatedAndRunTask(
            newCheckpointsFlowEnabled,
            processSegmentCheckpointsPayloadWithSequence(12L, 800L),
            fulfillmentPayload(SORTING_CENTER_AUTOMATICALLY_REMOVED_ITEMS)
        );

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.PROCESS_ORDER_CHANGED_BY_PARTNER_REQUEST);

        assertBillingTransactionQueueTaskCreated(
            TRANSIT_AUTOMATICALLY_REMOVED_ITEMS,
            "SORTING_CENTER_AUTOMATICALLY_REMOVED_ITEMS",
            2,
            1,
            1
        );

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=CREATE_REQUEST\t" +
                "payload=ORDER_CHANGED_BY_PARTNER/1/CREATE_REQUEST/CREATED\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1\t" +
                "tags=UPDATE_ORDER_ITEMS_STATS\t" +
                "extra_keys=requestType,requestId,partnerId,status\t" +
                "extra_values=ORDER_CHANGED_BY_PARTNER,1,145,CREATED\n"
        );

        verify(lmsClient).getPartner(FF_PARTNER_ID);
    }

    @SneakyThrows
    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @ValueSource(booleans = {true, false})
    @DisplayName(
        "Чекпоинт автоматически обновлённого заказа — дропшип партнёр может автоматически обновлять товары заказа, "
            + "значение параметра из партнера, настроек нет"
    )
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_order_items_changed_by_dropship_partner_change_request_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void orderChangedByDropshipPartnerHandleParamFromPartnerNoSettings(boolean newCheckpointsFlow) {
        mockLmsClientGetDropshipPartner(List.of(new PartnerExternalParam("AUTO_ITEM_REMOVING_ENABLED", null, "1")));
        checkAutoItemRemovingParamHandling(newCheckpointsFlow);

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=GET_EXTERNAL_PARAM_VALUE\t" +
                "payload=Checking if AUTO_ITEM_REMOVING_ENABLED param is true\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1\t" +
                "entity_types=order,lom_order\t" +
                "entity_values=order:null,lom_order:7\t" +
                "extra_keys=waybillSegmentId,partnerExternalParamType,partnerId\t" +
                "extra_values=9,AUTO_ITEM_REMOVING_ENABLED,1"
        );

        verify(lmsClient).getPartner(DS_PARTNER_ID);
    }

    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @ValueSource(booleans = {true, false})
    @DisplayName(
        "Чекпоинт автоматически обновлённого заказа — дропшип партнёр может автоматически обновлять товары заказа, "
            + "значение параметра из партнера, настройки без параметра"
    )
    @DatabaseSetup(
        value = "/controller/tracker/before/partner_setting_auto_item_removing_null.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_order_items_changed_by_dropship_partner_change_request_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void orderChangedByDropshipPartnerHandleParamFromPartnerSettingsWithoutParam(boolean newCheckpointsFlow) {
        mockLmsClientGetDropshipPartner(List.of(new PartnerExternalParam("AUTO_ITEM_REMOVING_ENABLED", null, "1")));
        checkAutoItemRemovingParamHandling(newCheckpointsFlow);

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=GET_EXTERNAL_PARAM_VALUE\t" +
                "payload=Checking if AUTO_ITEM_REMOVING_ENABLED param is true\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1\t" +
                "entity_types=order,lom_order\t" +
                "entity_values=order:null,lom_order:7\t" +
                "extra_keys=waybillSegmentId,partnerExternalParamType,partnerId\t" +
                "extra_values=9,AUTO_ITEM_REMOVING_ENABLED,1"
        );

        verify(lmsClient).getPartner(DS_PARTNER_ID);
    }

    @SneakyThrows
    private void checkAutoItemRemovingParamHandling(boolean newCheckpointsFlow) {
        setCheckpointsProcessingFlow(newCheckpointsFlow);
        notifyTracks(
            "controller/tracker/request/dropship_change_request.json",
            "controller/tracker/response/push_700_ok.json"
        );

        assertCheckpointsTaskCreatedAndRunTask(
            newCheckpointsFlow,
            processSegmentCheckpointsPayloadWithSequence(9L, 700L),
            dropshipPayload(SORTING_CENTER_AUTOMATICALLY_REMOVED_ITEMS)
        );

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.PROCESS_ORDER_CHANGED_BY_PARTNER_REQUEST);
    }

    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @ValueSource(booleans = {true, false})
    @DisplayName(
        "Чекпоинт автоматически обновлённого заказа — дропшип партнёр может автоматически обновлять товары заказа, "
            + "значение параметра из настроек"
    )
    @DatabaseSetup(
        value = "/controller/tracker/before/partner_setting_auto_item_removing_enabled.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_order_items_changed_by_dropship_partner_change_request_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void orderChangedByDropshipPartnerHandleParamFromSettings(boolean newCheckpointsFlow) {
        checkAutoItemRemovingParamHandling(newCheckpointsFlow);
        softly.assertThat(backLogCaptor.getResults().toString()).doesNotContain(
            "level=INFO\t" +
                "format=plain\t" +
                "code=GET_EXTERNAL_PARAM_VALUE\t" +
                "payload=Checking if AUTO_ITEM_REMOVING_ENABLED param is true\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1\t" +
                "entity_types=order,lom_order\t" +
                "entity_values=order:null,lom_order:7\t" +
                "extra_keys=waybillSegmentId,partnerExternalParamType,partnerId\t" +
                "extra_values=9,AUTO_ITEM_REMOVING_ENABLED,1"
        );
    }

    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @ValueSource(booleans = {true, false})
    @SneakyThrows
    @DisplayName("Чекпоинт автоматически обновлённого заказа — партнёр не может автоматически обновлять товары заказа")
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_order_items_changed_by_partner_change_request_cancel.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void orderChangedByPartnerHandlePartnerCannotAutoRemoveItems(boolean newCheckpointsFlow) {
        setCheckpointsProcessingFlow(newCheckpointsFlow);
        mockLmsClientGetFfPartner(List.of(new PartnerExternalParam("AUTO_ITEM_REMOVING_ENABLED", null, "0")));

        notifyTracks(
            "controller/tracker/request/lo8_order_changed_by_partner.json",
            "controller/tracker/response/push_800_ok.json"
        );
        assertCheckpointsTaskCreatedAndRunTask(
            newCheckpointsFlow,
            processSegmentCheckpointsPayloadWithSequence(12L, 800L),
            fulfillmentPayload(SORTING_CENTER_AUTOMATICALLY_REMOVED_ITEMS)
        );

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.PROCESS_ORDER_CHANGED_BY_PARTNER_REQUEST);
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CREATE_SEGMENT_CANCELLATION_REQUESTS,
            PayloadFactory.createOrderCancellationRequestIdPayload(1L, "2", 1, 1)
        );
        assertBillingTransactionQueueTaskCreated(
            TRANSIT_AUTOMATICALLY_REMOVED_ITEMS,
            "SORTING_CENTER_AUTOMATICALLY_REMOVED_ITEMS",
            3,
            1,
            2
        );

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=WARN\t" +
                "format=plain\t" +
                "code=UPDATE_ORDER_ITEMS_ERROR\t" +
                "payload=Partner with id 145 can't create request with type ORDER_CHANGED_BY_PARTNER\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1\t" +
                "tags=BUSINESS_ORDER_EVENT\t" +
                "entity_types=order,partner\t" +
                "entity_values=order:8,partner:145"
        );

        verify(lmsClient).getPartner(FF_PARTNER_ID);
    }

    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @ValueSource(booleans = {true, false})
    @SneakyThrows
    @DisplayName("Чекпоинт автоматически обновлённого заказа — заказ в процессе отмены")
    @DatabaseSetup("/controller/tracker/before/order_in_cancellation.xml")
    @ExpectedDatabase(
        value =
            "/controller/tracker/after/push_order_items_changed_by_partner_change_request_order_in_cancellation.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void orderChangedByPartnerHandleOrderInCancellation(boolean newCheckpointsFlow) {
        setCheckpointsProcessingFlow(newCheckpointsFlow);
        notifyTracks(
            "controller/tracker/request/lo8_order_changed_by_partner.json",
            "controller/tracker/response/push_800_ok.json"
        );
        assertCheckpointsTaskCreatedAndRunTask(
            newCheckpointsFlow,
            processSegmentCheckpointsPayloadWithSequence(12L, 800L),
            fulfillmentPayload(SORTING_CENTER_AUTOMATICALLY_REMOVED_ITEMS)
        );

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.PROCESS_ORDER_CHANGED_BY_PARTNER_REQUEST);

        assertBillingTransactionQueueTaskCreated(
            TRANSIT_AUTOMATICALLY_REMOVED_ITEMS,
            "SORTING_CENTER_AUTOMATICALLY_REMOVED_ITEMS",
            2,
            1,
            1
        );
    }

    @SneakyThrows
    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @DisplayName("Чекпоинт заказа с ненайденными товарами — партнёр может обновлять товары заказа")
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_item_not_found_change_request_success.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void orderItemNotFoundHandle(boolean newCheckpointsFlow) {
        when(lmsClient.searchInboundSchedule(refEq(INBOUND_SCHEDULE_FILTER))).thenReturn(
            List.of(
                new ScheduleDayResponse(1L, 1, LocalTime.of(7, 0), LocalTime.of(9, 0)),
                new ScheduleDayResponse(2L, 2, LocalTime.of(8, 0), LocalTime.of(10, 0)),
                new ScheduleDayResponse(3L, 3, LocalTime.of(9, 0), LocalTime.of(11, 0)),
                new ScheduleDayResponse(4L, 4, LocalTime.of(10, 0), LocalTime.of(12, 0)),
                new ScheduleDayResponse(5L, 5, LocalTime.of(12, 0), LocalTime.of(14, 0)),
                new ScheduleDayResponse(6L, 5, LocalTime.of(13, 0), LocalTime.of(15, 0)),
                new ScheduleDayResponse(7L, 6, LocalTime.of(14, 0), LocalTime.of(16, 0)),
                new ScheduleDayResponse(8L, 7, LocalTime.of(15, 0), LocalTime.of(17, 0))
            )
        );
        setCheckpointsProcessingFlow(newCheckpointsFlow);

        notifyTracks(
            "controller/tracker/request/lo8_order_items_not_found.json",
            "controller/tracker/response/push_800_ok.json"
        );
        assertCheckpointsTaskCreatedAndRunTask(
            newCheckpointsFlow,
            processSegmentCheckpointsPayloadWithSequence(12L, 800L),
            fulfillmentPayload(SORTING_CENTER_OUT_OF_STOCK)
        );

        queueTaskChecker.assertQueueTaskCreatedWithDelay(
            QueueType.PROCESS_ORDER_ITEM_NOT_FOUND_REQUEST,
            PayloadFactory.createChangeOrderRequestPayload(1L, "2", 1, 1),
            Duration.of(3, ChronoUnit.HOURS)
        );

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.CREATE_ORDER_ITEM_NOT_FOUND_REQUEST);

        assertBillingTransactionQueueTaskCreated(
            TRANSIT_OUT_OF_STOCK,
            "SORTING_CENTER_OUT_OF_STOCK",
            3,
            1,
            2
        );

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=CREATE_REQUEST\t" +
                "payload=ITEM_NOT_FOUND/1/CREATE_REQUEST/CREATED\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1\t" +
                "tags=UPDATE_ORDER_ITEMS_STATS\t" +
                "extra_keys=requestType,requestId,partnerId,status\t" +
                "extra_values=ITEM_NOT_FOUND,1,145,CREATED\n"
        );

        verify(lmsClient).searchInboundSchedule(refEq(INBOUND_SCHEDULE_FILTER));
    }

    @SneakyThrows
    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @DisplayName("Чекпоинт заказа с ненайденными товарами вместе с чекпоинтом на отмену")
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_item_not_found_change_request_with_cancel_success.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void orderItemNotFoundWithCancelHandle(boolean newCheckpointsFlow) {
        when(lmsClient.searchInboundSchedule(refEq(INBOUND_SCHEDULE_FILTER))).thenReturn(
            List.of(
                new ScheduleDayResponse(1L, 1, LocalTime.of(7, 0), LocalTime.of(9, 0)),
                new ScheduleDayResponse(2L, 2, LocalTime.of(8, 0), LocalTime.of(10, 0)),
                new ScheduleDayResponse(3L, 3, LocalTime.of(9, 0), LocalTime.of(11, 0)),
                new ScheduleDayResponse(4L, 4, LocalTime.of(10, 0), LocalTime.of(12, 0)),
                new ScheduleDayResponse(5L, 5, LocalTime.of(12, 0), LocalTime.of(14, 0)),
                new ScheduleDayResponse(6L, 5, LocalTime.of(13, 0), LocalTime.of(15, 0)),
                new ScheduleDayResponse(7L, 6, LocalTime.of(14, 0), LocalTime.of(16, 0)),
                new ScheduleDayResponse(8L, 7, LocalTime.of(15, 0), LocalTime.of(17, 0))
            )
        );
        setCheckpointsProcessingFlow(newCheckpointsFlow);

        notifyTracks(
            "controller/tracker/request/lo8_order_items_not_found_and_cancelled.json",
            "controller/tracker/response/push_800_ok.json"
        );

        OrderIdDeliveryTrackPayload orderIdDeliveryTrackPayload = orderIdDeliveryTrackPayload(
            8,
            List.of(
                createDeliveryTrackCheckpoint(
                    1L,
                    1565098800000L,
                    SORTING_CENTER_OUT_OF_STOCK
                ),
                createDeliveryTrackCheckpoint(
                    2L,
                    1565098800000L,
                    SORTING_CENTER_CANCELED
                )
            )
        );
        assertCheckpointsTaskCreatedAndRunTask(
            newCheckpointsFlow,
            processSegmentCheckpointsPayloadWithSequence(12L, 800L),
            orderIdDeliveryTrackPayload
        );

        queueTaskChecker.assertQueueTaskCreatedWithDelay(
            QueueType.PROCESS_ORDER_ITEM_NOT_FOUND_REQUEST,
            PayloadFactory.createChangeOrderRequestPayload(1L, "2", 1, 1),
            Duration.of(3, ChronoUnit.HOURS)
        );

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.CREATE_ORDER_ITEM_NOT_FOUND_REQUEST);
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.PROCESS_SEGMENT_CANCELLED);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CREATE_BILLING_TRANSACTION_BY_SEGMENT_STATUSES,
            PayloadFactory.createOrderIdSegmentStatusesPayload(
                8L,
                List.of(
                    LomSegmentCheckpoint.builder()
                        .trackerId(800L)
                        .trackerCheckpointId(2L)
                        .segmentStatus(CANCELLED)
                        .date(Instant.parse("2019-08-06T13:40:00.00Z"))
                        .trackerCheckpointStatus("SORTING_CENTER_CANCELED")
                        .build(),
                    LomSegmentCheckpoint.builder()
                        .trackerId(800L)
                        .trackerCheckpointId(1L)
                        .segmentStatus(TRANSIT_OUT_OF_STOCK)
                        .date(Instant.parse("2019-08-06T13:40:00.00Z"))
                        .trackerCheckpointStatus("SORTING_CENTER_OUT_OF_STOCK")
                        .build()
                ),
                1,
                2
            ),
            3
        );

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=CREATE_REQUEST\t" +
                "payload=ITEM_NOT_FOUND/1/CREATE_REQUEST/CREATED\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1\t" +
                "tags=UPDATE_ORDER_ITEMS_STATS\t" +
                "extra_keys=requestType,requestId,partnerId,status\t" +
                "extra_values=ITEM_NOT_FOUND,1,145,CREATED\n"
        );

        verify(lmsClient).searchInboundSchedule(refEq(INBOUND_SCHEDULE_FILTER));
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @SneakyThrows
    @DisplayName("Чекпоинт заказа с ненайденными товарами, чекпоинт пришёл после катоффа")
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_item_not_found_change_request_success.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void orderItemNotFoundHandleCheckpointAfterCutoff(boolean newCheckpointsFlow) {
        setCheckpointsProcessingFlow(newCheckpointsFlow);
        when(lmsClient.searchInboundSchedule(refEq(INBOUND_SCHEDULE_FILTER))).thenReturn(
            List.of(
                new ScheduleDayResponse(1L, 1, LocalTime.of(7, 0), LocalTime.of(9, 0)),
                new ScheduleDayResponse(2L, 2, LocalTime.of(8, 0), LocalTime.of(10, 0)),
                new ScheduleDayResponse(3L, 3, LocalTime.of(9, 0), LocalTime.of(11, 0)),
                new ScheduleDayResponse(4L, 4, LocalTime.of(10, 0), LocalTime.of(12, 0)),
                new ScheduleDayResponse(5L, 5, LocalTime.of(5, 0), LocalTime.of(7, 0)),
                new ScheduleDayResponse(6L, 5, LocalTime.of(13, 0), LocalTime.of(15, 0)),
                new ScheduleDayResponse(7L, 6, LocalTime.of(14, 0), LocalTime.of(16, 0)),
                new ScheduleDayResponse(8L, 7, LocalTime.of(15, 0), LocalTime.of(17, 0))
            )
        );

        notifyTracks(
            "controller/tracker/request/lo8_order_items_not_found.json",
            "controller/tracker/response/push_800_ok.json"
        );

        assertCheckpointsTaskCreatedAndRunTask(
            newCheckpointsFlow,
            processSegmentCheckpointsPayloadWithSequence(12L, 800L),
            fulfillmentPayload(SORTING_CENTER_OUT_OF_STOCK)
        );

        queueTaskChecker.assertQueueTaskCreatedWithDelay(
            QueueType.PROCESS_ORDER_ITEM_NOT_FOUND_REQUEST,
            PayloadFactory.createChangeOrderRequestPayload(1L, "2", 1, 1),
            Duration.ZERO
        );

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.CREATE_ORDER_ITEM_NOT_FOUND_REQUEST);
        assertBillingTransactionQueueTaskCreated(
            TRANSIT_OUT_OF_STOCK,
            "SORTING_CENTER_OUT_OF_STOCK",
            3,
            1,
            2

        );

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=CREATE_REQUEST\t" +
                "payload=ITEM_NOT_FOUND/1/CREATE_REQUEST/CREATED\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1\t" +
                "tags=UPDATE_ORDER_ITEMS_STATS\t" +
                "extra_keys=requestType,requestId,partnerId,status\t" +
                "extra_values=ITEM_NOT_FOUND,1,145,CREATED\n"
        );

        verify(lmsClient).searchInboundSchedule(refEq(INBOUND_SCHEDULE_FILTER));
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @SneakyThrows
    @DisplayName("Повторный чекпоинт заказа с ненайденными товарами — время катоффа ещё не наступило")
    @DatabaseSetup("/controller/tracker/before/order_item_not_found_change_request.xml")
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_repeated_item_not_found_change_request_before_cutoff.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void repeatedOrderItemNotFoundBeforeCutoffHandle(boolean newCheckpointsFlow) {
        when(lmsClient.searchInboundSchedule(refEq(INBOUND_SCHEDULE_FILTER))).thenReturn(
            List.of(
                new ScheduleDayResponse(1L, 1, LocalTime.of(7, 0), LocalTime.of(9, 0)),
                new ScheduleDayResponse(2L, 2, LocalTime.of(8, 0), LocalTime.of(10, 0)),
                new ScheduleDayResponse(3L, 3, LocalTime.of(9, 0), LocalTime.of(11, 0)),
                new ScheduleDayResponse(4L, 4, LocalTime.of(10, 0), LocalTime.of(12, 0)),
                new ScheduleDayResponse(5L, 5, LocalTime.of(12, 0), LocalTime.of(14, 0)),
                new ScheduleDayResponse(6L, 5, LocalTime.of(13, 0), LocalTime.of(15, 0)),
                new ScheduleDayResponse(7L, 6, LocalTime.of(14, 0), LocalTime.of(16, 0)),
                new ScheduleDayResponse(8L, 7, LocalTime.of(15, 0), LocalTime.of(17, 0))
            )
        );
        setCheckpointsProcessingFlow(newCheckpointsFlow);

        notifyTracks(
            "controller/tracker/request/lo8_order_items_not_found.json",
            "controller/tracker/response/push_800_ok.json"
        );
        assertCheckpointsTaskCreatedAndRunTask(
            newCheckpointsFlow,
            processSegmentCheckpointsPayloadWithSequence(12L, 800L),
            fulfillmentPayload(SORTING_CENTER_OUT_OF_STOCK)
        );

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.PROCESS_ORDER_ITEM_NOT_FOUND_REQUEST);

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.CREATE_ORDER_ITEM_NOT_FOUND_REQUEST);

        assertBillingTransactionQueueTaskCreated(
            TRANSIT_OUT_OF_STOCK,
            "SORTING_CENTER_OUT_OF_STOCK",
            2,
            1,
            1
        );

        verify(lmsClient).searchInboundSchedule(refEq(INBOUND_SCHEDULE_FILTER));
    }

    @SneakyThrows
    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @DisplayName("Повторный чекпоинт заказа с ненайденными товарами — время катоффа уже наступило")
    @DatabaseSetup("/controller/tracker/before/order_item_not_found_change_request.xml")
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_repeated_item_not_found_change_request_after_cutoff.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void repeatedOrderItemNotFoundAfterCutoffHandle(boolean newCheckpointsFlow) {
        when(lmsClient.searchInboundSchedule(refEq(INBOUND_SCHEDULE_FILTER))).thenReturn(
            List.of(
                new ScheduleDayResponse(1L, 1, LocalTime.of(7, 0), LocalTime.of(9, 0)),
                new ScheduleDayResponse(2L, 2, LocalTime.of(8, 0), LocalTime.of(10, 0)),
                new ScheduleDayResponse(3L, 3, LocalTime.of(9, 0), LocalTime.of(11, 0)),
                new ScheduleDayResponse(4L, 4, LocalTime.of(10, 0), LocalTime.of(12, 0)),
                new ScheduleDayResponse(5L, 5, LocalTime.of(5, 0), LocalTime.of(7, 0)),
                new ScheduleDayResponse(6L, 5, LocalTime.of(13, 0), LocalTime.of(15, 0)),
                new ScheduleDayResponse(7L, 6, LocalTime.of(14, 0), LocalTime.of(16, 0)),
                new ScheduleDayResponse(8L, 7, LocalTime.of(15, 0), LocalTime.of(17, 0))
            )
        );
        setCheckpointsProcessingFlow(newCheckpointsFlow);

        notifyTracks(
            "controller/tracker/request/lo8_order_items_not_found.json",
            "controller/tracker/response/push_800_ok.json"
        );
        assertCheckpointsTaskCreatedAndRunTask(
            newCheckpointsFlow,
            processSegmentCheckpointsPayloadWithSequence(12L, 800L),
            fulfillmentPayload(SORTING_CENTER_OUT_OF_STOCK)
        );

        queueTaskChecker.assertQueueTaskCreatedWithDelay(
            QueueType.PROCESS_ORDER_ITEM_NOT_FOUND_REQUEST,
            PayloadFactory.createChangeOrderRequestPayload(2L, "2", 1, 1),
            Duration.ZERO
        );

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.CREATE_ORDER_ITEM_NOT_FOUND_REQUEST);
        assertBillingTransactionQueueTaskCreated(
            TRANSIT_OUT_OF_STOCK,
            "SORTING_CENTER_OUT_OF_STOCK",
            3,
            1,
            2

        );

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=CREATE_REQUEST\t" +
                "payload=ITEM_NOT_FOUND/2/CREATE_REQUEST/CREATED\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1\t" +
                "tags=UPDATE_ORDER_ITEMS_STATS\t" +
                "extra_keys=requestType,requestId,partnerId,status\t" +
                "extra_values=ITEM_NOT_FOUND,2,145,CREATED\n"
        );

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=UPDATE_REQUEST\t" +
                "payload=ITEM_NOT_FOUND/1/UPDATE_REQUEST/CREATED/REJECTED\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1\t" +
                "tags=UPDATE_ORDER_ITEMS_STATS\t" +
                "extra_keys=" +
                "requestType,newStatus,oldStatus,requestId,timeFromCreateToUpdate,timeFromUpdate,partnerId\t" +
                "extra_values=ITEM_NOT_FOUND,REJECTED,CREATED,1,0.0,57600.0,145"
        );

        verify(lmsClient).searchInboundSchedule(refEq(INBOUND_SCHEDULE_FILTER));
    }

    @SneakyThrows
    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @DisplayName("Чекпоинт заказа с ненайденными товарами, у сегмента нет picking cutoff")
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_item_not_found_change_request_cancel.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void orderItemNotFoundHandleNoSchedule(boolean newCheckpointsFlow) {
        setCheckpointsProcessingFlow(newCheckpointsFlow);
        when(lmsClient.searchInboundSchedule(refEq(INBOUND_SCHEDULE_FILTER))).thenReturn(List.of());

        notifyTracks(
            "controller/tracker/request/lo8_order_items_not_found.json",
            "controller/tracker/response/push_800_ok.json"
        );
        assertCheckpointsTaskCreatedAndRunTask(
            newCheckpointsFlow,
            processSegmentCheckpointsPayloadWithSequence(12L, 800L),
            fulfillmentPayload(SORTING_CENTER_OUT_OF_STOCK)
        );

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.CREATE_ORDER_ITEM_NOT_FOUND_REQUEST);
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CREATE_SEGMENT_CANCELLATION_REQUESTS,
            PayloadFactory.createOrderCancellationRequestIdPayload(
                1L,
                "2",
                1,
                1
            )
        );

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.CREATE_ORDER_ITEM_NOT_FOUND_REQUEST);
        assertBillingTransactionQueueTaskCreated(
            TRANSIT_OUT_OF_STOCK,
            "SORTING_CENTER_OUT_OF_STOCK",
            3,
            1,
            2

        );
        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=WARN\t" +
                "format=plain\t" +
                "code=UPDATE_ORDER_ITEMS_ERROR\t" +
                "payload=Waybill segment with id 12 has no picking cutoff time\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1\t" +
                "tags=BUSINESS_ORDER_EVENT\t" +
                "entity_types=order,partner\t" +
                "entity_values=order:8,partner:145"
        );

        verify(lmsClient).searchInboundSchedule(refEq(INBOUND_SCHEDULE_FILTER));
    }

    @SneakyThrows
    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @DisplayName("Чекпоинт заказа с ненайденными товарами — партнёр не может обновлять товары заказа")
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_item_not_found_change_request_cancel.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void orderItemNotFoundHandlePartnerCannotUpdateItems(boolean newCheckpointsFlow) {
        setCheckpointsProcessingFlow(newCheckpointsFlow);
        notifyTracks(
            "controller/tracker/request/lo8_order_items_not_found.json",
            "controller/tracker/response/push_800_ok.json"
        );
        assertCheckpointsTaskCreatedAndRunTask(
            newCheckpointsFlow,
            processSegmentCheckpointsPayloadWithSequence(12L, 800L),
            fulfillmentPayload(SORTING_CENTER_OUT_OF_STOCK)
        );

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.CREATE_ORDER_ITEM_NOT_FOUND_REQUEST);
        assertBillingTransactionQueueTaskCreated(
            TRANSIT_OUT_OF_STOCK,
            "SORTING_CENTER_OUT_OF_STOCK",
            3,
            1,
            2
        );

        verify(lmsClient).searchInboundSchedule(refEq(INBOUND_SCHEDULE_FILTER));
    }

    @SneakyThrows
    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = CHECKPOINTS_FLOW_DISPLAY_NAME)
    @DisplayName("Чекпоинт заказа с ненайденными товарами, заказ в процессе отмены")
    @DatabaseSetup("/controller/tracker/before/order_in_cancellation.xml")
    @ExpectedDatabase(
        value = "/controller/tracker/after/push_item_not_found_change_request_order_in_cancellation.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void orderItemNotFoundHandleOrderInCancellation(boolean newCheckpointsFlow) {
        setCheckpointsProcessingFlow(newCheckpointsFlow);
        notifyTracks(
            "controller/tracker/request/lo8_order_items_not_found.json",
            "controller/tracker/response/push_800_ok.json"
        );
        assertCheckpointsTaskCreatedAndRunTask(
            newCheckpointsFlow,
            processSegmentCheckpointsPayloadWithSequence(12L, 800L),
            fulfillmentPayload(SORTING_CENTER_OUT_OF_STOCK)
        );

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.CREATE_ORDER_ITEM_NOT_FOUND_REQUEST);
        assertBillingTransactionQueueTaskCreated(
            TRANSIT_OUT_OF_STOCK,
            "SORTING_CENTER_OUT_OF_STOCK",
            2,
            1,
            1
        );
    }

    @Nonnull
    private static OrderIdDeliveryTrackPayload dropshipPayload(OrderDeliveryCheckpointStatus status) {
        return orderIdDeliveryTrackPayload(7, status);
    }

    @Nonnull
    private static OrderIdDeliveryTrackPayload fulfillmentPayload(OrderDeliveryCheckpointStatus status) {
        return orderIdDeliveryTrackPayload(8, status);
    }

    @Nonnull
    private static OrderIdDeliveryTrackPayload orderIdDeliveryTrackPayload(
        long orderId,
        OrderDeliveryCheckpointStatus status
    ) {
        return orderIdDeliveryTrackPayload(
            orderId,
            List.of(
                createDeliveryTrackCheckpoint(
                    1,
                    1565098800000L,
                    status
                )
            )
        );
    }

    @Nonnull
    private static OrderIdDeliveryTrackPayload orderIdDeliveryTrackPayload(
        long orderId,
        List<DeliveryTrackCheckpoint> checkpoints
    ) {
        return PayloadFactory.createOrderIdDeliveryTrackPayload(
            orderId,
            createDeliveryTrack(
                checkpoints,
                "LO" + orderId,
                orderId * 100,
                "1807474",
                1565092800000L
            ),
            "1",
            1
        );
    }
}
