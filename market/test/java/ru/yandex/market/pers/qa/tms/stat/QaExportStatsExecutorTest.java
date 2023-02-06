package ru.yandex.market.pers.qa.tms.stat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.pers.qa.PersQaTmsTest;

public class QaExportStatsExecutorTest extends PersQaTmsTest {

    @Autowired
    private QaExportStatsExecutor qaExportStatsExecutor;

    @Test
    public void testLogIndexingTimeForLastHour() {
        qaExportStatsExecutor.logIndexingTimeForLastHour();
    }

    @Test
    public void testLogGenerationStats() {
        qaExportStatsExecutor.logGenerationStats();
    }

    @Test
    public void testExportStats() {
        qaExportStatsExecutor.exportStats();
    }

    @Test
    public void testExportCommentStats() {
        qaExportStatsExecutor.exportCommentStats();
    }
}