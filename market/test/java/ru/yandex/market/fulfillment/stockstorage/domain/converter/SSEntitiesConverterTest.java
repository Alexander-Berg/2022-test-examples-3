package ru.yandex.market.fulfillment.stockstorage.domain.converter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.stockstorage.domain.entity.Sku;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class SSEntitiesConverterTest {

    private Sku getSku() {
        Sku sku = new Sku();
        sku.setUnitId(new UnitId("sku", 1L, 1));
        return sku;
    }

    private Set<UnitId> getUnitIds() {
        Set<UnitId> unitIds = new HashSet<>();
        unitIds.add(new UnitId("sku", 1L, 1));
        unitIds.add(new UnitId("sku1", 2L, 1));
        unitIds.add(new UnitId("sku2", 3L, 1));

        return unitIds;
    }

    @Test
    public void successfulConvertToLgwUnitId() {
        Sku sku = getSku();
        ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId unitId = SSEntitiesConverter.toLgwUnitId(sku);
        assertEquals(sku.getUnitId().getSku(), unitId.getArticle());
        assertEquals(sku.getUnitId().getVendorId(), unitId.getVendorId());
    }

    @Test
    public void successfulConvertToLgwUnitIds() {
        Set<UnitId> unitIds = getUnitIds();
        Collection<ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId> apiUnitIds =
                SSEntitiesConverter.toLgwUnitIds(unitIds);

        assertEquals(3, apiUnitIds.size());

        assertThat(apiUnitIds, hasItem(hasProperty("article", equalTo("sku"))));
        assertThat(apiUnitIds, hasItem(hasProperty("article", equalTo("sku1"))));
        assertThat(apiUnitIds, hasItem(hasProperty("article", equalTo("sku2"))));

        assertThat(apiUnitIds, hasItem(hasProperty("vendorId", equalTo(1L))));
        assertThat(apiUnitIds, hasItem(hasProperty("vendorId", equalTo(2L))));
        assertThat(apiUnitIds, hasItem(hasProperty("vendorId", equalTo(3L))));
    }
}
