package ru.yandex.market.feed.log;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.tms.jobs.CleanupOutdatedDataExecutor;
import ru.yandex.market.shop.FunctionalTest;

public class CleanupFeedLogHistoryExecutorTest extends FunctionalTest {
    @Autowired
    @Qualifier("cleanupFeedLogHistoryExecutor")
    CleanupOutdatedDataExecutor executor;

    @Test
    @DbUnitDataSet(before = "CleanupFeedLogHistory.before.csv",
            after = "CleanupFeedLogHistory.after.csv")
    void cleanerTest() {
        executor.doJob(null);
    }
}
