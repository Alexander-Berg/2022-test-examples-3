package ru.yandex.direct.scheduler.support;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class PeriodicJobWrapperTest {
    @Test
    public void executeStopsWhenInterrupted() {
        var job = new PeriodicShutdownJob();
        var periodicWrapper = new PeriodicJobWrapper(job);
        job.setWrapper(periodicWrapper);

        periodicWrapper.execute(null);

        // Check that onShutdown() has completed successfully
        assertTrue(job.getIsShutdown());
    }

    private class PeriodicShutdownJob extends BaseDirectJob {
        private PeriodicJobWrapper wrapper;
        private boolean isShutdown = false;

        void setWrapper(PeriodicJobWrapper wrapper) {
            this.wrapper = wrapper;
        }

        boolean getIsShutdown() {
            return this.isShutdown;
        }

        @Override
        public void execute() {
            this.wrapper.onShutdown();
        }

        @Override
        public void onShutdown() {
            this.isShutdown = true;
        }
    }
}
