package ru.yandex.market.core.auction.matchers;

import java.util.EnumSet;

import org.hamcrest.Factory;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.report.model.MarketSearchRequest;
import ru.yandex.market.common.report.model.RecommendationType;

import static org.hamcrest.Matchers.equalTo;

/**
 * Матчеры для {@link MarketSearchRequest}.
 *
 * @author vbudnev
 */
public class MarketSearchRequestMatchers {

    @Factory
    public static Matcher<MarketSearchRequest> hasPlace(final MarketReportPlace expectedPlace) {
        return new FeatureMatcher<MarketSearchRequest, MarketReportPlace>(
                equalTo(expectedPlace),
                "reportPlace",
                "reportPlace"
        ) {
            @Override
            protected MarketReportPlace featureValueOf(final MarketSearchRequest actual) {
                return actual.getPlace();
            }
        };
    }

    @Factory
    public static Matcher<MarketSearchRequest> hasRecommendationType(final EnumSet<RecommendationType> expectedTypes) {
        return new FeatureMatcher<MarketSearchRequest, EnumSet<RecommendationType>>(
                equalTo(expectedTypes),
                "recommendationTypes",
                "recommendationTypes"
        ) {
            @Override
            protected EnumSet<RecommendationType> featureValueOf(final MarketSearchRequest actual) {
                return actual.getRecommendationType();
            }
        };
    }

    @Factory
    public static Matcher<MarketSearchRequest> hasQuery(final String expectedQuery) {
        return new FeatureMatcher<MarketSearchRequest, String>(
                equalTo(expectedQuery),
                "query",
                "query"
        ) {
            @Override
            protected String featureValueOf(final MarketSearchRequest actual) {
                return actual.getQuery();
            }
        };
    }

    @Factory
    public static Matcher<MarketSearchRequest> hasClient(final String expectedClient) {
        return new FeatureMatcher<MarketSearchRequest, String>(
                equalTo(expectedClient),
                "client",
                "client"
        ) {
            @Override
            protected String featureValueOf(final MarketSearchRequest actual) {
                return actual.getClient();
            }
        };
    }
}
