package ru.yandex.market.sc.internal.domain.report;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.SneakyThrows;
import lombok.Value;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.inbound.InboundCommandService;
import ru.yandex.market.sc.core.domain.inbound.model.CreateInboundRegistrySortableRequest;
import ru.yandex.market.sc.core.domain.inbound.model.InboundDiscrepancyActDto;
import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.model.RegistryUnitType;
import ru.yandex.market.sc.core.domain.lot.LotCommandService;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.movement_courier.repository.MovementCourier;
import ru.yandex.market.sc.core.domain.order.AcceptService;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.place.PlaceNonBlockingQueryService;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.route.RouteCommandService;
import ru.yandex.market.sc.core.domain.route.RouteFacade;
import ru.yandex.market.sc.core.domain.route.RouteQueryService;
import ru.yandex.market.sc.core.domain.route.model.RouteDocumentType;
import ru.yandex.market.sc.core.domain.route.model.RouteFinishByCellsRequest;
import ru.yandex.market.sc.core.domain.route.model.TransferActGetRequest;
import ru.yandex.market.sc.core.domain.route.repository.Route;
import ru.yandex.market.sc.core.domain.route.repository.RouteRepository;
import ru.yandex.market.sc.core.domain.scan.ScanService;
import ru.yandex.market.sc.core.domain.scan.model.SortableSortRequestDto;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLotService;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.external.delivery_service.TplClient;
import ru.yandex.market.sc.core.external.s3.S3Client;
import ru.yandex.market.sc.core.resolver.dto.ScContext;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;
import ru.yandex.market.sc.internal.test.EmbeddedDbIntTest;
import ru.yandex.market.tpl.api.model.order.CallRequirement;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.order.partner.PartnerRecipientDto;
import ru.yandex.market.tpl.api.model.shift.routingList.RoutingListDataDto;
import ru.yandex.market.tpl.api.model.shift.routingList.RoutingListRow;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;
import ru.yandex.market.tpl.common.util.exception.TplIllegalArgumentException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.CREATE_LOTS_FROM_REGISTRY;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.ENABLE_INBOUND_TRANSFER_ACT_BUILD_FOR_PLACES;
import static ru.yandex.market.sc.core.test.TestFactory.order;
import static ru.yandex.market.sc.core.test.TestFactory.ordersRegistryFilter;

/**
 * @author valter
 */
@EmbeddedDbIntTest
class PartnerReportServiceTest {

    private static final String ANOMALY_BARCODE = "AN-12345";

    @Autowired
    TestFactory testFactory;
    @Autowired
    TransactionTemplate transactionTemplate;
    @Autowired
    PartnerReportService partnerReportService;
    @Autowired
    RouteQueryService routeQueryService;
    @Autowired
    ScOrderRepository scOrderRepository;
    @Autowired
    InboundCommandService inboundCommandService;
    @Autowired
    RouteRepository routeRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @MockBean
    TplClient tplClient;
    @MockBean
    S3Client s3Client;
    @Autowired
    XDocFlow flow;
    @Autowired
    RouteCommandService routeCommandService;
    @Autowired
    LotCommandService lotCommandService;
    @Autowired
    ScanService scanService;
    @Autowired
    AcceptService acceptService;
    @Autowired
    SortableLotService sortableLotService;
    @Autowired
    PlaceNonBlockingQueryService placeNonBlockingQueryService;
    @MockBean
    Clock clock;
    @Autowired
    RouteFacade routeFacade;

