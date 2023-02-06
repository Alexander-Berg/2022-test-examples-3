package ru.yandex.market.wms.scheduler.dao;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

class CleanAuthAuditDaoTest extends SchedulerIntegrationTest {

    private static final int DEFAULT_BATCH_SIZE = 10000;
    private static final int DEFAULT_DAYS = 30;
    private static final int DEFAULT_SELECT_TIMEOUT = 10;
    private static final int DEFAULT_DELETE_TIMEOUT = 3;
    private static final List<Integer> LIST_TO_DELETE = List.of(1, 2, 3);

    @Autowired
    private CleanAuthAuditDao dao;

    @Test
    @DatabaseSetup(value = "/db/dao/clean/clean-auth-audit/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/db/dao/clean/clean-auth-audit/before.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    void getEventIdsForDeleteTest() {
        List<Integer> eventIds = dao.getKeyList(DEFAULT_BATCH_SIZE, DEFAULT_DAYS, DEFAULT_SELECT_TIMEOUT);
        Assertions.assertEquals(LIST_TO_DELETE, eventIds);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/clean/clean-auth-audit/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/db/dao/clean/clean-auth-audit/after.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    void getAndDeleteByEventIdsTest() {
        int deletedRowsCount = dao.deleteByKeyList(LIST_TO_DELETE, DEFAULT_DELETE_TIMEOUT);
        Assertions.assertEquals(LIST_TO_DELETE.size(), deletedRowsCount);
    }
}
