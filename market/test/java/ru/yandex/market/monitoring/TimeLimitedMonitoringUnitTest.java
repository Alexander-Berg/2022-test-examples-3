package ru.yandex.market.monitoring;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TimeLimitedMonitoringUnitTest {
    private MonitoringUnit timeLimitedMonitoringUnit;

    @Before
    public void initMonitoring() {
        timeLimitedMonitoringUnit = new MonitoringUnit("Test unit");
        timeLimitedMonitoringUnit.setWarningTimeoutMillis(200);
        timeLimitedMonitoringUnit.setCriticalTimeoutMillis(500);
    }

    private void doSleep(int milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void checkTimeout() throws InterruptedException {
        timeLimitedMonitoringUnit.ok();
        Assert.assertEquals(MonitoringStatus.OK, timeLimitedMonitoringUnit.getStatus());
        doSleep(300);
        Assert.assertEquals(MonitoringStatus.WARNING, timeLimitedMonitoringUnit.getStatus());
        doSleep(300);
        Assert.assertEquals(MonitoringStatus.CRITICAL, timeLimitedMonitoringUnit.getStatus());
        timeLimitedMonitoringUnit.ok();
        Assert.assertEquals(MonitoringStatus.OK, timeLimitedMonitoringUnit.getStatus());
    }
}
