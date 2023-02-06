
package ru.yandex.market.sc.internal.controller;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.sc.core.domain.client_return.repository.ClientReturnBarcodePrefix;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryServiceProperty;
import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.model.RegistryUnitType;
import ru.yandex.market.sc.core.domain.inbound.repository.BoundRegistryLockRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.BoundRegistryRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.inbound.repository.Registry;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryOrder;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistrySortable;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistrySortableRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryType;
import ru.yandex.market.sc.core.domain.outbound.OutboundCommandService;
import ru.yandex.market.sc.core.domain.outbound.model.CreateOutboundPlannedRegistryRequest;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundType;
import ru.yandex.market.sc.core.domain.outbound.repository.Outbound;
import ru.yandex.market.sc.core.domain.sortable.model.enums.DirectFlowType;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.SortableRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.test.SortableTestFactory;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.CompareFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.sc.internal.test.ScTestUtils;
import ru.yandex.market.sc.internal.util.ScIntControllerCaller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.domain.inbound.repository.RegistryType.FACTUAL_DELIVERED_ORDERS_RETURN;
import static ru.yandex.market.sc.core.domain.inbound.repository.RegistryType.FACTUAL_UNDELIVERED_ORDERS_RETURN;
import static ru.yandex.market.sc.core.test.TestFactory.order;
import static ru.yandex.market.sc.core.util.CompareFactory.registryOrder;
import static ru.yandex.market.sc.internal.test.Template.fromFile;

