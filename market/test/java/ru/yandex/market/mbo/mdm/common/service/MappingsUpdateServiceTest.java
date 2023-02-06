package ru.yandex.market.mbo.mdm.common.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.service.mapping.MappingsUpdateService;
import ru.yandex.market.mbo.mdm.common.service.mapping.UpdatedMappingInfo;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

/**
 * @author dmserebr
 * @date 19/01/2021
 */
public class MappingsUpdateServiceTest extends MdmBaseDbTestClass {
    private static final ShopSkuKey SHOP_SKU_KEY_1 = new ShopSkuKey(42, "test");
    private static final ShopSkuKey SHOP_SKU_KEY_2 = new ShopSkuKey(42, "test2");
    private static final ShopSkuKey SHOP_SKU_KEY_3 = new ShopSkuKey(42, "test3");
    private static final long MSKU_1 = 100;
    private static final long MSKU_2 = 101;
    private static final long MSKU_3 = 102;
    private static final int CATEGORY = 10;
    private static final Instant TS_1 = Instant.ofEpochMilli(1000000);
    private static final Instant TS_2 = Instant.ofEpochMilli(1500000);
    private static final Instant TS_3 = Instant.ofEpochMilli(2000000);

    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private MappingsUpdateService mappingsUpdateService;

    @Test
    public void testAddNewMapping() {
        Assertions.assertThat(mappingsCacheRepository.findAll()).isEmpty();

        mappingsUpdateService.processUpdates(List.of(
            updatedMappingInfo(SHOP_SKU_KEY_1, MSKU_1, null, null, null, null)));

        List<MappingCacheDao> mappings = mappingsCacheRepository.findAll();
        Assertions.assertThat(mappings).hasSize(1);
        assertMappingCacheDao(mappings.get(0), SHOP_SKU_KEY_1, MSKU_1, null, null, false, false);
    }

    @Test
    public void testUpdateExistingMappingsNoVersions() {
        mappingsCacheRepository.insertOrUpdateAll(List.of(
            createMappingCacheDao(SHOP_SKU_KEY_1, MSKU_1, null, null,
                Instant.now(), null),
            createMappingCacheDao(SHOP_SKU_KEY_2, MSKU_2, null, null,
                Instant.now(), null),
            createMappingCacheDao(SHOP_SKU_KEY_3, MSKU_2, null, null,
                Instant.now(), null)));

        // change mappings for sskus 1 & 2
        mappingsUpdateService.processUpdates(List.of(
            updatedMappingInfo(SHOP_SKU_KEY_1, MSKU_2, MappingCacheDao.MappingSource.DATACAMP, null,
                null, Instant.now()),
            updatedMappingInfo(SHOP_SKU_KEY_2, MSKU_3, MappingCacheDao.MappingSource.MBOC_API, null,
                Instant.now(), null)));

        Map<ShopSkuKey, MappingCacheDao> mappings = mappingsCacheRepository.findAll().stream()
            .collect(Collectors.toMap(MappingCacheDao::getShopSkuKey, Function.identity()));

        Assertions.assertThat(mappings).hasSize(3);
        // mappings for sskus 1 & 2 are updated
        assertMappingCacheDao(mappings.get(SHOP_SKU_KEY_1), SHOP_SKU_KEY_1, MSKU_2,
            MappingCacheDao.MappingSource.DATACAMP, null, true, true);
        assertMappingCacheDao(mappings.get(SHOP_SKU_KEY_2), SHOP_SKU_KEY_2, MSKU_3,
            MappingCacheDao.MappingSource.MBOC_API, null, true, false);
        // mapping for ssku 3 is retained
        assertMappingCacheDao(mappings.get(SHOP_SKU_KEY_3), SHOP_SKU_KEY_3, MSKU_2, null, null,
            true, false);
    }

