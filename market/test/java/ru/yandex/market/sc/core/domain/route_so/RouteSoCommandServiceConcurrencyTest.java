package ru.yandex.market.sc.core.domain.route_so;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.SortableTestFactory;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedDbTest
public class RouteSoCommandServiceConcurrencyTest {

    @MockBean
    Clock clock;

    @Autowired
    TestFactory testFactory;

    @Autowired
    SortableTestFactory sortableTestFactory;

    @Autowired
    XDocFlow flow;

    SortingCenter sortingCenter;

    @BeforeEach
    void setUp() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.storedWarehouse();
        testFactory.storedUser(sortingCenter, TestFactory.USER_UID_LONG);
        testFactory.setupMockClock(clock);
    }

    @DisplayName("Одновременно созданные outbound не должны быть направлены в одну и ту же ячейку отгрузки")
    @Test
    void outboundsCreatedAtTheSameTimeShouldNotBeSentToTheSameShipmentCell() throws Exception {
        var samaraWH = testFactory.storedWarehouse("samara");
        var spbWH = testFactory.storedWarehouse("sbp");
        var rostovWH = testFactory.storedWarehouse("rostov");
        var ship1 = testFactory.storedCell(sortingCenter, "SHIP_1", CellType.COURIER, CellSubType.SHIP_XDOC);
        var ship2 = testFactory.storedCell(sortingCenter, "SHIP_2", CellType.COURIER, CellSubType.SHIP_XDOC);
        var ship3 = testFactory.storedCell(sortingCenter, "SHIP_3", CellType.COURIER, CellSubType.SHIP_XDOC);

        var keepSamaraCell = testFactory.storedCell(
                sortingCenter, "SAMARA_KEEP", CellType.BUFFER, CellSubType.BUFFER_XDOC, samaraWH.getYandexId());
        var keepSpbCell = testFactory.storedCell(
                sortingCenter, "SPB_KEEP", CellType.BUFFER, CellSubType.BUFFER_XDOC, spbWH.getYandexId());
        var keepRostovCell = testFactory.storedCell(
                sortingCenter, "ROSTOV_KEEP", CellType.BUFFER, CellSubType.BUFFER_XDOC, rostovWH.getYandexId());

        flow.inboundBuilder("in-1").nextLogisticPoint(samaraWH.getYandexId()).build()
                .linkPallets("XDOC-1")
                .fixInbound();
        flow.inboundBuilder("in-2").nextLogisticPoint(spbWH.getYandexId()).build()
                .linkPallets("XDOC-2")
                .fixInbound();
        flow.inboundBuilder("in-3").nextLogisticPoint(rostovWH.getYandexId()).build()
                .linkPallets("XDOC-3")
                .fixInbound();

        sortableTestFactory.sortByBarcode("XDOC-1", keepSamaraCell.getId());
        sortableTestFactory.sortByBarcode("XDOC-2", keepSpbCell.getId());
        sortableTestFactory.sortByBarcode("XDOC-3", keepRostovCell.getId());

        runSimultaneously(
                () -> flow.outboundBuilder("out-1")
                        .fromTime(Instant.now(clock).plus(2, ChronoUnit.HOURS))
                        .toTime(Instant.now(clock).plus(2, ChronoUnit.HOURS))
                        .logisticPointToExternalId(samaraWH.getYandexId())
                        .courierExternalId("samaraCourier")
                        .toRegistryBuilder()
                        .externalId("out-reg-1")
                        .addRegistryPallets("XDOC-1")
                        .buildRegistry(),
                () -> flow.outboundBuilder("out-2")
                        .fromTime(Instant.now(clock).plus(2, ChronoUnit.HOURS))
                        .toTime(Instant.now(clock).plus(2, ChronoUnit.HOURS))
                        .logisticPointToExternalId(spbWH.getYandexId())
                        .courierExternalId("spbCourier")
                        .toRegistryBuilder()
                        .externalId("out-reg-2")
                        .addRegistryPallets("XDOC-2")
                        .buildRegistry(),
                () -> flow.outboundBuilder("out-3")
                        .fromTime(Instant.now(clock).plus(2, ChronoUnit.HOURS))
                        .toTime(Instant.now(clock).plus(2, ChronoUnit.HOURS))
                        .logisticPointToExternalId(rostovWH.getYandexId())
                        .courierExternalId("rostovCourier")
                        .toRegistryBuilder()
                        .externalId("out-reg-3")
                        .addRegistryPallets("XDOC-3")
                        .buildRegistry()
        );

        var orderDtoSamara = sortableTestFactory.getOrder("XDOC-1");
        assertThat(orderDtoSamara.getAvailableCells()).hasSize(1);
        var firstCellDto = orderDtoSamara.getAvailableCells().get(0);

        var orderDtoSpb = sortableTestFactory.getOrder("XDOC-2");
        assertThat(orderDtoSpb.getAvailableCells()).hasSize(1);
        var secondCellDto = orderDtoSpb.getAvailableCells().get(0);

        var orderDtoRostov = sortableTestFactory.getOrder("XDOC-3");
        assertThat(orderDtoRostov.getAvailableCells()).hasSize(1);
        var thirdCellDto = orderDtoRostov.getAvailableCells().get(0);

        assertThat(List.of(firstCellDto, secondCellDto, thirdCellDto))  // убедимся что Sortable просятся в ячейку отгрузки
                .allMatch(cell -> cell.getType() == CellType.COURIER && cell.getSubType() == CellSubType.SHIP_XDOC);

        // все полученные ячейки разные
        assertThat(firstCellDto)
                .isNotEqualTo(secondCellDto)
                .isNotEqualTo(thirdCellDto);
        assertThat(secondCellDto)
                .isNotEqualTo(thirdCellDto);

        // для сортировки на отгрузку должны быть получены ячейки созданные в начале
        assertThat(List.of(ship1.getId(), ship2.getId(), ship3.getId()))
                .containsExactlyInAnyOrder(firstCellDto.getId(), secondCellDto.getId(), thirdCellDto.getId());
    }

    private static void runSimultaneously(Runnable... runnables) throws InterruptedException {
        var executor = Executors.newFixedThreadPool(runnables.length);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>(runnables.length));
        for (var runnable : runnables) {
            executor.submit(
                    () -> {
                        try {
                            runnable.run();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            exceptions.add(ex);
                        }
                    }
            );
        }
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        Assertions.assertEquals(exceptions.size(), 0,
                "При одновременном запуске " + exceptions.size() + " Runnable завершились ошибкой");
    }
}
