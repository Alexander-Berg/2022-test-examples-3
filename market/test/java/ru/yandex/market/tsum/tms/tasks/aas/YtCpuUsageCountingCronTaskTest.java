package ru.yandex.market.tsum.tms.tasks.aas;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.commune.bazinga.scheduler.ExecutionContext;
import ru.yandex.market.tsum.clients.yql.YqlApiClient;
import ru.yandex.market.tsum.core.TestMongo;
import ru.yandex.market.tsum.core.TestYql;
import ru.yandex.market.tsum.core.dao.ParamsDao;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestMongo.class, TestYql.class})
public class YtCpuUsageCountingCronTaskTest {

    private YtCpuUsageCountingCronTask task;

    @Autowired
    private ParamsDao paramsDao;

    @Autowired
    private YqlApiClient yqlApiClient;

    @Before
    public void setUp() throws Exception {
        this.task = new YtCpuUsageCountingCronTask(yqlApiClient, paramsDao, true);
    }

    @Test
    public void getLastRun() {
        Instant lastRun = task.getLastRun();
        assertNotNull(lastRun);
        assertEquals(LocalDate.of(2018, Month.JANUARY, 1).atStartOfDay().toInstant(ZoneOffset.UTC),
            lastRun);
    }

    @Test
    public void getLastOperation() {
        String lastOperation = task.getLastOperation();
        assertEquals("", lastOperation);
    }

    @Test
    public void getSqlTemplate() throws IOException {
        String expected = "PRAGMA yt.QueryCacheMode = \"normal\";\n" +
            "\n" +
            "$input = hahn.[home/logfeller/logs/yt-scheduler-log/1d/2018-01-02];\n" +
            "\n" +
            "$resource_limits = Yson::ParseJson(resource_limits);\n" +
            "$memory = Yson::LookupDouble($resource_limits, \"memory\");\n" +
            "$cpu = Yson::LookupDouble($resource_limits, \"cpu\");\n" +
            "$pool = Yson::LookupString(Yson::ParseJson(_other{'spec'}), \"pool\");\n" +
            "\n" +
            "SELECT\n" +
            "        ops.cluster AS cluster,\n" +
            "        ops.pool AS pool,\n" +
            "        ops.user AS user,\n" +
            "        SUM(jobs.cpu_count * jobs.exec_time_sec) as cpu_sum,\n" +
            "        SUM(jobs.exec_time_sec) AS exec_time_sec\n" +
            "    FROM (\n" +
            "        SELECT\n" +
            "            cluster_name AS cluster,\n" +
            "            operation_id,\n" +
            "            $memory AS memory,\n" +
            "            $cpu AS cpu_count,\n" +
            "            (DateTime::FromString(finish_time) - DateTime::FromString(start_time)) / 1000000. AS exec_time_sec\n" +
            "        FROM $input\n" +
            "        WHERE operation_id IS NOT NULL\n" +
            "            AND cluster_name IN ('hahn', 'arnold')\n" +
            "            AND event_type IN ('job_completed', 'job_failed', 'job_aborted')\n" +
            "    ) AS jobs\n" +
            "    JOIN (\n" +
            "        SELECT\n" +
            "            cluster,\n" +
            "            operation_id,\n" +
            "            MAX($pool) AS pool,\n" +
            "            MAX(_other{\"authenticated_user\"}) AS user\n" +
            "        FROM $input\n" +
            "        WHERE operation_id IS NOT NULL\n" +
            "            AND cluster_name IN ('hahn', 'arnold')\n" +
            "            AND ($pool LIKE 'market-%')\n" +
            "        GROUP BY cluster_name AS cluster, operation_id\n" +
            "    ) AS ops\n" +
            "    USING (cluster, operation_id)\n" +
            "    GROUP BY ops.cluster, ops.pool, ops.user\n";

        String sql = YtCpuUsageCountingCronTask.getSql(LocalDate.of(2018, Month.JANUARY, 2));
        assertEquals(expected, sql);
    }

    @Test
    @Ignore
    public void testTskvLog() {
        task.fetchResultOperation("W6o-qfvJNezwg5AaodLlt3SYc_7ng55K9NjSQQQEq2M=");
    }

    @Test
    @Ignore
    public void testTaskExecute() throws Exception {
        task.execute(mock(ExecutionContext.class));
        task.execute(mock(ExecutionContext.class));
    }
}