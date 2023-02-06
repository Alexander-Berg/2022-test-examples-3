package ru.yandex.common.util.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;
import ru.yandex.common.util.collections.Cf;

import java.util.List;

import static ru.yandex.common.util.db.DbUtil.formatEscaped;

/**
 * User: jkff
 * Date: 30.03.2009
 * Time: 11:41:04
 */
public class DbUtilTest {
    @Test
    public void testFormatEscaped() {
        assertEquals("select 1 from dual",
                formatEscaped("select 1 from dual"));
        assertEquals("select 1 from dual where 1 = 42",
                formatEscaped("select 1 from dual where 1 = %s", 42));
        assertEquals("select 1 from dual where 1 = 42",
                formatEscaped("select 1 from dual where 1 = %s", "42"));
        assertEquals("select 1 from dual where 1 = 'hello world'",
                formatEscaped("select 1 from dual where 1 = '%s'", "hello world"));
        assertEquals("select 1 from dual where 1 = 'hello, ye good ol'' o''world'",
                formatEscaped("select 1 from dual where 1 = '%s'", "hello, ye good ol' o'world"));
        assertEquals("select 1 from dual " +
                     "where 1 = 'hello, ye good ol'' o''world' " +
                     "  and 2 = 'hello again'" +
                     "  and 3 = 'o precious o''worldy'" +
                     "  and 42 = 42",
                formatEscaped(
                     "select 1 from dual " +
                     "where 1 = '%s' " +
                     "  and 2 = '%s'" +
                     "  and 3 = '%s'" +
                     "  and 42 = 42",
                     "hello, ye good ol' o'world", "hello again", "o precious o'worldy"));

        try {
            formatEscaped("select 1 from dual where 1=%s or 2=%s or 3=%s or 4=%s or probably 5=%s", 1,2,3,4);

            fail();
        } catch(IllegalArgumentException ignore) {}


        try {
            formatEscaped("select 1 from dual where 1=1", 1,2,3,4);

            fail();
        } catch(IllegalArgumentException ignore) {}
    }

    @Test
    public void testQuotes() throws Exception {
        assertEquals("'id1'", DbUtil.QUOTES.apply("id1"));
        assertEquals("'don''t use single quotes!'", DbUtil.QUOTES.apply("don't use single quotes!"));
        assertEquals(null, DbUtil.QUOTES.apply(null));
        assertEquals("''", DbUtil.QUOTES.apply(""));
    }

    @Test
    public void testGetInSection() throws Exception {
        final List<Integer> list = Cf.list(1, 2, 3);
        assertEquals("(1,2,3)", DbUtil.getInSection(list));
    }
}
