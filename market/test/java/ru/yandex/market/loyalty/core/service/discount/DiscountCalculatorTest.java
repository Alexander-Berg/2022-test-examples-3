package ru.yandex.market.loyalty.core.service.discount;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.model.order.ItemKey;
import ru.yandex.market.loyalty.core.service.discount.constants.DefaultRoundingMode;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;

/**
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
public class DiscountCalculatorTest extends MarketLoyaltyCoreMockedDbTestBase {

    @Autowired
    private DiscountCalculator discountCalculator;

    @Test
    public void couponCalculateTest() {
        Map<ItemKey, BigDecimal> result = discountCalculator.calculate(Collections.singletonList(
                new ItemToDiscountCalculation(DEFAULT_ITEM_KEY, BigDecimal.ONE, BigDecimal.valueOf(300))
        ), BigDecimal.valueOf(200), false, DefaultRoundingMode.RUBLES);

        assertThat(result.values(), contains(comparesEqualTo(BigDecimal.valueOf(200))));
    }

    @Test
    public void coinFullApplicationTest() {
        Map<ItemKey, BigDecimal> result = discountCalculator.calculate(Collections.singletonList(
                new ItemToDiscountCalculation(DEFAULT_ITEM_KEY, BigDecimal.ONE, BigDecimal.valueOf(300))
        ), BigDecimal.valueOf(200), true, DefaultRoundingMode.RUBLES);

        assertThat(result.values(), contains(comparesEqualTo(BigDecimal.valueOf(200))));
    }

    @Test
    public void partialDiscountApplication() {
        Map<ItemKey, BigDecimal> result = discountCalculator.calculate(Collections.singletonList(
                new ItemToDiscountCalculation(DEFAULT_ITEM_KEY, BigDecimal.ONE, BigDecimal.valueOf(300))
        ), BigDecimal.valueOf(400), true, DefaultRoundingMode.RUBLES);

        assertThat(result.values(), contains(comparesEqualTo(BigDecimal.valueOf(299))));
    }


    @Test
    public void partialDiscountApplication3Items() {
        Map<ItemKey, BigDecimal> result = discountCalculator.calculate(Arrays.asList(
                new ItemToDiscountCalculation(DEFAULT_ITEM_KEY, BigDecimal.valueOf(3), BigDecimal.valueOf(150)),
                new ItemToDiscountCalculation(ANOTHER_ITEM_KEY, BigDecimal.valueOf(2), BigDecimal.valueOf(75))
        ), BigDecimal.valueOf(1000), true, DefaultRoundingMode.RUBLES);

        assertThat(result.values(), containsInAnyOrder(
                comparesEqualTo(BigDecimal.valueOf(149)),
                comparesEqualTo(BigDecimal.valueOf(74))
        ));
    }

    @Test
    public void partialDiscountApplication2Items() {
        Map<ItemKey, BigDecimal> result = discountCalculator.calculate(Arrays.asList(
                new ItemToDiscountCalculation(DEFAULT_ITEM_KEY, BigDecimal.valueOf(11), BigDecimal.TEN),
                new ItemToDiscountCalculation(ANOTHER_ITEM_KEY, BigDecimal.valueOf(17), BigDecimal.TEN)
        ), BigDecimal.valueOf(1000), true, DefaultRoundingMode.RUBLES);

        assertThat(result.values(), containsInAnyOrder(
                comparesEqualTo(BigDecimal.valueOf(9)),
                comparesEqualTo(BigDecimal.valueOf(9))
        ));
    }

    @Test
    public void partialDiscountApplication2ItemsAndOneIsVerySmall() {
        Map<ItemKey, BigDecimal> result = discountCalculator.calculate(Arrays.asList(
                new ItemToDiscountCalculation(DEFAULT_ITEM_KEY, BigDecimal.ONE, BigDecimal.valueOf(2)),
                new ItemToDiscountCalculation(ANOTHER_ITEM_KEY, BigDecimal.valueOf(4), BigDecimal.valueOf(4))
        ), BigDecimal.valueOf(1000), true, DefaultRoundingMode.RUBLES);

        assertThat(result.values(), containsInAnyOrder(
                comparesEqualTo(BigDecimal.ONE),
                comparesEqualTo(BigDecimal.valueOf(3))
        ));
    }
}
