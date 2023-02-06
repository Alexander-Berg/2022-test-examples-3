package ru.yandex.market.abo.cpa.pinger;

import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.cutoff.feature.FeatureCutoff;
import ru.yandex.market.abo.core.partner.info.PartnerInfoService;
import ru.yandex.market.abo.core.partner.info.model.PartnerBaseInfo;
import ru.yandex.market.abo.core.partner.info.model.PartnerType;
import ru.yandex.market.abo.cpa.pinger.model.PingerMethod;
import ru.yandex.market.abo.cpa.pinger.model.PingerSchedule;
import ru.yandex.market.abo.cpa.pinger.model.PingerState;
import ru.yandex.market.abo.cpa.pushapi.pinger.PingerStat;
import ru.yandex.market.abo.cpa.pushapi.pinger.PingerStatService;
import ru.yandex.market.abo.util.db.toggle.ToggleService;
import ru.yandex.market.core.feature.model.FeatureType;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 03/03/2020.
 */
class PingerStateChangerTest {
    private static final Long SHOP_ID = 213242323L;
    private static final PingerMethod PING_METHOD = PingerMethod.CART;
    private static final int TOTAL_CNT = PingerStateChanger.ABSOLUTE_LIMIT_OFF * 100;

    @InjectMocks
    PingerStateChanger stateChanger;
    @Mock
    PingerCutoffService pingerCutoffService;
    @Mock
    PartnerInfoService partnerInfoService;
    @Mock
    PingerStatService statService;
    @Mock
    ToggleService toggleService;
    @Mock
    PingerShopsProvider pingerShopsProvider;

    @Mock
    PingerSchedule schedule;
    @Mock
    PingerStat stat;
    @Mock
    FeatureCutoff featureCutoff;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(schedule.getShopId()).thenReturn(SHOP_ID);
        when(schedule.getMethod()).thenReturn(PING_METHOD);
        when(schedule.getStartTime()).thenReturn(new Date());

        when(stat.getShopId()).thenReturn(SHOP_ID);
        when(stat.getCnt()).thenReturn(PingerStateChangerTest.TOTAL_CNT);

        when(pingerCutoffService.closeCutoff(SHOP_ID)).thenReturn(true);
        when(pingerCutoffService.openCutoff(SHOP_ID)).thenReturn(true);
        when(pingerCutoffService.lastPingerCutoffs(FeatureType.DROPSHIP)).thenReturn(List.of(featureCutoff));
        PartnerBaseInfo partnerInfo = new PartnerBaseInfo(1L, "name", PartnerType.SUPPLIER);
        when(partnerInfoService.loadPartnerBaseInfo(anyLong())).thenReturn(partnerInfo);

