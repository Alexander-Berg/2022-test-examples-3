package ru.yandex.market.ff.dbqueue.consumer.functional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.dbqueue.consumer.ValidateSupplyRequestQueueConsumer;
import ru.yandex.market.ff.model.dbqueue.ValidateRequestPayload;
import ru.yandex.market.ff.service.LogisticManagementService;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItem;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.Sku;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.Stock;
import ru.yandex.market.fulfillment.stockstorage.client.entity.request.search.SearchSkuFilter;
import ru.yandex.market.fulfillment.stockstorage.client.entity.response.StockFreezingResponse;
import ru.yandex.market.fulfillment.stockstorage.client.entity.response.search.ResultPagination;
import ru.yandex.market.fulfillment.stockstorage.client.entity.response.search.SearchSkuResponse;
import ru.yandex.market.mboc.http.MboMappingsForDelivery;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class ValidateSupplyRequestQueueConsumerIntegrationTest extends IntegrationTestWithDbQueueConsumers {

    @Autowired
    private ValidateSupplyRequestQueueConsumer consumer;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private LogisticManagementService logisticManagementService;

    @AfterEach
    public void resetMocks() {
        super.resetMocks();
        reset(deliveryParams, stockStorageSearchClient, logisticManagementService);
    }

    @Test
    @DatabaseSetup("classpath:db-queue/consumer/validate-supply-request/before.xml")
    @ExpectedDatabase(value = "classpath:db-queue/consumer/validate-supply-request/after-failure.xml",
            assertionMode = NON_STRICT)
    public void hibernateChangesRolledBackInCaseOfMboException() {
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenThrow(new RuntimeException("Connection timeout"));
        TaskExecutionResult result = executeTask();
        assertThat(result).isEqualTo(TaskExecutionResult.fail());
    }

    @Test
    @DatabaseSetup("classpath:db-queue/consumer/validate-supply-request/before.xml")
    @ExpectedDatabase(value = "classpath:db-queue/consumer/validate-supply-request/after-success.xml",
            assertionMode = NON_STRICT)
    public void allChangesFlushedInCaseOfSuccess() {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse paramsResponse = buildMappingResponse();
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(paramsResponse);
        when(stockStorageOutboundClient.freezeStocks(any(), anyList()))
                .thenReturn(new StockFreezingResponse());
        when(stockStorageSearchClient.searchSku(any()))
                .thenReturn(SearchSkuResponse.of(Collections.singletonList(buildSku()),
                        ResultPagination.builder().build(), SearchSkuFilter.empty()));
        when(logisticManagementService.getXDocSupplyAdditionalDateInterval(anyLong(), anyLong()))
                .thenReturn(Optional.of(10L));
        TaskExecutionResult result = executeTask();
        assertThat(result).isEqualTo(TaskExecutionResult.finish());
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
}
