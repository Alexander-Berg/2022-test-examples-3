package ru.yandex.market.wms.scheduler.dao;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class CleanBackgroundJobStatusDaoTest extends SchedulerIntegrationTest {

    private static final int DEFAULT_SAMPLE_SIZE = 50000;
    private static final int DEFAULT_DAYS_OLD_TO_BE_DELETED = 7;
    private static final int DEFAULT_SELECT_TIMEOUT = 30;
    private static final int DEFAULT_DELETE_TIMEOUT = 3;
    private static final List<Integer> LIST_TO_DELETE = List.of(1, 2, 3);

    @Autowired
    private CleanBackgroundJobStatusDao dao;

    @Test
    @DatabaseSetup(value = "/db/dao/clean/clean-background-job-status/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/db/dao/clean/clean-background-job-status/before.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    void getSerialKeysForDeleteTest() {
        List<Integer> serialKeyIds = dao.getSerialKeys(DEFAULT_SAMPLE_SIZE,
                DEFAULT_DAYS_OLD_TO_BE_DELETED, DEFAULT_SELECT_TIMEOUT);
        Assertions.assertEquals(serialKeyIds, LIST_TO_DELETE);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/clean/clean-background-job-status/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/db/dao/clean/clean-background-job-status/after.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    void getAndDeleteBySerialKeysTest() {
        List<Integer> transportOrderKeyIds = dao.getSerialKeys(DEFAULT_SAMPLE_SIZE,
                DEFAULT_DAYS_OLD_TO_BE_DELETED, DEFAULT_SELECT_TIMEOUT);
        int deleteRowsCount = dao.deleteBySerialKeys(transportOrderKeyIds, DEFAULT_DELETE_TIMEOUT);
        Assertions.assertEquals(3, deleteRowsCount);
    }
}
