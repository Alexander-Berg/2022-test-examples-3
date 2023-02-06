package ru.yandex.market.common.report;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.report.model.MarketSearchRequest;
import ru.yandex.market.common.report.model.filter.FilterMarks;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Mikhail Usachev <mailto:musachev@yandex-team.ru>
 *         Date: 26/04/2017.
 */
@RunWith(Parameterized.class)
public class DefaultMarketServiceSupportShowFilterMarkTest {

    @Parameterized.Parameter
    public boolean isShowFilterMarkExpectedInQuery;
    @Parameterized.Parameter(1)
    public MarketSearchRequest marketSearchRequest;

    @Parameterized.Parameters
    public static Iterable<Object[]> testData() throws IOException, ClassNotFoundException {
        return Arrays.asList(new Object[][]{
                {
                        true,
                        buildMarketSearchRequest(MarketReportPlace.OFFER_INFO, buildFilterMarks(true))
                },
                {
                        false,
                        buildMarketSearchRequest(MarketReportPlace.OFFER_INFO, buildFilterMarks(false))
                },
                {
                        false,
                        buildMarketSearchRequest(MarketReportPlace.OFFER_INFO, null)
                },
                {
                        true,
                        buildMarketSearchRequest(MarketReportPlace.CARD, buildFilterMarks(true))
                }
        });
    }

    private static FilterMarks buildFilterMarks(boolean flag) {
        FilterMarks filterMarks = new FilterMarks();
        filterMarks.setSpecifiedForOffer(flag);
        return filterMarks;
    }

    private static MarketSearchRequest buildMarketSearchRequest(MarketReportPlace place, FilterMarks filterMarks) {
        MarketSearchRequest request = new MarketSearchRequest(place);
        request.setPp(MarketSearchRequest.INVISIBLE_PP);
        if (filterMarks != null) {
            request.setItemFilterMarks(filterMarks);
        }
        return request;
    }

    @Test
    public void shouldAddShowFilterMarkParam() {
        String paramString = DefaultMarketServiceSupport.searchRequestToParams(marketSearchRequest);
        System.out.println(paramString);
        Assert.assertEquals(
                isShowFilterMarkExpectedInQuery,
                paramString.contains("show-filter-mark=" + FilterMarks.SPECIFIED_FOR_OFFER)
        );
    }

}
