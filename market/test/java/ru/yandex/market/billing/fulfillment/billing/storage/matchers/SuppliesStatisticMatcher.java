package ru.yandex.market.billing.fulfillment.billing.storage.matchers;

import org.hamcrest.Matcher;

import ru.yandex.market.billing.fulfillment.billing.storage.notification.model.SuppliesStatistic;
import ru.yandex.market.mbi.util.MbiMatchers;

import static org.hamcrest.Matchers.allOf;

/**
 * @author vbudnev
 */
public class SuppliesStatisticMatcher {

    public static Matcher<SuppliesStatistic> hasItemsCount(long expectedValue, String msg) {
        return MbiMatchers.<SuppliesStatistic>newAllOfBuilder()
                .add(SuppliesStatistic::getItemsCount, expectedValue, msg)
                .build();
    }

    public static Matcher<SuppliesStatistic> hasSuppliesCount(long expectedValue, String msg) {
        return MbiMatchers.<SuppliesStatistic>newAllOfBuilder()
                .add(SuppliesStatistic::getSuppliesCount, expectedValue, msg)
                .build();
    }

    public static Matcher<SuppliesStatistic> emptyStatsMatcher() {
        return allOf(
                hasItemsCount(0, "itemsCount"),
                hasSuppliesCount(0, "suppliesCount")
        );
    }
}
