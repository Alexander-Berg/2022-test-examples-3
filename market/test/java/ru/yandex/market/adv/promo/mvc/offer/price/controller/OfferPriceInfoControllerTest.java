package ru.yandex.market.adv.promo.mvc.offer.price.controller;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.adv.promo.utils.CommonTestUtils;
import ru.yandex.market.common.parser.LiteInputStreamParser;
import ru.yandex.market.common.report.AsyncMarketReportService;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
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

public class OfferPriceInfoControllerTest  extends FunctionalTest {

    @Autowired
    private MbiApiClient mbiApiClient;

    @Autowired
    private AsyncMarketReportService marketReportService;

    @Test
    @DisplayName("Проверка корректности получения расширенной информации о ценах офферов для указанных ssku")
    void getFullPriceInfoForSskuListTest() {
        long partnerId = 12345;
        String recommendationResponse = "recommendedPrices_response.json";
        String pricesResponse = "pricesInfo_response.json";
        String sskuInfoRequest = "sskuInfo_request.json";
        String fullPriceInfoResponse = "fullPriceInfo_response.json";

        doReturn(createPartnerInfo())
                .when(mbiApiClient).getPartnerInfo(any(Long.class));

        doReturn(createPartnerFulfillments())
                .when(mbiApiClient).getPartnerFulfillments(any(Long.class));

        mockMarketReportServiceResponse(recommendationResponse, MarketReportPlace.PRICE_RECOMMENDER_LOW_LATENCY);
        mockMarketReportServiceResponse(pricesResponse, MarketReportPlace.CHECK_PRICES_LOW_LATENCY);

        String requestBody = CommonTestUtils.getResource(this.getClass(), sskuInfoRequest);
        ResponseEntity<String> response = sendGetFullPriceInfoForSskuListRequest(partnerId, requestBody);

        String expected = CommonTestUtils.getResource(this.getClass(), fullPriceInfoResponse);
        JSONAssert.assertEquals(expected, response.getBody(), true);
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

    private ResponseEntity<String> sendGetFullPriceInfoForSskuListRequest(
            long partnerId,
            Object body
    ) {
        return FunctionalTestHelper.post(baseUrl() + "/partner/offer/price-info/by-ssku?partnerId=" + partnerId,
                new HttpEntity<>(body, getDefaultHeaders())
        );
    }
}
