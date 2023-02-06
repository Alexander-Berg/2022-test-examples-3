package ru.yandex.market.checkout.checkouter.actual;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.Test;

import ru.yandex.common.util.currency.Currency;

public class ActualItemTest {

    @Test
    public void testNullPrice() throws Exception {
        ActualItem actualItem = new ActualItem();
        actualItem.setPrice(null);
        actualItem.setCount(1);
        actualItem.setDelivery(true);
        actualItem.setOutletIds(Collections.singletonList(123L));
        actualItem.setCacheDate(new Date());
        actualItem.setShopCurrency(Currency.RUR);

        String serialied = actualItem.serializeToCache();

        ActualItem result = new ActualItem();
        ActualItem.deserializeCached(serialied, result);

    }

    @Test
    public void testNullShopOutletIds() {
        ActualItem actualItem = new ActualItem();
        actualItem.setPrice(BigDecimal.ONE);
        actualItem.setCount(1);
        actualItem.setDelivery(true);
        actualItem.setOutletIds(Collections.emptyList());
        actualItem.setCacheDate(new Date());
        actualItem.setShopCurrency(Currency.RUR);
        actualItem.setPreorder(true);

        String serialized = actualItem.serializeToCache();

        ActualItem result = new ActualItem();
        ActualItem.deserializeCached(serialized, result);
    }

}
