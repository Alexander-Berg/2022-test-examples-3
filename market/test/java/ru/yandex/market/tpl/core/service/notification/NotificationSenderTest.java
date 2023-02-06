package ru.yandex.market.tpl.core.service.notification;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.base.fsm.StatusTransition;
import ru.yandex.market.tpl.core.domain.base.fsm.StatusTransitionType;
import ru.yandex.market.tpl.core.domain.usershift.OrderPickupTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.events.OrderPickupTaskStatusChangedEvent;
import ru.yandex.market.tpl.core.service.tracking.listener.OnOrderPickupFinishedSendSms;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.api.model.task.OrderPickupTaskStatus.BOX_LOADING;
import static ru.yandex.market.tpl.api.model.task.OrderPickupTaskStatus.FINISHED;
import static ru.yandex.market.tpl.api.model.task.OrderPickupTaskStatus.IN_PROGRESS;
import static ru.yandex.market.tpl.api.model.task.OrderPickupTaskStatus.NOT_STARTED;
import static ru.yandex.market.tpl.api.model.task.OrderPickupTaskStatus.PARTIALLY_FINISHED;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class NotificationSenderTest {

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private OrderPickupTask pickupTask;

    @MockBean
    private RoutePoint routePoint;

    @MockBean
    private UserShift userShift;

    @Autowired
    private OnOrderPickupFinishedSendSms eventHandler;

    private final long userShiftId = 35L;

    @BeforeEach
    void setUp() {
        when(pickupTask.getRoutePoint()).thenReturn(routePoint);
        when(routePoint.getUserShift()).thenReturn(userShift);
        when(userShift.getId()).thenReturn(userShiftId);
    }

    @Test
    void shouldNotNotifyOnOrderPickupFinished() {
        eventHandler.processEvent(new OrderPickupTaskStatusChangedEvent(pickupTask,
                new StatusTransition<>(BOX_LOADING, FINISHED, StatusTransitionType.NORMAL, null)));

        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void shouldNotNotifyOnOrderPickupPartiallyFinished() {
        eventHandler.processEvent(new OrderPickupTaskStatusChangedEvent(pickupTask,
                new StatusTransition<>(BOX_LOADING, PARTIALLY_FINISHED, StatusTransitionType.NORMAL, null)));

        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void shouldNotNotifyOnSubmitted() {
        eventHandler.processEvent(new OrderPickupTaskStatusChangedEvent(pickupTask,
                new StatusTransition<>(NOT_STARTED, IN_PROGRESS, StatusTransitionType.NORMAL, null)));

        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void shouldNotifyWhenFinishScanned() {
        eventHandler.processEvent(new OrderPickupTaskStatusChangedEvent(pickupTask,
                new StatusTransition<>(IN_PROGRESS, BOX_LOADING, StatusTransitionType.NORMAL, null)));

        verify(notificationService).sendDeliverySmsForUserShiftId(userShiftId);
    }

}
