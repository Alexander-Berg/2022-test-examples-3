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
    private static final String PARAM_OBJECT = "object";

    private static final long VALUE_LONG = 1L;
    private static final int VALUE_INT = 2;
    private static final String VALUE_STRING = "str";
    private static final boolean VALUE_BOOLEAN = true;
    private static final Timestamp VALUE_TIMESTAMP = Timestamp.from(Instant.now());
    private static final Date VALUE_DATE = new Date(VALUE_TIMESTAMP.getTime());
    private static final Object VALUE_OBJECT = new Object();

    private static final String QUERY_CHECK_TYPES =
        "select * from dual where"
            + " cLong = :" + PARAM_LONG
            + " cInt = :" + PARAM_INT
            + " cString = :" + PARAM_STRING
            + " cBoolean = :" + PARAM_BOOLEAN
            + " cTimestamp = :" + PARAM_TIMESTAMP
            + " cDate = :" + PARAM_DATE
            + " cObject = :" + PARAM_OBJECT;


    @Test
    public void testActions() throws Exception {
        NamedParameterStatement statement = createParameterStatement();

        statement.addBatch();
        statement.execute();
        statement.executeBatch();
        statement.executeQuery();
        statement.executeUpdate();
        statement.close();

        PreparedStatement ps = statement.getStatement();

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
        PreparedStatement statement = createPreparedStatement(null, null, null, null, null, null, null);

        verify(statement, times(7)).setNull(anyInt(), anyInt());

        verifyNoMoreInteractions(statement);
    }

    @Test
    public void testSettersNonNull() throws Exception {
        PreparedStatement statement = createPreparedStatement(
            VALUE_LONG, VALUE_INT, VALUE_STRING, VALUE_BOOLEAN, VALUE_TIMESTAMP, VALUE_DATE, VALUE_OBJECT
        );

        verify(statement).setLong(anyInt(), eq(VALUE_LONG));
        verify(statement).setInt(anyInt(), eq(VALUE_INT));
        verify(statement).setString(anyInt(), any(String.class));
        verify(statement).setBoolean(anyInt(), eq(VALUE_BOOLEAN));
        verify(statement).setTimestamp(anyInt(), eq(VALUE_TIMESTAMP));
        verify(statement).setDate(anyInt(), eq(VALUE_DATE));
        verify(statement).setObject(anyInt(), eq(VALUE_OBJECT));

        verifyNoMoreInteractions(statement);
    }

    @Test(expected = NotificationException.class)
    public void testFactoryMethodNegative() throws Exception {
        Connection connection = mock(Connection.class);
        when(connection.prepareStatement(anyString())).thenThrow(SQLException.class);

        NamedParameterStatement statement = NamedParameterStatement.create(connection, "");
        fail("Should not create statement " + statement);
    }

    @Test
    public void testGetIndexesNegative() throws Exception {
        NamedParameterStatement statement = createParameterStatement();
        StatementConsumer[] setters = {
                st -> st.setLong(PARAM_BAD, VALUE_LONG),
                st -> st.setLong(PARAM_BAD, VALUE_LONG),
                st -> st.setInt(PARAM_BAD, VALUE_INT),
                st -> st.setString(PARAM_BAD, VALUE_STRING),
                st -> st.setBoolean(PARAM_BAD, VALUE_BOOLEAN),
                st -> st.setTimestamp(PARAM_BAD, VALUE_TIMESTAMP),
                st -> st.setDate(PARAM_BAD, VALUE_DATE),
                st -> st.setObject(PARAM_BAD, VALUE_OBJECT),
        };

        for (int index = 0; index < setters.length; index++) {
            StatementConsumer consumer = setters[index];
            try {
                consumer.accept(statement);
                fail("Should not pass this line of code, setter: " + index);
            } catch (IllegalArgumentException ignored) {}
        }
    }


    private NamedParameterStatement createParameterStatement() throws SQLException {
        Connection connection = createConnection();
        return NamedParameterStatement.create(connection, QUERY_CHECK_TYPES);
    }

    private PreparedStatement createPreparedStatement(
        Long valueLong,
        Integer valueInt,
        String valueString,
        Boolean valueBoolean,
        Timestamp valueTimestamp,
        Date valueDate,
        Object valueObject
    ) throws SQLException {
        PreparedStatement statement = createParameterStatement()
            .setLong(PARAM_LONG, valueLong)
            .setInt(PARAM_INT, valueInt)
            .setString(PARAM_STRING, valueString)
            .setBoolean(PARAM_BOOLEAN, valueBoolean)
            .setTimestamp(PARAM_TIMESTAMP, valueTimestamp)
            .setDate(PARAM_DATE, valueDate)
            .setObject(PARAM_OBJECT, valueObject)
            .getStatement();

        assertThat(statement, notNullValue());

        return statement;
    }

    private Connection createConnection() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);

        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(connection.prepareStatement(anyString(), any(int[].class))).thenReturn(statement);

        return connection;
    }


    @FunctionalInterface
    private interface StatementConsumer {

        void accept(NamedParameterStatement statement) throws Exception;

    }

}
