package ru.yandex.market.partner.auction.servantlet.bulk.actions;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.common.framework.core.MockServResponse;
import ru.yandex.market.core.auction.err.AuctionGroupOwnershipViolationException;
import ru.yandex.market.core.auction.err.BidIdTypeConflictException;
import ru.yandex.market.core.auction.err.BidValueLimitsViolationException;
import ru.yandex.market.core.auction.err.InvalidOfferNameException;
import ru.yandex.market.core.auction.err.InvalidSearchQueryException;
import ru.yandex.market.core.auction.model.AuctionOfferBid;
import ru.yandex.market.core.auction.model.BidPlace;
import ru.yandex.market.partner.auction.BulkReadQueryType;
import ru.yandex.market.partner.auction.servantlet.bulk.AuctionBulkServantletlMockBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.core.auction.AuctionService.DEFAULT_GROUP_ID;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasGroupId;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasId;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasPlaceBid;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasSearchQuery;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.AUCTION_OFFER_BID_VALUE_1;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.AUCTION_OFFER_BID_VALUE_2;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.AUCTION_OFFER_BID_VALUE_3;
import static ru.yandex.market.partner.auction.matchers.MockServResponseFeatureMatchers.hasBidUpdateCount;
import static ru.yandex.market.partner.auction.matchers.MockServResponseFeatureMatchers.hasFinalBidUpdateCount;
import static ru.yandex.market.partner.auction.matchers.MockServResponseFeatureMatchers.hasFoundBidCount;
import static ru.yandex.market.partner.auction.matchers.MockServResponseFeatureMatchers.hasWarnings;

/**
 * @author vbudnev
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class UpdateBidsSearchQueriesTest extends AuctionBulkServantletlMockBase {
    @Before
    public void before() {
        auctionBulkOfferBidsServantlet.configure();
        generalBulkServantletInit(null);
        usefullServResponse = new MockServResponse();

        mockServRequestCrudActionUPDATE();
        mockServRequestIdentificationParams();
        mockRegionsAndTariff();
    }

    /**
     * Для существующих в хранилище ставок, при обновлении searchQuery на всякий случай проверяем,
     * что не затрагивается/не сбрасывается значение компонент ставок.
     * При обновлении поисквый запрос использутеся пооферный, а не общий.
     */
    @Test
    public void test_updateBid_when_searchQueryModification_should_notAffectOtherFields() {
        mockAuctionExistingBid(SOME_TITLE_OFFER_ID, PARAM_DATASOURCE_ID);

        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&req1.searchQuery=new_query" +
                "&type=" + BulkReadQueryType.UPDATE_QUERY +
                "&searchQuery=someGeneralQuery"
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        List<AuctionOfferBid> setOfferBidsArgument = extractAuctionSetOffersBids();
        assertThat(setOfferBidsArgument, hasSize(1));
        AuctionOfferBid passedBidValue = setOfferBidsArgument.get(0);

        assertThat(passedBidValue, hasId(SOME_TITLE_OFFER_ID));
        assertThat(passedBidValue, hasGroupId(DEFAULT_GROUP_ID));
        assertThat(passedBidValue, hasSearchQuery("new_query"));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.SEARCH, AUCTION_OFFER_BID_VALUE_1));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.CARD, AUCTION_OFFER_BID_VALUE_2));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.MARKET_PLACE, AUCTION_OFFER_BID_VALUE_3));

        assertThat(usefullServResponse, not(hasWarnings()));
        assertThat(usefullServResponse, hasBidUpdateCount(1));
        assertThat(usefullServResponse, hasFoundBidCount(1));
        assertThat(usefullServResponse, hasFinalBidUpdateCount(1));
    }

    /**
     * Если в хранилище биддинга, ставка отсутствует, то нет вызова на установку + отдает корректный счетчик 0.
     */
    @Test
    public void test_updateBid_when_noExistingBid_should_notCallUselessSetMethod() throws BidIdTypeConflictException, InvalidSearchQueryException, AuctionGroupOwnershipViolationException, BidValueLimitsViolationException, InvalidOfferNameException {
        mockAuctionHasNoBids();

        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&req1.searchQuery=new_query" +
                "&type=" + BulkReadQueryType.UPDATE_QUERY
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        verify(auctionService, never())
                .setOfferBids(anyLong(), any(), anyLong());

        assertThat(usefullServResponse, not(hasWarnings()));
        assertThat(usefullServResponse, hasBidUpdateCount(1));
        assertThat(usefullServResponse, hasFoundBidCount(0));
        assertThat(usefullServResponse, hasFinalBidUpdateCount(0));
    }

}
