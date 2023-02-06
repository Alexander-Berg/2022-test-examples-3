package ru.yandex.market.sc.api.features;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.api.BaseApiControllerTest;
import ru.yandex.market.sc.core.domain.cargo.CargoCommandService;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.client_return.ClientReturnService;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.order.model.ApiOrderStatus;
import ru.yandex.market.sc.core.domain.order.model.UpdateReturnWarehouseRequestDto;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.warehouse.model.WarehouseType;
import ru.yandex.market.sc.core.exception.CargoCancelException;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.model.CourierDto;
import ru.yandex.market.sc.internal.model.WarehouseDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author: dbryndin
 * @date: 7/19/22
 */
public class MultiplaceUpdateReturnFlowTest extends BaseApiControllerTest {
    @Autowired
    CargoCommandService cargoService;
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
    }

    private record Cargo(String segmentUuid,
                         String cargoUnitId,
                         String placeBarcode,
                         String warehouseReturnYandexId,
                         String orderBarcode,
                         Long orderId,
                         Long placeId) {
    }

    private static Set<List<String>> getWhPair() {
        HashSet<List<String>> lists = new HashSet<>();
        lists.add(List.of("wh-1", "wh-1"));
        lists.add(List.of("wh-1", "wh-2"));
        return lists;
    }

    @ParameterizedTest
    @DisplayName("success две коробки прямого потока еще не на СЦ, обновляются LRM -> принимаются -> сортируются -> " +
            "отгружаются по очереди")
    @MethodSource("getWhPair")
    public void successCancelAcceptSortShipOneByOne(List<String> warehousesPair) {
        testFactory.storedFakeReturnDeliveryService();
        testFactory.storedCourier(-1, ClientReturnService.CLIENT_RETURN_COURIER);
        var ob = "O1";
        var pb1 = "P1";
        var placeBarcode2 = "P2";
        var lomWH = "lom-warehouse-0";

        var lomOrder = testFactory.createForToday(order(sortingCenter).externalId(ob)
                .places(List.of(pb1, placeBarcode2))
                .warehouseReturnId(lomWH).build()).get();
        var p1 = testFactory.findPlace(ob, pb1, sortingCenter);
        var p2 = testFactory.findPlace(ob, placeBarcode2, sortingCenter);

        assertThat(p1.getSortableStatus()).isEqualTo(SortableStatus.AWAITING_DIRECT);
        assertThat(p2.getSortableStatus()).isEqualTo(SortableStatus.AWAITING_DIRECT);

        var cargo1 = new Cargo("segment-uuid-1", "cargo-unit-id-1", "P1", warehousesPair.get(0), ob, lomOrder.getId(),
                p1.getId());
        updateSegment(cargo1);

        transactionTemplate.execute(ts -> {
            var p1AfterLrmUpdate = testFactory.findPlace(ob, pb1, sortingCenter);
            assertThat(p1AfterLrmUpdate.getSortableStatus()).isEqualTo(SortableStatus.AWAITING_RETURN);
            checkAssertPlaceCargo(p1AfterLrmUpdate, cargo1);

            var p2AfterLrmUpdate = testFactory.findPlace(ob, placeBarcode2, sortingCenter);
            assertThat(p2AfterLrmUpdate.getSortableStatus()).isEqualTo(SortableStatus.AWAITING_RETURN);
            assertThat(p2AfterLrmUpdate.getSegmentUid()).isNull();
            assertThat(p2AfterLrmUpdate.getCargoUnitId()).isNull();
            assertThat(p2AfterLrmUpdate.getWarehouseReturn().getYandexId()).isEqualTo(lomWH);
            return null;
        });
        var cargo2 = new Cargo("segment-uuid-2", "cargo-unit-id-2", "P2", warehousesPair.get(1), ob, lomOrder.getId(),
                p2.getId());

        updateSegment(cargo2);

        // принимаем сортируем отгружаем по очереди
        transactionTemplate.execute(ts -> {
            var p1AfterLrmUpdate = testFactory.findPlace(ob, pb1, sortingCenter);
            assertThat(p1AfterLrmUpdate.getSortableStatus()).isEqualTo(SortableStatus.AWAITING_RETURN);
            checkAssertPlaceCargo(p1AfterLrmUpdate, cargo1);

            var p2AfterLrmUpdate = testFactory.findPlace(ob, placeBarcode2, sortingCenter);
            assertThat(p2AfterLrmUpdate.getSortableStatus()).isEqualTo(SortableStatus.AWAITING_RETURN);
            checkAssertPlaceCargo(p2AfterLrmUpdate, cargo2);

            testFactory.acceptPlace(testFactory.findPlace(ob, placeBarcode2, sortingCenter));
            testFactory.sortPlace(testFactory.findPlace(ob, placeBarcode2, sortingCenter));
            testFactory.shipPlace(testFactory.findPlace(ob, placeBarcode2, sortingCenter));

            testFactory.acceptPlace(testFactory.findPlace(ob, pb1, sortingCenter));
            testFactory.sortPlace(testFactory.findPlace(ob, pb1, sortingCenter));
            testFactory.shipPlace(testFactory.findPlace(ob, pb1, sortingCenter));
            return null;

        });
        checkCargoSuccessShipped(cargo1);
        checkCargoSuccessShipped(cargo2);
    }


    @ParameterizedTest
    @DisplayName("success две коробки прямого потока еще не на СЦ, обновляются LRM -> принимаются -> сортируются -> " +
            "отгружаются вместе")
    @MethodSource("getWhPair")
    public void successCancelAcceptSortShipTogether(List<String> warehousesPair) {
        testFactory.storedFakeReturnDeliveryService();
        testFactory.storedCourier(-1, ClientReturnService.CLIENT_RETURN_COURIER);
        var ob = "O1";
        var pb1 = "P1";
        var placeBarcode2 = "P2";
        var lomWH = "lom-warehouse-0";

        var lomOrder = testFactory.createForToday(order(sortingCenter).externalId(ob)
                .places(List.of(pb1, placeBarcode2))
                .warehouseReturnId(lomWH).build()).get();
        var p1 = testFactory.findPlace(ob, pb1, sortingCenter);
        var p2 = testFactory.findPlace(ob, placeBarcode2, sortingCenter);

        assertThat(p1.getSortableStatus()).isEqualTo(SortableStatus.AWAITING_DIRECT);
        assertThat(p2.getSortableStatus()).isEqualTo(SortableStatus.AWAITING_DIRECT);

        var cargo1 = new Cargo("segment-uuid-1", "cargo-unit-id-1", "P1", warehousesPair.get(0), ob, lomOrder.getId(),
                p1.getId());
        updateSegment(cargo1);
        transactionTemplate.execute(ts -> {
            var p1AfterLrmUpdate = testFactory.findPlace(ob, pb1, sortingCenter);
            assertThat(p1AfterLrmUpdate.getSortableStatus()).isEqualTo(SortableStatus.AWAITING_RETURN);
            checkAssertPlaceCargo(p1AfterLrmUpdate, cargo1);

            var p2AfterLrmUpdate = testFactory.findPlace(ob, placeBarcode2, sortingCenter);
            assertThat(p2AfterLrmUpdate.getSortableStatus()).isEqualTo(SortableStatus.AWAITING_RETURN);
            assertThat(p2AfterLrmUpdate.getSegmentUid()).isNull();
            assertThat(p2AfterLrmUpdate.getCargoUnitId()).isNull();
            assertThat(p2AfterLrmUpdate.getWarehouseReturn().getYandexId()).isEqualTo(lomWH);
            return null;
        });
        var cargo2 = new Cargo("segment-uuid-2", "cargo-unit-id-2", "P2", warehousesPair.get(1), ob, lomOrder.getId(),
                p2.getId());
        updateSegment(cargo2);

        // принимаем сортируем отгружаем по очереди
        transactionTemplate.execute(ts -> {
            var p1AfterLrmUpdate = testFactory.findPlace(ob, pb1, sortingCenter);
            assertThat(p1AfterLrmUpdate.getSortableStatus()).isEqualTo(SortableStatus.AWAITING_RETURN);
            checkAssertPlaceCargo(p1AfterLrmUpdate, cargo1);

            var p2AfterLrmUpdate = testFactory.findPlace(ob, placeBarcode2, sortingCenter);
            assertThat(p2AfterLrmUpdate.getSortableStatus()).isEqualTo(SortableStatus.AWAITING_RETURN);
            checkAssertPlaceCargo(p2AfterLrmUpdate, cargo2);
            return null;
        });

        transactionTemplate.execute(ts -> {
            testFactory.acceptPlace(testFactory.findPlace(ob, placeBarcode2, sortingCenter));
            testFactory.acceptPlace(testFactory.findPlace(ob, pb1, sortingCenter));

            testFactory.sortPlace(testFactory.findPlace(ob, placeBarcode2, sortingCenter));
            testFactory.sortPlace(testFactory.findPlace(ob, pb1, sortingCenter));

            testFactory.shipPlace(testFactory.findPlace(ob, pb1, sortingCenter));
            testFactory.shipPlace(testFactory.findPlace(ob, placeBarcode2, sortingCenter));
            return null;
        });
        checkCargoSuccessShipped(cargo1);
        checkCargoSuccessShipped(cargo2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"p1", "p2", "p3"})
    @DisplayName("success принимаем апдейт возвратного грузоместа, если посылки не SORTED")
    public void successProcessUpdateFromLrmIfAllPlacesNotSorted(String placeToUpdate) {
        testFactory.storedFakeReturnDeliveryService();
        testFactory.storedCourier(-1, ClientReturnService.CLIENT_RETURN_COURIER);

        var places = testFactory.createOrder(order(sortingCenter, "o1")
                        .places("p1", "p2", "p3")
                        .build())
                .acceptPlaces("p2", "p3")
                .keepPlaces("p3")
                .getPlaces();

        updateSegment(createCargo(places.get(placeToUpdate), "wh-1"));

        assertThat(testFactory.updated(places.get("p1")).getSortableStatus())
                .isEqualTo(SortableStatus.AWAITING_RETURN);
        assertThat(testFactory.updated(places.get("p2")).getSortableStatus())
                .isEqualTo(SortableStatus.ACCEPTED_RETURN);
        assertThat(testFactory.updated(places.get("p3")).getSortableStatus())
                .isEqualTo(SortableStatus.ACCEPTED_RETURN);
    }

    @ParameterizedTest
    @ValueSource(strings = {"p1", "p2", "p3"})
    @DisplayName("fail не принимаем отмену тк одна из посылок отсортированна в ячейку на прямом потоке")
    public void failCancelOnePlaceSorted(String placeToUpdate) {
        testFactory.storedFakeReturnDeliveryService();
        testFactory.storedCourier(-1, ClientReturnService.CLIENT_RETURN_COURIER);

        var places = testFactory.createForToday(order(sortingCenter, "o1")
                        .places("p1", "p2", "p3")
                        .build())
                .acceptPlaces("p1", "p2", "p3")
                .sortPlace("p1")
                .getPlaces();

        assertThatThrownBy(() -> updateSegment(createCargo(places.get(placeToUpdate), "wh-1")))
                .isInstanceOf(CargoCancelException.class);

        assertThat(testFactory.updated(places.get("p1")).getSortableStatus())
                .isEqualTo(SortableStatus.SORTED_DIRECT);
        assertThat(testFactory.updated(places.get("p2")).getSortableStatus())
                .isEqualTo(SortableStatus.ARRIVED_DIRECT);
        assertThat(testFactory.updated(places.get("p3")).getSortableStatus())
                .isEqualTo(SortableStatus.ARRIVED_DIRECT);
    }

    @Test
    @DisplayName("success p1 отгржуенна на прямом потоке, p2, p3 приняты на прямом потоке. Отменяем коробки и по " +
            "почереди отгружаем")
    public void successOneDirectShippedTwoAccepted() {
        testFactory.storedFakeReturnDeliveryService();
        testFactory.storedCourier(-1, ClientReturnService.CLIENT_RETURN_COURIER);

        var places = testFactory.createForToday(order(sortingCenter, "o1")
                        .deliveryService(testFactory.storedDeliveryService("2", false))
                        .dsType(DeliveryServiceType.TRANSIT)
                        .places("p1", "p2", "p3")
                        .build())
                .acceptPlaces("p1", "p2")
                .sortPlace("p1")
                .shipPlace("p1")
                .getPlaces();

        var cargoP1 = createCargo(places.get("p1"), "wh-1");
        updateSegment(cargoP1);

        assertThat(testFactory.updated(places.get("p1")).getSortableStatus())
                .isEqualTo(SortableStatus.AWAITING_RETURN);
        assertThat(testFactory.updated(places.get("p2")).getSortableStatus())
                .isEqualTo(SortableStatus.ACCEPTED_RETURN);
        assertThat(testFactory.updated(places.get("p3")).getSortableStatus())
                .isEqualTo(SortableStatus.AWAITING_RETURN);

        var ob = testFactory.findOrder("o1", sortingCenter).getExternalId();
        transactionTemplate.execute(ts -> {
            testFactory.acceptPlace(testFactory.findPlace(ob, cargoP1.placeBarcode, sortingCenter));
            return null;
        });
        transactionTemplate.execute(ts -> {
            testFactory.sortPlace(testFactory.findPlace(ob, cargoP1.placeBarcode, sortingCenter));
            return null;
        });
        transactionTemplate.execute(ts -> {
            testFactory.shipPlace(testFactory.findPlace(ob, cargoP1.placeBarcode, sortingCenter));
            return null;
        });
        checkCargoSuccessShipped(cargoP1);

        var cargoP2 = createCargo(places.get("p2"), "wh-2");
        updateSegment(cargoP2);
        transactionTemplate.execute(ts -> {
            testFactory.sortPlace(testFactory.findPlace(ob, cargoP2.placeBarcode, sortingCenter));
            return null;
        });
        transactionTemplate.execute(ts -> {
            testFactory.shipPlace(testFactory.findPlace(ob, cargoP2.placeBarcode, sortingCenter));
            return null;
        });
        checkCargoSuccessShipped(cargoP2);

        var cargoP3 = createCargo(places.get("p3"), "wh-2");
        updateSegment(cargoP3);

        transactionTemplate.execute(ts -> {
            testFactory.acceptPlace(testFactory.findPlace(ob, cargoP3.placeBarcode, sortingCenter));
            return null;
        });

        transactionTemplate.execute(ts -> {
            testFactory.sortPlace(testFactory.findPlace(ob, cargoP3.placeBarcode, sortingCenter));
            return null;
        });
        transactionTemplate.execute(ts -> {
            testFactory.shipPlace(testFactory.findPlace(ob, cargoP3.placeBarcode, sortingCenter));
            return null;
        });
        checkCargoSuccessShipped(cargoP3);
    }

    @Test
    @DisplayName("success p1, p2, p3 приняты на СЦ прямого потока, отменяем по очереди " +
            "сортируем и отгружаем вместе")
    public void successOneDirectShippedTwoAccepted1() {
        testFactory.storedFakeReturnDeliveryService();
        testFactory.storedCourier(-1, ClientReturnService.CLIENT_RETURN_COURIER);

        var places = testFactory.createForToday(order(sortingCenter, "o1")
                        .deliveryService(testFactory.storedDeliveryService("2", false))
                        .dsType(DeliveryServiceType.TRANSIT)
                        .places("p1", "p2", "p3")
                        .build())
                .acceptPlaces("p1", "p2", "p3")
                .getPlaces();

        var cargoP1 = createCargo(places.get("p1"), "wh-1");
        var cargoP2 = createCargo(places.get("p2"), "wh-2");
        var cargoP3 = createCargo(places.get("p3"), "wh-2");

        updateSegment(cargoP1);
        updateSegment(cargoP2);
        updateSegment(cargoP3);
        transactionTemplate.execute(ts -> {
            checkAssertPlaceCargo(testFactory.getPlace(cargoP1.placeId()), cargoP1);
            checkAssertPlaceCargo(testFactory.getPlace(cargoP2.placeId()), cargoP2);
            checkAssertPlaceCargo(testFactory.getPlace(cargoP3.placeId()), cargoP3);
            return null;
        });

        testFactory.acceptPlace(testFactory.getPlace(cargoP1.placeId()));
        testFactory.acceptPlace(testFactory.getPlace(cargoP2.placeId()));
        testFactory.acceptPlace(testFactory.getPlace(cargoP3.placeId()));

        transactionTemplate.execute(ts -> {
            testFactory.sortPlace(testFactory.getPlace(cargoP1.placeId()));
            testFactory.sortPlace(testFactory.getPlace(cargoP2.placeId()));
            testFactory.sortPlace(testFactory.getPlace(cargoP3.placeId()));
            return null;
        });
        transactionTemplate.execute(ts -> {
            testFactory.shipPlace(testFactory.getPlace(cargoP1.placeId()));
            testFactory.shipPlace(testFactory.getPlace(cargoP2.placeId()));
            testFactory.shipPlace(testFactory.getPlace(cargoP3.placeId()));
            return null;
        });
        checkCargoSuccessShipped(cargoP1);
        checkCargoSuccessShipped(cargoP2);
        checkCargoSuccessShipped(cargoP3);

    }

    @Test
    @DisplayName("LRM отменил вторую посылку, когда первая уже была в АХ")
    public void secondPlaceWantsToBufferReturnsToo() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, true);
        var wh1 = testFactory.storedWarehouse("wh-1", WarehouseType.SHOP);

        Cell bufferCell = testFactory.storedCell(sortingCenter, "br1", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        Cell returnCell = testFactory.storedCell(sortingCenter, "r1", CellType.RETURN, CellSubType.DEFAULT, wh1.getYandexId());

        var places = testFactory.createForToday(order(sortingCenter, "o1").places("p1", "p2").build())
                .acceptPlaces()
                .sortPlaces()
                .shipPlaces()
                .getPlaces();
        Place place1 = places.get("p1");
        Place place2 = places.get("p2");

        updateSegment(createCargo(place1, wh1.getYandexId()));
        place1 = testFactory.updated(place1);

        testFactory.acceptPlace(place1);
        testFactory.assertApiPlaceDto(place1, ApiOrderStatus.KEEP_TO_WAREHOUSE, bufferCell);

        testFactory.sortPlace(place1, bufferCell.getId());
        // не просится на мерча, потому что второе место не в АХ
        testFactory.assertApiPlaceDto(place1, ApiOrderStatus.OK, bufferCell);

        // теперь обновляем второе место
        updateSegment(createCargo(place2, wh1.getYandexId()));
        place2 = testFactory.updated(place2);

        testFactory.acceptPlace(place2);
        testFactory.assertApiPlaceDto(place2, ApiOrderStatus.KEEP_TO_WAREHOUSE, bufferCell);

        testFactory.sortPlace(place2, bufferCell.getId());
        // теперь обе просятся на мерча
        testFactory.assertApiPlaceDto(place1, ApiOrderStatus.SORT_TO_WAREHOUSE, returnCell);
        testFactory.assertApiPlaceDto(place2, ApiOrderStatus.SORT_TO_WAREHOUSE, returnCell);

        testFactory.sortPlace(place1, returnCell.getId());
        testFactory.assertApiPlaceDto(place1, ApiOrderStatus.OK, returnCell);
        testFactory.assertApiPlaceDto(place2, ApiOrderStatus.SORT_TO_WAREHOUSE, returnCell);

        testFactory.sortPlace(place2, returnCell.getId());
        testFactory.assertApiPlaceDto(place1, ApiOrderStatus.OK, returnCell);
        testFactory.assertApiPlaceDto(place2, ApiOrderStatus.OK, returnCell);

        assertThat(testFactory.updated(place1).getSortableStatus()).isEqualTo(SortableStatus.SORTED_RETURN);
        assertThat(testFactory.updated(place2).getSortableStatus()).isEqualTo(SortableStatus.SORTED_RETURN);
    }

    @Test
    @DisplayName("отгружаем лоты на разные направления")
    public void  shipLotsToDifferentDirections() {
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");

        var places = testFactory.createForToday(order(sortingCenter, "o1").places("p1", "p2").build())
                .acceptPlaces()
                .sortPlaces()
                .shipPlaces()
                .getPlaces();

        updateSegment(createCargo(places.get("p1"), "wh-1"));
        updateSegment(createCargo(places.get("p2"), "wh-2"));

        transactionTemplate.execute(ts -> {
            var place1 = testFactory.findPlace("o1", "p1", sortingCenter);
            testFactory.acceptPlace(place1);

            // напрямую в лот
            Cell cell1 = testFactory.determineRouteCell(testFactory.findOutgoingRoute(place1).orElseThrow(), place1);
            SortableLot lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell1);

            testFactory.sortPlaceToLot(place1, lot1, user);
            assertThat(testFactory.updated(place1).getParent()).isNotNull();
            return null;
        });

        transactionTemplate.execute(ts -> {
            var place2 = testFactory.findPlace("o1", "p2", sortingCenter);
            testFactory.acceptPlace(place2);

            // сначала в ячейку, потом в лот
            Cell cell2 = testFactory.determineRouteCell(testFactory.findOutgoingRoute(place2).orElseThrow(), place2);
            SortableLot lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell2);

            testFactory.sortPlace(place2, cell2.getId());
            testFactory.sortPlaceToLot(place2, lot2, user);
            assertThat(testFactory.updated(place2).getParent()).isNotNull();
            return null;
        });

        transactionTemplate.execute(ts -> {
            // отгружаем маршрут первой посылки
            var place1 = testFactory.findPlace("o1", "p1", sortingCenter);
            testFactory.prepareToShipLot(place1.getParent());
            testFactory.shipLots(testFactory.findOutgoingRoute(place1).orElseThrow().getId(), sortingCenter);
            assertThat(testFactory.updated(place1).getSortableStatus())
                    .isEqualTo(SortableStatus.SHIPPED_RETURN);
            return null;
        });

        transactionTemplate.execute(ts -> {
            // отгружаем маршрут второй посылки
            var place2 = testFactory.findPlace("o1", "p2", sortingCenter);
            testFactory.prepareToShipLot(place2.getParent());
            testFactory.shipLots(testFactory.findOutgoingRoute(place2).orElseThrow().getId(), sortingCenter);
            assertThat(testFactory.updated(place2).getSortableStatus())
                    .isEqualTo(SortableStatus.SHIPPED_RETURN);
            return null;
        });
    }

    @Test
    @DisplayName("отгружаем лоты на разные направления (отмена в разных статусах)")
    public void  shipLotsToDifferentDirections_cancelOnDifferentStatuses() {
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");

        var places = testFactory.createForToday(order(sortingCenter, "o1")
                        .dsType(DeliveryServiceType.TRANSIT)
                        .places("p1", "p2")
                        .build())
                .acceptPlaces("p1")
                .sortPlaces("p1")
                .shipPlaces("p1")
                .getPlaces();

        updateSegment(createCargo(places.get("p1"), "wh-1"));
        updateSegment(createCargo(places.get("p2"), "wh-2"));

        transactionTemplate.execute(ts -> {
            var place1 = testFactory.findPlace("o1", "p1", sortingCenter);
            testFactory.acceptPlace(place1);

            Cell cell1 = testFactory.determineRouteCell(testFactory.findOutgoingRoute(place1).orElseThrow(), place1);
            SortableLot lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell1);

            testFactory.sortPlaceToLot(place1, lot1, user);
            assertThat(testFactory.updated(place1).getParent()).isNotNull();
            return null;
        });

        transactionTemplate.execute(ts -> {
            var place2 = testFactory.findPlace("o1", "p2", sortingCenter);
            testFactory.acceptPlace(place2);

            // сначала в ячейку, потом в лот
            Cell cell2 = testFactory.determineRouteCell(testFactory.findOutgoingRoute(place2).orElseThrow(), place2);
            SortableLot lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell2);

            testFactory.sortPlace(place2, cell2.getId());
            testFactory.sortPlaceToLot(place2, lot2, user);
            assertThat(testFactory.updated(place2).getParent()).isNotNull();
            return null;
        });

        transactionTemplate.execute(ts -> {
            // отгружаем маршрут первой посылки
            var place1 = testFactory.findPlace("o1", "p1", sortingCenter);
            testFactory.prepareToShipLot(place1.getParent());
            testFactory.shipLots(testFactory.findOutgoingRoute(place1).orElseThrow().getId(), sortingCenter);
            assertThat(testFactory.updated(place1).getSortableStatus())
                    .isEqualTo(SortableStatus.SHIPPED_RETURN);
            return null;
        });

        transactionTemplate.execute(ts -> {
            // отгружаем маршрут второй посылки
            var place2 = testFactory.findPlace("o1", "p2", sortingCenter);
            testFactory.prepareToShipLot(place2.getParent());
            testFactory.shipLots(testFactory.findOutgoingRoute(place2).orElseThrow().getId(), sortingCenter);
            assertThat(testFactory.updated(place2).getSortableStatus())
                    .isEqualTo(SortableStatus.SHIPPED_RETURN);
            return null;
        });
    }

    private Cargo createCargo(Place place, String warehouseReturnYandexId) {
        return new Cargo(
                "segment-uuid-" + place.getId(),
                "cargo-unit-id-" + place.getId(),
                place.getMainPartnerCode(),
                warehouseReturnYandexId,
                place.getExternalId(),
                place.getOrderId(),
                place.getId());
    }


    private void checkCargoSuccessShipped(Cargo cargo) {
        transactionTemplate.execute(ts -> {
            var place = testFactory.findPlace(cargo.orderBarcode(), cargo.placeBarcode(), sortingCenter);
            assertThat(place).isNotNull();
            assertThat(place.getSortableStatus()).isEqualTo(SortableStatus.SHIPPED_RETURN);
            assertThat(place.getSegmentUid()).isEqualTo(cargo.segmentUuid);
            assertThat(place.getCargoUnitId()).isEqualTo(cargo.cargoUnitId);
            assertThat(place.getWarehouseReturn().getYandexId()).isEqualTo(cargo.warehouseReturnYandexId);
            return null;
        });
    }

    private void checkAssertPlaceCargo(Place place, Cargo cargo) {
        assertThat(place.getSegmentUid()).isEqualTo(cargo.segmentUuid());
        assertThat(place.getCargoUnitId()).isEqualTo(cargo.cargoUnitId());
        assertThat(place.getWarehouseReturn().getYandexId()).isEqualTo(cargo.warehouseReturnYandexId());
    }


    private void updateSegment(Cargo cargo) {
        var returnWarehouseDto = WarehouseDto.builder()
                .yandexId(cargo.warehouseReturnYandexId)
                .logisticPointId("log_point-" + cargo.warehouseReturnYandexId)
                .type(WarehouseType.SORTING_CENTER.getName())
                .incorporation(cargo.warehouseReturnYandexId)
                .location(TestFactory.MOCK_WAREHOUSE_LOCATION)
                .build();
        var request = UpdateReturnWarehouseRequestDto.builder()
                .orderId(cargo.orderId())
                .placeId(cargo.placeId())
                .message("LRM_CANCELED_MESSAGE")
                .cargoUnitId(cargo.cargoUnitId())
                .courierFrom(new CourierDto(1 + cargo.placeId, "courier-for-" + cargo.placeBarcode, 1L))
                .segmentUuid(cargo.segmentUuid())
                .returnWarehouse(returnWarehouseDto)
                .timeOut(Instant.now(clock))
                .timeIn(Instant.now(clock))
                .build();
        cargoService.updateCargo(request, user);
    }
}
