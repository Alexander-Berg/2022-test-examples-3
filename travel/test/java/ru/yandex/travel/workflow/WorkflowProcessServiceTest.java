package ru.yandex.travel.workflow;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.Message;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.bolts.collection.Option;
import ru.yandex.travel.commons.proto.TError;
import ru.yandex.travel.test.fake.proto.TTestStartEvent;
import ru.yandex.travel.test.fake.proto.TTestState;
import ru.yandex.travel.workflow.entities.TestEntity;
import ru.yandex.travel.workflow.entities.Workflow;
import ru.yandex.travel.workflow.exceptions.ProcessingStoppedException;
import ru.yandex.travel.workflow.exceptions.RetryableException;
import ru.yandex.travel.workflow.exceptions.WorkflowCrashedException;
import ru.yandex.travel.workflow.repository.TestEntityRepository;
import ru.yandex.travel.workflow.repository.WorkflowRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;


@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "single-node.auto-start=true"
        }
)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Slf4j
public class WorkflowProcessServiceTest {
    @Autowired
    private WorkflowProcessService workflowProcessService;

    @Autowired
    private TestEntityRepository testEntityRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private WorkflowRepository workflowRepository;

    @MockBean
    private WorkflowEventHandlerMatcher workflowEventHandlerMatcher;

    @Autowired
    private WorkflowEventQueue workflowEventQueue;

    @Test
    public void testWorkflowRuns() {
        WorkflowEventHandler<?> mockHandler = mock(WorkflowEventHandler.class);
        when(workflowEventHandlerMatcher.findEventHandlerFor(any(), any(TTestStartEvent.class))).thenReturn(Option.of(mockHandler));

        TestEntityInfo info = createTestEntity(TTestState.TS_FIRST);

        doNothing().when(mockHandler).handleEvent(any(TTestStartEvent.class), any());

        Future<Void> future = workflowProcessService.scheduleEventWithLocalTracking(info.workflowId,
                TTestStartEvent.newBuilder().build());

        assertThatCode(future::get).doesNotThrowAnyException();
        verify(mockHandler, times(1)).handleEvent(any(TTestStartEvent.class), any());
        verifyWorkflow(info.workflowId, EWorkflowState.WS_RUNNING, 0);
    }

    @Test
    public void testWhenWorkflowRunsChangesToEntitiesSaved() {
        WorkflowEventHandler<?> mockHandler = mock(WorkflowEventHandler.class);
        when(workflowEventHandlerMatcher.findEventHandlerFor(any(), any(TTestStartEvent.class))).thenReturn(Option.of(mockHandler));

        TestEntityInfo info = createTestEntity(TTestState.TS_FIRST);

        doAnswer(invocation -> {
            MessagingContext<TestEntity> context = invocation.getArgument(1);
            context.getWorkflowEntity().setState(TTestState.TS_SECOND);
            return null;
        }).when(mockHandler).handleEvent(any(TTestStartEvent.class), any());

        Future<Void> future = workflowProcessService.scheduleEventWithLocalTracking(info.workflowId,
                TTestStartEvent.newBuilder().build());

        assertThatCode(future::get).doesNotThrowAnyException();
        verify(mockHandler, times(1)).handleEvent(any(TTestStartEvent.class), any());
        verifyWorkflow(info.workflowId, EWorkflowState.WS_RUNNING, 0);

        transactionTemplate.execute((ignored) -> {
            TestEntity entity = testEntityRepository.getOne(info.entityId);
            assertThat(entity.getState()).isEqualTo(TTestState.TS_SECOND);
            return null;
        });
    }

    @Test
    public void testWorkflowCrashesOnUnhandledException() {
        WorkflowEventHandler<?> mockHandler = mock(WorkflowEventHandler.class);
        when(workflowEventHandlerMatcher.findEventHandlerFor(any(), any(TTestStartEvent.class))).thenReturn(Option.of(mockHandler));

        TestEntityInfo info = createTestEntity(TTestState.TS_FIRST);

        doThrow(new RuntimeException("Injected Error")).when(mockHandler).handleEvent(any(TTestStartEvent.class),
                any());

        Future<Void> future = workflowProcessService.scheduleEventWithLocalTracking(info.workflowId,
                TTestStartEvent.newBuilder().build());

        assertThatCode(future::get).hasCauseInstanceOf(WorkflowCrashedException.class);
        verify(mockHandler, times(1)).handleEvent(any(TTestStartEvent.class), any());
        verifyWorkflow(info.workflowId, EWorkflowState.WS_CRASHED, 0);
    }

