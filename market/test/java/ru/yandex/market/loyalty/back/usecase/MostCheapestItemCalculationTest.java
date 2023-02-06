package ru.yandex.market.loyalty.back.usecase;

import org.junit.Test;

import ru.yandex.market.loyalty.core.model.order.Item;
import ru.yandex.market.loyalty.core.model.order.ItemKey;
import ru.yandex.market.loyalty.core.service.discount.DiscountRoundingMode;
import ru.yandex.market.loyalty.core.service.discount.FairCalculator;
import ru.yandex.market.loyalty.core.service.discount.ItemToDiscountCalculation;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;

import static java.util.stream.Collectors.toMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.loyalty.core.utils.CartUtils.TestItemBuilder.item;
import static ru.yandex.market.loyalty.core.utils.CartUtils.TestItemBuilder.key;
import static ru.yandex.market.loyalty.core.utils.CartUtils.TestItemBuilder.price;
import static ru.yandex.market.loyalty.core.utils.CartUtils.TestItemBuilder.quantity;

public class MostCheapestItemCalculationTest {
    private static final long FEED_ID = 123;

    final FairCalculator calculator = new FairCalculator();

    //MARKETCHECKOUT-12591 case 1
    @Test
    public void shouldSmoothDiscountByDifferentPrices() {
        assertThat(calculation(59,
                item(
                        key(FEED_ID, "tea 1"),
                        price(64)
                ),
                item(
                        key(FEED_ID, "tea 2"),
                        price(59)
                ),
                item(
                        key(FEED_ID, "tea 3"),
                        price(59)
                )
        ), allOf(
                hasEntry(hasProperty("offerId", is("tea 1")), comparesEqualTo(BigDecimal.valueOf(43))),
                hasEntry(hasProperty("offerId", is("tea 2")), comparesEqualTo(BigDecimal.valueOf(40))),
                hasEntry(hasProperty("offerId", is("tea 3")), comparesEqualTo(BigDecimal.valueOf(40)))
        ));
    }

    //MARKETCHECKOUT-12591 case 2
    @Test
    public void shouldSmoothDiscountByDifferentPricesWithSomeBundles() {
        assertThat(calculation(40 + 54,
                item(
                        key(FEED_ID, "tea 4"),
                        price(40)
                ),
                item(
                        key(FEED_ID, "tea 3"),
                        price(50)
                ),
                item(
                        key(FEED_ID, "tea 5"),
                        price(54)
                ),
                item(
                        key(FEED_ID, "tea 6"),
                        price(54)
                ),
                item(
                        key(FEED_ID, "tea 2"),
                        price(56)
                ),
                item(
                        key(FEED_ID, "tea 7"),
                        price(67)
                )
        ), allOf(
                hasEntry(hasProperty("offerId", is("tea 2")), comparesEqualTo(BigDecimal.valueOf(39))),
                hasEntry(hasProperty("offerId", is("tea 3")), comparesEqualTo(BigDecimal.valueOf(36))),
                hasEntry(hasProperty("offerId", is("tea 4")), comparesEqualTo(BigDecimal.valueOf(29))),
                hasEntry(hasProperty("offerId", is("tea 5")), comparesEqualTo(BigDecimal.valueOf(38))),
                hasEntry(hasProperty("offerId", is("tea 6")), comparesEqualTo(BigDecimal.valueOf(38))),
                hasEntry(hasProperty("offerId", is("tea 7")), comparesEqualTo(BigDecimal.valueOf(47)))
        ));
    }

    //MARKETCHECKOUT-12591 case 3
    @Test
    public void shouldSmoothDiscountByItemWithCount() {
        assertThat(calculation(64 + 64,
                item(
                        key(FEED_ID, "tea 1"),
                        quantity(6),
                        price(64)
                )
        ), allOf(
                hasEntry(hasProperty("offerId", is("tea 1")), comparesEqualTo(BigDecimal.valueOf(42)))
        ));
    }

