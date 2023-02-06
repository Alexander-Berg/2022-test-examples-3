package ru.yandex.market.monitoring;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Vlad Vinogradov <a href="mailto:vladvin@yandex-team.ru"></a> on 18.01.17
 */
public class MonitoringResultBuilderTest {

    private ComplicatedMonitoring.MonitoringResultBuilder resultBuilder;

    public MonitoringResultBuilderTest() {
    }

    private void addUnitAndCheck(MonitoringUnit unit, MonitoringStatus status, String message) {
        resultBuilder.addUnit(unit);
        final ComplicatedMonitoring.Result result = resultBuilder.getResult();
        Assert.assertEquals(status, result.getStatus());
        Assert.assertEquals(message, result.getMessage());
    }

    @Test
    public void getResult() throws Exception {
        this.resultBuilder = new ComplicatedMonitoring.MonitoringResultBuilder();

        final MonitoringUnit okUnit = new MonitoringUnit("Ok");
        okUnit.ok();
        addUnitAndCheck(okUnit, MonitoringStatus.OK, "OK");

        final MonitoringUnit warnUnit = new MonitoringUnit("Warn1");
        warnUnit.warning("Achtung!");
        addUnitAndCheck(warnUnit, MonitoringStatus.WARNING, "WARN {Warn1: Achtung!}");

        final MonitoringUnit warnUnit2 = new MonitoringUnit("Warn2");
        warnUnit2.warning("Achtung 2!");
        addUnitAndCheck(warnUnit2, MonitoringStatus.WARNING, "WARN {Warn1: Achtung!, Warn2: Achtung 2!}");

        final MonitoringUnit okUnitWithMessage = new MonitoringUnit("Ok message");
        okUnitWithMessage.ok("Don't panic");
        addUnitAndCheck(okUnitWithMessage, MonitoringStatus.WARNING, "WARN {Warn1: Achtung!, Warn2: Achtung 2!} OK {Ok message: Don't panic}");

        final MonitoringUnit critUnit = new MonitoringUnit("Crit");
        critUnit.critical("All dead!!!");
        addUnitAndCheck(critUnit, MonitoringStatus.CRITICAL, "CRIT {Crit: All dead!!!} WARN {Warn1: Achtung!, Warn2: Achtung 2!} OK {Ok message: Don't panic}");
    }

}