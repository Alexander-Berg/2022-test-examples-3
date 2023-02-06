package ru.yandex.travel.hotels.searcher.partners;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import ru.yandex.travel.commons.proto.ECurrency;
import ru.yandex.travel.hotels.proto.TOfferLandingInfo;
import ru.yandex.travel.hotels.proto.TPriceWithDetails;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

public class MatcherUtils {
    public static Matcher<TPriceWithDetails> price(int amount, ECurrency currency) {
        return allOf(priceAmount(is(amount)), priceCurrency(is(currency)));
    }

    public static Matcher<TOfferLandingInfo> landingUrl(String landingUrl) {
        return allOf(landingUrl(is(landingUrl)));
    }

    private static FeatureMatcher<TPriceWithDetails, Integer> priceAmount(Matcher<Integer> matcher) {
        return new FeatureMatcher<>(matcher, "amount", "amount") {
            @Override
            protected Integer featureValueOf(TPriceWithDetails actual) {
                return actual.getAmount();
            }
        };
    }

    private static FeatureMatcher<TPriceWithDetails, ECurrency> priceCurrency(Matcher<ECurrency> matcher) {
        return new FeatureMatcher<>(matcher, "currency", "currency") {
            @Override
            protected ECurrency featureValueOf(TPriceWithDetails actual) {
                return actual.getCurrency();
            }
        };
    }

    private static FeatureMatcher<TOfferLandingInfo, String> landingUrl(Matcher<String> matcher) {
        return new FeatureMatcher<>(matcher, "landing url", "landing url") {
            @Override
            protected String featureValueOf(TOfferLandingInfo actual) {
                return actual.getLandingPageUrl();
            }
        };
    }

}
