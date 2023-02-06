package ru.yandex.market.deepmind.common.services.background;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.mocks.BackgroundActionRepositoryMock;
import ru.yandex.market.mboc.common.TransactionTemplateMock;
import ru.yandex.market.mboc.common.dict.backgroundaction.BackgroundAction;
import ru.yandex.market.mboc.common.dict.backgroundaction.BackgroundActionServiceImpl;
import ru.yandex.market.mboc.common.dict.backgroundaction.BackgroundActionStatus;
import ru.yandex.market.mboc.common.dict.backgroundaction.BackgroundActionStatus.ActionStatus;
import ru.yandex.market.mboc.common.exceptions.BadUserRequestException;
import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class BackgroundServiceAdapterTest {

    private BackgroundActionRepositoryMock repository;
    private BackgroundServiceAdapter actionService;
    private BackgroundActionServiceImpl backgroundActionService1;
    private BackgroundActionServiceImpl backgroundActionService2;

    @Before
    public void setup() {
        repository = new BackgroundActionRepositoryMock();
        backgroundActionService1 = new BackgroundActionServiceImpl(repository, new TransactionTemplateMock(), 1);
        backgroundActionService1.init();
        actionService = new BackgroundServiceAdapter(backgroundActionService1);
    }

    @After
    public void tearDown() throws InterruptedException {
        backgroundActionService1.stop();
        if (backgroundActionService2 != null) {
            backgroundActionService2.stop();
        }
        RequestContextHolder.clearContext();
    }

    @Test
    public void testActionHandleOperations() throws InterruptedException {
        int[] actionIdd = new int[1];

        int actionId = actionService.startAction(BackgroundService.Context.builder()
                .description("Some action")
                .login("alice")
                .initialState(BackgroundActionStatus.idleWithParams("idle", "param"))
                .build(),
            actionHandle -> {
                assertThat(repository.findAll()).hasSize(1);

                int id = actionIdd[0] = repository.findAll().get(0).getId();
                assertThat(actionHandle.getActionId()).isEqualTo(id);

                BackgroundAction action;
                action = repository.findById(id);
                assertThat(action.getUserLogin()).isEqualTo("alice");
                assertThat(action.getDescription()).isEqualTo("Some action");
                assertThat(action.getState()).isNull();
                assertThat(action.getResult())
                    .isEqualTo(BackgroundActionStatus.idleWithParams("idle", "param"));
                assertThat(action.getHost()).isNotEmpty();
                assertThat(action.getThreadName()).isNotEmpty();

                actionHandle.inProgressWithParams("Some string", "param2");
                action = repository.findById(id);
                assertThat(action.getState()).isEqualTo(ActionStatus.IN_PROGRESS.toString());
                assertThat(action.getResult()).isEqualTo(
                    BackgroundActionStatus.inProgressWithParams("Some string", "param2"));
                assertThat(action.isActionFinished()).isFalse();

                actionHandle.success("some other state");
                action = repository.findById(id);
                assertThat(action.getState()).isEqualTo(ActionStatus.FINISHED.toString());
                assertThat(action.isActionFinished()).isTrue();
                assertThat(action.getResult()).isEqualTo(BackgroundActionStatus.success("some other state"));

                actionHandle.successWithParams("done", "param4");
            });

        backgroundActionService1.stop();

        assertThat(actionId).isEqualTo(actionIdd[0]);
        assertThat(repository.findById(actionId).getState()).isEqualTo(ActionStatus.FINISHED.toString());
    }

    @Test
    public void testContextCreated() throws InterruptedException {
        RequestContext context = RequestContextHolder.createNewContext();
        AtomicBoolean checked = new AtomicBoolean(false);
        actionService.startAction(
            handle -> {
                assertThat(RequestContextHolder.getContext().getRequestId())
                    .isNotEqualTo(context.getRequestId()) // Create new context DEEPMIND-165
                    .isNotEqualTo(RequestContext.EMPTY.getRequestId());
                checked.set(true);
            });
        backgroundActionService1.stop();
        assertThat(checked).isTrue();
    }

    @Test
    public void testThreadNameChanged() throws InterruptedException {
        AtomicBoolean checked = new AtomicBoolean(false);
        String threadName = Thread.currentThread().getName();
        actionService.startAction(
            handle -> {
                assertThat(Thread.currentThread().getName()).contains(threadName);
                checked.set(true);
            });
        backgroundActionService1.stop();
        assertThat(checked).isTrue();
    }

    @Test
    public void testCancel() throws InterruptedException {
        int actionId = actionService.startAction(
            actionHandle -> {
                // эмулируем долгую работу
                for (int i = 0; i < 50; i++) {
                    try {
                        Thread.sleep(200);
                        actionHandle.inProgress("in_progress");
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                actionHandle.successWithParams("done", Map.of("result", 42));
            });

        actionService.cancelAction(actionId);
        assertThat(repository.findById(actionId).isActionCancelled()).isTrue();
        assertThat(repository.findById(actionId).isActionFinished()).isTrue();
        assertThat(repository.findById(actionId).isCancelRequested()).isTrue();
        assertThat(repository.findById(actionId).getState()).doesNotContain("done");

        backgroundActionService1.stop();
    }

    @Test
    public void testCancelAsync() throws InterruptedException {
        int actionId = actionService.startAction(actionHandle -> {
            // эмулируем долгую работу
            for (int i = 0; i < 25; i++) {
                try {
                    Thread.sleep(200);
                    actionHandle.inProgress("in_progress");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            actionHandle.success("done");
        });

        actionService.cancelActionAsync(actionId);

        await().atMost(6, TimeUnit.SECONDS)
            .until(() -> repository.findById(actionId).isActionFinished());

        assertThat(repository.findById(actionId).isActionCancelled()).isTrue();
        assertThat(repository.findById(actionId).isActionFinished()).isTrue();
        assertThat(repository.findById(actionId).isCancelRequested()).isTrue();
        assertThat(repository.findById(actionId).getState()).doesNotContain("done");

        backgroundActionService1.stop();
    }

    @Test
    public void testCancelFromOtherService() throws InterruptedException {
        backgroundActionService2 = new BackgroundActionServiceImpl(repository, new TransactionTemplateMock(), 1);
        backgroundActionService2.init();
        var actionService2 = new BackgroundServiceAdapter(backgroundActionService2);

        int actionId = actionService.startAction(actionHandle -> {
            // эмулируем долгую работу
            for (int i = 0; i < 25; i++) {
                try {
                    Thread.sleep(200);
                    actionHandle.inProgress("in_progress");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            actionHandle.success("done");
        });

        // останавливаем из другого сервиса, должен остановиться везде
        // это синхронный метод, поэтому можно смело ждать изменений
        actionService2.cancelAction(actionId);

        assertThat(repository.findById(actionId).isActionCancelled()).isTrue();
        assertThat(repository.findById(actionId).isActionFinished()).isTrue();
        assertThat(repository.findById(actionId).isCancelRequested()).isTrue();
        assertThat(repository.findById(actionId).getState()).doesNotContain("done");
    }

    @Test
    public void testCancelAsyncFromOtherService() throws InterruptedException {
        backgroundActionService2 = new BackgroundActionServiceImpl(repository, new TransactionTemplateMock(), 1);
        backgroundActionService2.init();
        var actionService2 = new BackgroundServiceAdapter(backgroundActionService2);

        int actionId = actionService.startAction(actionHandle -> {
            // эмулируем долгую работу
            for (int i = 0; i < 25; i++) {
                try {
                    Thread.sleep(200);
                    actionHandle.inProgress("in_progress");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            actionHandle.success("done");
        });

        // останавливаем из другого сервиса, должен остановиться везде
        // это синхронный метод, поэтому можно смело ждать изменений
        actionService2.cancelActionAsync(actionId);

        await().atMost(6, TimeUnit.SECONDS)
            .until(() -> repository.findById(actionId).isActionFinished());

        assertThat(repository.findById(actionId).isActionCancelled()).isTrue();
        assertThat(repository.findById(actionId).isActionFinished()).isTrue();
        assertThat(repository.findById(actionId).isCancelRequested()).isTrue();
        assertThat(repository.findById(actionId).getState()).doesNotContain("done");
    }

    @Test(expected = BadUserRequestException.class)
    public void testCancelFinishedTaskThrowsException() throws InterruptedException {
        backgroundActionService2 = new BackgroundActionServiceImpl(repository, new TransactionTemplateMock(), 1);
        backgroundActionService2.init();
        var actionService2 = new BackgroundServiceAdapter(backgroundActionService1);

        int actionId = actionService.startAction(handler -> handler.success("done"));

        await().atMost(6, TimeUnit.SECONDS)
            .until(() -> repository.findById(actionId).isActionFinished());

        actionService2.cancelAction(actionId);
    }

}
