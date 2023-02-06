package ru.yandex.market.core.cutoff.listener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.api.cpa.CPAPlacementService;
import ru.yandex.market.core.abo.ModerationQueueService;
import ru.yandex.market.core.cutoff.model.CutoffInfo;
import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.core.moderation.event.EntityType;
import ru.yandex.market.core.moderation.event.EventSubtype;
import ru.yandex.market.core.moderation.event.EventType;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit тесты для  {@link AboCpaCutoffListener}.
 *
 * @author avetokhin 13/03/18.
 */
@RunWith(MockitoJUnitRunner.class)
public class AboCpaCutoffListenerTest {

    private static final long ACTION_ID = 100500;
    private static final long DS_READY = 1;
    private static final long DS_NOT_READY = 2;
    private static final CutoffInfo CUTOFF_READY = new CutoffInfo(-1, DS_READY, CutoffType.CPA_GENERAL, null, null);
    private static final CutoffInfo CUTOFF_NOT_READY = new CutoffInfo(-2, DS_NOT_READY, CutoffType.CPA_GENERAL, null, null);

    private static final CutoffInfo CPC_CUTOFF = new CutoffInfo(-3, DS_READY, CutoffType.CPC_PARTNER, null, null);

    @Mock
    private ModerationQueueService moderationQueueService;

    @Mock
    private CPAPlacementService cpaPlacementService;

    private AboCpaCutoffListener listener;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        when(cpaPlacementService.isCpaReady(DS_READY)).thenReturn(true);
        listener = new AboCpaCutoffListener(moderationQueueService, cpaPlacementService);
    }

    @Test
    public void onOpenFirstAndReady() {
        doOpen(CUTOFF_READY, true);
        verify(moderationQueueService).addEvent(ACTION_ID, DS_READY, EntityType.SHOP,
                EventType.CPA_SHOP_PLACEMENT_STATUS_CHANGED, EventSubtype.CPA_SHOP_SWITCHED_OFF);
    }

    @Test
    public void onOpenFirstAndNotReady() {
        verifyOnOpenNegative(CUTOFF_NOT_READY, true);
    }

    @Test
    public void onOpenNotFirstAndNotReady() {
        verifyOnOpenNegative(CUTOFF_NOT_READY, false);
    }

    @Test
    public void onOpenNotFirstAndReady() {
        verifyOnOpenNegative(CUTOFF_READY, false);
    }

    @Test
    public void onOpenCPC() {
        listener.onOpen(CPC_CUTOFF, ACTION_ID, true);
        listener.onOpen(CPC_CUTOFF, ACTION_ID, false);
        verifyZeroInteractions(cpaPlacementService);
        verify(moderationQueueService).addEvent(ACTION_ID, DS_READY, EntityType.SHOP,
                EventType.CPC_SHOP_PLACEMENT_STATUS_CHANGED, EventSubtype.CPC_SHOP_SWITCHED_OFF);
    }

    @Test
    public void onCloseLastAndReady() {
        doClose(CUTOFF_READY, true);
        verify(moderationQueueService).addEvent(ACTION_ID, DS_READY, EntityType.SHOP,
                EventType.CPA_SHOP_PLACEMENT_STATUS_CHANGED, EventSubtype.CPA_SHOP_SWITCHED_ON);
    }

    @Test
    public void onCloseLastAndNotReady() {
        verifyOnCloseNegative(CUTOFF_NOT_READY, true);
    }

    @Test
    public void onCloseNotLastAndNotReady() {
        verifyOnCloseNegative(CUTOFF_NOT_READY, false);
    }

    @Test
    public void onCloseNotLastAndReady() {
        verifyOnCloseNegative(CUTOFF_READY, false);
    }

    @Test
    public void onCloseCPC() {
        listener.onClose(CPC_CUTOFF, ACTION_ID, true);
        listener.onClose(CPC_CUTOFF, ACTION_ID, false);
        verifyZeroInteractions(cpaPlacementService);
        verify(moderationQueueService).addEvent(ACTION_ID, DS_READY, EntityType.SHOP,
                EventType.CPC_SHOP_PLACEMENT_STATUS_CHANGED, EventSubtype.CPC_SHOP_SWITCHED_ON);
    }

    private void verifyOnOpenNegative(CutoffInfo cutoffInfo, boolean firstDsCutoff) {
        doOpen(cutoffInfo, firstDsCutoff);
        verifyZeroInteractions(moderationQueueService);
    }

    private void doOpen(CutoffInfo cutoffInfo, boolean firstDsCutoff) {
        listener.onOpen(cutoffInfo, ACTION_ID, firstDsCutoff);
        verify(cpaPlacementService).isCpaReady(cutoffInfo.getDatasourceId());
    }

    private void verifyOnCloseNegative(CutoffInfo cutoffInfo, boolean lastDsCutoff) {
        doClose(cutoffInfo, lastDsCutoff);
        verifyZeroInteractions(moderationQueueService);
    }

    private void doClose(CutoffInfo cutoffInfo, boolean lastDsCutoff) {
        listener.onClose(cutoffInfo, ACTION_ID, lastDsCutoff);
        verify(cpaPlacementService).isCpaReady(cutoffInfo.getDatasourceId());
    }

}
