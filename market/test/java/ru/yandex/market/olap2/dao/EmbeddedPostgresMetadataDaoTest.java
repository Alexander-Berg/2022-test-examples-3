package ru.yandex.market.olap2.dao;


import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opentable.db.postgres.embedded.LiquibasePreparer;
import com.opentable.db.postgres.junit.EmbeddedPostgresRules;
import com.opentable.db.postgres.junit.PreparedDbRule;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.olap2.controller.JugglerConstants;
import ru.yandex.market.olap2.controller.MonitorUpdateTimeController;
import ru.yandex.market.olap2.load.tasks.ClickhouseLoadTask;
import ru.yandex.market.olap2.model.LoadTaskStatus;
import ru.yandex.market.olap2.model.StepEventsQueryResult;
import ru.yandex.market.olap2.model.TableAndPartition;
import ru.yandex.market.olap2.model.YtCluster;
import ru.yandex.market.olap2.sla.ImportantCubesPaths;
import ru.yandex.market.olap2.sla.SlaCubesHolder;
import ru.yandex.market.olap2.step.model.StepEvent;
import ru.yandex.market.olap2.step.model.StepEventParams;
import ru.yandex.market.olap2.util.ResultSetUtil;
import ru.yandex.market.olap2.yt.YtClusterLiveliness;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static ru.yandex.market.olap2.model.LoadTaskStatus.GOT_FROM_STEP;

public class EmbeddedPostgresMetadataDaoTest {
    private static final String PATH = "//cubes/kateleb/2020-09-01";
    private static final String PUBLISH = "marketstat_olap2_publish";
    private static final String REBUILD = "marketstat_olap2_rebuild";
    private static final String LEADER_HOST = "leader1.db.yandex-team.ru";


    private final YtClusterLiveliness liveliness = Mockito.mock(YtClusterLiveliness.class);

    {
        Mockito.when(liveliness.liveYtClusters()).thenReturn(ImmutableSet.of(new YtCluster("someytcluster")));
    }

    @Rule
    public PreparedDbRule db = EmbeddedPostgresRules.preparedDatabase(
            LiquibasePreparer.forClasspathLocation("liquibase/changelog.xml")
    );

    @Test
    public void testTablesMade() {
        LoggingJdbcTemplate l = new LoggingJdbcTemplate(new NamedParameterJdbcTemplate(db.getTestDatabase()),
                "localhost");
        l.exec("truncate table step_events");
        insertEvent(l, "someytcluster", "//path/dir1/dir2/tablename/2020-08", 202008,
                ImmutableMap.of("rows", 10, "size", 100, "event_name", "someventname", "loaded", false));
        insertEvent(l, "somedeadytcluster", "//path/dir1/dir2/tablename/2020-09", 202009,
                ImmutableMap.of("rows", 10, "size", 100, "event_name", "someventname", "loaded", false));


        MetadataDao dao = new MetadataDao(l, Mockito.mock(ImportantCubesPaths.class));
        List<StepEventsQueryResult> res = dao.getNotLoadedStepEvents(liveliness.liveYtClusters());
        assertThat(res.size(), is(1));
        assertThat(res.get(0).getPath(), is("//path/dir1/dir2/tablename/2020-08"));
        assertThat(res.get(0).getPartition(), is(202008));
        assertThat(res.get(0).getDestination(), is("clickhouse"));
        assertThat(res.get(0).getYtCluster(), equalTo(new YtCluster("someytcluster")));
    }

    @Test
    public void testNoAliveClusters() {
        YtClusterLiveliness allDead = Mockito.mock(YtClusterLiveliness.class);
        Mockito.when(allDead.liveYtClusters()).thenReturn(Collections.emptySet());

        LoggingJdbcTemplate l = new LoggingJdbcTemplate(new NamedParameterJdbcTemplate(db.getTestDatabase()),
                "localhost");
        l.exec("truncate table step_events");
        insertEvent(l, "somedeadytcluster", "//path/dir1/dir2/tablename/2020-09", 202009,
                ImmutableMap.of("rows", "10", "size", "100", "event_name", "someventname2"));

        MetadataDao dao = new MetadataDao(l, Mockito.mock(ImportantCubesPaths.class));
        assertThat(dao.getNotLoadedStepEvents(allDead.liveYtClusters()).size(), is(0));
    }

