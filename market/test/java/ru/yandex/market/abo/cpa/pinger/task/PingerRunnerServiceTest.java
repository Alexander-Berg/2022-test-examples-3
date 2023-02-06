package ru.yandex.market.abo.cpa.pinger.task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.cpa.pinger.PingerScheduleService;
import ru.yandex.market.abo.cpa.pinger.model.PingerMethod;
import ru.yandex.market.abo.cpa.pinger.model.PingerSchedule;
import ru.yandex.market.abo.cpa.pinger.model.PingerState;
import ru.yandex.market.util.db.ConfigurationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author imelnikov
 * @since 04.09.2020
 */
class PingerRunnerServiceTest {
    private static final long SHOP_ID = 213L;

    @InjectMocks
    PingerRunnerService pingerRunnerService;
    @Mock
    PingerTaskService pingerTaskService;
    @Mock
    PingerScheduleService scheduleService;
    @Mock
    ConfigurationService aboConfigurationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void checkPing() {
        PingerSchedule sch = new PingerSchedule(SHOP_ID, PingerState.PING);

        when(pingerTaskService.pingsAmountInProcess()).thenReturn(new HashMap<>());
        when(scheduleService.loadActiveForPing()).thenReturn(List.of(sch));
        when(pingerTaskService.pingIfNeed(sch, 1)).thenReturn(sch);

        pingerRunnerService.ping();
        verify(pingerTaskService).pingIfNeed(sch, 1);
        verify(scheduleService).updateAfterPing(List.of(sch));
    }

    @Test
    void nothingToPingNow() {
        when(pingerTaskService.pingsAmountInProcess()).thenReturn(new HashMap<>());
        when(scheduleService.loadActiveForPing()).thenReturn(List.of(new PingerSchedule(SHOP_ID, PingerState.PING)));
        when(pingerTaskService.pingIfNeed(any(), anyInt())).thenReturn(null);

        pingerRunnerService.ping();
        verify(pingerTaskService).pingIfNeed(any(), eq(1));
        verify(scheduleService).updateAfterPing(List.of());
    }

    @Test
    void getTasksNumberForActiveSchedules() {
        when(aboConfigurationService.getValueAsInt(anyInt())).thenReturn(10);
        when(pingerTaskService.pingsAmountInProcess()).thenReturn(Map.of(
                1L, Map.of(
                        PingerMethod.CART, 2,
                        PingerMethod.CHECKOUT, 3
                ),
                2L, Map.of(
                        PingerMethod.CART, 7,
                        PingerMethod.CHECKOUT, 1
                ),
                4L, Map.of(
                        PingerMethod.CHECKOUT, 1
                )
        ));

        PingerSchedule schedule1 = new PingerSchedule(1L, PingerState.PING);
        PingerSchedule schedule2 = new PingerSchedule(2L, PingerState.FREQUENT_PING);
        PingerSchedule schedule3 = new PingerSchedule(3L, PingerState.PING);
        PingerSchedule schedule4 = new PingerSchedule(4L, PingerState.FREQUENT_PING);

        when(scheduleService.loadActiveForPing()).thenReturn(List.of(schedule1, schedule2, schedule3, schedule4));

        assertThat(Map.of(schedule3, 1, schedule4, 10))
                .containsExactlyInAnyOrderEntriesOf(pingerRunnerService.getTasksNumberByActiveSchedules());
    }

}
