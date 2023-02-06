package ru.yandex.market.ir.yt.util.session;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeStringNode;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author astafurovme@yandex-team.ru
 * @timestamp 22.01.18
 * <p>
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class SessionUtilTest {
    @Mock
    private Cypress cypress;

    private static final YPath ROOT_SESSIONS_DIR = YPath.simple("//mock/sessionsRoot");

    // Good sessions
    private static final List<String> OK_SESSIONS = Arrays.asList(
        "20170101_0000",
        "20170103_0000",
        "20001221_0000",
        "20001222_0000",
        "20001223_0000",
        "20001224_0000",
        "20001225_0000",
        "20170807_1937",
        "20170807_2044",
        "20170807_2159",
        "20170807_2300",
        "20170907_2044",
        "20170907_2159",
        "20180101_0000"
    );

    // Failed or in process sessions
    private static final List<String> NOT_OK_SESSIONS = Arrays.asList(
        "20170104_0000",
        "20170102_0000",
        "20001226_0000",
        "20001227_0000",
        "20001228_0000",
        "20001229_0000",
        "20001230_0000",
        "20001231_0000",
        "20170707_1937",
        "20170707_2044",
        "20170707_2159",
        "20170707_2300",
        "20170907_1821",
        "20180907_1937"
    );

    @Before
    public void beforeClass() {
        YPath rootSessionsDir = YPath.simple("//mock/sessionsRoot");

        // Prepare sessions list response
        ListF<YTreeStringNode> sessionList = Cf.toList(CollectionUtils.join(OK_SESSIONS, NOT_OK_SESSIONS))
            .map(YTree::stringNode)
            .shuffle();

        when(cypress.exists(ROOT_SESSIONS_DIR)).thenReturn(true);
        when(cypress.list(rootSessionsDir)).thenReturn(sessionList);

        // Prepare session exists response
        OK_SESSIONS.forEach(sid ->
            when(cypress.exists(rootSessionsDir.child(sid).child(SessionUtil.SESSION_OK_FLAG)))
                .thenReturn(true)
        );
        NOT_OK_SESSIONS.forEach(sid ->
            when(cypress.exists(rootSessionsDir.child(sid).child(SessionUtil.SESSION_OK_FLAG)))
                .thenReturn(false)
        );
    }

    @Test
    public void testNoIntersection() {
        Assert.assertFalse(Cf.toSet(OK_SESSIONS).intersects(Cf.toSet(NOT_OK_SESSIONS)));
    }

    @Test
    public void testSessionsMethods() {
        // Test get last ok sessions
        String lastOkSession = SessionUtil.getLastOkSessionO(cypress, ROOT_SESSIONS_DIR).get();
        Assert.assertTrue(lastOkSession.equals("20180101_0000"));

        // Test get last any sessions
        String lastAnySession = SessionUtil.getLastAnySessionO(cypress, ROOT_SESSIONS_DIR).get();
        Assert.assertTrue(lastAnySession.equals("20180907_1937"));

        // Test last OK and NOT_OK sessions is empty
        when(cypress.list(ROOT_SESSIONS_DIR)).thenReturn(Cf.list());
        Assert.assertTrue(!SessionUtil.getLastOkSessionO(cypress, ROOT_SESSIONS_DIR).isPresent());
        Assert.assertTrue(!SessionUtil.getLastAnySessionO(cypress, ROOT_SESSIONS_DIR).isPresent());
    }

    @Test
    public void testDryRotation() {
        // Dry rotate. Keep all of 14 OK and NOT_OK sessions
        SessionUtil.rotateOldSessions(cypress, ROOT_SESSIONS_DIR, 14, 14);
        verify(cypress, never()).remove((YPath) any());
    }

    @Test
    public void testRotateOnlyOneFailedSession() {
        // Keep 14 OK sessions and 13 NOT_OK
        SessionUtil.rotateOldSessions(cypress, ROOT_SESSIONS_DIR, 14, 13);
        verify(cypress, times(1)).remove(ROOT_SESSIONS_DIR.child("20001226_0000"));
        verify(cypress, times(1)).remove((YPath) any());
    }

    @Test
    public void testRotationFull() {
        SessionUtil.rotateOldSessions(cypress, ROOT_SESSIONS_DIR, 0, 0);
        CollectionUtils.join(OK_SESSIONS, NOT_OK_SESSIONS).forEach(sid -> {
            verify(cypress, times(1)).remove(ROOT_SESSIONS_DIR.child(sid));
        });
        verify(cypress, times(28)).remove((YPath) any());
    }


}
