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

public class CleanProcessExecAttrDaoTest extends SchedulerIntegrationTest {

    private static final int DEFAULT_ATTR_BATCH_SIZE = 10000;
    private static final int DEFAULT_SELECT_TIMEOUT = 30;
    private static final int DEFAULT_INSERT_TIMEOUT = 5;
    private static final int DEFAULT_DELETE_TIMEOUT = 3;
    private static final List<Integer> LIST_1 = List.of(1, 2, 3, 4);
    private static final List<String> LIST_2 = List.of("1", "2");
    private static final List<String> LIST_3 = List.of("5", "6");

    @SpyBean
    @Qualifier("scprdi1NamedParameterJdbcTemplate")
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    private CleanProcessExecAttrDao dao;

    @BeforeEach
    void initDao() {
        dao = new CleanProcessExecAttrDao(jdbcTemplate);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/clean/clean-process-exec/exec-attr/before.xml", connection = "scprdi1Connection")
    @ExpectedDatabase(value = "/db/dao/clean/clean-process-exec/exec-attr/before.xml",
            connection = "scprdi1Connection", assertionMode = NON_STRICT_UNORDERED)
    void getProcessExecAttrSerialKeysTest() {
        List<Integer> serialKeyIds = dao.getProcessExecAttrSerialKeys(DEFAULT_ATTR_BATCH_SIZE, DEFAULT_SELECT_TIMEOUT);
        Assertions.assertEquals(LIST_1, serialKeyIds);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/clean/clean-process-exec/exec-attr/before.xml", connection = "scprdi1Connection")
    @ExpectedDatabase(value = "/db/dao/clean/clean-process-exec/exec-attr/after_archive.xml",
            connection = "scprdi1Connection", assertionMode = NON_STRICT_UNORDERED)
    void archiveProcessExecAttrBySerialKeysTest() {
        int deletedCount = dao.archiveProcessExecAttrBySerialKeys(
                LIST_1, DEFAULT_INSERT_TIMEOUT, DEFAULT_DELETE_TIMEOUT);
        Assertions.assertEquals(deletedCount, LIST_1.size());
    }

    @Test
    @DatabaseSetup(value = "/db/dao/clean/clean-process-exec/exec-attr/before_1.xml", connection = "scprdi1Connection")
    @ExpectedDatabase(value = "/db/dao/clean/clean-process-exec/exec-attr/before_1.xml",
            connection = "scprdi1Connection", assertionMode = NON_STRICT_UNORDERED)
    void getProcessExecAttrProcessHandleIdsTest() {
        List<String> processHandleIds = dao.getProcessExecAttrProcessHandleIds(2,
                DEFAULT_SELECT_TIMEOUT);
        Assertions.assertEquals(2, processHandleIds.size());
    }

    @Test
    @DatabaseSetup(value = "/db/dao/clean/clean-process-exec/exec-attr/before.xml", connection = "scprdi1Connection")
    @ExpectedDatabase(value = "/db/dao/clean/clean-process-exec/exec-attr/after_clean.xml",
            connection = "scprdi1Connection", assertionMode = NON_STRICT_UNORDERED)
    void cleanProcessExecAttrTest() {
        int deletedCount = dao.cleanProcessExecAttr(LIST_2, DEFAULT_DELETE_TIMEOUT);
        Assertions.assertEquals(deletedCount, LIST_2.size());
    }

    @Test
    @DatabaseSetup(value = "/db/dao/clean/clean-process-exec/exec-attr/before_2.xml", connection = "scprdi1Connection")
    @ExpectedDatabase(value = "/db/dao/clean/clean-process-exec/exec-attr/after_clean_history.xml",
            connection = "scprdi1Connection", assertionMode = NON_STRICT_UNORDERED)
    void cleanProcessExecAttrHistoryTest() {
        int deletedCount = dao.cleanProcessExecAttrHistory(DEFAULT_DELETE_TIMEOUT);
        Assertions.assertEquals(deletedCount, 2);
    }
}
