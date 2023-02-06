package ru.yandex.market.sc.api.features;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.api.BaseApiControllerTest;
import ru.yandex.market.sc.api.utils.TestControllerCaller;
import ru.yandex.market.sc.core.configuration.ConfigurationProperties;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.client_return.ClientReturnService;
import ru.yandex.market.sc.core.domain.client_return.repository.ClientReturnBarcodePrefix;
import ru.yandex.market.sc.core.domain.client_return.repository.ClientReturnType;
import ru.yandex.market.sc.core.domain.inbound.model.InboundRegistryDto;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryType;
import ru.yandex.market.sc.core.domain.movement_courier.model.MovementCourierRequest;
import ru.yandex.market.sc.core.domain.order.OrderCommandService;
import ru.yandex.market.sc.core.domain.order.OrderFlowService;
import ru.yandex.market.sc.core.domain.order.model.ApiOrderDto;
import ru.yandex.market.sc.core.domain.order.model.ApiOrderStatus;
import ru.yandex.market.sc.core.domain.order.model.CreateReturnRequest;
import ru.yandex.market.sc.core.domain.order.model.DeletedSegmentRequest;
import ru.yandex.market.sc.core.domain.order.model.OrderReturnType;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.order.model.UpdateSegmentRequest;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.outbound.OutboundCommandService;
import ru.yandex.market.sc.core.domain.outbound.OutboundQueryService;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundCreateRequest;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundInfo;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundType;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundStatus;
import ru.yandex.market.sc.core.domain.place.misc.PlaceHistoryTestHelper;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.route.RouteFacade;
import ru.yandex.market.sc.core.domain.route.model.FinishRouteRequestDto;
import ru.yandex.market.sc.core.domain.scan.model.AcceptOrderRequestDto;
import ru.yandex.market.sc.core.domain.scan.model.SortableSortRequestDto;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.stage.Stages;
import ru.yandex.market.sc.core.domain.user.model.UserRole;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.warehouse.model.WarehouseType;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.model.LocationDto;
import ru.yandex.market.sc.internal.model.WarehouseDto;
import ru.yandex.market.tpl.common.db.configuration.ConfigurationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN;
import static ru.yandex.market.sc.core.domain.order.repository.FakeOrderType.CLIENT_RETURN;

/**
 * @author: dbryndin
 * @date: 12/22/21
 */
public class ClientReturnTestFlow extends BaseApiControllerTest {

    @MockBean
    Clock clock;
    SortingCenter sortingCenter;
    SortingCenter sortingCenter2;
    User user;
    Cell cell;
    TestControllerCaller caller;

    @Autowired
    private OrderCommandService orderCommandService;
    @Autowired
    private ScOrderRepository scOrderRepository;
    @Autowired
    private RouteFacade routeFacade;
    @Autowired
    private OutboundCommandService outboundCommandService;
    @Autowired
    private OutboundQueryService outboundQueryService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private PlaceHistoryTestHelper placeHistoryHelper;

    @BeforeEach
    void init() throws JsonProcessingException {
        sortingCenter = testFactory.storedSortingCenter();
        user = testFactory.storedUser(sortingCenter, 1L);
        cell = testFactory.storedActiveCell(sortingCenter);
        testFactory.storedUser(sortingCenter, UID, UserRole.STOCKMAN);
        testFactory.setupMockClock(clock);
        testFactory.storedCourier(-1, ClientReturnService.CLIENT_RETURN_COURIER);
        caller = TestControllerCaller.createCaller(mockMvc, UID);
    }

    private final LocationDto MOCK_WAREHOUSE_LOCATION = LocationDto.builder()
            .country("Россия")
            .region("Москва и Московская область")
            .locality("Котельники")
            .build();

    private static Set<String> getClientBarcodes() {
        var barcode = "1234567890";
        var barcodes = new HashSet<String>();
        barcodes.add(barcode);
        for (var prefix : ClientReturnBarcodePrefix.values()) {
            barcodes.addAll(prefix.getPrefixes().stream().map(p -> p + barcode).collect(Collectors.toSet()));
        }
        return barcodes;
    }