    @Test
    public void testGetNotLoadedEvents() {
        MetadataDao md = getMetadataDao();
        md.insertStepEvent(createEvent(PUBLISH, PATH, 20200901, "step1", 5));
        md.insertStepEvent(createEvent(PUBLISH, PATH, 20200901, "step2",
                40));

        List<StepEventsQueryResult> events = md.getNotLoadedStepEvents(liveliness.liveYtClusters());
        assertThat(events.size(), is(0));
        md.updateAttributes(PATH, "someytcluster", 1000, 10000, LocalDateTime.now());
        events = md.getNotLoadedStepEvents(liveliness.liveYtClusters());
        assertThat(events.size(), is(1));
    }

    @Test
    public void testGetNotLoadedEventsWrongPartition() {
        MetadataDao md = getMetadataDao();
        md.insertStepEvent(createEvent(PUBLISH, PATH, 2020091, "step1", 5));
        md.insertStepEvent(createEvent(PUBLISH, PATH, 20200901, "step2",
                40));

        md.updateAttributes(PATH, "someytcluster", 1000, 10000, LocalDateTime.now());
        List<StepEventsQueryResult> events = md.getNotLoadedStepEvents(liveliness.liveYtClusters());
        assertThat(events.size(), is(2));
        assertThat(events.get(0).getEventStepId(), is("step2"));
    }

    @Test
    public void testGetNotLoadedEventsSortPartitions() {
        MetadataDao md = getMetadataDao();
        String pathDay = "//cubes/day/";
        String pathMonth = "//cubes/month/";
        String pathNull = "//cubes/no_partition/";
        String pathMonthAlt = "//cubes/month_alt/";
        md.insertStepEvent(createEvent(PUBLISH, pathDay + "2020-02-02", 20200202, "step-day-old", 15));
        md.insertStepEvent(createEvent(PUBLISH, pathDay + "2020-09-01", 20200901, "step-day-1", 5));
        md.insertStepEvent(createEvent(PUBLISH, pathDay + "2020-09-02", 20200902, "step-day-2", 23));
        md.insertStepEvent(createEvent(PUBLISH, pathDay + "2020-09-02", 20200902, "step-day-3", 15));
        md.insertStepEvent(createEvent(PUBLISH, pathMonth + "2020-02", 202002, "step-month-1", 5));
        md.insertStepEvent(createEvent(PUBLISH, pathMonth + "2020-08", 202008, "step-month-2", 5));
        md.insertStepEvent(createEvent(PUBLISH, pathMonth + "2020-09", 202009, "step-month-3", 5));
        md.insertStepEvent(createEvent(PUBLISH, pathNull, null, "step-null-1", 10));
        md.insertStepEvent(createEvent(PUBLISH, pathNull, null, "step-null-fresh", 9));
        md.insertStepEvent(createEvent(PUBLISH, pathMonthAlt + "2020-09", 202009, "step-month-2-fresh", 4));

        md.updateAttributes(pathDay + "2020-09-01", "someytcluster", 1000, 10000, LocalDateTime.now());
        md.updateAttributes(pathDay + "2020-09-02", "someytcluster", 1000, 10000, LocalDateTime.now());
        md.updateAttributes(pathDay + "2020-02-02", "someytcluster", 1000, 10000, LocalDateTime.now());
        md.updateAttributes(pathMonth + "2020-08", "someytcluster", 1000, 10000, LocalDateTime.now());
        md.updateAttributes(pathMonth + "2020-09", "someytcluster", 1000, 10000, LocalDateTime.now());
        md.updateAttributes(pathMonth + "2020-02", "someytcluster", 1000, 10000, LocalDateTime.now());
        md.updateAttributes(pathNull, "someytcluster", 1000, 10000, LocalDateTime.now());
        md.updateAttributesForTest(pathMonthAlt + "2020-09", "someytcluster", 1000, 10000, LocalDateTime.now());

        List<StepEventsQueryResult> events = md.getNotLoadedStepEvents(liveliness.liveYtClusters());
        assertThat(events.size(), is(8));
        // Первыми идут свежие партиции, сортировка между ними по step_created_at, чем старше тем выше
        assertThat(events.get(0).getEventStepId(), is("step-null-fresh"));
        assertThat(events.get(1).getEventStepId(), is("step-day-3"));
        assertThat(events.get(2).getEventStepId(), is("step-month-3"));
        assertThat(events.get(3).getEventStepId(), is("step-month-2-fresh"));
    }