    @Test
    public void testWhenWorkflowCrashesPendingFuturesResolved() {
        WorkflowEventHandler<?> mockHandler = mock(WorkflowEventHandler.class);
        when(workflowEventHandlerMatcher.findEventHandlerFor(any(), any(TTestStartEvent.class))).thenReturn(Option.of(mockHandler));

        TestEntityInfo info = createTestEntity(TTestState.TS_FIRST);

        CountDownLatch startSignal = new CountDownLatch(1);
        doAnswer(invocation -> {
            startSignal.await();
            throw new RuntimeException("Injected Error");
        }).when(mockHandler).handleEvent(any(TTestStartEvent.class), any());

        Future<Void> firstFuture = workflowProcessService.scheduleEventWithLocalTracking(info.workflowId,
                TTestStartEvent.newBuilder().build());
        Future<Void> secondFuture = workflowProcessService.scheduleEventWithLocalTracking(info.workflowId,
                TTestStartEvent.newBuilder().build());
        Future<Void> thirdFuture = workflowProcessService.scheduleEventWithLocalTracking(info.workflowId,
                TTestStartEvent.newBuilder().build());

        startSignal.countDown();

        assertThatCode(firstFuture::get).hasCauseInstanceOf(WorkflowCrashedException.class);
        assertThatCode(secondFuture::get).hasCauseInstanceOf(WorkflowCrashedException.class);
        assertThatCode(thirdFuture::get).hasCauseInstanceOf(WorkflowCrashedException.class);
        verify(mockHandler, times(1)).handleEvent(any(TTestStartEvent.class), any());
        verifyWorkflow(info.workflowId, EWorkflowState.WS_CRASHED, 2);
    }

    @Test
    public void testWhenWorkflowCrashesChangesToEntitiesNotSaved() {
        WorkflowEventHandler<?> mockHandler = mock(WorkflowEventHandler.class);
        when(workflowEventHandlerMatcher.findEventHandlerFor(any(), any(TTestStartEvent.class))).thenReturn(Option.of(mockHandler));

        TestEntityInfo info = createTestEntity(TTestState.TS_FIRST);

        doAnswer(invocation -> {
            MessagingContext<TestEntity> context = invocation.getArgument(1);
            context.getWorkflowEntity().setState(TTestState.TS_SECOND);
            throw new RuntimeException("Injected Error");
        }).when(mockHandler).handleEvent(any(TTestStartEvent.class), any());

        Future<Void> future = workflowProcessService.scheduleEventWithLocalTracking(info.workflowId,
                TTestStartEvent.newBuilder().build());

        assertThatCode(future::get).hasCauseInstanceOf(WorkflowCrashedException.class);
        verify(mockHandler, times(1)).handleEvent(any(TTestStartEvent.class), any());
        verifyWorkflow(info.workflowId, EWorkflowState.WS_CRASHED, 0);

        transactionTemplate.execute((ignored) -> {
            TestEntity entity = testEntityRepository.getOne(info.entityId);
            assertThat(entity.getState()).isEqualTo(TTestState.TS_FIRST);
            return null;
        });
    }

    @Test
    public void testWhenWorkflowCrashesUnhandledExceptionInfoPersisted() {
        WorkflowEventHandler<?> mockHandler = mock(WorkflowEventHandler.class);
        when(workflowEventHandlerMatcher.findEventHandlerFor(any(), any(TTestStartEvent.class))).thenReturn(Option.of(mockHandler));

        TestEntityInfo info = createTestEntity(TTestState.TS_FIRST);

        doThrow(new RuntimeException("Injected Error")).when(mockHandler).handleEvent(any(TTestStartEvent.class),
                any());

        Future<Void> future = workflowProcessService.scheduleEventWithLocalTracking(info.workflowId,
                TTestStartEvent.newBuilder().build());

        assertThatCode(future::get).hasCauseInstanceOf(WorkflowCrashedException.class);
        verify(mockHandler, times(1)).handleEvent(any(TTestStartEvent.class), any());
        verifyWorkflow(info.workflowId, EWorkflowState.WS_CRASHED, 0);

        transactionTemplate.execute((ignored) -> {
            Workflow workflow = workflowRepository.getOne(info.workflowId);
            assertThat(workflow.getState()).isEqualTo(EWorkflowState.WS_CRASHED);
            assertThat(workflow.getStateTransitions()).hasSize(1);
            Object transitionData = workflow.getStateTransitions().get(0).getData();
            assertThat(transitionData).isInstanceOf(TError.class);
            assertThat(((TError) transitionData).getMessage()).isEqualTo("Injected Error");
            assertThat(((TError) transitionData).getAttributeMap().get("class")).isEqualTo("java.lang" +
                    ".RuntimeException");
            return null;
        });
    }

