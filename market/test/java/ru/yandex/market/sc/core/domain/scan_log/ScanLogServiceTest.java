package ru.yandex.market.sc.core.domain.scan_log;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.scan_log.event.OrderScanLogEvent;
import ru.yandex.market.sc.core.domain.scan_log.model.OrderScanLogRequest;
import ru.yandex.market.sc.core.domain.scan_log.model.PartnerOrderScanLogEntryDto;
import ru.yandex.market.sc.core.domain.scan_log.model.ScanLogContext;
import ru.yandex.market.sc.core.domain.scan_log.model.ScanLogOperation;
import ru.yandex.market.sc.core.domain.scan_log.model.ScanLogResult;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author valter
 */
@EmbeddedDbTest
class ScanLogServiceTest {

    private static final String ORDER_ID = "123";

    @Autowired
    ScanLogService scanLogService;
    @Autowired
    ApplicationEventPublisher eventPublisher;
    @Autowired
    TestFactory testFactory;
    @Autowired
    Clock clock;

    SortingCenter sortingCenter;
    User dispatcher;
    Cell cellBefore;
    Cell cellAfter;
    Cell parentCell;
    SortableLot lotBefore;
    SortableLot lotAfter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        dispatcher = testFactory.storedUser(sortingCenter, 1L);
        cellBefore = testFactory.storedActiveCell(sortingCenter, CellType.COURIER, "1");
        cellAfter = testFactory.storedActiveCell(sortingCenter, CellType.COURIER, "2");
        parentCell = testFactory.storedCell(sortingCenter, "cell-lot", CellType.RETURN);
        lotBefore = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);
        lotAfter = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);
    }

    @Test
    void getOrderScanLogFull() {
        writeRequest(fullRequest());
        assertThat(scanLogService.getOrderScanLog(sortingCenter, ORDER_ID))
                .isEqualTo(List.of(fullPartnerEntry()));
    }

    @Test
    void getOrderScanLogMinimal() {
        writeRequest(minimalRequest());
        assertThat(scanLogService.getOrderScanLog(sortingCenter, ORDER_ID))
                .isEqualTo(List.of(minimalPartnerEntry()));
    }

    private OrderScanLogRequest fullRequest() {
        return OrderScanLogRequest.builder()
                .sortingCenter(sortingCenter)
                .scannedAt(Instant.now(clock))
                .externalOrderId(ORDER_ID)
                .externalPlaceId(ORDER_ID + "1")
                .dispatchPersonId(dispatcher.getId())
                .operation(ScanLogOperation.SCAN)
                .context(ScanLogContext.COURIER_SHIP)
                .cellBeforeId(cellBefore.getId())
                .cellAfterId(cellAfter.getId())
                .lotBeforeId(lotBefore.getLotId())
                .lotAfterId(lotAfter.getLotId())
                .parentBeforeId(lotBefore.getSortableId())
                .parentAfterId(lotAfter.getSortableId())
                .result(ScanLogResult.OK)
                .build();
    }

    private OrderScanLogRequest minimalRequest() {
        return OrderScanLogRequest.builder()
                .sortingCenter(sortingCenter)
                .scannedAt(Instant.now(clock))
                .externalOrderId(ORDER_ID)
                .dispatchPersonId(dispatcher.getId())
                .operation(ScanLogOperation.SCAN)
                .result(ScanLogResult.OK)
                .build();
    }

    private PartnerOrderScanLogEntryDto fullPartnerEntry() {
        return new PartnerOrderScanLogEntryDto(
                LocalDateTime.now(clock), null, null, ORDER_ID, ORDER_ID + "1",
                dispatcher.getName(), ScanLogOperation.SCAN, ScanLogContext.COURIER_SHIP,
                cellBefore.getScNumber(), null, parentCell.getScNumber(), null, lotAfter.getBarcode(), ScanLogResult.OK
        );
    }

    private PartnerOrderScanLogEntryDto minimalPartnerEntry() {
        return new PartnerOrderScanLogEntryDto(
                LocalDateTime.now(clock), null, null, ORDER_ID, null,
                dispatcher.getName(), ScanLogOperation.SCAN, null,
                null, null, null, null, null, ScanLogResult.OK
        );
    }

    private void writeRequest(OrderScanLogRequest request) {
        eventPublisher.publishEvent(new OrderScanLogEvent(request));
    }

}
