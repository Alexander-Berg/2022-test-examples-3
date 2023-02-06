package manual.ru.yandex.market.psku.postprocessor.bazinga.dna.config;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

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
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

public class ManualTestDatabaseConfig {
    @Value("${log_sql:false}")
    private boolean logSQL;
    private static final Logger log = LogManager.getLogger();

    @Value("${market.psku-post-processor.jdbc.driverClassName}")
    private String driverClassName;
    @Value("${market.psku-post-processor.jdbc.url}")
    private String jdbcUrl;
    @Value("${market.psku-post-processor.username}")
    private String username;
    @Value("${market.psku-post-processor.password}")
    private String password;
    @Value("${market.psku-post-processor.min_idle_connection:1}")
    private int minIdleConnection;
    @Value("${market.psku-post-processor.max_idle_connection:16}")
    private int maxIdleConnection;
    @Value("${market.psku-post-processor.max_total_connection:32}")
    private int maxTotalConnection;
    @Value("${postgres.validation_query:SELECT 1}")
    private String validationQuery;
    @Value("${postgres.validation_query_timeout:2}")
    private int validationQueryTimeout;

    @Bean(name = "transactionAwareDataSourceProxy.test")
    TransactionAwareDataSourceProxy transactionAwareDataSourceProxy() {
        DataSource dataSource = createDataSource(driverClassName, jdbcUrl, username, password);
        return new TransactionAwareDataSourceProxy(dataSource);
    }

    @Bean(name = "dataSourceConnectionProvider.test")
    public DataSourceConnectionProvider connectionProvider(
            @Qualifier("transactionAwareDataSourceProxy.test")
            TransactionAwareDataSourceProxy transactionAwareDataSource
    ) {
        return new DataSourceConnectionProvider(transactionAwareDataSource);
    }

    @Bean(name = "jooq.config.configuration.test")
    public org.jooq.Configuration configuration(
            @Qualifier("dataSourceConnectionProvider.test") DataSourceConnectionProvider dataSourceConnectionProvider) {
        DefaultConfiguration config = new DefaultConfiguration();
        initConfiguration(config, dataSourceConnectionProvider);
        return config;
    }

    @Bean(name = "testDataSource")
    DataSource dataSource() {
        return createDataSource(driverClassName, jdbcUrl, username, password);
    }

    @Bean("transactionManager.test")
    DataSourceTransactionManager transactionManager(@Qualifier("testDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
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
