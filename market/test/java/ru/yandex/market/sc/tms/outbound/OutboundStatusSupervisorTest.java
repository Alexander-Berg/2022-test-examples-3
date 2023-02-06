package ru.yandex.market.sc.tms.outbound;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryService;
import ru.yandex.market.sc.core.domain.inbound.repository.BoundRegistryRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundRegistryOrderStatus;
import ru.yandex.market.sc.core.domain.inbound.repository.Registry;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryOrder;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryType;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundType;
import ru.yandex.market.sc.core.domain.outbound.repository.Outbound;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundRepository;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundStatus;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundStatusHistory;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundStatusHistoryRepository;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.route.repository.Route;
import ru.yandex.market.sc.core.domain.route.repository.RouteFinish;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.tms.domain.outbound.OutboundStatusSupervisor;
import ru.yandex.market.sc.tms.test.EmbeddedDbTmsTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.deliveryService;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTmsTest
public class OutboundStatusSupervisorTest {

    private static final int AUTO_CLOSE_MINUTES = 10;

    @Autowired
    private OutboundStatusSupervisor outboundStatusSupervisor;

    @Autowired
    private OutboundRepository outboundRepository;

    @Autowired
    private OutboundStatusHistoryRepository outboundStatusHistoryRepository;

    @Autowired
    private RegistryRepository registryRepository;

    @Autowired
    private BoundRegistryRepository registryOrderRepository;

    @Autowired
    private TestFactory testFactory;

    @MockBean
    private Clock clock;

    private SortingCenter sortingCenter = null;

    @BeforeEach
    void setUp() {
        testFactory.setupMockClock(clock);
        sortingCenter = testFactory.storedSortingCenter(2222);
        autoCloseOutboundAfterMinutes(AUTO_CLOSE_MINUTES);
    }

    @Test
    void testOutdatedOutboundCancelled() {
        Outbound outdated = testFactory.createOutbound(
                "id1",
                OutboundStatus.CREATED,
                OutboundType.DS_SC,
                Instant.parse("2017-01-03T07:00:05.006Z"),
                clock.instant(),
                "partnerId",
                null,
                null
        );

        outboundStatusSupervisor.processOutdatedOutbounds(Instant.now(clock));

        Outbound outdatedAfterTask = outboundRepository.findById(outdated.getId()).orElseThrow();
        assertThat(outdatedAfterTask.getStatus()).isEqualTo(OutboundStatus.CANCELLED_BY_SC);
    }

    @Test
    void testXdocOutboundNotCancelled() {
        Outbound xdoc = testFactory.createOutbound(
                "id1",
                OutboundStatus.CREATED,
                OutboundType.XDOC,
                Instant.parse("2017-01-03T07:00:05.006Z"),
                clock.instant(),
                "partnerId",
                null,
                null
        );

        outboundStatusSupervisor.processOutdatedOutbounds(Instant.now(clock));

        Outbound notOutdatedAfterTask = outboundRepository.findByIdOrThrow(xdoc.getId());
        assertThat(notOutdatedAfterTask).isEqualTo(xdoc);
    }

    @Test
    void testNotOutdated() {
        Outbound notOutdated = testFactory.createOutbound(
                "id2",
                OutboundStatus.CREATED,
                OutboundType.DS_SC,
                Instant.parse("2017-01-03T07:14:05.006Z"),
                clock.instant(),
                "partnerId",
                null,
                null
        );

        outboundStatusSupervisor.processOutdatedOutbounds(Instant.now(clock));

        Outbound notOutdatedAfterTask = outboundRepository.findById(notOutdated.getId()).orElseThrow();
        assertThat(notOutdatedAfterTask).isEqualTo(notOutdated);
    }


