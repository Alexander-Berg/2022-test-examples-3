package ru.yandex.market.sc.internal.controller;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.sc.core.domain.cell.CellCommandService;
import ru.yandex.market.sc.core.domain.cell.repository.CellRepository;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.inbound.model.RegistryUnitType;
import ru.yandex.market.sc.core.domain.inbound.repository.BoundRegistryRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundRegistryOrderStatus;
import ru.yandex.market.sc.core.domain.inbound.repository.Registry;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryOrder;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistrySortable;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistrySortableRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryType;
import ru.yandex.market.sc.core.domain.lot.repository.Lot;
import ru.yandex.market.sc.core.domain.lot.repository.LotRepository;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.route.RouteFacade;
import ru.yandex.market.sc.core.domain.route.repository.RouteRepository;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLotService;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.stage.Stages;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.CompareFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.sc.internal.test.ScTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.util.CompareFactory.registryOrder;
import static ru.yandex.market.sc.internal.test.ScTestUtils.fileContent;

@ScIntControllerTest
public class FFApiControllerPutInboundRegistryTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestFactory testFactory;
    @Autowired
    RegistryRepository registryRepository;
    @Autowired
    RegistrySortableRepository registrySortableRepository;
    @Autowired
    BoundRegistryRepository boundRegistryRepository;
    @Autowired
    LotRepository lotRepository;
    @Autowired
    PlaceRepository placeRepository;
    @Autowired
    SortableLotService sortableLotService;
    @Autowired
    CellCommandService cellCommandService;
    @Autowired
    CellRepository cellRepository;
    @MockBean
    Clock clock;
    @Autowired
    RouteFacade routeFacade;
    @Autowired
    RouteRepository routeRepository;

    private SortingCenter sortingCenter;
    private SortingCenter sortingCenterFrom;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        sortingCenterFrom = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(101L)
                        .partnerName("Отгружающий СЦ")
                        .yandexId("6667778882")
                        .build());
        TestFactory.setupMockClock(clock);
    }

    @Test
    void testInboundRegistryRequestSpecialCase() {
        testInboundRegistry("ff_putInbound_registry_special_case.xml", false, false);
    }

    @Test
    void testInboundRegistryRequestNoItems() {
        testInboundRegistry("ff_putInbound_registry_no_items.xml", false, false);
    }

    @Test
    void testInboundRegistryRequestNoBoxes() {
        testInboundRegistry("ff_putInbound_registry_no_boxes.xml", false, true);
    }

    @Test
    void testInboundRegistryRequestNoNothing() {
        testInboundRegistry("ff_putInbound_registry_no_nothing.xml", true, true);
    }


    void testInboundRegistry(String fileName, boolean noNothing, boolean emptyRelations) {
        String inboundExternalId = "my_inbound_id";
        String body = String.format(fileContent("ff_putInbound.xml"), sortingCenter.getToken(), inboundExternalId,
                sortingCenter.getId());
        ScTestUtils.ffApiSuccessfulCall(mockMvc, body);
        var inbound = testFactory.getInbound(inboundExternalId);
        Long inboundId = inbound.getId();

        body = String.format(fileContent(fileName), sortingCenter.getToken(), inbound.getExternalId());
        ScTestUtils.ffApiSuccessfulCall(mockMvc, body);
        var registryList = testFactory.getRegistryByInboundId(inbound.getId());
        assertThat(registryList).hasSize(1);
        var registry = registryList.get(0);
        assertThat(registry.getExternalId()).isEqualTo("registry_external_id");
        assertThat(registry.getInbound().getId()).isEqualTo(inboundId);
        assertThat(registry.getType()).isEqualTo(RegistryType.PLANNED);
        List<RegistryOrder> orders = testFactory.getRegistryOrdersByRegistryExternalId(registry.getId());
        if (noNothing) {
            assertThat(orders).hasSize(0);
        } else {
            assertThat(orders).hasSize(4);
            assertThat(StreamEx.of(orders).filter(item -> item.getExternalId().equals("multiPlace_external_order_id"))
                    .toList()).hasSize(2);
            assertThat(StreamEx.of(orders).filter(item -> item.getStatus().equals(InboundRegistryOrderStatus.CREATED))
                    .toList()).hasSize(4);
            assertThat(StreamEx.of(orders).filter(item -> item.getPlaceId().equals("place_external_id_1"))
                    .toList()).hasSize(1);
            assertThat(StreamEx.of(orders).filter(item -> item.getPlaceId().equals("place_external_id_2"))
                    .toList()).hasSize(1);

            var multiPlaceOrderPlace1 = StreamEx.of(orders)
                    .findFirst(item -> item.getPlaceId().equals("place_external_id_1")).get();
            var multiPlaceOrderPlace2 = StreamEx.of(orders)
                    .findFirst(item -> item.getPlaceId().equals("place_external_id_2")).get();
            assertThat(multiPlaceOrderPlace1.getExternalId()).isEqualTo("multiPlace_external_order_id");
            assertThat(multiPlaceOrderPlace2.getExternalId()).isEqualTo("multiPlace_external_order_id");

            assertThat(testFactory
                    .getRegistryById(multiPlaceOrderPlace1.getRegistryId()).getExternalId())
                    .isEqualTo("registry_external_id");

            assertThat(testFactory
                    .getRegistryById(multiPlaceOrderPlace1.getRegistryId())
                    .getInbound().getId()).isEqualTo(inboundId);
            assertThat(testFactory
                    .getRegistryById(multiPlaceOrderPlace1.getRegistryId())
                    .getInbound().getId()).isEqualTo(inboundId);
            if (emptyRelations) {
                assertThat(multiPlaceOrderPlace1.getPalletId()).isNull();
                assertThat(multiPlaceOrderPlace2.getPalletId()).isNull();
            } else {
                assertThat(multiPlaceOrderPlace1.getPalletId()).isEqualTo("first_pallet_id");
                assertThat(multiPlaceOrderPlace2.getPalletId()).isEqualTo("first_pallet_id");
            }

            var regularOrder1 = StreamEx.of(orders)
                    .findFirst(item -> item.getPlaceId().equals("regular_order_external_id")).get();
            assertThat(regularOrder1.getPlaceId()).isEqualTo("regular_order_external_id");
            assertThat(testFactory
                    .getRegistryById(multiPlaceOrderPlace1.getRegistryId())
                    .getInbound().getId()).isEqualTo(inboundId);
            if (emptyRelations) {
                assertThat(regularOrder1.getPalletId()).isNull();
            } else {
                assertThat(regularOrder1.getPalletId()).isEqualTo("first_pallet_id");
            }
            assertThat(testFactory
                    .getRegistryById(regularOrder1.getRegistryId()).getExternalId())
                    .isEqualTo("registry_external_id");

            var regularOrder2 = StreamEx.of(orders)
                    .findFirst(item -> item.getPlaceId().equals("regular_order_external_id_2")).get();
            assertThat(regularOrder2.getPlaceId()).isEqualTo("regular_order_external_id_2");
            assertThat(testFactory
                    .getRegistryById(multiPlaceOrderPlace1.getRegistryId())
                    .getInbound().getId()).isEqualTo(inboundId);
            assertThat(testFactory
                    .getRegistryById(regularOrder2.getRegistryId()).getExternalId())
                    .isEqualTo("registry_external_id");
            if (emptyRelations) {
                assertThat(regularOrder1.getPalletId()).isNull();
            } else {
                assertThat(regularOrder2.getPalletId()).isEqualTo("second_pallet_id");
            }
        }
    }

    @Test
    void putInboundRegistryIdempotencyTest() {
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS, true);

        String inboundExternalId = "my_inbound_id";
        String putInboundRequest = String.format(fileContent("ffapi/inbound/requests/putInboundWithShipper.xml"),
                sortingCenter.getToken(), inboundExternalId,
                sortingCenter.getYandexId(), sortingCenterFrom.getYandexId());
        ScTestUtils.ffApiSuccessfulCall(mockMvc, putInboundRequest);


        var inbound = testFactory.getInbound(inboundExternalId);
        String putInboundRegistryRequest = String.format(
                fileContent("ffapi/inbound/requests/putInboundRegistryScToSc.xml"),
                sortingCenter.getToken(),
                inbound.getExternalId()
        );
        ScTestUtils.ffApiSuccessfulCall(mockMvc, putInboundRegistryRequest);

        var registries = registryRepository.findAll();
        var registrySortables = registrySortableRepository.findAll();
        var registryOrders = boundRegistryRepository.findAll();

        ScTestUtils.ffApiSuccessfulCall(mockMvc, putInboundRegistryRequest);

        assertThat(registryRepository.findAll()).containsExactlyInAnyOrderElementsOf(registries);
        assertThat(registrySortableRepository.findAll()).containsExactlyInAnyOrderElementsOf(registrySortables);
        assertThat(boundRegistryRepository.findAll()).containsExactlyInAnyOrderElementsOf(registryOrders);
    }

    @Test
    void scToScInbound() {
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS, true);

        String inboundExternalId = "my_inbound_id";
        String putInboundRequest = String.format(fileContent("ffapi/inbound/requests/putInboundWithShipper.xml"),
                sortingCenter.getToken(), inboundExternalId,
                sortingCenter.getYandexId(), sortingCenterFrom.getYandexId());
        ScTestUtils.ffApiSuccessfulCall(mockMvc, putInboundRequest);


        var inbound = testFactory.getInbound(inboundExternalId);
        String putInboundRegistryRequest = String.format(
                fileContent("ffapi/inbound/requests/putInboundRegistryScToSc.xml"),
                sortingCenter.getToken(),
                inbound.getExternalId()
        );
        ScTestUtils.ffApiSuccessfulCall(mockMvc, putInboundRegistryRequest);


        var registries = registryRepository.findAll();
        assertThat(registries).hasSize(1);

        var register = registries.get(0);
        assertThat(register)
                .extracting(Registry::getInbound, Registry::getType, Registry::getExternalId)
                .containsExactly(inbound, RegistryType.PLANNED, "registry_external_id");

        assertThat(registrySortableRepository.findAll())
                .allMatch(reg -> reg.getUnitType() == RegistryUnitType.PALLET)
                .allMatch(reg -> Objects.equals(reg.getRegistry(), register))
                .map(RegistrySortable::getSortableExternalId)
                .containsExactlyInAnyOrder("SC_LOT_pallet-1", "SC_LOT_pallet-31", "SC_LOT_pallet-32");

        var orderRegRecords = boundRegistryRepository.findAll()
                .stream().map(CompareFactory::registryOrder).toList();

        assertThat(orderRegRecords)
                .containsExactlyInAnyOrder(
                        registryOrder(register.getId(), "single-place-order", "single-place-order", "SC_LOT_pallet-1"),
                        registryOrder(register.getId(), "multi-place-order-1", "place-1", "SC_LOT_pallet-1"),
                        registryOrder(register.getId(), "multi-place-order-2", "place-1", "SC_LOT_pallet-1"),
                        registryOrder(register.getId(), "multi-place-order-2", "place-2", "SC_LOT_pallet-1"),
                        registryOrder(register.getId(), "multi-place-order-3", "place-31", "SC_LOT_pallet-31"),
                        registryOrder(register.getId(), "multi-place-order-3", "place-32", "SC_LOT_pallet-32")
                );
    }

    @Test
    void scToScInboundWithStamp() {
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS, true);
        testFactory.setSortingCenterProperty(sortingCenterFrom.getId(), SortingCenterPropertiesKey.IS_DROPOFF, true);

        createOrders();

        String inboundExternalId = "my_inbound_id";
        String putInboundRequest = String.format(fileContent("ffapi/inbound/requests/putInboundWithShipper.xml"),
                sortingCenter.getToken(), inboundExternalId, sortingCenter.getYandexId(),
                sortingCenterFrom.getYandexId());
        ScTestUtils.ffApiSuccessfulCall(mockMvc, putInboundRequest);


        var inbound = testFactory.getInbound(inboundExternalId);
        String putInboundRegistryRequest = String.format(
                fileContent("ffapi/inbound/requests/putInboundRegistryWithStamp.xml"),
                sortingCenter.getToken(),
                inbound.getExternalId()
        );
        ScTestUtils.ffApiSuccessfulCall(mockMvc, putInboundRegistryRequest);


        var registries = registryRepository.findAll();
        assertThat(registries).hasSize(1);

        var register = registries.get(0);
        assertThat(register)
                .extracting(Registry::getInbound, Registry::getType, Registry::getExternalId)
                .containsExactly(inbound, RegistryType.PLANNED, "registry_external_id");

        assertThat(registrySortableRepository.findAll())
                .allMatch(reg -> reg.getUnitType() == RegistryUnitType.PALLET)
                .allMatch(reg -> Objects.equals(reg.getRegistry(), register))
                .map(RegistrySortable::getSortableExternalId)
                .containsExactlyInAnyOrder("SC_LOT_pallet-1", "SC_LOT_pallet-31", "SC_LOT_pallet-32");

        var orderRegRecords = boundRegistryRepository.findAll()
                .stream().map(CompareFactory::registryOrder).toList();

        assertThat(orderRegRecords)
                .containsExactlyInAnyOrder(
                        registryOrder(register.getId(), "single-place-order", "single-place-order", "SC_LOT_pallet-1"),
                        registryOrder(register.getId(), "multi-place-order-3", "place-31", "SC_LOT_pallet-31"),
                        registryOrder(register.getId(), "multi-place-order-3", "place-32", "SC_LOT_pallet-32"),
                        registryOrder(register.getId(), "multi-place-order-3", "place-33", "SC_LOT_pallet-32")
                );

        List<Lot> lots = lotRepository.findAll();
        assertThat(lots).hasSize(3);

        List<Place> places = placeRepository.findAll();
        assertThat(places).allMatch(place -> Objects.nonNull(place.getLot()));
    }

    @Test
    void scToScCrossDock() {
        enableCrossDockProperties();

        var ds = testFactory.storedDeliveryService("34573456");
        var courierWithDs = testFactory.magistralCourier("12");
        var cell = testFactory.storedCell(sortingCenter, "117", courierWithDs.deliveryService());
        var multiPlaceOrder = testFactory.createOrder(
                TestFactory.CreateOrderParams.builder()
                        .externalId("multi-place-order-3")
                        .places("place-31", "place-32", "place-33")
                        .sortingCenter(sortingCenter)
                        .dsType(DeliveryServiceType.TRANSIT)
                        .deliveryService(courierWithDs.deliveryService())
                        .shipmentDate(LocalDate.now(clock))
                        .build()
        ).get();

        String inboundExternalId = "my_inbound_id";
        String putInboundRequest = String.format(fileContent("ffapi/inbound/requests/putInboundWithShipper.xml"),
                sortingCenter.getToken(), inboundExternalId, sortingCenter.getYandexId(),
                sortingCenterFrom.getYandexId());
        ScTestUtils.ffApiSuccessfulCall(mockMvc, putInboundRequest);

        var inbound = testFactory.getInbound(inboundExternalId);
        String putInboundRegistryRequest = String.format(
                fileContent("ffapi/inbound/requests/putInboundRegistryScToScCrossDock.xml"),
                sortingCenter.getToken(),
                inbound.getExternalId()
        );
        ScTestUtils.ffApiSuccessfulCall(mockMvc, putInboundRegistryRequest);

        var registries = registryRepository.findAll();
        assertThat(registries).hasSize(1);

        var register = registries.get(0);
        assertThat(register)
                .extracting(Registry::getInbound, Registry::getType, Registry::getExternalId)
                .containsExactly(inbound, RegistryType.PLANNED, "registry_external_id");

        assertThat(registrySortableRepository.findAll())
                .allMatch(reg -> reg.getUnitType() == RegistryUnitType.PALLET)
                .allMatch(reg -> Objects.equals(reg.getRegistry(), register))
                .map(RegistrySortable::getSortableExternalId)
                .containsExactlyInAnyOrder("SC_LOT_pallet-cross-dock", "SC_LOT_pallet-31");

        var orderRegRecords = boundRegistryRepository.findAll()
                .stream().map(CompareFactory::registryOrder).toList();

        assertThat(orderRegRecords)
                .containsExactlyInAnyOrder(
                        registryOrder(register.getId(), "single-place-order", "single-place-order",
                                "SC_LOT_pallet-cross-dock"),
                        registryOrder(register.getId(), "multi-place-order-3", "place-31", "SC_LOT_pallet-31"),
                        registryOrder(register.getId(), "multi-place-order-3", "place-32", "SC_LOT_pallet-31"),
                        registryOrder(register.getId(), "multi-place-order-3", "place-33", "SC_LOT_pallet-31")
                );

        var lotIds = lotRepository.findAll().stream().map(Lot::getId).collect(Collectors.toList());
        var sortableLots = sortableLotService.findAllByLotIds(lotIds);
        assertThat(sortableLots).hasSize(2);

        var crossDockSortableLot = sortableLots.stream()
                .filter(it -> Boolean.TRUE.equals(it.getCrossDock()))
                .findFirst()
                .orElseThrow();
        assertThat(crossDockSortableLot.getLotStatus()).isEqualTo(LotStatus.READY);
        assertThat(crossDockSortableLot.getSortable().getMutableState().getStageId())
                .isEqualTo(Stages.SORTED_DIRECT.getId());
        assertThat(crossDockSortableLot.getParentCell()).isEqualTo(cell);
        assertThat(crossDockSortableLot.getCrossDock()).isTrue();

        var casualSortableLot = sortableLots.stream()
                .filter(it -> !Boolean.TRUE.equals(it.getCrossDock()))
                .findFirst()
                .orElseThrow();

        assertThat(casualSortableLot.getSortable().getMutableState().getStageId())
                .isEqualTo(Stages.AWAITING_DIRECT.getId());
        assertThat(casualSortableLot.getLotStatus()).isEqualTo(LotStatus.READY);
        assertThat(casualSortableLot.getParentCell()).isNull();
        assertThat(casualSortableLot.getCrossDock()).isFalse();

        testFactory.shipLotRouteByParentCell(crossDockSortableLot);
        crossDockSortableLot = sortableLotService.findBySortableId(crossDockSortableLot.getSortableId()).orElseThrow();
        assertThat(crossDockSortableLot.getLotStatus()).isEqualTo(LotStatus.SHIPPED);
        assertThat(crossDockSortableLot.getSortable().getMutableState().getStageId())
                .isEqualTo(Stages.SHIPPED_DIRECT.getId());

        List<Place> places = placeRepository.findAll();
        assertThat(places).hasSize(3);
        assertThat(places).allMatch(place -> Objects.nonNull(place.getLot()));
        assertThat(places).allMatch(place -> place.getStageId().equals(Stages.AWAITING_DIRECT.getId()));
    }

    @Test
    void scToScCrossDockNewCellAndRouteCreated() {
        enableCrossDockProperties();

        var courier = testFactory.storedCourier(1100000000000012L);
        var putInboundRegistryRequest
                = buildInboundAndPutRequest("ffapi/inbound/requests/putInboundRegistryScToScCrossDock.xml");

        assertThat(cellRepository.findAll()).hasSize(0);
        assertThat(routeRepository.findAllByExpectedDateAndCourierTo(LocalDate.now(clock), courier)).isEmpty();

        ScTestUtils.ffApiSuccessfulCall(mockMvc, putInboundRegistryRequest);

        assertThat(cellRepository.findAll()).hasSize(1);
        assertThat(routeRepository.findAllByExpectedDateAndCourierTo(LocalDate.now(clock), courier)).isNotEmpty();

        var lotIds = lotRepository.findAll().stream().map(Lot::getId).collect(Collectors.toList());
        var sortableLots = sortableLotService.findAllByLotIds(lotIds);
        var crossDockSortableLot = sortableLots.stream()
                .filter(it -> Boolean.TRUE.equals(it.getCrossDock()))
                .findFirst()
                .orElseThrow();
        assertThat(crossDockSortableLot.getParentCell()).isNotNull();

        testFactory.shipLotRouteByParentCell(crossDockSortableLot);
        crossDockSortableLot = sortableLotService.findBySortableId(crossDockSortableLot.getSortableId()).orElseThrow();
        assertThat(crossDockSortableLot.getLotStatus()).isEqualTo(LotStatus.SHIPPED);
        assertThat(crossDockSortableLot.getSortable().getMutableState().getStageId())
                .isEqualTo(Stages.SHIPPED_DIRECT.getId());
    }

    @Test
    void scToScCrossDockNewRouteCreatedAndBoundWithExistingCell() {
        enableCrossDockProperties();

        var courier = testFactory.storedCourier(1100000000000012L);
        var putInboundRegistryRequest
                = buildInboundAndPutRequest("ffapi/inbound/requests/putInboundRegistryScToScCrossDock.xml");

        assertThat(routeRepository.findAllByExpectedDateAndCourierTo(LocalDate.now(clock), courier)).isEmpty();
        var cell = cellCommandService.createNotActiveCourierCellAndBindRoutes(sortingCenter, courier.getId());
        ScTestUtils.ffApiSuccessfulCall(mockMvc, putInboundRegistryRequest);

        assertThat(cellRepository.findAll()).containsExactly(cell);
        var routes = routeRepository.findAllByExpectedDateAndCourierTo(LocalDate.now(clock), courier);
        assertThat(routes).hasSize(1);
        assertThat(routes.get(0).allowReading().getCells()).containsExactly(cell);

        var lotIds = lotRepository.findAll().stream().map(Lot::getId).collect(Collectors.toList());
        var sortableLots = sortableLotService.findAllByLotIds(lotIds);
        var crossDockSortableLot = sortableLots.stream()
                .filter(it -> Boolean.TRUE.equals(it.getCrossDock()))
                .findFirst()
                .orElseThrow();
        assertThat(crossDockSortableLot.getParentCell()).isNotNull();

        testFactory.shipLotRouteByParentCell(crossDockSortableLot);
        crossDockSortableLot = sortableLotService.findBySortableId(crossDockSortableLot.getSortableId()).orElseThrow();
        assertThat(crossDockSortableLot.getLotStatus()).isEqualTo(LotStatus.SHIPPED);
        assertThat(crossDockSortableLot.getSortable().getMutableState().getStageId())
                .isEqualTo(Stages.SHIPPED_DIRECT.getId());
    }

    private String buildInboundAndPutRequest(String fileName) {
        testFactory.createOrder(
                TestFactory.CreateOrderParams.builder()
                        .externalId("multi-place-order-3")
                        .places("place-31", "place-32", "place-33")
                        .sortingCenter(sortingCenter)
                        .build()
        ).get();

        String inboundExternalId = "my_inbound_id";
        String putInboundRequest = String.format(fileContent("ffapi/inbound/requests/putInboundWithShipper.xml"),
                sortingCenter.getToken(), inboundExternalId, sortingCenter.getYandexId(),
                sortingCenterFrom.getYandexId());
        ScTestUtils.ffApiSuccessfulCall(mockMvc, putInboundRequest);

        var inbound = testFactory.getInbound(inboundExternalId);

        return String.format(
                fileContent(fileName),
                sortingCenter.getToken(),
                inbound.getExternalId()
        );
    }


    @Test
    void scToScCrossDockRegistryOrdersCreatedAfterShipment() {
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS, true);
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.CREATE_LOTS_FROM_REGISTRY, true);
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.CREATE_CROSS_DOCK_SORTED, true);

        var courier = testFactory.storedCourier(1100000000000012L);
        testFactory.createOrder(
                TestFactory.CreateOrderParams.builder()
                        .externalId("multi-place-order-3")
                        .places("place-31", "place-32", "place-33")
                        .sortingCenter(sortingCenter)
                        .build()
        ).get();

        String inboundExternalId = "my_inbound_id";
        String putInboundRequest = String.format(fileContent("ffapi/inbound/requests/putInboundWithShipper.xml"),
                sortingCenter.getToken(), inboundExternalId, sortingCenter.getYandexId(),
                sortingCenterFrom.getYandexId());
        ScTestUtils.ffApiSuccessfulCall(mockMvc, putInboundRequest);

        var inbound = testFactory.getInbound(inboundExternalId);
        String putInboundRegistryRequest = String.format(
                fileContent("ffapi/inbound/requests/putInboundRegistryScToScCrossDock.xml"),
                sortingCenter.getToken(),
                inbound.getExternalId()
        );

        assertThat(cellRepository.findAll()).hasSize(0);
        assertThat(routeRepository.findAllByExpectedDateAndCourierTo(LocalDate.now(clock), courier)).isEmpty();

        ScTestUtils.ffApiSuccessfulCall(mockMvc, putInboundRegistryRequest);

        assertThat(cellRepository.findAll()).hasSize(1);
        assertThat(routeRepository.findAllByExpectedDateAndCourierTo(LocalDate.now(clock), courier)).isNotEmpty();

        var lotIds = lotRepository.findAll().stream().map(Lot::getId).collect(Collectors.toList());
        var sortableLots = sortableLotService.findAllByLotIds(lotIds);
        var crossDockSortableLot = sortableLots.stream()
                .filter(it -> Boolean.TRUE.equals(it.getCrossDock()))
                .findFirst()
                .orElseThrow();
        assertThat(crossDockSortableLot.getParentCell()).isNotNull();

        testFactory.shipLotRouteByParentCell(crossDockSortableLot);
        crossDockSortableLot = sortableLotService.findBySortableId(crossDockSortableLot.getSortableId()).orElseThrow();
        assertThat(crossDockSortableLot.getLotStatus()).isEqualTo(LotStatus.SHIPPED);
        assertThat(crossDockSortableLot.getSortable().getMutableState().getStageId())
                .isEqualTo(Stages.SHIPPED_DIRECT.getId());
//        сюда добавляем отгрузку и проверку shipLotsWithSingleOrderMiddleMile7
    }

    private void createOrders() {
        testFactory.createOrder(
                TestFactory.CreateOrderParams.builder()
                        .externalId("single-place-order")
                        .places("single-place-order")
                        .sortingCenter(sortingCenter)
                        .build()
        );
        testFactory.createOrder(
                TestFactory.CreateOrderParams.builder()
                        .externalId("multi-place-order-3")
                        .places("place-31", "place-32", "place-33")
                        .sortingCenter(sortingCenter)
                        .build()
        );
    }

    private void enableCrossDockProperties() {
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS, true);
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.CREATE_LOTS_FROM_REGISTRY, true);
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.CREATE_CROSS_DOCK_SORTED, true);
    }
}
