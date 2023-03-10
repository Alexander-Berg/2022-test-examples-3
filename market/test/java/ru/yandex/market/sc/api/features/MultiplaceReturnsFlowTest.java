package ru.yandex.market.sc.api.features;

import java.time.Clock;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.api.BaseApiControllerTest;
import ru.yandex.market.sc.api.utils.TestControllerCaller;
import ru.yandex.market.sc.core.domain.cargo.Cargo;
import ru.yandex.market.sc.core.domain.cargo.CargoCommandService;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.client_return.ClientReturnService;
import ru.yandex.market.sc.core.domain.order.OrderQueryService;
import ru.yandex.market.sc.core.domain.order.model.ApiOrderStatus;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.route.model.FinishRouteRequestDto;
import ru.yandex.market.sc.core.domain.route_so.model.RouteType;
import ru.yandex.market.sc.core.domain.scan.model.SortableSortRequestDto;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.warehouse.model.WarehouseType;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM;
import static ru.yandex.market.sc.core.test.TestFactory.sortWithRouteSo;

/**
 * ???????????????? ?????????????? ?? ???????????????? ?????????????? ???????????? ?? ???????????? ??????????????????????
 * @author: dbryndin
 * @date: 7/11/22
 */
public class MultiplaceReturnsFlowTest extends BaseApiControllerTest {

    @Autowired
    TestFactory testFactory;
    @Autowired
    CargoCommandService cargoService;
    @Autowired
    OrderQueryService orderQueryService;
    @MockBean
    Clock clock;
    SortingCenter sortingCenter;
    User user;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    TransactionTemplate transactionTemplate;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter(1234L);
        user = testFactory.storedUser(sortingCenter, 123L);
        testFactory.setupMockClock(clock);

