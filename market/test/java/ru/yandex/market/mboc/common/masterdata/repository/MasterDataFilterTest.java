package ru.yandex.market.mboc.common.masterdata.repository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.IntStream;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

@SuppressWarnings("checkstyle:magicnumber")
public class MasterDataFilterTest {
    private EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(26).build();

    @Test
    public void whenCreatingShopSkuBatchFilterShouldSetCriteriaAndOrderBy() {
        int limit = 32;
        int supplierId = 19;
        String shopSku = "OLOLO-3000";
        MasterDataFilter filter = new MasterDataFilter().batch(new ShopSkuKey(supplierId, shopSku), limit);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(filter.getLimit()).isEqualTo(limit);
            softly.assertThat(filter.getOffset()).isEqualTo(null);
            softly.assertThat(filter.getOrderByFields()).containsExactly("supplier_id", "shop_sku");
            softly.assertThat(filter.getCriteria().size()).isEqualTo(1);
            softly.assertThat(filter.getCriteria().get(0) instanceof MDKeyGreaterCriteria).isTrue();
        });
    }

    @Test
    public void whenCreatingModificationTimeBatchFilterShouldSetCriteriaAndOrderBy() {
        int limit = 32;
        LocalDateTime localDateTime = LocalDateTime.of(1, 1, 1, 1, 1);
        ShopSkuKey shopSkuKey = new ShopSkuKey(19, "something");
        MasterDataFilter filter = new MasterDataFilter().batch(localDateTime, shopSkuKey, limit);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(filter.getLimit()).isEqualTo(limit);
            softly.assertThat(filter.getOffset()).isEqualTo(null);
            softly.assertThat(filter.getOrderByFields())
                .containsExactly("modified_timestamp", "supplier_id", "shop_sku");
            softly.assertThat(filter.getCriteria().size()).isEqualTo(1);
            softly.assertThat(filter.getCriteria().get(0) instanceof MdModifiedTimeAndKeyGreaterCriteria).isTrue();
        });
    }

    @Test
    public void whenCreatingModificationTimeBatchFilterWithoutOffsetKeyShouldSetMoidfiedAfterAndOrderBy() {
        int limit = 32;
        LocalDateTime localDateTime = LocalDateTime.of(1, 1, 1, 1, 1);
        MasterDataFilter filter = new MasterDataFilter().batch(localDateTime, null, limit);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(filter.getLimit()).isEqualTo(limit);
            softly.assertThat(filter.getOffset()).isEqualTo(null);
            softly.assertThat(filter.getOrderByFields())
                .containsExactly("modified_timestamp", "supplier_id", "shop_sku");
            softly.assertThat(filter.getCriteria().size()).isEqualTo(0);
            softly.assertThat(filter.getModifiedAfter()).isEqualTo(localDateTime);
        });
    }

    @Test
    public void whenCreatingBatchFilterWithOffsetShouldSetLimitOffsetAndOrderBy() {
        int limit = 1;
        int offset = 1;
        MasterDataFilter masterDataFilter = new MasterDataFilter()
            .setLimit(limit)
            .setOffset(offset)
            .setOrderByFields(Arrays.asList("supplier_id", "shop_sku"));
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(masterDataFilter.getLimit()).isEqualTo(limit);
            softAssertions.assertThat(masterDataFilter.getOffset()).isEqualTo(offset);
            softAssertions.assertThat(masterDataFilter.getOrderByFields()).containsExactly("supplier_id", "shop_sku");
        });
    }

    @Test
    public void copyShouldReturnEqualFilter() {
        IntStream.range(0, 100).forEach(n -> {
            MasterDataFilter filter = random.nextObject(MasterDataFilter.class);
            Assertions.assertThat(filter.copy()).isEqualTo(filter);
        });
    }

    @Test
    public void hasOrdersShouldReturnTrueIfOrderFieldsAreSet() {
        Assertions.assertThat(new MasterDataFilter().hasOrdersOrOffsetOrLimit()).isFalse();
        Assertions.assertThat(new MasterDataFilter().setLimit(1).hasOrdersOrOffsetOrLimit()).isTrue();
        Assertions.assertThat(new MasterDataFilter().setOffset(1).hasOrdersOrOffsetOrLimit()).isTrue();
        Assertions.assertThat(new MasterDataFilter()
            .setOrderByFields(Collections.singletonList("lol")).hasOrdersOrOffsetOrLimit()).isTrue();
    }
}
