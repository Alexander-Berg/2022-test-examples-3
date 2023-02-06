package ru.yandex.market.wms.scheduler.dao;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class CleanProcessExecDaoTest extends SchedulerIntegrationTest {

    private static final int DEFAULT_ATTR_BATCH_SIZE = 10000;
    private static final int DEFAULT_DELETE_INACTIVITY_DAYS_THRESHOLD = 7;
    private static final int DEFAULT_SELECT_TIMEOUT = 30;
    private static final int DEFAULT_INSERT_TIMEOUT = 5;
    private static final int DEFAULT_DELETE_TIMEOUT = 3;
    private static final List<Integer> LIST_1 = List.of(1, 2, 3);

    @SpyBean
    @Qualifier("scprdi1NamedParameterJdbcTemplate")
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    private CleanProcessExecDao dao;

    @BeforeEach
    void initDao() {
        dao = new CleanProcessExecDao(jdbcTemplate);
    }

    @DatabaseSetup(value = "/db/dao/clean/clean-process-exec/exec/before.xml", connection = "scprdi1Connection")
    @Test
    void getProcessExecArchiveSerialKeysTest() {
        List<Integer> serialKeyIds = dao.getProcessExecArchiveSerialKeys(DEFAULT_ATTR_BATCH_SIZE,
                DEFAULT_DELETE_INACTIVITY_DAYS_THRESHOLD, DEFAULT_SELECT_TIMEOUT);
        Assertions.assertEquals(LIST_1, serialKeyIds);
    }

    @DatabaseSetup(value = "/db/dao/clean/clean-process-exec/exec/before.xml", connection = "scprdi1Connection")
    @ExpectedDatabase(value = "/db/dao/clean/clean-process-exec/exec/after_archive.xml",
            connection = "scprdi1Connection", assertionMode = NON_STRICT_UNORDERED)
    @Test
    void archiveProcessExecBySerialKeys() {
        int deletedCount = dao.archiveProcessExecBySerialKeys(LIST_1, DEFAULT_INSERT_TIMEOUT, DEFAULT_DELETE_TIMEOUT);
        Assertions.assertEquals(deletedCount, LIST_1.size());
    }

    @DatabaseSetup(value = "/db/dao/clean/clean-process-exec/exec/before_history.xml", connection = "scprdi1Connection")
    @ExpectedDatabase(value = "/db/dao/clean/clean-process-exec/exec/after_history.xml",
            connection = "scprdi1Connection", assertionMode = NON_STRICT_UNORDERED)
    @Test
    void deleteProcessExecHistoryByDaysThresholdTest() {
        int deletedCount = dao.deleteProcessExecHistoryByDaysThreshold(DEFAULT_DELETE_INACTIVITY_DAYS_THRESHOLD,
                DEFAULT_DELETE_TIMEOUT);
        Assertions.assertEquals(deletedCount, 3);
    }
}
