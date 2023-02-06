package ru.yandex.market.fulfillment.stockstorage;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.fulfillment.stockstorage.domain.converter.SSEntitiesConverter;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.Stock;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.StockType;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.UnfreezeJob;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId;
import ru.yandex.market.fulfillment.stockstorage.repository.StockRepository;
import ru.yandex.market.fulfillment.stockstorage.repository.UnfreezeJobRepository;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.freezing.UnfreezeJobExecutor;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.queue.PushStocksEventExecutionQueueConsumer;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ItemStocks;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.fulfillment.stockstorage.service.SkuJpaLockService.DEFAULT_TIMEOUT_SEC;
import static ru.yandex.market.fulfillment.stockstorage.util.ModelUtil.resourceId;

@DatabaseSetup("classpath:database/states/system_property.xml")
public class SkuJpaLockServiceTest extends AbstractContextualTest {

    private static final String FREEZE_URL = "/stocks/freeze";
    private static final String PUSH_STOCK_URL = "/stocks";

    private static final long MORE_THAN_DEFAULT_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(DEFAULT_TIMEOUT_SEC + 2);

    private static final long LESS_THAN_DEFAULT_TIMEOUT_MS =
            Math.min(1000, TimeUnit.SECONDS.toMillis(DEFAULT_TIMEOUT_SEC / 2));

    public static final Partner PARTNER = new Partner(1L);

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private StockRepository stockRepository;
    @Autowired
    private UnfreezeJobExecutor unfreezeJobExecutor;
    @Autowired
    private UnfreezeJobRepository unfreezeJobRepository;
    @Autowired
    private FulfillmentClient lgwCLient;
    @Autowired
    private PushStocksEventExecutionQueueConsumer pushStocksEventExecutionQueueConsumer;

    private final CountDownLatch firstRequestDone = new CountDownLatch(1);

