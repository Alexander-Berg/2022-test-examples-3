package ru.yandex.market.tpl.core.domain.usershift.listener;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.locker.sync.BoxBotOrderSyncDataService;
import ru.yandex.market.tpl.core.domain.locker.sync.SyncBoxBotOrderProducer;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerSubtask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.events.locker.LockerDeliverySubtaskFailedEvent;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.SYNC_BOXBOT_STATUSES;

/**
 * @author sekulebyakin
 */
@ExtendWith(MockitoExtension.class)
public class OnLockerSubtaskFailedListenerTest {

    @InjectMocks
    private OnLockerSubtaskFailedListener listener;

    @Mock
    private ConfigurationProviderAdapter configurationProviderAdapter;

    @Mock
    private BoxBotOrderSyncDataService boxBotOrderSyncDataService;

    @Mock
    private SyncBoxBotOrderProducer syncBoxBotOrderProducer;

    @Mock
    private PickupPointRepository pickupPointRepository;

    @Spy
    private final TestableClock clock = new TestableClock();

    private final long orderId = 551L;
    private final long userShiftId = 777L;
    private final long pickupPointId = 234L;
    private final long subtaskId = 2345123L;
    private final PickupPoint pickupPoint = mock(PickupPoint.class);
    private final UserShift userShift = mock(UserShift.class);

    @BeforeEach
    void setup() {
        lenient().doReturn(pickupPoint).when(pickupPointRepository).findByIdOrThrow(eq(pickupPointId));
        lenient().doReturn(PartnerSubType.LOCKER).when(pickupPoint).getPartnerSubType();
        lenient().doReturn(true).when(configurationProviderAdapter).isBooleanEnabled(SYNC_BOXBOT_STATUSES);
        var clockTime = LocalDateTime.of(2000, 5, 10, 12, 30);
        var timeZone = ZoneOffset.ofHours(3);
        clock.setFixed(clockTime.toInstant(timeZone), timeZone);
        lenient().doReturn(userShiftId).when(userShift).getId();
    }

    @Test
    void processEventSuccessTest() {
        var event = mockEvent();
        listener.processFailedEvent(event);
        var instant = Instant.now(clock);
        verify(syncBoxBotOrderProducer, times(1))
                .produce(eq(orderId), eq(userShiftId), eq(0L), eq(0L), eq(subtaskId), eq(instant));
        verify(boxBotOrderSyncDataService, times(1)).startSync(eq(event.getSubtask()));
    }

    @Test
    void processFailNoFlagTest() {
        doReturn(false).when(configurationProviderAdapter).isBooleanEnabled(SYNC_BOXBOT_STATUSES);
        listener.processFailedEvent(mockEvent());
        verifyNoInteractions(syncBoxBotOrderProducer);
    }

    @Test
    void processFailNoCourierReasonTest() {
        var event = mockEvent();
        event.getFailReason().setSource(Source.OPERATOR);
        listener.processFailedEvent(event);
        verifyNoInteractions(syncBoxBotOrderProducer);
    }

    @Test
    void processNotLockerType() {
        doReturn(PartnerSubType.PVZ).when(pickupPoint).getPartnerSubType();
        listener.processFailedEvent(mockEvent());
        verifyNoMoreInteractions(syncBoxBotOrderProducer);
    }

    @Test
    void processFailReasonTypeIsBlocked() {
        var event = mockEvent();
        event.getFailReason().setType(OrderDeliveryTaskFailReasonType.LOCKER_NOT_WORKING);
        doReturn(Set.of(OrderDeliveryTaskFailReasonType.LOCKER_NOT_WORKING.name()))
                .when(configurationProviderAdapter)
                .getValueAsStrings(ConfigurationProperties.SYNC_BOXBOT_REASON_BLACK_LIST);
        listener.processFailedEvent(event);
        verifyNoInteractions(syncBoxBotOrderProducer);
    }

    private LockerDeliverySubtaskFailedEvent mockEvent() {
        var reason = new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.FINISHED_BY_SUPPORT,
                "", null, Source.COURIER);
        var subtask = mock(LockerSubtask.class);
        var task = mock(LockerDeliveryTask.class);
        var routePoint = mock(RoutePoint.class);

        lenient().doReturn(subtaskId).when(subtask).getId();
        lenient().doReturn(task).when(subtask).getTask();
        lenient().doReturn(pickupPointId).when(task).getPickupPointId();
        lenient().doReturn(orderId).when(subtask).getOrderId();
        lenient().doReturn(routePoint).when(task).getRoutePoint();
        lenient().doReturn(userShift).when(routePoint).getUserShift();

        return new LockerDeliverySubtaskFailedEvent(userShift, reason, subtask);
    }
}
