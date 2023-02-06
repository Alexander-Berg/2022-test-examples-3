package ru.yandex.market.tms.quartz2.model;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TmsMonitoringResultTest {

    @Test
    void testSuccessText() {
        TmsMonitoringResult monitoringResult = new TmsMonitoringResult(MonitoringStatus.OK,
                "Monitoring results",
                new ArrayList<>());
        assertEquals("0;OK", monitoringResult.getJugglerMessage());
        assertEquals("OK", monitoringResult.getTotalMessage());
    }

    @Test
    void testCritText() {
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
                "Monitoring results", jobResults);
        assertEquals("2;<CRIT> job1 : Job crit error, job delay time exceeded. Max: 20 seconds, actual 30 seconds, " +
                "<WARN> job2 : Job warn message", monitoringResult.getJugglerMessage());
        assertEquals("<CRIT> job1 : Job crit error, job delay time exceeded. Max: 20 seconds, actual 30 seconds, " +
                "<WARN> job2 : Job warn message", monitoringResult.getTotalMessage());
    }

    @Test
    void testNoJobResultsText() {
        TmsMonitoringResult monitoringResult = new TmsMonitoringResult(MonitoringStatus.CRIT,
                "There are no jobs configured for this component.",
                new ArrayList<>());
        assertEquals("2;There are no jobs configured for this component.",
                monitoringResult.getJugglerMessage());
        assertEquals("There are no jobs configured for this component.",
                monitoringResult.getTotalMessage());
    }

}
