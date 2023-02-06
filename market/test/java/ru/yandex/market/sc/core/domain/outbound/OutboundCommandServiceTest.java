package ru.yandex.market.sc.core.domain.outbound;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.bolts.function.Function;
import ru.yandex.market.sc.core.dbqueue.ScQueueType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.inbound.model.InboundRegistryDto;
import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.model.RegistryUnitType;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.inbound.repository.Registry;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistrySortable;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistrySortableRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryType;
import ru.yandex.market.sc.core.domain.location.LocationCreateRequest;
import ru.yandex.market.sc.core.domain.movement_courier.model.MovementCourierRequest;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.outbound.model.CreateOutboundPlannedRegistryRequest;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundCreateRequest;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundInfo;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundType;
import ru.yandex.market.sc.core.domain.outbound.repository.Outbound;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundRepository;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundStatus;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundStatusHistoryRepository;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.route.model.RouteDocumentType;
import ru.yandex.market.sc.core.domain.route.model.RouteFinishOrderRequest;
import ru.yandex.market.sc.core.domain.route.model.RouteFinishPlaceRequest;
import ru.yandex.market.sc.core.domain.route.model.RouteType;
import ru.yandex.market.sc.core.domain.route.repository.Route;
import ru.yandex.market.sc.core.domain.route.repository.RouteRepository;
import ru.yandex.market.sc.core.domain.route_so.RouteSoCommandService;
import ru.yandex.market.sc.core.domain.route_so.model.RouteDestinationType;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSo;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSoRepository;
import ru.yandex.market.sc.core.domain.sortable.SortableQueryService;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.model.enums.DirectFlowType;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sortable.repository.SortableRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterPropertySource;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.SortableFlowSwitcherExtension;
import ru.yandex.market.sc.core.test.SortableTestFactory;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.ScDateUtils;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;
import ru.yandex.market.tpl.common.db.configuration.ConfigurationService;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@EmbeddedDbTest
class OutboundCommandServiceTest {

    @Autowired
    private SortableTestFactory sortableTestFactory;
    @Autowired
    private SortableQueryService sortableQueryService;
    @Autowired
    private TestFactory testFactory;
    @Autowired
    private OutboundCommandService outboundCommandService;
    @Autowired
    private OutboundStatusHistoryRepository statusHistoryRepository;
    @Autowired
    private OutboundQueryService queryService;
    @Autowired
    private RegistryRepository registryRepository;
    @Autowired
    private RegistrySortableRepository registrySortableRepository;
    @Autowired
    private SortableRepository sortableRepository;
    @Autowired
    private OutboundRepository outboundRepository;
    @Autowired
    RouteRepository routeRepository;
    @Autowired
    RouteSoRepository routeSoRepository;
    @Autowired
    RouteSoCommandService routeSoCommandService;
    @Autowired
    DbQueueTestUtil dbQueueTestUtil;
    @Autowired
    private Clock clock;
    @Autowired
    private XDocFlow flow;
    @Autowired
    ConfigurationService configurationService;

    @SpyBean
    SortingCenterPropertySource sortingCenterPropertySource;

