package ru.yandex.market.api.partner.controllers.offers.stored;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferPrice;
import NMarketIndexer.Common.Common;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.logbroker.event.datacamp.DataCampEvent;
import ru.yandex.market.core.logbroker.event.datacamp.SyncChangeOfferLogbrokerEvent;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbi.util.MbiMatchers;

/**
 * Тесты для {@link StoredOffersController}.
 * <p>
 * Здесь только базовые smoke-тесты.
 * Более подробно запись в логброкер тестируется в
 * {@link ru.yandex.market.core.datacamp.OfferUpdateDataCampServiceTest}.
 * Более подробно валидация запросов тестируется в
 * <ul>
 * <li>{@link OfferValidationPipelineTest}
 * <li>{@link ShopIdentificationOfferValidationPipelineTest}
 * <li>{@link SupplierIdentificationOfferValidationPipelineTest}
 * </ul>
 */
@ParametersAreNonnullByDefault
@DbUnitDataSet(before = "StoredOffersControllerTest.csv")
class StoredOffersControllerTest extends FunctionalTest {
    private final long CAMPAIGN_ID = 10876;
    private final int SUPPLIER_ID = 876;

    @Autowired
    @Qualifier("marketQuickLogbrokerService")
    private LogbrokerEventPublisher<SyncChangeOfferLogbrokerEvent> logbrokerService;

    @Test
    void testSuccessfulRequestXmlBasicPriceOnly() {
        String url = String.format("%s/v2/campaigns/%d/offers/stored/updates", urlBasePrefix, CAMPAIGN_ID);
        //language=xml
        String requestXml = ""
                + "<offer-update>"
                + "    <offers>"
                + "        <offer shop-sku=\"ABX123\">"
                + "            <pricing currency-id=\"RUR\">"
                + "                <price value=\"123.55\" />"
                + "                <purchase-price value=\"67.89\" />"
                + "                <oldprice auto=\"true\" />"
                + "                <vat>VAT_20</vat>"
                + "            </pricing>"
                + "        </offer>"
                + "    </offers>"
                + "</offer-update>";
        //language=xml
        String expectedResponseXml = ""
                + "<response>"
                + "    <status>OK</status>"
                + "    <result has-ignored-errors=\"false\" update-time=\"2019-08-01T01:26:41.766+10:00\">"
                + "        <statistics requested-offers=\"1\""
                + "                    error-offers=\"0\""
                + "                    warning-offers=\"1\""
                + "                    processed-offers=\"1\"/>"
                + "        <offers>"
                + "            <offer result=\"WARNING\" shop-sku=\"ABX123\">"
                + "                 <messages>"
                + "                     <message code=\"FRACTIONAL_PRICE\" level=\"WARNING\">"
                + "                         <description>Price is fractional, rounded value will be used</description>"
                + "                     </message>"
                + "                 </messages>"
                + "            </offer>"
                + "        </offers>"
                + "    </result>"
                + "</response>";
        String responseXml =
                FunctionalTestHelper.makeRequest(url, HttpMethod.POST, Format.XML, requestXml, String.class, 123L).getBody();

        MatcherAssert.assertThat(responseXml, MbiMatchers.xmlEquals(expectedResponseXml, Collections.singleton(
                "update-time")));

        ArgumentCaptor<SyncChangeOfferLogbrokerEvent> captor =
                ArgumentCaptor.forClass(SyncChangeOfferLogbrokerEvent.class);
        Mockito.verify(logbrokerService).publishEvent(captor.capture());
        SyncChangeOfferLogbrokerEvent genericEvent = captor.getValue();
        Assertions.assertNotNull(genericEvent);
        List<DataCampOffer.Offer> writtenToLogbrokerOffers =
                genericEvent.getPayload()
                        .stream()
                        .map(DataCampEvent::convertToDataCampOffer)
                        .collect(Collectors.toList());
        Assertions.assertEquals(1, writtenToLogbrokerOffers.size());
        DataCampOfferIdentifiers.OfferIdentifiers expectedOffer1Id =
                DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setFeedId(76)
                        .setOfferId("ABX123")
                        .setShopId(SUPPLIER_ID)
                        .setBusinessId(1000)
                        .setExtra(DataCampOfferIdentifiers.OfferExtraIdentifiers.newBuilder()
                                .setShopSku("ABX123")
                                .build())
                        .build();
        Assertions.assertEquals(expectedOffer1Id, writtenToLogbrokerOffers.get(0).getIdentifiers());

        assertPurchasePrice(writtenToLogbrokerOffers.get(0).getPrice(), "RUR", 678900000);
    }

