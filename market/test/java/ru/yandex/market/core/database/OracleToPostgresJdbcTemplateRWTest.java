package ru.yandex.market.core.database;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.support.JdbcUtils;

import static org.mockito.Mockito.inOrder;

class OracleToPostgresJdbcTemplateRWTest extends OracleToPostgresJdbcTemplateTest {
    @Override
    protected boolean areDataSourcesReadOnly() {
        return false;
    }

    @Test
    void shouldHandleAllRemainingRequests() throws SQLException {
        // when
        testSqlSelect();

        // then
        var o = inOrder(experimentDataSource, regularDataSource);
        for (int queries = 0; queries < 6; queries++) {
            JdbcUtils.closeConnection(o.verify(experimentDataSource).getConnection());
            JdbcUtils.closeConnection(o.verify(regularDataSource).getConnection());
        }
    }
}
