package ru.yandex.market.sc.core.domain.order;

import java.time.Clock;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryService;
import ru.yandex.market.sc.core.domain.lot.repository.LotRepository;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTest
public class OrderLotShippedTest {

    @Autowired
    TestFactory testFactory;

    @Autowired
    LotRepository lotRepository;

    @Autowired
    ScOrderRepository scOrderRepository;

    @Autowired
    PlaceRepository placeRepository;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    Clock clock;

    @Autowired
    TransactionTemplate template;

    SortingCenter sortingCenter;
    User user;
    SortableLot returnPalletLot;
    Cell returnCell;
    DeliveryService deliveryService;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter(1234L);
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        user = testFactory.storedUser(sortingCenter, 123L);
        returnCell = testFactory.storedCell(sortingCenter, "return1", CellType.RETURN);
        returnPalletLot = testFactory.storedLot(sortingCenter, SortableType.PALLET, returnCell);
        deliveryService = testFactory.storedDeliveryService("345");
    }

    @Test
    void shipMultiplaceOrderSinglePlaceFromLotThenFromCell() {
        var order = testFactory.createForToday(
                        order(sortingCenter)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .deliveryService(deliveryService)
                                .externalId("o1")
                                .places("p1", "p2").build()
                ).cancel()
                .acceptPlaces("p1", "p2")
                .sortPlace("p1", returnCell.getId())
                .sortPlace("p2", returnCell.getId())
                .sortPlaceToLot("SC_LOT_100000", SortableType.PALLET, "p2")
                .prepareToShipLot(1)
                .get();

        var p1 = testFactory.orderPlace(order, "p1");
        assertThat(p1.getSortableStatus()).isEqualTo(SortableStatus.SORTED_RETURN);
        assertThat(p1.getCellId()).isEqualTo(Optional.of(returnCell.getId()));
        assertThat(p1.getLot()).isNull();

        var p2 = testFactory.orderPlace(order, "p2");
        assertThat(p2.getSortableStatus()).isEqualTo(SortableStatus.SORTED_RETURN);
        assertThat(p2.getCellId()).isEmpty();
        assertThat(p2.getLot()).isNotNull();

        testFactory.shipLots(testFactory.getRoutable(testFactory.findOutgoingWarehouseRoute(p1)
                .orElseThrow()).getId(), sortingCenter);

        p1 = testFactory.updated(p1);
        assertThat(p1.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);

        assertThat(p1.getSortableStatus()).isEqualTo(SortableStatus.SORTED_RETURN);
        assertThat(p1.getCellId()).isEqualTo(Optional.of(returnCell.getId()));
        assertThat(p1.getParent()).isNull();

        p2 = testFactory.updated(p2);
        assertThat(p2.getSortableStatus()).isEqualTo(SortableStatus.SHIPPED_RETURN);
        assertThat(p2.getCellId()).isEmpty();
        assertThat(p2.getParent()).isNull();

        testFactory.shipOrderRoute(p1);

        p1 = testFactory.updated(p1);
        assertThat(p1.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);

        assertThat(p1.getSortableStatus()).isEqualTo(SortableStatus.SHIPPED_RETURN);
        assertThat(p1.getCellId()).isEmpty();
        assertThat(p1.getParent()).isNull();

        p2 = testFactory.updated(p2);
        assertThat(p2.getSortableStatus()).isEqualTo(SortableStatus.SHIPPED_RETURN);
        assertThat(p2.getCellId()).isEmpty();
        assertThat(p2.getParent()).isNull();
    }

    @Test
    void shipMultiplaceOrderSinglePlaceFromCellThenFromLot() {
        var order = testFactory.createForToday(
                        order(sortingCenter).externalId("o1").places("p1", "p2").build()
                ).cancel()
                .acceptPlaces("p1", "p2")
                .sortPlace("p1", returnCell.getId())
                .sortPlace("p2", returnCell.getId())
                .sortPlaceToLot("SC_LOT_100000", SortableType.PALLET, "p2")
                .prepareToShipLot(1)
                .get();

        var p1 = testFactory.orderPlace(order, "p1");
        assertThat(p1.getSortableStatus()).isEqualTo(SortableStatus.SORTED_RETURN);
        assertThat(p1.getCellId()).isEqualTo(Optional.of(returnCell.getId()));
        assertThat(p1.getParent()).isNull();

        var p2 = testFactory.orderPlace(order, "p2");
        assertThat(p2.getSortableStatus()).isEqualTo(SortableStatus.SORTED_RETURN);
        assertThat(p2.getCellId()).isEmpty();
        assertThat(p2.getParent()).isNotNull();


        testFactory.shipOrderRoute(p1);

        p1 = testFactory.updated(p1);
        assertThat(p1.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);

        assertThat(p1.getSortableStatus()).isEqualTo(SortableStatus.SHIPPED_RETURN);
        assertThat(p1.getCellId()).isEmpty();
        assertThat(p1.getParent()).isNull();

        p2 = testFactory.updated(p2);
        assertThat(p2.getSortableStatus()).isEqualTo(SortableStatus.SORTED_RETURN);
        assertThat(p2.getCellId()).isEmpty();
        assertThat(p2.getParent()).isNotNull();

        testFactory.shipLots(
                testFactory.getRoutable(testFactory.findOutgoingWarehouseRoute(p2).orElseThrow()).getId(),
                sortingCenter
        );

        p1 = testFactory.updated(p1);
        assertThat(p1.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);

        assertThat(p1.getSortableStatus()).isEqualTo(SortableStatus.SHIPPED_RETURN);
        assertThat(p1.getCellId()).isEmpty();
        assertThat(p1.getParent()).isNull();

        p2 = testFactory.updated(p2);
        assertThat(p2.getSortableStatus()).isEqualTo(SortableStatus.SHIPPED_RETURN);
        assertThat(p2.getCellId()).isEmpty();
        assertThat(p2.getParent()).isNull();
    }

}
