package ru.yandex.market.core.database;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.verifyNoInteractions;

class OracleToPostgresJdbcTemplateROTest extends OracleToPostgresJdbcTemplateTest {
    @Override
    protected boolean areDataSourcesReadOnly() {
        return true;
    }

    @Test
    void shouldHandleAllRemainingRequests() {
        // when
        testSqlSelect();

        // then
        verifyNoInteractions(regularDataSource);
    }
}
