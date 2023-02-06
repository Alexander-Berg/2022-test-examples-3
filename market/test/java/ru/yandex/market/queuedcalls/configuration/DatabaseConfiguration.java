package ru.yandex.market.queuedcalls.configuration;

import javax.sql.DataSource;

import com.opentable.db.postgres.embedded.ConnectionInfo;
import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static ru.yandex.market.queuedcalls.AbstractQueuedCallTest.preparedDbExtension;

@Configuration
@EnableTransactionManagement
@PropertySource(value = {
        "classpath:db.properties"
})
public class DatabaseConfiguration {

    @Value("${jdbc.driver}")
    private String jdbcDriverClass;
    @Value("${jdbc.username}")
    private String username;
    @Value("${jdbc.password}")
    private String password;
    @Value("${jdbc.pool.maxConnections:50}")
    private Integer maxConnections;
    @Value("${jdbc.pool.minIdle:10}")
    private Integer minIdleConnections;
    @Value("${jdbc.pool.testWhileIdle:true}")
    private Boolean testConnectionsWhileIdle;
    @Value("${jdbc.pool.validationQuery}")
    private String validationQuery;
    @Value("${jdbc.pool.minEvictableIdleTimeMillis}")
    private Integer minEvictableIdleTimeMillis;
    @Value("${jdbc.pool.timeBetweenEvictionRunsMillis}")
    private Integer timeBetweenEvictionRunsMillis;
    @Value("${jdbc.pool.maxWait}")
    private Integer maxWaitMillis;
    @Value("${jdbc.pool.numTestsPerEvictionRun:-2}")
    private Integer numTestsPerEvictionRun;
    @Value("${jdbc.url}")
    private String url;


    @Bean
    public static ConnectionInfo embeddedPostgres() {
        return preparedDbExtension.getConnectionInfo();
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public BasicDataSource dataSource() {
        BasicDataSource datasource = new BasicDataSource();
        datasource.setDriverClassName(jdbcDriverClass);
        datasource.setUrl(url);
        datasource.setUsername(username);
        datasource.setPassword(password);
        datasource.setMaxTotal(maxConnections);
        datasource.setMinIdle(minIdleConnections);
        datasource.setTestWhileIdle(testConnectionsWhileIdle);
        datasource.setValidationQuery(validationQuery);
        datasource.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
        datasource.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
        datasource.setMaxWaitMillis(maxWaitMillis);
        datasource.setNumTestsPerEvictionRun(numTestsPerEvictionRun);

        return datasource;
    }
}
