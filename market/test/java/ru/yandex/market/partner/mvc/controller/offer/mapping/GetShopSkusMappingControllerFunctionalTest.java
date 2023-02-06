package ru.yandex.market.partner.mvc.controller.offer.mapping;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import Market.DataCamp.SyncAPI.SyncChangeOffer;
import com.google.common.collect.ImmutableSet;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mbi.util.MoreMbiMatchers;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.mboc.http.SupplierOffer.Offer.MappingProcessingStatus;
import ru.yandex.market.mboc.http.SupplierOffer.Offer.MappingProcessingStatus.ChangeStatus;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;

@ParametersAreNonnullByDefault
@DbUnitDataSet(before = "MappingControllerFunctionalTest.csv")
// AbstractMappingControllerFunctionalTest экстендит общий для ПИ FunctionText
class GetShopSkusMappingControllerFunctionalTest
        extends AbstractMappingControllerFunctionalTest /* extends FunctionText */ {

    private static final long CAMPAIGN_ID = 10774L;
    private static final long SUPPLIER_ID = 774L;

    @Autowired
    private MboMappingsService patientMboMappingsService;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private DataCampClient dataCampShopClient;

    @BeforeEach
    void setUp() {
        environmentService.setValue("mapping.controller.get.shop.sku.total.count", "true");
    }

    @Test
    @DisplayName("Запрос списка оферов с пустым ответов")
    void testListSkusWithEmptyResult() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(any()))
                .thenReturn(
                        MboMappings.SearchMappingsResponse.newBuilder()
                                .setTotalCount(0)
                                .build()
                );


        String url = String.format("%s", listShopSkusUrl(CAMPAIGN_ID));
        ResponseEntity<String> resultEntity = FunctionalTestHelper.post(url, "{}");
        MatcherAssert.assertThat(
                resultEntity,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyMatches("result",
                                MbiMatchers.jsonEquals(/*language=JSON*/ ""
                                        + "{"
                                        + "    \"shopSkus\": []"
                                        + "}"))));
        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.hasSupplierId()
                        && request.getSupplierId() == SUPPLIER_ID
                        && request.hasLimit()
                        && request.getLimit() == 100
                        && request.getOfferQueriesAnyOfList().isEmpty()
                        && !request.hasHasAnyMapping()
                        && request.getHasSupplierMappingStatusList().isEmpty()
                        && request.getHasNoSupplierMappingStatusList().isEmpty()
                        && request.getMarketCategoryIdsCount() == 0
                        && !request.hasTextQueryString()
                        && !request.hasOffsetKey()
                        && !request.getReturnMasterData()));
    }

    @Test
    @DisplayName("Запрос списка оферов")
    void testListSkus() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(any()))
                .thenReturn(
                        MboMappings.SearchMappingsResponse.newBuilder()
                                .addOffers(
                                        SupplierOffer.Offer.newBuilder()
                                                .setTitle("Test 1")
                                                .setSupplierId(SUPPLIER_ID)
                                                .setShopSkuId("1")
                                                .setAvailability(SupplierOffer.Availability.INACTIVE)
                                                .setMarketCategoryId(123)
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
                                                .setSuggestMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1289)
                                                                .setSkuName("MarketSku1289")
                                                                .setCategoryId(123)
                                                                .build())
                                                .build()
                                )
                                .addOffers(
                                        SupplierOffer.Offer.newBuilder()
                                                .setTitle("Test H124")
                                                .setSupplierId(SUPPLIER_ID)
                                                .setShopSkuId("H124")
                                                .setShopCategoryName("Shop/Category/Name")
                                                .setBarcode("sdkgjsdh12431254, sdjgh124314231, dskjghs124152")
                                                .addUrls("http://urls.urls.ru")
                                                .setVendorCode("sgsd23523")
                                                .setShopVendor("Apple")
                                                .setDescription("Test H124 Description")
                                                .setMarketCategoryId(123)
                                                .setApprovedMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1288)
                                                                .setSkuName("MarketSku1288")
                                                                .setCategoryId(123)
                                                                .build()
                                                )
                                                .setSupplierMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1214)
                                                                .setSkuName("MarketSku1214")
                                                                .setCategoryId(123)
                                                                .build()
                                                )
                                                .setSupplierMappingStatus(
                                                        MappingProcessingStatus.newBuilder()
                                                                .setStatus(ChangeStatus.MODERATION)
                                                                .build()
                                                )
                                                .build()
                                )
                                .setNextOffsetKey("xx564dsgdklsg")
                                .setTotalCount(12345)
                                .build()
                );
        doReturn(ProtoTestUtil.getProtoMessageByJson(
                SyncChangeOffer.FullOfferResponse.class,
                "proto/containsOffer.json",
                getClass()
        )).when(dataCampShopClient).getOffers(anyLong(), any(), any());
        String url = String.format("%s", listShopSkusUrl(CAMPAIGN_ID));
        ResponseEntity<String> resultEntity = FunctionalTestHelper.post(url, "{}");
        MatcherAssert.assertThat(
                resultEntity,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyMatches("result",
                                MbiMatchers.jsonEquals(/*language=JSON*/ "" +
                                        "{\n" +
                                        "  \"shopSkus\": [\n" +
                                        "    {\n" +
                                        "      \"shopSku\": \"1\",\n" +
                                        "      \"title\": \"Test 1\",\n" +
                                        "      \"barcodes\": [],\n" +
                                        "      \"description\":\"\",\n" +
                                        "      \"vendorCode\":\"\",\n" +
                                        "      \"urls\": [],\n" +
                                        "      \"offerProcessingStatus\": \"IN_WORK\",\n" +
                                        "      \"offerProcessingComments\": [],\n" +
                                        "      \"marketCategoryId\": 123,\n" +
                                        "      \"marketCategoryName\": \"Электроника\",\n" +
                                        "      \"price\": 3.0000000,\n" +
                                        "      \"oldPrice\": 2.0000000,\n" +
                                        "      \"vat\": 7,\n" +
                                        "      \"acceptGoodContent\": true,\n" +
                                        "      \"availability\": \"INACTIVE\",\n" +
                                        "      \"mappings\": {\n" +
                                        "        \"active\": {\n" +
                                        "          \"marketSku\": 1288,\n" +
                                        "          \"contentType\": \"market\",\n" +
                                        "          \"categoryName\": \"\"\n" +
                                        "        },\n" +
                                        "        \"awaitingModeration\": {\n" +
                                        "          \"marketSku\": 1214,\n" +
                                        "          \"contentType\": \"market\",\n" +
                                        "          \"categoryName\": \"\"\n" +
                                        "        },\n" +
                                        "        \"rejected\": [],\n" +
                                        "        \"suggested\": {\n" +
                                        "          \"marketSku\": 1289,\n" +
                                        "          \"contentType\": \"market\",\n" +
                                        "          \"categoryName\": \"\"\n" +
                                        "        }\n" +
                                        "      }\n" +
                                        "    },\n" +
                                        "    {\n" +
                                        "      \"shopSku\": \"H124\",\n" +
                                        "      \"title\": \"Test H124\",\n" +
                                        "      \"categoryName\": \"Shop/Category/Name\",\n" +
                                        "      \"description\": \"Test H124 Description\",\n" +
                                        "      \"brand\": \"Apple\",\n" +
                                        "      \"vendorCode\": \"sgsd23523\",\n" +
                                        "      \"barcodes\": [\n" +
                                        "        \"sdkgjsdh12431254\",\n" +
                                        "        \"sdjgh124314231\",\n" +
                                        "        \"dskjghs124152\"\n" +
                                        "      ],\n" +
                                        "      \"urls\": [\n" +
                                        "        \"http://urls.urls.ru\"\n" +
                                        "      ],\n" +
                                        "      \"offerProcessingStatus\": \"IN_WORK\",\n" +
                                        "      \"offerProcessingComments\": [],\n" +
                                        "      \"marketCategoryId\": 123,\n" +
                                        "      \"marketCategoryName\": \"Электроника\",\n" +
                                        "      \"acceptGoodContent\": true,\n" +
                                        "      \"availability\": \"ACTIVE\",\n" +
                                        "      \"mappings\": {\n" +
                                        "        \"active\": {\n" +
                                        "          \"marketSku\": 1288,\n" +
                                        "          \"contentType\": \"market\",\n" +
                                        "          \"categoryName\": \"\"\n" +
                                        "        },\n" +
                                        "        \"awaitingModeration\": {\n" +
                                        "          \"marketSku\": 1214,\n" +
                                        "          \"contentType\": \"market\",\n" +
                                        "          \"categoryName\": \"\"\n" +
                                        "        },\n" +
                                        "        \"rejected\": []\n" +
                                        "      }\n" +
                                        "    }\n" +
                                        "  ],\n" +
                                        "  \"nextPageToken\": \"xx564dsgdklsg\"" +
                                        "}"))));
        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.hasSupplierId()
                        && request.getSupplierId() == SUPPLIER_ID
                        && request.hasLimit()
                        && request.getLimit() == 100
                        && request.getOfferQueriesAnyOfList().isEmpty()
                        && !request.hasHasAnyMapping()
                        && request.getHasSupplierMappingStatusList().isEmpty()
                        && request.getHasNoSupplierMappingStatusList().isEmpty()
                        && request.getMarketCategoryIdsCount() == 0
                        && !request.hasTextQueryString()
                        && !request.hasOffsetKey()
                        && !request.getReturnMasterData()));
    }

    @Test
    @DisplayName("Запрос списка оферов с указанием page_token для выбора порции данных")
    void testListSkusWithPageToken() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(any()))
                .thenReturn(MboMappings.SearchMappingsResponse.newBuilder().build());

        String url = String.format("%s?page_token={pageToken}", listShopSkusUrl(CAMPAIGN_ID));
        FunctionalTestHelper.post(url, "{}", "TKN123");
        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.hasSupplierId()
                        && request.getSupplierId() == SUPPLIER_ID
                        && request.hasLimit()
                        && request.getLimit() == 100
                        && request.getOfferQueriesAnyOfList().isEmpty()
                        && !request.hasHasAnyMapping()
                        && request.getHasSupplierMappingStatusList().isEmpty()
                        && request.getHasNoSupplierMappingStatusList().isEmpty()
                        && request.getMarketCategoryIdsCount() == 0
                        && !request.hasTextQueryString()
                        && request.hasOffsetKey()
                        && request.getOffsetKey().equals("TKN123")
                        && request.hasReturnTotalCount()
                        && !request.getReturnTotalCount()
                        && !request.getReturnMasterData()));
    }

    @Test
    @DisplayName("Запрос списка оферов с параметром is_editable")
    void testListSkusWithIsEditable() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(any()))
                .thenReturn(MboMappings.SearchMappingsResponse.newBuilder().build());

        String url = String.format("%s?is_editable={isEditable}", listShopSkusUrl(CAMPAIGN_ID));
        FunctionalTestHelper.post(url, "{}", true);
        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.hasSupplierId()
                        && request.getSupplierId() == SUPPLIER_ID
                        && request.hasLimit()
                        && request.getLimit() == 100
                        && request.getOfferQueriesAnyOfList().size() == 1
                        && request.getOfferQueriesAnyOfList().contains(MboMappings.OfferQuery.MODEL_IS_EDITABLE)
                        && !request.hasHasAnyMapping()
                        && request.getHasSupplierMappingStatusList().isEmpty()
                        && request.getHasNoSupplierMappingStatusList().isEmpty()
                        && request.getMarketCategoryIdsCount() == 0
                        && !request.hasTextQueryString()
                        && !request.hasOffsetKey()
                        && !request.getReturnMasterData()));
    }

    @Test
    @DisplayName("Запрос списка оферов с указанием offerSattus=REJECTED для выбора порции данных")
    void testListSkusWithRejectedOfferStatusOnly() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(any()))
                .thenReturn(MboMappings.SearchMappingsResponse.newBuilder().build());

        String url = listShopSkusUrl(CAMPAIGN_ID);
        //language=json
        String jsonBody = "{\"offerProcessingStatuses\": [\"REJECTED\"]}";
        FunctionalTestHelper.post(url, jsonBody);
        List<SupplierOffer.OfferProcessingStatus> expectedChangeOfferStatusSet =
                Collections.singletonList(SupplierOffer.OfferProcessingStatus.REJECTED);
        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.hasSupplierId()
                        && request.getSupplierId() == SUPPLIER_ID
                        && request.hasLimit()
                        && request.getLimit() == 100
                        && request.getOfferQueriesAnyOfList().isEmpty()
                        && request.getOfferProcessingStatusList().equals(expectedChangeOfferStatusSet)
                        && request.getHasNoSupplierMappingStatusList().isEmpty()
                        && request.getMarketCategoryIdsCount() == 0
                        && !request.hasTextQueryString()
                        && !request.hasOffsetKey()
                        && !request.getReturnMasterData()));
    }

    @Test
    @DisplayName("Запрос списка оферов с указанием offerSattus=NEED_MAPPING для выбора порции данных")
    void testListSkusWithNeedMappingOfferStatusOnly() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(any()))
                .thenReturn(MboMappings.SearchMappingsResponse.newBuilder().build());

        String url = listShopSkusUrl(CAMPAIGN_ID);
        //language=json
        String jsonBody = "{\"offerProcessingStatuses\": [\"NEED_MAPPING\"]}";
        FunctionalTestHelper.post(url, jsonBody);
        List<SupplierOffer.OfferProcessingStatus> expectedChangeOfferStatusSet =
                Collections.singletonList(SupplierOffer.OfferProcessingStatus.NEED_MAPPING);
        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.hasSupplierId()
                        && request.getSupplierId() == SUPPLIER_ID
                        && request.hasLimit()
                        && request.getLimit() == 100
                        && request.getOfferQueriesAnyOfList().isEmpty()
                        && request.getOfferProcessingStatusList().equals(expectedChangeOfferStatusSet)
                        && request.getHasNoSupplierMappingStatusList().isEmpty()
                        && request.getMarketCategoryIdsCount() == 0
                        && !request.hasTextQueryString()
                        && !request.hasOffsetKey()
                        && !request.getReturnMasterData()));
    }

    @Test
    @DisplayName("Запрос списка оферов с указанием offerSattus=IN_WORK и offerSattus=CONTENT_PROCESSING")
    void testListSkusWithMultipleOfferStatusesOnly() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(any()))
                .thenReturn(MboMappings.SearchMappingsResponse.newBuilder().build());

        String url = listShopSkusUrl(CAMPAIGN_ID);
        //language=json
        String jsonBody = "{\"offerProcessingStatuses\": [\"IN_WORK\", \"CONTENT_PROCESSING\"]}";
        FunctionalTestHelper.post(url, jsonBody);
        Set<SupplierOffer.OfferProcessingStatus> expectedChangeOfferStatusSet =
                new HashSet<>(Arrays.asList(
                        SupplierOffer.OfferProcessingStatus.REVIEW,
                        SupplierOffer.OfferProcessingStatus.IN_WORK,
                        SupplierOffer.OfferProcessingStatus.CONTENT_PROCESSING
                ));
        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.hasSupplierId()
                        && request.getSupplierId() == SUPPLIER_ID
                        && request.hasLimit()
                        && request.getLimit() == 100
                        && request.getOfferQueriesAnyOfList().isEmpty()
                        && new HashSet<>(request.getOfferProcessingStatusList()).equals(expectedChangeOfferStatusSet)
                        && request.getHasNoSupplierMappingStatusList().isEmpty()
                        && request.getMarketCategoryIdsCount() == 0
                        && !request.hasTextQueryString()
                        && !request.hasOffsetKey()
                        && !request.getReturnMasterData()));
    }

    @Test
    @DisplayName("Запрос списка оферов с указанием неподдерживаемого статуса офера")
    void testListSkusWithUnknownOfferStatus() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(any()))
                .thenReturn(MboMappings.SearchMappingsResponse.newBuilder().build());

        String url = listShopSkusUrl(CAMPAIGN_ID);
        //language=json
        String jsonBody = "{\"offerProcessingStatuses\": [\"C3P0\"]}";
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(url, jsonBody)
        );
        MatcherAssert.assertThat(
                exception,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyMatches(
                                        "errors",
                                        MbiMatchers.jsonArrayEquals(/*language=JSON*/ ""
                                                + "{\n" +
                                                "  \"code\": \"BAD_PARAM\",\n" +
                                                "  \"details\": {\n" +
                                                "    \"subcode\": \"INVALID\",\n" +
                                                "    \"value\": \"C3P0\"\n" +
                                                "  }\n" +
                                                "}")
                                )
                        )
                )
        );
    }

    @Test
    void testListSkusWithCategoryId() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(any()))
                .thenReturn(MboMappings.SearchMappingsResponse.newBuilder().build());

        String url = listShopSkusUrl(CAMPAIGN_ID);
        //language=json
        String jsonBody = "{\"categoryIds\": [1421]}";
        FunctionalTestHelper.post(url, jsonBody);
        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.hasSupplierId()
                        && request.getSupplierId() == SUPPLIER_ID
                        && request.hasLimit()
                        && request.getLimit() == 100
                        && request.getOfferQueriesAnyOfList().isEmpty()
                        && !request.hasHasAnyMapping()
                        && request.getHasSupplierMappingStatusList().isEmpty()
                        && request.getHasNoSupplierMappingStatusList().isEmpty()
                        && request.getMarketCategoryIdsList().equals(Collections.singletonList(1421))
                        && !request.hasTextQueryString()
                        && !request.hasOffsetKey()
                        && !request.getReturnMasterData()));
    }

    @Test
    void testListSkusWithMultipleCategoryId() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(any()))
                .thenReturn(MboMappings.SearchMappingsResponse.newBuilder().build());

        String url = listShopSkusUrl(CAMPAIGN_ID);
        //language=json
        String jsonBody = "{\"categoryIds\": [1421, 1425]}";
        FunctionalTestHelper.post(url, jsonBody);
        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.hasSupplierId()
                        && request.getSupplierId() == SUPPLIER_ID
                        && request.hasLimit()
                        && request.getLimit() == 100
                        && request.getOfferQueriesAnyOfList().isEmpty()
                        && !request.hasHasAnyMapping()
                        && request.getHasSupplierMappingStatusList().isEmpty()
                        && request.getHasNoSupplierMappingStatusList().isEmpty()
                        && request.getMarketCategoryIdsList().equals(Arrays.asList(1421, 1425))
                        && !request.hasTextQueryString()
                        && !request.hasOffsetKey()
                        && !request.getReturnMasterData()));
    }

    @Test
    void testListSkusWithMultipleCategoryIdWithComma() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(any()))
                .thenReturn(MboMappings.SearchMappingsResponse.newBuilder().build());

        String url = listShopSkusUrl(CAMPAIGN_ID);
        //language=json
        String jsonBody = "{\"categoryIds\": [1421, 1425]}";
        FunctionalTestHelper.post(url, jsonBody);
        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.hasSupplierId()
                        && request.getSupplierId() == SUPPLIER_ID
                        && request.hasLimit()
                        && request.getLimit() == 100
                        //todo: check isEditable for deprecated GET method call
                        && request.getOfferQueriesAnyOfList().isEmpty()
                        && !request.hasHasAnyMapping()
                        && request.getHasSupplierMappingStatusList().isEmpty()
                        && request.getHasNoSupplierMappingStatusList().isEmpty()
                        && request.getMarketCategoryIdsList().equals(Arrays.asList(1421, 1425))
                        && !request.hasTextQueryString()
                        && !request.hasOffsetKey()
                        && !request.getReturnMasterData()));
    }

    @Test
    void testListSkusWithSearchQuery() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(any()))
                .thenReturn(MboMappings.SearchMappingsResponse.newBuilder().build());

        String url = String.format("%s?q={queryString}", listShopSkusUrl(CAMPAIGN_ID));
        FunctionalTestHelper.post(url, "{}", "needle");
        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.hasSupplierId()
                        && request.getSupplierId() == SUPPLIER_ID
                        && request.hasLimit()
                        && request.getLimit() == 100
                        && request.getOfferQueriesAnyOfList().isEmpty()
                        && !request.hasHasAnyMapping()
                        && request.getHasSupplierMappingStatusList().isEmpty()
                        && request.getHasNoSupplierMappingStatusList().isEmpty()
                        && request.getMarketCategoryIdsCount() == 0
                        && request.hasTextQueryString()
                        && request.getTextQueryString().equals("needle")
                        && !request.hasOffsetKey()
                        && !request.getReturnMasterData()));
    }

    @Test
    void testListSkusWithCustomLimit() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(any()))
                .thenReturn(MboMappings.SearchMappingsResponse.newBuilder().build());

        String url = String.format("%s?limit={limit}", listShopSkusUrl(CAMPAIGN_ID));
        FunctionalTestHelper.post(url, "{}", "27");
        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.hasSupplierId()
                        && request.getSupplierId() == SUPPLIER_ID
                        && request.hasLimit()
                        && request.getLimit() == 27
                        && request.getOfferQueriesAnyOfList().isEmpty()
                        && !request.hasHasAnyMapping()
                        && request.getHasSupplierMappingStatusList().isEmpty()
                        && request.getHasNoSupplierMappingStatusList().isEmpty()
                        && request.getMarketCategoryIdsCount() == 0
                        && !request.hasTextQueryString()
                        && !request.hasOffsetKey()
                        && !request.getReturnMasterData()));
    }

    @Test
    void testListSkusWithMSkuMapping() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(any()))
                .thenReturn(MboMappings.SearchMappingsResponse.newBuilder().build());

        String url = listShopSkusUrl(CAMPAIGN_ID);
        //language=json
        String jsonBody = "{\"contentType\": \"market\"}";
        FunctionalTestHelper.post(url, jsonBody);

        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.getSupplierId() == SUPPLIER_ID
                        && request.getLimit() == 100
                        && request.getOfferQueriesAnyOfList().isEmpty()
                        && request.getMappingFiltersList().equals(Collections.singletonList(
                        MboMappings.MappingFilter
                                .newBuilder()
                                .setMappingSkuKind(MboMappings.MappingSkuKind.MARKET)
                                .build()))
                        && request.getHasNoSupplierMappingStatusList().isEmpty()
                        && request.getMarketCategoryIdsCount() == 0
                        && !request.hasTextQueryString()
                        && !request.hasOffsetKey()
                        && !request.getReturnMasterData())
        );
    }

    @Test
    void testListSkusWithPSkuMapping() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(any()))
                .thenReturn(MboMappings.SearchMappingsResponse.newBuilder().build());

        String url = listShopSkusUrl(CAMPAIGN_ID);
        //language=json
        String jsonBody = "{\"contentType\": \"partner\"}";
        FunctionalTestHelper.post(url, jsonBody);

        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.getSupplierId() == SUPPLIER_ID
                        && request.getLimit() == 100
                        && request.getOfferQueriesAnyOfList().isEmpty()
                        && request.getMappingFiltersList().equals(Collections.singletonList(
                        MboMappings.MappingFilter
                                .newBuilder()
                                .setMappingSkuKind(MboMappings.MappingSkuKind.PARTNER)
                                .build()))
                        && request.getHasNoSupplierMappingStatusList().isEmpty()
                        && request.getMarketCategoryIdsCount() == 0
                        && !request.hasTextQueryString()
                        && !request.hasOffsetKey()
                        && !request.getReturnMasterData())
        );
    }

    @Test
    void testListSkusWithAvailabilityStatus() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(any()))
                .thenReturn(MboMappings.SearchMappingsResponse.newBuilder().build());

        String url = shopSkusAsXlsUrl(CAMPAIGN_ID);
        //language=json
        String jsonBody = "{\"availabilityStatuses\": [\"INACTIVE\"]}";
        FunctionalTestHelper.post(url, jsonBody);

        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.getSupplierId() == SUPPLIER_ID
                        && request.getAvailabilityList().equals(
                        Collections.singletonList(SupplierOffer.Availability.INACTIVE))
                        && request.getHasSupplierMappingStatusList().isEmpty()
                        && request.getHasNoSupplierMappingStatusList().isEmpty()
                        && request.getMarketCategoryIdsList().isEmpty()
                        && request.getMappingFiltersList().isEmpty()
                        && !request.hasTextQueryString()
                        && request.hasReturnTotalCount()
                        && !request.getReturnTotalCount()
                        && request.getReturnMasterData()
        ));
    }

    @Test
    void testListSkusWithVendors() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(any()))
                .thenReturn(MboMappings.SearchMappingsResponse.newBuilder().build());

        String url = shopSkusAsXlsUrl(CAMPAIGN_ID);
        //language=json
        String jsonBody = "{\"vendors\": [\"Tesla\", \"SpaceX\"]}";
        FunctionalTestHelper.post(url, jsonBody);

        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.getSupplierId() == SUPPLIER_ID
                        && request.getVendorsList().equals(Arrays.asList("Tesla", "SpaceX"))
                        && request.getAvailabilityList().isEmpty()
                        && request.getHasSupplierMappingStatusList().isEmpty()
                        && request.getHasNoSupplierMappingStatusList().isEmpty()
                        && request.getMarketCategoryIdsList().isEmpty()
                        && request.getMappingFiltersList().isEmpty()
                        && !request.hasTextQueryString()
                        && request.hasReturnTotalCount()
                        && !request.getReturnTotalCount()
                        && request.getReturnMasterData()
        ));
    }

    @Test
    void testListSkusWithSeveralAvailabilityStatuses() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(any()))
                .thenReturn(MboMappings.SearchMappingsResponse.newBuilder().build());

        String url = shopSkusAsXlsUrl(CAMPAIGN_ID);
        String jsonBody = "{\"availabilityStatuses\": [\"INACTIVE\", \"DELISTED\"]}";
        FunctionalTestHelper.post(url, jsonBody);
        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.getSupplierId() == SUPPLIER_ID
                        && new HashSet<>(request.getAvailabilityList()).equals(
                        ImmutableSet.of(SupplierOffer.Availability.INACTIVE, SupplierOffer.Availability.DELISTED))
                        && request.getHasSupplierMappingStatusList().isEmpty()
                        && request.getHasNoSupplierMappingStatusList().isEmpty()
                        && request.getMarketCategoryIdsList().isEmpty()
                        && request.getMappingFiltersList().isEmpty()
                        && !request.hasTextQueryString()
                        && request.hasReturnTotalCount()
                        && !request.getReturnTotalCount()
                        && request.getReturnMasterData()
        ));
    }

    @Test
    @DisplayName("Запрос списка офферов с указанием статусов маркировки")
    void testListSkusWithCisHandleModes() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(any()))
                .thenReturn(MboMappings.SearchMappingsResponse.newBuilder().build());

        String url = listShopSkusUrl(CAMPAIGN_ID);
        //language=json
        String jsonBody = "{\"cisHandleModes\": [\"CIS_DISTINCT\"]}";
        FunctionalTestHelper.post(url, jsonBody);
        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.hasSupplierId()
                        && request.getApprovedMappingCargoTypeFiltersList().size() == 1
                        && request.getApprovedMappingCargoTypeFilters(0).getCargoType985()
                        && !request.getApprovedMappingCargoTypeFilters(0).getCargoType990()
                        && !request.getApprovedMappingCargoTypeFilters(0).getCargoType980()));
    }

    @Test
    @DisplayName("Запрос общего количества оферов")
    void checkShopSkusCount() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(any()))
                .thenReturn(
                        MboMappings.SearchMappingsResponse.newBuilder()
                                .addOffers(
                                        SupplierOffer.Offer.newBuilder()
                                                .setTitle("Test 1")
                                                .setSupplierId(SUPPLIER_ID)
                                                .setShopSkuId("1")
                                                .setAvailability(SupplierOffer.Availability.INACTIVE)
                                                .setMarketCategoryId(123)
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
                                                .setSuggestMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1289)
                                                                .setSkuName("MarketSku1289")
                                                                .setCategoryId(123)
                                                                .build())
                                                .build()
                                )
                                .addOffers(
                                        SupplierOffer.Offer.newBuilder()
                                                .setTitle("Test H124")
                                                .setSupplierId(SUPPLIER_ID)
                                                .setShopSkuId("H124")
                                                .setShopCategoryName("Shop/Category/Name")
                                                .setBarcode("sdkgjsdh12431254, sdjgh124314231, dskjghs124152")
                                                .addUrls("http://urls.urls.ru")
                                                .setVendorCode("sgsd23523")
                                                .setShopVendor("Apple")
                                                .setDescription("Test H124 Description")
                                                .setMarketCategoryId(123)
                                                .setApprovedMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1288)
                                                                .setSkuName("MarketSku1288")
                                                                .setCategoryId(123)
                                                                .build()
                                                )
                                                .setSupplierMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1214)
                                                                .setSkuName("MarketSku1214")
                                                                .setCategoryId(123)
                                                                .build()
                                                )
                                                .setSupplierMappingStatus(
                                                        MappingProcessingStatus.newBuilder()
                                                                .setStatus(ChangeStatus.MODERATION)
                                                                .build()
                                                )
                                                .build()
                                )
                                .setNextOffsetKey("xx564dsgdklsg")
                                .setTotalCount(12345)
                                .build()
                );

        String url = String.format("%s", getShopSkusCount(CAMPAIGN_ID));
        ResponseEntity<String> resultEntity = FunctionalTestHelper.post(url, "{}");
        MatcherAssert.assertThat(
                resultEntity,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyMatches("result",
                                MbiMatchers.jsonEquals(/*language=JSON*/ "" +
                                        "{\n" +
                                        "  \"total\": 12345\n" +
                                        "}"))));
        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.hasSupplierId()
                        && request.getSupplierId() == SUPPLIER_ID
                        && request.hasLimit()
                        && request.getLimit() == 1
                        && request.getOfferQueriesAnyOfList().isEmpty()
                        && !request.hasHasAnyMapping()
                        && request.getHasSupplierMappingStatusList().isEmpty()
                        && request.getHasNoSupplierMappingStatusList().isEmpty()
                        && request.getMarketCategoryIdsCount() == 0
                        && !request.hasTextQueryString()
                        && !request.hasOffsetKey()
                        && request.hasReturnTotalCount()
                        && request.getReturnTotalCount()
                        && !request.getReturnMasterData()
                )
        );
    }
}
