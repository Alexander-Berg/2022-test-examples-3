package ru.yandex.market.partner.auction.servantlet.bulk.validation;

import java.math.BigInteger;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.common.framework.core.MockServResponse;
import ru.yandex.market.core.auction.model.AuctionBidComponentsLink;
import ru.yandex.market.partner.auction.errors.AuctionWarnings;
import ru.yandex.market.partner.auction.servantlet.bulk.AuctionBulkServantletlMockBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.AUCTION_OFFER_BID_VALUE_1;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.SOME_QUERY;
import static ru.yandex.market.partner.auction.matchers.MockServResponseFeatureMatchers.hasBidUpdateCount;
import static ru.yandex.market.partner.auction.matchers.MockServResponseFeatureMatchers.hasErrors;
import static ru.yandex.market.partner.auction.matchers.MockServResponseFeatureMatchers.hasFinalBidUpdateCount;
import static ru.yandex.market.partner.auction.matchers.MockServResponseFeatureMatchers.hasValidBidCount;
import static ru.yandex.market.partner.auction.matchers.MockServResponseFeatureMatchers.hasWarningOfTypeWithItem;
import static ru.yandex.market.partner.auction.matchers.MockServResponseFeatureMatchers.hasWarnings;

/**
 * Ошибки создания ставок с явно заданным значением.
 *
 * @author vbudnev
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class CreateBidsByValueErrorsTest extends AuctionBulkServantletlMockBase {

    @Before
    public void before() {
        auctionBulkOfferBidsServantlet.configure();
        generalBulkServantletInit(recommendationsService);

        mockBidLimits();
        usefullServResponse = new MockServResponse();

        mockServRequestCrudActionUPDATE();
        mockServRequestIdentificationParams();
        mockRegionsAndTariff();

        mockAuctionHasNoBids();
    }

    /**
     * Нельзя создать ставку без поискового запроса.
     */
    @Test
    public void test_createBid_when_noSearchQuery_should_returnWarning() {
        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&req1.bid.value=" + AUCTION_OFFER_BID_VALUE_1
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        assertThat(usefullServResponse, hasWarningOfTypeWithItem(AuctionWarnings.NOT_VALID_BID_NO_QUERY, SOME_OFFER_NAME));
        assertThat(usefullServResponse, hasValidBidCount(1));
        assertThat(usefullServResponse, hasBidUpdateCount(0));
        assertThat(usefullServResponse, hasFinalBidUpdateCount(0));
    }

    /**
     * Нельзя создать ставк без указания значений для компонент и без цели.
     */
    @Test
    public void test_createBid_when_noBidComponentesandNoGoals_should_returnWarning() {
        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&searchQuery=" + SOME_QUERY
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        assertThat(usefullServResponse, hasWarningOfTypeWithItem(AuctionWarnings.NOT_VALID_BID, SOME_OFFER_NAME));
        assertThat(usefullServResponse, hasValidBidCount(0));
        assertThat(usefullServResponse, hasBidUpdateCount(0));
        assertThat(usefullServResponse, hasFinalBidUpdateCount(0));
    }

    /**
     * Нельзя пропускать явное значение для компоненты, контейнера недостаточно - никакого дефолтного не будет.
     * Рассматриваем на примере bid компоненты.
     */
    @Test
    public void test_createBid_when_incompleteComponentDataInRequest_should_returnWarning() {
        mockServantletPassedArgs("" +
                "searchQuery=" + SOME_QUERY +
                "&req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&req1.bid=" + AUCTION_OFFER_BID_VALUE_1
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        assertThat(usefullServResponse, hasWarningOfTypeWithItem(AuctionWarnings.NOT_VALID_BID, SOME_OFFER_NAME));
        assertThat(usefullServResponse, hasValidBidCount(0));
        assertThat(usefullServResponse, hasBidUpdateCount(0));
        assertThat(usefullServResponse, hasFinalBidUpdateCount(0));
    }

    @DisplayName("Тип связи при создании ставки не имеет значения")
    @Test
    public void test_createBid_when_explicitDefaultLinkType_should_returnWarning() {
        mockServantletPassedArgs("" +
                "searchQuery=" + SOME_QUERY +
                "&req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&req1.bid.value=" + AUCTION_OFFER_BID_VALUE_1 +
                "&req1.linkType=" + AuctionBidComponentsLink.NOT_SPECIFIED
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        assertThat(usefullServResponse, not(hasWarnings()));
        assertThat(usefullServResponse, hasValidBidCount(1));
        assertThat(usefullServResponse, hasBidUpdateCount(1));
        assertThat(usefullServResponse, hasFinalBidUpdateCount(1));
    }

    /**
     * smoke проверка на допустимые значения.
     */
    @Test
    public void test_createBid_when_illegalComponentValue_should_returnWarning() {
        mockServantletPassedArgs("" +
                "searchQuery=" + SOME_QUERY +
                "&req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&req1.bid.value=" + BigInteger.valueOf(-4)
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        assertThat(usefullServResponse, hasWarningOfTypeWithItem(AuctionWarnings.NOT_VALID_BID, SOME_OFFER_NAME));
        assertThat(usefullServResponse, hasValidBidCount(0));
        assertThat(usefullServResponse, hasBidUpdateCount(0));
        assertThat(usefullServResponse, hasFinalBidUpdateCount(0));
    }

    /**
     * Ошибка, при попытке создать ставку с указнием несуществующей группы.
     */
    @Test
    public void test_createBid_when_passedNonExistingGroup_should_returnWarning() {
        mockServantletPassedArgs("" +
                "searchQuery=" + SOME_QUERY +
                "&req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&req1.bid.value=" + 2 +
                "&req1.newGroup=" + 123L
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        assertThat(usefullServResponse, hasErrors());
    }
}
