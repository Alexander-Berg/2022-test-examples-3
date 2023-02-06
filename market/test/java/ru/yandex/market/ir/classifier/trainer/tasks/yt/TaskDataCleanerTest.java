package ru.yandex.market.ir.classifier.trainer.tasks.yt;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.util.session.ClearableSessionManager;
import ru.yandex.market.util.session.SessionInfo;
import ru.yandex.market.util.session.SessionStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.when;
public class TaskDataCleanerTest {

    private static final String GENERATION = "generation";
    private TaskDataCleaner cleaner;
    private List<String> sessions;


    @Before
    public void setUp() {
        Map<SessionStatus, Integer> minExportSessionsByStatus = new HashMap<>();
        cleaner = new TaskDataCleaner();
        sessions = new ArrayList<>();
        minExportSessionsByStatus.put(SessionStatus.NEW, 1);
        minExportSessionsByStatus.put(SessionStatus.OK, 3);
        minExportSessionsByStatus.put(SessionStatus.FAIL, 1);
        minExportSessionsByStatus.put(SessionStatus.TESTED, 5);
        minExportSessionsByStatus.put(SessionStatus.REVOKED, 2);
//        minExportSessionsByStatus.put(SessionStatus.TESTING ,1)  //check null
        minExportSessionsByStatus.put(SessionStatus.CORRUPTED,1 );
        minExportSessionsByStatus.put(SessionStatus.FOR_SPECIAL_LOAD, 1);
        minExportSessionsByStatus.put(SessionStatus.DEFAULT, 1);
        sessions.add("20180102_101010");
        sessions.add("20180202_101010");
        sessions.add("20180202_101000");
        sessions.add("20170102_010000");
        sessions.add("20160102_101010");
        sessions.add("20161022_111111");
        sessions.add("20161023_111111");
        sessions.sort(String::compareTo);
        cleaner.setMinExportSessionsByStatus(minExportSessionsByStatus);


        ClearableSessionManager mockSessionManager = Mockito.mock(ClearableSessionManager.class);
        when(mockSessionManager.getClearableSessions(GENERATION, SessionStatus.NEW))
            .thenReturn(makeSessions(GENERATION, SessionStatus.NEW));
        when(mockSessionManager.getClearableSessions(GENERATION, SessionStatus.OK))
            .thenReturn(makeSessions(GENERATION, SessionStatus.OK));
        when(mockSessionManager.getClearableSessions(GENERATION, SessionStatus.FAIL))
            .thenReturn(makeSessions(GENERATION, SessionStatus.FAIL));
        when(mockSessionManager.getClearableSessions(GENERATION, SessionStatus.TESTED))
            .thenReturn(makeSessions(GENERATION, SessionStatus.TESTED));
        when(mockSessionManager.getClearableSessions(GENERATION, SessionStatus.TESTING))
            .thenReturn(makeSessions(GENERATION, SessionStatus.TESTING));

        cleaner.setSessionManager(mockSessionManager);
    }

    private List<SessionInfo> makeSessions(String type, SessionStatus status) {
        List<SessionInfo> sessionInfos = new ArrayList<>();
        for (String session : sessions) {
            sessionInfos.add(new SessionInfo(session, type, status, null));
        }
        return sessionInfos;
    }

    @Test
    public void getSessionToDeleteOnCluster() {
        List<SessionInfo> sessionToDeleteOnCluster = cleaner.getSessionToDeleteOnCluster(GENERATION);
        assertEquals(6, sessionToDeleteOnCluster.stream()
            .filter(s -> s.getSessionStatus().equals(SessionStatus.NEW)).count());
        assertEquals(4, sessionToDeleteOnCluster.stream()
            .filter(s -> s.getSessionStatus().equals(SessionStatus.OK)).count());
        assertEquals(2, sessionToDeleteOnCluster.stream()
            .filter(s -> s.getSessionStatus().equals(SessionStatus.TESTED)).count());
        assertEquals(6, sessionToDeleteOnCluster.stream()
            .filter(s -> s.getSessionStatus().equals(SessionStatus.FAIL)).count());
        assertEquals(6, sessionToDeleteOnCluster.stream()
            .filter(s -> s.getSessionStatus().equals(SessionStatus.TESTING)).count());
    }
}