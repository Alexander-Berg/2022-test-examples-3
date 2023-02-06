package ru.yandex.market.logistics.lom.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.AbstractContextualYdbTest;
import ru.yandex.market.logistics.lom.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.lom.entity.combinator.embedded.CombinedRoute;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderSegmentRequestPayload;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateRecalculatedOrderDatesRequestStatusProcessor;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateStatusHistoryTableDescription;
import ru.yandex.market.logistics.lom.repository.ydb.description.OrderCombinedRouteHistoryTableDescription;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.ydb.converter.OrderCombinedRouteHistoryYdbConverter;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static ru.yandex.market.logistics.lom.jobs.model.QueueType.EXPORT_ORDER_FROM_SHIPMENT_EXCLUSION_FINISHED;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DisplayName("Обновление статуса заявки на изменение даты доставки заказа на сегменте после перерасчета маршрута")
@DatabaseSetup("/controller/order/recalculateRouteDates/updaterequeststatus/prepare_data.xml")
class UpdateRecalculatedOrderDatesRequestStatusProcessorTest extends AbstractContextualYdbTest {
    private static final ChangeOrderSegmentRequestPayload PAYLOAD = PayloadFactory
        .createChangeOrderSegmentRequestPayload(1L, "1", 1L);
    private static final UUID NEW_ROUTE_UUID = UUID.fromString("92b2a721-8e98-4b4a-8f86-a045d570e036");

    @Autowired
    private UpdateRecalculatedOrderDatesRequestStatusProcessor processor;

    @Autowired
    private OrderCombinedRouteHistoryTableDescription routeHistoryTable;

    @Autowired
    private BusinessProcessStateStatusHistoryTableDescription businessProcessStateStatusHistoryTableDescription;

    @Autowired
    private OrderCombinedRouteHistoryYdbConverter converter;

