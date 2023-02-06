package ru.yandex.direct.scheduler.support;

import java.util.function.Consumer;

import org.junit.Test;

import ru.yandex.direct.scheduler.hourglass.TaskParametersMap;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DaemonJobWrapperTests {
    @Test
    public void executeStopsWhenInterrupted() {
        // Daemon is interrupted in the first call to payload.execute, should
        // only call timer.await once
        var job = new DaemonStoppingJob(DaemonJobWrapper::interrupt);
        var interruptibleTimer = mock(InterruptableTimer.class);

        var daemonWrapper = new DaemonJobWrapper(job, interruptibleTimer, 10, 100);
        job.setDaemon(daemonWrapper);

        daemonWrapper.execute(null);

        verify(interruptibleTimer, times(1)).await();
        // Check that onShutdown() hasn't been called
        assertFalse(job.getIsShutdown());
    }

    @Test
    public void executeStopsWhenShutdown() {
        // Same test as executeStopsWhenInterrupted, but with job calling onShutdown instead of interrupt
        var job = new DaemonStoppingJob(DaemonJobWrapper::onShutdown);
        var interruptibleTimer = mock(InterruptableTimer.class);

        var daemonWrapper = new DaemonJobWrapper(job, interruptibleTimer, 10, 100);
        job.setDaemon(daemonWrapper);

        daemonWrapper.execute(null);

        verify(interruptibleTimer, times(1)).await();
        // Check that onShutdown() has completed successfully
        assertTrue(job.getIsShutdown());
    }

    @Test
    public void executeProcessesItsUploadOnIteration() {
        var job = mock(BaseDirectJob.class);
        var interruptingTimer = new DaemonInterruptingTimer();

        var daemonWrapper = new DaemonJobWrapper(job, interruptingTimer, 10, 100);
        interruptingTimer.setDaemon(daemonWrapper);

        daemonWrapper.execute(mock(TaskParametersMap.class));

        verify(job).execute(any(TaskParametersMap.class));
    }

    @Test
    public void executeWaitsForFiringOfTheTimerOnIteration() {
        var job = new DaemonStoppingJob(DaemonJobWrapper::interrupt);
        var interruptibleTimer = mock(InterruptableTimer.class);

        var daemonWrapper = new DaemonJobWrapper(job, interruptibleTimer, 10, 100);
        job.setDaemon(daemonWrapper);

        daemonWrapper.execute(null);

        verify(interruptibleTimer).await();
    }

    @Test
    public void daemonRunFixIteration() {
        int maxIterations = 2;
        var job = new JobsFailAfterTenExecuting();
        var interruptibleTimer = mock(InterruptableTimer.class);

        var daemonWrapper = new DaemonJobWrapper(job, interruptibleTimer, 10, maxIterations);

        assertThatCode(() -> daemonWrapper.execute(null)).doesNotThrowAnyException();

        verify(interruptibleTimer, times(2)).await();
    }

    private class DaemonStoppingJob extends BaseDirectJob {
        private DaemonJobWrapper daemon;
        private Consumer<DaemonJobWrapper> stoppingMethod;
        private boolean isShutdown;

        DaemonStoppingJob(Consumer<DaemonJobWrapper> stoppingMethod) {
            this.stoppingMethod = stoppingMethod;
            this.isShutdown = false;
        }

        void setDaemon(DaemonJobWrapper daemon) {
            this.daemon = daemon;
        }

        boolean getIsShutdown() {
            return this.isShutdown;
        }

        @Override
        public void execute() {
            this.stoppingMethod.accept(this.daemon);
        }

        @Override
        public void onShutdown() {
            this.isShutdown = true;
        }
    }

    private class DaemonInterruptingTimer extends InterruptableTimer {
        private DaemonJobWrapper daemon;

        DaemonInterruptingTimer() {
            super();
        }

        void setDaemon(DaemonJobWrapper daemon) {
            this.daemon = daemon;
        }

        @Override
        public void set(int seconds) {
        }

        @Override
        public void await() {
            this.daemon.interrupt();
        }

        @Override
        public void interrupt() {
        }
    }

    /**
     * Джоба для проверки того, что DaemonJobWrapper выполнит только указанное количество итераций
     * Падает после 10 итераций для того, чтоб выполнение джобы не зависло в цикле внутри DaemonJobWrapper, если все
     * таки тест не работает
     */
    private class JobsFailAfterTenExecuting extends BaseDirectJob {
        private final int iterationsCnt = 10;
        private int currentIteration = 0;

        @Override
        public void execute() {
            if (currentIteration >= iterationsCnt) {
                throw new RuntimeException("Job has been run more than after " + iterationsCnt + " iterations");
            }
            currentIteration++;
        }
    }
}
