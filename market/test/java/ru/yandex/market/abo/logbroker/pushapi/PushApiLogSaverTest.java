package ru.yandex.market.abo.logbroker.pushapi;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.cpa.pushapi.PushApiLog;
import ru.yandex.market.abo.cpa.pushapi.PushApiLogRepo;
import ru.yandex.market.abo.cpa.pushapi.PushApiLogService;
import ru.yandex.market.abo.cpa.pushapi.PushApiMethod;
import ru.yandex.market.abo.cpa.pushapi.stat.PushApiLogStat;
import ru.yandex.market.abo.cpa.pushapi.stat.PushApiLogStatRepo;
import ru.yandex.market.abo.cpa.pushapi.stat.PushApiStatService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static ru.yandex.market.abo.cpa.pushapi.stat.PushApiLogStatRepoTest.initLog;
import static ru.yandex.market.abo.cpa.pushapi.stat.PushApiStatServiceTest.initStat;

/**
 * @author artemmz
 * @date 03/10/2019.
 */
class PushApiLogSaverTest extends EmptyTest {
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private PushApiStatService pushApiStatService;
    @Autowired
    private PushApiLogService pushApiLogService;
    @Autowired
    private PushApiLogRepo pushApiLogRepo;
    @Autowired
    private PushApiLogStatRepo pushApiLogStatRepo;

    private PushApiLogSaver pushApiLogSaver;

    /**
     * Need to force {@link EntityManager#clear()} after {@link PushApiStatService#calculate}
     * to emulate real behaviour in test.
     */
    @BeforeEach
    void setUp() {
        PushApiStatService logServiceSpy = spy(pushApiStatService);
        doAnswer(inv -> {
            List<PushApiLogStat> stats = pushApiStatService.calculate((LocalDate) inv.getArguments()[0]);
            flushAndClear();
            return stats;
        }).when(logServiceSpy).calculate(any(LocalDate.class));

        pushApiLogSaver = new PushApiLogSaver(logServiceSpy, pushApiLogService, transactionTemplate);
    }

    /**
     * logs older than today go to push_api_log_stat
     * from push_api_log.
     */
    @Test
    void testCompressLogs() {
        LocalDateTime coupleDaysAgo = LocalDateTime.now().minusDays(2);

        List<PushApiLog> apiLogs = Arrays.stream(PushApiMethod.values())
                .map(method -> initLog(coupleDaysAgo.minusDays(RND.nextInt(7)), RND.nextLong(), RND.nextBoolean(), method))
                .collect(Collectors.toList());
        apiLogs.add(initLog(LocalDateTime.now(), RND.nextLong(), RND.nextBoolean(), PushApiMethod.CART));

        pushApiStatService.saveRaw(apiLogs);
        assertEquals(apiLogs.size(), pushApiLogRepo.count());
        assertTrue(pushApiLogStatRepo.findAll().isEmpty());

        pushApiLogSaver.compressLogs();

        assertEquals(1, pushApiLogRepo.findAll().size());
        assertFalse(pushApiLogStatRepo.findAll().isEmpty());
    }

    @Test
    void testMergeStat() {
        LocalDate coupleDaysAgo = LocalDate.now().minusDays(2);
        PushApiLogStat stat = initStat(coupleDaysAgo, RND.nextLong(), 1L, 1L);
        pushApiStatService.save(stat);
        flushAndClear();

        List<PushApiLog> pushApiLogs = List.of(
                initLog(coupleDaysAgo.atStartOfDay(), stat.getId().getShopId(), true, PushApiMethod.CART),
                initLog(coupleDaysAgo.atStartOfDay(), stat.getId().getShopId(), false, PushApiMethod.ORDER_ACCEPT)
        );
        pushApiStatService.saveRaw(pushApiLogs);

        pushApiLogSaver.compressLogs();

        assertTrue(pushApiLogRepo.findAll().isEmpty());
        PushApiLogStat mergedStat = pushApiStatService.findBy(stat.getId()).orElseThrow();
        assertEquals(stat.getTotalCnt() + 2, mergedStat.getTotalCnt());
        assertEquals(stat.getTotalCntSuccess() + 1, mergedStat.getTotalCntSuccess());
        assertEquals(stat.getAcceptCnt() + 1, mergedStat.getAcceptCnt());
        assertEquals(stat.getAcceptCntSuccess(), mergedStat.getAcceptCntSuccess());
        assertEquals(stat.getCartCnt() + 1, mergedStat.getCartCnt());
        assertEquals(stat.getCartCntSuccess() + 1, mergedStat.getCartCntSuccess());
    }
}
