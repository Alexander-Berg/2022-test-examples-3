package ru.yandex.market.abo.cpa.pinger.task;

import java.util.Date;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.cpa.pinger.model.PingerSchedule;
import ru.yandex.market.abo.cpa.pinger.model.PingerState;
import ru.yandex.market.abo.cpa.pinger.task.pool.AccessCallableScheduledThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author artemmz
 * @date 19/02/2020.
 */
public class PingerTaskServiceTest {

    @InjectMocks
    PingerTaskService pingerTaskService;
    @Mock
    AccessCallableScheduledThreadPoolExecutor executor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @SuppressWarnings("unchecked")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void pingIfNeed(boolean needPing) {
        PingerSchedule schedule = new PingerSchedule(1L, needPing ? PingerState.PING : PingerState.NO_PING);
        schedule.setFiredTime(new Date(0));
        PingerSchedule pinged = pingerTaskService.pingIfNeed(schedule, 10);

        assertEquals(needPing, pinged != null);
        verify(executor, times(needPing ? 1 : 0)).submit(any(Callable.class));
    }
}
