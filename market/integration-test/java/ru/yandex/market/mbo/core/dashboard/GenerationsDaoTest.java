package ru.yandex.market.mbo.core.dashboard;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.mbo.configs.TestConfiguration;
import ru.yandex.market.mbo.gwt.models.dashboard.Generation;
import ru.yandex.market.mbo.gwt.models.dashboard.SessionStatusData;
import ru.yandex.market.mbo.gwt.models.dashboard.SessionsFilter;
import ru.yandex.market.mbo.gwt.models.dashboard.SuperControllerSession;
import ru.yandex.market.mbo.gwt.models.dashboard.SuperControllerSession.Database;
import ru.yandex.market.mbo.gwt.models.dashboard.SuperControllerSession.Status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author popfalushi
 */
@SuppressWarnings("checkstyle:MagicNumber")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class GenerationsDaoTest {
    private static final int TIME_DIFFERENCE = 1000 * 60 * 60 * 5;

    //Makes last 4 digits zeros because clickhouse uses seconds for DateTime/Timestamp storage.
    private static final long TEST_START_TIME_IN_MILLIS = new Date().getTime() / 10000 * 10000;
    private static final Date BASE_DATE = new Date(LocalDate.of(2000, 1, 1).toEpochDay());

    @Autowired
    GenerationsDao generationsDao;

    @Autowired
    DashboardClickhouseDDL ddl;

    private String suffix;

    @Before
    public void initTables() {
        suffix = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME).replaceAll("[.:-]", "_");
        ddl.checkTables(suffix);
        generationsDao.setSuffix(suffix);
    }

    @After
    public void dropTables() {
        ddl.dropTables(suffix);
    }

    private List<Generation> generateTestData() {
        List<Generation> generations = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Generation g = new Generation();
            g.setSessionId("sessionid-" + i);
            g.setType(Generation.Type.ORACLE);
            g.setTableName("params_20160618_1740");
            g.setStatus(Generation.Status.PUBLISHED);
            g.setAddedTime(new Date(TEST_START_TIME_IN_MILLIS - TIME_DIFFERENCE * 2));
            g.setStartTime(new Date(TEST_START_TIME_IN_MILLIS - TIME_DIFFERENCE));
            g.setFinishTime(new Date(TEST_START_TIME_IN_MILLIS));
            g.setRowCount(i);
            g.setIndexerType("indexerType");
            generations.add(g);
        }
        Generation lastInProcess = new Generation();
        lastInProcess.setSessionId("sessionid-pre-last-in-process");
        lastInProcess.setType(Generation.Type.ORACLE);
        lastInProcess.setTableName("params_20160618_1740");
        lastInProcess.setStatus(Generation.Status.IN_PROCESS);
        lastInProcess.setAddedTime(new Date(TEST_START_TIME_IN_MILLIS - TIME_DIFFERENCE * 3));
        lastInProcess.setStartTime(new Date(TEST_START_TIME_IN_MILLIS - TIME_DIFFERENCE * 2));
        lastInProcess.setFinishTime(new Date(TEST_START_TIME_IN_MILLIS));
        lastInProcess.setRowCount(10);
        lastInProcess.setIndexerType("indexerType");
        generations.add(lastInProcess);

        Generation preLastInProcess = new Generation();
        preLastInProcess.setSessionId("sessionid-last-in-process");
        preLastInProcess.setType(Generation.Type.ORACLE);
        preLastInProcess.setTableName("params_20160618_1740");
        preLastInProcess.setStatus(Generation.Status.IN_PROCESS);
        preLastInProcess.setAddedTime(new Date(TEST_START_TIME_IN_MILLIS - TIME_DIFFERENCE * 2));
        preLastInProcess.setStartTime(new Date(TEST_START_TIME_IN_MILLIS - TIME_DIFFERENCE));
        preLastInProcess.setFinishTime(new Date(TEST_START_TIME_IN_MILLIS));
        preLastInProcess.setRowCount(11);
        preLastInProcess.setIndexerType("indexerType");
        generations.add(preLastInProcess);

        for (int i = 0; i < 10; i++) {
            Generation g = new Generation();
            g.setSessionId("sessionid-" + i);
            g.setType(Generation.Type.SAAS);
            g.setTableName("mbo_offers_20160618_174" + (i % 2));
            g.setStatus(Generation.Status.PUBLISHED);
            g.setAddedTime(new Date(TEST_START_TIME_IN_MILLIS - TIME_DIFFERENCE * 3));
            g.setStartTime(new Date(TEST_START_TIME_IN_MILLIS - TIME_DIFFERENCE * 2)); //it is why it is pre-last :)
            g.setFinishTime(new Date(TEST_START_TIME_IN_MILLIS));
            g.setRowCount(i);
            g.setIndexerType("indexerType");
            generations.add(g);
        }
        return generations;
    }


    @Test
    public void testFilter() throws InterruptedException {
        List<Generation> generations = generateTestData();
        generationsDao.batchInsert(generations);

        List<String> prefixes = generationsDao.getTableNamePrefixes();
        Assert.assertTrue(prefixes.contains("mbo_offers"));
        Assert.assertTrue(prefixes.contains("params"));
        Assert.assertTrue(prefixes.size() == 2);

        List<SessionStatusData> sessionStatusDatas =
            generationsDao.loadSessionsStatuses(Database.ORACLE, prefixes);
        Assert.assertEquals(2, sessionStatusDatas.size());

        SuperControllerSession lastInProcess =
            generationsDao.getLastInProcessSession(Database.ORACLE);
        Assert.assertEquals("sessionid-last-in-process", lastInProcess.getSessionId());

        SessionsFilter sessionsFilter = new SessionsFilter();
        sessionsFilter.setStatus(Status.PUBLISHED);
        sessionsFilter.setAddedFrom(Date.from(
            LocalDate.of(2008, Month.APRIL, 1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()
        ));
        sessionsFilter.setDatabase(Database.SAAS);
        List<SuperControllerSession> scSessions = generationsDao.getSCSessions(sessionsFilter, 0, 500,
            SuperControllerSession.Field.ADDED_TIME, true);
        Assert.assertEquals(10, scSessions.size());
        int scSessionsCount = generationsDao.getSCSessionsCount(sessionsFilter);
        Assert.assertEquals(10, scSessionsCount);
        int scSessionsTimingsCount = generationsDao.getSessionsTimingsCount(sessionsFilter);
        Assert.assertEquals(2, scSessionsTimingsCount);
        List<SuperControllerSession> scSessionsTimings = generationsDao.getSessionsTimings(
                sessionsFilter, 0, 500, SuperControllerSession.Field.ADDED_TIME, false);
        Assert.assertEquals(2, scSessionsTimings.size());

        String lastPublishedBaseSessionId = generationsDao.getLastPublishedBaseSession(
            Database.SAAS, "mbo_offers");
        Assert.assertEquals("sessionid-9", lastPublishedBaseSessionId);

        Map<String, Long> sessions = new HashMap<>();
        sessions.put("session-6666", 6666L);
        sessions.put("session-6667", 6667L);
        Set<String> newSessionIds = generationsDao.createNewSessions(
            Database.YT, "params", "params_20160618_9999",
            sessions, "idxType", true);
        Assert.assertEquals(2, newSessionIds.size());

        generationsDao.failAllInProcessSessions(Database.ORACLE);
        SuperControllerSession failedLastInProcess = generationsDao.getLastInProcessSession(Database.ORACLE);
        Assert.assertNull(failedLastInProcess);
        List<String> tables = generationsDao.getTables(
            Status.NEW,
            Status.IN_PROCESS);
        Assert.assertEquals(2, tables.size());
    }

    @Test
    public void testUpdate() throws InterruptedException {
        Map<String, Long> sessions = new HashMap<>();
        sessions.put("session-6666", 6666L);
        sessions.put("session-6667", 6667L);
        Database yt = Database.YT;
        Set<String> newSessionIds = generationsDao.createNewSessions(
            yt, "params", "params_20160618_9999",
            sessions, "idxType", true);
        Assert.assertEquals(2, newSessionIds.size());

        // Check update status & start time
        SessionsFilter filter = new SessionsFilter(yt);
        filter.setStatus(Status.IN_PROCESS);

        List<SuperControllerSession> sessionsList =
            generationsDao.getSCSessions(filter, 0, 10, SuperControllerSession.Field.ADDED_TIME, false);

        Assert.assertEquals(0, sessionsList.size());

        generationsDao.updateSessions(yt, "params_20160618_9999", Collections.singleton("session-6666"),
            Status.NEW, Status.IN_PROCESS, true, false);


        sessionsList =
            generationsDao.getSCSessions(filter, 0, 10, SuperControllerSession.Field.ADDED_TIME, false);

        Assert.assertEquals(1, sessionsList.size());
        SuperControllerSession updated = sessionsList.get(0);
        Assert.assertTrue(updated.getStarted().compareTo(BASE_DATE) > 0);
        Assert.assertNull(updated.getFinished());

        // Update finish time also
        generationsDao.updateSessions(yt, "params_20160618_9999", Collections.singleton("session-6666"),
            Status.IN_PROCESS, Status.PUBLISHED, true, true);

        filter.setStatus(Status.PUBLISHED);
        sessionsList =
            generationsDao.getSCSessions(filter, 0, 10, SuperControllerSession.Field.ADDED_TIME, false);
        updated = sessionsList.get(0);
        Assert.assertTrue(updated.getFinished().compareTo(BASE_DATE) > 0);

        // No updates
        generationsDao.updateSessions(yt, "params_20160618_9999", Collections.singleton("session-6666"),
            Status.IN_PROCESS, Status.PUBLISHED, true, true);
    }

    @Test
    public void testLoadSessionsStatuses() {
        // Common session life-cycle
        generationsDao.createNewSessions(
            Database.SAAS, "not-used", "20180131_2017/mbo_offers_mr",
            ImmutableMap.of("20180131_2017", 100L), "stratocaster", true
        );

        generationsDao.updateSessions(
            Database.SAAS, "20180131_2017/mbo_offers_mr", Collections.singleton("20180131_2017"),
            Status.NEW, Status.IN_PROCESS, true, false);

        generationsDao.updateSessions(
            Database.SAAS, "20180131_2017/mbo_offers_mr", Collections.singleton("20180131_2017"),
            Status.IN_PROCESS, Status.PUBLISHED, false, true);

        List<SessionStatusData> sessions = generationsDao.loadSessionsStatuses(
            Database.SAAS,
            Collections.singletonList("20180131_2017/mbo_offers_mr"));

        Assert.assertEquals(1, sessions.size());
        Assert.assertEquals(Status.PUBLISHED, sessions.get(0).getStatus());
        Assert.assertEquals(100, sessions.get(0).getRowCount().intValue());
    }

    @Test
    public void testGetLastPublishedBaseSessionForYt() {
        // Older session
        generationsDao.createNewSessions(
            Database.ORACLE, "not-used", "20180131_2017/mbo_offers_mr",
            ImmutableMap.of("20180131_2017", 100L), "stratocaster", true
        );

        generationsDao.updateSessions(
            Database.ORACLE, "20180131_2017/mbo_offers_mr", Collections.singleton("20180131_2017"),
            Status.NEW, Status.PUBLISHED, true, false);

        String baseSession = generationsDao.getLastPublishedBaseSession(Database.ORACLE, "mbo_offers_mr");
        Assert.assertEquals("20180131_2017", baseSession);
    }

    @Test
    public void testCreateSkippedSession() {
        // Older session
        generationsDao.createNewSessions(
            Database.ORACLE, "mbo_offers_mr", "20180131_2017/mbo_offers_mr",
            ImmutableMap.of("20180131_2017", 100L), "stratocaster", true
        );

        generationsDao.updateSessions(
            Database.ORACLE, "20180131_2017/mbo_offers_mr", Collections.singleton("20180131_2017"),
            Status.NEW, Status.PUBLISHED, true, true);

        generationsDao.createNewSessions(
            Database.ORACLE, "mbo_offers_mr", "20180131_2017/mbo_offers_mr",
            ImmutableMap.of("20180131_2017", 1000L), "stratocaster", false // NOTE: false
        );

        List<SuperControllerSession> sessions = generationsDao.getSCSessions(
            new SessionsFilter(Database.ORACLE), 0, 100, SuperControllerSession.Field.ADDED_TIME, true);

        Assert.assertEquals(1, sessions.size());
        Assert.assertEquals(Status.SKIPPED, sessions.get(0).getStatus());
    }
}