    SortingCenter sortingCenter;
    User user;
    Warehouse warehouse;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter(100L, "Новый СЦ");
        user = testFactory.storedUser(sortingCenter, 123L);
        warehouse = testFactory.storedWarehouse();
        testFactory.setupMockClock(clock);
    }

    @Test
    @SneakyThrows
    void generateTransferActWithRouteSheetFallbackOnNoRouteSheetFromCourier() {
        var data = initTestData();
        doReturn(Collections.emptyList())
                .when(tplClient).getRoutingListDataByUid(eq(sortingCenter.getId()), any(LocalDate.class), any());
        Long routeId = testFactory.getRouteIdForSortableFlow(data.getRoute().getId());
        byte[] actual = partnerReportService.getTransferActPdf(routeId,
                RouteDocumentType.ALL,
                sortingCenter);
        assertThat(actual.length).isGreaterThan(0);
    }

    @Test
    @SneakyThrows
    void generateTransferActWithRouteSheetFallbackOnCourierRequestException() {
        var data = initTestData();
        doThrow(RuntimeException.class)
                .when(tplClient).getRoutingListDataByUid(eq(sortingCenter.getId()), any(LocalDate.class), any());
        Long routeId = testFactory.getRouteIdForSortableFlow(data.getRoute().getId());
        byte[] actual = partnerReportService.getTransferActPdf(routeId,
                RouteDocumentType.ALL,
                sortingCenter);
        assertThat(actual.length).isGreaterThan(0);
    }

    @Test
    @SneakyThrows
    void generateTransferActWithTwoCars() {
        var data = initTestData();
        doReturn(List.of(routingListDataDto(List.of(data.getOrder1(), data.getOrder2()))))
                .when(tplClient).getRoutingListDataV2ByUid(eq(sortingCenter.getId()), any(LocalDate.class), any());

        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.IS_DROPOFF, "false");
        Long routeId = testFactory.getRouteIdForSortableFlow(data.getRoute().getId());
        byte[] actual = partnerReportService.getTransferActPdf(
                routeId,
                RouteDocumentType.ALL,
                sortingCenter
        );

        byte[] expected = IOUtils.toByteArray(
                Objects.requireNonNull(
                        TestFactory.class.getClassLoader().getResourceAsStream(
                                "two_cars_barcode.pdf"
                        )
                )
        );

        assertThat(actual).contains(expected);
    }

    @Test
    @SneakyThrows
    void generateTransferActWithTwoCarsV3() {
        var data = initTestData();
        doReturn(List.of(routingListDataDto(List.of(data.getOrder1(), data.getOrder2()))))
                .when(tplClient).getRoutingListDataV3ByUid(eq(sortingCenter.getToken()), any(LocalDate.class), any());

        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.IS_DROPOFF, "false");
        Long routeId = testFactory.getRouteIdForSortableFlow(data.getRoute().getId());
        byte[] actual = partnerReportService.getTransferActPdf(
                routeId,
                RouteDocumentType.ALL,
                sortingCenter
        );

        byte[] expected = IOUtils.toByteArray(
                Objects.requireNonNull(
                        TestFactory.class.getClassLoader().getResourceAsStream(
                                "two_cars_barcode.pdf"
                        )
                )
        );

        assertThat(actual).contains(expected);
    }
    @Test
    @SneakyThrows
    void generateTransferActNoCarBarcode() {

        var data = initTestDatNoCars();
        doReturn(List.of(routingListDataDto(List.of(data.getOrder1(), data.getOrder2()))))
                .when(tplClient).getRoutingListDataV3ByUid(eq(sortingCenter.getToken()), any(LocalDate.class), any());
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.IS_DROPOFF, "false");

        Long routeId = testFactory.getRouteIdForSortableFlow(data.getRoute().getId());
        byte[] actual = partnerReportService.getTransferActPdf(
                routeId,
                RouteDocumentType.ALL,
                sortingCenter
        );

        byte[] expected = IOUtils.toByteArray(
                Objects.requireNonNull(
                        TestFactory.class.getClassLoader().getResourceAsStream(
                                "no_car_barcode.pdf"
                        )
                )
        );

        assertThat(actual).contains(expected);
    }

    @Test
    @SneakyThrows
    void generateOutgoingCourierTransferActPlace() {
        var courier1 = testFactory.storedCourier(1L, testFactory.defaultCourier().getName());

        var cell1 = testFactory.storedCell(sortingCenter, "cell1", CellType.COURIER, courier1.getId());
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell1, LotStatus.CREATED, false);
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell1, LotStatus.CREATED, false);
        var lot3 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell1, LotStatus.CREATED, false);

        var order11 = testFactory.createForToday(order(sortingCenter, "11").build())
                .accept().sort(cell1.getId()).sortToLot(lot1.getLotId()).get();
        var order12 = testFactory.createForToday(order(sortingCenter, "12")
                        .places("12-1", "12-2", "12-3", "12-4", "12-5").build())
                .accept().sort(cell1.getId()).sortToLot(lot1.getLotId()).get();
        var order13 = testFactory.createForToday(order(sortingCenter, "13").build())
                .accept().sort(cell1.getId()).sortToLot(lot1.getLotId()).get();
        var order21 = testFactory.createForToday(order(sortingCenter, "21").build())
                .accept().sort(cell1.getId()).sortToLot(lot2.getLotId()).get();
        var order22 = testFactory.createForToday(order(sortingCenter, "22").build())
                .accept().sort(cell1.getId()).sortToLot(lot2.getLotId()).get();
        var order31 = testFactory.createForToday(order(sortingCenter, "31").createTwoPlaces(true).build())
                .accept().sort(cell1.getId()).sortToLot(lot3.getLotId()).get();
        var order32 = testFactory.createForToday(order(sortingCenter, "33").createTwoPlaces(true).build())
                .accept().sort(cell1.getId()).get();

        testFactory.prepareToShipLot(lot1);
        testFactory.prepareToShipLot(lot2);
        testFactory.addStampToSortableLotAndPrepare(lot3.getBarcode(), "LOT_3_STAMP", user);

        var route = testFactory.findOutgoingCourierRoute(order11).orElseThrow();
        order11 = testFactory.shipOrderRoute(order11);
        testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);

        doReturn(List.of(routingListDataDto(List.of(order11, order12, order13, order21, order22, order31, order32))))
                .when(tplClient).getRoutingListDataV3ByUid(eq(sortingCenter.getToken()), any(LocalDate.class), any());

        byte[] actual = partnerReportService.getTransferActPdf(
                testFactory.getRouteIdForSortableFlow(route),
                RouteDocumentType.ALL,
                sortingCenter,
                null,
                null,
                true
        );

        byte[] expected = IOUtils.toByteArray(
                Objects.requireNonNull(
                        TestFactory.class.getClassLoader().getResourceAsStream(
                                "tacp_oc.pdf"
                        )
                )
        );

        assertThat(actual).contains(expected);
    }

    @Test
    @SneakyThrows
    void generateOutgoingCourierTransferActPlaceSeveralCells() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.CREATE_ROUTE_SO_FOR_SORTABLE, true);
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.MOVE_LOTS_FLOW_ENABLED, true);
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.SPLIT_REPORTS_BY_CARS_ENABLED, false);
        var courier1 = testFactory.storedCourier(1L, testFactory.defaultCourier().getName());
        var zone = testFactory.storedZone(sortingCenter, "BUF");

        var cell1 = testFactory.storedCell(sortingCenter, "cell1", CellType.COURIER, courier1.getId());

        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell1, LotStatus.CREATED, false);
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell1, LotStatus.CREATED, false);
        var lot3 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell1, LotStatus.CREATED, false);
        var lot4 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell1, LotStatus.CREATED, false);
        var lot5 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell1, LotStatus.CREATED, false);

        var cell2 = testFactory.storedShipBufferCell(sortingCenter, courier1.getId(), zone, 2, 3, 0, 5);

        var order11 = testFactory.createForToday(order(sortingCenter, "118013451").build())
                .accept().sort(cell1.getId()).sortToLot(lot1.getLotId()).get();
        var order12 = testFactory.createForToday(order(sortingCenter, "117558238")
                        .places("117558238-1", "117558238-2", "117558238-3", "117558238-4", "117558238-5").build())
                .accept().sort(cell1.getId()).sortToLot(lot1.getLotId()).get();
        var order13 = testFactory.createForToday(order(sortingCenter, "117502950").createTwoPlaces(true).build())
                .accept().sort(cell1.getId()).sortToLot(lot1.getLotId()).get();
        var order21 = testFactory.createForToday(order(sortingCenter, "117780771").build())
                .accept().sort(cell1.getId()).sortToLot(lot2.getLotId()).get();
        var order22 = testFactory.createForToday(order(sortingCenter, "117574847")
                        .places("117574847-1", "117574847-2", "117574847-3").build())
                .accept().sort(cell1.getId()).sortToLot(lot2.getLotId()).get();
        var order31 = testFactory.createForToday(order(sortingCenter, "118006956").build())
                .accept().sort(cell1.getId()).sortToLot(lot3.getLotId()).get();
        var order32 = testFactory.createForToday(order(sortingCenter, "118006964").createTwoPlaces(true).build())
                .accept().sort(cell1.getId()).get();

        var order34 = testFactory.createForToday(order(sortingCenter, "118003232").createTwoPlaces(true).build())
                .accept().sort(cell1.getId()).sortToLot(lot3.getLotId()).get();
        var order35 = testFactory.createForToday(order(sortingCenter, "117652140")
                        .places("117652140-1", "117652140-2", "117652140-3", "117652140-4").build())
                .accept().sort(cell1.getId()).sortToLot(lot3.getLotId()).get();
        var order36 = testFactory.createForToday(order(sortingCenter, "117652165").createTwoPlaces(true).build())
                .accept().sort(cell1.getId()).sortToLot(lot4.getLotId()).get();

        var order37 = testFactory.createForToday(order(sortingCenter, "117652234").createTwoPlaces(true).build())
                .accept().sort(cell1.getId()).sortToLot(lot5.getLotId()).get();
        var order38 = testFactory.createForToday(order(sortingCenter, "117652365").build())
                .accept().sort(cell1.getId()).sortToLot(lot5.getLotId()).get();

        testFactory.prepareToShipLot(lot1);
        testFactory.prepareToShipLot(lot2);

        //move lot to ship_buffer
        scanService.moveLot(new SortableSortRequestDto(
                lot2.getBarcode(),
                null,
                String.valueOf(cell2.getId())), new ScContext(user));

        testFactory.prepareToShipLot(lot4);
        scanService.moveLot(new SortableSortRequestDto(
                lot4.getBarcode(),
                null,
                String.valueOf(cell2.getId())), new ScContext(user));

        testFactory.addStampToSortableLotAndPrepare(lot3.getBarcode(), "LOT_3_STAMP", user);
        testFactory.addStampToSortableLotAndPrepare(lot5.getBarcode(), "LOT_5_STAMP", user);

        var route = testFactory.findOutgoingCourierRoute(order11).orElseThrow();
        order11 = testFactory.shipOrderRoute(order11);
        testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);

        byte[] actual = partnerReportService.getTransferActPdf(
                testFactory.getRouteIdForSortableFlow(route),
                RouteDocumentType.ALL,
                sortingCenter,
                null,
                null,
                true
        );

        byte[] expected = IOUtils.toByteArray(
                Objects.requireNonNull(
                        TestFactory.class.getClassLoader().getResourceAsStream(
                                "tacp_oc_sc.pdf"
                        )
                )
        );

        assertThat(actual).contains(expected);
    }

    @Test
    @SneakyThrows
    void generateOutgoingCourierTransferActPlaceDropoff() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.IS_DROPOFF, true);

        var order11 = testFactory.createForToday(order(sortingCenter, "11").build())
                .accept().get();
        Route route = testFactory.findOutgoingCourierRoute(
                        testFactory.getOrderLikeForRouteLookup(order11))
                .orElseThrow();
        var cell1 = testFactory.determineRouteCell(route, order11);
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell1, LotStatus.CREATED, false);
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell1, LotStatus.CREATED, false);
        var lot3 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell1, LotStatus.CREATED, false);

        var order12 = testFactory.createForToday(order(sortingCenter, "12")
                        .places("12-1", "12-2", "12-3", "12-4", "12-5").build())
                .accept().sort(cell1.getId()).sortToLot(lot1.getLotId()).get();
        var order13 = testFactory.createForToday(order(sortingCenter, "13").build())
                .accept().sort(cell1.getId()).sortToLot(lot1.getLotId()).get();
        var order21 = testFactory.createForToday(order(sortingCenter, "21").build())
                .accept().sort(cell1.getId()).sortToLot(lot2.getLotId()).get();
        var order22 = testFactory.createForToday(order(sortingCenter, "22").build())
                .accept().sort(cell1.getId()).sortToLot(lot2.getLotId()).get();
        var order31 = testFactory.createForToday(order(sortingCenter, "31").createTwoPlaces(true).build())
                .accept().sort(cell1.getId()).sortToLot(lot3.getLotId()).get();
        var order32 = testFactory.createForToday(order(sortingCenter, "33").createTwoPlaces(true).build())
                .accept().sort(cell1.getId()).get();

        testFactory.prepareToShipLot(lot1);
        testFactory.prepareToShipLot(lot2);
        testFactory.addStampToSortableLotAndPrepare(lot3.getBarcode(), "LOT_3_STAMP", user);

        order11 = testFactory.shipOrderRoute(order11);
        testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);

        byte[] actual = partnerReportService.getTransferActPdf(
                testFactory.getRouteIdForSortableFlow(route),
                RouteDocumentType.ALL,
                sortingCenter,
                null,
                null,
                true
        );

        byte[] expected = IOUtils.toByteArray(
                Objects.requireNonNull(
                        TestFactory.class.getClassLoader().getResourceAsStream(
                                "tacp_oc_do.pdf"
                        )
                )
        );

        assertThat(actual).contains(expected);
    }

    @Test
    @SneakyThrows
    void generateOutgoingCourierTransferActPlaceDropoffShipSeveralTimes() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.IS_DROPOFF, true);

        var order11 = testFactory.createOrder(order(sortingCenter, "11").build())
                .accept().sort().get();
        var order12 = testFactory.createOrder(order(sortingCenter, "12").build())
                .accept().sort().get();
        var order13 = testFactory.createOrder(order(sortingCenter, "13").createTwoPlaces(true)
                        .build()).accept().sort().get();
        var order14 = testFactory.createOrder(order(sortingCenter, "14").build()).accept().sort().get();
        Route route = testFactory.findOutgoingCourierRoute(
                        testFactory.getOrderLikeForRouteLookup(order11))
                .orElseThrow();

        //раз
        testFactory.shipPlace(testFactory.orderPlace(order11));
        testFactory.shipPlace(testFactory.orderPlace(order12));
        testFactory.shipPlace(testFactory.orderPlace(order13, "pk-13-1"));
        testFactory.shipPlace(testFactory.orderPlace(order13, "pk-13-2"));
        testFactory.shipPlace(testFactory.orderPlace(order14));

        //два
        testFactory.acceptPlace(testFactory.orderPlace(order11))
                .sortPlace(testFactory.orderPlace(order11))
                .shipPlace(testFactory.orderPlace(order11));

        //три
        testFactory.acceptPlace(testFactory.orderPlace(order11))
                .sortPlace(testFactory.orderPlace(order11))
                .shipPlace(testFactory.orderPlace(order11));
        Long routeId = testFactory.getRouteIdForSortableFlow(route.getId());
        byte[] actual = partnerReportService.getTransferActPdf(
                routeId,
                RouteDocumentType.ALL,
                sortingCenter,
                null,
                null,
                true
        );

    }

    @Test
    @SneakyThrows
    void generateIncomingWarehouseTransferActPlace() {
        var data = initIncomingWarehouseTestData();
        Long routeId = testFactory.getRouteIdForSortableFlow(data.getRoute().getId());
        byte[] actual = partnerReportService.getTransferActPdf(
                routeId, RouteDocumentType.ALL, sortingCenter, null, null, true);

        byte[] expected = IOUtils.toByteArray(
                Objects.requireNonNull(
                        TestFactory.class.getClassLoader().getResourceAsStream(
                                "tacp_iw.pdf"
                        )
                )
        );

        assertThat(actual).contains(expected);
    }

    @Test
    @SneakyThrows
    void generateIncomingCourierTransferActPlace() {
        var courier1 = testFactory.storedCourier(1L, testFactory.defaultCourier().getName());

        var cell1 = testFactory.storedCell(sortingCenter, "cell1", CellType.COURIER, courier1.getId());

        var order11 = testFactory.createForToday(order(sortingCenter, "11").build())
                .accept().sort(cell1.getId()).ship().accept().get();
        var order12 = testFactory.createForToday(order(sortingCenter, "12")
                        .places("12-1", "12-2", "12-3", "12-4", "12-5").build())
                .acceptPlaces().sortPlaces().shipPlaces().acceptPlaces().get();
        var order13 = testFactory.createForToday(order(sortingCenter, "13").build())
                .accept().sort(cell1.getId()).ship().accept().get();
        var order21 = testFactory.createForToday(order(sortingCenter, "21").build())
                .accept().sort(cell1.getId()).ship().accept().get();
        var order22 = testFactory.createForToday(order(sortingCenter, "22").build())
                .accept().sort(cell1.getId()).ship().accept().get();
        var order31 = testFactory.createForToday(order(sortingCenter, "31").createTwoPlaces(true).build())
                .accept().sort(cell1.getId()).ship().accept().get();
        var order32 = testFactory.createForToday(order(sortingCenter, "33").createTwoPlaces(true).build())
                .accept().sort(cell1.getId()).ship().accept().get();

        var route = testFactory.findPossibleIncomingCourierRoute(order11).orElseThrow();

        byte[] actual = partnerReportService.getTransferActPdf(
                testFactory.getRouteIdForSortableFlow(route), RouteDocumentType.ALL, sortingCenter, null, null, true);

        byte[] expected = IOUtils.toByteArray(
                Objects.requireNonNull(
                        TestFactory.class.getClassLoader().getResourceAsStream(
                                "tacp_ic.pdf"
                        )
                )
        );
        assertThat(actual).contains(expected);
    }

    @Test
    @SneakyThrows
    void generateClientReturnTransferActPlace() {
        var courier1 = testFactory.storedCourier(1L, testFactory.defaultCourier().getName());

        testFactory.storedWarehouse("10001700279");
        testFactory.storedDeliveryService("ds_for_client_return", sortingCenter.getId(), true);
        var order = testFactory.createClientReturnForToday(
                        sortingCenter.getId(),
                        sortingCenter.getToken(),
                        sortingCenter.getYandexId(),
                        testFactory.defaultCourier(),
                        "VOZVRAT_TAR_1"
                )
                .accept().get();
        var route = testFactory.findPossibleIncomingCourierRoute(order).orElseThrow();

        byte[] actual = partnerReportService.getTransferActPdf(
                testFactory.getRouteIdForSortableFlow(route), RouteDocumentType.ALL, sortingCenter, null, null, true);

        byte[] expected = IOUtils.toByteArray(
                Objects.requireNonNull(
                        TestFactory.class.getClassLoader().getResourceAsStream(
                                "tacp_ic.pdf"
                        )
                )
        );
        assertThat(actual).contains(expected);
    }

    @Test
    @SneakyThrows
    void generateOutgoingWarehouseTransferActPlace() {
        var cell1 = testFactory.storedCell(sortingCenter, "cell1", CellType.RETURN);
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell1, LotStatus.CREATED, false);
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell1, LotStatus.CREATED, false);
        var lot3 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell1, LotStatus.CREATED, false);

        var order11 = testFactory.createForToday(order(sortingCenter, "11").build())
                .cancel().acceptPlaces().sort(cell1.getId()).sortToLot(lot1.getLotId()).get();
        var order12 = testFactory.createForToday(order(sortingCenter, "12")
                        .places("12-1", "12-2", "12-3", "12-4", "12-5").build())
                .cancel().acceptPlaces().sort(cell1.getId()).sortToLot(lot1.getLotId()).get();
        var order13 = testFactory.createForToday(order(sortingCenter, "13").build())
                .cancel().acceptPlaces().sort(cell1.getId()).sortToLot(lot1.getLotId()).get();
        var order21 = testFactory.createForToday(order(sortingCenter, "21").build())
                .cancel().acceptPlaces().sort(cell1.getId()).sortToLot(lot2.getLotId()).get();
        var order22 = testFactory.createForToday(order(sortingCenter, "22").build())
                .cancel().acceptPlaces().sort(cell1.getId()).sortToLot(lot2.getLotId()).get();
        var order31 = testFactory.createForToday(order(sortingCenter, "31").createTwoPlaces(true).build())
                .cancel().acceptPlaces().sort(cell1.getId()).sortToLot(lot3.getLotId()).get();
        var order32 = testFactory.createForToday(order(sortingCenter, "33").createTwoPlaces(true).build())
                .cancel().acceptPlaces().sort(cell1.getId()).get();


        var route = testFactory.findOutgoingWarehouseRoute(order11).orElseThrow();
        testFactory.prepareToShipLot(lot1);
        testFactory.prepareToShipLot(lot2);
        testFactory.prepareToShipLot(lot3);

        routeCommandService.finishOutgoingRouteWithCell(new RouteFinishByCellsRequest(
                testFactory.getRouteIdForSortableFlow(route), new ScContext(user),
                List.of(cell1.getId()),
                null,
                false
        ));
        testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);

        byte[] actual = partnerReportService.getTransferActPdf(testFactory.getRouteIdForSortableFlow(route),
                RouteDocumentType.ALL,
                sortingCenter, null, null, true);

        byte[] expected = IOUtils.toByteArray(
                Objects.requireNonNull(
                        TestFactory.class.getClassLoader().getResourceAsStream(
                                "tacp_ow.pdf"
                        )
                )
        );
        assertThat(actual).contains(expected);
    }

    @Test
    @SneakyThrows
    void generateOutgoingWarehouseTransferActPlaceOnlyDamaged() {
        testFactory.setSortingCenterProperty(sortingCenter, DAMAGED_ORDERS_ENABLED, true);
        testFactory.createForToday(order(sortingCenter, "1").build())
                .accept().sort().ship().makeReturn().accept().sort().ship().get();
        var damagedOrdersCell = testFactory.storedCell(sortingCenter,
                "ПЗ-1",
                CellType.RETURN,
                CellSubType.RETURN_DAMAGED);
        var damagedOrder = testFactory.createForToday(
                order(sortingCenter, "2").createTwoPlaces(true).warehouseCanProcessDamagedOrders(true).build()
        ).accept().markOrderAsDamaged().sort(damagedOrdersCell.getId()).ship().get();

        var route = testFactory.findPossibleOutgoingWarehouseRoute(damagedOrder).orElseThrow();

        byte[] actual = partnerReportService.getTransferActPdf(testFactory.getRouteIdForSortableFlow(route),
                RouteDocumentType.ONLY_DAMAGED,
                sortingCenter, null, null, true);

        byte[] expected = IOUtils.toByteArray(
                Objects.requireNonNull(
                        TestFactory.class.getClassLoader().getResourceAsStream(
                                "tacp_ow_damaged.pdf"
                        )
                )
        );
        assertThat(actual).contains(expected);
    }

    @Test
    @SneakyThrows
    @DisplayName("Сгенерировать единый АПП по грузоместам для Поставки. Референсный файл -- tacp_inbound.pdf")
    void buildInboundTransferActForPlaces() {
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

        var inbound = testFactory.createInbound(TestFactory.CreateInboundParams.builder()
                .inboundType(InboundType.DS_SC)
                .sortingCenter(sortingCenter)
                .inboundExternalId("in-1")
                .movementCourier(testFactory.storedMovementCourier(1200000000L))
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
                .warehouseFromExternalId(warehouse.getYandexId())
                .build()
        );

        testFactory.preAcceptLotWithPlaces("SC_LOT_1", sortingCenter, user);
        testFactory.finishAcceptLotWithPlaces("SC_LOT_1", sortingCenter, user);
        testFactory.preAcceptLotWithPlaces("SC_LOT_2", sortingCenter, user);
        testFactory.finishAcceptLotWithPlaces("SC_LOT_2", sortingCenter, user);

        var actual = partnerReportService.buildInboundTransferActForPlaces(inbound.getExternalId(), sortingCenter);
        assertThat(actual).isNotEmpty();
    }

    @Test
    @Disabled("В акте есть дата/время, нужно принимать их извне")
    @SneakyThrows
    void generateTransferActWithRouteSheetV3() {
        var data = initTestData();
        doReturn(List.of(routingListDataDto(List.of(data.getOrder1(), data.getOrder2()))))
                .when(tplClient).getRoutingListDataV3ByUid(eq(sortingCenter.getToken()), eq(LocalDate.now(clock)), any());

        byte[] actual = partnerReportService.getTransferActPdf(data.getRoute().getId(),
                RouteDocumentType.ALL,
                sortingCenter);

        byte[] expected = IOUtils.toByteArray(
                Objects.requireNonNull(
                        TestFactory.class.getClassLoader().getResourceAsStream(
                                "transfer_act_2.pdf"
                        )
                )
        );

        assertThat(actual).hasSameSizeAs(expected);

        assertThat(actual).contains(expected);
    }

    @SneakyThrows
    @Test
    void generateTransferActWithLotsSuccess() {
        var order1 = testFactory.createOrder(order(sortingCenter)
                .externalId("o-1")
                .places("p-1", "p-2")
                .dsType(DeliveryServiceType.TRANSIT)
                .deliveryDate(LocalDate.now(clock).minusDays(1))
                .shipmentDate(LocalDate.now(clock).minusDays(1))
                .build()).updateShipmentDate(LocalDate.now(clock).minusDays(1)).get();

        var order2 = testFactory.createForToday(order(sortingCenter)
                .externalId("o-2")
                .places("p-21", "p-22")
                .dsType(DeliveryServiceType.TRANSIT)
                .deliveryDate(LocalDate.now(clock))
                .shipmentDate(LocalDate.now(clock))
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
                .warehouseFromExternalId(warehouse.getYandexId())
                .build()
        );

        Route route = testFactory.findPossibleIncomingWarehouseRoute(order2).orElseThrow();
        testFactory.acceptLot("stamp-1", user);

        transactionTemplate.execute(t -> {
            var actual = routeQueryService.getTransferAct(
                    new TransferActGetRequest(
                            testFactory.getRoutable(route), RouteDocumentType.ALL, sortingCenter, null, null));

            assertThat(actual.getOrders()).hasSize(1);
            assertThat(actual.getOrders().get(0).getExternalId()).isEqualTo("o-1");
            return null;
        });

    }

    @Test
    @Disabled
    @SneakyThrows
    void generateReturnTransferAct() {
        var data = initTestDataForReturn();

        byte[] actual = partnerReportService.getTransferActPdf(data.getRoute().getId(),
                RouteDocumentType.ALL,
                sortingCenter);

        byte[] expected = IOUtils.toByteArray(
                Objects.requireNonNull(
                        TestFactory.class.getClassLoader().getResourceAsStream(
                                "return_transfer_act_single_page.pdf"
                        )
                )
        );

        assertThat(actual).hasSameSizeAs(expected);
        assertThat(actual).contains(expected);
    }

    @Test
    @Disabled
    @SneakyThrows
    void generateIncomingWarehouseTransferAct() {
        var data = initIncomingWarehouseTestData();

        byte[] actual = partnerReportService.getTransferActPdf(
                data.getRoute().getId(), RouteDocumentType.ALL, sortingCenter);

        byte[] expected = IOUtils.toByteArray(
                Objects.requireNonNull(
                        TestFactory.class.getClassLoader().getResourceAsStream(
                                "incoming_warehouse_transfer_act_single_page.pdf"
                        )
                )
        );

        assertThat(actual).hasSameSizeAs(expected);
        assertThat(actual).contains(expected);
    }

    @Test
    @SneakyThrows
    void generateOutgoingCourierRoutingResults() {
        prepareOrdersForRoutingResults();
        byte[] actual = partnerReportService.getOrdersForRouteInCsv(sortingCenter.getToken(), LocalDate.now(clock))
                .getReportContent();

        byte[] expected = IOUtils.toByteArray(
                Objects.requireNonNull(
                        TestFactory.class.getClassLoader().getResourceAsStream(
                                "outgoingCourierRoutingResults.csv"
                        )
                )
        );

        assertThat(actual).hasSameSizeAs(expected);
        assertThat(actual).contains(expected);
    }

    private TransferActInitData initIncomingWarehouseTestData() {
        var order1 = testFactory.createForToday(
                order(sortingCenter, "1").places("1", "2").build())
                .acceptPlaces("1", "2").get();
        var order2 = testFactory.createForToday(order(sortingCenter, "2").build())
                .accept().get();

        var route = testFactory.findPossibleIncomingWarehouseRoute(order1).orElseThrow();
        return new TransferActInitData(order1, order2, route);
    }

    private void prepareOrdersForRoutingResults() {
        var courier1 = testFactory.storedCourier(1L);
        var courier2 = testFactory.storedCourier(2L);
        var courierMiddleMile = testFactory.storedCourier(1L, 1L);
        var order_r_c = testFactory.createForToday(
                order(sortingCenter, "o_r_c")
                        .build())
                .updateCourier(courier1)
                .get();
        var order_r_a = testFactory.createForToday(
                order(sortingCenter, "o_r_a")
                        .build())
                .updateCourier(courier1)
                .accept()
                .get();
        var order_r_s = testFactory.createForToday(
                order(sortingCenter, "o_r_s")
                        .build())
                .updateCourier(courier1)
                .accept()
                .sort()
                .get();
        var order_r_sh = testFactory.createForToday(
                order(sortingCenter, "o_r_sh")
                        .build())
                .updateCourier(courier1)
                .accept()
                .sort()
                .get();
        var order_m_c = testFactory.createForToday(
                order(sortingCenter, "o_m_c")
                        .places("o_m_c_1", "o_m_c_2").build())
                .updateCourier(courier1)
                .get();
        var order_m_a = testFactory.createForToday(
                order(sortingCenter, "o_m_a")
                        .places("o_m_a_1", "o_m_a_2").build())
                .updateCourier(courier1)
                .acceptPlaces(List.of("o_m_a_1", "o_m_a_2"))
                .get();
        var order_m_s = testFactory.createForToday(
                order(sortingCenter, "o_m_s")
                        .places("o_m_s_1", "o_m_s_2").build())
                .updateCourier(courier1)
                .acceptPlaces(List.of("o_m_s_1", "o_m_s_2"))
                .sortPlaces(List.of("o_m_s_1", "o_m_s_2"))
                .get();
        var order_m_sh = testFactory.createForToday(
                order(sortingCenter, "o_m_sh")
                        .places("o_m_sh_1", "o_m_sh_2").build())
                .updateCourier(courier1)
                .acceptPlaces(List.of("o_m_sh_1", "o_m_sh_2"))
                .sortPlaces(List.of("o_m_sh_1", "o_m_sh_2"))
                .ship()
                .get();
        var order_r_c_courier_2 = testFactory.createForToday(
                order(sortingCenter, "o_r_c_courier_2")
                        .build())
                .updateCourier(courier2)
                .get();
        var order_r_sh_courier_2 = testFactory.createForToday(
                order(sortingCenter, "o_r_sh_courier_2")
                        .build())
                .updateCourier(courier1)
                .accept()
                .sort()
                .ship()
                .get();
        var order_r_c_courier_middle_mile = testFactory.createForToday(
                order(sortingCenter, "order_r_c_courier_middle_mile")
                        .build())
                .updateCourier(courierMiddleMile)
                .get();
    }

    @Test
    @Disabled
    @SneakyThrows
    void generateIncomingCourierTransferAct() {
        var data = initIncomingCourierTestData();

        byte[] actual = partnerReportService.getTransferActPdf(
                data.getRoute().getId(), RouteDocumentType.ALL, sortingCenter);

        byte[] expected = IOUtils.toByteArray(
                Objects.requireNonNull(
                        TestFactory.class.getClassLoader().getResourceAsStream(
                                "incoming_courier_transfer_act_single_page.pdf"
                        )
                )
        );

        assertThat(actual).hasSameSizeAs(expected);
        assertThat(actual).contains(expected);
    }

    private TransferActInitData initTestData() {
        var order1 = testFactory.createForToday(
                order(sortingCenter, "o-ext-1").places("p1", "p2").build())
                .acceptPlaces("p1", "p2").sortPlaces("p1", "p2").get();
        var order2 = testFactory.createForToday(order(sortingCenter, "o-ext-2").build())
                .accept().sort().shipWithBarCode("123").get();
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.COURIER_ROUTE_SHEET_V3_ENABLED, "true");

        var route = testFactory.findOutgoingCourierRoute(order1).orElseThrow();
        order1 = testFactory.shipOrderRoute(order1);
        return new TransferActInitData(order1, order2, route);
    }

    private TransferActInitData initTestDatNoCars() {
        var order1 = testFactory.createForToday(
                order(sortingCenter, "1").places("1", "2").build())
                .acceptPlaces("1", "2").sortPlaces("1", "2").get();
        var order2 = testFactory.createForToday(order(sortingCenter, "2").build())
                .accept().sort().get();
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.COURIER_ROUTE_SHEET_V3_ENABLED, "true");

        var route = testFactory.findOutgoingCourierRoute(order1).orElseThrow();
        order1 = testFactory.shipOrderRoute(order1);
        return new TransferActInitData(order1, order2, route);
    }

    private TransferActInitData initIncomingCourierTestData() {
        var order1 = testFactory.createForToday(
                order(sortingCenter, "1").places("1", "2").build())
                .acceptPlaces("1", "2").sortPlaces("1", "2").ship().acceptPlaces("1", "2").get();
        var order2 = testFactory.createForToday(order(sortingCenter, "2").build())
                .accept().sort().ship().accept().get();

        var route = testFactory.findPossibleIncomingCourierRoute(order1).orElseThrow();
        return new TransferActInitData(order1, order2, route);
    }

    private TransferActInitData initTestDataForReturn() {
        var order1 = testFactory.createForToday(
                order(sortingCenter, "54688941").places("1", "2", "3").build()
        )
                .cancel().acceptPlaces("1", "2", "3").sortPlaces("1", "2")
                .changeRouteId(20000L)
                .shipPlace("1").shipPlace("2").get();
        var order2 = testFactory.createForToday(order(sortingCenter, "17683458").build())
                .cancel().accept().sort().ship().get();
        Cell parentCell = testFactory.storedCell(sortingCenter, "1234-возврат", CellType.RETURN);
        SortableLot lot = testFactory.storedLot(sortingCenter, parentCell, LotStatus.PROCESSING);
        testFactory.sortOrderToLot(order1, lot, user);
        testFactory.sortOrderToLot(order2, lot, user);

        var route = testFactory.findOutgoingWarehouseRoute(order1).orElseThrow();
        return new TransferActInitData(order1, order2, route);
    }

    private RoutingListDataDto routingListDataDto(List<ScOrder> orders) {
        var courier = Objects.requireNonNull(orders.get(0).getCourier());
        return new RoutingListDataDto(
                LocalDate.now(clock),
                courier.getName(),
                courier.getId(),
                12345L,
                orders.size(),
                orders.stream()
                        .map(o -> new RoutingListRow(
                                "m-" + o.getExternalId(),
                                true,
                                o.getExternalId(),
                                testFactory.orderPlaces(o).stream()
                                        .map(Place::getMainPartnerCode)
                                        .collect(Collectors.joining(", ")),
                                new PartnerRecipientDto(
                                        "Вася",
                                        "Вася",
                                        null,
                                        null,
                                        "+1000000000",
                                        "vasya@vasya.ru",
                                        "комментарий",
                                        "1234",
                                        "4321",
                                        "5678"
                                ),
                                "Очень длинный адрес - длиннее не придумаешь.",
                                o.getTotal(),
                                OrderPaymentType.CARD,
                                OrderPaymentStatus.PAID,
                                LocalTime.MIN,
                                LocalTime.MAX,
                                CallRequirement.CALL_REQUIRED
                        )).toList()
        );
    }

    @Test
    @SneakyThrows
    @DisplayName("Можно сравнить отчеты с примером")
    void generateOrderRegistry() {
        var order1 = testFactory.createForToday(
                order(sortingCenter, "1234567890abcdef").places("1", "2").build())
                .cancel().acceptPlaces("1").sortPlaces("1").ship().get();
        testFactory.createForToday(order(sortingCenter, "2").build())
                .cancel().accept().sort().ship().get();

        byte[] expected = IOUtils.toByteArray(
                Objects.requireNonNull(
                        TestFactory.class.getClassLoader().getResourceAsStream(
                                "return_registry_example.xlsx"
                        )
                )
        );

        var route = testFactory.findPossibleOutgoingWarehouseRoute(order1).orElseThrow();

        byte[] bytes =
                partnerReportService.getReturnRegistry(testFactory.getRouteIdForSortableFlow(route),
                        sortingCenter, ordersRegistryFilter(RouteDocumentType.NORMAL));

        assertThat(bytes).hasSameSizeAs(expected);

        assertThat(bytes).contains(expected);
    }

    @Test
    @Disabled
    @SneakyThrows
    void generateReturnPalletLabel() {
        var order = testFactory.createOrderForToday(sortingCenter)
                .cancel().accept().sort().changeRouteId(20000L).ship().get();
        byte[] expected = IOUtils.toByteArray(
                Objects.requireNonNull(
                        TestFactory.class.getClassLoader().getResourceAsStream(
                                "return_pallet_label_example.pdf"
                        )
                )
        );
        var route = testFactory.findPossibleOutgoingWarehouseRoute(order).orElseThrow();
        byte[] actual = partnerReportService.getReturnPalletLabelPdf(
                testFactory.getRouteIdForSortableFlow(route), sortingCenter, RouteDocumentType.NORMAL);
        assertThat(actual).hasSameSizeAs(expected);
        assertThat(actual).contains(expected);
    }

    @Test
    @SneakyThrows
    @Disabled
    @DisplayName("Нужен для проверок генерации отчета вручную")
    void generateOrderRegistryExample() {
        var order1 = testFactory.createForToday(
                order(sortingCenter, "1234567890abcdef").places("1", "2").build())
                .cancel().acceptPlaces("1").sortPlaces("1").ship().get();
        testFactory.createForToday(order(sortingCenter, "2").build())
                .cancel().accept().sort().ship().get();

        String path = "/Users/mariakuz/Desktop/example_" + Instant.now() + ".xlsx";

        var route = testFactory.findPossibleOutgoingWarehouseRoute(order1).orElseThrow();

        byte[] bytes =
                partnerReportService.getReturnRegistry(testFactory.getRouteIdForSortableFlow(route),
                        sortingCenter, ordersRegistryFilter(RouteDocumentType.NORMAL));
        FileOutputStream fos = new FileOutputStream(path);
        fos.write(bytes);
        fos.flush();
        fos.close();
    }

    @Test
    void generateXdocInboundDiscrepancyAct() {
        MovementCourier courier = testFactory.storedMovementCourier(348762387L);

        testFactory.createInbound(TestFactory.CreateInboundParams
                .builder()
                .inboundType(InboundType.XDOC_TRANSIT)
                .inboundExternalId("inbound-1")
                .movementCourier(courier)
                .fromDate(OffsetDateTime.now(clock))
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .registryMap(Map.of("registry_1", List.of(Pair.of("1000001", "1000011"), Pair.of("1000002", "1000012"))))
                .build());
        testFactory.createInbound(TestFactory.CreateInboundParams
                .builder()
                .inboundType(InboundType.XDOC_TRANSIT)
                .inboundExternalId("inbound-2")
                .movementCourier(courier)
                .fromDate(OffsetDateTime.now(clock))
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .registryMap(Map.of("registry_2", List.of(Pair.of("2000001", "2000011"))))
                .build());

        byte[] bytes = partnerReportService.getDiscrepancyAct("inbound-1", sortingCenter, Set.of(InboundType.values()));

        assertThat(bytes.length).isGreaterThan(0);
    }

    @Test
    @SneakyThrows
    void generateScToScInboundDiscrepancyAct() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.ENABLE_DISCREPANCY_ACT_GENERATING_BY_REGISTRY_MANAGER, true);

        MovementCourier courier = testFactory.storedMovementCourier(348762387L);

        var sortingCenterFrom = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(12413491)
                        .yandexId("5378264623")
                        .token("sc_from_token")
                        .partnerName("sc_from_partner_name")
                        .build()
        );

        var inbound = testFactory.createInbound(TestFactory.CreateInboundParams
                .builder()
                .inboundType(InboundType.DS_SC)
                .inboundExternalId("inbound-1")
                .movementCourier(courier)
                .fromDate(OffsetDateTime.now(clock))
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .registryMap(Map.of("registry_1", List.of(Pair.of("1000001", "1000011"), Pair.of("1000002", "1000012"))))
                .warehouseFromExternalId(sortingCenterFrom.getYandexId())
                .build());

        assertThatThrownBy(
                () -> partnerReportService.getDiscrepancyAct(
                        inbound.getExternalId(),
                        sortingCenter,
                        Set.of(InboundType.values())
                )
        ).isInstanceOf(TplIllegalArgumentException.class);

        inboundCommandService.setDiscrepancyAct(
                new InboundDiscrepancyActDto(
                        inbound.getTransportationId(),
                        "bucket",
                        "filename"
                )
        );

        var expectedDiscrepancyActBytes = "DISCREPANCY_ACT".getBytes(StandardCharsets.UTF_8);
        mockS3ClientWith(expectedDiscrepancyActBytes);

        byte[] bytes = partnerReportService.getDiscrepancyAct("inbound-1", sortingCenter, Set.of(InboundType.values()));

        assertThat(bytes).isEqualTo(expectedDiscrepancyActBytes);
    }

    private void mockS3ClientWith(byte[] bytes) {
        Mockito.when(
                s3Client.downloadFile(anyString(), anyString())
        ).thenReturn(bytes);
    }

    @Test
    void generateInboundRegistryReportInvalidSortingCenter() {
        testFactory.createInbound(TestFactory.CreateInboundParams
                .builder()
                .inboundType(InboundType.DS_SC)
                .fromDate(OffsetDateTime.now(clock))
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .build());

        SortingCenter otherSortingCenter = testFactory.storedSortingCenter(sortingCenter.getId() + 743);

        assertThatThrownBy(
                () -> partnerReportService.getDiscrepancyAct(
                        "inboundExternalId",
                        otherSortingCenter,
                        Set.of(InboundType.values())
                )
        ).isInstanceOf(TplEntityNotFoundException.class);
    }

    @Test
    void getInboundPlannedRegistry() {
        var inbound = flow.inboundBuilder("in-1")
                .nextLogisticPoint("0")
                .type(InboundType.XDOC_ANOMALY)
                .sortingCenter(sortingCenter)
                .build();

        inboundCommandService.createInboundRegistry(
                Collections.emptyList(),
                List.of(new CreateInboundRegistrySortableRequest(
                        "in-1",
                        ANOMALY_BARCODE,
                        RegistryUnitType.BOX
                )),
                "in-1",
                "TEST_REGESTRY",
                testFactory.getOrCreateAnyUser(sortingCenter)
        );

        inbound.linkBoxes(ANOMALY_BARCODE);

        var bytes = partnerReportService.getAcceptanceDiffWithPlan(sortingCenter, "in-1");
        Assertions.assertThat(bytes).isNotEmpty();
    }

    @Test
    void getInboundPlannedRegistryWithDiff() {
        var inbound = flow.inboundBuilder("in-1")
                .nextLogisticPoint("0")
                .type(InboundType.XDOC_ANOMALY)
                .sortingCenter(sortingCenter)
                .build();

        inboundCommandService.createInboundRegistry(
                Collections.emptyList(),
                List.of(new CreateInboundRegistrySortableRequest(
                                "in-1",
                                ANOMALY_BARCODE,
                                RegistryUnitType.BOX
                        ),
                        new CreateInboundRegistrySortableRequest(
                                "in-1",
                                ANOMALY_BARCODE + 1,
                                RegistryUnitType.BOX
                        )),
                "in-1",
                "TEST_REGESTRY",
                testFactory.getOrCreateAnyUser(sortingCenter)
        );

        inbound.linkBoxes(ANOMALY_BARCODE);

        var bytes = partnerReportService.getAcceptanceDiffWithPlan(sortingCenter, "in-1");
        Assertions.assertThat(bytes).isNotEmpty();
    }

    @SneakyThrows
    @Test
    @Disabled
    void getInboundPlannedRegistryWithFile() {
        var inbound = flow.inboundBuilder("in-1")
                .nextLogisticPoint("0")
                .type(InboundType.XDOC_ANOMALY)
                .sortingCenter(sortingCenter)
                .build();

        inboundCommandService.createInboundRegistry(
                Collections.emptyList(),
                List.of(new CreateInboundRegistrySortableRequest(
                                "in-1",
                                ANOMALY_BARCODE,
                                RegistryUnitType.BOX
                        ),
                        new CreateInboundRegistrySortableRequest(
                                "in-1",
                                ANOMALY_BARCODE + "1233212112",
                                RegistryUnitType.BOX
                        )),
                "in-1",
                "TEST_REGESTRY",
                testFactory.getOrCreateAnyUser(sortingCenter)
        );

        inbound.linkBoxes(ANOMALY_BARCODE);

        var bytes = partnerReportService.getAcceptanceDiffWithPlan(sortingCenter, "in-1");
        Assertions.assertThat(bytes).isNotEmpty();

        File downloadsFolder = new File(System.getProperty("user.home"), "Downloads");
        if (downloadsFolder.exists()) {
            downloadsFolder.mkdir();
        }
        File labelFile = new File(downloadsFolder, "test-label.xlsx");
        FileUtils.writeByteArrayToFile(labelFile, bytes);
    }

    @Value
    private static class TransferActInitData {

        ScOrder order1;
        ScOrder order2;
        Route route;

    }

}
