package ru.yandex.market.sberlog_tms.monitoring;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ru.yandex.market.sberlog_tms.SberlogtmsConfig;
import ru.yandex.market.sberlog_tms.lock.LockService;

import java.util.Date;
import java.util.regex.Pattern;

/**
 * @author Strakhov Artem <a href="mailto:dukeartem@yandex-team.ru"></a>
 * @date 19.11.19
 */
@Disabled
@SpringJUnitConfig(SberlogtmsConfig.class)
class UpdateMonitoringDataReportThreadTest {

    @Value("${sberlogtms.scheduled.uploadusertoyt.crittime}")
    long uploadUserToYtCrittime;

    @Test
    public void lockFileNotFound() throws InterruptedException {
        Thread.sleep(1000);
        UpdateMonitoringDataReportThread.setRun(true);
        LockService lockService = Mockito.mock(LockService.class);
        Mockito.when(lockService.checkPath(Mockito.anyString())).thenReturn(false);

        new UpdateMonitoringDataReportThread(lockService, uploadUserToYtCrittime);
        Thread.sleep(1000);
        UpdateMonitoringDataReportThread.setRun(false);

        final Pattern answer_pattern = Pattern.compile("1;lock file.*");

        Assertions.assertTrue(answer_pattern.matcher(UpdateMonitoringDataReportThread.getRefreshTimeStatus()).matches());
    }

    @Test
    public void dataTooOld() throws InterruptedException {
        Thread.sleep(1000);
        UpdateMonitoringDataReportThread.setRun(true);
        LockService lockService = Mockito.mock(LockService.class);
        Mockito.when(lockService.checkPath(Mockito.anyString())).thenReturn(true);

        long lastReportTime = (new Date().getTime() / 1000) - uploadUserToYtCrittime - 1;
        Mockito.when(lockService.getNodeInfo(Mockito.anyString())).thenReturn("localhost: " + lastReportTime);

        new UpdateMonitoringDataReportThread(lockService, uploadUserToYtCrittime);
        Thread.sleep(1000);
        UpdateMonitoringDataReportThread.setRun(false);

        final Pattern answer_pattern = Pattern.compile("2;sberlog users report is too old in YT.*");

        Assertions.assertTrue(answer_pattern.matcher(UpdateMonitoringDataReportThread.getRefreshTimeStatus()).matches());
    }

    @Test
    public void allOk() throws InterruptedException {
        Thread.sleep(1000);
        UpdateMonitoringDataReportThread.setRun(true);
        LockService lockService = Mockito.mock(LockService.class);
        Mockito.when(lockService.checkPath(Mockito.anyString())).thenReturn(true);

        long lastReportTime = (new Date().getTime() / 1000) + uploadUserToYtCrittime - 1;
        Mockito.when(lockService.getNodeInfo(Mockito.anyString())).thenReturn("localhost: " + lastReportTime);

        new UpdateMonitoringDataReportThread(lockService, uploadUserToYtCrittime);
        Thread.sleep(1000);
        UpdateMonitoringDataReportThread.setRun(false);

        final Pattern answer_pattern = Pattern.compile("0;OK");

        Assertions.assertTrue(answer_pattern.matcher(UpdateMonitoringDataReportThread.getRefreshTimeStatus()).matches());
    }

}
