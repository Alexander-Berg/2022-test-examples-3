package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.domain.v2.opinion.fact.Fact;
import ru.yandex.market.api.domain.v2.opinion.fact.ShopFact;
import ru.yandex.market.api.opinion.Delivery;

public class FactMatcher {
    public static Matcher<Fact> facts(Matcher<Fact> ... factors) {
        return Matchers.allOf(factors);
    }

    public static Matcher<ShopFact> shopFacts(Matcher<ShopFact> ... shopFactors) {
        return Matchers.allOf(shopFactors);
    }

    public static <T extends Fact> Matcher<T> id(int id) {
        return ApiMatchers.map(
            Fact::getId,
            "'id'",
            Matchers.is(id),
            FactMatcher::toStr
        );
    }

    public static <T extends Fact> Matcher<T> title(String title) {
        return ApiMatchers.map(
            Fact::getTitle,
            "'title'",
            Matchers.is(title),
            FactMatcher::toStr
        );
    }

    public static <T extends Fact> Matcher<T> description(String description) {
        return ApiMatchers.map(
            Fact::getDescription,
            "'description'",
            Matchers.is(description),
            FactMatcher::toStr
        );
    }

    public static <T extends Fact> Matcher<T> value(Integer value) {
        return ApiMatchers.map(
            Fact::getValue,
            "'value'",
            Matchers.is(value),
            FactMatcher::toStr
        );
    }

    public static Matcher<ShopFact> deliveryType(Delivery type) {
        return ApiMatchers.map(
            ShopFact::getDeliveryType,
            "'type'",
            Matchers.is(type),
            FactMatcher::toStr
        );
    }

    public static String toStr(Fact fact) {
        if (null == fact) {
            return "null";
        }
        return MoreObjects.toStringHelper(Fact.class)
            .add("id", fact.getId())
            .add("title", fact.getTitle())
            .add("description", fact.getDescription())
            .add("value", fact.getValue())
            .toString();
    }

    public static String toStr(ShopFact shopFact) {
        if (null == shopFact) {
            return "null";
        }
        return MoreObjects.toStringHelper(ShopFact.class)
            .add("id", shopFact.getId())
            .add("title", shopFact.getTitle())
            .add("description", shopFact.getDescription())
            .add("value", shopFact.getValue())
            .add("deliveryType", shopFact.getDeliveryType())
            .toString();
    }
}
