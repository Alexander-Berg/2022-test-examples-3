package ru.yandex.common.framework.pager;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PagerUtilTest {
    @Test
    public void wrapSqlPager() {
        assertThat(PagerUtil.wrapSql(new Pager(0, 5), "table", "c1,c2,c3", null, "c1")).isEqualTo(
                " select c1,c2,c3 from (" +
                        "select c1,c2,c3 from table order by c1" +
                        ") q offset 0 rows fetch next 5 rows only"
        );

        assertThat(PagerUtil.wrapSql(new Pager(3, 10), "table", "c1,c2,c3", null, "c1")).isEqualTo(
                " select c1,c2,c3 from (" +
                        "select c1,c2,c3 from table order by c1" +
                        ") q offset 30 rows fetch next 10 rows only"
        );
    }

    @Test
    public void wrapGenericQueryPager() {
        assertThat(PagerUtil.wrapGenericQuery(new Pager(0, 5), "select * from whatever")).isEqualTo(
                " select * from (select * from whatever) q offset 0 rows fetch next 5 rows only"
        );

        assertThat(PagerUtil.wrapGenericQuery(new Pager(3, 10), "select c1,c2,c3 from whatever")).isEqualTo(
                " select * from (select c1,c2,c3 from whatever) q offset 30 rows fetch next 10 rows only"
        );
    }

    @Test
    public void buildCountSql() {
        assertThat(PagerUtil.buildCountSql("t", "1=2")).isEqualTo("select count(1) from t where 1=2");
    }

    @Test
    public void buildGenericCountSql() {
        assertThat(PagerUtil.buildGenericCountSql("select * from table"))
                .isEqualTo("select count(1) from (select * from table) t");
    }
}
