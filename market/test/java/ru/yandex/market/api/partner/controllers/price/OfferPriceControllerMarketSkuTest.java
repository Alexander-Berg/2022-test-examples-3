package ru.yandex.market.api.partner.controllers.price;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.SyncAPI.SyncChangeOffer;
import Market.DataCamp.SyncAPI.SyncGetOffer;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.api.partner.helper.PartnerApiFunctionalTestHelper;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.logbroker.event.datacamp.DataCampEvent;
import ru.yandex.market.core.logbroker.event.datacamp.SyncChangeOfferLogbrokerEvent;
import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.ir.http.UltraControllerServiceStub;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.logbroker.event.LogbrokerEvent;
import ru.yandex.market.mbi.datacamp.stroller.DataCampStrollerConversions;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.mbi.util.io.MbiFiles;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;

import static org.mockito.ArgumentMatchers.any;
import static ru.yandex.market.mbi.util.MbiAsserts.assertXmlEquals;

@ParametersAreNonnullByDefault
@DbUnitDataSet(before = "OfferPriceControllerMarketSkuTest.before.csv")
class OfferPriceControllerMarketSkuTest extends AbstractOfferPriceControllerTest {
    private static final String SEND_TO_LOGBROKER = "market.quick.partner-api.send.to.logbroker";

    @Autowired
    @Qualifier("marketQuickLogbrokerService")
    private LogbrokerEventPublisher<SyncChangeOfferLogbrokerEvent> logbrokerService;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private UltraControllerServiceStub ultraControllerClient;

    @Autowired
    private MboMappingsService patientMboMappingsService;

    /**
     * Тест пагинации.
     * <p>
     * Заполняем базу несколькими ценами и дёргаем ручку с различными сочетаниями offset/limit.
     */
    @Test
    @Disabled("Тест проверяет синих пулл-партнёров, но это неактуально")
    @DbUnitDataSet(before = "OfferPriceControllerMarketSkuTest.defaultVat.csv")
    void testMarketSkuUpdates() throws Exception {
        mockUltraControllerClient();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = /*language=XML*/ ""
                    + "<offer-prices>"
                    + "    <offers>"
                    + "        <offer market-sku=\"1\">"
                    + "            <price value=\"125.25\" currency-id=\"RUR\" />"
                    + "        </offer>"
                    + "        <offer market-sku=\"2\">"
                    + "            <price value=\"74.25\" currency-id=\"RUR\" />"
                    + "        </offer>"
                    + "        <offer market-sku=\"3\">"
                    + "            <price value=\"215.05\" currency-id=\"RUR\" vat=\"2\" />"
                    + "        </offer>"
                    + "        <offer market-sku=\"4\">"
                    + "            <price value=\"101.50\" currency-id=\"RUR\"/>"
                    + "        </offer>"
                    + "    </offers>"
                    + "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(SUPPLIER_CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            }

            Map<String, String> modern = Collections.singletonMap("limit", "2");
            Map<String, String> legacy = Collections.singletonMap("pageSize", "2");
            for (Map<String, String> params : Arrays.asList(modern, legacy)) {
                request = getOfferPricesRequest(SUPPLIER_CAMPAIGN_ID, "xml", params);
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                    bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                    MbiAsserts.assertXmlEquals(/*language=XML*/ ""
                            + "<response>"
                            + "    <status>OK</status>"
                            + "    <result>"
                            + "        <total>4</total>"
                            + "        <paging next-page-token=\"Mg\"/>"
                            + "        <offers>"
                            + "            <offer market-sku=\"1\" updated-at=\"2017-12-04T01:15:01+03:00\">"
                            + "                <price value=\"125\" currency-id=\"RUR\" vat=\"6\"/>"
                            + "            </offer>"
                            + "            <offer market-sku=\"2\" updated-at=\"2017-12-04T01:15:01+03:00\">"
                            + "                <price value=\"74\" currency-id=\"RUR\" vat=\"6\"/>"
                            + "            </offer>"
                            + "        </offers>"
                            + "    </result>"
                            + "</response>", bodyString);
                }
            }

