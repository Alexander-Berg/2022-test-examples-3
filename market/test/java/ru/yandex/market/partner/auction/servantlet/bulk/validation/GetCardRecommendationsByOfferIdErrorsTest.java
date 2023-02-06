package ru.yandex.market.partner.auction.servantlet.bulk.validation;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.partner.auction.AuctionBulkOfferBidsServantlet;
import ru.yandex.market.partner.auction.BulkReadQueryType;
import ru.yandex.market.partner.auction.servantlet.bulk.AuctionBulkServantletlMockBase;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.hamcrest.core.AllOf.allOf;
import static ru.yandex.market.partner.auction.matchers.ErrorInfoExceptionMatchers.hasErrorCode;
import static ru.yandex.market.partner.auction.matchers.ErrorInfoExceptionMatchers.hasErrorMessage;

/**
 * Тесты поведения сервантлета {@link AuctionBulkOfferBidsServantlet} при некорректно заданных параметрах:
 * - ошибки некорректно заданного региона
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class GetCardRecommendationsByOfferIdErrorsTest extends AuctionBulkServantletlMockBase {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void before() {
        auctionBulkOfferBidsServantlet.configure();
        auctionBulkOfferBidsServantlet.setAuctionBulkValidator(auctionBulkValidator);
        auctionBulkOfferBidsServantlet.setBidComponentsInferenceManager(bidComponentsInferenceManager);

        mockRegionsAndTariff();
    }

    /**
     * Проверяем, что при явном задании некорректного региона в запросе оффера,
     * ответ сервантлета содержит ошибку.
     */
    @Test
    public void test_getCardRecommendationsByOffer_when_invalidRegionPassedToOfferCardRecs_should_throw() {
        expectedException.expect(
                allOf(
                        hasErrorCode(SC_BAD_REQUEST),
                        hasErrorMessage("Explicit regionId=123456 is incorrect")
                )
        );

        mockServRequestCrudActionREAD();
        mockServRequestIdentificationParams();
        mockServantletPassedArgs("" +
                "type=" + BulkReadQueryType.HYBRID_REC_OFFER +
                "&req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&regionId=" + PARAM_INCORRECT_REGION_ID
        );

        auctionBulkOfferBidsServantlet.process(servRequest, servResponse);
    }

}