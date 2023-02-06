package ru.yandex.market.sc.core.domain.place;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryService;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author mors741
 */
@EmbeddedDbTest
class PlaceNonBlockingQueryServiceTest {

    @Autowired
    PlaceNonBlockingQueryService placeNonBlockingQueryService;
    @Autowired
    TestFactory testFactory;
    @Autowired
    TransactionTemplate transactionTemplate;
    @MockBean
    Clock clock;

    SortingCenter sortingCenter;

    DeliveryService deliveryService;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        deliveryService = testFactory.storedDeliveryService();
        testFactory.setupMockClock(clock);
    }

    @Test
    void getPlacesInsideCell() {
        var cell = testFactory.storedCell(sortingCenter, "1", CellType.COURIER);
        var places = testFactory.createForToday(order(sortingCenter).places("p1", "p2").build())
                .acceptPlaces("p1").sortPlaces("p1")
                .getPlaces();

        transactionTemplate.execute(ts -> {
            assertThat(placeNonBlockingQueryService.findPlacesInsideCell(cell))
                    .isEqualTo(List.of(places.get("p1")));
            return null;
        });
    }

    @Test
    void getOrdersInsideCellsSinglePlaceSortedToCourier() {
        var cell = testFactory.storedCell(sortingCenter, "1", CellType.COURIER);
        var places = testFactory.createForToday(order(sortingCenter).places("p1", "p2").build())
                .acceptPlaces("p1").sortPlaces("p1")
                .getPlaces();
        transactionTemplate.execute(ts -> {
            assertThat(placeNonBlockingQueryService.getPlacesByCells(List.of(cell)))
                    .isEqualTo(Map.of(cell, List.of(places.get("p1"))));
            return null;
        });
    }
}