    @Test
    void testSuccessfulRequestJsonBasicPriceOnly() {
        String url = String.format("%s/v2/campaigns/%d/offers/stored/updates", urlBasePrefix, CAMPAIGN_ID);
        //language=json
        String requestJson = ""
                + "{"
                + "    \"offers\": ["
                + "        {"
                + "            \"shopSku\":\"ABX123\","
                + "            \"pricing\": {"
                + "                \"currencyId\":\"RUR\","
                + "                \"vat\":\"VAT_20\","
                + "                \"price\": { \"value\": 123.55 },"
                + "                \"purchase-price\": { \"value\": 67.89 },"
                + "                \"oldprice\": { \"auto\": true }"
                + "            }"
                + "        }"
                + "    ]"
                + "}";

        String expectedResponseJson = getExpectedResponseJson();
        String responseJson = FunctionalTestHelper.makeRequest(
                url, HttpMethod.POST, Format.JSON, requestJson, String.class, 123L).getBody();

        MatcherAssert.assertThat(responseJson, MbiMatchers.jsonEquals(expectedResponseJson));

        ArgumentCaptor<SyncChangeOfferLogbrokerEvent> captor =
                ArgumentCaptor.forClass(SyncChangeOfferLogbrokerEvent.class);
        Mockito.verify(logbrokerService).publishEvent(captor.capture());
        SyncChangeOfferLogbrokerEvent genericEvent = captor.getValue();
        Assertions.assertNotNull(genericEvent);
        List<DataCampOffer.Offer> writtenToLogbrokerOffers =
                genericEvent.getPayload()
                        .stream()
                        .map(DataCampEvent::convertToDataCampOffer)
                        .collect(Collectors.toList());
        Assertions.assertEquals(1, writtenToLogbrokerOffers.size());
        DataCampOfferIdentifiers.OfferIdentifiers expectedOffer1Id =
                DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setFeedId(76)
                        .setOfferId("ABX123")
                        .setShopId(SUPPLIER_ID)
                        .setBusinessId(1000)
                        .setExtra(DataCampOfferIdentifiers.OfferExtraIdentifiers.newBuilder()
                                .setShopSku("ABX123")
                                .build())
                        .build();
        Assertions.assertEquals(expectedOffer1Id, writtenToLogbrokerOffers.get(0).getIdentifiers());

        assertPurchasePrice(writtenToLogbrokerOffers.get(0).getPrice(), "RUR", 678900000);
    }