    @Test
    void registryNotCreatedForEmptyOutbound() {
        enableOutboundAutoClose();
        DeliveryService deliveryService = deliveryService();

        Outbound outdated = testFactory.createOutbound(
                "id1",
                OutboundStatus.CREATED,
                OutboundType.WH2WH,
                getTimeEligibleForAutoСlose(),
                clock.instant(),
                "partnerId",
                sortingCenter,
                testFactory.storedMovementCourier(1111L),
                deliveryService.getYandexId()
        );

        //method to test
        outboundStatusSupervisor.fixOutboundsByLastShippedOrder();


        Outbound outdatedAfterTask = outboundRepository.findById(outdated.getId()).orElseThrow();
        assertThat(outdatedAfterTask.getStatus()).isEqualTo(OutboundStatus.CREATED);

        List<Registry> registry = registryRepository.findAll();
        assertThat(registry).hasSize(0);

    }


    @Test
    void registryCreatedForOneStaleOrder() {
        long courierUid = 1111L;
        enableOutboundAutoClose();
        DeliveryService deliveryService = testFactory.storedDeliveryService();

        Outbound outbound = testFactory.createOutbound(
                "id1",
                OutboundStatus.CREATED,
                OutboundType.WH2WH,
                getTimeEligibleForAutoСlose(),
                clock.instant(),
                "partnerId",
                sortingCenter,
                testFactory.storedMovementCourier(courierUid),
                deliveryService.getYandexId()
        );

        Route route = testFactory.storedOutgoingCourierRoute(
                LocalDate.ofInstant(getTimeEligibleForAutoСlose(), ZoneId.of("UTC")), sortingCenter,
                testFactory.storedCourier(courierUid), testFactory.storedCell(sortingCenter)
        );


        OrderLike scOrder =
                testFactory.createOrder(order(sortingCenter).externalId("orderExternalId")
                        .deliveryService(deliveryService).build()).get();

        RouteFinish routeFinish = testFactory.storedEmptyRouteFinish(route, testFactory.storedUser(sortingCenter, 1231),
                getTimeEligibleForAutoСlose());

        storeRouteFinishesForOrder(routeFinish, scOrder);

        //method to test
        outboundStatusSupervisor.fixOutboundsByLastShippedOrder();


        Outbound modifiedInbound = outboundRepository.findById(outbound.getId()).orElseThrow();
        assertThat(modifiedInbound.getStatus()).isEqualTo(OutboundStatus.SHIPPED);

        Optional<OutboundStatusHistory> outboundStatusHistory = outboundStatusHistoryRepository
                .findAllByExternalIds(List.of(outbound.getExternalId())).stream()
                .filter(o -> o.getOutboundId().equals(modifiedInbound.getId())
                        && OutboundStatus.SHIPPED.equals(o.getStatus())).findFirst();
        assertThat(outboundStatusHistory.isPresent()).isTrue();


        List<Registry> registries = registryRepository.findAll();
        assertThat(registries).hasSize(1);
        Registry registry = registries.get(0);
        assertThat(registry.getType()).isEqualTo(RegistryType.FACTUAL);
        assertThat(registry.getOutbound()).isEqualTo(outbound);

        List<RegistryOrder> registryOrders = registryOrderRepository.findAll();
        assertThat(registryOrders).hasSize(1);

        RegistryOrder registryOrder = registryOrders.get(0);

        assertThat(registryOrder.getRegistryId()).isEqualTo(registry.getId());
        assertThat(registryOrder.getStatus()).isEqualTo(InboundRegistryOrderStatus.FIXED);
        assertThat(registryOrder.getExternalId()).isEqualTo(scOrder.getExternalId());
        assertThat(registryOrder.getPlaceId()).isEqualTo(scOrder.getExternalId());
    }

