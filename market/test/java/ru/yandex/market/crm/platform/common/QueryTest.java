package ru.yandex.market.crm.platform.common;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class QueryTest {

    private String expected;
    private Query query;

    @Parameters
    public static List<Object[]> data() {
        return asList(new Object[][] {
                {"* FROM [//home/path] LIMIT 10",
                        new UidQuery(false, false).limit(10)},

                {"* FROM [//home/path] WHERE (fact_id >= '10')",
                        new UidQuery(false, false).idBetween("10", null)},

                {"* FROM [//home/path] WHERE (fact_id >= '10' AND fact_id <= '20')",
                        new UidQuery(false, false).idBetween("10", "20")},

                {"* FROM [//home/path] WHERE (fact_id >= '10' AND fact_id <= '20') LIMIT 30",
                        new UidQuery(false, false).idBetween("10", "20").limit(30)},

                {"* FROM [//home/path] WHERE (fact_id >= '10' AND fact_id <= '20') LIMIT 30",
                        new UidQuery(false, false).idBetween("10", "20").limit(30)},

                {"* FROM [//home/path] WHERE (timestamp >= 777 AND timestamp <= 888) AND " +
                        "(fact_id >= '10' AND fact_id <= '20') LIMIT 30",
                        new UidQuery(false, false).between(777L, 888L).idBetween("10", "20").limit(30)},
        });
    }

    public QueryTest(String expected, Query query) {
        this.expected = expected;
        this.query = query;
    }

    @Test
    public void test() {
        assertEquals(expected, query.batch("//home/path").get(0));
    }
}