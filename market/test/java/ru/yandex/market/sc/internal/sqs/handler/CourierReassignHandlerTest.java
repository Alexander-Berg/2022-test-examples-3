package ru.yandex.market.sc.internal.sqs.handler;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.core.domain.cell.policy.OrderCellParams;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryService;
import ru.yandex.market.sc.core.domain.order.OrderNonBlockingQueryService;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.route.RouteCommandService;
import ru.yandex.market.sc.core.domain.route.model.RouteCreateRequest;
import ru.yandex.market.sc.core.domain.route.model.RouteType;
import ru.yandex.market.sc.core.domain.route.repository.Route;
import ru.yandex.market.sc.core.domain.route.repository.RouteRepository;
import ru.yandex.market.sc.core.domain.route_so.RouteSoCommandService;
import ru.yandex.market.sc.core.domain.route_so.model.RouteSoCreateRequest;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.ScDateUtils;
import ru.yandex.market.sc.internal.test.EmbeddedDbIntTest;
import ru.yandex.market.sc.internal.util.SqsEventFactory;
import ru.yandex.market.tpl.common.util.datetime.LocalDateInterval;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbIntTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CourierReassignHandlerTest {
    private final CourierReassignHandler courierReassignHandler;
    private final TestFactory testFactory;
    private final RouteCommandService routeCommandService;
    private final RouteSoCommandService routeSoCommandService;
    private final RouteRepository routeRepository;
    private final SqsEventFactory sqsEventFactory;
    private final TransactionTemplate transactionTemplate;
    private final OrderNonBlockingQueryService orderNonBlockingQueryService;

    @MockBean
    Clock clock;

    SortingCenter sortingCenter;
    DeliveryService deliveryService;
    Cell cell;
    Courier courier1;
    Courier courier2;
    Warehouse warehouse;

    @BeforeEach
    void init() {
        testFactory.setupMockClock(clock);
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(), SortingCenterPropertiesKey.REASSIGN_COURIER_BY_TPL_ENABLED, "true");
        cell = testFactory.storedCell(sortingCenter);
        courier1 = testFactory.storedCourier(777L);
        courier2 = testFactory.storedCourier(888L);
        warehouse = testFactory.storedWarehouse();
        deliveryService = testFactory.storedDeliveryService("1");
    }

    @Test
    void shouldReassignCourierWhenCameEvent() {
        var request = routeCreateRequest(RouteType.OUTGOING_COURIER, courier1);
        var route = createRouteIfNotExistsAndSelect(request);
        testFactory.create(order(sortingCenter, "1").build())
                .updateCourier(courier1).updateShipmentDate(LocalDate.now(clock))
                .accept().sort().get();

        var event = sqsEventFactory.createCourierReassignEvent(
                courier1, courier2, sortingCenter, LocalDate.now(clock)
        );

        assertThat(route.getCourierTo()).isEqualTo(courier1);
        courierReassignHandler.handle(event);

        var updatedRoute = routeRepository.findByIdOrThrow(route.getId());

        assertThat(updatedRoute.getCourierTo()).isEqualTo(courier2);
        transactionTemplate.execute(ts -> {
            List<Place> updatedPlaces = orderNonBlockingQueryService.getPlaces(updatedRoute);
            assertThat(updatedPlaces.stream().map(Place::getCourier).toList())
                    .containsOnly(courier2);
            return null;
        });
    }


    @Test
    void shouldReassignCourierWhenRouteExist() {
        var request1 = routeCreateRequest(RouteType.OUTGOING_COURIER, courier1);
        var route1 = createRouteIfNotExistsAndSelect(request1);
        testFactory.create(order(sortingCenter, "1").build())
                .updateCourier(courier1).updateShipmentDate(LocalDate.now(clock))
                .accept().sort().get();

        var request2 = routeCreateRequest(RouteType.OUTGOING_COURIER, courier2);
        var route2 = createRouteIfNotExistsAndSelect(request2);

        assertThat(route1.getCourierTo()).isEqualTo(courier1);

        var event = sqsEventFactory.createCourierReassignEvent(
                courier1, courier2, sortingCenter, LocalDate.now(clock)
        );

        courierReassignHandler.handle(event);

        var updatedRoute = routeRepository.findByIdOrThrow(route2.getId());

        assertThat(updatedRoute.getCourierTo()).isEqualTo(courier2);
        transactionTemplate.execute(ts -> {
            List<Place> updatedPlaces = orderNonBlockingQueryService.getPlaces(updatedRoute);
            assertThat(updatedPlaces.stream().map(Place::getCourier).toList())
                    .containsOnly(courier2);
            return null;
        });
    }


    private RouteCreateRequest routeCreateRequest(RouteType routeType, Courier courier) {
        return routeCreateRequest(routeType, courier, LocalDate.now(clock));
    }

    private RouteCreateRequest routeCreateRequest(RouteType routeType, Courier courier, LocalDate expectedDate) {
        return new RouteCreateRequest(
                routeType,
                sortingCenter,
                expectedDate,
                new LocalDateInterval(expectedDate, expectedDate),
                null,
                warehouse,
                courier,
                cellParams(routeType, courier, expectedDate, deliveryService, false),
                null
        );
    }

    private OrderCellParams cellParams(RouteType routeType, Courier courier, LocalDate expectedDate,
                                       DeliveryService deliveryService,
                                       boolean isMiddleMile) {
        if (routeType.isIncoming()) {
            return null;
        }
        if (routeType.isCourier()) {
            var builder = testFactory.create(order(sortingCenter)
                            .deliveryService(deliveryService)
                            .externalId("o" + routeType)
                            .warehouseReturnId(warehouse.getYandexId())
                            .dsType(isMiddleMile ? DeliveryServiceType.TRANSIT : DeliveryServiceType.LAST_MILE_COURIER)
                            .build())
                    .updateShipmentDate(expectedDate);
            if (!isMiddleMile) {
                builder = builder.updateCourier(courier);
            }
            return builder.get();
        } else {
            return testFactory.createForToday(order(sortingCenter)
                            .externalId("o" + routeType)
                            .warehouseReturnId(warehouse.getYandexId())
                            .build())
                    .accept().makeReturn().get();
        }
    }

    private Route createRouteIfNotExistsAndSelect(RouteCreateRequest request) {
        routeCommandService.createRouteIfNotExistsAndSetCell(request);
        long routeId = routeCommandService.findRouteIdByRequest(request).orElseThrow();

        var destType =
                testFactory.routeTypeToRouteSoDestinationType(request.getRouteType());
        Long destinationId = switch (destType) {
            case WAREHOUSE -> request.getWarehouse().getId();
            case COURIER -> request.getCourier().getId();
            default -> throw new IllegalStateException("Unexpected value: " + destType);
        };
        routeSoCommandService.createRouteIfNotExistsAndSetCell(
                new RouteSoCreateRequest(
                        testFactory.routeTypeToRouteSoType(request.getRouteType()),
                        request.getSortingCenter().getId(),
                        testFactory.routeTypeToRouteSoDestinationType(request.getRouteType()),
                        destinationId,
                        ScDateUtils.toBeginningOfDay(request.getExpectedDate()),
                        ScDateUtils.toEndOfDay(request.getExpectedDate()),
                        null,
                        null,
                        null,
                        request.getExpectedDate(),
                        request.getSortInterval(),
                        request.getExpectedTime(),
                        request.getCellParams(),
                        request.getPreferCellId()
                )
        );

        return routeRepository.findByIdWithRouteFinish(routeId).orElseThrow().allowReading();
    }
}