    @Test
    void registryNotCreatedWhenNoOutboundsOnSc() {
        long courierUid = 1111L;
        enableOutboundAutoClose();
        DeliveryService deliveryService = testFactory.storedDeliveryService();

        Route route = testFactory.storedOutgoingCourierRoute(
                LocalDate.ofInstant(getTimeEligibleForAutoСlose(), ZoneId.of("UTC")), sortingCenter,
                testFactory.storedCourier(courierUid), testFactory.storedCell(sortingCenter)
        );


        OrderLike scOrder =
                testFactory.createOrder(order(sortingCenter).externalId("orderExternalId").deliveryService(deliveryService).build()).get();

        RouteFinish routeFinish = testFactory.storedEmptyRouteFinish(route, testFactory.storedUser(sortingCenter, 1231),
                getTimeEligibleForAutoСlose());

        storeRouteFinishesForOrder(routeFinish, scOrder);

        //method to test
        outboundStatusSupervisor.fixOutboundsByLastShippedOrder();

        List<Registry> registries = registryRepository.findAll();
        assertThat(registries).hasSize(0);

        List<RegistryOrder> registryOrders = registryOrderRepository.findAll();
        assertThat(registryOrders).hasSize(0);
    }

    @Test
    void registryNotCreatedForOneFreshOrder() {
        long courierUid = 1111L;

        DeliveryService deliveryService = testFactory.storedDeliveryService();
        enableOutboundAutoClose();
        Outbound outbound = testFactory.createOutbound(
                "id1",
                OutboundStatus.CREATED,
                OutboundType.WH2WH,
                getTimeEligibleForAutoСlose(),
                clock.instant(),
                "partnerId",
                sortingCenter,
                testFactory.storedMovementCourier(courierUid),
                deliveryService.getYandexId()
        );

        Route route = testFactory.storedOutgoingCourierRoute(
                LocalDate.ofInstant(getTimeEligibleForAutoСlose(), ZoneId.systemDefault()), sortingCenter,
                testFactory.storedCourier(courierUid), testFactory.storedCell(sortingCenter)
        );

        OrderLike scOrder = testFactory.createOrder(
                order(sortingCenter).externalId("orderExternalId").deliveryService(deliveryService).build()
        ).get();

        RouteFinish routeFinish = testFactory.storedEmptyRouteFinish(route, testFactory.storedUser(sortingCenter, 1231),
                clock.instant());

        storeRouteFinishesForOrder(routeFinish, scOrder);

        //method to test
        outboundStatusSupervisor.fixOutboundsByLastShippedOrder();


        Outbound modifiedInbound = outboundRepository.findById(outbound.getId()).orElseThrow();
        assertThat(modifiedInbound.getStatus()).isEqualTo(OutboundStatus.SHIPPED);

        List<Registry> registries = registryRepository.findAll();
        assertThat(registries).hasSize(1);
        Registry registry = registries.get(0);
        assertThat(registry.getType()).isEqualTo(RegistryType.FACTUAL);
        assertThat(registry.getOutbound()).isEqualTo(outbound);

        List<RegistryOrder> registryOrders = registryOrderRepository.findAll();
        assertThat(registryOrders).hasSize(1);

        RegistryOrder registryOrder = registryOrders.get(0);

        assertThat(registryOrder.getRegistryId()).isEqualTo(registry.getId());
        assertThat(registryOrder.getStatus()).isEqualTo(InboundRegistryOrderStatus.FIXED);
        assertThat(registryOrder.getExternalId()).isEqualTo(scOrder.getExternalId());
        assertThat(registryOrder.getPlaceId()).isEqualTo(scOrder.getExternalId());


    }

