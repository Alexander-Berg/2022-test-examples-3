package ru.yandex.market.supercontroller2;

import org.junit.Test;
import ru.yandex.market.supercontroller.monitoring.ReportContainer.Report;
import ru.yandex.market.supercontroller.monitoring.ReportContainer.Stage;
import ru.yandex.market.supercontroller.monitoring.ReportContainer.Status;
import ru.yandex.market.supercontroller2.processes.util.ProcessResult;

import static junit.framework.Assert.*;


public class ProcessResultTest {
    @Test
    public void testResult() {
        assertTrue(ProcessResult.OK.isValid());
        assertTrue(new ProcessResult(Report.RELOAD_WAS_BROKEN).isValid());
        assertFalse(new ProcessResult(Report.RELOAD_WAS_BROKEN_TWICE).isValid());
    }

    @Test
    public void testCombine() {
        assertEquals(
                new ProcessResult(Status.OK, Stage.WORK, "", false),
                ProcessResult.combine(ProcessResult.OK, ProcessResult.OK));

        assertEquals(
                new ProcessResult(Status.WARNING, Stage.RELOAD, "Reload was broken;", false),
                ProcessResult.combine(ProcessResult.OK, new ProcessResult(Report.RELOAD_WAS_BROKEN)));

        assertEquals(
                new ProcessResult(Status.CRITICAL_ERROR, Stage.RELOAD, "Reload was broken;", false),
                ProcessResult.combine(ProcessResult.OK, new ProcessResult(Report.RELOAD_WAS_BROKEN_TWICE)));

        assertEquals(
                new ProcessResult(Status.WARNING, Stage.POSTPROCESS, "Logs copying was broken;Reload was broken;", false),
                ProcessResult.combine(new ProcessResult(Report.COPY_LOGS_BROKEN), new ProcessResult(Report.RELOAD_WAS_BROKEN)));
    }
}
