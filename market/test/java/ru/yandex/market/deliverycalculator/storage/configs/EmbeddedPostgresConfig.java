package ru.yandex.market.deliverycalculator.storage.configs;

import java.io.File;
import java.nio.file.Files;
import java.util.Properties;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import ru.yandex.market.common.postgres.test.PGConfigBuilder;
import ru.yandex.market.common.postgres.test.PGEmbeddedDatabase;
import ru.yandex.market.common.postgres.test.PGEmbeddedDatasource;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;

/**
 * Конфигурация, используемая в тестах, для запуска инстанса embedded PG.
 */
@Configuration
public class EmbeddedPostgresConfig {
    private static final String POSTGRESQL_VERSION = "10.5-1";
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "root";
    private static final String DB_SCHEMA = "public";

    @Bean
    public PostgresConfig postgresConfig() throws Exception {
        return new PGConfigBuilder()
                .setVersion(POSTGRESQL_VERSION)
                .setUser(DB_USERNAME)
                .setPassword(DB_PASSWORD)
                .build();
    }

    @Bean
    public PGEmbeddedDatabase pgEmbeddedDatabase(PostgresConfig postgresConfig) {
        boolean useUserHome = Files.isWritable(PGEmbeddedDatabase.USER_HOME.asFile().toPath());

        // Пытаемся использовать домашнюю директорию пользователя в качестве хранилища дистрибутива Postgres
        return useUserHome
                ? new PGEmbeddedDatabase(postgresConfig, PGEmbeddedDatabase.USER_HOME)
                : new PGEmbeddedDatabase(postgresConfig, new File(StringUtils.EMPTY).getAbsolutePath());
    }

    /**
     * Данный вид datasource'a создает при необходимости схему при первом подключении к БД.
     */
    @Bean(name = {"postgresDataSource"})
    @DependsOn("pgEmbeddedDatabase")
    public PGEmbeddedDatasource postgresDataSource(PostgresConfig postgresConfig) {
        Properties properties = new Properties();
        properties.setProperty("stringtype", "unspecified");
        return new PGEmbeddedDatasource(postgresConfig, DB_SCHEMA, properties);
    }

    /**
     * Создаем основной datasource только после накатки Liquibase патчей.
     */
    @Bean(name = {"primaryDataSource", "dataSource", "tmsDataSource"})
    @DependsOn("liquibase")
    public DataSource dataSource(PGEmbeddedDatasource postgresDataSource) {
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setUrl(postgresDataSource.getUrl());
        basicDataSource.setUsername(DB_USERNAME);
        basicDataSource.setPassword(DB_PASSWORD);
        basicDataSource.setDefaultSchema(DB_SCHEMA);
        basicDataSource.addConnectionProperty("stringtype", "unspecified");
        basicDataSource.setValidationQuery("select 1");
        return basicDataSource;
    }

    /**
     * Запускаем накатку Liquibase патчей сразу после создания datasource'a, до того как начнут отрабатывать
     * init-методы в различных Spring beans.
     */
    @Bean
    @DependsOn("postgresDataSource")
    public SpringLiquibase liquibase(DataSource postgresDataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(postgresDataSource);
        liquibase.setChangeLog("classpath:sql/changelog.xml");
        liquibase.setDropFirst(true);
        return liquibase;
    }

}
