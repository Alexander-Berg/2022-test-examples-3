package ru.yandex.market.checkout.test.providers;

import java.math.BigDecimal;

import ru.yandex.market.checkout.checkouter.order.ItemPrices;

/**
 * @author zagidullinri
 * @date 13.04.2021
 */
public abstract class ItemPricesProvider {
    public static final long SUBSUDY = 13L;

    public static ItemPrices getDefaultItemPrices() {
        ItemPrices itemPrices = new ItemPrices();
        itemPrices.setSubsidy(BigDecimal.valueOf(SUBSUDY));
        return itemPrices;
    }
}
