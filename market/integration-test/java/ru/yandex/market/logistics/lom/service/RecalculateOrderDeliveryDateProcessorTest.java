package ru.yandex.market.logistics.lom.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;

import yandex.market.combinator.common.Common;
import yandex.market.combinator.v0.CombinatorOuterClass;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.AbstractContextualYdbTest;
import ru.yandex.market.logistics.lom.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.lom.entity.combinator.embedded.CombinedRoute;
import ru.yandex.market.logistics.lom.jobs.consumer.ProcessRecalculateOrderDateConsumer;
import ru.yandex.market.logistics.lom.jobs.exception.DbQueueJobExecutionException;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResultStatus;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.RecalculateOrderDeliveryDateProcessor;
import ru.yandex.market.logistics.lom.model.enums.ServiceCodeName;
import ru.yandex.market.logistics.lom.repository.ydb.OrderCombinedRouteHistoryYdbRepository;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateStatusHistoryTableDescription;
import ru.yandex.market.logistics.lom.repository.ydb.description.OrderCombinedRouteHistoryTableDescription;
import ru.yandex.market.logistics.lom.service.order.combinator.CombinatorGrpcClient;
import ru.yandex.market.logistics.lom.utils.UuidGenerator;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.market.logistics.lom.utils.ydb.converter.OrderCombinedRouteHistoryYdbConverter;
import ru.yandex.market.logistics.management.entity.type.LogisticSegmentType;
import ru.yandex.market.ydb.integration.YdbTableDescription;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.entity.enums.UpdateDeliveryDateAfterRecalculationAvailability.ALL;
import static ru.yandex.market.logistics.lom.entity.enums.UpdateDeliveryDateAfterRecalculationAvailability.DELAY_OUT_ON_FIRST_SEGMENT;
import static ru.yandex.market.logistics.lom.entity.enums.UpdateDeliveryDateAfterRecalculationAvailability.NONE;
import static ru.yandex.market.logistics.lom.jobs.model.QueueType.CHANGE_ORDER_REQUEST;
import static ru.yandex.market.logistics.lom.jobs.model.QueueType.EXPORT_ORDER_FROM_SHIPMENT_EXCLUSION_FINISHED;
import static ru.yandex.market.logistics.lom.utils.TskvUtils.CODE_KEY;
import static ru.yandex.market.logistics.lom.utils.TskvUtils.LEVEL_KEY;
import static ru.yandex.market.logistics.lom.utils.TskvUtils.PAYLOAD_KEY;
import static ru.yandex.market.logistics.lom.utils.TskvUtils.tskvLogToMap;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DisplayName("Тест на обработку заявки на пересчёт даты в Комбинаторе")
@ParametersAreNonnullByDefault
@DatabaseSetup("/orders/recalculate_delivery_date/order_with_combined_route.xml")
class RecalculateOrderDeliveryDateProcessorTest extends AbstractContextualYdbTest {

    private static final int FF_SEGMENT_ID = 612909;
    private static final long DS_SEGMENT_ID = 614085L;
    private static final UUID EXISTING_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID NEW_UUID = UUID.fromString("92b2a721-8e98-4b4a-8f86-a045d570e036");

    @Autowired
    private RecalculateOrderDeliveryDateProcessor processor;

    @Autowired
    private CombinatorGrpcClient combinatorGrpcClient;

    @Autowired
    private OrderCombinedRouteHistoryTableDescription routeHistoryTable;

    @Autowired
    private BusinessProcessStateStatusHistoryTableDescription businessProcessStateStatusHistoryTableDescription;

    @Autowired
    private OrderCombinedRouteHistoryYdbConverter converter;

    @Autowired
    private FeatureProperties featureProperties;

    @Autowired
    private UuidGenerator uuidGenerator;

    @Autowired
    private TestableClock clock;

    @Autowired
    private OrderCombinedRouteHistoryYdbRepository orderCombinedRouteHistoryYdbRepository;

    @Autowired
    private ProcessRecalculateOrderDateConsumer consumer;
    @Captor
    private ArgumentCaptor<CombinatorOuterClass.RecalculationRequest> requestArgumentCaptor;

    private static final ChangeOrderRequestPayload CHANGE_ORDER_REQUEST_PAYLOAD =
        PayloadFactory.createChangeOrderRequestPayload(1, null);
    private static final int START_TIME = 1629361555;
    private static final int FROM_INTERVAL = 12;
    private static final int TO_INTERVAL = 14;

    @BeforeEach
    void setUp() {
        doReturn(NEW_UUID).when(uuidGenerator).randomUuid();
        clock.setFixed(Instant.parse("2021-09-22T12:30:00Z"), DateTimeUtils.MOSCOW_ZONE);
    }

