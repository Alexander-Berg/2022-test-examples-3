package ru.yandex.market.delivery;

import org.junit.jupiter.api.Test;

import ru.yandex.market.core.delivery.converter.DeliveryCalculatorModelConverter;
import ru.yandex.market.core.delivery.tariff.model.CategoryRule;
import ru.yandex.market.core.delivery.tariff.model.DeliveryRuleId;
import ru.yandex.market.deliverycalculator.indexerclient.model.DeliveryRuleFeedCategoryItem;
import ru.yandex.market.deliverycalculator.indexerclient.model.TreeCriteriaDefaultPolicy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class DeliveryCalculatorModelConverterTest {
    private static final long FEED_ID = 10L;

    @Test
    void createCategoryRule() {
        var expected = new DeliveryRuleBuilder()
                .withCategories(null, TreeCriteriaDefaultPolicy.INCLUDE)
                .build();

        var deliveryRuleId = new DeliveryRuleId(1, 2);
        var categoryRule = new CategoryRule(deliveryRuleId, true);
        var actual = DeliveryCalculatorModelConverter.createCategoryRule(categoryRule);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void createCategoryItem() {
        var expected = createDeliveryRuleFeedCategoryItem(800L);
        var item = DeliveryCalculatorModelConverter.createCategoryItem(FEED_ID, "000800");

        assertThat(item).isEqualTo(expected);
    }

    @Test
    void createCategoryItemZeroCat() {
        var expected = createDeliveryRuleFeedCategoryItem(0L);
        var item = DeliveryCalculatorModelConverter.createCategoryItem(FEED_ID, "00000");

        assertThat(item).isEqualTo(expected);
    }

    private static DeliveryRuleFeedCategoryItem createDeliveryRuleFeedCategoryItem(long categoryId) {
        var expected = new DeliveryRuleFeedCategoryItem();
        expected.setFeedId(FEED_ID);
        expected.setCategoryId(categoryId);
        return expected;
    }

}
