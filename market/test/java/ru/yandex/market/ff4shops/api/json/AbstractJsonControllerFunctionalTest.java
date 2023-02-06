package ru.yandex.market.ff4shops.api.json;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.checkout.pushapi.client.entity.stock.StockType;
import ru.yandex.market.ff4shops.api.model.ErrorSubCode;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.offer.datacamp.DataCampOfferService;
import ru.yandex.market.ff4shops.offer.model.PartnerOffer;
import ru.yandex.market.ff4shops.offer.model.PartnerOfferSlice;
import ru.yandex.market.ff4shops.util.JsonTestUtil;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

public abstract class AbstractJsonControllerFunctionalTest extends FunctionalTest {

    protected static final ImmutableSet<String> IGNORED_FIELDS = ImmutableSet.of("host", "timestamp");
    private static final String MBI_URL_STATE = "%s/partners/ff4shops/states/%d";

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    @Value("${mbi.api.url}")
    protected String mbiApiUrl;

    @Autowired
    @Qualifier("mbiApiRestTemplate")
    private RestTemplate mbiApiRestTemplate;

    protected MockRestServiceServer mbiApiMockRestServiceServer;

    @Autowired
    @Qualifier("pushApiRestTemplate")
    protected RestTemplate pushApiRestTemplate;

    protected MockRestServiceServer pushApiMockRestServiceServer;

    @Autowired
    @Value("${market.checkout.pushapi.url}")
    protected String pushApiUrl;

    @Autowired
    protected LMSClient lmsClient;

    @Autowired
    protected DataCampOfferService dataCampOfferService;

    @BeforeEach
    void initMocks() {
        mbiApiMockRestServiceServer = MockRestServiceServer.createServer(mbiApiRestTemplate);
        pushApiMockRestServiceServer = MockRestServiceServer.bindTo(pushApiRestTemplate)
                .ignoreExpectOrder(true)
                .build();
    }

    static class TestStock {
        private String sku;
        private long deliveryServiceId;
        private List<TestItem> items;

        private TestStock(String sku, long deliveryServiceId, List<TestItem> items) {
            this.sku = sku;
            this.deliveryServiceId = deliveryServiceId;
            this.items = items;
        }

        static TestStock of(String sku, long deliveryServiceId, List<TestItem> items) {
            return new TestStock(sku, deliveryServiceId, items);
        }

        private String getSku() {
            return sku;
        }

        private long getDeliveryServiceId() {
            return deliveryServiceId;
        }

        private List<TestItem> getItems() {
            return items;
        }
    }

    static class TestItem {
        private long count;
        private StockType type;
        private ZonedDateTime updatedAt;

        private TestItem(long count, StockType type, ZonedDateTime updatedAt) {
            this.count = count;
            this.type = type;
            this.updatedAt = updatedAt;
        }

        static TestItem of(long count, StockType type) {
            return new TestItem(count, type, ZonedDateTime.now());
        }

        private long getCount() {
            return count;
        }

        private StockType getType() {
            return type;
        }

        private ZonedDateTime getUpdatedAt() {
            return updatedAt;
        }
    }

    void mockPushApi(
            long supplierId,
            long deliveryServiceId,
            List<String> offers,
            List<TestStock> responseItemStocks
    ) {
        pushApiMockRestServiceServer = MockRestServiceServer.createServer(pushApiRestTemplate);

        StringBuilder xmlSkusBuilder = new StringBuilder();
        for (String offer : offers) {
            xmlSkusBuilder.append("<sku>").append(offer).append("</sku>");
        }

        StringBuilder stocksXmlBuilder = new StringBuilder();
        for (TestStock stock : responseItemStocks) {
            stocksXmlBuilder
                    .append("<stock ")
                    .append("sku=").append("'").append(stock.getSku()).append("'").append(" ")
                    .append("warehouseId=").append("'").append(stock.getDeliveryServiceId()).append("'")
                    .append(">");

            stocksXmlBuilder.append("<items>");
            for (TestItem item : stock.getItems()) {
                stocksXmlBuilder
                        .append("<item ")
                        .append("count=").append("'").append(item.getCount()).append("'").append(" ")
                        .append("type=").append("'").append(item.getType()).append("'").append(" ")
                        .append("updatedAt=").append("'").append(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(item.getUpdatedAt())).append("'")
                        .append("/>");
            }
            stocksXmlBuilder
                    .append("</items>")
                    .append("</stock>");
        }

        pushApiMockRestServiceServer.expect(
                requestTo(String.format("%s/shops/%d/stocks?context=MARKET&apiSettings=PRODUCTION&logResponse=true", pushApiUrl, supplierId))
        ).andExpect(
                method(HttpMethod.POST)
        ).andExpect(
                content().xml(//language=xml
                        "" +
                                "<stocksRequest warehouse-id='" + deliveryServiceId + "'>" +
                                "   <skus>" +
                                xmlSkusBuilder.toString() +
                                "   </skus>" +
                                "</stocksRequest>"
                )
        ).andRespond(
                withSuccess(//language=xml
                        "<?xml version='1.0' encoding='UTF-8'?>\n" +
                                "   <stocksResponse>" +
                                "       <stocks>" +
                                stocksXmlBuilder.toString() +
                                "       </stocks>" +
                                "</stocksResponse>", MediaType.APPLICATION_XML
                )
        );
    }

