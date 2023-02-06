package ru.yandex.market.sc.core.domain.route;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.client_return.repository.ClientReturnBarcodePrefix;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.order.OrderCommandService;
import ru.yandex.market.sc.core.domain.order.SortService;
import ru.yandex.market.sc.core.domain.place.PlaceCommandService;
import ru.yandex.market.sc.core.domain.place.model.PlaceId;
import ru.yandex.market.sc.core.domain.place.model.PlaceScRequest;
import ru.yandex.market.sc.core.domain.route.model.FinishRouteRequestDto;
import ru.yandex.market.sc.core.domain.route.model.ReturnPalletLabelDto;
import ru.yandex.market.sc.core.domain.route.model.RouteCreateRequest;
import ru.yandex.market.sc.core.domain.route.model.RouteFinishByCellsRequest;
import ru.yandex.market.sc.core.domain.route.model.RouteType;
import ru.yandex.market.sc.core.domain.route.model.TransferActDto;
import ru.yandex.market.sc.core.domain.route.model.TransferActGetRequest;
import ru.yandex.market.sc.core.domain.route.repository.RouteRepository;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.resolver.dto.ScContext;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.test.TestFactory.CreateOrderParams;
import ru.yandex.market.sc.internal.model.CourierDto;
import ru.yandex.market.tpl.common.util.datetime.LocalDateInterval;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.sc.core.domain.client_return.ClientReturnService.DELIVERY_SERVICE_YA_ID;
import static ru.yandex.market.sc.core.domain.route.model.RouteDocumentType.ALL;
import static ru.yandex.market.sc.core.domain.route.model.RouteDocumentType.NORMAL;
import static ru.yandex.market.sc.core.domain.route.model.RouteDocumentType.ONLY_CLIENT_RETURNS;
import static ru.yandex.market.sc.core.domain.route.model.RouteDocumentType.ONLY_DAMAGED;
import static ru.yandex.market.sc.core.test.TestFactory.order;
import static ru.yandex.market.sc.core.test.TestFactory.ordersRegistryFilter;

/**
 * @author valter
 */
@EmbeddedDbTest
class RouteQueryServiceTest {

    @Autowired
    SortService sortService;
    @Autowired
    RouteQueryService routeQueryService;
    @Autowired
    RouteCommandService routeCommandService;
    @Autowired
    RouteFacade routeFacade;
    @Autowired
    OrderCommandService orderCommandService;
    @Autowired
    PlaceCommandService placeCommandService;
    @Autowired
    TestFactory testFactory;
    @MockBean
    Clock clock;

    @Autowired
    RouteRepository routeRepository;
    @Autowired
    TransactionTemplate transactionTemplate;

