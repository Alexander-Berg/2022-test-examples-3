package ru.yandex.market.logistics.lom.jobs.processor;

import java.util.List;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionOperations;

import yandex.market.combinator.common.Common;
import yandex.market.combinator.v0.CombinatorOuterClass;
import ru.yandex.market.logistics.lom.AbstractContextualYdbTest;
import ru.yandex.market.logistics.lom.converter.RouteConverter;
import ru.yandex.market.logistics.lom.entity.combinator.embedded.CombinedRoute;
import ru.yandex.market.logistics.lom.entity.combinator.embedded.OrderCombinedRouteHistory;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute;
import ru.yandex.market.logistics.lom.model.enums.DeliveryType;
import ru.yandex.market.logistics.lom.repository.ydb.OrderCombinedRouteHistoryYdbRepository;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateStatusHistoryTableDescription;
import ru.yandex.market.logistics.lom.repository.ydb.description.OrderCombinedRouteHistoryTableDescription;
import ru.yandex.market.logistics.lom.service.order.OrderService;
import ru.yandex.market.logistics.lom.service.order.combinator.CombinatorGrpcClient;
import ru.yandex.market.logistics.lom.service.order.combinator.CombinatorRouteService;
import ru.yandex.market.logistics.lom.service.order.history.route.OrderCombinedRouteHistoryService;
import ru.yandex.market.logistics.lom.utils.UuidGenerator;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DisplayName("Тест таски пересчёта маршрута заказа")
@DatabaseSetup("/service/order/route/before/prepare_orders.xml")
class RecalculateOrderRouteProcessorTest extends AbstractContextualYdbTest {

    private static final Common.DeliveryType OLD_COMBINATOR_DELIVERY_TYPE = Common.DeliveryType.COURIER;
    private static final Common.DeliveryType NEW_COMBINATOR_DELIVERY_TYPE = Common.DeliveryType.PICKUP;
    private static final DeliveryType NEW_DELIVERY_TYPE = DeliveryType.PICKUP;
    private static final int DELIVERY_SERVICE_ID = 1003937;
    private static final int FIRST_POINT_LMS_ID = 100;
    private static final Long ORDER_ID = 1L;

    @Autowired
    private RecalculateOrderRouteProcessor processor;

    @Autowired
    private OrderCombinedRouteHistoryTableDescription routeHistoryTable;

    @Autowired
    private BusinessProcessStateStatusHistoryTableDescription businessProcessStateStatusHistoryTable;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CombinatorRouteService combinatorRouteService;

    @Autowired
    private RouteConverter routeConverter;

    @Autowired
    protected UuidGenerator uuidGenerator;

    @Autowired
    private CombinatorGrpcClient combinatorGrpcClient;

    @Autowired
    protected OrderCombinedRouteHistoryYdbRepository ydbRepository;

    @Autowired
    protected OrderCombinedRouteHistoryService orderCombinedRouteHistoryService;

    @Autowired
    protected TransactionOperations transactionTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    @NotNull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(routeHistoryTable, businessProcessStateStatusHistoryTable);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(combinatorGrpcClient, uuidGenerator, ydbRepository);
    }

    @Test
    @DatabaseSetup(
        value = "/jobs/processor/recalculate_order_route/order_validation_error.xml",
        type = DatabaseOperation.UPDATE
    )
    @DisplayName("Успех")
    void success() {
        transactionTemplate.execute(transactionStatus -> {
            orderCombinedRouteHistoryService.saveRoute(orderService.getOrderOrThrow(ORDER_ID), combinedRoute());
            return null;
        });

        Mockito.when(combinatorGrpcClient.getDeliveryRouteFromPointRequest(any())).thenReturn(
            CombinatorOuterClass.DeliveryRoute
                .newBuilder()
                .setRoute(
                    CombinatorOuterClass.Route
                        .newBuilder()
                        .setDeliveryType(NEW_COMBINATOR_DELIVERY_TYPE)
                )
                .build()
        );

        processor.processPayload(PayloadFactory.createOrderIdPayload(ORDER_ID));

        CombinedRoute combinedRoute = combinatorRouteService.getByRouteUuidOrThrow(
            orderService.getOrderRouteUuid(ORDER_ID)
        );
        CombinatorRoute newRoute = routeConverter.convertToCombinatorRoute(
            combinedRoute.getSourceRoute()
        );

        softly.assertThat(newRoute.getRoute().getDeliveryType()).isEqualTo(NEW_DELIVERY_TYPE);

        ArgumentCaptor<CombinatorOuterClass.DeliveryRouteFromPointRequest> argumentCaptor = ArgumentCaptor.forClass(
            CombinatorOuterClass.DeliveryRouteFromPointRequest.class
        );
        verify(combinatorGrpcClient).getDeliveryRouteFromPointRequest(argumentCaptor.capture());
        CombinatorOuterClass.DeliveryRouteFromPointRequest request = argumentCaptor.getValue();
        softly.assertThat(request.getDeliveryType()).isEqualTo(OLD_COMBINATOR_DELIVERY_TYPE);
        softly.assertThat(request.getDeliveryServiceId()).isEqualTo(DELIVERY_SERVICE_ID);
        softly.assertThat(request.getStartSegmentLmsId()).isEqualTo(FIRST_POINT_LMS_ID);
        verify(uuidGenerator, times(2)).randomUuid();
        verify(ydbRepository, times(2)).getRouteByUuid(any());
        verify(ydbRepository).saveRoute(any(OrderCombinedRouteHistory.class), refEq(combinedRoute()));
        verify(ydbRepository).saveRoute(
            any(OrderCombinedRouteHistory.class),
            refEq(objectMapper.valueToTree(newRoute))
        );
    }

    @Test
    @DisplayName("Неподходящий статус заказа")
    void notAllowedStatus() {
        transactionTemplate.execute(transactionStatus -> {
            orderCombinedRouteHistoryService.saveRoute(orderService.getOrderOrThrow(ORDER_ID), combinedRoute());
            return null;
        });

        ProcessingResult result = processor.processPayload(PayloadFactory.createOrderIdPayload(ORDER_ID));
        softly.assertThat(result).isEqualTo(ProcessingResult.unprocessed(
            "Replace route is allowed only in state VALIDATION_ERROR and without created waybill segments"
        ));

        verify(uuidGenerator).randomUuid();
        verify(ydbRepository).saveRoute(any(), refEq(combinedRoute()));
    }

    @Nonnull
    @SneakyThrows
    private JsonNode combinedRoute() {
        return objectMapper.readTree(extractFileContent("service/order/route/combined_route.json"));
    }

}
