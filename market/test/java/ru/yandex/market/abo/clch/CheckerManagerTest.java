package ru.yandex.market.abo.clch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.EmptyTestWithTransactionTemplate;
import ru.yandex.common.util.collections.CollectionFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 21.06.18.
 */
public class CheckerManagerTest extends EmptyTestWithTransactionTemplate {
    private static final long USER_ID = -1L;
    private static final long SHOP_SET = 1L;
    private static final long SESSION_ID = 0L;
    private static final long NEW_SESSION_ID = 11L;
    private static final ClchSourceWithUser CLCH_SOURCE = new ManualClchSource(USER_ID);

    @InjectMocks
    private CheckerManager checkerManager;
    @Mock
    private ClchService clchService;
    @Mock
    private CheckerAnalyzer checkerAnalyzer;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(clchService.getOrCreateShopSet(anySet())).thenReturn(SHOP_SET);
    }

    @Test
    public void createToRefresh_already_exists() {
        when(clchService.getSessionBySetId(SHOP_SET)).thenReturn(SESSION_ID);

        long sessionId = checkerManager.createSessionWithDelayedRefresh(new ManualClchSource(-1L),
                CollectionFactory.set(1L, 2L, 3L));
        assertEquals(SESSION_ID, sessionId);
        verify(checkerAnalyzer).eraseCurrentSameness(SESSION_ID);
        verify(clchService).updateSessionStatus(SESSION_ID, ClchSessionStatus.REFRESH_LATER);
        verify(clchService, never()).saveSession(any(), anyLong(), any());
    }

    @Test
    public void createToRefresh_not_exists() {
        when(clchService.getSessionBySetId(SHOP_SET)).thenReturn(null);
        when(clchService.saveSession(eq(CLCH_SOURCE), eq(SHOP_SET), any())).thenReturn(NEW_SESSION_ID);

        long sessionId = checkerManager.createSessionWithDelayedRefresh(CLCH_SOURCE, CollectionFactory.set(1L, 2L, 3L));
        assertEquals(NEW_SESSION_ID, sessionId);
        verify(clchService).saveSession(eq(CLCH_SOURCE), eq(SHOP_SET), any());
        verify(clchService).updateSessionStatus(NEW_SESSION_ID, ClchSessionStatus.CHECK_LATER);
    }
}
