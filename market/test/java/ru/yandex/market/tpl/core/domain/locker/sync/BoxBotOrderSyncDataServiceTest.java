package ru.yandex.market.tpl.core.domain.locker.sync;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.usershift.LockerSubtask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author sekulebyakin
 */
@ExtendWith(MockitoExtension.class)
public class BoxBotOrderSyncDataServiceTest {

    @InjectMocks
    private BoxBotOrderSyncDataService boxBotOrderSyncDataService;

    @Mock
    private BoxBotOrderSyncDataRepository boxBotOrderSyncDataRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private Clock clock;

    private final long orderId = 62374L;
    private final long subtaskId = 2348L;
    private final Instant now = Instant.now();

    private Order order;
    private LockerSubtask subtask;

    @BeforeEach
    void setupMocks() {
        order = mock(Order.class);
        subtask = mock(LockerSubtask.class);
        lenient().doReturn(orderId).when(order).getId();
        lenient().doReturn(subtaskId).when(subtask).getId();
        lenient().doReturn(OrderFlowStatus.READY_FOR_RETURN).when(order).getOrderFlowStatus();
        lenient().doReturn(orderId).when(subtask).getOrderId();
        lenient().doReturn(order).when(orderRepository).findByIdOrThrow(eq(orderId));
        lenient().doReturn(now).when(clock).instant();
        lenient().doReturn(order).when(orderRepository).findByIdOrThrow(eq(orderId));
    }

    @Test
    void startNewSyncSuccessTest() {
        boxBotOrderSyncDataService.startSync(subtask);

        var captor = ArgumentCaptor.forClass(BoxBotOrderSyncData.class);
        verify(boxBotOrderSyncDataRepository, times(1)).save(captor.capture());
        var syncData = captor.getValue();

        assertThat(syncData.getStatus()).isEqualTo(BoxBotOrderSyncStatus.IN_PROGRESS);
        assertThat(syncData.getStartedAt()).isEqualTo(now);
        assertThat(syncData.getOrderId()).isEqualTo(orderId);
        assertThat(syncData.getSubtaskId()).isEqualTo(subtaskId);
        assertThat(syncData.getOrderStatusBefore()).isEqualTo(order.getOrderFlowStatus());
        assertThat(syncData.getFinishedAt()).isNull();
        assertThat(syncData.getFailReason()).isNull();
        assertThat(syncData.getOrderStatusAfter()).isNull();
        assertThat(syncData.getBoxbotCheckpoint()).isNull();
        assertThat(syncData.getStatusSource()).isNull();
    }

    @Test
    void restartSyncSuccessTest() {
        var existingSyncData = BoxBotOrderSyncData.builder()
                .subtaskId(subtaskId)
                .orderId(orderId)
                .status(BoxBotOrderSyncStatus.FAILED)
                .startedAt(Instant.now().minusSeconds(60))
                .finishedAt(Instant.now())
                .failReason(BoxBotOrderSyncFailReason.WRONG_ORDER_STATUS)
                .boxbotCheckpoint(45)
                .statusSource(Source.COURIER)
                .orderStatusBefore(OrderFlowStatus.READY_FOR_RETURN)
                .orderStatusAfter(OrderFlowStatus.CANCELLED)
                .build();
        doReturn(Optional.of(existingSyncData)).when(boxBotOrderSyncDataRepository).findById(eq(subtaskId));

        boxBotOrderSyncDataService.startSync(subtask);

        var captor = ArgumentCaptor.forClass(BoxBotOrderSyncData.class);
        verify(boxBotOrderSyncDataRepository, times(1)).save(captor.capture());
        var syncData = captor.getValue();

        assertThat(syncData.getStatus()).isEqualTo(BoxBotOrderSyncStatus.IN_PROGRESS);
        assertThat(syncData.getStartedAt()).isEqualTo(now);
        assertThat(syncData.getOrderId()).isEqualTo(orderId);
        assertThat(syncData.getSubtaskId()).isEqualTo(subtaskId);
        assertThat(syncData.getOrderStatusBefore()).isEqualTo(order.getOrderFlowStatus());
        assertThat(syncData.getFinishedAt()).isNull();
        assertThat(syncData.getFailReason()).isNull();
        assertThat(syncData.getOrderStatusAfter()).isNull();
        assertThat(syncData.getBoxbotCheckpoint()).isNull();
        assertThat(syncData.getStatusSource()).isNull();
    }