    @Test
    public void testUpdateExistingMappingsWithVersions() {
        mappingsCacheRepository.insertOrUpdateAll(List.of(
            createMappingCacheDao(SHOP_SKU_KEY_1, MSKU_1, MappingCacheDao.MappingSource.MBOC_API, TS_2,
                Instant.now(), null),
            createMappingCacheDao(SHOP_SKU_KEY_2, MSKU_2, MappingCacheDao.MappingSource.MBOC_API, TS_2,
                Instant.now(), null),
            createMappingCacheDao(SHOP_SKU_KEY_3, MSKU_2, MappingCacheDao.MappingSource.MBOC_API, TS_2,
                Instant.now(), null)));

        // change mappings with different versions
        mappingsUpdateService.processUpdates(List.of(
            updatedMappingInfo(SHOP_SKU_KEY_1, MSKU_2, MappingCacheDao.MappingSource.DATACAMP, TS_1,
                null, Instant.now()),
            updatedMappingInfo(SHOP_SKU_KEY_2, MSKU_3, MappingCacheDao.MappingSource.DATACAMP, TS_2,
                null, Instant.now()),
            updatedMappingInfo(SHOP_SKU_KEY_3, MSKU_1, MappingCacheDao.MappingSource.DATACAMP, TS_3,
                null, Instant.now())));

        Map<ShopSkuKey, MappingCacheDao> mappings = mappingsCacheRepository.findAll().stream()
            .collect(Collectors.toMap(MappingCacheDao::getShopSkuKey, Function.identity()));

        Assertions.assertThat(mappings).hasSize(3);
        // mapping for ssku 1 is retained
        assertMappingCacheDao(mappings.get(SHOP_SKU_KEY_1), SHOP_SKU_KEY_1, MSKU_1,
            MappingCacheDao.MappingSource.MBOC_API, TS_2, true, false);
        // mappings for sskus 2 & 3 are updated
        assertMappingCacheDao(mappings.get(SHOP_SKU_KEY_2), SHOP_SKU_KEY_2, MSKU_3,
            MappingCacheDao.MappingSource.DATACAMP, TS_2, true, true);
        assertMappingCacheDao(mappings.get(SHOP_SKU_KEY_3), SHOP_SKU_KEY_3, MSKU_1,
            MappingCacheDao.MappingSource.DATACAMP, TS_3, true, true);
    }

    @Test
    public void testUpdateMultipleTimes() {
        // Check that after each update, timestamps in 'transports' map are correctly updated.

        mappingsCacheRepository.insertOrUpdateAll(List.of(
            createMappingCacheDao(SHOP_SKU_KEY_1, MSKU_1, null, null,
                Instant.now(), null),
            createMappingCacheDao(SHOP_SKU_KEY_2, MSKU_2, null, null,
                Instant.now(), null)));

        // change mappings for sskus 1 & 2
        mappingsUpdateService.processUpdates(List.of(
            updatedMappingInfo(SHOP_SKU_KEY_1, MSKU_2, MappingCacheDao.MappingSource.DATACAMP, null,
                null, Instant.now()),
            updatedMappingInfo(SHOP_SKU_KEY_2, MSKU_3, MappingCacheDao.MappingSource.MBOC_API, null,
                Instant.now(), null)));

        Map<ShopSkuKey, MappingCacheDao> mappings = mappingsCacheRepository.findAll().stream()
            .collect(Collectors.toMap(MappingCacheDao::getShopSkuKey, Function.identity()));

        // mappings for sskus 1 & 2 are updated
        assertMappingCacheDao(mappings.get(SHOP_SKU_KEY_1), SHOP_SKU_KEY_1, MSKU_2,
            MappingCacheDao.MappingSource.DATACAMP, null,
            true, true);
        assertMappingCacheDao(mappings.get(SHOP_SKU_KEY_2), SHOP_SKU_KEY_2, MSKU_3,
            MappingCacheDao.MappingSource.MBOC_API, null,
            true, false);

        Instant ssku1mbocTs = mappings.get(SHOP_SKU_KEY_1).getMbocTimestamp();
        Instant ssku1eoxTs = mappings.get(SHOP_SKU_KEY_1).getEoxTimestamp();
        Instant ssku2mbocTs = mappings.get(SHOP_SKU_KEY_2).getMbocTimestamp();

        // change mappings for sskus 1 & 2 again
        mappingsUpdateService.processUpdates(List.of(
            updatedMappingInfo(SHOP_SKU_KEY_1, MSKU_2, MappingCacheDao.MappingSource.DATACAMP, null,
                null, Instant.now()),
            updatedMappingInfo(SHOP_SKU_KEY_2, MSKU_3, MappingCacheDao.MappingSource.DATACAMP, null,
                null, Instant.now())));

        Map<ShopSkuKey, MappingCacheDao> latestMappings = mappingsCacheRepository.findAll().stream()
            .collect(Collectors.toMap(MappingCacheDao::getShopSkuKey, Function.identity()));

        // mappings for sskus 1 & 2 are the same
        assertMappingCacheDao(latestMappings.get(SHOP_SKU_KEY_1), SHOP_SKU_KEY_1, MSKU_2,
            MappingCacheDao.MappingSource.DATACAMP, null, true, true);
        assertMappingCacheDao(latestMappings.get(SHOP_SKU_KEY_2), SHOP_SKU_KEY_2, MSKU_3,
            MappingCacheDao.MappingSource.DATACAMP, null, true, true);

        Assertions.assertThat(latestMappings.get(SHOP_SKU_KEY_1).getEoxTimestamp())
            .isAfter(ssku1eoxTs);
        Assertions.assertThat(latestMappings.get(SHOP_SKU_KEY_1).getMbocTimestamp())
            .isEqualTo(ssku1mbocTs);
        Assertions.assertThat(latestMappings.get(SHOP_SKU_KEY_1).getMbocTimestamp())
            .isBefore(ssku1eoxTs);
        Assertions.assertThat(latestMappings.get(SHOP_SKU_KEY_2).getEoxTimestamp())
            .isAfter(ssku2mbocTs);
        Assertions.assertThat(latestMappings.get(SHOP_SKU_KEY_2).getMbocTimestamp())
            .isEqualTo(ssku2mbocTs);
    }

