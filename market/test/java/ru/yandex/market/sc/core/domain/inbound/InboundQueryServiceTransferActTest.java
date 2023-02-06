package ru.yandex.market.sc.core.domain.inbound;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.annotation.Nullable;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.sc.core.configuration.ConfigurationProperties;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.inbound.repository.Registry;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryType;
import ru.yandex.market.sc.core.domain.movement_courier.repository.MovementCourier;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.outbound.repository.Outbound;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.route.model.TransferActDto;
import ru.yandex.market.sc.core.domain.route.repository.Route;
import ru.yandex.market.sc.core.domain.route.repository.RouteFinish;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.tpl.common.db.configuration.ConfigurationService;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@SuppressWarnings("checkstyle:RegexpSingleline")
@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InboundQueryServiceTransferActTest {

    private final TestFactory testFactory;
    private SortingCenter sortingCenter;
    private final InboundQueryService inboundQueryService;
    private final Clock clock;
    private final Set<InboundType> inboundTypes = Set.of(InboundType.values());
    private final ConfigurationService configurationService;
    private final JdbcTemplate jdbcTemplate;

    @BeforeEach
    void init() {
        enableOpt(true);
        sortingCenter = testFactory.storedSortingCenter();
    }


    @Test
    void oneOrderTransferActGenerated() {

        String orderExternalId = "test-order-external-id";

        ScOrder scOrder =
                testFactory.createOrder(order(sortingCenter).externalId(orderExternalId).build()).accept().get();

        TestDataParams params = TestDataParams.builder()
                .scOrder(scOrder)
                .customSortingCenter(sortingCenter).build();
        Inbound inbound = generateTestData(params);

        assertThat(inbound.getMovementCourier()).isNotNull();

        TransferActDto transferAct = inboundQueryService.getTransferAct(inbound.getExternalId(), this.sortingCenter,
                inboundTypes, true);
        checkIfOnlyOrderPresent(transferAct, scOrder);
        assertThat(transferAct.getCourier()).isEqualTo(inbound.getMovementCourier().getLegalEntityName());
    }


    @Test
    void oneOrderTransferActGeneratedWithoutRegistry() {

        String orderExternalId = "test-order-external-id";

        var scOrder =
                testFactory.createOrder(order(sortingCenter).externalId(orderExternalId).build()).accept().get();

        TestDataParams params = TestDataParams.builder()
                .scOrder(scOrder)
                .customSortingCenter(sortingCenter)
                .build();
        Inbound inbound = generateTestData(params);

        TransferActDto transferAct = inboundQueryService.getTransferAct(inbound.getExternalId(), this.sortingCenter,
                inboundTypes, false);
        checkIfOnlyOrderPresent(transferAct, scOrder);
    }

    @Test
    void twoOrderTransferActGenerated() {

        String orderExternalId = "test-order-external-id";
        var courierUid = 1123L;
        var scOrderWithPlaces1 =
                testFactory.createOrder(order(sortingCenter).externalId(orderExternalId).build()).accept()
                        .getOrderWithPlaces();
        var scOrderWithPlaces2 =
                testFactory.createOrder(order(sortingCenter).externalId(orderExternalId + 2).build()).accept()
                        .getOrderWithPlaces();


        String warehouseFromId = "warehouse-from-id";

        OffsetDateTime inboundDate = OffsetDateTime.now(clock);
        OffsetDateTime routeDate = OffsetDateTime.now(clock);


        Cell cell = testFactory.storedCell(sortingCenter);

        var movementCourier = testFactory.storedMovementCourier(courierUid);
        var inbound = testFactory.createInbound(inboundParams(sortingCenter, warehouseFromId, inboundDate,
                movementCourier, "inboundExtId"));

        Courier courier = testFactory.storedCourier(courierUid);
        Route route = testFactory.storedIncomingCourierDropOffRoute(routeDate.toLocalDate(),
                sortingCenter,
                courier
        );


        RouteFinish routeFinish = testFactory.storedEmptyRouteFinish(route,
                testFactory.storedUser(sortingCenter,
                        courierUid));


        var place1 = scOrderWithPlaces1.place(scOrderWithPlaces1.order().getExternalId());
        testFactory.storedRouteFinishPlace(
                routeFinish,
                place1.getId(),
                place1.getMainPartnerCode(),
                place1.getOrderId(),
                PlaceStatus.ACCEPTED,
                SortableStatus.ARRIVED_DIRECT,
                null,
                null,
                null
        );

        var place2 = scOrderWithPlaces2.place(scOrderWithPlaces2.order().getExternalId());
        testFactory.storedRouteFinishPlace(
                routeFinish,
                place2.getId(),
                place2.getMainPartnerCode(),
                place2.getOrderId(),
                PlaceStatus.ACCEPTED,
                SortableStatus.ARRIVED_DIRECT,
                null,
                null,
                null
        );

        Outbound outbound = testFactory.createOutbound(sortingCenter);
        Registry outboundRegistry = testFactory.bindRegistry(outbound.getExternalId(), RegistryType.FACTUAL);
        Registry inboundRegistry = testFactory.bindRegistry(inbound, outboundRegistry.getExternalId(),
                RegistryType.PLANNED);


        testFactory.bindOrder(inboundRegistry, scOrderWithPlaces1.order().getExternalId(),
                scOrderWithPlaces1.order().getExternalId(), null);
        testFactory.bindOrder(inboundRegistry, scOrderWithPlaces2.order().getExternalId(),
                scOrderWithPlaces2.order().getExternalId(), null);


        TransferActDto transferAct = inboundQueryService.getTransferAct(inbound.getExternalId(),
                this.sortingCenter, inboundTypes, true);
        checkIfOrderCountEquals(transferAct, 2);
        checkIfOrderPresent(transferAct, scOrderWithPlaces1.order());
        checkIfOrderPresent(transferAct, scOrderWithPlaces2.order());
    }

    @Test
    void oneOrderTransferActGeneratedWhenOnlyOneOrderInInboundRegistry() {

        String orderExternalId = "test-order-external-id";
        var courierUid = 1123L;
        var scOrderWithPlaces1 =
                testFactory.createOrder(order(sortingCenter).externalId(orderExternalId).build()).accept()
                        .getOrderWithPlaces();
        var scOrderWithPlaces2 =
                testFactory.createOrder(order(sortingCenter).externalId(orderExternalId + 2).build()).accept()
                        .getOrderWithPlaces();


        String warehouseFromId = "warehouse-from-id";

        OffsetDateTime inboundDate = OffsetDateTime.now(clock);
        OffsetDateTime routeDate = OffsetDateTime.now(clock);


        Cell cell = testFactory.storedCell(sortingCenter);

        var movementCourier = testFactory.storedMovementCourier(courierUid);
        var inbound = testFactory.createInbound(inboundParams(sortingCenter, warehouseFromId, inboundDate,
                movementCourier, "inboundExtId"));

        Courier courier = testFactory.storedCourier(courierUid);
        Route route = testFactory.storedIncomingCourierDropOffRoute(
                routeDate.toLocalDate(),
                sortingCenter,
                courier);


        RouteFinish routeFinish = testFactory.storedEmptyRouteFinish(route,
                testFactory.storedUser(sortingCenter,
                        courierUid));


        var place1 = scOrderWithPlaces1.place(scOrderWithPlaces1.order().getExternalId());
        testFactory.storedRouteFinishPlace(
                routeFinish,
                place1.getId(),
                place1.getMainPartnerCode(),
                place1.getOrderId(),
                PlaceStatus.ACCEPTED,
                SortableStatus.ARRIVED_DIRECT,
                null,
                null,
                null
        );

        var place2 = scOrderWithPlaces2.place(scOrderWithPlaces2.order().getExternalId());
        testFactory.storedRouteFinishPlace(
                routeFinish,
                place2.getId(),
                place2.getMainPartnerCode(),
                place2.getOrderId(),
                PlaceStatus.ACCEPTED,
                SortableStatus.ARRIVED_DIRECT,
                null,
                null,
                null
        );

        Outbound outbound = testFactory.createOutbound(sortingCenter);
        Registry outboundRegistry = testFactory.bindRegistry(outbound.getExternalId(), RegistryType.FACTUAL);
        Registry inboundRegistry = testFactory.bindRegistry(inbound, outboundRegistry.getExternalId(),
                RegistryType.PLANNED);


        testFactory.bindOrder(inboundRegistry, scOrderWithPlaces1.order().getExternalId(),
                scOrderWithPlaces1.order().getExternalId(), null);

        TransferActDto transferAct = inboundQueryService.getTransferAct(inbound.getExternalId(),
                this.sortingCenter, inboundTypes, true);
        checkIfOnlyOrderPresent(transferAct, scOrderWithPlaces1.order());

    }

    @Test
    void twoOrderTransferActGeneratedWhenOnlyOneOrderHasRouteFinishOrder() {

        String orderExternalId = "test-order-external-id";
        var courierUid = 1123L;
        var scOrderWithPlaces1 =
                testFactory.createOrder(order(sortingCenter).externalId(orderExternalId).build()).accept()
                        .getOrderWithPlaces();
        OrderLike scOrder2 =
                testFactory.createOrder(order(sortingCenter).externalId(orderExternalId + 2).build()).get();


        String warehouseFromId = "warehouse-from-id";

        OffsetDateTime inboundDate = OffsetDateTime.now(clock);
        OffsetDateTime routeDate = OffsetDateTime.now(clock);


        Cell cell = testFactory.storedCell(sortingCenter);

        var movementCourier = testFactory.storedMovementCourier(courierUid);
        var inbound = testFactory.createInbound(inboundParams(sortingCenter, warehouseFromId, inboundDate,
                movementCourier, "inboundExtId"));

        Courier courier = testFactory.storedCourier(courierUid);
        Route route = testFactory.storedIncomingCourierDropOffRoute(
                routeDate.toLocalDate(),
                sortingCenter,
                courier);


        RouteFinish routeFinish = testFactory.storedEmptyRouteFinish(route,
                testFactory.storedUser(sortingCenter,
                        courierUid));


        var place1 = scOrderWithPlaces1.place(scOrderWithPlaces1.order().getExternalId());
        testFactory.storedRouteFinishPlace(
                routeFinish,
                place1.getId(),
                place1.getMainPartnerCode(),
                place1.getOrderId(),
                PlaceStatus.ACCEPTED,
                SortableStatus.ARRIVED_DIRECT,
                null,
                null,
                null
        );

        Outbound outbound = testFactory.createOutbound(sortingCenter);
        Registry outboundRegistry = testFactory.bindRegistry(outbound.getExternalId(), RegistryType.FACTUAL);
        Registry inboundRegistry = testFactory.bindRegistry(inbound, outboundRegistry.getExternalId(),
                RegistryType.PLANNED);


        testFactory.bindOrder(inboundRegistry, scOrderWithPlaces1.order().getExternalId(),
                scOrderWithPlaces1.order().getExternalId(), null);
        testFactory.bindOrder(inboundRegistry, scOrder2.getExternalId(),
                scOrder2.getExternalId(), null);


        TransferActDto transferAct = inboundQueryService.getTransferAct(inbound.getExternalId(),
                this.sortingCenter, inboundTypes, true);

        checkIfOrderPresent(transferAct, scOrderWithPlaces1.order());
        checkIfOrderPresent(transferAct, scOrder2);

    }

    @Test
    void twoOrderTransferActGeneratedWhenNoOrderHasRouteFinishOrder() {

        String orderExternalId = "test-order-external-id";
        var courierUid = 1123L;
        OrderLike scOrder1 =
                testFactory.createOrder(order(sortingCenter).externalId(orderExternalId).build()).accept().get();
        OrderLike scOrder2 =
                testFactory.createOrder(order(sortingCenter).externalId(orderExternalId + 2).build()).get();


        String warehouseFromId = "warehouse-from-id";

        OffsetDateTime inboundDate = OffsetDateTime.now(clock);
        OffsetDateTime routeDate = OffsetDateTime.now(clock);


        Cell cell = testFactory.storedCell(sortingCenter);

        var movementCourier = testFactory.storedMovementCourier(courierUid);
        var inbound = testFactory.createInbound(inboundParams(sortingCenter, warehouseFromId, inboundDate,
                movementCourier, "inboundExtId"));

        Courier courier = testFactory.storedCourier(courierUid);
        Route route = testFactory.storedIncomingCourierDropOffRoute(
                routeDate.toLocalDate(),
                sortingCenter,
                courier);


        Outbound outbound = testFactory.createOutbound(sortingCenter);
        Registry outboundRegistry = testFactory.bindRegistry(outbound.getExternalId(), RegistryType.FACTUAL);
        Registry inboundRegistry = testFactory.bindRegistry(inbound, outboundRegistry.getExternalId(),
                RegistryType.PLANNED);


        testFactory.bindOrder(inboundRegistry, scOrder1.getExternalId(),
                scOrder1.getExternalId(), null);
        testFactory.bindOrder(inboundRegistry, scOrder2.getExternalId(),
                scOrder2.getExternalId(), null);


        TransferActDto transferAct = inboundQueryService.getTransferAct(inbound.getExternalId(),
                this.sortingCenter, inboundTypes, true);

        checkIfOrderPresent(transferAct, scOrder1);
        checkIfOrderPresent(transferAct, scOrder2);

    }


    @Test
    void twoOrderTransferActGeneratedWithoutRegistry() {

        String orderExternalId = "test-order-external-id";
        var courierUid = 1123L;
        var scOrderWithPlaces1 =
                testFactory.createOrder(order(sortingCenter).externalId(orderExternalId).build()).accept()
                        .getOrderWithPlaces();
        var scOrderWithPlaces2 =
                testFactory.createOrder(order(sortingCenter).externalId(orderExternalId + 2).build()).accept()
                        .getOrderWithPlaces();


        String warehouseFromId = "warehouse-from-id";

        OffsetDateTime inboundDate = OffsetDateTime.now(clock);
        OffsetDateTime routeDate = OffsetDateTime.now(clock);


        Cell cell = testFactory.storedCell(sortingCenter);

        var movementCourier = testFactory.storedMovementCourier(courierUid);
        var inbound = testFactory.createInbound(inboundParams(sortingCenter, warehouseFromId, inboundDate,
                movementCourier, "inboundExtId"));

        Courier courier = testFactory.storedCourier(courierUid);
        Route route = testFactory.storedIncomingCourierDropOffRoute(
                routeDate.toLocalDate(),
                sortingCenter,
                courier);


        RouteFinish routeFinish = testFactory.storedEmptyRouteFinish(route,
                testFactory.storedUser(sortingCenter,
                        courierUid));


        var place1 = scOrderWithPlaces1.place(scOrderWithPlaces1.order().getExternalId());
        testFactory.storedRouteFinishOrder(
                routeFinish,
                place1.getOrderId(),
                place1.getExternalId(),
                place1.getFfStatus(),
                null
        );
        testFactory.storedRouteFinishPlace(
                routeFinish,
                place1.getId(),
                place1.getMainPartnerCode(),
                place1.getOrderId(),
                PlaceStatus.ACCEPTED,
                SortableStatus.ARRIVED_DIRECT,
                null,
                null,
                null
        );

        var place2 = scOrderWithPlaces2.place(scOrderWithPlaces2.order().getExternalId());
        testFactory.storedRouteFinishOrder(
                routeFinish,
                place2.getOrderId(),
                place2.getExternalId(),
                place2.getFfStatus(),
                null
        );
        testFactory.storedRouteFinishPlace(
                routeFinish,
                place2.getId(),
                place2.getMainPartnerCode(),
                place2.getOrderId(),
                PlaceStatus.ACCEPTED,
                SortableStatus.ARRIVED_DIRECT,
                null,
                null,
                null
        );

        Outbound outbound = testFactory.createOutbound(sortingCenter);
        Registry outboundRegistry = testFactory.bindRegistry(outbound.getExternalId(), RegistryType.FACTUAL);
        Registry inboundRegistry = testFactory.bindRegistry(inbound, outboundRegistry.getExternalId(),
                RegistryType.FACTUAL);


        testFactory.bindOrder(inboundRegistry, scOrderWithPlaces1.order().getExternalId(),
                scOrderWithPlaces1.order().getExternalId(), null);
        testFactory.bindOrder(inboundRegistry, scOrderWithPlaces2.order().getExternalId(),
                scOrderWithPlaces2.order().getExternalId(), null);


        TransferActDto transferAct = inboundQueryService.getTransferAct(inbound.getExternalId(),
                this.sortingCenter, inboundTypes, false);
        checkIfOrderCountEquals(transferAct, 2);
        checkIfOrderPresent(transferAct, scOrderWithPlaces1.order());
        checkIfOrderPresent(transferAct, scOrderWithPlaces2.order());
    }

    @Test
    void multiPlaceTransferActGenerated() {

        String orderExternalId = "test-order-external-id";

        OrderLike scOrder = testFactory.createOrder(
                order(sortingCenter).externalId(orderExternalId)
                        .places("place1", "place2", "place3").build()
        ).acceptPlaces().get();

        TestDataParams params = TestDataParams.builder().scOrder(scOrder).customSortingCenter(sortingCenter).build();
        Inbound inbound = generateTestData(params);

        TransferActDto transferAct = inboundQueryService.getTransferAct(inbound.getExternalId(), this.sortingCenter,
                inboundTypes, true);
        checkIfOnlyOrderPresent(transferAct, scOrder);
    }

    @Test
    void multiPlaceTransferActGeneratedWithoutRegistry() {

        String orderExternalId = "test-order-external-id";

        OrderLike scOrder = testFactory.createOrder(
                order(sortingCenter).externalId(orderExternalId)
                        .places("place1", "place2", "place3").build()
        ).acceptPlaces().get();

        TestDataParams params = TestDataParams.builder().scOrder(scOrder).customSortingCenter(sortingCenter).build();
        Inbound inbound = generateTestData(params);

        TransferActDto transferAct = inboundQueryService.getTransferAct(inbound.getExternalId(), this.sortingCenter,
                inboundTypes, false);
        checkIfOnlyOrderPresent(transferAct, scOrder);
    }

    @Test
    void emptyTransferActGeneratedWhenOrderOnOtherInbound() {

        String orderExternalId = "test-order-external-id";

        OrderLike scOrder =
                testFactory.createOrder(order(sortingCenter).externalId(orderExternalId).build()).accept().get();

        TestDataParams withOrder = TestDataParams.builder()
                .scOrder(scOrder)
                .customSortingCenter(sortingCenter)
                .courierUid(1000L)
                .cellName("cell1")
                .inboundExternalId("inboundExtId1")
                .build();

        TestDataParams withOutOrder = TestDataParams.builder()
                .scOrder(null)
                .customSortingCenter(sortingCenter)
                .courierUid(300L)
                .cellName("cell2")
                .inboundExternalId("inboundExtId2")
                .build();

        Inbound inboundWithOrder = generateTestData(withOrder);
        Inbound inboundWithOutOrder = generateTestData(withOutOrder);

        TransferActDto transferAct = inboundQueryService.getTransferAct(inboundWithOutOrder.getExternalId(),
                inboundWithOutOrder.getSortingCenter(), inboundTypes, true);

        checkIfEmpty(transferAct);
    }


    @Test
    void emptyTransferActGeneratedWhenOrderOnOtherInboundWithoutRegistry() {

        String orderExternalId = "test-order-external-id";

        OrderLike scOrder =
                testFactory.createOrder(order(sortingCenter).externalId(orderExternalId).build()).accept().get();

        TestDataParams withOrder = TestDataParams.builder()
                .scOrder(scOrder)
                .customSortingCenter(sortingCenter)
                .courierUid(1000L)
                .cellName("cell1")
                .inboundExternalId("inboundExtId1")
                .build();

        TestDataParams withOutOrder = TestDataParams.builder()
                .scOrder(null)
                .customSortingCenter(sortingCenter)
                .courierUid(300L)
                .cellName("cell2")
                .inboundExternalId("inboundExtId2")
                .build();

        Inbound inboundWithOrder = generateTestData(withOrder);
        Inbound inboundWithOutOrder = generateTestData(withOutOrder);

        TransferActDto transferAct = inboundQueryService.getTransferAct(inboundWithOutOrder.getExternalId(),
                inboundWithOutOrder.getSortingCenter(), inboundTypes, false);

        checkIfEmpty(transferAct);
    }

    @Test
    void emptyTransferActGeneratedWhenOrderWithTheSameCourierOnAnotherSortingCenter() {

        String orderExternalId = "test-order-external-id";
        SortingCenter otherSortingCenter = testFactory.storedSortingCenter(113);
        OrderLike scOrder =
                testFactory.createOrder(order(otherSortingCenter).externalId(orderExternalId).build()).accept().get();

        TestDataParams withOrder = TestDataParams.builder()
                .scOrder(scOrder)
                .customSortingCenter(otherSortingCenter)
                .courierUid(1000L)
                .cellName("cell1")
                .inboundExternalId("inboundExtId1")
                .build();

        TestDataParams withOutOrder = TestDataParams.builder()
                .scOrder(null)
                .customSortingCenter(sortingCenter)
                .courierUid(1000L)
                .cellName("cell2")
                .inboundExternalId("inboundExtId2")
                .build();

        Inbound inboundWithOrder = generateTestData(withOrder);
        Inbound inboundWithOutOrder = generateTestData(withOutOrder);

        TransferActDto transferAct = inboundQueryService.getTransferAct(inboundWithOutOrder.getExternalId(),
                inboundWithOutOrder.getSortingCenter(), inboundTypes, true);

        checkIfEmpty(transferAct);
    }

    @Test
    void emptyTransferActGeneratedWhenOrderWithTheSameCourierOnAnotherSortingCenterWithoutRegistry() {

        String orderExternalId = "test-order-external-id";
        SortingCenter otherSortingCenter = testFactory.storedSortingCenter(113);
        OrderLike scOrder =
                testFactory.createOrder(order(otherSortingCenter).externalId(orderExternalId).build()).accept().get();

        TestDataParams withOrder = TestDataParams.builder()
                .scOrder(scOrder)
                .customSortingCenter(otherSortingCenter)
                .courierUid(1000L)
                .cellName("cell1")
                .inboundExternalId("inboundExtId1")
                .build();

        TestDataParams withOutOrder = TestDataParams.builder()
                .scOrder(null)
                .customSortingCenter(sortingCenter)
                .courierUid(1000L)
                .cellName("cell2")
                .inboundExternalId("inboundExtId2")
                .build();

        Inbound inboundWithOrder = generateTestData(withOrder);
        Inbound inboundWithOutOrder = generateTestData(withOutOrder);

        TransferActDto transferAct = inboundQueryService.getTransferAct(inboundWithOutOrder.getExternalId(),
                inboundWithOutOrder.getSortingCenter(), inboundTypes, false);

        checkIfEmpty(transferAct);
    }

    @Test
    void emptyTransferActGeneratedWhenInboundDatesAreNotMatchedToRoute() {

        String orderExternalId = "test-order-external-id";

        OrderLike scOrder =
                testFactory.createOrder(order(sortingCenter).externalId(orderExternalId).build()).accept().get();

        TestDataParams withOutOrder = TestDataParams.builder()
                .scOrder(scOrder)
                .customSortingCenter(sortingCenter)
                .shiftInboundDates(true)
                .build();

        Inbound inbound = generateTestData(withOutOrder);

        TransferActDto transferAct = inboundQueryService.getTransferAct(inbound.getExternalId(),
                inbound.getSortingCenter(), inboundTypes, false);

        checkIfEmpty(transferAct);
    }

    @Test
    void emptyTransferActGeneratedWhenOrderInIncorrectStatus() {

        String orderExternalId = "test-order-external-id";

        OrderLike scOrder = testFactory.createOrder(order(sortingCenter).externalId(orderExternalId).build()).get();

        TestDataParams incorrectOrderStatus = TestDataParams.builder()
                .scOrder(scOrder)
                .customSortingCenter(sortingCenter)
                .build();

        Inbound inbound = generateTestData(incorrectOrderStatus);

        TransferActDto transferAct = inboundQueryService.getTransferAct(inbound.getExternalId(),
                inbound.getSortingCenter(), inboundTypes, false);

        checkIfEmpty(transferAct);
    }

    @Test
    void emptyTransferActGeneratedWhenRouteOnAnotherSortingCenter() {

        String orderExternalId = "test-order-external-id";

        OrderLike scOrder = testFactory.createOrder(order(sortingCenter).externalId(orderExternalId).build())
                .accept().get();

        TestDataParams incorrectOrderStatus = TestDataParams.builder()
                .scOrder(scOrder)
                .customSortingCenter(sortingCenter)
                .routeOnOtherSortingCenter(true)
                .build();

        Inbound inbound = generateTestData(incorrectOrderStatus);

        TransferActDto transferAct = inboundQueryService.getTransferAct(inbound.getExternalId(),
                inbound.getSortingCenter(), inboundTypes, false);

        checkIfEmpty(transferAct);
    }

    @Test
    void emptyTransferActGeneratedWhenRouteCreatedForDifferentCourier() {

        String orderExternalId = "test-order-external-id";

        OrderLike scOrder = testFactory.createOrder(order(sortingCenter).externalId(orderExternalId).build())
                .accept().get();

        TestDataParams incorrectOrderStatus = TestDataParams.builder()
                .scOrder(scOrder)
                .customSortingCenter(sortingCenter)
                .routeForDifferentCourier(true)
                .build();

        Inbound inbound = generateTestData(incorrectOrderStatus);

        TransferActDto transferAct = inboundQueryService.getTransferAct(inbound.getExternalId(),
                inbound.getSortingCenter(), inboundTypes, false);

        checkIfEmpty(transferAct);
    }

    @Test
    void emptyTransferActGeneratedWhenRouteFinishDoesntExist() {

        String orderExternalId = "test-order-external-id";

        OrderLike scOrder =
                testFactory.createOrder(order(sortingCenter).externalId(orderExternalId).build()).get();

        TestDataParams withOutOrder = TestDataParams.builder()
                .scOrder(scOrder)
                .customSortingCenter(sortingCenter)
                .createRouteFinish(false)
                .build();

        Inbound inbound = generateTestData(withOutOrder);

        TransferActDto transferAct = inboundQueryService.getTransferAct(inbound.getExternalId(),
                inbound.getSortingCenter(), inboundTypes, false);

        checkIfEmpty(transferAct);
    }

    @Test
    void emptyTransferActGeneratedWhenRouteFinishOrderDoesntExist() {
        String orderExternalId = "test-order-external-id";

        OrderLike scOrder =
                testFactory.createOrder(order(sortingCenter).externalId(orderExternalId).build()).get();

        TestDataParams withOutOrder = TestDataParams.builder()
                .scOrder(scOrder)
                .customSortingCenter(sortingCenter)
                .createRouteFinishOrder(false)
                .build();

        Inbound inbound = generateTestData(withOutOrder);

        TransferActDto transferAct = inboundQueryService.getTransferAct(inbound.getExternalId(),
                inbound.getSortingCenter(), inboundTypes, false);

        checkIfEmpty(transferAct);
    }

    @Test
    void emptyTransferActGeneratedWhenRouteFinishOrderInWrongStatus() {

        String orderExternalId = "test-order-external-id";

        OrderLike scOrder =
                testFactory.createOrder(order(sortingCenter).externalId(orderExternalId).build()).get();

        TestDataParams withOutOrder = TestDataParams.builder()
                .scOrder(scOrder)
                .customSortingCenter(sortingCenter)
                .incorrectRouteFinishOrderStatus(true)
                .build();

        Inbound inbound = generateTestData(withOutOrder);

        TransferActDto transferAct = inboundQueryService.getTransferAct(inbound.getExternalId(),
                inbound.getSortingCenter(), inboundTypes, false);

        checkIfEmpty(transferAct);
    }

    private void checkIfOnlyOrderPresent(TransferActDto transferAct, OrderLike scOrder) {
        checkIfOrderCountEquals(transferAct, 1);
        assertThat(transferAct).isNotNull();
        List<TransferActDto.Order> transferActOrders = transferAct.getOrders();
        assertThat(transferActOrders).isNotNull();
        assertThat(transferActOrders.isEmpty()).isEqualTo(false);

        BigDecimal magicSumFromXml = BigDecimal.valueOf(33600, 2);

        assertThat(transferActOrders.get(0)).isEqualTo(
                TransferActDto.Order.builder()
                        .externalId(scOrder.getExternalId())
                        .items(1)
                        .totalSum(magicSumFromXml)
                        .places(scOrder.getPlaceCount())
                        .build()
        );
    }

    private void checkIfOrderPresent(TransferActDto transferAct, OrderLike scOrder) {
        assertThat(transferAct).isNotNull();
        List<TransferActDto.Order> transferActOrders = transferAct.getOrders();
        assertThat(transferActOrders).isNotNull();
        assertThat(transferActOrders.isEmpty()).isEqualTo(false);

        BigDecimal magicSumFromXml = BigDecimal.valueOf(33600, 2);

        assertThat(transferActOrders.contains(
                        TransferActDto.Order.builder()
                                .externalId(scOrder.getExternalId())
                                .items(1)
                                .totalSum(magicSumFromXml)
                                .places(scOrder.getPlaceCount())
                                .build()
                )
        );
    }

    private void checkIfOrderCountEquals(TransferActDto transferAct, int size) {
        assertThat(transferAct).isNotNull();
        List<TransferActDto.Order> transferActOrders = transferAct.getOrders();
        assertThat(transferActOrders).isNotNull();
        assertThat(transferActOrders.size()).isEqualTo(size);
    }

    private void checkIfEmpty(TransferActDto transferAct) {
        checkIfOrderCountEquals(transferAct, 0);
    }

    @Builder
    private static class TestDataParams {

        @Builder.Default
        @Nullable
        OrderLike scOrder = null;

        SortingCenter customSortingCenter;

        @Builder.Default
        Long courierUid = 300L;

        @Builder.Default
        String cellName = "cell1";

        @Builder.Default
        String inboundExternalId = "inboundExternalId";

        @Builder.Default
        boolean shiftInboundDates = false;

        @Builder.Default
        boolean createRouteFinish = true;

        @Builder.Default
        boolean routeOnOtherSortingCenter = false;

        @Builder.Default
        boolean routeForDifferentCourier = false;

        @Builder.Default
        boolean createRouteFinishOrder = true;

        @Builder.Default
        boolean incorrectRouteFinishOrderStatus = false;

        @Builder.Default
        boolean createFactualRegistry = true;

    }


    private Inbound generateTestData(TestDataParams testParams) {
        String warehouseFromId = "warehouse-from-id";

        OffsetDateTime inboundDate = OffsetDateTime.now(clock);
        OffsetDateTime routeDate = OffsetDateTime.now(clock);

        if (testParams.shiftInboundDates) {
            inboundDate = inboundDate.plusDays(1);
        }


        Cell cell = testFactory.storedCell(testParams.customSortingCenter, testParams.cellName);
        var movementCourier = testFactory.getMovementCourier(testParams.courierUid);
        if (movementCourier == null) {
            movementCourier = testFactory.storedMovementCourier(testParams.courierUid);
        }
        var inbound = testFactory.createInbound(inboundParams(testParams.customSortingCenter,
                warehouseFromId, inboundDate, movementCourier, testParams.inboundExternalId));

        Courier courier = testFactory.storedCourier(testParams.courierUid);
        Route route = testFactory.storedIncomingCourierDropOffRoute(routeDate.toLocalDate(),
                testParams.routeOnOtherSortingCenter ? testFactory.storedSortingCenter(123) :
                        testParams.customSortingCenter,
                testParams.routeForDifferentCourier ? testFactory.storedCourier(123) : courier
        );

        if (testParams.createRouteFinish) {
            RouteFinish routeFinish = testFactory.storedEmptyRouteFinish(route,
                    testFactory.storedUser(testParams.customSortingCenter,
                            new Random().nextLong()
                    ));

            if (testParams.scOrder != null && testParams.createRouteFinishOrder) {
                testFactory.storedRouteFinishOrder(
                        routeFinish,
                        testParams.scOrder.getId(),
                        testParams.scOrder.getExternalId(),
                        testParams.scOrder.getFfStatus(),
                        null
                );

                var places = testFactory.orderPlaces(testParams.scOrder.getId());
                places.forEach(place -> testFactory.storedRouteFinishPlace(
                        routeFinish,
                        place.getId(),
                        place.getMainPartnerCode(),
                        place.getOrderId(),
                        testParams.incorrectRouteFinishOrderStatus ? PlaceStatus.CREATED : PlaceStatus.ACCEPTED,
                        testParams.incorrectRouteFinishOrderStatus
                                ? SortableStatus.AWAITING_DIRECT
                                : SortableStatus.ARRIVED_DIRECT,
                        cell.getId(),
                        null,
                        null
                ));
            }
        }
        if (testParams.createFactualRegistry) {
            Outbound outbound = testFactory.createOutbound(sortingCenter);
            Registry outboundRegistry = testFactory.bindRegistry(outbound.getExternalId(), RegistryType.FACTUAL);
            Registry inboundRegistry = testFactory.bindRegistry(inbound, outboundRegistry.getExternalId(),
                    RegistryType.PLANNED);


            if (testParams.scOrder != null) {
                testFactory.bindOrder(inboundRegistry, testParams.scOrder.getExternalId(),
                        testParams.scOrder.getExternalId(), null);
            }
        }
        return inbound;
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

    private void enableOpt(boolean value) {
        configurationService.mergeValue(ConfigurationProperties.OPT_GET_PARTNER_INBOUNDS_TRANSFER_ACT_ENABLED, value);
    }

}
