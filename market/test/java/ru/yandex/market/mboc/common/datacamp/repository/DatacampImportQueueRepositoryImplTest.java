package ru.yandex.market.mboc.common.datacamp.repository;

import java.time.temporal.ChronoUnit;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import ru.yandex.market.mboc.common.datacamp.model.DatacampImportQueueItem;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.data.Percentage.withPercentage;
import static ru.yandex.market.mboc.common.datacamp.model.DatacampImportQueueItem.createNow;

public class DatacampImportQueueRepositoryImplTest extends BaseDbTestClass {
    @Autowired
    private DatacampImportQueueRepository datacampImportQueueRepository;

    @Test
    public void multicolumnKeyWorks() {
        var key1 = new BusinessSkuKey(1, "sku1");
        var key2 = new BusinessSkuKey(1, "sku2");
        var key3 = new BusinessSkuKey(2, "sku1");

        datacampImportQueueRepository.insert(createNow(key1, ChronoUnit.MINUTES, 10, "a"));
        datacampImportQueueRepository.insert(createNow(key2, ChronoUnit.MINUTES, 10, "b"));
        datacampImportQueueRepository.insert(createNow(key3, ChronoUnit.MINUTES, 10, null));

        Assertions.assertThatThrownBy(() ->
            datacampImportQueueRepository.insert(createNow(key1, ChronoUnit.MINUTES, 15, "c"))
        ).isInstanceOf(DataIntegrityViolationException.class);
        Assertions.assertThatThrownBy(() ->
            datacampImportQueueRepository.insert(createNow(key2, ChronoUnit.MINUTES, 15, null))
        ).isInstanceOf(DataIntegrityViolationException.class);
        Assertions.assertThatThrownBy(() ->
            datacampImportQueueRepository.insert(createNow(key3, ChronoUnit.MINUTES, 15, "d"))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void testFindNextTryBefore() {
        assertThat(datacampImportQueueRepository.findWithNextTryBefore(now(), 1000)).isEmpty();

        var item1 = new DatacampImportQueueItem(1, "sku1", now(), now().plusMinutes(10), "a", 0);
        var item2 = new DatacampImportQueueItem(2, "sku2", now(), now().plusMinutes(15), "b", 1);
        var item3 = new DatacampImportQueueItem(3, "sku3", now(), now().plusMinutes(29), "c", 2);
        var item4 = new DatacampImportQueueItem(4, "sku4", now(), now().plusMinutes(30), "d", 3);
        var item5 = new DatacampImportQueueItem(5, "sku5", now(), now().plusMinutes(60), "e", 4);
        datacampImportQueueRepository.insertBatch(item1, item2, item3, item4, item5);

        assertThat(datacampImportQueueRepository.findWithNextTryBefore(item4.getNextTryAt(), 4))
            .hasSize(3)
            .containsExactly(item1, item2, item3);
    }

    @Test
    public void testInsertIfNotAlreadyPresent() {
        var itemOld = new DatacampImportQueueItem(1, "sku1", now().minusDays(1), now().minusDays(1), "old", 100);
        datacampImportQueueRepository.insert(itemOld);
        var itemOldCopy = new DatacampImportQueueItem(1, "sku1", now(), now(), "old copy", 1);
        var itemNew = new DatacampImportQueueItem(2, "sku2", now(), now(), "b", 1);
        datacampImportQueueRepository.insertIfNotPresent(List.of(itemOldCopy, itemNew));

        assertThat(datacampImportQueueRepository.findById(itemOldCopy.getBusinessSkuKey())).isEqualTo(itemOld);
        assertThat(datacampImportQueueRepository.findById(itemNew.getBusinessSkuKey())).isEqualTo(itemNew);
    }

    @Test
    public void insertOrUpdateFailedImports() {

        var existingKey = new BusinessSkuKey(1, "sku1");
        var nonExistingKey = new BusinessSkuKey(2, "sku2");

        var existingPrevious = datacampImportQueueRepository.insert(new DatacampImportQueueItem(
            existingKey, now().minusMinutes(30), now().minusMinutes(25), "oldCause", 0));

        var existingNew = new DatacampImportQueueItem(existingKey, now().minusMinutes(10), now(), "errorE", 0);
        var nonExisting = new DatacampImportQueueItem(nonExistingKey, now().minusMinutes(20), now(), "errorN", 0);
        datacampImportQueueRepository.insertOrUpdateFailedImports(
            List.of(existingNew.copy(), nonExisting.copy()));

        // If item does not exists, it should be inserted with all given values
        assertThat(datacampImportQueueRepository.findById(nonExistingKey)).isEqualTo(nonExisting);

        // If item does exists, only nextTryAt and cause should be updated
        var existingCurrent = datacampImportQueueRepository.findById(existingKey);
        assertThat(existingCurrent.getFailedAt())
            .isEqualTo(existingPrevious.getFailedAt())
            .isNotEqualTo(existingNew.getFailedAt());
        assertThat(existingCurrent.getNextTryAt()).isCloseTo(now(), within(1, SECONDS));
        assertThat(existingCurrent.getCause())
            .isEqualTo(existingNew.getCause())
            .isNotEqualTo(existingPrevious.getCause());
    }

    @Test
    public void testStats() {
        var statsEmpty = datacampImportQueueRepository.collectStats();
        assertThat(statsEmpty.getAmount()).isEqualTo(0);
        assertThat(statsEmpty.getMaxAgeInSeconds()).isEqualTo(0);

        var item1 = new DatacampImportQueueItem(1, "sku1", now().minusMinutes(10), now(), "a", 1);
        var item2 = new DatacampImportQueueItem(2, "sku2", now().minusMinutes(30), now(), "b", 2);
        var item3 = new DatacampImportQueueItem(3, "sku3", now().minusMinutes(60), now(), "c", 3);
        datacampImportQueueRepository.insertBatch(item1, item2, item3);

        var stats = datacampImportQueueRepository.collectStats();
        assertThat(stats.getAmount()).isEqualTo(3);
        assertThat(stats.getMaxAgeInSeconds()).isCloseTo(3600, withPercentage(99.99999));
    }
}