    @AfterEach
    void tearDown() {
        featureProperties.setUpdateDeliveryDateAfterRecalculationAvailability(NONE);
        featureProperties.setUpdateRouteIfDeliveryIntervalDidNotChange(false);
        featureProperties.setUpdateSegmentShipmentDates(false);
        verifyNoMoreInteractions(combinatorGrpcClient, orderCombinedRouteHistoryYdbRepository);
    }

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(routeHistoryTable, businessProcessStateStatusHistoryTableDescription);
    }

    @Test
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/recalc_date_change_payload_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешная обработка заявки")
    void testProcessPayloadSuccess() {
        processSuccess();
        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(EXISTING_UUID);
        verifyRecalculatingStarted();
    }

    @Test
    @DatabaseSetup(
        value = "/orders/recalculate_delivery_date/order_without_delivery_interval.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/recalc_date_change_payload_created_without_interval.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешная обработка заявки - в заказе нет интервала доставки")
    void testProcessPayloadSuccessWithoutDeliveryInterval() {
        processSuccess();

        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(EXISTING_UUID);
        verifyRecalculatingStarted();
    }

    @Test
    @DatabaseSetup(
        value = "/orders/recalculate_delivery_date/recalc_date_change_req_with_status.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/recalc_date_with_status_change_payload_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешная обработка заявки со статусом сегмента")
    void testProcessPayloadSuccessWithStatus() {
        processSuccess();

        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(EXISTING_UUID);
        verifyRecalculatingStarted();
    }

    @Test
    @DatabaseSetup(
        value = "/orders/recalculate_delivery_date/recalc_date_change_110_cp.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/recalc_date_change_payload_created_110_cp.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешная обработка заявки после опоздания 110 чп")
    void testProcessPayloadSuccess110() {
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("orders/recalculate_delivery_date/combined_route_1001.json")),
            converter::mapToItem
        );

        CombinatorOuterClass.RecalculationResponse recalculationResponse = buildResponse(buildPoint(
            deliveryServiceBuilder().setCode("INBOUND").build()
        ));
        doReturn(recalculationResponse).when(combinatorGrpcClient).recalculateRoute(any());

        softly.assertThat(processor.processPayload(CHANGE_ORDER_REQUEST_PAYLOAD).getStatus())
            .isEqualTo(ProcessingResultStatus.SUCCESS);

        verifyRecalculateRoute(ServiceCodeName.INBOUND.name());
        verifySaveRouteToYdb("orders/recalculate_delivery_date/route_1001_inbound.json");
        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(EXISTING_UUID);
        verifyRecalculatingStarted();
    }

    @Test
    @DatabaseSetup(
        value = {
            "/orders/recalculate_delivery_date/recalc_date_change_two_points_on_one_segment.xml",
            "/orders/recalculate_delivery_date/recalc_date_change_110_cp.xml"
        },
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/recalc_date_change_payload_created_110_cp.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешная обработка заявки если у одного waybill сегмента несколько комбинаторных точек")
    void testProcessPayloadSuccessTwoCombinatorPointsInSegment() {
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("orders/recalculate_delivery_date/combined_route_1001.json")),
            converter::mapToItem
        );

        CombinatorOuterClass.RecalculationResponse recalculationResponse = buildResponse(buildPoint(
            deliveryServiceBuilder().setCode("INBOUND").build()
        ));
        doReturn(recalculationResponse).when(combinatorGrpcClient).recalculateRoute(any());

        softly.assertThat(processor.processPayload(CHANGE_ORDER_REQUEST_PAYLOAD).getStatus())
            .isEqualTo(ProcessingResultStatus.SUCCESS);

        verifyRecalculateRoute(ServiceCodeName.INBOUND.name());
        verifySaveRouteToYdb("orders/recalculate_delivery_date/route_1001_inbound.json");
        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(EXISTING_UUID);
        verifyRecalculatingStarted();
    }

    @Test
    @DatabaseSetup(
        value = "/orders/recalculate_delivery_date/recalc_date_change_invalid_cp.xml",
        type = DatabaseOperation.UPDATE
    )
    @DisplayName("Ошибка обработки заявки - невалидный статус сегмента")
    void testProcessPayloadSuccessInvalidCp() {
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("orders/recalculate_delivery_date/combined_route_1001.json")),
            converter::mapToItem
        );

        testProcessFail("Unexpected value: STARTED");
    }

    @Test
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/recalc_date_change_payload_created_pickup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешная обработка заявки с типом сегмента PICKUP")
    void testProcessPayloadSuccessPickup() {
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("orders/recalculate_delivery_date/combined_route_1001_pickup.json")),
            converter::mapToItem
        );

        CombinatorOuterClass.RecalculationResponse buildResponse = buildResponse(
            CombinatorOuterClass.Route.Point.newBuilder().setSegmentType("pickup").build()
        );

        testProcessSuccess(buildResponse, "orders/recalculate_delivery_date/route_1001_pickup.json");

        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(EXISTING_UUID);
        verifyRecalculatingStarted();
        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=RECALCULATE_DELIVERY_DATE_FOR_PICKUP_ROUTE\t" +
                "payload=Processing pickup route\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "entity_types=changeOrderRequest\t" +
                "entity_values=changeOrderRequest:1"
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/recalc_date_change_payload_created_pickup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешная обработка заявки с типом сегмента PICKUP и handing сервисом")
    void testProcessPayloadSuccessPickupWithHanding() {
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("orders/recalculate_delivery_date/combined_route_1001_pickup.json")),
            converter::mapToItem
        );

        CombinatorOuterClass.RecalculationResponse buildResponse = buildResponse(
            CombinatorOuterClass.Route.Point.newBuilder().setSegmentType("pickup")
                .addServices(deliveryServiceBuilder().clearDeliveryIntervals().build()).build()
        );

        testProcessSuccess(
            buildResponse,
            "orders/recalculate_delivery_date/route_1001_pickup_with_handing.json"
        );

        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(EXISTING_UUID);
        verifyRecalculatingStarted();
        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=RECALCULATE_DELIVERY_DATE_FOR_PICKUP_ROUTE\t" +
                "payload=Processing pickup route\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "entity_types=changeOrderRequest\t" +
                "entity_values=changeOrderRequest:1"
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/recalc_date_change_payload_created_without_route_saving.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешная обработка заявки, маршрут не сохраняется, так как выключен флаг сохранения маршрута")
    void testProcessPayloadSuccessWithoutRouteSaving() {
        processSuccess();
        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(EXISTING_UUID);
        verifyRecalculatingStarted();
    }

    @Test
    @DatabaseSetup(
        value = "/orders/recalculate_delivery_date/no_combinator_segment_ids_in_ff_waybill_segment.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/recalc_date_change_payload_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешная обработка заявки - отсутствуют combinator_segment_ids в вейбилл сегменте, берём их из рута")
    public void testProcessPayloadNoCombinatorSegmentIdsInFfWaybillSegmentSuccess() {
        processSuccess();
        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(EXISTING_UUID);
        verifyRecalculatingStarted();
    }

    @Test
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/fail_change_request_remains_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Неуспешная обработка заявки - некорректный ответ")
    void testProcessPayloadWrongResponseFail() {
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("orders/recalculate_delivery_date/combined_route_1001.json")),
            converter::mapToItem
        );
        CombinatorOuterClass.Date deliveryDate = buildDate(19);
        CombinatorOuterClass.RecalculationResponse responseWithoutHanding =
            CombinatorOuterClass.RecalculationResponse.newBuilder()
                .setRoute(buildRoute(deliveryDate, deliveryDate))
                .build();

        doReturn(responseWithoutHanding).when(combinatorGrpcClient).recalculateRoute(any());
        testProcessFail("No last mile point found in recalculated route for change request 1");
        verifyRecalculateRoute(ServiceCodeName.SHIPMENT.name());
        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(EXISTING_UUID);
        verify(orderCombinedRouteHistoryYdbRepository).saveRoute(any(), any());
        verifyRecalculatingStarted();
        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=ERROR\t" +
                "format=plain\t" +
                "code=RECALCULATE_DELIVERY_DATE_NO_LAST_MILE_SEGMENT\t" +
                "payload=No last mile point found in recalculated route\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "entity_types=changeOrderRequest\t" +
                "entity_values=changeOrderRequest:1\t" +
                "extra_keys=route\t"
        );
    }

    @Test
    @DisplayName("Неуспешная обработка заявки - не найдена заявка")
    @DatabaseSetup(
        value = "/orders/recalculate_delivery_date/drop_change_request_payload.xml",
        type = DatabaseOperation.DELETE
    )
    @DatabaseSetup(
        value = "/orders/recalculate_delivery_date/drop_change_request.xml",
        type = DatabaseOperation.DELETE
    )
    void testProcessPayloadNoChangeRequestFail() {
        testProcessFail("Failed to find [ORDER_CHANGE_REQUEST] with id [1]");
    }

    @Test
    @DatabaseSetup(
        value = "/orders/recalculate_delivery_date/change_req_wrong_status.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/fail_change_request_remains_info_received.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Неуспешная обработка заявки - заявка в некорректном статусе")
    void testProcessPayloadChangeRequestInWrongStatusFail() {
        testProcessFail("Cannot process change request with status INFO_RECEIVED. Processable status is CREATED");
        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=ERROR\t" +
                "format=plain\t" +
                "code=RECALCULATE_DELIVERY_DATE_INVALID_REQUEST_STATUS\t" +
                "payload=Cannot process change request with required status\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "entity_types=order,lom_order\t" +
                "entity_values=order:1002,lom_order:1001\t" +
                "extra_keys=actualStatus,requiredStatus,changeOrderRequest\t" +
                "extra_values=INFO_RECEIVED,CREATED,1"
        );
    }

    @Test
    @DatabaseSetup(
        value = "/orders/recalculate_delivery_date/payload_wrong_status.xml",
        type = DatabaseOperation.UPDATE
    )
    @DisplayName("Неуспешная обработка заявки - пейлоад заявки в некорректном статусе")
    void testProcessPayloadPayloadInWrongStatusFail() {
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("orders/recalculate_delivery_date/combined_route_1001.json")),
            converter::mapToItem
        );
        testProcessFail("No available payload for change request 1 in status CREATED");
        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=ERROR\t" +
                "format=plain\t" +
                "code=RECALCULATE_DELIVERY_DATE_INVALID_PAYLOAD_STATUS\t" +
                "payload=Failed to get recalculateOrderDeliveryDatePayload in status CREATED\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "entity_types=order,lom_order,changeOrderRequest\t" +
                "entity_values=order:1002,lom_order:1001,changeOrderRequest:1"
        );
    }

    @Test
    @DatabaseSetup(
        value = "/orders/recalculate_delivery_date/no_combined_route.xml",
        type = DatabaseOperation.UPDATE
    )
    @DisplayName("Неуспешная обработка заявки - не найден комбинированный маршрут заказа")
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/fail_change_request_remains_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void testProcessPayloadNoCombinedRouteFail() {
        testProcessFail("No route for order 1001");
    }

    @Test
    @DatabaseSetup(value = "/orders/recalculate_delivery_date/no_recipient_geo_id.xml", type = DatabaseOperation.UPDATE)
    @DisplayName("Неуспешная обработка заявки - не найден geo id локации получателя заказа")
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/fail_change_request_remains_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void testProcessPayloadNoRecipientGeoIdFail() {
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("orders/recalculate_delivery_date/combined_route_1001.json")),
            converter::mapToItem
        );
        testProcessFail("No recipient geo id found on order 1001");
        queueTaskChecker.assertNoQueueTasksCreated();
        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(EXISTING_UUID);
    }

    @Test
    @DisplayName("Неуспешная обработка заявки - Комбинатор ответил с ошибкой")
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/fail_change_request_remains_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void testProcessPayloadRequestFail() {
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("orders/recalculate_delivery_date/combined_route_1001.json")),
            converter::mapToItem
        );
        when(combinatorGrpcClient.recalculateRoute(any())).thenThrow(RuntimeException.class);
        testProcessFail(null);
        verifyRecalculateRoute(ServiceCodeName.SHIPMENT.name());
        queueTaskChecker.assertNoQueueTasksCreated();
        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(EXISTING_UUID);

        softly.assertThat(backLogCaptor.getResults()).anyMatch(log -> {
            Map<String, String> tskvMap = tskvLogToMap(log);
            return "RECALCULATE_DELIVERY_DATE".equals(tskvMap.get(CODE_KEY))
                && Optional.ofNullable(tskvMap.get(PAYLOAD_KEY))
                .map(payload -> payload.contains("Cannot get route from combinator"))
                .orElse(false);
        });
    }

    @Test
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/recalc_date_change_payload_info_received.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName(
        "Обработка заявки с флагом обновления пересчитанной комбинатором даты доставки - таска на обновление создалась"
    )
    void successWithRecalculateOrderDeliveryDateTask() {
        featureProperties.setUpdateDeliveryDateAfterRecalculationAvailability(ALL);

        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("orders/recalculate_delivery_date/combined_route_1001.json")),
            converter::mapToItem
        );
        testProcessSuccess();
        queueTaskChecker.assertQueueTaskCreated(
            CHANGE_ORDER_REQUEST,
            PayloadFactory.createChangeOrderRequestPayload(1, "1", 1)
        );
        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(EXISTING_UUID);
        verifyRecalculatingStarted();
        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=CHANGE_ORDER_REQUEST\t" +
                "payload=Successfully added new task with payload = " +
                "{\\\"requestId\\\":\\\"1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1\\\"," +
                "\\\"changeOrderRequestId\\\":1,\\\"sequenceId\\\":1} " +
                "and id = 1 into queue\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=DB_QUEUE\t" +
                "extra_keys=status\t" +
                "extra_values=NEW"
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/fail_change_request_remains_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Неуспешная обработка заявки - ошибка при сохранении маршрута")
    void failWithRouteSavingError() {
        doReturn(buildResponse()).when(combinatorGrpcClient).recalculateRoute(any());
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("orders/recalculate_delivery_date/combined_route_1001.json")),
            converter::mapToItem
        );
        doThrow(new RuntimeException("ERROR")).when(orderCombinedRouteHistoryYdbRepository).saveRoute(any(), any());

        testProcessFail("java.lang.RuntimeException: ERROR");

        verifyRecalculateRoute(ServiceCodeName.SHIPMENT.name());
        verifySaveRouteToYdb("orders/recalculate_delivery_date/route_1001.json");
        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(EXISTING_UUID);
        queueTaskChecker.assertNoQueueTasksCreated();
        verifyRecalculatingStarted();
    }

    @Test
    @DatabaseSetup(
        value = "/orders/recalculate_delivery_date/recalc_date_change_req_for_delivery.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/recalc_date_change_payload_fail_for_delivery.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName(
        "Обработка заявки с флагом обновления пересчитанной комбинатором даты доставки "
            + "- таска на обновление не создалась тк пересчет доступен только по опозданию на первом сегменте, "
            + "а пересчет на DELIVERY сегменте"
    )
    void failWithRecalculateOrderDeliveryDateTaskDelayOnFirstSegmentTagNotFFSegment() {
        featureProperties.setUpdateDeliveryDateAfterRecalculationAvailability(DELAY_OUT_ON_FIRST_SEGMENT);

        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("orders/recalculate_delivery_date/combined_route_1001.json")),
            converter::mapToItem
        );
        testProcessSuccess(614085L);
        queueTaskChecker.assertNoQueueTasksCreated();
        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(EXISTING_UUID);
        verifyRecalculatingStarted();
    }

    @Test
    @DatabaseSetup(
        value = "/orders/recalculate_delivery_date/update_order_delivery_date.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/recalc_date_change_payload_fail_invalid_date.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName(
        "Обработка заявки с флагом обновления пересчитанной комбинатором даты доставки "
            + "- таска на обновление не создалась - пересчитанная дата меньше плановой"
    )
    void failWithRecalculateOrderDeliveryDateTaskInvalidDate() {
        featureProperties.setUpdateDeliveryDateAfterRecalculationAvailability(ALL);
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("orders/recalculate_delivery_date/combined_route_1001.json")),
            converter::mapToItem
        );
        testProcessSuccess();
        queueTaskChecker.assertNoQueueTasksCreated();
        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(EXISTING_UUID);
        verifyRecalculatingStarted();
    }

    @Test
    @DatabaseSetup(
        value = "/orders/recalculate_delivery_date/update_order_delivery_dates_and_interval.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/recalc_date_change_payload_dates_and_interval_did_not_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName(
        "Обработка заявки с флагом обновления пересчитанной комбинатором даты доставки "
            + "- таска на обновление не создалась - пересчитанная дата совпадает с исходной"
    )
    void failWithRecalculateOrderDeliveryDateTaskSameDatesAndInterval() {
        featureProperties.setUpdateDeliveryDateAfterRecalculationAvailability(ALL);
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("orders/recalculate_delivery_date/combined_route_1001.json")),
            converter::mapToItem
        );
        testProcessSuccess();
        queueTaskChecker.assertNoQueueTasksCreated();
        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(EXISTING_UUID);
        verifyRecalculatingStarted();
    }

    @Test
    @DatabaseSetup(
        value = "/orders/recalculate_delivery_date/update_order_delivery_dates_and_interval.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/orders/recalculate_delivery_date/has_exclusion_request.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/dates_and_interval_did_not_change_les_notified.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName(
        "Обработка заявки с флагом обновления пересчитанной комбинатором даты доставки "
            + "- таска на обновление не создалась - пересчитанная дата совпадает с исходной, нужно сообщить в LES"
    )
    void failWithRecalculateOrderDeliveryDateTaskSameDatesAndIntervalSendEventToLes() {
        featureProperties.setUpdateDeliveryDateAfterRecalculationAvailability(ALL);
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("orders/recalculate_delivery_date/combined_route_1001.json")),
            converter::mapToItem
        );
        testProcessSuccess();
        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(EXISTING_UUID);
        verifyRecalculatingStarted();
        queueTaskChecker.assertQueueTaskCreated(
            EXPORT_ORDER_FROM_SHIPMENT_EXCLUSION_FINISHED,
            PayloadFactory.exportOrderFromShipmentExclusionFinishedPayload(1L, 1001L, 101L, 1L, "1", 1L)
        );
    }

    @Test
    @DatabaseSetup(
        value = "/orders/recalculate_delivery_date/processing_delayed_by_partner_reason.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/orders/recalculate_delivery_date/has_exclusion_request.xml",
        type = DatabaseOperation.REFRESH
    )
    @DatabaseSetup(
        value = "/orders/recalculate_delivery_date/update_order_delivery_dates_and_interval.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/recalc_date_update_route_dates_and_interval_did_not_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName(
        "Обработка заявки с флагом обновления пересчитанной комбинатором даты доставки "
            + "- таска на обновление не создалась - пересчитанная дата совпадает с исходной, нужно обновить маршрут"
    )
    void successWithRecalculateOrderDeliveryDateTaskSameDatesAndInterval() {
        featureProperties.setUpdateRouteIfDeliveryIntervalDidNotChange(true);
        featureProperties.setUpdateSegmentShipmentDates(true);
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("orders/recalculate_delivery_date/combined_route_1001.json")),
            converter::mapToItem
        );
        testProcessSuccess();
        queueTaskChecker.assertQueueTaskCreated(
            EXPORT_ORDER_FROM_SHIPMENT_EXCLUSION_FINISHED,
            PayloadFactory.exportOrderFromShipmentExclusionFinishedPayload(1L, 1001L, 101L, 1L, "1", 1L)
        );
        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(EXISTING_UUID);
        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(eq(NEW_UUID));
        verifyRecalculatingStarted();
    }

    @Test
    @DatabaseSetup("/orders/recalculate_delivery_date/on_demand.xml")
    @DatabaseSetup(
        value = "/orders/recalculate_delivery_date/update_order_delivery_dates.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/recalc_date_change_payload_dates_did_not_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName(
        "Обработка заявки с флагом обновления пересчитанной комбинатором даты доставки "
            + "- таска на обновление не создалась - интервал отличается, но заказ OnDemand"
    )
    void failOnDemandOrderSameDatesAndIntervalChanged() {
        featureProperties.setUpdateDeliveryDateAfterRecalculationAvailability(ALL);
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("orders/recalculate_delivery_date/combined_route_1001.json")),
            converter::mapToItem
        );
        testProcessSuccess();
        queueTaskChecker.assertNoQueueTasksCreated();
        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(EXISTING_UUID);
        verifyRecalculatingStarted();
    }

    @Test
    @DatabaseSetup("/orders/recalculate_delivery_date/on_demand.xml")
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/recalc_date_change_payload_on_demand_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName(
        "Обработка заявки с флагом обновления пересчитанной комбинатором даты доставки "
            + "- таска на обновление создалась - заказ OnDemand, даты обновились, интервал null"
    )
    void successOnDemandOrderDeliveryDatesChanged() {
        featureProperties.setUpdateDeliveryDateAfterRecalculationAvailability(ALL);
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("orders/recalculate_delivery_date/combined_route_1001.json")),
            converter::mapToItem
        );
        testProcessSuccess();
        queueTaskChecker.assertQueueTaskCreated(
            CHANGE_ORDER_REQUEST,
            PayloadFactory.createChangeOrderRequestPayload(1, "1", 1)
        );
        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(EXISTING_UUID);
        verifyRecalculatingStarted();
        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=CHANGE_ORDER_REQUEST\t" +
                "payload=Successfully added new task with payload = " +
                "{\\\"requestId\\\":\\\"1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1\\\"," +
                "\\\"changeOrderRequestId\\\":1,\\\"sequenceId\\\":1} " +
                "and id = 1 into queue\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=DB_QUEUE\t" +
                "extra_keys=status\t" +
                "extra_values=NEW"
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/recalc_date_change_payload_info_received.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName(
        "Успешная обработка заявки с флагом обновления пересчитанной комбинатором даты доставки "
            + "- таска на обновление создалась тк пересчет доступен только по опозданию на первом сегменте, "
            + "а пересчет на первом FULFILLMENT сегменте"
    )
    void successWithRecalculateOrderDeliveryDateTaskDelayOnFirstSegmentTagFFSegment() {
        featureProperties.setUpdateDeliveryDateAfterRecalculationAvailability(DELAY_OUT_ON_FIRST_SEGMENT);

        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("orders/recalculate_delivery_date/combined_route_1001.json")),
            converter::mapToItem
        );
        testProcessSuccess();
        queueTaskChecker.assertQueueTaskCreated(
            CHANGE_ORDER_REQUEST,
            PayloadFactory.createChangeOrderRequestPayload(1, "1", 1)
        );
        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(EXISTING_UUID);
    }

    @Test
    @DatabaseSetup(value = "/orders/recalculate_delivery_date/dropship_sc_order.xml", type = DatabaseOperation.UPDATE)
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/recalc_date_change_payload_info_received.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName(
        "Успешная обработка заявки с флагом обновления пересчитанной комбинатором даты доставки "
            + "- таска на обновление создалась тк пересчет доступен только по опозданию на первом сегменте, "
            + "а пересчет на первом DROPSHIP сегменте"
    )
    void successWithRecalculateOrderDeliveryDateTaskDelayOnFirstSegmentTagDropshipSegment() {
        featureProperties.setUpdateDeliveryDateAfterRecalculationAvailability(DELAY_OUT_ON_FIRST_SEGMENT);

        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("orders/recalculate_delivery_date/combined_route_1001.json")),
            converter::mapToItem
        );
        testProcessSuccess();
        queueTaskChecker.assertQueueTaskCreated(
            CHANGE_ORDER_REQUEST,
            PayloadFactory.createChangeOrderRequestPayload(1, "1", 1)
        );

        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(EXISTING_UUID);
    }

    @Test
    @DatabaseSetup(
        value = {
            "/orders/recalculate_delivery_date/update_change_request_waybill_segment.xml",
            "/orders/recalculate_delivery_date/dropship_sc_order.xml",
        },
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/recalc_date_change_payload_info_received_dropship_to_sc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успех, если пересчет со статуса IN на первом СЦ сегменте после DROPSHIP")
    void recalculationRejectedFirstScSegment() {
        featureProperties.setUpdateDeliveryDateAfterRecalculationAvailability(DELAY_OUT_ON_FIRST_SEGMENT);

        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("orders/recalculate_delivery_date/combined_route_1001.json")),
            converter::mapToItem
        );

        testProcessSuccess(
            buildResponse(),
            "orders/recalculate_delivery_date/route_1001.json",
            614085,
            ServiceCodeName.INBOUND
        );
        queueTaskChecker.assertQueueTaskCreated(
            CHANGE_ORDER_REQUEST,
            PayloadFactory.createChangeOrderRequestPayload(1, "1", 1)
        );

        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(EXISTING_UUID);
    }

    @Test
    @DatabaseSetup(
        value = {
            "/orders/recalculate_delivery_date/update_change_request_waybill_segment.xml",
            "/orders/recalculate_delivery_date/dropship_dropoff_order.xml",
        },
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/recalc_date_change_payload_info_received_dropship_to_sc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успех, если пересчет со статуса IN на ДО сегменте после DROPSHIP")
    void recalculationDsToDoSuccess() {
        featureProperties.setUpdateDeliveryDateAfterRecalculationAvailability(DELAY_OUT_ON_FIRST_SEGMENT);

        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("orders/recalculate_delivery_date/combined_route_1001.json")),
            converter::mapToItem
        );

        testProcessSuccess(
            buildResponse(),
            "orders/recalculate_delivery_date/route_1001.json",
            614085,
            ServiceCodeName.INBOUND
        );
        queueTaskChecker.assertQueueTaskCreated(
            CHANGE_ORDER_REQUEST,
            PayloadFactory.createChangeOrderRequestPayload(1, "1", 1)
        );

        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(EXISTING_UUID);
    }

    @Test
    @DatabaseSetup(
        value = {
            "/orders/recalculate_delivery_date/update_change_request_second_sc.xml",
            "/orders/recalculate_delivery_date/dropship_sc_order.xml",
        },
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/recalc_date_change_payload_fail_sorting_center.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName(
        "Заявка отклонена, так как пересчет доступен только по опозданию на первом сегменте "
            + "(IN на втором СЦ сегменте)"
    )
    void recalculationRejectedSecondScSegment() {
        featureProperties.setUpdateDeliveryDateAfterRecalculationAvailability(DELAY_OUT_ON_FIRST_SEGMENT);

        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("orders/recalculate_delivery_date/combined_route_1001.json")),
            converter::mapToItem
        );

        testProcessSuccess(
            buildResponse(),
            "orders/recalculate_delivery_date/route_1001.json",
            703279,
            ServiceCodeName.INBOUND
        );
        queueTaskChecker.assertNoQueueTasksCreated();
        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(EXISTING_UUID);
    }

    @Test
    @DatabaseSetup(
        value = {
            "/orders/recalculate_delivery_date/dropship_order.xml",
            "/orders/recalculate_delivery_date/update_change_request_waybill_segment.xml",
        },
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/recalc_date_change_payload_fail_sorting_center.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName(
        "Успешная обработка заявки с флагом обновления пересчитанной комбинатором даты доставки "
            + "- таска на обновление не создалась тк пересчет доступен только по опозданию на первом сегменте, "
            + " а пересчет на DELIVERY сегменте по опоздавшей приемке от DROPSHIP партнера"
    )
    void successWithRecalculateOrderDeliveryDateTaskDelayOnFirstSegmentTagDeliverySegment() {
        featureProperties.setUpdateDeliveryDateAfterRecalculationAvailability(DELAY_OUT_ON_FIRST_SEGMENT);

        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("orders/recalculate_delivery_date/combined_route_1001.json")),
            converter::mapToItem
        );

        testProcessSuccess(
            buildResponse(),
            "orders/recalculate_delivery_date/route_1001.json",
            614085,
            ServiceCodeName.INBOUND
        );
        queueTaskChecker.assertNoQueueTasksCreated();
        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(EXISTING_UUID);
    }

    @Test
    @DatabaseSetup(
        value = "/orders/recalculate_delivery_date/recalc_date_change_req_for_delivery.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/recalc_date_change_payload_info_received_for_delivery.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName(
        "Успешная обработка заявки с флагом обновления пересчитанной комбинатором даты доставки "
            + "- таска на обновление создалась тк пересчет доступен всем, пересчет не на FF сегменте"
    )
    void successWithRecalculateOrderDeliveryDateTaskAllTagNotFFSegment() {
        featureProperties.setUpdateDeliveryDateAfterRecalculationAvailability(ALL);

        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("orders/recalculate_delivery_date/combined_route_1001.json")),
            converter::mapToItem
        );
        testProcessSuccess(DS_SEGMENT_ID);
        queueTaskChecker.assertQueueTaskCreated(
            CHANGE_ORDER_REQUEST,
            PayloadFactory.createChangeOrderRequestPayload(1, "1", 1)
        );
        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(EXISTING_UUID);
    }

    @Test
    @DatabaseSetup(
        value = "/orders/recalculate_delivery_date/recalc_date_change_req_for_delivery.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/recalc_date_change_payload_fail_for_delivery.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName(
        "Успешная обработка заявки с флагом обновления пересчитанной комбинатором даты доставки "
            + "- таска на обновление не создалась тк пересчет недоступен, пересчет не на FF сегменте"
    )
    void successWithRecalculateOrderDeliveryDateTaskNoneTagNotFFSegment() {
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("orders/recalculate_delivery_date/route_1001_handing.json")),
            converter::mapToItem
        );
        testProcessSuccess(614085L);
        queueTaskChecker.assertNoQueueTasksCreated();
        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(EXISTING_UUID);
    }

    @Test
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/recalc_date_change_payload_created_go_platform.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешная обработка заявки с типом сегмента GO_PLATFORM")
    void testProcessPayloadSuccessGoPlatform() {
        processSuccessGoPlatform();

        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(EXISTING_UUID);
    }

    @Test
    @DatabaseSetup(
        value = "/orders/recalculate_delivery_date/deferred_courier_order.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/recalc_date_change_payload_created_for_deferred_courier_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешная обработка заявки - заказ в часовые слоты, интервал (время от/до) остается прежним")
    void testProcessPayloadSuccessForDeferredCourierOrder() {
        processSuccessGoPlatform();

        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(EXISTING_UUID);
    }

    @Test
    @DatabaseSetup(
        value = {
            "/orders/recalculate_delivery_date/recalc_date_change_payload_created_without_segment_status.xml",
            "/orders/recalculate_delivery_date/dropship_order.xml",
        },
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/recalc_date_change_without_segment_status_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешная обработка заявки без указания статуса сегмента")
    void testProcessPayloadNoSegmentStatus() {
        featureProperties.setUpdateDeliveryDateAfterRecalculationAvailability(ALL);
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("orders/recalculate_delivery_date/combined_route_1001_dropship.json")),
            converter::mapToItem
        );

        testProcessSuccess(
            buildResponse(),
            "orders/recalculate_delivery_date/route_1001.json",
            612909,
            ServiceCodeName.PROCESSING
        );

        verify(orderCombinedRouteHistoryYdbRepository).getRouteByUuid(EXISTING_UUID);
    }

    @Test
    @DatabaseSetup(
        value = "/orders/recalculate_delivery_date/recalc_date_change_payload_no_segment_status_and_service.xml",
        type = DatabaseOperation.REFRESH
    )
    @DisplayName("Ошибка обработки заявки без указания статуса сегмента и сервиса")
    void testProcessPayloadNoSegmentStatusAndService() {
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("orders/recalculate_delivery_date/combined_route_1001_dropship.json")),
            converter::mapToItem
        );
        testProcessFail("No data to convert service code");
    }

    @Test
    @DatabaseSetup(
        value = "/orders/recalculate_delivery_date/no_combined_route.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/orders/recalculate_delivery_date/add_business_process_state.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/no_error_business_process_state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Не переводить cor в TechFail статус при первой попытке")
    void doNotMakeTechFailStatusIfFirstAttempt() {
        TaskExecutionResult taskExecutionResult = consumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_RECALCULATE_ORDER_DELIVERY_DATE,
            queueTaskChecker.getProducedTaskPayload(
                QueueType.PROCESS_RECALCULATE_ORDER_DELIVERY_DATE,
                ChangeOrderRequestPayload.class
            ),
            1
        ));
        softly.assertThat(taskExecutionResult).isEqualTo(TaskExecutionResult.fail());
    }

    @Test
    @DatabaseSetup(
        value = "/orders/recalculate_delivery_date/no_combined_route.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/orders/recalculate_delivery_date/add_business_process_state.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/orders/recalculate_delivery_date/failed_business_process_state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Переводить cor в TechFail статус при 3ей попытке")
    void doMakeTechFailStatusIfMaxAttempt() {
        TaskExecutionResult taskExecutionResult = consumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_RECALCULATE_ORDER_DELIVERY_DATE,
            queueTaskChecker.getProducedTaskPayload(
                QueueType.PROCESS_RECALCULATE_ORDER_DELIVERY_DATE,
                ChangeOrderRequestPayload.class
            ),
            3
        ));
        softly.assertThat(taskExecutionResult).isEqualTo(TaskExecutionResult.finish());
    }

    private void processSuccess() {
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("orders/recalculate_delivery_date/combined_route_1001.json")),
            converter::mapToItem
        );
        testProcessSuccess();
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    private void processSuccessGoPlatform() {
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("orders/recalculate_delivery_date/combined_route_1001_go_platform.json")),
            converter::mapToItem
        );
        CombinatorOuterClass.RecalculationResponse buildResponse = createGoPlatformResponse();
        testProcessSuccess(
            buildResponse,
            "orders/recalculate_delivery_date/route_1001_go_platform.json"
        );
    }

    private void testProcessSuccess() {
        testProcessSuccess(buildResponse(), "orders/recalculate_delivery_date/route_1001.json");
    }

    private void testProcessSuccess(
        CombinatorOuterClass.RecalculationResponse response,
        String routeFilePath
    ) {
        testProcessSuccess(response, routeFilePath, FF_SEGMENT_ID, ServiceCodeName.SHIPMENT);
    }

    private void testProcessSuccess(
        CombinatorOuterClass.RecalculationResponse response,
        String routeFilePath,
        long segmentId,
        ServiceCodeName serviceCodeName
    ) {
        doReturn(response).when(combinatorGrpcClient).recalculateRoute(any());

        ProcessingResult processPayload = processor.processPayload(CHANGE_ORDER_REQUEST_PAYLOAD);
        softly.assertThat(processPayload.getStatus()).isEqualTo(ProcessingResultStatus.SUCCESS);

        verifyRecalculateRoute(serviceCodeName.name(), segmentId);
        verifySaveRouteToYdb(routeFilePath);
    }

    private void testProcessSuccess(long segmentId) {
        testProcessSuccess(
            buildResponse(),
            "orders/recalculate_delivery_date/route_1001.json",
            segmentId,
            ServiceCodeName.SHIPMENT
        );
    }

    private void verifyRecalculateRoute(String serviceCode) {
        verifyRecalculateRoute(serviceCode, FF_SEGMENT_ID);
    }

    private void verifyRecalculateRoute(String serviceCode, long segmentId) {
        verify(combinatorGrpcClient).recalculateRoute(requestArgumentCaptor.capture());
        CombinatorOuterClass.RecalculationRequest request = requestArgumentCaptor.getValue();

        softly.assertThat(request).isNotNull();
        softly.assertThat(request.getSegmentId()).isEqualTo(segmentId);
        softly.assertThat(request.getServiceCode()).isEqualTo(serviceCode);
        softly.assertThat(request.getStartTime().getSeconds()).isEqualTo(1628199325);
        softly.assertThat(request.getDeliveryType()).isEqualTo(Common.DeliveryType.COURIER);
        softly.assertThat(request.getRoute()).isNotNull();
        softly.assertThat(request.getOrderId()).isEqualTo("1002");
    }

    @SneakyThrows
    private void verifySaveRouteToYdb(String filePath) {
        verify(orderCombinedRouteHistoryYdbRepository)
            .saveRoute(any(), eq(objectMapper.readTree(extractFileContent(filePath))));
    }

    private void testProcessFail(@Nullable String message) {
        softly.assertThatThrownBy(() -> processor.processPayload(CHANGE_ORDER_REQUEST_PAYLOAD))
            .isInstanceOf(DbQueueJobExecutionException.class)
            .hasMessage("Error while RecalculateOrderDeliveryDateProcessor processing, changeOrderRequestId=1")
            .getCause()
            .hasMessage(message);

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Nonnull
    private CombinatorOuterClass.RecalculationResponse createGoPlatformResponse() {
        return buildResponse(
            CombinatorOuterClass.Route.Point.newBuilder().setSegmentType("go_platform")
                .addServices(
                    deliveryServiceBuilder().clearDeliveryIntervals().addDeliveryIntervals(
                        CombinatorOuterClass.DeliveryInterval.newBuilder()
                            .setFrom(buildTime(16))
                            .setTo(buildTime(17))
                            .build()
                    ).build()
                ).build()
        );
    }

    @Nonnull
    private static CombinatorOuterClass.RecalculationResponse buildResponse(CombinatorOuterClass.Route.Point point) {
        return CombinatorOuterClass.RecalculationResponse.newBuilder()
            .setRoute(buildRoute(buildDate(20), buildDate(30), point))
            .build();
    }

    @Nonnull
    private static CombinatorOuterClass.RecalculationResponse buildResponse() {
        return buildResponse(buildPoint());
    }

    @Nonnull
    private static CombinatorOuterClass.DeliveryRoute buildRoute(
        CombinatorOuterClass.Date deliveryDateFrom,
        CombinatorOuterClass.Date deliveryDateTo,
        CombinatorOuterClass.Route.Point... points
    ) {
        CombinatorOuterClass.Route.Builder builder = CombinatorOuterClass.Route.newBuilder();
        Stream.of(points).forEach(builder::addPoints);
        CombinatorOuterClass.Route route = builder
            .setDateFrom(deliveryDateFrom)
            .setDateTo(deliveryDateTo)
            .build();

        return CombinatorOuterClass.DeliveryRoute.newBuilder()
            .setRoute(route)
            .build();
    }

    @Nonnull
    private static CombinatorOuterClass.DeliveryService.Builder deliveryServiceBuilder() {
        return CombinatorOuterClass.DeliveryService.newBuilder()
            .setCode(ServiceCodeName.HANDING.name())
            .setStartTime(Timestamp.newBuilder().setSeconds(START_TIME))
            .setDuration(Duration.newBuilder().setSeconds(0))
            .addDeliveryIntervals(buildDeliveryInterval());
    }

    @Nonnull
    private static CombinatorOuterClass.Route.Point buildPoint(CombinatorOuterClass.DeliveryService... services) {
        CombinatorOuterClass.Route.Point.Builder builder = CombinatorOuterClass.Route.Point.newBuilder()
            .setSegmentType(LogisticSegmentType.HANDING.name().toLowerCase());
        Stream.of(services).forEach(builder::addServices);
        return builder.build();
    }

    @Nonnull
    private static CombinatorOuterClass.Route.Point buildPoint() {
        return buildPoint(deliveryServiceBuilder().build());
    }

    @Nonnull
    private static CombinatorOuterClass.DeliveryInterval buildDeliveryInterval() {
        return CombinatorOuterClass.DeliveryInterval.newBuilder()
            .setFrom(buildTime(FROM_INTERVAL))
            .setTo(buildTime(TO_INTERVAL))
            .build();
    }

    @Nonnull
    private static CombinatorOuterClass.Time buildTime(int hour) {
        return CombinatorOuterClass.Time.newBuilder()
            .setHour(hour)
            .setMinute(0)
            .build();
    }

    @Nonnull
    private static CombinatorOuterClass.Date buildDate(int day) {
        return CombinatorOuterClass.Date.newBuilder()
            .setYear(2021)
            .setMonth(8)
            .setDay(day)
            .build();
    }

    @Nonnull
    @SneakyThrows
    private CombinedRoute createCombinedRoute(String file) {
        return new CombinedRoute()
            .setOrderId(1001L)
            .setSourceRoute(objectMapper.readTree(extractFileContent(file)))
            .setRouteUuid(EXISTING_UUID);
    }

    private void verifyRecalculatingStarted() {
        softly.assertThat(backLogCaptor.getResults()).anyMatch(log -> {
                Map<String, String> tskvMap = tskvLogToMap(log);
                return "INFO".equals(tskvMap.get(LEVEL_KEY))
                    && "RECALCULATE_DELIVERY_DATE".equals(tskvMap.get(CODE_KEY))
                    && tskvMap.get(PAYLOAD_KEY).startsWith("Received combinator response.");
            }
        );
    }
}
