package ru.yandex.market.loyalty.back.util;

import java.math.BigDecimal;

/**
 * @author <a href="mailto:khamitov-rail@yandex-team.ru">Rail Khamitov</a>
 * @date 08.12.2020
 */
public class ItemPropertiesForTest {

    private final BigDecimal price;
    private final BigDecimal quantity;
    private final String sku;

    public ItemPropertiesForTest(BigDecimal price, BigDecimal quantity, String sku) {
        this.price = price;
        this.quantity = quantity;
        this.sku = sku;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getSku() {
        return sku;
    }
}
