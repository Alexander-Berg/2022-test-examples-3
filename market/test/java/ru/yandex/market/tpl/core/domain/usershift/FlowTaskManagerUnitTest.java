package ru.yandex.market.tpl.core.domain.usershift;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.commands.RunTaskActionCommand;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.query.usershift.UserShiftQueryService;
import ru.yandex.market.tpl.core.task.flow.EmptyPayload;
import ru.yandex.market.tpl.core.task.projection.TaskActionType;
import ru.yandex.market.tpl.core.task.service.TaskService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author sekulebyakin
 */
@ExtendWith(MockitoExtension.class)
public class FlowTaskManagerUnitTest {

    @InjectMocks
    private FlowTaskManager manager;

    @Mock
    private UserShiftCommandService userShiftCommandService;

    @Mock
    private UserShiftQueryService userShiftQueryService;

    @Mock
    private TaskService taskService;

    private final long userShiftId = 444L;
    private final long routePointId = 555L;
    private final long taskId = 123L;
    private final long actionId = 234L;
    private final User user = mock(User.class);

    @BeforeEach
    void setup() {
        reset(user);
        lenient().doReturn(userShiftId).when(userShiftQueryService).getCurrentShiftId(user);
        lenient().doReturn(routePointId).when(userShiftQueryService).getRoutePointId(user, taskId);
    }

    @Test
    void executeActionTest() {
        var payload = new EmptyPayload();
        manager.executeTaskAction(taskId, TaskActionType.EMPTY_ACTION, actionId,
                payload, user, Source.COURIER);

        var commandCaptor = ArgumentCaptor.forClass(RunTaskActionCommand.class);
        verify(userShiftCommandService, times(1)).runTaskAction(commandCaptor.capture());
        var command = commandCaptor.getValue();

        assertThat(command.getTaskId()).isEqualTo(taskId);
        assertThat(command.getTaskActionId()).isEqualTo(actionId);
        assertThat(command.getPayload()).isSameAs(payload);
        assertThat(command.getRoutePointId()).isEqualTo(routePointId);
        assertThat(command.getUserShiftId()).isEqualTo(userShiftId);
        assertThat(command.getUser()).isSameAs(user);
        assertThat(command.getActionType()).isEqualTo(TaskActionType.EMPTY_ACTION);
        assertThat(command.getSource()).isEqualTo(Source.COURIER);
    }

    @Test
    void reopenTaskTest() {

        manager.reopenTask(taskId, user, Source.COURIER);

        var commandCaptor = ArgumentCaptor.forClass(
                UserShiftCommand.ReopenFlowTaskCommand.class);
        verify(userShiftCommandService, times(1)).reopenFlowTask(eq(user), commandCaptor.capture());
        var command = commandCaptor.getValue();

        assertThat(command.getTaskId()).isEqualTo(taskId);
        assertThat(command.getRoutePointId()).isEqualTo(routePointId);
        assertThat(command.getUserShiftId()).isEqualTo(userShiftId);
        assertThat(command.getSource()).isEqualTo(Source.COURIER);

        command.getExecutionWrapper().apply(taskId, user);
        verify(taskService).reopenTask(eq(taskId), eq(user));
    }

}