    @Test
    @DisplayName("клиентский возврат без префикса отгружается на склад в реестре клиентских возвратов")
    public void clientReturnWithoutPrefixPassedToClientReturnOutboundRegistry() {
        var time = Instant.parse("2021-06-01T12:00:00Z");
        testFactory.setupMockClock(clock, time);

        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.RETURN_OUTBOUND_ENABLED, true);
        testFactory.storedFakeReturnDeliveryService();
        var place = createClientReturnAndCheck(WarehouseType.SHOP, "no_prefix");
        testFactory.acceptPlace(place);
        testFactory.sortPlace(place);

        place = testFactory.updated(place);
        var route = testFactory.findOutgoingWarehouseRoute(place).orElseThrow();

        var outboundId = "outbound-1";

        outboundCommandService.put(OutboundCreateRequest.builder()
                .externalId(outboundId)
                .type(OutboundType.ORDERS_RETURN)
                .fromTime(time)
                .toTime(time)
                .courierRequest(
                        new MovementCourierRequest(
                                "fffdjhwkh3j4jgkbc",
                                "Courier name",
                                "Courier legal name",
                                "О868АС198",
                                212_85_06L,
                                "phone2345"
                        )
                )
                .locationCreateRequest(
                        TestFactory.locationCreateRequest()
                )
                .comment("Please, do your best")
                .sortingCenter(sortingCenter)
                .type(OutboundType.ORDERS_RETURN)
                .logisticPointToExternalId(Objects.requireNonNull(route.getWarehouseTo()).getYandexId())
                .build());

        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, place.getCell());
        testFactory.sortPlaceToLot(place, lot, user);
        lot = testFactory.prepareToShipLot(lot);
        testFactory.shipLotRouteByParentCell(lot);

        OutboundInfo outboundInfo = outboundQueryService.getOutboundInfoBy(outboundId);
        // шипнется потом dbqueue таской
        assertThat(outboundInfo.getOutbound().getStatus()).isEqualTo(OutboundStatus.CREATED);
        Map<RegistryType, InboundRegistryDto> registries = outboundInfo.getOrderRegistries().stream()
                .collect(Collectors.toMap(InboundRegistryDto::getType, registry -> registry));
        assertThat(registries).hasSize(1);
        assertThat(registries.get(RegistryType.FACTUAL_DELIVERED_ORDERS_RETURN).getOrders()).hasSize(1);
    }

    /**
     * создание-приемка-сортировка-отгрузка клиентского возврата
     * - тип возвратного склада SHOP
     * - фешн идет через ячейку с поддитоп DEFAULT, все остальные клиентские возвраты идут в ячейки CLIENT_RETURN
     * возратов
     */
    @ParameterizedTest
    @DisplayName("success создание-приемка-сортировка-отгрузка клиентского возврата через ячейку")
    @MethodSource("getClientBarcodes")
    public void successClientReturn0(String barcode) throws Exception {
        testFactory.storedFakeReturnDeliveryService();
        createClientReturnAndCheck(WarehouseType.SHOP, barcode);

        var acceptDto = acceptClientReturn(barcode);
        assertThat(acceptDto.getStatus()).isEqualTo(ApiOrderStatus.SORT_TO_WAREHOUSE);
        assertThat(acceptDto.getAvailableCells()).isNotEmpty();
        var cellToSort = acceptDto.getAvailableCells().stream().findFirst().get();

        var routeId = testFactory.findOutgoingWarehouseRoute(acceptDto.getId()).get().getId();

        var outgoingRouteMultiCellsDto = routeFacade.getApiOutgoingRouteDto(
                null, testFactory.getRouteIdForSortableFlow(routeId), sortingCenter);
        assertThat(outgoingRouteMultiCellsDto).isNotNull();

        var acceptedPlace = testFactory.orderPlace(acceptDto.getId());

        if (acceptedPlace.getClientReturnType().isPresent() &&
                acceptedPlace.getClientReturnType().get() == ClientReturnType.FASHION) {
            assertThat(cellToSort.getSubType()).isEqualTo(CellSubType.DEFAULT);
        } else {
            assertThat(cellToSort.getSubType()).isEqualTo(CellSubType.CLIENT_RETURN);
        }

        assertThat(acceptedPlace.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);

        sort(acceptedPlace.getExternalId(), cellToSort.getId());

        var sortedPlace = testFactory.orderPlace(acceptDto.getId());
        assertThat(sortedPlace.getFfStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(sortedPlace.getCell().getId()).isEqualTo(cellToSort.getId());

        ship(sortedPlace.getOrderId(), cellToSort.getId());
        var shippedPlace = testFactory.orderPlace(acceptDto.getId());
        assertEquals(RETURNED_ORDER_DELIVERED_TO_IM, shippedPlace.getOrderStatus());
    }

    /**
     * создание-приемка-сортировка-отгрузка клиентского возврата через АХ
     * - свойство BUFFER_RETURNS_ENABLED включено
     * - тип возвратного склада SHOP
     * - фешн идет через ячейку с поддитоп DEFAULT, все остальные клиентские возвраты идут в ячейки CLIENT_RETURN
     */
    @ParameterizedTest
    @DisplayName("success создание-приемка-сортировка-отгрузка клиентского возврата ")
    @MethodSource("getClientBarcodes")
    public void successClientReturn2(String barcode) throws Exception {
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, "true");
        testFactory.storedFakeReturnDeliveryService();
        createClientReturnAndCheck(WarehouseType.SHOP, barcode);

        var acceptDto = acceptClientReturn(barcode);
        assertThat(acceptDto.getStatus()).isEqualTo(ApiOrderStatus.KEEP_TO_WAREHOUSE);
        assertThat(acceptDto.getAvailableCells()).isNotEmpty();
        var brCellToSort = acceptDto.getAvailableCells().stream().findFirst().get();

        var routeId = testFactory.getRouteIdForSortableFlow(
                testFactory.findOutgoingWarehouseRoute(acceptDto.getId()).get().getId()
        );
        var outgoingRouteDto = routeFacade.getApiOutgoingRouteDto(null, routeId, sortingCenter);

        assertThat(outgoingRouteDto).isNotNull();
        var acceptedPlace = testFactory.orderPlace(acceptDto.getId());

        assertThat(brCellToSort.getSubType()).isEqualTo(CellSubType.BUFFER_RETURNS);

        assertThat(acceptedPlace.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);

        // сортируем в АХ
        sort(acceptedPlace.getExternalId(), brCellToSort.getId());

        var sortedPlaceToBufferReturns = testFactory.orderPlace(acceptDto.getId());
        assertThat(sortedPlaceToBufferReturns.getFfStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(sortedPlaceToBufferReturns.getCell().getId()).isEqualTo(brCellToSort.getId());

        // сканируем в АХ
        var acceptDtoFromBR = accept(barcode);
        assertThat(acceptDtoFromBR.getStatus()).isEqualTo(ApiOrderStatus.SORT_TO_WAREHOUSE);
        assertThat(acceptDtoFromBR.getAvailableCells()).isNotEmpty();
        var rCellToSort = acceptDtoFromBR.getAvailableCells().stream().findFirst().get();
        if (acceptedPlace.getClientReturnType().isPresent() &&
                acceptedPlace.getClientReturnType().get() == ClientReturnType.FASHION) {
            assertThat(rCellToSort.getSubType()).isEqualTo(CellSubType.DEFAULT);
        } else {
            assertThat(rCellToSort.getSubType()).isEqualTo(CellSubType.CLIENT_RETURN);
        }

        sort(acceptDtoFromBR.getExternalId(), rCellToSort.getId());

        var sortedPlace = testFactory.orderPlace(acceptDtoFromBR.getId());
        assertThat(sortedPlace.getFfStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(sortedPlace.getCell().getId()).isEqualTo(rCellToSort.getId());

        ship(sortedPlace.getOrderId(), rCellToSort.getId());

        var shippedPlace = testFactory.orderPlace(sortedPlace.getOrderId());
        assertEquals(RETURNED_ORDER_DELIVERED_TO_IM, shippedPlace.getOrderStatus());
    }

    /**
     * создание-приемка-сортировка-отгрузка клиентского возврата
     * - тип возвратного склада DROPOFF
     * - фешн идет через ячейку с поддитоп DEFAULT, все остальные клиентские возвраты идут в ячейки CLIENT_RETURN
     * возратов
     */
    @ParameterizedTest
    @DisplayName("success создание-приемка-сортировка-отгрузка клиентского возврата через ячейку")
    @MethodSource("getClientBarcodes")
    public void successClientReturn3(String barcode) throws Exception {
        testFactory.storedFakeReturnDeliveryService();
        createClientReturnAndCheck(WarehouseType.DROPOFF, barcode);

        var acceptDto = acceptClientReturn(barcode);
        assertThat(acceptDto.getStatus()).isEqualTo(ApiOrderStatus.SORT_TO_WAREHOUSE);
        assertThat(acceptDto.getAvailableCells()).isNotEmpty();
        var cellToSort = acceptDto.getAvailableCells().stream().findFirst().get();

        var acceptedPlace = testFactory.orderPlace(acceptDto.getId());

        var routeId = testFactory.getRouteIdForSortableFlow(
                testFactory.findOutgoingWarehouseRoute(acceptDto.getId()).orElseThrow()
        );
        var outgoingRouteDto = routeFacade.getApiOutgoingRouteDto(null, routeId, sortingCenter);
        assertThat(outgoingRouteDto).isNotNull();

        if (acceptedPlace.getClientReturnType().isPresent() &&
                acceptedPlace.getClientReturnType().get() == ClientReturnType.FASHION) {
            assertThat(cellToSort.getSubType()).isEqualTo(CellSubType.DEFAULT);
        } else {
            assertThat(cellToSort.getSubType()).isEqualTo(CellSubType.CLIENT_RETURN);
        }

        assertThat(acceptedPlace.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);

        sort(acceptedPlace.getExternalId(), cellToSort.getId());

        var sortedPlace = testFactory.orderPlace(acceptDto.getId());
        assertThat(sortedPlace.getFfStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(sortedPlace.getCell().getId()).isEqualTo(cellToSort.getId());

        ship(sortedPlace.getOrderId(), cellToSort.getId());

        var shippedPlace = testFactory.orderPlace(sortedPlace.getOrderId());
        assertEquals(RETURNED_ORDER_DELIVERED_TO_IM, shippedPlace.getOrderStatus());
    }

    /**
     * создание-приемка-сортировка-отгрузка клиентского возврата
     * - тип возвратного склада WAREHOUSE
     * - фешн идет через ячейку с поддитоп DEFAULT, все остальные клиентские возвраты идут в ячейки CLIENT_RETURN
     * возратов
     */
    @ParameterizedTest
    @DisplayName("success создание-приемка-сортировка-отгрузка клиентского возврата через ячейку")
    @MethodSource("getClientBarcodes")
    public void successClientReturn4(String barcode) throws Exception {
        testFactory.storedFakeReturnDeliveryService();
        createClientReturnAndCheck(WarehouseType.SORTING_CENTER, barcode);

        var acceptDto = acceptClientReturn(barcode);
        assertThat(acceptDto.getStatus()).isEqualTo(ApiOrderStatus.SORT_TO_WAREHOUSE);
        assertThat(acceptDto.getAvailableCells()).isNotEmpty();
        var cellToSort = acceptDto.getAvailableCells().stream().findFirst().get();

        var acceptedPlace = testFactory.orderPlace(acceptDto.getId());
        var routeId = testFactory.getRouteIdForSortableFlow(
                testFactory.findOutgoingWarehouseRoute(acceptDto.getId()).get().getId()
        );
        var outgoingRouteDto = routeFacade.getApiOutgoingRouteDto(null, routeId, sortingCenter);
        assertThat(outgoingRouteDto).isNotNull();
        if (acceptedPlace.getClientReturnType().isPresent() &&
                acceptedPlace.getClientReturnType().get() == ClientReturnType.FASHION) {
            assertThat(cellToSort.getSubType()).isEqualTo(CellSubType.DEFAULT);
        } else {
            assertThat(cellToSort.getSubType()).isEqualTo(CellSubType.CLIENT_RETURN);
        }

        assertThat(acceptedPlace.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);

        sort(acceptedPlace.getExternalId(), cellToSort.getId());

        var sortedPlace = testFactory.orderPlace(acceptDto.getId());
        assertThat(sortedPlace.getFfStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(sortedPlace.getCell().getId()).isEqualTo(cellToSort.getId());

        ship(sortedPlace.getOrderId(), cellToSort.getId());

        var shippedPlace = testFactory.orderPlace(sortedPlace.getOrderId());
        assertEquals(RETURNED_ORDER_DELIVERED_TO_IM, shippedPlace.getOrderStatus());
    }

    @Test
    @DisplayName("success создание-приемка-сортировка-отгрузка-обновление сегмента-приемка-сортировка-отгрузка " +
            "клиентского возврата через ячейку")
    public void successClientReturn5() throws Exception {
        var barcode = "1234567890";
        testFactory.storedFakeReturnDeliveryService();
        createClientReturnAndCheck(WarehouseType.SHOP, barcode);
        var shippedPlace = acceptSortShip(barcode);
        var newReturnWarehouse = WarehouseDto.builder()
                .type(WarehouseType.SHOP.name())
                .yandexId("123123123")
                .logisticPointId("123123123")
                .incorporation("ООО new ретурн мерчант")
                .location(MOCK_WAREHOUSE_LOCATION)
                .shopId("444")
                .build();
        var newSegmentUuid = UUID.randomUUID().toString();
        String newCargoUnitId = "1aaaa123";

        orderCommandService.updateClientReturnSegment(UpdateSegmentRequest.builder()
                .orderId(shippedPlace.getOrderId())
                .segmentUuid(newSegmentUuid)
                .cargoUnitId(newCargoUnitId)
                .warehouseTo(newReturnWarehouse).build(), user);

        var updatedPlace = testFactory.updated(shippedPlace);
        assertThat(updatedPlace.getFfStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        assertThat(updatedPlace.getSegmentUid()).isEqualTo(newSegmentUuid);
        assertThat(updatedPlace.getCargoUnitId()).isEqualTo(newCargoUnitId);

        var yetShippedPlace = acceptSortShip(barcode);

        assertThat(yetShippedPlace.getWarehouseReturn().getYandexId()).isEqualTo(newReturnWarehouse.getYandexId());
        assertThat(yetShippedPlace.getSegmentUid()).isEqualTo(newSegmentUuid);
    }

    @Test
    @DisplayName("success коробка уже лежит ")
    public void successClientReturnUpdateSameWarehouse() throws Exception {
        var barcode = "1234567890";
        testFactory.storedFakeReturnDeliveryService();
        var place = createClientReturnAndCheck(WarehouseType.SHOP, barcode);
        testFactory.acceptPlace(place);
        testFactory.sortPlace(place);
        var newReturnWarehouse = WarehouseDto.builder()
                .type(place.getWarehouseReturn().getType().name())
                .yandexId(place.getWarehouseReturn().getYandexId())
                .incorporation(place.getWarehouseReturn().getIncorporation())
                .location(MOCK_WAREHOUSE_LOCATION)
                .shopId(place.getWarehouseReturn().getShopId())
                .build();
        var newSegmentUuid = UUID.randomUUID().toString();
        String newCargoUnitId = "1aaaa123";
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.updateClientReturnSegment(UpdateSegmentRequest.builder()
                .orderId(place.getOrderId())
                .segmentUuid(newSegmentUuid)
                .cargoUnitId(newCargoUnitId)
                .warehouseTo(newReturnWarehouse).build(), user);

        placeHistoryHelper.validateThatOneRecordWithUserCollected(user);
        place = testFactory.updated(place);
        assertThat(place.getFfStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(place.getSegmentUid()).isEqualTo(newSegmentUuid);
        assertThat(place.getCargoUnitId()).isEqualTo(newCargoUnitId);
        assertThat(place.getSortableStatus()).isEqualTo(SortableStatus.SORTED_RETURN);

        var shippedOrder = testFactory.shipOrderRoute(place);
        assertThat(shippedOrder.getFfStatus()).isEqualTo(RETURNED_ORDER_DELIVERED_TO_IM);
    }


    @Test
    @DisplayName("success смена склада возврата")
    public void successClientReturnChangeWh() throws Exception {
        var barcode = "1234567890";
        testFactory.storedFakeReturnDeliveryService();
        var newWh = testFactory.storedWarehouse("5555");
        var place = createClientReturnAndCheck(WarehouseType.SHOP, barcode);
        testFactory.acceptPlace(place);
        testFactory.sortPlace(place);
        var newReturnWarehouse = WarehouseDto.builder()
                .type(place.getWarehouseReturn().getType().name())
                .yandexId(newWh.getYandexId())
                .incorporation("новый мерчант")
                .location(MOCK_WAREHOUSE_LOCATION)
                .shopId(place.getWarehouseReturn().getShopId())
                .build();
        var newSegmentUuid = UUID.randomUUID().toString();
        String newCargoUnitId = "1aaaa123";
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.updateReturnSegment(UpdateSegmentRequest.builder()
                .orderId(place.getOrderId())
                .placeId(place.getId())
                .segmentUuid(newSegmentUuid)
                .timeOut(Instant.now(clock))
                .cargoUnitId(newCargoUnitId)
                .warehouseTo(newReturnWarehouse).build());

        place = testFactory.updated(place);
        assertThat(place.getSortableStatus()).isEqualTo(SortableStatus.ACCEPTED_RETURN);
        assertThat(place.getStageId()).isEqualTo(Stages.AWAITING_SORT_RETURN.getId());
        assertThat(place.getSegmentUid()).isEqualTo(newSegmentUuid);
        assertThat(place.getCargoUnitId()).isEqualTo(newCargoUnitId);

        var a = accept(place.getExternalId());
        assertThat(a.getStatus()).isEqualTo(ApiOrderStatus.SORT_TO_WAREHOUSE);
        assertThat(a.getWarehouse().getId()).isEqualTo(newWh.getId());
    }

    @Test
    @Disabled("MARKETTPLSC-5120")
    @DisplayName("success если заказ был на СЦ в статусе DELETED то обрабатываем его как засыл")
    public void successCreateUnexpectedOrder() throws Exception {
        String warehouseReturnYandexId = "10001";
        testFactory.storedWarehouse(warehouseReturnYandexId, WarehouseType.SHOP);
        sortingCenter2 = testFactory.storedSortingCenter(75001L);
//        testFactory.storedWarehouse("10001700279", WarehouseType.SHOP);

        var misdeliveryProcessingScs = Map.of(
                sortingCenter.getId(),
                new OrderFlowService.MisdeliveryProcessingProperties("MSK", 1,
                        OrderFlowService.MisdeliveryReturnDirection.SORTING_CENTER, warehouseReturnYandexId, List.of()
                ),
                sortingCenter2.getId(),
                new OrderFlowService.MisdeliveryProcessingProperties("MSK", 1,
                        OrderFlowService.MisdeliveryReturnDirection.SORTING_CENTER, warehouseReturnYandexId, List.of()
                )
        );

        configurationService.insertValue(
                ConfigurationProperties.MISDELIVERY_RETURNS_MAPPINGS,
                new ObjectMapper().writeValueAsString(misdeliveryProcessingScs)
        );


        var barcode = "1234567890";
        testFactory.storedFakeReturnDeliveryService();
        // создаем заказ засыльном СЦ
        var clientReturn = createClientReturnAndCheck(WarehouseType.SHOP, barcode, sortingCenter,
                "10001700279");

        // удаляем его
        orderCommandService.deleteSegmentUUI(DeletedSegmentRequest.builder()
                .cargoUnitId(clientReturn.getCargoUnitId())
                .segmentUuid(clientReturn.getSegmentUid())
                .build());

        // создаем заказ в целевом СЦ
        createClientReturnAndCheck(WarehouseType.SHOP, barcode, sortingCenter2, "10001700279");

        clientReturn = testFactory.updated(clientReturn);
        assertThat(clientReturn.getSegmentUid()).isNull();
        assertThat(clientReturn.getFfStatus()).isEqualTo(ScOrderFFStatus.DELETED);
        assertThat(clientReturn.getStatus()).isEqualTo(PlaceStatus.DELETED);
        assertThat(clientReturn.getSortableStatus()).isEqualTo(SortableStatus.DELETED);

        // принимаем и отгружаем заказ обратно
        acceptSortShip(barcode);
        clientReturn = testFactory.updated(clientReturn);
        assertThat(clientReturn.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        assertThat(clientReturn.getWarehouseReturn().getYandexId()).isEqualTo(warehouseReturnYandexId);
    }

    private Place acceptSortShip(String barcode) throws Exception {
        var acceptDto = acceptClientReturn(barcode);
        assertThat(acceptDto.getStatus()).isEqualTo(ApiOrderStatus.SORT_TO_WAREHOUSE);
        assertThat(acceptDto.getAvailableCells()).isNotEmpty();
        var cellToSort = acceptDto.getAvailableCells().stream().findFirst().get();

        var routeId = testFactory.findOutgoingWarehouseRoute(acceptDto.getId()).get().getId();
        var outgoingRouteDto = routeFacade.getApiOutgoingRouteDto(null, routeId, sortingCenter);
        assertThat(outgoingRouteDto).isNotNull();

        var acceptedPlace = testFactory.orderPlace(acceptDto.getId());
        if (acceptedPlace.getClientReturnType().isPresent() &&
                acceptedPlace.getClientReturnType().get() == ClientReturnType.FASHION) {
            assertThat(cellToSort.getSubType()).isEqualTo(CellSubType.DEFAULT);
        } else {
            assertThat(cellToSort.getSubType()).isEqualTo(CellSubType.CLIENT_RETURN);
        }
        assertThat(acceptedPlace.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);

        sort(acceptedPlace.getExternalId(), cellToSort.getId());

        var sortedPlace = testFactory.orderPlace(acceptDto.getId());
        assertThat(sortedPlace.getFfStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(sortedPlace.getCell().getId()).isEqualTo(cellToSort.getId());

        ship(sortedPlace.getOrderId(), cellToSort.getId());
        var shippedPlace = testFactory.orderPlace(sortedPlace.getOrderId());
        assertEquals(RETURNED_ORDER_DELIVERED_TO_IM, shippedPlace.getOrderStatus());
        return shippedPlace;
    }


    private ApiOrderDto acceptClientReturn(String externalId) throws Exception {
        var acceptReturn0 = caller.acceptReturn(new AcceptOrderRequestDto(
                        externalId,
                        null))
                .andExpect(status().is2xxSuccessful());

        return readContentAsClass(acceptReturn0, ApiOrderDto.class);
    }

    private ApiOrderDto accept(String externalId) throws Exception {
        var acceptReturn0 = caller.acceptOrder(new AcceptOrderRequestDto(
                        externalId,
                        null))
                .andExpect(status().is2xxSuccessful());

        return readContentAsClass(acceptReturn0, ApiOrderDto.class);
    }

    private void sort(String orderExternalId, Long cellId) throws Exception {
        SortableSortRequestDto request = new SortableSortRequestDto(
                orderExternalId,
                orderExternalId,
                String.valueOf(cellId));
        caller.sortableBetaSort(request)
                .andExpect(status().is2xxSuccessful());
    }

    private void ship(Long orderId, Long cellId) throws Exception {
        var shipDtoReq = new FinishRouteRequestDto();
        shipDtoReq.setCellId(cellId);
        Long routableId = testFactory.getRouteIdForSortableFlow(
                testFactory.findOutgoingWarehouseRoute(orderId)
                .get().getId());
        caller.ship(routableId, shipDtoReq)
                .andExpect(status().is2xxSuccessful());
    }

    private Place createClientReturnAndCheck(WarehouseType whType, String externalId) {
        return createClientReturnAndCheck(whType, externalId, sortingCenter, "222222");
    }

    private Place createClientReturnAndCheck(WarehouseType whType, String externalId,
                                             SortingCenter sortingCenter, String whReturnYandexId) {
        var cargoUnitId = "1aaaa123";
        var segmentUuid = "1123-12asdf-sadf-3213";
        var fromWarehouse = WarehouseDto.builder()
                .type(whType.name())
                .yandexId("123123")
                .logisticPointId("123123")
                .incorporation("ООО фром мерчант")
                .location(MOCK_WAREHOUSE_LOCATION)
                .shopId("123")
                .build();
        var returnWarehouse = WarehouseDto.builder()
                .type(whType.name())
                .yandexId(whReturnYandexId)
                .logisticPointId(whReturnYandexId)
                .incorporation("ООО ретурн мерчант")
                .location(MOCK_WAREHOUSE_LOCATION)
                .shopId("10001700279")
                .build();

        orderCommandService.createReturn(CreateReturnRequest.builder()
                        .sortingCenter(sortingCenter)
                        .orderBarcode(externalId)
                        .returnDate(LocalDate.now(clock))
                        .returnWarehouse(returnWarehouse)
                        .fromWarehouse(fromWarehouse)
                        .segmentUuid(segmentUuid)
                        .cargoUnitId(cargoUnitId)
                        .timeIn(Instant.now(clock))
                        .timeOut(Instant.now(clock))
                        .orderReturnType(OrderReturnType.CLIENT_RETURN)
                        .assessedCost(new BigDecimal(10_000))
                        .build()
                , user);
        var clientReturn = scOrderRepository.findBySortingCenterAndExternalId(
                sortingCenter, externalId
        ).orElseThrow();
        assertThat(clientReturn.getOrderStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        assertThat(clientReturn.isClientReturn()).isTrue();
        assertThat(clientReturn.getFakeOrderType()).isEqualTo(CLIENT_RETURN);
        assertThat(clientReturn.getSegmentUid()).isEqualTo(segmentUuid);
        assertThat(clientReturn.getCargoUnitId()).isEqualTo(cargoUnitId);

        // проверка fromWarehouse
        assertThat(clientReturn.getWarehouseFrom().getYandexId()).isEqualTo(fromWarehouse.getYandexId());
        assertThat(clientReturn.getWarehouseFrom().getType().name()).isEqualTo(fromWarehouse.getType());
        if (clientReturn.getWarehouseFrom().getType() == WarehouseType.SHOP) {
            assertThat(clientReturn.getWarehouseFrom().getShopId()).isEqualTo(fromWarehouse.getShopId());
        }
        // проверка returnWarehouse
        assertThat(clientReturn.getWarehouseReturn().getYandexId()).isEqualTo(returnWarehouse.getYandexId());
        assertThat(clientReturn.getWarehouseReturn().getType().name()).isEqualTo(returnWarehouse.getType());
        if (clientReturn.getWarehouseReturn().getType() == WarehouseType.SHOP) {
            assertThat(clientReturn.getWarehouseReturn().getShopId()).isEqualTo(returnWarehouse.getShopId());
        }
        return testFactory.orderPlace(clientReturn);
    }

}
