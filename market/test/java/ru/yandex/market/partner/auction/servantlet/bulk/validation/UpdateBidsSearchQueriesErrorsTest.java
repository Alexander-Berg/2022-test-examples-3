package ru.yandex.market.partner.auction.servantlet.bulk.validation;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.partner.auction.BulkReadQueryType;
import ru.yandex.market.partner.auction.servantlet.bulk.AuctionBulkServantletlMockBase;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.hamcrest.core.AllOf.allOf;
import static ru.yandex.market.partner.auction.matchers.ErrorInfoExceptionMatchers.hasErrorCode;
import static ru.yandex.market.partner.auction.matchers.ErrorInfoExceptionMatchers.hasErrorMessage;

/**
 * Тесты на обновление поисковых запросов для ТП.
 *
 * @author vbudnev
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class UpdateBidsSearchQueriesErrorsTest extends AuctionBulkServantletlMockBase {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void before() {
        auctionBulkOfferBidsServantlet.configure();
        generalBulkServantletInit(recommendationsService);

        mockServRequestCrudActionUPDATE();
        mockServRequestIdentificationParams();
        mockRegionsAndTariff();
    }

    @Test
    public void test_updateSearchQuery_when_noSearchQuery_should_throw() {

        expectedException.expect(
                allOf(
                        hasErrorCode(SC_BAD_REQUEST),
                        hasErrorMessage("Empty search query for: someOfferName")
                )
        );

        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&type=" + BulkReadQueryType.UPDATE_QUERY
        );

        auctionBulkOfferBidsServantlet.process(servRequest, servResponse);
    }

}