    @Test
    void registryNotCreatedForScWithDisabledAutocloseProperty() {
        disableOutboundAutoClose();
        long courierUid = 1111L;

        DeliveryService deliveryService = testFactory.storedDeliveryService();

        Outbound outbound = testFactory.createOutbound(
                "id1",
                OutboundStatus.CREATED,
                OutboundType.WH2WH,
                getTimeEligibleForAutoСlose(),
                clock.instant(),
                "partnerId",
                sortingCenter,
                testFactory.storedMovementCourier(courierUid),
                deliveryService.getYandexId()
        );

        Route route = testFactory.storedOutgoingCourierRoute(
                LocalDate.ofInstant(getTimeEligibleForAutoСlose(), ZoneId.of("UTC")), sortingCenter,
                testFactory.storedCourier(courierUid), testFactory.storedCell(sortingCenter)
        );

        OrderLike scOrder = testFactory.createOrder(
                order(sortingCenter).externalId("orderExternalId").deliveryService(deliveryService).build()
        ).get();

        RouteFinish routeFinish = testFactory.storedEmptyRouteFinish(route, testFactory.storedUser(sortingCenter, 1231),
                getTimeEligibleForAutoСlose());

        storeRouteFinishesForOrder(routeFinish, scOrder);

        //method to test
        outboundStatusSupervisor.fixOutboundsByLastShippedOrder();


        Outbound modifiedInbound = outboundRepository.findById(outbound.getId()).orElseThrow();
        assertThat(modifiedInbound.getStatus()).isEqualTo(OutboundStatus.CREATED);

        List<Registry> registries = registryRepository.findAll();
        assertThat(registries).hasSize(0);

        List<RegistryOrder> registryOrders = registryOrderRepository.findAll();
        assertThat(registryOrders).hasSize(0);


    }

    @Test
    void registryNotCreatedForOneFreshAndOneStaleOrder() {
        long courierUid = 1111L;

        DeliveryService deliveryService = testFactory.storedDeliveryService();
        enableOutboundAutoClose();
        Outbound outbound = testFactory.createOutbound(
                "id1",
                OutboundStatus.CREATED,
                OutboundType.WH2WH,
                getTimeEligibleForAutoСlose(),
                clock.instant(),
                "partnerId",
                sortingCenter,
                testFactory.storedMovementCourier(courierUid),
                deliveryService.getYandexId()
        );

        Route route = testFactory.storedOutgoingCourierRoute(
                LocalDate.ofInstant(getTimeEligibleForAutoСlose(), ZoneId.systemDefault()), sortingCenter,
                testFactory.storedCourier(courierUid), testFactory.storedCell(sortingCenter)
        );

        OrderLike scOrder = testFactory.createOrder(
                order(sortingCenter).externalId("orderExternalId").deliveryService(deliveryService).build()
        ).get();

        OrderLike scOrder2 = testFactory.createOrder(
                order(sortingCenter).externalId("orderExternalId2").deliveryService(deliveryService).build()
        ).get();

        User user = testFactory.storedUser(sortingCenter, 1231);

        RouteFinish routeFinish = testFactory.storedEmptyRouteFinish(route, user, getTimeNotEligibleForAutoСlose());
        RouteFinish routeFinish2 = testFactory.storedEmptyRouteFinish(route, user, getTimeEligibleForAutoСlose());

        storeRouteFinishesForOrder(routeFinish, scOrder);
        storeRouteFinishesForOrder(routeFinish2, scOrder2);

        //method to test
        outboundStatusSupervisor.fixOutboundsByLastShippedOrder();


        Outbound modifiedInbound = outboundRepository.findById(outbound.getId()).orElseThrow();
        assertThat(modifiedInbound.getStatus()).isEqualTo(OutboundStatus.SHIPPED);

        List<Registry> registries = registryRepository.findAll();
        assertThat(registries).hasSize(1);
        Registry registry = registries.get(0);
        assertThat(registry.getType()).isEqualTo(RegistryType.FACTUAL);
        assertThat(registry.getOutbound()).isEqualTo(outbound);

        List<RegistryOrder> registryOrders = registryOrderRepository.findAll();
        assertThat(registryOrders).hasSize(2);

        RegistryOrder registryOrder = registryOrders.get(0);

        assertThat(registryOrder.getRegistryId()).isEqualTo(registry.getId());
        assertThat(registryOrder.getStatus()).isEqualTo(InboundRegistryOrderStatus.FIXED);
        assertThat(registryOrder.getExternalId()).isEqualTo(scOrder.getExternalId());
        assertThat(registryOrder.getPlaceId()).isEqualTo(scOrder.getExternalId());

        RegistryOrder registryOrder2 = registryOrders.get(1);

        assertThat(registryOrder2.getRegistryId()).isEqualTo(registry.getId());
        assertThat(registryOrder2.getStatus()).isEqualTo(InboundRegistryOrderStatus.FIXED);
        assertThat(registryOrder2.getExternalId()).isEqualTo(scOrder2.getExternalId());
        assertThat(registryOrder2.getPlaceId()).isEqualTo(scOrder2.getExternalId());
    }

