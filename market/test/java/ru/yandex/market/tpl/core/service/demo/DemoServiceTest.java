package ru.yandex.market.tpl.core.service.demo;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.shift.ShiftStatus;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.RoutePointRepository;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.tpl.core.domain.partner.SortingCenter.DEMO_SC_ID;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CoreTest
class DemoServiceTest {
    private final DemoService demoService;
    private final UserShiftRepository userShiftRepository;
    private final TestUserHelper userHelper;
    private final RoutePointRepository routePointRepository;
    private final UserPropertyService userPropertyService;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final OrderGenerateService orderGenerateService;
    private final Clock clock;
    private final OrderRepository orderRepository;
    private final PickupPointRepository pickupPointRepository;
    private final TestDataFactory testDataFactory;
    private final TestUserHelper testUserHelper;
    private final UserShiftCommandService userShiftCommandService;
    private User user;

    @BeforeEach
    void setUp() {
        user = userHelper.findOrCreateUser(824125L);
        userPropertyService.addPropertyToUser(user, UserProperties.DEMO_ENABLED, true);
    }

    @Test
    void successfullyCreatedDemoShiftTest() {
        demoService.createClientDemoDelivery(user);

        Optional<UserShift> currentShift = userShiftRepository.findCurrentShift(user);
        assertThat(currentShift.isPresent()).isTrue();
        assertThat(Objects.equals(DEMO_SC_ID, currentShift.get().getShift().getSortingCenter().getId())).isTrue();
    }

    @Test
    void hideOrdersInDemoMode() {
        var userShift = testUserHelper.createEmptyShift(user, LocalDate.now(clock));
        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L)
        );
        var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPoint)
                .build());
        var lockerDeliveryTask = testUserHelper.addLockerDeliveryTaskToShift(user, userShift, order);
        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.finishPickupAtStartOfTheDay(userShift);
        testUserHelper.arriveAtRoutePoint(userShift, lockerDeliveryTask.getRoutePoint().getId());

        var orders = orderRepository.findCurrentUserOrderIds(user.getId());
        assertThat(orders).hasSize(1);
        assertThat(orders).containsExactly(order.getId());

        demoService.createClientDemoDelivery(user);

        orders = orderRepository.findCurrentUserOrderIds(user.getId());
        // 0 т.к. переключились на демо смену и скрываем настоящие заказы.
        // При этом, поскольку демо смена не началась, то и курьер не взял демо заказы
        assertThat(orders).hasSize(0);

        Optional<UserShift> currentShift = userShiftRepository.findCurrentShift(user);
        assertThat(currentShift.isPresent()).isTrue();

        testUserHelper.openShift(user, currentShift.get().getId());
        testUserHelper.finishPickupAtStartOfTheDay(currentShift.get());

        //Тут курьер уже получил все демо заказы
        orders = orderRepository.findCurrentUserOrderIds(user.getId());
        assertThat(orders).isNotEmpty();
        assertThat(orders).doesNotContain(order.getId());

        assertThat(Objects.equals(DEMO_SC_ID, currentShift.get().getShift().getSortingCenter().getId())).isTrue();

        userShiftCommandService.switchActiveUserShift(user, userShift.getId());

        // Переключились обратно на "настоящую" смену, не закрывая (и не удаляя) демо смену.
        // Должны видеть только изначальный заказ
        orders = orderRepository.findCurrentUserOrderIds(user.getId());
        assertThat(orders).hasSize(1);
        assertThat(orders).containsExactly(order.getId());

    }

    @Test
    @Disabled("https://st.yandex-team.ru/MARKETTPL-7095")
    void successfullyCreatedPVZDemoShiftTest() {
        demoService.createPVZDemoDelivery(user);

        Optional<UserShift> currentShift = userShiftRepository.findCurrentShift(user);
        assertThat(currentShift.isPresent()).isTrue();
        assertThat(Objects.equals(DEMO_SC_ID, currentShift.get().getShift().getSortingCenter().getId())).isTrue();
    }

    @Test
    void demoIsDisabledForUserTest() {
        userPropertyService.addPropertyToUser(user, UserProperties.DEMO_ENABLED, false);

        var exception = assertThrows(TplInvalidActionException.class, () ->
                demoService.createClientDemoDelivery(user));
        assertEquals(String.format("Demo is disabled for user %d", user.getUid()), exception.getMessage());
    }

    @Test
    void userAlreadyInDemoShiftTest() {
        demoService.createClientDemoDelivery(user);

        var exception = assertThrows(TplInvalidActionException.class, () ->
                demoService.createClientDemoDelivery(user));

        assertEquals(String.format("Demo shift exists for user %d", user.getId()), exception.getMessage());
    }

    @Test
    void successfullyExitDemoShiftTest() {
        var userShiftDto = demoService.createClientDemoDelivery(user);
        demoService.exitDemo(user);
        dbQueueTestUtil.assertQueueHasSize(QueueType.DELETE_DEMO_SHIFT, 1);

        dbQueueTestUtil.executeSingleQueueItem(QueueType.DELETE_DEMO_SHIFT);
        assertThat(userShiftRepository.findCurrentShift(user).isEmpty()).isTrue();
        assertThat(userShiftRepository.findById(userShiftDto.getId())).isEmpty();
    }

    @Test
    void successfullyChangedBetweenDemoAndNormalShiftTest() {
        var shift = userHelper.findOrCreateShiftForScWithStatus(LocalDate.now(), 3L,
                ShiftStatus.OPEN);
        var userShift = userHelper.createEmptyShift(user, shift);

        demoService.createClientDemoDelivery(user);
        demoService.exitDemo(user);
        Optional<UserShift> currentShift = userShiftRepository.findCurrentShift(user);
        assertThat(currentShift.isPresent()).isTrue();

        assertThat(Objects.equals(userShift.getId(), currentShift.get().getId())).isTrue();
    }

    @Test
    void userCannotExitNonDemoShiftTest() {
        var shift = userHelper.findOrCreateShiftForScWithStatus(LocalDate.now(), 3L,
                ShiftStatus.OPEN);
        userHelper.createEmptyShift(user, shift);

        var exception = assertThrows(TplInvalidActionException.class, () ->
                demoService.exitDemo(user));

        assertEquals(String.format("Current user %d is not in demo shift", user.getId()), exception.getMessage());
    }

    @Test
    @Disabled("https://st.yandex-team.ru/MARKETTPL-7095")
    void ordersGenerationTest() {
        var shift = userHelper.findOrCreateShiftForScWithStatus(LocalDate.now(), 2L,
                ShiftStatus.OPEN);
        var userShift = userHelper.createEmptyShift(user, shift);

        demoService.generate(user.getUid(), userShift.getId(), user.getPhone(), RoutePointType.DELIVERY);
        demoService.generate(user.getUid(), userShift.getId(), user.getPhone(), RoutePointType.LOCKER_DELIVERY);

        assertThat(routePointRepository.findAll()).isNotEmpty();
    }
}
