package ru.yandex.market.api.partner.controllers.price;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferPrice;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.api.partner.helper.PartnerApiFunctionalTestHelper;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.logbroker.event.datacamp.PapiOfferPriceDataCampEvent;
import ru.yandex.market.core.logbroker.event.datacamp.SyncChangeOfferLogbrokerEvent;
import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.ir.http.UltraControllerServiceStub;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.mbi.util.io.MbiFiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.mbi.util.MbiAsserts.assertXmlEquals;

@DbUnitDataSet(before = "OfferPriceControllerPushPartnerTest.before.csv")
public class OfferPriceControllerPushPartnerTest extends AbstractOfferPriceControllerTest {

    public static final String UPDATE_PRICE_SUPPLIER_REQUEST_BODY = "" +
            "<offer-prices>" +
            "    <offers>" +
            "        <offer market-sku=\"5\" shop-sku=\"123\">" +
            "            <price value=\"125.25\" currency-id=\"RUR\" />" +
            "        </offer>" +
            "    </offers>" +
            "</offer-prices>";
    public static final String UPDATE_PRICE_SHOP_REQUEST_BODY = "" +
            "<offer-prices>" +
            "    <offers>" +
            "        <offer id=\"123\">" +
            "            <price value=\"125.25\" currency-id=\"RUR\" />" +
            "        </offer>" +
            "    </offers>" +
            "</offer-prices>";
    private static final String SEND_TO_LOGBROKER = "market.quick.partner-api.send.to.logbroker";
    private static final long PULL_TO_PUSH_SUPPLIER_CAMPAIGN_ID = 10667L;
    private static final int BUSINESS_ID = 90667;
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
        Mockito.when(ultraControllerClient.getMarketSKU(any())).thenReturn(
                UltraController.SKUMappingResponse.newBuilder()
                        .addSkuMapping(
                                UltraController.SKUMappingResponse.SKUMapping.newBuilder()
                                        .addShopSku("123")
                                        .setMarketSkuId(5)
                                        .build())
                        .addSkuMapping(
                                UltraController.SKUMappingResponse.SKUMapping.newBuilder()
                                        .addShopSku("423")
                                        .setMarketSkuId(5)
                                        .build())
                        .addSkuMapping(
                                UltraController.SKUMappingResponse.SKUMapping.newBuilder()
                                        .addShopSku("134")
                                        .setMarketSkuId(5)
                                        .build())
                        .addSkuMapping(
                                UltraController.SKUMappingResponse.SKUMapping.newBuilder()
                                        .addShopSku("612")
                                        .setMarketSkuId(5)
                                        .build())
                        .build());
        mockUltraControllerClient();
        environmentService.setValue(SEND_TO_LOGBROKER, "true");
    }

    @DisplayName("При значении PARAM_VALUE = NO или PUSH_TO_PULL в логброкер уходит сообщение как от пулл-партнёра")
    @ValueSource(longs = {10665, 10668})
    @ParameterizedTest
    void notPushPartner(long campaignId) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            sendRequest(httpClient, UPDATE_PRICE_SUPPLIER_REQUEST_BODY, campaignId);

            assertSendInfoToLogbroker(DataCampOfferMeta.DataSource.PULL_PARTNER_API);
        }
    }

    @DisplayName("Для синих в логброкер уходит сообщение на удаление цены")
    @CsvSource({
            "PULL_PARTNER_API,10665",
            "PUSH_PARTNER_API,10667"
    })
    @ParameterizedTest(name = "При значении PARAM_VALUE = {0}")
    void updatePrices_noPushOfferWithDelete_success(DataCampOfferMeta.DataSource source,
                                                    long campaignId) {
        String response = PartnerApiFunctionalTestHelper.postForXml(
                patchOfferPricesURI(campaignId, "xml").toString(),
                fileToString("supplierOfferWithDelete.request"),
                "67282295"
        );

        assertXmlEquals(fileToString("successful.response"), response);
        assertSendInfoToLogbroker(source);
    }

    private void assertSendInfoToLogbroker(DataCampOfferMeta.DataSource source) {
        verify(logbrokerService, times(1)).publishEvent(any());
        verify(logbrokerService).publishEvent(captor.capture());

        PapiOfferPriceDataCampEvent event = (PapiOfferPriceDataCampEvent) captor.getValue()
                .getPayload()
                .iterator()
                .next();

        DataCampOffer.Offer offer = event.convertToDataCampOffer();
        DataCampOfferPrice.OfferPrice price = offer
                .getPrice();
        DataCampOfferPrice.PriceBundle bundle = source == DataCampOfferMeta.DataSource.PUSH_PARTNER_API
                ? price.getBasic()
                : price.getPriority();

        Assertions.assertEquals(source, bundle.getMeta().getSource());
        Assertions.assertFalse(offer.getStatus().hasUnitedCatalog());
        Assertions.assertFalse(offer.getStatus().hasOriginalCpa());
    }

    @DisplayName("Для синих работает запрос на удаление цен")
    @CsvSource({
            "NO,10665",
            "REAL,10667"
    })
    @ParameterizedTest(name = "При значении PARAM_VALUE = {0}")
    void clearOutPrices_noPushOfferWithDelete_success(String type,
                                                      long campaignId) {
        String response = PartnerApiFunctionalTestHelper.postForXml(
                removeAllOfferPricesURI(campaignId, "xml").toString(),
                fileToString("removeAll.request", "clearOutPrices"),
                "67282295"
        );

        assertXmlEquals(fileToString("successful.response"), response);
    }

    //TODO: revert
    @DisplayName("При значении PARAM_VALUE = REAL в логброкер уходит сообщение как от пуш-партнёра")
    @Test
    void pullToPushPartner() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            sendRequest(httpClient, UPDATE_PRICE_SUPPLIER_REQUEST_BODY, PULL_TO_PUSH_SUPPLIER_CAMPAIGN_ID);

            verify(logbrokerService, times(1)).publishEvent(any());
            verify(logbrokerService).publishEvent(captor.capture());
            PapiOfferPriceDataCampEvent event =
                    (PapiOfferPriceDataCampEvent) captor.getValue().getPayload().iterator().next();
            DataCampOffer.Offer offer = event.convertToDataCampOffer();

            Assertions.assertEquals(DataCampOfferMeta.DataSource.PUSH_PARTNER_API,
                    offer.getPrice().getBasic().getMeta().getSource());
            Assertions.assertFalse(offer.getIdentifiers().hasBusinessId());
            Assertions.assertFalse(offer.getStatus().getUnitedCatalog().getFlag());
            Assertions.assertFalse(offer.getStatus().hasOriginalCpa());
        }
    }

    @DisplayName("Для белого ДБС партнера уходит признак единого каталога и original cpa")
    @Test
    void updatePrices() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            sendRequest(httpClient, UPDATE_PRICE_SHOP_REQUEST_BODY, 107777);

            verify(logbrokerService, times(1)).publishEvent(any());
            verify(logbrokerService).publishEvent(captor.capture());
            PapiOfferPriceDataCampEvent event =
                    (PapiOfferPriceDataCampEvent) captor.getValue().getPayload().iterator().next();
            DataCampOffer.Offer offer = event.convertToDataCampOffer();

            Assertions.assertEquals(DataCampOfferMeta.DataSource.PUSH_PARTNER_API,
                    offer.getPrice().getBasic().getMeta().getSource());
            Assertions.assertFalse(offer.getIdentifiers().hasBusinessId());
            Assertions.assertTrue(offer.getStatus().hasUnitedCatalog());
            Assertions.assertTrue(offer.getStatus().getUnitedCatalog().getFlag());
            Assertions.assertTrue(offer.getStatus().hasOriginalCpa());
            Assertions.assertTrue(offer.getStatus().getOriginalCpa().getFlag());
        }
    }

    @DisplayName("Если у партнера есть бизнес, то он будет указан в сообщении лб")
    @Test
    @DbUnitDataSet(before = "OfferPriceControllerPushPartnerTest.partnerWithBusiness.before.csv")
    void partnerWithBusiness() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            sendRequest(httpClient, UPDATE_PRICE_SUPPLIER_REQUEST_BODY, PULL_TO_PUSH_SUPPLIER_CAMPAIGN_ID);

            verify(logbrokerService, times(1)).publishEvent(any());
            verify(logbrokerService).publishEvent(captor.capture());
            PapiOfferPriceDataCampEvent event =
                    (PapiOfferPriceDataCampEvent) captor.getValue().getPayload().iterator().next();
            DataCampOffer.Offer offer = event.convertToDataCampOffer();

            Assertions.assertEquals(DataCampOfferMeta.DataSource.PUSH_PARTNER_API,
                    offer.getPrice().getBasic().getMeta().getSource());
            Assertions.assertEquals(BUSINESS_ID,
                    offer.getIdentifiers().getBusinessId());
        }
    }

    @DisplayName("Проверяет, что для многоскладового fbs по схеме 1:N в процессе расклейки обновляем цену для всех " +
            "его реплик")
    @Test
    @DbUnitDataSet(before = "OfferPriceControllerPushPartnerTest.partnerInMigration.before.csv")
    void fbsInMigration() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            sendRequest(httpClient, UPDATE_PRICE_SUPPLIER_REQUEST_BODY, PULL_TO_PUSH_SUPPLIER_CAMPAIGN_ID);

            // проверяем, что отправили запрос для двух поставщиков 665 и 667
            verify(logbrokerService, times(2)).publishEvent(captor.capture());
            List<Integer> shopIds = captor.getAllValues().stream()
                    .map(SyncChangeOfferLogbrokerEvent::getPayload)
                    .flatMap(Collection::stream)
                    .map(PapiOfferPriceDataCampEvent.class::cast)
                    .map(e -> e.convertToDataCampOffer().getIdentifiers().getShopId())
                    .collect(Collectors.toList());
            assertThat(shopIds).containsOnly(665, 667);
        }
    }

    private void sendRequest(@Nonnull CloseableHttpClient httpClient, @Nonnull String requestBody,
                             long campaignId) throws IOException {
        StringEntity bodyEntity = new StringEntity(requestBody, ContentType.TEXT_XML);
        HttpUriRequest request = patchOfferPricesRequest(campaignId, "xml", bodyEntity);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String bodyResponse = MbiFiles.readText(response.getEntity()::getContent, StandardCharsets.UTF_8);
            Assertions.assertEquals(200, response.getStatusLine().getStatusCode(), bodyResponse);
        }
    }

    @DisplayName("При значении PARAM_VALUE = REAL и признаки delete возвращаем ошибку")
    @CsvSource({
            "shopPushOfferWithDelete,107700",
            "supplierPushOfferWithDelete,106669"
    })
    @ParameterizedTest(name = "{0}")
    void updatePrices_pushOfferWithDelete_badRequest(String test, long campaignId) {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> PartnerApiFunctionalTestHelper.postForXml(
                        patchOfferPricesURI(campaignId, "xml").toString(),
                        fileToString(test + ".request"),
                        "67282295"
                )
        );

        assertXmlEquals(fileToString(test + ".response"), exception.getResponseBodyAsString());
    }

    @DisplayName("При значении PARAM_VALUE = REAL и вызове ручки удаления")
    @CsvSource({
            "SHOP,107700",
            "SUPPLIER,106669"
    })
    @ParameterizedTest(name = "{0}")
    void clearOutPrices_pushClearOutPrices_badRequest(String test, long campaignId) {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> PartnerApiFunctionalTestHelper.postForXml(
                        removeAllOfferPricesURI(campaignId, "xml").toString(),
                        fileToString("removeAll.request", "clearOutPrices"),
                        "67282295"
                )
        );

        assertXmlEquals(fileToString("removeAll.response", "clearOutPrices"),
                exception.getResponseBodyAsString());
    }

    @Nonnull
    private String fileToString(@Nonnull String test) {
        return fileToString(test, "updatePrices");
    }

    @DisplayName("Падаем с 423 кодом при активной миграции на другой бизнес, updates, белый")
    @Test
    @DbUnitDataSet(before = "OfferPriceControllerPushPartnerTest.BusinessMigration.before.csv")
    void updatePrices_activeBusinessMigration_fail() {
        try {
            PartnerApiFunctionalTestHelper.postForXml(
                    patchOfferPricesURI(107700L, "xml").toString(),
                    fileToString("supplierOfferWithDelete.request"),
                    "67282295"
            );
            Assertions.fail("Migration check failed: check ignored");
        } catch (HttpClientErrorException ex) {
            Assertions.assertEquals(HttpStatus.LOCKED, ex.getStatusCode());
            Assertions.assertEquals("Locked", ex.getStatusText());
            MbiAsserts.assertXmlEquals("<response>\n" +
                    "    <status>ERROR</status>\n" +
                    "    <errors>\n" +
                    "        <error code=\"LOCKED\" message=\"Partner is in business migration: 7700\"/>\n" +
                    "    </errors>\n" +
                    "</response>", ex.getResponseBodyAsString());
        }
    }

    @DisplayName("Падаем с 423 кодом при активной миграции на другой бизнес, removals, синий")
    @Test
    @DbUnitDataSet(before = "OfferPriceControllerPushPartnerTest.BusinessMigration.before.csv")
    void clearOutPrices_activeBusinessMigration_fail() {
        try {
            String response = PartnerApiFunctionalTestHelper.postForXml(
                    removeAllOfferPricesURI(10665L, "xml").toString(),
                    fileToString("removeAll.request", "clearOutPrices"),
                    "67282295"
            );
            Assertions.fail("Migration check failed: check ignored");
        } catch (HttpClientErrorException ex) {
            Assertions.assertEquals(HttpStatus.LOCKED, ex.getStatusCode());
            Assertions.assertEquals("Locked", ex.getStatusText());
            MbiAsserts.assertXmlEquals("<response>\n" +
                    "    <status>ERROR</status>\n" +
                    "    <errors>\n" +
                    "        <error code=\"LOCKED\" message=\"Partner is in business migration: 665\"/>\n" +
                    "    </errors>\n" +
                    "</response>", ex.getResponseBodyAsString());
        }
    }
}
