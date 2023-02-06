package ru.yandex.market.core.offer.mapping;

import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mboc.http.MboMappings.SearchMappingsByKeysRequest.ShopSkuKey;

@ParametersAreNonnullByDefault
class MappedOfferConversionsTest {
    @Test
    void testToShopSkuKey() {
        MatcherAssert.assertThat(
                MappedOfferConversions.toShopSkuKey(MboOfferKey.of(123, "SKU456")),
                Matchers.allOf(
                        MbiMatchers.transformedBy(ShopSkuKey::getShopSku, Matchers.is("SKU456")),
                        MbiMatchers.transformedBy(ShopSkuKey::getSupplierId, Matchers.is(123))
                )
        );
    }
}
