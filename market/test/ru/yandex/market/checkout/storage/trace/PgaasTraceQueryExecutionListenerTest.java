package ru.yandex.market.checkout.storage.trace;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.postgresql.jdbc.PgConnection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PgaasTraceQueryExecutionListenerTest {

    @CsvSource(delimiter = ';', value = {
            "jdbc:postgresql://localhost/postgres; localhost",
            "jdbc:postgresql://localhost:5432/postgres; localhost:5432",
            "jdbc:postgresql://host1:5431/postgres?param=sd; host1:5431"
    })
    @ParameterizedTest
    public void testSingleHostUrl(String url, String expectedHostname) throws SQLException {
        var statement = mockStatement(url);
        var listener = getListener(url);
        var hostname = listener.getHostname(statement);
        assertEquals(expectedHostname, hostname);
    }

    @MethodSource(value = "multipleHostCases")
    @ParameterizedTest
    public void testMultipleHostUrl(
            String initialUrl,
            String connectionUrl,
            String expectedHostname) throws SQLException {
        var statement = mockStatement(connectionUrl);
        var listener = getListener(initialUrl);
        var hostname = listener.getHostname(statement);
        assertEquals(expectedHostname, hostname);
    }

    private Statement mockStatement(String url) throws SQLException {
        var statement = mock(Statement.class);
        var connection = mock(PgConnection.class);
        var metadata = mock(DatabaseMetaData.class);
        when(statement.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getURL()).thenReturn(url);
        return statement;
    }

    private PgaasTraceQueryExecutionListener getListener(String url) {
        return new PgaasTraceQueryExecutionListener(url, "market-checkouter");
    }

    private static Stream<Arguments> multipleHostCases() {
        return Stream.of(
                Arguments.of(
                        "jdbc:pgcluster://host1:5431,host2:5432,host3:5433/postgres?targetServerType=master",
                        "jdbc:postgresql://host1:5431/postgres?targetServerType=master",
                        "host1:5431"),
                Arguments.of(
                        "jdbc:pgcluster://host1:5431,host2:5432,host3:5433/postgres?targetServerType=master",
                        "jdbc:postgresql://host3:5433/postgres?targetServerType=master",
                        "host3:5433"),
                Arguments.of(
                        "jdbc:postgresql://host1:5431,host2:5432,host3:5433/postgres?targetServerType=master",
                        "jdbc:postgresql://host1:5431,host2:5432,host3:5433/postgres?targetServerType=master",
                        null),
                Arguments.of(
                        "jdbc:postgresql://host1:5431,host2:5432,host3:5433/postgres?targetServerType=preferSlave",
                        "jdbc:postgresql://host1:5431,host2:5432/postgres?targetServerType=preferSlave",
                        null)
        );
    }
}
