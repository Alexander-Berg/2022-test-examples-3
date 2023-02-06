package ru.yandex.market.tpl.core.domain.order.postpone;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.CallToRecipientTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.service.order.PostponeOrderService;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static ru.yandex.market.tpl.core.service.order.PostponeOrderService.MAX_POSTPONEMENTS;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
public class PostponeOrderExceptionTest {
    private final TestUserHelper testUserHelper;
    private final SortingCenterService sortingCenterService;
    private final TestDataFactory testDataFactory;
    private final UserShiftRepository userShiftRepository;
    private final Clock clock;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftReassignManager userShiftReassignManager;
    private final PostponeOrderService postponeOrderService;

    private User user;
    private Shift shift;
    private UserShift userShift;
    private Order order1;
    private Order order3;
    private List<CallToRecipientTask> callTasks;

    @BeforeEach
    void init() {
        user = testUserHelper.findOrCreateUser(1L);
        shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock),
                sortingCenterService.findSortCenterForDs(239).getId());
        userShift = userShiftRepository
                .findByIdOrThrow(testDataFactory.createEmptyShift(shift.getId(), user));

        order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("231432")
                .deliveryDate(LocalDate.now(clock))
                .deliveryServiceId(239L)
                .deliveryInterval(LocalTimeInterval.valueOf("09:00-11:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(GeoPointGenerator.generateLonLat())
                        .build())
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                .build());

        Order order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("3245234")
                .deliveryDate(LocalDate.now(clock))
                .deliveryServiceId(239L)
                .deliveryInterval(LocalTimeInterval.valueOf("11:30-12:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(GeoPointGenerator.generateLonLat())
                        .build())
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                .build());

        order3 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("23445322")
                .deliveryDate(LocalDate.now(clock))
                .deliveryServiceId(239L)
                .deliveryInterval(LocalTimeInterval.valueOf("12:30-14:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(GeoPointGenerator.generateLonLat())
                        .build())
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                .build());

        Order order4 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("2344533222")
                .deliveryDate(LocalDate.now(clock))
                .deliveryServiceId(239L)
                .deliveryInterval(LocalTimeInterval.valueOf("14:00-16:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(GeoPointGenerator.generateLonLat())
                        .build())
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                .build());

        Order order5 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("2344435322")
                .deliveryDate(LocalDate.now(clock))
                .deliveryServiceId(239L)
                .deliveryInterval(LocalTimeInterval.valueOf("16:30-19:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(GeoPointGenerator.generateLonLat())
                        .build())
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                .build());

        Order order6 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("2344435321322")
                .deliveryDate(LocalDate.now(clock))
                .deliveryServiceId(239L)
                .deliveryInterval(LocalTimeInterval.valueOf("19:05-19:30"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(GeoPointGenerator.generateLonLat())
                        .build())
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                .build());

        Order order7 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("2344435321323")
                .deliveryDate(LocalDate.now(clock))
                .deliveryServiceId(239L)
                .deliveryInterval(LocalTimeInterval.valueOf("19:35-19:50"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(GeoPointGenerator.generateLonLat())
                        .build())
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                .build());


        Order order8 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("21344435321323")
                .deliveryDate(LocalDate.now(clock))
                .deliveryServiceId(239L)
                .deliveryInterval(LocalTimeInterval.valueOf("19:55-20:10"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(GeoPointGenerator.generateLonLat())
                        .build())
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                .build());

        userShiftReassignManager.assign(userShift, order1);
        userShiftReassignManager.assign(userShift, order2);
        userShiftReassignManager.assign(userShift, order3);
        userShiftReassignManager.assign(userShift, order4);
        userShiftReassignManager.assign(userShift, order5);
        userShiftReassignManager.assign(userShift, order6);
        userShiftReassignManager.assign(userShift, order7);
        userShiftReassignManager.assign(userShift, order8);
        testUserHelper.checkinAndFinishPickup(userShift);
        callTasks = userShift.streamCallTasks().collect(Collectors.toList());
    }

    @Test
    @DisplayName("Тест, чтобы не было возможности отложить таску на время, с которым не получится переключить роут поинт")
    void exceptionWhenPostponedOrderExpectedTimeBeforeNextRoutePoint() {
        thenThrownBy(() ->
                postponeOrderService.postponeMultiOrder(callTasks.get(0).getId().toString(), Duration.ofMinutes(1), user)
        ).isInstanceOf(TplInvalidActionException.class);
    }

    @Test
    @DisplayName("Тест, чтобы курьер не мог отложить более N тасок")
    void exceptionWhenPostponeOrderMoreAvailableCountAllowed() {
        for (int i = 0; i < MAX_POSTPONEMENTS; i++) {
            postponeOrderService.postponeMultiOrder(callTasks.get(i).getId().toString(), Duration.ofHours(15), user);
        }
        thenThrownBy(() ->
                postponeOrderService.postponeMultiOrder(
                        callTasks.get(MAX_POSTPONEMENTS).getId().toString(),
                        Duration.ofHours(15),
                        user)
        ).isInstanceOf(TplInvalidActionException.class);
    }

    @Test
    @DisplayName("Тест, чтобы курьер не мог отложить задание, когда следующее задание - возврат на сц")
    void exceptionWhenNextTaskIsReturnOrder() {
        testUserHelper.finishDelivery(userShift.getCurrentRoutePoint(), false);
        testUserHelper.finishDelivery(userShift.getCurrentRoutePoint(), false);
        testUserHelper.finishDelivery(userShift.getCurrentRoutePoint(), false);
        testUserHelper.finishDelivery(userShift.getCurrentRoutePoint(), false);
        testUserHelper.finishDelivery(userShift.getCurrentRoutePoint(), false);
        testUserHelper.finishDelivery(userShift.getCurrentRoutePoint(), false);
        testUserHelper.finishDelivery(userShift.getCurrentRoutePoint(), false);

        thenThrownBy(() ->
                postponeOrderService.postponeMultiOrder(
                        callTasks.get(4).getId().toString(),
                        Duration.ofHours(10),
                        user)
        ).isInstanceOf(TplInvalidActionException.class);


    }
}