    @Test
    public void testGetNotLoadedEventsSortPartitionsMonthStart() {
        LocalDateTime currentTime=LocalDateTime.of(2020,3,1,1,0);
        MetadataDao md = prepareMetadataDaoSpy(currentTime);

        String pathDay = "//cubes/day/";
        String pathMonth = "//cubes/month/";
        String pathMonthAlt = "//cubes/month_alt/";
        String pathNull = "//cubes/no_partition/";
        md.insertStepEvent(createEvent(PUBLISH, pathDay + "2020-01-10", 20200110, "step-day-1", currentTime.minusMinutes(5)));
        md.insertStepEvent(createEvent(PUBLISH, pathDay + "2020-02-27", 20200227, "step-day-2", currentTime.minusMinutes(5)));
        md.insertStepEvent(createEvent(PUBLISH, pathDay + "2020-02-28", 20200228, "step-day-fresh", currentTime.minusMinutes(15)));
        md.insertStepEvent(createEvent(PUBLISH, pathMonth + "2019-12", 201912, "step-month-1", currentTime.minusMinutes(5)));
        md.insertStepEvent(createEvent(PUBLISH, pathMonth + "2020-01", 202001, "step-month-2", currentTime.minusMinutes(5)));
        md.insertStepEvent(createEvent(PUBLISH, pathMonth + "2020-02", 202002, "step-month-fresh", currentTime.minusMinutes(5)));
        md.insertStepEvent(createEvent(PUBLISH, pathNull, null, "step-null-1", currentTime.minusMinutes(10)));
        md.insertStepEvent(createEvent(PUBLISH, pathNull, null, "step-null-fresh", currentTime.minusMinutes(9)));
        md.insertStepEvent(createEvent(PUBLISH, pathMonthAlt + "2020-02", 202002, "step-month-2-fresh", currentTime.minusMinutes(4)));


        md.updateAttributesForTest(pathDay + "2020-01-10", "someytcluster", 1000, 10000, currentTime);
        md.updateAttributesForTest(pathDay + "2020-02-27", "someytcluster", 1000, 10000, currentTime);
        md.updateAttributesForTest(pathDay + "2020-02-28", "someytcluster", 1000, 10000, currentTime);
        md.updateAttributesForTest(pathMonth + "2019-12", "someytcluster", 1000, 10000, currentTime);
        md.updateAttributesForTest(pathMonth + "2020-01", "someytcluster", 1000, 10000, currentTime);
        md.updateAttributesForTest(pathMonth + "2020-02", "someytcluster", 1000, 10000, currentTime);
        md.updateAttributesForTest(pathNull, "someytcluster", 1000, 10000, currentTime);
        md.updateAttributesForTest(pathMonthAlt + "2020-02", "someytcluster", 1000, 10000, currentTime);

        List<StepEventsQueryResult> events = md.getNotLoadedStepEvents(liveliness.liveYtClusters());
        assertThat(events.size(), is(8));
        // Первыми идут свежие партиции, сортировка между ними по step_created_at, чем старше тем выше
        assertThat(events.get(0).getEventStepId(), is("step-day-fresh"));
        assertThat(events.get(1).getEventStepId(), is("step-null-fresh"));
        assertThat(events.get(2).getEventStepId(), is("step-month-fresh"));
        assertThat(events.get(3).getEventStepId(), is("step-month-2-fresh"));
    }


