package ru.yandex.market.notification.safe.util;

import java.io.Reader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Random;

import org.junit.Test;

import ru.yandex.market.notification.test.util.ClassUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link DbUtils}.
 *
 * @author Vladislav Bauer
 */
public class DbUtilsTest {

    private static final Random RANDOM = new Random();


    @Test
    public void testConstructor() {
        ClassUtils.checkConstructor(DbUtils.class);
    }

    @Test
    public void testWasNull() throws Exception {
        final ResultSet rsFalse = mock(ResultSet.class);
        final ResultSet rsTrue = mock(ResultSet.class);
        when(rsTrue.wasNull()).thenReturn(true);

        assertThat(DbUtils.wasNull(rsFalse, null), equalTo(null));
        assertThat(DbUtils.<Number>wasNull(rsFalse, 1), equalTo(1));
        assertThat(DbUtils.<Number>wasNull(rsFalse, 0), equalTo(0));
        assertThat(DbUtils.<Number>wasNull(rsTrue, 0), equalTo(null));
    }

    @Test
    public void testSetClobNullValue() throws Exception {
        final PreparedStatement statement = createStatement();
        final int index = RANDOM.nextInt();

        DbUtils.setClob(statement, index, null);

        verify(statement).setNull(index, Types.CLOB);
        verifyNoMoreInteractions(statement);
    }

    @Test
    public void testSetClobStringValue() throws Exception {
        final PreparedStatement statement = createStatement();
        final int index = RANDOM.nextInt();
        final byte[] data = {};

        DbUtils.setClob(statement, index, data);

        verify(statement).setString(index, "");
        verifyNoMoreInteractions(statement);
    }

    @Test
    public void testSetClobValue() throws Exception {
        assertThat(DbUtils.MAX_VARCHAR_SIZE, equalTo(4000));

        final PreparedStatement statement = createStatement();
        final int size = DbUtils.MAX_VARCHAR_SIZE + 1;
        final int index = RANDOM.nextInt();
        final byte[] data = new byte[size];

        DbUtils.setClob(statement, index, data);

        verify(statement).setClob(eq(index), any(Reader.class), eq(Long.valueOf(size)));
        verifyNoMoreInteractions(statement);
    }


    private PreparedStatement createStatement() {
        return mock(PreparedStatement.class);
    }

}
