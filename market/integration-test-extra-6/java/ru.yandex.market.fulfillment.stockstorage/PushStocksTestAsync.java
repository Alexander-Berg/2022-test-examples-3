package ru.yandex.market.fulfillment.stockstorage;

import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import lombok.SneakyThrows;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.fulfillment.stockstorage.configuration.AsyncTestConfiguration;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId;
import ru.yandex.market.fulfillment.stockstorage.service.export.rty.availability.SkuChangeAvailabilityExecutionQueueMessageProcessor;
import ru.yandex.market.fulfillment.stockstorage.service.export.rty.availability.SkuChangeAvailabilityMessageProducer;
import ru.yandex.market.fulfillment.stockstorage.service.export.rty.stocks.SkuChangeStocksAmountExecutionQueueConsumer;
import ru.yandex.market.fulfillment.stockstorage.service.export.rty.stocks.SkuChangeStocksAmountExecutionQueueMessageProcessor;
import ru.yandex.market.fulfillment.stockstorage.service.export.rty.stocks.SkuChangeStocksAmountMessageProducer;
import ru.yandex.market.fulfillment.stockstorage.service.export.rty.strategy.stocks.DefaultSkuChangeStocksAmountProducingStrategy;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.queue.PushStocksEventExecutionQueueConsumer;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.updating.strategy.AbstractStockUpdatingStrategy;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.updating.strategy.DefaultStockUpdatingStrategy;
import ru.yandex.market.fulfillment.stockstorage.util.AsyncWaiterService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(AsyncTestConfiguration.class)
@DatabaseSetup("classpath:database/states/system_property.xml")
public class PushStocksTestAsync extends AbstractContextualTest {

    @Autowired
    private AsyncWaiterService asyncWaiterService;

    @Autowired
    private SkuChangeStocksAmountExecutionQueueConsumer changeStocksAmountExecutionQueueConsumer;

    @Autowired
    private IDatabaseConnection databaseConnection;

    @Autowired
    private PushStocksEventExecutionQueueConsumer pushStocksEventExecutionQueueConsumer;

    @SpyBean
    private SkuChangeAvailabilityMessageProducer skuChangeAvailabilityMessageProducer;

    @SpyBean
    private SkuChangeStocksAmountMessageProducer skuChangeStocksAmountMessageProducer;

    @SpyBean
    private DefaultSkuChangeStocksAmountProducingStrategy skuChangeDataProducingStrategy;

    @SpyBean
    private SkuChangeStocksAmountExecutionQueueMessageProcessor changeStocksAmountExecutionQueueMessageProcessor;

    @SpyBean
    private SkuChangeAvailabilityExecutionQueueMessageProcessor availabilityExecutionQueueMessageProcessor;

    @SpyBean
    private DefaultStockUpdatingStrategy defaultStockUpdatingStrategy;

    @AfterEach
    @Override
    public void resetMocks() {
        super.resetMocks();
        Mockito.reset(skuChangeAvailabilityMessageProducer);
        Mockito.reset(skuChangeStocksAmountMessageProducer);
        Mockito.reset(skuChangeDataProducingStrategy);
        Mockito.reset(changeStocksAmountExecutionQueueMessageProcessor);
        Mockito.reset(availabilityExecutionQueueMessageProcessor);
        Mockito.reset(defaultStockUpdatingStrategy);
    }

    /**
     * Стоки успешно запушатся, все таски будут добавлены в очередь в асинхронном режиме
     */
    @Test
    @ExpectedDatabase(
            value = "classpath:database/expected/stocks_update/pushed_fit_one_warehouse.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void pushStocksFitSuccessful() {
        testPushStocks();
    }

    /**
     * Стоки успешно запушатся несмотря на ошибку, произошедушую при заведении новой таски в очередь
     */
    @Test
    @ExpectedDatabase(
            value = "classpath:database/expected/stocks_update/pushed_fit_with_failed_push_to_queue.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void pushStocksFitSuccessfulOnEmptyDatabase() {
        doThrow(new RuntimeException("weee"))
                .when(skuChangeDataProducingStrategy)
                .push(anyList());

        testPushStocks();
    }

