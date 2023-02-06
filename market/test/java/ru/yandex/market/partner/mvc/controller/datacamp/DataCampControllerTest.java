package ru.yandex.market.partner.mvc.controller.datacamp;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferStatus;
import Market.DataCamp.DataCampResolution;
import Market.DataCamp.DataCampValidationResult;
import Market.DataCamp.SyncAPI.SyncGetOffer;
import Market.DataCamp.SyncAPI.SyncSearch;
import org.hamcrest.CoreMatchers;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.parser.LiteInputStreamParser;
import ru.yandex.market.common.report.AsyncMarketReportService;
import ru.yandex.market.common.report.model.MarketSearchRequest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.campaign.model.CampaignInfo;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.currency.Currency;
import ru.yandex.market.core.datacamp.DataCampClientStub;
import ru.yandex.market.core.tax.model.VatRate;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.partner.mvc.controller.datacamp.dto.PriceDTO;
import ru.yandex.market.partner.mvc.controller.datacamp.dto.PriceUpdate;
import ru.yandex.market.partner.mvc.controller.datacamp.dto.PricesUpdateDTO;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.partner.mvc.controller.datacamp.dto.PublishingStatusDTO.Status;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 * <p>
 * Link для забора имен моделей и параметров из репорта балком:
 * http://warehouse-report.vs.market.yandex.net:17051/
 * yandsearch?place=modelinfo&hyperid=153254413&hyperid=175941311&rids=213&show-models-specs=full
 * @see DataCampController
 */
class DataCampControllerTest extends FunctionalTest {

    private static final Long BUSINESS_ID = 2000L;

    private static final String TEST_OFFER_URL = "/v1/campaigns/10774/offer";
    private static final String TEST_OFFERS_URL = "/v1/campaigns/10774/offers";

    private static final String SUPPLIER_TEST_OFFER_URL = "/v1/campaigns/10999/offer";

    @Autowired
    private DataCampClient dataCampShopClient;

    @Autowired
    @Qualifier(value = "marketReportService")
    private AsyncMarketReportService marketReportService;

    @Autowired
    private CampaignService campaignService;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(CALLS_REAL_METHODS)
                .when(marketReportService).executeSearchAndParse(any(MarketSearchRequest.class), any());
        when(marketReportService.async(any(), any())).then(this::onAsync);

        when(campaignService.getCampaignByDatasource(anyLong())).thenReturn(new CampaignInfo(0, 0, 0, 0,
                CampaignType.SHOP));

