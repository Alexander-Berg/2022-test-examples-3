package ru.yandex.market.olap2.load;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import ru.yandex.market.olap2.config.IntegrationTestConfig;
import ru.yandex.market.olap2.util.SleepUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@ActiveProfiles("integration-test")
@SpringBootTest(classes = {IntegrationTestConfig.class})
public class CopyIntoVerticaITest {
    @Autowired
    private CopyIntoVertica copyIntoVertica;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private LoadTask task;

    @Before
    public void init() {
        task = new TestLoadTask("eventid", "//some/testyt/path", 201803);
        jdbcTemplate.getJdbcOperations().execute("drop table if exists " + task.getTmpTable() + " cascade");
        jdbcTemplate.getJdbcOperations().execute("drop table if exists " + task.getTable() + " cascade");
        jdbcTemplate.getJdbcOperations().execute(
            "create table " + task.getTable() + " (datetime timestamp not null, value int, strvalue varchar(64)) " +
                "partition by (datetime)");
    }

    @After
    public void cleanUp() {
        jdbcTemplate.getJdbcOperations().execute("drop table if exists " + task.getTmpTable() + " cascade");
        jdbcTemplate.getJdbcOperations().execute("drop table if exists " + task.getTable() + " cascade");
    }

    @Test
    public void mustCopy() {
        write(
            "{\"datetime\":\"2018-10-14 13:45:57\",\"value\":8,\"strvalue\":\"somestr\"}\n" +
                "{\"datetime\":\"2018-10-14 13:45:58\",\"value\":9,\"strvalue\":\"somestr\"}\n");
        copyIntoVertica.copyIntoTmp(task);
        List<String> data = jdbcTemplate.queryForList("select datetime || value || strvalue as v " +
                "from " + task.getTmpTable() + " order by v",
            Collections.emptyMap(),
            String.class);
        System.out.println(data);
        assertThat(data, is(Arrays.asList("2018-10-14 13:45:578somestr", "2018-10-14 13:45:589somestr")));
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void mustReject() {
        write(
            "{\"datetime\":\"not date time\",\"value\":8,\"strvalue\":\"somestr\"}\n" +
                "{\"datetime\":\"2018-10-14 13:45:58\",\"value\":9,\"strvalue\":\"somestr\"}\n");
        copyIntoVertica.copyIntoTmp(task);
    }

    @Test
    public void mustCreateTmpTable() {
        // create tmp
        copyIntoVertica.createTmpTable(task);
        long tbls = jdbcTemplate.queryForObject("select count(*) from tables where table_name = :table_name",
            ImmutableMap.of("table_name", task.getTmpTable()),
            Long.class);
        assertThat(tbls, is(1L));
    }

    @Test
    public void mustDropOldTmpTables() {
        // create tmp
        copyIntoVertica.createTmpTable(
            new TestLoadTask("eventid", "//some/testyt/path", 201803));
        SleepUtil.sleep(10); // =)
        copyIntoVertica.createTmpTable(
            new TestLoadTask("eventid", "//some/testyt/path", 201803));
        SleepUtil.sleep(10); // =)
        copyIntoVertica.createTmpTable(
            new TestLoadTask("eventid", "//some/testyt/path", 201803));

        copyIntoVertica.tryDropOldTmpTables(
            new TestLoadTask("eventid", "//some/testyt/path", 201803)
        );

        assertThat(jdbcTemplate.queryForObject("select count(*) from tables where table_name like '" +
            new TestLoadTask("eventid", "//some/testyt/path", 201803).getTmpTablePrefix() +
            "%'", Collections.emptyMap(), Long.class), is(0L));
    }

    @SneakyThrows
    private void write(String data) {
        try(
            ByteArrayOutputStream baos = new ByteArrayOutputStream(data.getBytes().length);
            GZIPOutputStream zip = new GZIPOutputStream(baos)
        ) {
            zip.write(data.getBytes());
            zip.finish();
            zip.close();
            baos.close();
            Files.write(baos.toByteArray(), new File(task.getFile()));
        }
    }

}
