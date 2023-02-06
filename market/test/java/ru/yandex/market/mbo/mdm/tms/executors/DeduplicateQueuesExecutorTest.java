package ru.yandex.market.mbo.mdm.tms.executors;

import java.util.Comparator;
import java.util.List;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueueInfoBase;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

public class DeduplicateQueuesExecutorTest extends MdmBaseDbTestClass {
    @Autowired
    private SskuToRefreshRepository sskuQueue;
    @Autowired
    private MskuToRefreshRepository mskuQueue;
    @Autowired
    private StorageKeyValueService skv;

    private EnhancedRandom random;
    private DeduplicateQueuesExecutor executor;

    @Before
    public void setup() {
        random = TestDataUtils.defaultRandom(435243L);
        executor = new DeduplicateQueuesExecutor(skv, mskuQueue, sskuQueue);
    }

    @Test
    public void testEmptySkvDoesNothing() {
        var sskuKey = sskuKey();
        var mskuKey = mskuKey();
        sskuQueue.enqueue(sskuKey, MdmEnqueueReason.DEFAULT);
        dupeLastRecord(sskuQueue);
        mskuQueue.enqueue(mskuKey, MdmEnqueueReason.DEFAULT);
        dupeLastRecord(mskuQueue);

        executor.execute();

        Assertions.assertThat(sskuQueue.findAll()).hasSize(2);
        Assertions.assertThat(mskuQueue.findAll()).hasSize(2);

        skv.putValue(MdmProperties.DEDUPLICATEABLE_QUEUES, List.of());
        skv.invalidateCache();

        executor.execute();

        Assertions.assertThat(sskuQueue.findAll()).hasSize(2);
        Assertions.assertThat(mskuQueue.findAll()).hasSize(2);
    }

    @Test
    public void testOneQueueInSkvDoesNotTouchAnother() {
        var sskuKey = sskuKey();
        var mskuKey = mskuKey();
        sskuQueue.enqueue(sskuKey, MdmEnqueueReason.DEFAULT);
        dupeLastRecord(sskuQueue);
        mskuQueue.enqueue(mskuKey, MdmEnqueueReason.DEFAULT);
        dupeLastRecord(mskuQueue);

        skv.putValue(MdmProperties.DEDUPLICATEABLE_QUEUES, List.of("SSKU"));
        skv.invalidateCache();

        executor.execute();

        Assertions.assertThat(sskuQueue.findAll()).hasSize(1); // deduped
        Assertions.assertThat(mskuQueue.findAll()).hasSize(2);
    }

    @Test
    public void testGeneralHappyPathForDeduplication() {
        var sskuKey = sskuKey();
        var mskuKey = mskuKey();
        sskuQueue.enqueue(sskuKey, MdmEnqueueReason.DEFAULT);
        dupeLastRecord(sskuQueue);
        mskuQueue.enqueue(mskuKey, MdmEnqueueReason.DEFAULT);
        dupeLastRecord(mskuQueue);

        skv.putValue(MdmProperties.DEDUPLICATEABLE_QUEUES, List.of("SSKU", "MSKU"));
        skv.invalidateCache();

        executor.execute();

        Assertions.assertThat(sskuQueue.findAll()).hasSize(1); // deduped
        Assertions.assertThat(mskuQueue.findAll()).hasSize(1); // deduped
    }

    private ShopSkuKey sskuKey() {
        return random.nextObject(ShopSkuKey.class);
    }

    private long mskuKey() {
        return random.nextLong();
    }

    private void dupeLastRecord(SskuToRefreshRepository queue) {
        var last = queue.findAll().stream().max(Comparator.comparing(MdmQueueInfoBase::getId)).get();
        queue.insert(last);
    }

    private void dupeLastRecord(MskuToRefreshRepository queue) {
        var last = queue.findAll().stream().max(Comparator.comparing(MdmQueueInfoBase::getId)).get();
        queue.insert(last);
    }
}
