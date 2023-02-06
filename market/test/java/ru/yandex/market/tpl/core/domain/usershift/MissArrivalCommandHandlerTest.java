package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.Instant;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.api.model.task.OrderPickupTaskStatus;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MissArrivalCommandHandlerTest {

    @Mock
    private Clock clock;

    private UserShift userShift;

    @BeforeEach
    void init() {
        when(clock.instant()).thenReturn(Instant.now());
        var task = createPickupTask(OrderPickupTaskStatus.WAITING_ARRIVAL, 1L);
        userShift = createUserShift(task);
    }

    @Test
    void handle_wrongTaskId() {
        //when
        var handler = MissArrivalCommandHandler.builder()
                .command(new UserShiftCommand.MissArrival(
                        1L,
                        1L,
                        123L,
                        true,
                        Source.COURIER
                ))
                .clock(clock)
                .build();

        //then
        assertThrows(TplEntityNotFoundException.class, () -> handler.handle(userShift));
    }

    @Test
    void handle_taskNotInWaitingArrivalStatus() {
        //when
        MissArrivalCommandHandler.builder()
                .command(new UserShiftCommand.MissArrival(
                        1L,
                        1L,
                        1L,
                        true,
                        Source.COURIER
                ))
                .clock(clock)
                .build()
                .handle(userShift);

        //then
        Mockito.verify(userShift, Mockito.never()).missArrival(Mockito.anyLong(), Mockito.anyLong(), Mockito.any());
    }

    @Test
    void handle_taskInWaitingArrivalStatus() {
        //when
        MissArrivalCommandHandler.builder()
                .command(new UserShiftCommand.MissArrival(
                        1L,
                        1L,
                        1L,
                        false,
                        Source.COURIER
                ))
                .clock(clock)
                .build()
                .handle(userShift);

        //then
        Mockito.verify(userShift).missArrival(Mockito.anyLong(), Mockito.anyLong(), Mockito.any());
    }

    private UserShift createUserShift(OrderPickupTask task) {
        var userShift = Mockito.mock(UserShift.class);
        when(userShift.streamPickupTasks()).thenReturn(StreamEx.of(task));

        return userShift;
    }

    private OrderPickupTask createPickupTask(OrderPickupTaskStatus status, Long id) {
        var task = new OrderPickupTask();
        task.setStatus(status.toString());
        task.setInvitedArrivalTimeToLoading(Instant.now(clock));
        task.setId(id);

        return task;
    }

}
