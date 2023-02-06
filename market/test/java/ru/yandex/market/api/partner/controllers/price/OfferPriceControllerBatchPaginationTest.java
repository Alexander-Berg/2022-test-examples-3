package ru.yandex.market.api.partner.controllers.price;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import Market.DataCamp.SyncAPI.SyncGetOffer;
import com.google.common.collect.ImmutableMap;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampStrollerConversions;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.mbi.util.io.MbiFiles;

import static org.mockito.ArgumentMatchers.any;

@ParametersAreNonnullByDefault
public class OfferPriceControllerBatchPaginationTest extends AbstractOfferPriceControllerTest {

    @BeforeEach
    public void setUp() {
        mockUltraControllerClient();
    }

    /**
     * Тест пагинации.
     * <p>
     * Заполняем базу несколькими ценами и дёргаем ручку с различными сочетаниями offset/limit.
     */
    @Test
    public void smokeTestPaginationXml() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer id=\"offer1\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"125.250\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer2\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"74.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer3\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"215.05\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer4\">" +
                    "            <feed id=\"479633\"/>" +
                    "            <price value=\"101.50\" currency-id=\"RUR\"/>" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            }

            Map<String, String> modern = Collections.singletonMap("limit", "2");
            Map<String, String> legacy = Collections.singletonMap("pageSize", "2");
            for (Map<String, String> params : Arrays.asList(modern, legacy)) {
                request = getOfferPricesRequest(CAMPAIGN_ID, "xml", params);
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                    bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                    MbiAsserts.assertXmlEquals(
                            "" +
                                    "<response>" +
                                    "    <status>OK</status>" +
                                    "    <result>" +
                                    "        <total>4</total>" +
                                    "        <paging next-page-token=\"Mg\"/>" +
                                    "        <offers>" +
                                    "            <offer id=\"offer1\" updated-at=\"2017-12-04T01:15:01+03:00\">" +
                                    "                <feed id=\"479633\"/>" +
                                    "                <price value=\"125.25\" currency-id=\"RUR\"/>" +
                                    "            </offer>" +
                                    "            <offer id=\"offer2\" updated-at=\"2017-12-04T01:15:01+03:00\">" +
                                    "                <feed id=\"479633\"/>" +
                                    "                <price value=\"74.25\" currency-id=\"RUR\"/>" +
                                    "            </offer>" +
                                    "        </offers>" +
                                    "    </result>" +
                                    "</response>",
                            bodyString
                    );
                }
            }

            request = getOfferPricesRequest(CAMPAIGN_ID, "xml", ImmutableMap.of("limit", "2", "offset", "1"));
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                MbiAsserts.assertXmlEquals(
                        "" +
                                "<response>" +
                                "    <status>OK</status>" +
                                "    <result>" +
                                "        <total>4</total>" +
                                "        <paging prev-page-token=\"MA\" next-page-token=\"Mw\"/>" +
                                "        <offers>" +
                                "            <offer id=\"offer2\" updated-at=\"2017-12-04T01:15:01+03:00\">" +
                                "                <feed id=\"479633\"/>" +
                                "                <price value=\"74.25\" currency-id=\"RUR\"/>" +
                                "            </offer>" +
                                "            <offer id=\"offer3\" updated-at=\"2017-12-04T01:15:01+03:00\">" +
                                "                <feed id=\"479633\" />" +
                                "                <price value=\"215.05\" currency-id=\"RUR\" />" +
                                "            </offer>" +
                                "        </offers>" +
                                "    </result>" +
                                "</response>",
                        bodyString
                );
            }

            modern = ImmutableMap.of("limit", "2", "offset", "2");
            legacy = ImmutableMap.of("pageSize", "2", "page", "2");
            for (Map<String, String> params : Arrays.asList(modern, legacy)) {
                request = getOfferPricesRequest(CAMPAIGN_ID, "xml", params);
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                    bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                    MbiAsserts.assertXmlEquals(
                            "" +
                                    "<response>" +
                                    "    <status>OK</status>" +
                                    "    <result>" +
                                    "        <total>4</total>" +
                                    "        <paging prev-page-token=\"MA\" next-page-token=\"NA\"/>" +
                                    "        <offers>" +
                                    "            <offer id=\"offer3\" updated-at=\"2017-12-04T01:15:01+03:00\">" +
                                    "                <feed id=\"479633\" />" +
                                    "                <price value=\"215.05\" currency-id=\"RUR\" />" +
                                    "            </offer>" +
                                    "            <offer id=\"offer4\" updated-at=\"2017-12-04T01:15:01+03:00\">" +
                                    "                <feed id=\"479633\" />" +
                                    "                <price value=\"101.5\" currency-id=\"RUR\" />" +
                                    "            </offer>" +
                                    "        </offers>" +
                                    "    </result>" +
                                    "</response>",
                            bodyString
                    );
                }
            }

            request = getOfferPricesRequest(CAMPAIGN_ID, "xml", ImmutableMap.of("limit", "2", "offset", "3"));
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                MbiAsserts.assertXmlEquals(
                        "" +
                                "<response>" +
                                "    <status>OK</status>" +
                                "    <result>" +
                                "        <total>4</total>" +
                                "        <paging prev-page-token=\"MQ\"/>" +
                                "        <offers>" +
                                "            <offer id=\"offer4\" updated-at=\"2017-12-04T01:15:01+03:00\">" +
                                "                <feed id=\"479633\" />" +
                                "                <price value=\"101.5\" currency-id=\"RUR\" />" +
                                "            </offer>" +
                                "        </offers>" +
                                "    </result>" +
                                "</response>",
                        bodyString
                );
            }
        }

    }


    /**
     * Тест пагинации с использованием page_token и честным запросом в ОХ с указанием position.
     * <p>
     * Заполняем базу несколькими ценами и дёргаем ручку с различными сочетаниями page_token/limit.
     */
    @Test
    public void smokeTestTokenPaginationXml() throws Exception {
        SyncGetOffer.GetUnitedOffersResponse datacampResponse =
                getUnitedDatacampResponse("getPricesForWhitePush.json");
        Mockito.doReturn(DataCampStrollerConversions.fromStrollerResponse(datacampResponse))
                .when(dataCampShopClient)
                .searchBusinessOffers(any());
        mockSaasService(3, 998L);
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer id=\"offer1\">" +
                    "            <feed id=\"473955\" />" +
                    "            <price value=\"125.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer2\">" +
                    "            <feed id=\"473955\" />" +
                    "            <price value=\"74.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer3\">" +
                    "            <feed id=\"473955\" />" +
                    "            <price value=\"215.05\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer4\">" +
                    "            <feed id=\"473955\"/>" +
                    "            <price value=\"101.50\" currency-id=\"RUR\"/>" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(PUSH_SHOP_CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            }

            Map<String, String> params = Collections.singletonMap("limit", "2");
            request = getOfferPricesRequest(PUSH_SHOP_CAMPAIGN_ID, "xml", params);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                MbiAsserts.assertXmlEquals(
                        "" +
                                "<response>" +
                                "    <status>OK</status>" +
                                "    <result>" +
                                "        <total>3</total>" +
                                "        <paging next-page-token=\"eyJvcCI6Ij4iLCJrZXkiOiIxNjIxMDExIiwic2tpcCI6MH0\"/>" +
                                "        <offers>\n" +
                                "            <offer id=\"126952\" updated-at=\"2020-07-28T07:31:32+03:00\">\n" +
                                "                <feed id=\"473955\"/>\n" +
                                "                <price value=\"2770\" currency-id=\"RUR\"/>\n" +
                                "            </offer>\n" +
                                "            <offer id=\"1513666\" updated-at=\"2020-07-28T07:31:32+03:00\">\n" +
                                "                <feed id=\"473955\"/>\n" +
                                "                <price value=\"1500\" currency-id=\"RUR\"/>\n" +
                                "            </offer>\n" +
                                "            <offer id=\"1621011\" updated-at=\"2020-07-28T07:31:32+03:00\">\n" +
                                "                <feed id=\"473955\"/>\n" +
                                "                <price value=\"13990\" currency-id=\"RUR\"/>\n" +
                                "            </offer>\n" +
                                "        </offers>\n" +
                                "    </result>" +
                                "</response>",
                        bodyString
                );
            }

            request = getOfferPricesRequest(PUSH_SHOP_CAMPAIGN_ID, "xml", ImmutableMap.of("limit", "2", "page_token", "eyJvcCI6Ij4iLCJrZXkiOiIxNjIxMDExIiwic2tpcCI6MH0"));
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                MbiAsserts.assertXmlEquals(
                        "" +
                                "<response>" +
                                "    <status>OK</status>" +
                                "    <result>" +
                                "        <total>3</total>" +
                                "        <paging next-page-token=\"eyJvcCI6Ij4iLCJrZXkiOiIxNjIxMDExIiwic2tpcCI6MH0\"/>" +
                                "        <offers>\n" +
                                "            <offer id=\"126952\" updated-at=\"2020-07-28T07:31:32+03:00\">\n" +
                                "                <feed id=\"473955\"/>\n" +
                                "                <price value=\"2770\" currency-id=\"RUR\"/>\n" +
                                "            </offer>\n" +
                                "            <offer id=\"1513666\" updated-at=\"2020-07-28T07:31:32+03:00\">\n" +
                                "                <feed id=\"473955\"/>\n" +
                                "                <price value=\"1500\" currency-id=\"RUR\"/>\n" +
                                "            </offer>\n" +
                                "            <offer id=\"1621011\" updated-at=\"2020-07-28T07:31:32+03:00\">\n" +
                                "                <feed id=\"473955\"/>\n" +
                                "                <price value=\"13990\" currency-id=\"RUR\"/>\n" +
                                "            </offer>\n" +
                                "        </offers>\n" +
                                "    </result>" +
                                "</response>",
                        bodyString
                );
                //TODO: проверить что запросили в хранилище с position
            }
            //тут пустой ответ от строллера
            Mockito.doReturn(SearchBusinessOffersResult.builder().setTotalCount(3).build())
                    .when(dataCampShopClient)
                    .searchBusinessOffers(any());
            mockSaasService(3, 998L);
            request = getOfferPricesRequest(PUSH_SHOP_CAMPAIGN_ID, "xml",
                    ImmutableMap.of("limit", "2", "page_token", "eyJvcCI6Ij4iLCJrZXkiOiIxNjIxMDExIiwic2tpcCI6MH0"));
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                MbiAsserts.assertXmlEquals(
                        "" +
                                "<response>" +
                                "    <status>OK</status>" +
                                "    <result>" +
                                "        <total>3</total>" +
                                "        <paging/>" +
                                "        <offers/>" +
                                "    </result>" +
                                "</response>",
                        bodyString
                );
            }
        }

    }
}
