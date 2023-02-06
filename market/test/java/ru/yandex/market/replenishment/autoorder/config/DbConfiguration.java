package ru.yandex.market.replenishment.autoorder.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.common.postgres.spring.configs.PGCommonConfig;

@Slf4j
@Configuration
@Import({
    PGCommonConfig.class
})
public class DbConfiguration {

    @Value("${autoorder.datasource.schema}")
    private String schema;

    @Primary
    @Bean
    public DataSource dataSource(PGCommonConfig pgCommonConfig) {
        final BasicDataSource dataSource = new BasicDataSource();
        dataSource.setUsername(pgCommonConfig.getUserName());
        dataSource.setPassword(pgCommonConfig.getPassword());
        dataSource.setDriverClassName(pgCommonConfig.getDriverName());
        dataSource.setUrl(pgCommonConfig.getUrl());
        dataSource.setDefaultSchema(schema);

        try (final Connection connection = dataSource.getConnection()) {
            createSchema(connection, schema);
        } catch (SQLException e) {
            log.error("Unable to create schema", e);
        }

        return dataSource;
    }

    private void createSchema(Connection connection, String schemaName) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("CREATE SCHEMA IF NOT EXISTS " + schemaName)) {
            ps.executeUpdate();
        }
    }
}

