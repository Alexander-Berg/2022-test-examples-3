package ru.yandex.market.partner.mvc.controller.offer.mapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import com.googlecode.protobuf.format.JsonFormat;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.core.offer.mapping.TimePeriodWithUnits;
import ru.yandex.market.core.offer.warehouse.MboDeliveryParamsClient;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mbi.util.MoreMbiMatchers;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsForDelivery;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mdm.http.MasterDataProto.MasterDataInfo;
import static ru.yandex.market.mdm.http.MasterDataProto.ProviderProductMasterData;

/**
 * Функциональные тесты на {@link MappingController}.
 */
@ParametersAreNonnullByDefault
@DbUnitDataSet(before = "MappingControllerFunctionalTest.csv")
class NewMappingMappingControllerFunctionalTest extends AbstractMappingControllerFunctionalTest {

    private static final long DROPSHIP_CAMPAIGN_ID = 1047608;

    @Autowired
    private MboMappingsService patientMboMappingsService;

    @Autowired
    private MboDeliveryParamsClient mboDeliveryParamsClient;

    @Test
    @DisplayName("Заведение нового офера с пустым Shop-SKU")
    void testEmptySkuInNewMapping() {
        long marketSku = 111222333;
        String url = String.format("%s/%d/shop-skus", mappedMarketSkuUrl(CAMPAIGN_ID), marketSku);
        HttpEntity<?> request = json(/*language=JSON*/ ""
                + "{"
                + "    \"shopSku\": \"\","
                + "    \"title\": \"Test offer\","
                + "    \"categoryName\": \"my/cateogory1/subcategory2\""
                + "}");
        Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(url, request)
        );
    }

    @Test
    @DisplayName("Заведение нового офера без каких-либо полей")
    void testEmptyNewMapping() {
        long marketSku = 111222333;
        String url = String.format("%s/%d/shop-skus", mappedMarketSkuUrl(CAMPAIGN_ID), marketSku);
        HttpEntity<?> request = json(/*language=JSON*/ "{}");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(url, request)
        );
        MatcherAssert.assertThat(exception, HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST));
    }


    @Test
    @DisplayName("Заведение нового офера без Shop-SKU")
    void testMissingSkuInNewMapping() {
        long marketSku = 111222333;
        String url = String.format("%s/%d/shop-skus", mappedMarketSkuUrl(CAMPAIGN_ID), marketSku);
        HttpEntity<?> request = json(/*language=JSON*/ ""
                + "{"
                + "    \"title\": \"Test 123\","
                + "    \"categoryName\": \"my/cateogory1/subcategory2\","
                + "    \"description\": \"Test description 3457\",\n"
                + "    \"brand\": \"Apple\",\n"
                + "    \"barcodes\": [\"xxxy\"],\n"
                + "    \"urls\": [\"test.ru\"],"
                + "    \"masterData\": {\n"
                + "        \"manufacturer\": \"ОАО Ромашка\",\n"
                + "        \"manufacturerCountry\": \"Россия\"\n"
                + "    }\n"
                + "}");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(url, request)
        );
        MatcherAssert.assertThat(
                exception,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyMatches("errors", MbiMatchers.jsonEquals(/*language=JSON*/ ""
                                        + "["
                                        + "    {"
                                        + "        \"code\":\"BAD_PARAM\","
                                        + "        \"message\":\"must not be null\","
                                        + "        \"details\":{"
                                        + "            \"field\":\"shopSku\","
                                        + "            \"subcode\":\"INVALID\""
                                        + "        }"
                                        + "    }"
                                        + "]")
                                )
                        )
                )
        );
    }


    @Test
    @DisplayName("Заведение нового офера без названия")
    void testMissingTitleInNewMapping() {
        long marketSku = 111222333;
        String url = String.format("%s/%d/shop-skus", mappedMarketSkuUrl(CAMPAIGN_ID), marketSku);
        HttpEntity<?> request = json(/*language=JSON*/ ""
                + "{"
                + "    \"shopSku\": \"SS14/88\","
                + "    \"categoryName\": \"my/cateogory1/subcategory2\","
                + "    \"description\": \"Test description 3457\",\n"
                + "    \"brand\": \"Apple\",\n"
                + "    \"barcodes\": [\"xxxy\"],\n"
                + "    \"urls\": [\"test.ru\"],"
                + "    \"masterData\": {\n"
                + "        \"manufacturer\": \"ОАО Ромашка\",\n"
                + "        \"manufacturerCountry\": \"Россия\"\n"
                + "    }\n"
                + "}");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(url, request)
        );
        MatcherAssert.assertThat(
                exception,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyMatches("errors", MbiMatchers.jsonEquals(/*language=JSON*/ ""
                                        + "["
                                        + "    {"
                                        + "        \"code\":\"BAD_PARAM\","
                                        + "        \"message\":\"must not be null\","
                                        + "        \"details\":{"
                                        + "            \"field\":\"title\","
                                        + "            \"subcode\":\"INVALID\""
                                        + "        }"
                                        + "    }"
                                        + "]")
                                )
                        )
                )
        );
    }

    @Test
    @DisplayName("Заведение нового офера без указания категории")
    void testMissingCategoryNameInNewMapping() {
        long marketSku = 111222333;
        String url = String.format("%s/%d/shop-skus", mappedMarketSkuUrl(CAMPAIGN_ID), marketSku);
        HttpEntity<?> request = json(/*language=JSON*/ ""
                + "{"
                + "    \"shopSku\": \"SS14/88\","
                + "    \"title\": \"Title1\","
                + "    \"description\": \"Test description 3457\",\n"
                + "    \"brand\": \"Apple\",\n"
                + "    \"barcodes\": [\"xxxy\"],\n"
                + "    \"urls\": [\"test.ru\"],"
                + "    \"masterData\": {\n"
                + "        \"manufacturer\": \"ОАО Ромашка\",\n"
                + "        \"manufacturerCountry\": \"Россия\"\n"
                + "    }\n"
                + "}");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(url, request)
        );
        MatcherAssert.assertThat(
                exception,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyMatches("errors", MbiMatchers.jsonEquals(/*language=JSON*/ ""
                                        + "["
                                        + "    {"
                                        + "        \"code\":\"BAD_PARAM\","
                                        + "        \"message\":\"must not be null\","
                                        + "        \"details\":{"
                                        + "            \"field\":\"categoryName\","
                                        + "            \"subcode\":\"INVALID\""
                                        + "        }"
                                        + "    }"
                                        + "]")
                                )
                        )
                )
        );
    }

    @Test
    @DisplayName("Заведение нового офера с пустым штрихкодом")
    void testEmptyBarcodeInNewMapping() {
        long marketSku = 111222333;
        String url = String.format("%s/%d/shop-skus", mappedMarketSkuUrl(CAMPAIGN_ID), marketSku);
        HttpEntity<?> request = json(/*language=JSON*/ ""
                + "{"
                + "    \"shopSku\": \"SS14/88\","
                + "    \"title\": \"Title1\","
                + "    \"categoryName\": \"my/cateogory1/subcategory2\","
                + "    \"description\": \"Test description 3457\",\n"
                + "    \"brand\": \"Apple\",\n"
                + "    \"urls\": [\"test.ru\"],"
                + "    \"barcodes\": [\"kjashfa\", \"\", \"ashfjash\"],"
                + "    \"masterData\": {\n"
                + "        \"manufacturer\": \"ОАО Ромашка\",\n"
                + "        \"manufacturerCountry\": \"Россия\"\n"
                + "    }\n"
                + "}");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(url, request)
        );
        MatcherAssert.assertThat(
                exception,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyMatches("errors", MbiMatchers.jsonEquals(/*language=JSON*/ ""
                                        + "["
                                        + "    {"
                                        + "        \"code\":\"BAD_PARAM\","
                                        + "        \"message\":\"size must be between 1 and 512\","
                                        + "        \"details\":{"
                                        + "            \"field\":\"barcodes[1].barcodeAsString\","
                                        + "            \"subcode\":\"INVALID\""
                                        + "        }"
                                        + "    }"
                                        + "]")
                                )
                        )
                )
        );
    }

    @Test
    @DisplayName("Заведение нового офера с Shop-SKU состоящим из символов пробела")
    void testBlankSkuInNewMapping() {
        long marketSku = 111222333;
        String url = String.format("%s/%d/shop-skus", mappedMarketSkuUrl(CAMPAIGN_ID), marketSku);
        HttpEntity<?> request = json(/*language=JSON*/ ""
                + "{"
                + "    \"shopSku\": \"  \\t \\r\\n\","
                + "    \"title\": \"Test offer\","
                + "    \"categoryName\": \"my/cateogory1/subcategory2\","
                + "    \"description\": \"Test description 3457\",\n"
                + "    \"brand\": \"Apple\",\n"
                + "    \"barcodes\": [\"xxxy\"],\n"
                + "    \"urls\": [\"test.ru\"],"
                + "    \"masterData\": {\n"
                + "        \"manufacturer\": \"ОАО Ромашка\",\n"
                + "        \"manufacturerCountry\": \"Россия\"\n"
                + "    }\n"
                + "}");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(url, request)
        );
        MatcherAssert.assertThat(
                exception,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyMatches("errors", MbiMatchers.jsonEquals(/*language=JSON*/ ""
                                        + "["
                                        + "    {"
                                        + "        \"code\":\"BAD_PARAM\","
                                        + "        \"message\":\"Illegal character(s) in shop-sku\","
                                        + "        \"details\":{"
                                        + "            \"field\":\"shopSku\","
                                        + "            \"subcode\":\"INVALID\""
                                        + "        }"
                                        + "    }"
                                        + "]")
                                )
                        )
                )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"?", "+", "#"})
    @DisplayName("Заведение нового офера с Shop-SKU в котором встречаются запрещённые символы")
    void testIsIllegal(String c) {
        long marketSku = 111222333;
        String url = String.format("%s/%d/shop-skus", mappedMarketSkuUrl(CAMPAIGN_ID), marketSku);
        HttpEntity<?> request = json(/*language=JSON*/ ""
                + "{"
                + "    \"shopSku\": \"abc123" + c + "55\","
                + "    \"title\": \"Test offer\","
                + "    \"categoryName\": \"my/cateogory1/subcategory2\",\n"
                + "    \"description\": \"Test description 3457\",\n"
                + "    \"brand\": \"Apple\",\n"
                + "    \"barcodes\": [\"xxxy\"],\n"
                + "    \"urls\": [\"test.ru\"],\n"
                + "    \"masterData\": {\n"
                + "        \"manufacturer\": \"ОАО Ромашка\",\n"
                + "        \"manufacturerCountry\": \"Россия\"\n"
                + "    }\n"
                + "}");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(url, request)
        );
        MatcherAssert.assertThat(
                exception,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyMatches(
                                        "errors",
                                        MbiMatchers.jsonArrayEquals(/*language=JSON*/ ""
                                                + "{"
                                                + "    \"code\":\"BAD_PARAM\","
                                                + "    \"message\":\"Illegal character(s) in shop-sku\","
                                                + "    \"details\":{"
                                                + "        \"field\":\"shopSku\","
                                                + "        \"subcode\":\"INVALID\""
                                                + "    }"
                                                + "}")
                                )
                        )
                )
        );
    }

    @Test
    @DisplayName("Заведение нового офера с кривым расписанием привоза")
    void testWrongScheduleInMasterDataInNewMapping() {
        long marketSku = 111222333;
        String url = String.format("%s/%d/shop-skus", mappedMarketSkuUrl(CAMPAIGN_ID), marketSku);
        HttpEntity<?> request = json(/*language=JSON*/ "{\n"
                + "  \"shopSku\": \"SS14/88\",\n"
                + "  \"title\": \"Title1\",\n"
                + "  \"categoryName\": \"my/cateogory1/subcategory2\","
                + "  \"description\": \"Test description 3457\",\n"
                + "  \"brand\": \"Apple\",\n"
                + "  \"barcodes\": [\"xxxy\"],\n"
                + "  \"urls\":  [\"test.ru\"],\n"
                + "  \"masterData\": {\n"
                + "    \"manufacturer\": \"ОАО Ромашка\",\n"
                + "    \"manufacturerCountry\": \"Россия\",\n"
                + "    \"supplyScheduleDays\": [\"MONDAY\", \"SOMEDAY\"]\n"
                + "  }\n"
                + "}");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(url, request)
        );
        MatcherAssert.assertThat(
                exception,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyMatches("errors", MbiMatchers.jsonEquals(/*language=JSON*/ ""
                                        + "[\n"
                                        + "  {\n"
                                        + "    \"code\":\"BAD_PARAM\",\n"
                                        + "    \"message\":\"must match \\\"MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY\\\"\",\n"
                                        + "    \"details\":{\n"
                                        + "      \"field\":\"masterData.supplyScheduleDays[1].dayOfWeek\",\n"
                                        + "      \"subcode\":\"INVALID\"\n"
                                        + "    }\n"
                                        + "  }\n"
                                        + "]")
                                )
                        )
                )
        );
    }

    @Test
    @DisplayName("Заведение нового офера с отсутствующей страной производителем")
    void testMissingManufacturerCountryInMasterDataInNewMapping() {
        long marketSku = 111222333;
        String url = String.format("%s/%d/shop-skus", mappedMarketSkuUrl(CAMPAIGN_ID), marketSku);
        HttpEntity<?> request = json(/*language=JSON*/ "{\n"
                + "  \"shopSku\": \"SS14/88\",\n"
                + "  \"title\": \"Title1\",\n"
                + "  \"categoryName\": \"my/cateogory1/subcategory2\",\n"
                + "  \"description\": \"Test description 3457\",\n"
                + "  \"brand\": \"Apple\",\n"
                + "  \"barcodes\": [\"xxxy\"],\n"
                + "  \"urls\": [\"test.ru\"],\n"
                + "  \"masterData\": {\n"
                + "    \"manufacturer\": \"ОАО Ромашка\"\n"
                + "  }\n"
                + "}");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(url, request)
        );
        MatcherAssert.assertThat(
                exception,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyMatches("errors", MbiMatchers.jsonEquals(/*language=JSON*/ ""
                                        + "[\n"
                                        + "  {\n"
                                        + "    \"code\":\"BAD_PARAM\",\n"
                                        + "    \"message\":\"must not be empty\",\n"
                                        + "    \"details\":{\n"
                                        + "      \"field\":\"masterData.manufacturerCountry\",\n"
                                        + "      \"subcode\":\"INVALID\"\n"
                                        + "    }\n"
                                        + "  }\n"
                                        + "]")
                                )
                        )
                )
        );
    }

    @Test
    @DisplayName("Заведение нового офера с некорректным url")
    void badUrl() {
        long marketSku = 111222333;
        String url = String.format("%s/%d/shop-skus", mappedMarketSkuUrl(CAMPAIGN_ID), marketSku);
        HttpEntity<?> request = json(/*language=JSON*/ ""
                + "{\n"
                + "  \"shopSku\": \"vendor123.item-123\",\n"
                + "  \"title\": \"Test offer 567\",\n"
                + "  \"categoryName\": \"my/cateogory1/subcategory2\",\n"
                + "  \"description\": \"Test description 3457\",\n"
                + "  \"brand\": \"Apple\",\n"
                + "  \"vendorCode\": \"IPHO123\",\n"
                + "  \"urls\": [\n"
                + "    \"\"\n"
                + "  ],\n"
                + "  \"barcodes\": [\n"
                + "    \"xxxy\",\n"
                + "    \"sdhfs\",\n"
                + "    \"dslkfhdslk\"\n"
                + "  ],\n"
                + "  \"masterData\": {\n"
                + "    \"manufacturer\": \"ОАО Ромашка\",\n"
                + "    \"manufacturerCountry\": \"Россия\"\n"
                + "  }\n"
                + "}\n");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(url, request)
        );
        MatcherAssert.assertThat(
                exception,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyMatches("errors", MbiMatchers.jsonEquals(/*language=JSON*/ "[\n" +
                                        "  {\n" +
                                        "    \"code\":\"BAD_PARAM\",\n" +
                                        "    \"message\":\"must not be empty\",\n" +
                                        "    \"details\":{\n" +
                                        "      \"field\":\"urls[0]\",\n" +
                                        "      \"subcode\":\"INVALID\"\n" +
                                        "    }\n" +
                                        "  }\n" +
                                        "]")
                                )
                        )
                )
        );
    }

    @Test
    @DisplayName("Заведение нового офера с пустым shelfLife без маппинга")
    void testEmptyShelfLifeTimeLifeAndGuaranteePeriod() {
        HttpEntity<?> request = json(/*language=JSON*/ ""
                + "{"
                + commonOfferInformationForEmptyFields()
                + "        \"shelfLife\": {}\n"
                + "    }\n"
                + "}");

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(shopSkusUrl(CAMPAIGN_ID), request)
        );
        MatcherAssert.assertThat(
                exception,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyMatches("errors", MbiMatchers.jsonEquals(/*language=JSON*/ "[\n" +
                                        "  {\n" +
                                        "    \"code\":\"BAD_PARAM\",\n" +
                                        "    \"message\":\"must not be null\",\n" +
                                        "    \"details\":{\n" +
                                        "      \"field\":\"masterData.shelfLife.timeUnit\",\n" +
                                        "      \"subcode\":\"INVALID\"\n" +
                                        "    }\n" +
                                        "  }\n" +
                                        "]")
                                )
                        )
                )
        );
    }

    @Test
    @DisplayName("Заведение нового офера с пустым guaranteePeriod без маппинга")
    void testEmptyGuaranteePeriod() {
        HttpEntity<?> request = json(/*language=JSON*/ ""
                + "{"
                + commonOfferInformationForEmptyFields()
                + "        \"guaranteePeriod\": {}\n"
                + "    }\n"
                + "}");

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(shopSkusUrl(CAMPAIGN_ID), request)
        );
        MatcherAssert.assertThat(
                exception,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyMatches("errors", MbiMatchers.jsonEquals(/*language=JSON*/ "[\n" +
                                        "  {\n" +
                                        "    \"code\":\"BAD_PARAM\",\n" +
                                        "    \"message\":\"must not be null\",\n" +
                                        "    \"details\":{\n" +
                                        "      \"field\":\"masterData.guaranteePeriod.timeUnit\",\n" +
                                        "      \"subcode\":\"INVALID\"\n" +
                                        "    }\n" +
                                        "  }\n" +
                                        "]")
                                )
                        )
                )
        );
    }

    @Test
    @DisplayName("Заведение нового офера с пустым timeLife без маппинга")
    void testEmptyTimeLife() {
        HttpEntity<?> request = json(/*language=JSON*/ ""
                + "{"
                + commonOfferInformationForEmptyFields()
                + "        \"lifeTime\": {}\n"
                + "    }\n"
                + "}");

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(shopSkusUrl(CAMPAIGN_ID), request)
        );
        MatcherAssert.assertThat(
                exception,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyMatches("errors", MbiMatchers.jsonEquals(/*language=JSON*/ "[\n" +
                                        "  {\n" +
                                        "    \"code\":\"BAD_PARAM\",\n" +
                                        "    \"message\":\"must not be null\",\n" +
                                        "    \"details\":{\n" +
                                        "      \"field\":\"masterData.lifeTime.timeUnit\",\n" +
                                        "      \"subcode\":\"INVALID\"\n" +
                                        "    }\n" +
                                        "  }\n" +
                                        "]")
                                )
                        )
                )
        );
    }

    @Test
    @DisplayName("Успешное заведение нового офера")
    void testNewMapping() {
        long marketSku = 111222333;
        when(patientMboMappingsService.addProductInfo(any()))
                .thenReturn(MboMappings.ProviderProductInfoResponse.newBuilder()
                        .setStatus(MboMappings.ProviderProductInfoResponse.Status.OK)
                        .build());

        String url = String.format("%s/%d/shop-skus", mappedMarketSkuUrl(CAMPAIGN_ID), marketSku);
        HttpEntity<?> request = json(/*language=JSON*/ ""
                + "{"
                + "    \"shopSku\": \"vendor123.item-123\","
                + "    \"title\": \"Test offer 567\","
                + "    \"categoryName\": \"my/cateogory1/subcategory2\","
                + "    \"description\": \"Test description 3457\",\n"
                + "    \"brand\": \"Apple\",\n"
                + "    \"barcodes\": [\"xxxy\"],\n"
                + "    \"urls\": [\"test.ru\"],"
                + "    \"masterData\": {\n"
                + "        \"manufacturer\": \"ОАО Ромашка\",\n"
                + "        \"manufacturerCountry\": \"Россия\"\n"
                + "    }\n"
                + "}");
        ResponseEntity<String> processedEntity = FunctionalTestHelper.post(url, request);
        MatcherAssert.assertThat(processedEntity,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyMatches("result",
                                MbiMatchers.jsonEquals(/*language=JSON*/ ""
                                        + "{"
                                        + "    \"shopSku\": \"vendor123.item-123\","
                                        + "    \"title\": \"Test offer 567\","
                                        + "    \"categoryName\": \"my/cateogory1/subcategory2\","
                                        + "    \"description\": \"Test description 3457\",\n"
                                        + "    \"brand\": \"Apple\",\n"
                                        + "    \"barcodes\": [\"xxxy\"],\n"
                                        + "    \"urls\": [\"test.ru\"],"
                                        + "    \"masterData\": {\n"
                                        + "        \"manufacturer\": \"ОАО Ромашка\",\n"
                                        + "        \"manufacturerCountry\": \"Россия\","
                                        + "        \"supplyScheduleDays\":[]\n"
                                        + "    }\n"
                                        + "}"))));
        verify(patientMboMappingsService).addProductInfo(ArgumentMatchers.argThat(response -> {
            List<MboMappings.ProviderProductInfo> productInfoList = response.getProviderProductInfoList();
            if (productInfoList.size() != 1) {
                return false;
            } else {
                MboMappings.ProviderProductInfo productInfo = productInfoList.get(0);
                return productInfo.hasMarketSkuId()
                        && productInfo.getMarketSkuId() == 111222333
                        && productInfo.hasShopSkuId()
                        && productInfo.getShopSkuId().equals("vendor123.item-123")
                        && productInfo.hasTitle()
                        && productInfo.getTitle().equals("Test offer 567")
                        && productInfo.hasShopId()
                        && productInfo.getShopId() == 774
                        && productInfo.hasShopCategoryName()
                        && productInfo.getShopCategoryName().equals("my/cateogory1/subcategory2")
                        && productInfo.hasVendor()
                        && productInfo.getVendor().equals("Apple")
                        && productInfo.hasDescription()
                        && productInfo.getDescription().equals("Test description 3457")
                        && productInfo.getBarcodeList().equals(List.of("xxxy"))
                        && productInfo.getUrlList().equals(List.of("test.ru"))
                        && productInfo.hasMasterDataInfo()
                        && productInfo.getMasterDataInfo().hasProviderProductMasterData()
                        && productInfo.getMasterDataInfo().getProviderProductMasterData().hasManufacturer()
                        && productInfo.getMasterDataInfo().getProviderProductMasterData().getManufacturer().equals("ОАО Ромашка")
                        && productInfo.getMasterDataInfo().getProviderProductMasterData().getManufacturerCountryList().equals(List.of("Россия"));
            }
        }));
    }

    @Test
    @DisplayName("Заведение нового офера с пробрасыванием ошибок от MBOC'а в ответ ручки")
    void testNewMappingWithErrorPropagation() {
        long marketSku = 111222333;
        when(patientMboMappingsService.addProductInfo(any()))
                .thenReturn(MboMappings.ProviderProductInfoResponse.newBuilder()
                        .setStatus(MboMappings.ProviderProductInfoResponse.Status.ERROR)
                        .addResults(MboMappings.ProviderProductInfoResponse.ProductResult.newBuilder()
                                .setStatus(MboMappings.ProviderProductInfoResponse.Status.ERROR)
                                .addErrors(MboMappings.ProviderProductInfoResponse.Error.newBuilder()
                                        .setErrorKind(MboMappings.ProviderProductInfoResponse.ErrorKind.SKU_NOT_EXISTS)
                                        .setMessage("111222333 market sku doesn't exists")
                                        .build())
                                .build())
                        .build());

        String url = String.format("%s/%d/shop-skus", mappedMarketSkuUrl(CAMPAIGN_ID), marketSku);
        HttpEntity<?> request = json(/*language=JSON*/ ""
                + "{"
                + "    \"shopSku\": \"vendor123.item-123\","
                + "    \"title\": \"Test offer 567\","
                + "    \"categoryName\": \"my/cateogory1/subcategory2\","
                + "    \"description\": \"Test description 3457\",\n"
                + "    \"brand\": \"Apple\",\n"
                + "    \"barcodes\": [\"xxxy\"],\n"
                + "    \"urls\":  [\"test.ru\"],"
                + "    \"masterData\": {\n"
                + "        \"manufacturer\": \"ОАО Ромашка\",\n"
                + "        \"manufacturerCountry\": \"Россия\"\n"
                + "    }\n"
                + "}");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(url, request)
        );
        MatcherAssert.assertThat(
                exception,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyMatches("errors",
                                        MbiMatchers.jsonEquals(/*language=JSON*/ ""
                                                + "["
                                                + "    {"
                                                + "        \"code\":\"BAD_PARAM\","
                                                + "        \"message\":\"Constraint violation\","
                                                + "        \"details\":{"
                                                + "            \"subcode\":\"SKU_NOT_EXISTS\","
                                                + "            \"message\":\"111222333 market sku doesn't exists\""
                                                + "        }"
                                                + "    }"
                                                + "]")
                                )
                        )
                )
        );
    }

    @Test
    @DisplayName("Заведение нового офера со всеми поддерживаемыми полями")
    void testNewMappingWithAllTheFieldsAndMasterData() {
        long marketSku = 111222333;
        when(patientMboMappingsService.addProductInfo(any()))
                .thenReturn(MboMappings.ProviderProductInfoResponse.newBuilder()
                        .setStatus(MboMappings.ProviderProductInfoResponse.Status.OK)
                        .build());

        String url = String.format("%s/%d/shop-skus", mappedMarketSkuUrl(CAMPAIGN_ID), marketSku);
        String requestText = /*language=JSON*/ ""
                + "{\n"
                + "    \"shopSku\": \"vendor123.item-123\",\n"
                + "    \"title\": \"Test offer 567\",\n"
                + "    \"categoryName\": \"my/cateogory1/subcategory2\",\n"
                + "    \"description\": \"Test description 3457\",\n"
                + "    \"brand\": \"Apple\",\n"
                + "    \"vendorCode\": \"IPHO123\",\n"
                + "    \"barcodes\": [\"xxxy\", \"sdhfs\", \"dslkfhdslk\"],\n"
                + "    \"urls\": [\"https://beru.ru/product/100324822646\"],\n"
                + "    \"masterData\": {\n"
                + "        \"manufacturer\": \"ОАО Ромашка\",\n"
                + "        \"manufacturerCountry\": \"Россия\",\n"
                + "        \"shelfLifeDays\": 10,\n"
                + "        \"guaranteePeriodDays\": 180,\n"
                + "        \"lifeTimeDays\": 365,\n"
                + "        \"minShipment\": 100,\n"
                + "        \"deliveryDurationDays\": 3,\n"
                + "        \"quantumOfSupply\": 20,\n"
                + "        \"boxCount\": 2,\n"
                + "        \"customsCommodityCodes\": [\"HG405235\"],\n"
                + "        \"weightDimensions\": {\n"
                + "            \"height\": 70,\n"
                + "            \"width\": 20.5,\n"
                + "            \"length\": 120,\n"
                + "            \"weight\": 5.3\n"
                + "        },\n"
                + "        \"transportUnitSize\": 10,\n"
                + "        \"supplyScheduleDays\": [\"MONDAY\", \"WEDNESDAY\", \"SUNDAY\"]\n"
                + "    }\n"
                + "}";
        HttpEntity<?> request = json(requestText);
        ResponseEntity<String> processedEntity = FunctionalTestHelper.post(url, request);
        MatcherAssert.assertThat(
                processedEntity,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyMatches("result", MbiMatchers.jsonEquals(requestText))));
        verify(patientMboMappingsService).addProductInfo(ArgumentMatchers.argThat(response -> {
            List<MboMappings.ProviderProductInfo> productInfoList = response.getProviderProductInfoList();
            if (productInfoList.size() != 1) {
                return false;
            } else {
                MboMappings.ProviderProductInfo productInfo = productInfoList.get(0);
                if (!productInfo.hasMasterDataInfo()) {
                    return false;
                } else {
                    MasterDataInfo masterData = productInfo.getMasterDataInfo();
                    if (!masterData.hasProviderProductMasterData()) {
                        return false;
                    } else {
                        ProviderProductMasterData providerMasterData = masterData.getProviderProductMasterData();
                        return productInfo.hasMarketSkuId()
                                && productInfo.getMarketSkuId() == 111222333
                                && productInfo.hasShopSkuId()
                                && productInfo.getShopSkuId().equals("vendor123.item-123")
                                && productInfo.hasTitle()
                                && productInfo.getTitle().equals("Test offer 567")
                                && productInfo.hasShopId()
                                && productInfo.getShopId() == 774
                                && productInfo.hasShopCategoryName()
                                && productInfo.getShopCategoryName().equals("my/cateogory1/subcategory2")
                                && productInfo.hasVendor()
                                && productInfo.getVendor().equals("Apple")
                                && productInfo.hasVendorCode()
                                && productInfo.getVendorCode().equals("IPHO123")
                                && productInfo.getBarcodeList().equals(Arrays.asList("xxxy", "sdhfs", "dslkfhdslk"))
                                && productInfo.hasDescription()
                                && productInfo.getDescription().equals("Test description 3457")
                                && productInfo.getUrlList().equals(ImmutableList.of("https://beru.ru/product/100324822646"))
                                && providerMasterData.hasManufacturer()
                                && providerMasterData.getManufacturer().equals("ОАО Ромашка")
                                && providerMasterData.getManufacturerCountryList().equals(Collections.singletonList("Россия"))
                                && masterData.hasShelfLife()
                                && masterData.getShelfLife().equals("10")
                                && masterData.getShelfLifeWithUnits().equals(TimePeriodWithUnits.ofDays(10).getTime())
                                && masterData.hasGuaranteePeriod()
                                && masterData.getGuaranteePeriod().equals("180")
                                && masterData.getGuaranteePeriodWithUnits().equals(TimePeriodWithUnits.ofDays(180).getTime())
                                && masterData.hasLifeTime()
                                && masterData.getLifeTime().equals("365")
                                && providerMasterData.hasMinShipment()
                                && providerMasterData.getMinShipment() == 100
                                && providerMasterData.hasDeliveryTime()
                                && providerMasterData.getDeliveryTime() == 3
                                && providerMasterData.hasQuantumOfSupply()
                                && providerMasterData.getQuantumOfSupply() == 20
                                && providerMasterData.hasTransportUnitSize()
                                && providerMasterData.getTransportUnitSize() == 10
                                && providerMasterData.hasSupplySchedule()
                                && providerMasterData.hasBoxCount()
                                && providerMasterData.getBoxCount() == 2
                                && providerMasterData.getSupplySchedule().getSupplyEventCount() == 3
                                && providerMasterData.getSupplySchedule().getSupplyEvent(0).hasDayOfWeek()
                                && providerMasterData.getSupplySchedule().getSupplyEvent(0).getDayOfWeek() == 1
                                && providerMasterData.getSupplySchedule().getSupplyEvent(1).hasDayOfWeek()
                                && providerMasterData.getSupplySchedule().getSupplyEvent(1).getDayOfWeek() == 3
                                && providerMasterData.getSupplySchedule().getSupplyEvent(2).hasDayOfWeek()
                                && providerMasterData.getSupplySchedule().getSupplyEvent(2).getDayOfWeek() == 7
                                && providerMasterData.getCustomsCommodityCode().equals("HG405235")
                                && providerMasterData.getWeightDimensionsInfo().getBoxHeightUm() == 700_000L
                                && providerMasterData.getWeightDimensionsInfo().getBoxWidthUm() == 205_000L
                                && providerMasterData.getWeightDimensionsInfo().getBoxLengthUm() == 1_200_000L
                                && providerMasterData.getWeightDimensionsInfo().getWeightGrossMg() == 5_300_000L;
                    }
                }
            }
        }));
    }

    @Test
    @DisplayName("Заведение нового офера со всеми поддерживаемыми полями но без маппига")
    void testNewOfferWithoutMappingWithAllTheFields() {
        when(patientMboMappingsService.addProductInfo(any()))
                .thenReturn(MboMappings.ProviderProductInfoResponse.newBuilder()
                        .setStatus(MboMappings.ProviderProductInfoResponse.Status.OK)
                        .build());

        HttpEntity<?> request = json(/*language=JSON*/ ""
                + "{"
                + "    \"shopSku\": \"vendor123.item-123\","
                + "    \"title\": \"Test offer 567\","
                + "    \"categoryName\": \"my/cateogory1/subcategory2\","
                + "    \"description\": \"Test description 3457\","
                + "    \"brand\": \"Apple\",\n"
                + "    \"vendorCode\": \"IPHO123\",\n"
                + "    \"barcodes\": [\"xxxy\", \"sdhfs\", \"dslkfhdslk\"], \n"
                + "    \"urls\": [\"http://beru.ru/product/44444\"], \n"
                + "    \"masterData\": {\n"
                + "        \"manufacturer\": \"ОАО Ромашка\",\n"
                + "        \"manufacturerCountry\": \"Россия\",\n"
                + "        \"shelfLifeDays\": 10,\n"
                + "        \"guaranteePeriodDays\": 180,\n"
                + "        \"lifeTimeDays\": 365,\n"
                + "        \"minShipment\": 100,\n"
                + "        \"deliveryDurationDays\": 3,\n"
                + "        \"quantumOfSupply\": 20,\n"
                + "        \"boxCount\": 2,\n"
                + "        \"customsCommodityCodes\": [\"HG405235\"],\n"
                + "        \"weightDimensions\": {\n"
                + "            \"height\": 70,\n"
                + "            \"width\": 20.5,\n"
                + "            \"length\": 120,\n"
                + "            \"weight\": 5.3\n"
                + "        },\n"
                + "        \"transportUnitSize\": 10,\n"
                + "        \"supplyScheduleDays\": [\"MONDAY\", \"WEDNESDAY\", \"SUNDAY\"]\n"
                + "    }\n"
                + "}");
        ResponseEntity<String> processedEntity = FunctionalTestHelper.post(shopSkusUrl(CAMPAIGN_ID), request);
        MatcherAssert.assertThat(processedEntity, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyMatches(
                "result",
                MbiMatchers.jsonEquals(/*language=JSON*/ ""
                        + "{"
                        + "    \"shopSku\": \"vendor123.item-123\","
                        + "    \"title\": \"Test offer 567\","
                        + "    \"categoryName\": \"my/cateogory1/subcategory2\","
                        + "    \"description\": \"Test description 3457\","
                        + "    \"brand\": \"Apple\",\n"
                        + "    \"vendorCode\": \"IPHO123\",\n"
                        + "    \"barcodes\": [\"xxxy\", \"sdhfs\", \"dslkfhdslk\"],\n"
                        + "    \"urls\": [\"http://beru.ru/product/44444\"],\n"
                        + "    \"masterData\": {\n"
                        + "        \"manufacturer\": \"ОАО Ромашка\",\n"
                        + "        \"manufacturerCountry\": \"Россия\",\n"
                        + "        \"shelfLifeDays\": 10,\n"
                        + "        \"guaranteePeriodDays\": 180,\n"
                        + "        \"lifeTimeDays\": 365,\n"
                        + "        \"minShipment\": 100,\n"
                        + "        \"deliveryDurationDays\": 3,\n"
                        + "        \"quantumOfSupply\": 20,\n"
                        + "        \"boxCount\": 2,\n"
                        + "        \"customsCommodityCodes\": [\"HG405235\"],\n"
                        + "        \"weightDimensions\": {\n"
                        + "            \"height\": 70,\n"
                        + "            \"width\": 20.5,\n"
                        + "            \"length\": 120,\n"
                        + "            \"weight\": 5.3\n"
                        + "        },\n"
                        + "        \"transportUnitSize\": 10,\n"
                        + "        \"supplyScheduleDays\": [\"MONDAY\", \"WEDNESDAY\", \"SUNDAY\"]\n"
                        + "    }\n"
                        + "}")
        )));
        verify(patientMboMappingsService).addProductInfo(ArgumentMatchers.argThat(response -> {
            List<MboMappings.ProviderProductInfo> productInfoList = response.getProviderProductInfoList();
            if (productInfoList.size() != 1) {
                return false;
            } else {
                MboMappings.ProviderProductInfo productInfo = productInfoList.get(0);
                ProviderProductMasterData providerMasterData = productInfo.getMasterDataInfo().getProviderProductMasterData();
                return !productInfo.hasMarketSkuId()
                        && !productInfo.hasMarketCategoryId()
                        && !productInfo.hasMarketModelId()
                        && productInfo.hasShopSkuId()
                        && productInfo.getShopSkuId().equals("vendor123.item-123")
                        && productInfo.hasTitle()
                        && productInfo.getTitle().equals("Test offer 567")
                        && productInfo.hasShopId()
                        && productInfo.getShopId() == 774
                        && productInfo.hasShopCategoryName()
                        && productInfo.getShopCategoryName().equals("my/cateogory1/subcategory2")
                        && productInfo.hasVendor()
                        && productInfo.getVendor().equals("Apple")
                        && productInfo.hasVendorCode()
                        && productInfo.getVendorCode().equals("IPHO123")
                        && productInfo.getBarcodeList().equals(Arrays.asList("xxxy", "sdhfs", "dslkfhdslk"))
                        && productInfo.hasDescription()
                        && productInfo.getDescription().equals("Test description 3457")
                        && providerMasterData.getCustomsCommodityCode().equals("HG405235")
                        && providerMasterData.getWeightDimensionsInfo().getBoxHeightUm() == 700_000L
                        && providerMasterData.getWeightDimensionsInfo().getBoxWidthUm() == 205_000L
                        && providerMasterData.getWeightDimensionsInfo().getBoxLengthUm() == 1_200_000L
                        && providerMasterData.getWeightDimensionsInfo().getWeightGrossMg() == 5_300_000L;
            }
        }));
    }

    @Test
    void testNewMappingWithUpdateComment() {
        long marketSku = 111222333;
        when(patientMboMappingsService.addProductInfo(any()))
                .thenReturn(MboMappings.ProviderProductInfoResponse.newBuilder()
                        .setStatus(MboMappings.ProviderProductInfoResponse.Status.OK)
                        .build());

        String url = String.format("%s/%d/shop-skus", mappedMarketSkuUrl(CAMPAIGN_ID), marketSku);
        HttpEntity<?> request = json(/*language=JSON*/ ""
                + "{"
                + "    \"shopSku\": \"vendor123.item-123\","
                + "    \"title\": \"Test offer 567\","
                + "    \"categoryName\": \"my/cateogory1/subcategory2\","
                + "    \"description\": \"Test description 3457\",\n"
                + "    \"brand\": \"Apple\",\n"
                + "    \"barcodes\": [\"xxxy\"],\n"
                + "    \"urls\": [\"test.ru\"],"
                + "    \"masterData\": {\n"
                + "        \"manufacturer\": \"ОАО Ромашка\",\n"
                + "        \"manufacturerCountry\": \"Россия\","
                + "        \"supplyScheduleDays\":[]\n"
                + "    },\n"
                + "    \"updateComment\": \"Please, pretty, please!\""
                + "}");
        ResponseEntity<String> processedEntity = FunctionalTestHelper.post(url, request);
        MatcherAssert.assertThat(processedEntity,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyMatches("result",
                                MbiMatchers.jsonEquals(/*language=JSON*/ ""
                                        + "{"
                                        + "    \"shopSku\": \"vendor123.item-123\","
                                        + "    \"title\": \"Test offer 567\","
                                        + "    \"categoryName\": \"my/cateogory1/subcategory2\","
                                        + "    \"description\": \"Test description 3457\",\n"
                                        + "    \"brand\": \"Apple\",\n"
                                        + "    \"barcodes\": [\"xxxy\"],\n"
                                        + "    \"urls\": [\"test.ru\"],"
                                        + "    \"masterData\": {\n"
                                        + "        \"manufacturer\": \"ОАО Ромашка\",\n"
                                        + "        \"manufacturerCountry\": \"Россия\","
                                        + "        \"supplyScheduleDays\":[]\n"
                                        + "    }\n"
                                        + "}"))));
        verify(patientMboMappingsService).addProductInfo(ArgumentMatchers.argThat(response -> {
            List<MboMappings.ProviderProductInfo> productInfoList = response.getProviderProductInfoList();
            if (productInfoList.size() != 1) {
                return false;
            } else {
                MboMappings.ProviderProductInfo productInfo = productInfoList.get(0);
                return productInfo.hasMarketSkuId()
                        && productInfo.getMarketSkuId() == 111222333
                        && productInfo.hasShopSkuId()
                        && productInfo.getShopSkuId().equals("vendor123.item-123")
                        && productInfo.hasTitle()
                        && productInfo.getTitle().equals("Test offer 567")
                        && productInfo.hasShopId()
                        && productInfo.getShopId() == 774
                        && productInfo.hasShopCategoryName()
                        && productInfo.getShopCategoryName().equals("my/cateogory1/subcategory2")
                        && productInfo.hasDescription()
                        && productInfo.getDescription().equals("Test description 3457")
                        && productInfo.getBarcodeList().equals(List.of("xxxy"))
                        && productInfo.getUrlList().equals(List.of("test.ru"))
                        && productInfo.hasMasterDataInfo()
                        && productInfo.getMasterDataInfo().hasProviderProductMasterData()
                        && productInfo.getMasterDataInfo().getProviderProductMasterData().hasManufacturer()
                        && productInfo.getMasterDataInfo().getProviderProductMasterData().getManufacturer().equals("ОАО Ромашка")
                        && productInfo.getMasterDataInfo().getProviderProductMasterData().getManufacturerCountryList().equals(List.of("Россия"))
                        && productInfo.hasMappingChangeReasonComment()
                        && productInfo.getMappingChangeReasonComment().equals("Please, pretty, please!");
            }
        }));
    }

    @Test
    @DisplayName("Привязка существующего shopSKU к marketSKU")
    void testLinkExistingShopSKU() {
        when(patientMboMappingsService.updateMappings(any()))
                .thenReturn(MboMappings.ProviderProductInfoResponse.newBuilder()
                        .setStatus(MboMappings.ProviderProductInfoResponse.Status.OK)
                        .build());

        String url = String.format("%s/by-shop-sku/market-skus?shop_sku={shopSku}", shopSkusUrl(CAMPAIGN_ID));
        String requestText = /*language=JSON*/ ""
                + "{\n"
                + "    \"marketSku\": 111222333\n"
                + "}";
        ResponseEntity<String> processedEntity = FunctionalTestHelper.put(url, json(requestText), "vendor123.item-123");
        MatcherAssert.assertThat(
                processedEntity,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyMatches("result", MbiMatchers.jsonEquals(requestText))));
        verify(patientMboMappingsService).updateMappings(ArgumentMatchers.argThat(response -> {
            List<MboMappings.UpdateMappingsRequest.MappingUpdate> updates = response.getUpdatesList();
            if (updates.size() != 1) {
                return false;
            } else {
                MboMappings.UpdateMappingsRequest.MappingUpdate update = updates.get(0);
                return update.hasMarketSkuId()
                        && update.getMarketSkuId() == 111222333
                        && update.hasShopSku()
                        && update.getShopSku().equals("vendor123.item-123")
                        && update.hasSupplierId()
                        && update.getSupplierId() == 774
                        && !update.hasVerifyLastVersion();
            }
        }));
    }

    @Test
    @DisplayName("Обновление признака доступности shopSKU")
    void testUpdateAvailability() {
        when(patientMboMappingsService.updateAvailability(any()))
                .thenReturn(MboMappings.UpdateAvailabilityResponse.newBuilder()
                        .setStatus(MboMappings.UpdateAvailabilityResponse.Status.OK)
                        .build());

        String url = String.format("%s/by-shop-sku/availability?shop_sku={shopSku}", shopSkusUrl(CAMPAIGN_ID));
        String requestText = /*language=JSON*/ ""
                + "{\n"
                + "    \"availability\": \"ACTIVE\""
                + "}";
        ResponseEntity<String> processedEntity = FunctionalTestHelper.put(url, json(requestText), "vendor123.item-123");
        MatcherAssert.assertThat(
                processedEntity,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyMatches("result", MbiMatchers.jsonEquals(requestText))));
        verify(patientMboMappingsService).updateAvailability(ArgumentMatchers.argThat(response -> {
            SupplierOffer.Availability availability = response.getAvailability();
            List<MboMappings.UpdateAvailabilityRequest.Locator> mappingsList = response.getMappingsList();
            if (mappingsList.size() != 1) {
                return false;
            } else {
                MboMappings.UpdateAvailabilityRequest.Locator locator = mappingsList.get(0);
                return availability == SupplierOffer.Availability.ACTIVE
                        && locator.hasSupplierId()
                        && locator.getSupplierId() == 774
                        && locator.hasShopSku()
                        && locator.getShopSku().equals("vendor123.item-123");
            }
        }));
    }

    @Test
    @DisplayName("Склад доступен для карточки товара")
    void warehouseAvailabilityAllowed() throws IOException {
        mockMboDeliveryParamsClient("searchFulfillmentSskuParams.allowed.data.json");
        ResponseEntity<String> response = getWarehousesResponse(CAMPAIGN_ID, "PI-AUTOTEST-SKU1", "145,171");
        JsonTestUtil.assertEquals(
                response,
                "{\n" +
                        "    \"cargoTypes\": [\n" +
                        "      {\n" +
                        "        \"id\": 200,\n" +
                        "        \"name\": \"техника и электроника\"\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"id\": 40,\n" +
                        "        \"name\": \"ценное\"\n" +
                        "      }\n" +
                        "    ],\n" +
                        "    \"warehouses\": [\n" +
                        "      {\n" +
                        "        \"id\": 145,\n" +
                        "        \"name\": \"Маршрут (Котельники)\",\n" +
                        "        \"allowed\": true\n" +
                        "      }\n" +
                        "    ]\n" +
                        "  }");
    }

    @Test
    @DisplayName("Дропшипы обрабатываются по особому, с ними не ходим в МБО, но забираем карготипы")
    void ignoreDropshipWarehouses() throws IOException {
        mockMboDeliveryParamsClient("searchFulfillmentSskuParams.dropship.data.json");
        ResponseEntity<String> response = getWarehousesResponse(DROPSHIP_CAMPAIGN_ID, "DROP_STUFF", "47744");
        JsonTestUtil.assertEquals(
                response,
                "{\"cargoTypes\":[],\"warehouses\":[]}");
        Mockito.verifyNoMoreInteractions(mboDeliveryParamsClient);
    }

    @Test
    @DisplayName("Склад недоступен для карточки товара")
    void warehouseAvailabilityNotAllowed() throws IOException {
        mockMboDeliveryParamsClient("searchFulfillmentSskuParams.notAllowed.data.json");
        ResponseEntity<String> response = getWarehousesResponse(CAMPAIGN_ID, "DC-AT-216704532", "145,171");
        JsonTestUtil.assertEquals(
                response,
                "{\n" +
                        "    \"cargoTypes\": [\n" +
                        "      {\n" +
                        "        \"id\": 200,\n" +
                        "        \"name\": \"техника и электроника\"\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"id\": 310,\n" +
                        "        \"name\": \"хрупкое\"\n" +
                        "      }\n" +
                        "    ],\n" +
                        "    \"warehouses\": [\n" +
                        "      {\n" +
                        "        \"id\": 145,\n" +
                        "        \"name\": \"Маршрут (Котельники)\",\n" +
                        "        \"allowed\": false,\n" +
                        "        \"reason\": {\n" +
                        "          \"missingCargoTypes\": [],\n" +
                        "          \"message\": {\n" +
                        "            \"code\": \"mboc.msku.error.supply-forbidden.category.warehouse\",\n" +
                        "            \"jsonData\": \"{\\\"warehouseId\\\":145,\\\"categoryName\\\":\\\"Ноутбуки\\\",\\\"warehouseName\\\":\\\"Маршрут ФФ\\\",\\\"categoryId\\\":91013}\",\n" +
                        "            \"rendered\": \"Запрещены поставки msku в категории Ноутбуки #91013 на склад Маршрут ФФ #145\"\n" +
                        "          }\n" +
                        "        }\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"id\": 171,\n" +
                        "        \"name\": \"Яндекс.Маркет (Томилино)\",\n" +
                        "        \"allowed\": true\n" +
                        "      }\n" +
                        "    ]\n" +
                        "  }");
    }

    @Test
    @DisplayName("Склад недоступен для карточки товара по карготипу")
    void warehouseAvailabilityMissingCargoTypes() throws IOException {
        mockMboDeliveryParamsClient("searchFulfillmentSskuParams.missCargoType.data.json");
        ResponseEntity<String> response = getWarehousesResponse(CAMPAIGN_ID, "sapsanDS-SKU1", "145");
        JsonTestUtil.assertEquals(
                response,
                "{\n" +
                        "    \"cargoTypes\": [\n" +
                        "      {\n" +
                        "        \"id\": 200,\n" +
                        "        \"name\": \"техника и электроника\"\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"id\": 300,\n" +
                        "        \"name\": \"тяжеловесный и крупногабаритный\"\n" +
                        "      }\n" +
                        "    ],\n" +
                        "    \"warehouses\": [\n" +
                        "      {\n" +
                        "        \"id\": 145,\n" +
                        "        \"name\": \"Маршрут (Котельники)\",\n" +
                        "        \"allowed\": false,\n" +
                        "        \"reason\": {\n" +
                        "          \"missingCargoTypes\": [\n" +
                        "            {\n" +
                        "              \"id\": 300,\n" +
                        "              \"name\": \"тяжеловесный и крупногабаритный\"\n" +
                        "            }\n" +
                        "          ],\n" +
                        "          \"message\": {\n" +
                        "            \"code\": \"mboc.msku.error.supply-forbidden.cargo-type-missing\",\n" +
                        "            \"jsonData\": \"{\\\"cargoTypes\\\":\\\"тяжеловесный и крупногабаритный #300\\\",\\\"mskuTitle\\\":\\\"Стиральная машина Hotpoint-Ariston AQ7F 05 U\\\",\\\"warehouseId\\\":145,\\\"mskuId\\\":4864915,\\\"warehouseName\\\":\\\"Маршрут ФФ\\\"}\",\n" +
                        "            \"rendered\": \"Запрещены поставки msku Стиральная машина Hotpoint-Ariston AQ7F 05 U #4864915 на склад Маршрут ФФ #145, на складе не поддерживаются следующие карготипы: тяжеловесный и крупногабаритный #300\"\n" +
                        "          }\n" +
                        "        }\n" +
                        "      }\n" +
                        "    ]\n" +
                        "  }");
    }

    @Test
    @DisplayName("У поставщика нет апрувнутых маппингов")
    void warehouseAvailabilityNoApprovedMappings() {
        // given
        when(mboDeliveryParamsClient.searchFulfilmentSskuParams(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(List.of());

        // when
        ResponseEntity<String> response = getWarehousesResponse(CAMPAIGN_ID, "sapsanDS-SKU1", "145");

        // then
        JsonTestUtil.assertEquals(response, "{\"cargoTypes\":[],\"warehouses\":[]}");
    }

    private ResponseEntity<String> getWarehousesResponse(long campaignId, String shopSku, String warehouseIds) {
        return FunctionalTestHelper.get(
                String.format("%s/by-shop-sku/warehouses?shop_sku={shopSku}&warehouse_id={warehouseId}", shopSkusUrl(campaignId)),
                shopSku,
                warehouseIds
        );
    }

    private String commonOfferInformationForEmptyFields() {
        return "      \"shopSku\": \"vendor123.item-123\","
                + "    \"title\": \"Test offer 567\","
                + "    \"categoryName\": \"my/cateogory1/subcategory2\","
                + "    \"description\": \"Test description 3457\","
                + "    \"brand\": \"Apple\",\n"
                + "    \"barcodes\": [\"xxxy\"], \n"
                + "    \"urls\": [\"http://beru.ru/product/44444\"], \n"
                + "    \"masterData\": {\n"
                + "        \"manufacturer\": \"ОАО Ромашка\",\n"
                + "        \"manufacturerCountry\": \"Россия\",\n";
    }

    private void mockMboDeliveryParamsClient(String filename) throws IOException {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse ffSkuParamsResponse =
                ffResponseFromJsonStream(
                        new ClassPathResource(
                                "data/" + filename,
                                NewMappingMappingControllerFunctionalTest.class
                        ).getInputStream());

        when(mboDeliveryParamsClient.searchFulfilmentSskuParams(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(ffSkuParamsResponse.getFulfilmentInfoList());
    }

    private MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse ffResponseFromJsonStream(InputStream jsonStream) {
        try {
            MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse.Builder responseBuilder =
                    MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse.newBuilder();

            JsonFormat.merge(new BufferedReader(new InputStreamReader(jsonStream)), responseBuilder);

            return responseBuilder.build();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
