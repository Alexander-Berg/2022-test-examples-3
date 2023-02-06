package ru.yandex.market.sc.core.domain.transfer_act;

import java.time.Clock;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryService;
import ru.yandex.market.sc.core.domain.order.model.ScOrderState;
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
class OnlineTransferActPlaceQueryServiceTest {

    @MockBean
    Clock clock;

    private final TestFactory testFactory;
    private final OnlineTransferActPlaceQueryService onlineTransferActPlaceQueryService;
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
    void getOrdersWithPlaces() {
        var order1 = testFactory.createForToday(order(sortingCenter, "1").build())
                .accept()
                .sort()
                .get();
        var order2 = testFactory.createForToday(order(sortingCenter, "2").places("2-1", "2-2").build())
                .acceptPlaces("2-1", "2-2")
                .sortPlaces("2-1", "2-2")
                .get();

        var outgoingCourierRoute = testFactory.findOutgoingCourierRoute(order2).orElseThrow();
        var cell = testFactory.determineRouteCell(outgoingCourierRoute, order2);
        routeCommandService.finishOutgoingRouteWithCell(new RouteFinishByCellsRequest(
                testFactory.getRouteIdForSortableFlow(outgoingCourierRoute), new ScContext(dispatcher),
                List.of(cell.getId()),
                null,
                false
        ));

        List<OnlineTransferActPlaceQueryService.PlaceDetails> ordersWithPlaces =
                onlineTransferActPlaceQueryService.getOrdersWithPlaces(sortingCenter, Set.of("1", "2"));

        assertThat(ordersWithPlaces)
                .containsExactlyInAnyOrder(
                        new OnlineTransferActPlaceQueryService.PlaceDetails(
                                "1",
                                "1",
                                ScOrderState.SHIPPED,
                                cell.getCellName().orElse(null)
                        ),
                        new OnlineTransferActPlaceQueryService.PlaceDetails(
                                "2",
                        "2-1",
                        ScOrderState.SHIPPED,
                                cell.getCellName().orElse(null)
                        ),
                        new OnlineTransferActPlaceQueryService.PlaceDetails(
                                "2",
                                "2-2",
                                ScOrderState.SHIPPED,
                                cell.getCellName().orElse(null)
                        )
                );
    }
}
