package ru.yandex.market.api.internal.report.parsers.json.filters.registry;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.domain.v2.FilterField;
import ru.yandex.market.api.domain.v2.filters.Filter;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.filters.Filters;
import ru.yandex.market.api.internal.report.parsers.json.filters.FilterFactory;
import ru.yandex.market.api.internal.report.parsers.json.filters.FilterV2JsonParser;
import ru.yandex.market.api.matchers.FilterValueMatcher;
import ru.yandex.market.api.matchers.FiltersMatcher;
import ru.yandex.market.api.util.ResourceHelpers;
import ru.yandex.market.api.util.parser.Enums;

public class FilterRegistryParserTest extends BaseTest {
    private FilterV2JsonParser parser = new FilterV2JsonParser(
        Enums.allOf(FilterField.class),
        new FilterFactory()
    );

    @Test
    public void deliveryPerksEligible() {
        Filter filter = parse("delivery-perks-eligible.json");

        Assert.assertThat(
            filter,
                FiltersMatcher.filter(
                    FiltersMatcher.id(Filters.FILTER_DELIVERY_PERKS_ELIGIBLE),
                    FiltersMatcher.name("Бесплатная доставка"),
                    FiltersMatcher.type("BOOLEAN"),
                    FiltersMatcher.values(
                        Matchers.containsInAnyOrder(
                            FilterValueMatcher.id("0"),
                            FilterValueMatcher.id("1")
                        )
                    )
                )
        );
    }

    @Test
    public void blueFastDeliveryy() {
        Filter filter = parse("blue-fast-delivery.json");

        Assert.assertThat(
            filter,
            FiltersMatcher.filter(
                FiltersMatcher.id(Filters.FILTER_BLUE_FAST_DELIVERY),
                FiltersMatcher.name("Быстрая доставка"),
                FiltersMatcher.type("BOOLEAN"),
                FiltersMatcher.values(
                    Matchers.contains(
                        FilterValueMatcher.id("1")
                    )
                )
            )
        );
    }

    @Test
    public void paymentFilter() {
        Filter filter = parse("payment.json");

        Assert.assertThat(
                filter,
                FiltersMatcher.filter(
                        FiltersMatcher.id(Filters.FILTER_PAYMENTS),
                        FiltersMatcher.name("Способы оплаты"),
                        FiltersMatcher.type("ENUM"),
                        FiltersMatcher.values(
                                Matchers.containsInAnyOrder(
                                        FilterValueMatcher.id("prepayment_card"),
                                        FilterValueMatcher.id("delivery_card"),
                                        FilterValueMatcher.id("delivery_cash")
                                )
                        )
                )
        );
    }

    private Filter parse(String filename) {
        return parser.parse(ResourceHelpers.getResource(filename));
    }
}
