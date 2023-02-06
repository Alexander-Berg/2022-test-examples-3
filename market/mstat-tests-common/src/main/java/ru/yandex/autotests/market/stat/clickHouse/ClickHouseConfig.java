package ru.yandex.autotests.market.stat.clickHouse;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.qatools.properties.PropertyLoader;
import ru.yandex.qatools.properties.annotations.Property;
import ru.yandex.qatools.properties.annotations.Resource;

import javax.sql.DataSource;

/**
 * Created by timofeevb on 27.01.17.
 */
@Configuration
@Resource.Classpath("clickhouse.properties")
public class ClickHouseConfig {

    private String jdbcDriver = "ru.yandex.clickhouse.ClickHouseDriver";

    @Property("clickHouse.host")
    private String jdbcHost = "health-house-testing.market.yandex.net:8123";

    @Property("clickHouse.user")
    private String jdbcUsername = "marketstat";

    @Property("clickHouse.password")
    private String jdbcPassword = "MARKETSTAT";

    @Property("clickHouse.market.database.name")
    private String clickHouseMarketName = "market";

    @Property("clickHouse.dict.database.name")
    private String clickHouseDictName = "dict";

    public ClickHouseConfig() {
        PropertyLoader.populate(this);
    }

    @Bean
    public DataSource marketDataSource() {
        return dataSourceFor(clickHouseMarketName);
    }

    @Bean
    public DataSource dictDataSource() {
        return dataSourceFor(clickHouseDictName);
    }

    @Bean
    public JdbcTemplate marketJdbcTemplate() {
        return new JdbcTemplate(marketDataSource());
    }

    @Bean
    public JdbcTemplate dictJdbcTemplate() {
        return new JdbcTemplate(dictDataSource());
    }

    private String databaseUrlFor(String databaseName) {
        return "jdbc:clickhouse://" + jdbcHost + "/" + databaseName;
    }

    private BasicDataSource dataSourceFor(String databaseName) {
        final BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(jdbcDriver);
        dataSource.setUrl(databaseUrlFor(databaseName));
        dataSource.setUsername(jdbcUsername);
        dataSource.setPassword(jdbcPassword);
        return dataSource;
    }

}
