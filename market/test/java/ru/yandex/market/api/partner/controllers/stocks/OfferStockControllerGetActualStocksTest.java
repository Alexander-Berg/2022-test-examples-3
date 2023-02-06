package ru.yandex.market.api.partner.controllers.stocks;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import Market.DataCamp.SyncAPI.OffersBatch;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.controllers.order.ResourceUtilitiesMixin;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.api.partner.context.FunctionalTestHelper.makeRequest;

class OfferStockControllerGetActualStocksTest extends FunctionalTest implements ResourceUtilitiesMixin {
    private static final long BUSINESS_ID = 20774L;
    private static final long CAMPAIGN_ID = 10774L;
    private static final long PARTNER_ID = 774L;
    private static final long WAREHOUSE_ID = 30774L;

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    @Test
    @DbUnitDataSet(before = "OfferStockControllerGetActualStocksTest.getActualStocks.before.csv")
    void getActualStocksJson() {
        prepareDatacampClientMock();

        var response = makeRequest(
                actualStocksUri(CAMPAIGN_ID, WAREHOUSE_ID, List.of("59", "60", "nafania", "notexist")),
                HttpMethod.GET,
                Format.JSON
        );

        MbiAsserts.assertJsonEquals(
                String.format(
                        resourceAsString("OfferStockControllerGetActualStocksTest.getActualStocks.result.json"),
                        withOffset("2022-01-24T08:14:44Z"),
                        withOffset("1970-01-01T00:00:00Z"),
                        withOffset("2021-12-03T20:23:24Z")
                ),
                response.getBody()
        );
    }

    @Test
    @DbUnitDataSet(before = "OfferStockControllerGetActualStocksTest.getActualStocks.before.csv")
    void getActualStocksXml() {
        prepareDatacampClientMock();

        var response = makeRequest(
                actualStocksUri(CAMPAIGN_ID, WAREHOUSE_ID, List.of("59", "60", "nafania", "notexist")),
                HttpMethod.GET,
                Format.XML
        );

        MbiAsserts.assertXmlEquals(
                String.format(
                        resourceAsString("OfferStockControllerGetActualStocksTest.getActualStocks.result.xml"),
                        withOffset("2022-01-24T08:14:44Z"),
                        withOffset("1970-01-01T00:00:00Z"),
                        withOffset("2021-12-03T20:23:24Z")
                ),
                response.getBody()
        );
    }

    private String withOffset(String dt) {
        return OffsetDateTime.ofInstant(Instant.parse(dt), ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private URI actualStocksUri(long campaignId, long warehouseId, List<String> skus) {
        String uriString = "{base}/campaigns/{campaignId}/warehouses/{warehouseId}/stocks/actual";
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(uriString);
        for (String sku : skus) {
            builder.queryParam("sku", sku);
        }
        return builder.buildAndExpand(ImmutableMap.of(
                "base", urlBasePrefix,
                "campaignId", campaignId,
                "warehouseId", warehouseId
        )).toUri();
    }

    private void prepareDatacampClientMock() {
        var offersResponse = ProtoTestUtil.getProtoMessageByJson(
                OffersBatch.UnitedOffersBatchResponse.class,
                "OfferStockControllerGetActualStocksTest.UnitedOffersBatchResponse.json",
                getClass()
        );
        when(dataCampShopClient.getBusinessUnitedOffers(
                eq(BUSINESS_ID),
                eq(Set.of("59", "60", "nafania", "notexist")),
                eq(PARTNER_ID)))
                .thenReturn(offersResponse);
    }
}
