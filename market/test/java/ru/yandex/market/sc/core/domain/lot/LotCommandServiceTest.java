package ru.yandex.market.sc.core.domain.lot;

import java.time.Clock;
import java.util.Collections;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.core.dbqueue.ScQueueType;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.lot.model.PartnerLotRequestDto;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSo;
import ru.yandex.market.sc.core.domain.sortable.SortableQueryService;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableAPIAction;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.SortableRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.exception.ScException;
import ru.yandex.market.sc.core.test.DefaultScUserWarehouseExtension;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.SortableTestFactory;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.util.exception.TplIllegalStateException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.sc.core.domain.lot.LotCommandService.MAX_LOT_PER_REQUEST_COUNT;

@EmbeddedDbTest
@ExtendWith(DefaultScUserWarehouseExtension.class)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LotCommandServiceTest {

    private final LotCommandService lotCommandService;
    private final TestFactory testFactory;
    private final ScOrderRepository scOrderRepository;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final SortableQueryService sortableQueryService;
    private final SortableRepository sortableRepository;
    private final XDocFlow flow;
    private final SortableTestFactory sortableTestFactory;
    @MockBean
    Clock clock;

    SortingCenter sortingCenter;
    Cell returnCell;
    Cell courierCell;
    Courier courier;
    User user;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        user = testFactory.storedUser(sortingCenter, 123L);
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        courier = testFactory.storedCourier();
        returnCell = testFactory.storedCell(sortingCenter, "r1", CellType.RETURN);
        courierCell = testFactory.storedCell(sortingCenter, "c1", CellType.COURIER, courier.getId());
        testFactory.setupMockClock(clock);
    }

    @Test
    void createLots() {
        var lots = lotCommandService.createEmptyLots(sortingCenter, new PartnerLotRequestDto(
                returnCell.getId(),
                2
        ));
        assertThat(lots.size()).isEqualTo(2);
    }


    @DisplayName("Можно создавать XDoc лоты для ячеек BUFFER_XDOC")
    @Test
    void createLotForBufferCellXDoc() {
        var xDocCell = testFactory.storedCell(sortingCenter, "buffer1", CellType.BUFFER, CellSubType.BUFFER_XDOC);
        var lots = lotCommandService.createEmptyLots(sortingCenter, new PartnerLotRequestDto(
                xDocCell.getId(),
                2
        ));
        assertThat(lots.size()).isEqualTo(2);
    }

    @Test
    void createCourierLot() {
        var place = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().getPlace();
        assertThat(place.getCell()).isNotNull();
        assertThat(place.getCell().getType()).isEqualTo(CellType.COURIER);
        var lot = testFactory.storedLot(sortingCenter, place.getCell(), LotStatus.CREATED);
        assertThat(lot.getStatus()).isEqualTo(SortableStatus.KEEPED_DIRECT);
        testFactory.sortPlaceToLot(place, lot, testFactory.storedUser(sortingCenter, 1L));
        testFactory.prepareToShipLot(lot);
        testFactory.shipLotRouteByParentCell(lot);
        lot = testFactory.getLot(lot.getLotId());
        assertThat(lot.getStatus()).isEqualTo(SortableStatus.SHIPPED_DIRECT);

    }

    @Test
    void createReturnLot() {
        var lot = testFactory.storedLot(sortingCenter, returnCell, LotStatus.CREATED);
        assertThat(lot.getStatus()).isEqualTo(SortableStatus.KEEPED_RETURN);
        var place = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().ship().makeReturn().accept().sort().getPlace();
        assertThat(place.getCell()).isEqualTo(returnCell);
        testFactory.sortPlaceToLot(place, lot, testFactory.storedUser(sortingCenter, 1L));
        testFactory.prepareToShipLot(lot);
        testFactory.shipLotRouteByParentCell(lot);
        lot = testFactory.getLot(lot.getLotId());
        assertThat(lot.getStatus()).isEqualTo(SortableStatus.SHIPPED_RETURN);
    }

    @ParameterizedTest(name = "Нельзя создать Lot в BUFFER ячейку с подтипом {0}")
    @MethodSource("getNonXDocBufferCellSubTypes")
    void dontCreateLotsForNonXDocBufferCellSubType(CellSubType subType) {
        var cell = testFactory.storedCell(sortingCenter, "b1", CellType.BUFFER, subType);
        assertThatThrownBy(() ->
                lotCommandService.createEmptyLots(sortingCenter, new PartnerLotRequestDto(
                        cell.getId(),
                        2
                ))).isInstanceOf(ScException.class);
    }

    private static Set<CellSubType> getNonXDocBufferCellSubTypes() {
        return StreamEx.of(CellType.BUFFER.getSubTypes())
                .remove(subType -> subType == CellSubType.BUFFER_XDOC)
                .remove(subType -> subType == CellSubType.BUFFER_XDOC_LOCATION)
                .toSet();
    }

    @Test
    void checkLotsCount() {
        assertThatThrownBy(() ->
                lotCommandService.createEmptyLots(sortingCenter, new PartnerLotRequestDto(
                        returnCell.getId(),
                        MAX_LOT_PER_REQUEST_COUNT + 1
                ))).isInstanceOf(ScException.class);
    }

    @Test
    void deleteLot() {
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, returnCell);

        lotCommandService.deleteLot(sortingCenter, lot.getLotId());
        SortableLot updatedLot = testFactory.getLot(lot.getLotId());

        assertThat(updatedLot.isDeleted()).isTrue();
        assertThat(updatedLot.getParentCell()).isNull();
    }

    @Test
    void failOnDeleteShippedLot() {
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, returnCell, LotStatus.SHIPPED,
                false);

        assertThatThrownBy(() -> lotCommandService.deleteLot(sortingCenter, lot.getLotId()));
    }

    @Test
    void failOnDeleteLotWithBoxes() {
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.XDOC_BASKET, returnCell);
        var lotSortable = sortableQueryService.find(lot.getSortableId())
                .orElseThrow(() -> new IllegalStateException("Sortable is not present"));
        Inbound inbound = flow.createInboundAndGet("IN-123");
        var boxId = sortableTestFactory.createSortable("XDOC-sortable1", SortableType.XDOC_BOX, inbound, sortingCenter);
        var box = sortableQueryService.find(boxId.getId())
                .orElseThrow(() -> new IllegalStateException("Box is not present"));
        box.setMutableState(box.getMutableState().withParent(lotSortable));
        sortableRepository.save(box);

        var e = assertThrows(
                ScException.class,
                () -> lotCommandService.deleteLot(sortingCenter, lot.getLotId())
        );
        Assertions.assertThat(e.getMessage()).isEqualTo("Can't delete not empty lot");
    }

    @Test
    void prepareToShipLotWhenActionReadyForShipment() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");

        SortableLot lot = testFactory.storedLot(sortingCenter, returnCell, LotStatus.PROCESSING);
        ScOrder scOrder = testFactory.createOrder(sortingCenter).cancel().accept().get();
        testFactory.sortOrderToLot(scOrder, lot, user);
        lotCommandService.prepareToShipLot(lot.getLotId(), SortableAPIAction.READY_FOR_SHIPMENT,
                user);
        SortableLot result = testFactory.getLot(lot.getLotId());

        assertThat(result.getLotStatusOrNull()).isEqualTo(LotStatus.READY);
        dbQueueTestUtil.assertQueueHasSize(ScQueueType.BATCH_REGISTER_READY, 0);
    }

    @Test
    void prepareToShipLotWhenActionNotReadyForShipmentEmptyLot() {
        SortableLot lot = testFactory.storedLot(sortingCenter, returnCell, LotStatus.PROCESSING);
        assertThatThrownBy(() ->
                lotCommandService.prepareToShipLot(lot.getLotId(), SortableAPIAction.READY_FOR_SHIPMENT, user))
                .isInstanceOf(TplIllegalStateException.class);
    }

    @Test
    void prepareToShipLotExceptionWhenActionNotReadyForShipment() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");

        SortableLot lot = testFactory.storedLot(sortingCenter, returnCell, LotStatus.PROCESSING);
        ScOrder scOrder = testFactory.createOrder(sortingCenter).cancel().accept().get();
        testFactory.sortOrderToLot(scOrder, lot, user);
        lotCommandService.prepareToShipLot(lot.getLotId(), SortableAPIAction.READY_FOR_SHIPMENT, user);

        lotCommandService.prepareToShipLot(lot.getLotId(), SortableAPIAction.NOT_READY_FOR_SHIPMENT, user);
        SortableLot result = testFactory.getLot(lot.getLotId());

        assertThat(result.getLotStatusOrNull()).isEqualTo(LotStatus.PROCESSING);
        dbQueueTestUtil.assertQueueHasSize(ScQueueType.BATCH_REGISTER_READY, 0);
    }

    @Test
    void prepareToShipExceptionWhenLotIsEmpty() {
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, returnCell);

        assertThatThrownBy(
                () -> lotCommandService.prepareToShipLot(lot.getLotId(), SortableAPIAction.READY_FOR_SHIPMENT, user),
                "Can't ship lot(" + lot.getLotId() + "). Lot is empty.",
                Collections.emptyList()
        );
        dbQueueTestUtil.assertQueueHasSize(ScQueueType.BATCH_REGISTER_READY, 0);
    }

    @Test
    void createLotSortableWithRouteSo() {
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, courierCell);
        RouteSo outRoute = lot.getSortable().getOutRoute();

        assertThat(outRoute).isNotNull();

        SortableLot lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, courierCell);
        RouteSo outRoute2 = lot2.getSortable().getOutRoute();
        assertThat(outRoute).isEqualTo(outRoute2);
    }

}
