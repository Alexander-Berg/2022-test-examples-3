package ru.yandex.market.logistics.lom.service.order.combinator;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;

import yandex.market.combinator.common.Common;
import yandex.market.combinator.v0.CombinatorOuterClass;

import ru.yandex.market.logistics.lom.admin.AbstractCombinedRouteTest;
import ru.yandex.market.logistics.lom.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.lom.entity.combinator.embedded.CombinedRoute;
import ru.yandex.market.logistics.lom.service.order.combinator.dto.RouteRecalculationParams;
import ru.yandex.market.logistics.lom.service.order.combinator.dto.RouteRecalculationResult;
import ru.yandex.market.logistics.lom.service.order.combinator.enums.RecalculationStatus;
import ru.yandex.market.logistics.lom.utils.UuidGenerator;
import ru.yandex.market.logistics.lom.utils.ydb.converter.OrderCombinedRouteHistoryYdbConverter;
import ru.yandex.market.logistics.management.entity.type.ServiceCodeName;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("Интеграционные тесты пересчета маршрутов")
@DatabaseSetup("/service/order/combinator/before/order_set_up.xml")
class RouteRecalculationServiceImplTest extends AbstractCombinedRouteTest {

    public static final Instant START_TIME = Instant.parse("2022-03-31T10:00:00.00Z");
    protected static final UUID NEW_UUID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final String DISABLE_SKIP_SCHEDULE_REAR_FLAG = "disable_skip_schedule=1";
    @Autowired
    private RouteRecalculationService routeRecalculationService;
    @Autowired
    private OrderCombinedRouteHistoryYdbConverter converter;
    @Autowired
    private CombinatorGrpcClient combinatorGrpcClient;
    @Autowired
    private FeatureProperties featureProperties;
    @Autowired
    private UuidGenerator uuidGenerator;
    @Captor
    private ArgumentCaptor<CombinatorOuterClass.RecalculationRequest> requestArgumentCaptor;

    @Nonnull
    @SneakyThrows
    private static CombinatorOuterClass.RecalculationResponse buildResponse() {
        return CombinatorOuterClass.RecalculationResponse.newBuilder()
            .setRoute(buildRoute(buildDate(20), buildDate(30)))
            .build();
    }

    @Nonnull
    private static CombinatorOuterClass.DeliveryRoute buildRoute(
        CombinatorOuterClass.Date deliveryDateFrom,
        CombinatorOuterClass.Date deliveryDateTo
    ) {
        CombinatorOuterClass.Route route = CombinatorOuterClass.Route.newBuilder()
            .setDateFrom(deliveryDateFrom)
            .setDateTo(deliveryDateTo)
            .build();

        return CombinatorOuterClass.DeliveryRoute.newBuilder()
            .setRoute(route)
            .build();
    }

    @Nonnull
    private static CombinatorOuterClass.Date buildDate(int day) {
        return CombinatorOuterClass.Date.newBuilder()
            .setYear(2022)
            .setMonth(3)
            .setDay(day)
            .build();
    }

    @Test
    @DisplayName("Интеграционный тест успешного пересчета")
    void testSuccessRouteRecalculation() {
        insertAllIntoTable(
            routeHistoryTable,
            List.of(combinedRoute("orders/recalculate_delivery_date/combined_route_1001.json")),
            converter::mapToItem
        );
        doReturn(buildResponse()).when(combinatorGrpcClient).recalculateRoute(any());
        doReturn(NEW_UUID).when(uuidGenerator).randomUuid();
        RouteRecalculationParams routeRecalculationParams = RouteRecalculationParams.builder()
            .orderId(1001L)
            .startSegmentId(1L)
            .startService(ServiceCodeName.INBOUND)
            .startTime(START_TIME)
            .build();
        CombinedRoute route = routeRecalculationService.recalculateRoute(routeRecalculationParams);
        CombinedRoute expectedRoute = combinedRoute(
            1001,
            NEW_UUID,
            "service/order/combinator/after/expected_route.json"
        );
        softly.assertThat(route).isEqualTo(expectedRoute);
        verifyRecalculateRoute(ServiceCodeName.INBOUND.name(), 612909L, false);
        verify(newRepository).getRouteByUuid(EXISTING_UUID);
        verify(newRepository).saveRoute(any(), any());
    }

