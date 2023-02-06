package ru.yandex.market.mbo.mdm.common.service.mapping;

import java.time.Instant;
import java.util.List;
import java.util.Random;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

public class MappingUpdatesCalculatorTest {

    private Random random;

    @Before
    public void setup() {
        random = new Random(10909);
    }

    @Test
    public void shouldDeleteAndNotifyIfMappingExist() {
        // given
        ShopSkuKey ssku = testShopSskuKey();
        MappingCacheDao existedMapping = mappingCacheDao(ssku, 15);
        MappingCacheDao deletedMapping = mappingCacheDao(ssku, 16);
        var delete = new UpdatedMappingInfo(deletedMapping, true);

        // when
        var updates = MappingUpdatesCalculator.calculate(List.of(delete), List.of(existedMapping));

        // then
        Assertions.assertThat(updates.getToNotify())
            .containsExactlyInAnyOrder(delete.getMappingCacheDao(), existedMapping);
        Assertions.assertThat(updates.getToDelete()).containsExactly(delete.getMappingCacheDao());
        Assertions.assertThat(updates.getToUpdate()).isEmpty();
    }

    @Test
    public void shouldNotDeleteAndNotifyIfMappingNotExist() {
        // given
        ShopSkuKey ssku = testShopSskuKey();
        MappingCacheDao deletedMapping = mappingCacheDao(ssku, 16);
        var delete = new UpdatedMappingInfo(deletedMapping, true);

        // when
        var updates = MappingUpdatesCalculator.calculate(List.of(delete), List.of());

        // then
        Assertions.assertThat(updates.getToNotify()).isEmpty();
        Assertions.assertThat(updates.getToDelete()).isEmpty();
        Assertions.assertThat(updates.getToUpdate()).isEmpty();
    }

    private ShopSkuKey testShopSskuKey() {
        return new ShopSkuKey(random.nextInt(), "test");
    }

    private static MappingCacheDao mappingCacheDao(ShopSkuKey shopSkuKey, long mskuId) {
        return new MappingCacheDao()
            .setSupplierId(shopSkuKey.getSupplierId())
            .setShopSku(shopSkuKey.getShopSku())
            .setMskuId(mskuId)
            .setCategoryId(111)
            .setVersionTimestamp(Instant.now());
    }
}
