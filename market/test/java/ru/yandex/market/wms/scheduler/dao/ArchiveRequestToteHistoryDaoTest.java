package ru.yandex.market.wms.scheduler.dao;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;

public class ArchiveRequestToteHistoryDaoTest extends SchedulerIntegrationTest {

    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final int DEFAULT_SELECT_TIMEOUT = 10;
    private static final int DEFAULT_DAYS_THRESHOLD = 30;
    private static final List<Long> LIST_TO_ARCHIVE = List.of(1L, 2L, 3L);

    @Autowired
    private ArchiveRequestToteHistoryDao dao;

    @Test
    @DatabaseSetup(value = "/db/dao/archive/request-tote-history/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/db/dao/archive/request-tote-history/before.xml", connection = "wmwhseConnection",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void getRequestToteEventIdsTest() {
        List<Long> ids = dao.getRequestToteEventIds(DEFAULT_BATCH_SIZE, DEFAULT_DAYS_THRESHOLD, DEFAULT_SELECT_TIMEOUT);
        Assertions.assertEquals(LIST_TO_ARCHIVE, ids);
    }
}
