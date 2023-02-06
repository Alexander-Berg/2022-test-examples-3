package ru.yandex.market.loyalty.core.dao.coin;

import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.TableFormat;
import ru.yandex.market.loyalty.core.dao.custom.coin.BunchGenerationRequestDaoCustomImpl;
import ru.yandex.market.loyalty.core.model.coin.BunchGenerationRequest;
import ru.yandex.market.loyalty.core.model.coin.BunchRequestShard;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author artemmz
 */
public class BunchGenerationRequestShardDaoTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    BunchGenerationRequestShardDao requestShardService;
    @Autowired
    PromoManager promoManager;
    @Autowired
    BunchGenerationRequestDaoCustomImpl requestDaoCustom;

    @Test
    public void testCreateOrRestart() {
        long requestId = createBunchRequest();

        BunchRequestShard shard = new BunchRequestShard(1, requestId, 1, 10, 10);
        requestShardService.createOrRestart(new TreeSet<>(List.of(shard)));

        List<BunchRequestShard> shards = requestShardService.getShardsSortedStream(requestId).collect(Collectors.toList());
        assertEquals(shards, Collections.singletonList(shard));

        // should not recreate not committed
        assertThrows(IllegalStateException.class, () -> requestShardService.createOrRestart(new TreeSet<>(List.of(shard))));

        assertFalse(getFromDb(requestId).isCommitted());
        assertFalse(getFromDb(requestId).isMerged());
        requestShardService.commitAndMergeUndoneShards(requestId);
        BunchRequestShard committedAndMerged = getFromDb(requestId);
        assertTrue(committedAndMerged.isCommitted());
        assertTrue(committedAndMerged.isMerged());

        // should not recreate same uids
        assertThrows(IllegalStateException.class, () -> requestShardService.createOrRestart(new TreeSet<>(List.of(shard))));

        int newCnt = 5;
        BunchRequestShard nextShard = new BunchRequestShard(1, requestId, 11, 20, newCnt);
        requestShardService.createOrRestart(new TreeSet<>(List.of(nextShard)));

        BunchRequestShard recreated = getFromDb(requestId);
        assertFalse(recreated.isCommitted());
        assertFalse(recreated.isMerged());
        assertEquals(0, recreated.getProcessedCount());
        assertEquals(newCnt, recreated.getCount());
    }

    private long createBunchRequest() {
        Promo promo =  promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());
        return requestDaoCustom.insertRequest(BunchGenerationRequest.scheduled(promo.getPromoId().getId(),
                "REQ_KEY", 100500, "", TableFormat.YT, "", GeneratorType.COIN));
    }

    private BunchRequestShard getFromDb(long requestId) {
        return requestShardService.getShardsSortedStream(requestId).findFirst().orElseThrow();
    }
}
