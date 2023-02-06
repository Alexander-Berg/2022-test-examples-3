package ru.yandex.market.common.test.jdbc;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.PlatformTransactionManager;

import ru.yandex.market.common.postgres.spring.configs.PGCommonConfig;
import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.common.test.util.DirtyContextBeforeClassTestExecutionListener;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
        initializers = PGaaSZonkyInitializer.class,
        classes = {
                PGCommonConfig.class,
                BasePostgresTest.Config.class,
        }
)
@TestExecutionListeners(value = {
        DirtyContextBeforeClassTestExecutionListener.class,
        DependencyInjectionTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        DbUnitTestExecutionListener.class,
})
public abstract class BasePostgresTest {

    static {
        // to disable postgres reuse in PGaaSZonkyInitializer
        System.setProperty("reuse-pg", Boolean.FALSE.toString());
    }

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Configuration
    public static class Config {

        @Autowired
        protected PGCommonConfig pgCommonConfig;

        @Primary
        @Bean
        public DataSource dataSource() {
            BasicDataSource dataSource = new BasicDataSource();
            dataSource.setUsername(pgCommonConfig.getUserName());
            dataSource.setPassword(pgCommonConfig.getPassword());
            dataSource.setDriverClassName(pgCommonConfig.getDriverName());
            dataSource.setUrl(pgCommonConfig.getUrl());
            return dataSource;
        }

        @Bean
        public JdbcTemplate jdbcTemplate() {
            return new JdbcTemplate(dataSource());
        }

        @Bean
        public PlatformTransactionManager platformTransactionManager() {
            return new DataSourceTransactionManager(dataSource());
        }

        @Bean
        public SpringLiquibase springLiquibase() {
            SpringLiquibase liquibase = new SpringLiquibase();
            liquibase.setDataSource(dataSource());
            liquibase.setChangeLog("classpath:liquibase/postgresql.sql");
            return liquibase;
        }
    }
}
