package ru.yandex.market.tpl.core.task.flow;

import java.time.Clock;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;
import ru.yandex.market.tpl.core.domain.logistic_request.LogisticRequestRepository;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.task.TestTaskFlowUtils;
import ru.yandex.market.tpl.core.task.defaults.EmptyActionProcessor;
import ru.yandex.market.tpl.core.task.persistence.LogisticRequestLinkRepository;
import ru.yandex.market.tpl.core.task.persistence.TaskActionEntity;
import ru.yandex.market.tpl.core.task.projection.FlowTask;
import ru.yandex.market.tpl.core.task.projection.TaskAction;
import ru.yandex.market.tpl.core.task.projection.TaskActionStatus;
import ru.yandex.market.tpl.core.task.projection.TaskActionType;
import ru.yandex.market.tpl.core.task.projection.TaskFlowType;
import ru.yandex.market.tpl.core.task.projection.TaskStatus;
import ru.yandex.market.tpl.core.task.service.LogisticRequestLinkService;
import ru.yandex.market.tpl.core.task.service.TaskRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * @author sekulebyakin
 */
@ExtendWith(MockitoExtension.class)
public class TaskFlowUnitTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private LogisticRequestLinkService logisticRequestLinkService;
    @Mock
    private LogisticRequestRepository logisticRequestRepository;
    @Mock
    private Clock clock;

    static boolean canResetHandler = true;

    private final EmptyActionProcessor processor = spy(new EmptyActionProcessor());

    private final ActionPrecondition preconditionToFail = spy(TestTaskFlowUtils.alwaysFalsePrecondition());
    private final ActionPrecondition preconditionToPass = spy(TestTaskFlowUtils.alwaysTruePrecondition());
    private final AfterActionHandler<EmptyPayload> simpleHandler = spy(TestTaskFlowUtils.emptyHandler());
    private final AfterActionHandler<EmptyPayload> handlerForReset = spy(new HandlerForResetTest());

    private List<TaskAction> actions;
    private FlowTask task;
    private User user;

    private TaskFlowService flowService;

    @BeforeEach
    void setupFlowConfigs() {
        clearInvocations(taskRepository, processor, preconditionToFail, simpleHandler);
        canResetHandler = true;
        var flowConfigurator = new TaskFlowConfigurator() {
            @Override
            public TaskFlowConfiguration configure() {
                return new TaskFlowConfiguration(TaskFlowType.TEST_FLOW)
                        .action(TaskActionType.EMPTY_ACTION)
                        .action(TaskActionType.EMPTY_ACTION).precondition(preconditionToFail).afterActionHandler(simpleHandler)
                        .action(TaskActionType.EMPTY_ACTION).precondition(preconditionToPass).afterActionHandler(handlerForReset)
                        .action(TaskActionType.EMPTY_ACTION).offlineAvailable(true).afterActionHandler(simpleHandler)
                        .action(TaskActionType.EMPTY_ACTION).afterActionHandler(simpleHandler)
                        .build();
            }
        };
        this.flowService = new TaskFlowService(List.of(processor), List.of(flowConfigurator), taskRepository,
                logisticRequestLinkService, logisticRequestRepository, clock);

        user = mock(User.class);
        task = mock(FlowTask.class);
        lenient().doReturn(12345L).when(task).getId();
        lenient().doReturn(TaskFlowType.TEST_FLOW).when(task).getFlowType();
        lenient().doReturn(TaskStatus.NOT_STARTED).when(task).getStatus();
        lenient().doReturn(101L).when(task).getCurrentActionId();
        lenient().doReturn(task).when(taskRepository).getTask(eq(12345L));

        actions = IntStream.range(1, 6)
                .mapToObj(this::createTestAction)
                .collect(Collectors.toList());

        lenient().doReturn(actions).when(taskRepository).getTaskActions(eq(12345L));
    }

    @Test
    void resetProgressFullSuccessTest() {
        var flow = flowService.initFlow(task, user);
        var payload = new EmptyPayload();

        // проходим весь флоу
        actions.forEach(action -> flow.runWithAction(action.getId(), payload));
        when(task.getStatus()).thenReturn(TaskStatus.FINISHED);

        var result = flow.resetProgress();
        assertThat(result.getResult()).isEqualTo(TaskActionRunResult.SUCCESS);
        assertThat(result.getProcessedActions()).hasSize(actions.size());

        // actions.get(0) без обработчиков
        verifyActionReset(null, actions.get(0), true);
        // actions.get(1) в статусе SKIPPED
        verifyActionReset(simpleHandler, actions.get(1), false);
        // для остальных должен быть вызван revert обработчиков
        verifyActionReset(handlerForReset, actions.get(2), true);
        verifyActionReset(simpleHandler, actions.get(3), true);
        verifyActionReset(simpleHandler, actions.get(4), true);
    }

    @Test
    void resetForbiddenForActionTest() {
        var flow = flowService.initFlow(task, user);
        var payload = new EmptyPayload();

        // проходим весь флоу
        actions.forEach(action -> flow.runWithAction(action.getId(), payload));
        when(task.getStatus()).thenReturn(TaskStatus.FINISHED);

        // запрещаем откатывать действие
        canResetHandler = false;

        assertThatThrownBy(flow::resetProgress)
                .isInstanceOf(TplInvalidActionException.class)
                .hasMessage(TestTaskFlowUtils.RESET_ERROR_MESSAGE);
    }

    @Test
    void runSimpleActionTest() {
        doReturn(101L).when(task).getCurrentActionId();
        var flow = flowService.initFlow(task, user);
        var payload = new EmptyPayload();

        var result = flow.runWithAction(101L, payload);

        // Успешно выполнено, обработано одно действие
        assertThat(result.getResult()).isEqualTo(TaskActionRunResult.SUCCESS);
        assertThat(actions.get(0).getStatus()).isEqualTo(TaskActionStatus.FINISHED);
        assertThat(result.getProcessedActions()).hasSize(1);
        assertThat(result.getProcessedActions().get(0).getId()).isEqualTo(101L);
        assertThat(result.getNextAvailableActions()).hasSize(2);
        assertThat(result.getNextAvailableActions().get(0).getId()).isEqualTo(103L);
        assertThat(result.getNextAvailableActions().get(1).getId()).isEqualTo(104L);
        verify(task).setCurrentActionId(eq(103L));
        verify(simpleHandler, never()).doAfterAction(any(), any());

        // Контекст сформирован корректно, не нужные компоненты не запускались
        var contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(processor, times(1)).processAction(contextCaptor.capture(), any());
        verifyNoInteractions(simpleHandler);

        var context = contextCaptor.getValue();
        assertThat(context.getTask()).isSameAs(task);
        assertThat(context.getUser()).isSameAs(user);
    }

    @Test
    void runActionWithPreconditionTest() {
        doReturn(102L).when(task).getCurrentActionId();
        var flow = flowService.initFlow(task, user);
        var payload = new EmptyPayload();

        var result = flow.runWithAction(102L, payload);

        // Не выполнились предусловия - действие пропущено
        assertThat(result.getResult()).isEqualTo(TaskActionRunResult.PRECONDITION_FAILED);
        assertThat(actions.get(1).getStatus()).isEqualTo(TaskActionStatus.SKIPPED);
        assertThat(result.getProcessedActions()).isEmpty();
        assertThat(result.getNextAvailableActions()).hasSize(2);
        assertThat(result.getNextAvailableActions().get(0).getId()).isEqualTo(103L);
        assertThat(result.getNextAvailableActions().get(1).getId()).isEqualTo(104L);
        verify(simpleHandler, never()).doAfterAction(any(), any());
        verify(task).setCurrentActionId(eq(103L));

        // Контекст сформирован корректно, пост обработчики не запускались
        var contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(preconditionToFail, times(1)).test(contextCaptor.capture());
        var context = contextCaptor.getValue();
        assertThat(context.getTask()).isSameAs(task);
        assertThat(context.getUser()).isSameAs(user);
        verifyNoInteractions(simpleHandler);
    }

    @Test
    void runActionWithPreconditionAndAfterHandlers() {
        doReturn(103L).when(task).getCurrentActionId();
        var flow = flowService.initFlow(task, user);
        var payload = new EmptyPayload();

        var result = flow.runWithAction(103L, payload);

        // Успешно выполнено, обработано одно действие
        assertThat(result.getResult()).isEqualTo(TaskActionRunResult.SUCCESS);
        assertThat(actions.get(2).getStatus()).isEqualTo(TaskActionStatus.FINISHED);
        assertThat(result.getProcessedActions()).hasSize(1);
        assertThat(result.getProcessedActions().get(0).getId()).isEqualTo(103L);
        assertThat(result.getNextAvailableActions()).hasSize(1);
        assertThat(result.getNextAvailableActions().get(0).getId()).isEqualTo(104L);
        verify(task).setCurrentActionId(eq(104L));

        // Контекст сформирован корректно, все компонены запустились
        var contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(processor, times(1)).processAction(contextCaptor.capture(), any());
        verify(preconditionToPass, times(1)).test(contextCaptor.capture());
        verify(handlerForReset, times(1)).doAfterAction(contextCaptor.capture(), any());

        var allContexts = contextCaptor.getAllValues();
        assertThat(allContexts).hasSize(3);

        var context = allContexts.get(0);
        assertThat(context.getTask()).isSameAs(task);
        assertThat(context.getUser()).isSameAs(user);
        assertThat(context).isSameAs(allContexts.get(1));
        assertThat(context).isSameAs(allContexts.get(2));
    }

    @Test
    void runFlowWrongActionOrderCanSkipTest() {
        doReturn(102L).when(task).getCurrentActionId();
        var flow = flowService.initFlow(task, user);
        var payload = new EmptyPayload();

        var result = flow.runWithAction(103L, payload);

        // Успешно выполнено, действие 102 пропущено по предусловию
        assertThat(result.getResult()).isEqualTo(TaskActionRunResult.SUCCESS);
        assertThat(actions.get(1).getStatus()).isEqualTo(TaskActionStatus.SKIPPED);
        assertThat(actions.get(2).getStatus()).isEqualTo(TaskActionStatus.FINISHED);
        assertThat(result.getProcessedActions()).hasSize(2);
        assertThat(result.getProcessedActions().get(0).getId()).isEqualTo(102L);
        assertThat(result.getProcessedActions().get(1).getId()).isEqualTo(103L);
        assertThat(result.getNextAvailableActions()).hasSize(1);
        assertThat(result.getNextAvailableActions().get(0).getId()).isEqualTo(104L);
        verify(task).setCurrentActionId(eq(104L));
    }

    @Test
    void runFlowWrongActionOrderCannotSkipTest() {
        doReturn(103L).when(task).getCurrentActionId();
        var flow = flowService.initFlow(task, user);
        var payload = new EmptyPayload();

        var result = flow.runWithAction(104L, payload);

        // Успешно выполнено, действие 102 пропущено по предусловию
        assertThat(result.getResult()).isEqualTo(TaskActionRunResult.WRONG_ACTION_CALL);
        assertThat(actions.get(2).getStatus()).isEqualTo(TaskActionStatus.CREATED);
        assertThat(actions.get(3).getStatus()).isEqualTo(TaskActionStatus.CREATED);
        assertThat(result.getProcessedActions()).isEmpty();
        assertThat(result.getNextAvailableActions()).hasSize(2);
        assertThat(result.getNextAvailableActions().get(0).getId()).isEqualTo(103L);
        assertThat(result.getNextAvailableActions().get(1).getId()).isEqualTo(104L);
        verify(task).setCurrentActionId(eq(103L));
    }

    @Test
    void finishFlowTest() {
        doReturn(105L).when(task).getCurrentActionId();
        var flow = flowService.initFlow(task, user);
        var payload = new EmptyPayload();

        var result = flow.runWithAction(105L, payload);

        // Успешно выполнено, действие 102 пропущено по предусловию
        assertThat(result.getResult()).isEqualTo(TaskActionRunResult.FLOW_FINISHED);
        assertThat(actions.get(4).getStatus()).isEqualTo(TaskActionStatus.FINISHED);
        assertThat(result.getNextAvailableActions()).isEmpty();
        verify(task).setCurrentActionId(eq(105L));
    }

    private TaskAction createTestAction(int ordinal) {
        var action = new TaskActionEntity();
        action.setId(100L + ordinal);
        action.setType(TaskActionType.EMPTY_ACTION.name());
        action.setStatus(TaskActionStatus.CREATED);
        action.setOrdinal(ordinal);
        return action;
    }

    private void verifyActionReset(AfterActionHandler<EmptyPayload> handler, TaskAction action, boolean shouldRevert) {
        assertThat(action.getStatus()).isEqualTo(TaskActionStatus.CREATED);
        if (shouldRevert) {
            if (handler != null) {
                verify(handler, times(1)).validateRevert(any(), eq(action));
                verify(handler, times(1)).revert(any(), eq(action));
            }
            verify(processor, times(1)).validateResetProgress(any(), eq(action));
            verify(processor, times(1)).resetProgress(any(), eq(action));
        } else {
            if (handler != null) {
                verify(handler, never()).validateRevert(any(), eq(action));
                verify(handler, never()).revert(any(), eq(action));
            }
            verify(processor, never()).validateResetProgress(any(), eq(action));
            verify(processor, never()).resetProgress(any(), eq(action));
        }
    }

    static class HandlerForResetTest implements AfterActionHandler<EmptyPayload> {

        @Override
        public void doAfterAction(Context context, EmptyPayload actionOutput) {

        }

        @Override
        public ResetValidationResult validateRevert(Context context, TaskAction action) {
            if (canResetHandler) {
                return ResetValidationResult.ok();
            }
            return ResetValidationResult.forbidden(TestTaskFlowUtils.RESET_ERROR_MESSAGE);
        }
    }
}