    @Ignore
    @Test
    public void testWhenServiceIsPausedReturnsCompletedExceptionallyFuture() {
        TestEntityInfo info = createTestEntity(TTestState.TS_FIRST);

        workflowProcessService.pauseAll();
        Future<Void> future = workflowProcessService.scheduleEventWithLocalTracking(info.workflowId,
                TTestStartEvent.newBuilder().build());
        workflowProcessService.resume();

        assertThatCode(future::get).hasCauseInstanceOf(ProcessingStoppedException.class);
    }

    @Test
    public void testWorkflowRunsWithRetries() {
        WorkflowEventHandler<?> mockHandler = mock(WorkflowEventHandler.class);
        when(workflowEventHandlerMatcher.findEventHandlerFor(any(), any(TTestStartEvent.class))).thenReturn(Option.of(mockHandler));

        TestEntityInfo info = createTestEntity(TTestState.TS_FIRST);

        AtomicBoolean firstCall = new AtomicBoolean(false);
        doAnswer(invocation -> {
            if (firstCall.compareAndSet(false, true)) {
                throw new RetryableException("Injected Error");
            } else {
                MessagingContext<TestEntity> ctx = invocation.getArgument(1);
                ctx.getWorkflowEntity().setState(TTestState.TS_SECOND);
                return null;
            }
        }).when(mockHandler).handleEvent(any(TTestStartEvent.class), any());

        Future<Void> future = workflowProcessService.scheduleEventWithLocalTracking(info.workflowId,
                TTestStartEvent.newBuilder().build());

        assertThatCode(future::get).doesNotThrowAnyException();
        verify(mockHandler, times(2)).handleEvent(any(TTestStartEvent.class), any());
        verifyWorkflow(info.workflowId, EWorkflowState.WS_RUNNING, 0);
    }

    @Test
    public void testWorkflowCrashesOnMaxRetries() {
        WorkflowEventHandler<?> mockHandler = mock(WorkflowEventHandler.class);
        when(workflowEventHandlerMatcher.findEventHandlerFor(any(), any(TTestStartEvent.class))).thenReturn(Option.of(mockHandler));

        TestEntityInfo entitiesInfo = createTestEntity(TTestState.TS_FIRST);

        doThrow(new RetryableException("Injected Error")).when(mockHandler).handleEvent(any(TTestStartEvent.class),
                any());

        Future<Void> future = workflowProcessService.scheduleEventWithLocalTracking(entitiesInfo.workflowId,
                TTestStartEvent.newBuilder().build());

        assertThatCode(future::get).hasCauseInstanceOf(WorkflowCrashedException.class);
        verify(mockHandler, times(4)).handleEvent(any(TTestStartEvent.class), any());
        verifyWorkflow(entitiesInfo.workflowId, EWorkflowState.WS_CRASHED, 0);
    }

