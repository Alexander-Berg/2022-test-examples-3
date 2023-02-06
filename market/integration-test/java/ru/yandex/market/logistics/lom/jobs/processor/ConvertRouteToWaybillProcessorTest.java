package ru.yandex.market.logistics.lom.jobs.processor;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualYdbTest;
import ru.yandex.market.logistics.lom.controller.order.OrderHistoryTestUtil;
import ru.yandex.market.logistics.lom.entity.combinator.embedded.CombinedRoute;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.repository.ydb.OrderCombinedRouteHistoryYdbRepository;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateStatusHistoryTableDescription;
import ru.yandex.market.logistics.lom.repository.ydb.description.OrderCombinedRouteHistoryTableDescription;
import ru.yandex.market.logistics.lom.utils.ydb.converter.OrderCombinedRouteHistoryYdbConverter;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createOrderIdAuthorPayload;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@ParametersAreNonnullByDefault
@DisplayName("Обработчик конвертации комбинированного маршрута")
class ConvertRouteToWaybillProcessorTest extends AbstractContextualYdbTest {

    private static final UUID ROUTE_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private ConvertRouteToWaybillProcessor convertRouteToWaybillProcessor;

    @Autowired
    private OrderCombinedRouteHistoryTableDescription routeHistoryTable;

    @Autowired
    private BusinessProcessStateStatusHistoryTableDescription businessProcessStateStatusHistoryTable;

    @Autowired
    private OrderCombinedRouteHistoryYdbConverter routeHistoryConverter;

