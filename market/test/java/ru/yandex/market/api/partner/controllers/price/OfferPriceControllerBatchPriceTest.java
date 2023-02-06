package ru.yandex.market.api.partner.controllers.price;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import Market.DataCamp.SyncAPI.SyncGetOffer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import ru.yandex.market.api.partner.controllers.price.model.OfferPriceDTO;
import ru.yandex.market.api.partner.controllers.price.model.OfferPriceListDTO;
import ru.yandex.market.api.partner.controllers.price.model.PriceDTO;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.mbi.datacamp.stroller.DataCampStrollerConversions;
import ru.yandex.market.mbi.jaxb.jackson.ApiObjectMapperFactory;
import ru.yandex.market.mbi.jaxb.jackson.XmlNamingStrategy;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.mbi.util.io.MbiFiles;

import static org.mockito.ArgumentMatchers.any;

@ParametersAreNonnullByDefault
class OfferPriceControllerBatchPriceTest extends AbstractOfferPriceControllerTest {

    public static Stream<Arguments> configs() {
        return Stream.of(
                Arguments.of(CAMPAIGN_ID, CampaignType.SHOP, 479633L));
    }

    @BeforeEach
    void setUp() {
        mockUltraControllerClient();
    }

    /**
     * Тест ручек для batch-update'ов цен.
     *
     * <ul>
     * <li>Вводим несколько цен одним запросом;
     * <li>Делаем ещё один запрос, который одновременно
     * <ul>
     * <li>добавляет новые,
     * <li>меняет существующие и
     * <li>удаляет существующие;
     * </ul>
     * <li>Запрашиваем список цен с дефолтной сортировкой и проверяем ожидаемый результат;
     * <li>Очищаем все цены;
     * <li>Запрашиваем список цен и проверяем, что список пустой;
     * </ul>
     */
    @ParameterizedTest
    @MethodSource("configs")
    void smokeTestXml(long campaignId, CampaignType campaignType, long feedId) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = ""
                    + "<offer-prices>"
                    + "    <offers>"
                    + "        <offer id=\"offer1\">"
                    + "            <feed id=\"" + feedId + "\" />"
                    + "            <price value=\"125.25\" currency-id=\"RUR\" />"
                    + "        </offer>"
                    + "        <offer id=\"offer2\">"
                    + "            <feed id=\"" + feedId + "\" />"
                    + "            <price value=\"74.25\" currency-id=\"RUR\" />"
                    + "        </offer>"
                    + "        <offer id=\"offer3\">"
                    + "            <feed id=\"" + feedId + "\" />"
                    + "            <price value=\"215.05\" currency-id=\"RUR\" />"
                    + "        </offer>"
                    + "    </offers>"
                    + "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(campaignId, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            }

            bodyString = ""
                    + "<offer-prices>"
                    + "    <offers>"
                    + "        <offer id=\"offer4\">"
                    + "            <feed id=\"" + feedId + "\" />"
                    + "            <price value=\"101.50\" currency-id=\"RUR\" />"
                    + "        </offer>"
                    + "        <offer id=\"offer2\">"
                    + "            <feed id=\"" + feedId + "\" />"
                    + "            <price value=\"94.25\" currency-id=\"RUR\" />"
                    + "        </offer>"
                    + "        <offer id=\"offer3\" delete=\"true\" >"
                    + "            <feed id=\"" + feedId + "\" />"
                    + "        </offer>"
                    + "    </offers>"
                    + "</offer-prices>";
            bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            request = patchOfferPricesRequest(campaignId, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            }

