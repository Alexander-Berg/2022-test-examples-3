package ru.yandex.market.application.monitoring;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author s-ermakov
 */
public class ComplexMonitoringTest {

    @Test
    public void testMonitoringWithNoUnits() {
        ComplexMonitoring monitoring = new ComplexMonitoring();
        Assert.assertEquals(MonitoringStatus.OK, monitoring.getResult().getStatus());
    }

    @Test
    public void testUnit() {
        ComplexMonitoring monitoring = new ComplexMonitoring();

        MonitoringUnit testUnit = monitoring.createUnit("test");
        Assert.assertEquals(MonitoringStatus.OK, monitoring.getResult().getStatus());

        testUnit.warning("warning message");
        Assert.assertEquals(MonitoringStatus.WARNING, monitoring.getResult().getStatus());
        Assert.assertEquals("WARN {test: warning message}", monitoring.getResult().getMessage());

        testUnit.critical("crit message");
        Assert.assertEquals(MonitoringStatus.CRITICAL, monitoring.getResult().getStatus());
        Assert.assertEquals("CRIT {test: crit message}", monitoring.getResult().getMessage());

        testUnit.ok();
        Assert.assertEquals(MonitoringStatus.OK, monitoring.getResult().getStatus());
    }

    @Test
    public void testSeveralUnits() {
        ComplexMonitoring monitoring = new ComplexMonitoring();
        MonitoringUnit test1Unit = monitoring.createUnit("test1");
        MonitoringUnit test2Unit = monitoring.createUnit("test2");
        Assert.assertEquals(MonitoringStatus.OK, monitoring.getResult().getStatus());

        test1Unit.warning("warning message");
        Assert.assertEquals(MonitoringStatus.WARNING, monitoring.getResult().getStatus());
        Assert.assertEquals("WARN {test1: warning message}", monitoring.getResult().getMessage());

        test2Unit.warning("warning message");
        Assert.assertEquals(MonitoringStatus.WARNING, monitoring.getResult().getStatus());
        Assert.assertEquals("WARN {test2: warning message, test1: warning message}",
            monitoring.getResult().getMessage());

        test1Unit.critical("crit message");
        Assert.assertEquals(MonitoringStatus.CRITICAL, monitoring.getResult().getStatus());
        Assert.assertEquals("CRIT {test1: crit message} WARN {test2: warning message}",
            monitoring.getResult().getMessage());

        test2Unit.critical("crit message");
        Assert.assertEquals(MonitoringStatus.CRITICAL, monitoring.getResult().getStatus());
        Assert.assertEquals("CRIT {test2: crit message, test1: crit message}",
            monitoring.getResult().getMessage());
    }
}