    void mockPushApi(long supplierId, long warehouseId, List<String> offers, RuntimeException e) {
        StringBuilder xmlSkusBuilder = new StringBuilder();
        for (String offer : offers) {
            xmlSkusBuilder.append("<sku>").append(offer).append("</sku>");
        }

        pushApiMockRestServiceServer.expect(
                requestTo(String.format("%s/shops/%d/stocks?context=MARKET&apiSettings=PRODUCTION&logResponse=true", pushApiUrl, supplierId))
        ).andExpect(
                method(HttpMethod.POST)
        ).andExpect(
                content().xml(//language=xml
                        "" +
                                "<stocksRequest warehouse-id='" + warehouseId + "'>" +
                                "   <skus>" +
                                xmlSkusBuilder.toString() +
                                "   </skus>" +
                                "</stocksRequest>"
                )
        ).andRespond(
                response -> {
                    throw e;
                }
        );
    }

    protected void mockMappings(long supplierId, List<PartnerOffer> partnerOffers) {
        when(
                dataCampOfferService.findOffers(Mockito.any(), Mockito.any())
        ).thenReturn(
                new PartnerOfferSlice.Builder().partnerOffers(partnerOffers).build()
        );
    }

    protected void mockGetPartnerTypes() {
        when(lmsClient.searchPartners(refEq(SearchPartnerFilter.builder().setIds(Set.of(110L, 111L)).build())))
            .thenReturn(List.of(
                PartnerResponse.newBuilder().id(110L).partnerType(PartnerType.SUPPLIER).build(),
                PartnerResponse.newBuilder().id(111L).partnerType(PartnerType.FULFILLMENT).build()
            ));
    }

    void mockGetPartner(long partnerId, boolean stocksEnabled) {
        when(lmsClient.getPartner(partnerId))
            .thenReturn(
                Optional.of(PartnerResponse.newBuilder().id(partnerId).stockSyncEnabled(stocksEnabled).build())
            );
    }

    protected void mockSearchPartners(long partnerId, boolean stockSyncEnabled) {
        when(lmsClient.searchPartners(refEq(SearchPartnerFilter.builder().setIds(Set.of(partnerId)).build())))
            .thenReturn(List.of(
                PartnerResponse.newBuilder().id(partnerId).stockSyncEnabled(stockSyncEnabled).build()
            ));
    }

    void mockHttpStatusFromLms(long partnerId, HttpStatus httpStatus) {
        when(lmsClient.getPartner(partnerId))
            .thenThrow(new HttpClientErrorException(httpStatus));
    }

    protected ResponseActions expectMbi(long partnerId) {
        return mbiApiMockRestServiceServer.expect(
                requestTo(String.format(MBI_URL_STATE, mbiApiUrl, partnerId))
        ).andExpect(
                method(HttpMethod.GET)
        );
    }

    protected void assertErrorResponse(ResponseEntity<String> response, HttpStatus returnCode, ErrorSubCode errorSubCode) {
        Assertions.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assertions.assertEquals(response.getHeaders().get("x-return-code").get(0), returnCode.toString());

        JsonObject result = JsonTestUtil.parseJson(response.getBody()).getAsJsonObject();
        Assertions.assertTrue(result.get("result").isJsonNull());
        JsonObject error = result.getAsJsonArray("errors").get(0).getAsJsonObject();
        Assertions.assertEquals(error.getAsJsonPrimitive("subCode").getAsString(), errorSubCode.toString());
    }

    protected void assertResponseBody(String actualResponseBody, String expectedResponseBodyFilePath) {
        JSONAssert.assertEquals(
            Objects.requireNonNull(actualResponseBody),
            extractFileContent(expectedResponseBodyFilePath),
            new CustomComparator(
                JSONCompareMode.LENIENT,
                new Customization("host", (o1, o2) -> true),
                new Customization("timestamp", (o1, o2) -> true)
            ));
    }
}