    @Autowired
    private OrderCombinedRouteHistoryYdbRepository newRouteYdbRepository;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(newRouteYdbRepository);
    }

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(routeHistoryTable, businessProcessStateStatusHistoryTable);
    }

    @Test
    @DisplayName("Конвертация для заказа типа Дропофф")
    @DatabaseSetup("/jobs/processor/convert_route_to_waybill/before/order.xml")
    @ExpectedDatabase(
        value = "/jobs/processor/convert_route_to_waybill/after/order_without_validation_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processOrderWithDropoffRoute() {
        convertRouteToWaybillProcessor.processPayload(createOrderIdAuthorPayload(1, null, "2", 2));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.COMMIT_ORDER,
            createOrderIdAuthorPayload(1, null, "1", 1)
        );

        verify(newRouteYdbRepository).getRouteByUuid(ROUTE_UUID);
    }

    @Test
    @DisplayName("Конвертация для заказа c дропофовым returnSortingCenterId")
    @DatabaseSetup("/jobs/processor/convert_route_to_waybill/before/order.xml")
    @ExpectedDatabase(
        value = "/jobs/processor/convert_route_to_waybill/after/order_with_return_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processFromOrderInValidationErrorWithDropoffReturnSortingCenterId() throws IOException {
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("controller/order/combined/combined_route_with_return_center_id.json")),
            routeHistoryConverter::mapToItem
        );
        convertRouteToWaybillProcessor.processPayload(createOrderIdAuthorPayload(1, null, "2", 2));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.COMMIT_ORDER,
            createOrderIdAuthorPayload(1, null, "1", 1)
        );

        verify(newRouteYdbRepository).getRouteByUuid(ROUTE_UUID);
    }

    @Test
    @DisplayName("Конвертация для заказа c returnSortingCenterId для невыкупов экспресса")
    @DatabaseSetup("/jobs/processor/convert_route_to_waybill/before/order.xml")
    @ExpectedDatabase(
        value = "/jobs/processor/convert_route_to_waybill/after/express_order_with_return_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processWithExpressReturnSortingCenterId() throws IOException {
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("controller/order/combined/combined_route_call_courier_with_return_sc.json")),
            routeHistoryConverter::mapToItem
        );
        convertRouteToWaybillProcessor.processPayload(createOrderIdAuthorPayload(1, null, "2", 2));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.COMMIT_ORDER,
            createOrderIdAuthorPayload(1, null, "1", 1)
        );

        verify(newRouteYdbRepository).getRouteByUuid(ROUTE_UUID);
    }

    @Test
    @DisplayName("Конвертация экспресс заказа без сц для невыкупов")
    @DatabaseSetup("/jobs/processor/convert_route_to_waybill/before/order.xml")
    @ExpectedDatabase(
        value = "/jobs/processor/convert_route_to_waybill/after/order_without_express_return_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/jobs/processor/convert_route_to_waybill/after/express_order_default_tags.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processExpressWithoutReturnSc() throws IOException {
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("controller/order/combined/combined_route_call_courier_movement.json")),
            routeHistoryConverter::mapToItem
        );
        convertRouteToWaybillProcessor.processPayload(createOrderIdAuthorPayload(1, null, "2", 2));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.COMMIT_ORDER,
            createOrderIdAuthorPayload(1, null, "1", 1)
        );

        verify(newRouteYdbRepository).getRouteByUuid(ROUTE_UUID);
    }

    @Test
    @DisplayName("Конвертация для заказа c returnSortingCenterId для невыкупов экспресса без CALL_COURIER")
    @DatabaseSetup("/jobs/processor/convert_route_to_waybill/before/order.xml")
    @ExpectedDatabase(
        value = "/jobs/processor/convert_route_to_waybill/after/order_without_express_call_courier.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processWithExpressReturnSortingCenterIdWithoutCallCourier() throws IOException {
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("controller/order/combined/combined_route_with_return_sc.json")),
            routeHistoryConverter::mapToItem
        );
        convertRouteToWaybillProcessor.processPayload(createOrderIdAuthorPayload(1, null, "2", 2));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.COMMIT_ORDER,
            createOrderIdAuthorPayload(1, null, "1", 1)
        );

        verify(newRouteYdbRepository).getRouteByUuid(ROUTE_UUID);
    }

    @Test
    @DisplayName("Конвертация для экспресс заказа c признаками широких слотов")
    @DatabaseSetup("/jobs/processor/convert_route_to_waybill/before/order.xml")
    @ExpectedDatabase(
        value = "/jobs/processor/convert_route_to_waybill/after/express_order_wide_interval_tags.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processWideIntervalExpress() throws IOException {
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("controller/order/combined/combined_route_call_courier_wide_interval.json")),
            routeHistoryConverter::mapToItem
        );
        convertRouteToWaybillProcessor.processPayload(createOrderIdAuthorPayload(1, null, "2", 2));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.COMMIT_ORDER,
            createOrderIdAuthorPayload(1, null, "1", 1)
        );

        verify(newRouteYdbRepository).getRouteByUuid(ROUTE_UUID);
    }

    @Test
    @DisplayName("Конвертация для экспресс заказа, не истина в признаках широких слотов")
    @DatabaseSetup("/jobs/processor/convert_route_to_waybill/before/order.xml")
    @ExpectedDatabase(
        value = "/jobs/processor/convert_route_to_waybill/after/express_order_default_tags.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processFalseWideIntervalExpress() throws IOException {
        insertAllIntoTable(
            routeHistoryTable,
            List.of(
                createCombinedRoute("controller/order/combined/combined_route_call_courier_false_wide_interval.json")
            ),
            routeHistoryConverter::mapToItem
        );
        convertRouteToWaybillProcessor.processPayload(createOrderIdAuthorPayload(1, null, "2", 2));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.COMMIT_ORDER,
            createOrderIdAuthorPayload(1, null, "1", 1)
        );

        verify(newRouteYdbRepository).getRouteByUuid(ROUTE_UUID);
    }

    @Test
    @DisplayName("Конвертация для заказа без оффсета в HANDING.HANDING")
    @DatabaseSetup("/jobs/processor/convert_route_to_waybill/before/order.xml")
    @ExpectedDatabase(
        value = "/jobs/processor/convert_route_to_waybill/after/order_with_return_id_without_offset.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processWithoutOffset() throws IOException {
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute(
                "controller/order/combined/combined_route_with_return_center_id_without_offset.json"
            )),
            routeHistoryConverter::mapToItem
        );
        convertRouteToWaybillProcessor.processPayload(createOrderIdAuthorPayload(1, null, "2", 2));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.COMMIT_ORDER,
            createOrderIdAuthorPayload(1, null, "1", 1)
        );

        verify(newRouteYdbRepository).getRouteByUuid(ROUTE_UUID);
    }

    @Test
    @DisplayName("Конвертация для заказа, находящегося в ошибке проверки")
    @DatabaseSetup("/jobs/processor/convert_route_to_waybill/before/order_with_validation_error.xml")
    @ExpectedDatabase(
        value = "/jobs/processor/convert_route_to_waybill/after/order_in_draft.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processFromOrderInValidationError() throws IOException {
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute("controller/order/combined/combined_route.json")),
            routeHistoryConverter::mapToItem
        );
        convertRouteToWaybillProcessor.processPayload(createOrderIdAuthorPayload(1, null, "2", 2));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.COMMIT_ORDER,
            createOrderIdAuthorPayload(1, null, "1", 1)
        );

        verify(newRouteYdbRepository).getRouteByUuid(ROUTE_UUID);
    }

    @Test
    @DisplayName("Заказа отменен")
    @DatabaseSetup("/jobs/processor/convert_route_to_waybill/before/order_early_cancel.xml")
    @ExpectedDatabase(
        value = "/jobs/processor/convert_route_to_waybill/before/order_early_cancel.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processFromOrderWithCancellationRequest() {
        convertRouteToWaybillProcessor.processPayload(createOrderIdAuthorPayload(1, null, "2", 2));

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Маршрут уже был сконвертирован в вейбил")
    @DatabaseSetup({
        "/jobs/processor/convert_route_to_waybill/before/order.xml",
        "/jobs/processor/convert_route_to_waybill/before/waybill_segments.xml"
    })
    void routeAlreadyConvertedToWaybill() {
        convertRouteToWaybillProcessor.processPayload(createOrderIdAuthorPayload(1, null, "2", 2));
        queueTaskChecker.assertNoQueueTasksCreated();
        OrderHistoryTestUtil.assertOrderHistoryEventCount(jdbcTemplate, 1, 0);
        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t" +
                "format=plain\t" +
                "payload=Route already converted to waybill\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "entity_types=order,lom_order\t" +
                "entity_values=order:1001,lom_order:1"
        );
    }

    @Nonnull
    private CombinedRoute createCombinedRoute(String file) throws IOException {
        return new CombinedRoute()
            .setOrderId(1L)
            .setSourceRoute(objectMapper.readTree(extractFileContent(file)))
            .setRouteUuid(ROUTE_UUID);
    }
}
