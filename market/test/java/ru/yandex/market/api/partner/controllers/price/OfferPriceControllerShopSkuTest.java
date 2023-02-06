package ru.yandex.market.api.partner.controllers.price;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferMeta;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.core.logbroker.event.datacamp.PapiOfferPriceDataCampEvent;
import ru.yandex.market.core.logbroker.event.datacamp.SyncChangeOfferLogbrokerEvent;
import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.ir.http.UltraControllerServiceStub;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.logbroker.event.LogbrokerEvent;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.mbi.util.io.MbiFiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

class OfferPriceControllerShopSkuTest extends AbstractOfferPriceControllerTest {

    private static final String SEND_TO_LOGBROKER = "market.quick.partner-api.send.to.logbroker";

    private static final String STOP_USING_MARKET_SHOP_SKU = "market.supplier.1p.stopUsingMskuSsku";

    private static final long PUSH_SUPPLIER_CAMPAIGN_ID = 10663L;

    private static final long PUSH_SUPPLIER_3P_CAMPAIGN_ID = 10664L;

    private static final List<UltraController.SKUMappingResponse.SKUMapping> MAPPING_LIST =
            List.of(
                    UltraController.SKUMappingResponse.SKUMapping.newBuilder()
                            .addShopSku("123")
                            .setMarketSkuId(1)
                            .build(),
                    UltraController.SKUMappingResponse.SKUMapping.newBuilder()
                            .addShopSku("12344")
                            .addShopSku("12345")
                            .setMarketSkuId(2)
                            .build()
            );
    @Captor
    private final ArgumentCaptor<SyncChangeOfferLogbrokerEvent> captor =
            ArgumentCaptor.forClass(SyncChangeOfferLogbrokerEvent.class);
    @Autowired
    @Qualifier("marketQuickLogbrokerService")
    private LogbrokerEventPublisher<SyncChangeOfferLogbrokerEvent> logbrokerService;
    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private UltraControllerServiceStub ultraControllerClient;

    @BeforeEach
    void setUp() {
        environmentService.setValue(SEND_TO_LOGBROKER, "false");
        environmentService.setValue(STOP_USING_MARKET_SHOP_SKU, "false");
        Mockito.when(ultraControllerClient.getMarketSKU(any())).thenReturn(
                UltraController.SKUMappingResponse.newBuilder()
                        .addAllSkuMapping(MAPPING_LIST)
                        .build());
        Mockito.when(ultraControllerClient.getShopSKU(any())).thenReturn(
                UltraController.SKUMappingResponse.newBuilder()
                        .addAllSkuMapping(MAPPING_LIST)
                        .build());
    }

    @DisplayName("Управление ценой по shopSku - через Логброкер отправляется сообщение с shopSku без warehouseId" +
            " при отключении размножения по складам")
    @Test
    void shopSkuUpdatesFeedExpansionDisabled() throws Exception {
        prepareMocksForShopSkuLogbrokerTests();

        verify(logbrokerService).publishEvent(captor.capture());
        Assertions.assertEquals(captor.getAllValues().size(), 1);
        //2 оффера
        var offers = captor.getValue().getPayload();
        Assertions.assertEquals(offers.size(), 2);
        var iterator = offers.iterator();

        DataCampOffer.Offer offer = iterator.next().convertToDataCampOffer();
        Assertions.assertAll(
                () -> assertEquals(offer.getIdentifiers().getShopId(), 663),
                () -> assertEquals(offer.getIdentifiers().getFeedId(), 663663),
                () -> assertEquals(offer.getIdentifiers().getExtra().getMarketSkuId(), 1L),
                () -> assertFalse(offer.getIdentifiers().hasWarehouseId()),
                () -> assertEquals(offer.getIdentifiers().getExtra().getShopSku(), "123"),
                () -> assertEquals(offer.getMeta().getRgb(), DataCampOfferMeta.MarketColor.BLUE),
                () -> assertEquals(offer.getPrice().getBasic().getMeta().getSource(),
                        DataCampOfferMeta.DataSource.PUSH_PARTNER_API)
        );

        DataCampOffer.Offer offer1 = iterator.next().convertToDataCampOffer();
        Assertions.assertAll(
                () -> assertEquals(offer1.getIdentifiers().getShopId(), 663),
                () -> assertEquals(offer1.getIdentifiers().getFeedId(), 663663),
                () -> assertEquals(offer1.getIdentifiers().getExtra().getMarketSkuId(), 2L),
                () -> assertFalse(offer1.getIdentifiers().hasWarehouseId()),
                () -> assertEquals(offer1.getIdentifiers().getExtra().getShopSku(), "456"),
                () -> assertEquals(offer1.getMeta().getRgb(), DataCampOfferMeta.MarketColor.BLUE),
                () -> assertEquals(offer1.getPrice().getBasic().getMeta().getSource(),
                        DataCampOfferMeta.DataSource.PUSH_PARTNER_API)
        );
    }

