package ru.yandex.market.partner.auction.matchers;

import org.hamcrest.Factory;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import ru.yandex.market.common.report.model.RecommendationType;
import ru.yandex.market.core.auction.recommend.ReportRecommendationSearchRequest;

import static org.hamcrest.Matchers.equalTo;

/**
 * feature-матчеры для {@link ReportRecommendationSearchRequest}.
 *
 * @author vbudnev
 */
public class ReportRecommendationSearchRequestFeatureMatchers {

    @Factory
    public static Matcher<ReportRecommendationSearchRequest> hasRecommendationType(final RecommendationType expectedType) {
        return new FeatureMatcher<ReportRecommendationSearchRequest, RecommendationType>(
                equalTo(expectedType),
                "recommendationTypes",
                "recommendationTypes"
        ) {
            @Override
            protected RecommendationType featureValueOf(final ReportRecommendationSearchRequest actual) {
                return actual.getRecommendationType();
            }
        };
    }

}
