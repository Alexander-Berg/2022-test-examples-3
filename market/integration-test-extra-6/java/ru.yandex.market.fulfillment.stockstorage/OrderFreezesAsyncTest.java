package ru.yandex.market.fulfillment.stockstorage;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.fulfillment.stockstorage.configuration.AsyncTestConfiguration;
import ru.yandex.market.fulfillment.stockstorage.domain.dto.FreezeData;
import ru.yandex.market.fulfillment.stockstorage.domain.dto.FreezingMeta;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.FreezeReason;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.FreezeReasonType;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.StockType;
import ru.yandex.market.fulfillment.stockstorage.service.export.rty.availability.SkuChangeAvailabilityMessageProducer;
import ru.yandex.market.fulfillment.stockstorage.service.export.rty.stocks.SkuChangeStocksAmountMessageProducer;
import ru.yandex.market.fulfillment.stockstorage.service.export.rty.strategy.stocks.DefaultSkuChangeStocksAmountProducingStrategy;
import ru.yandex.market.fulfillment.stockstorage.util.AsyncWaiterService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.fulfillment.stockstorage.client.StockStorageOrderRestClient.ORDER;

@Import(AsyncTestConfiguration.class)
@DatabaseSetup("classpath:database/states/system_property.xml")
public class OrderFreezesAsyncTest extends AbstractContextualTest {

    @Autowired
    private AsyncWaiterService asyncWaiterService;

    @SpyBean
    private SkuChangeAvailabilityMessageProducer skuChangeAvailabilityMessageProducer;

    @SpyBean
    private SkuChangeStocksAmountMessageProducer skuChangeStocksAmountMessageProducer;

    @SpyBean
    private DefaultSkuChangeStocksAmountProducingStrategy skuChangeDataProducingStrategy;

    @AfterEach
    @Override
    public void resetMocks() {
        super.resetMocks();
        Mockito.reset(skuChangeDataProducingStrategy);
        Mockito.reset(skuChangeAvailabilityMessageProducer);
        Mockito.reset(skuChangeStocksAmountMessageProducer);
    }

    /**
     * Стоки успешно зарефризятся, все таски будут добавлены в очередь в асинхронном режиме
     */
    @Test
    @DatabaseSetup("classpath:database/states/stocks_frozen_two_orders.xml")
    @ExpectedDatabase(value = "classpath:database/expected/freeze/refreezed_by_second_order.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void refreezeStocksSuccessful() {
        testRefreeze();
    }

    /**
     * Стоки успешно зарефризятся несмотря на ошибку, произошедушую при заведении новой таски в очередь
     * Ошибка только для CHANGED_STOCKS_AMOUNT_EVENT,
     * при этом CHANGED_AVAILABILITY_EVENT, CHANGED_ANY_TYPE_OF_STOCKS_AMOUNT_EVENT и все записи event_audit будут добавлены
     */
    @Test
    @DatabaseSetup("classpath:database/states/stocks_frozen_two_orders.xml")
    @ExpectedDatabase(value = "classpath:database/expected/freeze/" +
            "refreezed_by_second_order_with_failed_push_to_queue.xml", assertionMode = NON_STRICT_UNORDERED)
    public void refreezeStocksSuccessfulWithFailedPushToQueue() {
        doThrow(new RuntimeException("weee"))
                .when(skuChangeDataProducingStrategy)
                .push(anyList());

        testRefreeze();
    }

    public void testRefreeze() {
        sendRefreezeRequest(
                "123456",
                ImmutableMap.of("sku1", FreezeData.of(80, false)),
                1,
                status().is2xxSuccessful());

        verify(skuEventAuditService).logStockFreeze(anyList());
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("123456", FreezeReasonType.ORDER), StockType.FIT,
                                null, 1)),
                        anyMap()
                );

        asyncWaiterService.awaitTasks();
        verify(stockEventsHandler, times(3)).handle(anyList());
        verify(skuChangeAvailabilityMessageProducer, times(1)).produceIfNecessaryAsync(anyList());
        verify(skuChangeStocksAmountMessageProducer, times(1)).produceIfNecessaryAsync(anySet());
    }

    private MvcResult sendRefreezeRequest(String orderId, Map<String, FreezeData> items, long version,
                                          ResultMatcher... matchers) {
        return sendRefreezeRequest(orderId, items, version, 1, matchers);
    }

    private MvcResult sendRefreezeRequest(String orderId, Map<String, FreezeData> items, long version, int warehouseId,
                                          ResultMatcher... matchers) {
        try {
            List<String> itemsList = items.entrySet().stream()
                    .map(entry -> String.format(
                            "{\"item\":{\"shopSku\":\"%s\", \"vendorId\": 12, \"warehouseId\":%s}," +
                                    "\"amount\":%s, \"backorder\":%s}",
                            entry.getKey(), warehouseId, entry.getValue().getQuantity(), entry.getValue().isBackorder()
                    )).collect(Collectors.toList());

            String content = "{\"orderId\":" + orderId + ", " +
                    "\"items\":" + itemsList + ", " +
                    "\"freezeVersion\": " + version + "}";

            ResultActions resultActions = mockMvc.perform(post(ORDER)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(content));
            for (ResultMatcher matcher : matchers) {
                resultActions.andExpect(matcher);
            }
            return resultActions.andReturn();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
