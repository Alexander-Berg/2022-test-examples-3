package ru.yandex.market.api.partner.controllers.auction.controller.v2.bids.recommended.post.params;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;

import ru.yandex.market.api.partner.controllers.auction.AuctionControllerV2;
import ru.yandex.market.api.partner.controllers.auction.controller.v2.AuctionControllerV2BaseMock;
import ru.yandex.market.core.geobase.model.Region;

import static org.mockito.Mockito.when;
import static ru.yandex.market.api.partner.controllers.auction.controller.v2.bids.recommended.RecommendedCommon.PARAM_MODEL_CARD;
import static ru.yandex.market.api.partner.controllers.auction.controller.v2.bids.recommended.RecommendedCommon.PARAM_SOME_POSITIONS;
import static ru.yandex.market.api.partner.controllers.auction.controller.v2.bids.recommended.RecommendedCommon.assertRecommenderGetRecommendationsCalledWithRegion;

/**
 * Проверка трансляции параметров переданных в ручку контроллера до важных сервисов в ручках поулчения рекомендованных значений.
 * В идеале нужно сделать перебор по всем рекомендаторам.
 */
@ExtendWith(MockitoExtension.class)
class VerifyPostRecommendedParamsTransitionTest extends AuctionControllerV2BaseMock {

    @InjectMocks
    private AuctionControllerV2 auctionControllerV2;

    @BeforeEach
    void before() {
        auctionControllerV2.setMaxOffersBulkSize(MAX_OFFERS_SIZE);
        mockAuction();
        mockRegions();
        mockCampaignsToShops();
        mockRecommendersWithEmptyRepsonses();
    }

    @DisplayName("Регион явно задан в запросе")
    @Test
    void test_transferParam_when_passedExplicitRegion() {

        auctionControllerV2.getRecommendations(
                request,
                response,
                PARAM_CAMPAIGN_WITH_TITLE_OFFERS_IDENTIFICATION,
                PARAM_SOME_POSITIONS,
                PARAM_MODEL_CARD,
                PARAM_REGION_ID,
                PARAM_V2_EMPTY_OFFERS_SET
        );
        assertRecommenderGetRecommendationsCalledWithRegion(modelCardRecommender, PARAM_REGION_ID);
    }

    @DisplayName("Регион не задан в запросе но удалось найти локальный")
    @Test
    void test_transferParam_when_foundLocalInDb() {
        when(datasourceService.getLocalDeliveryRegion(SHOP_WITH_TITLE_OFFERS_IDENTIFICATION))
                .thenReturn(SOME_LOCAL_REGION_ID);

        when(regionService.getRegion(SOME_LOCAL_REGION_ID))
                .thenReturn(new Region(SOME_LOCAL_REGION_ID, "Some local region", null));

        auctionControllerV2.getRecommendations(
                request,
                response,
                PARAM_CAMPAIGN_WITH_TITLE_OFFERS_IDENTIFICATION,
                PARAM_SOME_POSITIONS,
                PARAM_MODEL_CARD,
                PARAM_REGION_NOT_SPECIFIED,
                PARAM_V2_EMPTY_OFFERS_SET
        );
        assertRecommenderGetRecommendationsCalledWithRegion(modelCardRecommender, SOME_LOCAL_REGION_ID);
    }

    @DisplayName("Регион не задан в запросе и НЕ удалось найти локальный")
    @Test
    void test_transferParam_when_nothingFound() {

        auctionControllerV2.getRecommendations(
                request,
                response,
                PARAM_CAMPAIGN_WITH_TITLE_OFFERS_IDENTIFICATION,
                PARAM_SOME_POSITIONS,
                PARAM_MODEL_CARD,
                PARAM_REGION_NOT_SPECIFIED,
                PARAM_V2_EMPTY_OFFERS_SET
        );
        assertRecommenderGetRecommendationsCalledWithRegion(modelCardRecommender, REGARDLESS_OF_THE_REGION);
    }
}
