package ru.yandex.market.olap2.load;

import com.google.common.base.Joiner;
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
import ru.yandex.market.olap2.util.SleepUtil;

import java.sql.SQLNonTransientException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@ActiveProfiles("integration-test")
@SpringBootTest(classes = {IntegrationTestConfig.class})
public class DirectoryLoadTaskDispatcherITest {
    private static final String YT_PATH = "//some/testyttable/path01";
    private static final String TABLE_NAME = "testyttable__path01";
    private static final int REVISION = 12;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private DirectoryLoadTaskDispatcher directoryLoadTaskDispatcher;

    private LoadTask task;

    @Before
    public void init() {
        List<String> manuallyLoadedTableNames = jdbcTemplate.queryForList("select distinct table_name from manual_loads",
            Collections.emptyMap(),
            String.class);
        if(manuallyLoadedTableNames.size() > 0) {
            jdbcTemplate.getJdbcOperations().execute("drop table if exists " +
                Joiner.on(',').join(manuallyLoadedTableNames) + " cascade");
        }
        jdbcTemplate.getJdbcOperations().execute("truncate table manual_loads");
        this.task = directoryLoadTaskDispatcher.createTask(YT_PATH, 12);
    }

    @Test
    public void testDeleteAbsentInYtFromVertica() {
        String tmpTblName = "manual__test_DeleteAbsentInYtFromVertica_" +
            ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
        try {
            jdbcTemplate.getJdbcOperations().execute("create table " + tmpTblName + " (col1 int not null)");
            jdbcTemplate.getJdbcOperations().execute("insert into manual_loads (yt_path, table_name) values " +
                "('//some/testDeleteAbsentInYtFromVertica/ytpath', '" +tmpTblName+ "')");
            directoryLoadTaskDispatcher.deleteAbsentInYtFromVertica();
            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from tables where table_name = :table_name",
                    ImmutableMap.of("table_name", tmpTblName),
                    Integer.class
                ), is(0));
        } finally {
            jdbcTemplate.getJdbcOperations().execute(
                "drop table if exists " + tmpTblName + " cascade");
        }
    }

    @Test(timeout = 60_000)
    public void testReloadFromDir() throws Exception {
        Map<String, Long> ytTables = directoryLoadTaskDispatcher.listYtTables();
        directoryLoadTaskDispatcher.reloadFromDir();

        // ugly wait for task finish
        while(true) {
            SleepUtil.sleep(500);
            long loads = jdbcTemplate.queryForObject(
                "select count(*) from manual_loads", Collections.emptyMap(), Integer.class);

            if(loads >= ytTables.size()) {
                break;
            }
        }

        try {
            assertThat(jdbcTemplate.queryForObject(
                "select count(*) from manual_loads", Collections.emptyMap(), Integer.class),
                is(ytTables.size()));
            jdbcTemplate.query("select yt_path, table_name from manual_loads where loaded = true",
                Collections.emptyMap(),
                rs -> {
                    assertTrue(ytTables.containsKey(rs.getString("yt_path")));
                    assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from tables where table_name = :table_name",
                        ImmutableMap.of("table_name", rs.getString("table_name")), Integer.class),
                        is(1));
                });
        } finally {
            jdbcTemplate.query(
                "select table_name from tables where table_name like '" + LoadTask.TMP_PREFFIX + "%'",
                Collections.emptyMap(),
                rs -> {
                    jdbcTemplate.getJdbcOperations().execute(
                        "drop table if exists " + rs.getString("table_name") + " cascade");
                });
        }
    }

    @Test
    public void testListYtTables() {
        Map<String, Long> ytTables = directoryLoadTaskDispatcher.listYtTables();
        assertTrue(ytTables.size() > 0);
        ytTables.forEach((k, v) -> {
            assertTrue(v > 0);
        });
    }

    @Test
    public void testGetLastLoads() {
        jdbcTemplate.getJdbcOperations().execute("insert into manual_loads " +
            "(yt_path, revision, table_name) values ('" + YT_PATH + "', " + REVISION + ", '" + TABLE_NAME + "')");
        jdbcTemplate.getJdbcOperations().execute("insert into manual_loads " +
            "(yt_path, revision, table_name) values ('" + YT_PATH + "', " + (REVISION + 1) + ", '" + TABLE_NAME + "')");
        assertThat(directoryLoadTaskDispatcher.getLastLoads().size(),
            is(1));
        assertThat(directoryLoadTaskDispatcher.getLastLoads().get(YT_PATH),
            is(REVISION + 1L));
    }

    @Test
    public void testCreateSuccessTask() {
        task.success();
        assertNTaskExist(1, true, false);
    }

    @Test
    public void testCreateFailTask() {
        task.fail(new RuntimeException("Test"));
        assertNTaskExist(0, false, false);
    }

    @Test
    public void testCreateRejectedTask() {
        task.fail(new SQLNonTransientException("Test"));
        assertNTaskExist(1, false, true);
    }

    private void assertNTaskExist(Integer n, boolean loaded, boolean rejected) {
        assertThat(jdbcTemplate.queryForObject("select count(*) from manual_loads " +
                "where loaded = :loaded and data_rejected = :rejected and " +
                "yt_path = :yt_path and revision = :revision",
            ImmutableMap.of(
                "loaded", loaded,
                "rejected", rejected,
                "yt_path", YT_PATH,
                "revision", REVISION
            ), Integer.class), is(n));
    }

}