            request = getOfferPricesRequest(campaignId, "xml", Collections.emptyMap());
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                MbiAsserts.assertXmlEquals(
                        "" +
                                "<response>" +
                                "    <status>OK</status>" +
                                "    <result>" +
                                "        <total>3</total>" +
                                "        <paging/>" +
                                "        <offers>" +
                                "            <offer id=\"offer1\" updated-at=\"2017-12-04T01:15:01+03:00\">" +
                                "                <feed id=\"" + feedId + "\"/>" +
                                "                <price value=\"125.25\" currency-id=\"RUR\"/>" +
                                "            </offer>" +
                                "            <offer id=\"offer2\" updated-at=\"2017-12-04T01:15:03+03:00\">" +
                                "                <feed id=\"" + feedId + "\"/>" +
                                "                <price value=\"94.25\" currency-id=\"RUR\"/>" +
                                "            </offer>" +
                                "            <offer id=\"offer4\" updated-at=\"2017-12-04T01:15:03+03:00\">" +
                                "                <feed id=\"" + feedId + "\"/>" +
                                "                <price value=\"101.5\" currency-id=\"RUR\"/>" +
                                "            </offer>" +
                                "        </offers>" +
                                "    </result>" +
                                "</response>",
                        bodyString
                );
            }

            bodyEntity = new StringEntity("<offer-price-removal remove-all=\"true\" />", ContentType.TEXT_XML);
            request = removeAllOfferPricesRequest(campaignId, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            }

            request = getOfferPricesRequest(campaignId, "xml", Collections.emptyMap());
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                MbiAsserts.assertXmlEquals(
                        "" +
                                "<response>" +
                                "    <status>OK</status>" +
                                "    <result>" +
                                "        <total>3</total>" +
                                "        <paging/>" +
                                "        <offers>" +
                                "            <offer id=\"offer1\" updated-at=\"2017-12-04T01:15:01+03:00\">" +
                                "                <feed id=\"" + feedId + "\"/>" +
                                "                <price value=\"125.25\" currency-id=\"RUR\"/>" +
                                "            </offer>" +
                                "            <offer id=\"offer2\" updated-at=\"2017-12-04T01:15:03+03:00\">" +
                                "                <feed id=\"" + feedId + "\"/>" +
                                "                <price value=\"94.25\" currency-id=\"RUR\"/>" +
                                "            </offer>" +
                                "            <offer id=\"offer4\" updated-at=\"2017-12-04T01:15:03+03:00\">" +
                                "                <feed id=\"" + feedId + "\"/>" +
                                "                <price value=\"101.5\" currency-id=\"RUR\"/>" +
                                "            </offer>" +
                                "        </offers>" +
                                "    </result>" +
                                "</response>",
                        bodyString
                );
            }

            papiMarketSkuOfferService.removeExpiredPrices(1000, offerPriceControllerClock.instant());

            request = getOfferPricesRequest(campaignId, "xml", Collections.emptyMap());
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                MbiAsserts.assertXmlEquals(
                        "" +
                                "<response>" +
                                "    <status>OK</status>" +
                                "    <result>" +
                                "        <total>0</total>" +
                                "        <paging />" +
                                "        <offers />" +
                                "    </result>" +
                                "</response>",
                        bodyString
                );
            }
        }
    }

    @Test
    void sameOfferIdButDifferentFeedIdIsAllowed() throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer id=\"offer1\">" +
                    "            <feed id=\"479438\" />" +
                    "            <price value=\"125.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer1\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"74.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer3\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"215.05\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    void maximumRequestSizeIsStillAllowed() throws IOException {
        ObjectMapper xmlMapper = new ApiObjectMapperFactory().createXmlMapper(new XmlNamingStrategy());
        OfferPriceListDTO list = new OfferPriceListDTO();
        for (int i = 0; i < OfferPriceListDTO.MAX_REQUEST_SIZE; i++) {
            OfferPriceDTO offerPrice = new OfferPriceDTO();
            offerPrice.setFeedId(479633L);
            offerPrice.setId("offer" + i);
            PriceDTO price = new PriceDTO();
            price.setValue(BigDecimal.valueOf(100000099, 2));
            price.setDiscountBase(BigDecimal.valueOf(200000099, 2));
            price.setCurrencyId("RUR");
            offerPrice.setPrice(price);
            list.getOfferPrices().add(offerPrice);
        }
        StringWriter stream = new StringWriter();
        xmlMapper.writeValue(stream, list);
        String bodyString = stream.toString();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    void precisionIsPreserved() throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer id=\"offer1\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"112.9501327\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            }

            request = getOfferPricesRequest(CAMPAIGN_ID, "xml", Collections.emptyMap());
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                MbiAsserts.assertXmlEquals("" +
                                "<response>" +
                                "    <status>OK</status>" +
                                "    <result>" +
                                "        <total>1</total>" +
                                "        <paging/>" +
                                "        <offers>" +
                                "            <offer id=\"offer1\" updated-at=\"2017-12-04T01:15:01+03:00\">" +
                                "                <feed id=\"479633\" />" +
                                "                <price value=\"112.9501327\" currency-id=\"RUR\" />" +
                                "            </offer>" +
                                "        </offers>" +
                                "    </result>" +
                                "</response>",
                        bodyString
                );
            }

        }
    }

    @Test
    @DisplayName("Получение цен для белых пушей работает через единое офферное")
    void getPricesForWhitePush() throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            SyncGetOffer.GetUnitedOffersResponse datacampResponse =
                    getUnitedDatacampResponse("getPricesForWhitePush.json");
            Mockito.doReturn(DataCampStrollerConversions.fromStrollerResponse(datacampResponse))
                    .when(dataCampShopClient)
                    .searchBusinessOffers(any());
            mockSaasService(3, 998L);

            HttpUriRequest request = getOfferPricesRequest(PUSH_SHOP_CAMPAIGN_ID, "xml", Collections.emptyMap());
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                String bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                MbiAsserts.assertXmlEquals("" +
                                "<response>\n" +
                                "    <status>OK</status>\n" +
                                "    <result>\n" +
                                "        <total>3</total>\n" +
                                "        <paging next-page-token=\"eyJvcCI6Ij4iLCJrZXkiOiIxNjIxMDExIiwic2tpcCI6MH0\"/>\n" +
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
                                "    </result>\n" +
                                "</response>",
                        bodyString
                );
            }

        }
    }

    @Test
    @DisplayName("Получение цен для smb работает через единое офферное")
    void getPricesForSmb() throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            SyncGetOffer.GetUnitedOffersResponse datacampResponse = getUnitedDatacampResponse("getPricesForSmb.json");
            Mockito.doReturn(DataCampStrollerConversions.fromStrollerResponse(datacampResponse))
                    .when(dataCampShopClient)
                    .searchBusinessOffers(any());
            mockSaasService(5, 997L);

            HttpUriRequest request = getOfferPricesRequest(SMB_CAMPAIGN_ID, "xml", Collections.emptyMap());
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                String bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                MbiAsserts.assertXmlEquals("" +
                                "<response>\n" +
                                "    <status>OK</status>\n" +
                                "    <result>\n" +
                                "        <total>5</total>\n" +
                                "        <paging next-page-token=\"eyJvcCI6Ij4iLCJrZXkiOiIxMzEyMzg1Iiwic2tpcCI6MH0\"/>\n" +
                                "        <offers>\n" +
                                "            <offer id=\"126952\" updated-at=\"2020-07-28T07:31:32+03:00\">\n" +
                                "                <feed id=\"473957\"/>\n" +
                                "                <price value=\"2770\" currency-id=\"RUR\"/>\n" +
                                "            </offer>\n" +
                                "            <offer id=\"1513666\" updated-at=\"2020-07-28T07:31:32+03:00\">\n" +
                                "                <feed id=\"473957\"/>\n" +
                                "                <price value=\"1500\" currency-id=\"RUR\"/>\n" +
                                "            </offer>\n" +
                                "            <offer id=\"1621011\" updated-at=\"2020-07-28T07:31:32+03:00\">\n" +
                                "                <feed id=\"473957\"/>\n" +
                                "                <price value=\"13990\" currency-id=\"RUR\"/>\n" +
                                "            </offer>\n" +
                                "            <offer id=\"1312384\" updated-at=\"2020-07-28T07:31:32+03:00\">\n" +
                                "                <feed id=\"473957\"/>\n" +
                                "                <price value=\"1501\" currency-id=\"RUR\"/>\n" +
                                "            </offer>\n" +
                                "            <offer id=\"1312385\" updated-at=\"2020-07-28T07:31:32+03:00\">\n" +
                                "                <feed id=\"473957\"/>\n" +
                                "                <price value=\"1499\" currency-id=\"RUR\"/>\n" +
                                "            </offer>\n" +
                                "        </offers>\n" +
                                "    </result>\n" +
                                "</response>",
                        bodyString
                );
            }

        }
    }

    @Test
    void hugeMagnitudeIsPreserved() throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer id=\"offer1\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"922337203685.4775807\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer2\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"922337203685.47\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            }

            request = getOfferPricesRequest(CAMPAIGN_ID, "xml", Collections.emptyMap());
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                MbiAsserts.assertXmlEquals("" +
                                "<response>" +
                                "    <status>OK</status>" +
                                "    <result>" +
                                "        <total>2</total>" +
                                "        <paging/>" +
                                "        <offers>" +
                                "            <offer id=\"offer1\" updated-at=\"2017-12-04T01:15:01+03:00\">" +
                                "                <feed id=\"479633\" />" +
                                "                <price value=\"922337203685.4775807\" currency-id=\"RUR\" />" +
                                "            </offer>" +
                                "            <offer id=\"offer2\" updated-at=\"2017-12-04T01:15:01+03:00\">" +
                                "                <feed id=\"479633\" />" +
                                "                <price value=\"922337203685.47\" currency-id=\"RUR\" />" +
                                "            </offer>" +
                                "        </offers>" +
                                "    </result>" +
                                "</response>",
                        bodyString
                );
            }

        }
    }

    @Test
    void borderlineDiscountTestXml() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer id=\"offer1\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"25.00\" discount-base=\"100.00\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer2\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"50.00\" discount-base=\"100.00\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer3\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"95.00\" discount-base=\"100.00\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer4\">" +
                    "            <feed id=\"479633\" />" +
                    "            <price value=\"49500.00\" discount-base=\"50000.00\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    void borderlineDirectDiscountTestXml() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer id=\"offer1\">" +
                    "            <feed id=\"473970\" />" +
                    "            <price value=\"5.00\" discount-base=\"100.00\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer2\">" +
                    "            <feed id=\"473970\" />" +
                    "            <price value=\"50.00\" discount-base=\"100.00\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer3\">" +
                    "            <feed id=\"473970\" />" +
                    "            <price value=\"95.00\" discount-base=\"100.00\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer id=\"offer4\">" +
                    "            <feed id=\"473970\" />" +
                    "            <price value=\"45000.00\" discount-base=\"50000.00\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(DIRECT_CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Disabled("Тест проверяет синих пулл-партнёров, но это неактуально")
    @Test
    void supplierBorderlineAndIllegalDiscountTestXml() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer market-sku=\"1241254\">" +
                    "            <price value=\"5.00\" discount-base=\"100.00\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer market-sku=\"1241255\">" +
                    "            <price value=\"50.00\" discount-base=\"100.00\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer market-sku=\"1241256\">" +
                    "            <price value=\"95.00\" discount-base=\"100.00\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer market-sku=\"1241257\">" +
                    "            <price value=\"49500.00\" discount-base=\"50000.00\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer market-sku=\"23562323\">" +
                    "            <price value=\"1.00\" discount-base=\"100.00\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer market-sku=\"23562324\">" +
                    "            <price value=\"4.00\" discount-base=\"100.00\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer market-sku=\"23562325\">" +
                    "            <price value=\"96.00\" discount-base=\"100.00\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer market-sku=\"23562326\">" +
                    "            <price value=\"99.00\" discount-base=\"100.00\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer market-sku=\"23562327\">" +
                    "            <price value=\"99010.00\" discount-base=\"100000.00\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(SUPPLIER_CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            }

            request = getOfferPricesRequest(SUPPLIER_CAMPAIGN_ID, "xml", Map.of("limit", "9"));
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                MbiAsserts.assertXmlEquals("" +
                                "<response>" +
                                "    <status>OK</status>" +
                                "    <result>" +
                                "        <total>9</total>" +
                                "        <paging next-page-token=\"OQ\"/>" +
                                "        <offers>" +
                                "            <offer market-sku=\"1241254\" updated-at=\"2017-12-04T01:15:01+03:00\">" +
                                "                <price value=\"5\" discount-base=\"100\" currency-id=\"RUR\" />" +
                                "            </offer>" +
                                "            <offer market-sku=\"1241255\" updated-at=\"2017-12-04T01:15:01+03:00\">" +
                                "                <price value=\"50\" discount-base=\"100\" currency-id=\"RUR\" />" +
                                "            </offer>" +
                                "            <offer market-sku=\"1241256\" updated-at=\"2017-12-04T01:15:01+03:00\">" +
                                "                <price value=\"95\" discount-base=\"100\" currency-id=\"RUR\" />" +
                                "            </offer>" +
                                "            <offer market-sku=\"1241257\" updated-at=\"2017-12-04T01:15:01+03:00\">" +
                                "                <price value=\"49500\" discount-base=\"50000\" currency-id=\"RUR\" />" +
                                "            </offer>" +
                                "            <offer market-sku=\"23562323\" updated-at=\"2017-12-04T01:15:01+03:00\">" +
                                "                <price value=\"1\" currency-id=\"RUR\" />" +
                                "            </offer>" +
                                "            <offer market-sku=\"23562324\" updated-at=\"2017-12-04T01:15:01+03:00\">" +
                                "                <price value=\"4\" currency-id=\"RUR\" />" +
                                "            </offer>" +
                                "            <offer market-sku=\"23562325\" updated-at=\"2017-12-04T01:15:01+03:00\">" +
                                "                <price value=\"96\" currency-id=\"RUR\" />" +
                                "            </offer>" +
                                "            <offer market-sku=\"23562326\" updated-at=\"2017-12-04T01:15:01+03:00\">" +
                                "                <price value=\"99\" currency-id=\"RUR\" />" +
                                "            </offer>" +
                                "            <offer market-sku=\"23562327\" updated-at=\"2017-12-04T01:15:01+03:00\">" +
                                "                <price value=\"99010\" currency-id=\"RUR\" />" +
                                "            </offer>" +
                                "        </offers>" +
                                "    </result>" +
                                "</response>",
                        bodyString
                );
            }

            request = getOfferPricesRequest(SUPPLIER_CAMPAIGN_ID, "xml", Map.of("token", "OQ"));
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                System.out.println(bodyString);
                MbiAsserts.assertXmlEquals("" +
                                "<response>" +
                                "    <status>OK</status>" +
                                "    <result>" +
                                "        <total>9</total>" +
                                "        <paging/>" +
                                "        <offers>" +
                                "            <offer market-sku=\"1241254\" updated-at=\"2017-12-04T01:15:01+03:00\">" +
                                "                <price value=\"5\" discount-base=\"100\" currency-id=\"RUR\" />" +
                                "            </offer>" +
                                "            <offer market-sku=\"1241255\" updated-at=\"2017-12-04T01:15:01+03:00\">" +
                                "                <price value=\"50\" discount-base=\"100\" currency-id=\"RUR\" />" +
                                "            </offer>" +
                                "            <offer market-sku=\"1241256\" updated-at=\"2017-12-04T01:15:01+03:00\">" +
                                "                <price value=\"95\" discount-base=\"100\" currency-id=\"RUR\" />" +
                                "            </offer>" +
                                "            <offer market-sku=\"1241257\" updated-at=\"2017-12-04T01:15:01+03:00\">" +
                                "                <price value=\"49500\" discount-base=\"50000\" currency-id=\"RUR\" />" +
                                "            </offer>" +
                                "            <offer market-sku=\"23562323\" updated-at=\"2017-12-04T01:15:01+03:00\">" +
                                "                <price value=\"1\" currency-id=\"RUR\" />" +
                                "            </offer>" +
                                "            <offer market-sku=\"23562324\" updated-at=\"2017-12-04T01:15:01+03:00\">" +
                                "                <price value=\"4\" currency-id=\"RUR\" />" +
                                "            </offer>" +
                                "            <offer market-sku=\"23562325\" updated-at=\"2017-12-04T01:15:01+03:00\">" +
                                "                <price value=\"96\" currency-id=\"RUR\" />" +
                                "            </offer>" +
                                "            <offer market-sku=\"23562326\" updated-at=\"2017-12-04T01:15:01+03:00\">" +
                                "                <price value=\"99\" currency-id=\"RUR\" />" +
                                "            </offer>" +
                                "            <offer market-sku=\"23562327\" updated-at=\"2017-12-04T01:15:01+03:00\">" +
                                "                <price value=\"99010\" currency-id=\"RUR\" />" +
                                "            </offer>" +
                                "        </offers>" +
                                "    </result>" +
                                "</response>",
                        bodyString
                );
            }
        }
    }
}
