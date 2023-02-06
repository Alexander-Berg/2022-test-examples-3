package ru.yandex.market.db.monitoring;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.application.monitoring.ComplexMonitoring.Result;
import ru.yandex.market.application.monitoring.MonitoringStatus;

public class DbMonitoringUtilsTest {

    @Test
    public void mergeOkAndOk() {
        Result result1 = new Result(MonitoringStatus.OK, MonitoringStatus.OK.getGolemName());
        Result result2 = new Result(MonitoringStatus.OK, MonitoringStatus.OK.getGolemName());

        Result result = DbMonitoringUtils.merge(result1, result2);
        Assertions.assertThat(result.getStatus()).isEqualTo(MonitoringStatus.OK);
        Assertions.assertThat(result.getMessage()).isEqualTo(MonitoringStatus.OK.getGolemName());
    }

    @Test
    public void mergeOkWithCustomMessageAndOk() {
        Result result1 = new Result(MonitoringStatus.OK, "Everything is ok");
        Result result2 = new Result(MonitoringStatus.OK, MonitoringStatus.OK.getGolemName());

        Result result = DbMonitoringUtils.merge(result1, result2);
        Assertions.assertThat(result.getStatus()).isEqualTo(MonitoringStatus.OK);
        Assertions.assertThat(result.getMessage()).isEqualTo("Everything is ok");
    }

    @Test
    public void mergeOkAndOkWithCustomMessage() {
        Result result1 = new Result(MonitoringStatus.OK, MonitoringStatus.OK.getGolemName());
        Result result2 = new Result(MonitoringStatus.OK, "Everything is ok");

        Result result = DbMonitoringUtils.merge(result1, result2);
        Assertions.assertThat(result.getStatus()).isEqualTo(MonitoringStatus.OK);
        Assertions.assertThat(result.getMessage()).isEqualTo("Everything is ok");
    }

    @Test
    public void mergeWarnAndOk() {
        Result result1 = new Result(MonitoringStatus.WARNING, "Warn!!");
        Result result2 = new Result(MonitoringStatus.OK, MonitoringStatus.OK.getGolemName());

        Result result = DbMonitoringUtils.merge(result1, result2);
        Assertions.assertThat(result.getStatus()).isEqualTo(MonitoringStatus.WARNING);
        Assertions.assertThat(result.getMessage()).isEqualTo("Warn!!");
    }

    @Test
    public void mergeWarnAndOkWithCustomMessage() {
        Result result1 = new Result(MonitoringStatus.WARNING, "Warn!!");
        Result result2 = new Result(MonitoringStatus.OK, "Everything is ok");

        Result result = DbMonitoringUtils.merge(result1, result2);
        Assertions.assertThat(result.getStatus()).isEqualTo(MonitoringStatus.WARNING);
        Assertions.assertThat(result.getMessage()).isEqualTo("Warn!!; Everything is ok");
    }

    @Test
    public void mergeCritAndOk() {
        Result result1 = new Result(MonitoringStatus.CRITICAL, "Crit!!");
        Result result2 = new Result(MonitoringStatus.OK, MonitoringStatus.OK.getGolemName());

        Result result = DbMonitoringUtils.merge(result1, result2);
        Assertions.assertThat(result.getStatus()).isEqualTo(MonitoringStatus.CRITICAL);
        Assertions.assertThat(result.getMessage()).isEqualTo("Crit!!");
    }

    @Test
    public void mergeCritAndOkWithCustomMessage() {
        Result result1 = new Result(MonitoringStatus.CRITICAL, "Crit!!");
        Result result2 = new Result(MonitoringStatus.OK, "Everything is ok");

        Result result = DbMonitoringUtils.merge(result1, result2);
        Assertions.assertThat(result.getStatus()).isEqualTo(MonitoringStatus.CRITICAL);
        Assertions.assertThat(result.getMessage()).isEqualTo("Crit!!; Everything is ok");
    }

    @Test
    public void mergeCritAndWarn() {
        Result result1 = new Result(MonitoringStatus.CRITICAL, "Crit!!");
        Result result2 = new Result(MonitoringStatus.WARNING, "Warn!!");

        Result result = DbMonitoringUtils.merge(result1, result2);
        Assertions.assertThat(result.getStatus()).isEqualTo(MonitoringStatus.CRITICAL);
        Assertions.assertThat(result.getMessage()).isEqualTo("Crit!!; Warn!!");
    }

    @Test
    public void mergeWarnAndCrit() {
        Result result1 = new Result(MonitoringStatus.WARNING, "Warn!!");
        Result result2 = new Result(MonitoringStatus.CRITICAL, "Crit!!");

        Result result = DbMonitoringUtils.merge(result1, result2);
        Assertions.assertThat(result.getStatus()).isEqualTo(MonitoringStatus.CRITICAL);
        Assertions.assertThat(result.getMessage()).isEqualTo("Warn!!; Crit!!");
    }
}
