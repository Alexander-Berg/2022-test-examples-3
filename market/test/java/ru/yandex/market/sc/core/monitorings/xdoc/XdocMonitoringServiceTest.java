package ru.yandex.market.sc.core.monitorings.xdoc;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.core.domain.sortable.SortableQueryService;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.monitorings.xdoc.model.InboundTelegramNotificationDto;
import ru.yandex.market.sc.core.monitorings.xdoc.model.LotTelegramNotificationDto;
import ru.yandex.market.sc.core.monitorings.xdoc.model.OutboundTelegramNotificationDto;
import ru.yandex.market.sc.core.monitorings.xdoc.model.SortableTelegramNotificationDto;
import ru.yandex.market.sc.core.test.DefaultScUserWarehouseExtension;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;

import static org.mockito.Mockito.verify;


@EmbeddedDbTest
@ExtendWith(DefaultScUserWarehouseExtension.class)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class XdocMonitoringServiceTest {

    private final XDocFlow flow;
    private final TestFactory testFactory;
    @Spy
    private final XdocMonitoringService xdocMonitoringService;
    private final SortableQueryService sortableQueryService;

    @MockBean
    Clock clock;
    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter(TestFactory.SortingCenterParams.builder()
                .id(TestFactory.SC_ID)
                .regionSuffix("test-1")
                .build());
        testFactory.setSortingCenterProperty(flow.getSortingCenter(), SortingCenterPropertiesKey.XDOC_ENABLED, "true");
        testFactory.setupMockClock(clock, Instant.now());
    }

    @SneakyThrows
    @Test
    void notifyKeepedPallets() {
        flow.inboundBuilder("in-1")
                .informationListBarcode("Зп-1")
                .build()
                .linkPallets("XDOC-1")
                .fixInbound()
                .sortToAvailableCell("XDOC-1");
        testFactory.setupMockClock(clock, clock.instant().plus(10, ChronoUnit.DAYS));
        xdocMonitoringService.notifyKeepedPallets();
        verify(xdocMonitoringService).pushKeepedSortable(flow.getSortingCenter(),
                List.of(new SortableTelegramNotificationDto("XDOC-1",
                        new InboundTelegramNotificationDto("Зп-1",
                                testFactory.findWarehouseBy(TestFactory.WAREHOUSE_YANDEX_ID).getIncorporation(),
                                flow.getSortingCenter().getId()))));
    }

    @SneakyThrows
    @Test
    void notifySameStatus() {
        flow.inboundBuilder("in-1")
                .informationListBarcode("Зп-1")
                .build()
                .linkPallets("XDOC-1")
                .fixInbound()
                .createOutbound("out01")
                .buildRegistry("XDOC-1")
                .sortToAvailableCell("XDOC-1");
        testFactory.setupMockClock(clock, clock.instant().plus(10, ChronoUnit.DAYS));
        xdocMonitoringService.notifyPalletsInSameStatus();
        verify(xdocMonitoringService).pushSortableSameStatus(flow.getSortingCenter(),
                List.of(new SortableTelegramNotificationDto("XDOC-1",
                        new InboundTelegramNotificationDto("Зп-1",
                                testFactory.findWarehouseBy(TestFactory.WAREHOUSE_YANDEX_ID).getIncorporation(),
                                flow.getSortingCenter().getId()))));
    }

    @SneakyThrows
    @Test
    void notifyBoxNotInLot() {
        flow.inboundBuilder("in-1")
                .informationListBarcode("Зп-1")
                .build()
                .linkBoxes("XDOC-1");

        testFactory.setupMockClock(clock, clock.instant().plus(10, ChronoUnit.DAYS));
        xdocMonitoringService.notifyBoxNotInLot();
        verify(xdocMonitoringService).pushBoxNotInLot(flow.getSortingCenter(),
                List.of(new SortableTelegramNotificationDto("XDOC-1",
                        new InboundTelegramNotificationDto("Зп-1",
                                testFactory.findWarehouseBy(TestFactory.WAREHOUSE_YANDEX_ID).getIncorporation(),
                                flow.getSortingCenter().getId()))));
    }

    @SneakyThrows
    @Test
    void notifyInboundUnfixed() {
        flow.inboundBuilder("in-1")
                .informationListBarcode("Зп-1")
                .build()
                .linkPallets("XDOC-1");

        testFactory.setupMockClock(clock, clock.instant().plus(10, ChronoUnit.DAYS));

        xdocMonitoringService.notifyUnfixedInbounds();

        verify(xdocMonitoringService).pushInboundNotFixed(flow.getSortingCenter(),
                List.of(new InboundTelegramNotificationDto("Зп-1",
                        testFactory.findWarehouseBy(TestFactory.WAREHOUSE_YANDEX_ID).getIncorporation(),
                        flow.getSortingCenter().getId())));
    }

    @SneakyThrows
    @Test
    void notifyLotUnpacked() {
        var cell = flow.createBufferCellAndGet("cell-1", TestFactory.WAREHOUSE_YANDEX_ID);
        var lot = flow.createBasket(cell);
        flow.inboundBuilder("in-1")
                .informationListBarcode("Зп-1")
                .build()
                .linkBoxes("XDOC-1")
                .fixInbound();

        Sortable box = sortableQueryService.find(flow.getSortingCenter(), "XDOC-1").orElseThrow();
        flow.sortBoxToLot(box, lot);

        testFactory.setupMockClock(clock, clock.instant().plus(10, ChronoUnit.DAYS));

        xdocMonitoringService.notifyUnpackedLots();


        verify(xdocMonitoringService).pushLotUnpacked(flow.getSortingCenter(),
                List.of(new LotTelegramNotificationDto(flow.getSortingCenter().getId(), lot.getBarcode(),
                        testFactory.storedWarehouse().getIncorporation())));
    }

    @SneakyThrows
    @Test
    void notifyInboundsNotShipped() {
        var cell = flow.createBufferCellAndGet("cell-1", TestFactory.WAREHOUSE_YANDEX_ID);
        var lot = flow.createBasket(cell);
        flow.inboundBuilder("in-1")
                .informationListBarcode("Зп-1")
                .build()
                .linkBoxes("XDOC-1")
                .fixInbound();

        Sortable box = sortableQueryService.find(flow.getSortingCenter(), "XDOC-1").orElseThrow();
        flow.sortBoxToLot(box, lot);

        testFactory.setupMockClock(clock, clock.instant().plus(10, ChronoUnit.DAYS));

        xdocMonitoringService.notifyInboundsNotShipped();

        verify(xdocMonitoringService).pushInboundsNotShipped(flow.getSortingCenter(),
                List.of(new InboundTelegramNotificationDto("Зп-1", TestFactory.warehouse().getIncorporation(),
                        sortingCenter.getId())));
    }

    @SneakyThrows
    @Test
    void notifyOutboundsNotShipped() {
        var cell = flow.createBufferCellAndGet("cell-1", TestFactory.WAREHOUSE_YANDEX_ID);
        var shipCell = flow.createShipCellAndGet("cell-2");
        var lot = flow.createBasket(cell);
        flow.inboundBuilder("in-1")
                .informationListBarcode("Зп-1")
                .build()
                .linkPallets("XDOC-1")
                .fixInbound()
                .createOutbound("out-1")
                .buildRegistry("XDOC-1")
                .sortToAvailableCell("XDOC-1")
                .prepareToShip("XDOC-1");

        testFactory.setupMockClock(clock, clock.instant().plus(1, ChronoUnit.DAYS));

        xdocMonitoringService.notifyOutboundsNotShipped();

        verify(xdocMonitoringService).pushOutboundsNotShipped(flow.getSortingCenter(),
                List.of(new OutboundTelegramNotificationDto("out-1", TestFactory.warehouse().getIncorporation(),
                        sortingCenter.getId())));
    }

}

