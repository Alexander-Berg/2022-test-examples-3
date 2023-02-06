package ru.yandex.market.wms.scheduler.service.archive;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;
import ru.yandex.market.wms.scheduler.dao.ArchiveExternOrderkeyRegistryDao;
import ru.yandex.market.wms.scheduler.dao.entity.ArchiveExternOrderkeyRegistryParam;
import ru.yandex.market.wms.scheduler.exception.JobExecutionAttemptsOverException;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

class ArchiveExternOrderkeyRegistryServiceTest extends SchedulerIntegrationTest {

    public static final String TWO_RECORDS_ARCHIVED_RESULT_MESSAGE =
            "2 records were moved to archive in externorderkey_registry";

    @Autowired
    private ArchiveExternOrderkeyRegistryService archiveService;

    @Autowired
    private DbConfigService dbConfigService;

    @Test
    @DatabaseSetup(value = "/service/archive/extern-order-key-registry-service/scprd-before.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/archive/extern-order-key-registry-service/scprdarc-before.xml",
            connection = "archiveConnection")
    @ExpectedDatabase(value = "/service/archive/extern-order-key-registry-service/scprd-after.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/service/archive/extern-order-key-registry-service/scprdarc-after.xml",
            connection = "archiveConnection",
            assertionMode = NON_STRICT_UNORDERED)
    void successTwoRecordsArchivedWhenExecute() throws InterruptedException {
        String result = archiveService.execute();
        Assertions.assertEquals(TWO_RECORDS_ARCHIVED_RESULT_MESSAGE, result);
    }

    @Test
    void exceptionWhenAttemptsOver() {
        NamedParameterJdbcTemplate jdbcTemplateMock = Mockito.mock(NamedParameterJdbcTemplate.class);
        Mockito.when(jdbcTemplateMock.query(any(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenThrow(new QueryTimeoutException("Test timeout"));
        ArchiveExternOrderkeyRegistryParam.MAX_RETRY_ATTEMPTS_NUMBER.setValue(1);
        ArchiveExternOrderkeyRegistryDao fakeDao =
                new ArchiveExternOrderkeyRegistryDao(jdbcTemplateMock, jdbcTemplateMock);
        ArchiveExternOrderkeyRegistryService fakeArchiveService =
                new ArchiveExternOrderkeyRegistryService(fakeDao, dbConfigService);
        assertThrows(JobExecutionAttemptsOverException.class, fakeArchiveService::execute);
    }
}
