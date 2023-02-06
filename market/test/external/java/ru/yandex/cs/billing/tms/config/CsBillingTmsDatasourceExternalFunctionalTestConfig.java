package ru.yandex.cs.billing.tms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.cs.billing.util.NamedDataSource;

import javax.sql.DataSource;

@Configuration
public class CsBillingTmsDatasourceExternalFunctionalTestConfig {

    @Value("${oracle.jdbc.maxActive:8}")
    private int oracleJdbcMaxActive;

    @Value("${oracle.jdbc.maxIdle:8}")
    private int oracleJdbsMaxIdle;

    @Value("${oracle.jdbc.minIdle:1}")
    private int oracleJdbcMinIdle;

    @Value("${oracle.jdbc.minEvictableIdleTimeMillis}")
    private int oracleJdbcMinEvictableIdleTimeMillis;

    @Value("${oracle.jdbc.timeBetweenEvictionRunsMillis}")
    private int oracleJdbcTimeBetweenEvictionRunsMillis;

    @Bean
    public DataSource liquibaseDataSource(@Value("${liquibase.cs_billing.cs_billing.jdbc.url}") String jdbcUrl,
                                          @Value("${liquibase.cs_billing.username}") String username,
                                          @Value("${liquibase.cs_billing.password}") String password,
                                          @Value("${liquibase.cs_billing.jdbc.driverClassName}") String driverClassName) {
        NamedDataSource dataSource = new NamedDataSource();
        dataSource.setUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);
        return dataSource;
    }

    @DependsOn({"springLiquibase"})
    @Bean
    public DataSource csBillingDataSource(@Value("${cs_billing.cs_billing.jdbc.driverClassName}") String driverClassName,
                                          @Value("${cs_billing.cs_billing.jdbc.url}") String url,
                                          @Value("${cs_billing.cs_billing.username}") String username,
                                          @Value("${cs_billing.cs_billing.password}") String password) {
        NamedDataSource namedDataSource = abstractOracleDataSource();
        namedDataSource.setDriverClassName(driverClassName);
        namedDataSource.setUrl(url);
        namedDataSource.setUsername(username);
        namedDataSource.setPassword(password);
        return namedDataSource;
    }

    @Bean
    public JdbcTemplate csBillingJdbcTemplate(DataSource csBillingDataSource) {
        return new JdbcTemplate(csBillingDataSource);
    }

    @Bean
    public NamedParameterJdbcTemplate csBillingNamedParameterJdbcTemplate(DataSource csBillingDataSource) {
        return new NamedParameterJdbcTemplate(csBillingDataSource);
    }

    @Bean
    public PlatformTransactionManager csBillingTransactionManager(DataSource csBillingDataSource) {
        return new DataSourceTransactionManager(csBillingDataSource);
    }

    @Bean
    public TransactionTemplate csBillingTransactionTemplate(PlatformTransactionManager csBillingTransactionManager) {
        return new TransactionTemplate(csBillingTransactionManager);
    }

    @DependsOn({"springLiquibase"})
    @Bean
    public DataSource mbiStatsDataSource(@Value("${mb_stat_report.marketstat.jdbc.driverClassName}") String driverClassName,
                                         @Value("${mb_stat_report.billing.jdbc.url}") String url,
                                         @Value("${mb_stat_report.billing.username}") String username,
                                         @Value("${mb_stat_report.billing.password}") String password) {
        NamedDataSource namedDataSource = abstractOracleDataSource();
        namedDataSource.setDriverClassName(driverClassName);
        namedDataSource.setUrl(url);
        namedDataSource.setUsername(username);
        namedDataSource.setPassword(password);
        return namedDataSource;
    }

    @Bean
    public JdbcTemplate mbiStatsJdbcTemplate(DataSource mbiStatsDataSource) {
        return new JdbcTemplate(mbiStatsDataSource);
    }

    @Bean
    public NamedParameterJdbcTemplate mbiStatsNamedParameterJdbcTemplate(DataSource mbiStatsDataSource) {
        return new NamedParameterJdbcTemplate(mbiStatsDataSource);
    }

    @Bean
    public PlatformTransactionManager mstApiClientTransactionManager(DataSource csBillingDataSource) {
        return new DataSourceTransactionManager(csBillingDataSource);
    }

    @Bean
    public TransactionTemplate mbiStatsTransactionTemplate(PlatformTransactionManager csBillingTransactionManager) {
        return new TransactionTemplate(csBillingTransactionManager);
    }

    private NamedDataSource abstractOracleDataSource() {
        NamedDataSource abstractOracleDataSource = new NamedDataSource();
        abstractOracleDataSource.setValidationQuery("select 1 from dual");
        abstractOracleDataSource.setMaxActive(oracleJdbcMaxActive);
        abstractOracleDataSource.setMaxIdle(oracleJdbsMaxIdle);
        abstractOracleDataSource.setMinIdle(oracleJdbcMinIdle);
        abstractOracleDataSource.setMinEvictableIdleTimeMillis(oracleJdbcMinEvictableIdleTimeMillis);
        abstractOracleDataSource.setTimeBetweenEvictionRunsMillis(oracleJdbcTimeBetweenEvictionRunsMillis);

        return abstractOracleDataSource;
    }

}
