package ru.yandex.market.api.partner.controllers.auction.controller.v2.bids.recommended.put.params;

import java.security.AccessControlException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.market.api.partner.auth.AuthPrincipal;
import ru.yandex.market.api.partner.controllers.auction.AuctionControllerV2;
import ru.yandex.market.api.partner.controllers.auction.controller.v2.AuctionControllerV2BaseMock;
import ru.yandex.market.api.partner.controllers.auction.dto.OfferIdDto;
import ru.yandex.market.api.partner.controllers.auction.dto.OfferSetViaRecommendationsRequestDto;
import ru.yandex.market.api.partner.controllers.auction.model.RecommendationTarget;
import ru.yandex.market.api.partner.request.InvalidRequestException;
import ru.yandex.market.core.auction.model.AuctionNAReason;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.api.partner.controllers.auction.controller.v2.bids.recommended.RecommendedCommon.PARAM_MAX_VALUE_NOT_SPECIFIED;
import static ru.yandex.market.api.partner.controllers.auction.controller.v2.bids.recommended.RecommendedCommon.PARAM_OFFSET_NOT_SPECIFIED;
import static ru.yandex.market.api.partner.controllers.auction.controller.v2.bids.recommended.RecommendedCommon.PARAM_OFFSET_PCT_NOT_SPECIFIED;
import static ru.yandex.market.api.partner.controllers.auction.controller.v2.bids.recommended.RecommendedCommon.PARAM_SOME_VALID_POS;

/**
 * Обработка некорректных параметров в ручках установки рекомендованных значений.
 */
@ExtendWith(MockitoExtension.class)
class InvalidArgumentsTest extends AuctionControllerV2BaseMock {


    @InjectMocks
    private AuctionControllerV2 auctionControllerV2;

    @Mock
    private AuthPrincipal authPrincipal;

    @BeforeEach
    void before() {
        auctionControllerV2.setMaxOffersBulkSize(MAX_OFFERS_SIZE);
    }

    /**
     * Обработка верного синтаксически, но не поддерживаемого в запросе на установку таргета.
     */
    @Test
    void test_checkParams_when_unknownTargetPassed_should_throw() {
        InvalidRequestException requestException = Assertions.assertThrows(
                InvalidRequestException.class,
                () -> auctionControllerV2.setBidsViaRecommendations(
                        request,
                        PARAM_CAMPAIGN_WITH_TITLE_OFFERS_IDENTIFICATION,
                        PARAM_SOME_VALID_POS,
                        PARAM_MAX_VALUE_NOT_SPECIFIED,
                        PARAM_OFFSET_NOT_SPECIFIED,
                        PARAM_OFFSET_PCT_NOT_SPECIFIED,
                        RecommendationTarget.MODEL_CARD_CPA.getCode(),
                        PARAM_REGION_NOT_SPECIFIED,
                        PUT_RECS_BY_ID_DTO,
                        authPrincipal
                ));

        assertThat(requestException.getMessage(), Matchers.containsString("Illegal target: MODEL-CARD-CPA; allowed targets MODEL-CARD,SEARCH"));

    }

    /**
     * Если для кампании аукцион не включен, возвращаем bad request.
     * На промере одного из валидных таргетов.
     */
    @Test
    void test_checkParams_when_unavailableRegionSpecified_should_thro() {

        mockAuction();
        mockRegions();
        mockCampaignsToShops();

        AuctionNAReason SOME_NONEMPTY_REASON = AuctionNAReason.OFFLINE_SHOP;
        when(auctionService.canManageAuction(SHOP_WITH_TITLE_OFFERS_IDENTIFICATION))
                .thenReturn(SOME_NONEMPTY_REASON);

        AccessControlException requestException = Assertions.assertThrows(
                AccessControlException.class,
                () -> auctionControllerV2.setBidsViaRecommendations(
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
                ));
        assertThat(requestException.getMessage(), Matchers.containsString("Auction is not allowed for campaign"));
    }

    /**
     * Если в запросе слишком много офферов - возвращаем bad request.
     */
    @DisplayName("Bad request если слишком много офферов в запросе")
    @Test
    void test_checkParams_when_auctionIsDisabledFroCampaignShop_should_throw() {

        mockAuction();
        mockRegions();
        mockCampaignsToShops();

        //большой набор офферов(достаточно только индентифиакторов)
        final List<OfferIdDto> tooManyOffers = IntStream.rangeClosed(0, MAX_OFFERS_SIZE)
                .mapToObj(i -> new OfferIdDto(null, null, "offer_with_name_" + i, null))
                .collect(Collectors.toList());

        final OfferSetViaRecommendationsRequestDto tooMayOffersReq = new OfferSetViaRecommendationsRequestDto(
                tooManyOffers);

        InvalidRequestException requestException = Assertions.assertThrows(
                InvalidRequestException.class,
                () -> auctionControllerV2.setBidsViaRecommendations(
                        request,
                        PARAM_CAMPAIGN_WITH_TITLE_OFFERS_IDENTIFICATION,
                        PARAM_SOME_VALID_POS,
                        PARAM_MAX_VALUE_NOT_SPECIFIED,
                        PARAM_OFFSET_NOT_SPECIFIED,
                        PARAM_OFFSET_PCT_NOT_SPECIFIED,
                        RecommendationTarget.SEARCH.getCode(),
                        PARAM_REGION_NOT_SPECIFIED,
                        tooMayOffersReq,
                        authPrincipal
                ));
        assertThat(requestException.getMessage(), Matchers.containsString("Too many offers"));
    }

}
