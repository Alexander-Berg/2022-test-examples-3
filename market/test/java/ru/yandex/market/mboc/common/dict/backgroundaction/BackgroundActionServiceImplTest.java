package ru.yandex.market.mboc.common.dict.backgroundaction;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mboc.common.TransactionTemplateMock;
import ru.yandex.market.mboc.common.exceptions.BadUserRequestException;
import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * @author yuramalinov
 * @created 14.05.18
 */
@SuppressWarnings("checkstyle:magicnumber")
public class BackgroundActionServiceImplTest {
    private BackgroundActionRepositoryMock repository;
    private BackgroundActionServiceImpl actionService;
    private BackgroundActionServiceImpl actionService2;

    @Before
    public void setup() {
        repository = new BackgroundActionRepositoryMock();
        actionService = new BackgroundActionServiceImpl(repository, new TransactionTemplateMock(), 1);
        actionService.init();
    }

    @After
    public void tearDown() throws InterruptedException {
        actionService.stop();
        if (actionService2 != null) {
            actionService2.stop();
        }
        RequestContextHolder.clearContext();
    }

    @Test
    public void testActionHandleOperations() throws InterruptedException {
        int[] actionIdd = new int[1];

        int actionId = actionService.startAction(BackgroundActionService.Context.builder()
                .description("Some action")
                .login("alice")
                .initialState(Collections.singletonMap("test", 1))
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
                assertThat(action.getResult()).isEqualTo(Collections.singletonMap("test", 1));
                assertThat(action.getHost()).isNotEmpty();
                assertThat(action.getThreadName()).isNotEmpty();

                actionHandle.updateState("Some string", Collections.singletonMap("second", 2));
                action = repository.findById(id);
                assertThat(action.getState()).isEqualTo("Some string");
                assertThat(action.getResult()).isEqualTo(Collections.singletonMap("second", 2));
                assertThat(action.isActionFinished()).isFalse();

                actionHandle.finish("some other state", Collections.singletonMap("result", 42));
                action = repository.findById(id);
                assertThat(action.getState()).isEqualTo("some other state");
                assertThat(action.isActionFinished()).isTrue();
                assertThat(action.getResult()).isEqualTo(Collections.singletonMap("result", 42));

                // Не особо страшно, зато даст хорошую ошибку в случае чего
                actionHandle.finish("done", Collections.singletonMap("result", 42));
            });

        actionService.stop();

