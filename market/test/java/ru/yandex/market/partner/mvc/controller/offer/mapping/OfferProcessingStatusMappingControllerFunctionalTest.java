package ru.yandex.market.partner.mvc.controller.offer.mapping;

import java.util.Arrays;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasSearchResult;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mbi.util.MoreMbiMatchers;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Тесты для ресурсов маппинга связанных со статусами процессинга оферов.
 */
@ParametersAreNonnullByDefault
@DbUnitDataSet(before = "MappingControllerFunctionalTest.csv")
class OfferProcessingStatusMappingControllerFunctionalTest extends AbstractMappingControllerFunctionalTest {

    private static final long CAMPAIGN_ID = 10774L;
    private static final long SUPPLIER_ID = 774L;

    @Autowired
    private MboMappingsService patientMboMappingsService;

    @Autowired
    private SaasService saasService;


    @Test
    @DisplayName("Получить статистику по интегральным статусам оферов")
    void getIntegralStatuses() {
        SaasSearchResult searchResult = SaasSearchResult.builder().setTotalCount(50).build();
        Mockito.when(saasService.searchBusinessOffers(Mockito.any())).thenReturn(searchResult);

        String url = shopSkuOfferIntegralStatusesUrl(10776L);
        ResponseEntity<String> resultEntity = FunctionalTestHelper.get(url);

        JSONAssert.assertEquals("" +
                        "{" +
                        "\"published\":50, " +
                        "\"notPublished\":0, " +
                        "\"notPublishedNeedInfo\":0, " +
                        "\"total\":50" +
                        "}",
                new JSONObject(resultEntity.getBody()).getJSONObject("result"), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DisplayName("Запрос статусов без архивных маппингов")
    void getStatusWithoutDelisted() {
        Mockito.when(patientMboMappingsService.searchOfferProcessingStatusesByShopId(Mockito.any()))
                .thenReturn(MboMappings.SearchOfferProcessingStatusesResponse.newBuilder()
                        .setStatus(MboMappings.SearchOfferProcessingStatusesResponse.Status.OK)
                        .addOfferProcessingStatuses(
                                MboMappings.SearchOfferProcessingStatusesResponse.OfferProcessingStatusInfo.newBuilder()
                                        .setOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.NEED_CONTENT)
                                        .setOfferCount(123)
                                        .build()
                        )
                        .build()
                );

        String url = String.format("%s?q=abc&category_id=101&availability=ACTIVE,INACTIVE",
                shopSkuOfferProcessingStatusesUrl(CAMPAIGN_ID));
        ResponseEntity<String> resultEntity = FunctionalTestHelper.get(url);

        JSONAssert.assertEquals(/*language=JSON*/ "{\n" +
                        "  \"offerProcessingStatusStats\": [\n" +
                        "    {\n" +
                        "      \"offerProcessingStatus\": \"NEED_CONTENT\",\n" +
                        "      \"count\": 123\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}",
                new JSONObject(resultEntity.getBody()).getJSONObject("result"), JSONCompareMode.NON_EXTENSIBLE);

        Mockito.verify(patientMboMappingsService)
                .searchOfferProcessingStatusesByShopId(ArgumentMatchers.argThat(request -> {
                    return request.hasSupplierId()
                            && request.getSupplierId() == SUPPLIER_ID
                            && !request.hasHasAnyMapping()
                            && request.getMarketCategoryIdsList().equals(List.of(101))
                            && request.getTextQueryString().equals("abc")
                            && !request.getAvailabilityList().isEmpty();
                }));
    }

    @Test
    @DisplayName("Запрос статистики по всем статусам")
    void testGetAllStatuses() {
        mockMboResponse();
        String url = String.format("%s?q=abc&category_id=101&category_id=102",
                shopSkuOfferProcessingStatusesUrl(CAMPAIGN_ID));
        ResponseEntity<String> resultEntity = FunctionalTestHelper.get(url);

        JSONAssert.assertEquals(/*language=JSON*/ "{\n" +
                        "  \"offerProcessingStatusStats\": [\n" +
                        "    {\n" +
                        "      \"offerProcessingStatus\": \"REJECTED\",\n" +
                        "      \"count\": 3\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"offerProcessingStatus\": \"READY\",\n" +
                        "      \"count\": 11\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}",
                new JSONObject(resultEntity.getBody()).getJSONObject("result"), JSONCompareMode.NON_EXTENSIBLE);

        Mockito.verify(patientMboMappingsService)
                .searchOfferProcessingStatusesByShopId(ArgumentMatchers.argThat(request -> {
                    return request.hasSupplierId()
                            && request.getSupplierId() == SUPPLIER_ID
                            && !request.hasHasAnyMapping()
                            && request.getMarketCategoryIdsList().equals(Arrays.asList(101, 102))
                            && request.getTextQueryString().equals("abc")
                            && request.getAvailabilityList().isEmpty();
                }));
    }

    @Test
    @DisplayName("Запрос сатистики по нескольким статусам: IN_WORK и CONTENT_PROCESSING")
    void testGetMultipleStatus() {
        Mockito.when(patientMboMappingsService.searchOfferProcessingStatusesByShopId(Mockito.any()))
                .thenReturn(MboMappings.SearchOfferProcessingStatusesResponse.newBuilder()
                        .setStatus(MboMappings.SearchOfferProcessingStatusesResponse.Status.OK)
                        .addOfferProcessingStatuses(
                                MboMappings.SearchOfferProcessingStatusesResponse.OfferProcessingStatusInfo.newBuilder()
                                        .setOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.REVIEW)
                                        .setOfferCount(3)
                                        .build()
                        )
                        .addOfferProcessingStatuses(
                                MboMappings.SearchOfferProcessingStatusesResponse.OfferProcessingStatusInfo.newBuilder()
                                        .setOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.IN_WORK)
                                        .setOfferCount(5)
                                        .build()
                        )
                        .addOfferProcessingStatuses(
                                MboMappings.SearchOfferProcessingStatusesResponse.OfferProcessingStatusInfo.newBuilder()
                                        .setOfferProcessingStatus(
                                                SupplierOffer.OfferProcessingStatus.CONTENT_PROCESSING)
                                        .setOfferCount(11)
                                        .build()
                        )
                        .addOfferProcessingStatuses(
                                MboMappings.SearchOfferProcessingStatusesResponse.OfferProcessingStatusInfo.newBuilder()
                                        .setOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.REJECTED)
                                        .setOfferCount(3)
                                        .build()
                        )
                        .build()
                );
        String url = String.format(
                "%s?offer_processing_status=IN_WORK&offer_processing_status=CONTENT_PROCESSING",
                shopSkuOfferProcessingStatusesUrl(CAMPAIGN_ID)
        );
        ResponseEntity<String> resultEntity = FunctionalTestHelper.get(url);
        MatcherAssert.assertThat(resultEntity, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyMatches(
                "result",
                MbiMatchers.jsonEquals(/*language=JSON*/ ""
                        + "{\n"
                        + "  \"offerProcessingStatusStats\": [\n"
                        + "    {\n"
                        + "      \"offerProcessingStatus\": \"IN_WORK\",\n"
                        + "      \"count\": 8\n"
                        + "    },\n"
                        + "    {\n"
                        + "      \"offerProcessingStatus\": \"CONTENT_PROCESSING\",\n"
                        + "      \"count\": 11\n"
                        + "    }\n"
                        + "  ]\n"
                        + "}")
        )));

        Mockito.verify(patientMboMappingsService)
                .searchOfferProcessingStatusesByShopId(ArgumentMatchers.argThat(request -> {
                    return request.hasSupplierId()
                            && request.getSupplierId() == SUPPLIER_ID
                            && !request.hasHasAnyMapping()
                            && request.getMarketCategoryIdsCount() == 0
                            && !request.hasTextQueryString()
                            && request.getAvailabilityList().isEmpty();
                }));
    }

    @Test
    @DisplayName("Запрос сатистики по статусу READY")
    void testGetOneStatus() {
        mockMboResponse();
        String url = String.format("%s?offer_processing_status=READY", shopSkuOfferProcessingStatusesUrl(CAMPAIGN_ID));
        ResponseEntity<String> resultEntity = FunctionalTestHelper.get(url);
        MatcherAssert.assertThat(
                resultEntity,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyMatches("result",
                                MbiMatchers.jsonEquals(/*language=JSON*/ ""
                                        + "{\n" +
                                        "  \"offerProcessingStatusStats\": [\n" +
                                        "    {\n" +
                                        "      \"offerProcessingStatus\": \"READY\",\n" +
                                        "      \"count\": 11\n" +
                                        "    }\n" +
                                        "  ]\n" +
                                        "}"))));

        Mockito.verify(patientMboMappingsService)
                .searchOfferProcessingStatusesByShopId(ArgumentMatchers.argThat(request -> request.hasSupplierId()
                        && request.getSupplierId() == SUPPLIER_ID
                        && !request.hasHasAnyMapping()
                        && request.getMarketCategoryIdsCount() == 0
                        && !request.hasTextQueryString()
                        && request.getAvailabilityList().isEmpty()));
    }

    @Test
    @DisplayName("Тест для неподдерживаемого статуса процессинга офера")
    void testErrorOnIncorrectOfferProcessingStatus() {
        String url = String.format("%s?offer_processing_status={status}",
                shopSkuOfferProcessingStatusesUrl(CAMPAIGN_ID));
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(url, "XXX")
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
                                                + "    \"message\":\"Unsupported value XXX\","
                                                + "    \"details\":{"
                                                + "        \"field\":\"offer_processing_status\","
                                                + "        \"subcode\":\"INVALID\""
                                                + "    }"
                                                + "}")
                                )
                        )
                )
        );
    }

    @Test
    @DisplayName("Тест ошибки при статусе ответа \"ошибка\" от mboc")
    void testGetStatusesErrorOnMbocErrorStatus() {
        Mockito.when(patientMboMappingsService.searchOfferProcessingStatusesByShopId(Mockito.any()))
                .thenReturn(MboMappings.SearchOfferProcessingStatusesResponse.newBuilder()
                        .setStatus(MboMappings.SearchOfferProcessingStatusesResponse.Status.ERROR)
                        .build()
                );

        String url = String.format("%s", shopSkuOfferProcessingStatusesUrl(CAMPAIGN_ID));
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(url)
        );
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    private void mockMboResponse() {
        Mockito.when(patientMboMappingsService.searchOfferProcessingStatusesByShopId(Mockito.any()))
                .thenReturn(MboMappings.SearchOfferProcessingStatusesResponse.newBuilder()
                        .setStatus(MboMappings.SearchOfferProcessingStatusesResponse.Status.OK)
                        .addOfferProcessingStatuses(
                                MboMappings.SearchOfferProcessingStatusesResponse.OfferProcessingStatusInfo.newBuilder()
                                        .setOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.READY)
                                        .setOfferCount(11)
                                        .build()
                        )
                        .addOfferProcessingStatuses(
                                MboMappings.SearchOfferProcessingStatusesResponse.OfferProcessingStatusInfo.newBuilder()
                                        .setOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.REJECTED)
                                        .setOfferCount(3)
                                        .build()
                        )
                        .build()
                );
    }
}
