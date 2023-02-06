package ru.yandex.market.fulfillment.stockstorage.domain.converter;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.OutboundItem;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SimpleStock;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class OutboundConverterTest {

    @Test
    public void successfulConvert() {
        SimpleStock simpleStock = OutboundConverter.convert(
                OutboundItem.of(1L, "sku", 23), 1);

        assertEquals(Long.valueOf(1L), simpleStock.getVendorId());
        assertEquals("sku", simpleStock.getShopSku());
        assertEquals(Integer.valueOf(23), simpleStock.getQuantity());
        assertEquals(Integer.valueOf(1), simpleStock.getWarehouseId());
    }

    @Test
    public void successfulConvertList() {
        List<OutboundItem> outboundItemList = Arrays.asList(
                OutboundItem.of(1L, "sku1", 21),
                OutboundItem.of(2L, "sku2", 22),
                OutboundItem.of(3L, "sku3", 23));

        List<SimpleStock> simpleStockList = OutboundConverter.convert(outboundItemList, 1);

        for (SimpleStock simpleStock : simpleStockList) {
            assertEquals(Integer.valueOf(1), simpleStock.getWarehouseId());
        }

        assertThat(simpleStockList, hasItem(hasProperty("vendorId", equalTo(1L))));
        assertThat(simpleStockList, hasItem(hasProperty("vendorId", equalTo(2L))));
        assertThat(simpleStockList, hasItem(hasProperty("vendorId", equalTo(3L))));

        assertThat(simpleStockList, hasItem(hasProperty("shopSku", equalTo("sku1"))));
        assertThat(simpleStockList, hasItem(hasProperty("shopSku", equalTo("sku2"))));
        assertThat(simpleStockList, hasItem(hasProperty("shopSku", equalTo("sku3"))));

        assertThat(simpleStockList, hasItem(hasProperty("quantity", equalTo(21))));
        assertThat(simpleStockList, hasItem(hasProperty("quantity", equalTo(22))));
        assertThat(simpleStockList, hasItem(hasProperty("quantity", equalTo(23))));
    }

}
