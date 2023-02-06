package ru.yandex.market.sc.core.domain.transfer_act;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryService;
import ru.yandex.market.sc.core.domain.route.RouteCommandService;
import ru.yandex.market.sc.core.domain.route.model.RouteFinishByCellsRequest;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.resolver.dto.ScContext;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OnlineTransferActRouteQueryServiceTest {

    @MockBean
    Clock clock;

    private final TestFactory testFactory;
    private final OnlineTransferActRouteQueryService onlineTransferActRouteQueryService;
    private final RouteCommandService routeCommandService;

    SortingCenter sortingCenter;
    Warehouse warehouse;
    DeliveryService deliveryService;
    User dispatcher;

    @BeforeEach
    void init() {
        testFactory.setupMockClock(clock);
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        warehouse = testFactory.storedWarehouse();
        deliveryService = testFactory.storedDeliveryService("1");
        dispatcher = testFactory.storedUser(sortingCenter, 123L);

        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, false);
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, true);
    }

    @Test
    void getLastStockmanSignatureDataByCourier() {
        var order1 = testFactory.createForToday(order(sortingCenter, "1").build())
                .accept()
                .sort()
                .get();

        var outgoingCourierRoute = testFactory.findOutgoingCourierRoute(order1).orElseThrow();
        var cell = testFactory.determineRouteCell(outgoingCourierRoute, order1);
        routeCommandService.finishOutgoingRouteWithCell(new RouteFinishByCellsRequest(
                testFactory.getRouteIdForSortableFlow(outgoingCourierRoute),
                new ScContext(dispatcher),
                List.of(cell.getId()),
                null,
                false
        ));

        var transferDto = OnlineTransferActTestFactory.createTransferDto(
                "1",
                LocalDate.now(clock),
                sortingCenter.getYandexId(),
                String.valueOf(testFactory.defaultCourier().getId()),
                List.of(),
                List.of()
        );
        OnlineTransferActRouteQueryService.SignatureData signatureData =
                onlineTransferActRouteQueryService.getLastStockmanSignatureData(
                sortingCenter,
                transferDto
        );

        assertThat(signatureData).isEqualTo(new OnlineTransferActRouteQueryService.SignatureData(
                dispatcher.getUid(),
                dispatcher.getName()
        ));
    }

    @Test
    void getLastStockmanSignatureDataByOrder() {
        var order1 = testFactory.createForToday(order(sortingCenter, "order-1").build())
                .accept(dispatcher)
                .sort(dispatcher)
                .get();

        var outgoingCourierRoute = testFactory.findOutgoingCourierRoute(order1).orElseThrow();
        var cell = testFactory.determineRouteCell(outgoingCourierRoute, order1);

        testFactory.setupMockClock(clock, clock.instant().plus(1, ChronoUnit.MINUTES));

        routeCommandService.finishOutgoingRouteWithCell(new RouteFinishByCellsRequest(
                testFactory.getRouteIdForSortableFlow(outgoingCourierRoute),
                new ScContext(dispatcher),
                List.of(cell.getId()),
                null,
                false
        ));

        var unknownCourier = testFactory.storedCourier(274L, "unknown-courier");
        var transferDto = OnlineTransferActTestFactory.createTransferDto(
                "1",
                LocalDate.now(clock),
                sortingCenter.getYandexId(),
                String.valueOf(unknownCourier.getId()),
                List.of("order-1"),
                List.of()
        );
        OnlineTransferActRouteQueryService.SignatureData signatureData =
                onlineTransferActRouteQueryService.getLastStockmanSignatureData(
                        sortingCenter,
                        transferDto
                );

        assertThat(signatureData).isEqualTo(new OnlineTransferActRouteQueryService.SignatureData(
                dispatcher.getUid(),
                dispatcher.getName()
        ));
    }

}
