package ru.yandex.market.tpl.core.domain.specialrequest;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.api.model.logisticrequest.LogisticRequestType;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.specialrequest.SpecialRequestType;
import ru.yandex.market.tpl.core.domain.base.fsm.StatusTransition;
import ru.yandex.market.tpl.core.domain.logistic_request.LogisticRequestCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.events.UserShiftStatusChangedEvent;
import ru.yandex.market.tpl.core.task.persistence.FlowTaskEntity;
import ru.yandex.market.tpl.core.task.persistence.LogisticRequestLinkRepository;
import ru.yandex.market.tpl.core.task.persistence.LogisticRequestLinkStatus;
import ru.yandex.market.tpl.core.task.projection.TaskStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.api.model.specialrequest.SpecialRequestType.LOCKER_INVENTORY;
import static ru.yandex.market.tpl.api.model.specialrequest.SpecialRequestType.PVZ_OTHER_DELIVERY;
import static ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequestStatus.CANCELLED;
import static ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequestStatus.CREATED;
import static ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequestStatus.FINISHED;
import static ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequestStatus.IN_PROGRESS;

/**
 * @author sekulebyakin
 */
@ExtendWith(MockitoExtension.class)
public class SpecialRequestShiftListenerUnitTest {

    @InjectMocks
    private SpecialRequestShiftListener listener;

    @Mock
    private LogisticRequestCommandService logisticRequestCommandService;
    @Mock
    private LogisticRequestLinkRepository logisticRequestLinkRepository;
    @Mock
    private SpecialRequestRepository specialRequestRepository;

    @Test
    void startSpecialRequestsTest() {

        var sr1 = mockSpecialRequest(11L, IN_PROGRESS, LOCKER_INVENTORY); // не подходит
        var sr2 = mockSpecialRequest(12L, CREATED, LOCKER_INVENTORY); // подходит
        var sr3 = mockSpecialRequest(13L, CANCELLED, LOCKER_INVENTORY); // не подходит
        var sr4 = mockSpecialRequest(14L, CREATED, LOCKER_INVENTORY); // подходит
        var sr5 = mockSpecialRequest(15L, FINISHED, LOCKER_INVENTORY); // не подходит
        var sr6 = mockSpecialRequest(16L, CREATED, PVZ_OTHER_DELIVERY); // не подходит

        var task1 = mockTask(1L, TaskStatus.NOT_STARTED); // подходит
        var task2 = mockTask(2L, TaskStatus.NOT_STARTED); // подходит
        var task3 = mockTask(3L, TaskStatus.FINISHED);  // не подходит

        var userShift = mock(UserShift.class);
        when(userShift.streamTasks(eq(FlowTaskEntity.class))).thenReturn(StreamEx.of(task1, task2, task3));

        // mock links
        // для тасок с id=1 и id=2, с id=3 не подходит - она в статусе FINISHED
        doReturn(List.of(11L, 12L, 13L, 14L, 15L, 16L)).when(logisticRequestLinkRepository).findLRIdsForTasksInStatus(
                eq(Set.of(1L, 2L)),
                eq(Set.of(LogisticRequestLinkStatus.ACTIVE.name())),
                eq(LogisticRequestType.SPECIAL_REQUEST.name())
        );
        doReturn(List.of(sr1, sr2, sr3, sr4, sr5, sr6))
                .when(specialRequestRepository).findAllById(eq(List.of(11L, 12L, 13L, 14L, 15L, 16L)));

        // run event
        listener.onUserShiftStartedActions(mockEvent(userShift, UserShiftStatus.ON_TASK));

        var commandCaptor = ArgumentCaptor.forClass(SpecialRequestCommand.Start.class);
        // команда Start должна быть вызвана для спецзаданий с id=11, 12 и 14, спецзадания с id=13 и 15 не подходят
        // по статусу, с id=16 не подходит по типу
        verify(logisticRequestCommandService, times(2)).execute(commandCaptor.capture());
        var startedIds = commandCaptor.getAllValues().stream()
                .map(SpecialRequestCommand.Start::getLogisticRequestId)
                .collect(Collectors.toSet());

        assertThat(startedIds).containsAll(List.of(12L, 14L));
    }

    @Test
    void doNotRunWithOtherStatusTransitionsTest() {
        var userShift = mock(UserShift.class);
        for (var userShiftStatus : UserShiftStatus.values()) {
            if (userShiftStatus == UserShiftStatus.ON_TASK) {
                // только перевод в ON_TASK должен обрабатываться
                continue;
            }
            listener.onUserShiftStartedActions(mockEvent(userShift, userShiftStatus));
            verifyNoInteractions(userShift);
            verifyNoInteractions(logisticRequestLinkRepository);
            verifyNoInteractions(specialRequestRepository);
            verifyNoInteractions(logisticRequestCommandService);
        }
    }

    private SpecialRequest mockSpecialRequest(long id, SpecialRequestStatus status, SpecialRequestType type) {
        var specialRequest = mock(SpecialRequest.class);
        lenient().doReturn(id).when(specialRequest).getId();
        lenient().doReturn(type).when(specialRequest).getSpecialRequestType();
        when(specialRequest.getStatus()).thenReturn(status);
        return specialRequest;
    }

    private FlowTaskEntity mockTask(long id, TaskStatus status) {
        var task = mock(FlowTaskEntity.class);
        lenient().doReturn(id).when(task).getId();
        when(task.getStatus()).thenReturn(status);
        return task;
    }

    @SuppressWarnings("unchecked")
    private UserShiftStatusChangedEvent mockEvent(UserShift userShift, UserShiftStatus statusTo) {
        var event = mock(UserShiftStatusChangedEvent.class);
        var transition = mock(StatusTransition.class);
        when(event.getTransition()).thenReturn(transition);
        when(transition.getTo()).thenReturn(statusTo);
        lenient().doReturn(userShift).when(event).getAggregate();
        return event;
    }

}
