package ru.yandex.market.mbo.mdm.common.masterdata.repository;

import java.time.Instant;
import java.util.Map;
import java.util.Random;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.util.TimestampUtil;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;

public class MappingsCacheRepositoryImplTest extends MdmBaseDbTestClass {
    private static final long SEED = 7993002L;
    private static final long MSKU1 = 1L;
    private static final long MSKU2 = 2L;
    private static final long CATEGORY1 = 11L;
    private static final long CATEGORY2 = 12L;
    private static final long CATEGORY3 = 13L;
    private Random random;

    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;

    @Before
    public void setup() {
        random = new Random(SEED);
    }

    @Test
    public void whenSeveralCategoryMappingsShouldPickLatestByVersion() {
        mappingsCacheRepository.insertBatch(
            mapping(MSKU1, CATEGORY1, 5L, null, null), // <---
            mapping(MSKU1, CATEGORY2, null, 10L, 15L),
            mapping(MSKU1, CATEGORY3, 4L, 8L, 10L),

            mapping(MSKU2, CATEGORY2, null, null, null),
            mapping(MSKU2, CATEGORY3, 5L, 10L, 15L), // <---
            mapping(MSKU2, CATEGORY2, 3L, 15L, 10L)
        );

        Assertions.assertThat(mskuCategories()).isEqualTo(Map.of(
            MSKU1, CATEGORY1,
            MSKU2, CATEGORY3
        ));
    }

    @Test
    public void whenSeveralCategoryMappingsShouldPickLatestByStamp() {
        mappingsCacheRepository.insertBatch(
            mapping(MSKU1, CATEGORY3, null, null, 20L),
            mapping(MSKU1, CATEGORY2, null, 10L, 15L), // <---
            mapping(MSKU1, CATEGORY3, null, 8L, 10L),

            mapping(MSKU2, CATEGORY1, null, null, null),
            mapping(MSKU2, CATEGORY3, null, 10L, 15L),
            mapping(MSKU2, CATEGORY2, null, 15L, null) // <---
        );

        Assertions.assertThat(mskuCategories()).isEqualTo(Map.of(
            MSKU1, CATEGORY2,
            MSKU2, CATEGORY2
        ));
    }

    @Test
    public void whenSeveralCategoryMappingsShouldIgnoreZeroCategory() {
        mappingsCacheRepository.insertBatch(
            mapping(MSKU1, 0L, 5L, null, null),
            mapping(MSKU1, CATEGORY2, null, 10L, 15L),
            mapping(MSKU1, CATEGORY3, 4L, 8L, 10L), // <---

            mapping(MSKU2, CATEGORY1, null, null, null),
            mapping(MSKU2, 0L, 5L, 10L, 15L),
            mapping(MSKU2, CATEGORY2, 3L, 15L, 10L) // <---
        );

        Assertions.assertThat(mskuCategories()).isEqualTo(Map.of(
            MSKU1, CATEGORY3,
            MSKU2, CATEGORY2
        ));
    }

    private Map<Long, Long> mskuCategories() {
        return mappingsCacheRepository.findCategoriesForMskuByLatestTs(mappingsCacheRepository.findAll());
    }

    private MappingCacheDao mapping(long mskuId, long categoryId, Long version, Long stamp, Long ts) {
        return new MappingCacheDao()
            .setSupplierId(random.nextInt())
            .setShopSku(String.valueOf(random.nextInt()))
            .setMskuId(mskuId)
            .setCategoryId((int) categoryId)
            .setVersionTimestamp(version != null ? Instant.ofEpochMilli(version) : null)
            .setUpdateStamp(stamp)
            .setMappingKind(MappingCacheDao.MappingKind.values()[random.nextInt(2)])
            .setModifiedTimestamp(ts != null ? TimestampUtil.toLocalDateTime(ts) : null);
    }
}
