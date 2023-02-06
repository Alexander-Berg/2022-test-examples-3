package ru.yandex.market.mbo.mdm.common.masterdata.repository.queue;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmMskuIdOrOrphanSskuKeyInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmMskuIdOrShopSkuKeyContainer;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueuePriorities;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

import static org.assertj.core.api.Assertions.assertThat;

public class MskuAndSskuQueueImplTest extends MdmBaseDbTestClass {

    @Autowired
    private MskuAndSskuQueue repository;
    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;

    protected EnhancedRandom random;

    private ShopSkuKey getRandomShopSkuKeyOrphan() {
        return getShopSkuKey(null, true);
    }

    private ShopSkuKey getSskuKeysForMsku(Long mskuId) {
        return getShopSkuKey(mskuId, false);
    }

    private ShopSkuKey getShopSkuKey(Long mskuId, boolean isOrphan) {
        var key = new ShopSkuKey(random.nextInt(), String.valueOf(random.nextInt()));
        if (!isOrphan) {
            map(key, mskuId);
        }
        return key;
    }

    @Before
    public void setup() {
        random = TestDataUtils.defaultRandom(1280);
    }

    @Test
    public void testMultipleInsertSskusLinkedToMsku() {
        List<Long> mskus = List.of(1L, 2L, 3L);
        List<ShopSkuKey> sskuKeys = new ArrayList<>();

        mskus.forEach(item -> {
            for (int i = 0; i < 10; ++i) {
                var queueItem = getSskuKeysForMsku(item);
                sskuKeys.add(queueItem);
            }
        });

        sskuKeys.forEach(q -> repository.enqueueSsku(q, getRandomReason(), MdmQueuePriorities.NORMAL_PRIORITY));
        List<Long> ids = repository.findAll()
            .stream()
            .map(MdmMskuIdOrOrphanSskuKeyInfo::getId)
            .sorted()
            .collect(Collectors.toList());

        var result = repository.getUnprocessedBatch(30);
        Assertions.assertThat(result).hasSize(3);
        Assertions.assertThat(result.get(0).getLinkedSskuKeys()).hasSize(10);


        // if marked as processed, should take next value
        repository.markProcessed(List.of(ids.get(0)));
        var result2 = repository.getUnprocessedBatch(3);
        assertThat(result2).hasSize(2);
    }


    @Test
    public void testBatchInsertSskusLinkedToMsku() {
        List<Long> mskus = List.of(1L, 2L, 3L);
        List<ShopSkuKey> sskuKeys = new ArrayList<>();

        mskus.forEach(item -> {
            for (int i = 0; i < 10; ++i) {
                var queueItem = getSskuKeysForMsku(item);
                sskuKeys.add(queueItem);
            }
        });

        repository.enqueueSskus(sskuKeys, getRandomReason(), MdmQueuePriorities.NORMAL_PRIORITY);
        var result1 = repository.getUnprocessedBatch(30);
        Assertions.assertThat(result1).hasSize(3);
        Assertions.assertThat(result1.get(0).getRefreshReasons()).hasSize(1);
    }

    @Test
    public void testMultipleInsertSskusLinkedToMskuSskuMerge() {
        List<Long> mskus = List.of(1L, 2L, 3L);
        List<ShopSkuKey> sskuKeys = new ArrayList<>();

        mskus.forEach(item -> {
            for (int i = 0; i < 10; ++i) {
                var queueItem = getSskuKeysForMsku(item);
                sskuKeys.add(queueItem);
            }
        });

        sskuKeys.forEach(q -> repository.enqueueSsku(q, getRandomReason(), MdmQueuePriorities.NORMAL_PRIORITY));

        var result = repository.getUnprocessedBatch(30);
        Assertions.assertThat(result).hasSize(3);
        Assertions.assertThat(result.get(0).getLinkedSskuKeys()).hasSize(10);

        sskuKeys.forEach(q -> repository.enqueueSsku(q, getRandomReason(), MdmQueuePriorities.NORMAL_PRIORITY));
        var result1 = repository.getUnprocessedBatch(30);
        Assertions.assertThat(result1).hasSize(3);
        Assertions.assertThat(result1.get(0).getLinkedSskuKeys()).hasSize(10);
    }

    @Test
    public void testMultipleInsertAllCases() {
        List<Long> mskus = List.of(1L, 2L, 3L);
        Long orphanMsku = 4L;
        List<ShopSkuKey> sskuKeys = new ArrayList<>();

        mskus.forEach(item -> {
            for (int i = 0; i < 10; ++i) {
                var queueItem = getSskuKeysForMsku(item);
                sskuKeys.add(queueItem);
            }
        });

        sskuKeys.forEach(q -> repository.enqueueSsku(q, getRandomReason(), MdmQueuePriorities.NORMAL_PRIORITY));

        repository.enqueueSsku(getRandomShopSkuKeyOrphan(), getRandomReason(), MdmQueuePriorities.NORMAL_PRIORITY);
        repository.enqueueMsku(orphanMsku, getRandomReason(), MdmQueuePriorities.NORMAL_PRIORITY);
        repository.enqueueMsku(1L, getRandomReason(), MdmQueuePriorities.NORMAL_PRIORITY);

        var result = repository.getUnprocessedBatch(32);
        Assertions.assertThat(result).hasSize(5);
    }

