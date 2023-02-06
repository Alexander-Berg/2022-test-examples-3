package ru.yandex.market.ff.dbqueue.consumer.functional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.googlecode.protobuf.format.JsonFormat;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.dbqueue.consumer.ValidateShadowSupplyRequestQueueConsumer;
import ru.yandex.market.ff.model.bo.EnrichmentResultContainer;
import ru.yandex.market.ff.model.dbqueue.ValidateRequestPayload;
import ru.yandex.market.ff.service.ExternalRequestItemErrorService;
import ru.yandex.market.ff.service.RequestItemErrorService;
import ru.yandex.market.ff.service.RequestItemService;
import ru.yandex.market.ff.service.util.RequestItemErrorCollectionsUtils;
import ru.yandex.market.ff.util.CalendaringServiceUtils;
import ru.yandex.market.ff.util.FileContentUtils;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItem;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.Sku;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.Stock;
import ru.yandex.market.fulfillment.stockstorage.client.entity.request.search.SearchSkuFilter;
import ru.yandex.market.fulfillment.stockstorage.client.entity.response.search.ResultPagination;
import ru.yandex.market.fulfillment.stockstorage.client.entity.response.search.SearchSkuResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerCargoTypesDto;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParamGroup;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.mboc.http.MboMappingsForDelivery;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class ValidateShadowSupplyRequestQueueConsumerIntegrationTest extends IntegrationTestWithDbQueueConsumers {

    public static final String EXPECTED_ERROR_VALIDATION_MESSAGE = "Запрещены поставки msku в категории Рожковые, " +
            "накидные, комбинированные ключи #91626 на склад Яндекс.Маркет Ростов #147";
    @Autowired
    private ValidateShadowSupplyRequestQueueConsumer consumer;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private RequestItemErrorService requestItemErrorService;

    @Autowired
    private ExternalRequestItemErrorService externalRequestItemErrorService;

    @Autowired
    private RequestItemService requestItemService;

    @AfterEach
    public void resetMocks() {
        super.resetMocks();
        reset(deliveryParams, stockStorageSearchClient);
    }

    @Test
    @DatabaseSetup("classpath:db-queue/consumer/validate-shadow-supply-request/before.xml")
    @ExpectedDatabase(value = "classpath:db-queue/consumer/validate-shadow-supply-request/after-success.xml",
            assertionMode = NON_STRICT)
    void successValidation() {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse paramsResponse = buildMappingResponse();
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(paramsResponse);
        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(Collections.singletonList(buildSku()),
                        ResultPagination.builder().build(), SearchSkuFilter.empty()));

        var serviceId = 145L;
        mockLmsClient(serviceId);

        TaskExecutionResult result = executeTask();
        assertThat(result).isEqualTo(TaskExecutionResult.finish());
    }


    @Test
    @DatabaseSetup("classpath:db-queue/consumer/validate-shadow-supply-request/before-third-party.xml")
    @ExpectedDatabase(value =
            "classpath:db-queue/consumer/validate-shadow-supply-request/after-success-third-party.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void successValidationForThirdPartyRequest() {
        var mboResponse = loadMboResponseFromJsonFile("two_skus_two_days.json");
        when(deliveryParams.searchFulfillmentSskuParamsForInterval(any())).thenReturn(mboResponse);
        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(Collections.singletonList(buildSku()),
                        ResultPagination.builder().build(), SearchSkuFilter.empty()));
        CalendaringServiceUtils.mockGetSlotsWithoutQuotaCheck(1, csClient);

        mockLmsClient(145L);
        mockLmsClient(147L);
        mockLmsClient(171L);
        mockLmsClient(172L);

        when(lmsClient.getPartnerCargoTypes(Collections.singletonList(171L))).thenReturn(
                Collections.singletonList(new PartnerCargoTypesDto(0L, 0L, Set.of(200, 300, 310)))
        );

        TaskExecutionResult result = executeTask();
        assertThat(result).isEqualTo(TaskExecutionResult.finish());

        assertThatWeCorrectlyRestoredValidationErrorsDataStructure();
    }

    private void assertThatWeCorrectlyRestoredValidationErrorsDataStructure() {
        long requestId = 1;
        var items = requestItemService.findAllByRequestId(requestId);
        var allItemErrors = requestItemErrorService
                .findByRequestIdWithFetchedAttributesAndServiceIds(requestId);
        var allExternalErrors = externalRequestItemErrorService
                .findByRequestIdWithFetchedServiceIds(requestId);

        HashMap<String, EnrichmentResultContainer> articleToErrorsMap =
                RequestItemErrorCollectionsUtils.combineErrorsIntoArticleToErrorsMap(requestId, items, allItemErrors,
                        allExternalErrors);
        articleToErrorsMap.get("PI-GC-14803").getExternalErrors().stream()
                .filter(e -> Long.valueOf(147L).equals(e.getServiceId()))
                .findFirst().ifPresentOrElse(
                res -> assertThat(res)
                        .as("Restoring external validation error message")
                        .hasFieldOrPropertyWithValue("fullErrorMessage", EXPECTED_ERROR_VALIDATION_MESSAGE),
                () -> fail("Validation error message not found"));
    }

    /**
     * Входные данные:
     * - теневая поставка с двумя айтемами
     * - после всех валидаций, кроме валидации квот, доступно только (171, 2018-01-01)
     * - для 171, 2018-01-01 доступно 5 квот вторичной приёмки, товары суммарно занимают 6 (3 + 3)
     * <p>
     * Ожидаем:
     * - запуск мультискладской валидация теневой поставки заканчивается успешно
     * - результат валидации: поставка невалидна
     * - по итогам валидации сохранена 1 ошибка с типом "нет квот" - на второй товар
     */
    @Test
    @DatabaseSetup("classpath:db-queue/consumer/validate-shadow-supply-request/before-third-party-not-enough-quota.xml")
    @ExpectedDatabase(value =
            "classpath:db-queue/consumer/validate-shadow-supply-request/after-invalid-third-party-not-enough-quota.xml",
            query = "select * from request_item_error where error_type = 4",
            table = "query_result",
            assertionMode = NON_STRICT)
    void invalidThirdPartyRequestNotEnoughQuotas() {
        var mboResponse = loadMboResponseFromJsonFile("two_skus_two_days.json");
        when(deliveryParams.searchFulfillmentSskuParamsForInterval(any())).thenReturn(mboResponse);
        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(Collections.singletonList(buildSku()),
                        ResultPagination.builder().build(), SearchSkuFilter.empty()));
        CalendaringServiceUtils.mockGetSlotsWithoutQuotaCheck(1, csClient);

        mockLmsClient(145L);
        mockLmsClient(147L);
        mockLmsClient(171L);
        mockLmsClient(172L);

        when(lmsClient.getPartnerCargoTypes(Collections.singletonList(171L))).thenReturn(
                Collections.singletonList(new PartnerCargoTypesDto(0L, 0L, Set.of(200, 300, 310)))
        );

        TaskExecutionResult result = executeTask();
        assertThat(result).isEqualTo(TaskExecutionResult.finish());
    }

    /**
     * Входные данные:
     * - теневая поставка с двумя айтемами
     * - в мбо нет маппингов для этих айтемов
     * <p>
     * Ожидаем:
     * - запуск мультискладской валидация теневой поставки заканчивается успешно
     * - результат валидации: поставка невалидна
     * - по итогам валидации сохранены 2 ошибки ("нет маппингов") - по одной на товар
     */
    @Test
    @DatabaseSetup("classpath:db-queue/consumer/validate-shadow-supply-request/before-third-party.xml")
    @ExpectedDatabase(value =
            "classpath:db-queue/consumer/validate-shadow-supply-request/after-invalid-third-party.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void invalidThirdPartyRequest() {
        var mboResponse = loadMboResponseFromJsonFile("no_mappings.json");
        when(deliveryParams.searchFulfillmentSskuParamsForInterval(any())).thenReturn(mboResponse);

        TaskExecutionResult result = executeTask();

        assertThat(result).isEqualTo(TaskExecutionResult.finish());
    }

    @Test
    @DatabaseSetup(
            "classpath:db-queue/consumer/validate-shadow-supply-request/before-with-need-confirmation-flag.xml"
    )
    @ExpectedDatabase(
            value = "classpath:db-queue/consumer/validate-shadow-supply-request/after-with-need-confirmation-flag.xml",
            assertionMode = NON_STRICT
    )
    void successValidationForRequestWithNeedConfirmationFlagAsTrue() {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse paramsResponse = buildMappingResponse();
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(paramsResponse);
        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(Collections.singletonList(buildSku()),
                        ResultPagination.builder().build(), SearchSkuFilter.empty()));

        var serviceId = 145L;
        mockLmsClient(serviceId);

        TaskExecutionResult result = executeTask();
        assertThat(result).isEqualTo(TaskExecutionResult.finish());
    }

    private void mockLmsClient(long serviceId) {
        List<PartnerExternalParamGroup> returnedValue = List.of(new PartnerExternalParamGroup(
                serviceId,
                List.of(new PartnerExternalParam(PartnerExternalParamType.IS_CALENDARING_ENABLED.name(), "", "true")))
        );
        when(lmsClient.getPartnerExternalParams(Set.of(PartnerExternalParamType.IS_CALENDARING_ENABLED)))
                .thenReturn(returnedValue);
    }

    private TaskExecutionResult executeTask() {
        ValidateRequestPayload payload = new ValidateRequestPayload(1);
        Task<ValidateRequestPayload> task = new Task<>(new QueueShardId("shard"), payload, 0,
                ZonedDateTime.now(ZoneId.systemDefault()), null, null);
        return transactionTemplate.execute(status -> consumer.execute(task));
    }

    private MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse buildMappingResponse() {

        return MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse.newBuilder()
                .addFulfilmentInfo(
                        MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                                .setSupplierId(1)
                                .setShopSku("SHOPSKU1")
                                .setAllowInbound(true)
                                .build())
                .build();
    }

    private Sku buildSku() {
        return Sku.builder()
                .withUnitId(SSItem.of("SHOPSKU1", 1, 145))
                .withStocks(Collections.singletonList(Stock.of(1, 0, 1, "type")))
                .withEnabled(true)
                .withUpdatable(true)
                .build();
    }

    @SneakyThrows
    private MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse loadMboResponseFromJsonFile(String fileName) {
        String mboResponseJson = FileContentUtils.getFileContent(
                "db-queue/consumer/validate-shadow-supply-request/" + fileName);
        var builder = MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse.newBuilder();
        JsonFormat.merge(mboResponseJson, builder);
        return builder.build();
    }
}
