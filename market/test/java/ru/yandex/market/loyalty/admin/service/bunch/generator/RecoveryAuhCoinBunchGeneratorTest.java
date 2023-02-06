package ru.yandex.market.loyalty.admin.service.bunch.generator;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.loyalty.admin.service.bunch.BunchRequestException;
import ru.yandex.market.loyalty.admin.utils.YtTestHelper;
import ru.yandex.market.loyalty.core.model.coin.BunchGenerationRequest;
import ru.yandex.market.loyalty.core.model.coin.BunchRequestShard;
import ru.yandex.market.loyalty.core.model.coin.BunchRequestShardResult;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.BUNCH_REQUEST_LAST_PROCESSED_UID;
import static ru.yandex.market.loyalty.core.dao.coin.CoinDao.DISCOUNT_TABLE;
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;

/**
 * @author artemmz
 */
public class RecoveryAuhCoinBunchGeneratorTest extends BunchGeneratorTest {
    private static final String REQ_KEY = "RECOVERY_REQ";

    @Autowired
    RecoveryAuhCoinBunchGenerator recoveryGenerator;
    @Autowired
    YtTestHelper ytTestHelper;

    @Test
    public void testRecovery() throws BunchRequestException {
        BunchGenerationRequest request = createBunchRequest(REQ_KEY).getLeft();
        SortedSet<BunchRequestShard> undoneShards = buildUndoneShards(Map.of(1, 10, 11, 20), request.getId());
        requestShardDao.createOrRestart(undoneShards);

        List<Long> uidsFromYt = LongStream.range(1, 21).boxed().collect(Collectors.toList());
        ytTestHelper.mockYtInputTableReads(
                YPath.simple(YT_INPUT_TABLE),
                uidsFromYt
        );

        recoveryGenerator.generate(request, 1, Instant.MAX);
        checkShards(request.getId());
        checkGeneration(request, uidsFromYt, REQ_KEY);
    }

    @Test
    public void testRecoveryUnmerged() throws BunchRequestException {
        BunchGenerationRequest request = createBunchRequest(REQ_KEY).getLeft();
        int maxUid = 20;
        SortedSet<BunchRequestShard> undoneShards = buildUndoneShards(Map.of(1, 10, 11, maxUid), request.getId());

        requestShardDao.createOrRestart(undoneShards);
        undoneShards.forEach(shard -> requestShardDao.commit(new BunchRequestShardResult(request.getId(),
                shard.getShardNum(), shard.getCount())));

        recoveryGenerator.generate(request, 1, Instant.MAX);

        checkShards(request.getId());
        BunchGenerationRequest requestFromDb = bunchRequestService.getRequest(request.getId());
        assertEquals(undoneShards.stream().mapToLong(BunchRequestShard::getCount).sum(),
                (int) requestFromDb.getProcessedCount());
        assertEquals(maxUid, (long) requestFromDb.getParam(BUNCH_REQUEST_LAST_PROCESSED_UID).orElseThrow());

        int coinsCount = coinDao.getCoinsCount(DISCOUNT_TABLE.sourceKey.like("%" + REQ_KEY + "%"));
        assertEquals(0, coinsCount); // need only to increment counters here
    }

    @Test
    public void testRecoveryHalfCommitted() throws BunchRequestException {
        BunchGenerationRequest request = createBunchRequest(REQ_KEY).getLeft();
        int maxUid = 20;

        SortedSet<BunchRequestShard> undoneShards = buildUndoneShards(Map.of(1, 10, 11, maxUid), request.getId());
        BunchRequestShard nonCommitted = undoneShards.first();
        BunchRequestShard committed = undoneShards.last();

        requestShardDao.createOrRestart(undoneShards);
        requestShardDao.commit(new BunchRequestShardResult(request.getId(), committed.getShardNum(),
                committed.getCount()));

        ytTestHelper.mockYtInputTableReads(
                YPath.simple(YT_INPUT_TABLE),
                LongStream.range(1, 21).boxed().collect(Collectors.toList()) // mock all range - see feedUidsToConsumer why
        );

        recoveryGenerator.generate(request, 1, Instant.MAX); // handle committed first

        BunchRequestShard committedFromDb = requestShardDao.getShardsSortedStream(request.getId())
                .filter(shard -> shard.getShardNum() == committed.getShardNum())
                .collect(Collectors.toList()).get(0);
        assertTrue(committedFromDb.isMerged());
        BunchGenerationRequest requestFromDb = bunchRequestService.getRequest(request.getId());
        assertEquals(committed.getCount(), (int) requestFromDb.getProcessedCount());
        assertEquals(maxUid, (long) requestFromDb.getParam(BUNCH_REQUEST_LAST_PROCESSED_UID).orElseThrow());
        // need only to increment counters here
        assertEquals(0, coinDao.getCoinsCount(DISCOUNT_TABLE.sourceKey.like("%" + REQ_KEY + "%")));
        // non committed should stay unprocessed
        assertTrue(requestShardDao.getShardsSortedStream(request.getId()).anyMatch(shard -> !shard.isCommitted()));

        recoveryGenerator.generate(request, 1, Instant.MAX); // now  handle notCommitted

        checkShards(request.getId());
        requestFromDb = bunchRequestService.getRequest(request.getId());
        assertEquals(undoneShards.stream().mapToLong(BunchRequestShard::getCount).sum(),
                (int) requestFromDb.getProcessedCount());
        assertEquals(maxUid, (long) requestFromDb.getParam(BUNCH_REQUEST_LAST_PROCESSED_UID).orElseThrow());

        int coinsCount = coinDao.getCoinsCount(DISCOUNT_TABLE.sourceKey.like("%" + REQ_KEY + "%"));
        assertEquals(nonCommitted.getCount(), coinsCount);
    }

    @Test
    public void testGenerateValidation() {
        BunchGenerationRequest request = createBunchRequest(REQ_KEY).getLeft();
        assertThrows(IllegalStateException.class, () -> recoveryGenerator.generate(request, 10, Instant.MAX));
    }

    @Test
    public void testUidFilter() {
        var whereClause = RecoveryAuhCoinBunchGenerator.constructUidFilter(buildUndoneShards(Map.of(1, 10, 11, 20),
                100500L));
        assertEquals(" where (uid >= 1 and uid <= 10) or (uid >= 11 and uid <= 20)", whereClause);
    }

    private void checkShards(long requestId) {
        var requestShards = requestShardDao.getShardsSortedStream(requestId);
        assertTrue(requestShards.allMatch(shard -> shard.isMerged() && shard.isCommitted()));
    }

    private SortedSet<BunchRequestShard> buildUndoneShards(Map<Integer, Integer> minMaxUids, long requestId) {
        AtomicInteger counter = new AtomicInteger();
        return minMaxUids.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .map(e -> new BunchRequestShard(
                        counter.getAndIncrement(),
                        requestId,
                        e.getKey(),
                        e.getValue(),
                        e.getValue() - e.getKey() + 1
                )).collect(Collectors.toCollection(TreeSet::new));
    }
}
