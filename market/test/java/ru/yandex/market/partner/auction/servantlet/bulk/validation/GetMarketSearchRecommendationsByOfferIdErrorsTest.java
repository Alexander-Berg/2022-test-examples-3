package ru.yandex.market.partner.auction.servantlet.bulk.validation;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.core.auction.AuctionService;
import ru.yandex.market.partner.auction.BulkReadQueryType;
import ru.yandex.market.partner.auction.servantlet.bulk.AuctionBulkServantletlMockBase;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.hamcrest.core.AllOf.allOf;
import static ru.yandex.market.partner.auction.matchers.ErrorInfoExceptionMatchers.hasErrorCode;
import static ru.yandex.market.partner.auction.matchers.ErrorInfoExceptionMatchers.hasErrorMessage;

/**
 * Проверяем, что связный запрос в репорт при получении маркетных рекомендациях по идентификатору ТП
 * содержит ожидаемый набор аттрибутов для ТП.
 * ТП берется из мока ставок для группы в {@link AuctionService}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class GetMarketSearchRecommendationsByOfferIdErrorsTest extends AuctionBulkServantletlMockBase {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();


    @Before
    public void before() {
        auctionBulkOfferBidsServantlet.configure();
        generalBulkServantletInit(recommendationsService);

        mockBidLimits();
        mockRegionsAndTariff();

        mockAuctionServicePartialBidsForDefaultGroup(true);
        mockCheckHomeRegionInIndex();
        mockRecommendationServiceEmptyCalculateResult();

        mockServRequestCrudActionREAD();
        mockServRequestIdentificationParams();
    }

    /**
     * При запросе по конкретным офферам, поисковый запрос должен бять явно указан per offer.
     * Никакого "общего" в этом случае не существует.
     */
    @Test
    public void test_getMSRecommendationsByOffer_when_noQuery_should_throw() {
        expectedException.expect(
                allOf(
                        hasErrorCode(SC_BAD_REQUEST),
                        hasErrorMessage("Search query must be specified and be nonempty for offer: someOfferName2")
                )
        );

        mockServantletPassedArgs("" +
                "type=" + BulkReadQueryType.MARKET_SEARCH_REC_OFFER +
                "&req.size=2" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&req1.searchQuery=" + SOME_SEARCH_QUERY +
                "&req2.offerName=" + SOME_OFFER_NAME + 2
        );

        auctionBulkOfferBidsServantlet.process(servRequest, servResponse);
    }
}