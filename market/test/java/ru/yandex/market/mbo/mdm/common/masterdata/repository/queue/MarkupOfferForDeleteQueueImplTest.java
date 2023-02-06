package ru.yandex.market.mbo.mdm.common.masterdata.repository.queue;

import java.util.List;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;

public class MarkupOfferForDeleteQueueImplTest extends MdmBaseDbTestClass {
    private static final int BATCH_SIZE = 100;

    @Autowired
    private MarkupOfferForDeleteQueue repository;

    private EnhancedRandom random;

    @Before
    public void setup() {
        random = TestDataUtils.defaultRandom(1280);
    }

    @Test
    public void testEnqueueAll() {
        var key = new OfferWithVersion(random.nextInt(), String.valueOf(random.nextInt()), random.nextLong());

        repository.enqueueAll(List.of(key));

        var enqueued = repository.getUnprocessedBatch(BATCH_SIZE);
        Assertions.assertThat(enqueued.size()).isOne();
        Assertions.assertThat(enqueued.get(0).getEntityKey()).isEqualTo(key);
    }
}
