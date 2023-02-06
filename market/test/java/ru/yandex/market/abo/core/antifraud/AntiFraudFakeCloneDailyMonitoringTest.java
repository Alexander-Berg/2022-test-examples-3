package ru.yandex.market.abo.core.antifraud;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.clch.CheckerManager;
import ru.yandex.market.abo.clch.ClchService;
import ru.yandex.market.abo.clch.ClchSessionSource;
import ru.yandex.market.abo.clch.model.TimeLimits;
import ru.yandex.market.abo.core.antifraud.model.AntiFraudCloneCheckResult;
import ru.yandex.market.abo.core.antifraud.model.AntiFraudCloneFeature;
import ru.yandex.market.abo.core.antifraud.service.AntiFraudCloneCheckResultService;
import ru.yandex.market.abo.core.antifraud.service.AntiFraudClonesStTicketService;
import ru.yandex.market.abo.core.antifraud.yt.YtAntiFraudDailyManager;
import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.util.db.ConfigurationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 30.06.2020
 */
class AntiFraudFakeCloneDailyMonitoringTest {

    private static final long SHOP_ID = 123L;
    private static final long CLONE_SHOP_ID = 124L;

    private static final Set<Long> SHOP_SET = Set.of(SHOP_ID, CLONE_SHOP_ID);
    private static final long SESSION_ID = 11111L;

    private static final double DISTANCE = 1.0;
    private static final Set<AntiFraudCloneFeature> CLONE_FEATURES = Set.of(AntiFraudCloneFeature.CLONE_IS_PLACED);

    private static final String LAST_ANTI_FRAUD_YT_TABLE = "2020-07-07";

    @InjectMocks
    private AntiFraudFakeCloneDailyMonitoring antiFraudFakeCloneDailyMonitoring;

    @Mock
    private YtAntiFraudDailyManager ytAntiFraudDailyManager;
    @Mock
    private AntiFraudClonesStTicketService antiFraudClonesStTicketService;
    @Mock
    private AntiFraudCloneCheckResultService antiFraudCloneCheckResultService;
    @Mock
    private CheckerManager checkerManager;
    @Mock
    private ClchService clchService;
    @Mock
    private ConfigurationService coreCounterService;

    @Mock
    private AntiFraudCloneCheckResult antiFraudCloneCheckResult;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        when(antiFraudClonesStTicketService.getCreatedTodayTicketsCountForFakeClone()).thenReturn(0);

        when(ytAntiFraudDailyManager.loadLastYtClonesTable()).thenReturn(LAST_ANTI_FRAUD_YT_TABLE);
        when(ytAntiFraudDailyManager.loadClonesForRegularCheck(LAST_ANTI_FRAUD_YT_TABLE, true))
                .thenReturn(List.of(antiFraudCloneCheckResult));

        when(antiFraudCloneCheckResult.getShopId()).thenReturn(SHOP_ID);
        when(antiFraudCloneCheckResult.getCloneShopId()).thenReturn(CLONE_SHOP_ID);
        when(antiFraudCloneCheckResult.getDistance()).thenReturn(DISTANCE);
        when(antiFraudCloneCheckResult.getCloneFeatures()).thenReturn(CLONE_FEATURES);
        when(antiFraudCloneCheckResult.getCloneCutoffs()).thenReturn(Set.of(CutoffType.QMANAGER_FRAUD));
    }

    @Test
    void doNotMonitorNotFakeClone() {
        when(antiFraudCloneCheckResult.getCloneCutoffs()).thenReturn(Set.of(CutoffType.QMANAGER_CLONE));

        antiFraudFakeCloneDailyMonitoring.monitor();
        verify(antiFraudClonesStTicketService, never()).createStTicketForFakeClone(anyLong(), any(AntiFraudCloneCheckResult.class));
        verifyNoMoreInteractions(antiFraudCloneCheckResultService);
    }

    @Test
    void monitorFakeCloneWhenPairChecked() {
        when(antiFraudClonesStTicketService.noNewTicketsForFakeClone(antiFraudCloneCheckResult)).thenReturn(false);

        antiFraudFakeCloneDailyMonitoring.monitor();

        verify(antiFraudClonesStTicketService, never()).createStTicketForFakeClone(anyLong(), any(AntiFraudCloneCheckResult.class));
        verify(checkerManager, never())
                .createDelayedSession(eq(ClchSessionSource.ANTI_FRAUD_FAKE_SHOPS), eq(SHOP_SET), any(TimeLimits.class));
    }

    @Test
    void monitorFakeCloneWhenPairNotChecked() {
        when(antiFraudClonesStTicketService.noNewTicketsForFakeClone(antiFraudCloneCheckResult)).thenReturn(true);
        when(checkerManager.createDelayedSession(
                eq(ClchSessionSource.ANTI_FRAUD_FAKE_SHOPS), eq(SHOP_SET), any(TimeLimits.class))
        ).thenReturn(SESSION_ID);

        when(clchService.getActiveShopIds(SHOP_SET)).thenReturn(SHOP_SET);

        antiFraudFakeCloneDailyMonitoring.monitor();

        verify(checkerManager).createDelayedSession(
                eq(ClchSessionSource.ANTI_FRAUD_FAKE_SHOPS), eq(SHOP_SET), any(TimeLimits.class)
        );
        verify(antiFraudClonesStTicketService).createStTicketForFakeClone(SESSION_ID, antiFraudCloneCheckResult);
    }
}
