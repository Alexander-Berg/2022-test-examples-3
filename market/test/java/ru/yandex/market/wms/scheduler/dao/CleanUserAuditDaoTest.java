package ru.yandex.market.wms.scheduler.dao;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class CleanUserAuditDaoTest extends SchedulerIntegrationTest {

    private static final int DEFAULT_BATCH_SIZE = 4;
    private static final int DEFAULT_INACTIVITY_DAYS_THRESHOLD = 2;
    private static final int DEFAULT_SELECTION_TIMEOUT = 20;
    private static final int DEFAULT_DELETION_TIMEOUT = 4;

    private static final List<String> LIST_TO_DELETE = List.of("2", "3", "4");

    @Qualifier("scprdd1NamedParameterJdbcTemplate")
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    private CleanUserAuditDao cleanUserAuditDao;

    @BeforeEach
    void initDao() {
        cleanUserAuditDao = new CleanUserAuditDao(jdbcTemplate);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/clean/clean-user-audit/before.xml", connection = "scprdd1Connection")
    @ExpectedDatabase(value = "/db/dao/clean/clean-user-audit/before.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT_UNORDERED)
    void getUserAuditIdsTest() {
        List<String> auditIds = cleanUserAuditDao.getUserAuditIds(
                DEFAULT_BATCH_SIZE, DEFAULT_INACTIVITY_DAYS_THRESHOLD, DEFAULT_SELECTION_TIMEOUT);
        Assertions.assertEquals(LIST_TO_DELETE, auditIds);
    }
}
