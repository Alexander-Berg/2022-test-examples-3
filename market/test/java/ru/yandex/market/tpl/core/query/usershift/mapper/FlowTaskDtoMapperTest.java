package ru.yandex.market.tpl.core.query.usershift.mapper;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.api.model.order.locker.LockerDto;
import ru.yandex.market.tpl.api.model.task.TaskType;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.task.flow.EmptyPayload;
import ru.yandex.market.tpl.core.task.projection.FlowTask;
import ru.yandex.market.tpl.core.task.projection.TaskActionData;
import ru.yandex.market.tpl.core.task.projection.TaskActionType;
import ru.yandex.market.tpl.core.task.projection.TaskFlowType;
import ru.yandex.market.tpl.core.task.projection.TaskStatus;
import ru.yandex.market.tpl.core.task.service.TaskService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author sekulebyakin
 */
@ExtendWith(MockitoExtension.class)
public class FlowTaskDtoMapperTest {

    @InjectMocks
    private FlowTaskDtoMapper mapper;

    @Mock
    private TaskService taskService;

    @Mock
    private PickupPointRepository pickupPointRepository;

    @Mock
    private LockerMapper lockerMapper;

    @Test
    void mapFlowTaskTest() {
        var taskId = 5555L;
        var flowTask = mock(FlowTask.class);
        when(flowTask.getId()).thenReturn(taskId);
        when(flowTask.getFlowType()).thenReturn(TaskFlowType.TEST_FLOW);
        when(flowTask.getStatus()).thenReturn(TaskStatus.IN_PROGRESS);
        when(flowTask.getName()).thenReturn("flow task");
        when(flowTask.getPickupPointId()).thenReturn(222L);

        var pickupPoint = mock(PickupPoint.class);
        doReturn(pickupPoint).when(pickupPointRepository).findByIdOrThrow(eq(222L));
        var lockerDto = mock(LockerDto.class);
        doReturn(lockerDto).when(lockerMapper).map(pickupPoint);

        var action1 = mock(TaskActionData.class);
        when(action1.getId()).thenReturn(101L);
        when(action1.getType()).thenReturn(TaskActionType.EMPTY_ACTION);
        when(action1.getOrdinal()).thenReturn(1);
        when(action1.getData()).thenReturn(new EmptyPayload());

        var action2 = mock(TaskActionData.class);
        when(action2.getId()).thenReturn(102L);
        when(action2.getType()).thenReturn(TaskActionType.EMPTY_ACTION);
        when(action2.getOrdinal()).thenReturn(2);
        when(action2.getData()).thenReturn(new EmptyPayload());

        var user = mock(User.class);

        var actions = List.of(action1, action2);
        doReturn(actions).when(taskService).getActiveActions(eq(taskId), eq(user));

        var result = mapper.mapFlowTask(flowTask, user);

        assertThat(result.getId()).isEqualTo(flowTask.getId());
        assertThat(result.getFlowType()).isEqualTo(flowTask.getFlowType().name());
        assertThat(result.getType()).isEqualTo(TaskType.FLOW_TASK);
        assertThat(result.getStatus()).isEqualTo(flowTask.getStatus().name());
        assertThat(result.getTaskOrdinal()).isEqualTo(flowTask.getFlowType().getOrdinalPriority());
        assertThat(result.getFinishedAt()).isEqualTo(flowTask.getFinishedAt());
        assertThat(result.getLocker()).isSameAs(lockerDto);

        assertThat(result.getFlowActions()).hasSize(2);
        assertThat(result.getFlowActions().get(0).getId()).isEqualTo(action1.getId());
        assertThat(result.getFlowActions().get(0).getType()).isEqualTo(action1.getType().getSynonym());
        assertThat(result.getFlowActions().get(0).getOrdinal()).isEqualTo(action1.getOrdinal());
        assertThat(result.getFlowActions().get(0).getActionData()).isSameAs(action1.getData());
        assertThat(result.getFlowActions().get(1).getId()).isEqualTo(action2.getId());
        assertThat(result.getFlowActions().get(1).getOrdinal()).isEqualTo(action2.getOrdinal());
    }

}
