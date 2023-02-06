package ru.yandex.market.mbi.partner_stat.config;

import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.clickhouse.BalancedClickhouseDataSource;
import ru.yandex.clickhouse.settings.ClickHouseProperties;
import ru.yandex.market.mbi.partner_stat.repository.distribution.DistributionClickHouseDao;
import ru.yandex.market.mbi.partner_stat.repository.expenses.ExpenseStatClickHouseDao;
import ru.yandex.market.mbi.partner_stat.repository.stat.StatClickHouseDao;
import ru.yandex.market.mbi.partner_stat.repository.stat.StatDictionaryClickHouseDao;
import ru.yandex.market.request.datasource.trace.DataSourceTraceUtil;
import ru.yandex.market.request.trace.Module;

@Profile({"clickHouseTest", "functionalTest"})
@Configuration
public class ClickHouseTestConfig {
    public static final String DATA_SOURCE = "clickHouseDataSource";

    public String getUrl() {
        String port = Optional.ofNullable(System.getenv("RECIPE_CLICKHOUSE_HTTP_PORT")).orElse("8123");
        return "jdbc:clickhouse://localhost:" + port;
    }

    public ClickHouseProperties getClickHouseProperties() {
        ClickHouseProperties properties = new ClickHouseProperties();
        properties.setDatabase("mbi");
        properties.setSocketTimeout(30000);
        return properties;
    }

    @Bean(name = DATA_SOURCE)
    public DataSource clickHouseDataSource() {
        BalancedClickhouseDataSource dataSource =
                new BalancedClickhouseDataSource(
                        getUrl(),
                        getClickHouseProperties()
                );
        return DataSourceTraceUtil.wrap(dataSource, Module.CLICKHOUSE);
    }

    @Bean
    public NamedParameterJdbcTemplate clickHouseJdbcTemplate() {
        return new NamedParameterJdbcTemplate(clickHouseDataSource());
    }

    @Bean
    public DistributionClickHouseDao distributionClickHouseDao(NamedParameterJdbcTemplate clickHouseJdbcTemplate) {
        return new DistributionClickHouseDao(clickHouseJdbcTemplate);
    }

    @Bean
    ExpenseStatClickHouseDao expenseStatClickHouseDao(NamedParameterJdbcTemplate clickHouseJdbcTemplate) {
        return new ExpenseStatClickHouseDao(clickHouseJdbcTemplate, true);
    }

    @Bean
    StatClickHouseDao statClickHouseDao(NamedParameterJdbcTemplate clickHouseJdbcTemplate) {
        return new StatClickHouseDao(clickHouseJdbcTemplate, true);
    }

    @Bean
    StatDictionaryClickHouseDao statDictionaryClickHouseDao(NamedParameterJdbcTemplate clickHouseJdbcTemplate) {
        return new StatDictionaryClickHouseDao(clickHouseJdbcTemplate, true);
    }
}