    private static MappingCacheDao createMappingCacheDao(ShopSkuKey shopSkuKey,
                                                         long mskuId,
                                                         MappingCacheDao.MappingSource mappingSource,
                                                         Instant versionTs,
                                                         Instant mbocTs,
                                                         Instant eoxTs) {
        return new MappingCacheDao()
            .setShopSkuKey(shopSkuKey)
            .setMskuId(mskuId)
            .setCategoryId(CATEGORY)
            .setMappingSource(mappingSource)
            .setVersionTimestamp(versionTs)
            .setMbocTimestamp(mbocTs)
            .setEoxTimestamp(eoxTs);
    }

    private static UpdatedMappingInfo updatedMappingInfo(ShopSkuKey shopSkuKey,
                                                         long mskuId,
                                                         MappingCacheDao.MappingSource mappingSource,
                                                         Instant versionTs,
                                                         Instant mbocTs,
                                                         Instant eoxTs) {
        var mappingCacheDao = createMappingCacheDao(shopSkuKey, mskuId, mappingSource, versionTs, mbocTs, eoxTs);
        return new UpdatedMappingInfo(mappingCacheDao);
    }

    private static void assertMappingCacheDao(MappingCacheDao mapping,
                                              ShopSkuKey shopSkuKey,
                                              long mskuId,
                                              MappingCacheDao.MappingSource mappingSource,
                                              Instant versionTs,
                                              boolean hasMbocTs,
                                              boolean hasEoxTs) {
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(mapping.getShopSkuKey()).isEqualTo(shopSkuKey);
            softAssertions.assertThat(mapping.getMskuId()).isEqualTo(mskuId);
            softAssertions.assertThat(mapping.getCategoryId()).isEqualTo(CATEGORY);
            softAssertions.assertThat(mapping.getMappingSource()).isEqualTo(mappingSource);
            softAssertions.assertThat(mapping.getVersionTimestamp()).isEqualTo(versionTs);
            softAssertions.assertThat(mapping.getModifiedTimestamp()).isNotNull();
            softAssertions.assertThat(mapping.getMbocTimestamp() != null).isEqualTo(hasMbocTs);
            softAssertions.assertThat(mapping.getEoxTimestamp() != null).isEqualTo(hasEoxTs);
        });
    }
}
