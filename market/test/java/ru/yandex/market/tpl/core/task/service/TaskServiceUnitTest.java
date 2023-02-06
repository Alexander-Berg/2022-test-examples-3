package ru.yandex.market.tpl.core.task.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.core.domain.base.DomainEvent;
import ru.yandex.market.tpl.core.domain.base.DomainEvents;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.task.flow.EmptyPayload;
import ru.yandex.market.tpl.core.task.flow.TaskActionRunResult;
import ru.yandex.market.tpl.core.task.flow.TaskFlow;
import ru.yandex.market.tpl.core.task.flow.TaskFlowRunResult;
import ru.yandex.market.tpl.core.task.flow.TaskFlowService;
import ru.yandex.market.tpl.core.task.persistence.LogisticRequestLinkStatusChangeReason;
import ru.yandex.market.tpl.core.task.projection.CreateActionRequest;
import ru.yandex.market.tpl.core.task.projection.FlowTask;
import ru.yandex.market.tpl.core.task.projection.TaskAction;
import ru.yandex.market.tpl.core.task.projection.TaskActionData;
import ru.yandex.market.tpl.core.task.projection.TaskActionType;
import ru.yandex.market.tpl.core.task.projection.TaskFlowType;
import ru.yandex.market.tpl.core.task.projection.TaskStatus;
import ru.yandex.market.tpl.core.test.ClockUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author sekulebyakin
 */
@ExtendWith(MockitoExtension.class)
public class TaskServiceUnitTest {

    private static final Long TEST_TASK_ID = 12345L;

    @Mock
    private TaskFlowService taskFlowService;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private LogisticRequestLinkService logisticRequestLinkService;

    @Mock
    private Clock clock;

    @InjectMocks
    private TaskService taskService;

    @BeforeEach
    void setup() {
        ClockUtil.initFixed(clock);
    }

    @Test
    void executeActionSuccessTest() {
        var now = Instant.now(clock);
        var user = mock(User.class);
        mockFlow(TaskActionRunResult.SUCCESS);

        // Task status NOT_STARTED, result SUCCESS
        var task = mockTask(TaskStatus.NOT_STARTED);
        var taskStartedEvent = mock(DomainEvent.class);
        when(task.start(eq(now))).thenReturn(DomainEvents.of(taskStartedEvent));

        var result = taskService.executeAction(task.getId(), 1L, new EmptyPayload(), user);
        assertThat(result.getResult()).isEqualTo(TaskActionRunResult.SUCCESS);
        assertThat(result.getNextActions()).isNotEmpty();
        assertThat(result.getDomainEvents()).hasSize(3);
        // Событие начала таски пишется первым
        assertThat(result.getDomainEvents().iterator().next()).isSameAs(taskStartedEvent);
        verify(task, times(1)).start(eq(now));
        verify(task, never()).finish(any());
    }

    @Test
    void executeActionFinishFlowTest() {
        var now = Instant.now(clock);
        var user = mock(User.class);
        mockFlow(TaskActionRunResult.FLOW_FINISHED);

        // Task status NOT_STARTED, result SUCCESS
        var task = mockTask(TaskStatus.IN_PROGRESS);
        var taskFinishedEvent = mock(DomainEvent.class);
        when(task.finish(eq(now))).thenReturn(DomainEvents.of(taskFinishedEvent));

        var result = taskService.executeAction(task.getId(), 1L, new EmptyPayload(), user);
        assertThat(result.getResult()).isEqualTo(TaskActionRunResult.FLOW_FINISHED);
        assertThat(result.getNextActions()).isEmpty();
        assertThat(result.getDomainEvents()).hasSize(3);
        // Событие завершения таски пишется последним
        assertThat(result.getDomainEvents().tailPositionListIterator().previous()).isSameAs(taskFinishedEvent);
        verify(task, never()).start(any());
        verify(task, times(1)).finish(eq(now));
    }