    @Test
    void startFailAlreadyInProgressTest() {
        var existingSyncData = BoxBotOrderSyncData.builder()
                .subtaskId(subtaskId)
                .orderId(orderId)
                .status(BoxBotOrderSyncStatus.IN_PROGRESS)
                .build();
        doReturn(Optional.of(existingSyncData)).when(boxBotOrderSyncDataRepository).findById(eq(subtaskId));

        boxBotOrderSyncDataService.startSync(subtask);

        verify(boxBotOrderSyncDataRepository, times(1)).findById(eq(subtaskId));
        verifyNoMoreInteractions(boxBotOrderSyncDataRepository);
    }

    @Test
    void finishSyncSuccessTest() {
        var startTime = now.minusSeconds(60);
        var existingSyncData = BoxBotOrderSyncData.builder()
                .subtaskId(subtaskId)
                .orderId(orderId)
                .status(BoxBotOrderSyncStatus.IN_PROGRESS)
                .startedAt(startTime)
                .orderStatusBefore(OrderFlowStatus.LOST)
                .build();
        doReturn(existingSyncData).when(boxBotOrderSyncDataRepository).findByIdOrThrow(eq(subtaskId));

        boxBotOrderSyncDataService.finishSync(subtaskId, 45, Source.OPERATOR);

        var captor = ArgumentCaptor.forClass(BoxBotOrderSyncData.class);
        verify(boxBotOrderSyncDataRepository, times(1)).save(captor.capture());
        var syncData = captor.getValue();

        assertThat(syncData.getStatus()).isEqualTo(BoxBotOrderSyncStatus.FINISHED);
        assertThat(syncData.getStartedAt()).isEqualTo(startTime);
        assertThat(syncData.getOrderId()).isEqualTo(orderId);
        assertThat(syncData.getSubtaskId()).isEqualTo(subtaskId);
        assertThat(syncData.getOrderStatusBefore()).isEqualTo(OrderFlowStatus.LOST);
        assertThat(syncData.getOrderStatusAfter()).isEqualTo(order.getOrderFlowStatus());
        assertThat(syncData.getFinishedAt()).isEqualTo(now);
        assertThat(syncData.getFailReason()).isNull();
        assertThat(syncData.getBoxbotCheckpoint()).isEqualTo(45);
        assertThat(syncData.getStatusSource()).isEqualTo(Source.OPERATOR);
    }

    @Test
    void failSyncSuccessTest() {
        var startTime = now.minusSeconds(60);
        var existingSyncData = BoxBotOrderSyncData.builder()
                .subtaskId(subtaskId)
                .orderId(orderId)
                .status(BoxBotOrderSyncStatus.IN_PROGRESS)
                .startedAt(startTime)
                .orderStatusBefore(OrderFlowStatus.LOST)
                .build();
        doReturn(existingSyncData).when(boxBotOrderSyncDataRepository).findByIdOrThrow(eq(subtaskId));

        boxBotOrderSyncDataService.failSync(subtaskId, BoxBotOrderSyncFailReason.WRONG_ORDER_STATUS);

        var captor = ArgumentCaptor.forClass(BoxBotOrderSyncData.class);
        verify(boxBotOrderSyncDataRepository, times(1)).save(captor.capture());
        var syncData = captor.getValue();

        assertThat(syncData.getStatus()).isEqualTo(BoxBotOrderSyncStatus.FAILED);
        assertThat(syncData.getStartedAt()).isEqualTo(startTime);
        assertThat(syncData.getOrderId()).isEqualTo(orderId);
        assertThat(syncData.getSubtaskId()).isEqualTo(subtaskId);
        assertThat(syncData.getOrderStatusBefore()).isEqualTo(OrderFlowStatus.LOST);
        assertThat(syncData.getOrderStatusAfter()).isEqualTo(order.getOrderFlowStatus());
        assertThat(syncData.getFinishedAt()).isEqualTo(now);
        assertThat(syncData.getFailReason()).isEqualTo(BoxBotOrderSyncFailReason.WRONG_ORDER_STATUS);
        assertThat(syncData.getBoxbotCheckpoint()).isNull();
        assertThat(syncData.getStatusSource()).isNull();
    }

}
