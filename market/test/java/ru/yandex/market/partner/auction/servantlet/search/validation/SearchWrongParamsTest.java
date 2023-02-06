package ru.yandex.market.partner.auction.servantlet.search.validation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.core.error.ErrorInfoException;
import ru.yandex.market.partner.auction.SearchAuctionOffersServantlet;
import ru.yandex.market.partner.auction.servantlet.AuctionServantletMockBase;

/**
 * Тесты поведения сервантлета {@link SearchAuctionOffersServantlet} при некорректно заданных параметрах:
 * - ошибки некорректно заданного региона
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class SearchWrongParamsTest extends AuctionServantletMockBase {
    @InjectMocks
    private SearchAuctionOffersServantlet searchAuctionOffersServantlet;

    @Before
    public void before() {
        mockRegionsAndTariff();
    }

    @Test(expected = ErrorInfoException.class)
    public void test_regionValidation_when_invalidRegionPassedToSearchRequest_should_throw() {
        mockServRequestCrudActionREAD();
        mockServRequestIdentificationParams();

        mockServantletPassedArgs("regionId=" + PARAM_INCORRECT_REGION_ID);

        searchAuctionOffersServantlet.process(servRequest, servResponse);
    }

}