    @Test
    public void testGetNotLoadedEventsSortPartitionsMonthAlmostStart() {
        LocalDateTime currentTime=LocalDateTime.of(2020,3,2,0,0);
        MetadataDao md = prepareMetadataDaoSpy(currentTime);

        String pathDay = "//cubes/day/";
        String pathMonth = "//cubes/month/";
        String pathMonthAlt = "//cubes/month_alt/";
        String pathNull = "//cubes/no_partition/";
        md.insertStepEvent(createEvent(PUBLISH, pathDay + "2020-01-10", 20200110, "step-day-1", currentTime.minusMinutes(5)));
        md.insertStepEvent(createEvent(PUBLISH, pathDay + "2020-02-28", 20200228, "step-day-2", currentTime.minusMinutes(5)));
        md.insertStepEvent(createEvent(PUBLISH, pathDay + "2020-03-01", 20200301, "step-day-fresh", currentTime.minusMinutes(7)));
        md.insertStepEvent(createEvent(PUBLISH, pathNull, null, "step-null-fresh", currentTime.minusMinutes(6)));
        md.insertStepEvent(createEvent(PUBLISH, pathMonth + "2020-01", 202001, "step-month-1", currentTime.minusMinutes(5)));
        md.insertStepEvent(createEvent(PUBLISH, pathMonth + "2020-02", 202002, "step-month-2", currentTime.minusMinutes(5)));
        md.insertStepEvent(createEvent(PUBLISH, pathMonth + "2020-03", 202003, "step-month-fresh", currentTime.minusMinutes(5)));
        md.insertStepEvent(createEvent(PUBLISH, pathMonthAlt + "2020-03", 202003, "step-month-2-fresh", currentTime.minusMinutes(4)));

        md.updateAttributesForTest(pathDay + "2020-01-10", "someytcluster", 1000, 10000, currentTime);
        md.updateAttributesForTest(pathDay + "2020-02-28", "someytcluster", 1000, 10000, currentTime);
        md.updateAttributesForTest(pathDay + "2020-03-01", "someytcluster", 1000, 10000, currentTime);
        md.updateAttributesForTest(pathNull, "someytcluster", 1000, 10000, currentTime);
        md.updateAttributesForTest(pathMonth + "2020-01", "someytcluster", 1000, 10000, currentTime);
        md.updateAttributesForTest(pathMonth + "2020-02", "someytcluster", 1000, 10000, currentTime);
        md.updateAttributesForTest(pathMonth + "2020-03", "someytcluster", 1000, 10000, currentTime);
        md.updateAttributesForTest(pathMonthAlt + "2020-03", "someytcluster", 1000, 10000, currentTime);

        List<StepEventsQueryResult> events = md.getNotLoadedStepEvents(liveliness.liveYtClusters());
        assertThat(events.size(), is(8));
        // Первыми идут свежие партиции, сортировка между ними по step_created_at, чем старше тем выше
        assertThat(events.get(0).getEventStepId(), is("step-day-fresh"));
        assertThat(events.get(1).getEventStepId(), is("step-null-fresh"));
        assertThat(events.get(2).getEventStepId(), is("step-month-fresh"));
        assertThat(events.get(3).getEventStepId(), is("step-month-2-fresh"));
    }

    @Test
    public void testGetNotLoadedEventsSortPartitionsMonthEnd() {
        LocalDateTime currentTime=LocalDateTime.of(2020,3,31,0,0);
        MetadataDao md = prepareMetadataDaoSpy(currentTime);

        String pathDay = "//cubes/day/";
        String pathMonth = "//cubes/month/";
        String pathMonthAlt = "//cubes/month_alt/";
        String pathNull = "//cubes/no_partition/";
        md.insertStepEvent(createEvent(PUBLISH, pathDay + "2020-01-10", 20200110, "step-day-1", currentTime.minusMinutes(5)));
        md.insertStepEvent(createEvent(PUBLISH, pathDay + "2020-03-30", 20200330, "step-day-2", currentTime.minusMinutes(5)));
        md.insertStepEvent(createEvent(PUBLISH, pathDay + "2020-03-31", 20200331, "step-day-fresh", currentTime.minusMinutes(15)));
        md.insertStepEvent(createEvent(PUBLISH, pathMonth + "2020-01", 202001, "step-month-1", currentTime.minusMinutes(5)));
        md.insertStepEvent(createEvent(PUBLISH, pathMonth + "2020-02", 202002, "step-month-2", currentTime.minusMinutes(5)));
        md.insertStepEvent(createEvent(PUBLISH, pathMonth + "2020-03", 202003, "step-month-fresh", currentTime.minusMinutes(5)));
        md.insertStepEvent(createEvent(PUBLISH, pathNull, null, "step-null-fresh", currentTime.minusMinutes(9)));
        md.insertStepEvent(createEvent(PUBLISH, pathMonthAlt + "2020-03", 202003, "step-month-2-fresh", currentTime.minusMinutes(4)));

        md.updateAttributesForTest(pathDay + "2020-01-10", "someytcluster", 1000, 10000, currentTime);
        md.updateAttributesForTest(pathDay + "2020-03-30", "someytcluster", 1000, 10000, currentTime);
        md.updateAttributesForTest(pathDay + "2020-03-31", "someytcluster", 1000, 10000, currentTime);
        md.updateAttributesForTest(pathMonth + "2020-01", "someytcluster", 1000, 10000, currentTime);
        md.updateAttributesForTest(pathMonth + "2020-02", "someytcluster", 1000, 10000, currentTime);
        md.updateAttributesForTest(pathMonth + "2020-03", "someytcluster", 1000, 10000, currentTime);
        md.updateAttributesForTest(pathNull, "someytcluster", 1000, 10000, currentTime);
        md.updateAttributesForTest(pathMonthAlt + "2020-03", "someytcluster", 1000, 10000, currentTime);

        List<StepEventsQueryResult> events = md.getNotLoadedStepEvents(liveliness.liveYtClusters());
        assertThat(events.size(), is(8));
        // Первыми идут свежие партиции, сортировка между ними по step_created_at, чем старше тем выше
        assertThat(events.get(0).getEventStepId(), is("step-day-fresh"));
        assertThat(events.get(1).getEventStepId(), is("step-null-fresh"));
        assertThat(events.get(2).getEventStepId(), is("step-month-fresh"));
        assertThat(events.get(3).getEventStepId(), is("step-month-2-fresh"));
    }

