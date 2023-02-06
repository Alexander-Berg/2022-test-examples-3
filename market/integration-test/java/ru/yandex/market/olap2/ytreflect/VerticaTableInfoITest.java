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
import ru.yandex.market.olap2.load.LoadTask;
import ru.yandex.market.olap2.load.TestLoadTask;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@ActiveProfiles("integration-test")
@SpringBootTest(classes = {IntegrationTestConfig.class})
public class VerticaTableInfoITest {
    private static final LoadTask TASK = new TestLoadTask("eid1", "//sometest/vtiit/tbl");

    @Autowired
    private VerticaTableInfo verticaTableInfo;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;


    @Before
    public void init() {
        jdbcTemplate.getJdbcOperations().execute(
            "drop table if exists " + TASK.getTable() + " cascade");
    }

    @Test
    public void testMustBePartitioned() throws Exception {
        jdbcTemplate.getJdbcOperations().execute(
            "create table " + TASK.getTable() + " " +
                "(a int not null) partition by a");
        assertTrue(verticaTableInfo.isPartitioned(TASK));
    }

    @Test
    public void testMustBeNotPartitioned() throws Exception {
        jdbcTemplate.getJdbcOperations().execute(
            "create table " + TASK.getTable() + " " +
                "(a int not null)");
        assertTrue(!verticaTableInfo.isPartitioned(TASK));
    }

    @Test
    public void getPKs() throws Exception {
        jdbcTemplate.getJdbcOperations().execute(
            "create table " + TASK.getTable() + " " +
                "(a int not null, b int not null, primary key (a, b))");
        assertThat(verticaTableInfo.getPKs(TASK), is(Arrays.asList("a", "b")));
    }

    @Test
    public void getEmptyPKs() throws Exception {
        jdbcTemplate.getJdbcOperations().execute(
            "create table " + TASK.getTable() + " " +
                "(a int not null, b int not null)");
        assertThat(verticaTableInfo.getPKs(TASK), is(Collections.emptyList()));
    }

    @Test
    public void getColumns() throws Exception {
        jdbcTemplate.getJdbcOperations().execute(
            "create table " + TASK.getTable() + " " +
                "(a int not null, \"b c d\" varchar(28) not null)");
        assertThat(verticaTableInfo.getColumns(TASK), is(ImmutableMap.of(
            "a", "int",
            "b c d", "varchar"
        )));

    }

    @Test
    public void stripSize() throws Exception {
        assertThat(VerticaTableInfo.stripSize("varchar(123)"), is("varchar"));
        assertThat(VerticaTableInfo.stripSize("varchar (123)"), is("varchar"));
        assertThat(VerticaTableInfo.stripSize("varchar"), is("varchar"));
    }

}
