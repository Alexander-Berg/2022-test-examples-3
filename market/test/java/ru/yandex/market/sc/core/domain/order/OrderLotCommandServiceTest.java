package ru.yandex.market.sc.core.domain.order;

import java.time.Clock;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryService;
import ru.yandex.market.sc.core.domain.lot.LotCommandService;
import ru.yandex.market.sc.core.domain.lot.repository.LotRepository;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.order.model.ScOrderUpdateEvent;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderUpdateHistoryItem;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.exception.ScException;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM;
import static ru.yandex.market.sc.core.test.SortableFlowSwitcherExtension.testNotMigrated;
import static ru.yandex.market.sc.core.test.TestFactory.order;
import static ru.yandex.market.sc.core.test.TestFactory.useNewSortableFlow;

@EmbeddedDbTest
class OrderLotCommandServiceTest {

    @Autowired
    TestFactory testFactory;

    @Autowired
    LotRepository lotRepository;

    @Autowired
    ScOrderRepository scOrderRepository;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    PlaceRepository placeRepository;

    @Autowired
    LotCommandService lotCommandService;

    @Autowired
    Clock clock;

    SortingCenter sortingCenter;
    User user;
    SortableLot pallet;
    Cell returnCell;
    Cell courierCell;
    Courier courier;
    DeliveryService middleMileDeliveryService;
    DeliveryService pvzDeliveryService;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter(1234L);
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        courier = testFactory.storedCourier(321L, "new courier");
        user = testFactory.storedUser(sortingCenter, 123L);
        returnCell = testFactory.storedCell(sortingCenter, "return1", CellType.RETURN);
        pallet = testFactory.storedLot(sortingCenter, SortableType.PALLET, returnCell);
        courierCell = testFactory.storedCell(sortingCenter, "courier1", CellType.COURIER, courier.getId());
        long id = 123L;
        middleMileDeliveryService = testFactory.storedDeliveryService("10");
        pvzDeliveryService = testFactory.storedDeliveryService("11");
    }

    @Test
    void sortToPalletLotFromCell() {
        if (useNewSortableFlow()) {
            testNotMigrated();
            return;
        }

        var order = testFactory.createOrderForToday(sortingCenter)
                .accept().cancel().sort(returnCell.getId()).get();
        testFactory.sortOrderToLot(order, pallet, user);
        Place place = testFactory.orderPlace(order);
        assertThat(place.getParent()).isEqualTo(pallet.getSortable());
        assertThat(place.getCell()).isNull();
        assertThat(place.getSortableStatus()).isEqualTo(SortableStatus.SORTED_RETURN);
        assertThat(place.getFfStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        checkLotStatus(pallet.getLotId(), LotStatus.PROCESSING);
        transactionTemplate.execute(ts -> {
            var actualOrder = scOrderRepository.findByIdOrThrow(order.getId());
            var eventOfStatus = actualOrder.getUpdateHistoryItems()
                    .stream().filter(i -> i.getEvent() == ScOrderUpdateEvent.UPDATE_LOT).findFirst().orElseThrow();
            ScOrder sortedOrder = testFactory.getOrder(order.getId());
            assertThat(eventOfStatus)
                    .isEqualToComparingOnlyGivenFields(new ScOrderUpdateHistoryItem(sortedOrder,
                            ScOrderUpdateEvent.UPDATE_LOT, Instant.now(clock),
                            null, null, user), "event", "dispatchPerson");
            return null;
        });
    }

    @Test
    void sortToPalletLotDirectly() {
        var place = testFactory.createForToday(
                        order(sortingCenter)
                                .deliveryService(middleMileDeliveryService)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .build())
                .accept().cancel().getPlace();

        var sortedPlace = testFactory.sortPlaceToLot(place, pallet, user);

        assertThat(sortedPlace.getParent()).isEqualTo(pallet.getSortable());
        assertThat(sortedPlace.getCell()).isNull();
        checkLotStatus(pallet.getLotId(), LotStatus.PROCESSING);
        assertThat(sortedPlace.getFfStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);

        transactionTemplate.execute(ts -> {
            Place actualPlace = testFactory.updated(sortedPlace);
            var eventOfStatus = actualPlace.getHistory()
                    .stream().filter(i -> i.getMutableState().getParent() != null).findFirst().orElseThrow();
            assertThat(eventOfStatus)
                    .isEqualToComparingOnlyGivenFields(pallet);
            return null;
        });
    }

    @Test
    void cantDeleteLotWithPlaceInside() {
        var order = testFactory.createForToday(order(sortingCenter)
                        .places("p1", "p2")
                        .externalId("o1")
                        .build())
                .acceptPlaces().cancel()
                .sortPlace("p1", returnCell.getId())
                .sortPlace("p2", returnCell.getId())
                .get();
        var place1 = placeRepository.findByOrderIdAndMainPartnerCode(order.getId(), "p1").get();
        var place2 = placeRepository.findByOrderIdAndMainPartnerCode(order.getId(), "p2").get();
        var sortedPlace1 = testFactory.sortPlaceToLot(place1, pallet, user);
        assertThat(sortedPlace1.getLot().getId()).isEqualTo(pallet.getLotId());
        assertThat(sortedPlace1.getCell()).isNull();
        checkLotStatus(pallet.getLotId(), LotStatus.PROCESSING);
        var sortedOrder1 = scOrderRepository.findByIdOrThrow(order.getId());
        assertThat(sortedOrder1.getFfStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThatThrownBy(() -> lotCommandService.deleteLot(sortingCenter, pallet.getLotId()));
    }


    @Test
    void sortMultiOrderToPalletLotFromCell() {
        var order = testFactory.createForToday(order(sortingCenter)
                        .places("p1", "p2")
                        .externalId("o1")
                        .build())
                .acceptPlaces().cancel()
                .sortPlace("p1", returnCell.getId())
                .sortPlace("p2", returnCell.getId())
                .get();
        var place1 = placeRepository.findByOrderIdAndMainPartnerCode(order.getId(), "p1").get();
        var place2 = placeRepository.findByOrderIdAndMainPartnerCode(order.getId(), "p2").get();
        var sortedPlace1 = testFactory.sortPlaceToLot(place1, pallet, user);
        assertThat(sortedPlace1.getLot().getId()).isEqualTo(pallet.getLotId());
        assertThat(sortedPlace1.getCell()).isNull();
        checkLotStatus(pallet.getLotId(), LotStatus.PROCESSING);
        var sortedOrder1 = scOrderRepository.findByIdOrThrow(order.getId());
        assertThat(sortedOrder1.getFfStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);

        var sortedPlace2 = testFactory.sortPlaceToLot(place2, pallet, user);
        assertThat(sortedPlace2.getLot().getId()).isEqualTo(pallet.getLotId());
        assertThat(sortedPlace2.getCell()).isNull();
        checkLotStatus(pallet.getLotId(), LotStatus.PROCESSING);
        var sortedOrder2 = scOrderRepository.findByIdOrThrow(order.getId());
        assertThat(sortedOrder2.getFfStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
    }

    @Test
    void sortMultiOrderToPalletLotDirectly() {
        var order = testFactory.createForToday(
                order(sortingCenter)
                        .deliveryService(middleMileDeliveryService)
                        .dsType(DeliveryServiceType.TRANSIT)
                        .places("p1", "p2")
                        .externalId("o1")
                        .build())
                .acceptPlaces().cancel()
                .get();
        var place1 = placeRepository.findByOrderIdAndMainPartnerCode(order.getId(), "p1").get();
        var place2 = placeRepository.findByOrderIdAndMainPartnerCode(order.getId(), "p2").get();
        var sortedPlace1 = testFactory.sortPlaceToLot(place1, pallet, user);
        assertThat(sortedPlace1.getLot().getId()).isEqualTo(pallet.getLotId());
        assertThat(sortedPlace1.getCell()).isNull();
        checkLotStatus(pallet.getLotId(), LotStatus.PROCESSING);
        var sortedOrder1 = scOrderRepository.findByIdOrThrow(order.getId());
        assertThat(sortedOrder1.getFfStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);

        var sortedPlace2 = testFactory.sortPlaceToLot(place2, pallet, user);
        assertThat(sortedPlace2.getLot().getId()).isEqualTo(pallet.getLotId());
        assertThat(sortedPlace2.getCell()).isNull();
        checkLotStatus(pallet.getLotId(), LotStatus.PROCESSING);
        var sortedOrder2 = scOrderRepository.findByIdOrThrow(order.getId());
        assertThat(sortedOrder2.getFfStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
    }

    @Test
    void dontSortOrderToUnsuitableLot() {
        var order = testFactory.createForToday(
                TestFactory.CreateOrderParams.builder()

                        .sortingCenter(sortingCenter)
                        .build()
        ).cancel().accept().sort().get();
        Cell clientReturnCell = testFactory.storedCell(
                sortingCenter, "client_return1", CellType.RETURN, CellSubType.CLIENT_RETURN);
        SortableLot clientReturnPallet = testFactory.storedLot(sortingCenter, SortableType.PALLET, clientReturnCell);
        assertThatThrownBy(() -> testFactory.sortOrderToLot(order, clientReturnPallet, user))
                .isInstanceOf(ScException.class);
    }

    private void checkLotStatus(long lotId, LotStatus expectingStatus) {
        transactionTemplate.execute(ts -> {
            var lot = lotRepository.findByIdOrThrow(lotId);
            assertThat(lot.getStatus()).isEqualTo(expectingStatus);
            return null;
        });
    }

}
