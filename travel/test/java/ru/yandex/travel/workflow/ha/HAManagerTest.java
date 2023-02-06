package ru.yandex.travel.workflow.ha;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import ru.yandex.misc.ExceptionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HAManagerTest {

    @Test
    public void testHAManagerTriesLockAcquiring() throws Exception {
        MasterLockManager masterLockManager = mock(MasterLockManager.class);

        when(masterLockManager.acquireLock(any())).thenReturn(false);
        MasterStatusAwareResource masterStatusAwareResource = mock(MasterStatusAwareResource.class);
        HAManager subject = new HAManager(
                masterLockManager,
                masterStatusAwareResource,
                Duration.ofMillis(100),
                Duration.ofMillis(80),
                Duration.ofMillis(20)
        );

        subject.afterPropertiesSet();

        Thread.sleep(100);

        subject.destroy();

        assertThat(subject.getCurrentState()).isEqualTo(HAManager.HAManagerState.STOPPED);
        verify(masterLockManager, atLeast(4)).acquireLock(any());
    }

    @Test
    public void testHAManagerExceptionWhenAcquireLock() throws Exception {
        MasterLockManager masterLockManager = mock(MasterLockManager.class);
        MasterStatusAwareResource masterStatusAwareResource = mock(MasterStatusAwareResource.class);
        CountDownLatch finished = new CountDownLatch(1);

        ExecutorService executor = Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder().setDaemon(true).build()
        );

        when(masterLockManager.acquireLock(any())).thenReturn(false)
                .thenThrow(new RuntimeException("Smth bad"))
                .thenReturn(false)
                .then((invocation) -> {
                    finished.countDown();
                    return false;
                });


        HAManager subject = new HAManager(
                masterLockManager,
                masterStatusAwareResource,
                Duration.ofMillis(100),
                Duration.ofMillis(20),
                Duration.ofMillis(20)
        );

        subject.afterPropertiesSet();
        assertThat(subject.getCurrentState()).isEqualTo(HAManager.HAManagerState.STANDBY);
        finished.await(1L, TimeUnit.SECONDS);
        verify(masterLockManager, atLeast(4)).acquireLock(any());
    }

    @Test
    public void testHAManagerPromotedToMaster() throws Exception {
        MasterLockManager masterLockManager = mock(MasterLockManager.class);
        CountDownLatch releaseLockCounter = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);
        CountDownLatch forceFinished = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        CountDownLatch stopped = new CountDownLatch(1);

        ExecutorService executor = Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder().setDaemon(true).build()
        );

        when(masterLockManager.acquireLock(any())).thenAnswer(
                (Answer<Boolean>) invocation -> {
                    executor.submit(
                            () -> {
                                try {
                                    releaseLockCounter.await();
                                    LockLostCallback lockLostCallback = invocation.getArgument(0);
                                    lockLostCallback.lockLost();
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                    );
                    return true;
                }
        );


        MasterStatusAwareResource masterStatusAwareResource = new MasterStatusAwareResource() {
            @Override
            public void promotedToMaster() {
                started.countDown();
            }

            @Override
            public void prepareToStandby() {
                try {
                    finish.await();
                } catch (InterruptedException e) {
                    throw ExceptionUtils.throwException(e);
                }
                finished.countDown();
            }

            @Override
            public void forceStandby() {
                forceFinished.countDown();
            }

            @Override
            public void stopAll() {
                stopped.countDown();
            }
        };


        HAManager subject = new HAManager(
                masterLockManager,
                masterStatusAwareResource,
                Duration.ofMillis(100),
                Duration.ofMillis(20),
                Duration.ofMillis(20)
        );

        subject.afterPropertiesSet();
        assertThat(subject.getCurrentState()).isEqualTo(HAManager.HAManagerState.PROMOTING);
        started.await();
        assertThat(subject.getCurrentState()).isEqualTo(HAManager.HAManagerState.MASTER);
        releaseLockCounter.countDown();
        forceFinished.await();
        assertThat(subject.getCurrentState()).isEqualTo(HAManager.HAManagerState.FORCED_STOP);
        finish.countDown();
        finish.await();
        Thread.sleep(200);
        assertThat(subject.getCurrentState()).isEqualTo(HAManager.HAManagerState.STANDBY);
        subject.destroy();
        stopped.await();
        assertThat(subject.getCurrentState()).isEqualTo(HAManager.HAManagerState.STOPPED);
    }

}