    @Test
    @DisplayName("Передавать флаг, если пересчет с сц перед сц мк и был out с сц перед сц мк")
    @DatabaseSetup(value = "/service/order/combinator/before/out_on_second.xml", type = DatabaseOperation.INSERT)
    void passFlagIfThereRecalculationFromScBeforeScMk() {
        insertAllIntoTable(
            routeHistoryTable,
            List.of(combinedRoute("orders/recalculate_delivery_date/combined_route_1001.json")),
            converter::mapToItem
        );
        doReturn(buildResponse()).when(combinatorGrpcClient).recalculateRoute(any());
        doReturn(NEW_UUID).when(uuidGenerator).randomUuid();
        RouteRecalculationParams routeRecalculationParams = RouteRecalculationParams.builder()
            .orderId(1001L)
            .startSegmentId(2L)
            .startService(ServiceCodeName.SHIPMENT)
            .startTime(START_TIME)
            .build();
        CombinedRoute route = routeRecalculationService.recalculateRoute(routeRecalculationParams);
        CombinedRoute expectedRoute = combinedRoute(
            1001,
            NEW_UUID,
            "service/order/combinator/after/expected_route.json"
        );
        softly.assertThat(route).isEqualTo(expectedRoute);
        verifyRecalculateRoute(ServiceCodeName.SHIPMENT.name(), 614085L, true);
        verify(newRepository).getRouteByUuid(EXISTING_UUID);
        verify(newRepository).saveRoute(any(), any());
    }

    @Test
    @DisplayName("Передавать флаг, если пересчет с сц мк и был out с сц перед сц мк")
    @DatabaseSetup(value = "/service/order/combinator/before/out_on_second.xml", type = DatabaseOperation.INSERT)
    void passFlagIfThereRecalculationFromScMk() {
        insertAllIntoTable(
            routeHistoryTable,
            List.of(combinedRoute("orders/recalculate_delivery_date/combined_route_1001.json")),
            converter::mapToItem
        );
        doReturn(buildResponse()).when(combinatorGrpcClient).recalculateRoute(any());
        doReturn(NEW_UUID).when(uuidGenerator).randomUuid();
        RouteRecalculationParams routeRecalculationParams = RouteRecalculationParams.builder()
            .orderId(1001L)
            .startSegmentId(3L)
            .startService(ServiceCodeName.SHIPMENT)
            .startTime(START_TIME)
            .build();
        CombinedRoute route = routeRecalculationService.recalculateRoute(routeRecalculationParams);
        CombinedRoute expectedRoute = combinedRoute(
            1001,
            NEW_UUID,
            "service/order/combinator/after/expected_route.json"
        );
        softly.assertThat(route).isEqualTo(expectedRoute);
        verifyRecalculateRoute(ServiceCodeName.SHIPMENT.name(), 703279L, true);
        verify(newRepository).getRouteByUuid(EXISTING_UUID);
        verify(newRepository).saveRoute(any(), any());
    }

