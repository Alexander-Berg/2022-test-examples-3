package ru.yandex.market.mbi.partner_stat.mvc;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.clickhouse.BalancedClickhouseDataSource;
import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.mbi.partner_stat.FunctionalTest;
import ru.yandex.market.mbi.partner_stat.datasource.clickhouse.ClickHouseHealthConfig;
import ru.yandex.market.mbi.partner_stat.datasource.postgres.PostgresHealthConfig;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Тест на пинг.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class PingControllerTest extends FunctionalTest {

    @Autowired
    private ComplexMonitoring ping;

    @BeforeEach
    void init() throws SQLException {
        // ClickHouse init
        final NamedParameterJdbcTemplate clickHouseJdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        final JdbcTemplate mockJdbcTemplate = Mockito.mock(JdbcTemplate.class);
        final BalancedClickhouseDataSource clickHouseDataSource = Mockito.mock(BalancedClickhouseDataSource.class);

        Mockito.when(clickHouseJdbcTemplate.getJdbcTemplate()).thenReturn(mockJdbcTemplate);
        Mockito.when(mockJdbcTemplate.getDataSource()).thenReturn(clickHouseDataSource);
        Mockito.when(clickHouseDataSource.isWrapperFor(eq(BalancedClickhouseDataSource.class))).thenReturn(true);
        Mockito.when(clickHouseDataSource.unwrap(eq(BalancedClickhouseDataSource.class))).thenReturn(clickHouseDataSource);
        Mockito.when(clickHouseDataSource.actualize()).thenReturn(1);

        final ClickHouseHealthConfig clickHouseHealthConfig = new ClickHouseHealthConfig(ping, clickHouseJdbcTemplate);
        final Runnable runCHCheck = clickHouseHealthConfig::checkPostgresHealth;

        // Postgres init
        final DataSource postgresDataSource = Mockito.mock(DataSource.class);
        final Connection postgresConnection = Mockito.mock(Connection.class);

        Mockito.when(postgresDataSource.getConnection()).thenReturn(postgresConnection);
        Mockito.when(postgresConnection.isValid(anyInt())).thenReturn(true);

        final PostgresHealthConfig postgresHealthConfig = new PostgresHealthConfig(ping, postgresDataSource);
        final Runnable runPGCheck = postgresHealthConfig::checkPostgresHealth;

        // Check
        runCHCheck.run();
        runPGCheck.run();
    }

    @Test
    @DisplayName("Тест на пинг")
    void pingTest() {
        final var expectedResponse = "0;OK";
        final String actualResponse = FunctionalTestHelper.get(baseUrl() + "/ping", String.class).getBody();
        Assertions.assertEquals(expectedResponse, actualResponse);
    }
}
