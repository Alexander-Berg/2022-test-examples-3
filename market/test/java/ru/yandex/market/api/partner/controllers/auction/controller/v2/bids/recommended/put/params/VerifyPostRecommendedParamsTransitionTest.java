package ru.yandex.market.api.partner.controllers.auction.controller.v2.bids.recommended.put.params;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.market.api.partner.auth.AuthPrincipal;
import ru.yandex.market.api.partner.controllers.auction.AuctionControllerV2;
import ru.yandex.market.api.partner.controllers.auction.controller.v2.AuctionControllerV2BaseMock;
import ru.yandex.market.api.partner.controllers.auction.model.RecommendationTarget;
import ru.yandex.market.core.auction.err.AuctionValidationException;
import ru.yandex.market.core.geobase.model.Region;

import static org.mockito.Mockito.when;
import static ru.yandex.market.api.partner.controllers.auction.controller.v2.bids.recommended.RecommendedCommon.PARAM_MAX_VALUE_NOT_SPECIFIED;
import static ru.yandex.market.api.partner.controllers.auction.controller.v2.bids.recommended.RecommendedCommon.PARAM_OFFSET_NOT_SPECIFIED;
import static ru.yandex.market.api.partner.controllers.auction.controller.v2.bids.recommended.RecommendedCommon.PARAM_OFFSET_PCT_NOT_SPECIFIED;
import static ru.yandex.market.api.partner.controllers.auction.controller.v2.bids.recommended.RecommendedCommon.PARAM_SOME_VALID_POS;
import static ru.yandex.market.api.partner.controllers.auction.controller.v2.bids.recommended.RecommendedCommon.assertRecommenderGetRecommendationsCalledWithRegion;

/**
 * Проверка трансляции параметров переданных в ручку контроллера до важных сервисов в ручках установки рекомендованных значений.
 * В идеале нужно сделать перебор по всем рекомендаторам.
 */
@ExtendWith(MockitoExtension.class)
class VerifyPostRecommendedParamsTransitionTest extends AuctionControllerV2BaseMock {

    @InjectMocks
    private AuctionControllerV2 auctionControllerV2;

    @Mock
    private AuthPrincipal authPrincipal;

    @BeforeEach
    void before() {
        auctionControllerV2.setMaxOffersBulkSize(MAX_OFFERS_SIZE);
        mockAuction();
        mockRegions();
        mockCampaignsToShops();
        mockRecommendersWithEmptyRepsonses();
        mockBidLimits();

        Mockito.reset(datasourceService);
    }

    @DisplayName("Удалось найти локальный регион")
    @Test
    void test_transferParam_when_foundLocalInDb() throws AuctionValidationException {
        when(datasourceService.getLocalDeliveryRegion(SHOP_WITH_TITLE_OFFERS_IDENTIFICATION))
                .thenReturn(SOME_LOCAL_REGION_ID);

        when(regionService.getRegion(SOME_LOCAL_REGION_ID))
                .thenReturn(new Region(SOME_LOCAL_REGION_ID, "Some local region", null));

        auctionControllerV2.setBidsViaRecommendations(
                request,
                PARAM_CAMPAIGN_WITH_TITLE_OFFERS_IDENTIFICATION,
                PARAM_SOME_VALID_POS,
                PARAM_MAX_VALUE_NOT_SPECIFIED,
                PARAM_OFFSET_NOT_SPECIFIED,
                PARAM_OFFSET_PCT_NOT_SPECIFIED,
                RecommendationTarget.MODEL_CARD.getCode(),
                PARAM_REGION_NOT_SPECIFIED,
                PUT_RECS_BY_ID_DTO,
                authPrincipal
        );

        assertRecommenderGetRecommendationsCalledWithRegion(modelCardRecommender, SOME_LOCAL_REGION_ID);
    }

    @DisplayName("Регион НЕ удалось найти локальный")
    @Test
    void test_transferParam_when_noLocalFoundDb() throws AuctionValidationException {

        auctionControllerV2.setBidsViaRecommendations(
                request,
                PARAM_CAMPAIGN_WITH_TITLE_OFFERS_IDENTIFICATION,
                PARAM_SOME_VALID_POS,
                PARAM_MAX_VALUE_NOT_SPECIFIED,
                PARAM_OFFSET_NOT_SPECIFIED,
                PARAM_OFFSET_PCT_NOT_SPECIFIED,
                RecommendationTarget.SEARCH.getCode(),
                PARAM_REGION_NOT_SPECIFIED,
                PUT_RECS_BY_ID_DTO,
                authPrincipal
        );

        assertRecommenderGetRecommendationsCalledWithRegion(parallelSearchRecommender, REGARDLESS_OF_THE_REGION);
    }

    @DisplayName("Регион явно задан в запросе")
    @Test
    void test_transferParam_when_explicitRegionSpecified() throws AuctionValidationException {

        auctionControllerV2.setBidsViaRecommendations(
                request,
                PARAM_CAMPAIGN_WITH_TITLE_OFFERS_IDENTIFICATION,
                PARAM_SOME_VALID_POS,
                PARAM_MAX_VALUE_NOT_SPECIFIED,
                PARAM_OFFSET_NOT_SPECIFIED,
                PARAM_OFFSET_PCT_NOT_SPECIFIED,
                RecommendationTarget.SEARCH.getCode(),
                PARAM_REGION_ID,
                PUT_RECS_BY_ID_DTO,
                authPrincipal
        );

        assertRecommenderGetRecommendationsCalledWithRegion(parallelSearchRecommender, PARAM_REGION_ID);
    }
}