        assertThat(actionId).isEqualTo(actionIdd[0]);
        assertThat(repository.findById(actionId).getState()).isEqualTo("done");
    }

    @Test
    public void testContextCreated() throws InterruptedException {
        RequestContext context = RequestContextHolder.createNewContext();
        AtomicBoolean checked = new AtomicBoolean(false);
        actionService.startAction(BackgroundActionService.Context.builder().build(),
            handle -> {
                assertThat(RequestContextHolder.getContext().getRequestId())
                    .isNotEqualTo(context.getRequestId()) // Create new context DEEPMIND-165
                    .isNotEqualTo(RequestContext.EMPTY.getRequestId());
                checked.set(true);
            });
        actionService.stop();
        assertThat(checked).isTrue();
    }

    @Test
    public void testThreadNameChanged() throws InterruptedException {
        AtomicBoolean checked = new AtomicBoolean(false);
        String threadName = Thread.currentThread().getName();
        actionService.startAction(BackgroundActionService.Context.builder().build(),
            handle -> {
                assertThat(Thread.currentThread().getName()).contains(threadName);
                checked.set(true);
            });
        actionService.stop();
        assertThat(checked).isTrue();
    }

    @Test
    public void testCancel() throws InterruptedException {
        int actionId = actionService.startAction(BackgroundActionService.Context.builder().build(),
            actionHandle -> {
                // эмулируем долгую работу
                for (int i = 0; i < 50; i++) {
                    try {
                        Thread.sleep(200);
                        actionHandle.updateState("in_progress");
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                actionHandle.finish("done", Collections.singletonMap("result", 42));
            });

        actionService.cancel(actionId);
        assertThat(repository.findById(actionId).isActionCancelled()).isTrue();
        assertThat(repository.findById(actionId).isActionFinished()).isTrue();
        assertThat(repository.findById(actionId).isCancelRequested()).isTrue();
        assertThat(repository.findById(actionId).getState()).doesNotContain("done");

        actionService.stop();
    }

    @Test
    public void testCancelAsync() throws InterruptedException {
        int actionId = actionService.startAction(BackgroundActionService.Context.builder().build(),
            actionHandle -> {
                // эмулируем долгую работу
                for (int i = 0; i < 25; i++) {
                    try {
                        Thread.sleep(200);
                        actionHandle.updateState("in_progress");
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                actionHandle.finish("done", Collections.singletonMap("result", 42));
            });

        actionService.cancelAsync(actionId);

        await().atMost(6, TimeUnit.SECONDS)
            .until(() -> repository.findById(actionId).isActionFinished());

        assertThat(repository.findById(actionId).isActionCancelled()).isTrue();
        assertThat(repository.findById(actionId).isActionFinished()).isTrue();
        assertThat(repository.findById(actionId).isCancelRequested()).isTrue();
        assertThat(repository.findById(actionId).getState()).doesNotContain("done");

        actionService.stop();
    }

    @Test
    public void testCancelFromOtherService() throws InterruptedException {
        actionService2 = new BackgroundActionServiceImpl(repository, new TransactionTemplateMock(), 1);
        actionService2.init();

        int actionId = actionService.startAction(BackgroundActionService.Context.builder().build(),
            actionHandle -> {
                // эмулируем долгую работу
                for (int i = 0; i < 25; i++) {
                    try {
                        Thread.sleep(200);
                        actionHandle.updateState("in_progress");
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                actionHandle.finish("done", Collections.singletonMap("result", 42));
            });

        // останавливаем из другого сервиса, должен остановиться везде
        // это синхронный метод, поэтому можно смело ждать изменений
        actionService2.cancel(actionId);

        assertThat(repository.findById(actionId).isActionCancelled()).isTrue();
        assertThat(repository.findById(actionId).isActionFinished()).isTrue();
        assertThat(repository.findById(actionId).isCancelRequested()).isTrue();
        assertThat(repository.findById(actionId).getState()).doesNotContain("done");
    }

    @Test
    public void testCancelAsyncFromOtherService() throws InterruptedException {
        actionService2 = new BackgroundActionServiceImpl(repository, new TransactionTemplateMock(), 1);
        actionService2.init();

        int actionId = actionService.startAction(BackgroundActionService.Context.builder().build(),
            actionHandle -> {
                // эмулируем долгую работу
                for (int i = 0; i < 25; i++) {
                    try {
                        Thread.sleep(200);
                        actionHandle.updateState("in_progress");
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                actionHandle.finish("done", Collections.singletonMap("result", 42));
            });

        // останавливаем из другого сервиса, должен остановиться везде
        // это синхронный метод, поэтому можно смело ждать изменений
        actionService2.cancelAsync(actionId);

        await().atMost(6, TimeUnit.SECONDS)
            .until(() -> repository.findById(actionId).isActionFinished());

        assertThat(repository.findById(actionId).isActionCancelled()).isTrue();
        assertThat(repository.findById(actionId).isActionFinished()).isTrue();
        assertThat(repository.findById(actionId).isCancelRequested()).isTrue();
        assertThat(repository.findById(actionId).getState()).doesNotContain("done");
    }

    @Test(expected = BadUserRequestException.class)
    public void testCancelFinishedTaskThrowsException() throws InterruptedException {
        actionService2 = new BackgroundActionServiceImpl(repository, new TransactionTemplateMock(), 1);
        actionService2.init();

        int actionId = actionService.startAction(BackgroundActionService.Context.builder().build(),
            actionHandle -> actionHandle.finish("done", Collections.singletonMap("result", 42)));

        await().atMost(6, TimeUnit.SECONDS)
            .until(() -> repository.findById(actionId).isActionFinished());

        actionService2.cancel(actionId);
    }
}
