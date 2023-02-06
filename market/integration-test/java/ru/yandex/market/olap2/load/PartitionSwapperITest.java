package ru.yandex.market.olap2.load;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import ru.yandex.market.olap2.config.IntegrationTestConfig;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@ActiveProfiles("integration-test")
@SpringBootTest(classes = {IntegrationTestConfig.class})
public class PartitionSwapperITest {

    @Autowired
    private PartitionSwapper partitionSwapper;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    public void mustSwapHistorical() {
        TestLoadTask task = task(true);

        jdbcTemplate.getJdbcOperations().execute("drop table if exists " + task.getTable() + " cascade");
        jdbcTemplate.getJdbcOperations().execute("create table " + task.getTable() + " " +
            "(datetime datetime not null, col1 varchar(64)) " +
            "partition by extract(year from datetime)*100 + " +
            "extract(month from datetime)");
        for(String value: Arrays.asList(
            "('2018-01-01 15:30:20', 'part1_1')",
            "('2018-01-02 15:30:20', 'part1_2')",
            "('2018-02-01 15:30:20', 'part2_1')")) {
            jdbcTemplate.getJdbcOperations().execute("insert into " + task.getTable() + " " +
                "(datetime, col1) values " + value);
        }

        try {
            jdbcTemplate.getJdbcOperations().execute("create table " + task.getTmpTable() +
                " like " + task.getTable());
            for (String value : Arrays.asList(
                "('2018-01-01 15:30:20', 'part1_1_modified')",
                "('2018-02-01 15:30:20', 'part2_1_modified')")) {
                jdbcTemplate.getJdbcOperations().execute("insert into " + task.getTmpTable() + " " +
                    "(datetime, col1) values " + value);
            }

            partitionSwapper.swap(task);

            assertThat(jdbcTemplate.queryForList("select datetime || col1 as v " +
                    "from " + task.getTable() + " order by v", Collections.emptyMap(), String.class),
                is(Arrays.asList(
                    "2018-01-01 15:30:20part1_1",
                    "2018-01-02 15:30:20part1_2",
                    "2018-02-01 15:30:20part2_1_modified")));
        } finally {
            jdbcTemplate.getJdbcOperations().execute("drop table if exists " + task.getTmpTable());
        }
    }

    @Test
    public void mustSwapDimensional() {
        TestLoadTask task = task(false);
        jdbcTemplate.getJdbcOperations().execute("drop table if exists " + task.getTable());
        jdbcTemplate.getJdbcOperations().execute("create table " + task.getTable() + "(col1 varchar(64))");
        jdbcTemplate.getJdbcOperations().execute("insert into " + task.getTable() + "(col1) values ('part1')");
        try {
            jdbcTemplate.getJdbcOperations().execute("create table " + task.getTmpTable() +
                " like " + task.getTable());
            jdbcTemplate.getJdbcOperations().execute("insert into " + task.getTmpTable() + "(col1) values ('part1_modified')");

            partitionSwapper.swap(task);

            assertThat(jdbcTemplate.queryForList("select col1 from " + task.getTable() + " order by col1",
                Collections.emptyMap(), String.class),
                is(Arrays.asList("part1_modified")));
        } finally {
            jdbcTemplate.getJdbcOperations().execute("drop table if exists " + task.getTmpTable());
        }
    }

    private TestLoadTask task(boolean historical) {
        return new TestLoadTask(
            "eventid1",
            "//some/testyt/path",
            historical ? 201802 : null);
    }
}
