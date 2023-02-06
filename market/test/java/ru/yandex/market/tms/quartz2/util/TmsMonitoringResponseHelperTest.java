package ru.yandex.market.tms.quartz2.util;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.tms.quartz2.model.JobMonitoringResult;
import ru.yandex.market.tms.quartz2.model.MonitoringStatus;
import ru.yandex.market.tms.quartz2.model.TmsMonitoringResult;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author otedikova
 */
class TmsMonitoringResponseHelperTest {

    @Test
    void getSuccessResponseString() {
        TmsMonitoringResult monitoringResult = new TmsMonitoringResult(MonitoringStatus.OK,
                "Monitoring results",
                new ArrayList<>());
        String responseString = TmsMonitoringResponseHelper.getResponseString(monitoringResult);
        assertEquals("0;OK", responseString);
    }

    @Test
    void getCritResponseString() {
        ArrayList<JobMonitoringResult> jobResults = new ArrayList<>();
        ArrayList<String> critMessages = new ArrayList<>();
        critMessages.add("Job crit error");
        critMessages.add("job delay time exceeded. Max: 20 seconds, actual 30 seconds");
        jobResults.add(new JobMonitoringResult("job1", MonitoringStatus.CRIT, critMessages));
        List<String> warnMessages = new ArrayList<>();
        warnMessages.add("Job warn message");
        jobResults.add(new JobMonitoringResult("job2", MonitoringStatus.WARN, warnMessages));
        jobResults.add(new JobMonitoringResult("job3", MonitoringStatus.OK, new ArrayList<>()));
        TmsMonitoringResult monitoringResult = new TmsMonitoringResult(MonitoringStatus.CRIT,
                "Monitoring results",
                jobResults);
        String responseString = TmsMonitoringResponseHelper.getResponseString(monitoringResult);
        assertEquals("2;<CRIT> job1 : Job crit error, job delay time exceeded. Max: 20 seconds, actual 30 seconds, " +
                "<WARN> job2 : Job warn message", responseString);
    }

    @Test
    void getNoJobResultsResponseString() {
        TmsMonitoringResult monitoringResult = new TmsMonitoringResult(MonitoringStatus.CRIT,
                "There are no jobs configured for this component.",
                new ArrayList<>());
        String responseString = TmsMonitoringResponseHelper.getResponseString(monitoringResult);
        assertEquals("2;There are no jobs configured for this component.", responseString);
    }
}
