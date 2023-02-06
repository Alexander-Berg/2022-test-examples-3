package ru.yandex.market.gutgin.tms.manual.config;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.ExecuteListenerProvider;
import org.jooq.SQLDialect;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.jooq.impl.ThreadLocalTransactionProvider;
import org.jooq.tools.LoggerListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class ManualProductionDatabaseConfig {

    @Value("${log_sql:false}")
    private boolean logSQL;
    private static final Logger log = LogManager.getLogger();

    @Value("${market.gutgin.jdbc.driverClassName}")
    private String driverClassName;
    @Value("${market.gutgin.jdbc.readUrl}")
    private String jdbcReadUrl;
    @Value("${market.gutgin.username}")
    private String username;
    @Value("${market.gutgin.password}")
    private String password;
    @Value("${market.gutgin.min_idle_connection:1}")
    private int minIdleConnection;
    @Value("${market.gutgin.max_idle_connection:16}")
    private int maxIdleConnection;
    @Value("${market.gutgin.max_total_connection:32}")
    private int maxTotalConnection;
    @Value("${postgres.validation_query:SELECT 1}")
    private String validationQuery;
    @Value("${postgres.validation_query_timeout:2}")
    private int validationQueryTimeout;

    @Bean(name = "roDataSourceConnectionProvider.production")
    public DataSourceConnectionProvider roConnectionProvider(
        @Qualifier("roTransactionAwareDataSourceProxy.production") TransactionAwareDataSourceProxy roTransactionAwareDataSource) {
        return new DataSourceConnectionProvider(roTransactionAwareDataSource);
    }

    @Bean(name = "jooq.config.configuration.ro.production")
    public org.jooq.Configuration roConfiguration(
        @Qualifier("roDataSourceConnectionProvider.production") DataSourceConnectionProvider dataSourceConnectionProvider) {
        DefaultConfiguration config = new DefaultConfiguration();
        initConfiguration(config, dataSourceConnectionProvider);
        return config;
    }

    @Bean(name = "productionDataSource")
    DataSource dataSource() {
        return createDataSource(driverClassName, jdbcReadUrl, username, password);
    }

    @Bean("transactionManager.production")
    DataSourceTransactionManager transactionManager(@Qualifier("productionDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean(name = "roTransactionAwareDataSourceProxy.production")
    TransactionAwareDataSourceProxy roTransactionAwareDataSourceProxy() {
        DataSource dataSource = createDataSource(driverClassName, jdbcReadUrl, username, password);
        return new TransactionAwareDataSourceProxy(dataSource);
    }

    private DataSource createDataSource(String driverClassName, String jdbcUrl, String username, String password) {
        log.debug("Creating datasource with url {} user {}", jdbcUrl, username);
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName(driverClassName);
        ds.setUrl(jdbcUrl);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setMinIdle(minIdleConnection);
        ds.setMaxIdle(maxIdleConnection);
        ds.setMaxTotal(maxTotalConnection);
        ds.setValidationQuery(validationQuery);
        ds.setValidationQueryTimeout(validationQueryTimeout);
        ds.setFastFailValidation(true);
        try {
            //call for side effect - initializing the connection pool
            ds.getLogWriter();
        } catch (SQLException e) {
            throw new IllegalStateException("Can't initialize dataSource", e);
        }
        return ds;
    }
    private void initConfiguration(DefaultConfiguration config,
                                   DataSourceConnectionProvider dataSourceConnectionProvider) {
        config.setSQLDialect(SQLDialect.POSTGRES_9_5);
        config.setConnectionProvider(dataSourceConnectionProvider);
        List<ExecuteListenerProvider> listeners = new ArrayList<>();
        if (logSQL) {
            listeners.add(new DefaultExecuteListenerProvider(new LoggerListener()));
        }
        config.setExecuteListenerProvider(listeners.toArray(new ExecuteListenerProvider[0]));
        config.setTransactionProvider(new ThreadLocalTransactionProvider(dataSourceConnectionProvider));
    }
}
