package ru.yandex.market.abo.cpa.pushapi.pinger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.cpa.pinger.model.PingerSchedule;
import ru.yandex.market.abo.cpa.pinger.model.PingerScheduleRepo;
import ru.yandex.market.abo.cpa.pinger.model.PingerState;
import ru.yandex.market.abo.cpa.pushapi.PushApiLogRepo;
import ru.yandex.market.abo.cpa.pushapi.PushApiMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.abo.cpa.pushapi.stat.PushApiLogStatRepoTest.initLog;

/**
 * @author artemmz
 * @date 14/10/2019.
 */
class PingerStatServiceTest extends EmptyTest {
    private static final LocalDateTime HOUR_AGO = LocalDateTime.now().minusHours(1);
    private static final LocalDateTime DAY_AGO = LocalDateTime.now().minusDays(1);

    private static final long SHOP_ID = RND.nextLong();;
    private static final int LIMIT = 10;

    @Autowired
    private PingerStatService pingerStatService;
    @Autowired
    private PushApiLogRepo pushApiLogRepo;
    @Autowired
    private PingerScheduleRepo pingerScheduleRepo;

    @ParameterizedTest
    @CsvSource({
            (LIMIT - 1) + ", true,, false",
            LIMIT + ", true,, true",
            LIMIT + ", false,, false",
            (LIMIT + 5) + ", true, 2, false"
    })
    void cntConsequentSuccess(int reqCnt, boolean methodConsequentSuccess, Integer hoursOld, boolean totalSuccess) {
        assertFalse(pingerStatService.cntConsequentSuccess(SHOP_ID, HOUR_AGO, LIMIT));
        IntStream.range(0, reqCnt)
                .mapToObj(i -> initLog(
                        LocalDateTime.now().minusHours(hoursOld != null ? hoursOld : 0),
                        SHOP_ID,
                        methodConsequentSuccess || i % 2 > 0,
                        PushApiMethod.CART)).forEach(pushApiLogRepo::save);
        assertEquals(totalSuccess, pingerStatService.cntConsequentSuccess(SHOP_ID, HOUR_AGO, LIMIT));
    }

    @Test
    void getStats() {
        var shopId = RND.nextLong();
        PingerSchedule schedule = new PingerSchedule(shopId, PingerState.PING);
        pingerScheduleRepo.save(schedule);

        Map<Long, PingerStat> pingerStats = pingerStatService.getStats();
        assertEquals(1, pingerStats.size());
        PingerStat stat = pingerStats.get(shopId);
        assertEquals(0, stat.getCnt());

        pushApiLogRepo.saveAll(List.of(
                initLog(LocalDateTime.now(), shopId, true, PushApiMethod.CART),
                initLog(DAY_AGO, shopId, true, PushApiMethod.CART),
                initLog(LocalDateTime.now(), shopId, false, PushApiMethod.CART)
        ));

        pingerStats = pingerStatService.getStats();
        assertEquals(1, pingerStats.size());
        stat = pingerStats.get(shopId);

        assertEquals(2, stat.getCnt());
        assertEquals(1, stat.getCntSuccess());
        assertEquals(1, stat.getCntUsersSuccess());
    }

    @Test
    void getAcceptStats() {
        assertTrue(pingerStatService.pingerAcceptStats().isEmpty());

        var shopId = RND.nextLong();
        pushApiLogRepo.saveAll(List.of(
                initLog(LocalDateTime.now().minusMinutes(16), shopId, false, PushApiMethod.ORDER_ACCEPT),
                initLog(LocalDateTime.now(), shopId, false, PushApiMethod.ORDER_ACCEPT)
        ));

        PingerAcceptStat stat = pingerStatService.pingerAcceptStats().get(0);
        assertEquals(shopId, stat.getShopId());

        pushApiLogRepo.save(
                initLog(LocalDateTime.now(), shopId, true, PushApiMethod.ORDER_ACCEPT)
        );
        assertTrue(pingerStatService.pingerAcceptStats().isEmpty());
    }
}
