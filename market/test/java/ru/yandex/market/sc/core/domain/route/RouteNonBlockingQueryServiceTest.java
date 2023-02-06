package ru.yandex.market.sc.core.domain.route;

import java.time.Clock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@EmbeddedDbTest
class RouteNonBlockingQueryServiceTest {

    @Autowired
    TestFactory testFactory;
    @Autowired
    RouteNonBlockingQueryService routeNonBlockingQueryService;
    @Autowired
    Clock clock;
    @Autowired
    TransactionTemplate transactionTemplate;

    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
    }

    @Test
    void findCellActiveRouteCourier() {


        var cell = testFactory.storedCell(sortingCenter, "1", CellType.COURIER);
        var order = testFactory.createOrderForToday(sortingCenter).get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                .orElseThrow().allowReading();


        transactionTemplate.execute(ts -> {
            assertThat(routeNonBlockingQueryService.findCellActiveRouteForToday(cell).orElseThrow().allowReading())
                    .isEqualTo(route);
            return null;
        });
    }

    @Test
    void findCellActiveRouteReturn() {
        var cell = testFactory.storedCell(sortingCenter, "1", CellType.RETURN);
        var order = testFactory.createOrderForToday(sortingCenter).cancel().accept().get();
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        transactionTemplate.execute(ts -> {
            assertThat(routeNonBlockingQueryService.findCellActiveRouteForToday(cell).orElseThrow().allowReading())
                    .isEqualTo(route);
            return null;
        });
    }

    @Test
    void findCellActiveRouteBuffer() {
        var cell = testFactory.storedCell(sortingCenter, "1", CellType.BUFFER);
        testFactory.createForToday(order(sortingCenter).externalId("o1").build()).cancel().accept().get();
        testFactory.createForToday(order(sortingCenter).externalId("o2").build()).accept().get();
        transactionTemplate.execute(ts -> {
            assertThat(routeNonBlockingQueryService.findCellActiveRouteForToday(cell)).isEmpty();
            return null;
        });
    }

}
