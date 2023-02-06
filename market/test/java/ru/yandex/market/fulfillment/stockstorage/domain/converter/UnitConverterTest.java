package ru.yandex.market.fulfillment.stockstorage.domain.converter;

import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.OutboundItem;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItem;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItemAmount;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId;

import static org.junit.Assert.assertEquals;


public class UnitConverterTest {

    private SSItemAmount getSSItemAmount() {
        return SSItemAmount.of(
                SSItem.of("sku", 1L, 1),
                1000);
    }

    private OutboundItem getOutboundItem() {
        return OutboundItem.of(1L, "sku", 1000);
    }

    @Test
    public void successfulConvertSSItemAmount() {
        SSItemAmount ssItemAmount = getSSItemAmount();
        UnitId unitId = UnitConverter.toEmbeddedUnitId(ssItemAmount);
        assertEquals(ssItemAmount.getItem().getShopSku(), unitId.getSku());
        assertEquals(Long.valueOf(ssItemAmount.getItem().getVendorId()), unitId.getVendorId());
        assertEquals(Integer.valueOf(ssItemAmount.getItem().getWarehouseId()), unitId.getWarehouseId());
    }

    @Test
    public void successfulConvertOutboundItem() {
        OutboundItem outboundItem = getOutboundItem();
        UnitId unitId = UnitConverter.toEmbeddedUnitId(outboundItem, 1);
        assertEquals(outboundItem.getShopSku(), unitId.getSku());
        assertEquals(outboundItem.getVendorId(), unitId.getVendorId());
        assertEquals(Integer.valueOf(1), unitId.getWarehouseId());
    }
}
