package ru.yandex.market.olap2.load;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import ru.yandex.market.olap2.config.IntegrationTestConfig;
import ru.yandex.market.olap2.graphite.Graphite;
import ru.yandex.market.olap2.util.SleepUtil;

import java.sql.SQLNonTransientException;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

@RunWith(SpringRunner.class)
@ActiveProfiles("integration-test")
@SpringBootTest(classes = {IntegrationTestConfig.class})
public class LoadTaskDispatcherITest {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;
    private final AtomicReference<LoadTask> result = new AtomicReference<>();
    private LoadTaskDispatcher dispatcher;

    @Before
    public void init() {
        LoadTaskExecutor executorMock = mock(LoadTaskExecutor.class);
        Graphite graphiteMock = mock(Graphite.class);
        this.dispatcher = new LoadTaskDispatcher(
            jdbcTemplate,
            executorMock,
            graphiteMock,
            "cluster1");
        Mockito.doAnswer((invocation) -> {
            result.set(invocation.getArgument(0));
            return null;
        }).when(executorMock).putTask(any());
        jdbcTemplate.getJdbcOperations().execute("truncate table step_events");
    }

    @Test
    public void testCheckNewStepEventsPartitioned() {
        jdbcTemplate.getJdbcOperations().execute(
            "insert into step_events (" +
                "event_step_id, cluster, event_name, path, partition, created_at, step_created_at) values (" +
                "'eid1', 'cluster1', 'ename1', '//some/yttestpath1/LoadTaskDispatcherITest', 201801, '2018-01-01 10:30:45', '2018-01-01 10:30:44')");
        dispatcher.checkNewStepEvents();
        assertTrue(result.get().getTable().length() > 0);
        assertThat(result.get().getPartition(), notNullValue());
        assertTrue(result.get().isHistoricalTable());
    }

    @Test
    public void testCheckNewStepEvents() {
        jdbcTemplate.getJdbcOperations().execute(
            "insert into step_events (" +
                "event_step_id, cluster, event_name, path, partition, created_at, step_created_at) values (" +
                "'eid2', 'cluster1', 'ename1', '//some/yttestpath1/LoadTaskDispatcherITest2', null, '2018-01-01 10:30:45', '2018-01-01 10:30:44')");
        dispatcher.checkNewStepEvents();
        assertTrue(result.get().getTable().length() > 0);
        assertThat(result.get().getPartition(), nullValue());
        assertTrue(!result.get().isHistoricalTable());
    }

    @Test
    public void testSuccessCallbackDifferentPartitiones() {
        insertPair("201802", "201803");
        jdbcTemplate.getJdbcOperations().execute("update step_events set data_rejected = true where event_step_id = 'eid1'");
        dispatcher.checkNewStepEvents();
        result.get().success();
        assertExists("eid2", true);
    }

    @Test
    public void testSuccessCallbackSamePartitiones() {
        insertPair("201802", "201802");
        dispatcher.checkNewStepEvents();
        result.get().success();
        assertThat(result.get().getStepEventId(), is("eid2"));
        SleepUtil.sleep(4000);
        assertExists("eid1", true);
        assertExists("eid2", true);
    }

    @Test
    public void testSuccessCallbackNotPartitioned() {
        insertPair("null", "null");
        dispatcher.checkNewStepEvents();
        result.get().success();
        assertThat(result.get().getStepEventId(), is("eid2"));
        assertExists("eid1", true);
        assertExists("eid2", true);
    }

    @Test
    public void testDataRejectedCallback() {
        jdbcTemplate.getJdbcOperations().execute(
            "insert into step_events (" +
                "event_step_id, cluster, event_name, path, partition, created_at, step_created_at) values (" +
                "'eid1_rej', 'cluster1', 'ename1', '//some/yttestpath1/LoadTaskDispatcherITest2', null, '2018-01-01 09:30:42', '2018-01-01 09:30:40')");
        dispatcher.checkNewStepEvents();
        result.get().fail(new SQLNonTransientException("test"));
        assertThat(result.get().getStepEventId(), is("eid1_rej"));
        assertExists("eid1_rej", false);
    }

    private void insertPair(String p1, String p2) {
        jdbcTemplate.getJdbcOperations().execute(
            "insert into step_events (" +
                "event_step_id, cluster, event_name, path, partition, created_at, step_created_at) values (" +
                "'eid1', 'cluster1', 'ename1'," +
                "'//some/yttestpath1/LoadTaskDispatcherITest2" +
                (p1.equals("null") ? "" : ("/" + LoadTask.hyphenate(Integer.parseInt(p1)))) + "', " + p1 + "," +
                "'2018-01-01 09:30:42', '2018-01-01 09:30:40')");
        jdbcTemplate.getJdbcOperations().execute(
            "insert into step_events (" +
                "event_step_id, cluster, event_name, path, partition, created_at, step_created_at) values (" +
                "'eid2', 'cluster1', 'ename1'," +
                "'//some/yttestpath1/LoadTaskDispatcherITest2" +
                (p2.equals("null") ? "" : ("/" + LoadTask.hyphenate(Integer.parseInt(p2)))) + "', " + p2 + "," +
                "'2018-01-01 10:30:45', '2018-01-01 10:30:44')");
    }

    private void assertExists(String id, boolean loaded) {
        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from step_events where event_step_id = :id and loaded = :loaded",
            ImmutableMap.of(
                "id", result.get().getStepEventId(),
                "loaded", loaded),
            Integer.class),
            is(1));
    }

}
