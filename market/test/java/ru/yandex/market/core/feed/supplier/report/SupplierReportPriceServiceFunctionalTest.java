package ru.yandex.market.core.feed.supplier.report;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.amazonaws.util.StringInputStream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.parser.LiteInputStreamParser;
import ru.yandex.market.common.report.AsyncMarketReportService;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.feed.supplier.report.model.CheckPricesReportResponse;

import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "SupplierReportPriceServiceFunctionalTest.before.csv")
class SupplierReportPriceServiceFunctionalTest extends FunctionalTest {
    private static final Map<Long, String> GET_OFFERS_REPORT_RESPONSES = new HashMap<>() {{
        put(1001L, "json/getOffersPrices_1001.reportResponse.json");
        put(2002L, "json/getOffersPrices_2002.reportResponse.json");
    }};

    @Autowired
    private SupplierReportPriceService supplierReportPriceService;

    @Autowired
    @Qualifier("marketReportService")
    private AsyncMarketReportService marketReportService;

    @Test
    void getOffersPricesInfoForSupplierTest() {
        long supplierId = 2002;
        Set<String> skuSet = Set.of("offerId_2002_2224_1", "offerId_2002_2224_2", "offerId_2002_2225_1");
        boolean lowLatency = false;

        mockNotEmptyOffersPricesReportResponse(lowLatency, supplierId);
        Map<String, CheckPricesReportResponse> response =
                supplierReportPriceService.getOffersPricesInfo(supplierId, skuSet, lowLatency).join().stream()
                        .map(Map::entrySet)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toMap(
                                Map.Entry::getKey, Map.Entry::getValue
                        ));

        Assertions.assertEquals(3, response.size());
        MatcherAssert.assertThat(response.keySet(), Matchers.containsInAnyOrder(skuSet.toArray()));
        MatcherAssert.assertThat(
                response.values(),
                Matchers.containsInAnyOrder(
                        new CheckPricesReportResponse.Builder()
                                .setOfferId("offerId_2002_2224_1")
                                .setReportPrice(new BigDecimal(9000))
                                .build(),
                        new CheckPricesReportResponse.Builder()
                                .setOfferId("offerId_2002_2224_2")
                                .setReportPrice(new BigDecimal(7500))
                                .build(),
                        new CheckPricesReportResponse.Builder()
                                .setOfferId("offerId_2002_2225_1")
                                .setReportPrice(new BigDecimal(60))
                                .build()
                )
        );
    }

    @Test
    void getOffersPricesInfoForDbsTest() {
        long supplierId = 1001;
        Set<String> skuSet = Set.of("offerId_1001_1221_1", "offerId_1001_1221_2", "offerId_1001_1441_1");
        boolean lowLatency = true;

        mockNotEmptyOffersPricesReportResponse(lowLatency, supplierId);
        Map<String, CheckPricesReportResponse> response =
                supplierReportPriceService.getOffersPricesInfo(supplierId, skuSet, lowLatency).join().stream()
                        .map(Map::entrySet)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toMap(
                                Map.Entry::getKey, Map.Entry::getValue
                        ));;

        Assertions.assertEquals(3, response.size());
        MatcherAssert.assertThat(response.keySet(), Matchers.containsInAnyOrder(skuSet.toArray()));
        MatcherAssert.assertThat(
                response.values(),
                Matchers.containsInAnyOrder(
                        new CheckPricesReportResponse.Builder()
                                .setOfferId("offerId_1001_1221_1")
                                .setReportPrice(new BigDecimal(15000))
                                .build(),
                        new CheckPricesReportResponse.Builder()
                                .setOfferId("offerId_1001_1221_2")
                                .setReportPrice(new BigDecimal(4000))
                                .build(),
                        new CheckPricesReportResponse.Builder()
                                .setOfferId("offerId_1001_1441_1")
                                .setReportPrice(new BigDecimal(500))
                                .build()
                )
        );
    }

    private void mockNotEmptyOffersPricesReportResponse(boolean lowLatency, long partnerId) {
        String reportResponse = StringTestUtil.getString(this.getClass(), GET_OFFERS_REPORT_RESPONSES.get(partnerId));
        mockOffersPricesReportResponse(lowLatency, reportResponse);
    }

    private void mockOffersPricesReportResponse(boolean lowLatency, String reportResponse) {
        MarketReportPlace place = lowLatency ?
                MarketReportPlace.CHECK_PRICES_LOW_LATENCY : MarketReportPlace.CHECK_PRICES;

        when(marketReportService.async(
                ArgumentMatchers.argThat(request -> request != null && request.getPlace() == place),
                Mockito.any()
        )).then(invocation -> {
            LiteInputStreamParser<?> parser = invocation.getArgument(1);
            CompletableFuture<Object> future = new CompletableFuture<>();
            Object result = parser == null ? null : parser.parse(new StringInputStream(reportResponse));
            future.complete(result);
            return future;
        });
    }
}
