package ru.yandex.market.olap2.ytreflect;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import ru.yandex.market.olap2.config.IntegrationTestConfig;
import ru.yandex.market.olap2.load.TestLoadTask;

import java.sql.SQLNonTransientException;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@ActiveProfiles("integration-test")
@SpringBootTest(classes = {IntegrationTestConfig.class})
public class CreateTableLikeYtITest {

    @Autowired
    private CreateTableLikeYt createTableLikeYt;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    private static final TestLoadTask TASK = new TestLoadTask(
        "tstsid1", YtTestTable.TBL);

    @Before
    public void init() {
        // any single column required for table creation
        checkArgument(YtTestTable.COLUMNS.containsKey("datetime"));
        jdbcTemplate.getJdbcOperations().execute("drop table if exists " + TASK.getTable() + " cascade");
    }

    @Test
    public void mustRecreateTable() throws SQLNonTransientException {
        createTableLikeYt.checkTable(TASK);
        assertTrue(tableExists());
        assertTrue(defaultProjectionExists());
        assertTrue(createTableLikeYt.hasProjections(TASK));
        assertThat(getVerticaColumns(), is(YtTestTable.COLUMNS.keySet()));
    }

    @Test
    public void mustAlterTable() throws SQLNonTransientException {
        jdbcTemplate.getJdbcOperations().execute(
            "create table " + TASK.getTable() + " (datetime datetime not null)");
        createTableLikeYt.checkTable(TASK);
        assertTrue(tableExists());
        assertThat(getVerticaColumns(), is(YtTestTable.COLUMNS.keySet()));
    }

    @Test(expected = SQLNonTransientException.class)
    public void mustFailColumnTypesDiffer() throws SQLNonTransientException {
        jdbcTemplate.getJdbcOperations().execute(
            "create table " + TASK.getTable() + " (datetime int not null)");
        createTableLikeYt.checkTable(TASK);
    }

    private boolean tableExists() {
        return jdbcTemplate.queryForObject("select count(*) from tables where table_name = :table_name",
            ImmutableMap.of("table_name", TASK.getTable()),
            Long.class) == 1;
    }

    private boolean defaultProjectionExists() {
        return jdbcTemplate.queryForObject("select count(*) from projections where anchor_table_name = :table_name",
            ImmutableMap.of("table_name", TASK.getTable()),
            Long.class) > 0;
    }

    private Set<String> getVerticaColumns() {
        return new HashSet<>(jdbcTemplate.queryForList("select column_name from columns where table_name = :table_name",
            ImmutableMap.of("table_name", TASK.getTable()),
            String.class));
    }
}
