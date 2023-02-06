package ru.yandex.market.partner.mvc.controller.offer.mapping;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.ByteString;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.ir.http.MboRobot;
import ru.yandex.market.ir.http.PartnerContent;
import ru.yandex.market.ir.http.PartnerContentService;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Функциональные тесты на на {@link ru.yandex.market.partner.mvc.controller.offer.content.PartnerContentController}.
 */
@ParametersAreNonnullByDefault
@DbUnitDataSet(before = "MappingControllerFunctionalTest.csv")
class PartnerContentServiceFunctionalTest extends AbstractMappingControllerFunctionalTest {
    private static final int USER_ID = 10;
    private static final int SOURCE_ID = 1;
    private static final int CATEGORY_ID = 123;
    private static final String SKU = "H123";
    private static final String SKU_2 = "H124";
    private static final String SKU_WITH_PSKU_MAPPING = "P321";

    @Autowired
    private MboMappingsService patientMboMappingsService;

    @Autowired
    private PartnerContentService marketProtoPartnerContentService;

    private static SupplierOffer.Offer skuToOffer(String skuId) {
        return SupplierOffer.Offer.newBuilder()
                .setTitle("Test " + skuId)
                .setSupplierId(SUPPLIER_ID)
                .setShopSkuId(skuId)
                .build();
    }