    /**
     * Тест проверяет, что если изменение стоков будет в очень долго транзакции, то таски в очередях
     * CHANGED_STOCKS_AMOUNT_EVENT и CHANGED_AVAILABILITY_EVENT не будут видны для консьюмера, пока не завершится
     * транзакция по изменению стоков.
     * Это нужно, чтобы консьюымер всегда видел актуальне стоки и передал их в логброкер.
     */
    @Test
    @ExpectedDatabase(
            value = "classpath:database/expected/stocks_update/pushed_fit_single_stock.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void pushStocksWithLongTransaction() throws Exception {
        CountDownLatch latchInsideTransaction = new CountDownLatch(1);
        CountDownLatch latchOutsideTransaction = new CountDownLatch(1);
        mockWait(latchInsideTransaction, latchOutsideTransaction);

        CompletableFuture<Void> futureWait = CompletableFuture.runAsync(() ->
                pushStocks("requests/push/normal_push_fit_single_stock.json", status().is2xxSuccessful())
        );

        // Ждем когда выполнится основная логика
        latchInsideTransaction.await();

        // Проверяем, что консьюмер ничего не возьмет из очереди, пока транзакция не завершится
        changeStocksAmountExecutionQueueConsumer.consume();
        verify(changeStocksAmountExecutionQueueMessageProcessor, never()).process(any());

        // Ждем пока не завершится транзакция
        latchOutsideTransaction.await();

        // Проверяем, что после завершения транзакции данные в базе видны, но тасок еще нет
        IDataSet actual = databaseConnection.createDataSet();
        IDataSet expected = new FlatXmlDataSetBuilder()
                .build(new StringReader(
                        extractFileContent("database/expected/stocks_update/pushed_fit_medium_result.xml")));
        NON_STRICT_UNORDERED.getDatabaseAssertion().assertEquals(expected, actual, Collections.emptyList());

        // Дожидаемся окончания выполнения метода
        futureWait.join();
        asyncWaiterService.awaitTasks();

        // Проверяем, что консьюмер возьмет таску из очереди, после заверешения работы ручки
        changeStocksAmountExecutionQueueConsumer.consume();
        verify(changeStocksAmountExecutionQueueMessageProcessor).process(any());

        verify(skuEventAuditService, times(1)).logNewStockAmountPushed(anyList());
        verify(skuEventAuditService, times(1)).logSkuCreated(any(UnitId.class), any(Long.class));
        verify(skuEventAuditService).logSkuCreated(eq(new UnitId("sku1", 12L, 1)), any(Long.class));

        verify(stockEventsHandler, times(2)).handle(anyList());
        verify(skuChangeAvailabilityMessageProducer, times(1)).produceIfNecessaryAsync(anySet());
        verify(skuChangeStocksAmountMessageProducer, times(1)).produceIfNecessaryAsync(anySet());
    }

    private void mockAfterTransactionWait(CountDownLatch latchOutsideTransaction,
                                          AbstractStockUpdatingStrategy.StockUpdatingResult stockUpdatingResult) {
        doAnswer(invocation -> {
            latchOutsideTransaction.countDown();
            Thread.sleep(1000);
            //Ждем после выполнения всей основной логики снаружи транзакции, но до выставления тасок
            return invocation.callRealMethod();
        })
                .when(stockUpdatingResult)
                .getContext();
    }

    public void mockWait(CountDownLatch latchInsideTransaction,
                         CountDownLatch latchOutsideTransaction) {
        // Мокаем ожидание внутри транзакции
        doAnswer(invocation -> {
            Object o = invocation.callRealMethod();

            // Мокаем ожидание вне транзакции
            AbstractStockUpdatingStrategy.StockUpdatingResult stockUpdatingResult =
                    spy((AbstractStockUpdatingStrategy.StockUpdatingResult) ((List) o).get(0));
            mockAfterTransactionWait(latchOutsideTransaction, stockUpdatingResult);

            latchInsideTransaction.countDown();
            //Ждем после выполнения всей основной логики внутри транзакции, но до выставления тасок
            Thread.sleep(1000);
            return List.of(stockUpdatingResult);
        })
                .when(defaultStockUpdatingStrategy)
                .doUpdateStocks(anyList(), anyMap());
    }

    private void testPushStocks() {
        pushStocks("requests/push/normal_push_fit_stocks_one_warehouse.json", status().is2xxSuccessful());

        pushStocksEventExecutionQueueConsumer.consume();

        verify(skuEventAuditService, times(1)).logNewStockAmountPushed(anyList());
        verify(skuEventAuditService, times(2)).logSkuCreated(any(UnitId.class), any(Long.class));
        verify(skuEventAuditService).logSkuCreated(eq(new UnitId("sku0", 12L, 1)), any(Long.class));
        verify(skuEventAuditService).logSkuCreated(eq(new UnitId("sku1", 12L, 1)), any(Long.class));

        asyncWaiterService.awaitTasks();
        verify(stockEventsHandler, times(3)).handle(anyList());
        verify(skuChangeAvailabilityMessageProducer, times(1)).produceIfNecessaryAsync(anyCollection());
        verify(skuChangeStocksAmountMessageProducer, times(1)).produceIfNecessaryAsync(anyCollection());
    }

    @SneakyThrows
    public void pushStocks(String request, ResultMatcher internalServerError) {
        mockMvc.perform(post(PushStocksTest.STOCKS_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent(request)))
                .andExpect(internalServerError)
                .andReturn();
        pushStocksEventExecutionQueueConsumer.consume();
    }
}
