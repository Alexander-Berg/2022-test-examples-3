package ru.yandex.market.olap2.step;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import ru.yandex.market.olap2.config.IntegrationTestConfig;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@ActiveProfiles("integration-test")
@SpringBootTest(classes = {IntegrationTestConfig.class})
public class StepBackupITest {

    @Autowired
    private StepBackup stepBackup;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Value("${olap2.cluster}")
    private String cluster;

    @Test
    public void mustJsonBackup() throws Exception {
        jdbcTemplate.getJdbcOperations().execute(
            "drop table if exists testbackup_tbl");
        jdbcTemplate.getJdbcOperations().execute(
            "create table testbackup_tbl (" +
                "a_int int not null," +
                "b_char varchar(512) null," +
                "c_bool boolean not null default false)");
        jdbcTemplate.getJdbcOperations().execute("insert into testbackup_tbl (a_int, b_char, c_bool) values (" +
            "1, null, true)");
        jdbcTemplate.getJdbcOperations().execute("insert into testbackup_tbl (a_int, b_char, c_bool) values (" +
            "2, 'aza_za_za', false)");
        if(stepBackup.jsonfile("testbackup_tbl", "20180702").exists()) {
            stepBackup.jsonfile("testbackup_tbl", "20180702").delete();
        }
        stepBackup.doJsonBackup("testbackup_tbl", "20180702");
        assertThat(splitSort(new String(Files.readAllBytes(
            stepBackup.jsonfile("testbackup_tbl", "20180702").toPath()))),
            is(ImmutableSet.of("{\"a_int\":1,\"b_char\":null,\"c_bool\":true}",
                "{\"a_int\":2,\"b_char\":\"aza_za_za\",\"c_bool\":false}")));
    }


    private Set<String> splitSort(String multiline) {
        return Arrays.stream(multiline.split("\n"))
            .map(line -> line.trim())
            .collect(Collectors.toSet());
    }

    @Test
    public void mustNotBackup() throws Exception {
        String day = "2018-01-03";
        String expected = "StepBackupITest_mustNotBackup_" + ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
        Files.write(stepBackup.jsonfile("step_events", day).toPath(), expected.getBytes());
        stepBackup.backup("step_events", day);
        String got = Files.readAllLines(stepBackup.jsonfile("step_events", day).toPath()).get(0);
        assertThat(got, is(expected));
    }

    @Test
    // any day is good, it is used only in filename
    public void checkRestorable() throws Exception {
        String day = "2018-01-02";
        try {
            stepBackup.doJsonBackup("step_events", day);
            stepBackup.checkRestorable("step_events", day);
        } finally {
            if(stepBackup.jsonfile("step_events", day).exists()) {
                stepBackup.jsonfile("step_events", day).delete();
            }
        }
    }

    @Test
    public void doBackupStepEvents() throws Exception {
        jdbcTemplate.getJdbcOperations().execute(
            "truncate table step_events");
        jdbcTemplate.getJdbcOperations().execute(
            "insert into step_events (" +
                "event_step_id, cluster, event_name, path, partition, created_at, step_created_at, loaded, loaded_at, data_rejected, data_rejected_at) values (" +
                "'eid1', '" + cluster + "', 'ename1', 'path1', 201801, '2018-01-01 10:30:45', '2018-01-01 10:30:44', true, '2018-01-01 10:30:50', false, null)");
        jdbcTemplate.getJdbcOperations().execute(
            "insert into step_events (" +
                "event_step_id, cluster, event_name, path, partition, created_at, step_created_at, loaded, loaded_at, data_rejected, data_rejected_at) values (" +
                "'eid2', '" + cluster + "', 'ename2', 'path2', 201802, '2018-01-01 12:40:25', '2018-01-01 12:40:24', false, null, true, '2018-01-01 12:40:30')");
        doBackupTestRunner("step_events");
    }

    @Test
    public void doBackupTabloUsers() throws Exception {
        jdbcTemplate.getJdbcOperations().execute(
            "truncate table tablo_users");
        jdbcTemplate.getJdbcOperations().execute(
            "insert into tablo_users (login, role) values ('login1', 'role1')");
        jdbcTemplate.getJdbcOperations().execute(
            "insert into tablo_users (login, role) values ('login2', 'role2')");
        doBackupTestRunner("tablo_users");
    }

    private void doBackupTestRunner(String table) throws Exception {
        String day = "2018-01-01";
        try {
            stepBackup.doJsonBackup(table, day);
        } finally {
            if(stepBackup.jsonfile(table, day).exists()) {
                stepBackup.jsonfile(table, day).delete();
            }
        }
    }
}