    @Test
    @DisplayName("Не передавать флаг игнорирования расписания, если не было отгрузки с сц до сц мк")
    void doNotPassFlagIfThereIsNoOutOnScBeforeScMk() {
        insertAllIntoTable(
            routeHistoryTable,
            List.of(combinedRoute("orders/recalculate_delivery_date/combined_route_1001.json")),
            converter::mapToItem
        );
        doReturn(buildResponse()).when(combinatorGrpcClient).recalculateRoute(any());
        doReturn(NEW_UUID).when(uuidGenerator).randomUuid();
        RouteRecalculationParams routeRecalculationParams = RouteRecalculationParams.builder()
            .orderId(1001L)
            .startSegmentId(2L)
            .startService(ServiceCodeName.SHIPMENT)
            .startTime(START_TIME)
            .build();
        CombinedRoute route = routeRecalculationService.recalculateRoute(routeRecalculationParams);
        CombinedRoute expectedRoute = combinedRoute(
            1001,
            NEW_UUID,
            "service/order/combinator/after/expected_route.json"
        );
        softly.assertThat(route).isEqualTo(expectedRoute);
        verifyRecalculateRoute(ServiceCodeName.SHIPMENT.name(), 614085L, false);
        verify(newRepository).getRouteByUuid(EXISTING_UUID);
        verify(newRepository).saveRoute(any(), any());
    }

    @Test
    @DisplayName("Интеграционный тест успешного пересчета нескольких заказов")
    void testSuccessRoutesRecalculation() {
        int ordersListSize = 30;
        featureProperties.setRouteRecalculationThreads(20);
        insertAllIntoTable(
            routeHistoryTable,
            List.of(combinedRoute("orders/recalculate_delivery_date/combined_route_1001.json")),
            converter::mapToItem
        );
        List<RouteRecalculationParams> dtoList = Stream.iterate(0, (value) -> value + 1)
            .limit(ordersListSize)
            .map(value -> RouteRecalculationParams.builder()
                .orderId(1001L)
                .startSegmentId((value % 4L) + 1)
                .startService(ServiceCodeName.INBOUND)
                .startTime(START_TIME)
                .build()
            )
            .collect(Collectors.toList());
        doAnswer(
            invocation -> {
                Thread.sleep(100);
                return buildResponse();
            }
        ).when(combinatorGrpcClient).recalculateRoute(any());
        long startTime = System.nanoTime();
        List<RouteRecalculationResult> routeRecalculationResults = routeRecalculationService.recalculateRoutes(dtoList);
        long computationTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        softly.assertThat(dtoList.size()).isEqualTo(routeRecalculationResults.size());
        for (int i = 0; i < dtoList.size(); i++) {
            softly.assertThat(dtoList.get(i).getOrderId()).isEqualTo(routeRecalculationResults.get(i).getOrderId());
            softly.assertThat(routeRecalculationResults.get(i).getStatus()).isEqualTo(RecalculationStatus.SUCCESS);
            softly.assertThat(routeRecalculationResults.get(i).getNewRoute()).isNotNull();
        }
        verify(newRepository, times(ordersListSize)).getRouteByUuid(EXISTING_UUID);
        verify(newRepository, times(ordersListSize)).saveRoute(any(), any());
    }

    @Nonnull
    protected CombinedRoute combinedRoute(String routeFileName) {
        return combinedRoute(1001, EXISTING_UUID, routeFileName);
    }

    private void verifyRecalculateRoute(String serviceCode, long segmentId, boolean hasRearFlag) {
        verify(combinatorGrpcClient).recalculateRoute(requestArgumentCaptor.capture());
        CombinatorOuterClass.RecalculationRequest request = requestArgumentCaptor.getValue();

        softly.assertThat(request).isNotNull();
        softly.assertThat(request.getSegmentId()).isEqualTo(segmentId);
        softly.assertThat(request.getServiceCode()).isEqualTo(serviceCode);
        softly.assertThat(request.getStartTime().getSeconds()).isEqualTo(START_TIME.getEpochSecond());
        softly.assertThat(request.getDeliveryType()).isEqualTo(Common.DeliveryType.COURIER);
        softly.assertThat(request.getRoute()).isNotNull();
        softly.assertThat(request.getDestinationRegionId()).isEqualTo(10000L);
        softly.assertThat(request.getOrderId()).isEqualTo("1002");
        if (hasRearFlag) {
            softly.assertThat(request.getRearrFactors()).isEqualTo(DISABLE_SKIP_SCHEDULE_REAR_FLAG);
        } else {
            softly.assertThat(request.getRearrFactors()).isEmpty();
        }
    }
}
