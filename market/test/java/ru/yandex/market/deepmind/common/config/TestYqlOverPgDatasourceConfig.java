package ru.yandex.market.deepmind.common.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mboc.common.config.YqlConfig;

/**
 * @deprecated YqlOverPg не развивается и должен быть закопан в пользу yql-tests
 * Смотри https://st.yandex-team.ru/DEEPMIND-1961
 */
@Deprecated
@Configuration
@Import({
    TestDeepmindSqlDatasourceConfig.class
})
public class TestYqlOverPgDatasourceConfig extends YqlConfig {
    public static final String YQL_OVER_PG_DATASOURCE = "yqlOverPgDataSource";
    public static final String YQL_OVER_PG_TEMPLATE = "yqlOverPgTemplate";
    public static final String YQL_OVER_PG_NAMED_TEMPLATE = "yqlOverPgNamedTemplate";

    public static final String YQL_OVER_PG_SECONDARY_DATASOURCE = "yqlOverPgSecondaryDataSource";
    public static final String YQL_OVER_PG_SECONDARY_TEMPLATE = "yqlOverPgSecondaryTemplate";
    public static final String YQL_OVER_PG_SECONDARY_NAMED_TEMPLATE = "yqlOverPgSecondaryNamedTempate";

    private final TestDeepmindSqlDatasourceConfig testDeepmindSqlDatasourceConfig;

    public TestYqlOverPgDatasourceConfig(TestDeepmindSqlDatasourceConfig testDeepmindSqlDatasourceConfig) {
        this.testDeepmindSqlDatasourceConfig = testDeepmindSqlDatasourceConfig;
    }

    @Bean(name = YQL_OVER_PG_DATASOURCE)
    public DataSource yqlDataSource() {
        return testDeepmindSqlDatasourceConfig.deepmindDataSource();
    }

    @Bean(name = YQL_OVER_PG_TEMPLATE)
    @Override
    public JdbcTemplate yqlJdbcTemplate() {
        return super.yqlJdbcTemplate();
    }

    @Bean(name = YQL_OVER_PG_NAMED_TEMPLATE)
    @Override
    public NamedParameterJdbcTemplate namedYqlJdbcTemplate() {
        return super.namedYqlJdbcTemplate();
    }

    @Bean(name = YQL_OVER_PG_SECONDARY_DATASOURCE)
    @Override
    public DataSource secondaryYqlDataSource() {
        return yqlDataSource();
    }

    @Bean(name = YQL_OVER_PG_SECONDARY_TEMPLATE)
    @Override
    public JdbcTemplate secondaryYqlJdbcTemplate() {
        return yqlJdbcTemplate();
    }

    @Bean(name = YQL_OVER_PG_SECONDARY_NAMED_TEMPLATE)
    @Override
    public NamedParameterJdbcTemplate secondaryNamedYqlJdbcTemplate() {
        return namedYqlJdbcTemplate();
    }
}
