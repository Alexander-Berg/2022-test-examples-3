package ru.yandex.market.api.partner.controllers.auction.controller.v2.bids.recommended.post.params;

import java.security.AccessControlException;
import java.util.HashSet;
import java.util.Set;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;

import ru.yandex.market.api.partner.controllers.auction.AuctionControllerV2;
import ru.yandex.market.api.partner.controllers.auction.controller.v2.AuctionControllerV2BaseMock;
import ru.yandex.market.api.partner.controllers.auction.model.AuctionOffer;
import ru.yandex.market.api.partner.controllers.auction.model.AuctionOffers;
import ru.yandex.market.api.partner.request.InvalidRequestException;
import ru.yandex.market.core.auction.model.AuctionNAReason;
import ru.yandex.market.core.auction.model.AuctionOfferId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.api.partner.controllers.auction.controller.v2.bids.recommended.RecommendedCommon.PARAM_INVALID_TARGET;
import static ru.yandex.market.api.partner.controllers.auction.controller.v2.bids.recommended.RecommendedCommon.PARAM_INVALID_TARGET_COMBINATION;
import static ru.yandex.market.api.partner.controllers.auction.controller.v2.bids.recommended.RecommendedCommon.PARAM_MODEL_CARD;
import static ru.yandex.market.api.partner.controllers.auction.controller.v2.bids.recommended.RecommendedCommon.PARAM_SOME_POSITIONS;

/**
 * Обработка некорректных параметров в ручках поулчения рекомендованных значений.
 */
@ExtendWith(MockitoExtension.class)
public class InvalidArgumentsTest extends AuctionControllerV2BaseMock {

    @InjectMocks
    private AuctionControllerV2 auctionControllerV2;

    @BeforeEach
    void before() {
        auctionControllerV2.setMaxOffersBulkSize(MAX_OFFERS_SIZE);
    }

    /**
     * Обработка неверного таргета.
     */
    @Test
    void test_checkParams_when_unknownTargetPassed_should_throw() {
        InvalidRequestException requestException = Assertions.assertThrows(InvalidRequestException.class,
                () -> auctionControllerV2.getRecommendations(
                        request,
                        response,
                        PARAM_CAMPAIGN_ID,
                        PARAM_SOME_POSITIONS,
                        PARAM_INVALID_TARGET,
                        PARAM_REGION_ID,
                        PARAM_V2_EMPTY_OFFERS_SET
                ));
        assertThat(requestException.getMessage(), Matchers.containsString("Illegal target: SOME_INVALID_TARGET"));

    }

    /**
     * Обработка неверного сочетания валидных таргетов.
     * В идеале тут надо делать перебор по сочетаниям, через {@link org.junit.runners.Parameterized}.
     */
    @Test
    void test_checkParams_when_unavailableTargetsCombinationPassed_should_throw() {
        InvalidRequestException requestException = Assertions.assertThrows(InvalidRequestException.class,
                () -> auctionControllerV2.getRecommendations(
                        request,
                        response,
                        PARAM_CAMPAIGN_ID,
                        PARAM_SOME_POSITIONS,
                        PARAM_INVALID_TARGET_COMBINATION,
                        PARAM_REGION_ID,
                        PARAM_V2_EMPTY_OFFERS_SET
                ));
        assertThat(requestException.getMessage(), Matchers.containsString("Illegal targets:"));
        assertThat(requestException.getMessage(), Matchers.containsString("allowed combinations"));
    }

    /**
     * Если передан неверный регион - возваращем bad request.
     * В идеале тут надо делать перебор по таргетам, через {@link org.junit.runners.Parameterized}.
     */
    @Test
    void test_checkParams_when_unavailableRegionSpecified_should_throw() {
        InvalidRequestException requestException = Assertions.assertThrows(InvalidRequestException.class,
                () -> auctionControllerV2.getRecommendations(
                        request,
                        response,
                        PARAM_CAMPAIGN_ID,
                        PARAM_SOME_POSITIONS,
                        PARAM_MODEL_CARD,
                        PARAM_INVALID_REGION_ID,
                        PARAM_V2_EMPTY_OFFERS_SET
                ));
        assertThat(requestException.getMessage(), Matchers.containsString("Invalid region_id:"));
    }

    /**
     * Если для кампании аукцион не включен, возвращаем bad request.
     */
    @Test
    void test_checkParams_when_auctionIsDisabledFroCampaignShop_should_throw() {

        mockAuction();
        mockRegions();
        mockCampaignsToShops();

        AuctionNAReason SOME_NONEMPTY_REASON = AuctionNAReason.OFFLINE_SHOP;
        when(auctionService.canManageAuction(SHOP_WITH_TITLE_OFFERS_IDENTIFICATION))
                .thenReturn(SOME_NONEMPTY_REASON);

        AccessControlException requestException = Assertions.assertThrows(AccessControlException.class,
                () -> auctionControllerV2.getRecommendations(
                        request,
                        response,
                        PARAM_CAMPAIGN_WITH_TITLE_OFFERS_IDENTIFICATION,
                        PARAM_SOME_POSITIONS,
                        PARAM_MODEL_CARD,
                        PARAM_REGION_ID,
                        PARAM_V2_EMPTY_OFFERS_SET
                ));
        assertThat(requestException.getMessage(), Matchers.containsString("Auction is not allowed for campaign"));
    }

    /**
     * Если в запросе слишком много офферов - возвращаем bad request.
     */
    @Test
    public void test_checkParams_when_tooManyOffersPassed_should_throw() {

        mockAuction();
        mockRegions();
        mockCampaignsToShops();

        //большой набор офферов(достаточно только индентифиакторов)
        AuctionOffers paramBigOffersSet = new AuctionOffers();
        Set<AuctionOffer> bigSet = new HashSet<>(MAX_OFFERS_SIZE + 1);
        for (int i = 0; i <= MAX_OFFERS_SIZE; i++) {
            AuctionOffer offer = new AuctionOffer();
            offer.setOfferId(new AuctionOfferId("offer_with_id_" + i));
            bigSet.add(offer);
        }
        paramBigOffersSet.setOffers(bigSet);
        InvalidRequestException requestException = Assertions.assertThrows(InvalidRequestException.class,
                () -> auctionControllerV2.getRecommendations(
                        request,
                        response,
                        PARAM_CAMPAIGN_WITH_TITLE_OFFERS_IDENTIFICATION,
                        PARAM_SOME_POSITIONS,
                        PARAM_MODEL_CARD,
                        PARAM_REGION_ID,
                        paramBigOffersSet
                ));
        assertThat(requestException.getMessage(), Matchers.containsString("Too many offers"));

    }

}