    @Test
    @DisplayName("Получить xls шаблон из контент сервиса")
    void testQueryByShopSku() throws IOException {
        prepareMocks(Collections.singletonList(SKU), PartnerContent.FileContentType.BETTER_XLS);

        String url = String.format("%s?_user_id={userId}",
                shopSkuCategoryContentTemplateUrl(CAMPAIGN_ID, CATEGORY_ID)
        );

        ResponseEntity<byte[]> responseEntity = FunctionalTestHelper.post(url, "{}", byte[].class, USER_ID);

        Assertions.assertEquals(responseEntity.getBody().length, 509440);
        Mockito.verify(marketProtoPartnerContentService).addSource(
                Mockito.argThat(req -> req.getShopId() == SUPPLIER_ID && req.getAuthorUid() == USER_ID)
        );
        Mockito.verify(marketProtoPartnerContentService).getFileTemplate(Mockito.argThat(request ->
                request.getCategoryId() == CATEGORY_ID
                        && request.getShopSkuIdList().equals(Collections.singletonList(SKU))));

        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                matchSearchMappingsBySupplierIdRequest(
                        request,
                        Collections.emptySet(),
                        Collections.emptySet(),
                        ""
                ))
        );
    }

    @Test
    @DisplayName("Получить xls шаблон из контент сервиса с фильтром по гуд контенту")
    void testQueryByShopSkuWithGoodContent() throws IOException {
        prepareMocks(Collections.singletonList(SKU), PartnerContent.FileContentType.GOOD_XLS);

        String url = String.format("%s?_user_id={userId}",
                shopSkuCategoryContentTemplateUrl(CAMPAIGN_ID, CATEGORY_ID)
        );

        //language=json
        String jsonBody = "{\"acceptGoodContent\": true}";

        ResponseEntity<byte[]> responseEntity = FunctionalTestHelper.post(url, jsonBody, byte[].class, USER_ID);

        Assertions.assertEquals(responseEntity.getBody().length, 509440);
        Mockito.verify(marketProtoPartnerContentService).addSource(
                Mockito.argThat(req -> req.getShopId() == SUPPLIER_ID && req.getAuthorUid() == USER_ID)
        );
        Mockito.verify(marketProtoPartnerContentService)
                .getFileTemplate(Mockito.argThat(request ->
                        request.getCategoryId() == CATEGORY_ID
                                && request.getShopSkuIdList().equals(Collections.singletonList(SKU))
                                && request.getFileContentType().equals(PartnerContent.FileContentType.GOOD_XLS)));

        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                matchSearchMappingsBySupplierIdRequest(
                        request,
                        Collections.emptySet(),
                        Collections.emptySet(),
                        ""
                ))
        );
    }

    @Test
    @DisplayName("Получить xls шаблон из контент сервиса с фильтром по статусу")
    void testQueryByShopSkuWithStatusFilter() throws IOException {
        prepareMocks(Collections.singletonList(SKU), PartnerContent.FileContentType.BETTER_XLS);

        String url = String.format(
                "%s?_user_id={userId}",
                shopSkuCategoryContentTemplateUrl(CAMPAIGN_ID, CATEGORY_ID)
        );

        //language=json
        String jsonBody = "{\"offerProcessingStatuses\": [\"NEED_CONTENT\", \"NEED_MAPPING\"]}";

        FunctionalTestHelper.post(url, jsonBody, byte[].class, USER_ID);

        Set<SupplierOffer.OfferProcessingStatus> expectedChangeOfferStatusSet = ImmutableSet.of(SupplierOffer.OfferProcessingStatus.NEED_CONTENT, SupplierOffer.OfferProcessingStatus.NEED_MAPPING);
        Mockito.verify(marketProtoPartnerContentService).addSource(
                Mockito.argThat(req -> req.getShopId() == SUPPLIER_ID && req.getAuthorUid() == USER_ID)
        );

        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                matchSearchMappingsBySupplierIdRequest(
                        request,
                        expectedChangeOfferStatusSet,
                        Collections.emptySet(),
                        ""
                ))
        );
    }

    @Test
    @DisplayName("Получить xls шаблон из контент сервиса со строкой поиска")
    void testQueryByShopSkuWithQueryString() throws IOException {
        prepareMocks(Collections.singletonList(SKU), PartnerContent.FileContentType.BETTER_XLS);

        String url = String.format("%s?_user_id={userId}&q=test123",
                shopSkuCategoryContentTemplateUrl(CAMPAIGN_ID, CATEGORY_ID)
        );

        FunctionalTestHelper.post(url, "{}", byte[].class, USER_ID);

        Mockito.verify(marketProtoPartnerContentService).addSource(
                Mockito.argThat(req -> req.getShopId() == SUPPLIER_ID && req.getAuthorUid() == USER_ID)
        );

        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                matchSearchMappingsBySupplierIdRequest(
                        request,
                        Collections.emptySet(),
                        Collections.emptySet(),
                        "test123"
                ))
        );
    }

    @Test
    @DisplayName("Получить xls шаблон из контент сервиса для множества оферов")
    void testQueryByShopManyOffersSku() throws IOException {
        prepareMocks(Arrays.asList(SKU, SKU_2), PartnerContent.FileContentType.BETTER_XLS);

        String url = String.format("%s?_user_id={userId}",
                shopSkuCategoryContentTemplateUrl(CAMPAIGN_ID, CATEGORY_ID)
        );

        ResponseEntity<byte[]> responseEntity = FunctionalTestHelper.post(url, "{}", byte[].class, USER_ID);

        verifyFileTemplate(responseEntity, ImmutableSet.of(SKU, SKU_2));

        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                matchSearchMappingsBySupplierIdRequest(
                        request,
                        Collections.emptySet(),
                        Collections.emptySet(),
                        ""
                ))
        );
    }

    @Test
    @DisplayName("Получить xls шаблон из контент сервиса для пустого множества оферов")
    void testQueryByShopNoOffersSku() throws IOException {
        prepareMocks(Collections.emptyList(), PartnerContent.FileContentType.BETTER_XLS);

        String url = String.format("%s?_user_id={userId}&",
                shopSkuCategoryContentTemplateUrl(CAMPAIGN_ID, CATEGORY_ID)
        );

        ResponseEntity<byte[]> responseEntity = FunctionalTestHelper.post(url, "{}", byte[].class, USER_ID);

        verifyFileTemplate(responseEntity, Collections.emptySet());

        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                matchSearchMappingsBySupplierIdRequest(
                        request,
                        Collections.emptySet(),
                        Collections.emptySet(),
                        ""
                ))
        );
    }

    @Test
    @DisplayName("Получить xls шаблон из контент сервиса для статуса READY")
    void testQueryByReadyStatus() throws IOException {
        prepareMocks(Collections.singletonList(SKU_WITH_PSKU_MAPPING), PartnerContent.FileContentType.BETTER_XLS);

        String url = String.format("%s?_user_id={userId}",
                shopSkuCategoryContentTemplateUrl(CAMPAIGN_ID, CATEGORY_ID)
        );

        //language=json
        String jsonBody = "{\"offerProcessingStatuses\": [\"READY\"]}";

        ResponseEntity<byte[]> responseEntity = FunctionalTestHelper.post(url, jsonBody, byte[].class, USER_ID);

        verifyFileTemplate(responseEntity, Collections.singleton(SKU_WITH_PSKU_MAPPING));

        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                matchSearchMappingsBySupplierIdRequest(
                        request,
                        Collections.singleton(SupplierOffer.OfferProcessingStatus.READY),
                        Collections.emptySet(),
                        ""
                ))
        );
    }

    @Test
    @DisplayName("Получить xls шаблон из контент сервиса для статусов READY и NEED_CONTENT")
    void testQueryByReadyAndNeedContentStatuses() throws IOException {
        prepareMocks(Arrays.asList(SKU, SKU_WITH_PSKU_MAPPING), PartnerContent.FileContentType.BETTER_XLS);

        String url = String.format("%s?_user_id={userId}",
                shopSkuCategoryContentTemplateUrl(CAMPAIGN_ID, CATEGORY_ID)
        );

        //language=json
        String jsonBody = "{\"offerProcessingStatuses\": [\"READY\", \"NEED_CONTENT\"]}";

        ResponseEntity<byte[]> responseEntity = FunctionalTestHelper.post(url, jsonBody, byte[].class, USER_ID);

        verifyFileTemplate(responseEntity, ImmutableSet.of(SKU, SKU_WITH_PSKU_MAPPING));

        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                matchSearchMappingsBySupplierIdRequest(
                        request,
                        ImmutableSet.of(SupplierOffer.OfferProcessingStatus.READY, SupplierOffer.OfferProcessingStatus.NEED_CONTENT),
                        Collections.emptySet(),
                        ""
                ))
        );
    }

    @Test
    @DisplayName("Выдать ошибку при INTERNAL_ERROR контент сервиса")
    void testErrorOnContentStatusNotOk() {
        mockSearchMappings(Arrays.asList(SKU, SKU_WITH_PSKU_MAPPING));

        Mockito.when(marketProtoPartnerContentService.getFileTemplate(Mockito.any()))
                .thenReturn(PartnerContent.GetFileTemplateResponse.newBuilder()
                        .setStatus(PartnerContent.GetFileTemplateResponse.GenerationStatus.INTERNAL_ERROR)
                        .setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        .build());

        String url = String.format("%s?_user_id={userId}", shopSkuCategoryContentTemplateUrl(CAMPAIGN_ID, CATEGORY_ID));

        HttpServerErrorException exception = Assertions.assertThrows(
                HttpServerErrorException.class,
                () -> FunctionalTestHelper.post(url, "{}", USER_ID)
        );
        Mockito.verify(marketProtoPartnerContentService).addSource(
                Mockito.argThat(req -> req.getShopId() == SUPPLIER_ID && req.getAuthorUid() == USER_ID)
        );
        Assertions.assertEquals(exception.getStatusCode(), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("Выдать ошибку при статусе GROUPED_CATEGORY")
    void testErrorOnGroupedCategoryStatus() {
        mockSearchMappings(Arrays.asList(SKU, SKU_WITH_PSKU_MAPPING));
        Mockito.when(marketProtoPartnerContentService.getFileTemplate(Mockito.any()))
                .thenReturn(PartnerContent.GetFileTemplateResponse.newBuilder()
                        .setStatus(PartnerContent.GetFileTemplateResponse.GenerationStatus.GROUPED_CATEGORY)
                        .setErrorMessage("Category is grouped")
                        .setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        .build());

        String url = String.format("%s?_user_id={userId}", shopSkuCategoryContentTemplateUrl(CAMPAIGN_ID, CATEGORY_ID));

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(url, "{}", USER_ID)
        );
        Mockito.verify(marketProtoPartnerContentService).addSource(
                Mockito.argThat(req -> req.getShopId() == SUPPLIER_ID && req.getAuthorUid() == USER_ID)
        );
        MatcherAssert.assertThat(
                exception,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyMatches("errors", MbiMatchers.jsonEquals(/*language=JSON*/ ""
                                        + "["
                                        + "    {"
                                        + "        \"code\":\"INVALID_CATEGORY\","
                                        + "        \"message\":\"Category is grouped\","
                                        + "        \"details\":{"
                                        + "            \"reason\":\"GROUPED_CATEGORY\","
                                        + "            \"category_id\":123"
                                        + "        }"
                                        + "    }"
                                        + "]")
                                )
                        )
                )
        );
    }

    @Test
    @DisplayName("Получить xls шаблон из контент сервиса для статуса доступности ACTIVE")
    void testQueryByActiveAvailabilityStatus() throws IOException {
        prepareMocks(Arrays.asList(SKU, SKU_WITH_PSKU_MAPPING), PartnerContent.FileContentType.BETTER_XLS);

        String url = String.format(
                "%s?_user_id={userId}",
                shopSkuCategoryContentTemplateUrl(CAMPAIGN_ID, CATEGORY_ID)
        );

        //language=json
        String jsonBody = "{\"availabilityStatuses\": [\"ACTIVE\"]}";
        ResponseEntity<byte[]> responseEntity = FunctionalTestHelper.post(url, jsonBody, byte[].class, USER_ID);

        verifyFileTemplate(responseEntity, ImmutableSet.of(SKU, SKU_WITH_PSKU_MAPPING));

        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                matchSearchMappingsBySupplierIdRequest(
                        request,
                        Collections.emptySet(),
                        Collections.singleton(SupplierOffer.Availability.ACTIVE),
                        ""
                ))
        );
    }

    @Test
    @DisplayName("Получить xls шаблон из контент сервиса для статуса доступности INACTIVE и DELISTED")
    void testQueryByInactiveAndDelistedAvailabilityStatuses() throws IOException {
        prepareMocks(Arrays.asList(SKU, SKU_WITH_PSKU_MAPPING), PartnerContent.FileContentType.BETTER_XLS);

        String url = String.format(
                "%s?_user_id={userId}&availability_status=INACTIVE&availability_status=DELISTED",
                shopSkuCategoryContentTemplateUrl(CAMPAIGN_ID, CATEGORY_ID)
        );

        //language=json
        String jsonBody = "{\"availabilityStatuses\": [\"INACTIVE\", \"DELISTED\"]}";

        ResponseEntity<byte[]> responseEntity = FunctionalTestHelper.post(url, jsonBody, byte[].class, USER_ID);

        verifyFileTemplate(responseEntity, ImmutableSet.of(SKU, SKU_WITH_PSKU_MAPPING));

        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                matchSearchMappingsBySupplierIdRequest(
                        request,
                        Collections.emptySet(),
                        ImmutableSet.of(SupplierOffer.Availability.INACTIVE, SupplierOffer.Availability.DELISTED),
                        ""
                ))
        );
    }

    private void mockSearchMappings(Collection<String> skuIds) {
        Mockito.doReturn(
                MboMappings.SearchMappingsResponse.newBuilder()
                        .addAllOffers(
                                skuIds.stream()
                                        .map(PartnerContentServiceFunctionalTest::skuToOffer)
                                        .collect(Collectors.toList())
                        )
                        .setTotalCount(skuIds.size())
                        .build()
        ).when(patientMboMappingsService).searchMappingsByShopId(Mockito.any());

        Mockito.when(marketProtoPartnerContentService.addSource(Mockito.any())).thenReturn(
                MboRobot.AddSourceResponse.newBuilder()
                        .setSourceId(SOURCE_ID)
                        .setAddingSourceResult(MboRobot.AddSourceResponse.Result.OK)
                        .build()
        );
    }

    private void prepareMocks(Collection<String> skuIds, PartnerContent.FileContentType contentType) throws IOException {
        mockSearchMappings(skuIds);

        Set<String> expectedSKUs = new HashSet<>(skuIds);

        Mockito.when(marketProtoPartnerContentService.getFileTemplate(ArgumentMatchers.argThat(request ->
                request.getCategoryId() == CATEGORY_ID
                        && request.getSourceId() == SOURCE_ID
                        && new HashSet<>(request.getShopSkuIdList()).equals(expectedSKUs)
                        && request.getFileContentType() == contentType)))
                .thenReturn(PartnerContent.GetFileTemplateResponse.newBuilder()
                        .setStatus(PartnerContent.GetFileTemplateResponse.GenerationStatus.OK)
                        .setContent(ByteString.readFrom(
                                new ClassPathResource("supplier/feed/Stock_xls-sku.xls").getInputStream()))
                        .setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        .build());
    }

    private boolean matchSearchMappingsBySupplierIdRequest(
            MboMappings.SearchMappingsBySupplierIdRequest request,
            Set<SupplierOffer.OfferProcessingStatus> offerProcessingStatuses,
            Set<SupplierOffer.Availability> availabilityStatuses,
            String textQuery
    ) {
        return request.hasSupplierId()
                && request.getSupplierId() == SUPPLIER_ID
                && request.hasLimit()
                && request.getLimit() == 1000
                && new HashSet<>(request.getOfferProcessingStatusList()).equals(offerProcessingStatuses)
                && new HashSet<>(request.getAvailabilityList()).equals(availabilityStatuses)
                && request.getHasNoSupplierMappingStatusList().isEmpty()
                && request.getMappingFiltersList().isEmpty()
                && Collections.singletonList(CATEGORY_ID).equals(request.getMarketCategoryIdsList())
                && request.getTextQueryString().equals(textQuery)
                && !request.hasOffsetKey()
                && request.hasReturnTotalCount()
                && !request.getReturnTotalCount()
                && !request.getReturnMasterData()
                && request.getOfferQueriesAnyOfList().equals(Collections.singletonList(MboMappings.OfferQuery.READY_TO_CONTENT_PROCESSING_PARTNER_OFFERS));
    }

    private void verifyFileTemplate(ResponseEntity<byte[]> responseEntity, Set<String> expectedSKUs) {
        Assertions.assertEquals(responseEntity.getBody().length, 509440);
        Mockito.verify(marketProtoPartnerContentService).addSource(
                Mockito.argThat(req -> req.getShopId() == SUPPLIER_ID && req.getAuthorUid() == USER_ID)
        );
        Mockito.verify(marketProtoPartnerContentService).getFileTemplate(
                Mockito.argThat(request ->
                        request.getCategoryId() == CATEGORY_ID
                                && new HashSet<>(request.getShopSkuIdList()).equals(expectedSKUs)
                )
        );
    }
}
