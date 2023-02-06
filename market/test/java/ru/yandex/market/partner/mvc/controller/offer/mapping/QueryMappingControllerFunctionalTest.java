package ru.yandex.market.partner.mvc.controller.offer.mapping;

import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mbi.util.MoreMbiMatchers;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.MbocCommon;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.mboc.http.SupplierOffer.Offer.MappingProcessingStatus;
import ru.yandex.market.mboc.http.SupplierOffer.Offer.MappingProcessingStatus.ChangeStatus;
import ru.yandex.market.partner.util.FunctionalTestHelper;

@ParametersAreNonnullByDefault
@DbUnitDataSet(before = "MappingControllerFunctionalTest.csv")
/**
 * Функциональные тесты на на {@link MappingController}.
 */
// AbstractMappingControllerFunctionalTest экстендит общий для ПИ FunctionText
class QueryMappingControllerFunctionalTest
        extends AbstractMappingControllerFunctionalTest /* extends FunctionText */ {

    @Autowired
    private MboMappingsService patientMboMappingsService;


    @Test
    @DisplayName("Поиск оферов привязанных к одному из запрошенных Market-SKU")
    void testQuery() {
        Mockito.when(patientMboMappingsService.searchMappingsByMarketSkuId(Mockito.any()))
                .thenReturn(
                        MboMappings.SearchMappingsResponse.newBuilder()
                                .addOffers(
                                        SupplierOffer.Offer.newBuilder()
                                                .setTitle("Test H123")
                                                .setSupplierId(SUPPLIER_ID)
                                                .setShopSkuId("H123")
                                                .setAvailability(SupplierOffer.Availability.INACTIVE)
                                                .setApprovedMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1288)
                                                                .setSkuName("MarketSku1288")
                                                                .setCategoryId(123)
                                                                .build())
                                                .setSupplierMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1214)
                                                                .setSkuName("MarketSku1214")
                                                                .setCategoryId(123)
                                                                .build())
                                                .setSupplierMappingStatus(
                                                        MappingProcessingStatus.newBuilder()
                                                                .setStatus(ChangeStatus.MODERATION)
                                                                .build())
                                                .addUrls("https://beru.ru/product/100324822646")
                                                .build())
                                .addOffers(
                                        SupplierOffer.Offer.newBuilder()
                                                .setTitle("Test K124")
                                                .setSupplierId(SUPPLIER_ID)
                                                .setShopSkuId("K124")
                                                .setApprovedMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1214)
                                                                .setSkuName("MarketSku1214")
                                                                .setCategoryId(123)
                                                                .build())
                                                .build())
                                .addOffers(
                                        SupplierOffer.Offer.newBuilder()
                                                .setTitle("Test H145")
                                                .setSupplierId(SUPPLIER_ID)
                                                .setShopSkuId("H145")
                                                .setSupplierMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1245)
                                                                .setSkuName("MarketSku1245")
                                                                .setCategoryId(123)
                                                                .build())
                                                .setSupplierMappingStatus(
                                                        MappingProcessingStatus.newBuilder()
                                                                .setStatus(ChangeStatus.REJECTED)
                                                                .build())
                                                .build())
                                .build());
        String url = String.format("%s/queries", mappedMarketSkuUrl(CAMPAIGN_ID));
        HttpEntity<?> request = json("{\"marketSkus\": [1214,1245,1246]}");
        ResponseEntity<String> processedEntity = FunctionalTestHelper.post(url, request);
        MatcherAssert.assertThat(
                processedEntity,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyMatches(
                                "result",
                                MbiMatchers.jsonEquals(/*language=JSON*/ ""
                                        + "{\n" +
                                        " \"marketSkus\": [1214, 1245, 1246],\n" +
                                        " \"mappedMarketSkus\": [{\n" +
                                        "  \"marketSku\": 1214,\n" +
                                        "  \"shopSkus\": [{\n" +
                                        "   \"shopSku\": \"H123\",\n" +
                                        "   \"title\": \"Test H123\",\n" +
                                        "   \"urls\": [\"https://beru.ru/product/100324822646\"],\n" +
                                        "   \"status\": \"MODERATION\",\n" +
                                        "   \"barcodes\": [],\n" +
                                        "   \"description\": \"\",\n" +
                                        "   \"vendorCode\": \"\",\n" +
                                        "   \"offerProcessingStatus\": \"IN_WORK\",\n" +
                                        "   \"offerProcessingComments\": [],\n" +
                                        "   \"availability\": \"INACTIVE\",\n" +
                                        "   \"acceptGoodContent\": false\n" +
                                        "  }, {\n" +
                                        "   \"shopSku\": \"K124\",\n" +
                                        "   \"title\": \"Test K124\",\n" +
                                        "   \"status\": \"ACCEPTED\",\n" +
                                        "   \"barcodes\": [],\n" +
                                        "   \"description\": \"\",\n" +
                                        "   \"vendorCode\": \"\",\n" +
                                        "   \"urls\": [],\n" +
                                        "   \"offerProcessingStatus\": \"IN_WORK\",\n" +
                                        "   \"offerProcessingComments\": [],\n" +
                                        "   \"availability\": \"ACTIVE\",\n" +
                                        "   \"acceptGoodContent\": false\n" +
                                        "  }]\n" +
                                        " }, {\n" +
                                        "  \"marketSku\": 1245,\n" +
                                        "  \"shopSkus\": [{\n" +
                                        "   \"shopSku\": \"H145\",\n" +
                                        "   \"title\": \"Test H145\",\n" +
                                        "   \"status\": \"REJECTED\",\n" +
                                        "   \"barcodes\": [],\n" +
                                        "   \"description\": \"\",\n" +
                                        "   \"vendorCode\": \"\",\n" +
                                        "   \"urls\": [],\n" +
                                        "   \"offerProcessingStatus\": \"IN_WORK\",\n" +
                                        "   \"offerProcessingComments\": [],\n" +
                                        "   \"availability\": \"ACTIVE\",\n" +
                                        "   \"acceptGoodContent\": false\n" +
                                        "  }]\n" +
                                        " }]\n" +
                                        "}"))));
    }

    @Test
    @DisplayName("Поиск оферов привязанных к одному из запрошенных Market-SKU и"
            + " отображение результатов со всеми поддерживаемыми полями оферов")
    void testQueryWithAllProperties() {
        Mockito.when(patientMboMappingsService.searchMappingsByMarketSkuId(Mockito.any()))
                .thenReturn(
                        MboMappings.SearchMappingsResponse.newBuilder()
                                .addOffers(
                                        SupplierOffer.Offer.newBuilder()
                                                .setTitle("Test H123")
                                                .setSupplierId(SUPPLIER_ID)
                                                .setShopSkuId("H123")
                                                .setShopCategoryName("Shop/Category/Name")
                                                .setBarcode("sdkgjsdh12431254, sdjgh124314231, dskjghs124152")
                                                .setVendorCode("sgsd23523")
                                                .setShopVendor("Apple")
                                                .setDescription("Test H123 Description")
                                                .setApprovedMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1288)
                                                                .setSkuName("MarketSku1288")
                                                                .setCategoryId(123)
                                                                .build())
                                                .setSupplierMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1214)
                                                                .setSkuName("MarketSku1214")
                                                                .setCategoryId(123)
                                                                .build())
                                                .setSupplierMappingStatus(
                                                        MappingProcessingStatus.newBuilder()
                                                                .setStatus(ChangeStatus.MODERATION)
                                                                .build())
                                                .addUrls("https://beru.ru/product/100324822646")
                                                .setProcessingStatus(SupplierOffer.OfferProcessingStatus.NEED_INFO)
                                                .addContentComment(SupplierOffer.ContentComment.newBuilder()
                                                        .setMessage(MbocCommon.Message.newBuilder()
                                                                .setMessageCode("code 1")
                                                                .setMustacheTemplate("template 1")
                                                                .build())
                                                        .build()
                                                )
                                                .addContentComment(SupplierOffer.ContentComment.newBuilder()
                                                        .setMessage(MbocCommon.Message.newBuilder()
                                                                .setMessageCode("code 2")
                                                                .setMustacheTemplate("template 2")
                                                                .build())
                                                        .build()
                                                )
                                                .build())
                                .build());
        String url = String.format("%s/queries", mappedMarketSkuUrl(CAMPAIGN_ID));
        HttpEntity<?> request = json("{\"marketSkus\": [1214,1245,1246]}");
        ResponseEntity<String> processedEntity = FunctionalTestHelper.post(url, request);
        MatcherAssert.assertThat(
                processedEntity,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyMatches(
                                "result",
                                MbiMatchers.jsonEquals(/*language=JSON*/ "{\n"
                                        + "  \"marketSkus\": [\n"
                                        + "    1214,\n"
                                        + "    1245,\n"
                                        + "    1246\n"
                                        + "  ],\n"
                                        + "  \"mappedMarketSkus\": [\n"
                                        + "    {\n"
                                        + "      \"marketSku\": 1214,\n"
                                        + "      \"shopSkus\": [\n"
                                        + "        {\n"
                                        + "          \"shopSku\": \"H123\",\n"
                                        + "          \"title\": \"Test H123\",\n"
                                        + "          \"categoryName\": \"Shop/Category/Name\",\n"
                                        + "          \"barcodes\": [\n"
                                        + "            \"sdkgjsdh12431254\",\n"
                                        + "            \"sdjgh124314231\",\n"
                                        + "            \"dskjghs124152\"\n"
                                        + "          ],\n"
                                        + "          \"urls\": [\n"
                                        + "            \"https://beru.ru/product/100324822646\"\n"
                                        + "          ],\n"
                                        + "          \"offerProcessingComments\": [\n"
                                        + "            {\n"
                                        + "              \"code\": \"mboc.code 1\",\n"
                                        + "              \"template\": \"комментарий 1\",\n"
                                        + "              \"templateArguments\": \"{}\",\n"
                                        + "              \"text\": \"комментарий 1\"\n"
                                        + "            },\n"
                                        + "            {\n"
                                        + "              \"code\":\"mboc.code 2\",\n"
                                        + "              \"template\":\"template 2\",\n"
                                        + "              \"templateArguments\":\"{}\",\n"
                                        + "              \"text\":\"Обратитесь за подробностями к вашему менеджеру или в службу поддержки на merchant@market.yandex.ru.\"\n"
                                        + "            }\n"
                                        + "          ],\n"
                                        + "          \"status\": \"MODERATION\",\n"
                                        + "          \"description\": \"Test H123 Description\",\n"
                                        + "          \"brand\": \"Apple\",\n"
                                        + "          \"vendorCode\": \"sgsd23523\",\n"
                                        + "          \"offerProcessingStatus\": \"NEED_INFO\",\n"
                                        + "          \"availability\": \"ACTIVE\",\n"
                                        + "          \"acceptGoodContent\":false\n"
                                        + "        }\n"
                                        + "      ]\n"
                                        + "    }\n"
                                        + "  ]\n"
                                        + "}"))));
    }

    @Test
    @DisplayName("Поиск оферов привязанных к одному из запрошенных Market-SKU, когда"
            + " список запрошенных Market-SKU пустой")
    void testEmptyQuery() {
        String url = String.format("%s/queries", mappedMarketSkuUrl(CAMPAIGN_ID));
        HttpEntity<?> request = json("{\"marketSkus\": []}");
        ResponseEntity<String> processedEntity = FunctionalTestHelper.post(url, request);
        MatcherAssert.assertThat(processedEntity,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyMatches(
                                "result",
                                MbiMatchers.jsonEquals(/*language=JSON*/ ""
                                        + "{"
                                        + "    \"marketSkus\": [],"
                                        + "    \"mappedMarketSkus\": []"
                                        + "}"))));
        Mockito.verify(patientMboMappingsService, Mockito.never()).searchMappingsByMarketSkuId(Mockito.any());
    }

    @Test
    @DisplayName("Поиск оферов привязанных к одному из запрошенных Market-SKU, когда"
            + " формат запроса некорректный")
    void testBadQuery() {
        String url = String.format("%s/queries", mappedMarketSkuUrl(CAMPAIGN_ID));
        HttpEntity<?> request = json("{}");
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
                                                + "    \"message\":\"must not be null\","
                                                + "    \"details\":{"
                                                + "        \"field\":\"marketSkus\","
                                                + "        \"subcode\":\"INVALID\""
                                                + "    }"
                                                + "}")
                                )
                        )
                )
        );
    }
}
