package ru.yandex.market.abo.cpa.pinger;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.cpa.pinger.model.PingerSchedule;
import ru.yandex.market.abo.cpa.pinger.model.PingerState;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author artemmz
 * @date 13/03/2020.
 */
class PingerConcurrentJobsTest extends EmptyTest {
    private static final long SHOP_ID = 1231254L;

    @Autowired
    PingerScheduleService scheduleService;

    @Test
    void pingAndUpdateConcurrently() {
        PingerSchedule sch = new PingerSchedule(SHOP_ID, PingerState.PING);
        scheduleService.save(List.of(sch));
        PingerSchedule schForPing = new PingerSchedule(SHOP_ID, PingerState.PING);

        // делаем в один поток, покуда сложно заставить в тестах нормально работать PersistenceContext в нескольких потоках
        ping(schForPing);
        updateState(sch);
        updateState(sch);
        ping(schForPing);

        PingerSchedule saved = scheduleService.load(SHOP_ID);
        assertEquals(sch.getState(), saved.getState());
        assertEquals(sch.getStartTime(), saved.getStartTime());
        assertEquals(schForPing.getFiredTime(), saved.getFiredTime());
        assertEquals(
                scheduleService.loadActive().get(0).getShopId(),
                scheduleService.loadActiveForPing().get(0).getShopId()
        );
    }

    private void ping(PingerSchedule schForPing) {
        schForPing.setFiredTime(new Date());
        scheduleService.updateAfterPing(List.of(schForPing));
        flushAndClear();
    }

    private void updateState(PingerSchedule sch) {
        sch.setState(PingerState.CONTROL_PING);
        sch.setStartTime(new Date());
        scheduleService.save(List.of(sch));
    }

}
