package ru.yandex.market.adv.shop.integration.metrika.mapper;

import java.math.BigDecimal;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.shop.integration.AbstractShopIntegrationTest;
import ru.yandex.market.adv.shop.integration.metrika.model.order.MarketOrderItem;
import ru.yandex.metrika.pub.client.model.MetrikaEcomProduct;

class MetrikaEcomProductMapperTest extends AbstractShopIntegrationTest {

    private static final String ID = "ecom_product_id";
    private static final String NAME = "test_name";
    private static final String CATEGORY = "test_category";
    private static final int COUNT = 10;

    @Autowired
    private MetrikaEcomProductMapper mapper;

    @DisplayName("Успешный маппинг при заполненном ID поставщика")
    @Test
    void map_withSupplierId_success() {
        Assertions.assertThat(
                mapper.map(getOffer(1L), 22L)
        ).isEqualTo(getMetrikaEcomProduct(1L));
    }

    @DisplayName("Успешный маппинг при незаполненном ID поставщика")
    @Test
    void map_withoutSupplierId_success() {
        Assertions.assertThat(
                mapper.map(getOffer(null), 22L)
        ).isEqualTo(getMetrikaEcomProduct(22L));
    }

    private MarketOrderItem getOffer(Long supplierId) {
        MarketOrderItem marketOrderItem = new MarketOrderItem();
        marketOrderItem.setPp(51);
        marketOrderItem.setOfferId(ID);
        marketOrderItem.setName(NAME);
        marketOrderItem.setCategory(CATEGORY);
        marketOrderItem.setPrice(new BigDecimal(150));
        marketOrderItem.setCount(COUNT);
        marketOrderItem.setPartnerId(supplierId);
        return marketOrderItem;
    }

    private MetrikaEcomProduct getMetrikaEcomProduct(long supplierId) {
        return new MetrikaEcomProduct(ID, NAME, null, CATEGORY, 1.5d, COUNT, null, supplierId);
    }
}