    SortingCenter sortingCenter;
    Cell cell;
    User user;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter(100L, "Новый СЦ");
        cell = testFactory.storedCell(sortingCenter);
        user = testFactory.storedUser(sortingCenter, 123L);
        testFactory.setupMockClock(clock);
    }

    @Test
    void getTransferActFromInterval() {
        var initialDateTime = LocalDateTime.of(
                LocalDate.of(2021, 12, 5),
                LocalTime.of(15, 0)
        ).atZone(clock.getZone());
        testFactory.setupMockClock(clock, initialDateTime.toInstant());
        var order1 = testFactory.createForToday(order(sortingCenter, "1").build()).accept().sort().get();
        var route = testFactory.findOutgoingCourierRoute(order1).orElseThrow();
        var cell = testFactory.determineRouteCell(route, order1);
        var finishByCellsRequest = new RouteFinishByCellsRequest(
                testFactory.getRouteIdForSortableFlow(route), new ScContext(user),
                List.of(cell.getId()),
                null,
                false
        );
        routeCommandService.finishOutgoingRouteWithCell(finishByCellsRequest);

        testFactory.setupMockClock(clock, initialDateTime.toInstant().plus(1, ChronoUnit.HOURS));
        var order2 = testFactory.createForToday(order(sortingCenter, "2").build()).accept().sort().get();
        routeCommandService.finishOutgoingRouteWithCell(finishByCellsRequest);

        testFactory.setupMockClock(clock, initialDateTime.toInstant().plus(3, ChronoUnit.HOURS));
        var order3 = testFactory.createForToday(order(sortingCenter, "3").build()).accept().sort().get();
        routeCommandService.finishOutgoingRouteWithCell(finishByCellsRequest);

        transactionTemplate.execute(t -> {
            var transferAct = routeQueryService.getTransferAct(
                    new TransferActGetRequest(
                            testFactory.getRoutable(route), ALL,
                            sortingCenter,
                            initialDateTime.toLocalTime(),
                            initialDateTime.plus(2, ChronoUnit.HOURS).toLocalTime()
                    )
            );

            var ordersExternalIds = transferAct.getOrders()
                    .stream()
                    .map(TransferActDto.Order::getExternalId)
                    .toList();
            assertThat(ordersExternalIds).containsExactly(order1.getExternalId(), order2.getExternalId());
            return null;
        });
    }

    @Test
    void getTransferAct() {
        var order1 = testFactory.createForToday(order(sortingCenter, "1").build()).accept().sort().get();
        var order2 = testFactory.createForToday(order(sortingCenter, "2").build()).accept().sort().get();
        var route = testFactory.findOutgoingCourierRoute(order1).orElseThrow();
        var cell = testFactory.determineRouteCell(route, order1);
        routeCommandService.finishOutgoingRouteWithCell(new RouteFinishByCellsRequest(
                testFactory.getRouteIdForSortableFlow(route), new ScContext(user),
                List.of(cell.getId()),
                null,
                false
        ));
        transactionTemplate.execute(t -> {
            var transferAct = routeQueryService.getTransferAct(
                    new TransferActGetRequest(ALL, sortingCenter, testFactory.getRoutable(route)));
            assertThat(transferAct)
                    .isEqualTo(TransferActDto.builder()
                            .routeType(RouteType.OUTGOING_COURIER)
                            .number(String.valueOf(testFactory.getRouteIdForSortableFlow(route)))
                            .date(LocalDate.now(clock))
                            .sender("Новый СЦ")
                            .orders(
                                    List.of(
                                            TransferActDto.Order.builder()
                                                    .externalId("1")
                                                    .items(1)
                                                    .totalSum(new BigDecimal("336.00"))
                                                    .places(1)
                                                    .routeDocumentType(NORMAL)
                                                    .build(),
                                            TransferActDto.Order.builder()
                                                    .externalId("2")
                                                    .items(1)
                                                    .totalSum(new BigDecimal("336.00"))
                                                    .places(1)
                                                    .routeDocumentType(NORMAL)
                                                    .build()
                                    )
                            )
                            .dataCollectionTerminalLiabilityCaution(true)
                            .totalSum(new BigDecimal("336.00").multiply(BigDecimal.valueOf(2)))
                            .totalItems(2)
                            .totalPlaces(2)
                            .courier(Objects.requireNonNull(order1.getCourier()).getName())
                            .build()
                    );
            return null;
        });
    }

    @Test
    void getTransferActWithLots() {
        var order1 = testFactory.createForToday(order(sortingCenter, "1").build()).accept().cancel().accept().sort().get();
        var order2 = testFactory.createForToday(order(sortingCenter, "2").build()).accept().cancel().accept().sort().get();
        var route = testFactory.findOutgoingRoute(order1).orElseThrow();

        var cell = testFactory.determineRouteCell(route, order1);
        SortableLot l1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
        testFactory.sortPlaceToLot(testFactory.orderPlace(order1), l1, user);
        testFactory.prepareToShipLot(l1);
        routeFacade.finishOutgoingRoute(
                testFactory.getRouteIdForSortableFlow(route),
                FinishRouteRequestDto.builder().cellId(cell.getId()).build(),
                new ScContext(user));
        transactionTemplate.execute(t -> {
            var transferAct = routeQueryService.getTransferAct(
                    new TransferActGetRequest(ALL, sortingCenter, testFactory.getRoutable(route)));


            TransferActDto transferActDto = TransferActDto.builder()
                    .routeType(RouteType.OUTGOING_WAREHOUSE)
                    .executor("ООО Ромашка-Склад")
                    .number(String.valueOf(testFactory.getRouteIdForSortableFlow(route)))
                    .date(LocalDate.now(clock))
                    .sender("Новый СЦ")
                    .orders(
                            List.of(
                                    TransferActDto.Order.builder()
                                            .externalId("1")
                                            .items(1)
                                            .totalSum(new BigDecimal("336.00"))
                                            .places(1)
                                            .routeDocumentType(NORMAL)
                                            .lotName(l1.getBarcode())
                                            .build(),
                                    TransferActDto.Order.builder()
                                            .externalId("2")
                                            .items(1)
                                            .totalSum(new BigDecimal("336.00"))
                                            .places(1)
                                            .routeDocumentType(NORMAL)
                                            .build()
                            )
                    )
                    .dataCollectionTerminalLiabilityCaution(false)
                    .totalSum(new BigDecimal("336.00").multiply(BigDecimal.valueOf(2)))
                    .totalItems(2)
                    .totalPlaces(2)
                    .build();

            assertThat(transferAct).isEqualTo(transferActDto);
            return null;
        });
    }

    @Test
    void getTransferActClientReturn() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_PVZ.getWarehouseReturnId());
        var order = testFactory.createForToday(order(sortingCenter, "1").isClientReturn(true).build())
                .accept().sort().get();
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = testFactory.determineRouteCell(route, order);
        routeCommandService.finishOutgoingRouteWithCell(new RouteFinishByCellsRequest(
                testFactory.getRouteIdForSortableFlow(route), new ScContext(user),
                List.of(cell.getId()),
                null,
                false
        ));
        transactionTemplate.execute(t -> {
            assertThat(routeQueryService.getTransferAct(
                    new TransferActGetRequest(ALL, sortingCenter, testFactory.getRoutable(route))))
                    .isEqualTo(TransferActDto.builder()
                            .routeType(RouteType.OUTGOING_WAREHOUSE)
                            .number(String.valueOf(testFactory.getRouteIdForSortableFlow(route)))
                            .date(LocalDate.now(clock))
                            .sender("Новый СЦ")
                            .executor("ООО Ромашка-Склад")
                            .orders(
                                    List.of(
                                            TransferActDto.Order.builder()
                                                    .externalId("VOZVRAT_SF_PVZ_1")
                                                    .items(1)
                                                    .totalSum(new BigDecimal("10000"))
                                                    .places(1)
                                                    .routeDocumentType(ONLY_CLIENT_RETURNS)
                                                    .build()
                                    )
                            )
                            .totalSum(new BigDecimal("10000"))
                            .totalItems(1)
                            .totalPlaces(1)
                            .build()
                    );
            return null;
        });
    }

    @Test
    void getTransferActFashion() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_PVZ.getWarehouseReturnId());
        var order = testFactory.create(order(sortingCenter, "1").isFashion(true).build())
                .accept().sort().get();
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = testFactory.determineRouteCell(route, order);
        routeCommandService.finishOutgoingRouteWithCell(new RouteFinishByCellsRequest(
                testFactory.getRouteIdForSortableFlow(route), new ScContext(user),
                List.of(cell.getId()),
                null,
                false
        ));
        transactionTemplate.execute(t -> {
            assertThat(
                    routeQueryService.getTransferAct(
                            new TransferActGetRequest(
                                    ONLY_CLIENT_RETURNS, sortingCenter,
                                    testFactory.getRoutable(route))).getOrders()
            ).isEmpty();
            assertThat(routeQueryService.getTransferAct(
                    new TransferActGetRequest(NORMAL, sortingCenter, testFactory.getRoutable(route))))
                    .isEqualTo(TransferActDto.builder()
                            .routeType(RouteType.OUTGOING_WAREHOUSE)
                            .number(testFactory.getRouteIdForSortableFlow(route) + "-1")
                            .date(LocalDate.now(clock))
                            .sender("Новый СЦ")
                            .executor("ООО Ромашка-Склад")
                            .orders(
                                    List.of(
                                            TransferActDto.Order.builder()
                                                    .externalId("HLP_PVZ_FSN_RET_1")
                                                    .items(1)
                                                    .totalSum(new BigDecimal("10000"))
                                                    .places(1)
                                                    .routeDocumentType(NORMAL)
                                                    .build()
                                    )
                            )
                            .totalSum(new BigDecimal("10000"))
                            .totalItems(1)
                            .totalPlaces(1)
                            .build()
                    );
            return null;
        });
    }

    @Test
    void getTransferActReturnPartialMultiplaceOrderAfterCourierShip() {
        var order = testFactory.createForToday(order(sortingCenter, "1").places("1", "2").build())
                .acceptPlaces().sortPlaces().ship().makeReturn().acceptPlaces("1").sortPlaces("1").get();
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = testFactory.determineRouteCell(route, order);
        routeCommandService.finishOutgoingRouteWithCell(new RouteFinishByCellsRequest(
                testFactory.getRouteIdForSortableFlow(route), new ScContext(user),
                List.of(cell.getId()),
                null,
                false
        ));
        transactionTemplate.execute(t -> {
            assertThat(routeQueryService.getTransferAct(
                    new TransferActGetRequest(ALL, sortingCenter, testFactory.getRoutable(route))))
                    .isEqualTo(TransferActDto.builder()
                            .routeType(RouteType.OUTGOING_WAREHOUSE)
                            .number(String.valueOf(testFactory.getRouteIdForSortableFlow(route)))
                            .date(LocalDate.now(clock))
                            .sender("Новый СЦ")
                            .executor("ООО Ромашка-Склад")
                            .orders(
                                    List.of(
                                            TransferActDto.Order.builder()
                                                    .externalId("1")
                                                    .items(1)
                                                    .totalSum(new BigDecimal("336.00"))
                                                    .places(1)
                                                    .routeDocumentType(NORMAL)
                                                    .build()
                                    )
                            )
                            .totalSum(new BigDecimal("336.00"))
                            .totalItems(1)
                            .totalPlaces(1)
                            .build()
                    );
            return null;
        });
    }

    @Test
    void getTransferActReturnPartialMultiplaceOrderAfterCourierShipIncoming() {
        var order = testFactory.createForToday(order(sortingCenter, "1").places("1", "2").build())
                .acceptPlaces().sortPlaces().ship().makeReturn()
                .acceptPlaces("1").sortPlaces("1").acceptPlaces("2").get();
        var route = testFactory.findPossibleIncomingCourierRoute(order).orElseThrow();
        transactionTemplate.execute(t -> {
            assertThat(routeQueryService.getTransferAct(
                    new TransferActGetRequest(ALL, sortingCenter, testFactory.getRoutable(route))))
                    .isEqualTo(TransferActDto.builder()
                            .routeType(RouteType.INCOMING_COURIER)
                            .number(String.valueOf(testFactory.getRouteIdForSortableFlow(route)))
                            .date(LocalDate.now(clock))
                            .courier("Иван Пивовар Таранов")
                            .sender("Новый СЦ")
                            .orders(
                                    List.of(
                                            TransferActDto.Order.builder()
                                                    .externalId("1")
                                                    .items(1)
                                                    .totalSum(new BigDecimal("336.00"))
                                                    .places(2)
                                                    .routeDocumentType(NORMAL)
                                                    .build()
                                    )
                            )
                            .totalSum(new BigDecimal("336.00"))
                            .totalItems(1)
                            .totalPlaces(2)
                            .build()
                    );
            return null;
        });
    }

    @Test
    void getTransferActReturnPartialMultiplaceOrder() {
        var order = testFactory.createForToday(order(sortingCenter, "1").places("1", "2").build())
                .cancel().acceptPlaces("1").sortPlaces("1").get();
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = testFactory.determineRouteCell(route, order);
        routeCommandService.finishOutgoingRouteWithCell(new RouteFinishByCellsRequest(
                testFactory.getRouteIdForSortableFlow(route), new ScContext(user),
                List.of(cell.getId()),
                null,
                false
        ));
        transactionTemplate.execute(t -> {
            assertThat(routeQueryService.getTransferAct(
                    new TransferActGetRequest(ALL, sortingCenter, testFactory.getRoutable(route))))
                    .isEqualTo(TransferActDto.builder()
                            .routeType(RouteType.OUTGOING_WAREHOUSE)
                            .number(String.valueOf(testFactory.getRouteIdForSortableFlow(route)))
                            .date(LocalDate.now(clock))
                            .sender("Новый СЦ")
                            .executor("ООО Ромашка-Склад")
                            .orders(
                                    List.of(
                                            TransferActDto.Order.builder()
                                                    .externalId("1")
                                                    .items(1)
                                                    .totalSum(new BigDecimal("336.00"))
                                                    .places(1)
                                                    .routeDocumentType(NORMAL)
                                                    .build()
                                    )
                            )
                            .totalSum(new BigDecimal("336.00"))
                            .totalItems(1)
                            .totalPlaces(1)
                            .build()
                    );
            return null;
        });
    }

    @Test
    void getTransferActReturnPartialMultiplaceOrderNextDay() {
        var order = testFactory.createForToday(order(sortingCenter, "1").places("1", "2").build())
                .cancel().acceptPlaces("1", "2").sortPlaces("1", "2").shipPlace("1").get();
        doReturn(clock.instant().plus(1, ChronoUnit.DAYS)).when(clock).instant();
        order = testFactory.rescheduleReturn(order, LocalDate.now(clock));
        var route = testFactory.findPossibleOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = testFactory.determineRouteCell(route, order);
        routeCommandService.finishOutgoingRouteWithCell(new RouteFinishByCellsRequest(
                testFactory.getRouteIdForSortableFlow(route), new ScContext(user),
                List.of(cell.getId()),
                null,
                false
        ));
        transactionTemplate.execute(t -> {
            assertThat(routeQueryService.getTransferAct(
                    new TransferActGetRequest(ALL, sortingCenter, testFactory.getRoutable(route))))
                    .isEqualTo(TransferActDto.builder()
                            .routeType(RouteType.OUTGOING_WAREHOUSE)
                            .number(String.valueOf(testFactory.getRouteIdForSortableFlow(route)))
                            .date(LocalDate.now(clock))
                            .sender("Новый СЦ")
                            .executor("ООО Ромашка-Склад")
                            .orders(
                                    List.of(
                                            TransferActDto.Order.builder()
                                                    .externalId("1")
                                                    .items(1)
                                                    .totalSum(new BigDecimal("336.00"))
                                                    .places(1)
                                                    .routeDocumentType(NORMAL)
                                                    .build()
                                    )
                            )
                            .totalSum(new BigDecimal("336.00"))
                            .totalItems(1)
                            .totalPlaces(1)
                            .build()
                    );
            return null;
        });
    }

    @Test
    void getTransferActReturnFullMultiplaceOrder() {
        var order = testFactory.createForToday(order(sortingCenter, "1").places("1", "2").build())
                .cancel().acceptPlaces("1").sortPlaces("1").acceptPlaces("2").sortPlaces("2").get();
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = testFactory.determineRouteCell(route, order);
        routeCommandService.finishOutgoingRouteWithCell(new RouteFinishByCellsRequest(
                testFactory.getRouteIdForSortableFlow(route), new ScContext(user),
                List.of(cell.getId()),
                null,
                false
        ));
        transactionTemplate.execute(t -> {
            assertThat(routeQueryService.getTransferAct(
                    new TransferActGetRequest(ALL, sortingCenter, testFactory.getRoutable(route))))
                    .isEqualTo(TransferActDto.builder()
                            .routeType(RouteType.OUTGOING_WAREHOUSE)
                            .number(String.valueOf(testFactory.getRouteIdForSortableFlow(route)))
                            .date(LocalDate.now(clock))
                            .sender("Новый СЦ")
                            .executor("ООО Ромашка-Склад")
                            .orders(
                                    List.of(
                                            TransferActDto.Order.builder()
                                                    .externalId("1")
                                                    .items(1)
                                                    .totalSum(new BigDecimal("336.00"))
                                                    .places(2)
                                                    .routeDocumentType(NORMAL)
                                                    .build()
                                    )
                            )
                            .totalSum(new BigDecimal("336.00"))
                            .totalItems(1)
                            .totalPlaces(2)
                            .build()
                    );
            return null;
        });
    }

    @Test
    void getTransferActIncomingCourierRoute() {
        var order = testFactory.createForToday(order(sortingCenter, "1").build())
                .accept().sort().ship().makeReturn().accept().get();
        testFactory.createForToday(order(sortingCenter, "2").build())
                .accept().sort().ship().makeReturn().accept().get();
        var route = testFactory.findPossibleIncomingCourierRoute(order).orElseThrow();
        transactionTemplate.execute(t -> {
            assertThat(routeQueryService.getTransferAct(
                    new TransferActGetRequest(ALL, sortingCenter, testFactory.getRoutable(route))))
                    .isEqualTo(TransferActDto.builder()
                            .routeType(RouteType.INCOMING_COURIER)
                            .number(String.valueOf(testFactory.getRouteIdForSortableFlow(route)))
                            .date(LocalDate.now(clock))
                            .sender("Новый СЦ")
                            .orders(
                                    List.of(
                                            TransferActDto.Order.builder()
                                                    .externalId("1")
                                                    .items(1)
                                                    .totalSum(new BigDecimal("336.00"))
                                                    .places(1)
                                                    .routeDocumentType(NORMAL)
                                                    .build(),
                                            TransferActDto.Order.builder()
                                                    .externalId("2")
                                                    .items(1)
                                                    .totalSum(new BigDecimal("336.00"))
                                                    .places(1)
                                                    .routeDocumentType(NORMAL)
                                                    .build()
                                    )
                            )
                            .totalSum(new BigDecimal("336.00").multiply(BigDecimal.valueOf(2)))
                            .totalItems(2)
                            .totalPlaces(2)
                            .courier(Objects.requireNonNull(order.getCourier()).getName())
                            .build()
                    );
            return null;
        });
    }

    @Test
    void getTransferActIncomingCourierRouteNoReturn() {
        var order = testFactory.createForToday(order(sortingCenter, "1").build())
                .accept().sort().ship().accept().get();
        testFactory.createForToday(order(sortingCenter, "2").build())
                .accept().sort().ship().accept().get();
        var route = testFactory.findPossibleIncomingCourierRoute(order).orElseThrow();
        transactionTemplate.execute(t -> {
            assertThat(routeQueryService.getTransferAct(
                    new TransferActGetRequest(ALL, sortingCenter, testFactory.getRoutable(route))))
                    .isEqualTo(TransferActDto.builder()
                            .routeType(RouteType.INCOMING_COURIER)
                            .number(String.valueOf(testFactory.getRouteIdForSortableFlow(route)))
                            .date(LocalDate.now(clock))
                            .sender("Новый СЦ")
                            .orders(
                                    List.of(
                                            TransferActDto.Order.builder()
                                                    .externalId("1")
                                                    .items(1)
                                                    .totalSum(new BigDecimal("336.00"))
                                                    .places(1)
                                                    .routeDocumentType(NORMAL)
                                                    .build(),
                                            TransferActDto.Order.builder()
                                                    .externalId("2")
                                                    .items(1)
                                                    .totalSum(new BigDecimal("336.00"))
                                                    .places(1)
                                                    .routeDocumentType(NORMAL)
                                                    .build()
                                    )
                            )
                            .totalSum(new BigDecimal("336.00").multiply(BigDecimal.valueOf(2)))
                            .totalItems(2)
                            .totalPlaces(2)
                            .courier(Objects.requireNonNull(order.getCourier()).getName())
                            .build()
                    );
            return null;
        });
    }

    @Test
    void getTransferActOnlyShippedOrdersCourier() {
        var order = testFactory.createForToday(order(sortingCenter, "1").build())
                .accept().sort().ship().get();
        var orderNotShipped = testFactory.createForToday(order(sortingCenter, "2").build())
                .accept().sort().get();
        var route = testFactory.findOutgoingCourierRoute(orderNotShipped).orElseThrow();
        transactionTemplate.execute(t -> {
            assertThat(routeQueryService.getTransferAct(
                    new TransferActGetRequest(ALL, sortingCenter, testFactory.getRoutable(route))))
                    .isEqualTo(TransferActDto.builder()
                            .routeType(RouteType.OUTGOING_COURIER)
                            .number(String.valueOf(testFactory.getRouteIdForSortableFlow(route)))
                            .date(LocalDate.now(clock))
                            .sender("Новый СЦ")
                            .orders(
                                    List.of(
                                            TransferActDto.Order.builder()
                                                    .externalId("1")
                                                    .items(1)
                                                    .totalSum(new BigDecimal("336.00"))
                                                    .places(1)
                                                    .routeDocumentType(NORMAL)
                                                    .build()
                                    )
                            )
                            .dataCollectionTerminalLiabilityCaution(true)
                            .totalSum(new BigDecimal("336.00"))
                            .totalItems(1)
                            .totalPlaces(1)
                            .courier(Objects.requireNonNull(order.getCourier()).getName())
                            .build()
                    );
            return null;
        });
    }

    @Test
    void getTransferActOnlyShippedOrdersWarehouse() {
        testFactory.createForToday(order(sortingCenter, "1").build())
                .cancel().accept().sort().ship().get();
        var orderNotShipped = testFactory.createForToday(order(sortingCenter, "2").build())
                .cancel().accept().sort().get();
        var route = testFactory.findOutgoingWarehouseRoute(orderNotShipped).orElseThrow();
        transactionTemplate.execute(t -> {
            assertThat(routeQueryService.getTransferAct(
                    new TransferActGetRequest(ALL, sortingCenter, testFactory.getRoutable(route))))
                    .isEqualTo(TransferActDto.builder()
                            .routeType(RouteType.OUTGOING_WAREHOUSE)
                            .number(String.valueOf(testFactory.getRouteIdForSortableFlow(route)))
                            .date(LocalDate.now(clock))
                            .sender("Новый СЦ")
                            .executor("ООО Ромашка-Склад")
                            .orders(
                                    List.of(
                                            TransferActDto.Order.builder()
                                                    .externalId("1")
                                                    .items(1)
                                                    .totalSum(new BigDecimal("336.00"))
                                                    .places(1)
                                                    .routeDocumentType(NORMAL)
                                                    .build()
                                    )
                            )
                            .totalSum(new BigDecimal("336.00"))
                            .totalItems(1)
                            .totalPlaces(1)
                            .build()
                    );
            return null;
        });
    }

    @Test
    void getTransferActWithCourierCompanyName() {
        var courierDto = new CourierDto(10L, "Иванов Иван Иванович", null);
        courierDto.setCompanyName("Рога и копыта");
        var order = testFactory.createForToday(order(sortingCenter, "1").build(), courierDto)
                .accept().sort().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = testFactory.determineRouteCell(route, order);
        routeCommandService.finishOutgoingRouteWithCell(new RouteFinishByCellsRequest(
                testFactory.getRouteIdForSortableFlow(route), new ScContext(user),
                List.of(cell.getId()),
                null,
                false
        ));
        transactionTemplate.execute(t -> {
            assertThat(routeQueryService.getTransferAct(
                    new TransferActGetRequest(ALL, sortingCenter, testFactory.getRoutable(route))))
                    .extracting(TransferActDto::getExecutor)
                    .isEqualTo(courierDto.getCompanyName());
            return null;
        });
    }

    @Test
    void getTransferActWarehouseReturnOnlyDamagedOrders() {
        var damagedOrdersCell = testFactory.storedCell(sortingCenter,
                "ПЗ-1",
                CellType.RETURN,
                CellSubType.RETURN_DAMAGED);
        var damagedOrder = testFactory.createForToday(
                        CreateOrderParams.builder()
                                .sortingCenter(sortingCenter)
                                .externalId("1")
                                .warehouseCanProcessDamagedOrders(true)
                                .build())
                .accept().markOrderAsDamaged().sort(damagedOrdersCell.getId()).ship().get();
        testFactory.createForToday(
                        CreateOrderParams.builder()
                                .sortingCenter(sortingCenter)
                                .externalId("2")
                                .warehouseCanProcessDamagedOrders(true)
                                .build())
                .cancel().accept().sort().ship().get();
        var routeByDamagedOrder = testFactory.findPossibleOutgoingWarehouseRoute(damagedOrder).orElseThrow();

        Long routeId = testFactory.getRouteIdForSortableFlow(routeByDamagedOrder.getId());
        var transferActDto = TransferActDto.builder()
                .routeType(RouteType.OUTGOING_WAREHOUSE)
                .number(routeId + "-2")
                .date(LocalDate.now(clock))
                .sender("Новый СЦ")
                .executor("ООО Ромашка-Склад")
                .orders(
                        List.of(
                                TransferActDto.Order.builder()
                                        .externalId(damagedOrder.getExternalId())
                                        .items(1)
                                        .totalSum(new BigDecimal("336.00"))
                                        .places(1)
                                        .routeDocumentType(ONLY_DAMAGED)
                                        .build()
                        )
                )
                .totalSum(new BigDecimal("336.00"))
                .totalItems(1)
                .totalPlaces(1)
                .build();
        transactionTemplate.execute(t -> {
            assertThat(routeQueryService.getTransferAct(new
                    TransferActGetRequest(ONLY_DAMAGED, sortingCenter, testFactory.getRoutable(routeByDamagedOrder))))
                    .isEqualTo(transferActDto);
            return null;
        });
    }

    @Test
    void getTransferActWarehouseReturnOnlyNotDamagedOrders() {
        var damagedOrdersCell = testFactory.storedCell(sortingCenter,
                "ПЗ-1",
                CellType.RETURN,
                CellSubType.RETURN_DAMAGED);
        var damagedOrder = testFactory.createForToday(
                        CreateOrderParams.builder()
                                .sortingCenter(sortingCenter)
                                .externalId("1")
                                .warehouseCanProcessDamagedOrders(true)
                                .build())
                .accept().markOrderAsDamaged().sort(damagedOrdersCell.getId()).ship().get();
        var notDamagedOrder = testFactory.createForToday(
                        CreateOrderParams.builder()
                                .sortingCenter(sortingCenter)
                                .externalId("2")
                                .warehouseCanProcessDamagedOrders(true)
                                .build())
                .cancel().accept().sort().ship().get();
        var routeByNotDamagedOrder = testFactory.findPossibleOutgoingWarehouseRoute(notDamagedOrder).orElseThrow();

        long routeIdForSo = testFactory.getRouteIdForSortableFlow(routeByNotDamagedOrder.getId());

        var transferActDto = TransferActDto.builder()
                .routeType(RouteType.OUTGOING_WAREHOUSE)
                .number(routeIdForSo + "-2")
                .date(LocalDate.now(clock))
                .sender("Новый СЦ")
                .executor("ООО Ромашка-Склад")
                .orders(
                        List.of(
                                TransferActDto.Order.builder()
                                        .externalId(damagedOrder.getExternalId())
                                        .items(1)
                                        .totalSum(new BigDecimal("336.00"))
                                        .places(1)
                                        .routeDocumentType(ONLY_DAMAGED)
                                        .build()
                        )
                )
                .totalSum(new BigDecimal("336.00"))
                .totalItems(1)
                .totalPlaces(1)
                .build();

        transactionTemplate.execute(t -> {
            TransferActGetRequest request = new TransferActGetRequest(
                    ONLY_DAMAGED, sortingCenter, testFactory.getRoutable(routeByNotDamagedOrder));
            assertThat(routeQueryService.getTransferAct(
                    request))
                    .isEqualTo(transferActDto);
            return null;
        });
    }

    @Test
    void getTransferActWarehouseReturnAllOrders() {
        var damagedOrdersCell = testFactory.storedCell(sortingCenter,
                "ПЗ-1",
                CellType.RETURN,
                CellSubType.RETURN_DAMAGED);
        var damagedOrder = testFactory.createForToday(
                        CreateOrderParams.builder()
                                .sortingCenter(sortingCenter)
                                .externalId("1")
                                .warehouseCanProcessDamagedOrders(true)
                                .build())
                .accept().markOrderAsDamaged().sort(damagedOrdersCell.getId()).ship().get();
        var notDamagedOrder = testFactory.createForToday(
                        CreateOrderParams.builder()
                                .sortingCenter(sortingCenter)
                                .externalId("2")
                                .warehouseCanProcessDamagedOrders(true)
                                .build())
                .cancel().accept().sort().ship().get();
        var routeByDamagedOrder = testFactory.findPossibleOutgoingWarehouseRoute(damagedOrder).orElseThrow();
        var routeByNotDamagedOrder = testFactory.findPossibleOutgoingWarehouseRoute(notDamagedOrder).orElseThrow();
        assertThat(routeByDamagedOrder).isEqualTo(routeByNotDamagedOrder);

        Long routeId = testFactory.getRouteIdForSortableFlow(routeByDamagedOrder.getId());
        var transferActDto = TransferActDto.builder()
                .routeType(RouteType.OUTGOING_WAREHOUSE)
                .number(String.valueOf(routeId))
                .date(LocalDate.now(clock))
                .sender("Новый СЦ")
                .executor("ООО Ромашка-Склад")
                .orders(
                        List.of(
                                TransferActDto.Order.builder()
                                        .externalId(damagedOrder.getExternalId())
                                        .items(1)
                                        .totalSum(new BigDecimal("336.00"))
                                        .places(1)
                                        .routeDocumentType(ONLY_DAMAGED)
                                        .build(),
                                TransferActDto.Order.builder()
                                        .externalId(notDamagedOrder.getExternalId())
                                        .items(1)
                                        .totalSum(new BigDecimal("336.00"))
                                        .places(1)
                                        .routeDocumentType(NORMAL)
                                        .build()

                        )
                )
                .totalSum(new BigDecimal("336.00").multiply(BigDecimal.valueOf(2)))
                .totalItems(2)
                .totalPlaces(2)
                .build();
        var transferActOnlyDamagedDto = TransferActDto.builder()
                .routeType(RouteType.OUTGOING_WAREHOUSE)
                .number(routeId + "-2")
                .date(LocalDate.now(clock))
                .sender("Новый СЦ")
                .executor("ООО Ромашка-Склад")
                .orders(
                        List.of(
                                TransferActDto.Order.builder()
                                        .externalId(damagedOrder.getExternalId())
                                        .items(1)
                                        .totalSum(new BigDecimal("336.00"))
                                        .places(1)
                                        .routeDocumentType(ONLY_DAMAGED)
                                        .build()
                        )
                )
                .totalSum(new BigDecimal("336.00").multiply(BigDecimal.valueOf(1)))
                .totalItems(1)
                .totalPlaces(1)
                .build();
        transactionTemplate.execute(t -> {
            assertThat(routeQueryService.getTransferAct(
                    new TransferActGetRequest(ALL, sortingCenter, testFactory.getRoutable(routeByDamagedOrder))))
                    .isEqualTo(transferActDto);
            assertThat(routeQueryService.getTransferAct(
                    new TransferActGetRequest(ONLY_DAMAGED, sortingCenter, testFactory.getRoutable(routeByDamagedOrder))))
                    .isEqualTo(transferActOnlyDamagedDto);
            return null;
        });
    }

    @Test
    void getTransferActIncomingCourierClientReturn() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_PVZ.getWarehouseReturnId());
        testFactory.storedFakeReturnDeliveryService();

        Long courierId = 321L;
        var courierDto = new CourierDto(courierId, "Курьер с возратами", null);
        String clientReturnLocker = "VOZVRAT_SF_PS_1234";

        var scOrder = testFactory.createClientReturnForToday(
                sortingCenter.getId(),
                sortingCenter.getToken(),
                sortingCenter.getYandexId(),
                courierDto,
                clientReturnLocker
        ).accept().get();
        var incomingCourierRoute = testFactory.findPossibleIncomingCourierRoute(scOrder).orElseThrow();

        Long routeId = testFactory.getRouteIdForSortableFlow(incomingCourierRoute);
        var transferActDto = TransferActDto.builder()
                .routeType(RouteType.INCOMING_COURIER)
                .number(routeId + "-3")
                .date(LocalDate.now(clock))
                .sender("Новый СЦ")
                .orders(
                        List.of(
                                TransferActDto.Order.builder()
                                        .externalId(scOrder.getExternalId())
                                        .items(1)
                                        .places(1)
                                        .routeDocumentType(ONLY_CLIENT_RETURNS)
                                        .build()
                        )
                )
                .totalSum(BigDecimal.valueOf(0))
                .totalItems(1)
                .totalPlaces(1)
                .courier("Курьер с возратами")
                .build();
        transactionTemplate.execute(t -> {
            assertThat(routeQueryService.getTransferAct(
                    new TransferActGetRequest(
                            ONLY_CLIENT_RETURNS, sortingCenter, testFactory.getRoutable(incomingCourierRoute))))
                    .isEqualTo(transferActDto);

            var transferActDto2 = transferActDto.withNumber(String.valueOf(routeId));

            assertThat(routeQueryService.getTransferAct(
                    new TransferActGetRequest(ALL, sortingCenter, testFactory.getRoutable(incomingCourierRoute))))
                    .isEqualTo(transferActDto2);
            return null;
        });
    }

    @Test
    void getTransferActOutgoingWarehouseClientReturn() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_PVZ.getWarehouseReturnId());
        testFactory.storedFakeReturnDeliveryService();

        Long courierId = 321L;
        var courierDto = new CourierDto(courierId, "Курьер с возратами", null);
        String clientReturnLocker = "VOZVRAT_SF_PS_1234";

        var clientReturnOrder = testFactory.createClientReturnForToday(
                sortingCenter.getId(),
                sortingCenter.getToken(),
                sortingCenter.getYandexId(),
                courierDto,
                clientReturnLocker
        ).accept().sort().ship().get();
        var outgoingWarehouseRoute = testFactory.findPossibleOutgoingWarehouseRoute(clientReturnOrder).orElseThrow();

        Long routeId = testFactory.getRouteIdForSortableFlow(outgoingWarehouseRoute.getId());

        var normalTransferActDto = TransferActDto.builder()
                .routeType(RouteType.OUTGOING_WAREHOUSE)
                .number(routeId + "-1")
                .date(LocalDate.now(clock))
                .sender("Новый СЦ")
                .executor("ООО Ромашка-Склад")
                .orders(List.of())
                .totalSum(BigDecimal.valueOf(0))
                .totalItems(0)
                .totalPlaces(0)
                .build();
        transactionTemplate.execute(t -> {
                    assertThat(routeQueryService.getTransferAct(
                            new TransferActGetRequest(NORMAL, sortingCenter, testFactory.getRoutable(outgoingWarehouseRoute))))
                            .isEqualTo(normalTransferActDto);
            return null;
        });

        var allTransferActDto = TransferActDto.builder()
                .routeType(RouteType.OUTGOING_WAREHOUSE)
                .number(String.valueOf(routeId))
                .date(LocalDate.now(clock))
                .sender("Новый СЦ")
                .executor("ООО Ромашка-Склад")
                .orders(
                        List.of(
                                TransferActDto.Order.builder()
                                        .externalId(clientReturnOrder.getExternalId())
                                        .items(1)
                                        .totalSum(BigDecimal.valueOf(10000))
                                        .places(1)
                                        .routeDocumentType(ONLY_CLIENT_RETURNS)
                                        .build()
                        )
                )
                .totalSum(BigDecimal.valueOf(10000))
                .totalItems(1)
                .totalPlaces(1)
                .build();
        transactionTemplate.execute(t -> {
            assertThat(routeQueryService.getTransferAct(new TransferActGetRequest(

                    ALL,
                    sortingCenter, testFactory.getRoutable(outgoingWarehouseRoute))))
                    .isEqualTo(allTransferActDto);
            return null;
        });
    }

    @Test
    void getTransferActIncomingCourierClientReturnOnly() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_PVZ.getWarehouseReturnId());
        testFactory.storedFakeReturnDeliveryService();

        Long courierId = 321L;
        var courierDto = new CourierDto(courierId, "Курьер с возратами", null);
        String clientReturnLocker = "VOZVRAT_SF_PS_1234";

        var scOrder = testFactory.createClientReturnForToday(
                sortingCenter.getId(),
                sortingCenter.getToken(),
                sortingCenter.getYandexId(),
                courierDto,
                clientReturnLocker
        ).accept().get();
        testFactory.create(order(sortingCenter)
                        .externalId("regular-1")
                        .warehouseReturnId(ClientReturnBarcodePrefix.CLIENT_RETURN_PVZ.getWarehouseReturnId())
                        .shipmentDate(LocalDate.now(clock)).build())
                .updateCourier(courierDto)
                .accept().sort().ship().accept().get();
        var incomingCourierRoute = testFactory.findPossibleIncomingCourierRoute(scOrder).orElseThrow();

        Long routeId = testFactory.getRouteIdForSortableFlow(incomingCourierRoute);
        var transferActDto = TransferActDto.builder()
                .routeType(RouteType.INCOMING_COURIER)
                .number(routeId + "-3")
                .date(LocalDate.now(clock))
                .sender("Новый СЦ")
                .orders(
                        List.of(
                                TransferActDto.Order.builder()
                                        .externalId(scOrder.getExternalId())
                                        .items(1)
                                        .places(1)
                                        .routeDocumentType(ONLY_CLIENT_RETURNS)
                                        .build()
                        )
                )
                .totalSum(BigDecimal.valueOf(0))
                .totalItems(1)
                .totalPlaces(1)
                .courier("Курьер с возратами")
                .build();
        transactionTemplate.execute(t -> {
            assertThat(routeQueryService.getTransferAct(
                    new TransferActGetRequest(ONLY_CLIENT_RETURNS, sortingCenter,
                            testFactory.getRoutable(incomingCourierRoute))))
                    .isEqualTo(transferActDto);
            return null;
        });
    }

    @Test
    void getOrderRegistryOutgoingWarehouseRoute() {
        var order1 = testFactory.createForToday(
                order(sortingCenter, "1").places("1", "2").build()
        ).cancel().acceptPlaces("1").sortPlaces("1").ship().get();

        var damagedOrdersCell = testFactory.storedCell(sortingCenter,
                "ПЗ-1",
                CellType.RETURN,
                CellSubType.RETURN_DAMAGED);
        testFactory.createForToday(
                order(sortingCenter, "2")
                        .warehouseCanProcessDamagedOrders(true)
                        .build()
        ).cancel().accept().markOrderAsDamaged().sort(damagedOrdersCell.getId()).ship().get();

        var route = testFactory.findPossibleOutgoingWarehouseRoute(order1).orElseThrow();

        var result = routeQueryService.getOrderRegistry(testFactory.getRouteIdForSortableFlow(route),
                sortingCenter, ordersRegistryFilter(NORMAL));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getExternalOrderId()).isEqualTo(order1.getExternalId());
    }

    @Test
    void getOrderRegistryOnlyDamaged() {
        var order1 = testFactory.createForToday(
                order(sortingCenter, "1").places("1", "2").build()
        ).cancel().acceptPlaces("1").sortPlaces("1").ship().get();
        var damagedOrdersCell = testFactory.storedCell(sortingCenter,
                "ПЗ-1",
                CellType.RETURN,
                CellSubType.RETURN_DAMAGED);
        var order2 = testFactory.createForToday(
                order(sortingCenter, "2")
                        .warehouseCanProcessDamagedOrders(true)
                        .build()
        ).cancel().accept().markOrderAsDamaged().sort(damagedOrdersCell.getId()).ship().get();
        var route = testFactory.findPossibleOutgoingWarehouseRoute(order1).orElseThrow();

        var result = routeQueryService.getOrderRegistry(testFactory.getRouteIdForSortableFlow(route),
                sortingCenter, ordersRegistryFilter(ONLY_DAMAGED));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getExternalOrderId()).isEqualTo(order2.getExternalId());
    }

    @Test
    void getOrderRegistryNotClientReturns() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_PVZ.getWarehouseReturnId());
        testFactory.storedDeliveryService(DELIVERY_SERVICE_YA_ID, true);
        Long courierId = 321L;
        var courierDto = new CourierDto(courierId, "Курьер с возратами", null);
        String clientReturnLocker = "VOZVRAT_SF_PS_1234";

        var scOrder = testFactory.createClientReturnForToday(
                sortingCenter.getId(),
                sortingCenter.getToken(),
                sortingCenter.getYandexId(),
                courierDto,
                clientReturnLocker
        ).accept().sort().ship().get();

        var route = testFactory.findPossibleOutgoingWarehouseRoute(scOrder).orElseThrow();

        var result = routeQueryService.getOrderRegistry(testFactory.getRouteIdForSortableFlow(route),
                sortingCenter, ordersRegistryFilter(NORMAL));
        assertThat(result).hasSize(0);
    }

    @Test
    void getOrderRegistryOnlyClientReturns() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_PVZ.getWarehouseReturnId());
        testFactory.storedFakeReturnDeliveryService();
        Long courierId = 321L;
        var courierDto = new CourierDto(courierId, "Курьер с возратами", null);
        String clientReturnLocker = "VOZVRAT_SF_PS_1234";

        var scOrder = testFactory.createClientReturnForToday(
                sortingCenter.getId(),
                sortingCenter.getToken(),
                sortingCenter.getYandexId(),
                courierDto,
                clientReturnLocker
        ).accept().sort().ship().get();

        var route = testFactory.findPossibleOutgoingWarehouseRoute(scOrder).orElseThrow();

        var result = routeQueryService.getOrderRegistry(testFactory.getRouteIdForSortableFlow(route),
                sortingCenter, ordersRegistryFilter(ONLY_CLIENT_RETURNS));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getExternalOrderId()).isEqualTo(scOrder.getExternalId());
    }

    @Test
    void getOrderRegistryOutgoingCourierRouteThrowException() {
        var order = testFactory.createForToday(order(sortingCenter, "1").build())
                .accept().sort().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();

        assertThatThrownBy(() -> routeQueryService.getOrderRegistry(testFactory.getRouteIdForSortableFlow(route),
                sortingCenter, ordersRegistryFilter(NORMAL)))
                .isInstanceOf(TplInvalidActionException.class);
    }

    @Test
    void getOrderRegistryIncomingWarehouseThrowException() {
        var order = testFactory.createForToday(order(sortingCenter, "1").build()).get();
        var route = testFactory.findPossibleIncomingWarehouseRouteByOrderId(order.getId()).orElseThrow();


        assertThatThrownBy(() -> routeQueryService.getOrderRegistry(testFactory.getRouteIdForSortableFlow(route),
                sortingCenter, ordersRegistryFilter(NORMAL)))
                .isInstanceOf(TplInvalidActionException.class);
    }

    @Test
    void getOrderRegistryIncomingCourierThrowException() {
        var order = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().ship().makeReturn().get();
        var route = testFactory.findPossibleIncomingCourierRoute(order).orElseThrow();

        assertThatThrownBy(() -> routeQueryService.getOrderRegistry(testFactory.getRouteIdForSortableFlow(route),
                sortingCenter, ordersRegistryFilter(NORMAL)))
                .isInstanceOf(TplInvalidActionException.class);
    }

    @Test
    void getRoutesIncomingWhenOrdersInManyDaysTest() {
        testFactory.setupMockClock(clock, Clock.offset(clock, Duration.ofHours(-72)).instant());
        var cell = testFactory.storedCell(sortingCenter, "b-1", CellType.BUFFER);

        var oldPlace = testFactory.create(order(sortingCenter)
                        .shipmentDate(LocalDate.now(clock)).externalId("o2").build())
                .updateCourier(testFactory.defaultCourier())
                .accept()
                .getPlace();
        testFactory.setupMockClock(clock);
        testFactory.createOrderForToday(sortingCenter)
                .accept()
                .sort();

        placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(oldPlace), user), cell.getId(), false);

        var route = routeQueryService.getRoutesWithStatsForPi(
                        LocalDate.now(clock), sortingCenter, RouteType.INCOMING_WAREHOUSE, Pageable.unpaged(), null)
                .stream().findFirst().orElseThrow();
        assertThat(route.getAcceptedButNotSorted()).isEqualTo(0);
        assertThat(route.getOrdersAccepted()).isEqualTo(1);
        assertThat(route.getOrdersInCell()).isEqualTo(1);
    }

    @Test
    void acceptedButNotSortedSeesLotSort() {
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.createForToday(order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build())
                .accept().sortToLot().get();
        var route = routeQueryService.getRoutesWithStatsForPi(
                        LocalDate.now(clock), sortingCenter, RouteType.INCOMING_WAREHOUSE, Pageable.unpaged(), null)
                .stream().findFirst().orElseThrow();

        assertThat(route.getAcceptedButNotSorted()).isEqualTo(0);
        assertThat(route.getOrdersAccepted()).isEqualTo(1);
        assertThat(route.getOrdersInCell()).isEqualTo(1);
    }


    /**
     * Логические одинаковые запросы на создание маршрута, но для разных заказов
     */
    @Test
    public void sameRouteCreateRequest() {
        var courier = testFactory.storedCourier();
        var order1 = testFactory.create(order(sortingCenter).shipmentDate(LocalDate.now(clock))
                        .externalId("1").build())
                .updateCourier(courier).updateShipmentDate(LocalDate.now(clock))
                .get();
        var order2 = testFactory.create(order(sortingCenter).shipmentDate(LocalDate.now(clock))
                        .externalId("2").build())
                .updateCourier(courier).updateShipmentDate(LocalDate.now(clock))
                .get();
        Set<RouteCreateRequest> requests = new HashSet<>();
        requests.add(
                new RouteCreateRequest(
                        RouteType.OUTGOING_COURIER,
                        sortingCenter,
                        LocalDate.now(clock),
                        new LocalDateInterval(LocalDate.now(clock), LocalDate.now(clock)),
                        LocalTime.now(clock),
                        null,
                        courier,
                        order1,
                        null
                ));
        requests.add(
                new RouteCreateRequest(
                        RouteType.OUTGOING_COURIER,
                        sortingCenter,
                        LocalDate.now(clock),
                        new LocalDateInterval(LocalDate.now(clock), LocalDate.now(clock)),
                        LocalTime.now(clock),
                        null,
                        courier,
                        order2,
                        null
                ));
        assertThat(requests).hasSize(1);
        requests.clear();
        requests.add(
                new RouteCreateRequest(
                        RouteType.INCOMING_WAREHOUSE,
                        sortingCenter,
                        LocalDate.now(clock),
                        new LocalDateInterval(LocalDate.now(clock), LocalDate.now(clock)),
                        LocalTime.now(clock),
                        order1.getWarehouseFrom(),
                        null,
                        order1,
                        null
                ));
        requests.add(
                new RouteCreateRequest(
                        RouteType.INCOMING_WAREHOUSE,
                        sortingCenter,
                        LocalDate.now(clock),
                        new LocalDateInterval(LocalDate.now(clock), LocalDate.now(clock)),
                        LocalTime.now(clock),
                        order2.getWarehouseFrom(),
                        null,
                        order2,
                        null
                ));
        assertThat(requests).hasSize(1);
    }

    /**
     * Разные запросы на создание маршрутов. Отличаются курьером
     */
    @Test
    void differentRouteCreateRequests() {
        var courier1 = testFactory.storedCourier(1L);
        var courier2 = testFactory.storedCourier(2L);
        var order1 = testFactory.create(order(sortingCenter).shipmentDate(LocalDate.now(clock))
                        .externalId("1").build())
                .updateCourier(courier1).updateShipmentDate(LocalDate.now(clock))
                .get();
        var order2 = testFactory.create(order(sortingCenter).shipmentDate(LocalDate.now(clock))
                        .externalId("2").build())
                .updateCourier(courier2).updateShipmentDate(LocalDate.now(clock))
                .get();
        Set<RouteCreateRequest> requests = new HashSet<>();
        requests.add(
                new RouteCreateRequest(
                        RouteType.OUTGOING_COURIER,
                        sortingCenter,
                        LocalDate.now(clock),
                        new LocalDateInterval(LocalDate.now(clock), LocalDate.now(clock)),
                        LocalTime.now(clock),
                        null,
                        courier1,
                        order1,
                        null
                ));
        requests.add(
                new RouteCreateRequest(
                        RouteType.OUTGOING_COURIER,
                        sortingCenter,
                        LocalDate.now(clock),
                        new LocalDateInterval(LocalDate.now(clock), LocalDate.now(clock)),
                        LocalTime.now(clock),
                        null,
                        courier2,
                        order2,
                        null
                ));
        assertThat(requests).hasSize(2);
        requests.clear();
        requests.add(
                new RouteCreateRequest(
                        RouteType.INCOMING_WAREHOUSE,
                        sortingCenter,
                        LocalDate.now(clock),
                        new LocalDateInterval(LocalDate.now(clock), LocalDate.now(clock)),
                        LocalTime.now(clock),
                        order1.getWarehouseFrom(),
                        null,
                        order1,
                        null
                ));
        requests.add(
                new RouteCreateRequest(
                        RouteType.INCOMING_WAREHOUSE,
                        sortingCenter,
                        LocalDate.now(clock),
                        new LocalDateInterval(LocalDate.now(clock), LocalDate.now(clock)),
                        LocalTime.now(clock),
                        order2.getWarehouseFrom(),
                        null,
                        order2,
                        null
                ));
        assertThat(requests).hasSize(2);
    }

    /**
     * Разные запросы на создание маршрутов. Отличаются интервалом
     */
    @Test
    void differentCreateRouteRequestsDiffIntervals() {
        var courier1 = testFactory.storedCourier(1L);
        var order1 = testFactory.create(order(sortingCenter).shipmentDate(LocalDate.now(clock))
                        .externalId("1").build())
                .updateCourier(courier1).updateShipmentDate(LocalDate.now(clock))
                .get();
        var order2 = testFactory.create(order(sortingCenter).shipmentDate(LocalDate.now(clock))
                        .externalId("2").build())
                .updateCourier(courier1).updateShipmentDate(LocalDate.now(clock))
                .get();
        Set<RouteCreateRequest> requests = new HashSet<>();
        requests.add(
                new RouteCreateRequest(
                        RouteType.OUTGOING_COURIER,
                        sortingCenter,
                        LocalDate.now(clock),
                        new LocalDateInterval(LocalDate.now(clock), LocalDate.now(clock)),
                        LocalTime.now(clock),
                        null,
                        courier1,
                        order1,
                        null
                ));
        requests.add(
                new RouteCreateRequest(
                        RouteType.OUTGOING_COURIER,
                        sortingCenter,
                        LocalDate.now(clock),
                        new LocalDateInterval(LocalDate.now(clock), LocalDate.now(clock).plusDays(1)),
                        LocalTime.now(clock),
                        null,
                        courier1,
                        order2,
                        null
                ));
        assertThat(requests).hasSize(2);
        requests.clear();
        requests.add(
                new RouteCreateRequest(
                        RouteType.INCOMING_WAREHOUSE,
                        sortingCenter,
                        LocalDate.now(clock),
                        new LocalDateInterval(LocalDate.now(clock), LocalDate.now(clock)),
                        LocalTime.now(clock),
                        order1.getWarehouseFrom(),
                        null,
                        order1,
                        null
                ));
        requests.add(
                new RouteCreateRequest(
                        RouteType.INCOMING_WAREHOUSE,
                        sortingCenter,
                        LocalDate.now(clock),
                        new LocalDateInterval(LocalDate.now(clock), LocalDate.now(clock).plusDays(1)),
                        LocalTime.now(clock),
                        order2.getWarehouseFrom(),
                        null,
                        order2,
                        null
                ));
        assertThat(requests).hasSize(2);
    }

    /**
     * Разные запросы на создание маршрутов. Отличаются датой отгрузки
     */
    @Test
    void differentCreateRouteRequestsDiffIncomingDate() {
        var courier1 = testFactory.storedCourier(1L);
        var order1 = testFactory.create(order(sortingCenter)
                        .shipmentDate(LocalDate.now(clock))
                        .externalId("1").build())
                .updateCourier(courier1).updateShipmentDate(LocalDate.now(clock))
                .get();
        var order2 = testFactory.create(order(sortingCenter).shipmentDate(LocalDate.now(clock))
                        .externalId("2").build())
                .updateCourier(courier1).updateShipmentDate(LocalDate.now(clock).plusDays(1))
                .get();
        Set<RouteCreateRequest> requests = new HashSet<>();
        requests.add(
                new RouteCreateRequest(
                        RouteType.OUTGOING_COURIER,
                        sortingCenter,
                        LocalDate.now(clock),
                        new LocalDateInterval(LocalDate.now(clock), LocalDate.now(clock)),
                        LocalTime.now(clock),
                        null,
                        courier1,
                        order1,
                        null
                ));
        requests.add(
                new RouteCreateRequest(
                        RouteType.OUTGOING_COURIER,
                        sortingCenter,
                        LocalDate.now(clock),
                        new LocalDateInterval(LocalDate.now(clock), LocalDate.now(clock)),
                        LocalTime.now(clock),
                        null,
                        courier1,
                        order2,
                        null
                ));
        assertThat(requests).hasSize(2);
    }

    /**
     * Разные запросы на создание маршрутов. Первый заказ выброшен из маршрутизации - должен идти в ячейку
     * дропнутых заказов
     */
    @Test
    void differentCreateRouteRequestsDiffCellType() {
        var courier1 = testFactory.storedCourier(1L);
        var order1 = testFactory.create(order(sortingCenter)
                        .shipmentDate(LocalDate.now(clock))
                        .externalId("1").build())
                .updateShipmentDate(LocalDate.now(clock))
                .get(); //dropped order
        var order2 = testFactory.create(order(sortingCenter).shipmentDate(LocalDate.now(clock))
                        .externalId("2").build())
                .updateCourier(courier1).updateShipmentDate(LocalDate.now(clock))
                .get();
        Set<RouteCreateRequest> requests = new HashSet<>();
        requests.add(
                new RouteCreateRequest(
                        RouteType.OUTGOING_COURIER,
                        sortingCenter,
                        LocalDate.now(clock),
                        new LocalDateInterval(LocalDate.now(clock), LocalDate.now(clock)),
                        LocalTime.now(clock),
                        null,
                        courier1,
                        order1,
                        null
                ));
        requests.add(
                new RouteCreateRequest(
                        RouteType.OUTGOING_COURIER,
                        sortingCenter,
                        LocalDate.now(clock),
                        new LocalDateInterval(LocalDate.now(clock), LocalDate.now(clock)),
                        LocalTime.now(clock),
                        null,
                        courier1,
                        order2,
                        null
                ));
        assertThat(requests).hasSize(2);
    }

    @Test
    void getReturnPalletLabel() {
        var order = testFactory.createOrderForToday(sortingCenter).cancel().accept().get();
        var route = testFactory.findOutgoingWarehouseRoute(
                testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var actual = routeQueryService.getReturnPalletLabel(testFactory.getRouteIdForSortableFlow(route), sortingCenter, NORMAL);
        assertThat(actual).isEqualTo(new ReturnPalletLabelDto(
                sortingCenter.getAddress(),
                Objects.requireNonNull(route.getWarehouseTo()).getIncorporation(),
                testFactory.getRouteIdForSortableFlow(route) + "-1",
                testFactory.getRouteIdForSortableFlow(route) + "-1",
                NORMAL.getReturnCategory()
        ));
    }

    @Test
    void getReturnPalletLabelForCourierRoute() {
        var order = testFactory.createOrderForToday(sortingCenter).accept().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        assertThatThrownBy(() -> routeQueryService.getReturnPalletLabel(
                testFactory.getRouteIdForSortableFlow(route), sortingCenter, NORMAL))
                .isInstanceOf(TplInvalidActionException.class);
    }

}