    //MARKETCHECKOUT-12591 case 4
    @Test
    public void shouldSmoothDiscountByItemsWithSameCount() {
        assertThat(calculation(59,
                item(
                        key(FEED_ID, "tea 1"),
                        quantity(2),
                        price(64)
                ),
                item(
                        key(FEED_ID, "tea 2"),
                        quantity(2),
                        price(59)
                )
        ), allOf(
                hasEntry(hasProperty("offerId", is("tea 1")), comparesEqualTo(BigDecimal.valueOf(48))),
                hasEntry(hasProperty("offerId", is("tea 2")), comparesEqualTo(BigDecimal.valueOf(45)))
        ));
    }

    //MARKETCHECKOUT-12591 case 5
    @Test
    public void shouldSmoothDiscountByItemsWithDifferentCount() {
        assertThat(calculation(59 + 59,
                item(
                        key(FEED_ID, "tea 1"),
                        quantity(2),
                        price(64)
                ),
                item(
                        key(FEED_ID, "tea 2"),
                        quantity(6),
                        price(59)
                )
        ), allOf(
                hasEntry(hasProperty("offerId", is("tea 1")), comparesEqualTo(BigDecimal.valueOf(47))),
                hasEntry(hasProperty("offerId", is("tea 2")), comparesEqualTo(BigDecimal.valueOf(45)))
        ));
    }

    //MARKETCHECKOUT-12591 case 6
    @Test
    public void shouldSmoothDiscountByItemsWithDifferentCount2() {
        assertThat(calculation(59 + 59,
                item(
                        key(FEED_ID, "tea 1"),
                        price(64)
                ),
                item(
                        key(FEED_ID, "tea 2"),
                        quantity(7),
                        price(59)
                )
        ), allOf(
                hasEntry(hasProperty("offerId", is("tea 1")), comparesEqualTo(BigDecimal.valueOf(44))),
                hasEntry(hasProperty("offerId", is("tea 2")), comparesEqualTo(BigDecimal.valueOf(45)))
        ));
    }

    //MARKETCHECKOUT-12591 case 7
    @Test
    public void shouldSmoothDiscountByItemsWithDifferentCount3() {
        assertThat(calculation(59 + 59 + 59,
                item(
                        key(FEED_ID, "tea 1"),
                        quantity(2),
                        price(64)
                ),
                item(
                        key(FEED_ID, "tea 2"),
                        quantity(7),
                        price(59)
                )
        ), allOf(
                hasEntry(hasProperty("offerId", is("tea 1")), comparesEqualTo(BigDecimal.valueOf(42))),
                hasEntry(hasProperty("offerId", is("tea 2")), comparesEqualTo(BigDecimal.valueOf(40)))
        ));
    }

    //MARKETCHECKOUT-12591 case 8
    @Test
    public void shouldSmoothDiscountByItemsWithDifferentCount4() {
        assertThat(calculation(64 + 59 + 59,
                item(
                        key(FEED_ID, "tea 1"),
                        quantity(3),
                        price(64)
                ),
                item(
                        key(FEED_ID, "tea 2"),
                        quantity(7),
                        price(59)
                )
        ), allOf(
                hasEntry(hasProperty("offerId", is("tea 1")), comparesEqualTo(BigDecimal.valueOf(43))),
                hasEntry(hasProperty("offerId", is("tea 2")), comparesEqualTo(BigDecimal.valueOf(42)))
        ));
    }

    private Map<ItemKey, BigDecimal> calculation(Number discount, Item... items) {
        Map<ItemKey, BigDecimal> discounts = calculator.calculate(
                ItemToDiscountCalculation.fromItems(Arrays.asList(items)),
                BigDecimal.valueOf(discount.doubleValue()),
                false,
                DiscountRoundingMode.DEFAULT_ROUNDING_MODE
        );

        return Arrays.stream(items)
                .collect(toMap(Item::getItemKey,
                        item -> item.getPrice().subtract(
                                discounts.getOrDefault(item.getItemKey(), BigDecimal.ZERO))));
    }
}
