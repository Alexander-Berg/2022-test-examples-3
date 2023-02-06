package ru.yandex.market.pers.tms.timer.export.abo;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.pers.tms.export.abo.ModerationExportUidPreprocessor;
import ru.yandex.market.pers.tms.quartz.MonitorableVerboseExecutor;

public class ExportModerationMonitoringExecutorTest extends MockedPersTmsTest {
    @Autowired
    @Qualifier("exportModerationMonitoringExecutor")
    private MonitorableVerboseExecutor executor;

    @Autowired
    private ModerationExportUidPreprocessor preprocessor;

    private long moderatorId = 1L;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void testNothingToModerate() throws Exception {
        executor.runTmsJob();
    }

    @Test
    public void testFailure() throws Exception {
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("Moderators with unknown Staff UID: " + List.of(moderatorId).toString());

        addSomethingModerated(moderatorId);

        // fire monitoring because of absent mapping
        executor.runTmsJob();
    }

    @Test
    public void testSuccess() throws Exception {
        addSomethingModerated(moderatorId);

        // add moderator mapping
        pgJdbcTemplate.update("insert into moderator_id_mapping values (?, ?)", moderatorId, moderatorId);

        executor.runTmsJob();
    }

    private void addSomethingModerated(long moderatorId) {
        pgJdbcTemplate.update("insert into grade_complaint " +
            "(moderator_id, mod_time, state, type, reason_id, source_id, id) " +
            "values (?, now() - interval '1' day, 1, 1, 1, 1, 1)", moderatorId);
    }
}