    @Test
    void testSuccessfulRequestJsonBothBasicAndOverriddenPrices() {
        String url = String.format("%s/v2/campaigns/%d/offers/stored/updates", urlBasePrefix, CAMPAIGN_ID);
        //language=json
        String requestJson = ""
                + "{"
                + "    \"offers\": ["
                + "        {"
                + "            \"shopSku\":\"ABX123\","
                + "            \"pricing\": {"
                + "                \"currencyId\":\"RUR\","
                + "                \"vat\":\"VAT_0\","
                + "                \"price\": { \"value\": 123.55 }"
                + "            },"
                + "             \"pricingOverrides\": ["
                + "                {"
                + "                         \"currencyId\":\"RUR\","
                + "                         \"warehouseId\": 147,"
                + "                         \"vat\":\"VAT_20\","
                + "                         \"price\": { \"value\": 130.55 }"
                + "                 }"
                + "              ]"
                + "        }"
                + "    ]"
                + "}";
        String expectedResponseJson = getExpectedResponseJson();
        String responseJson = FunctionalTestHelper.makeRequest(
                url, HttpMethod.POST, Format.JSON, requestJson, String.class, 123L).getBody();

        MatcherAssert.assertThat(responseJson, MbiMatchers.jsonEquals(expectedResponseJson));

        ArgumentCaptor<SyncChangeOfferLogbrokerEvent> captor =
                ArgumentCaptor.forClass(SyncChangeOfferLogbrokerEvent.class);
        Mockito.verify(logbrokerService).publishEvent(captor.capture());
        SyncChangeOfferLogbrokerEvent genericEvent = captor.getValue();
        Assertions.assertNotNull(genericEvent);
        List<DataCampOffer.Offer> writtenToLogbrokerOffers =
                genericEvent.getPayload()
                        .stream()
                        .map(DataCampEvent::convertToDataCampOffer)
                        .collect(Collectors.toList());
        Assertions.assertEquals(1, writtenToLogbrokerOffers.size());
        DataCampOfferIdentifiers.OfferIdentifiers expectedOffer1Id =
                DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId("ABX123")
                        .setFeedId(76)
                        .setShopId(SUPPLIER_ID)
                        .setBusinessId(1000)
                        .setExtra(DataCampOfferIdentifiers.OfferExtraIdentifiers.newBuilder()
                                .setShopSku("ABX123")
                                .build())
                        .build();

        Common.PriceExpression priceWh1 = Common.PriceExpression.newBuilder()
                .setId("RUR")
                .setPrice(1310000000L)
                .build();

        DataCampOfferPrice.PriceBundle priceBundleWh1 = DataCampOfferPrice.PriceBundle.newBuilder()
                .setVat(5)
                .setEnabled(true)
                .setBinaryPrice(priceWh1).build();

        DataCampOfferIdentifiers.OfferIdentifiers actualOfferId = writtenToLogbrokerOffers.get(0).getIdentifiers();
        DataCampOfferPrice.OfferPrice actualPrice = writtenToLogbrokerOffers.get(0).getPrice();
        Assertions.assertEquals(expectedOffer1Id, actualOfferId);
        Assertions.assertFalse(actualOfferId.hasWarehouseId());

        Assertions.assertEquals(1, actualPrice.getPriceByWarehouseCount());
        Assertions.assertTrue(actualPrice.hasBasic());

        Map<Integer, DataCampOfferPrice.PriceBundle> actualPriceByWarehouseMap = actualPrice.getPriceByWarehouseMap();
        Assertions.assertNotNull(actualPriceByWarehouseMap.get(147));
        DataCampOfferPrice.PriceBundle actualBundle1 = actualPriceByWarehouseMap.get(147);
        Assertions.assertEquals(priceBundleWh1.getVat(), actualBundle1.getVat());
        Assertions.assertEquals(priceBundleWh1.getBinaryPrice(), actualBundle1.getBinaryPrice());
        Assertions.assertEquals(DataCampOfferPrice.Vat.VAT_0, actualPrice.getOriginalPriceFields().getVat().getValue());
    }

