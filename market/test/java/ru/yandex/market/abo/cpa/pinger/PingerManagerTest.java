package ru.yandex.market.abo.cpa.pinger;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.cutoff.feature.FeatureCutoff;
import ru.yandex.market.abo.core.exception.ExceptionalShopsService;
import ru.yandex.market.abo.cpa.pinger.model.PingerSchedule;
import ru.yandex.market.abo.cpa.pinger.model.PingerState;
import ru.yandex.market.abo.cpa.pushapi.pinger.PingerStat;
import ru.yandex.market.abo.cpa.pushapi.pinger.PingerStatService;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.param.model.ParamCheckStatus;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 19/02/2020.
 */
class PingerManagerTest {
    private static final Long SHOP_ID = 2123L;

    @InjectMocks
    PingerManager pingerManager;

    @Mock
    ExceptionalShopsService exceptionalShopsService;
    @Mock
    PingerScheduleService scheduleService;
    @Mock
    PingerShopsProvider pingerShopsProvider;
    @Mock
    PingerStateChanger stateChanger;
    @Mock
    PingerCutoffService pingerCutoffService;
    @Mock
    PingerStatService pingerStatService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(exceptionalShopsService.loadShopsNonCached(PingerManager.NO_PING_EXCEPTION)).thenReturn(Set.of());
    }

    @Test
    void startPing() {
        when(pingerShopsProvider.getShopsToPing()).thenReturn(Set.of(SHOP_ID));
        when(scheduleService.loadActive()).thenReturn(new ArrayList<>());

        pingerManager.startStopPing();
        verify(scheduleService).save(List.of(new PingerSchedule(SHOP_ID, PingerState.PING)));
    }

    @Test
    void alreadyStarted() {
        when(pingerShopsProvider.getShopsToPing()).thenReturn(Set.of(SHOP_ID));
        when(scheduleService.loadActive()).thenReturn(List.of(new PingerSchedule(SHOP_ID, PingerState.PING)));

        pingerManager.startStopPing();
        verify(scheduleService, never()).save(List.of(new PingerSchedule(SHOP_ID, PingerState.PING)));
    }

    @Test
    void stopPing() {
        when(pingerShopsProvider.getShopsToPing()).thenReturn(Set.of());
        when(scheduleService.loadActive()).thenReturn(List.of(new PingerSchedule(SHOP_ID, PingerState.PING)));

        pingerManager.startStopPing();
        verify(scheduleService).save(List.of(new PingerSchedule(SHOP_ID, PingerState.NO_PING)));
    }

    @Test
    void stopPingCauseTimeoutPassed() {
        FeatureCutoff featureCutoff = new FeatureCutoff();
        featureCutoff.setShopId(SHOP_ID);
        featureCutoff.setCreationTime(LocalDateTime.now().minusDays(PingerManager.CUTOFF_DATE_LIMIT + 1));
        featureCutoff.setStatus(ParamCheckStatus.FAIL);
        when(pingerCutoffService.lastPingerCutoffs(FeatureType.DROPSHIP)).thenReturn(List.of(featureCutoff));
        when(scheduleService.loadActive()).thenReturn(List.of(new PingerSchedule(SHOP_ID, PingerState.PING)));

        pingerManager.stopPingLongFeatureCutoff();
        verify(scheduleService).save(List.of(new PingerSchedule(SHOP_ID, PingerState.NO_PING)));
    }

    @Test
    void stopPingCauseExceptional() {
        when(exceptionalShopsService.loadShopsNonCached(PingerManager.NO_PING_EXCEPTION)).thenReturn(Set.of(SHOP_ID));
        when(pingerShopsProvider.getShopsToPing()).thenReturn(Set.of(SHOP_ID));
        when(scheduleService.loadActive()).thenReturn(List.of(new PingerSchedule(SHOP_ID, PingerState.PING)));

        pingerManager.startStopPing();
        verify(scheduleService).save(List.of(new PingerSchedule(SHOP_ID, PingerState.NO_PING)));
    }

    @ParameterizedTest
    @CsvSource({"true, true", "true, false", "false, true", "false, false"})
    void updatePingState(boolean hasStats, boolean updated) {
        PingerStat pingerStat = mock(PingerStat.class);
        PingerSchedule sch = new PingerSchedule(SHOP_ID, PingerState.PING);
        when(scheduleService.loadActive()).thenReturn(List.of(sch));
        when(pingerStatService.getStats()).thenReturn(hasStats ? Map.of(SHOP_ID, pingerStat) : Collections.emptyMap());
        when(stateChanger.updateStateIfNeed(eq(sch), eq(pingerStat), eq(false))).thenReturn(updated ? sch : null);
        when(pingerCutoffService.getShopsWithOpenedPingerCutoffs()).thenReturn(Set.of());

        pingerManager.updatePingState();
        verify(scheduleService, times(hasStats & updated ? 1 : 0)).save(List.of(sch));
    }

}
