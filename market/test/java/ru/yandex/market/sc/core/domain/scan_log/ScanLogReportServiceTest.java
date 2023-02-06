package ru.yandex.market.sc.core.domain.scan_log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.scan.ScanService;
import ru.yandex.market.sc.core.domain.scan.model.AcceptOrderRequestDto;
import ru.yandex.market.sc.core.domain.scan.model.PrepareToShipRequestDto;
import ru.yandex.market.sc.core.domain.scan.model.SortableSortRequestDto;
import ru.yandex.market.sc.core.domain.scan_log.model.ScanLogContext;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.resolver.dto.ScContext;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@EmbeddedDbTest
class ScanLogReportServiceTest {

    @Autowired
    ScanService scanService;
    @Autowired
    TestFactory testFactory;
    @Autowired
    ScanLogReportService scanLogReportService;

    SortingCenter sortingCenter;
    User user;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        user = testFactory.storedUser(sortingCenter, 123L);
    }

    @Test
    void getOrderScanLogReport() {
        var cell = testFactory.storedCell(sortingCenter, "c1", CellType.COURIER);
        var order = testFactory.createForToday(
                order(sortingCenter, "o1").places("p1", "p2").build()).get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        scanService.getOrder(order.getExternalId(), "p1", null,
                new ScContext(user, ScanLogContext.INITIAL_ACCEPTANCE)
        );
        scanService.acceptOrder(new AcceptOrderRequestDto(order.getExternalId(), "p1"),
                new ScContext(user, ScanLogContext.SORT));
        scanService.acceptOrder(new AcceptOrderRequestDto(order.getExternalId(), "p2"),
                new ScContext(user, ScanLogContext.SORT));
        scanService.sortSortable(
                new SortableSortRequestDto(order.getExternalId(), "p1", String.valueOf(cell.getId())),
                new ScContext(user, ScanLogContext.SORT));
        scanService.sortSortable(
                new SortableSortRequestDto(order.getExternalId(), "p2", String.valueOf(cell.getId())),
                new ScContext(user, ScanLogContext.SORT));
        scanService.prepareToShipPlace(order.getId(), new PrepareToShipRequestDto(cell.getId(), testFactory.getRouteIdForSortableFlow(route), "p1"),
                new ScContext(user));
        testFactory.shipOrderRoute(order, user);

        var actual = scanLogReportService.getOrderScanLogReport(sortingCenter, order.getExternalId());
        assertThat(actual)
                .allMatch(dto -> dto.getDateTime().equals(LocalDateTime.parse("1970-01-01T03:00:00",
                        DateTimeFormatter.ISO_DATE_TIME)));
    }

}