    @Test
    void executeActionNotSuccessTest() {
        var user = mock(User.class);
        mockFlow(TaskActionRunResult.PRECONDITION_FAILED);

        // Task status NOT_STARTED, result SUCCESS
        var task = mockTask(TaskStatus.NOT_STARTED);

        var result = taskService.executeAction(task.getId(), 1L, new EmptyPayload(), user);
        assertThat(result.getResult()).isEqualTo(TaskActionRunResult.PRECONDITION_FAILED);
        assertThat(result.getNextActions()).isNotEmpty();
        assertThat(result.getDomainEvents()).hasSize(2);
        verify(task, never()).start(any());
        verify(task, never()).finish(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void createActionsFullTest() {
        var task = mockTask(TaskStatus.NOT_STARTED);
        var actionsMap = Map.of(
                1, TaskActionType.EMPTY_ACTION,
                2, TaskActionType.EMPTY_ACTION,
                3, TaskActionType.EMPTY_ACTION
        );
        doReturn(actionsMap).when(taskFlowService).getActionTypeMap(eq(TaskFlowType.TEST_FLOW));

        var actions = List.of(mockAction(11L), mockAction(22L), mockAction(33L));
        doReturn(actions).when(taskRepository).createActions(anyLong(), anyList());

        taskService.createActionsForTask(task.getId());

        var actionDataCaptor = ArgumentCaptor.forClass(List.class);
        verify(taskRepository, times(1)).createActions(eq(task.getId()), actionDataCaptor.capture());
        verify(taskRepository, times(1)).saveTask(task);
        verify(task).setCurrentActionId(eq(actions.get(0).getId()));

        var createRequests = (List<CreateActionRequest>) actionDataCaptor.getValue();
        assertThat(createRequests).hasSize(3);
        for (int i = 0; i < 3; i++) {
            var createRequest = createRequests.get(i);
            var ordinal = i + 1;
            assertThat(createRequest.getOrdinal()).isEqualTo(ordinal);
            assertThat(createRequest.getType()).isEqualTo(TaskActionType.EMPTY_ACTION);
        }
    }

    @Test
    void createActionsStartedTaskTest() {
        var task = mockTask(TaskStatus.IN_PROGRESS);
        assertThatThrownBy(() -> taskService.createActionsForTask(task.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("started");
    }

    @Test
    void reopenTaskTest() {
        var task = mockTask(TaskStatus.FINISHED);
        var user = mock(User.class);
        var flow = mock(TaskFlow.class);
        var actions = List.of(mock(TaskAction.class), mock(TaskAction.class));
        var flowResult = new TaskFlowRunResult(TaskActionRunResult.SUCCESS, DomainEvents.of(), actions, List.of());

        doReturn(flow).when(taskFlowService).initFlow(task, user);
        doReturn(flowResult).when(flow).resetProgress();
        doReturn(DomainEvents.of()).when(task).reopen(any());

        taskService.reopenTask(task.getId(), user);

        verify(logisticRequestLinkService).restoreCancelledLinks(
                eq(task.getId()), eq(LogisticRequestLinkStatusChangeReason.TASK_CANCELLED));
        verify(taskFlowService).initFlow(task, user);
        verify(flow).resetProgress();
        verify(task).reopen(eq(Instant.now(clock)));
        verify(taskRepository).saveTask(task);
        verify(taskRepository).saveActions(eq(actions));
    }

    private void mockFlow(TaskActionRunResult runResult) {
        var flow = mock(TaskFlow.class);
        var flowFinished = runResult == TaskActionRunResult.FLOW_FINISHED;
        var result = new TaskFlowRunResult(
                runResult,
                DomainEvents.of(mock(DomainEvent.class), mock(DomainEvent.class)),
                List.of(mock(TaskAction.class), mock(TaskAction.class)),
                flowFinished ? List.of() : List.of(mock(TaskActionData.class), mock(TaskActionData.class))
        );
        doReturn(result).when(flow).runWithAction(anyLong(), any());
        doReturn(flow).when(taskFlowService).initFlow(any(), any());
    }

    private FlowTask mockTask(TaskStatus status) {
        var task = mock(FlowTask.class);
        var terminalState = status == TaskStatus.CANCELLED || status == TaskStatus.FINISHED;
        lenient().doReturn(TEST_TASK_ID).when(task).getId();
        lenient().doReturn(TaskFlowType.TEST_FLOW).when(task).getFlowType();
        lenient().doReturn(status).when(task).getStatus();
        lenient().doReturn(terminalState).when(task).hasTerminalState();
        doReturn(task).when(taskRepository).getTask(eq(TEST_TASK_ID));
        return task;
    }

    private TaskAction mockAction(long id) {
        var action = mock(TaskAction.class);
        lenient().doReturn(id).when(action).getId();
        return action;
    }

}
