package ru.yandex.market.abo.cpa.pushapi.stat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.cpa.pushapi.PushApiLog;
import ru.yandex.market.abo.cpa.pushapi.PushApiLogRepo;
import ru.yandex.market.abo.cpa.pushapi.PushApiMethod;
import ru.yandex.market.checkout.checkouter.order.Context;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author artemmz
 * @date 03/10/2019.
 */
public class PushApiLogStatRepoTest extends EmptyTest {
    private static final long SHOP_ID_1 = 110000L;
    private static final long SHOP_ID_2 = 1210000L;
    @Autowired
    private PushApiLogRepo pushApiLogRepo;
    @Autowired
    private PushApiLogStatRepo pushApiLogStatRepo;

    @Test
    void calculate() {
        LocalDateTime now = LocalDateTime.now();
        List<PushApiLog> pushApiLogs = List.of(
                initLog(now.minusDays(1), SHOP_ID_1, true, PushApiMethod.CART),
                initLog(now.minusDays(1), SHOP_ID_1, false, PushApiMethod.CART),

                initLog(now.minusDays(1), SHOP_ID_2, true, PushApiMethod.ORDER_ACCEPT),
                initLog(now.minusDays(2), SHOP_ID_2, true, PushApiMethod.ORDER_ACCEPT)
        );
        assertEquals(pushApiLogs.size(), pushApiLogRepo.saveAll(pushApiLogs).size());

        List<PushApiLogStat> stats = pushApiLogStatRepo.calculate(now.toLocalDate());
        assertEquals(3, stats.size());

        List<PushApiLogStat> shop1Stats = stats.stream()
                .filter(stat -> stat.getId().getShopId() == SHOP_ID_1)
                .collect(Collectors.toList());
        assertEquals(1, shop1Stats.size());
        assertEquals(2, shop1Stats.get(0).getCartCnt());
        assertEquals(1, shop1Stats.get(0).getCartCntSuccess());
        assertEquals(0, shop1Stats.get(0).getAcceptCnt());

        List<PushApiLogStat> shop2Stats = stats.stream()
                .filter(stat -> stat.getId().getShopId() == SHOP_ID_2)
                .collect(Collectors.toList());
        assertEquals(2, shop2Stats.size());
        assertEquals(1, shop2Stats.get(0).getAcceptCntSuccess());
        assertEquals(1, shop2Stats.get(0).getAcceptCnt());
        assertEquals(1, shop2Stats.get(1).getAcceptCnt());
        assertEquals(0, shop2Stats.get(1).getStocksCnt());
    }

    public static PushApiLog initLog(LocalDateTime eventTime, long shopId, boolean success, PushApiMethod method) {
        PushApiLog log = new PushApiLog();
        log.setRequestId(RND.nextLong() + "_req_id");
        log.setEventTime(eventTime);
        log.setShopId(shopId);
        log.setSuccess(success);
        log.setRequestMethod(method.getUrl());
        log.setContext(Context.MARKET);
        return log;
    }
}
