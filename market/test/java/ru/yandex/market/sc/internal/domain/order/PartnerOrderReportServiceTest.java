package ru.yandex.market.sc.internal.domain.order;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.order.model.ScOrderState;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.partner.order.PartnerOrderParamsDto;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.route.RouteQueryService;
import ru.yandex.market.sc.core.domain.route.model.RouteType;
import ru.yandex.market.sc.core.domain.scan.ScanService;
import ru.yandex.market.sc.core.domain.scan.model.SortableSortRequestDto;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLotService;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.resolver.dto.ScContext;
import ru.yandex.market.sc.core.test.DefaultScUserWarehouseExtension;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;
import ru.yandex.market.sc.internal.controller.dto.PartnerOrderDto;
import ru.yandex.market.sc.internal.model.CourierDto;
import ru.yandex.market.sc.internal.test.EmbeddedDbIntTest;
import ru.yandex.market.sc.internal.util.ScIntControllerCaller;
import ru.yandex.market.tpl.common.db.configuration.ConfigurationService;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@EmbeddedDbIntTest
@ExtendWith(DefaultScUserWarehouseExtension.class)
class PartnerOrderReportServiceTest {

    @Autowired
    PartnerOrderReportService partnerOrderReportService;

    @Autowired
    TestFactory testFactory;
    @Autowired
    ScanService scanService;
    @Autowired
    RouteQueryService routeQueryService;
    @Autowired
    Clock clock;
    @Autowired
    ScOrderRepository orderRepository;
    @Autowired
    SortableLotService sortableLotService;
    @Autowired
    XDocFlow flow;
    @Autowired
    ScIntControllerCaller caller;
    @Autowired
    ConfigurationService configurationService;
    @Autowired
    JdbcTemplate jdbcTemplate;
    private SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.XDOC_ENABLED, "false");
    }

    @Test
    void getMultipleOrder() {
        List<ScOrder> orders = List.of(
                testFactory.createForToday(
                                order(sortingCenter).externalId("0").places("o5p1", "o5p2").build())
                        .get()
        );
        assertOrdersEqual(orders, getReportOrders(
                PartnerOrderParamsDto.builder().build()
        ));
    }

    @Test
    void getOrderWithIdFullFilter() {
        List<ScOrder> orders =
                List.of(testFactory.createForToday(order(sortingCenter).externalId("0").build()).get());
        assertOrdersEqual(orders, getReportOrders(PartnerOrderParamsDto.builder().id("0").build()));
    }

    @Test
    void getOrderWithIdPartialFilter() {
        List<ScOrder> orders = List.of(
                testFactory.createForToday(order(sortingCenter).externalId("abcde").build()).get()
        );
        assertOrdersEqual(orders, getReportOrders(PartnerOrderParamsDto.builder().id("abcd").build()));
    }

    @Test
    void getOrderWithCellNumberFullFilter() {
        testFactory.storedCell(sortingCenter, "c0", CellType.COURIER);
        List<ScOrder> orders = List.of(testFactory.createOrderForToday(sortingCenter).accept().sort().get());
        assertOrdersEqual(orders, getReportOrders(PartnerOrderParamsDto.builder().cellNumber("c0").build()));
    }

    @Test
    void getOrderWithCellNumberPartialFilter() {
        testFactory.storedCell(sortingCenter, "abcde", CellType.COURIER);
        assertThat(getReportOrders(PartnerOrderParamsDto.builder().cellNumber("bcd").build())).isEmpty();
    }

    @Test
    void getOrdersWithRouteFilter() {
        testFactory.createForToday(order(sortingCenter).externalId("0").build()).cancel().get();
        List<ScOrder> orders = List.of(
                testFactory.createForToday(order(sortingCenter).externalId("1").build()).get(),
                testFactory.createForToday(order(sortingCenter).externalId("2").build()).accept().get(),
                testFactory.createForToday(order(sortingCenter).externalId("3").build()).accept().sort().get(),
                testFactory.createForToday(order(sortingCenter).externalId("4").build()).accept().sort().shipPlace("4").get(),
                testFactory.createForToday(
                                order(sortingCenter).externalId("5").places("o5p1", "o5p2").build())
                        .get()
        );
        var route = testFactory.findPossibleIncomingWarehouseRoute(orders.get(0)).orElseThrow();
        assertOrdersEqual(orders, getReportOrders(
                PartnerOrderParamsDto.builder().routeId(testFactory.getRouteIdForSortableFlow(route)).build()
        ));
    }

    @Test
    void getOrdersWithRouteAndStateFilter() {
        testFactory.createForToday(order(sortingCenter).externalId("0").build()).cancel().get();
        testFactory.createForToday(order(sortingCenter).externalId("1").build()).get();
        List<ScOrder> orders = List.of(
                testFactory.createForToday(order(sortingCenter).externalId("2").build()).accept().get(),
                testFactory.createForToday(order(sortingCenter).externalId("3").build()).accept().sort().get(),
                testFactory.createForToday(order(sortingCenter).externalId("4").build()).accept().sort().shipPlace("4").get()
        );
        var route = testFactory.findPossibleIncomingWarehouseRoute(orders.get(0)).orElseThrow();
        assertOrdersEqual(orders, getReportOrders(
                PartnerOrderParamsDto.builder().routeId(testFactory.getRouteIdForSortableFlow(route)).state(List.of(ScOrderState.ACCEPTED)).build()
        ));
    }

    @Test
    void getOrdersWithCurrentRouteAndStateFilter() {
        testFactory.createForToday(order(sortingCenter).externalId("0").build()).cancel().get();
        testFactory.createForToday(order(sortingCenter).externalId("1").build()).get();
        testFactory.createForToday(order(sortingCenter).externalId("2").build()).accept().get();
        List<ScOrder> orders = List.of(
                testFactory.createForToday(order(sortingCenter).externalId("3").build()).accept().sort().get()
        );
        testFactory.createForToday(order(sortingCenter).externalId("4").build()).accept().sort().shipPlace("4").get();

        var route = testFactory.findOutgoingCourierRoute(orders.get(0)).orElseThrow();
        assertOrdersEqual(orders, getReportOrders(
                PartnerOrderParamsDto.builder().routeId(testFactory.getRouteIdForSortableFlow(route))
                        .state(List.of(ScOrderState.SORTED)).routeOnlyCurrent(true).build()
        ));
    }

    @Test
    void getOrdersAcceptedButNotSorted() {
        testFactory.createForToday(order(sortingCenter).externalId("0").build()).cancel().get();
        testFactory.createForToday(order(sortingCenter).externalId("1").build()).get();
        List<ScOrder> orders = List.of(
                testFactory.createForToday(order(sortingCenter).externalId("2").build()).accept().get(),
                testFactory.createForToday(order(sortingCenter).externalId("3").build()).accept().get()
        );
        testFactory.createForToday(order(sortingCenter).externalId("4").build()).accept().sort().get();
        testFactory.createForToday(order(sortingCenter).externalId("5").build()).accept().sort().ship().get();
        var routeIncoming = testFactory.findPossibleIncomingWarehouseRoute(orders.get(0)).orElseThrow();
        Long routableId = testFactory.getRouteIdForSortableFlow(routeIncoming);
        List<PartnerOrderDto> reportOrders = getReportOrders(
                PartnerOrderParamsDto.builder().routeId(routableId)
                        .state(List.of(ScOrderState.ACCEPTED)).routeOnlyCurrent(false).strictState(true).build()
        );
        assertOrdersEqual(orders, reportOrders);

    }

    @Test
    void getOrdersInCellRightNowForIncomingRoute() {
        testFactory.createForToday(order(sortingCenter).externalId("0").build()).cancel().get();
        testFactory.createForToday(order(sortingCenter).externalId("1").build()).get();
        testFactory.createForToday(order(sortingCenter).externalId("2").build()).accept().get();
        testFactory.createForToday(order(sortingCenter).externalId("3").build()).accept().get();
        List<ScOrder> orders = List.of(
                testFactory.createForToday(order(sortingCenter).externalId("4").build()).accept().sort().get(),
                testFactory.createForToday(order(sortingCenter).externalId("5").build()).accept().sort().get(),
                testFactory.createForToday(order(sortingCenter).externalId("6").build()).accept().sort().get()
        );
        testFactory.createForToday(order(sortingCenter).externalId("7").build()).accept().sort().shipPlace("7").get();
        testFactory.createForToday(order(sortingCenter).externalId("8").build()).accept().sort().shipPlace("8").get();
        var routeIncoming = testFactory.findPossibleIncomingWarehouseRoute(orders.get(0)).orElseThrow();
        Long routableId = testFactory.getRouteIdForSortableFlow(routeIncoming);
        assertOrdersEqual(orders, getReportOrders(
                PartnerOrderParamsDto.builder().routeId(routableId)
                        .state(List.of(ScOrderState.SORTED)).routeOnlyCurrent(false).strictState(true).build()
        ));
    }

    /**
     * На странице поставок в колонке "в ячейке" должны отображаться заказы которые когда-либо были отсортированы или
     * в курьерскую ячейку или в ячейку хранения. Причем эти заказы должны будут отобразиться даже после того как
     * они покинут эту ячейку
     */
    @Test
    void getOrdersInCellHasBeenEverForIncomingRoute() {
        testFactory.createForToday(order(sortingCenter).externalId("00").build()).cancel().get();
        testFactory.createForToday(order(sortingCenter).externalId("01").build()).get();
        testFactory.createForToday(order(sortingCenter).externalId("02").build()).accept().get();
        testFactory.createForToday(order(sortingCenter).externalId("03").build()).accept().get();
        List<ScOrder> orders = new ArrayList<>();
        orders.add(testFactory.createForToday(order(sortingCenter).externalId("04").build()).accept().sort().get());
        orders.add(testFactory.createForToday(order(sortingCenter).externalId("05").build()).accept().sort().get());
        orders.add(testFactory.createForToday(order(sortingCenter).externalId("06").build()).accept().sort().get());
        orders.add(testFactory.createForToday(order(sortingCenter).externalId("07").build()).accept().sort().shipPlace("07").get());
        orders.add(testFactory.createForToday(order(sortingCenter).externalId("08").build()).accept().sort().shipPlace("08").get());
        orders.add(testFactory.createOrder(order(sortingCenter).externalId("09").build())
                .updateShipmentDate(LocalDate.now(clock).plusDays(1)).accept().keep().get());
        var o9 = testFactory.createOrder(order(sortingCenter).externalId("10").build()).accept().get();
        User user = testFactory.storedUser(sortingCenter, 1234L);
        Cell cell = testFactory.storedCell(sortingCenter, "buffer-1", CellType.BUFFER);

        String placeExternalId = testFactory.orderPlaces(o9).get(0).getMainPartnerCode();

        scanService.sortSortable(
                new SortableSortRequestDto(o9.getExternalId(), placeExternalId, String.valueOf(cell.getId())),
                new ScContext(user));
        orders.add(orderRepository.findAllByExternalId("10").get(0));
        var routeIncoming = testFactory.findPossibleIncomingWarehouseRoute(orders.get(0)).orElseThrow();
        Long routableId = testFactory.getRouteIdForSortableFlow(routeIncoming);
        assertOrdersEqual(orders, getReportOrders(
                PartnerOrderParamsDto.builder().routeId(routableId)
                        .state(List.of(ScOrderState.ACCEPTED, ScOrderState.SORTED)).routeOnlyCurrent(false)
                        .isInCell(true).build()
        ));
    }

    /**
     * Отгрузка курьеру
     */
    @Test
    void getAcceptedButNotShipped() {
        testFactory.createForToday(order(sortingCenter).externalId("0").build()).cancel().get();
        testFactory.createForToday(order(sortingCenter).externalId("1").build()).get();
        List<ScOrder> orders = List.of(
                testFactory.createForToday(order(sortingCenter).externalId("2").build()).accept().get(),
                testFactory.createForToday(order(sortingCenter).externalId("3").build()).accept().get(),
                testFactory.createForToday(order(sortingCenter).externalId("4").build()).accept().sort().get(),
                testFactory.createForToday(order(sortingCenter).externalId("5").build()).accept().sort().get(),
                testFactory.createForToday(order(sortingCenter).externalId("6").build()).accept().sort().get()
        );
        testFactory.createForToday(order(sortingCenter).externalId("7").build()).accept().sort().shipPlace("7").get();
        testFactory.createForToday(order(sortingCenter).externalId("8").build()).accept().sort().shipPlace("8").get();
        var outgoingRoute = testFactory.findOutgoingCourierRoute(orders.get(0)).orElseThrow();
        Long routableId = testFactory.getRouteIdForSortableFlow(outgoingRoute.getId());
        assertOrdersEqual(orders, getReportOrders(
                PartnerOrderParamsDto.builder().routeId(routableId)
                        .state(List.of(ScOrderState.ACCEPTED, ScOrderState.SORTED))
                        .routeOnlyCurrent(false).strictState(true).build()
        ));
    }

    @Test
    void getOrdersForOutgoingCourierRoute() {
        var o0 = testFactory.createForToday(order(sortingCenter).externalId("or-0").build()).cancel().get();
        var o1 = testFactory.createForToday(order(sortingCenter).externalId("or-1").build()).get();
        var o2 = testFactory.createForToday(order(sortingCenter).externalId("or-2").build()).accept().get();
        var o3 = testFactory.createForToday(order(sortingCenter).externalId("or-3").build()).accept().get();
        var route = testFactory.findOutgoingCourierRoute(o2).orElseThrow();
        var routeDto = routeQueryService.getRoutesWithStatsForPi(
                LocalDate.now(clock), sortingCenter, RouteType.OUTGOING_COURIER, Pageable.unpaged(), null);
        assertThat(routeDto.get(0).getOrdersSorted()).isEqualTo(0);
        assertThat(routeDto.get(0).getAcceptedButNotShipped()).isEqualTo(2);
        var o4 = testFactory.createForToday(order(sortingCenter).externalId("or-4").build()).accept().sort().get();
        var o5 = testFactory.createForToday(order(sortingCenter).externalId("or-5").build()).accept().sort().get();
        var o6 = testFactory.createForToday(order(sortingCenter).externalId("or-6").build()).accept().sort().get();
        var o7 = testFactory.createForToday(order(sortingCenter).externalId("or-7").build())
                .accept().sort().shipPlace("or-7").get();
        routeDto = routeQueryService.getRoutesWithStatsForPi(
                LocalDate.now(clock), sortingCenter, RouteType.OUTGOING_COURIER, Pageable.unpaged(), null);
        assertThat(routeDto.get(0).getOrdersSorted()).isEqualTo(3);//то, что прямо сейчас в ячейке
        assertThat(routeDto.get(0).getAcceptedButNotShipped()).isEqualTo(5);//принято, но не отгружено
        assertThat(routeDto.get(0).getOrdersPlanned()).isEqualTo(7);//всего запланировано
        assertOrdersEqual(List.of(o2, o3, o4, o5, o6), getReportOrders(
                PartnerOrderParamsDto.builder().routeId(testFactory.getRouteIdForSortableFlow(route))
                        .state(List.of(ScOrderState.ACCEPTED, ScOrderState.SORTED))
                        .routeOnlyCurrent(false).strictState(true).build()));//принято, но не отгружено
        assertOrdersEqual(List.of(o4, o5, o6), getReportOrders(
                PartnerOrderParamsDto.builder().routeId(testFactory.getRouteIdForSortableFlow(route))
                        .state(List.of(ScOrderState.SORTED))
                        .routeOnlyCurrent(false).strictState(true).build()));//в ячейке
    }

    @Test
    void getOrdersWithCurrentRouteFilter() {
        testFactory.createForToday(order(sortingCenter).externalId("0").build()).cancel().get();
        List<ScOrder> orders = List.of(
                testFactory.createForToday(order(sortingCenter).externalId("1").build()).get(),
                testFactory.createForToday(order(sortingCenter).externalId("2").build()).accept().get(),
                testFactory.createForToday(order(sortingCenter).externalId("3").build()).accept().sort().get()
        );
        testFactory.createForToday(order(sortingCenter).externalId("4").build()).accept().sort().shipPlace("4").get();

        var route = testFactory.findOutgoingCourierRoute(orders.get(0)).orElseThrow();
        assertOrdersEqual(orders, getReportOrders(
                PartnerOrderParamsDto.builder().routeId(testFactory.getRouteIdForSortableFlow(route)).routeOnlyCurrent(true).build()
        ));
    }

    @Test
    void getOrdersWithQuotes() {
        var order = testFactory.createForToday(order(sortingCenter).externalId("0'0").build())
                .updateCourier(testFactory.storedCourier())
                .accept().sort().get();
        var user = testFactory.storedUser(sortingCenter, 1234L);
        var cell = testFactory.storedCell(sortingCenter, "buf''fer", CellType.BUFFER);
        var routeIncoming = testFactory.findPossibleIncomingWarehouseRoute(order).orElseThrow();
        Long routableId = testFactory.getRouteIdForSortableFlow(routeIncoming);
        var reportOrdersList =
                getReportOrders(PartnerOrderParamsDto.builder()
                        .id(order.getExternalId())
                        .routeId(routableId)
                        .state(List.of(ScOrderState.ACCEPTED, ScOrderState.SORTED))
                        .routeOnlyCurrent(false)
                        .cellNumber(cell.getScNumber()
                        ).isInCell(true).build()
                );
    }

    @Test
    void getPartnerOrdersXdoc() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.XDOC_ENABLED, "true");
        Cell cell = testFactory.storedCell(sortingCenter, "cell-1", CellType.BUFFER, CellSubType.BUFFER_XDOC,
                TestFactory.WAREHOUSE_YANDEX_ID);
        flow.createInbound("in1")
                .linkPallets("XDOC-1")
                .fixInbound()
                .sortToAvailableCell("XDOC-1");

        List<PartnerOrderDto> dtos = getReportOrders(PartnerOrderParamsDto.builder().build());
        assertThat(dtos).hasSize(1);
        assertThat(dtos).anyMatch(partnerOrderDto -> partnerOrderDto.getId().equals("XDOC-1")
                && cell.getScNumber().equals(partnerOrderDto.getCellNumber())
                && partnerOrderDto.getPlaceCount() == 1
                && partnerOrderDto.getState().equals(ScOrderState.ACCEPTED)
                && Objects.nonNull(partnerOrderDto.getArrivedToSoDate())
        );
    }

    @Test
    void getPartnerOrdersXdocSeveralSortables() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.XDOC_ENABLED, "true");
        Cell cell = testFactory.storedCell(sortingCenter, "cell-1", CellType.BUFFER, CellSubType.BUFFER_XDOC,
                TestFactory.WAREHOUSE_YANDEX_ID);

        Cell shipCell = testFactory.storedCell(sortingCenter, "cell-2", CellType.COURIER, CellSubType.SHIP_XDOC,
                TestFactory.WAREHOUSE_YANDEX_ID);

        Sortable basket = flow.createBasketAndGet(cell);
        sortableLotService.findBySortableId(basket.getId()).orElseThrow();
        flow.inboundBuilder("in1")
                .informationListBarcode("Зп-1")
                .build()
                .linkPallets("XDOC-1")
                .fixInbound()
                .sortToAvailableCell("XDOC-1")
                .inboundBuilder("in2")
                .informationListBarcode("Зп-2")
                .build()
                .linkPallets("XDOC-2")
                .fixInbound()
                .inboundBuilder("in3")
                .informationListBarcode("Зп-3")
                .build()
                .linkBoxes("XDOC-3", "XDOC-4")
                .fixInbound()
                .createOutbound("out-1")
                .addRegistryBoxes("XDOC-3", "XDOC-4")
                .buildRegistry("XDOC-2", basket.getRequiredBarcodeOrThrow())
                .sortToAvailableLot("XDOC-3", "XDOC-4")
                .packLot(basket.getRequiredBarcodeOrThrow())
                .sortToAvailableCell("XDOC-2", basket.getRequiredBarcodeOrThrow())
                .prepareToShip(basket.getRequiredBarcodeOrThrow());

        List<PartnerOrderDto> dtos = getReportOrders(PartnerOrderParamsDto.builder().build());
        assertThat(dtos)
                .hasSize(3)
                .anyMatch(partnerOrderDto ->
                        partnerOrderDto.getId().equals("XDOC-1")
                                && cell.getScNumber().equals(partnerOrderDto.getCellNumber())
                                && partnerOrderDto.getPlaceCount() == 1
                                && partnerOrderDto.getState().equals(ScOrderState.ACCEPTED)
                                && Objects.nonNull(partnerOrderDto.getArrivedToSoDate())
                                && "Зп-1".equals(partnerOrderDto.getInboundCode())
                )
                .anyMatch(partnerOrderDto ->
                        partnerOrderDto.getId().equals("XDOC-2")
                                && shipCell.getScNumber().equals(partnerOrderDto.getCellNumber())
                                && partnerOrderDto.getPlaceCount() == 1
                                && partnerOrderDto.getState().equals(ScOrderState.SORTED)
                                && Objects.nonNull(partnerOrderDto.getArrivedToSoDate())
                                && "Зп-2".equals(partnerOrderDto.getInboundCode())
                )
                .anyMatch(partnerOrderDto ->
                        partnerOrderDto.getId().equals(basket.getRequiredBarcodeOrThrow())
                                && partnerOrderDto.getPlaceCount() == 2
                                && partnerOrderDto.getState().equals(ScOrderState.SORTED)
                                && Objects.nonNull(partnerOrderDto.getArrivedToSoDate())
                                && "Зп-3".equals(partnerOrderDto.getInboundCode())
                );
    }

    @Test
    @DisplayName("success количество заказов по фильтру")
    void getOrderCount() {
        int countOrders = 151;
        var courierDto = new CourierDto(10L, "Иванов Иван Иванович", null);
        for (int i = 0; i < countOrders; i++) {
            courierDto.setCompanyName("Рога и копыта");
            testFactory.createForToday(order(sortingCenter).externalId("ex_id_" + i).build(), courierDto).get();
        }
        var params = PartnerOrderParamsDto.builder()
                .courierId(courierDto.getId())
                .build();
        var oc = partnerOrderReportService.getOrdersCountByParams(sortingCenter, params);
        assertThat(oc).isEqualTo(countOrders);
    }


    private List<PartnerOrderDto> getReportOrders(PartnerOrderParamsDto param) {
        return partnerOrderReportService.getOrders(sortingCenter, param, Pageable.unpaged())
                .get().sorted(Comparator.comparing(PartnerOrderDto::getId)).toList();
    }

    private void assertOrdersEqual(List<ScOrder> orders, List<PartnerOrderDto> reportOrders) {
        assertThat(reportOrders)
                .usingElementComparatorIgnoringFields(
                        "dispatchPersonName", "arrivedToSoDate", "arrivedToSoTime",
                        "shippedToCourierDate", "shippedToCourierTime"
                )
                .isEqualTo(orders.stream()
                        .map(o -> buildFromOrder(o, testFactory.orderPlaces(o)))
                        .toList());
    }

    private PartnerOrderDto buildFromOrder(OrderLike order, List<Place> places) {
        return new PartnerOrderDto(
                order.getExternalId(),
                null, null, null, null,
                Optional.ofNullable(places.get(0).getCell()).map(Cell::getId).orElse(null),
                Optional.ofNullable(places.get(0).getCell()).map(Cell::getScNumber).orElse(null),
                Optional.ofNullable(order.getCourier()).map(Courier::getName).orElse(null),
                order.getState(),
                order.getWarehouseReturn().getIncorporation(),
                null,
                places.size(),
                null,
                null,
                null,
                null
        );
    }

}
