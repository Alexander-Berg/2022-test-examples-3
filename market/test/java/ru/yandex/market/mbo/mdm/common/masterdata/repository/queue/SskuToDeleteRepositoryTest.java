package ru.yandex.market.mbo.mdm.common.masterdata.repository.queue;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author albina-gima
 * @date 6/10/22
 */
public class SskuToDeleteRepositoryTest extends MdmBaseDbTestClass {

    @Autowired
    private StorageKeyValueService storageKeyValueService;

    @Autowired
    private SskuToDeleteRepository repository;

    private EnhancedRandom random;

    @Before
    public void setup() {
        random = TestDataUtils.defaultRandom(1435);
    }

    @Test
    public void testSimpleInsert() {
        // given
        var key = getRandomKey();

        // when
        repository.enqueue(key, MdmEnqueueReason.DEFAULT);

        // then
        var result = repository.getUnprocessedBatch(1).get(0);
        Assertions.assertThat(result.getEntityKey()).isEqualTo(key);
        Assertions.assertThat(result.getRetryCount()).isEqualTo(0);
    }

    @Test
    public void testNewItemsDoesntProcessedBecauseOfDelay() {
        // given
        var key = getRandomKey();
        var reason = getRandomReason();
        var repository = new SskuToDeleteRepositoryImpl(jdbcTemplate, transactionTemplate, 1, storageKeyValueService);

        // when
        repository.enqueue(key, reason);

        // then
        var result = repository.getUnprocessedBatch(1);
        assertThat(result).isEmpty();
    }

    private ShopSkuKey getRandomKey() {
        return new ShopSkuKey(random.nextInt(), String.valueOf(random.nextInt()));
    }

    private MdmEnqueueReason getRandomReason() {
        var enumValues = MdmEnqueueReason.values();
        return enumValues[random.nextInt(enumValues.length)];
    }
}
