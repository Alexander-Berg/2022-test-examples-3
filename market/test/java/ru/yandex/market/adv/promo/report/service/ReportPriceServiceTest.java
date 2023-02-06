package ru.yandex.market.adv.promo.report.service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.adv.promo.report.dto.PriceRecommendationDTO;
import ru.yandex.market.adv.promo.report.dto.PriceRecommendationsDTO;
import ru.yandex.market.adv.promo.report.model.CheckPricesReportResponse;
import ru.yandex.market.adv.promo.report.model.PriceSuggestType;
import ru.yandex.market.adv.promo.report.model.PriceSuggestion;
import ru.yandex.market.common.parser.LiteInputStreamParser;
import ru.yandex.market.common.report.AsyncMarketReportService;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.delivery.DeliveryServiceType;
import ru.yandex.market.core.manager.dto.ManagerInfoDTO;
import ru.yandex.market.core.orginfo.model.OrganizationInfoSource;
import ru.yandex.market.core.orginfo.model.OrganizationType;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerFulfillmentLinkDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerFulfillmentLinksDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerInfoDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerOrgInfoDTO;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class ReportPriceServiceTest extends FunctionalTest {

    @Autowired
    private MbiApiClient mbiApiClient;

    @Autowired
    private AsyncMarketReportService marketReportService;

    @Autowired
    private ReportPriceService reportPriceService;

    @Test
    @DisplayName("Проверка корректности получения рекомендованных цен")
    void getRecommendedPriceTest() {
        Long marketSku = 12345678L;
        String reportResponse = "recommendedPrices_reportResponse.json";
        MarketReportPlace place = MarketReportPlace.PRICE_RECOMMENDER_LOW_LATENCY;

        mockMarketReportServiceResponse(reportResponse, place);

        PriceRecommendationsDTO response =
                reportPriceService.getRecommendedPrice(marketSku).join().get(0);

        Assertions.assertEquals(marketSku, response.getMarketSku());
        MatcherAssert.assertThat(
                response.getPriceRecommendations(),
                Matchers.containsInAnyOrder(
                        new PriceRecommendationDTO(
                                new BigDecimal(100),
                                "minPriceMarket"
                        ),
                        new PriceRecommendationDTO(
                                new BigDecimal(200),
                                "buybox"
                        ),
                        new PriceRecommendationDTO(
                                new BigDecimal(300),
                                "maxOldPrice"
                        )
                )
        );
    }

    @Test
    @DisplayName("Проверка корректности получения информации о ценах")
    void getOffersPricesInfoTest() {
        long partnerId = 12345;
        String reportResponse = "pricesInfo_reportResponse.json";
        MarketReportPlace place = MarketReportPlace.CHECK_PRICES_LOW_LATENCY;
        Set<String> skuSet = Set.of("11111", "22222");

        doReturn(createPartnerInfo())
                .when(mbiApiClient).getPartnerInfo(any(Long.class));

        doReturn(createPartnerFulfillments())
                .when(mbiApiClient).getPartnerFulfillments(any(Long.class));

        mockMarketReportServiceResponse(reportResponse, place);

        Map<String, CheckPricesReportResponse> response =
                reportPriceService.getOffersPricesInfo(partnerId, skuSet).join().stream()
                        .map(Map::entrySet)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toMap(
                                Map.Entry::getKey, Map.Entry::getValue
                        ));

        Assertions.assertEquals(2, response.size());
        MatcherAssert.assertThat(response.keySet(), Matchers.containsInAnyOrder(skuSet.toArray()));
        MatcherAssert.assertThat(
                response.values(),
                Matchers.containsInAnyOrder(
                        new CheckPricesReportResponse.Builder()
                                .setOfferId("11111")
                                .setReportPrice(new BigDecimal(1200))
                                .build(),
                        new CheckPricesReportResponse.Builder()
                                .setOfferId("22222")
                                .setReportPrice(new BigDecimal(1500))
                                .build()
                )
        );
    }

    @Test
    @DisplayName("Проверка корректности получения рекомендованных цен по списку msku")
    void suggestPricesForMskuTest() {
        String reportResponse = "suggestedPrices_reportResponse.json";
        MarketReportPlace place = MarketReportPlace.PRICE_RECOMMENDER_LOW_LATENCY;
        Long msku  = 11111L;
        Set<Long> mskuSet = Set.of(msku);

        mockMarketReportServiceResponse(reportResponse, place);

        Map<Long, Set<PriceSuggestion>> response =
                reportPriceService.suggestPricesForMsku(mskuSet).join().get(0);

        MatcherAssert.assertThat(response.keySet(), Matchers.containsInAnyOrder(mskuSet.toArray()));
        MatcherAssert.assertThat(
                response.get(msku),
                Matchers.containsInAnyOrder(
                        new PriceSuggestion(
                                PriceSuggestType.MIN_PRICE_MARKET,
                                new BigDecimal(100)
                        ),
                        new PriceSuggestion(
                                PriceSuggestType.BUYBOX,
                                new BigDecimal(200)
                        ),
                        new PriceSuggestion(
                                PriceSuggestType.MAX_OLD_PRICE,
                                new BigDecimal(300)
                        )
                )
        );
    }

    private void mockMarketReportServiceResponse(
            String reportResponse,
            MarketReportPlace place
    ) {
        when(marketReportService.async(
                ArgumentMatchers.argThat(request -> request != null && request.getPlace() == place),
                Mockito.any()
        )).then(invocation -> {
            LiteInputStreamParser<?> parser = invocation.getArgument(1);
            CompletableFuture<Object> future = new CompletableFuture<>();
            Object result = parser == null ? null : parser.parse(getResource(reportResponse));
            future.complete(result);
            return future;
        });
    }

    private InputStream getResource(String name) {
        Class<?> testClass = this.getClass();
        return testClass.getResourceAsStream(testClass.getSimpleName() + "/" + name);
    }

    PartnerFulfillmentLinksDTO createPartnerFulfillments() {
        Collection<PartnerFulfillmentLinkDTO> fullfillments = new ArrayList<>();
        fullfillments.add(
                new PartnerFulfillmentLinkDTO(12345, 1, 123L, DeliveryServiceType.FULFILLMENT));
        fullfillments.add(
                new PartnerFulfillmentLinkDTO(12345, 1, 567L, DeliveryServiceType.FULFILLMENT));
        fullfillments.add(
                new PartnerFulfillmentLinkDTO(12345, 1, 789L, DeliveryServiceType.FULFILLMENT));

        return new PartnerFulfillmentLinksDTO(fullfillments);
    }

    PartnerInfoDTO createPartnerInfo() {
        ManagerInfoDTO manager = new ManagerInfoDTO(123, "test", "email", "phone", "staffEmail");
        PartnerOrgInfoDTO orgInfo = new PartnerOrgInfoDTO(
                OrganizationType.OOO,
                "test",
                "test",
                "address",
                "jaddress",
                OrganizationInfoSource.YANDEX_MARKET,
                "123",
                "url");

        return new PartnerInfoDTO(
                1,
                2L,
                CampaignType.SUPPLIER,
                "test",
                "test",
                "123",
                "address",
                orgInfo,
                false, manager
        );
    }
}