    private MetadataDao prepareMetadataDaoSpy(LocalDateTime currentTime) {
        MetadataDao realMd=getMetadataDao();
        MetadataDao md = spy(realMd);
        Mockito.when(md.prepareNotLoadedEventsQuery()).thenReturn(
                realMd.prepareNotLoadedEventsQuery().toLowerCase().replace(
                        "now()","to_timestamp('"+ currentTime.format(DateTimeFormatter.ISO_DATE_TIME)+"','YYYY-MM-DDThh24:mi:ss')"));
        return md;
    }

    @Test
    public void testEverythingForUpdate() {
        MetadataDao md = getMetadataDao();
        MonitorUpdateTimeController monitoring = new MonitorUpdateTimeController(md);
        //inserting 2 events
        md.insertStepEvent(createEvent(PUBLISH, PATH, 20200901, "step1", 5));
        md.insertStepEvent(createEvent(PUBLISH, PATH, 20200901, "step2",
                40));

        //check partitions need to update
        List<PartitionData> events = md.getNotUpdatedStepEvents();
        assertThat(events.size(), is(1));

        //check for monitoring before update
        checkMonitoringForNotLoaded(md, monitoring, 2, 2, JugglerConstants.CRIT);
        md.updateAttributes(PATH, "someytcluster", 1000, 10000, LocalDateTime.now());
        checkAttrs(1000L, 10000L);

        //check for monitoring after update
        checkMonitoringForNotLoaded(md, monitoring, 0, 0, JugglerConstants.OK);
    }


    @Test
    public void testSameTablePublish() {
        MetadataDao md = getMetadataDao();
        //inserting 1 event
        md.insertStepEvent(createEvent(PUBLISH, PATH, 20200901, "step1", 5));
        md.updateAttributes(PATH, "someytcluster", 1000, 2000, LocalDateTime.now());

        //check no same tables are loaded
        assertNull(md.getLoadIdForSameTable("step1", PATH));

        // insert and load same table with different attributes
        md.insertStepEvent(createEvent(PUBLISH, PATH, 20200901, "step2",
                40));
        md.updateAttributes(PATH, "someytcluster", 2000, 3000, LocalDateTime.now());
        md.updateEventLoadedSuccessfully(PATH, 20200901, "step2", "clickhouse");

        //check no same tables are loaded
        assertNull(md.getLoadIdForSameTable("step1", PATH));

        // insert and load same table with same attributes
        md.insertStepEvent(createEvent(PUBLISH, PATH, 20200901, "step3",
                40));
        md.updateAttributes(PATH, "someytcluster", 1000, 2000, LocalDateTime.now());
        md.updateEventLoadedSuccessfully(PATH, 20200901, "step3", "clickhouse");

        //check same tables are loaded
        assertThat(md.getLoadIdForSameTable("step1", PATH), is("step3"));
    }


    @Test
    public void testDoublePublish() {
        MetadataDao md = getMetadataDao();
        //inserting 3 events
        md.insertStepEvent(createEvent(PUBLISH, PATH, 20200901, "step1", 5));
        md.insertStepEvent(createEvent(REBUILD, PATH, 20200901, "step2", 40));
        md.insertStepEvent(createEvent(PUBLISH, PATH, 20200901, "step3", 40));

        //check no publishes for today are loaded
        assertNull(md.getLoadedPublishIdForSameTable("step1", PATH));
        // load another table for rebuild event
        md.updateEventLoadedSuccessfully(PATH, 20200901, "step2", "clickhouse");

        //check no publishes for today are loaded
        assertNull(md.getLoadedPublishIdForSameTable("step1", PATH));

        // load another table for publish event
        md.updateEventLoadedSuccessfully(PATH, 20200901, "step3", "clickhouse");
        //check another publish is loaded
        assertThat(md.getLoadedPublishIdForSameTable("step1", PATH), is("step3"));
    }


