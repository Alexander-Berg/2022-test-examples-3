package ru.yandex.market.tpl.core.task.service;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequest;
import ru.yandex.market.tpl.core.task.projection.TaskFlowType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author sekulebyakin
 */
public class TaskFlowResolverUnitTest {

    private final TaskFlowResolver taskFlowResolver = new TaskFlowResolver();

    @Test
    void resolveTaskFlowsTest() {

        var lr1 = mock(SpecialRequest.class);
        var lr2 = mock(SpecialRequest.class);
        var lr3 = mock(SpecialRequest.class);
        var lr4 = mock(SpecialRequest.class);
        var lr5 = mock(SpecialRequest.class);
        when(lr1.resolveTaskFlow()).thenReturn(TaskFlowType.PVZ_OTHER_DELIVERY);
        when(lr2.resolveTaskFlow()).thenReturn(TaskFlowType.PVZ_OTHER_DELIVERY);
        when(lr3.resolveTaskFlow()).thenReturn(TaskFlowType.LOCKER_INVENTORY);
        when(lr4.resolveTaskFlow()).thenReturn(TaskFlowType.LOCKER_INVENTORY);
        when(lr5.resolveTaskFlow()).thenReturn(TaskFlowType.LOCKER_INVENTORY);

        var resolveResults = taskFlowResolver.resolveTaskFlows(List.of(lr1, lr2, lr3, lr4, lr5));

        assertThat(resolveResults).hasSize(2);
        assertThat(resolveResults.get(0).getTaskFlowType()).isEqualTo(TaskFlowType.LOCKER_INVENTORY);
        assertThat(resolveResults.get(0).getLogisticRequests()).hasSize(3);
        assertThat(resolveResults.get(1).getTaskFlowType()).isEqualTo(TaskFlowType.PVZ_OTHER_DELIVERY);
        assertThat(resolveResults.get(1).getLogisticRequests()).hasSize(2);

    }

}
