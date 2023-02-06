package ru.yandex.market.tpl.internal.service;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.tracking.Tracking;
import ru.yandex.market.tpl.core.domain.usershift.tracking.TrackingRepository;
import ru.yandex.market.tpl.internal.TplIntAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.shift.UserShiftStatus.ON_TASK;
import static ru.yandex.market.tpl.internal.service.CrmManualService.TEST_COURIER_UID;

@RequiredArgsConstructor
class CrmManualServiceTest extends TplIntAbstractTest {

    private final UserShiftRepository userShiftRepository;
    private final OrderRepository orderRepository;
    private final TrackingRepository trackingRepository;

    private final OrderGenerateService orderGenerateService;
    private final TestUserHelper testUserHelper;

    private final CrmManualService underTest;

    @Test
    void when_creating_order_tracking_then_tracking_and_related_courier_shift_with_rout_points_and_tasks_are_generated() {
        //given
        Order givenOrder = orderRepository.save(orderGenerateService.createOrder("3"));
        User givenCourier = testUserHelper.findOrCreateUser(TEST_COURIER_UID);

        assertThat(userShiftRepository.findAll()).hasSize(0);
        assertThat(trackingRepository.findAll()).hasSize(0);

        //when
        underTest.createTrackingForOrder("3");

        //then
        UserShift createdCourierShift = userShiftRepository.findCurrentShift(givenCourier).orElse(null);
        assertThat(createdCourierShift).isNotNull();
        assertThat(createdCourierShift.getStatus()).isEqualTo(ON_TASK);
        assertThat(createdCourierShift.getCurrentRoutePoint()).isNotNull();
        assertThat(createdCourierShift.getCurrentRoutePoint().getUnfinishedTasks()).hasSizeGreaterThan(0);

        Tracking createdTracking = trackingRepository.findByUserShiftId(createdCourierShift.getId()).get(0);
        assertThat(createdTracking).isNotNull();
        assertThat(createdTracking.getUserShift()).isEqualTo(createdCourierShift);
        assertThat(createdTracking.getOrder()).isEqualTo(givenOrder);
    }
}