    @Test
    public void testNotDoublePublishFprRebuild() {
        MetadataDao md = getMetadataDao();
        //inserting 3 events
        md.insertStepEvent(createEvent(PUBLISH, PATH, 20200901, "step1", 5));
        md.insertStepEvent(createEvent(REBUILD, PATH, 20200901, "step2", 40));
        md.updateAttributes(PATH, "someytcluster", 1000, 2000, LocalDateTime.now());

        md.updateEventLoadedSuccessfully(PATH, 20200901, "step1", "clickhouse");

        //check if step2 can be  skipped as second publish
        assertNull(md.getLoadedPublishIdForSameTable("step2", PATH));

    }

    @Test
    public void testLoadStartTimeIsWritten() {
        MetadataDao md = getMetadataDao();
        ClickhouseLoadTask task = Mockito.mock(ClickhouseLoadTask.class);
        {
            Mockito.when(task.getStepEventId()).thenReturn("test_event_id_for_load_start");
            Mockito.when(task.getTmpTable()).thenReturn("test_tmp_table");
        }
        md.insertLeader(LEADER_HOST);
        md.insertStepEvent(createEvent(PUBLISH, PATH, 20200901, task.getStepEventId(), 5));
        md.updateStartLoadStatus(task);

        LoggingJdbcTemplate l = new LoggingJdbcTemplate(new NamedParameterJdbcTemplate(db.getTestDatabase()),
                "localhost");
        Timestamp load_started_at = l.queryForObject("select load_started_at " +
                        "from step_events where event_step_id = :event_step_id ",
                ImmutableMap.of("event_step_id", task.getStepEventId()),
                Timestamp.class);
        assertThat(load_started_at.toInstant().isAfter(Instant.now().minus(5, ChronoUnit.HOURS)), equalTo(true));

    }

    //  Они вообще все вставляются одинаково, но для порядка разные статусы накидала в тесты
    @Test
    public void testLeaderIsWrittenOnGotFromStep() {
        String leaderHost = prepareLeaderHostTest(GOT_FROM_STEP);
        assertThat(leaderHost, equalTo(LEADER_HOST));

    }

    @Test
    public void testLeaderIsWrittenOnSuccess() {
        String leaderHost = prepareLeaderHostTest(LoadTaskStatus.SUCCESS);
        assertThat(leaderHost, equalTo(LEADER_HOST));

    }

    @Test
    public void testLeaderIsWrittenOnFailure() {
        String leaderHost = prepareLeaderHostTest(LoadTaskStatus.FAILURE);
        assertThat(leaderHost, equalTo(LEADER_HOST));

    }

    @Test
    public void testLeaderIsWrittenOnRejected() {
        String leaderHost = prepareLeaderHostTest(LoadTaskStatus.REJECTED);
        assertThat(leaderHost, equalTo(LEADER_HOST));
    }

    @Test
    public void getTodaysEventsByPathByClusterTest() {
        LoggingJdbcTemplate l = new LoggingJdbcTemplate(new NamedParameterJdbcTemplate(db.getTestDatabase()),
                "localhost");
        l.exec("truncate table step_events");
        insertEvent(l, "someytcluster_1", "//path/dir1/dir2/tablename/2020-08", 202008,
                ImmutableMap.of("rows", "10", "size", "100"));
        insertEvent(l, "someytcluster_1", "//path/dir1/dir2/tablename/2020-08", 202008,
                ImmutableMap.of("rows", "11", "size", "101"));
        insertEvent(l, "someytcluster_2", "//path/dir1/dir2/tablename/2020-08", 202008,
                ImmutableMap.of("rows", "12", "size", "102"));
        insertEvent(l, "someytcluster_2", "//path/dir1/dir2/tablename/2020-09", 202009,
                ImmutableMap.of("rows", "13", "size", "103"));

        Map<TableAndPartition, Map<YtCluster, List<Pair<Long, Long>>>> eventsByPathByCluster =
                new MetadataDao(l, Mockito.mock(ImportantCubesPaths.class)).getTodaysEventsByTablePartitionByCluster();
        assertThat(eventsByPathByCluster, Matchers.equalTo(ImmutableMap.of(
                new TableAndPartition("cubes_clickhouse__tablename", 202008), ImmutableMap.of(
                        new YtCluster("someytcluster_1"), Arrays.asList(Pair.of(10L, 100L), Pair.of(11L, 101L)),
                        new YtCluster("someytcluster_2"), Arrays.asList(Pair.of(12L, 102L))),
                new TableAndPartition("cubes_clickhouse__tablename", 202009), ImmutableMap.of(
                        new YtCluster("someytcluster_2"), Arrays.asList(Pair.of(13L, 103L))
                )
        )));
    }

