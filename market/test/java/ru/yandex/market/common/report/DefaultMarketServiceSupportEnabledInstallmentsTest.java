package ru.yandex.market.common.report;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.report.model.MarketSearchRequest;

@RunWith(Parameterized.class)
public class DefaultMarketServiceSupportEnabledInstallmentsTest {
    @Parameterized.Parameter
    public Boolean isEnabledInstallmentsExpectedInQuery;
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

    private static MarketSearchRequest buildMarketSearchRequest(
            MarketReportPlace place,
            Boolean isEnabledInstallments
    ) {
        MarketSearchRequest request = new MarketSearchRequest(place);
        request.setEnableInstallments(isEnabledInstallments);
        return request;
    }

    @Test
    public void shouldAddEnabledInstallmentsParam() {
        String paramString = DefaultMarketServiceSupport.searchRequestToParams(marketSearchRequest);
        System.out.println(paramString);
        if (isEnabledInstallmentsExpectedInQuery == null) {
            Assert.assertFalse(paramString.contains("enable_installments="));
        } else {
            Assert.assertTrue(paramString.contains("enable_installments=" + (isEnabledInstallmentsExpectedInQuery ?
                    1 : 0)));
        }
    }

}
