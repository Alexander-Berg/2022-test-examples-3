package ru.yandex.market.api.partner.controllers.auction.controller.v2;

import java.util.Collections;

import com.google.common.collect.ImmutableList;

import ru.yandex.market.api.partner.controllers.auction.controller.AuctionControllerBaseMock;
import ru.yandex.market.api.partner.controllers.auction.dto.OfferIdDto;
import ru.yandex.market.api.partner.controllers.auction.dto.OfferSetViaRecommendationsRequestDto;
import ru.yandex.market.api.partner.controllers.auction.model.AuctionOffers;
import ru.yandex.market.common.report.model.MarketSearchRequest;

/**
 * Базовые mock-и компоннет и константы для тестов v2 контроллера.
 */
public class AuctionControllerV2BaseMock extends AuctionControllerBaseMock {

    /**
     * Не учитывать регион в поиске. См. {@link MarketSearchRequest#setRegionId(Long)}
     */
    public static final long REGARDLESS_OF_THE_REGION = 0L;

    protected static final AuctionOffers PARAM_V2_EMPTY_OFFERS_SET = createEmptyOffers();
    protected static final int MAX_OFFERS_SIZE = 500;

    protected final static OfferSetViaRecommendationsRequestDto PUT_RECS_BY_ID_DTO = new OfferSetViaRecommendationsRequestDto(
            ImmutableList.of(
                    new OfferIdDto(null, 100L, "someOfferName1", "query"),
                    new OfferIdDto(null, 10L, "someOfferName2", "query2")
            )
    );

    private static AuctionOffers createEmptyOffers() {
        final AuctionOffers offers = new AuctionOffers();
        offers.setOffers(Collections.emptySet());
        return offers;
    }

}
