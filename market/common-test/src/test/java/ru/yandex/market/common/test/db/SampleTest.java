package ru.yandex.market.common.test.db;

import org.dbunit.database.DatabaseConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.common.test.jdbc.H2SysdateSqlTransformer;
import ru.yandex.market.common.test.jdbc.InstrumentedDataSource;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SampleTestConfig.class)
@DbUnitDataSet(nonTruncatedTables = "MY.DICT")
@DbUnitDataBaseConfig({
        @DbUnitDataBaseConfig.Entry(
                name = DatabaseConfig.PROPERTY_TABLE_TYPE,
                value = "TABLE, VIEW"
        )
})
public class SampleTest extends DbUnitTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    DataSource dataSource;

    @Test
    @DbUnitDataSet(before = "testSample.before.csv", after = "testSample.after.csv")
    public void testSample() {
        jdbcTemplate.update(
                "delete from my.test where id in " +
                        " (select value(t) from table (CAST(? as shops_web.t_number_tbl)) t)",
                (Supplier<Long[]>) () -> new Long[]{1L, 2L}
        );
    }

    @Test
    @DbUnitDataSet(after = "nonTruncatedSample.after.csv")
    public void nonTruncatedSample() {
    }

    @Test
    @DbUnitDataSet
    public void testDdd() {
        final Instant instant = Instant.now()
                .truncatedTo(ChronoUnit.SECONDS)
                .minus(1, ChronoUnit.DAYS);

        ((InstrumentedDataSource) dataSource).withExtraTransformer(new H2SysdateSqlTransformer(instant),
                () -> jdbcTemplate.update("insert into my.dt_table (id, dt) values (1, sysdate)"));

        final Timestamp dt = jdbcTemplate.queryForObject("select dt from my.dt_table", Timestamp.class);

        Assert.assertEquals(instant, dt.toInstant());
    }

    @Test
    @DbUnitDataSet(before = "testSample.view.before.csv", after = "testSample.view.after.csv")
    public void testViewInAfter() {
    }
}
