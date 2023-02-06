package ru.yandex.market.sc.internal.controller;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.sc.core.configuration.ConfigurationProperties;
import ru.yandex.market.sc.core.domain.client_return.repository.ClientReturnBarcodePrefix;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.repository.BoundRegistryLockRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryOrder;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryType;
import ru.yandex.market.sc.core.domain.lot.repository.LotSize;
import ru.yandex.market.sc.core.domain.outbound.OutboundCommandService;
import ru.yandex.market.sc.core.domain.outbound.model.CreateOutboundPlannedRegistryRequest;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundType;
import ru.yandex.market.sc.core.domain.outbound.repository.Outbound;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLotService;
import ru.yandex.market.sc.core.domain.sortable.model.enums.DirectFlowType;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.test.SortableTestFactory;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.sc.internal.test.ScTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;
import static ru.yandex.market.sc.core.domain.inbound.repository.RegistryType.FACTUAL_DELIVERED_ORDERS_RETURN;
import static ru.yandex.market.sc.core.domain.inbound.repository.RegistryType.FACTUAL_UNDELIVERED_ORDERS_RETURN;
import static ru.yandex.market.sc.core.test.TestFactory.order;
import static ru.yandex.market.sc.core.test.TestFactory.sortWithRouteSo;
import static ru.yandex.market.sc.internal.test.Template.fromFile;