            request = getOfferPricesRequest(SUPPLIER_CAMPAIGN_ID, "xml", ImmutableMap.of("limit", "2", "offset", "1"));
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                MbiAsserts.assertXmlEquals(/*language=XML*/ ""
                        + "<response>"
                        + "    <status>OK</status>"
                        + "    <result>"
                        + "        <total>4</total>"
                        + "        <paging prev-page-token=\"MA\" next-page-token=\"Mw\"/>"
                        + "        <offers>"
                        + "            <offer market-sku=\"2\" updated-at=\"2017-12-04T01:15:01+03:00\">"
                        + "                <price value=\"74\" currency-id=\"RUR\" vat=\"6\"/>"
                        + "            </offer>"
                        + "            <offer market-sku=\"3\" updated-at=\"2017-12-04T01:15:01+03:00\">"
                        + "                <price value=\"215\" currency-id=\"RUR\" vat=\"2\"/>"
                        + "            </offer>"
                        + "        </offers>"
                        + "    </result>"
                        + "</response>", bodyString);
            }

            modern = ImmutableMap.of("limit", "2", "offset", "2");
            legacy = ImmutableMap.of("pageSize", "2", "page", "2");
            for (Map<String, String> params : Arrays.asList(modern, legacy)) {
                request = getOfferPricesRequest(SUPPLIER_CAMPAIGN_ID, "xml", params);
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                    bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                    MbiAsserts.assertXmlEquals(/*language=XML*/ ""
                            + "<response>"
                            + "    <status>OK</status>"
                            + "    <result>"
                            + "        <total>4</total>"
                            + "        <paging prev-page-token=\"MA\" next-page-token=\"NA\"/>"
                            + "        <offers>"
                            + "            <offer market-sku=\"3\" updated-at=\"2017-12-04T01:15:01+03:00\">"
                            + "                <price value=\"215\" currency-id=\"RUR\" vat=\"2\" />"
                            + "            </offer>"
                            + "            <offer market-sku=\"4\" updated-at=\"2017-12-04T01:15:01+03:00\">"
                            + "                <price value=\"102\" currency-id=\"RUR\" vat=\"6\"/>"
                            + "            </offer>"
                            + "        </offers>"
                            + "    </result>"
                            + "</response>", bodyString);
                }
            }

            request = getOfferPricesRequest(SUPPLIER_CAMPAIGN_ID, "xml", ImmutableMap.of("limit", "2", "offset", "3"));
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                MbiAsserts.assertXmlEquals(/*language=XML*/ ""
                        + "<response>"
                        + "    <status>OK</status>"
                        + "    <result>"
                        + "        <total>4</total>"
                        + "        <paging prev-page-token=\"MQ\" />"
                        + "        <offers>"
                        + "            <offer market-sku=\"4\" updated-at=\"2017-12-04T01:15:01+03:00\">"
                        + "                <price value=\"102\" currency-id=\"RUR\" vat=\"6\" />"
                        + "            </offer>"
                        + "        </offers>"
                        + "    </result>"
                        + "</response>", bodyString);
            }
        }

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
    @Test
    @Disabled("Тест проверяет синих пулл-партнёров, но это неактуально")
    @DbUnitDataSet(before = "OfferPriceControllerMarketSkuTest.defaultVat.csv")
    void smokeTestXml() throws Exception {
        mockUltraControllerClient();
        environmentService.setValue(SEND_TO_LOGBROKER, "true");

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = /*language=XML*/ ""
                    + "<offer-prices>"
                    + "    <offers>"
                    + "        <offer market-sku=\"1\">"
                    + "            <price value=\"125.25\" currency-id=\"RUR\" />"
                    + "        </offer>"
                    + "        <offer market-sku=\"2\">"
                    + "            <price value=\"74.25\" currency-id=\"RUR\" />"
                    + "        </offer>"
                    + "        <offer market-sku=\"3\">"
                    + "            <price value=\"215.05\" currency-id=\"RUR\" />"
                    + "        </offer>"
                    + "    </offers>"
                    + "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(SUPPLIER_CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            }

            bodyString = /*language=XML*/ ""
                    + "<offer-prices>"
                    + "    <offers>"
                    + "        <offer market-sku=\"4\">"
                    + "            <price value=\"101.50\" currency-id=\"RUR\" />"
                    + "        </offer>"
                    + "        <offer market-sku=\"2\">"
                    + "            <price value=\"94.25\" currency-id=\"RUR\" />"
                    + "        </offer>"
                    + "        <offer market-sku=\"3\" delete=\"true\" />"
                    + "    </offers>"
                    + "</offer-prices>";
            bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            request = patchOfferPricesRequest(SUPPLIER_CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            }

            //Проверяем, что 3 эвента (обновление, обновление + удаление)  были отправлены в Логброкер
            Mockito.verify(logbrokerService, Mockito.times(3))
                    .publishEvent(any(SyncChangeOfferLogbrokerEvent.class));

            request = getOfferPricesRequest(SUPPLIER_CAMPAIGN_ID, "xml", Collections.emptyMap());
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                MbiAsserts.assertXmlEquals(/*language=XML*/ ""
                        + "<response>"
                        + "    <status>OK</status>"
                        + "    <result>"
                        + "        <total>3</total>"
                        + "        <paging/>"
                        + "        <offers>"
                        + "            <offer market-sku=\"1\" updated-at=\"2017-12-04T01:15:01+03:00\">"
                        + "                <price value=\"125\" currency-id=\"RUR\" vat=\"6\"/>"
                        + "            </offer>"
                        + "            <offer market-sku=\"2\" updated-at=\"2017-12-04T01:15:03+03:00\">"
                        + "                <price value=\"94\" currency-id=\"RUR\" vat=\"6\"/>"
                        + "            </offer>"
                        + "            <offer market-sku=\"4\" updated-at=\"2017-12-04T01:15:03+03:00\">"
                        + "                <price value=\"102\" currency-id=\"RUR\" vat=\"6\"/>"
                        + "            </offer>"
                        + "        </offers>"
                        + "    </result>"
                        + "</response>", bodyString);
            }

            bodyEntity = new StringEntity("<offer-price-removal remove-all=\"true\" />", ContentType.TEXT_XML);
            request = removeAllOfferPricesRequest(SUPPLIER_CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            }

            request = getOfferPricesRequest(SUPPLIER_CAMPAIGN_ID, "xml", Collections.emptyMap());
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                MbiAsserts.assertXmlEquals(/*language=XML*/ ""
                        + "<response>"
                        + "    <status>OK</status>"
                        + "    <result>"
                        + "        <total>3</total>"
                        + "        <paging/>"
                        + "        <offers>"
                        + "            <offer market-sku=\"1\" updated-at=\"2017-12-04T01:15:01+03:00\">"
                        + "                <price value=\"125\" currency-id=\"RUR\" vat=\"6\"/>"
                        + "            </offer>"
                        + "            <offer market-sku=\"2\" updated-at=\"2017-12-04T01:15:03+03:00\">"
                        + "                <price value=\"94\" currency-id=\"RUR\" vat=\"6\"/>"
                        + "            </offer>"
                        + "            <offer market-sku=\"4\" updated-at=\"2017-12-04T01:15:03+03:00\">"
                        + "                <price value=\"102\" currency-id=\"RUR\" vat=\"6\"/>"
                        + "            </offer>"
                        + "        </offers>"
                        + "    </result>"
                        + "</response>", bodyString);
            }

            papiMarketSkuOfferService.removeExpiredPrices(1000, offerPriceControllerClock.instant());

            request = getOfferPricesRequest(SUPPLIER_CAMPAIGN_ID, "xml", Collections.emptyMap());
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                MbiAsserts.assertXmlEquals(/*language=XML*/ ""
                        + "<response>"
                        + "    <status>OK</status>"
                        + "    <result>"
                        + "        <total>0</total>"
                        + "        <paging/>"
                        + "        <offers />"
                        + "    </result>"
                        + "</response>", bodyString);
            }
        }
    }

    /**
     * Тест ручек для batch-update'ов цен.
     *
     * <ul>
     * <li>Вводим несколько цен одним запросом;
     * <li>Делаем ещё один запрос, который одновременно
     * <li>добавляет новые,
     * <li>меняет существующие и
     * <li>удаляет существующие;
     * </ul>
     * <li>Запрашиваем список цен с дефолтной сортировкой и проверяем ожидаемый результат;
     * <li>Очищаем все цены - для пуш партнера ничего не поменятся;
     * </ul>
     */
    @Test
    @DbUnitDataSet(before = "OfferPriceControllerMarketSkuTest.pushpartner.csv",
            after = "OfferPriceControllerMarketSkuTest.pushpartner.after.csv"
    )
    void smokeTestXmlPushPartner() throws Exception {
        environmentService.setValue(SEND_TO_LOGBROKER, "true");
        List<UltraController.SKUMappingResponse.SKUMapping> skuMappings =
                List.of(UltraController.SKUMappingResponse.SKUMapping.newBuilder()
                                .addShopSku("111")
                                .setMarketSkuId(1)
                                .build(),
                        UltraController.SKUMappingResponse.SKUMapping.newBuilder()
                                .addShopSku("222")
                                .setMarketSkuId(2)
                                .build(),
                        UltraController.SKUMappingResponse.SKUMapping.newBuilder()
                                .addShopSku("333")
                                .setMarketSkuId(3)
                                .build());
        Mockito.when(ultraControllerClient.getShopSKU(any())).thenReturn(
                UltraController.SKUMappingResponse.newBuilder()
                        .addAllSkuMapping(skuMappings)
                        .build());
        Mockito.when(ultraControllerClient.getMarketSKU(any())).thenReturn(
                UltraController.SKUMappingResponse.newBuilder()
                        .addAllSkuMapping(skuMappings)
                        .build());

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = /*language=XML*/ ""
                    + "<offer-prices>"
                    + "    <offers>"
                    + "        <offer market-sku=\"1\">"
                    + "            <price value=\"125.25\" currency-id=\"RUR\" />"
                    + "        </offer>"
                    + "        <offer market-sku=\"2\">"
                    + "            <price value=\"74.25\" currency-id=\"RUR\" />"
                    + "        </offer>"
                    + "        <offer market-sku=\"3\">"
                    + "            <price value=\"215.05\" currency-id=\"RUR\" />"
                    + "        </offer>"
                    + "    </offers>"
                    + "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(SUPPLIER_CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            }

            bodyString = /*language=XML*/ ""
                    + "<offer-prices>"
                    + "    <offers>"
                    + "        <offer market-sku=\"1\">"
                    + "            <price value=\"125.25\" currency-id=\"RUR\" />"
                    + "        </offer>"
                    + "        <offer market-sku=\"4\">"
                    + "            <price value=\"101.50\" currency-id=\"RUR\" />"
                    + "        </offer>"
                    + "        <offer market-sku=\"2\">"
                    + "            <price value=\"94.25\" currency-id=\"RUR\" />"
                    + "        </offer>"
                    + "        <offer market-sku=\"3\" delete=\"true\" />"
                    + "    </offers>"
                    + "</offer-prices>";
            bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            request = patchOfferPricesRequest(SUPPLIER_CAMPAIGN_ID, "xml", bodyEntity);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
            }

            SyncGetOffer.GetUnitedOffersResponse datacampResponse =
                    getUnitedDatacampResponse("getPricesFor661.json");
            Mockito.doReturn(DataCampStrollerConversions.fromStrollerResponse(datacampResponse))
                    .when(dataCampShopClient)
                    .searchBusinessOffers(any());
            mockSaasService(3, 661);

            //Проверяем, что 1 эвента отправлен в Логброкер
            Mockito.verify(logbrokerService).publishEvent(any(SyncChangeOfferLogbrokerEvent.class));

            request = getOfferPricesRequest(SUPPLIER_CAMPAIGN_ID, "xml", Collections.emptyMap());
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                MbiAsserts.assertXmlEquals(/*language=XML*/ ""
                        + "<response>"
                        + "    <status>OK</status>"
                        + "    <result>"
                        + "        <total>3</total>"
                        + "        <paging next-page-token=\"eyJvcCI6Ij4iLCJrZXkiOiI0NDQiLCJza2lwIjowfQ\"/>"
                        + "        <offers>"
                        + "            <offer id=\"111\" market-sku=\"1\" updated-at=\"2017-12-04T01:15:03+03:00\">"
                        + "                <price value=\"125\" currency-id=\"RUR\" vat=\"6\"/>"
                        + "            </offer>"
                        + "            <offer id=\"222\" market-sku=\"2\" updated-at=\"2017-12-04T01:15:03+03:00\">"
                        + "                <price value=\"94\" currency-id=\"RUR\" vat=\"6\"/>"
                        + "            </offer>"
                        + "            <offer id=\"444\" market-sku=\"4\" updated-at=\"2017-12-04T01:15:03+03:00\">"
                        + "                <price value=\"102\" currency-id=\"RUR\" vat=\"6\"/>"
                        + "            </offer>"
                        + "        </offers>"
                        + "    </result>"
                        + "</response>", bodyString);
            }

            bodyEntity = new StringEntity("<offer-price-removal remove-all=\"true\" />", ContentType.TEXT_XML);
            //для пуш партнеров ничего не поменяется, если у них не было записей в БД
            request = removeAllOfferPricesRequest(SUPPLIER_CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            }

            request = getOfferPricesRequest(SUPPLIER_CAMPAIGN_ID, "xml", Collections.emptyMap());
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                MbiAsserts.assertXmlEquals(/*language=XML*/ ""
                        + "<response>"
                        + "    <status>OK</status>"
                        + "    <result>"
                        + "        <total>3</total>"
                        + "        <paging next-page-token=\"eyJvcCI6Ij4iLCJrZXkiOiI0NDQiLCJza2lwIjowfQ\"/>"
                        + "        <offers>"
                        + "            <offer id=\"111\" market-sku=\"1\" updated-at=\"2017-12-04T01:15:03+03:00\">"
                        + "                <price value=\"125\" currency-id=\"RUR\" vat=\"6\"/>"
                        + "            </offer>"
                        + "            <offer id=\"222\" market-sku=\"2\" updated-at=\"2017-12-04T01:15:03+03:00\">"
                        + "                <price value=\"94\" currency-id=\"RUR\" vat=\"6\"/>"
                        + "            </offer>"
                        + "            <offer id=\"444\" market-sku=\"4\" updated-at=\"2017-12-04T01:15:03+03:00\">"
                        + "                <price value=\"102\" currency-id=\"RUR\" vat=\"6\"/>"
                        + "            </offer>"
                        + "        </offers>"
                        + "    </result>"
                        + "</response>", bodyString);
            }

        }
    }

    @DbUnitDataSet(before = "OfferPriceControllerMarketSkuTest.pushpartner.csv",
            after = "OfferPriceControllerMarketSkuTest.pushpartner.after.csv"
    )
    @DisplayName("Проверка на то, что синий может пойти в ЕОХ если включен ЕКат")
    @Test
    void listPrices_pushBlueInUnitedCatalog_success() {
        SyncGetOffer.GetUnitedOffersResponse datacampResponse =
                getUnitedDatacampResponse("getPricesForUnitedBluePush.json");
        Mockito.doReturn(DataCampStrollerConversions.fromStrollerResponse(datacampResponse))
                .when(dataCampShopClient)
                .searchBusinessOffers(any());
        mockSaasService(3, 665L);

        ResponseEntity<String> response = PartnerApiFunctionalTestHelper.getForXml(
                offerPricesURI(10665L, "xml", Collections.emptyMap()).toString(),
                "67282295"
        );

        assertXmlEquals(fileToString("pushBlueInUnitedCatalog.response"), response.getBody());
    }


    @DbUnitDataSet(before = "OfferPriceControllerMarketSkuTest.pushpartner.csv",
            after = "OfferPriceControllerMarketSkuTest.pushpartner.after.csv"
    )
    @DisplayName("Проверка на то, что синий может пойти в ЕОХ если включен ЕКат")
    @Test
    void listPrices_pushBlueInUnitedCatalog_success_big_offset() {
        environmentService.setValue("united.catalog.offer-prices.offset.threshold", "50");
        SyncGetOffer.GetUnitedOffersResponse datacampResponse =
                getUnitedDatacampResponse("getPricesForUnitedBluePush.json");
        Mockito.doReturn(DataCampStrollerConversions.fromStrollerResponse(datacampResponse))
                .when(dataCampShopClient)
                .searchBusinessOffers(any());
        mockSaasService(3, 665L);

        ResponseEntity<String> response = PartnerApiFunctionalTestHelper.getForXml(
                offerPricesURI(10665L, "xml", Map.of("offset", "100")).toString(),
                "67282295"
        );

        Mockito.verify(dataCampShopClient, Mockito.times(2))
                .searchBusinessOffers(any());

        assertXmlEquals(fileToString("pushBlueInUnitedCatalogOffset.response"), response.getBody());


    }

    @Test
    @DisplayName("Обновляем цену. Передаем msku. За маппингом в ssku должны пойти в КИ")
    @DbUnitDataSet(
            before = {
                    "OfferPriceControllerMarketSkuTest.pushpartner.csv",
                    "OfferPriceControllerMarketSkuTest.testMappingsFromCI.csv"
            }
    )
    void testMappingsFromCI() {
        MboMappings.ShopSKURequest mappingRequest = MboMappings.ShopSKURequest.newBuilder()
                .setShopId(661)
                .addMarketSkuId(1)
                .addMarketSkuId(2)
                .build();

        MboMappings.SKUMappingResponse mappingResponse = MboMappings.SKUMappingResponse.newBuilder()
                .setShopId(661)
                .addSkuMapping(MboMappings.SKUMappingResponse.SKUMapping.newBuilder()
                        .setMarketSkuId(1)
                        .addShopSku("sku-1")
                        .build())
                .addSkuMapping(MboMappings.SKUMappingResponse.SKUMapping.newBuilder()
                        .setMarketSkuId(2)
                        .addShopSku("sku-21")
                        .addShopSku("sku-22")
                        .build())
                .build();

        Mockito.when(patientMboMappingsService.getShopSKU(mappingRequest))
                .thenReturn(mappingResponse);

        ResponseEntity<String> response = PartnerApiFunctionalTestHelper.postForJson(
                patchOfferPricesURI(SUPPLIER_CAMPAIGN_ID, "json").toString(),
                StringTestUtil.getString(getClass(), "json/OfferPriceControllerMarketSkuTest.testMappingsFromCI" +
                        ".request.json"),
                "67282295"
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

        ArgumentCaptor<SyncChangeOfferLogbrokerEvent> captor =
                ArgumentCaptor.forClass(SyncChangeOfferLogbrokerEvent.class);
        Mockito.verify(logbrokerService).publishEvent(captor.capture());
        List<SyncChangeOfferLogbrokerEvent> events = captor.getAllValues();
        Assertions.assertEquals(1, events.size());
        Set<String> actualOfferIds = events.stream()
                .map(LogbrokerEvent::getPayload)
                .flatMap(Collection::stream)
                .map(DataCampEvent::convertToDataCampOffer)
                .map(DataCampOffer.Offer::getIdentifiers)
                .map(DataCampOfferIdentifiers.OfferIdentifiers::getOfferId)
                .collect(Collectors.toSet());
        Assertions.assertEquals(Set.of("sku-1", "sku-21", "sku-22"), actualOfferIds);
    }

    /**
     * Тест ручек для с явными и неявными Ват'ами batch-update'ов цен.
     *
     * <ul>
     * <li>Вводим несколько цен одним запросом;
     * <li>Запрашиваем список цен с дефолтной сортировкой и проверяем ожидаемый результат;
     * </ul>
     */
    @Disabled("Тест проверяет синих пулл-партнёров, но это неактуально")
    @Test
    @DbUnitDataSet(before = "OfferPriceControllerMarketSkuTest.vatRespectsDefault.csv")
    void vatRespectsDefault() throws Exception {
        mockUltraControllerClient();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = /*language=XML*/ ""
                    + "<offer-prices>"
                    + "    <offers>"
                    + "        <offer market-sku=\"1\">"
                    + "            <price value=\"125.25\" currency-id=\"RUR\" vat=\"2\"/>"
                    + "        </offer>"
                    + "        <offer market-sku=\"2\">"
                    + "            <price value=\"74.25\" currency-id=\"RUR\" />"
                    + "        </offer>"
                    + "        <offer market-sku=\"3\">"
                    + "            <price value=\"215.05\" currency-id=\"RUR\" vat=\"4\" />"
                    + "        </offer>"
                    + "    </offers>"
                    + "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(SUPPLIER_CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            }

            request = getOfferPricesRequest(SUPPLIER_CAMPAIGN_ID, "xml", Collections.emptyMap());
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                MbiAsserts.assertXmlEquals(/*language=XML*/ ""
                        + "<response>"
                        + "    <status>OK</status>"
                        + "    <result>"
                        + "        <total>3</total>"
                        + "        <paging/>"
                        + "        <offers>"
                        + "            <offer market-sku=\"1\" updated-at=\"2017-12-04T01:15:01+03:00\">"
                        + "                <price value=\"125\" currency-id=\"RUR\" vat=\"2\"/>"
                        + "            </offer>"
                        + "            <offer market-sku=\"2\" updated-at=\"2017-12-04T01:15:01+03:00\">"
                        + "                <price value=\"74\" currency-id=\"RUR\" vat=\"8\"/>"
                        + "            </offer>"
                        + "            <offer market-sku=\"3\" updated-at=\"2017-12-04T01:15:01+03:00\">"
                        + "                <price value=\"215\" currency-id=\"RUR\" vat=\"4\"/>"
                        + "            </offer>"
                        + "        </offers>"
                        + "    </result>"
                        + "</response>", bodyString);
            }
        }
    }


    /**
     * Тест ручек для с явными и неявными Ват'ами batch-update'ов цен.
     *
     * <ul>
     * <li>Вводим несколько цен одним запросом;
     * <li>Запрашиваем список цен с дефолтной сортировкой и проверяем ожидаемый результат;
     * </ul>
     */
    @Test
    @DbUnitDataSet(before = "OfferPriceControllerMarketSkuTest.vatIsValidated.csv")
    void vatIsValidated() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = /*language=XML*/ ""
                    + "<offer-prices>"
                    + "    <offers>"
                    + "        <offer market-sku=\"1\">"
                    + "            <price value=\"125.25\" currency-id=\"RUR\" vat=\"2\"/>"
                    + "        </offer>"
                    + "        <offer market-sku=\"2\">"
                    + "            <price value=\"74.25\" currency-id=\"RUR\" />"
                    + "        </offer>"
                    + "        <offer market-sku=\"3\">"
                    + "            <price value=\"215.05\" currency-id=\"RUR\" vat=\"6\" />"
                    + "        </offer>"
                    + "        <offer market-sku=\"4\">"
                    + "            <price value=\"215.05\" currency-id=\"RUR\" vat=\"3\" />"
                    + "        </offer>"
                    + "    </offers>"
                    + "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(SUPPLIER_CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                MbiAsserts.assertXmlEquals(/*language=XML*/ ""
                        + "<response>"
                        + "    <status>ERROR</status>"
                        + "    <errors>"
                        + "        <error code=\"BAD_REQUEST\""
                        + "               message=\"Vat setting of VAT_10 (2) not supported for current USN (1) " +
                        "taxSystem: feedId = 661661, marketSku = 1\"/>"
                        + "        <error code=\"BAD_REQUEST\""
                        + "               message=\"Vat setting of VAT_18_118 (3) not supported for current USN (1) " +
                        "taxSystem: feedId = 661661, marketSku = 4\"/>"
                        + "    </errors>"
                        + "</response>", bodyString);
            }
        }
    }


    /**
     * Тест ручек для с явными НДС'ами batch-update'ов цен.
     *
     * <ul>
     * <li>Вводим несколько цен одним запросом;
     * <li>Смотрим, что значения с НДС 20% отклоняются до времени перехода с НДС 18% на НДС 20%;
     * </ul>
     */
    @Test
    @DbUnitDataSet(before = "OfferPriceControllerMarketSkuTest.vat20IsAcceptedAfterSwitchover.csv")
    void vat20IsRejectedBeforeSwitchover() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = /*language=XML*/ ""
                    + "<offer-prices>"
                    + "    <offers>"
                    + "        <offer market-sku=\"1\">"
                    + "            <price value=\"125.25\" currency-id=\"RUR\" vat=\"1\"/>"
                    + "        </offer>"
                    + "        <offer market-sku=\"2\">"
                    + "            <price value=\"125.25\" currency-id=\"RUR\" vat=\"3\"/>"
                    + "        </offer>"
                    + "        <offer market-sku=\"3\">"
                    + "            <price value=\"125.25\" currency-id=\"RUR\" vat=\"7\"/>"
                    + "        </offer>"
                    + "        <offer market-sku=\"4\">"
                    + "            <price value=\"125.25\" currency-id=\"RUR\" vat=\"8\"/>"
                    + "        </offer>"
                    + "        <offer market-sku=\"5\">"
                    + "            <price value=\"125.25\" currency-id=\"RUR\" vat=\"2\"/>"
                    + "        </offer>"
                    + "        <offer market-sku=\"6\">"
                    + "            <price value=\"125.25\" currency-id=\"RUR\" vat=\"4\"/>"
                    + "        </offer>"
                    + "    </offers>"
                    + "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(SUPPLIER_CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                MbiAsserts.assertXmlEquals(/*language=XML*/ ""
                        + "<response>\n"
                        + "    <status>ERROR</status>\n"
                        + "    <errors>\n"
                        + "        <error code=\"BAD_REQUEST\" message=\"Vat setting of VAT_18 (1) not supported " +
                        "since year 2019: feedId = 661661, marketSku = 1\"/>\n"
                        + "        <error code=\"BAD_REQUEST\" message=\"Vat setting of VAT_18_118 (3) not supported " +
                        "since year 2019: feedId = 661661, marketSku = 2\"/>\n"
                        + "    </errors>\n"
                        + "</response>\n", bodyString);
            }
        }
    }

    /**
     * Тест ручек для с явными НДС'ами batch-update'ов цен.
     *
     * <ul>
     * <li>Вводим несколько цен одним запросом;
     * <li>Смотрим, что значения с НДС, как 18%, так и 20% принимаются. Но при этом 18% считаются синонимом 20%;
     * </ul>
     */
    @Test
    @Disabled("Тест проверяет синих пулл-партнёров, но это неактуально")
    @DbUnitDataSet(before = "OfferPriceControllerMarketSkuTest.vat20IsAcceptedAfterSwitchover.csv")
    void vat20IsAcceptedAfterSwitchover() throws Exception {
        mockUltraControllerClient();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = /*language=XML*/ ""
                    + "<offer-prices>"
                    + "    <offers>"
                    + "        <offer market-sku=\"1\">"
                    + "            <price value=\"125.25\" currency-id=\"RUR\" vat=\"2\"/>"
                    + "        </offer>"
                    + "        <offer market-sku=\"2\">"
                    + "            <price value=\"125.25\" currency-id=\"RUR\" vat=\"4\"/>"
                    + "        </offer>"
                    + "        <offer market-sku=\"3\">"
                    + "            <price value=\"125.25\" currency-id=\"RUR\" vat=\"7\"/>"
                    + "        </offer>"
                    + "        <offer market-sku=\"4\">"
                    + "            <price value=\"125.25\" currency-id=\"RUR\" vat=\"8\"/>"
                    + "        </offer>"
                    + "    </offers>"
                    + "</offer-prices>";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(SUPPLIER_CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            }
            request = getOfferPricesRequest(SUPPLIER_CAMPAIGN_ID, "xml", Collections.emptyMap());
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                MbiAsserts.assertXmlEquals(/*language=XML*/ ""
                        + "<response>\n"
                        + "    <status>OK</status>\n"
                        + "    <result>\n"
                        + "        <total>4</total>\n"
                        + "        <paging/>\n"
                        + "        <offers>\n"
                        + "            <offer updated-at=\"2017-12-04T01:15:01+03:00\" market-sku=\"1\">\n"
                        + "                <price value=\"125\" vat=\"2\" currency-id=\"RUR\"/>\n"
                        + "            </offer>\n"
                        + "            <offer updated-at=\"2017-12-04T01:15:01+03:00\" market-sku=\"2\">\n"
                        + "                <price value=\"125\" vat=\"4\" currency-id=\"RUR\"/>\n"
                        + "            </offer>\n"
                        + "            <offer updated-at=\"2017-12-04T01:15:01+03:00\" market-sku=\"3\">\n"
                        + "                <price value=\"125\" vat=\"7\" currency-id=\"RUR\"/>\n"
                        + "            </offer>\n"
                        + "            <offer updated-at=\"2017-12-04T01:15:01+03:00\" market-sku=\"4\">\n"
                        + "                <price value=\"125\" vat=\"8\" currency-id=\"RUR\"/>\n"
                        + "            </offer>\n"
                        + "        </offers>\n"
                        + "    </result>\n"
                        + "</response>\n", bodyString);
            }
        }
    }

    @Test
    @DbUnitDataSet(before = {
            "OfferPriceControllerMarketSkuTest.defaultVat.csv",
            "OfferPriceController.lb.enabled.csv"
    })
    void discountIsAllowedOnBlueMarkerFor1PTestXml() throws Exception {
        mockUltraControllerClient();

        String papiResponse = PartnerApiFunctionalTestHelper.postForXml(
                patchOfferPricesURI(ONE_P_SUPPLIER_CAMPAIGN_ID, "xml").toString(),
                StringTestUtil.getString(getClass(), "xml/blue1PWithDiscount.request.xml"),
                String.valueOf(SPBTESTER_UID_6)
        );
        assertResponse("xml/ok.response.xml", papiResponse);

        SyncChangeOffer.ChangeOfferRequest actual = getActualLbEvent();
        SyncChangeOffer.ChangeOfferRequest expected = ProtoTestUtil.getProtoMessageByJson(
                SyncChangeOffer.ChangeOfferRequest.class,
                "datacamp/blue1PWithDiscount.event.json",
                getClass()
        );

        ProtoTestUtil.assertThat(actual)
                .ignoringFieldsMatchingRegexes(".*timestamp.*")
                .isEqualTo(expected);
    }

    @Test
    @Disabled("Тест проверяет синих пулл-партнёров, но это неактуально")
    @DbUnitDataSet(before = {
            "OfferPriceControllerMarketSkuTest.defaultVat.csv",
            "OfferPriceController.lb.enabled.csv"
    })
    void invalidDiscountIsIgnoredOnBlueMarkerFor1PTestXml() throws Exception {
        mockUltraControllerClient();

        String papiResponse = PartnerApiFunctionalTestHelper.postForXml(
                patchOfferPricesURI(ONE_P_SUPPLIER_CAMPAIGN_ID, "xml").toString(),
                StringTestUtil.getString(getClass(), "xml/blue1PWithInvalidDiscount.request.xml"),
                String.valueOf(SPBTESTER_UID_6)
        );
        assertResponse("xml/ok.response.xml", papiResponse);

        SyncChangeOffer.ChangeOfferRequest actual = getActualLbEvent();
        SyncChangeOffer.ChangeOfferRequest expected = ProtoTestUtil.getProtoMessageByJson(
                SyncChangeOffer.ChangeOfferRequest.class,
                "datacamp/blue1PWithInvalidDiscount.event.json",
                getClass()
        );

        ProtoTestUtil.assertThat(actual)
                .ignoringFieldsMatchingRegexes(".*timestamp.*")
                .isEqualTo(expected);
    }

    @Test
    @DbUnitDataSet(before = "OfferPriceController.lb.enabled.csv")
    void discountIsAllowedOnBlueMarkerTestXml() throws Exception {
        mockUltraControllerClient();

        String papiResponse = PartnerApiFunctionalTestHelper.postForXml(
                patchOfferPricesURI(SUPPLIER_CAMPAIGN_ID, "xml").toString(),
                StringTestUtil.getString(getClass(), "xml/blueWithDiscount.request.xml"),
                String.valueOf(SPBTESTER_UID_5)
        );
        assertResponse("xml/ok.response.xml", papiResponse);

        SyncChangeOffer.ChangeOfferRequest actual = getActualLbEvent();
        SyncChangeOffer.ChangeOfferRequest expected = ProtoTestUtil.getProtoMessageByJson(
                SyncChangeOffer.ChangeOfferRequest.class,
                "datacamp/blueWithDiscount.event.json",
                getClass()
        );

        ProtoTestUtil.assertThat(actual)
                .ignoringFieldsMatchingRegexes(".*timestamp.*")
                .isEqualTo(expected);
    }

    private void assertResponse(String expectedPath, String actual) {
        MbiAsserts.assertXmlEquals(
                StringTestUtil.getString(getClass(), expectedPath),
                actual
        );
    }

    private SyncChangeOffer.ChangeOfferRequest getActualLbEvent() throws InvalidProtocolBufferException {
        ArgumentCaptor<SyncChangeOfferLogbrokerEvent> lbCaptor =
                ArgumentCaptor.forClass(SyncChangeOfferLogbrokerEvent.class);
        Mockito.verify(logbrokerService).publishEvent(lbCaptor.capture());
        Assertions.assertEquals(1, lbCaptor.getAllValues().size());
        SyncChangeOfferLogbrokerEvent event = lbCaptor.getValue();
        return SyncChangeOffer.ChangeOfferRequest.parseFrom(event.getBytes());
    }

    @Test
    void testOffersInsertOnly() throws Exception {
        mockUltraControllerClient();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "{\"offers\":[]}";
            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(CAMPAIGN_ID, "json", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Nonnull
    private String fileToString(@Nonnull String test) {
        return fileToString(test, "listPrices");
    }
}
