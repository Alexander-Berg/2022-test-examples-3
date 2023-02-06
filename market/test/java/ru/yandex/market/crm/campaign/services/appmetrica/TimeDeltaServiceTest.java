package ru.yandex.market.crm.campaign.services.appmetrica;

import java.time.LocalTime;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author zloddey
 */
public class TimeDeltaServiceTest {
    private static final int OFFSET_TOKYO = -21600;
    private static final int OFFSET_SAMARA = -3600;
    private static final int OFFSET_MOSCOW = 0;
    private static final int OFFSET_VIENNA = 3600;

    private TimeDeltaService timeDeltaService;
    private LocalTime finishTime;

    @Test
    public void noFinishTimeMeansNoTTLIsSet() {
        timeDeltaService = new TimeDeltaService(new FakeTimeProvider(2020, 9, 2, 16, 0));
        assertNull(timeDeltaService.countTimeToLive(OFFSET_MOSCOW, null));
        assertNull(timeDeltaService.countTimeToLive(OFFSET_TOKYO, null));
        assertNull(timeDeltaService.countTimeToLive(OFFSET_SAMARA, null));
        assertNull(timeDeltaService.countTimeToLive(OFFSET_VIENNA, null));
    }

    @Test
    public void allZonesLieWithinLimit() {
        timeDeltaService = new TimeDeltaService(new FakeTimeProvider(2020, 9, 2, 12, 0));
        finishTime = LocalTime.of(22, 0);
        assertEquals(35400, countTTL(OFFSET_MOSCOW));
        assertEquals(13800, countTTL(OFFSET_TOKYO));
        assertEquals(31800, countTTL(OFFSET_SAMARA));
        assertEquals(39000, countTTL(OFFSET_VIENNA));
    }

    @Test
    public void someZonesLieOutsideTheLimit() {
        timeDeltaService = new TimeDeltaService(new FakeTimeProvider(2020, 9, 2, 21, 30));
        finishTime = LocalTime.of(22, 0);
        assertEquals(1200, countTTL(OFFSET_MOSCOW));
        assertEquals(-20400, countTTL(OFFSET_TOKYO));
        assertEquals(-2400, countTTL(OFFSET_SAMARA));
        assertEquals(4800, countTTL(OFFSET_VIENNA));
    }

    private int countTTL(int deviceOffset) {
        return timeDeltaService.countTimeToLive(deviceOffset, finishTime);
    }
}
