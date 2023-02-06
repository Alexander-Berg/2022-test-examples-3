package ru.yandex.market.supercontroller.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class SqlResourceTest {
    @Test
    public void testLoad() throws Exception {
        SqlResource sqls = new SqlResource(
            getClass().getResourceAsStream("/test.sql")
        );

        assertEquals("select * from test1 ", sqls.get("test1"));
        assertEquals("select * from test2 ", sqls.get("test2"));
        assertEquals("select * from test3 ", sqls.get("test3"));

        assertEquals("select * from test1 ", sqls.get("test_params1", "param1", "test1"));
        //select ${p1}, ${p1}, ${p3}, ${p2} from ${p1}, $${p2} order by ${p2}}${p3}
        assertEquals("select a, a, c, b from a, $b order by b}c ",
            sqls.get("test_params2", "p1", "a", "p2", "b", "p3", "c", "p4", "d"));
    }
}
