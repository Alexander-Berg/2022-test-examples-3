package ru.yandex.market.mbo.synchronizer.export.registry;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.synchronizer.export.ExportRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class RegistryWorkerTemplateTest {

    private enum Workers {
        WORKER_1, WORKER_2
    }

    @Mock
    private ExportRegistry registry;
    private InOrder inOrder;

    private ExecutorService threadPool;
    private RegistryWorkerTemplate<TestContext> workerTemplate;

    @Before
    public void setUp() {
        workerTemplate = RegistryWorkerTemplate.newRegistryWorker(registry, null, new TestContext());
        threadPool = Executors.newCachedThreadPool();
        inOrder = inOrder(registry);
    }

    @After
    public void teardown() {
        threadPool.shutdownNow();
    }

    @Test
    public void testWithNewContext() {
        AnotherTestContext anotherContext = new AnotherTestContext();

        RegistryWorkerTemplate<AnotherTestContext> newTemplate = workerTemplate.withNewContext(anotherContext);

        assertThat(newTemplate.getRegistry()).isEqualTo(workerTemplate.getRegistry());
        assertThat(newTemplate.getWorkerPrefix()).isEqualTo(workerTemplate.getWorkerPrefix());
        assertThat(newTemplate.getContext()).isEqualTo(anotherContext);
    }

    @Test
    public void testWorkerTemplateRun() {
        workerTemplate.run(Workers.WORKER_1.name(), ctx -> ctx.value++);
        workerTemplate.run(Workers.WORKER_2.name(), ctx -> ctx.value++);

        TestContext context = workerTemplate.getContext();
        assertThat(context.value).isEqualTo(2);

        verify(registry, never()).registerFailure(anyString(), any());

        inOrder.verify(registry).processWorkerStart(Workers.WORKER_1.name());
        inOrder.verify(registry).processWorkerFinish(Workers.WORKER_1.name());
        inOrder.verify(registry).processWorkerStart(Workers.WORKER_2.name());
        inOrder.verify(registry).processWorkerFinish(Workers.WORKER_2.name());
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testAdditionalInfoIsSuccessfulInsideFunction() {
        workerTemplate.run(Workers.WORKER_1.name(), ctx -> {
            ctx.writeAdditionalInfo("test info");
        });

        verify(registry).writeAdditionalInfo(Workers.WORKER_1.name(), "test info");

        // check outside function write is failed
        Assertions.assertThatThrownBy(() -> workerTemplate.getContext().writeAdditionalInfo("test"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testAdditionalInfoIsFailedOutsideFunction() {
        Assertions.assertThatThrownBy(() -> workerTemplate.getContext().writeAdditionalInfo("test"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testWorkerTemplateRunWithFailure() {
        assertThatThrownBy(() -> {
            workerTemplate.run(Workers.WORKER_1.name(), ctx -> {
                ctx.value++;
                throw new FailedWorkerException(Workers.WORKER_1.name() + " failed");
            });
            workerTemplate.run(Workers.WORKER_2.name(), ctx ->
                ctx.value++
            );
        })
            .isInstanceOf(FailedWorkerException.class);

        TestContext context = workerTemplate.getContext();
        assertThat(context.value).isEqualTo(1);

        verify(registry, never()).processWorkerFinish(anyString());

        inOrder.verify(registry).processWorkerStart(Workers.WORKER_1.name());
        inOrder.verify(registry).registerFailure(eq(Workers.WORKER_1.name()), any(FailedWorkerException.class));
        inOrder.verifyNoMoreInteractions();

        // check outside function write is failed (if inner method is failed)
        Assertions.assertThatThrownBy(() -> context.writeAdditionalInfo("test"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testManualFinishExternally() {
        RegistryWorkerTemplate.FinishCallback<TestContext> finishCallback =
            workerTemplate.runWithManualFinish(Workers.WORKER_1.name(), ctx -> {
                ctx.value++;
            });

        assertThat(workerTemplate.getContext().value).isEqualTo(1);

        inOrder.verify(registry).processWorkerStart(Workers.WORKER_1.name());
        verify(registry, never()).processWorkerFinish(anyString());

        finishCallback.doFinish();

        inOrder.verify(registry).processWorkerFinish(Workers.WORKER_1.name());
        verify(registry, never()).registerFailure(anyString(), any());
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testAdditionalInfoIsSuccessfulInManualFinishExternally() {
        RegistryWorkerTemplate.FinishCallback<TestContext> finishCallback =
            workerTemplate.runWithManualFinish(Workers.WORKER_1.name(), ctx -> {
                ctx.writeAdditionalInfo("test info");
            });

        verify(registry).writeAdditionalInfo(Workers.WORKER_1.name(), "test info");

        // write more additional data
        finishCallback.writeAdditionalInfo("test info 2");
        verify(registry).writeAdditionalInfo(Workers.WORKER_1.name(), "test info 2");

        finishCallback.doFinish();

        // check outside function write is failed
        Assertions.assertThatThrownBy(() -> finishCallback.writeAdditionalInfo("test"))
            .isInstanceOf(IllegalStateException.class);
        Assertions.assertThatThrownBy(() -> workerTemplate.getContext().writeAdditionalInfo("test"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testManualFinishExternallyWithFailure() {
        assertThatThrownBy(() ->
            workerTemplate.runWithManualFinish(Workers.WORKER_1.name(), ctx -> {
                throw new FailedWorkerException(Workers.WORKER_1.name() + " failed");
            })
        )
            .isInstanceOf(FailedWorkerException.class);

        inOrder.verify(registry).processWorkerStart(eq(Workers.WORKER_1.name()));
        inOrder.verify(registry).registerFailure(eq(Workers.WORKER_1.name()), any(FailedWorkerException.class));
        verify(registry, never()).processWorkerFinish(anyString());
    }

    @Test
    public void testAdditionalInfoIsFailedInManualFinishExternallyWithFailure() {
        assertThatThrownBy(() ->
            workerTemplate.runWithManualFinish(Workers.WORKER_1.name(), ctx -> {
                throw new FailedWorkerException(Workers.WORKER_1.name() + " failed");
            })
        )
            .isInstanceOf(FailedWorkerException.class);

        // check outside function write is failed
        Assertions.assertThatThrownBy(() -> workerTemplate.getContext().writeAdditionalInfo("test"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testAsyncManualFinishExternallyWithFailure() {
        RegistryWorkerTemplate.FinishCallback<TestContext> finishCallback;
        List<Future<?>> tasks = new ArrayList<>();

        try {
            finishCallback =
                workerTemplate.runWithManualFinish(Workers.WORKER_1.name(), ctx -> {
                    tasks.add(
                        threadPool.submit(() -> {
                            ctx.atomicValue.incrementAndGet();
                        }));
                    tasks.add(
                        threadPool.submit(() -> {
                            throw new FailedWorkerException(Workers.WORKER_1.name() + " failed");
                        }));
                });
        } finally {
            inOrder.verify(registry).processWorkerStart(eq(Workers.WORKER_1.name()));
            verify(registry, never()).processWorkerFinish(anyString());
            verify(registry, never()).registerFailure(anyString(), any(FailedWorkerException.class));
        }

        assertNotNull(finishCallback);
        assertFalse(tasks.isEmpty());

        assertThatThrownBy(() ->
            finishCallback.doFinishAfter(ctx ->
                waitForTasks(tasks)
            )
        )
            .isInstanceOf(FailedWorkerException.class);

        inOrder.verify(registry).registerFailure(eq(Workers.WORKER_1.name()), any(FailedWorkerException.class));
        verify(registry, never()).processWorkerFinish(anyString());
    }

    @Test
    public void testManualFinishInWorker() {
        workerTemplate.runWithManualFinish(Workers.WORKER_1.name(),
            (ctx, finishCallback) -> {
                ctx.value++;
                finishCallback.doFinish();
            });

        assertThat(workerTemplate.getContext().value).isEqualTo(1);

        inOrder.verify(registry).processWorkerStart(Workers.WORKER_1.name());
        inOrder.verify(registry).processWorkerFinish(Workers.WORKER_1.name());
        verify(registry, never()).registerFailure(anyString(), any());
    }

    @Test
    public void testManualFinishInWorkerWithFailure() {
        assertThatThrownBy(() -> {
            workerTemplate.runWithManualFinish(Workers.WORKER_1.name(),
                (ctx, finishCallback) -> {
                    throw new FailedWorkerException(Workers.WORKER_1.name() + " failed");
                });
        })
            .isInstanceOf(FailedWorkerException.class);

        inOrder.verify(registry).processWorkerStart(Workers.WORKER_1.name());
        inOrder.verify(registry).registerFailure(eq(Workers.WORKER_1.name()), any(FailedWorkerException.class));
        verify(registry, never()).processWorkerFinish(anyString());
    }

    @Test
    public void testCountDownWrapperWithTaskList() {
        List<Callable<Void>> tasks = new ArrayList<>();

        workerTemplate.runWithManualFinish(Workers.WORKER_1.name(),
            (ctx, finishCallback) -> {
                RegistryWorkerTemplate.FinishOnCountDownWrapper wrapper = finishCallback.toCountDownWrapper();
                tasks.add(
                    wrapper.wrap(
                        () -> {
                            ctx.atomicValue.incrementAndGet();
                            return null;
                        }
                    ));
                tasks.add(
                    wrapper.wrap(
                        () -> {
                            ctx.atomicValue.incrementAndGet();
                            return null;
                        }
                    ));
                wrapper.setNotFinishedCount(tasks.size());
            });

        assertFalse(tasks.isEmpty());

        waitForTasks(
            tasks.stream()
                .map(task -> threadPool.submit(task))
                .collect(Collectors.toList())
        );

        assertThat(workerTemplate.getContext().atomicValue.get()).isEqualTo(2);
        inOrder.verify(registry).processWorkerStart(eq(Workers.WORKER_1.name()));
        inOrder.verify(registry).processWorkerFinish(eq(Workers.WORKER_1.name()));
        verify(registry, never()).registerFailure(anyString(), any(FailedWorkerException.class));
    }

    @Test
    public void testCountDownWrapperForCallableWithFailure() {
        List<Callable<Void>> tasks = new ArrayList<>();

        workerTemplate.runWithManualFinish(Workers.WORKER_1.name(),
            (ctx, finishCallback) -> {
                RegistryWorkerTemplate.FinishOnCountDownWrapper wrapper = finishCallback.toCountDownWrapper();
                tasks.add(
                    wrapper.wrap(
                        () -> {
                            ctx.atomicValue.incrementAndGet();
                            return null;
                        }
                    ));
                tasks.add(
                    wrapper.wrap(
                        () -> {
                            throw new FailedWorkerException(Workers.WORKER_1.name() + " failed");
                        }
                    ));
                wrapper.setNotFinishedCount(tasks.size());
            });

        assertFalse(tasks.isEmpty());

        assertThatThrownBy(() ->
            waitForTasks(
                tasks.stream()
                    .map(task -> threadPool.submit(task))
                    .collect(Collectors.toList())
            )
        )
            .isInstanceOf(FailedWorkerException.class);

        inOrder.verify(registry).processWorkerStart(eq(Workers.WORKER_1.name()));
        inOrder.verify(registry).registerFailure(eq(Workers.WORKER_1.name()), any(FailedWorkerException.class));
        verify(registry, never()).processWorkerFinish(anyString());
    }

    @Test
    public void testCountDownWrapperForRunnableWithFailure() {
        List<Runnable> tasks = new ArrayList<>();

        workerTemplate.runWithManualFinish(Workers.WORKER_1.name(),
            (ctx, finishCallback) -> {
                RegistryWorkerTemplate.FinishOnCountDownWrapper wrapper = finishCallback.toCountDownWrapper();
                tasks.add(
                    wrapper.wrap(
                        () -> {
                            ctx.atomicValue.incrementAndGet();
                        }
                    ));
                tasks.add(
                    wrapper.wrap(
                        (Runnable) () -> {
                            throw new FailedWorkerException(Workers.WORKER_1.name() + " failed");
                        }
                    ));
                wrapper.setNotFinishedCount(tasks.size());
            });

        assertFalse(tasks.isEmpty());

        assertThatThrownBy(() ->
            waitForTasks(
                tasks.stream()
                    .map(task -> threadPool.submit(task))
                    .collect(Collectors.toList())
            )
        )
            .isInstanceOf(FailedWorkerException.class);

        inOrder.verify(registry).processWorkerStart(eq(Workers.WORKER_1.name()));
        inOrder.verify(registry).registerFailure(eq(Workers.WORKER_1.name()), any(FailedWorkerException.class));
        verify(registry, never()).processWorkerFinish(anyString());
    }

    @Test
    public void testWrapAndFinishOnGetSuccess() throws Exception {
        Future<Void> future = workerTemplate.wrapAndFinishOnGet(
            Workers.WORKER_1.name(), ctx -> {
                ctx.writeAdditionalInfo("test");
                return threadPool.submit(() -> null);
            });

        inOrder.verify(registry).processWorkerStart(eq(Workers.WORKER_1.name()));
        Mockito.verify(registry).writeAdditionalInfo(eq(Workers.WORKER_1.name()), eq("test"));
        verify(registry, never()).processWorkerFinish(anyString());

        future.get();

        inOrder.verify(registry).processWorkerFinish(eq(Workers.WORKER_1.name()));
        verify(registry, never()).registerFailure(eq(Workers.WORKER_1.name()), any());
    }

    @Test
    public void testWrapAndFinishOnGetFailure() {
        assertThatThrownBy(() -> {
            workerTemplate.wrapAndFinishOnGet(
                Workers.WORKER_1.name(), ctx -> {
                    ctx.writeAdditionalInfo("test");
                    if (ctx.atomicValue != null) {
                        throw new FailedWorkerException(Workers.WORKER_1.name() + " failed");
                    }
                    return threadPool.submit(() -> null);
                });
        })
            .isInstanceOf(FailedWorkerException.class);

        inOrder.verify(registry).processWorkerStart(eq(Workers.WORKER_1.name()));
        Mockito.verify(registry).writeAdditionalInfo(eq(Workers.WORKER_1.name()), eq("test"));
        inOrder.verify(registry).registerFailure(eq(Workers.WORKER_1.name()), any());
        verify(registry, never()).processWorkerFinish(eq(Workers.WORKER_1.name()));
    }

    @Test
    public void testWrapAndFinishOnGetFailureInFuture() {
        Future<Void> future = workerTemplate.wrapAndFinishOnGet(
            Workers.WORKER_1.name(), ctx -> {
                ctx.writeAdditionalInfo("test");
                return threadPool.submit(() -> {
                    throw new FailedWorkerException(Workers.WORKER_1.name() + " failed");
                });
            });

        inOrder.verify(registry).processWorkerStart(eq(Workers.WORKER_1.name()));
        Mockito.verify(registry).writeAdditionalInfo(eq(Workers.WORKER_1.name()), eq("test"));
        verify(registry, never()).processWorkerFinish(anyString());

        assertThatThrownBy(
            future::get
        )
            .isInstanceOf(ExecutionException.class)
            .hasCauseInstanceOf(FailedWorkerException.class);

        inOrder.verify(registry).registerFailure(eq(Workers.WORKER_1.name()), any());
        verify(registry, never()).processWorkerFinish(eq(Workers.WORKER_1.name()));
    }

    private void waitForTasks(List<Future<?>> tasks) {
        tasks.forEach(task -> {
            try {
                task.get();
            } catch (Exception e) {
                throw new FailedWorkerException(e);
            }
        });
    }

    private static class TestContext extends BaseRegistryContext {
        private int value;
        private AtomicInteger atomicValue = new AtomicInteger();
    }

    private static class AnotherTestContext extends BaseRegistryContext {
        private int value;
    }

    private static class FailedWorkerException extends RuntimeException {
        FailedWorkerException(String s) {
            super(s);
        }

        FailedWorkerException(Throwable throwable) {
            super(throwable);
        }
    }
}
