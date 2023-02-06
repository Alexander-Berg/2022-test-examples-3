package ru.yandex.market.fulfillment.stockstorage.config;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RequestContextExecutionSupportTest {

    @Test
    public void executeWithInheritedRequestContext() {
        ThreadPoolTaskExecutor executor =
            createExecutor(RequestContextExecutionSupport::runnableWithInheritedRequestContext);
        CyclicBarrier barrier = new CyclicBarrier(2);

        AtomicReference<RequestContext> executorContext = new AtomicReference<>();
        RequestContext externalContext = RequestContext.EMPTY;
        RequestContextHolder.setContext(externalContext);

        // Создаем с пустым изначальным контекстом, так как при создании потока контекст наследуется из родителя
        executeAndReset(executor, barrier, executorContext);

        assertEquals(executorContext.get(), externalContext);

        // Создаем новый контекст и смотрим, что он унаследовался в экзекьюторе
        externalContext = RequestContextHolder.createNewContext();
        executeAndReset(executor, barrier, executorContext);

        assertEquals(executorContext.get(), externalContext);

        // Создаем новый контекст и смотрим, что он снова унаследовался в экзекьюторе, а не остался прежним
        externalContext = RequestContextHolder.createNewContext();
        executeAndReset(executor, barrier, executorContext);

        assertEquals(executorContext.get(), externalContext);
    }

    @Test
    public void executeWithNewRequestContext() {
        ThreadPoolTaskExecutor executor =
            createExecutor(RequestContextExecutionSupport::runnableWithNewRequestContext);
        CyclicBarrier barrier = new CyclicBarrier(2);

        AtomicReference<RequestContext> executorContext = new AtomicReference<>();
        RequestContext externalContext = RequestContext.EMPTY;

        // Создаем с пустым изначальным контекстом, так как при создании потока контекст наследуется из родителя
        // В экзекьюторе создастся полностью новый контекст
        executeAndReset(executor, barrier, executorContext);

        assertNotEquals(executorContext.get(), externalContext);
        assertNotEquals(executorContext.get(), RequestContext.EMPTY);
        assertNotNull(executorContext.get().getRequestId());

        // Создаем новый контекст и смотрим, что он НЕ унаследовался в экзекьюторе
        // В экзекьюторе создастся полностью новый контекст
        externalContext = RequestContextHolder.createNewContext();
        executeAndReset(executor, barrier, executorContext);

        assertNotEquals(executorContext.get(), externalContext);
        assertNotEquals(executorContext.get(), RequestContext.EMPTY);
        assertNotNull(executorContext.get().getRequestId());

        // Создаем новый контекст и смотрим, что он НЕ унаследовался в экзекьюторе
        // В экзекьюторе создастся полностью новый контекст
        externalContext = RequestContextHolder.createNewContext();
        executeAndReset(executor, barrier, executorContext);

        assertNotEquals(executorContext.get(), externalContext);
        assertNotEquals(executorContext.get(), RequestContext.EMPTY);
        assertNotNull(executorContext.get().getRequestId());
    }

    @Test
    public void executeWithoutRequestContextHandling() {
        ThreadPoolTaskExecutor executor = createExecutor(null);
        CyclicBarrier barrier = new CyclicBarrier(2);

        AtomicReference<RequestContext> executorContext = new AtomicReference<>();
        RequestContext firstExternalContext = RequestContextHolder.createNewContext();

        // Создаем с НЕпустым изначальным контекстом, так как при создании потока контекст наследуется из родителя
        // В экзекьюторе будет контекст от родителя
        executeAndReset(executor, barrier, executorContext);

        assertEquals(executorContext.get(), firstExternalContext);
        assertNotEquals(executorContext.get(), RequestContext.EMPTY);

        // Создаем новый контекст и смотрим, что он НЕ унаследовался в экзекьюторе
        // В экзекьюторе будет контекст из первично созданного контекста
        RequestContext secondExternalContext = RequestContextHolder.createNewContext();
        executeAndReset(executor, barrier, executorContext);

        assertEquals(executorContext.get(), firstExternalContext);
        assertNotEquals(executorContext.get(), secondExternalContext);
    }

    @SneakyThrows
    public void executeAndReset(ThreadPoolTaskExecutor executor, CyclicBarrier barrier,
                                AtomicReference<RequestContext> executorContext) {
        executor.execute(() -> taskForExecution(barrier, executorContext));
        barrier.await();
        barrier.reset();
    }

    @SneakyThrows
    private void taskForExecution(CyclicBarrier barrier, AtomicReference<RequestContext> executorContext) {
        executorContext.set(RequestContextHolder.getContext());
        barrier.await();
    }

    public ThreadPoolTaskExecutor createExecutor(TaskDecorator decorator) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setTaskDecorator(decorator);
        executor.initialize();
        return executor;
    }
}
