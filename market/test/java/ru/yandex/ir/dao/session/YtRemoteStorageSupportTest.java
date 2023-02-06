package ru.yandex.ir.dao.session;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class YtRemoteStorageSupportTest {
    private static final YPath EXISTS = YPath.simple("//exists");
    private static final ListF<String> ALL_SESSIONS = Cf.list(
        "20170102_0100",
        "20170102_0101",
        "20170102_0102",
        "20170102_0103",
        "20170102#0104",
        "20170102_0105",
        "0170102_0106",
        "20170102_0107",
        "20170102_010",
        "20170102_0109"
    );
    private static final ListF<String> GOOD_SESSIONS = Cf.list(
        "20170102_0100",
        "20170102_0101",
        "20170102_0102",
        "20170102_0103",
        "20170102_0105",
        "20170102_0107",
        "20170102_0109"
    );
    private static final String A_SESSION = "20170102_0102";

    @Mock
    private Yt ytApi;
    @Mock
    private Cypress cypress;
    private YtRemoteStorageSupport support;

    private static List<String> getSessionIds(List<YtRemoteSessionLocation> x) {
        return x.stream().map(RemoteSessionLocation::getSessionId).collect(Collectors.toList());
    }

    @Before
    public void setup() {
        when(ytApi.cypress()).thenReturn(cypress);

        when(cypress.exists(EXISTS)).thenReturn(true);

        when(cypress.list(any())).thenReturn(
            ALL_SESSIONS.map(x -> new YTreeStringNodeImpl(x, Cf.map()))
        );

        support = new YtRemoteStorageSupport(ytApi, EXISTS.toString(), true);
    }

    @Test
    public void getAllSessions() throws Exception {
        assertEquals(GOOD_SESSIONS, getSessionIds(support.getAllSessions(x -> true)));
        assertEquals(Cf.list(), getSessionIds(support.getAllSessions(x -> false)));
        assertEquals(Cf.list(A_SESSION), getSessionIds(support.getAllSessions(x -> x.sessionId.equals(A_SESSION))));
    }
}