    @Test
    void registryCreatedForTwoStaleOrders() {
        long courierUid = 1111L;
        enableOutboundAutoClose();
        DeliveryService deliveryService = testFactory.storedDeliveryService();

        Outbound outbound = testFactory.createOutbound(
                "id1",
                OutboundStatus.CREATED,
                OutboundType.WH2WH,
                getTimeEligibleForAutoСlose(),
                clock.instant(),
                "partnerId",
                sortingCenter,
                testFactory.storedMovementCourier(courierUid),
                deliveryService.getYandexId()
        );

        Route route = testFactory.storedOutgoingCourierRoute(
                LocalDate.ofInstant(getTimeEligibleForAutoСlose(), ZoneId.systemDefault()), sortingCenter,
                testFactory.storedCourier(courierUid), testFactory.storedCell(sortingCenter)
        );

        OrderLike scOrder1 = testFactory.createOrder(
                order(sortingCenter).externalId("orderExternalId1").deliveryService(deliveryService).build()
        ).get();
        OrderLike scOrder2 = testFactory.createOrder(
                order(sortingCenter).externalId("orderExternalId2").deliveryService(deliveryService).build()
        ).get();

        RouteFinish routeFinish = testFactory.storedEmptyRouteFinish(route, testFactory.storedUser(sortingCenter, 1231),
                getTimeEligibleForAutoСlose());

        storeRouteFinishesForOrder(routeFinish, scOrder1);
        storeRouteFinishesForOrder(routeFinish, scOrder2);

        //method to test
        outboundStatusSupervisor.fixOutboundsByLastShippedOrder();


        Outbound modifiedOutbound = outboundRepository.findById(outbound.getId()).orElseThrow();
        assertThat(modifiedOutbound.getStatus()).isEqualTo(OutboundStatus.SHIPPED);

        Optional<OutboundStatusHistory> outboundStatusHistory = outboundStatusHistoryRepository
                .findAllByExternalIds(List.of(outbound.getExternalId())).stream()
                .filter(o -> o.getOutboundId().equals(modifiedOutbound.getId())
                        && OutboundStatus.SHIPPED.equals(o.getStatus())).findFirst();
        assertThat(outboundStatusHistory.isPresent()).isTrue();

        List<Registry> registries = registryRepository.findAll();
        assertThat(registries).hasSize(1);
        Registry registry = registries.get(0);
        assertThat(registry.getType()).isEqualTo(RegistryType.FACTUAL);
        assertThat(registry.getOutbound()).isEqualTo(outbound);

        List<RegistryOrder> registryOrders = registryOrderRepository.findAll();
        assertThat(registryOrders).hasSize(2);


        Optional<RegistryOrder> first =
                registryOrders.stream().filter(ro -> ro.getExternalId().equals(scOrder1.getExternalId()))
                        .findFirst();

        assert (first.isPresent());

        RegistryOrder registryOrder = first.get();

        assertThat(registryOrder.getRegistryId()).isEqualTo(registry.getId());
        assertThat(registryOrder.getStatus()).isEqualTo(InboundRegistryOrderStatus.FIXED);
        assertThat(registryOrder.getExternalId()).isEqualTo(scOrder1.getExternalId());
        assertThat(registryOrder.getPlaceId()).isEqualTo(scOrder1.getExternalId());


        Optional<RegistryOrder> second =
                registryOrders.stream().filter(ro -> ro.getExternalId().equals(scOrder2.getExternalId()))
                        .findFirst();

        assert (second.isPresent());

        registryOrder = second.get();

        assertThat(registryOrder.getRegistryId()).isEqualTo(registry.getId());
        assertThat(registryOrder.getStatus()).isEqualTo(InboundRegistryOrderStatus.FIXED);
        assertThat(registryOrder.getExternalId()).isEqualTo(scOrder2.getExternalId());
        assertThat(registryOrder.getPlaceId()).isEqualTo(scOrder2.getExternalId());
    }

