package ru.yandex.market.psku.postprocessor.service.yt.session;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.market.psku.postprocessor.common.db.dao.SessionDao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Fedor Dergachev <a href="mailto:dergachevfv@yandex-team.ru"></a>
 */
@RunWith(MockitoJUnitRunner.class)
public class YtSessionServiceTest {

    private static final String YT_PSKU_SESSION_PATH = "//psku-test";
    private static final String YT_ENRICH_SESSION_PATH = "//enriched-psku-test";
    private static final String SESSION_REGEXP = "\\d{8}_\\d{4}";

    @Mock
    private Yt yt;
    @Mock
    private Cypress cypress;

    private YtPskuSessionService ytPskuSessionService;
    private YtEnrichSessionService ytEnrichSessionService;

    @Mock
    private SessionDao sessionDao;

    @Before
    public void setUp() {
        when(yt.cypress()).thenReturn(cypress);
        when(yt.cypress().exists(any())).thenReturn(false).thenReturn(true);
        when(yt.cypress().list(any())).thenReturn(Option.empty());
        ytPskuSessionService = new YtPskuSessionService(yt, sessionDao, YT_PSKU_SESSION_PATH);
        ytEnrichSessionService = new YtEnrichSessionService(yt, sessionDao, YT_ENRICH_SESSION_PATH);
    }

    @Test
    public void whenStartPskuNewSessionOk() {
        SessionParam sessionParam = ytPskuSessionService.startNewPskuSession();
        String sessionName = sessionParam.getName();

        assertNotNull(sessionName);
        assertTrue(sessionName.matches(SESSION_REGEXP));
        YPath sessionPath = YPath.simple(YT_PSKU_SESSION_PATH).child(sessionName);
        verify(cypress).create(eq(sessionPath), eq(CypressNodeType.MAP), anyBoolean(), anyBoolean());
    }

    @Test
    public void whenPskuSessionExistsStartNewSessionShouldDoNothing() {
        when(cypress.exists(any(YPath.class))).thenReturn(true);

        SessionParam sessionParam = ytPskuSessionService.startNewPskuSession();
        String sessionName = sessionParam.getName();

        assertNotNull(sessionName);
        assertTrue(sessionName.matches(SESSION_REGEXP));
        YPath sessionPath = YPath.simple(YT_PSKU_SESSION_PATH).child(sessionName);
        verify(cypress, never()).create(eq(sessionPath), eq(CypressNodeType.MAP), anyBoolean(), anyBoolean());
    }

    @Test
    public void whenStartNewPskuSessionWithPrefixOk() {
        String prefix = "prefix";
        SessionParam sessionParam = ytPskuSessionService.startNewPskuSession(prefix);
        String sessionName = sessionParam.getName();

        assertNotNull(sessionName);
        assertTrue(sessionName.matches(prefix + "_" + SESSION_REGEXP));
        YPath sessionPath = YPath.simple(YT_PSKU_SESSION_PATH).child(sessionName);
        verify(cypress).create(eq(sessionPath), eq(CypressNodeType.MAP), anyBoolean(), anyBoolean());
    }

    @Test
    public void whenSwitchPskuRecentOk() {
        String sessionName = "test_session";

        ytPskuSessionService.updateSessionRecentLink(sessionName);

        YPath sessionPath = YPath.simple(YT_PSKU_SESSION_PATH).child(sessionName);
        YPath recentPath = YPath.simple(YT_PSKU_SESSION_PATH).child("recent");
        verify(cypress).link(eq(sessionPath), eq(recentPath), anyBoolean());
    }

    @Test
    public void whenFinishPskuSessionOk() {
        String sessionName = "test_session";
        long sessionId = 1234L;
        SessionParam sessionParam = new SessionParam(sessionId, sessionName);
        YPath sessionPath = YPath.simple(YT_PSKU_SESSION_PATH).child(sessionName);
        when(cypress.exists(sessionPath)).thenReturn(true);

        ytPskuSessionService.finishSession(sessionParam);

        verify(cypress).create(eq(sessionPath.child("SESSION_OK")),
                eq(CypressNodeType.INT64), eq(false), eq(false));
        verify(cypress).set(eq(sessionPath.child("SESSION_OK")), eq(sessionId));
    }

    @Test(expected = YtSessionServiceException.class)
    public void whenNoPskuSessionFinishSessionFail() {
        when(cypress.exists(any(YPath.class))).thenReturn(false);
        String sessionName = "test_session";
        long sessionId = 1234L;
        SessionParam sessionParam = new SessionParam(sessionId, sessionName);
        ytPskuSessionService.finishSession(sessionParam);
    }

    @Test
    public void whenRotatePskuSessionsOk() {
        List<String> toRemove = Arrays.asList(
                "20190101_1200",
                "20190101_1502");
        List<String> toLeave = Arrays.asList(
                "20190808_1008",
                "recent",
                "should_skip1",
                "should_skip2",
                "should_skip3");

        List<String> all = new ArrayList<>();
        all.addAll(toRemove);
        all.addAll(toLeave);

        when(cypress.list(any(YPath.class)))
                .thenReturn(
                        Stream.concat(all.stream(), Stream.of("recent"))
                                .map(sessionId -> new YTreeStringNodeImpl(sessionId, Cf.map()))
                                .collect(Collectors.toCollection(Cf::arrayList)));

        when(cypress.exists(any(YPath.class))).thenReturn(true);

        ytPskuSessionService.rotateSessions(1);

        verify(cypress, times(toRemove.size()))
                .remove(argThat(yPath -> toRemove.contains(yPath.name())));
        verify(cypress, never())
                .remove(argThat(yPath -> toLeave.contains(yPath.name())));
    }

    @Test
    public void whenStartEnrichNewSessionOk() {
        String sessionName = "my-session";
        boolean result = ytEnrichSessionService.startNewEnrichSession(sessionName);

        assertTrue(result);
        YPath sessionPath = YPath.simple(YT_ENRICH_SESSION_PATH).child(sessionName);
        verify(cypress).create(eq(sessionPath), eq(CypressNodeType.MAP), anyBoolean(), anyBoolean());
    }
}