    @Test
    public void testSimpleInsertWithSeveralPriorities() {
        Long mskuId = 1L;

        var queueItem = getSskuKeysForMsku(mskuId);
        Set<MdmEnqueueReason> expectedReasons = new LinkedHashSet<>();
        for (int i = 0; i < 3; ++i) {
            var reason = getRandomReason();
            expectedReasons.add(reason);
            repository.enqueueSsku(queueItem, reason, MdmQueuePriorities.NORMAL_PRIORITY * i);
        }

        var result = repository.getUnprocessedBatch(1).get(0);

        assertThat(result.getEntityKey().getMskuId()).isEqualTo(mskuId);
        assertThat(result.getOnlyReasons()).containsExactlyInAnyOrderElementsOf(expectedReasons);
        assertThat(result.getPriority()).isEqualTo(MdmQueuePriorities.NORMAL_PRIORITY * 2);

    }

    @Test
    public void whenHaveMappingToZeroMskuCreateOrphanSskuInfo() {
        // given
        ShopSkuKey shopSkuKey = new ShopSkuKey(1945, "U-238");

        MappingCacheDao mapping = new MappingCacheDao()
            .setShopSkuKey(shopSkuKey)
            .setMskuId(0L)
            .setCategoryId(90587);
        mappingsCacheRepository.insertOrUpdateAll(List.of(mapping));

        int priority = 100500;
        MdmEnqueueReason reason = MdmEnqueueReason.CHANGED_BY_MDM_OPERATOR;

        //when
        repository.enqueueSsku(shopSkuKey, reason, priority);

        //then
        assertThat(repository.totalCount()).isEqualTo(1);
        List<MdmMskuIdOrOrphanSskuKeyInfo> unprocessed = repository.getUnprocessedBatch(100);
        assertThat(unprocessed).hasSize(1);
        MdmMskuIdOrOrphanSskuKeyInfo info = unprocessed.iterator().next();
        assertThat(info.isOrphanSsku()).isTrue();
        assertThat(info.getEntityKey().isSsku()).isTrue();
        assertThat(info.getEntityKey().getShopSkuKey()).isEqualTo(shopSkuKey);
        assertThat(info.getOnlyReasons()).containsExactly(reason);
        assertThat(info.getPriority()).isEqualTo(priority);
    }

    @Test
    public void whenDeduplicateInfosKeepAllLinkedSskus() {
        // given
        ShopSkuKey shopSkuKey1 = new ShopSkuKey(1945, "U-238");
        ShopSkuKey shopSkuKey2 = new ShopSkuKey(1946, "U-238");
        ShopSkuKey shopSkuKey3 = new ShopSkuKey(1947, "U-238");
        long msku = 100L;

        int priority = 100500;
        MdmEnqueueReason reason = MdmEnqueueReason.CHANGED_BY_MDM_OPERATOR;

        List<MappingCacheDao> mappings = Stream.of(shopSkuKey1, shopSkuKey2, shopSkuKey3)
            .map(sskuKey -> new MappingCacheDao()
                .setShopSkuKey(sskuKey)
                .setMskuId(msku)
                .setCategoryId(90587))
            .collect(Collectors.toList());
        mappingsCacheRepository.insertOrUpdateAll(mappings);

        List<MdmMskuIdOrOrphanSskuKeyInfo> existingInfos = Stream.of(shopSkuKey1, shopSkuKey2)
            .map(key -> new MdmMskuIdOrOrphanSskuKeyInfo()
                .setEntityKey(MdmMskuIdOrShopSkuKeyContainer.ofMsku(msku))
                .addLinkedSskuKeys(List.of(key)))
            .peek(info -> info.setPriority(priority))
            .peek(info -> info.addRefreshReason(reason))
            .collect(Collectors.toList());
        repository.insertOrUpdateAll(existingInfos);

        //when
        repository.enqueueSsku(shopSkuKey3, reason, priority);

        //then
        assertThat(repository.totalCount()).isEqualTo(2);
        List<MdmMskuIdOrOrphanSskuKeyInfo> unprocessed = repository.getUnprocessedBatch(100);
        assertThat(unprocessed).hasSize(1);
        MdmMskuIdOrOrphanSskuKeyInfo info = unprocessed.iterator().next();
        assertThat(info.isOrphanSsku()).isFalse();
        assertThat(info.getEntityKey().isMsku()).isTrue();
        assertThat(info.getEntityKey().getMskuId()).isEqualTo(msku);
        assertThat(info.getLinkedSskuKeys()).containsExactlyInAnyOrder(shopSkuKey1, shopSkuKey2, shopSkuKey3);
        assertThat(info.getOnlyReasons()).containsOnly(reason);
        assertThat(info.getPriority()).isEqualTo(priority);
    }

    protected MdmEnqueueReason getRandomReason() {
        var enumValues = MdmEnqueueReason.values();
        return enumValues[random.nextInt(enumValues.length)];
    }
    private void map(ShopSkuKey key, long modelId) {
        mappingsCacheRepository.insert(new MappingCacheDao()
            .setMskuId(modelId)
            .setShopSkuKey(key)
            .setModifiedTimestamp(LocalDateTime.now())
        );
    }
}