    private SortingCenter sortingCenter;
    private Warehouse warehouse;
    private User user;
    private LocationCreateRequest locationCreateRequest;
    private final String externalId = "sfsdefdge4rfdv";
    private final OutboundType type = OutboundType.ORDERS_RETURN;
    private final Instant fromTime = Instant.parse("2021-06-01T12:00:00Z");
    private final Instant toTime = Instant.parse("2021-06-01T15:00:00Z");
    private final MovementCourierRequest courierRequest = MovementCourierRequest.builder()
            .externalId("fffdjhwkh3j4jgkbc")
            .name("Courier name")
            .legalName("Courier legal name")
            .carNumber("О868АС198")
            .uid(212_85_06L)
            .phone("phone2345")
            .build();
    private final String comment = "Please, do your best";

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        warehouse = testFactory.storedWarehouse();
        user = testFactory.storedUser(sortingCenter, 123);
        locationCreateRequest = TestFactory.locationCreateRequest();
    }

    @Test
    void createTest() {
        createOutbound();

        var outbound = queryService.getOutboundInfoBy(externalId).getOutbound();
        assertEquals(externalId, outbound.getExternalId());
        assertEquals(type, outbound.getType());
        assertEquals(fromTime, outbound.getFromTime());
        assertEquals(toTime, outbound.getToTime());
        assertEquals(OutboundStatus.CREATED, outbound.getStatus());
        assertEquals(sortingCenter.getId(), outbound.getSortingCenter().getId());
        assertEquals(comment, outbound.getComment());
    }

    @Test
    void historyTest() {
        createOutbound();

        var histories = statusHistoryRepository.findAllByExternalIds(List.of(externalId));
        assertEquals(1, histories.size());
        var history = histories.get(0);
        assertEquals(externalId, history.getOutboundExternalId());
        assertEquals(OutboundStatus.CREATED, history.getStatus());
    }

    @Test
    void registryTest() {
        createOutbound();
        Outbound outbound = outboundRepository.findByExternalId(externalId).orElseThrow();

        String registryExternalId = "csdsfrr3fsfda";
        RegistryType registryType = RegistryType.FACTUAL_DELIVERED_ORDERS_RETURN;
        String documentId = RouteDocumentType.ONLY_CLIENT_RETURNS.getDocumentId(104877811, 0);

        registryRepository.save(Registry.outboundRegistry(registryExternalId, outbound, registryType, documentId));

        OutboundInfo outboundInfo = queryService.getOutboundInfoBy(externalId);
        assertEquals(1, outboundInfo.getOrderRegistries().size());
        var registryDto = outboundInfo.getOrderRegistries().get(0);
        assertEquals(registryExternalId, registryDto.getExternalId());
        assertEquals(documentId, registryDto.getDocumentId());
    }

    @Test
    @DisplayName("Успешное создание планового реестра")
    void plannedRegistryTest() {
        Inbound firstInbound = createdInbound("101", InboundType.XDOC_TRANSIT);
        testFactory.linkSortableToInbound(firstInbound, "XDOC-11", SortableType.XDOC_PALLET, user);
        testFactory.linkSortableToInbound(firstInbound, "XDOC-12", SortableType.XDOC_PALLET, user);

        Inbound secondInbound = createdInbound("102", InboundType.XDOC_TRANSIT);
        testFactory.linkSortableToInbound(secondInbound, "XDOC-21", SortableType.XDOC_PALLET, user);
        testFactory.finishInbound(firstInbound);
        testFactory.finishInbound(secondInbound);

        var outbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("1001")
                .type(OutboundType.XDOC)
                .fromTime(fromTime)
                .toTime(toTime)
                .sortingCenter(sortingCenter)
                .partnerToExternalId(warehouse.getPartnerId())
                .logisticPointToExternalId(warehouse.getYandexId())
                .build()
        );

        outboundCommandService.putPlannedRegistry(CreateOutboundPlannedRegistryRequest.builder()
                .sortingCenter(sortingCenter)
                .registryExternalId("12324")
                .outboundExternalId("1001")
                .palletExternalIds(List.of("XDOC-11", "XDOC-12", "XDOC-21"))
                .boxExternalIds(List.of())
                .build());
        var registries = registryRepository.findAllByOutboundId(outbound.getId());
        assertThat(registries)
                .hasSize(1)
                .allMatch(registry -> registry.getType() == RegistryType.PLANNED);
        assertThat(registrySortableRepository.findAllByRegistryIn(new HashSet<>(registries)))
                .hasSize(3)
                .allMatch(registrySortable -> registrySortable.getUnitType() == RegistryUnitType.PALLET)
                .matches(registrySortables -> StreamEx.of(registrySortables)
                        .map(RegistrySortable::getSortableExternalId)
                        .toSet()
                        .containsAll(List.of("XDOC-11", "XDOC-12", "XDOC-21"))
                );
        assertThat(sortableRepository.findAll())
                .allMatch(sortable -> Objects.equals(sortable.getOutbound(), outbound))
                .allMatch(sortable -> sortable.getOutRoute() != null);
    }

    @Test
    @DisplayName("Успешное обновление планового реестра")
    void plannedRegistryUpdateTest() {
        Inbound firstInbound = createdInbound("101", InboundType.XDOC_TRANSIT);
        testFactory.linkSortableToInbound(firstInbound, "XDOC-11", SortableType.XDOC_PALLET, user);
        testFactory.linkSortableToInbound(firstInbound, "XDOC-12", SortableType.XDOC_PALLET, user);

        Inbound secondInbound = createdInbound("102", InboundType.XDOC_TRANSIT);
        testFactory.linkSortableToInbound(secondInbound, "XDOC-21", SortableType.XDOC_PALLET, user);
        testFactory.finishInbound(firstInbound);
        testFactory.finishInbound(secondInbound);

        var outbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("1001")
                .type(OutboundType.XDOC)
                .fromTime(fromTime)
                .toTime(toTime)
                .sortingCenter(sortingCenter)
                .partnerToExternalId(warehouse.getPartnerId())
                .logisticPointToExternalId(warehouse.getYandexId())
                .build()
        );

        outboundCommandService.putPlannedRegistry(CreateOutboundPlannedRegistryRequest.builder()
                .sortingCenter(sortingCenter)
                .registryExternalId("12324")
                .outboundExternalId("1001")
                .palletExternalIds(List.of("XDOC-11"))
                .boxExternalIds(List.of())
                .build());
        assertThat(sortableQueryService.findAllHavingAllBarcodes(sortingCenter, List.of("XDOC-11")))
                .allMatch(sortable -> sortable.getOutbound() != null)
                .allMatch(sortable -> sortable.getOutRoute() != null);
        outboundCommandService.putPlannedRegistry(CreateOutboundPlannedRegistryRequest.builder()
                .sortingCenter(sortingCenter)
                .registryExternalId("12324")
                .outboundExternalId("1001")
                .palletExternalIds(List.of("XDOC-12", "XDOC-21"))
                .boxExternalIds(List.of())
                .build());
        var registries = registryRepository.findAllByOutboundId(outbound.getId());
        assertThat(registries)
                .hasSize(1)
                .allMatch(registry -> registry.getType() == RegistryType.PLANNED);
        assertThat(registrySortableRepository.findAllByRegistryIn(new HashSet<>(registries)))
                .hasSize(2)
                .allMatch(registrySortable -> registrySortable.getUnitType() == RegistryUnitType.PALLET)
                .matches(registrySortables -> StreamEx.of(registrySortables)
                        .map(RegistrySortable::getSortableExternalId)
                        .toSet()
                        .containsAll(List.of("XDOC-12", "XDOC-21"))
                );
        assertThat(sortableQueryService.findAllHavingAllBarcodes(sortingCenter, List.of("XDOC-11")))
                .allMatch(sortable -> sortable.getOutbound() == null)
                .allMatch(sortable -> sortable.getOutRoute() == null);
        assertThat(sortableQueryService.findAllHavingAllBarcodes(sortingCenter, List.of("XDOC-12", "XDOC-21")))
                .allMatch(sortable -> Objects.equals(sortable.getOutbound(), outbound))
                .allMatch(sortable -> sortable.getOutRoute() != null);
    }

    @Test
    void putPlannedRegistryByOutboundId() {
        flow.createInbound("in-1")
                .linkPallets("XDOC-1")
                .fixInbound()
                .createInbound("in-2")
                .linkPallets("XDOC-2")
                .fixInbound()
                .createOutbound("out-1");

        outboundCommandService.putPlannedRegistry(CreateOutboundPlannedRegistryRequest.builder()
                .outboundExternalId("out-1")
                .registryExternalId("reg-1")
                .palletExternalIds(List.of("XDOC-1"))
                .sortingCenter(flow.getSortingCenter())
                .boxExternalIds(List.of())
                .build());

        assertThat(registrySortableRepository.findAllByOutboundExternalId("out-1"))
                .hasSize(1)
                .anyMatch(registrySortable -> registrySortable.getSortableExternalId().equals("XDOC-1"));

        outboundCommandService.putPlannedRegistry(CreateOutboundPlannedRegistryRequest.builder()
                .outboundExternalId("out-1")
                .registryExternalId("reg-1")
                .palletExternalIds(List.of("XDOC-1", "XDOC-2"))
                .sortingCenter(flow.getSortingCenter())
                .boxExternalIds(List.of())
                .build());

        assertThat(registrySortableRepository.findAllByOutboundExternalId("out-1"))
                .hasSize(2)
                .anyMatch(registrySortable -> registrySortable.getSortableExternalId().equals("XDOC-1"))
                .anyMatch(registrySortable -> registrySortable.getSortableExternalId().equals("XDOC-2"));
    }

    @Test
    void shipReturningOrdersClientReturnsTest() {
        testRegistryTypeAndId(RouteDocumentType.ONLY_CLIENT_RETURNS, RegistryType.FACTUAL_DELIVERED_ORDERS_RETURN,
                route -> RouteDocumentType.ONLY_CLIENT_RETURNS.getDocumentId(testFactory.getRouteIdForSortableFlow(route), 0));
    }

    @Test
    void shipReturningOrdersNormalTest() {
        testRegistryTypeAndId(RouteDocumentType.NORMAL, RegistryType.FACTUAL_UNDELIVERED_ORDERS_RETURN,
                route -> RouteDocumentType.NORMAL.getDocumentId(testFactory.getRouteIdForSortableFlow(route), 0));
    }

    @Test
    void shipReturningOrdersDamagedOrdersNotSupportedTest() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED, "false");
        testRegistryTypeAndId(RouteDocumentType.ONLY_DAMAGED, RegistryType.FACTUAL_UNDELIVERED_ORDERS_RETURN,
                route -> RouteDocumentType.NORMAL.getDocumentId(testFactory.getRouteIdForSortableFlow(route), 0));
    }

    @Test
    void shipReturningOrdersDamagedOrdersSupportedTest() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED, "true");
        testRegistryTypeAndId(RouteDocumentType.ONLY_DAMAGED, RegistryType.FACTUAL_UNDELIVERED_ORDERS_RETURN,
                route -> RouteDocumentType.ONLY_DAMAGED.getDocumentId(testFactory.getRouteIdForSortableFlow(route), 0));
    }

    private void testRegistryTypeAndId(RouteDocumentType requestedDocumentType,
                                       RegistryType expectedRegistryType,
                                       Function<Route, String> expectedRegistryId) {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.RETURN_OUTBOUND_ENABLED, "true");

        Route route = new Route(
                sortingCenter, RouteType.OUTGOING_WAREHOUSE,
                LocalDate.ofInstant(toTime, clock.getZone()), OffsetTime.ofInstant(toTime, clock.getZone()),
                null, warehouse, null, null
        );
        route = routeRepository.save(route);
        RouteSo routeSo = null;
        boolean writeRouteSo = SortableFlowSwitcherExtension.useNewRouteSoStage1_2();
        boolean sortWithRouteSo = SortableFlowSwitcherExtension.useNewRouteSoStage2();
        if (writeRouteSo) {
            RouteSo routeSoBlueprint = new RouteSo(
                    sortingCenter,
                    ru.yandex.market.sc.core.domain.route_so.model.RouteType.OUT_RETURN,
                    warehouse.getId(),
                    RouteDestinationType.WAREHOUSE,
                    ScDateUtils.beginningOfDay(toTime),ScDateUtils.endOfDay(toTime),
                    null
            );

            routeSo = routeSoRepository.save(routeSoBlueprint);
        }


        outboundCommandService.put(OutboundCreateRequest.builder()
                .externalId(externalId)
                .type(type)
                .fromTime(fromTime)
                .toTime(toTime)
                .courierRequest(courierRequest)
                .locationCreateRequest(locationCreateRequest)
                .comment(comment)
                .sortingCenter(sortingCenter)
                .type(OutboundType.ORDERS_RETURN)
                .logisticPointToExternalId(Objects.requireNonNull(route.allowNextRead().getWarehouseTo()).getYandexId())
                .build());

        Cell cell = testFactory.storedCell(sortingCenter, "1", CellType.RETURN, warehouse.getYandexId());
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);

        outboundCommandService.shipReturningOrders(
                sortWithRouteSo ? routeSo : route,
                List.of(new RouteFinishOrderRequest(155, "3", ScOrderFFStatus.ORDER_CANCELLED_FF, cell.getId(),
                        requestedDocumentType)),
                List.of(new RouteFinishPlaceRequest(
                233, 155, "4", PlaceStatus.ACCEPTED,
                SortableStatus.ACCEPTED_RETURN,
                cell.getId(), null, null, null)));

        OutboundInfo outboundInfo = queryService.getOutboundInfoBy(externalId);
        // шипнется потом dbqueue таской
        assertEquals(OutboundStatus.CREATED, outboundInfo.getOutbound().getStatus());
        assertEquals(1, outboundInfo.getOrderRegistries().size());

        final RegistryType outboundRegistryType = outboundInfo.getRegistries().get(0).getType();
        assertThat(outboundRegistryType).isEqualTo(expectedRegistryType);

        InboundRegistryDto registryDto = outboundInfo.getOrderRegistries().get(0);
        assertEquals(expectedRegistryId.apply(route), registryDto.getDocumentId());
        assertEquals(1, Objects.requireNonNull(registryDto.getOrders()).size());
        assertEquals(expectedRegistryType, registryDto.getType());
    }

    @Test
        // проверка фикса MARKETTPLSC-3548 - должны уметь подмерживать реестры при повторном шипе лотов
    void multipleShipReturningOrdersTest() {
        when(sortingCenterPropertySource.supportsReturnOutbound(anyLong())).thenReturn(true);
        Route route = new Route(
                sortingCenter, RouteType.OUTGOING_WAREHOUSE,
                LocalDate.ofInstant(toTime, clock.getZone()), OffsetTime.ofInstant(toTime, clock.getZone()),
                null, warehouse, null, null
        );
        Route storedRoute = routeRepository.save(route);
        RouteSo routeSo = null;
        if (SortableFlowSwitcherExtension.useNewRouteSoStage1_2()) {
            RouteSo routeSoBlueprint = new RouteSo(
                    sortingCenter,
                    ru.yandex.market.sc.core.domain.route_so.model.RouteType.OUT_RETURN,
                    warehouse.getId(),
                    RouteDestinationType.WAREHOUSE,
                    ScDateUtils.beginningOfDay(toTime),ScDateUtils.endOfDay(toTime),
                    null
            );

            routeSo = routeSoRepository.save(routeSoBlueprint);
        }


        outboundCommandService.put(OutboundCreateRequest.builder()
                .externalId(externalId)
                .type(type)
                .fromTime(fromTime)
                .toTime(toTime)
                .courierRequest(courierRequest)
                .locationCreateRequest(locationCreateRequest)
                .comment(comment)
                .sortingCenter(sortingCenter)
                .type(OutboundType.ORDERS_RETURN)
                .logisticPointToExternalId(route.allowNextRead().getWarehouseTo().getYandexId())
                .build());

        Cell cell = testFactory.storedCell(sortingCenter, "1", CellType.RETURN, warehouse.getYandexId());
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
        var sortWithRouteSo = SortableFlowSwitcherExtension.useNewRouteSoStage2();
        // Первый шип
        outboundCommandService.shipReturningOrders(
                sortWithRouteSo ? routeSo : route,
                List.of(generateRouteFinishOrderRequest(100, cell.getId(), RouteDocumentType.NORMAL)),
                List.of(generateRouteFinishPlaceRequest(100, cell.getId())));

        OutboundInfo outboundInfo = queryService.getOutboundInfoBy(externalId);
        // шипнется потом dbqueue таской
        assertThat(outboundInfo.getOutbound().getStatus()).isEqualTo(OutboundStatus.CREATED);
        Map<RegistryType, InboundRegistryDto> registries = outboundInfo.getOrderRegistries().stream()
                .collect(Collectors.toMap(InboundRegistryDto::getType, registry -> registry));
        assertThat(registries).hasSize(1);
        assertThat(registries.get(RegistryType.FACTUAL_UNDELIVERED_ORDERS_RETURN).getOrders()).hasSize(1);

        // Второй шип
        outboundCommandService.shipReturningOrders(
                sortWithRouteSo ? routeSo : route,
                List.of(
                        generateRouteFinishOrderRequest(200, cell.getId(), RouteDocumentType.NORMAL),
                        generateRouteFinishOrderRequest(201, cell.getId(), RouteDocumentType.ONLY_CLIENT_RETURNS)
                ),
                List.of(
                        generateRouteFinishPlaceRequest(200, cell.getId()),
                        generateRouteFinishPlaceRequest(201, cell.getId())
                ));

        outboundInfo = queryService.getOutboundInfoBy(externalId);
        // шипнется потом dbqueue таской
        assertThat(outboundInfo.getOutbound().getStatus()).isEqualTo(OutboundStatus.CREATED);
        registries = outboundInfo.getOrderRegistries().stream()
                .collect(Collectors.toMap(InboundRegistryDto::getType, registry -> registry));
        assertThat(registries).hasSize(2);
        assertThat(registries.get(RegistryType.FACTUAL_UNDELIVERED_ORDERS_RETURN).getOrders()).hasSize(2);
        assertThat(registries.get(RegistryType.FACTUAL_DELIVERED_ORDERS_RETURN).getOrders()).hasSize(1);

        // Третий шип
        outboundCommandService.shipReturningOrders(
                sortWithRouteSo ? routeSo : route,
                List.of(generateRouteFinishOrderRequest(300, cell.getId(), RouteDocumentType.ONLY_CLIENT_RETURNS)),
                List.of(generateRouteFinishPlaceRequest(300, cell.getId())));

        outboundInfo = queryService.getOutboundInfoBy(externalId);
        // шипнется потом dbqueue таской
        assertThat(outboundInfo.getOutbound().getStatus()).isEqualTo(OutboundStatus.CREATED);
        registries = outboundInfo.getOrderRegistries().stream()
                .collect(Collectors.toMap(InboundRegistryDto::getType, registry -> registry));
        assertThat(registries).hasSize(2);
        assertThat(registries.get(RegistryType.FACTUAL_UNDELIVERED_ORDERS_RETURN).getOrders()).hasSize(2);
        assertThat(registries.get(RegistryType.FACTUAL_DELIVERED_ORDERS_RETURN).getOrders()).hasSize(2);
    }

    private RouteFinishOrderRequest generateRouteFinishOrderRequest(long orderId, long cellId,
                                                                    RouteDocumentType routeDocumentType) {
        return new RouteFinishOrderRequest(orderId, String.valueOf(orderId),
                ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF, cellId, routeDocumentType);
    }

    private RouteFinishPlaceRequest generateRouteFinishPlaceRequest(long orderId, long cellId) {
        return new RouteFinishPlaceRequest(
                orderId + 1000,
                orderId,
                orderId + "-1",
                PlaceStatus.SORTED,
                SortableStatus.SORTED_DIRECT,
                cellId,
                null,
                null,
                null);
    }

    @Test
    void cancelEmptyOutboundWithRegistries() {
        createOutbound(OutboundType.XDOC);
        Inbound inbound = createdInbound("IN-11", InboundType.XDOC_TRANSIT);
        Sortable sortable = sortableTestFactory
                .storeSortable(sortingCenter, SortableType.XDOC_PALLET, DirectFlowType.TRANSIT, "XDOC-1111",
                        inbound, null).get();

        sortableTestFactory.createOutboundRegistry(SortableTestFactory.CreateOutboundRegistryParams.builder()
                .sortables(List.of(sortable))
                .outboundExternalId(externalId)
                .sortingCenter(sortingCenter)
                .registryExternalId("REG111111")
                .build());

        outboundCommandService.customManualShip(externalId, null);
        Outbound outbound = outboundRepository.findByExternalId(externalId).orElseThrow();
        Sortable sortableAfterCancel = sortableRepository.findByIdOrThrow(sortable.getId());
        assertThat(outbound.getStatus()).isEqualTo(OutboundStatus.SHIPPED);
        assertThat(sortableAfterCancel.getStatus()).isNotEqualTo(SortableStatus.SHIPPED_DIRECT);
        assertDoesNotThrow(() ->
                registryRepository.findAll().stream()
                        .filter(registry -> registry.getType() == RegistryType.FACTUAL
                                && registry.getOutbound().getExternalId().equals(externalId))
                        .findFirst().orElseThrow()
        );
    }

    @Test
    void cancelOutbound() {
        createOutbound(OutboundType.XDOC);
        outboundCommandService.customManualShip(externalId, null);
        Outbound outbound = outboundRepository.findByExternalId(externalId).orElseThrow();
        assertThat(outbound.getStatus()).isEqualTo(OutboundStatus.SHIPPED);
        assertDoesNotThrow(() ->
                registryRepository.findAll().stream()
                        .filter(registry -> registry.getType() == RegistryType.FACTUAL
                                && registry.getOutbound().getExternalId().equals(externalId))
                        .findFirst().orElseThrow()
        );
    }

    private Inbound createdInbound(String externalId, InboundType inboundType) {
        var params = TestFactory.CreateInboundParams.builder()
                .inboundExternalId(externalId)
                .inboundType(inboundType)
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId("warehouse-from-id")
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .registryMap(Map.of())
                .confirmed(true)
                .build();
        return testFactory.createInbound(params);
    }

    private void createOutbound() {
        createOutbound(type);
    }

    private void createOutbound(OutboundType outboundType) {
        outboundCommandService.put(OutboundCreateRequest.builder()
                .externalId(externalId)
                .type(outboundType)
                .fromTime(fromTime)
                .toTime(toTime)
                .courierRequest(courierRequest)
                .locationCreateRequest(locationCreateRequest)
                .comment(comment)
                .sortingCenter(sortingCenter)
                .logisticPointToExternalId(warehouse.getYandexId())
                .partnerToExternalId(warehouse.getPartnerId())
                .build());
    }

    @Test
    @DisplayName("Отправка уведомления в чат при ошибках отгрузки лотов (возвраты на склад)")
    void sendJugglerNotificationsWhenShipReturningOrdersFailed() {
        when(sortingCenterPropertySource.supportsReturnOutbound(anyLong())).thenReturn(true);

        Route route = new Route(
                sortingCenter, RouteType.OUTGOING_WAREHOUSE,
                LocalDate.ofInstant(toTime, clock.getZone()), OffsetTime.ofInstant(toTime, clock.getZone()),
                null, warehouse, null, null
        );
        routeRepository.save(route);
        RouteSo routeSo = null;
        if (SortableFlowSwitcherExtension.useNewRouteSoStage1_2()) {
            RouteSo routeSoBlueprint = new RouteSo(
                    sortingCenter,
                    ru.yandex.market.sc.core.domain.route_so.model.RouteType.OUT_RETURN,
                    warehouse.getId(),
                    RouteDestinationType.WAREHOUSE,
                    ScDateUtils.beginningOfDay(toTime),ScDateUtils.endOfDay(toTime),
                    null
            );

            routeSo = routeSoRepository.save(routeSoBlueprint);
        }

        outboundCommandService.put(OutboundCreateRequest.builder()
                .externalId(externalId)
                .type(type)
                .fromTime(fromTime)
                .toTime(toTime)
                .courierRequest(courierRequest)
                .locationCreateRequest(locationCreateRequest)
                .comment(comment)
                .sortingCenter(sortingCenter)
                .type(OutboundType.ORDERS_RETURN)
                .logisticPointToExternalId(Objects.requireNonNull(route.allowNextRead().getWarehouseTo()).getYandexId())
                .build());

        Cell cell = testFactory.storedCell(sortingCenter, "1", CellType.RETURN, warehouse.getYandexId());
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
        var sortWithRouteSo = SortableFlowSwitcherExtension.useNewRouteSoStage2();
        outboundCommandService.shipReturningOrders(sortWithRouteSo ? routeSo : route, null, null);

        dbQueueTestUtil.assertQueueHasSize(ScQueueType.SEND_RETURN_REGISTRY_FAILED, 1);
        dbQueueTestUtil.executeSingleQueueItem(ScQueueType.SEND_RETURN_REGISTRY_FAILED);
    }

    @Test
    @DisplayName("Отмена отгрузки")
    void cancel() {
        var outbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("1001")
                .type(OutboundType.XDOC)
                .fromTime(fromTime)
                .toTime(toTime)
                .sortingCenter(sortingCenter)
                .partnerToExternalId(warehouse.getPartnerId())
                .logisticPointToExternalId(warehouse.getYandexId())
                .build()
        );
        outboundCommandService.cancel(outbound.getId());

        Outbound actual = testFactory.getOutbound(outbound.getExternalId());
        assertThat(actual.getStatus()).isEqualTo(OutboundStatus.CANCELLED_BY_LOGISTICS);
    }

    @Test
    @DisplayName("Отмена отгрузки некорректный статус")
    void cancelIncorrectStatus() {
        var outbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("1001")
                .type(OutboundType.XDOC)
                .fromTime(fromTime)
                .toTime(toTime)
                .sortingCenter(sortingCenter)
                .partnerToExternalId(warehouse.getPartnerId())
                .logisticPointToExternalId(warehouse.getYandexId())
                .build()
        );
        outbound.setStatus(OutboundStatus.SHIPPED);
        outboundRepository.save(outbound);

        assertThatThrownBy(() -> outboundCommandService.cancel(outbound.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Outbound %d can't be cancelled because of uncancelable status or not exists"
                                .formatted(outbound.getId())
                );

        Outbound actual = testFactory.getOutbound(outbound.getExternalId());
        assertThat(actual.getStatus()).isEqualTo(OutboundStatus.SHIPPED);
    }

    @Test
    @DisplayName("Отмена отменённо отгрузки")
    void cancelTwice() {
        var outbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("1001")
                .type(OutboundType.XDOC)
                .fromTime(fromTime)
                .toTime(toTime)
                .sortingCenter(sortingCenter)
                .partnerToExternalId(warehouse.getPartnerId())
                .logisticPointToExternalId(warehouse.getYandexId())
                .build()
        );
        outbound.setStatus(OutboundStatus.CANCELLED_BY_SC);
        outboundRepository.save(outbound);

        outboundCommandService.cancel(outbound.getId());

        Outbound actual = testFactory.getOutbound(outbound.getExternalId());
        assertThat(actual.getStatus()).isEqualTo(OutboundStatus.CANCELLED_BY_SC);
    }

    @Test
    void createMovementCouriersForOutboundsTest() {
        when(sortingCenterPropertySource.supportsReturnOutbound(anyLong())).thenReturn(true);

        Route route = new Route(
                sortingCenter, RouteType.OUTGOING_WAREHOUSE,
                LocalDate.ofInstant(toTime, clock.getZone()), OffsetTime.ofInstant(toTime, clock.getZone()),
                null, warehouse, null, null
        );
        var courierRequestBuilder = MovementCourierRequest.builder()
                .externalId("fffdjhwkh3j4jgkbc")
                .name("Courier name")
                .legalName("Courier legal name")
                .carNumber("О868АС198")
                .uid(212_85_06L)
                .phone("phone2345");
        var outboundCreateRequestBuilder = OutboundCreateRequest.builder()
                .type(type)
                .fromTime(fromTime)
                .toTime(toTime)
                .locationCreateRequest(locationCreateRequest)
                .comment(comment)
                .sortingCenter(sortingCenter)
                .type(OutboundType.ORDERS_RETURN)
                .logisticPointToExternalId(route.allowNextRead().getWarehouseTo().getYandexId());

        outboundCommandService.put(outboundCreateRequestBuilder
                .externalId(externalId + "no-car-number")
                .courierRequest(courierRequestBuilder.carNumber(null).build())
                .build());
        outboundCommandService.put(outboundCreateRequestBuilder
                .externalId(externalId + "car-number-1")
                .courierRequest(courierRequestBuilder.carNumber("1").build())
                .build());
        outboundCommandService.put(outboundCreateRequestBuilder
                .externalId(externalId + "car-number-2")
                .courierRequest(courierRequestBuilder.carNumber("2").build())
                .build());

        var movementCouriersList = testFactory.getMovementCouriers();
        assertThat(movementCouriersList).hasSize(3);
    }

}