        ((DataCampClientStub) dataCampShopClient).reset();
    }

    @DisplayName("Ручка поиска оферов в оферном хранилище")
    @DbUnitDataSet(before = "DataCampControllerTest.search.csv")
    @Test
    void search() {
        ResponseEntity<String> response = FunctionalTestHelper.post(baseUrl + TEST_OFFERS_URL + "/search?original=1");
        verify(dataCampShopClient, times(2)).searchOffers(eq(774L), eq(BUSINESS_ID), eq(true),
                any(SyncSearch.SearchRequest.class));
        JsonTestUtil.assertEquals(response, getClass(), "DataCampControllerTest.search.response.json");
    }

    @DisplayName("Ручка поиска оферов в оферном хранилище с пейджингом и лимитом выдачи")
    @DbUnitDataSet(before = "DataCampControllerTest.search.csv")
    @Test
    void searchWithPagination() {
        ResponseEntity<String> response = FunctionalTestHelper.post(baseUrl + TEST_OFFERS_URL + "/search?original=0" +
                "&pageSize=2");
        verify(dataCampShopClient, times(2)).searchOffers(eq(774L), eq(BUSINESS_ID), eq(false),
                any(SyncSearch.SearchRequest.class));
        JsonTestUtil.assertEquals(response, getClass(), "DataCampControllerTest.search.paging.response.json");
    }

    @DisplayName("Ручка получения единичного офера из оферного хранилища")
    @DbUnitDataSet(before = "DataCampControllerTest.search.csv")
    @Test
    void getOffer() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + TEST_OFFER_URL + "?offerId=push" +
                ".2&_user_id=1");
        JsonTestUtil.assertEquals(response, getClass(), "DataCampControllerTest.getoffer.response.json");
    }

    @DisplayName("Получить единичный офер из синего хранилища, запросив его с минимальным складом")
    @DbUnitDataSet(before = "DataCampControllerTest.search.csv")
    @Test
    void getSupplierOffer() {
        asSupplier();
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + SUPPLIER_TEST_OFFER_URL + "?offerId=push" +
                ".2&_user_id=1");
        JsonTestUtil.assertEquals(response, getClass(), "DataCampControllerTest.getoffer.response.json");
        assertEquals(((DataCampClientStub) dataCampShopClient).getRequestParams().get(0), "getOffer: 999, push.2," +
                " 145");
    }

    @DisplayName("Выкидывание 404, если офер не найден")
    @DbUnitDataSet(before = "DataCampControllerTest.search.csv")
    @Test
    void offerNotExist() {
        HttpClientErrorException httpException =
                Assertions.assertThrows(HttpClientErrorException.class,
                        () -> FunctionalTestHelper.get(baseUrl + TEST_OFFER_URL + "?offerId=999b")
                );
        Assertions.assertEquals(httpException.getStatusCode(), HttpStatus.NOT_FOUND);
    }

    @DisplayName("Изменения статуса скрытия офера")
    @ParameterizedTest
    @ValueSource(strings = {
            TEST_OFFER_URL + "/disabled?offerId=1",
            TEST_OFFER_URL + "/disabled?offerId=z.,\\/()[]-=ггг"
    })
    void changeOfferStatus(String url) {
        ChangeOfferStatusRequest requestBody = new ChangeOfferStatusRequest(true);
        ResponseEntity<String> response = FunctionalTestHelper.put(baseUrl + url, requestBody);
        JsonTestUtil.assertEquals(response, "{\"publishingStatus\": { \"summary\": \"hidden\" }}");
    }

    @DisplayName("Изменение статуса скрытия синего офера, размноженного по складам")
    @DbUnitDataSet(before = "DataCampControllerTest.search.csv")
    @ParameterizedTest
    @ValueSource(strings = {"1", "z.,\\/()[]-=ггг"})
    void changeSupplierOfferStatus(String offerId) {
        asSupplier();
        ChangeOfferStatusRequest requestBody = new ChangeOfferStatusRequest(true);
        ResponseEntity<String> response = FunctionalTestHelper.put(baseUrl + SUPPLIER_TEST_OFFER_URL + ("/disabled" +
                "?offerId=" + offerId), requestBody);
        JsonTestUtil.assertEquals(response, "{\"publishingStatus\": { \"summary\": \"hidden\" }}");
        List<String> requestParams = ((DataCampClientStub) dataCampShopClient).getRequestParams();
        assertEquals(requestParams.size(), 1);
        assertTrue(requestParams.stream().anyMatch(val -> val.contains("changeStatus: 999, " + offerId)));
    }


    @DisplayName("Проверка, что повторная передача флага с тем же значением, не изменяет статус")
    @ParameterizedTest
    @ValueSource(strings = {
            TEST_OFFER_URL + "/disabled?offerId=1",
            TEST_OFFER_URL + "/disabled?offerId=z.,\\/()[]-=ггг"
    })
    void switchOfferStatus(String path) {
        ChangeOfferStatusRequest requestBody;
        ResponseEntity<String> response;

        requestBody = new ChangeOfferStatusRequest(true);
        String queryUrl = baseUrl + path;
        response = FunctionalTestHelper.put(queryUrl, requestBody);
        JsonTestUtil.assertEquals(response, "{\"publishingStatus\": { \"summary\": \"hidden\" }}");

        requestBody = new ChangeOfferStatusRequest(true);
        response = FunctionalTestHelper.put(queryUrl, requestBody);
        JsonTestUtil.assertEquals(response, "{\"publishingStatus\": { \"summary\": \"hidden\" }}");

        requestBody = new ChangeOfferStatusRequest(false);
        response = FunctionalTestHelper.put(queryUrl, requestBody);
        JsonTestUtil.assertEquals(response, "{\"publishingStatus\": { \"summary\": \"available\" }}");

        requestBody = new ChangeOfferStatusRequest(false);
        response = FunctionalTestHelper.put(queryUrl, requestBody);
        JsonTestUtil.assertEquals(response, "{\"publishingStatus\": { \"summary\": \"available\" }}");

        requestBody = new ChangeOfferStatusRequest(true);
        response = FunctionalTestHelper.put(queryUrl, requestBody);
        JsonTestUtil.assertEquals(response, "{\"publishingStatus\": { \"summary\": \"hidden\" }}");
    }

    @DisplayName("Изменения цены офера")
    @ParameterizedTest
    @ValueSource(strings = {
            TEST_OFFER_URL + "/price?offerId=1",
            TEST_OFFER_URL + "/price?offerId=z.,\\/()[]-=ггг"
    })
    void changeOfferPrice(String path) {
        PriceDTO request = new PriceDTO(new BigDecimal(2025), Currency.RUR, VatRate.VAT_20,
                new PriceDTO.OldPriceDTO(new BigDecimal(2250), null), null, null);

        ResponseEntity<String> response = FunctionalTestHelper.put(baseUrl + path, request);
        String responseStr =
                "{" +
                        "  \"prices\": {" +
                        "    \"value\": 2025.0000000," +
                        "    \"currency\": \"RUR\"," +
                        "    \"vat\": 7," +
                        "    \"oldPrice\": {" +
                        "      \"oldMin\": 2250.0000000," +
                        "      \"percent\": 0" +
                        "    }," +
                        "\"updateSource\":\"PUSH_PARTNER_OFFICE\"" +
                        "  }" +
                        "}";
        assertJson(responseStr, response);
    }

    @DisplayName("Изменение цены офера поставщика")
    @DbUnitDataSet(before = "DataCampControllerTest.search.csv")
    @ParameterizedTest
    @ValueSource(strings = {"1", "z.,\\/()[]-=ггг"})
    void changeSupplierOfferPrice(String offerId) {
        asSupplier();
        PriceDTO request = new PriceDTO(new BigDecimal(2025), Currency.RUR, VatRate.VAT_20,
                new PriceDTO.OldPriceDTO(new BigDecimal(2250), null), null, null);

        ResponseEntity<String> response =
                FunctionalTestHelper.put(baseUrl + SUPPLIER_TEST_OFFER_URL + ("/price?offerId=" + offerId), request);
        String responseStr =
                "{" +
                        "  \"prices\": {" +
                        "    \"value\": 2025.0000000," +
                        "    \"currency\": \"RUR\"," +
                        "    \"vat\": 7," +
                        "    \"oldPrice\": {" +
                        "      \"oldMin\": 2250.0000000," +
                        "      \"percent\": 0" +
                        "    }," +
                        "\"updateSource\":\"PUSH_PARTNER_OFFICE\"" +
                        "  }" +
                        "}";
        assertJson(responseStr, response);
        List<String> requestParams = ((DataCampClientStub) dataCampShopClient).getRequestParams();
        assertEquals(requestParams.size(), 1);
        assertTrue(requestParams.stream()
                .anyMatch(val -> val.contains("changePrice: 999, " + offerId)));
    }

    @DisplayName("Изменение цен офера поставщика")
    @DbUnitDataSet(before = "DataCampControllerTest.search.csv")
    @Test
    void changeSupplierOfferPrices() {
        asSupplier();
        PricesUpdateDTO request = new PricesUpdateDTO(Arrays.asList(new PriceUpdate("1",
                        new PriceDTO(new BigDecimal(2025), Currency.RUR, VatRate.VAT_20,
                                new PriceDTO.OldPriceDTO(new BigDecimal(2250), null), null, null)),
                new PriceUpdate("z.,\\/()[]-=ггг",
                        new PriceDTO(new BigDecimal(2027), Currency.RUR, VatRate.VAT_20,
                                new PriceDTO.OldPriceDTO(new BigDecimal(2252), null), null, null))));

        ResponseEntity<String> response =
                FunctionalTestHelper.put(baseUrl + SUPPLIER_TEST_OFFER_URL + ("/prices"), request);
        String responseStr =
                "[\n"
                        + "  {\n"
                        + "    \"id\": \"1\",\n"
                        + "    \"prices\": {\n"
                        + "      \"value\": 2025.0000000,\n"
                        + "      \"currency\": \"RUR\",\n"
                        + "      \"vat\": 7,\n"
                        + "      \"oldPrice\": {\n"
                        + "        \"oldMin\": 2250.0000000,\n"
                        + "        \"percent\": 0\n"
                        + "      }\n"
                        + "    }\n"
                        + "  },\n"
                        + "  {\n"
                        + "    \"id\": \"z.,\\\\/()[]-=ггг\",\n"
                        + "    \"prices\": {\n"
                        + "      \"value\": 2027.0000000,\n"
                        + "      \"currency\": \"RUR\",\n"
                        + "      \"vat\": 7,\n"
                        + "      \"oldPrice\": {\n"
                        + "        \"oldMin\": 2252.0000000,\n"
                        + "        \"percent\": 0\n"
                        + "      }\n"
                        + "    }\n"
                        + "  }\n"
                        + "]";
        JSONAssert.assertEquals(responseStr,
                new JSONObject(response.getBody()).getJSONArray("result").toString(),
                JSONCompareMode.LENIENT);
        List<String> requestParams = ((DataCampClientStub) dataCampShopClient).getRequestParams();
        assertEquals(requestParams.size(), 2);
        assertTrue(requestParams.stream().anyMatch(val -> val.contains("changePrice: 999, 1")));
        assertTrue(requestParams.stream().anyMatch(val -> val.contains("changePrice: 999, z.,\\/()[]-=ггг")));
    }


    @DisplayName("Изменение цен офера, не все оферы вернули mboc")
    @DbUnitDataSet(before = "DataCampControllerTest.search.csv")
    @Test
    void changeSupplierOfferPricesNoAllUpdated() {
        asSupplier();
        PricesUpdateDTO request = new PricesUpdateDTO(Arrays.asList(new PriceUpdate("1",
                        new PriceDTO(new BigDecimal(2025), Currency.RUR, VatRate.VAT_20,
                                new PriceDTO.OldPriceDTO(new BigDecimal(2250), null), null, null)),
                new PriceUpdate("z.,\\/()[]-=ггг",
                        new PriceDTO(new BigDecimal(2027), Currency.RUR, VatRate.VAT_20,
                                new PriceDTO.OldPriceDTO(new BigDecimal(2252), null), null, null)),
                new PriceUpdate("404",
                        new PriceDTO(new BigDecimal(2025), Currency.RUR, VatRate.VAT_20,
                                new PriceDTO.OldPriceDTO(new BigDecimal(2250), null), null, null))));
        Assertions.assertThrows(HttpClientErrorException.NotFound.class,
                () -> FunctionalTestHelper.put(baseUrl + SUPPLIER_TEST_OFFER_URL + ("/prices"), request));
    }


    @DisplayName("Изменение цены без необязательных параметров")
    @ParameterizedTest
    @ValueSource(strings = {
            TEST_OFFER_URL + "/price?offerId=1",
            TEST_OFFER_URL + "/price?offerId=z.,\\/()[]-=ггг"
    })
    void changeOfferPriceEmptyFields(String path) {
        PriceDTO request = new PriceDTO(BigDecimal.ZERO, null, null, null, null, null);

        String url = baseUrl + path;
        ResponseEntity<String> response = FunctionalTestHelper.put(url, request);

        String responseStr =
                "{" +
                        "  \"prices\": {" +
                        "    \"value\": 0," +
                        "    \"currency\": \"RUR\"," +
                        "\"updateSource\": \"PUSH_PARTNER_OFFICE\"" +
                        "  }" +
                        "}";
        assertJson(responseStr, response);

        HttpClientErrorException httpException =
                Assertions.assertThrows(HttpClientErrorException.class,
                        () -> FunctionalTestHelper.put(url,
                                new PriceDTO(null, null, null, null, null, null))
                );
        Assertions.assertEquals(httpException.getStatusCode(), HttpStatus.BAD_REQUEST);
    }

    @DisplayName("Изменение цены без НДС возвращается с дефолтным НДС партнёра, а НДС 18% конвертится в 20%")
    @DbUnitDataSet(before = "DataCampControllerTest.search.csv")
    @Test
    void changeOfferPriceWithOldVat() {
        asSupplier();
        PriceDTO request = new PriceDTO(BigDecimal.TEN, Currency.RUR, null, null, null, null);
        String url = baseUrl + SUPPLIER_TEST_OFFER_URL + "/price?offerId=push.2";
        ResponseEntity<String> response = FunctionalTestHelper.put(url, request);
        String responseStr =
                "{" +
                        "  \"prices\": {" +
                        "    \"value\": 10.0000000," +
                        "    \"currency\": \"RUR\"," +
                        "    \"vat\": 7," +
                        "    \"updateSource\":\"PUSH_PARTNER_OFFICE\"" +
                        "  }" +
                        "}";
        assertJson(responseStr, response);

        request = new PriceDTO(BigDecimal.ONE, Currency.RUR, VatRate.VAT_18, null, null, null);
        response = FunctionalTestHelper.put(url, request);
        responseStr =
                "{" +
                        "  \"prices\": {" +
                        "    \"value\": 1.0000000," +
                        "    \"currency\": \"RUR\"," +
                        "    \"vat\": 7," +
                        "    \"updateSource\":\"PUSH_PARTNER_OFFICE\"" +
                        "  }" +
                        "}";
        assertJson(responseStr, response);

        request = new PriceDTO(BigDecimal.ONE, Currency.RUR, VatRate.VAT_18_118, null, null, null);
        response = FunctionalTestHelper.put(url, request);
        responseStr =
                "{" +
                        "  \"prices\": {" +
                        "    \"value\": 1.0000000," +
                        "    \"currency\": \"RUR\"," +
                        "    \"vat\": 8," +
                        "    \"updateSource\":\"PUSH_PARTNER_OFFICE\"" +
                        "  }" +
                        "}";
        assertJson(responseStr, response);
    }

    private void callAndCheckNotFound(String offerId, String testOfferUrl) {
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(baseUrl + testOfferUrl + "?offerId=" + offerId));
        assertEquals(exception.getStatusCode(), HttpStatus.NOT_FOUND);
    }

    @DisplayName("Выкидываем 404 Not Found если у синего поставщика не получается найти оффер в хранилище")
    @Test
    @DbUnitDataSet(before = "DataCampControllerTest.dropshipOfferCreate.csv")
    void throwExceptionIfNoOfferFound() {
        asSupplier();
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(baseUrl + "/v1/campaigns/101010/offer?offerId=push.21")
        );
        Assertions.assertEquals(exception.getStatusCode(), HttpStatus.NOT_FOUND);
        Assertions.assertTrue(
                new String(exception.getResponseBodyAsByteArray(), StandardCharsets.UTF_8)
                        .contains("Не удается найти офер, либо отсутсвуют склады (не загружен фид): push.21"));

        assertEquals("getOffer: 1010, push.21, 0",
                ((DataCampClientStub) dataCampShopClient).getRequestParams().get(0));

    }

    @DisplayName("Проверить, что удаляем скрытие мигратора и отправляем обновленный оффер без него")
    @Test
    @DbUnitDataSet(before = "DataCampControllerTest.search.csv")
    void testRemoveMigrationVerdict() {
        //given
        asSupplier();
        willAnswer(invocation -> SyncGetOffer.GetOffersResponse.newBuilder()
                .addOffers(
                        DataCampOffer.Offer.newBuilder()
                                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                        .setBusinessId(BUSINESS_ID.intValue())
                                        .setOfferId("push.2")
                                )
                                .setResolution(DataCampResolution.Resolution.newBuilder()
                                        .addBySource(DataCampResolution.Verdicts.newBuilder()
                                                .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                                        .setTimestamp(DateTimes.toTimestamp(Instant.now()))
                                                        .setSource(DataCampOfferMeta.DataSource.MARKET_MBI_MIGRATOR)
                                                        .build()
                                                )
                                                .build()
                                        )
                                        .addBySource(DataCampResolution.Verdicts.newBuilder()
                                                .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                                        .setTimestamp(DateTimes.toTimestamp(Instant.now()))
                                                        .setSource(DataCampOfferMeta.DataSource.MARKET_MBI)
                                                        .build()
                                                )
                                                .build()
                                        )
                                        .build())
                                .build())
                .build()).given(dataCampShopClient).getBusinessBasicOffer(anyLong(), anyString());
        willAnswer(invocation -> DataCampOffer.Offer.newBuilder().build())
                .given(dataCampShopClient).changeBusinessBasicOffer(anyLong(), anyString(), any());
        String url = baseUrl + SUPPLIER_TEST_OFFER_URL + "/migration-verdict?offerId=push.2";
        //when
        FunctionalTestHelper.delete(url);
        //then

        ArgumentCaptor<DataCampOffer.Offer> captor = ArgumentCaptor.forClass(DataCampOffer.Offer.class);
        then(dataCampShopClient).should()
                .changeBusinessBasicOffer(eq(BUSINESS_ID), eq("push.2"), captor.capture());

        var sentRequest = captor.getValue().toBuilder();

        // очищаю таймстемп, чтобы не мешался при сравнении
        sentRequest.getResolutionBuilder().getBySourceBuilder(0).getMetaBuilder().clearTimestamp();

        assertThat(sentRequest.build(),
                CoreMatchers.equalTo(DataCampOffer.Offer.newBuilder()
                        .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setBusinessId(BUSINESS_ID.intValue())
                                .setOfferId("push.2"))
                        .setMeta(DataCampOfferMeta.OfferMeta.newBuilder()
                                .setScope(DataCampOfferMeta.OfferScope.BASIC)
                                .build())
                        .setResolution(DataCampResolution.Resolution.newBuilder()
                                .addBySource(DataCampResolution.Verdicts.newBuilder()
                                        .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                                .setSource(DataCampOfferMeta.DataSource.MARKET_MBI_MIGRATOR))
                                        .addVerdict(DataCampResolution.Verdict.newBuilder()
                                                .addResults(DataCampValidationResult.ValidationResult.newBuilder()
                                                        .setIsBanned(false)
                                                        .setIsValid(true)
                                                )
                                        ))
                        ).build()));
    }

    private Object onAsync(InvocationOnMock invocation) throws IOException {
        LiteInputStreamParser parser = invocation.getArgument(1);
        InputStream reportInput = this.getClass().getResourceAsStream("reportModel.stub.xml");
        CompletableFuture future = CompletableFuture.completedFuture(parser.parse(reportInput));
        return future;
    }

    private void asSupplier() {
        when(campaignService.getCampaignByDatasource(anyLong())).thenReturn(new CampaignInfo(0, 0, 0, 0,
                CampaignType.SUPPLIER));
    }

    @DisplayName("Конвертация статусов из индексатора в статусы офера для фронта")
    @Test
    void checkStatusMapping() {
        DataCampOfferStatus.OfferStatus.Builder bl = DataCampOfferStatus.OfferStatus.newBuilder();
        assertEquals(
                Status.AVAILABLE,
                Status.fromOfferStatus(bl.setPublishByPartner(DataCampOfferStatus.SummaryPublicationStatus.AVAILABLE).build()));
        assertEquals(
                Status.INDEXING,
                Status.fromOfferStatus(bl.setPublishByPartner(DataCampOfferStatus.SummaryPublicationStatus.INPROGRESS).build()));
        assertEquals(
                Status.HIDDEN,
                Status.fromOfferStatus(bl.setPublishByPartner(DataCampOfferStatus.SummaryPublicationStatus.HIDDEN).build()));
        assertEquals(
                Status.AVAILABLE,
                Status.fromOfferStatus(bl.clear().build()));
    }

    private void assertJson(String expected, ResponseEntity<String> response) {
        JSONAssert.assertEquals(expected,
                new JSONObject(response.getBody()).getJSONObject("result").toString(),
                JSONCompareMode.LENIENT);
    }
}