    @Test
    void registryCreatedForTwoStaleRoutes() {
        enableOutboundAutoClose();
        long courierUid = 1111L;

        DeliveryService deliveryService = testFactory.storedDeliveryService();

        Outbound outbound = testFactory.createOutbound(
                "id1",
                OutboundStatus.CREATED,
                OutboundType.WH2WH,
                getTimeEligibleForAutoСlose().minus(1, ChronoUnit.DAYS),
                clock.instant(),
                "partnerId",
                sortingCenter,
                testFactory.storedMovementCourier(courierUid),
                deliveryService.getYandexId()
        );

        Courier courier = testFactory.storedCourier(courierUid);
        Cell cell = testFactory.storedCell(sortingCenter);

        Route route1 = testFactory.storedOutgoingCourierRoute(
                LocalDate.ofInstant(getTimeEligibleForAutoСlose(), ZoneId.of("UTC")), sortingCenter,
                courier, cell
        );
        Route route2 = testFactory.storedOutgoingCourierRoute(
                LocalDate.ofInstant(getTimeEligibleForAutoСlose().minus(1, ChronoUnit.DAYS), ZoneId.of(
                        "UTC")), sortingCenter,
                courier, cell
        );

        OrderLike scOrder1 = testFactory.createOrder(
                order(sortingCenter).externalId("orderExternalId1").deliveryService(deliveryService).build()
        ).get();
        OrderLike scOrder2 = testFactory.createOrder(
                order(sortingCenter).externalId("orderExternalId2").deliveryService(deliveryService).build()
        ).get();

        User user = testFactory.storedUser(sortingCenter, 1231);
        RouteFinish routeFinish1 = testFactory.storedEmptyRouteFinish(route1, user,
                getTimeEligibleForAutoСlose());
        RouteFinish routeFinish2 = testFactory.storedEmptyRouteFinish(route2, user,
                getTimeEligibleForAutoСlose());

        storeRouteFinishesForOrder(routeFinish1, scOrder1);
        storeRouteFinishesForOrder(routeFinish2, scOrder2);

        //method to test
        outboundStatusSupervisor.fixOutboundsByLastShippedOrder();


        Outbound modifiedInbound = outboundRepository.findById(outbound.getId()).orElseThrow();
        assertThat(modifiedInbound.getStatus()).isEqualTo(OutboundStatus.SHIPPED);

        Optional<OutboundStatusHistory> outboundStatusHistory = outboundStatusHistoryRepository
                .findAllByExternalIds(List.of(outbound.getExternalId())).stream()
                .filter(o -> o.getOutboundId().equals(modifiedInbound.getId())
                        && OutboundStatus.SHIPPED.equals(o.getStatus())).findFirst();
        assertThat(outboundStatusHistory.isPresent()).isTrue();


        List<Registry> registries = registryRepository.findAll();
        assertThat(registries).hasSize(1);
        Registry registry = registries.get(0);
        assertThat(registry.getType()).isEqualTo(RegistryType.FACTUAL);
        assertThat(registry.getOutbound()).isEqualTo(outbound);

        List<RegistryOrder> registryOrders = registryOrderRepository.findAll();
        assertThat(registryOrders).hasSize(2);


        Optional<RegistryOrder> first =
                registryOrders.stream().filter(ro -> ro.getExternalId().equals(scOrder1.getExternalId()))
                        .findFirst();

        assert (first.isPresent());

        RegistryOrder registryOrder = first.get();

        assertThat(registryOrder.getRegistryId()).isEqualTo(registry.getId());
        assertThat(registryOrder.getStatus()).isEqualTo(InboundRegistryOrderStatus.FIXED);
        assertThat(registryOrder.getExternalId()).isEqualTo(scOrder1.getExternalId());
        assertThat(registryOrder.getPlaceId()).isEqualTo(scOrder1.getExternalId());


        Optional<RegistryOrder> second =
                registryOrders.stream().filter(ro -> ro.getExternalId().equals(scOrder2.getExternalId()))
                        .findFirst();

        assert (second.isPresent());

        registryOrder = second.get();

        assertThat(registryOrder.getRegistryId()).isEqualTo(registry.getId());
        assertThat(registryOrder.getStatus()).isEqualTo(InboundRegistryOrderStatus.FIXED);
        assertThat(registryOrder.getExternalId()).isEqualTo(scOrder2.getExternalId());
        assertThat(registryOrder.getPlaceId()).isEqualTo(scOrder2.getExternalId());
    }

