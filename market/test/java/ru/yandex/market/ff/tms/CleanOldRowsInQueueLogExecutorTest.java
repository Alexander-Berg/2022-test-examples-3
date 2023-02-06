package ru.yandex.market.ff.tms;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.service.DbQueueLogService;
import ru.yandex.market.ff.util.query.count.JpaQueriesCount;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

public class CleanOldRowsInQueueLogExecutorTest extends IntegrationTest {

    @Autowired
    private DbQueueLogService dbQueueLogService;

    private CleanOldRowsInQueueLogExecutor cleanOldRowsInQueueLogExecutor;

    @BeforeEach
    public void init() {
        cleanOldRowsInQueueLogExecutor = new CleanOldRowsInQueueLogExecutor(dbQueueLogService);
    }

    @Test
    @DatabaseSetup("classpath:tms/clean-old-rows-in-queue-log/before-clean.xml")
    @ExpectedDatabase(value = "classpath:tms/clean-old-rows-in-queue-log/after-clean.xml", assertionMode = NON_STRICT)
    @JpaQueriesCount(5)
    public void onlyCorrectRowsWereDeleted() {
        cleanOldRowsInQueueLogExecutor.doJob(null);
    }

    @Test
    @DatabaseSetup("classpath:tms/clean-old-rows-in-queue-log/before-no-clean.xml")
    @ExpectedDatabase(value = "classpath:tms/clean-old-rows-in-queue-log/before-no-clean.xml",
            assertionMode = NON_STRICT)
    public void nothingWasDeletedIfShouldNot() {
        cleanOldRowsInQueueLogExecutor.doJob(null);
    }
}