        testFactory.storedFakeReturnDeliveryService();
        testFactory.storedCourier(-1, ClientReturnService.CLIENT_RETURN_COURIER);
    }

    @Test
    @DisplayName("success ???????????????? ???????? ?????????????????? ???????????? ????????????")
    public void createTwoPlaces() {
        String orderBarcode = "O1";
        var cargo1 = new Cargo("segment-uuid-1", "cargo-unit-id-1", "P1", "wh-1", orderBarcode);
        testFactory.createReturn(cargo1, sortingCenter, user);
        var cargo2 = new Cargo("segment-uuid-2", "cargo-unit-id-2", "P2", "wh-2", orderBarcode);
        testFactory.createReturn(cargo2, sortingCenter, user);

        var order = testFactory.findOrder(orderBarcode, sortingCenter);
        checkCargoSuccessCreated(cargo1, order);
        checkCargoSuccessCreated(cargo2, order);
    }


    @Test
    @DisplayName("success ???????????????? ???????? ?????????????????? ???????????? ???????????? ????????????????????-????????????????-???? ???????????? ??????????????????????")
    public void createTwoPlacesOnDifferentDirection() {
        String orderBarcode = "O1";
        var cargo1 = new Cargo("segment-uuid-1", "cargo-unit-id-1", "P1", "wh-1", orderBarcode);
        testFactory.createReturn(cargo1, sortingCenter, user);
        var cargo2 = new Cargo("segment-uuid-2", "cargo-unit-id-2", "P2", "wh-2", orderBarcode);
        testFactory.createReturn(cargo2, sortingCenter, user);
        var order = testFactory.findOrder(orderBarcode, sortingCenter);

        checkCargoSuccessCreated(cargo1, order);
        checkCargoSuccessCreated(cargo2, order);

        // ?????????????????? ?? ?????????????????? ???? ???????????? ??????????????????????
        transactionTemplate.execute(ts -> {
            testFactory.acceptPlace(order, cargo1.placeBarcode());
            testFactory.acceptPlace(order, cargo2.placeBarcode());
            checkCargoSuccessAccepted(cargo1, order);
            checkCargoSuccessAccepted(cargo2, order);
            testFactory.sortPlace(testFactory.orderPlace(order, cargo1.placeBarcode()));
            testFactory.sortPlace(testFactory.orderPlace(order, cargo2.placeBarcode()));
            checkCargoSuccessSorted(cargo1, order);
            checkCargoSuccessSorted(cargo2, order);
            return null;
        });

        // ?????????????????? ???? ???????????? ??????????????????????
        transactionTemplate.execute(ts -> {
            testFactory.shipPlace(testFactory.orderPlace(order, cargo1.placeBarcode()));
            testFactory.shipPlace(testFactory.orderPlace(order, cargo2.placeBarcode()));
            checkCargoSuccessShipped(cargo1, order);
            checkCargoSuccessShipped(cargo2, order);
            return null;
        });

    }

    @Test
    @DisplayName("success ???????????????? ???????? ?????????????????? ????????????????????-????????????????-???? ???????????? ?????????????????????? ?????????????????? ?????? ???????? ?????????? " +
            "??????????????????-??????????????????-??????????????????")
    public void createThreePlacesOnDifferentDirection() {
        String orderBarcode = "O1";
        var cargo1 = new Cargo("segment-uuid-1", "cargo-unit-id-1", "P1", "wh-1", orderBarcode);
        testFactory.createReturn(cargo1, sortingCenter, user);
        var cargo2 = new Cargo("segment-uuid-2", "cargo-unit-id-2", "P2", "wh-2", orderBarcode);
        testFactory.createReturn(cargo2, sortingCenter, user);
        var order = testFactory.findOrder(orderBarcode, sortingCenter);

        // ?????????????????? P1 ?? P2 ?????????????????? ??????????????????
        checkCargoSuccessCreated(cargo1, order);
        checkCargoSuccessCreated(cargo2, order);

        // ?????????????????? P1 ?? P2 ?????????????????????? ?? ?????????????????????? ???? ?????????? ????????????????????????
        transactionTemplate.execute(ts -> {
            testFactory.acceptPlace(order, cargo1.placeBarcode());
            testFactory.acceptPlace(order, cargo2.placeBarcode());
            checkCargoSuccessAccepted(cargo1, order);
            checkCargoSuccessAccepted(cargo2, order);
            testFactory.sortPlace(testFactory.orderPlace(order, cargo1.placeBarcode()));
            testFactory.sortPlace(testFactory.orderPlace(order, cargo2.placeBarcode()));
            checkCargoSuccessSorted(cargo1, order);
            checkCargoSuccessSorted(cargo2, order);
            return null;
        });


        // ?????????????????? P1 ?? P2 ???? ???????? ??????????????????????
        transactionTemplate.execute(ts -> {
            testFactory.shipPlace(testFactory.orderPlace(order, cargo1.placeBarcode()));
            testFactory.shipPlace(testFactory.orderPlace(order, cargo2.placeBarcode()));
            checkCargoSuccessShipped(cargo1, order);
            checkCargoSuccessShipped(cargo2, order);
            return null;
        });

        var cargo3 = new Cargo("segment-uuid-3", "cargo-unit-id-3", "P3", "wh-3", orderBarcode);
        testFactory.createReturn(cargo3, sortingCenter, user);
        checkCargoSuccessCreated(cargo3, order);


        // ?????????????????? ?????????????????? P3
        transactionTemplate.execute(ts -> {
            testFactory.acceptPlace(order, cargo3.placeBarcode());
            checkCargoSuccessAccepted(cargo3, order);

            testFactory.sortPlace(testFactory.orderPlace(order, cargo3.placeBarcode()));
            checkCargoSuccessSorted(cargo3, order);
            return null;
        });

        // ?????????????????? P3
        transactionTemplate.execute(ts -> {
            testFactory.shipPlace(testFactory.orderPlace(order, cargo3.placeBarcode()));
            checkCargoSuccessShipped(cargo3, order);
            return null;
        });
    }


    @Test
    @DisplayName("???????????????????????????????? ???????????????? ?????????????????? ?????????????? ?? ????????????????")
    public void createTwoPlacesOnDifferentDirection2() {
        String orderBarcode = "O1";
        var cargo1 = new Cargo("segment-uuid-1", "cargo-unit-id-1", "P1", "wh-1", orderBarcode);
        testFactory.createReturn(cargo1, sortingCenter, user);

        var order = testFactory.findOrder(orderBarcode, sortingCenter);

        // ?????????????????? -> ?????????????????? -> ?????????????????? P1 ?????????????????? ?????? ?????? ????
        transactionTemplate.execute(ts -> {
            testFactory.acceptPlace(order, cargo1.placeBarcode())
                    .sortPlace(testFactory.orderPlace(order, cargo1.placeBarcode()))
                    .shipPlace(testFactory.orderPlace(order, cargo1.placeBarcode()));
            checkCargoSuccessShipped(cargo1, order);
            return null;
        });

        var cargo2 = new Cargo("segment-uuid-2", "cargo-unit-id-2", "P2", "wh-2", orderBarcode);
        testFactory.createReturn(cargo2, sortingCenter, user);

        // ?????????????????? -> ?????????????????? P2 ?????????????????? ?????? ?????? ????
        transactionTemplate.execute(ts -> {
            testFactory.acceptPlace(order, cargo2.placeBarcode())
                    .sortPlace(testFactory.orderPlace(order, cargo2.placeBarcode()));
            checkCargoSuccessSorted(cargo2, order);
            return null;
        });

        var cargo3 = new Cargo("segment-uuid-3", "cargo-unit-id-3", "P3", "wh-1", orderBarcode);
        testFactory.createReturn(cargo3, sortingCenter, user);

        // ?????????????????? -> ?????????????????? P3 ?????????????????? ?????? ?????? ????
        transactionTemplate.execute(ts -> {
            testFactory.acceptPlace(order, cargo3.placeBarcode())
                    .sortPlace(testFactory.orderPlace(order, cargo3.placeBarcode()));
            checkCargoSuccessSorted(cargo3, order);
            return null;
        });

        // ?????????????????? P2 ?????????????????? ?????? ?????? ????
        transactionTemplate.execute(ts -> {
            testFactory.shipPlace(order, cargo2.placeBarcode());
            checkCargoSuccessShipped(cargo2, order);
            return null;
        });

        // ?????????????????? -> ?????????????????? P3 ?????????????????? ?????? ?????? ????
        transactionTemplate.execute(ts -> {
            testFactory.shipPlace(order, cargo3.placeBarcode());
            checkCargoSuccessShipped(cargo3, order);
            return null;
        });
    }

    @Test
    @DisplayName("?????????????????? ???????? ???? ???????????? ??????????????????????")
    public void shipLotsToDifferentDirections() {
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");

        String orderBarcode = "O1";
        var cargo1 = new Cargo("segment-uuid-1", "cargo-unit-id-1", "P1", "wh-1", orderBarcode);
        testFactory.createReturn(cargo1, sortingCenter, user);
        var cargo2 = new Cargo("segment-uuid-2", "cargo-unit-id-2", "P2", "wh-2", orderBarcode);
        testFactory.createReturn(cargo2, sortingCenter, user);

        transactionTemplate.execute(ts -> {
            var place1 = testFactory.findPlace(orderBarcode, "P1", sortingCenter);
            testFactory.acceptPlace(place1);

            // ???????????????? ?? ??????
            Cell cell1 = testFactory.determineRouteCell(testFactory.findOutgoingRoute(place1).orElseThrow(), place1);
            SortableLot lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell1);

            testFactory.sortPlaceToLot(place1, lot1, user);
            assertThat(testFactory.updated(place1).getParent()).isNotNull();
            return null;
        });

        transactionTemplate.execute(ts -> {
            var place2 = testFactory.findPlace(orderBarcode, "P2", sortingCenter);
            testFactory.acceptPlace(place2);

            // ?????????????? ?? ????????????, ?????????? ?? ??????
            Cell cell2 = testFactory.determineRouteCell(testFactory.findOutgoingRoute(place2).orElseThrow(), place2);
            SortableLot lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell2);

            testFactory.sortPlace(place2, cell2.getId());
            testFactory.sortPlaceToLot(place2, lot2, user);
            assertThat(testFactory.updated(place2).getParent()).isNotNull();
            return null;
        });

        transactionTemplate.execute(ts -> {
            // ?????????????????? ?????????????? ???????????? ??????????????
            var place1 = testFactory.findPlace(orderBarcode, "P1", sortingCenter);
            testFactory.prepareToShipLot(place1.getParent());
            Long routableId = testFactory.getRouteIdForSortableFlow(
                                    testFactory.findOutgoingRoute(place1).orElseThrow());
            testFactory.shipLots(routableId, sortingCenter);
            assertThat(testFactory.updated(place1).getSortableStatus())
                    .isEqualTo(SortableStatus.SHIPPED_RETURN);
            return null;
        });

        transactionTemplate.execute(ts -> {
            // ?????????????????? ?????????????? ???????????? ??????????????
            var place2 = testFactory.findPlace(orderBarcode, "P2", sortingCenter);
            testFactory.prepareToShipLot(place2.getParent());
            Long routableId = testFactory.getRouteIdForSortableFlow(
                    testFactory.findOutgoingRoute(place2).orElseThrow().getId()
            );
            testFactory.shipLots(routableId, sortingCenter);
            assertThat(testFactory.updated(place2).getSortableStatus())
                    .isEqualTo(SortableStatus.SHIPPED_RETURN);
            return null;
        });
    }

    @Test
    @DisplayName("success ???????????????? ?????????????????? ????????????-??????????????-???????????????????? ?? ???? - ????????????????")
    public void successGigachadTestCreateByLrmAndUseBufferReturn() throws Exception {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, true);
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.IMPERMANENT_ENABLED, true);

        var wh1 = testFactory.storedWarehouse("wh-1", WarehouseType.SHOP);

        String orderBarcode = "O1";
        var cargo1 = new Cargo("segment-uuid-1", "cargo-unit-id-1", "P1", wh1.getYandexId(), orderBarcode);
        testFactory.createReturn(cargo1, sortingCenter, user);

        var cargo2 = new Cargo("segment-uuid-2", "cargo-unit-id-2", "P2", wh1.getYandexId(), orderBarcode);
        testFactory.createReturn(cargo2, sortingCenter, user);

        var order = testFactory.findOrder(orderBarcode, sortingCenter);

        transactionTemplate.execute(ts -> {
            testFactory.acceptPlace(order, cargo1.placeBarcode());
            testFactory.acceptPlace(order, cargo2.placeBarcode());
            return null;
        });

        var bufferCell = testFactory.storedCell(
                sortingCenter, "b-1", CellType.BUFFER, CellSubType.BUFFER_RETURNS, wh1.getYandexId());
        var impermanenceCell = testFactory.storedCell(
                sortingCenter, "i-1", CellType.RETURN, CellSubType.IMPERMANENT, wh1.getYandexId());

        var caller = TestControllerCaller.createCaller(mockMvc, UID);

        // ?????????????????? ?????? ?????????? ???? ???????????? ?????????????????????? ?? ???????????? ????????????????
        caller.sortableBetaSort(new SortableSortRequestDto(
                order.getExternalId(),
                cargo1.placeBarcode(),
                String.valueOf(bufferCell.getId()))).andExpect(status().is2xxSuccessful());
        caller.sortableBetaSort(new SortableSortRequestDto(
                order.getExternalId(),
                cargo2.placeBarcode(),
                String.valueOf(bufferCell.getId()))).andExpect(status().is2xxSuccessful());

        // ?????????????????? ?????? ?????????????? ?? ????
        transactionTemplate.execute(ts -> {
            var place1 = testFactory.orderPlace(order, cargo1.placeBarcode());
            assertThat(place1.getCell().getId()).isEqualTo(bufferCell.getId());
            assertThat(place1.getSortableStatus()).isEqualTo(SortableStatus.KEEPED_RETURN);

            var place2 = testFactory.orderPlace(order, cargo2.placeBarcode());
            assertThat(place2.getCell().getId()).isEqualTo(bufferCell.getId());
            assertThat(place2.getSortableStatus()).isEqualTo(SortableStatus.KEEPED_RETURN);

            var o = testFactory.getOrder(order.getId());
            assertThat(o.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
            return null;
        });

        caller.sortableBetaSort(new SortableSortRequestDto(
                cargo1.orderBarcode(),
                cargo1.placeBarcode(),
                String.valueOf(impermanenceCell.getId()))).andExpect(status().is2xxSuccessful());
        caller.sortableBetaSort(new SortableSortRequestDto(
                cargo2.orderBarcode(),
                cargo2.placeBarcode(),
                String.valueOf(impermanenceCell.getId()))).andExpect(status().is2xxSuccessful());

        // ?????????????????? ?????? ?????????????? ???????????????????????????? ?? ???????????????????????? ????????????
        transactionTemplate.execute(ts -> {
            var place1 = testFactory.orderPlace(order, cargo1.placeBarcode());
            assertThat(place1.getCell().getId()).isEqualTo(impermanenceCell.getId());
            assertThat(place1.getSortableStatus()).isEqualTo(SortableStatus.SORTED_RETURN);

            var place2 = testFactory.orderPlace(order, cargo2.placeBarcode());
            assertThat(place2.getCell().getId()).isEqualTo(impermanenceCell.getId());
            assertThat(place2.getSortableStatus()).isEqualTo(SortableStatus.SORTED_RETURN);

            var o = testFactory.getOrder(order.getId());
            assertThat(o.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
            return null;
        });

        var route = testFactory.findOutgoingRoute(order).orElseThrow();
        caller.ship(testFactory.getRouteIdForSortableFlow(route),
                        FinishRouteRequestDto.builder().cellId(impermanenceCell.getId()).build())
                .andExpect(status().isOk());

        var order1 = testFactory.getOrder(order.getId());
        assertThat(order1.getOrderStatus()).isEqualTo(RETURNED_ORDER_DELIVERED_TO_IM);
    }

    @Test
    @DisplayName("success ???????? ?????????????? ???????????? ???? ?? ????, ?????????? ?????????????????? ?? ?????????????? 170 ?? ???? ?????????? ?? ???????????? ??????????")
    public void success1() throws Exception {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, true);
        var orderBarcode = "O1";
        var wh1 = testFactory.storedWarehouse("wh-1", WarehouseType.SHOP);
        var cargo1 = new Cargo("segment-uuid-1", "cargo-unit-id-1", "P1", wh1.getYandexId(), orderBarcode);
        testFactory.createReturn(cargo1, sortingCenter, user);
        var cargo2 = new Cargo("segment-uuid-2", "cargo-unit-id-2", "P2", wh1.getYandexId(), orderBarcode);
        testFactory.createReturn(cargo2, sortingCenter, user);

        var order = testFactory.findOrder(orderBarcode, sortingCenter);

        // ?????????????????? P1
        transactionTemplate.execute(ts -> {
            testFactory.acceptPlace(order, cargo1.placeBarcode());
            return null;
        });

        var bufferCell = testFactory.storedCell(sortingCenter, "br1", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        var caller = TestControllerCaller.createCaller(mockMvc, UID);

        // ?????????????????? ?????? ?????????? ???? ???????????? ?????????????????????? ?? ???????????? ????????????????
        SortableSortRequestDto request = new SortableSortRequestDto(
                order.getExternalId(),
                cargo1.placeBarcode(),
                String.valueOf(bufferCell.getId()));
        caller.sortableBetaSort(request)
                .andExpect(status().is2xxSuccessful());

        // ?????????????????? ?????? ?????????????? P1 ?? ???? ?? ?????????? ?? 170 ??????????????
        transactionTemplate.execute(ts -> {
            var place1 = testFactory.orderPlace(order, cargo1.placeBarcode());
            assertThat(place1.getCell().getId()).isEqualTo(bufferCell.getId());

            var o = testFactory.getOrder(order.getId());
            assertThat(o.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
            return null;
        });
        var actualPlace = testFactory.orderPlace(order, cargo1.placeBarcode());
        assertThat(actualPlace.getCellId()).isEqualTo(Optional.of(bufferCell.getId()));
        assertThat(actualPlace.isKeeped()).isTrue();

    }

    @Test
    @DisplayName("?????????????????????????? ???????? ?????????? ?? ???? ?? ???? ??????????, ?????????? ?????????????? ???????????? ?? ?????????? ?? ???? ?? ???? ??????????")
    public void secondPlaceWantsToBufferReturnsToo() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, true);
        var wh1 = testFactory.storedWarehouse("wh-1", WarehouseType.SHOP);

        Cell bufferCell = testFactory.storedCell(sortingCenter, "br1", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        Cell returnCell = testFactory.storedCell(sortingCenter, "r1", CellType.RETURN, CellSubType.DEFAULT,
                wh1.getYandexId());

        var orderBarcode = "O1";
        var cargo1 = new Cargo("segment-uuid-1", "cargo-unit-id-1", "P1", wh1.getYandexId(), orderBarcode);
        testFactory.createReturn(cargo1, sortingCenter, user);

        var place1 = testFactory.findPlace(orderBarcode, "P1", sortingCenter);

        testFactory.acceptPlace(place1);
        testFactory.assertApiPlaceDto(place1, ApiOrderStatus.KEEP_TO_WAREHOUSE, bufferCell);

        testFactory.sortPlace(place1, bufferCell.getId());
        testFactory.assertApiPlaceDto(place1, ApiOrderStatus.SORT_TO_WAREHOUSE, returnCell);

        testFactory.sortPlace(place1, returnCell.getId());
        testFactory.assertApiPlaceDto(place1, ApiOrderStatus.OK, returnCell);

        // ???????????? ?????????????? ???????????? ??????????
        var cargo2 = new Cargo("segment-uuid-2", "cargo-unit-id-2", "P2", wh1.getYandexId(), orderBarcode);
        testFactory.createReturn(cargo2, sortingCenter, user);

        var place2 = testFactory.findPlace(orderBarcode, "P2", sortingCenter);

        testFactory.acceptPlace(place2);
        testFactory.assertApiPlaceDto(place2, ApiOrderStatus.KEEP_TO_WAREHOUSE, bufferCell);

        testFactory.sortPlace(place2, bufferCell.getId());
        testFactory.assertApiPlaceDto(place2, ApiOrderStatus.SORT_TO_WAREHOUSE, returnCell);

        testFactory.sortPlace(place2, returnCell.getId());
        testFactory.assertApiPlaceDto(place2, ApiOrderStatus.OK, returnCell);

        assertThat(testFactory.updated(place1).getSortableStatus()).isEqualTo(SortableStatus.SORTED_RETURN);
        assertThat(testFactory.updated(place2).getSortableStatus()).isEqualTo(SortableStatus.SORTED_RETURN);
    }


    private void checkCargoSuccessCreated(Cargo cargo, ScOrder order) {
        transactionTemplate.execute(ts -> {
            var place = testFactory.orderPlace(order, cargo.placeBarcode());
            assertThat(place).isNotNull();
            assertThat(place.getSortableStatus()).isEqualTo(SortableStatus.AWAITING_RETURN);
            assertThat(place.getSegmentUid()).isEqualTo(cargo.segmentUuid());
            assertThat(place.getCargoUnitId()).isEqualTo(cargo.cargoUnitId());
            assertThat(place.getWarehouseReturn().getYandexId()).isEqualTo(cargo.warehouseReturnYandexId());
            return null;
        });
    }

    private void checkCargoSuccessAccepted(Cargo cargo, ScOrder order) {
        transactionTemplate.execute(ts -> {
            var place = testFactory.orderPlace(order, cargo.placeBarcode());
            assertThat(place).isNotNull();
            assertThat(place.getSortableStatus()).isEqualTo(SortableStatus.ACCEPTED_RETURN);
            assertThat(place.getSegmentUid()).isEqualTo(cargo.segmentUuid());
            assertThat(place.getCargoUnitId()).isEqualTo(cargo.cargoUnitId());
            assertThat(place.getWarehouseReturn().getYandexId()).isEqualTo(cargo.warehouseReturnYandexId());

            var route = testFactory.findOutgoingRoute(place).orElseThrow();
            assertThat(route.getType()).isEqualTo(ru.yandex.market.sc.core.domain.route.model.RouteType.OUTGOING_WAREHOUSE);
            assertThat(route.getWarehouse().get().getYandexId()).isEqualTo(cargo.warehouseReturnYandexId());
            if (sortWithRouteSo()) {
                var routeSo = place.getOutRoute();
                assertThat(routeSo.getType()).isEqualTo(RouteType.OUT_RETURN);
            }
            return null;
        });
    }

    private void checkCargoSuccessSorted(Cargo cargo, ScOrder order) {
        transactionTemplate.execute(ts -> {
            var place = testFactory.orderPlace(order, cargo.placeBarcode());
            assertThat(place).isNotNull();
            assertThat(place.getSortableStatus()).isEqualTo(SortableStatus.SORTED_RETURN);
            assertThat(place.getSegmentUid()).isEqualTo(cargo.segmentUuid());
            assertThat(place.getCargoUnitId()).isEqualTo(cargo.cargoUnitId());
            assertThat(place.getWarehouseReturn().getYandexId()).isEqualTo(cargo.warehouseReturnYandexId());
            assertThat(place.getCell().getWarehouseYandexId()).isEqualTo(cargo.warehouseReturnYandexId());

            var route = testFactory.findOutgoingRoute(place).orElseThrow();
            assertThat(route.getType()).isEqualTo(ru.yandex.market.sc.core.domain.route.model.RouteType.OUTGOING_WAREHOUSE);
            assertThat(route.getWarehouse().get().getYandexId()).isEqualTo(cargo.warehouseReturnYandexId());
            if (sortWithRouteSo()) {
                var routeSo = place.getOutRoute();
                assertThat(routeSo.getType()).isEqualTo(RouteType.OUT_RETURN);
            }
            return null;
        });
    }

    private void checkCargoSuccessShipped(Cargo cargo, ScOrder order) {
        transactionTemplate.execute(ts -> {
            var place = testFactory.orderPlace(order, cargo.placeBarcode());
            assertThat(place).isNotNull();
            assertThat(place.getSortableStatus()).isEqualTo(SortableStatus.SHIPPED_RETURN);
            assertThat(place.getSegmentUid()).isEqualTo(cargo.segmentUuid());
            assertThat(place.getCargoUnitId()).isEqualTo(cargo.cargoUnitId());
            assertThat(place.getWarehouseReturn().getYandexId()).isEqualTo(cargo.warehouseReturnYandexId());
            return null;
        });
    }
}
