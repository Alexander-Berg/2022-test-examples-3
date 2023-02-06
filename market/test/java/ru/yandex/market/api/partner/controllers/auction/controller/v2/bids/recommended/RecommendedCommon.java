package ru.yandex.market.api.partner.controllers.auction.controller.v2.bids.recommended;

import java.math.BigDecimal;

import org.mockito.ArgumentCaptor;

import ru.yandex.market.api.partner.controllers.auction.model.ApiRecommendationsRequest;
import ru.yandex.market.api.partner.controllers.auction.model.RecommendationTarget;
import ru.yandex.market.api.partner.controllers.auction.model.recommender.ApiAuctionAbstractRecommender;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;

/**
 * Общие константы для тестов ручек рекомендаций.
 */
public class RecommendedCommon {
    public static final String[] PARAM_INVALID_TARGET = new String[]{"SOME_INVALID_TARGET"};
    public static final String[] PARAM_MODEL_CARD = new String[]{RecommendationTarget.MODEL_CARD.getCode()};

    public static final String[] PARAM_INVALID_TARGET_COMBINATION = new String[]{
            RecommendationTarget.SEARCH.getCode(),
            RecommendationTarget.MODEL_CARD.getCode()
    };

    /**
     * Какие-то непустые позиции, в случаях если какие-то позиции нужны, но неважно какие.
     */
    public static final String PARAM_SOME_POSITIONS = "1,3,5";

    public static final int PARAM_SOME_VALID_POS = 1;

    public static final BigDecimal PARAM_MAX_VALUE_NOT_SPECIFIED = null;
    public static final BigDecimal PARAM_OFFSET_NOT_SPECIFIED = null;
    public static final Integer PARAM_OFFSET_PCT_NOT_SPECIFIED = null;


    public static void assertRecommenderGetRecommendationsCalledWithRegion(ApiAuctionAbstractRecommender recommender, Long expectedRegionId) {
        ArgumentCaptor<ApiRecommendationsRequest> calculateCaptor = ArgumentCaptor.forClass(ApiRecommendationsRequest.class);
        verify(recommender).getRecommendations(calculateCaptor.capture());

        assertThat(
                calculateCaptor.getValue().getRegion().getId(),
                is(expectedRegionId)
        );
    }
}
