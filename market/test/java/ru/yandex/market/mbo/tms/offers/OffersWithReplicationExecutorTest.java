package ru.yandex.market.mbo.tms.offers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.mbo.synchronizer.export.storage.ReplicationService;
import ru.yandex.market.mbo.tms.offers.utils.ParamsLogUtils;
import ru.yandex.market.mbo.tms.sqlmonitoring.models.JugglerEvent;
import ru.yandex.market.mbo.yt.TestYt;
import ru.yandex.market.mbo.yt.utils.GlobalLogUtils;
import ru.yandex.market.mbo.yt.utils.MediumLogUtils;
import ru.yandex.yt.ytclient.proxy.request.CreateNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OffersWithReplicationExecutorTest {

    public static final String INPUT = "//home/blabla/input";
    public static final String OUTPUT = "//home/blabla/output";

    public static final String SESSION_1 = "20211004_1657";
    public static final String SESSION_2 = "20211005_1236";
    public static final String SESSION_3 = "20211005_1657";

    private OffersWithReplicationExecutor offersExecutorSpy;
    private TestYt testYt;

    private ReplicationService replicationService;

    @Before
    public void setUp() {
        testYt = new TestYt();
        replicationService = Mockito.mock(ReplicationService.class);
        NamedParameterJdbcTemplate siteCatalogPgNamedJdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);

        OffersWithReplicationExecutor offersExecutor = new OffersWithReplicationExecutor(
            testYt,
            new TestYt(),
            replicationService,
            "super_pool",
            INPUT,
            OUTPUT,
            siteCatalogPgNamedJdbcTemplate
        );
        offersExecutorSpy = spy(offersExecutor);
        doReturn("integration-test").when(offersExecutorSpy).getServiceWithEnv(isNull(), isNull());
        doNothing().when(offersExecutorSpy)
            .savePgMonitorJugglerEvent(any(JugglerEvent.class), any(NamedParameterJdbcTemplate.class));
    }

    @Test
    public void testOfferMigration() throws Exception {
        Mockito.when(replicationService.copySession(any())).thenReturn(true);

        JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
        JobDetail jobDetail = Mockito.mock(JobDetail.class);
        when(context.getJobDetail()).thenReturn(jobDetail);
        JobKey jobKey = new JobKey("superKey");
        when(jobDetail.getKey()).thenReturn(jobKey);

        prepareSession(SESSION_1);
        linkInputSession(SESSION_1);
        offersExecutorSpy.doJob(context);
        assertSession(SESSION_1);

        prepareSession(SESSION_2);
        linkInputSession(SESSION_2);
        offersExecutorSpy.doJob(context);
        assertSession(SESSION_2);

        offersExecutorSpy.doJob(context);
        assertSession(SESSION_2);

        prepareSession(SESSION_3);
        linkInputSession(SESSION_3);
        offersExecutorSpy.doJob(context);
        assertSession(SESSION_3);
    }

    @Test(expected = IllegalStateException.class)
    public void testOfferMigrationFailedOnReplicaFailed() throws Exception {
        Mockito.when(replicationService.copySession(any())).thenReturn(true);

        JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
        JobDetail jobDetail = Mockito.mock(JobDetail.class);
        when(context.getJobDetail()).thenReturn(jobDetail);
        JobKey jobKey = new JobKey("superKey");
        when(jobDetail.getKey()).thenReturn(jobKey);

        prepareSession(SESSION_1);
        linkInputSession(SESSION_1);
        offersExecutorSpy.doJob(context);
        assertSession(SESSION_1);

        Mockito.when(replicationService.copySession(any())).thenReturn(false);
        prepareSession(SESSION_2);
        linkInputSession(SESSION_2);
        try {
            offersExecutorSpy.doJob(context);
        } catch (Exception e) {
            assertSessionAbsent(SESSION_2);
            assertSession(SESSION_1);
            throw e;
        }
    }

    private void prepareSession(String sessionId) {
        YPath sessionPath = YPath.simple(INPUT).child(sessionId);
        YPath mboOffersMr = sessionPath.child("mbo_offers_mr");
        CreateNode request = new CreateNode(mboOffersMr, CypressNodeType.TABLE, GlobalLogUtils.sortedTableAttrs());
        testYt.cypress().create(request);

        YPath paramsMr = sessionPath.child("params_mr");
        request = new CreateNode(paramsMr, CypressNodeType.TABLE, ParamsLogUtils.tableAttrs());
        testYt.cypress().create(request);
    }

    private void linkInputSession(String sessionId) {
        YPath sessionPath = YPath.simple(INPUT).child(sessionId);
        YPath recent = YPath.simple(INPUT).child("recent");
        GUID guid = testYt.cypress().link(sessionPath, recent);
        testYt.transactions().commit(guid);
    }

    private void assertSession(String sessionId) {
        YPath sessionPath = YPath.simple(OUTPUT).child(sessionId);
        assertTrue(testYt.cypress().exists(sessionPath));
        YPath globalLog = sessionPath.child(GlobalLogUtils.GLOBAL_LOG);
        assertTrue(testYt.cypress().exists(globalLog));
        YPath mediumLog = sessionPath.child(MediumLogUtils.MEDIUM_LOG);
        assertTrue(testYt.cypress().exists(mediumLog));
        YPath paramLog = sessionPath.child(ParamsLogUtils.PARAMS_LOG);
        assertTrue(testYt.cypress().exists(paramLog));
        YPath linkPath = YPath.simple(OUTPUT).child("recent&");
        assertTrue(testYt.cypress().exists(linkPath));
        assertEquals(sessionPath.toString(), testYt.cypress().get(linkPath.attribute("target_path")).stringValue());
    }

    private void assertSessionAbsent(String sessionId) {
        YPath sessionPath = YPath.simple(OUTPUT).child(sessionId);
        assertFalse(testYt.cypress().exists(sessionPath));
        YPath globalLog = sessionPath.child(GlobalLogUtils.GLOBAL_LOG);
        assertFalse(testYt.cypress().exists(globalLog));
        YPath mediumLog = sessionPath.child(MediumLogUtils.MEDIUM_LOG);
        assertFalse(testYt.cypress().exists(mediumLog));
        YPath linkPath = YPath.simple(OUTPUT).child("recent&");
        assertTrue(testYt.cypress().exists(linkPath));
    }
}
