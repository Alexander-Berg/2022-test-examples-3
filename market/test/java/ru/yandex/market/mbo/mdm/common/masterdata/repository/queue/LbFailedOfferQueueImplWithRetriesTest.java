package ru.yandex.market.mbo.mdm.common.masterdata.repository.queue;

import java.util.List;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.SskuToRefreshInfo;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

/**
 * @author albina-gima
 * @date 11/29/21
 */
public class LbFailedOfferQueueImplWithRetriesTest extends MdmBaseDbTestClass {
    @Autowired
    private LbFailedOfferQueueRepository repository;

    private EnhancedRandom random;

    @Before
    public void setup() {
        random = TestDataUtils.defaultRandom(1435);
    }

    @Test
    public void testSimpleInsert() {
        var key = getRandomKey();

        repository.enqueue(key, MdmEnqueueReason.DEFAULT);

        var result = repository.getUnprocessedBatch(1).get(0);
        Assertions.assertThat(result.getEntityKey()).isEqualTo(key);
        Assertions.assertThat(result.getRetryCount()).isEqualTo(0);
    }

    @Test
    public void testSimpleInsertWithSeveralPrioritiesWhenRetriesWereAttempted() {
        var key = getRandomKey();
        int retryCount = 5;

        int priority1 = -1;
        int priority2 = 25;
        int priority3 = 100500;

        repository.enqueue(key, MdmEnqueueReason.DEFAULT, priority1);
        // поменяем кол-во ретраев для этой записи
        List<SskuToRefreshInfo> enqueued = repository.findAll();
        enqueued.get(0).setRetryCount(retryCount);
        repository.updateBatch(enqueued);

        repository.enqueue(key, MdmEnqueueReason.DEFAULT, priority2);
        repository.enqueue(key, MdmEnqueueReason.DEFAULT, priority3);

        var result = repository.getUnprocessedBatch(1).get(0);
        Assertions.assertThat(result.getEntityKey()).isEqualTo(key);
        Assertions.assertThat(result.getRetryCount()).isEqualTo(retryCount);
    }

    protected ShopSkuKey getRandomKey() {
        return new ShopSkuKey(random.nextInt(), String.valueOf(random.nextInt()));
    }
}