    @Test
    @DatabaseSetup("classpath:database/states/stocks_korobytes_pushed.xml")
    @ExpectedDatabase(value = "classpath:database/expected/freeze/stocks_are_frozen_by_one_order.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void concurrentFreezesLockTimeout() {
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(
                () -> freezeAndHoldSkuInTransaction(MORE_THAN_DEFAULT_TIMEOUT_MS));

        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
            String contentAsString = freezeStock(status().is4xxClientError(), "requests/freeze/normal_single_stock" +
                    ".json");
            softly
                    .assertThat(contentAsString)
                    .contains("Cannot acquire lock in 10 SECONDS");
        });
        future1.join();
        future2.join();
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_korobytes_pushed.xml")
    @ExpectedDatabase(value = "classpath:database/expected/freeze/stocks_are_frozen_by_concurrent_freezes.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void concurrentFreezesSuccess() {
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(
                () -> freezeAndHoldSkuInTransaction(LESS_THAN_DEFAULT_TIMEOUT_MS));

        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() ->
                freezeStock(status().is2xxSuccessful(), "requests/freeze/normal_single_stock.json"));
        future1.join();
        future2.join();
    }

    @Test
    @DatabaseSetup({
            "classpath:database/states/backorder/warehouses.xml",
            "classpath:database/states/stocks_korobytes_pushed.xml"
    })
    @ExpectedDatabase(value = "classpath:database/expected/freeze/stocks_are_frozen_by_concurrent_freezes.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void concurrentFreezesBackorderSuccess() {
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(
                () -> freezeAndHoldSkuInTransaction(extractFileContent("requests/freeze/backorder_stocks.json"),
                        LESS_THAN_DEFAULT_TIMEOUT_MS));

        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() ->
                freezeStock(status().is2xxSuccessful(), "requests/freeze/backorder_single_stock.json"));
        future1.join();
        future2.join();
    }

    @Test
    @DatabaseSetup({
            "classpath:database/states/backorder/warehouses.xml",
            "classpath:database/states/stocks_korobytes_pushed.xml"
    })
    @ExpectedDatabase(value = "classpath:database/expected/freeze" +
            "/stocks_are_frozen_by_concurrent_backorder_overrun_freezes.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void concurrentFreezesBackorderOverrunSuccess() {
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(
                () -> freezeAndHoldSkuInTransaction(extractFileContent("requests/freeze/backorder_overrun_stocks.json"),
                        LESS_THAN_DEFAULT_TIMEOUT_MS));

        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() ->
                freezeStock(status().is2xxSuccessful(), "requests/freeze/backorder_overrun_single_stock.json"));
        future1.join();
        future2.join();
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_unfreeze_scheduled_freeze_available.xml")
    public void concurrentFreezeUnfreeze() {
        UnitId unitId = new UnitId("sku0", 12L, 1);
        when(lgwCLient.getStocks(Collections.singletonList(SSEntitiesConverter.toLgwUnitId(unitId)), PARTNER))
                .thenReturn(getSku0Stock());

        CompletableFuture<Void> future1 = CompletableFuture.runAsync(
                () -> freezeAndHoldSkuInTransaction(LESS_THAN_DEFAULT_TIMEOUT_MS));

        CompletableFuture<Void> future2 = CompletableFuture.runAsync(this::unfreezeStock);
        future1.join();
        future2.join();

        Stock stock = stockRepository.findByUnitIdAndType(unitId, StockType.FIT);
        softly
                .assertThat(stock.getAmount())
                .isEqualTo(99990);
        softly
                .assertThat(stock.getFreezeAmount())
                .isEqualTo(50040); // 50000 + 50 - 10    (50 - freeze, 10 - unfreeze)

        UnfreezeJob unfreezeJob = unfreezeJobRepository.findById(10011L).orElse(null);
        softly
                .assertThat(unfreezeJob.getExecuted())
                .isNotNull();
        softly
                .assertThat(unfreezeJob.getFailReason())
                .isNull();
        softly
                .assertThat(unfreezeJob.getAttemptNumber())
                .isEqualTo(0);
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_unfreeze_scheduled_freeze_available.xml")
    @ExpectedDatabase(value = "classpath:database/expected/freeze/stocks_frozen_and_pushed.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void concurrentFreezePush() {
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(
                () -> freezeAndHoldSkuInTransaction(LESS_THAN_DEFAULT_TIMEOUT_MS));

        CompletableFuture<Void> future2 = CompletableFuture.runAsync(this::pushStock);
        future1.join();
        future2.join();
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_unfreeze_scheduled_freeze_available.xml")
    public void pushFreezeNotEnough() {
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(
                () -> pushAndHoldSkuInTransaction());

        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
            String contentAsString = freezeStock(status().is4xxClientError(), "requests/freeze" +
                    "/large_quantity_single_stock.json");
            softly
                    .assertThat(contentAsString)
                    .contains("Failed to freeze stocks. Not enough available items [{12:sku1:1} required 5000, but " +
                            "found -49900]");
        });

        future1.join();
        future2.join();
    }

    private void freezeAndHoldSkuInTransaction(long holdTransactionTimeMs) {
        freezeAndHoldSkuInTransaction(extractFileContent("requests/freeze/normal_stocks.json"), holdTransactionTimeMs);
    }

    private void freezeAndHoldSkuInTransaction(String content, long holdTransactionTimeMs) {
        transactionTemplate.execute(t -> {
            try {
                mockMvc.perform(post(FREEZE_URL)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(content))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn();
                firstRequestDone.countDown();
                Thread.sleep(holdTransactionTimeMs);
                return null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void pushAndHoldSkuInTransaction() {
        transactionTemplate.execute(t -> {
            try {
                mockMvc.perform(post(PUSH_STOCK_URL)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(extractFileContent("requests/push/normal_push_fit_single_stock.json")))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn();
                pushStocksEventExecutionQueueConsumer.consume();
                firstRequestDone.countDown();
                return null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private String freezeStock(ResultMatcher statusMatcher, String requestFilePath) {
        try {
            firstRequestDone.await();
            return mockMvc.perform(post(FREEZE_URL)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(extractFileContent(requestFilePath)))
                    .andExpect(statusMatcher)
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void pushStock() {
        try {
            firstRequestDone.await();
            mockMvc.perform(post(PUSH_STOCK_URL)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(extractFileContent("requests/push/normal_push_fit_stocks_one_warehouse.json")))
                    .andExpect(status().is2xxSuccessful())
                    .andReturn();
            pushStocksEventExecutionQueueConsumer.consume();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void unfreezeStock() {
        try {
            firstRequestDone.await();
            unfreezeJobExecutor.executeNextJob();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<ItemStocks> getSku0Stock() {
        DateTime updated = DateTime.fromOffsetDateTime(OffsetDateTime.now());
        return Collections.singletonList(
                new ItemStocks(
                        new ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId(null, 12L, "sku0"),
                        resourceId("1", "1"),
                        ImmutableList.of(
                                new ru.yandex.market.logistic.gateway.common.model.fulfillment.Stock(
                                        ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType.FIT,
                                        99990, updated
                                )
                        )
                )
        );
    }

}
