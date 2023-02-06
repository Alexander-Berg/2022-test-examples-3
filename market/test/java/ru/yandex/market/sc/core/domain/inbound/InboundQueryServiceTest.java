package ru.yandex.market.sc.core.domain.inbound;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.inbound.repository.Registry;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryOrder;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryType;
import ru.yandex.market.sc.core.domain.movement_courier.repository.MovementCourier;
import ru.yandex.market.sc.core.domain.order.AcceptService;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.outbound.repository.Outbound;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.route.model.TransferActPlaceDto;
import ru.yandex.market.sc.core.domain.route.repository.Route;
import ru.yandex.market.sc.core.domain.route.repository.RouteFinish;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLotService;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.warehouse.model.WarehouseType;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.CREATE_LOTS_FROM_REGISTRY;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.ENABLE_INBOUND_TRANSFER_ACT_BUILD_FOR_PLACES;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InboundQueryServiceTest {

    private final TestFactory testFactory;
    private SortingCenter sortingCenter;
    private final InboundQueryService inboundQueryService;
    private final Clock clock;
    private final SortableLotService sortableLotService;
    private final AcceptService acceptService;
    private final PlaceRepository placeRepository;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
    }

    /**
     * Найти поставки на сегодня. Когда созданы поставки только на сегодня
     */
    @Test
    void findInboundsForTodayTest() {
        String warehouseFromId = "warehouse_from_id";
        var params = TestFactory.CreateInboundParams
                .builder()
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId(warehouseFromId)
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .build();
        var inbound = testFactory.createInbound(params);
        var registries = testFactory.getRegistryByInboundId(inbound.getId());
        var inbounds = inboundQueryService.getInboundsByDateAndWarehouseAndSortingCenter(
                LocalDate.now(clock),
                inbound.getWarehouseFromId(),
                sortingCenter.getId()
        );
        assertThat(inbounds.size()).isEqualTo(1);
        var inboundInfo = inbounds.get(0);
        assertThat(inboundInfo.getExternalId()).isEqualTo(inbound.getExternalId());
        assertThat(inboundInfo.getInbound().getFromDate()).isEqualTo(inbound.getFromDate());
        assertThat(inboundInfo.getInbound().getWarehouseFromId()).isEqualTo(inbound.getWarehouseFromId());
        assertThat(inboundInfo.getRegistries().size()).isEqualTo(registries.size());
        assertThat(inboundInfo.getRegistries().get(0).getExternalId()).isEqualTo(registries.get(0).getExternalId());
    }

    @Test
    void findInboundsForTodayOnlySpecifiedScTest() {
        String warehouseFromId = "warehouse_from_id";
        var ourInbound = testFactory.createInbound(TestFactory.CreateInboundParams
                .builder()
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId(warehouseFromId)
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .build());
        testFactory.createInbound(TestFactory.CreateInboundParams
                .builder()
                .inboundExternalId("inboundExternalId-2")
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId(warehouseFromId)
                .registryMap(Map.of())
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(testFactory.storedSortingCenter(sortingCenter.getId() + 743))
                .build());
        var inbounds = inboundQueryService.getInboundsByDateAndWarehouseAndSortingCenter(
                LocalDate.now(clock),
                ourInbound.getWarehouseFromId(),
                sortingCenter.getId()
        );
        assertThat(inbounds.size()).isEqualTo(1);
    }

    @Test
    void getDiscrepancyInboundTest() {
        String warehouseFromId = "warehouse_from_id";
        String orderExternalId = "order_ext_id_1";
        String orderExternalId2 = "another_order";
        var params = TestFactory.CreateInboundParams
                .builder()
                .inboundType(InboundType.DS_SC)
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId(warehouseFromId)
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .build();
        var inbound = testFactory.createInbound(params);
        var discrepancyOrders = inboundQueryService.getDiscrepancyInbounds(List.of(inbound))
                .get(0).getAllOrders();
        assertThat(discrepancyOrders.size()).isEqualTo(1);
        assertThat(discrepancyOrders.get(0).getExternalId()).isEqualTo(orderExternalId);
        testFactory.create(
                        order(sortingCenter)
                                .externalId(orderExternalId2)
                                .warehouseReturnId(warehouseFromId)
                                .build()
                )
                .updateShipmentDate(LocalDate.now(clock)).accept().get();
        discrepancyOrders = inboundQueryService.getDiscrepancyInbounds(List.of(inbound))
                .get(0).getAllOrders();
        assertThat(discrepancyOrders.size()).isEqualTo(1);
        assertThat(discrepancyOrders.get(0).getExternalId()).isEqualTo(orderExternalId);
        testFactory.create(
                        order(sortingCenter)
                                .externalId(orderExternalId)
                                .warehouseReturnId(warehouseFromId)
                                .build()
                )
                .updateShipmentDate(LocalDate.now(clock)).accept().get();
        discrepancyOrders = inboundQueryService.getDiscrepancyInbounds(List.of(inbound))
                .get(0).getAllOrders();
        assertTrue(discrepancyOrders.isEmpty());

    }

    @Test
    @SneakyThrows
    void getDiscrepancyInboundTestWithLots() {

        testFactory.createForToday(order(sortingCenter)
                .externalId("o-1")
                .places("p-1", "p-2")
                .build()).get();

        var user = testFactory.storedUser(sortingCenter, 1919L);

        var inbound = testFactory.createInbound(TestFactory.CreateInboundParams.builder()
                .inboundType(InboundType.DS_SC)
                .sortingCenter(sortingCenter)
                .inboundExternalId("in-1")
                .registryMap(Map.of("registry_1", List.of(Pair.of("o-1", "p-1"), Pair.of("o-1", "p-2"))))
                .placeInPallets(Map.of("p-1", "SC_LOT_1", "p-2", "SC_LOT_1"))
                .palletToStamp(Map.of("SC_LOT_1", "stamp-1"))
                .fromDate(OffsetDateTime.now(clock))
                .toDate(OffsetDateTime.now(clock))
                .transportationId("TM123")
                .build()
        );

        testFactory.acceptLot("stamp-1", user);

        var data = inboundQueryService.getDiscrepancyInbounds(List.of(inbound)).get(0).getAllOrders();

        assertThat(data).isEmpty();

    }

    @Test
    @SneakyThrows
    void getDiscrepancyInboundTestWithLotsNotArrived() {

        testFactory.createForToday(order(sortingCenter)
                .externalId("o-1")
                .places("p-1", "p-2")
                .build()).get();

        var inbound = testFactory.createInbound(TestFactory.CreateInboundParams.builder()
                .inboundType(InboundType.DS_SC)
                .sortingCenter(sortingCenter)
                .inboundExternalId("in-1")
                .registryMap(Map.of("registry_1", List.of(Pair.of("o-1", "p-1"), Pair.of("o-1", "p-2"))))
                .placeInPallets(Map.of("p-1", "SC_LOT_1", "p-2", "SC_LOT_1"))
                .palletToStamp(Map.of("SC_LOT_1", "stamp-1"))
                .fromDate(OffsetDateTime.now(clock))
                .toDate(OffsetDateTime.now(clock))
                .transportationId("TM123")
                .build()
        );

        var data = inboundQueryService.getDiscrepancyInbounds(List.of(inbound)).get(0).getAllOrders();

        assertThat(data).hasSize(2);
        assertThat(data.get(0).getPlaceId()).isEqualTo("p-1");
        assertThat(data.get(1).getPlaceId()).isEqualTo("p-2");

    }

    @Test
    void getInboundGroupForSameCourierTest() {
        MovementCourier courier2 = testFactory.storedMovementCourier(20000000L);
        MovementCourier courier4 = testFactory.storedMovementCourier(40000000L);
        SortingCenter otherSortingCenter = testFactory.storedSortingCenter(17L);
        Warehouse dropoff = testFactory.storedWarehouse("wh-dropoff-id", WarehouseType.DROPOFF);
        Warehouse warehouse = testFactory.storedWarehouse("wh-warehouse-id", WarehouseType.SORTING_CENTER);
        Warehouse otherWarehouse = testFactory.storedWarehouse("wh-warehouse-id-2", WarehouseType.SORTING_CENTER);
        Warehouse shop = testFactory.storedWarehouse("wh-warehouse-id", WarehouseType.SHOP);

        Inbound inbound1 = createInbound("e-1", OffsetDateTime.now(clock), courier2, sortingCenter, dropoff);
        Inbound inbound2 = createInbound("e-2", OffsetDateTime.now(clock), courier2, sortingCenter, dropoff);
        Inbound inboundOtherDate = createInbound(
                "e-3", OffsetDateTime.now(clock).minusDays(1), courier2, sortingCenter, dropoff);
        Inbound inboundOtherCourier = createInbound(
                "e-4", OffsetDateTime.now(clock), courier4, sortingCenter, dropoff);
        Inbound inboundOtherSortingCenter = createInbound(
                "e-5", OffsetDateTime.now(clock), courier2, otherSortingCenter, dropoff);
        Inbound inboundForWarehouse = createInbound(
                "e-6", OffsetDateTime.now(clock), courier2, sortingCenter, warehouse);
        Inbound inboundForOtherWarehouse = createInbound(
                "e-7", OffsetDateTime.now(clock), courier2, sortingCenter, otherWarehouse);
        Inbound inboundForShop = createInbound(
                "e-8", OffsetDateTime.now(clock), courier2, sortingCenter, shop);

        assertInboundGroup(inbound1, List.of(inbound1, inbound2));
        assertInboundGroup(inbound2, List.of(inbound1, inbound2));
        assertInboundGroup(inboundOtherDate, List.of(inboundOtherDate));
        assertInboundGroup(inboundOtherCourier, List.of(inboundOtherCourier));
        assertInboundGroup(inboundOtherSortingCenter, List.of(inboundOtherSortingCenter));
        assertInboundGroup(inboundForWarehouse, List.of(inboundForWarehouse));
        assertInboundGroup(inboundForOtherWarehouse, List.of(inboundForOtherWarehouse));
        assertInboundGroup(inboundForShop, List.of(inboundForShop));
    }

    @Test
    void checkIfMatchesMarkettplsc2662() {
        String orderExternalId = "test-order-external-id";
        String orderExternalId2 = "test-order-external-id2";
        String orderExternalId3 = "test-order-external-id3";
        String orderExternalId4 = "test-order-external-id4";

        Long courierUid = 1000L;
        String inboundExternalId = "inboundExtId1";

        var scOrderWithPlaces =
                testFactory.createOrder(order(sortingCenter).externalId(orderExternalId).build()).accept()
                        .getOrderWithPlaces();
        var scOrder = scOrderWithPlaces.order();
        var place = scOrderWithPlaces.place(scOrder.getExternalId());
        OrderLike scOrder2 =
                testFactory.createOrder(order(sortingCenter).externalId(orderExternalId2).build()).accept().get();

        String warehouseFromId = "warehouse-from-id";

        OffsetDateTime inboundDate = OffsetDateTime.now(clock);
        OffsetDateTime routeDate = OffsetDateTime.now(clock);

        Cell cell = testFactory.storedCell(sortingCenter, "cellName");
        var movementCourier = testFactory.getMovementCourier(courierUid);
        if (movementCourier == null) {
            movementCourier = testFactory.storedMovementCourier(courierUid);
        }
        var inbound = testFactory.createInbound(inboundParams(sortingCenter,
                warehouseFromId, inboundDate, movementCourier, inboundExternalId));

        Courier courier = testFactory.storedCourier(courierUid);
        Route route = testFactory.storedIncomingCourierDropOffRoute(routeDate.toLocalDate(),
                sortingCenter, courier);

        RouteFinish routeFinish = testFactory.storedEmptyRouteFinish(route,
                testFactory.storedUser(sortingCenter,
                        new Random().nextLong()
                ));

        testFactory.storedRouteFinishPlace(
                routeFinish,
                place.getId(),
                place.getExternalId(),
                scOrder.getId(),
                PlaceStatus.ACCEPTED,
                SortableStatus.ARRIVED_DIRECT,
                cell.getId(),
                null,
                null
        );

        var discrepancyOrders = inboundQueryService.getDiscrepancyInbounds(List.of(inbound))
                .get(0).getAllOrders();
        Assertions.assertThat(discrepancyOrders.size()).isEqualTo(0);


        Outbound outbound = testFactory.createOutbound(sortingCenter);
        Registry outboundRegistry = testFactory.bindRegistry(outbound.getExternalId(), RegistryType.FACTUAL);
        Registry inboundRegistry = testFactory.bindRegistry(inbound, outboundRegistry.getExternalId(),
                RegistryType.PLANNED);


        testFactory.bindOrder(inboundRegistry, scOrder.getExternalId(), scOrder.getExternalId(), null);
        testFactory.bindOrder(inboundRegistry, scOrder2.getExternalId(), scOrder2.getExternalId(), null);
        testFactory.bindOrder(inboundRegistry, orderExternalId3, orderExternalId3, null);
        testFactory.bindOrder(inboundRegistry, orderExternalId4, orderExternalId4, null);

        discrepancyOrders = inboundQueryService.getDiscrepancyInbounds(List.of(inbound))
                .get(0).getAllOrders();

        Assertions.assertThat(discrepancyOrders.size()).isEqualTo(2);
        discrepancyOrders.sort(Comparator.comparing(RegistryOrder::getExternalId));
        Assertions.assertThat(discrepancyOrders.get(0).getExternalId()).isEqualTo(orderExternalId3);
        Assertions.assertThat(discrepancyOrders.get(1).getExternalId()).isEqualTo(orderExternalId4);

        testFactory.createOrder(order(sortingCenter).externalId(orderExternalId3).build()).accept().get();

        discrepancyOrders = inboundQueryService.getDiscrepancyInbounds(List.of(inbound))
                .get(0).getAllOrders();

        Assertions.assertThat(discrepancyOrders.size()).isEqualTo(1);
        discrepancyOrders.sort(Comparator.comparing(RegistryOrder::getExternalId));
        Assertions.assertThat(discrepancyOrders.get(0).getExternalId()).isEqualTo(orderExternalId4);

        testFactory.createOrder(order(sortingCenter).externalId(orderExternalId4).build()).accept().get();

        discrepancyOrders = inboundQueryService.getDiscrepancyInbounds(List.of(inbound))
                .get(0).getAllOrders();

        Assertions.assertThat(discrepancyOrders.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("Сгенерировать дто для единого АПП по грузоместам для Поставки")
    void getTransferActForPlacesTest() {
        testFactory.setSortingCenterProperty(sortingCenter, CREATE_LOTS_FROM_REGISTRY, true);
        testFactory.setSortingCenterProperty(sortingCenter, ENABLE_INBOUND_TRANSFER_ACT_BUILD_FOR_PLACES, true);

        testFactory.createOrder(order(sortingCenter)
                .externalId("o-1")
                .places("p-11", "p-12", "p-13")
                .dsType(DeliveryServiceType.TRANSIT)
                .deliveryDate(LocalDate.now(clock).minusDays(1))
                .shipmentDate(LocalDate.now(clock).minusDays(1))
                .build()).updateShipmentDate(LocalDate.now(clock).minusDays(1)).get();

        testFactory.createForToday(order(sortingCenter)
                .externalId("o-2")
                .places("p-21", "p-22")
                .dsType(DeliveryServiceType.TRANSIT)
                .deliveryDate(LocalDate.now(clock))
                .shipmentDate(LocalDate.now(clock))
                .build()).get();

        var user = testFactory.storedUser(sortingCenter, 1919L);

        var movementCourier = testFactory.storedMovementCourier(1200000000L);
        var inbound = testFactory.createInbound(TestFactory.CreateInboundParams.builder()
                .inboundType(InboundType.DS_SC)
                .sortingCenter(sortingCenter)
                .inboundExternalId("in-1")
                .movementCourier(movementCourier)
                .registryMap(
                        Map.of("registry_1",
                                List.of(
                                        Pair.of("o-1", "p-11"),
                                        Pair.of("o-1", "p-12"),
                                        Pair.of("o-2", "p-21"))
                        )
                ).placeInPallets(
                        Map.of(
                                "p-11", "SC_LOT_1",
                                "p-12", "SC_LOT_1",
                                "p-21", "SC_LOT_2"
                        )
                ).fromDate(OffsetDateTime.now(clock))
                .toDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId(testFactory.storedWarehouse().getYandexId())
                .build()
        );

        testFactory.preAcceptLotWithPlaces("SC_LOT_1", sortingCenter, user);
        testFactory.finishAcceptLotWithPlaces("SC_LOT_1", sortingCenter, user);
        testFactory.preAcceptLotWithPlaces("SC_LOT_2", sortingCenter, user);
        testFactory.finishAcceptLotWithPlaces("SC_LOT_2", sortingCenter, user);

        var transferActForPlaces = inboundQueryService.getTransferActForPlaces(inbound);
        assertThat(transferActForPlaces)
                .isEqualToIgnoringGivenFields(
                        TransferActPlaceDto.builder()
                                .number("П-in-1")
                                .date(inbound.getToDate().toLocalDate())
                                .recipient("ООО Яндекс.Маркет")
                                .sender("ООО Ромашка-Склад")
                                .executor("ООО Яндекс.Маркет")
                                .courier(movementCourier.getLegalEntityName())
                                .build(),
                        "places"
                );

        assertThat(transferActForPlaces.getPlaces())
                .usingElementComparatorIgnoringFields("totalSum", "routeDocumentType")
                .containsExactlyInAnyOrder(
                        TransferActPlaceDto.Place.builder()
                                .placeMainPartnerCode("p-11")
                                .orderExternalId("o-1")
                                .lotName("SC_LOT_1")
                                .build(),
                        TransferActPlaceDto.Place.builder()
                                .placeMainPartnerCode("p-12")
                                .orderExternalId("o-1")
                                .lotName("SC_LOT_1")
                                .build(),
                        TransferActPlaceDto.Place.builder()
                                .placeMainPartnerCode("p-21")
                                .orderExternalId("o-2")
                                .lotName("SC_LOT_2")
                                .build()
                );
    }

    @ParameterizedTest
    @ValueSource(longs = {-100000, 0, 100000})
    void inboundCanHaveAnyDateAccordingToMarkettplsc2662(Long shift) {
        String orderExternalId = "test-order-external-id";
        String orderExternalId2 = "test-order-external-id2";
        String orderExternalId3 = "test-order-external-id3";
        String orderExternalId4 = "test-order-external-id4";

        Long courierUid = 1000L;
        String inboundExternalId = "inboundExtId1";

        var scOrderWithPlaces =
                testFactory.createOrder(order(sortingCenter).externalId(orderExternalId).build()).accept()
                        .getOrderWithPlaces();
        var scOrder = scOrderWithPlaces.order();
        var place = scOrderWithPlaces.place(scOrder.getExternalId());
        OrderLike scOrder2 =
                testFactory.createOrder(order(sortingCenter).externalId(orderExternalId2).build()).accept().get();

        String warehouseFromId = "warehouse-from-id";

        OffsetDateTime inboundDate = OffsetDateTime.now(clock);
        OffsetDateTime routeDate = OffsetDateTime.now(clock);

        // даты инбаунда теперь не должны играть роли при выборе заказов
        inboundDate = inboundDate.plusDays(shift);


        Cell cell = testFactory.storedCell(sortingCenter, "cellName");
        var movementCourier = testFactory.getMovementCourier(courierUid);
        if (movementCourier == null) {
            movementCourier = testFactory.storedMovementCourier(courierUid);
        }
        var inbound = testFactory.createInbound(inboundParams(sortingCenter,
                warehouseFromId, inboundDate, movementCourier, inboundExternalId));

        Courier courier = testFactory.storedCourier(courierUid);
        Route route = testFactory.storedIncomingCourierDropOffRoute(routeDate.toLocalDate(),
                sortingCenter, courier);

        RouteFinish routeFinish = testFactory.storedEmptyRouteFinish(route,
                testFactory.storedUser(sortingCenter,
                        new Random().nextLong()
                ));

        testFactory.storedRouteFinishPlace(
                routeFinish,
                place.getId(),
                place.getExternalId(),
                scOrder.getId(),
                PlaceStatus.ACCEPTED,
                SortableStatus.ARRIVED_DIRECT,
                cell.getId(),
                null,
                null
        );

        Outbound outbound = testFactory.createOutbound(sortingCenter);
        Registry outboundRegistry = testFactory.bindRegistry(outbound.getExternalId(), RegistryType.FACTUAL);
        Registry inboundRegistry = testFactory.bindRegistry(inbound, outboundRegistry.getExternalId(),
                RegistryType.PLANNED);


        testFactory.bindOrder(inboundRegistry, scOrder.getExternalId(), scOrder.getExternalId(), null);
        testFactory.bindOrder(inboundRegistry, scOrder2.getExternalId(), scOrder2.getExternalId(), null);
        testFactory.bindOrder(inboundRegistry, orderExternalId3, orderExternalId3, null);
        testFactory.bindOrder(inboundRegistry, orderExternalId4, orderExternalId4, null);

        var discrepancyOrders = inboundQueryService.getDiscrepancyInbounds(List.of(inbound))
                .get(0).getAllOrders();

        Assertions.assertThat(discrepancyOrders.size()).isEqualTo(2);
        discrepancyOrders.sort(Comparator.comparing(RegistryOrder::getExternalId));
        Assertions.assertThat(discrepancyOrders.get(0).getExternalId()).isEqualTo(orderExternalId3);
        Assertions.assertThat(discrepancyOrders.get(1).getExternalId()).isEqualTo(orderExternalId4);
    }


    private Inbound createInbound(
            String externalId,
            OffsetDateTime date,
            MovementCourier courier,
            SortingCenter sortingCenter,
            Warehouse warehouse
    ) {
        return testFactory.createInbound(TestFactory.CreateInboundParams
                .builder()
                .inboundType(InboundType.DS_SC)
                .inboundExternalId(externalId)
                .fromDate(date)
                .toDate(date)
                .movementCourier(courier)
                .sortingCenter(sortingCenter)
                .warehouseFromExternalId(warehouse.getYandexId())
                .registryMap(Map.of())
                .build());
    }

    private void assertInboundGroup(Inbound inbound, List<Inbound> expectedGroup) {
        List<Inbound> actualGroup = inboundQueryService.getInboundGroupForSameCourier(
                inbound.getExternalId(),
                inbound.getSortingCenter(),
                Set.of(InboundType.values())
        );
        assertThat(actualGroup).hasSameElementsAs(expectedGroup);
    }

    private TestFactory.CreateInboundParams inboundParams(SortingCenter sortingCenter, String warehouseFromId,
                                                          OffsetDateTime inboundDate,
                                                          MovementCourier courier1, String inboundExternalId) {
        return TestFactory.CreateInboundParams
                .builder()
                .inboundExternalId(inboundExternalId)
                .inboundType(InboundType.DS_SC)
                .sortingCenter(sortingCenter)
                .movementCourier(courier1)
                .fromDate(inboundDate)
                .warehouseFromExternalId(warehouseFromId)
                .toDate(inboundDate)
                .registryMap(new HashMap<>())
                .build();
    }

}
