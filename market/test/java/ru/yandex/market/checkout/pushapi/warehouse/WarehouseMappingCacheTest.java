package ru.yandex.market.checkout.pushapi.warehouse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.pushapi.application.AbstractWebTestBase;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты для кеша маппингов идентификаторов складов.
 * @author Vadim Lyalin
 * @see WarehouseMappingCache
 */
public class WarehouseMappingCacheTest extends AbstractWebTestBase {
    @Autowired
    private WarehouseMappingCache warehouseMappingCache;

    @Test
    void testGetPartnerWarehouseId() {
        assertThat(warehouseMappingCache)
                .returns("super-sklad", w -> w.getPartnerWarehouseId(300501L))
                .returns(null, w -> w.getPartnerWarehouseId(2))
                .returns(null, w -> w.getPartnerWarehouseId(3));
    }
}