@Slf4j
@ScIntControllerTest
class FFApiControllerGetOutboundTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestFactory testFactory;
    @Autowired
    BoundRegistryLockRepository registryLockRepository;
    @Autowired
    SortableLotService sortableLotService;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    SortableTestFactory sortableTestFactory;
    @Autowired
    OutboundCommandService outboundCommandService;
    @Autowired
    TransactionTemplate transactionTemplate;

    private SortingCenter sortingCenter;
    private Warehouse warehouse;
    private Outbound outbound;
    private String requestUID;

    @MockBean
    private Clock clock;

    @BeforeEach
    void setup() {
        sortingCenter = testFactory.storedSortingCenter(100L, "Новый СЦ");
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
        ScTestUtils.ffApiSuccessfulCall(mockMvc,
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
        var outboundLocal = testFactory.createOutbound(sortingCenter);
        var registry = testFactory.bindRegistry(outboundLocal.getExternalId(), FACTUAL_DELIVERED_ORDERS_RETURN);
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
        var externalId = outboundLocal.getExternalId();
        ScTestUtils.ffApiSuccessfulCall(
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

        var request = fromFile("ffapi/outbound/getOutboundRequestTemplate.xml")
                .setValue("token", sortingCenter.getToken())
                .setValue("uniq", requestUID)
                .setValue("outboundId.yandexId", xDocOutbound.getExternalId())
                .setValue("outboundId.partnerId", xDocOutbound.getExternalId())
                .resolve();

        var expectedResponse = fromFile("ffapi/outbound/getOutboundResponseXDocTemplate.xml")
                .setValue("uniq", requestUID)
                .setValue("outboundId.yandexId", xDocOutbound.getExternalId())
                .setValue("outboundId.partnerId", xDocOutbound.getExternalId())
                .setValue("registryId.yandexId", "1234")
                .setValue("registryId.partnerId", "1234")
                .setValue("boxId", sortableBox.getRequiredBarcodeOrThrow())
                .setValue("palletId", sortablePallet.getRequiredBarcodeOrThrow())
                .resolve();

        ScTestUtils.ffApiSuccessfulCall(mockMvc, request)
                .andExpect(content().xml(expectedResponse));
    }

    @Test
    @DisplayName("[Одноместный заказ] Отсортирован напрямую в лот - проверка наличия паллет в реестре")
    void shipLotsWithSingleOrderMiddleMile3() throws Exception {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.RETURN_OUTBOUND_ENABLED, true);
        testFactory.setConfiguration(ConfigurationProperties.SEND_PALLETS_IN_REGISTRY, true);

        var outboundLocal = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("1010")
                .type(OutboundType.ORDERS_RETURN)
                .fromTime(clock.instant())
                .toTime(clock.instant())
                .sortingCenter(sortingCenter)
                .partnerToExternalId(warehouse.getPartnerId())
                .logisticPointToExternalId(warehouse.getYandexId())
                .build()
        );

        var order = testFactory.createForToday(
                        order(sortingCenter, UUID.randomUUID().toString())
                                .dsType(DeliveryServiceType.TRANSIT)
                                .warehouseReturnId(warehouse.getYandexId())
                                .warehouseFromId(warehouse.getYandexId())
                                .build())
                .accept().sort().ship()
                .makeReturn().accept()
                .enableSortMiddleMileToLot()
                .sortToLot("SC_LOT_100000", SortableType.PALLET)
                .prepareToShipLot(1)
                .get();

        var lot = Objects.requireNonNull(testFactory.orderPlace(order).getLot());
        var sortableLot = sortableLotService.findByLotIdOrThrow(lot.getId());
        assertThat(sortableLot.getBarcode()).isNotNull();
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);

        var request = fromFile("ffapi/outbound/getOutboundRequestTemplate.xml")
                .setValue("token", sortingCenter.getToken())
                .setValue("uniq", requestUID)
                .setValue("outboundId.yandexId", outboundLocal.getExternalId())
                .setValue("outboundId.partnerId", outboundLocal.getExternalId())
                .resolve();

        var boxPalletIdXpath = "//boxes/box/unitInfo/compositeId/partialIds/partialId" +
                "/idType[contains(text(), 'PALLET_ID')]/parent::partialId/value/text()";
        var palletIdXpath = "//pallets/pallet/unitInfo/compositeId/partialIds/partialId" +
                "/idType[contains(text(), 'PALLET_ID')]/parent::partialId/value/text()";

        var resultActions = ScTestUtils.ffApiSuccessfulCall(mockMvc, request);
        resultActions.andExpect(xpath(boxPalletIdXpath).string(sortableLot.getBarcode()));
        resultActions.andExpect(xpath(palletIdXpath).string(sortableLot.getBarcode()));
    }

    @ParameterizedTest
    @EnumSource(value = LotSize.class)
    @DisplayName("[Одноместный заказ] Отсортирован напрямую в лот - проверка наличия габаритов лота в реестре")
    void shipLotsWithSingleOrderMiddleMile4(LotSize lotSize) throws Exception {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.RETURN_OUTBOUND_ENABLED, true);
        testFactory.setConfiguration(ConfigurationProperties.SEND_PALLETS_IN_REGISTRY, true);

        var outboundLocal = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("1010")
                .type(OutboundType.ORDERS_RETURN)
                .fromTime(clock.instant())
                .toTime(clock.instant())
                .sortingCenter(sortingCenter)
                .partnerToExternalId(warehouse.getPartnerId())
                .logisticPointToExternalId(warehouse.getYandexId())
                .build()
        );

        var order = testFactory.createForToday(
                        order(sortingCenter, UUID.randomUUID().toString())
                                .dsType(DeliveryServiceType.TRANSIT)
                                .warehouseReturnId(warehouse.getYandexId())
                                .warehouseFromId(warehouse.getYandexId())
                                .build())
                .accept().sort().ship()
                .makeReturn().accept()
                .enableSortMiddleMileToLot()
                .sortToLot("SC_LOT_100000", SortableType.PALLET)
                .prepareToShipLot(1)
                .get();

        var lot = Objects.requireNonNull(testFactory.orderPlace(order).getLot());
        var sortableLot = sortableLotService.findByLotIdOrThrow(lot.getId());
        assertThat(sortableLot.getBarcode()).isNotNull();
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();

        testFactory.switchLotSize(lot.getId(), lotSize);
        Long routeId = testFactory.getRouteIdForSortableFlow(route.getId());
        testFactory.shipLots(routeId, sortingCenter);

        var request = fromFile("ffapi/outbound/getOutboundRequestTemplate.xml")
                .setValue("token", sortingCenter.getToken())
                .setValue("uniq", requestUID)
                .setValue("outboundId.yandexId", outboundLocal.getExternalId())
                .setValue("outboundId.partnerId", outboundLocal.getExternalId())
                .resolve();

        var orderPalletCountQuantityXpath = "//pallets/pallet/unitInfo/counts/count/" +
                "countType[contains(text(), 'FIT')]/parent::count/quantity/text()";

        var resultActions = ScTestUtils.ffApiSuccessfulCall(mockMvc, request);
        resultActions.andExpect(xpath(orderPalletCountQuantityXpath).number((double) lotSize.getSize()));
    }

    @Test
    @DisplayName("[Одноместный заказ] Отсортирован напрямую в лот - проверка наличия направления в реестре")
    void shipLotsWithSingleOrderMiddleMile5() throws Exception {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.RETURN_OUTBOUND_ENABLED, true);
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS, true);
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS_FOR_ALL_DELIVERY_SERVICES, true);
        testFactory.setConfiguration(ConfigurationProperties.SEND_PALLETS_IN_REGISTRY, true);


        var firstHopSc = testFactory.storedSortingCenter(TestFactory.SortingCenterParams.builder()
                .id(1010101L)
                .token("first-hop-sc-token")
                .yandexId("first-hop-sc-yandex-id")
                .build());
        var destinationSc = testFactory.storedSortingCenter(2020202L);
        testFactory.storedCrossDockMapping(sortingCenter.getId(), firstHopSc.getId(), destinationSc.getId());

        var outboundLocal = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("1010")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(sortingCenter)
                .partnerToExternalId(String.valueOf(firstHopSc.getId()))
                .logisticPointToExternalId(firstHopSc.getYandexId())
                .build()
        );

        var courierWithDs = testFactory.magistralCourier(String.valueOf(destinationSc.getId()));
        var order = testFactory.createForToday(
                        order(sortingCenter, UUID.randomUUID().toString())
                                .dsType(DeliveryServiceType.TRANSIT)
                                .warehouseReturnId(warehouse.getYandexId())
                                .warehouseFromId(warehouse.getYandexId())
                                .deliveryService(courierWithDs.deliveryService())
                                .build())
                .accept().sort()
                .enableSortMiddleMileToLot()
                .sortToLot("SC_LOT_100000", SortableType.PALLET)
                .prepareToShipLot(1)
                .get();

        var lot = Objects.requireNonNull(testFactory.orderPlace(order).getLot());
        var sortableLot = sortableLotService.findByLotIdOrThrow(lot.getId());
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var user = testFactory.storedUser(sortingCenter, 1000L);

        testFactory.bindLotToOutbound(outboundLocal.getExternalId(), sortableLot.getBarcode(),
                testFactory.getRouteIdForSortableFlow(route), user);
        testFactory.shipOutbound(outboundLocal.getExternalId());

        var request = fromFile("ffapi/outbound/getOutboundRequestTemplate.xml")
                .setValue("token", sortingCenter.getToken())
                .setValue("uniq", requestUID)
                .setValue("outboundId.yandexId", outboundLocal.getExternalId())
                .setValue("outboundId.partnerId", outboundLocal.getExternalId())
                .resolve();

        var resultActions = ScTestUtils.ffApiSuccessfulCall(mockMvc, request);
        var palletDescriptionXpath = "//pallets/pallet/unitInfo/description/text()";
        resultActions.andExpect(xpath(palletDescriptionXpath).string("DESTINATION_ID:" +
                courierWithDs.courier().getId())
        );
    }

    @Test
    @DisplayName("[Одноместный заказ] Отсортирован напрямую в лот - проверка ненаправления в реестре для некроссдока")
    void shipLotsWithSingleOrderMiddleMile6() throws Exception {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.RETURN_OUTBOUND_ENABLED, true);
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS, true);
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS_FOR_ALL_DELIVERY_SERVICES, true);
        testFactory.setConfiguration(ConfigurationProperties.SEND_PALLETS_IN_REGISTRY, true);

        var firstHopSc = testFactory.storedSortingCenter(TestFactory.SortingCenterParams.builder()
                .id(1010101L)
                .token("first-hop-sc-token")
                .yandexId("first-hop-sc-yandex-id")
                .build());
        testFactory.storedCrossDockMapping(sortingCenter.getId(), firstHopSc.getId(), firstHopSc.getId());

        var outboundLocal = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("1010")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(sortingCenter)
                .partnerToExternalId(String.valueOf(firstHopSc.getId()))
                .logisticPointToExternalId(firstHopSc.getYandexId())
                .build()
        );

        var crossdockCourier = testFactory.magistralCourier(String.valueOf(firstHopSc.getId()));
        var order = testFactory.createForToday(
                        order(sortingCenter, UUID.randomUUID().toString())
                                .dsType(DeliveryServiceType.TRANSIT)
                                .warehouseReturnId(warehouse.getYandexId())
                                .warehouseFromId(warehouse.getYandexId())
                                .deliveryService(crossdockCourier.deliveryService())
                                .build())
                .accept().sort()
                .enableSortMiddleMileToLot()
                .sortToLot("SC_LOT_100000", SortableType.PALLET)
                .prepareToShipLot(1)
                .get();

        var lot = Objects.requireNonNull(testFactory.orderPlace(order).getLot());
        var sortableLot = sortableLotService.findByLotIdOrThrow(lot.getId());
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                .orElseThrow();
        var user = testFactory.storedUser(sortingCenter, 1000L);

        testFactory.bindLotToOutbound(outboundLocal.getExternalId(), sortableLot.getBarcode(),
                testFactory.getRouteIdForSortableFlow(route), user);
        testFactory.shipOutbound(outboundLocal.getExternalId());

        var request = fromFile("ffapi/outbound/getOutboundRequestTemplate.xml")
                .setValue("token", sortingCenter.getToken())
                .setValue("uniq", requestUID)
                .setValue("outboundId.yandexId", outboundLocal.getExternalId())
                .setValue("outboundId.partnerId", outboundLocal.getExternalId())
                .resolve();

        var resultActions = ScTestUtils.ffApiSuccessfulCall(mockMvc, request);
        var palletDescriptionXpath = "//pallets/pallet/unitInfo/description/text()";
        resultActions.andExpect(xpath(palletDescriptionXpath).doesNotExist());
    }

    @Test
    @DisplayName("Кроссдочный лот на промежуточном СЦ - проверка наличия в реестре отгрузки")
    void shipLotsWithSingleOrderMiddleMile7() throws Exception {
        testFactory.setConfiguration(ConfigurationProperties.SEND_PALLETS_IN_REGISTRY, true);

        var firstHopSc = testFactory.storedSortingCenter(1010101L, "first-hop-sc");

        testFactory.setSortingCenterProperty(firstHopSc.getId(),
                SortingCenterPropertiesKey.CREATE_LOTS_FROM_REGISTRY, true);
        testFactory.setSortingCenterProperty(firstHopSc.getId(),
                SortingCenterPropertiesKey.CREATE_CROSS_DOCK_SORTED, true);

        testFactory.setSortingCenterProperty(firstHopSc,
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS, true);
        testFactory.setSortingCenterProperty(firstHopSc,
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS_FOR_ALL_DELIVERY_SERVICES, true);

        var destinationSc = testFactory.storedSortingCenter(2020202L);

        var obOnFirstHopSc = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("1010777")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(firstHopSc)
                .partnerToExternalId(String.valueOf(destinationSc.getId()))
                .logisticPointToExternalId(destinationSc.getYandexId())
                .build()
        );

        var courier = testFactory.magistralCourier(String.valueOf(destinationSc.getId()));
        var crossDockLotBarcode1 = "SC_LOT_1";
        var crossDockLotBarcode2 = "SC_LOT_2";

        testFactory.createInbound(TestFactory.CreateInboundParams.builder()
                .inboundType(InboundType.DS_SC)
                .sortingCenter(firstHopSc)
                .inboundExternalId("in-1")
                .registryMap(Map.of("registry_1", List.of(Pair.of("o-1", "p-1"), Pair.of("o-2", "p-2"))))
                .placeInPallets(Map.of("p-1", crossDockLotBarcode1, "p-2", crossDockLotBarcode2))
                .crossDockPalletDestinations(Map.of(
                        crossDockLotBarcode1, courier.courier().getId(),
                        crossDockLotBarcode2, courier.courier().getId()
                )).fromDate(OffsetDateTime.now(clock))
                .toDate(OffsetDateTime.now(clock))
                .build()
        );

        var user = testFactory.storedUser(firstHopSc, 1000L);
        var route = testFactory.storedOutgoingCourierRoute(LocalDate.now(clock), firstHopSc, courier.courier());
        Long routableId =
                sortWithRouteSo()
                    ? transactionTemplate.execute( t -> testFactory.getRoutesSo(route).stream()
                        .filter(r -> !r.getRouteSoSites().isEmpty())
                        .findAny()
                        .orElseThrow()
                        .getId())
                    : route.getId();

        testFactory.bindLotToOutbound(obOnFirstHopSc.getExternalId(), crossDockLotBarcode1,
                routableId, user);
        testFactory.bindLotToOutbound(obOnFirstHopSc.getExternalId(), crossDockLotBarcode2,
                routableId, user);
        testFactory.shipOutbound(obOnFirstHopSc.getExternalId());

        var request = fromFile("ffapi/outbound/getOutboundRequestTemplate.xml")
                .setValue("token", firstHopSc.getToken())
                .setValue("uniq", requestUID)
                .setValue("outboundId.yandexId", obOnFirstHopSc.getExternalId())
                .setValue("outboundId.partnerId", obOnFirstHopSc.getExternalId())
                .resolve();

        var palletXpath1 = "//pallets/pallet/unitInfo/compositeId/partialIds/partialId" +
                "/value[contains(text(), '" + crossDockLotBarcode1 + "')]/parent::partialId/idType/text()";
        ScTestUtils.ffApiSuccessfulCall(mockMvc, request).andExpect(xpath(palletXpath1).exists());

        var palletXpath2 = "//pallets/pallet/unitInfo/compositeId/partialIds/partialId" +
                "/value[contains(text(), '" + crossDockLotBarcode2 + "')]/parent::partialId/idType/text()";
        ScTestUtils.ffApiSuccessfulCall(mockMvc, request).andExpect(xpath(palletXpath2).exists());
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

}
