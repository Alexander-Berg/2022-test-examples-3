package ru.yandex.market.pricelabs.tms;

import java.sql.SQLException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import liquibase.sqlgenerator.core.CreateDatabaseChangeLogTableGenerator;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;

import ru.yandex.market.pricelabs.RandomizedTestListener;
import ru.yandex.market.pricelabs.misc.Utils;

@Slf4j
public class PostgresConfigurationForTests {

    @Bean
    public SchemaInitializer postgresForTests(@Autowired BasicDataSource dataSource) {
        return new SchemaInitializerImpl(dataSource);
    }

    @Bean
    @DependsOn("postgresForTests")
    public SpringLiquibase postgresLiquibase(
            @Value("classpath:changelog.xml") String changeLog,
            @Autowired DataSource dataSource) {
        return getLiquibaseImpl(changeLog, dataSource);
    }


    interface SchemaInitializer {
        SchemaInitializer EMPTY = new SchemaInitializer() {
        };
    }

    @RequiredArgsConstructor
    @Slf4j
    static class SchemaInitializerImpl implements SchemaInitializer {

        @NonNull
        private final BasicDataSource dataSource;

        private String defaultSchema;

        @PostConstruct
        public void init() throws SQLException {
            this.defaultSchema = dataSource.getDefaultSchema();
            if (Utils.isNonEmpty(defaultSchema)) {
                log.info("Initializing test schema: {}", defaultSchema);
                try (var conn = dataSource.getConnection()) {
                    try (var statement = conn.createStatement()) {
                        statement.execute("create schema if not exists " + defaultSchema);
                    }
                }
            } else {
                log.info("Test schema is empty, nothing to initialize");
            }
        }

        @PreDestroy
        public void close() {
            if (RandomizedTestListener.REUSE_INSTANCE != null) {
                log.info("Skip dropping reusable schema: {}", defaultSchema);
                return; // ---
            }
            if (Utils.isNonEmpty(defaultSchema)) {
                log.info("Dropping test schema: {}", defaultSchema);

                try (var conn = dataSource.getConnection()) {
                    try (var statement = conn.createStatement()) {
                        statement.execute("drop schema " + defaultSchema + " cascade");
                    }
                } catch (SQLException sql) {
                    throw new RuntimeException("Unable to drop schema " + defaultSchema, sql);
                }
            }
        }
    }

    static SpringLiquibase getLiquibaseImpl(String changeLog, DataSource dataSource) {
        log.info("Configuring Liquibase {} for {}", changeLog, dataSource);
        SqlGeneratorFactory.getInstance().register(new CreateDatabaseChangeLogTableGenerator() {
            @Override
            protected String getFilenameColumnSize() {
                return "1024";
            }

            @Override
            public int getPriority() {
                return super.getPriority() + 1;
            }
        });
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(changeLog);
        return liquibase;
    }
}