    @Test
    void registryNotCreatedWhenDatesAreDifferent() {
        long courierUid = 1111L;
        enableOutboundAutoClose();
        DeliveryService deliveryService = testFactory.storedDeliveryService();

        Outbound outbound = testFactory.createOutbound(
                "id1",
                OutboundStatus.CREATED,
                OutboundType.WH2WH,
                getTimeEligibleForAutoСlose(),
                clock.instant(),
                "partnerId",
                sortingCenter,
                testFactory.storedMovementCourier(courierUid),
                deliveryService.getYandexId()
        );

        Route route = testFactory.storedOutgoingCourierRoute(
                LocalDate.ofInstant(getTimeEligibleForAutoСlose().minus(1, ChronoUnit.DAYS), ZoneId.of(
                        "UTC")), sortingCenter,
                testFactory.storedCourier(courierUid), testFactory.storedCell(sortingCenter)
        );

        OrderLike scOrder = testFactory.createOrder(
                order(sortingCenter).externalId("orderExternalId").deliveryService(deliveryService).build()
        ).get();

        RouteFinish routeFinish = testFactory.storedEmptyRouteFinish(route, testFactory.storedUser(sortingCenter, 1231),
                getTimeEligibleForAutoСlose());

        storeRouteFinishesForOrder(routeFinish, scOrder);

        //method to test
        outboundStatusSupervisor.fixOutboundsByLastShippedOrder();


        Outbound modifiedInbound = outboundRepository.findById(outbound.getId()).orElseThrow();
        assertThat(modifiedInbound.getStatus()).isEqualTo(OutboundStatus.CREATED);

        List<Registry> registries = registryRepository.findAll();
        assertThat(registries).hasSize(0);

        List<RegistryOrder> registryOrders = registryOrderRepository.findAll();
        assertThat(registryOrders).hasSize(0);
    }

    private void storeRouteFinishesForOrder(RouteFinish routeFinish, OrderLike scOrder) {
        List<Place> places = testFactory.orderPlaces(scOrder.getId());
        for (Place place : places) {
            testFactory.storedRouteFinishPlace(
                    routeFinish, place.getId(), place.getMainPartnerCode(), place.getOrderId(),
                    PlaceStatus.SHIPPED, SortableStatus.SHIPPED_DIRECT, null, null, null
            );
        }
    }

    private void enableOutboundAutoClose() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.AUTO_CLOSE_OUTBOUND_ENABLED,
                "true");
    }

    private void disableOutboundAutoClose() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.AUTO_CLOSE_OUTBOUND_ENABLED,
                "false");
    }

    private void autoCloseOutboundAfterMinutes(Integer minutes) {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.AUTO_CLOSE_OUTBOUND_AFTER_X_MINUTES,
                minutes.toString());
    }

    private Instant getTimeEligibleForAutoСlose() {
        return clock.instant().minus(AUTO_CLOSE_MINUTES + 1, ChronoUnit.MINUTES);
    }

    private Instant getTimeNotEligibleForAutoСlose() {
        return clock.instant().minus(AUTO_CLOSE_MINUTES - 1, ChronoUnit.MINUTES);
    }

    //empty commit to push release server to release hotfix
}