    @Test
    void testSuccessfulRequestXmlWithPricesOverridesOnly() {
        String url = String.format("%s/v2/campaigns/%d/offers/stored/updates", urlBasePrefix, CAMPAIGN_ID);
        //language=xml
        String requestXml = ""
                + "<offer-update>"
                + "    <offers>"
                + "        <offer shop-sku=\"ABX123\">"
                + "             <pricing-overrides>"
                + "                 <pricing currency-id=\"RUR\" warehouse-id=\"145\">"
                + "                     <price value=\"123.55\"/>"
                + "                     <vat>VAT_20</vat>"
                + "                 </pricing>"
                + "                 <pricing currency-id=\"RUR\" warehouse-id=\"147\">"
                + "                     <price value=\"163.55\"/>"
                + "                     <oldprice value=\"180.30\"/>"
                + "                     <vat>VAT_20</vat>"
                + "                 </pricing>"
                + "             </pricing-overrides>"
                + "        </offer>"
                + "    </offers>"
                + "</offer-update>";
        //language=xml
        String expectedResponseXml = ""
                + "<response>"
                + "    <status>OK</status>"
                + "    <result has-ignored-errors=\"false\" update-time=\"2020-11-13T15:26:41.766+10:00\">"
                + "        <statistics requested-offers=\"1\""
                + "                    error-offers=\"0\""
                + "                    warning-offers=\"1\""
                + "                    processed-offers=\"1\"/>"
                + "        <offers>"
                + "            <offer result=\"WARNING\" shop-sku=\"ABX123\">"
                + "                 <messages>"
                + "                     <message code=\"FRACTIONAL_OLD_PRICE\" level=\"WARNING\">"
                + "                         <description>"
                + "                          Old price is fractional, rounded value will be used</description>"
                + "                     </message>"
                + "                     <message code=\"FRACTIONAL_PRICE\" level=\"WARNING\">"
                + "                         <description>Price is fractional, rounded value will be used</description>"
                + "                     </message>"
                + "                 </messages>"
                + "            </offer>"
                + "        </offers>"
                + "    </result>"
                + "</response>";
        String responseXml =
                FunctionalTestHelper.makeRequest(url, HttpMethod.POST, Format.XML, requestXml, String.class, 123L).getBody();

        MatcherAssert.assertThat(responseXml, MbiMatchers.xmlEquals(expectedResponseXml, Collections.singleton(
                "update-time")));

        ArgumentCaptor<SyncChangeOfferLogbrokerEvent> captor =
                ArgumentCaptor.forClass(SyncChangeOfferLogbrokerEvent.class);
        Mockito.verify(logbrokerService).publishEvent(captor.capture());
        SyncChangeOfferLogbrokerEvent genericEvent = captor.getValue();
        Assertions.assertNotNull(genericEvent);
        List<DataCampOffer.Offer> writtenToLogbrokerOffers =
                genericEvent.getPayload()
                        .stream()
                        .map(DataCampEvent::convertToDataCampOffer)
                        .collect(Collectors.toList());
        Assertions.assertEquals(1, writtenToLogbrokerOffers.size());
        DataCampOfferIdentifiers.OfferIdentifiers expectedOffer1Id =
                DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId("ABX123")
                        .setFeedId(76)
                        .setShopId(SUPPLIER_ID)
                        .setBusinessId(1000)
                        .setExtra(DataCampOfferIdentifiers.OfferExtraIdentifiers.newBuilder()
                                .setShopSku("ABX123")
                                .build())
                        .build();

        Common.PriceExpression priceWh1 = Common.PriceExpression.newBuilder()
                .setId("RUR")
                .setPrice(1240000000L)
                .build();

        Common.PriceExpression priceWh2 = Common.PriceExpression.newBuilder()
                .setId("RUR")
                .setPrice(1640000000L)
                .build();

        Common.PriceExpression oldPriceWh2 = Common.PriceExpression.newBuilder()
                .setId("RUR")
                .setPrice(1800000000L)
                .build();

        DataCampOfferPrice.PriceBundle priceBundleWh1 = DataCampOfferPrice.PriceBundle.newBuilder()
                .setVat(7)
                .setEnabled(true)
                .setBinaryPrice(priceWh1).build();

        DataCampOfferPrice.PriceBundle priceBundleWh2 = DataCampOfferPrice.PriceBundle.newBuilder()
                .setVat(7)
                .setEnabled(true)
                .setBinaryPrice(priceWh2)
                .setBinaryOldprice(oldPriceWh2).build();

        DataCampOfferIdentifiers.OfferIdentifiers actualOfferId = writtenToLogbrokerOffers.get(0).getIdentifiers();
        DataCampOfferPrice.OfferPrice actualPrice = writtenToLogbrokerOffers.get(0).getPrice();
        Assertions.assertEquals(expectedOffer1Id, actualOfferId);
        Assertions.assertFalse(actualOfferId.hasWarehouseId());

        Assertions.assertEquals(2, actualPrice.getPriceByWarehouseCount());
        Assertions.assertFalse(actualPrice.hasBasic());

        Map<Integer, DataCampOfferPrice.PriceBundle> actualPriceByWarehouseMap = actualPrice.getPriceByWarehouseMap();
        Assertions.assertNotNull(actualPriceByWarehouseMap.get(145));
        DataCampOfferPrice.PriceBundle actualBundle1 = actualPriceByWarehouseMap.get(145);
        Assertions.assertFalse(actualBundle1.hasVat());
        Assertions.assertEquals(priceBundleWh1.getBinaryPrice(), actualBundle1.getBinaryPrice());

        Assertions.assertNotNull(actualPriceByWarehouseMap.get(147));
        DataCampOfferPrice.PriceBundle actualBundle2 = actualPriceByWarehouseMap.get(147);
        Assertions.assertFalse(actualBundle2.hasVat());
        Assertions.assertEquals(priceBundleWh2.getBinaryPrice(), actualBundle2.getBinaryPrice());
        Assertions.assertEquals(priceBundleWh2.getBinaryOldprice(), actualBundle2.getBinaryOldprice());
    }