@ScIntControllerTest
class FFApiControllerV2GetOutboundTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestFactory testFactory;
    @Autowired
    BoundRegistryLockRepository registryLockRepository;
    @Autowired
    BoundRegistryRepository boundRegistryRepository;
    @Autowired
    RegistryRepository registryRepository;
    @Autowired
    RegistrySortableRepository registrySortableRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    SortableTestFactory sortableTestFactory;
    @Autowired
    OutboundCommandService outboundCommandService;
    @Autowired
    SortableRepository sortableRepository;

    private SortingCenter sortingCenter;
    private SortingCenter sortingCenterTo;
    private User user;
    private Warehouse warehouse;
    private Outbound outbound;
    private String requestUID;

    @MockBean
    private Clock clock;

    @BeforeEach
    void setup() {
        var sortingCenterPartner = testFactory.storedSortingCenterPartner(1000, "sortingCenter-token");
        sortingCenter = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(100L)
                        .partnerName("Новый СЦ")
                        .sortingCenterPartnerId(sortingCenterPartner.getId())
                        .token(sortingCenterPartner.getToken())
                        .yandexId("6667778881")
                        .build());
        user = testFactory.getOrCreateStoredUser(sortingCenter);

        sortingCenterTo = testFactory.storedSortingCenter2();

        warehouse = testFactory.storedWarehouse();
        outbound = testFactory.createOutbound(sortingCenter);
        requestUID = UUID.randomUUID().toString().replace("-", "");
        testFactory.setupMockClock(clock);
    }

    @ParameterizedTest(name = "{index} : {0}")
    @MethodSource("testData")
    @SneakyThrows
    void getOutboundWithUnpaidOrder(RegistryType registryType, String responseFile) {
        var registry = testFactory.bindRegistry(outbound.getExternalId(), registryType);
        registryLockRepository.saveRw(new RegistryOrder("order_id", "place1", registry.getId(), null));
        jdbcTemplate.update("UPDATE registry SET created_at = '2021-05-10T10:00:00Z' WHERE id = 1");
        String externalId = outbound.getExternalId();
        ScTestUtils.ffApiV2SuccessfulCall(mockMvc,
                        sortingCenter.getToken(),
                        "getOutbound",
                        "<outboundId>" +
                                "<yandexId>" + externalId + "</yandexId>" +
                                "<partnerId>" + externalId + "</partnerId>" +
                                "</outboundId>")
                .andExpect(content().xml(ScTestUtils.fileContent(responseFile)));
    }

    private static Stream<Arguments> testData() {
        return Stream.of(
                Arguments.of(FACTUAL_UNDELIVERED_ORDERS_RETURN, "ff_getOutbound_response.xml"),
                Arguments.of(FACTUAL_DELIVERED_ORDERS_RETURN, "ff_getOutbound_clientReturn_response.xml")
        );
    }

    @Test
    @SneakyThrows
    @DisplayName("Не заполнять order_id для заказов типа ClientReturnType.FASHION")
    void getOutboundClientReturnFashion() {
        var specialOutbound = testFactory.createOutbound(sortingCenter);
        var registry = testFactory.bindRegistry(specialOutbound.getExternalId(), FACTUAL_DELIVERED_ORDERS_RETURN);
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getWarehouseReturnId());
        testFactory.storedFakeReturnDeliveryService();
        var order = testFactory.createClientReturnForToday(
                        sortingCenter.getId(),
                        sortingCenter.getToken(),
                        sortingCenter.getYandexId(),
                        testFactory.defaultCourier(),
                        "FSN_RET_" + 666
                )
                .get();
        jdbcTemplate.update("UPDATE registry SET created_at = '2021-09-20T12:00:00Z' WHERE id = 1");
        registryLockRepository.saveRw(
                new RegistryOrder(order.getExternalId(), order.getExternalId(), registry.getId(), null));
        var externalId = specialOutbound.getExternalId();
        ScTestUtils.ffApiV2SuccessfulCall(
                        mockMvc,
                        sortingCenter.getToken(),
                        "getOutbound",
                        "<outboundId>" +
                                "<yandexId>" + externalId + "</yandexId>" +
                                "<partnerId>" + externalId + "</partnerId>" +
                                "</outboundId>"
                )
                .andDo(print())
                .andExpect(content().xml(
                        ScTestUtils.fileContent("ff_getOutbound_clientReturnFashion_response.xml")));
    }

    @Test
    @DisplayName("getOutbound для xDoc поставки")
    @SneakyThrows
    void doIt() {
        Inbound inbound = createInbound();
        var sortablePallet = sortableTestFactory
                .storeSortable(sortingCenter, SortableType.XDOC_PALLET, DirectFlowType.TRANSIT, "XDOC-p1",
                        inbound, null)
                .dummyChangeSortableStatus(SortableStatus.SORTED_DIRECT)
                .get();

        var sortableBox = sortableTestFactory
                .storeSortable(sortingCenter, SortableType.XDOC_BOX, DirectFlowType.TRANSIT, "XDOC-b1",
                        inbound, null)
                .dummyChangeSortableStatus(SortableStatus.SORTED_DIRECT)
                .get();

        var xDocOutbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("1010")
                .type(OutboundType.XDOC)
                .fromTime(clock.instant())
                .toTime(clock.instant())
                .sortingCenter(sortingCenter)
                .partnerToExternalId(warehouse.getPartnerId())
                .logisticPointToExternalId(warehouse.getYandexId())
                .build()
        );
        outboundCommandService.putPlannedRegistry(CreateOutboundPlannedRegistryRequest.builder()
                .sortingCenter(sortingCenter)
                .registryExternalId("1234")
                .outboundExternalId(xDocOutbound.getExternalId())
                .palletExternalIds(List.of(sortablePallet.getRequiredBarcodeOrThrow()))
                .boxExternalIds(List.of(sortableBox.getRequiredBarcodeOrThrow()))
                .build());
        jdbcTemplate.update("UPDATE registry SET created_at = '2021-05-10T10:00:00Z' WHERE external_id = '1234'");

        var expectedResponse = fromFile("ffapi/outbound/getOutboundResponseXDocTemplate.xml")
                .setValue("uniq", requestUID)
                .setValue("outboundId.yandexId", xDocOutbound.getExternalId())
                .setValue("outboundId.partnerId", xDocOutbound.getExternalId())
                .setValue("registryId.yandexId", "1234")
                .setValue("registryId.partnerId", "1234")
                .setValue("boxId", sortableBox.getRequiredBarcodeOrThrow())
                .setValue("palletId", sortablePallet.getRequiredBarcodeOrThrow())
                .resolve();

        ScTestUtils.ffApiV2SuccessfulCall(mockMvc, getOutboundRequest(xDocOutbound))
                .andExpect(content().xml(expectedResponse));
    }

    @Test
    @DisplayName("getOutbound для отгрузки из дропоффа на Сорт центр. Лоты с пломбами")
    @SneakyThrows
    void dropoffToSCOutboundWithStamp() {
        final String stampId = "stampId123";
        testFactory.increaseScOrderId();
        ScIntControllerCaller caller = ScIntControllerCaller.createCaller(mockMvc, sortingCenter.getPartnerId());
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS, true);

        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterTo.getId()));
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(sortingCenter.getId()));

        var scToScOutbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("1011")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(sortingCenter)
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );


        // single place order
        var place1 = testFactory.createForToday(
                        order(sortingCenter, "single-place-order")
                                .deliveryService(deliveryService)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .build())
                .accept().sort().getPlace();
        var cell = place1.getCell();
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
        place1 = testFactory.sortPlaceToLot(place1, lot, user);

        // multi place order
        var multiPlaceOrder2 = testFactory.createForToday(
                order(sortingCenter, "multi-place-order-2")
                        .places("place-1", "place-2")
                        .deliveryService(deliveryService)
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build()
        ).acceptPlaces("place-1", "place-2").sortPlaces("place-1", "place-2").getPlaces();
        var place21 = multiPlaceOrder2.get("place-1");
        var place22 = multiPlaceOrder2.get("place-2");

        multiPlaceOrder2.values()
                .forEach(p -> testFactory.sortPlaceToLot(p, lot, user));

        var route = testFactory.findOutgoingCourierRoute(place1).orElseThrow();
        var route3 = testFactory.findOutgoingCourierRoute(place22).orElseThrow();
        assertThat(route)
                .isEqualTo(route3);
        testFactory.addStampToSortableLotAndPrepare(lot.getBarcode(), stampId, user);
        testFactory.bindLotToOutbound(scToScOutbound.getExternalId(), lot.getBarcode(), testFactory.getRouteIdForSortableFlow(route), user);

        caller.shipOutbound(scToScOutbound.getExternalId())
                .andExpect(status().isOk());

        List<Registry> registries = registryRepository.findAllByOutboundId(scToScOutbound.getId());
        assertThat(registries).hasSize(1);

        var register = registries.get(0);
        assertThat(register)
                .extracting(Registry::getOutbound, Registry::getType)
                .containsExactly(scToScOutbound, RegistryType.FACTUAL);

        assertThat(registrySortableRepository.findAll())
                .allMatch(reg -> reg.getUnitType() == RegistryUnitType.PALLET)
                .allMatch(reg -> Objects.equals(reg.getRegistry(), register))
                .map(RegistrySortable::getSortableExternalId)
                .containsExactlyInAnyOrder(
                        lot.getBarcode()
                );

        var orderRegRecords = boundRegistryRepository.findAll()
                .stream().map(CompareFactory::registryOrder).toList();

        assertThat(orderRegRecords)
                .containsExactlyInAnyOrder(
                        registryOrder(register.getId(), "single-place-order", "single-place-order", lot.getBarcode()),
                        registryOrder(register.getId(), "multi-place-order-2", "place-1", lot.getBarcode()),
                        registryOrder(register.getId(), "multi-place-order-2", "place-2", lot.getBarcode())
                );

        jdbcTemplate.update("UPDATE registry SET created_at = '2021-05-10T10:00:00Z' WHERE id = ?", register.getId());

        var expectedResponse = fromFile("ffapi/outbound/responses/getOutboundDropoffToScWithStamp.xml")
                .setValue("uniq", requestUID)
                .setValue("registerId", Long.toString(register.getId()))
                .resolve();

        ScTestUtils.ffApiV2SuccessfulCall(mockMvc, getOutboundRequest(scToScOutbound))
                .andExpect(content().xml(expectedResponse));
    }

    @Test
    @DisplayName("getOutbound для отгрузки из Сорт центра на Сорт центр")
    @SneakyThrows
    void scToSCOutbound() {
        testFactory.increaseScOrderId();
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS, true);
        ScIntControllerCaller caller = ScIntControllerCaller.createCaller(mockMvc, sortingCenter.getPartnerId());

        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterTo.getId()));
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(sortingCenter.getId()));

        var scToScOutbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("1011")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(sortingCenter)
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );


        // single place order
        var place1 = testFactory.createForToday(
                        order(sortingCenter, "single-place-order")
                                .deliveryService(deliveryService)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .build()
                )
                .accept().sort().getPlace();
        var cell = place1.getCell();
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
        place1 = testFactory.sortPlaceToLot(place1, lot, user);


        // incomplete multi place order
        var multiPlaceOrder2 = testFactory.createForToday(
                order(sortingCenter, "multi-place-order-2")
                        .places("place-1", "place-2")
                        .deliveryService(deliveryService)
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build()
        ).acceptPlaces("place-1", "place-2").sortPlaces("place-1", "place-2").getPlaces();

        testFactory.sortPlaceToLot(multiPlaceOrder2.get("place-1"), lot, user);


        // multi place order
        var multiPlaceOrder3 = testFactory.createForToday(
                order(sortingCenter, "multi-place-order-3")
                        .places("place-1", "place-2")
                        .deliveryService(deliveryService)
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build()
        ).acceptPlaces("place-1", "place-2").sortPlaces("place-1", "place-2").getPlaces();

        multiPlaceOrder3.values()
                .forEach(p -> testFactory.sortPlaceToLot(p, lot, user));


        // multi place order in several lots
        var multiPlaceOrder4 = testFactory.createForToday(
                order(sortingCenter, "multi-place-order-4")
                        .places("place-41", "place-42")
                        .deliveryService(deliveryService)
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build()
        ).acceptPlaces("place-41", "place-42").sortPlaces("place-41", "place-42").getPlaces();

        var cell4 = multiPlaceOrder4.get("place-41").getCell();
        var lot41 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell4);
        var lot42 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell4);

        var place41 = multiPlaceOrder4.get("place-41");
        var place42 = multiPlaceOrder4.get("place-42");
        testFactory.sortPlaceToLot(place41, lot41, user);
        testFactory.sortPlaceToLot(place42, lot42, user);


        testFactory.prepareToShipLot(lot);
        testFactory.prepareToShipLot(lot41);
        testFactory.prepareToShipLot(lot42);
        var route = testFactory.findOutgoingCourierRoute(place1).orElseThrow();
        var route2 = testFactory.findOutgoingCourierRoute(multiPlaceOrder2.get("place-1")).orElseThrow();
        var route3 = testFactory.findOutgoingCourierRoute(multiPlaceOrder3.get("place-1")).orElseThrow();
        var route4 = testFactory.findOutgoingCourierRoute(multiPlaceOrder4.get("place-41")).orElseThrow();
        assertThat(route)
                .isEqualTo(route2)
                .isEqualTo(route3)
                .isEqualTo(route4);

        testFactory.bindLotToOutbound(scToScOutbound.getExternalId(), lot.getBarcode(), testFactory.getRouteIdForSortableFlow(route), user);
        testFactory.bindLotToOutbound(scToScOutbound.getExternalId(), lot41.getBarcode(), testFactory.getRouteIdForSortableFlow(route), user);
        testFactory.bindLotToOutbound(scToScOutbound.getExternalId(), lot42.getBarcode(), testFactory.getRouteIdForSortableFlow(route), user);

        caller.shipOutbound(scToScOutbound.getExternalId())
                .andExpect(status().isOk());

        List<Registry> registries = registryRepository.findAllByOutboundId(scToScOutbound.getId());
        assertThat(registries).hasSize(1);

        var register = registries.get(0);
        assertThat(register)
                .extracting(Registry::getOutbound, Registry::getType)
                .containsExactly(scToScOutbound, RegistryType.FACTUAL);

        assertThat(registrySortableRepository.findAll())
                .allMatch(reg -> reg.getUnitType() == RegistryUnitType.PALLET)
                .allMatch(reg -> Objects.equals(reg.getRegistry(), register))
                .map(RegistrySortable::getSortableExternalId)
                .containsExactlyInAnyOrder(
                        lot.getBarcode(),
                        lot41.getBarcode(),
                        lot42.getBarcode()
                );

        var orderRegRecords = boundRegistryRepository.findAll()
                .stream().map(CompareFactory::registryOrder).toList();

        assertThat(orderRegRecords)
                .containsExactlyInAnyOrder(
                        registryOrder(register.getId(), "single-place-order", "single-place-order", lot.getBarcode()),
                        registryOrder(register.getId(), "multi-place-order-2", "place-1", lot.getBarcode()),
                        registryOrder(register.getId(), "multi-place-order-3", "place-1", lot.getBarcode()),
                        registryOrder(register.getId(), "multi-place-order-3", "place-2", lot.getBarcode()),
                        registryOrder(register.getId(), "multi-place-order-4", "place-41", lot41.getBarcode()),
                        registryOrder(register.getId(), "multi-place-order-4", "place-42", lot42.getBarcode())
                );

        jdbcTemplate.update("UPDATE registry SET created_at = '2021-05-10T10:00:00Z' WHERE id = ?", register.getId());

        var expectedResponse = fromFile("ffapi/outbound/responses/getOutboundScToSc.xml")
                .setValue("uniq", requestUID)
                .setValue("registerId", Long.toString(register.getId()))
                .resolve();

        ScTestUtils.ffApiV2SuccessfulCall(mockMvc, getOutboundRequest(scToScOutbound))
                .andExpect(content().xml(expectedResponse));
    }

    private Inbound createInbound() {
        return testFactory.createInbound(TestFactory.CreateInboundParams.builder()
                .inboundType(InboundType.XDOC_FINAL)
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId("warehouse-from-id")
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .registryMap(Map.of())
                .build());
    }

    private String getOutboundRequest(Outbound outbound) {
        return fromFile("ffapi/outbound/getOutboundRequestTemplate.xml")
                .setValue("token", sortingCenter.getToken())
                .setValue("uniq", requestUID)
                .setValue("outboundId.yandexId", outbound.getExternalId())
                .setValue("outboundId.partnerId", outbound.getExternalId())
                .resolve();
    }

}