        when(pingerShopsProvider.shopHasNoCutoffsPreventingPinger(anyLong())).thenReturn(true);
    }

    @ParameterizedTest
    @EnumSource(value = PingerState.class, mode = EnumSource.Mode.MATCH_ALL)
    void badStat(PingerState state) {
        when(schedule.getState()).thenReturn(state);
        when(stat.getCntSuccess()).thenReturn(0);
        when(stat.getCntUsersSuccess()).thenReturn(0);

        switch (state) {
            case PING:
            case FREQUENT_PING:
            case SKIP_PING: {
                PingerSchedule changed = stateChanger.updateStateIfNeed(schedule, stat, false);
                verify(pingerCutoffService).openCutoff(SHOP_ID);
                verify(changed).setState(PingerState.CONTROL_PING);
                break;
            }
            case NO_PING:
            case CONTROL_PING: {
                PingerSchedule changed = stateChanger.updateStateIfNeed(schedule, stat, false);
                assertNull(changed);
                break;
            }
            case CHECK_PING: {
                PingerSchedule changed = stateChanger.updateStateIfNeed(schedule, stat, true);
                verify(changed).setState(PingerState.CONTROL_PING);
                break;
            }
        }
    }

    @ParameterizedTest
    @EnumSource(value = PingerState.class, mode = EnumSource.Mode.MATCH_ALL)
    void goodStat(PingerState state) {
        when(schedule.getState()).thenReturn(state);
        when(stat.getCntSuccess()).thenReturn(TOTAL_CNT);
        when(stat.getCntUsersSuccess()).thenReturn(0);
        when(statService.cntConsequentSuccess(eq(SHOP_ID), any(), anyInt())).thenReturn(true);

        switch (state) {
            case PING:
            case NO_PING: {
                PingerSchedule changed = stateChanger.updateStateIfNeed(schedule, stat, false);
                assertNull(changed);
                break;
            }
            case FREQUENT_PING:
            case SKIP_PING: {
                PingerSchedule changed = stateChanger.updateStateIfNeed(schedule, stat, false);
                verify(changed).setState(PingerState.PING);
                break;
            }
            case CONTROL_PING: {
                PingerSchedule changed = stateChanger.updateStateIfNeed(schedule, stat, true);
                verify(changed).setState(PingerState.CHECK_PING);
                break;
            }
            case CHECK_PING: {
                PingerSchedule changed = stateChanger.updateStateIfNeed(schedule, stat, true);
                verify(pingerCutoffService).closeCutoff(SHOP_ID);
                verify(changed).setState(PingerState.PING);
                break;
            }
        }
    }

    @ParameterizedTest
    @EnumSource(value = PingerState.class, mode = EnumSource.Mode.MATCH_ALL)
    void okStat(PingerState state) {
        when(schedule.getState()).thenReturn(state);
        when(stat.getCntSuccess()).thenReturn(TOTAL_CNT - 1);
        when(stat.getCntUsersSuccess()).thenReturn(0);
        when(statService.cntConsequentSuccess(eq(SHOP_ID), any(), anyInt())).thenReturn(true);

        switch (state) {
            case PING:
            case SKIP_PING: {
                PingerSchedule changed = stateChanger.updateStateIfNeed(schedule, stat, false);
                verify(changed).setState(PingerState.FREQUENT_PING);
                break;
            }
            case FREQUENT_PING: {
                PingerSchedule changed = stateChanger.updateStateIfNeed(schedule, stat, false);
                verify(changed).setState(PingerState.PING);
                break;
            }
            case CONTROL_PING: {
                PingerSchedule changed = stateChanger.updateStateIfNeed(schedule, stat, true);
                verify(changed).setState(PingerState.CHECK_PING);
                break;
            }
            case CHECK_PING: {
                PingerSchedule changed = stateChanger.updateStateIfNeed(schedule, stat, true);
                verify(pingerCutoffService).closeCutoff(SHOP_ID);
                verify(changed).setState(PingerState.PING);
                break;
            }
            case NO_PING: {
                PingerSchedule changed = stateChanger.updateStateIfNeed(schedule, stat, false);
                assertNull(changed);
                break;
            }
        }
    }

    @Test
    void fromPingToSkipPing() {
        when(toggleService.configEnabledInProduction(any())).thenReturn(true);
        when(schedule.getState()).thenReturn(PingerState.PING);
        when(stat.getCntSuccess()).thenReturn(TOTAL_CNT);
        when(stat.getCntUsersSuccess()).thenReturn(1);

        PingerSchedule changed = stateChanger.updateStateIfNeed(schedule, stat, false);
        verify(changed).setState(PingerState.SKIP_PING);
    }

    @Test
    void fromSkipPingToPing() {
        when(schedule.getState()).thenReturn(PingerState.SKIP_PING);
        when(stat.getCntSuccess()).thenReturn(TOTAL_CNT);
        when(stat.getCntUsersSuccess()).thenReturn(0);

        PingerSchedule changed = stateChanger.updateStateIfNeed(schedule, stat, false);
        verify(changed).setState(PingerState.PING);
    }

    @ParameterizedTest
    @MethodSource("incorrectStateTestMethodSource")
    void incorrectStateBadStat(PingerState state, boolean hasPingerCutoff) {
        when(schedule.getState()).thenReturn(state);
        when(stat.getCntSuccess()).thenReturn(0);
        when(stat.getCntUsersSuccess()).thenReturn(0);

        PingerSchedule changed = stateChanger.updateStateIfNeed(schedule, stat, hasPingerCutoff);

        switch (state) {
            case PING:
            case FREQUENT_PING:
            case SKIP_PING:
            case CHECK_PING: {
                assertNotNull(changed);
                verify(changed).setState(PingerState.CONTROL_PING);
                break;
            }
            case NO_PING: {
                if (hasPingerCutoff) {
                    assertNotNull(changed);
                    verify(changed).setState(PingerState.CONTROL_PING);
                } else {
                    assertNull(changed);
                }
                break;
            }
            case CONTROL_PING: {
                assertNull(changed);
                break;
            }
        }
    }

    @ParameterizedTest
    @MethodSource("incorrectStateTestMethodSource")
    void incorrectStateGoodStat(PingerState state, boolean hasPingerCutoff) {
        when(schedule.getState()).thenReturn(state);
        when(stat.getCntSuccess()).thenReturn(TOTAL_CNT);
        when(stat.getCntUsersSuccess()).thenReturn(0);
        when(statService.cntConsequentSuccess(eq(SHOP_ID), any(), anyInt())).thenReturn(true);

        PingerSchedule changed = stateChanger.updateStateIfNeed(schedule, stat, hasPingerCutoff);

        if (hasPingerCutoff) {
            switch (state) {
                case PING:
                case FREQUENT_PING:
                case SKIP_PING:
                case NO_PING:
                case CONTROL_PING: {
                    assertNotNull(changed);
                    verify(changed).setState(PingerState.CHECK_PING);
                    break;
                }
                case CHECK_PING: {
                    assertNotNull(changed);
                    verify(changed).setState(PingerState.PING);
                    break;
                }
            }
        } else {
            switch (state) {
                case PING:
                case NO_PING: {
                    assertNull(changed);
                    break;
                }
                case FREQUENT_PING:
                case SKIP_PING:
                case CONTROL_PING:
                case CHECK_PING: {
                    assertNotNull(changed);
                    verify(changed).setState(PingerState.PING);
                    break;
                }
            }
        }
    }

    static Stream<Arguments> incorrectStateTestMethodSource() {
        return StreamEx.of(PingerState.values())
                .cross(List.of(true, false))
                .mapKeyValue(Arguments::of);
    }

}
