package ru.yandex.market.sc.api.features;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.sc.api.BaseApiControllerTest;
import ru.yandex.market.sc.api.domain.control.AppControl;
import ru.yandex.market.sc.api.domain.user.CheckUserDto;
import ru.yandex.market.sc.api.utils.TestControllerCaller;
import ru.yandex.market.sc.core.domain.cell.model.ApiCellDto;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.order.AcceptService;
import ru.yandex.market.sc.core.domain.order.OrderCommandService;
import ru.yandex.market.sc.core.domain.order.PreShipService;
import ru.yandex.market.sc.core.domain.order.model.ApiOrderDto;
import ru.yandex.market.sc.core.domain.order.model.ApiOrderStatus;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.order.model.OrdersScRequest;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.place.PlaceCommandService;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.route.model.FinishRouteRequestDto;
import ru.yandex.market.sc.core.domain.route.repository.Route;
import ru.yandex.market.sc.core.domain.scan.model.AcceptOrderRequestDto;
import ru.yandex.market.sc.core.domain.scan.model.SortableSortRequestDto;
import ru.yandex.market.sc.core.domain.sortable.model.ApiSortableSortDto;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterPropertySource;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.model.UserRole;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.user.repository.UserRepository;
import ru.yandex.market.sc.core.domain.warehouse.model.WarehouseType;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.exception.ScErrorCode;
import ru.yandex.market.sc.core.resolver.dto.ScContext;
import ru.yandex.market.sc.internal.model.CourierDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE;
import static ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus.ARRIVED_DIRECT;
import static ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus.PREPARED_DIRECT;
import static ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus.SORTED_DIRECT;
import static ru.yandex.market.sc.core.test.SortableFlowSwitcherExtension.testNotMigrated;
import static ru.yandex.market.sc.core.test.TestFactory.order;
import static ru.yandex.market.sc.core.test.TestFactory.useNewSortableFlow;

/**
 * @author: dbryndin
 * @date: 10/14/21
 */
public class UtilizationFlowTest extends BaseApiControllerTest {

    private final static Long UID_SENIOR_STOCKMAN = 222L;

