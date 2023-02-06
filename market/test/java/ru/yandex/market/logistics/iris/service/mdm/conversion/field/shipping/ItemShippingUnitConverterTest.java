package ru.yandex.market.logistics.iris.service.mdm.conversion.field.shipping;


import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.logistics.iris.core.index.field.FieldValue;

public class ItemShippingUnitConverterTest {
    @Test
    public void shouldReturnEmptyCollectionIfConfigurationIsNotItem() {
        MdmIrisPayload.ShippingUnit itemShippingUnitToConvert = MdmIrisPayload.ShippingUnit.newBuilder()
                .setConfiguraion(MdmIrisPayload.ShippingUnitConfiguration.PALLET)
                .build();

        Collection<FieldValue<?>> result = ItemShippingUnitConverter.fromItemShippingUnit(itemShippingUnitToConvert);

        Assert.assertTrue(result.isEmpty());
    }
}