    @Test
    public void getLoadedSizeSumTest() {
        LoggingJdbcTemplate l = new LoggingJdbcTemplate(new NamedParameterJdbcTemplate(db.getTestDatabase()),
                "localhost");
        l.exec("truncate table step_events");
        insertEvent(l, "someytcluster_1", "//path/dir1/dir2/tablename/2020-08", 202008,
                ImmutableMap.of("rows", "10", "size", "100"));
        insertEvent(l, "someytcluster_2", "//path/dir1/dir2/tablename/2020-09", 202009,
                ImmutableMap.of("rows", "20", "size", "200"));
        long bytesSize = new MetadataDao(l, Mockito.mock(ImportantCubesPaths.class)).getLoadedSizeSum(2);
        assertThat(bytesSize, is(300L));
    }

    @Test
    public void getPerCubeSizeTest() {
        MetadataDao dao = initPerCubeXTest("size");
        Map<String, Long> m = dao.getLoadSizePerTables(2);
        assertThat(m, equalTo(ImmutableMap.of(
                "cubes_clickhouse__tablename", 300L,
                "cubes_clickhouse__othertable", 50L)));
    }

    @Test
    public void getPerCubeCountTest() {
        MetadataDao dao = initPerCubeXTest("count");
        Map<String, Long> m = dao.getLoadCountPerTables(2);
        assertThat(m, equalTo(ImmutableMap.of(
                "cubes_clickhouse__tablename", 3L,
                "cubes_clickhouse__othertable", 1L)));
    }

    @Test
    public void getCurrentReleaseJobExtractsFieldsCorrectly() {

        MetadataDao md = getMetadataDao();
        StepEvent event = createEvent(PUBLISH, PATH, 20200901, "step1", 5);
        event.getStepEventParams().setRevisionNumber(1337L);
        md.insertStepEvent(event);

        Optional<StepEventsQueryResult> result = md.getCurrentReleaseJob(1337L);
        assertThat(result.get().getEventStepId(), equalTo("step1"));
        assertThat(result.get().getDestination(), equalTo("clickhouse"));
        assertThat(result.get().getPartition(), equalTo(20200901));
        assertThat(result.get().getPriority(), equalTo("Low"));
        assertThat(result.get().getStatus(), equalTo(GOT_FROM_STEP));
    }

    private MetadataDao initPerCubeXTest(String x) {
        Preconditions.checkArgument(ImmutableSet.of("size", "count").contains(x));

        LoggingJdbcTemplate l = new LoggingJdbcTemplate(new NamedParameterJdbcTemplate(db.getTestDatabase()),
                "localhost");
        l.exec("truncate table step_events");
        insertEvent(l, "someytcluster_1", "//path/dir1/dir2/tablename/2020-08", 202008,
                ImmutableMap.of(x, "100"));
        insertEvent(l, "someytcluster_1", "//path/dir1/dir2/tablename/2020-08", 202008,
                ImmutableMap.of(x, "100"));
        insertEvent(l, "someytcluster_2", "//path/dir1/dir2/tablename/2020-09", 202009,
                ImmutableMap.of(x, "100"));
        insertEvent(l, "someytcluster_2", "//path/dir1/dir2/othertable", null,
                ImmutableMap.of(x, "50"));
        insertEvent(l, "someytcluster_2", "//path/dir1/dir2/othertable", null,
                ImmutableMap.of(x, "60", "loaded", false));
        return new MetadataDao(l, Mockito.mock(ImportantCubesPaths.class));
    }

    private static final AtomicLong uniqEventId = new AtomicLong(1);

    private void insertEvent(LoggingJdbcTemplate l, String cluster, String path, Integer partition, Map<String, Object> other) {
        l.exec("insert into step_events (event_step_id, cluster, event_name, path, partition, step_created_at, " +
                "destination, rows, size, loaded, loaded_at) values " +
                "('somestepid_" + uniqEventId.getAndIncrement() + "', '" + cluster + "', '" +
                other.getOrDefault("event_name", "marketstat_olap2_publish") + "', " +
                "'" + path + "', " + (partition == null ? "null" : partition) + ", now()" +
                ", 'clickhouse', " + other.getOrDefault("rows", "0") + ", " +
                other.getOrDefault("size", "0") + ", " + other.getOrDefault("loaded", true) +
                ", " + ((boolean) other.getOrDefault("loaded", true) ? "now()" : "null") + ")");
    }