    @MockBean
    Clock clock;
    @Autowired
    SortingCenterPropertySource sortingCenterPropertySource;
    @Autowired
    OrderCommandService orderCommandService;
    @Autowired
    PlaceCommandService placeCommandService;
    @Autowired
    PreShipService preShipService;
    @Autowired
    ScOrderRepository scOrderRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    AcceptService acceptService;
    SortingCenter sortingCenter;
    Cell cell;
    User seniorStockman;
    Cell utillCell;
    Warehouse utilizatorWH;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        cell = testFactory.storedActiveCell(sortingCenter);
        utilizatorWH = testFactory.storedWarehouse("utilizator-1", WarehouseType.UTILIZATOR);
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.UTILIZATION_ENABLED, true);
        seniorStockman = testFactory.storedUser(sortingCenter, UID_SENIOR_STOCKMAN, UserRole.SENIOR_STOCKMAN);
        testFactory.setupMockClock(clock);
        utillCell = testFactory.storedCell(sortingCenter,
                "utils-cell-1",
                CellType.RETURN,
                CellSubType.UTILIZATION,
                utilizatorWH.getYandexId()
        );
    }

    @Test
    @DisplayName("success пользователь с ролью SENIOR_STOCKMAN получает кнопку 'Утилизация' на ТСД")
    public void successGetButtonUtilization() throws Exception {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.UTILIZATION_ENABLED, true);
        var controllerCaller = TestControllerCaller.createCaller(mockMvc, UID_SENIOR_STOCKMAN);
        var checkUserResp = controllerCaller.checkUser();
        assertTrue(readContentAsClass(checkUserResp, CheckUserDto.class).getControls().contains(AppControl.Utilization));
    }

    @Test
    @DisplayName("fail пользователь с ролью SENIOR_STOCKMAN не получает кнопку 'Утилизация' на ТСД если утилизация не" +
            " поддерживается")
    public void successGetButtonUtilization0() throws Exception {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.UTILIZATION_ENABLED, false);
        var controllerCaller = TestControllerCaller.createCaller(mockMvc, UID_SENIOR_STOCKMAN);
        var checkUserResp = controllerCaller.checkUser();
        assertFalse(readContentAsClass(checkUserResp, CheckUserDto.class).getControls().contains(AppControl.Utilization));
    }

    @Test
    @DisplayName("fail пользователь с ролью NEWBIE_STOCKMAN не получает кнопку 'Утилизация' на ТСД")
    public void failGetButtonUtilization() throws Exception {
        long uid = 333L;
        testFactory.storedUser(sortingCenter, uid, UserRole.NEWBIE_STOCKMAN);
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.UTILIZATION_ENABLED, false);
        var controllerCaller = TestControllerCaller.createCaller(mockMvc, uid);
        var checkUserResp = controllerCaller.checkUser();
        assertFalse(readContentAsClass(checkUserResp, CheckUserDto.class).getControls().contains(AppControl.Utilization));
    }

    @Test
    @Disabled // утилизация будет сделана иначе - текущую реализацию выключили
    @DisplayName("success сортировка одноместного возврата в ячейку утилизации и отгрузка")
    public void successSortOrderToCell() throws Exception {
        var place = testFactory.createOrder(order(sortingCenter).build())
                .cancel()
                .getPlace();

        var controllerCaller = TestControllerCaller.createCaller(mockMvc, UID_SENIOR_STOCKMAN);

        var cellForShip = acceptAndSort(controllerCaller, place);
        ship(controllerCaller, place, cellForShip.getId());
    }


    @Test
    @Disabled // утилизация будет сделана иначе - текущую реализацию выключили
    @DisplayName("success сортировка всех посылок возврата в ячейку утилизации и отгрузка")
    public void successSortMultiplaceOrder() throws Exception {
        var places = testFactory.createOrder(order(sortingCenter)
                        .places("p1", "p2", "p3")
                        .build())
                .cancel().getPlacesList();

        var controllerCaller = TestControllerCaller.createCaller(mockMvc, UID_SENIOR_STOCKMAN);
        ApiCellDto cellForShip = null;
        for (var place : places) {
            cellForShip = acceptAndSort(controllerCaller, place);
        }
        ship(controllerCaller, places.get(0), cellForShip.getId());
    }

    @Test
    @Disabled("Переписать DefaultCellDistributionPolicy на плейсы")
    @DisplayName("success сортировка и отгрузка посылки за посылкой")
    public void successSortAndShipMultiplaceOrderPlaceByPlace() throws Exception {
        var places = testFactory.createOrder(order(sortingCenter)
                        .places("p1", "p2", "p3")
                        .build())
                .cancel().getPlacesList();

        var controllerCaller = TestControllerCaller.createCaller(mockMvc, UID_SENIOR_STOCKMAN);
        for (var place : places) {
            var cellForShip = acceptAndSort(controllerCaller, place);
            ship(controllerCaller, place, cellForShip.getId());
        }
    }

    @Test
    @Disabled // утилизация будет сделана иначе - текущую реализацию выключили
    @DisplayName("fail сортировка места из заказа который уже отгрузили утилизатору через обычную приемку")
    public void failAcceptAlreadyUtilizationPlace() throws Exception {

        var places = testFactory.createOrder(order(sortingCenter)
                        .places("p1", "p2")
                        .build())
                .cancel().getPlacesList();

        var controllerCaller = TestControllerCaller.createCaller(mockMvc, UID_SENIOR_STOCKMAN);
        {
            var cellForShip = acceptAndSort(controllerCaller, places.get(0));
            ship(controllerCaller, places.get(0), cellForShip.getId());
        }
        {
            controllerCaller.acceptOrder(new AcceptOrderRequestDto(places.get(0)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(ScErrorCode.NEED_UTILIZATE_PLACE.name()));
        }
    }

    @Test
    @DisplayName("fail заказ в статусе ORDER_CREATED_F не сортируется в ячейку утилазации")
    public void failSortOrderUtilizationCell_ORDER_CREATED_FF() throws Exception {
        if (useNewSortableFlow()) {
            testNotMigrated();
            return;
        }

        var order = testFactory.createOrder(order(sortingCenter).build()).get();
        checkErrorStatusTransit(order, "p1", ScOrderFFStatus.ORDER_CREATED_FF);

    }

    @Test
    @DisplayName("fail заказ в статусе ORDER_PARTIALLY_ARRIVED_TO_SO_WAREHOUSE не сортируется в ячейку утилазации")
    public void failSortOrderUtilizationCell_ORDER_PARTIALLY_ARRIVED_TO_SO_WAREHOUSE() throws Exception {
        var order = testFactory.createForToday(
                order(sortingCenter).places("p1", "p2").dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlace("p1").sortPlace("p1").get();
        checkErrorSortableStatusTransit(order, "p1", SORTED_DIRECT);
    }

    @Test
    @DisplayName("fail заказ в статусе ORDER_ARRIVED_TO_SO_WAREHOUSE не сортируется в ячейку утилазации")
    public void failSortOrderUtilizationCell_ORDER_ARRIVED_TO_SO_WAREHOUSE() throws Exception {
        if (useNewSortableFlow()) {
            testNotMigrated();
            return;
        }

        var order = testFactory.createForToday(
                order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().get();
        checkErrorStatusTransit(order, "p1", ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE);
    }

    @Test
    @DisplayName("fail заказ в статусе ORDER_AWAITING_CLARIFICATION_FF не сортируется в ячейку утилазации")
    public void failSortOrderUtilizationCell_ORDER_AWAITING_CLARIFICATION_FF() throws Exception {
        if (useNewSortableFlow()) {
            testNotMigrated();
            return;
        }

        long newCourierUid = 321L;
        var order = testFactory.createOrderForToday(sortingCenter).accept().sort().ship().get();
        orderCommandService.updateCourier(
                order.getId(), new CourierDto(newCourierUid, "Другой курьер", null, null, null, null, null, false),
                testFactory.getOrCreateAnyUser(sortingCenter)
        );
        order = scOrderRepository.findByIdOrThrow(order.getId());
        assertThat(Objects.requireNonNull(order.getCourier()).getId()).isEqualTo(newCourierUid);

        var newCourier = userRepository.findByUid(newCourierUid).orElseThrow();
        acceptService.acceptPlace(testFactory.placeScRequest(order, testFactory.storedUser(sortingCenter, 123123L)));

        checkErrorStatusTransit(order, "p1", ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF);
    }

    @Test
    @DisplayName("fail заказ в статусе ORDER_READY_TO_BE_SEND_TO_SO_FF не сортируется в ячейку утилазации")
    public void failSortOrderUtilizationCell_ORDER_READY_TO_BE_SEND_TO_SO_FF() throws Exception {
        if (useNewSortableFlow()) {
            testNotMigrated();
            return;
        }

        var order = testFactory.create(order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build())
                .accept().sort().get();
        checkErrorStatusTransit(order, "p1", ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
    }

    @Test
    @DisplayName("fail заказ в статусе ORDER_PREPARED_TO_BE_SEND_TO_SO не сортируется в ячейку утилазации")
    public void failSortOrderUtilizationCell_ORDER_PREPARED_TO_BE_SEND_TO_SO() throws Exception {
        var order = testFactory.createOrderForToday(sortingCenter).accept().sort().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        Cell designatedCell = testFactory.determineRouteCell(route, order);

        Place place = testFactory.orderPlace(order);
        preShipService.prepareToShipPlace(
                testFactory.placeScRequest(order, testFactory.storedUser(sortingCenter, 123123L)), testFactory.getRouteIdForSortableFlow(route), place.getCell().getId());
        checkErrorSortableStatusTransit(order, testFactory.orderPlace(order).getMainPartnerCode(), PREPARED_DIRECT);
    }

    @Test
    @DisplayName("fail заказ в статусе ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE не сортируется в ячейку утилазации")
    public void failSortOrderUtilizationCell_ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE() throws Exception {
        var order = testFactory.createForToday(
                        order(sortingCenter)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .places("p1", "p2").externalId("o1").build()
                )
                .get();
        testFactory.acceptPlace(order, "p1");
        testFactory.sortPlace(order, "p1");
        testFactory.acceptPlace(order, "p2");
        order = testFactory.getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ORDER_ARRIVED_TO_SO_WAREHOUSE);
        orderCommandService.shipOrders(OrdersScRequest.ofExternalIds(
                Map.of("o1", List.of()), new ScContext(seniorStockman)
        ), Objects.requireNonNull(order.getCourier()).getId(), null);
        order = testFactory.getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE);
        checkErrorSortableStatusTransit(order, "p2", ARRIVED_DIRECT);
    }

    @Test
    @DisplayName("fail заказ в статусе ORDER_SHIPPED_TO_SO_FF не сортируется в ячейку утилазации")
    public void failSortOrderUtilizationCell_ORDER_SHIPPED_TO_SO_FF() throws Exception {
        if (useNewSortableFlow()) {
            testNotMigrated();
            return;
        }

        var order = testFactory.createForToday(order(sortingCenter).build())
                .accept().sort().ship().get();
        checkErrorStatusTransit(
                order,
                testFactory.orderPlace(order).getMainPartnerCode(),
                ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);

    }

    private void checkErrorStatusTransit(OrderLike order, String place, ScOrderFFStatus wrongStatus) throws Exception {
        var controllerCaller = TestControllerCaller.createCaller(mockMvc, UID_SENIOR_STOCKMAN);

        controllerCaller.sortableBetaSort(
                        new SortableSortRequestDto(order.getExternalId(), place,
                                String.valueOf(utillCell.getId())))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.error").value("ORDER_IN_WRONG_STATUS"))
                .andExpect(jsonPath("$.message")
                        .value("Can't sortOrderToUtilization from status " + wrongStatus.name()));
    }

    private void checkErrorSortableStatusTransit(OrderLike order, String place, SortableStatus wrongStatus) throws Exception {
        var controllerCaller = TestControllerCaller.createCaller(mockMvc, UID_SENIOR_STOCKMAN);

            controllerCaller.sortableBetaSort(
                            new SortableSortRequestDto(order.getExternalId(), place,
                                    String.valueOf(utillCell.getId())))
                    .andExpect(status().is4xxClientError())
                    .andExpect(jsonPath("$.error").value("ORDER_IN_WRONG_STATUS"))
                    .andExpect(jsonPath("$.message")
                            .value("Can't sortPlaceToUtilization from status " + wrongStatus.name()));

    }


    private ApiCellDto acceptAndSort(TestControllerCaller caller, Place place) throws Exception {
        // принимаем заказа на утилизацию
        // должен вернуть доступные ячейки утилизации
        // должен вернуть статус SORT_TO_UTILIZATION
        var acceptOrderRes0 = caller.acceptUtilization(new AcceptOrderRequestDto(place))
                .andExpect(status().isOk());
        var acceptOrderDtoResponse0 = readContentAsClass(acceptOrderRes0, ApiOrderDto.class);
        assertEquals(ApiOrderStatus.SORT_TO_UTILIZATION, acceptOrderDtoResponse0.getStatus());
        var utilizationCellO = acceptOrderDtoResponse0.getAvailableCells().stream().findFirst();
        assertTrue(utilizationCellO.isPresent());
        var utilizationCell = utilizationCellO.get();
        assertEquals(utilizationCell.getId(),
                acceptOrderDtoResponse0.getAvailableCells().stream().findFirst().get().getId());

        // сортируем заказ в ячейку утилизации
        // заказ должен перейти в статус ScOrderFFStatus#SORTING_CENTER_PREPARED_FOR_UTILIZE
        var sortResponse0 = caller.sortableBetaSort(
                        new SortableSortRequestDto(place.getExternalId(), place.getMainPartnerCode(),
                                String.valueOf(utilizationCell.getId())))
                .andExpect(status().is2xxSuccessful());

        assertEquals(readContentAsClass(sortResponse0, ApiSortableSortDto.class).getDestination().getId(),
                String.valueOf(utilizationCell.getId()));

        place = testFactory.updated(place);
        assertEquals(ScOrderFFStatus.SORTING_CENTER_PREPARED_FOR_UTILIZE, place.getFfStatus());
        assertNotNull(place.getCell());
        assertEquals(CellSubType.UTILIZATION, place.getCell().getSubtype());
        assertThat(place.isSortedToShipmentCell()).isTrue();
        return utilizationCell;
    }


    private void ship(TestControllerCaller controllerCaller, Place place, long cellIdForShip) throws Exception {
        var shipDtoReq = new FinishRouteRequestDto();
        shipDtoReq.setCellId(cellIdForShip);
        Optional<Route> outgoingWarehouseRoute = testFactory.findOutgoingWarehouseRoute(place);

        ResultActions cell = controllerCaller.getCellForRoute(cellIdForShip, outgoingWarehouseRoute.get().getId());
        cell.andExpect(status().is2xxSuccessful());

        controllerCaller.ship(outgoingWarehouseRoute.get().getId(), shipDtoReq)
                .andExpect(status().is2xxSuccessful());
        place = testFactory.updated(place);
        assertNull(place.getCell());
        assertEquals(ScOrderFFStatus.SORTING_CENTER_SHIPPED_FOR_UTILIZER,
                place.getFfStatus());
    }

}
