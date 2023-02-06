package ru.yandex.market.wms.scheduler.job;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ClickHouseItrnMigrationJobTest extends SchedulerIntegrationTest {

    private static final int BATCH_SIZE_DEFAULT = 1;

    @Mock
    private JobExecutionContext context;

    @SpyBean
    @Qualifier("clickHouseNamedParameterJdbcTemplate")
    @Autowired
    private NamedParameterJdbcTemplate clickHouseJdbcTemplate;

    @SpyBean
    @Autowired
    private ClickHouseItrnMigrationJob clickHouseItrnMigrationJob;

    @SpyBean
    @Autowired
    private DbConfigService dbConfigService;

    @BeforeEach
    void initMocks() {
        Mockito.reset(clickHouseJdbcTemplate, clickHouseItrnMigrationJob);
        when(
                dbConfigService.getConfigAsInteger(
                        eq(ClickHouseItrnMigrationJob.CONFIG_NAME), any()))
                .thenReturn(BATCH_SIZE_DEFAULT);
    }

    @Test
    @SneakyThrows
    @DatabaseSetup(value = "/db/dao/click-house/itrn/beforeScprd.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/db/dao/click-house/itrn/afterClickHouse.xml", connection = "clickHouseConnection",
            assertionMode = NON_STRICT)
    public void execute_When_ClickHouse_Db_Is_Empty() {
        clickHouseItrnMigrationJob.execute(context);
    }

    @Test
    @SneakyThrows
    @DatabaseSetup(value = "/db/dao/click-house/itrn/beforeScprd.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/click-house/itrn/afterClickHouse.xml", connection = "clickHouseConnection")
    @ExpectedDatabase(value = "/db/dao/click-house/itrn/afterClickHouseAll.xml", connection = "clickHouseConnection",
            assertionMode = NON_STRICT)
    public void execute_When_ClickHouse_Db_Not_Empty() {
        clickHouseItrnMigrationJob.execute(context);
    }

    @Test
    @SneakyThrows
    @DatabaseSetup(value = "/db/dao/click-house/itrn/beforeScprd.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/click-house/itrn/afterClickHouseAll.xml", connection = "clickHouseConnection")
    public void execute_When_ClickHouse_Db_Has_All_Data() {
        clickHouseItrnMigrationJob.execute(context);
        verify(clickHouseJdbcTemplate, times(0))
                .batchUpdate(any(String.class), any(MapSqlParameterSource[].class));
    }

    @Test
    @SneakyThrows
    public void execute_When_Exception() {
        when(dbConfigService.getConfigAsInteger(any(String.class), any(Integer.class)))
                .thenThrow(new RuntimeException("some text"));

        Assertions.assertDoesNotThrow(() -> clickHouseItrnMigrationJob.execute(context));
    }
}
