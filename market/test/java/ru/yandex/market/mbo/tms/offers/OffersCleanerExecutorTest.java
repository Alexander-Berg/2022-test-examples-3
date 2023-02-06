package ru.yandex.market.mbo.tms.offers;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;

import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.mbo.yt.utils.GlobalLogUtils;
import ru.yandex.market.mbo.yt.TestYt;
import ru.yandex.yt.ytclient.proxy.request.CreateNode;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class OffersCleanerExecutorTest {

    public static final String PATH = "//home/super/output";

    public static final String SESSION_1 = "20211004_1657";
    public static final String SESSION_2 = "20211005_1236";
    public static final String SESSION_3 = "20211005_1657";
    public static final String SESSION_4 = "20211006_1152";
    public static final String SESSION_5 = "20211006_1236";
    public static final String SESSION_6 = "20211006_1657";
    public static final String SESSION_7 = "20211007_1152";
    public static final String SESSION_8 = "20211007_1236";
    public static final String SESSION_9 = "20211007_1657";
    public static final String SESSION_10 = "20211008_1152";
    public static final String SESSION_11 = "20211008_1236";
    public static final String SESSION_12 = "20211008_1657";

    private OffersCleanerExecutor offersCleanerExecutor;
    private TestYt testYt;
    private TestYt replicationTestYt;

    @Before
    public void setUp() {
        testYt = new TestYt();
        replicationTestYt = new TestYt();
        offersCleanerExecutor = new OffersCleanerExecutor(
            testYt,
            replicationTestYt,
            PATH
        );
    }

    @Test
    public void testRemoveWhileSessionsLower10() throws Exception {
        List<String> sessionsToGenerate = Arrays.asList(SESSION_1, SESSION_2, SESSION_3, SESSION_4);
        generateSessions(sessionsToGenerate);

        JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
        JobDetail jobDetail = Mockito.mock(JobDetail.class);
        when(context.getJobDetail()).thenReturn(jobDetail);
        JobKey jobKey = new JobKey("superKey");
        when(jobDetail.getKey()).thenReturn(jobKey);

        offersCleanerExecutor.doJob(context);

        sessionsToGenerate.forEach(this::assertSessionExists);
    }

    @Test
    public void testRemoveWhileSessionsEq10() throws Exception {
        List<String> sessionsToGenerate = Arrays.asList(SESSION_1, SESSION_2, SESSION_3, SESSION_4, SESSION_5,
            SESSION_6, SESSION_7, SESSION_8, SESSION_9, SESSION_10);
        generateSessions(sessionsToGenerate);
        linkSession(SESSION_10);

        JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
        JobDetail jobDetail = Mockito.mock(JobDetail.class);
        when(context.getJobDetail()).thenReturn(jobDetail);
        JobKey jobKey = new JobKey("superKey");
        when(jobDetail.getKey()).thenReturn(jobKey);

        offersCleanerExecutor.doJob(context);

        sessionsToGenerate.forEach(this::assertSessionExists);
    }

    @Test
    public void testRemoveSessions() throws Exception {
        List<String> sessionsToGenerate = Arrays.asList(SESSION_1, SESSION_2, SESSION_3, SESSION_4, SESSION_5,
            SESSION_6, SESSION_7, SESSION_8, SESSION_9, SESSION_10, SESSION_11, SESSION_12);
        generateSessions(sessionsToGenerate);
        linkSession(SESSION_12);

        JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
        JobDetail jobDetail = Mockito.mock(JobDetail.class);
        when(context.getJobDetail()).thenReturn(jobDetail);
        JobKey jobKey = new JobKey("superKey");
        when(jobDetail.getKey()).thenReturn(jobKey);

        offersCleanerExecutor.doJob(context);

        List<String> removed = Arrays.asList(SESSION_1, SESSION_2);
        removed.forEach(this::assertSessionRemoved);
        List<String> alive = Arrays.asList(SESSION_3, SESSION_4, SESSION_5,
            SESSION_6, SESSION_7, SESSION_8, SESSION_9, SESSION_10, SESSION_11, SESSION_12);
        alive.forEach(this::assertSessionExists);
    }

    @Test
    public void testRemoveSessionsAndRecentOnOldSession() throws Exception {
        List<String> sessionsToGenerate = Arrays.asList(SESSION_1, SESSION_2, SESSION_3, SESSION_4, SESSION_5,
            SESSION_6, SESSION_7, SESSION_8, SESSION_9, SESSION_10, SESSION_11, SESSION_12);
        generateSessions(sessionsToGenerate);
        linkSession(SESSION_2);

        JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
        JobDetail jobDetail = Mockito.mock(JobDetail.class);
        when(context.getJobDetail()).thenReturn(jobDetail);
        JobKey jobKey = new JobKey("superKey");
        when(jobDetail.getKey()).thenReturn(jobKey);

        offersCleanerExecutor.doJob(context);

        List<String> removed = Arrays.asList(SESSION_1, SESSION_3);
        removed.forEach(this::assertSessionRemoved);
        List<String> alive = Arrays.asList(SESSION_2, SESSION_4, SESSION_5,
            SESSION_6, SESSION_7, SESSION_8, SESSION_9, SESSION_10, SESSION_11, SESSION_12);
        alive.forEach(this::assertSessionExists);
    }

    private void generateSessions(List<String> sessions) {
        sessions.forEach(this::generateSession);
    }

    private void generateSession(String session) {
        YPath sessionPath = YPath.simple(PATH).child(session);
        YPath globalLog = sessionPath.child(GlobalLogUtils.GLOBAL_LOG);
        CreateNode request = new CreateNode(globalLog, CypressNodeType.TABLE, GlobalLogUtils.sortedTableAttrs());
        request.setRecursive(true);
        testYt.cypress().create(request);
        replicationTestYt.cypress().create(request);
    }

    private void assertSessionExists(String sessionId) {
        YPath sessionPath = YPath.simple(PATH).child(sessionId);
        assertTrue(testYt.cypress().exists(sessionPath));
        assertTrue(replicationTestYt.cypress().exists(sessionPath));
        YPath globalLog = sessionPath.child("global_log");
        assertTrue(testYt.cypress().exists(globalLog));
        assertTrue(replicationTestYt.cypress().exists(globalLog));
    }

    private void assertSessionRemoved(String sessionId) {
        YPath sessionPath = YPath.simple(PATH).child(sessionId);
        assertFalse(testYt.cypress().exists(sessionPath));
        assertFalse(replicationTestYt.cypress().exists(sessionPath));
        YPath globalLog = sessionPath.child("global_log");
        assertFalse(testYt.cypress().exists(globalLog));
        assertFalse(replicationTestYt.cypress().exists(globalLog));
    }

    private void linkSession(String sessionId) {
        YPath sessionPath = YPath.simple(PATH).child(sessionId);
        YPath recent = YPath.simple(PATH).child("recent");
        GUID guid = testYt.cypress().link(sessionPath, recent);
        testYt.transactions().commit(guid);
        guid = replicationTestYt.cypress().link(sessionPath, recent);
        replicationTestYt.transactions().commit(guid);
    }
}
