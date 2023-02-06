package ru.yandex.market.partner.mvc.controller.offer.mapping;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableSet;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mbi.util.MoreMbiMatchers;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappings.SearchMappingCategoriesResponse;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.partner.util.FunctionalTestHelper;

@ParametersAreNonnullByDefault
@DbUnitDataSet(before = "CategoriesMappingControllerFunctionalTest.csv")
// AbstractMappingControllerFunctionalTest экстендит общий для ПИ FunctionText
class CategoriesMappingControllerFunctionalTest extends AbstractMappingControllerFunctionalTest {

    private static final long CAMPAIGN_ID = 10774L;
    private static final long SUPPLIER_ID = 774L;

    @Autowired
    private MboMappingsService patientMboMappingsService;

    private static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of("",
                        "["
                                + "    {\"categoryId\":1101,\"name\":\"Все товары\", \"acceptGoodContent\":false},"
                                + "    {\"categoryId\":1102,\"name\":\"Электроника\",\"parentId\":1101, \"acceptGoodContent\":false},"
                                + "    {\"categoryId\":1103,\"name\":\"Мобильные телефоны\",\"parentId\":1102, \"offerCount\":134, \"pskuMappedOfferCount\":34, \"acceptPartnerSkus\":true, \"acceptPartnerModels\":false, \"acceptGoodContent\":true},"
                                + "    {\"categoryId\":1104,\"name\":\"Не мобильные телефоны\",\"parentId\":1102, \"offerCount\":145, \"pskuMappedOfferCount\":45, \"acceptPartnerSkus\":true, \"acceptPartnerModels\":true, \"acceptGoodContent\":true},"
                                + "    {\"categoryId\":1112,\"name\":\"Бытовая химия\",\"parentId\":1101, \"offerCount\":123, \"pskuMappedOfferCount\":23, \"acceptPartnerSkus\":false, \"acceptPartnerModels\":true, \"acceptGoodContent\":false}"
                                + "]",
                        "Запрос списка категорий"),
                Arguments.of("?tree=false",
                        "["
                                + "    {\"categoryId\":1103,\"name\":\"Мобильные телефоны\",\"parentId\":1102, \"offerCount\":134, \"pskuMappedOfferCount\":34, \"acceptPartnerSkus\":true, \"acceptPartnerModels\":false, \"acceptGoodContent\":true},"
                                + "    {\"categoryId\":1104,\"name\":\"Не мобильные телефоны\",\"parentId\":1102, \"offerCount\":145, \"pskuMappedOfferCount\":45, \"acceptPartnerSkus\":true, \"acceptPartnerModels\":true, \"acceptGoodContent\":true},"
                                + "    {\"categoryId\":1112,\"name\":\"Бытовая химия\",\"parentId\":1101, \"offerCount\":123, \"pskuMappedOfferCount\":23, \"acceptPartnerSkus\":false, \"acceptPartnerModels\":true, \"acceptGoodContent\":false}"
                                + "]",
                        "Запрос списка категорий tree=false"),
                Arguments.of("?accept_good_content=true",
                        "["
                                + "    {\"categoryId\":1103,\"name\":\"Мобильные телефоны\",\"parentId\":1102, \"offerCount\":134, \"pskuMappedOfferCount\":34, \"acceptPartnerSkus\":true, \"acceptPartnerModels\":false, \"acceptGoodContent\":true},"
                                + "    {\"categoryId\":1104,\"name\":\"Не мобильные телефоны\",\"parentId\":1102, \"offerCount\":145, \"pskuMappedOfferCount\":45, \"acceptPartnerSkus\":true, \"acceptPartnerModels\":true, \"acceptGoodContent\":true}"
                                + "]",
                        "Запрос категорий с гуд контентом"),
                Arguments.of("?accept_good_content=false",
                        "["
                                + "    {\"categoryId\":1101,\"name\":\"Все товары\", \"acceptGoodContent\":false},"
                                + "    {\"categoryId\":1102,\"name\":\"Электроника\",\"parentId\":1101, \"acceptGoodContent\":false},"
                                + "    {\"categoryId\":1112,\"name\":\"Бытовая химия\",\"parentId\":1101, \"offerCount\":123, \"pskuMappedOfferCount\":23, \"acceptPartnerSkus\":false, \"acceptPartnerModels\":true, \"acceptGoodContent\":false}"
                                + "]",
                        "Запрос категорий без гуд контента"),
                Arguments.of("?tree=false&accept_good_content=true",
                        "["
                                + "    {\"categoryId\":1103,\"name\":\"Мобильные телефоны\",\"parentId\":1102, \"offerCount\":134, \"pskuMappedOfferCount\":34, \"acceptPartnerSkus\":true, \"acceptPartnerModels\":false, \"acceptGoodContent\":true},"
                                + "    {\"categoryId\":1104,\"name\":\"Не мобильные телефоны\",\"parentId\":1102, \"offerCount\":145, \"pskuMappedOfferCount\":45, \"acceptPartnerSkus\":true, \"acceptPartnerModels\":true, \"acceptGoodContent\":true}"
                                + "]",
                        "Запрос категорий с гуд контентом tree=false"),
                Arguments.of("?tree=false&accept_good_content=false",
                        "["
                                + "    {\"categoryId\":1112,\"name\":\"Бытовая химия\",\"parentId\":1101, \"offerCount\":123, \"pskuMappedOfferCount\":23, \"acceptPartnerSkus\":false, \"acceptPartnerModels\":true, \"acceptGoodContent\":false}"
                                + "]",
                        "Запрос категорий без гуд контента tree=false")
        );
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("args")
    void testListCategories(String urlParameters, String expectedJSON, String description) {
        mockSearchMappingCategoriesByShopId();
        String url = String.format("%s%s", shopSkuCategoriesUrl(CAMPAIGN_ID), urlParameters);
        ResponseEntity<String> responseEntity = FunctionalTestHelper.post(url, "{}");
        MatcherAssert.assertThat(
                responseEntity,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyMatches("result", MbiMatchers.jsonEquals(expectedJSON))
                )
        );

        verifyMboMappingService(
                "",
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptyList()
        );
    }

    @Test
    @DisplayName("Запрос списка категорий, когда результат отсутствует")
    void testListCategoriesWithEmptyResult() {
        Mockito.when(patientMboMappingsService.searchMappingCategoriesByShopId(Mockito.any()))
                .thenReturn(
                        SearchMappingCategoriesResponse.newBuilder()
                                .build()
                );
        String url = String.format("%s", shopSkuCategoriesUrl(CAMPAIGN_ID));
        ResponseEntity<String> resultEntity = FunctionalTestHelper.post(url, "{}");
        MatcherAssert.assertThat(
                resultEntity,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyMatches("result",
                                MbiMatchers.jsonEquals(/*language=JSON*/ ""
                                        + "[]"))));

        verifyMboMappingService(
                "",
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptyList()
        );
    }

    @Test
    @DisplayName("Запрос списка категорий, вместе со строкой поиска")
    void testListCategoriesWithQueryString() {
        Mockito.when(patientMboMappingsService.searchMappingCategoriesByShopId(Mockito.any()))
                .thenReturn(
                        SearchMappingCategoriesResponse.newBuilder()
                                .build()
                );
        String url = String.format("%s?q={query}", shopSkuCategoriesUrl(CAMPAIGN_ID));
        FunctionalTestHelper.post(url, "{}", "query123");

        verifyMboMappingService(
                "query123",
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptyList()
        );
    }

    @Test
    @DisplayName("Запрос списка категорий с оферами с указанным статусом")
    void testListCategoriesWithOfferStatus() {
        Mockito.when(patientMboMappingsService.searchMappingCategoriesByShopId(Mockito.any()))
                .thenReturn(
                        SearchMappingCategoriesResponse.newBuilder()
                                .build()
                );
        String url = shopSkuCategoriesUrl(CAMPAIGN_ID);
        //language=json
        String jsonBody = "{\"offerProcessingStatuses\": [\"NEED_CONTENT\"]}";
        FunctionalTestHelper.post(url, jsonBody);

        verifyMboMappingService(
                "",
                Collections.emptySet(),
                Collections.singleton(SupplierOffer.OfferProcessingStatus.NEED_CONTENT),
                Collections.emptySet(),
                Collections.emptyList()
        );
    }

    @Test
    @DisplayName("Запрос списка категорий с оферами с указанными несколькими статусами")
    void testListCategoriesWithMultipleOfferStatus() {
        Mockito.when(patientMboMappingsService.searchMappingCategoriesByShopId(Mockito.any()))
                .thenReturn(
                        SearchMappingCategoriesResponse.newBuilder()
                                .build()
                );
        String url = shopSkuCategoriesUrl(CAMPAIGN_ID);
        //language=json
        String jsonBody = "{\"offerProcessingStatuses\": [\"IN_WORK\", \"CONTENT_PROCESSING\", \"NEED_CONTENT\"]}";
        FunctionalTestHelper.post(url, jsonBody);

        verifyMboMappingService(
                "",
                Collections.emptySet(),
                new HashSet<>(Arrays.asList(
                        SupplierOffer.OfferProcessingStatus.CONTENT_PROCESSING,
                        SupplierOffer.OfferProcessingStatus.NEED_CONTENT,
                        SupplierOffer.OfferProcessingStatus.IN_WORK,
                        SupplierOffer.OfferProcessingStatus.REVIEW
                )),
                Collections.emptySet(),
                Collections.emptyList()
        );
    }

    @Test
    @DisplayName("Запрос списка категорий, который будет включать только подкатегории переданных категорий")
    void testListCategoriesForSpecificSubcategories() {
        Mockito.when(patientMboMappingsService.searchMappingCategoriesByShopId(Mockito.any()))
                .thenReturn(
                        SearchMappingCategoriesResponse.newBuilder()
                                .build()
                );
        String url = shopSkuCategoriesUrl(CAMPAIGN_ID);
        //language=json
        String jsonBody = "{\"categoryIds\": [34, 46]}";
        FunctionalTestHelper.post(url, jsonBody);
        verifyMboMappingService(
                "",
                new HashSet<>(Arrays.asList(34, 46)),
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptyList()
        );
    }

    @Test
    @DisplayName("Запрос списка категорий, для неподдерживаемого статуса офера")
    void testListCategoriesWithUnsupportedOfferMappingStatus() {
        Mockito.when(patientMboMappingsService.searchMappingCategoriesByShopId(Mockito.any()))
                .thenReturn(
                        SearchMappingCategoriesResponse.newBuilder()
                                .build()
                );
        String url = shopSkuCategoriesUrl(CAMPAIGN_ID);
        //language=json
        String jsonBody = "{\"offerProcessingStatuses\": [\"XXSDG\"]}";
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
                                        MbiMatchers.jsonArrayEquals(/*language=JSON*/ "" +
                                                "{\n" +
                                                "  \"code\": \"BAD_PARAM\",\n" +
                                                "  \"details\": {\n" +
                                                "    \"subcode\": \"INVALID\",\n" +
                                                "    \"value\": \"XXSDG\"\n" +
                                                "  }\n" +
                                                "}")
                                )
                        )
                )
        );
    }

    @Test
    @DisplayName("Запрос списка категорий для Market SKU")
    void testListCategoriesWithMSkuMapping() {
        Mockito.when(patientMboMappingsService.searchMappingCategoriesByShopId(Mockito.any()))
                .thenReturn(
                        SearchMappingCategoriesResponse.newBuilder()
                                .build()
                );
        String url = shopSkuCategoriesUrl(CAMPAIGN_ID);
        //language=json
        String jsonBody = "{\"contentType\": \"market\"}";
        ResponseEntity<String> response = FunctionalTestHelper.post(url, jsonBody);

        JsonTestUtil.assertEquals(response, "[]");
    }

    @Test
    @DisplayName("Запрос списка категорий для Partner SKU")
    void testListCategoriesWithPSkuMapping() {
        Mockito.when(patientMboMappingsService.searchMappingCategoriesByShopId(Mockito.any()))
                .thenReturn(
                        SearchMappingCategoriesResponse.newBuilder()
                                .build()
                );
        String url = shopSkuCategoriesUrl(CAMPAIGN_ID);
        //language=json
        String jsonBody = "{\"contentType\": \"partner\"}";
        FunctionalTestHelper.post(url, jsonBody);

        verifyMboMappingService(
                "",
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.singletonList(MboMappings.MappingFilter
                        .newBuilder()
                        .setMappingSkuKind(MboMappings.MappingSkuKind.PARTNER)
                        .build()
                )
        );
    }

    @Test
    @DisplayName("Запрос списка категорий с указанным статусом доступности")
    void testListCategoriesWithSpecificAvailabilityStatus() {
        Mockito.when(patientMboMappingsService.searchMappingCategoriesByShopId(Mockito.any()))
                .thenReturn(
                        SearchMappingCategoriesResponse.newBuilder()
                                .build()
                );
        String url = shopSkuCategoriesUrl(CAMPAIGN_ID);
        //language=json
        String jsonBody = "{\"availabilityStatuses\": [\"INACTIVE\"]}";
        FunctionalTestHelper.post(url, jsonBody);

        verifyMboMappingService(
                "",
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.singleton(SupplierOffer.Availability.INACTIVE),
                Collections.emptyList()
        );
    }

    @Test
    @DisplayName("Запрос списка категорий с указанным списком производителей")
    void testListCategoriesWithVendors() {
        Mockito.when(patientMboMappingsService.searchMappingCategoriesByShopId(Mockito.any()))
                .thenReturn(
                        SearchMappingCategoriesResponse.newBuilder()
                                .build()
                );
        String url = shopSkuCategoriesUrl(CAMPAIGN_ID);
        //language=json
        String jsonBody = "{\"vendors\": [\"Tesla\", \"SpaceX\"]}";
        FunctionalTestHelper.post(url, jsonBody);

        MboMappings.SearchMappingCategoriesRequest request = verifyMboMappingService(
                "",
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptyList()
        );
        Assertions.assertEquals(Arrays.asList("Tesla", "SpaceX"), request.getVendorsList());
    }

    @Test
    @DisplayName("Запрос списка категорий с несколькими статусами доступности")
    void testListCategoriesWithSeveralAvailabilityStatuses() {
        Mockito.when(patientMboMappingsService.searchMappingCategoriesByShopId(Mockito.any()))
                .thenReturn(
                        SearchMappingCategoriesResponse.newBuilder()
                                .build()
                );
        String url = shopSkuCategoriesUrl(CAMPAIGN_ID);
        //language=json
        String jsonBody = "{\"availabilityStatuses\": [\"ACTIVE\", \"DELISTED\"]}";
        FunctionalTestHelper.post(url, jsonBody);

        verifyMboMappingService(
                "",
                Collections.emptySet(),
                Collections.emptySet(),
                ImmutableSet.of(SupplierOffer.Availability.ACTIVE, SupplierOffer.Availability.DELISTED),
                Collections.emptyList()
        );
    }

    @Test
    @DisplayName("Тест флажка, по которому не передается READY_TO_CONTENT_PROCESSING_PARTNER_OFFERS")
    void testListCategoriesWithNotReadyForProcessing() {
        String expectedJSON = "["
                + "    {\"categoryId\":1112,\"name\":\"Бытовая химия\",\"parentId\":1101, \"offerCount\":123, \"pskuMappedOfferCount\":23, \"acceptPartnerSkus\":false, \"acceptPartnerModels\":true, \"acceptGoodContent\":false}"
                + "]";
        mockSearchMappingCategoriesByShopId();
        String body = "{}";
        String url = String.format("%s%s", shopSkuCategoriesUrl(CAMPAIGN_ID),
                "?tree=false&accept_good_content=false&not_ready_for_processing=true");
        ResponseEntity<String> responseEntity = FunctionalTestHelper.post(url, body);
        MatcherAssert.assertThat(
                responseEntity,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyMatches("result", MbiMatchers.jsonEquals(expectedJSON))
                )
        );

        ArgumentCaptor<MboMappings.SearchMappingCategoriesRequest> requestCaptor =
                ArgumentCaptor.forClass(MboMappings.SearchMappingCategoriesRequest.class);

        Mockito.verify(patientMboMappingsService, Mockito.times(1)).searchMappingCategoriesByShopId(requestCaptor.capture());

        MboMappings.SearchMappingCategoriesRequest request = requestCaptor.getAllValues().get(0);

        Assertions.assertEquals(Collections.emptyList(), request.getOfferQueriesAnyOfList());
    }

    private void mockSearchMappingCategoriesByShopId() {
        Mockito.doReturn(
                SearchMappingCategoriesResponse.newBuilder()
                        .addCategories(
                                SearchMappingCategoriesResponse.CategoryInfo.newBuilder()
                                        .setCategoryId(1112)
                                        .setOfferCount(123)
                                        .setOfferMappedOnPskuCount(23)
                                        .build()
                        )
                        .addCategories(
                                SearchMappingCategoriesResponse.CategoryInfo.newBuilder()
                                        .setCategoryId(1103)
                                        .setOfferCount(134)
                                        .setOfferMappedOnPskuCount(34)
                                        .build()
                        )
                        .addCategories(
                                SearchMappingCategoriesResponse.CategoryInfo.newBuilder()
                                        .setCategoryId(1104)
                                        .setOfferCount(145)
                                        .setOfferMappedOnPskuCount(45)
                                        .build()
                        )
                        .build()
        ).when(patientMboMappingsService).searchMappingCategoriesByShopId(Mockito.any());
    }

    private MboMappings.SearchMappingCategoriesRequest verifyMboMappingService(
            String queryText,
            Set<Integer> categoryIds,
            Set<SupplierOffer.OfferProcessingStatus> offerProcessingStatuses,
            Set<SupplierOffer.Availability> availabilityStatuses,
            List<MboMappings.MappingFilter> mappingFilters
    ) {
        ArgumentCaptor<MboMappings.SearchMappingCategoriesRequest> requestCaptor =
                ArgumentCaptor.forClass(MboMappings.SearchMappingCategoriesRequest.class);

        Mockito.verify(patientMboMappingsService, Mockito.times(1)).searchMappingCategoriesByShopId(requestCaptor.capture());

        MboMappings.SearchMappingCategoriesRequest request = requestCaptor.getAllValues().get(0);

        Assertions.assertEquals(SUPPLIER_ID, request.getSupplierId());

        Assertions.assertEquals(categoryIds, new HashSet<>(request.getMarketCategoryIdsList()));

        Assertions.assertEquals(queryText, request.getTextQueryString());

        Assertions.assertEquals(offerProcessingStatuses, new HashSet<>(request.getOfferProcessingStatusList()));

        Assertions.assertEquals(availabilityStatuses, new HashSet<>(request.getAvailabilityList()));

        Assertions.assertEquals(mappingFilters, request.getMappingFiltersList());

        Assertions.assertEquals(Collections.singletonList(MboMappings.OfferQuery.READY_TO_CONTENT_PROCESSING_PARTNER_OFFERS), request.getOfferQueriesAnyOfList());

        return request;
    }
}
