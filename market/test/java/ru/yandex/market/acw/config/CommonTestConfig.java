package ru.yandex.market.acw.config;

import java.sql.SQLException;

import javax.sql.DataSource;

import com.google.common.collect.ImmutableMap;
import liquibase.integration.spring.SpringLiquibase;
import org.apache.commons.dbcp2.BasicDataSource;
import org.jooq.SQLDialect;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.jooq.tools.LoggerListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import ru.yandex.market.acw.config.db.DAOConfig;
import ru.yandex.market.common.postgres.spring.configs.PGCommonConfig;

@Configuration
@Import({
        DAOConfig.class
})
public class CommonTestConfig {


    private static final String SCHEMA_NAME = "public";
    private static final String CHANGELOG = "classpath:/liquibase/db-changelog.xml";

    @Autowired
    private ApplicationContext ctx;

    @Bean
    DataSource dataSource() {
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl(ctx.getEnvironment().getProperty(PGCommonConfig.SQL_URL));
        ds.setUsername(ctx.getEnvironment().getProperty(PGCommonConfig.SQL_USER_NAME));
        ds.setPassword(ctx.getEnvironment().getProperty(PGCommonConfig.SQL_PASSWORD));
        ds.setFastFailValidation(true);
        try {
            //call for side effect - initializing the connection pool
            ds.getLogWriter();
        } catch (SQLException e) {
            throw new IllegalStateException("Can't initialize dataSource", e);
        }
        return ds;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = {"transactionAwareDataSourceProxy"})
    public DataSource transactionAwareDataSourceProxy(DataSource dataSource) {
        return new TransactionAwareDataSourceProxy(dataSource);
    }

    @Bean(name = "dataSourceConnectionProvider")
    DataSourceConnectionProvider connectionProvider(
            @Qualifier("transactionAwareDataSourceProxy") DataSource jooqDataSource) {
        return new DataSourceConnectionProvider(jooqDataSource);
    }

    @Bean(name = "dataSourceTransactionManager")
    public DataSourceTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    SpringTransactionProvider transactionProvider(
            @Qualifier("dataSourceTransactionManager") DataSourceTransactionManager transactionManager,
            @Qualifier("dataSourceConnectionProvider") DataSourceConnectionProvider connectionProvider) {
        return new SpringTransactionProvider(transactionManager, connectionProvider);
    }

    @Bean
    public SpringLiquibase liquibase(
            @Qualifier("transactionAwareDataSourceProxy") DataSource jooqDataSource) {
        try {
            jooqDataSource.getConnection()
                    .createStatement()
                    .execute("CREATE SCHEMA IF NOT EXISTS " + SCHEMA_NAME + ";");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(jooqDataSource);
        liquibase.setChangeLog(CHANGELOG);
        liquibase.setDefaultSchema(SCHEMA_NAME);
        liquibase.setChangeLogParameters(
                ImmutableMap.of(
                        "database.liquibaseSchemaName", SCHEMA_NAME,
                        "database.defaultSchemaName", SCHEMA_NAME
                )
        );
        return liquibase;
    }

    @Bean(name = {"jooq.config.configuration", "jooq.config.configuration.ro"})
    @Primary
    org.jooq.Configuration configuration(
            @Qualifier("dataSourceConnectionProvider") DataSourceConnectionProvider connectionProvider,
            SpringTransactionProvider springTransactionProvider) {
        DefaultConfiguration config = new DefaultConfiguration();
        config.setSQLDialect(SQLDialect.POSTGRES);
        config.setConnectionProvider(connectionProvider);
        config.setTransactionProvider(springTransactionProvider);
        config.setExecuteListenerProvider(new DefaultExecuteListenerProvider(new LoggerListener()));
        return config;
    }

    @Bean
    SequenceStartRandomizer sequenceRandomizer(SpringLiquibase liquibase,
                                               @Qualifier("jooq.config.configuration")
                                                       org.jooq.Configuration configuration) {
        return new SequenceStartRandomizer(liquibase, configuration);
    }

}
