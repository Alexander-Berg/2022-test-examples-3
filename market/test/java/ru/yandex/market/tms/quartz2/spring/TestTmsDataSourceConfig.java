package ru.yandex.market.tms.quartz2.spring;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tms.quartz2.spring.config.TmsDataSourceConfig;

public class TestTmsDataSourceConfig implements TmsDataSourceConfig {

    private final DataSource tmsDataSource;
    private final PlatformTransactionManager tmsTransactionManager;

    TestTmsDataSourceConfig(DataSource tmsDataSource) {
        this.tmsDataSource = tmsDataSource;
        this.tmsTransactionManager = new DataSourceTransactionManager(tmsDataSource);
    }

    @Override
    public DataSource tmsDataSource() {
        return tmsDataSource;
    }

    @Override
    public JdbcTemplate tmsJdbcTemplate() {
        return new JdbcTemplate(tmsDataSource);
    }

    @Override
    public TransactionTemplate tmsTransactionTemplate() {
        return new TransactionTemplate(tmsTransactionManager);
    }

    @Override
    public PlatformTransactionManager tmsTransactionManager() {
        return tmsTransactionManager;
    }
}
