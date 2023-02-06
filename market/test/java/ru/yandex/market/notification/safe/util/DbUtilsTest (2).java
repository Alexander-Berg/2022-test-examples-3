package ru.yandex.market.notification.safe.util;

import java.sql.ResultSet;

import org.junit.jupiter.api.Test;

import ru.yandex.market.notification.test.util.ClassUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link DbUtils}.
 *
 * @author Vladislav Bauer
 */
public class DbUtilsTest {
    @Test
    public void testConstructor() {
        ClassUtils.checkConstructor(DbUtils.class);
    }

    @Test
    public void testWasNull() throws Exception {
        ResultSet rsFalse = mock(ResultSet.class);
        ResultSet rsTrue = mock(ResultSet.class);
        when(rsTrue.wasNull()).thenReturn(true);

        assertThat(DbUtils.wasNull(rsFalse, null), equalTo(null));
        assertThat(DbUtils.<Number>wasNull(rsFalse, 1), equalTo(1));
        assertThat(DbUtils.<Number>wasNull(rsFalse, 0), equalTo(0));
        assertThat(DbUtils.<Number>wasNull(rsTrue, 0), equalTo(null));
    }
}
