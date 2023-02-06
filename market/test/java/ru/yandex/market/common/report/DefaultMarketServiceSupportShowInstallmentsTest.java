package ru.yandex.market.common.report;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.report.model.MarketSearchRequest;

@RunWith(Parameterized.class)
public class DefaultMarketServiceSupportShowInstallmentsTest {
    @Parameterized.Parameter
    public Boolean isShowInstallmentsExpectedInQuery;
    @Parameterized.Parameter(1)
    public MarketSearchRequest marketSearchRequest;

    @Parameterized.Parameters
    public static Iterable<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {
                        true,
                        buildMarketSearchRequest(MarketReportPlace.CREDIT_INFO, true)
                },
                {
                        false,
                        buildMarketSearchRequest(MarketReportPlace.CREDIT_INFO, false)
                },
                {
                        null,
                        buildMarketSearchRequest(MarketReportPlace.CREDIT_INFO, null)
                }
        });
    }

    private static MarketSearchRequest buildMarketSearchRequest(MarketReportPlace place, Boolean showInstallments) {
        MarketSearchRequest request = new MarketSearchRequest(place);
        request.setShowInstallments(showInstallments);
        return request;
    }

    @Test
    public void shouldAddShowInstallmentsParam() {
        String paramString = DefaultMarketServiceSupport.searchRequestToParams(marketSearchRequest);
        System.out.println(paramString);
        if (isShowInstallmentsExpectedInQuery == null) {
            Assert.assertFalse(paramString.contains("show-installments="));
        } else {
            Assert.assertTrue(paramString.contains("show-installments=" + (isShowInstallmentsExpectedInQuery ? 1 : 0)));
        }
    }

}
