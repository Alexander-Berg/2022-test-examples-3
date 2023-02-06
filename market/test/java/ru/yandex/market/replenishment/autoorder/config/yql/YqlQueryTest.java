package ru.yandex.market.replenishment.autoorder.config.yql;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.replenishment.autoorder.repository.postgres.EnvironmentRepository;
import ru.yandex.market.replenishment.autoorder.service.AppendableTableTimestampService;
import ru.yandex.market.replenishment.autoorder.service.DbYtTableWatchLogService;
import ru.yandex.market.replenishment.autoorder.service.TimeService;
import ru.yandex.market.replenishment.autoorder.service.environment.EnvironmentService;
import ru.yandex.market.replenishment.autoorder.service.yt.YtTableService;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.YtResultSetSaver;
import ru.yandex.market.replenishment.autoorder.service.yt.watchable.YtInterWarehouseReplenishmentWatchable;
import ru.yandex.market.replenishment.autoorder.utils.StockStatisticsPathDateExtractor;
import ru.yandex.market.yql_query_service.config.YqlQueryServiceConfig;
import ru.yandex.market.yql_query_service.config.YqlTemplateVariablesConfiguration;
import ru.yandex.market.yql_query_service.service.QryTblService;
import ru.yandex.market.yql_query_service.service.QueryService;
import ru.yandex.market.yql_query_service.service.YqlTemplateVariablesManager;

@RunWith(SpringRunner.class)
@TestPropertySource(properties = {
    "yql.tables-config-yaml=/public/yt-tables.yaml",
    "yql.market.db-profile=test",
    "yql.market.mstat-profile=prestable"})
@ActiveProfiles("unittest")
@ContextConfiguration(classes = {
    QueryService.class,
    YqlTemplateVariablesManager.class,
    YqlQueryServiceConfig.class,
    YqlTemplateVariablesConfiguration.class,
    QryTblService.class
})
@MockBeans({
    @MockBean(classes = {
        AppendableTableTimestampService.class,
        JdbcTemplate.class,
        YtResultSetSaver.class,
        SqlSessionFactory.class,
        SqlSession.class,
        EnvironmentService.class,
        EnvironmentRepository.class,
        TimeService.class,
        StockStatisticsPathDateExtractor.class,
        YtInterWarehouseReplenishmentWatchable.class,
        YtTableService.class,
        DbYtTableWatchLogService.class
    }),
    @MockBean(value = JdbcTemplate.class, name = "yqlJdbcTemplate"),
})
public class YqlQueryTest {
    @Test
    public void initializationTest(){}
}