    private void prepareMocksForShopSkuLogbrokerTests() throws IOException {
        environmentService.setValue(SEND_TO_LOGBROKER, "true");
        Mockito.when(ultraControllerClient.getMarketSKU(any())).thenReturn(
                UltraController.SKUMappingResponse.newBuilder()
                        .addSkuMapping(
                                UltraController.SKUMappingResponse.SKUMapping.newBuilder()
                                        .addShopSku("123")
                                        .setMarketSkuId(1)
                                        .build())
                        .addSkuMapping(
                                UltraController.SKUMappingResponse.SKUMapping.newBuilder()
                                        .addShopSku("456")
                                        .setMarketSkuId(2)
                                        .build())
                        .build());
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer market-sku=\"1\" shop-sku=\"123\">" +
                    "            <price value=\"125.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer market-sku=\"2\" shop-sku=\"456\">" +
                    "            <price value=\"125.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";

            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(PUSH_SUPPLIER_CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode(), bodyString);
            }
        }
    }

    @DisplayName("Пуш-партнёры могут указать offerId без marketSku")
    @Test
    void absenceMarketSkuOnlyForPushPartner() throws Exception {
        Mockito.when(ultraControllerClient.getMarketSKU(any())).thenReturn(
                UltraController.SKUMappingResponse.newBuilder()
                        .addSkuMapping(
                                UltraController.SKUMappingResponse.SKUMapping.newBuilder()
                                        .addShopSku("123")
                                        .setMarketSkuId(1)
                                        .build())
                        .build());

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer shop-sku=\"123\">" +
                    "            <price value=\"125.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";

            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(PUSH_SUPPLIER_CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            }
        }
    }

    @DisplayName("Маппинг msku -> ssku для пуш-партнера")
    @Test
    void absenceMarketSkuOnlyForPushPartnerWithMapping() throws Exception {
        environmentService.setValue(SEND_TO_LOGBROKER, "true");
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer shop-sku=\"123\">" +
                    "            <price value=\"125.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer market-sku=\"2\">" +
                    "            <price value=\"25.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";

            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(PUSH_SUPPLIER_CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            }
        }
        //проверили, что дозапрпшиваем маппинги маркет-ску - шоп-ску
        verify(ultraControllerClient).getShopSKU(Mockito.any());
        verify(logbrokerService).publishEvent(captor.capture());
        Assertions.assertEquals(captor.getAllValues().size(), 1);
        //3 оффера
        var offers = captor.getValue().getPayload();
        Assertions.assertEquals(3, offers.size());
    }

    @DisplayName("Маппинг msku -> ssku для 3P пуш-партнера")
    @Test
    void absenceMarketSkuOnlyFor3PPushPartnerWithMapping() throws Exception {
        environmentService.setValue(SEND_TO_LOGBROKER, "true");
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer shop-sku=\"123\">" +
                    "            <price value=\"125.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer market-sku=\"2\">" +
                    "            <price value=\"25.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";

            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(PUSH_SUPPLIER_3P_CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            }
        }
        //проверили, что дозапрпшиваем маппинги маркет-ску - шоп-ску
        verify(ultraControllerClient).getShopSKU(Mockito.any());
        verify(logbrokerService).publishEvent(captor.capture());
        Assertions.assertEquals(captor.getAllValues().size(), 1);
        //3 оффера
        var offers = captor.getValue().getPayload();
        Assertions.assertEquals(offers.size(), 3);
    }

    //TODO:MBI-44071 проверить, что будет возвращать 400 или удалить тест, если доработка не требуется
    @DisplayName("Пуш-партнер. Не удалось найти маппинг мску и шоп-ску")
    @Test
    void absenceMarketSkuMappingToShopSkuForPushPartner() throws Exception {
        Mockito.when(ultraControllerClient.getMarketSKU(any())).thenReturn(
                UltraController.SKUMappingResponse.newBuilder()
                        .addSkuMapping(
                                UltraController.SKUMappingResponse.SKUMapping.newBuilder()
                                        .addShopSku("123")
                                        .setMarketSkuId(1)
                                        .build())
                        .build());
        Mockito.when(ultraControllerClient.getShopSKU(any())).thenReturn(
                UltraController.SKUMappingResponse.newBuilder()
                        .build());
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer shop-sku=\"123\">" +
                    "            <price value=\"125.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer market-sku=\"2\">" +
                    "            <price value=\"25.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";

            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(PUSH_SUPPLIER_CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                MbiAsserts.assertXmlEquals(/*language=XML*/ "" +
                        "<response>" +
                        "    <status>ERROR</status><errors><error code=\"BAD_REQUEST\" message=\"Unable to find " +
                        "mapping for marketSku: 2\"/></errors>" +
                        "</response>", bodyString);
            }
        }
    }

    @DisplayName("Можно одновременно указывать shopSku и marketSku")
    @Test
    void marketSkuAndShopSkuForSuppliers() throws Exception {
        environmentService.setValue(SEND_TO_LOGBROKER, "true");
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer market-sku=\"1\" shop-sku=\"123\">" +
                    "            <price value=\"125.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";

            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(PUSH_SUPPLIER_3P_CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            }
            verify(logbrokerService).publishEvent(captor.capture());
            Assertions.assertEquals(1, captor.getAllValues().size());
            var offers = captor.getValue().getPayload();
            Assertions.assertEquals(1, offers.size());
            DataCampOffer.Offer offer = offers.iterator().next().convertToDataCampOffer();
            Assertions.assertEquals("123", offer.getIdentifiers().getOfferId());
            Assertions.assertEquals(664664, offer.getIdentifiers().getFeedId());
        }
    }

    @DisplayName("Если одновременно указывать shopSku и marketSku, то marketSku не валидируется")
    @Test
    void ignoreMarketSkuWhenShopSkuIsProvided() throws Exception {
        Mockito.when(ultraControllerClient.getShopSKU(any())).thenReturn(
                UltraController.SKUMappingResponse.newBuilder()
                        .build());
        environmentService.setValue(SEND_TO_LOGBROKER, "true");
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer market-sku=\"100500\" shop-sku=\"123\">" +
                    "            <price value=\"125.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";

            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(PUSH_SUPPLIER_3P_CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            }
            verify(logbrokerService).publishEvent(captor.capture());
            Assertions.assertEquals(1, captor.getAllValues().size());
            var offers = captor.getValue().getPayload();
            Assertions.assertEquals(1, offers.size());
            DataCampOffer.Offer offer = offers.iterator().next().convertToDataCampOffer();
            Assertions.assertEquals("123", offer.getIdentifiers().getOfferId());
            Assertions.assertEquals(664664, offer.getIdentifiers().getFeedId());
        }
    }

    @DisplayName("Удаление shopSku и схлопывание цен с одинаковым marketSku при поднятом флаге " +
            "market.supplier.1p.stopUsingMskuSsku")
    @Test
    void clearShopSkusIfFlagIsRaised() throws Exception {
        environmentService.setValue(SEND_TO_LOGBROKER, "true");
        environmentService.setValue(STOP_USING_MARKET_SHOP_SKU, "true");
        PriceChangeHelpService.usingMskuSskuNextCheckTime = 0L;
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer market-sku=\"1\" shop-sku=\"123\">" +
                    "            <price value=\"125.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "        <offer market-sku=\"1\" shop-sku=\"456\">" +
                    "            <price value=\"125.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";

            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(PUSH_SUPPLIER_CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode(), bodyString);
            }
        }

        verify(logbrokerService, times(1)).publishEvent(any());
        verify(logbrokerService).publishEvent(captor.capture());
        PapiOfferPriceDataCampEvent event =
                (PapiOfferPriceDataCampEvent) captor.getValue().getPayload().iterator().next();
        DataCampOffer.Offer offer = event.convertToDataCampOffer();

        assertEquals(captor.getAllValues().size(), 1);
        assertEquals(offer.getIdentifiers().getExtra().getMarketSkuId(), 1L);
        assertTrue(offer.getIdentifiers().getExtra().hasShopSku());
    }

    @DisplayName("При поднятом флаге market.supplier.1p.stopUsingMskuSsku нормально проходят по offerId")
    @Test
    void clearShopSkuIsNotAffectedOfferIds() throws Exception {
        environmentService.setValue(SEND_TO_LOGBROKER, "true");
        environmentService.setValue(STOP_USING_MARKET_SHOP_SKU, "true");
        long feedId = 479633L;
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
            HttpUriRequest request = patchOfferPricesRequest(CAMPAIGN_ID, "xml", bodyEntity);
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
            request = patchOfferPricesRequest(CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            }
        }
    }

    @DisplayName("Кирилистические символы в shopSku")
    @Test
    void cyrillicInShopSku() throws Exception {
        environmentService.setValue(SEND_TO_LOGBROKER, "true");
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer shop-sku=\"тут.немного,кириллицы555zuk\">" +
                    "            <price value=\"250\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";

            Mockito.when(ultraControllerClient.getMarketSKU(any())).thenReturn(
                    UltraController.SKUMappingResponse.newBuilder()
                            .addSkuMapping(
                                    UltraController.SKUMappingResponse.SKUMapping.newBuilder()
                                            .addShopSku("тут.немного,кириллицы555zuk")
                                            .setMarketSkuId(1)
                                            .build())
                            .build());

            StringEntity bodyEntity = new StringEntity(bodyString, StandardCharsets.UTF_8);
            HttpUriRequest request = patchOfferPricesRequest(PUSH_SUPPLIER_CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            }

            verify(logbrokerService, times(1)).publishEvent(any());
            verify(logbrokerService).publishEvent(captor.capture());
            PapiOfferPriceDataCampEvent event =
                    (PapiOfferPriceDataCampEvent) captor.getValue().getPayload().iterator().next();
            DataCampOffer.Offer offer = event.convertToDataCampOffer();

            Assertions.assertEquals(captor.getAllValues().size(), 1);
            Assertions.assertEquals("тут.немного,кириллицы555zuk", offer.getIdentifiers().getOfferId());
            Assertions.assertEquals("тут.немного,кириллицы555zuk", offer.getIdentifiers().getExtra().getShopSku());
        }
    }

    @DisplayName("Отсутствуют маппинги")
    @Test
    void shopSkuUpdatesNoMarketSku() throws Exception {
        environmentService.setValue(SEND_TO_LOGBROKER, "true");
        Mockito.when(ultraControllerClient.getMarketSKU(any())).thenReturn(
                UltraController.SKUMappingResponse.newBuilder()
                        .addSkuMapping(
                                UltraController.SKUMappingResponse.SKUMapping.newBuilder()
                                        .addShopSku("123")
                                        .setMarketSkuId(1)
                                        .build())
                        .addSkuMapping(
                                UltraController.SKUMappingResponse.SKUMapping.newBuilder()
                                        .addShopSku("456")
                                        .build())
                        .addSkuMapping(
                                UltraController.SKUMappingResponse.SKUMapping.newBuilder()
                                        .addShopSku("789")
                                        .build())
                        .build());
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer shop-sku=\"123\">" +
                    "            <price value=\"125.25\" currency-id=\"RUR\" />" +
                    "        <offer shop-sku=\"456\">" +
                    "            <price value=\"125.25\" currency-id=\"RUR\" />" +
                    "        <offer shop-sku=\"789\">" +
                    "            <price value=\"125.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";

            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(PUSH_SUPPLIER_CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                Assertions.assertEquals(400, response.getStatusLine().getStatusCode(), bodyString);
            }
        }
        verifyZeroInteractions(logbrokerService);
    }

    @DisplayName("Пустой ответ от УК")
    @Test
    void shopSkuUpdatesEmptyUkResponse() throws Exception {
        environmentService.setValue(SEND_TO_LOGBROKER, "true");
        Mockito.when(ultraControllerClient.getMarketSKU(any())).thenReturn(
                UltraController.SKUMappingResponse.newBuilder()
                        .build());
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String bodyString = "" +
                    "<offer-prices>" +
                    "    <offers>" +
                    "        <offer shop-sku=\"123\">" +
                    "            <price value=\"125.25\" currency-id=\"RUR\" />" +
                    "        <offer shop-sku=\"456\">" +
                    "            <price value=\"125.25\" currency-id=\"RUR\" />" +
                    "        <offer shop-sku=\"789\">" +
                    "            <price value=\"125.25\" currency-id=\"RUR\" />" +
                    "        </offer>" +
                    "    </offers>" +
                    "</offer-prices>";

            StringEntity bodyEntity = new StringEntity(bodyString, ContentType.TEXT_XML);
            HttpUriRequest request = patchOfferPricesRequest(PUSH_SUPPLIER_CAMPAIGN_ID, "xml", bodyEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                bodyString = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
                Assertions.assertEquals(400, response.getStatusLine().getStatusCode(), bodyString);
            }
        }
        verifyZeroInteractions(logbrokerService);
    }
}
