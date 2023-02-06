package ru.yandex.market.sc.core.domain.lot;

import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.lot.repository.LotHistory;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LotHistoryFlowTest {

    private final TestFactory testFactory;

    private SortingCenter sortingCenter;
    private Cell returnCell;
    private Cell courierCell;
    private Courier courier;

    @BeforeEach
    void init() {
        courier = testFactory.storedCourier();
        sortingCenter = testFactory.storedSortingCenter();
        returnCell = testFactory.storedCell(sortingCenter, "r1", CellType.RETURN);
        courierCell = testFactory.storedCell(sortingCenter, "c1", CellType.COURIER, courier.getId());
    }

    @Test
    void createLot() {
        SortableLot sortableLot = testFactory.storedLot(sortingCenter, SortableType.PALLET, courierCell);
        List<LotHistory> lotHistories = Objects.requireNonNull(sortableLot.getLot().getHistory());
        assertThat(sortableLot.getLotStatus()).isEqualTo(LotStatus.CREATED);
        assertThat(lotHistories).extracting(LotHistory::getStatus)
                .contains(LotStatus.CREATED);
    }

    @Test
    @Transactional
    void processingLot() {
        SortableLot sortableLot = testFactory.storedLot(sortingCenter, returnCell, LotStatus.CREATED);
        var lot = sortableLot.getLot();
        testFactory.createForToday(order(sortingCenter, "o1").places("p1", "p2").build())
                .cancel().acceptPlaces()
                .enableSortMiddleMileToLot()
                .sortPlaceToLot(sortableLot.getLot().getId(), "p1")
                .sortPlaceToLot(sortableLot.getLot().getId(), "p2")
                .get();
        List<LotHistory> lotHistories = Objects.requireNonNull(testFactory.getLot(lot.getId()).getLot().getHistory());
        sortableLot = testFactory.getLot(sortableLot.getLotId());

        assertThat(sortableLot.getLotStatus()).isEqualTo(LotStatus.PROCESSING);
        assertThat(lotHistories).extracting(LotHistory::getStatus)
                .contains(LotStatus.CREATED, LotStatus.PROCESSING);
    }

    @Test
    @Transactional
    void readyLot() {
        SortableLot sortableLot = testFactory.storedLot(sortingCenter, returnCell, LotStatus.CREATED);
        testFactory.createForToday(order(sortingCenter, "o1").places("p1", "p2").build())
                .cancel().acceptPlaces()
                .enableSortMiddleMileToLot()
                .sortPlaceToLot(sortableLot.getLot().getId(), "p1")
                .sortPlaceToLot(sortableLot.getLot().getId(), "p2")
                .prepareToShipLot(sortableLot.getLotId())
                .get();

        List<LotHistory> lotHistories = Objects.requireNonNull(sortableLot.getLot().getHistory());
        sortableLot = testFactory.getLot(sortableLot.getLotId());

        assertThat(sortableLot.getLotStatus()).isEqualTo(LotStatus.READY);
        assertThat(lotHistories).extracting(LotHistory::getStatus)
                .contains(LotStatus.CREATED, LotStatus.PROCESSING, LotStatus.READY);
    }

    @Test
    @Transactional
    void shippedLot() {
        SortableLot sortableLot = testFactory.storedLot(sortingCenter, returnCell, LotStatus.CREATED);
        var order = testFactory.createForToday(order(sortingCenter, "o1").places("p1", "p2").build())
                .cancel().acceptPlaces()
                .enableSortMiddleMileToLot()
                .sortPlaceToLot(sortableLot.getLot().getId(), "p1")
                .sortPlaceToLot(sortableLot.getLot().getId(), "p2")
                .prepareToShipLot(sortableLot.getLotId())
                .get();

        var route = testFactory.findOutgoingRoute(order).orElseThrow();
        testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);

        sortableLot = testFactory.getLot(sortableLot.getLotId());

        assertThat(sortableLot.getLotStatus()).isEqualTo(LotStatus.SHIPPED);
        assertThat(sortableLot.getLot().getHistory()).extracting(LotHistory::getStatus)
                .contains(LotStatus.CREATED, LotStatus.PROCESSING, LotStatus.READY, LotStatus.SHIPPED);
    }
}
