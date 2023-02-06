package ru.yandex.market.notification.safe.dao.db;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import org.junit.Test;

import ru.yandex.market.notification.exception.NotificationException;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link NamedParameterStatement}.
 *
 * @author Vladislav Bauer
 */
public class NamedParameterStatementTest {

    private static final String PARAM_BAD = "bad";
    private static final String PARAM_LONG = "long";
    private static final String PARAM_INT = "int";
    private static final String PARAM_STRING = "string";
    private static final String PARAM_BOOLEAN = "boolean";
    private static final String PARAM_TIMESTAMP = "timestamp";
    private static final String PARAM_DATE = "date";
    private static final String PARAM_CLOB = "clob";
    private static final String PARAM_OBJECT = "object";

    private static final long VALUE_LONG = 1L;
    private static final int VALUE_INT = 2;
    private static final String VALUE_STRING = "str";
    private static final boolean VALUE_BOOLEAN = true;
    private static final Timestamp VALUE_TIMESTAMP = Timestamp.from(Instant.now());
    private static final Date VALUE_DATE = new Date(VALUE_TIMESTAMP.getTime());
    private static final byte[] VALUE_CLOB = {1, 2};
    private static final Object VALUE_OBJECT = new Object();

    private static final String QUERY_CHECK_TYPES =
        "select * from dual where"
            + " cLong = :" + PARAM_LONG
            + " cInt = :" + PARAM_INT
            + " cString = :" + PARAM_STRING
            + " cBoolean = :" + PARAM_BOOLEAN
            + " cTimestamp = :" + PARAM_TIMESTAMP
            + " cDate = :" + PARAM_DATE
            + " cClob = :" + PARAM_CLOB
            + " cObject = :" + PARAM_OBJECT;


    @Test
    public void testActions() throws Exception {
        final NamedParameterStatement statement = createParameterStatement();

        statement.addBatch();
        statement.execute();
        statement.executeBatch();
        statement.executeQuery();
        statement.executeUpdate();
        statement.close();

        final PreparedStatement ps = statement.getStatement();

        verify(ps, times(1)).addBatch();
        verify(ps, times(1)).execute();
        verify(ps, times(1)).executeBatch();
        verify(ps, times(1)).executeQuery();
        verify(ps, times(1)).executeUpdate();
        verify(ps, times(1)).close();

        verifyNoMoreInteractions(ps);
    }

    @Test
    public void testSettersNull() throws Exception {
        final PreparedStatement statement = createPreparedStatement(null, null, null, null, null, null, null, null);

        verify(statement, times(8)).setNull(anyInt(), anyInt());

        verifyNoMoreInteractions(statement);
    }

    @Test
    public void testSettersNonNull() throws Exception {
        final PreparedStatement statement = createPreparedStatement(
            VALUE_LONG, VALUE_INT, VALUE_STRING, VALUE_BOOLEAN, VALUE_TIMESTAMP, VALUE_DATE, VALUE_CLOB, VALUE_OBJECT
        );

        verify(statement, times(1)).setLong(anyInt(), eq(VALUE_LONG));
        verify(statement, times(1)).setInt(anyInt(), eq(VALUE_INT));
        verify(statement, times(2)).setString(anyInt(), any(String.class));
        verify(statement, times(1)).setBoolean(anyInt(), eq(VALUE_BOOLEAN));
        verify(statement, times(1)).setTimestamp(anyInt(), eq(VALUE_TIMESTAMP));
        verify(statement, times(1)).setDate(anyInt(), eq(VALUE_DATE));
        verify(statement, times(1)).setObject(anyInt(), eq(VALUE_OBJECT));

        verifyNoMoreInteractions(statement);
    }

    @Test(expected = NotificationException.class)
    public void testFactoryMethodNegative() throws Exception {
        final Connection connection = mock(Connection.class);
        when(connection.prepareStatement(anyString())).thenThrow(SQLException.class);

        final NamedParameterStatement statement = NamedParameterStatement.create(connection, "");
        fail("Should not create statement " + statement);
    }

    @Test
    public void testGetIndexesNegative() throws Exception {
        final NamedParameterStatement statement = createParameterStatement();
        final StatementConsumer[] setters = new StatementConsumer[] {
            (st) -> st.setLong(PARAM_BAD, VALUE_LONG),
            (st) -> st.setLong(PARAM_BAD, VALUE_LONG),
            (st) -> st.setInt(PARAM_BAD, VALUE_INT),
            (st) -> st.setString(PARAM_BAD, VALUE_STRING),
            (st) -> st.setBoolean(PARAM_BAD, VALUE_BOOLEAN),
            (st) -> st.setTimestamp(PARAM_BAD, VALUE_TIMESTAMP),
            (st) -> st.setDate(PARAM_BAD, VALUE_DATE),
            (st) -> st.setClob(PARAM_BAD, VALUE_CLOB),
            (st) -> st.setObject(PARAM_BAD, VALUE_OBJECT),
        };

        for (int index = 0; index < setters.length; index++) {
            final StatementConsumer consumer = setters[index];
            try {
                consumer.accept(statement);
                fail("Should not pass this line of code, setter: " + index);
            } catch (final IllegalArgumentException ignored) {}
        }
    }


    private NamedParameterStatement createParameterStatement() throws SQLException {
        final Connection connection = createConnection();
        return NamedParameterStatement.create(connection, QUERY_CHECK_TYPES);
    }

    private PreparedStatement createPreparedStatement(
        final Long valueLong,
        final Integer valueInt,
        final String valueString,
        final Boolean valueBoolean,
        final Timestamp valueTimestamp,
        final Date valueDate,
        final byte[] valueClob,
        final Object valueObject
    ) throws SQLException {
        final PreparedStatement statement = createParameterStatement()
            .setLong(PARAM_LONG, valueLong)
            .setInt(PARAM_INT, valueInt)
            .setString(PARAM_STRING, valueString)
            .setBoolean(PARAM_BOOLEAN, valueBoolean)
            .setTimestamp(PARAM_TIMESTAMP, valueTimestamp)
            .setDate(PARAM_DATE, valueDate)
            .setClob(PARAM_CLOB, valueClob)
            .setObject(PARAM_OBJECT, valueObject)
            .getStatement();

        assertThat(statement, notNullValue());

        return statement;
    }

    private Connection createConnection() throws SQLException {
        final Connection connection = mock(Connection.class);
        final PreparedStatement statement = mock(PreparedStatement.class);

        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(connection.prepareStatement(anyString(), any(int[].class))).thenReturn(statement);

        return connection;
    }


    @FunctionalInterface
    private interface StatementConsumer {

        void accept(NamedParameterStatement statement) throws Exception;

    }

}
