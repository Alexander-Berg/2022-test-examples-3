package ru.yandex.market.loyalty.admin.service.bunch.generator;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName;
import ru.yandex.market.loyalty.core.model.coin.BunchGenerationRequest;
import ru.yandex.market.loyalty.core.model.coin.BunchRequestShard;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static ru.yandex.market.loyalty.admin.service.bunch.generator.ConcurrentAuthCoinBunchGenerator.MIN_COINS_SIZE_FOR_CONCURRENT_INSERT;
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;

/**
 * @author artemmz
 */
public class ConcurrentAuthCoinBunchGeneratorTest extends BunchGeneratorTest {
    private static final String REQUEST_KEY = "REQ_KEY";

    @Autowired
    ConcurrentAuthCoinBunchGenerator concurrentGenerator;
    @Autowired
    ConfigurationService configurationService;
    @Autowired
    CoinEmissionDispatcher coinEmissionDispatcher;

    @Test
    public void testGenerateSingleThread() {
        setParallelism(1);

        List<Long> uids = generateUids(MIN_COINS_SIZE_FOR_CONCURRENT_INSERT * 2);
        BunchGenerationRequest request = generateCoins(uids);

        checkGeneration(request, uids, REQUEST_KEY);
        assertEquals(0, requestShardDao.getShardsSortedStream(request.getId()).count());
    }

    @Test
    public void testGenerateConcurrent() {
        int parallelism = setParallelism(3);
        List<Long> uids = generateUids(MIN_COINS_SIZE_FOR_CONCURRENT_INSERT * 2);

        BunchGenerationRequest request = generateCoins(uids);
        checkGeneration(request, uids, REQUEST_KEY);
        checkShards(request.getId(), parallelism);
    }

    @Test
    public void testGenerateConcurrentWithThreadFail() {
        setParallelism(4);
        List<Long> uids = generateUids(MIN_COINS_SIZE_FOR_CONCURRENT_INSERT * 2);
        CoinEmissionDispatcher coinEmissionDispatcherSpy = spy(coinEmissionDispatcher);
        doCallRealMethod().doCallRealMethod().doCallRealMethod()
                .doThrow(new RuntimeException("one shard failed!!"))
                .when(coinEmissionDispatcherSpy).commitShard(any());
        concurrentGenerator.setCoinEmissionDispatcher(coinEmissionDispatcherSpy);

        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed().setEmissionBudget(100500));
        BunchGenerationRequest request = createBunchRequest(promo, REQUEST_KEY);

        assertThrows(RuntimeException.class, () -> concurrentGenerator.processBatch(request, promo, 0, uids));
        checkProcessingWithErrorsNotMerged(request.getId());
    }

    @Test
    public void testGenerateConcurrentWithNotCommitted() {
        setParallelism(4);
        List<Long> uids = generateUids(MIN_COINS_SIZE_FOR_CONCURRENT_INSERT * 2);
        CoinEmissionDispatcher coinEmissionDispatcherSpy = spy(coinEmissionDispatcher);
        doReturn(List.of(new BunchRequestShard(1, 1, 1, 1, 1))).when(coinEmissionDispatcherSpy).getNotCommittedShards(anyLong());
        concurrentGenerator.setCoinEmissionDispatcher(coinEmissionDispatcherSpy);

        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed().setEmissionBudget(100500));
        BunchGenerationRequest request = createBunchRequest(promo, REQUEST_KEY);

        assertThrows(IllegalStateException.class, () -> concurrentGenerator.processBatch(request, promo, 0, uids));
        checkProcessingWithErrorsNotMerged(request.getId());
    }

    private BunchGenerationRequest generateCoins(List<Long> uids) {
        Pair<BunchGenerationRequest, Promo> requestWithPromo = createBunchRequest(REQUEST_KEY);
        concurrentGenerator.processBatch(requestWithPromo.getLeft(), requestWithPromo.getRight(), 0, uids);
        return requestWithPromo.getLeft();
    }

    private void checkShards(long requestId, int parallelism) {
        List<BunchRequestShard> requestShards = requestShardDao.getShardsSortedStream(requestId).collect(Collectors.toList());
        assertEquals(parallelism, requestShards.size());
        assertTrue(requestShards.stream().allMatch(sh -> sh.isCommitted() && sh.isMerged()));

        // check shard uids order
        long previousMaxUid = 0;
        for (BunchRequestShard requestShard : requestShards) {
            if (requestShard.getMinUid() < previousMaxUid || requestShard.getMaxUid() < requestShard.getMinUid()) {
                fail("shards should contain sorted uids! " + requestShards);
            }
            previousMaxUid = requestShard.getMaxUid();
        }
    }

    private void checkProcessingWithErrorsNotMerged(long requestId) {
        assertFalse(coinEmissionDispatcher.getUndoneShards(requestId).isEmpty());
        assertTrue(requestShardDao.getShardsSortedStream(requestId).noneMatch(BunchRequestShard::isMerged));
        BunchGenerationRequest requestFromDb = bunchRequestService.getRequest(requestId);
        assertEquals(0, (int) requestFromDb.getProcessedCount());
        assertTrue(requestFromDb.getParam(BunchGenerationRequestParamName.BUNCH_REQUEST_LAST_PROCESSED_UID).isEmpty());
    }

    private List<Long> generateUids(int cnt) {
        return Stream.iterate(0L, i -> i + 1).limit(cnt).collect(Collectors.toList());
    }

    private int setParallelism(int parallelism) {
        configurationService.set(ConfigurationService.COIN_CONCURRENT_EMISSION_THREADS_NUM, parallelism);
        return parallelism;
    }
}