    @Test
    public void testSendAndReceive() {
        WorkflowEventHandler<?> mockHandler = mock(WorkflowEventHandler.class);
        when(workflowEventHandlerMatcher.findEventHandlerFor(any(), any(TTestStartEvent.class))).thenReturn(Option.of(mockHandler));

        TestEntityInfo aliceInfo = createTestEntity(TTestState.TS_ALICE_NEW);
        TestEntityInfo bobInfo = createTestEntity(TTestState.TS_BOB_NEW);

        CompletableFuture<Void> aliceFuture = new CompletableFuture<>();
        CompletableFuture<Void> bobFuture = new CompletableFuture<>();

        doAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            MessagingContext<TestEntity> ctx = invocation.getArgument(1);
            TestEntity entity = ctx.getWorkflowEntity();
            switch (entity.getState()) {
                case TS_ALICE_NEW:
                    assertThat(message).isInstanceOf(TTestStartEvent.class);
                    ctx.scheduleExternalEvent(bobInfo.workflowId, TTestStartEvent.newBuilder().build());
                    entity.setState(TTestState.TS_ALICE_SENT);
                    break;
                case TS_ALICE_SENT:
                    assertThat(message).isInstanceOf(TTestStartEvent.class);
                    ctx.scheduleEvent(TTestStartEvent.newBuilder().build());
                    entity.setState(TTestState.TS_ALICE_ACKNOWLEDGED);
                    break;
                case TS_ALICE_ACKNOWLEDGED:
                    assertThat(message).isInstanceOf(TTestStartEvent.class);
                    TxSynchronization.afterCommit(() -> aliceFuture.complete(null));
                    break;
                case TS_BOB_NEW:
                    assertThat(message).isInstanceOf(TTestStartEvent.class);
                    ctx.scheduleExternalEvent(aliceInfo.workflowId, TTestStartEvent.newBuilder().build());
                    ctx.scheduleEvent(TTestStartEvent.newBuilder().build());
                    entity.setState(TTestState.TS_BOB_RECEIVED);
                    break;
                case TS_BOB_RECEIVED:
                    assertThat(message).isInstanceOf(TTestStartEvent.class);
                    TxSynchronization.afterCommit(() -> bobFuture.complete(null));
                    break;
            }
            return null;
        }).when(mockHandler).handleEvent(any(), any());

        Future<Void> startFuture = workflowProcessService.scheduleEventWithLocalTracking(aliceInfo.workflowId,
                TTestStartEvent.newBuilder().build());

        assertThatCode(startFuture::get).doesNotThrowAnyException();
        assertThatCode(aliceFuture::get).doesNotThrowAnyException();
        assertThatCode(bobFuture::get).doesNotThrowAnyException();

        verify(mockHandler, times(5)).handleEvent(any(TTestStartEvent.class), any());
        verifyWorkflow(aliceInfo.workflowId, EWorkflowState.WS_RUNNING, 0);
        verifyWorkflow(bobInfo.workflowId, EWorkflowState.WS_RUNNING, 0);

        transactionTemplate.execute((ignored) -> {
            TestEntity aliceEntity = testEntityRepository.getOne(aliceInfo.entityId);
            assertThat(aliceEntity.getState()).isEqualTo(TTestState.TS_ALICE_ACKNOWLEDGED);
            TestEntity bobEntity = testEntityRepository.getOne(bobInfo.entityId);
            assertThat(bobEntity.getState()).isEqualTo(TTestState.TS_BOB_RECEIVED);
            return null;
        });
    }

    @Test
    public void testWhenWorkflowCrashesDeathLetterDeliveredToSupervisor() {
        WorkflowEventHandler<?> mockHandler = mock(WorkflowEventHandler.class);
        when(workflowEventHandlerMatcher.findEventHandlerFor(any(), any())).thenReturn(Option.of(mockHandler));

        TestEntityInfo aliceInfo = createTestEntity(TTestState.TS_ALICE_NEW);
        TestEntityInfo bobInfo = createTestEntity(TTestState.TS_BOB_NEW);

        transactionTemplate.execute((ignored) -> {
            workflowRepository.getOne(bobInfo.workflowId).setSupervisorId(aliceInfo.workflowId);
            return null;
        });

        CompletableFuture<Void> aliceFuture = new CompletableFuture<>();

        doAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            MessagingContext<TestEntity> ctx = invocation.getArgument(1);
            TestEntity entity = ctx.getWorkflowEntity();
            switch (entity.getState()) {
                case TS_ALICE_NEW:
                    assertThat(message).isInstanceOf(TTestStartEvent.class);
                    ctx.scheduleExternalEvent(bobInfo.workflowId, TTestStartEvent.newBuilder().build());
                    entity.setState(TTestState.TS_ALICE_SENT);
                    break;
                case TS_ALICE_SENT:
                    assertThat(message).isInstanceOf(TWorkflowCrashed.class);
                    assertThat(((TWorkflowCrashed) message).getWorkflowId()).isEqualTo(bobInfo.workflowId.toString());
                    ctx.scheduleEvent(TTestStartEvent.newBuilder().build());
                    entity.setState(TTestState.TS_ALICE_ACKNOWLEDGED);
                    break;
                case TS_ALICE_ACKNOWLEDGED:
                    TxSynchronization.afterCommit(() -> aliceFuture.complete(null));
                    break;
                case TS_BOB_NEW:
                    assertThat(message).isInstanceOf(TTestStartEvent.class);
                    throw new RuntimeException("Injected Error (Bob)");
            }
            return null;
        }).when(mockHandler).handleEvent(any(), any());

        Future<Void> startFuture = workflowProcessService.scheduleEventWithLocalTracking(aliceInfo.workflowId,
                TTestStartEvent.newBuilder().build());

        assertThatCode(startFuture::get).doesNotThrowAnyException();
        assertThatCode(aliceFuture::get).doesNotThrowAnyException();

        verify(mockHandler, times(4)).handleEvent(any(), any());
        verifyWorkflow(aliceInfo.workflowId, EWorkflowState.WS_RUNNING, 0);
        verifyWorkflow(bobInfo.workflowId, EWorkflowState.WS_CRASHED, 0);

        transactionTemplate.execute((ignored) -> {
            TestEntity aliceEntity = testEntityRepository.getOne(aliceInfo.entityId);
            assertThat(aliceEntity.getState()).isEqualTo(TTestState.TS_ALICE_ACKNOWLEDGED);
            TestEntity bobEntity = testEntityRepository.getOne(bobInfo.entityId);
            assertThat(bobEntity.getState()).isEqualTo(TTestState.TS_BOB_NEW);
            return null;
        });
    }

    @Test
    public void testWhenConcurrencyFailureExceptionThrownMessageProcessingRetried() throws InterruptedException {

        WorkflowEventHandler<?> mockHandler = mock(WorkflowEventHandler.class);
        when(workflowEventHandlerMatcher.findEventHandlerFor(any(), any())).thenReturn(Option.of(mockHandler));

        TestEntityInfo info = createTestEntity(TTestState.TS_FIRST);

        CountDownLatch startLatch = new CountDownLatch(2);
        CountDownLatch proceedLatch = new CountDownLatch(1);
        CountDownLatch continueLatch = new CountDownLatch(1);


        doAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            MessagingContext<TestEntity> ctx = invocation.getArgument(1);
            TestEntity entity = ctx.getWorkflowEntity();
            switch (entity.getState()) {
                case TS_FIRST:
                    startLatch.countDown();
                    proceedLatch.await();
                    // that must be useless as a concurrent failure exception will be thrown
                    entity.setState(TTestState.TS_THIRD);
                    System.out.println("third");
                    break;
                case TS_SECOND:
                    assertThat(message).isInstanceOf(TTestStartEvent.class);
                    entity.setState(TTestState.TS_THIRD);
                    break;
                default:
                    throw new RuntimeException("Test flow gone wrong we must not come here in any other state");
            }
            return null;
        }).when(mockHandler).handleEvent(any(), any());

        ExecutorService executorService = Executors.newFixedThreadPool(1,
                new ThreadFactoryBuilder().setDaemon(true).build());
        try {
            executorService.execute(() -> {
                transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        startLatch.countDown();
                        TestEntity testEntity = testEntityRepository.getOne(info.entityId);
                        testEntity.setState(TTestState.TS_SECOND);
                        System.out.println("second");
                        try {
                            continueLatch.await();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                proceedLatch.countDown();
            });


            Future<Void> startFuture = workflowProcessService.scheduleEventWithLocalTracking(info.workflowId,
                    TTestStartEvent.newBuilder().build());

            startLatch.await();
            continueLatch.countDown();

            assertThatCode(startFuture::get).doesNotThrowAnyException();
            verifyWorkflow(info.workflowId, EWorkflowState.WS_RUNNING, 0);
            verify(mockHandler, times(2)).handleEvent(any(), any());
        } finally {
            executorService.shutdown();
        }
    }

    @Test
    public void testWorkflowRunsWithDelayedRetries() {
        WorkflowEventHandler<?> mockHandler = mock(WorkflowEventHandler.class);
        when(workflowEventHandlerMatcher.findEventHandlerFor(any(), any(TTestStartEvent.class))).thenReturn(Option.of(mockHandler));

        TestEntityInfo info = createTestEntity(TTestState.TS_FIRST);

        AtomicInteger calls = new AtomicInteger(0);
        doAnswer(invocation -> {
            int attempt = calls.incrementAndGet();
            if (attempt == 1) {
                throw new RetryableException("first long retry", Duration.ofMinutes(10));
            } else if (attempt <= 5) {
                throw new RetryableException("next fast retry", Duration.ofMillis(100));
            } else {
                MessagingContext<TestEntity> ctx = invocation.getArgument(1);
                ctx.getWorkflowEntity().setState(TTestState.TS_SECOND);
                return null;
            }
        }).when(mockHandler).handleEvent(any(TTestStartEvent.class), any());

        Future<Void> future = workflowProcessService.scheduleEventWithLocalTracking(info.workflowId,
                TTestStartEvent.newBuilder().build());

        waitForState(() -> transactionTemplate.execute(tx -> {
            Workflow wf = workflowRepository.getOne(info.workflowId);
            return wf.getSleepTill() != null && workflowRepository.getOne(info.workflowId).getSleepTill()
                    .isAfter(Instant.now().plus(Duration.ofMinutes(9)));

        }), Duration.ofSeconds(10), "first wf.sleepTill is 10 minutes");

        // forcefully skipping the first long delay
        Instant longDelayEndTs = Instant.now();
        transactionTemplate.execute(tx -> {
            workflowRepository.getOne(info.workflowId).setSleepTill(Instant.now());
            return null;
        });

        assertThatCode(future::get).doesNotThrowAnyException();
        // the default strategy limit (4) doesn't apply to delayed events
        verify(mockHandler, times(6)).handleEvent(any(TTestStartEvent.class), any());
        verifyWorkflow(info.workflowId, EWorkflowState.WS_RUNNING, 0);
        // other delayed events have been processed faster
        assertThat(longDelayEndTs.plus(Duration.ofSeconds(10))).isAfter(Instant.now());
    }

    @Test
    public void testWorkflowCrashesOnMaxDelayedRetries() {
        WorkflowEventHandler<?> mockHandler = mock(WorkflowEventHandler.class);
        when(workflowEventHandlerMatcher.findEventHandlerFor(any(), any(TTestStartEvent.class))).thenReturn(Option.of(mockHandler));

        TestEntityInfo info = createTestEntity(TTestState.TS_FIRST);

        doThrow(new RetryableException("delayed retry", Duration.ofMillis(10)))
                .when(mockHandler).handleEvent(any(TTestStartEvent.class), any());

        Future<Void> future = workflowProcessService.scheduleEventWithLocalTracking(info.workflowId,
                TTestStartEvent.newBuilder().build());

        assertThatCode(future::get).hasCauseInstanceOf(WorkflowCrashedException.class);
        // delayed retries have their own limit (10)
        verify(mockHandler, times(11)).handleEvent(any(TTestStartEvent.class), any());
        verifyWorkflow(info.workflowId, EWorkflowState.WS_CRASHED, 0);
    }

    private static final class TestEntityInfo {
        UUID workflowId;
        UUID entityId;
    }

    private TestEntityInfo createTestEntity(TTestState state) {
        return transactionTemplate.execute(status -> {
            TestEntity newEntity = new TestEntity();
            newEntity.setId(UUID.randomUUID());
            newEntity.setState(state);
            testEntityRepository.save(newEntity);

            Workflow newWorkflow = Workflow.createWorkflowForEntity(newEntity);
            workflowRepository.saveAndFlush(newWorkflow);

            TestEntityInfo info = new TestEntityInfo();
            info.entityId = newEntity.getId();
            info.workflowId = newWorkflow.getId();
            return info;
        });
    }

    private void verifyWorkflow(UUID workflowId, EWorkflowState state, int pendingEvents) {
        transactionTemplate.execute(status -> {
            Workflow workflow = workflowRepository.getOne(workflowId);
            assertThat(workflow.getState()).isEqualTo(state);
            assertThat(workflowEventQueue.getPendingEventsCount(workflowId)).isEqualTo(pendingEvents);
            return null;
        });
    }

    @SuppressWarnings({"SameParameterValue", "BusyWait"})
    private void waitForState(Supplier<Boolean> stateTest, Duration timeout, String stateDescription) {
        long tryTill = System.currentTimeMillis() + timeout.toMillis();
        while (stateTest.get() != Boolean.TRUE && tryTill > System.currentTimeMillis()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        assertThat(tryTill)
                .withFailMessage("The desired state hasn't been reached: %s", stateDescription)
                .isGreaterThan(System.currentTimeMillis());
    }

    private static class TxSynchronization {
        public static void afterCommit(Runnable action) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        }
    }
}
