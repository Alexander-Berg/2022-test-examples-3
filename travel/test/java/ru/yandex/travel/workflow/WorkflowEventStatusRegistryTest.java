package ru.yandex.travel.workflow;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.junit.Test;

import ru.yandex.travel.testing.time.SettableClock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class WorkflowEventStatusRegistryTest {

    @Test
    public void testRegisterResolve() throws ExecutionException, InterruptedException {
        SettableClock clock = new SettableClock();
        ExecutorService es = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder().setDaemon(true).build()
        );

        WorkflowEventStatusRegistry subj = new WorkflowEventStatusRegistry(
                clock, Duration.ofMillis(100), es
        );

        CompletableFuture<EWorkflowEventState> cs = subj.register(1L);
        subj.resolve(1L, EWorkflowEventState.WES_CRASHED);

        EWorkflowEventState st = cs.get();
        assertThat(st).isEqualTo(EWorkflowEventState.WES_CRASHED);

        MoreExecutors.shutdownAndAwaitTermination(es, 5, TimeUnit.SECONDS);
    }

    @Test
    public void testRegisterExpireOnResolve() {
        SettableClock clock = new SettableClock();
        ExecutorService es = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder().setDaemon(true).build()
        );

        WorkflowEventStatusRegistry subj = new WorkflowEventStatusRegistry(
                clock, Duration.ofMillis(100), es
        );

        CompletableFuture<EWorkflowEventState> cs = subj.register(1L);

        clock.setCurrentTime(Instant.now());

        CompletableFuture<EWorkflowEventState> cs2 = subj.register(2L);

        subj.resolve(2L, EWorkflowEventState.WES_CRASHED);

        // ExecutionException outside with cause TimeoutException inside
        assertThatCode(cs::get)
                .hasCauseInstanceOf(TimeoutException.class);
    }
}