    @Autowired
    private FeatureProperties featureProperties;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2021-09-21T12:30:00Z"), DateTimeUtils.MOSCOW_ZONE);
    }

    @AfterEach
    void tearDown() {
        featureProperties.setUpdateSegmentShipmentDates(false);
    }

    @Test
    @DisplayName("Все сегменты в статусе SUCCESS")
    @ExpectedDatabase(
        value = "/controller/order/recalculateRouteDates/updaterequeststatus/after/all_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void allSuccess() {
        processor.processPayload(PAYLOAD);
        softly.assertThat(backLogCaptor.getResults())
            .anyMatch(line -> line.contains(
                "level=INFO\t" +
                    "format=plain\t" +
                    "code=UPDATE_REQUEST\t" +
                    "payload=RECALCULATE_ROUTE_DATES/1/UPDATE_REQUEST/PROCESSING/SUCCESS\t" +
                    "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                    "tags=UPDATE_ORDER_ITEMS_STATS\t" +
                    "extra_keys=requestType,newStatus,oldStatus,requestId,timeFromCreateToUpdate,timeFromUpdate," +
                    "partnerId\textra_values=RECALCULATE_ROUTE_DATES,SUCCESS,PROCESSING,1"
            ));
        queueTaskChecker.assertQueueTaskNotCreated(EXPORT_ORDER_FROM_SHIPMENT_EXCLUSION_FINISHED);
    }

    @Test
    @DisplayName("Все сегменты в статусе SUCCESS - нужно отправить событие в LES")
    @DatabaseSetup(
        value = "/controller/order/recalculateRouteDates/updaterequeststatus/has_exclusion_request.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/controller/order/recalculateRouteDates/updaterequeststatus/after/all_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void allSuccessSendEventToLes() {
        processor.processPayload(PAYLOAD);
        softly.assertThat(backLogCaptor.getResults())
            .anyMatch(line -> line.contains(
                "level=INFO\t" +
                    "format=plain\t" +
                    "code=UPDATE_REQUEST\t" +
                    "payload=RECALCULATE_ROUTE_DATES/1/UPDATE_REQUEST/PROCESSING/SUCCESS\t" +
                    "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                    "tags=UPDATE_ORDER_ITEMS_STATS\t" +
                    "extra_keys=requestType,newStatus,oldStatus,requestId,timeFromCreateToUpdate,timeFromUpdate," +
                    "partnerId\textra_values=RECALCULATE_ROUTE_DATES,SUCCESS,PROCESSING,1"
            ));
        queueTaskChecker.assertQueueTaskCreated(
            EXPORT_ORDER_FROM_SHIPMENT_EXCLUSION_FINISHED,
            PayloadFactory.exportOrderFromShipmentExclusionFinishedPayload(1L, 1L, 101L, 1L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Все сегменты в статусе SUCCESS - обновить дату отгрузки сегментов")
    @ExpectedDatabase(
        value = "/controller/order/recalculateRouteDates/updaterequeststatus/after/updated_shipment_dates.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void allSuccessUpdateShipmentDates() {
        featureProperties.setUpdateSegmentShipmentDates(true);
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("controller/order/recalculateRouteDates/updaterequeststatus/route.json")),
            converter::mapToItem
        );
        processor.processPayload(PAYLOAD);
    }

    @Test
    @DisplayName("Все сегменты в статусе SUCCESS, routeUuid у заказа обновляется на новый")
    @DatabaseSetup(
        value = "/controller/order/recalculateRouteDates/updaterequeststatus/order_with_route_uuid_history.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/controller/order/recalculateRouteDates/updaterequeststatus/order_route_history.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/order/recalculateRouteDates/updaterequeststatus/after/all_success_route_uuid_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void allSuccessRouteUuidUpdated() {
        allSuccess();
    }

    @Test
    @DisplayName("Все сегменты в статусе FAIL")
    @DatabaseSetup(
        value = "/controller/order/recalculateRouteDates/updaterequeststatus/all_fail.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/recalculateRouteDates/updaterequeststatus/after/all_fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void allFail() {
        processor.processPayload(PAYLOAD);
        softly.assertThat(backLogCaptor.getResults()).anyMatch(line -> line.contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=UPDATE_REQUEST\t" +
                "payload=RECALCULATE_ROUTE_DATES/1/UPDATE_REQUEST/PROCESSING/FAIL\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=UPDATE_ORDER_ITEMS_STATS\t" +
                "extra_keys=requestType,newStatus,oldStatus,requestId,timeFromCreateToUpdate,timeFromUpdate," +
                "partnerId\textra_values=RECALCULATE_ROUTE_DATES,FAIL,PROCESSING,1"
        ));
        queueTaskChecker.assertQueueTaskNotCreated(EXPORT_ORDER_FROM_SHIPMENT_EXCLUSION_FINISHED);
    }

    @Test
    @DisplayName("Один сегмент в статусе SUCCESS")
    @DatabaseSetup(
        value = "/controller/order/recalculateRouteDates/updaterequeststatus/one_success.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/recalculateRouteDates/updaterequeststatus/after/one_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void oneSuccess() {
        processor.processPayload(PAYLOAD);
        softly.assertThat(backLogCaptor.getResults()).anyMatch(line -> line.contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=UPDATE_REQUEST\t" +
                "payload=RECALCULATE_ROUTE_DATES/1/UPDATE_REQUEST/PROCESSING/REQUIRED_SEGMENT_SUCCESS\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=UPDATE_ORDER_ITEMS_STATS\t" +
                "extra_keys=requestType,newStatus,oldStatus,requestId,timeFromCreateToUpdate,timeFromUpdate," +
                "partnerId\textra_values=RECALCULATE_ROUTE_DATES,REQUIRED_SEGMENT_SUCCESS,PROCESSING,1"
        ));
        queueTaskChecker.assertQueueTaskNotCreated(EXPORT_ORDER_FROM_SHIPMENT_EXCLUSION_FINISHED);
    }

    @Test
    @DisplayName("Один сегмент в статусе FAIL")
    @DatabaseSetup(
        value = "/controller/order/recalculateRouteDates/updaterequeststatus/one_fail.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/recalculateRouteDates/updaterequeststatus/after/one_fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void oneFail() {
        processor.processPayload(PAYLOAD);
        softly.assertThat(backLogCaptor.getResults()).anyMatch(line -> line.contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=UPDATE_REQUEST\t" +
                "payload=RECALCULATE_ROUTE_DATES/1/UPDATE_REQUEST/PROCESSING/REQUIRED_SEGMENT_SUCCESS\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=UPDATE_ORDER_ITEMS_STATS\t" +
                "extra_keys=requestType,newStatus,oldStatus,requestId,timeFromCreateToUpdate,timeFromUpdate," +
                "partnerId\textra_values=RECALCULATE_ROUTE_DATES,REQUIRED_SEGMENT_SUCCESS,PROCESSING,1"
        ));
        queueTaskChecker.assertQueueTaskNotCreated(EXPORT_ORDER_FROM_SHIPMENT_EXCLUSION_FINISHED);
    }

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(routeHistoryTable, businessProcessStateStatusHistoryTableDescription);
    }

    @Nonnull
    @SneakyThrows
    private CombinedRoute createCombinedRoute(String file) {
        return new CombinedRoute()
            .setOrderId(1L)
            .setSourceRoute(objectMapper.readTree(extractFileContent(file)))
            .setRouteUuid(NEW_ROUTE_UUID);
    }
}