    @Test
    void testFailedRequestXml() {
        String url = String.format("%s/v2/campaigns/%d/offers/stored/updates", urlBasePrefix, CAMPAIGN_ID);
        //language=xml
        String requestXml = ""
                + "<offer-update>"
                + "    <offers>"
                + "        <offer shop-sku=\"ABX123\">"
                + "            <pricing>"
                + "                <price value=\"123\" />"
                + "                <oldprice auto=\"true\" />"
                + "                <vat>VAT_20</vat>"
                + "            </pricing>"
                + "        </offer>"
                + "    </offers>"
                + "</offer-update>";
        //language=xml
        String expectedResponseXml = ""
                + "<response>"
                + "    <status>ERROR</status>"
                + "    <errors>"
                + "        <error code=\"BAD_OFFERS\" message=\"Recevied some invalid offers\">"
                + "            <statistics requested-offers=\"1\""
                + "                        error-offers=\"1\""
                + "                        warning-offers=\"0\""
                + "                        processed-offers=\"0\"/>"
                + "            <offers>"
                + "                <offer result=\"ERROR\" shop-sku=\"ABX123\">"
                + "                    <messages>"
                + "                        <message code=\"MISSING_CURRENCY_ID\" level=\"ERROR\">"
                + "                            <description>Missing currencyId</description>"
                + "                        </message>"
                + "                    </messages>"
                + "                </offer>"
                + "            </offers>"
                + "        </error>"
                + "    </errors>"
                + "</response>";
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(url, HttpMethod.POST, Format.XML, requestXml, String.class, 123L)
        );
        Assertions.assertEquals(400, exception.getStatusCode().value());
        MatcherAssert.assertThat(exception.getResponseBodyAsString(), MbiMatchers.xmlEquals(expectedResponseXml));
        Mockito.verifyNoMoreInteractions(logbrokerService);
    }

    @Test
    void testFailedRequestJson() {
        String url = String.format("%s/v2/campaigns/%d/offers/stored/updates", urlBasePrefix, CAMPAIGN_ID);
        //language=json
        String requestJson = ""
                + "{"
                + "    \"offers\": ["
                + "        {"
                + "            \"shopSku\":\"ABX123\","
                + "            \"pricing\": {"
                + "                \"vat\":\"VAT_20\","
                + "                \"price\": { \"value\": 123 },"
                + "                \"oldprice\": { \"auto\": true }"
                + "            }"
                + "        }"
                + "    ]"
                + "}";
        //language=json
        String expectedResponseJson = ""
                + "{"
                + "    \"status\":\"ERROR\","
                + "    \"errors\":["
                + "        {"
                + "            \"code\":\"BAD_OFFERS\","
                + "            \"message\":\"Recevied some invalid offers\","
                + "            \"statistics\":{"
                + "                \"requestedOffers\":1,"
                + "                \"errorOffers\":1,"
                + "                \"warningOffers\":0,"
                + "                \"processedOffers\":0"
                + "            },"
                + "            \"offers\":["
                + "                {"
                + "                    \"shopSku\":\"ABX123\","
                + "                    \"result\":\"ERROR\","
                + "                    \"messages\":["
                + "                        {"
                + "                            \"code\":\"MISSING_CURRENCY_ID\","
                + "                            \"level\":\"ERROR\","
                + "                            \"description\":\"Missing currencyId\""
                + "                        }"
                + "                    ]"
                + "                }"
                + "            ]"
                + "        }"
                + "    ]"
                + "}";
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(url, HttpMethod.POST, Format.XML, requestJson, String.class, 123L)
        );
        Assertions.assertEquals(400, exception.getStatusCode().value());
        MatcherAssert.assertThat(exception.getResponseBodyAsString(), MbiMatchers.jsonEquals(expectedResponseJson));
        Mockito.verifyNoMoreInteractions(logbrokerService);
    }

    @Test
    void testSuccessfulEmptyRequest() {
        String url = String.format("%s/v2/campaigns/%d/offers/stored/updates", urlBasePrefix, CAMPAIGN_ID);
        //language=xml
        String requestXml = ""
                + "<offer-update><offers/></offer-update>";
        //language=xml
        String expectedResponseXml = ""
                + "<response>"
                + "    <status>OK</status>"
                + "    <result has-ignored-errors=\"false\" update-time=\"2019-08-01T01:26:41.766+10:00\">"
                + "        <statistics requested-offers=\"0\""
                + "                    error-offers=\"0\""
                + "                    warning-offers=\"0\""
                + "                    processed-offers=\"0\"/>"
                + "        <offers/>"
                + "    </result>"
                + "</response>";
        String responseXml =
                FunctionalTestHelper.makeRequest(url, HttpMethod.POST, Format.XML, requestXml, String.class, 123L).getBody();

        MatcherAssert.assertThat(responseXml, MbiMatchers.xmlEquals(expectedResponseXml, Collections.singleton(
                "update-time")));
        Mockito.verifyZeroInteractions(logbrokerService);
    }

    @Test
    void testSomeRequestsFailed() {
        String url = String.format("%s/v2/campaigns/%d/offers/stored/updates", urlBasePrefix, CAMPAIGN_ID);
        //language=xml
        String requestXml = ""
                + "<offer-update>"
                + "    <offers>"
                + "        <offer shop-sku=\"ABX123\">"
                + "            <pricing currency-id=\"RUR\">"
                + "                <price value=\"123\" />"
                + "                <oldprice auto=\"true\" />"
                + "                <vat>VAT_20</vat>"
                + "            </pricing>"
                + "        </offer>"
                + "        <offer shop-sku=\"ABX124\">"
                + "            <pricing>"
                + "                <price value=\"125\" />"
                + "                <oldprice auto=\"true\" />"
                + "                <vat>VAT_20</vat>"
                + "            </pricing>"
                + "        </offer>"
                + "    </offers>"
                + "</offer-update>";
        //language=xml
        String expectedResponseXml = ""
                + "<response>"
                + "    <status>ERROR</status>"
                + "    <errors>"
                + "        <error code=\"BAD_OFFERS\" message=\"Recevied some invalid offers\">"
                + "            <statistics requested-offers=\"2\""
                + "                        error-offers=\"1\""
                + "                        warning-offers=\"0\""
                + "                        processed-offers=\"0\"/>"
                + "            <offers>"
                + "                <offer result=\"ERROR\" shop-sku=\"ABX124\">"
                + "                    <messages>"
                + "                        <message code=\"MISSING_CURRENCY_ID\" level=\"ERROR\">"
                + "                            <description>Missing currencyId</description>"
                + "                        </message>"
                + "                    </messages>"
                + "                </offer>"
                + "            </offers>"
                + "        </error>"
                + "    </errors>"
                + "</response>";
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(url, HttpMethod.POST, Format.XML, requestXml, String.class, 123L)
        );
        Assertions.assertEquals(400, exception.getStatusCode().value());
        MatcherAssert.assertThat(exception.getResponseBodyAsString(), MbiMatchers.xmlEquals(expectedResponseXml));
        Mockito.verifyZeroInteractions(logbrokerService);
    }

    @Test
    void testIgnoredErrors() {
        String url = String.format("%s/v2/campaigns/%d/offers/stored/updates", urlBasePrefix, CAMPAIGN_ID);
        //language=xml
        String requestXml = ""
                + "<offer-update ignore-errors=\"true\">"
                + "    <offers>"
                + "        <offer shop-sku=\"ABX123\">"
                + "            <pricing currency-id=\"RUR\">"
                + "                <price value=\"123\" />"
                + "                <oldprice auto=\"true\" />"
                + "                <vat>VAT_20</vat>"
                + "            </pricing>"
                + "        </offer>"
                + "        <offer shop-sku=\"ABX124\">"
                + "            <pricing>"
                + "                <price value=\"125\" />"
                + "                <oldprice auto=\"true\" />"
                + "                <vat>VAT_20</vat>"
                + "            </pricing>"
                + "        </offer>"
                + "    </offers>"
                + "</offer-update>";
        //language=xml
        String expectedResponseXml = ""
                + "<response>"
                + "    <status>OK</status>"
                + "    <result has-ignored-errors=\"true\" update-time=\"2019-08-01T01:26:41.766+10:00\">"
                + "            <statistics requested-offers=\"2\""
                + "                        error-offers=\"1\""
                + "                        warning-offers=\"0\""
                + "                        processed-offers=\"1\"/>"
                + "            <offers>"
                + "                <offer result=\"ERROR\" shop-sku=\"ABX124\">"
                + "                    <messages>"
                + "                        <message code=\"MISSING_CURRENCY_ID\" level=\"ERROR\">"
                + "                            <description>Missing currencyId</description>"
                + "                        </message>"
                + "                    </messages>"
                + "                </offer>"
                + "            </offers>"
                + "    </result>"
                + "</response>";
        String responseXml =
                FunctionalTestHelper.makeRequest(url, HttpMethod.POST, Format.XML, requestXml, String.class, 123L).getBody();

        MatcherAssert.assertThat(responseXml, MbiMatchers.xmlEquals(expectedResponseXml, Collections.singleton(
                "update-time")));

        ArgumentCaptor<SyncChangeOfferLogbrokerEvent> captor =
                ArgumentCaptor.forClass(SyncChangeOfferLogbrokerEvent.class);
        Mockito.verify(logbrokerService).publishEvent(captor.capture());
        SyncChangeOfferLogbrokerEvent genericEvent = captor.getValue();
        Assertions.assertNotNull(genericEvent);
        List<DataCampOffer.Offer> writtenToLogbrokerOffers =
                genericEvent.getPayload()
                        .stream()
                        .map(DataCampEvent::convertToDataCampOffer)
                        .collect(Collectors.toList());
        Assertions.assertEquals(1, writtenToLogbrokerOffers.size());
        DataCampOfferIdentifiers.OfferIdentifiers expectedOffer1Id =
                DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setFeedId(76)
                        .setOfferId("ABX123")
                        .setShopId(SUPPLIER_ID)
                        .setBusinessId(1000)
                        .setExtra(DataCampOfferIdentifiers.OfferExtraIdentifiers.newBuilder()
                                .setShopSku("ABX123")
                                .build())
                        .build();
        Assertions.assertEquals(expectedOffer1Id, writtenToLogbrokerOffers.get(0).getIdentifiers());
    }

    @Test
    void testIgnoreErrorsWithAllErroneousOffers() {
        String url = String.format("%s/v2/campaigns/%d/offers/stored/updates", urlBasePrefix, CAMPAIGN_ID);
        //language=xml
        String requestXml = ""
                + "<offer-update ignore-errors=\"true\">"
                + "    <offers>"
                + "        <offer shop-sku=\"ABX123\">"
                + "            <pricing>"
                + "                <price value=\"123\" />"
                + "                <oldprice auto=\"true\" />"
                + "                <vat>VAT_20</vat>"
                + "            </pricing>"
                + "        </offer>"
                + "        <offer shop-sku=\"ABX124\">"
                + "            <pricing>"
                + "                <price value=\"125\" />"
                + "                <oldprice auto=\"true\" />"
                + "                <vat>VAT_20</vat>"
                + "            </pricing>"
                + "        </offer>"
                + "    </offers>"
                + "</offer-update>";
        //language=xml
        String expectedResponseXml = ""
                + "<response>"
                + "    <status>ERROR</status>"
                + "    <errors>"
                + "        <error code=\"BAD_OFFERS\" message=\"Recevied some invalid offers\">"
                + "            <statistics requested-offers=\"2\""
                + "                        error-offers=\"2\""
                + "                        warning-offers=\"0\""
                + "                        processed-offers=\"0\"/>"
                + "            <offers>"
                + "                <offer result=\"ERROR\" shop-sku=\"ABX123\">"
                + "                    <messages>"
                + "                        <message code=\"MISSING_CURRENCY_ID\" level=\"ERROR\">"
                + "                            <description>Missing currencyId</description>"
                + "                        </message>"
                + "                    </messages>"
                + "                </offer>"
                + "                <offer result=\"ERROR\" shop-sku=\"ABX124\">"
                + "                    <messages>"
                + "                        <message code=\"MISSING_CURRENCY_ID\" level=\"ERROR\">"
                + "                            <description>Missing currencyId</description>"
                + "                        </message>"
                + "                    </messages>"
                + "                </offer>"
                + "            </offers>"
                + "        </error>"
                + "    </errors>"
                + "</response>";
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(url, HttpMethod.POST, Format.XML, requestXml, String.class, 123L)
        );
        Assertions.assertEquals(400, exception.getStatusCode().value());
        MatcherAssert.assertThat(exception.getResponseBodyAsString(), MbiMatchers.xmlEquals(expectedResponseXml));
        Mockito.verifyZeroInteractions(logbrokerService);
    }

    private void assertPurchasePrice(DataCampOfferPrice.OfferPrice actual, String id, long value) {
        Common.PriceExpression binaryPrice = actual.getPurchasePrice().getBinaryPrice();
        Assertions.assertEquals(id, binaryPrice.getId());
        Assertions.assertEquals(value, binaryPrice.getPrice());
    }

    private String getExpectedResponseJson() {
        //language=json
        return ""
                + "{"
                + "    \"status\":\"OK\","
                + "    \"result\":{"
                + "        \"hasIgnoredErrors\":false,"
                + "        \"updateTime\":\"${mbiMatchers.ignore}\","
                + "        \"statistics\":{"
                + "            \"requestedOffers\":1,"
                + "            \"errorOffers\":0,"
                + "            \"warningOffers\":1,"
                + "            \"processedOffers\":1"
                + "        },"
                + "        \"offers\":["
                + "            {"
                + "                \"shopSku\":\"ABX123\","
                + "                \"result\":\"WARNING\","
                + "                \"messages\":["
                + "                    {"
                + "                        \"code\":\"FRACTIONAL_PRICE\","
                + "                        \"level\":\"WARNING\","
                + "                        \"description\":\"Price is fractional, rounded value will be used\""
                + "                    }"
                + "                ]"
                + "            }"
                + "        ]"
                + "    }"
                + "}";
    }
}