    private String prepareLeaderHostTest(LoadTaskStatus status) {
        MetadataDao md = getMetadataDao();
        ClickhouseLoadTask task = Mockito.mock(ClickhouseLoadTask.class);
        {
            Mockito.when(task.getStepEventId()).thenReturn("test_event_id_w_leader");
        }
        md.insertLeader(LEADER_HOST);
        md.insertStepEvent(createEvent(PUBLISH, PATH, 20200901, task.getStepEventId(), 5));
        md.updateLastTaskStatus(task, status);

        LoggingJdbcTemplate l = new LoggingJdbcTemplate(new NamedParameterJdbcTemplate(db.getTestDatabase()),
                "localhost");
        return l.queryForObject("select leader_host " +
                        "from step_events where event_step_id = :event_step_id ",
                ImmutableMap.of("event_step_id", task.getStepEventId()),
                String.class);
    }

    private void checkMonitoringForNotLoaded(MetadataDao md, MonitorUpdateTimeController monitoring,
                                             long notUpdated24h, long notUpdated30m, String status) {
        assertThat(md.getNotUpdatedEventsCountFor24h(), is(notUpdated24h));
        assertThat(md.getNotUpdatedEventsCountFor30minutes(), is(notUpdated30m));
        assertThat(monitoring.getEventsNotUpdatedInAday(), is(ResponseEntity.ok(JugglerConstants.OK)));
        assertThat(monitoring.getEventsNotUpdatedInLast30Minutes(), is(ResponseEntity.ok(JugglerConstants.OK)));
        assertThat(monitoring.getUpdateLag().getBody(), startsWith(status));
    }

    private void checkAttrs(long rows, long size) {
        LoggingJdbcTemplate md = new LoggingJdbcTemplate(new NamedParameterJdbcTemplate(db.getTestDatabase()),
                "localhost");
        StepEventsQueryResult step1 = getDataForSE(md, "step1");
        StepEventsQueryResult step2 = getDataForSE(md, "step2");
        assertThat(step1.getRows(), is(rows));
        assertThat(step2.getRows(), is(rows));
        assertThat(step1.getSize(), is(size));
        assertThat(step2.getSize(), is(size));
    }

    private StepEventsQueryResult getDataForSE(LoggingJdbcTemplate jdbcTemplate, String stepEventId) {
        return jdbcTemplate.queryForObject("select event_step_id," +
                        "path,  partition, destination, retry_count, priority, cluster,  size, rows, last_status" +
                        " from step_events " +
                        "where event_step_id = :event_step_id",
                ImmutableMap.of("event_step_id", stepEventId),
                (rs, rowNum) -> new StepEventsQueryResult(
                        rs.getString("event_step_id"),
                        rs.getString("path"),
                        ResultSetUtil.getIntOrNull(rs, "partition"),
                        rs.getString("destination"),
                        rs.getInt("retry_count"),
                        rs.getString("priority"),

                        new YtCluster(rs.getString("cluster")),
                        rs.getLong("size"),
                        rs.getLong("rows"),
                        LoadTaskStatus.valueOf(rs.getString("last_status")),
                        LocalDateTime.now()));
    }

    private MetadataDao getMetadataDao() {
        LoggingJdbcTemplate l = new LoggingJdbcTemplate(new NamedParameterJdbcTemplate(db.getTestDatabase()),
                "localhost");
        l.exec("truncate table step_events");
        return new MetadataDao(l, new ImportantCubesPaths(new SlaCubesHolder(new HashMap<>())));
    }

    private StepEvent createEvent(String name, String path, Integer partition, String eventId, int minutesAgo) {
        StepEventParams params = new StepEventParams();
        params.setPath(path);
        params.setDestination("clickhouse");
        params.setPartition(partition==null?null:String.valueOf(partition));
        params.setCluster("someytcluster");
        params.setPriority("low");
        StepEvent e = new StepEvent();
        e.setName(name);
        e.setTimeCreated(LocalDateTime.now().minusMinutes(minutesAgo).format(DateTimeFormatter.ISO_DATE_TIME).replace("T", " "));
        e.setId(eventId);
        e.setStepEventParams(params);
        return e;
    }

    private StepEvent createEvent(String name, String path, Integer partition, String eventId, LocalDateTime timeCreated) {
        StepEventParams params = new StepEventParams();
        params.setPath(path);
        params.setDestination("clickhouse");
        params.setPartition(partition==null?null:String.valueOf(partition));
        params.setCluster("someytcluster");
        params.setPriority("low");
        StepEvent e = new StepEvent();
        e.setName(name);
        e.setTimeCreated(timeCreated.format(DateTimeFormatter.ISO_DATE_TIME).replace("T", " "));
        e.setId(eventId);
        e.setStepEventParams(params);
        return e;
    }


}
