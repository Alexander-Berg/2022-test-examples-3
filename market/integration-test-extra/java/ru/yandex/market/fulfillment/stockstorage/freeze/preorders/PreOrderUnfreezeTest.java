package ru.yandex.market.fulfillment.stockstorage.freeze.preorders;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.fulfillment.stockstorage.AbstractContextualTest;
import ru.yandex.market.fulfillment.stockstorage.client.entity.StockStorageErrorStatusCode;
import ru.yandex.market.fulfillment.stockstorage.configuration.BasicColumnsFilter;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.FreezeReason;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.FreezeReasonType;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.Stock;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.StockFreeze;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.UnfreezeJob;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId;
import ru.yandex.market.fulfillment.stockstorage.domain.exception.FreezeNotFoundException;
import ru.yandex.market.fulfillment.stockstorage.repository.StockFreezeRepository;
import ru.yandex.market.fulfillment.stockstorage.repository.StockRepository;
import ru.yandex.market.fulfillment.stockstorage.repository.UnfreezeJobRepository;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.freezing.UnfreezeJobExecutor;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.fulfillment.stockstorage.client.StockStoragePreOrderRestClient.PREORDER;

@DatabaseSetup("classpath:database/states/system_property.xml")
public class PreOrderUnfreezeTest extends AbstractContextualTest {


    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private UnfreezeJobExecutor unfreezeJobExecutor;
    @Autowired
    private StockRepository stockRepository;
    @Autowired
    private UnfreezeJobRepository unfreezeJobRepository;
    @Autowired
    private StockFreezeRepository stockFreezeRepository;
    @Autowired
    private FulfillmentClient lgwClient;

    private final CountDownLatch firstRequestDone = new CountDownLatch(1);

    /**
     * Проверка:
     * Запрос на анфриз /preorder при существующем задании на анфриз.
     * <p>
     * Результат:
     * Код 200
     * В БД ничего не создано
     */
    @Test
    @DatabaseSetup("classpath:database/states/preorder/preorder_stocks_frozen_two_orders_with_unfreeze.xml")
    @ExpectedDatabase(value = "classpath:database/states/preorder/preorder_stocks_frozen_two_orders_with_unfreeze.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void skipUnfreezeIfUnfreezeAlreadyCaught() throws Exception {
        mockMvc.perform(delete(PREORDER + "/preorder1?cancel=true"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

    /**
     * Проверка:
     * Запрос на анфриз /preorder. С cancel=true и не указанным cancel(=false)
     * <p>
     * Результат:
     * Код 200
     * В БД созданы оба задания на анфриз
     * При анфризе с cancel=true - состояние стока не изменилось
     * При анфризе с cancel=false - PREORDER.amount стало меньше на размер фриза
     */
    @Test
    @DatabaseSetup("classpath:database/states/preorder/preorder_stocks_frozen_two_orders.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/preorder/preorder_stocks_frozen_with_two_orders_unfreeze_created.xml",
            assertionMode = NON_STRICT_UNORDERED,
            columnFilters = {BasicColumnsFilter.class})
    public void unfreezeScheduled() throws Exception {
        mockMvc.perform(delete(PREORDER + "/preorder1"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        mockMvc.perform(delete(PREORDER + "/preorder2?cancel=true"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(freezeEventAuditService, times(2)).logUnfreezeScheduled(anyList());
        verify(stockEventsHandler, times(3))
                .handle(anyList());

    }

    /**
     * Проверка:
     * Запрос на анфриз /preorder. С cancel=false. Запросы выполняются конкуррентно.
     * <p>
     * Результат:
     * Код 200
     * В БД созданы оба задания на анфриз
     * Т.к. преордер сток закончен - создана задача на обноление SKU_FILTER и
     * в execution_queue с типом смены availability
     */
    @Test
    @DatabaseSetup("classpath:database/states/preorder/preorder_stocks_frozen_two_orders.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/preorder" +
                    "/preorder_stocks_frozen_with_two_orders_unfreeze_created_concurrent.xml",
            assertionMode = NON_STRICT_UNORDERED,
            columnFilters = {BasicColumnsFilter.class})
    public void concurrentUnfreezeScheduled() {

        CompletableFuture<Void> future1 = CompletableFuture.runAsync(
                () -> unfreezeAndHoldSkuInTransaction("/preorder2", 2000)
        );

        CompletableFuture<Void> future2 = CompletableFuture.runAsync(this::doUnfreeze);

        future1.join();
        future2.join();

        verify(freezeEventAuditService, times(2)).logUnfreezeScheduled(anyList());
        verify(stockEventsHandler, times(5))
                .handle(anyList());

    }

    private void doUnfreeze() {
        try {
            firstRequestDone.await();
            mockMvc.perform(delete(PREORDER + "/preorder1"))
                    .andExpect(status().is2xxSuccessful())
                    .andReturn();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unfreezeAndHoldSkuInTransaction(String order, long holdTransactionTimeMs) {
        transactionTemplate.execute(t -> {
            try {
                mockMvc.perform(delete(PREORDER + "/" + order))
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

    /**
     * Проверка:
     * Исполнен анфриз на PREORDER сток
     * <p>
     * Результат:
     * Кол-во PREORDER.freeze_amount уменьшилось
     * Синхронизация стоков не была вызвана
     * Фриз помечен как удаленный
     * Джоба помечена как исполненная.
     */
    @Test
    @DatabaseSetup("classpath:database/states/preorder/preorder_stocks_frozen_two_orders_with_unfreeze.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/preorder/preorder_stocks_frozen_two_orders_with_unfreeze_executed.xml",
            assertionMode = NON_STRICT_UNORDERED,
            columnFilters = {BasicColumnsFilter.class})
    public void simpleUnfreezeJobExecuted() {
        UnitId unitId = new UnitId("sku0", 12L, 1);
        unfreezeJobExecutor.executeNextJob();

        verify(freezeEventAuditService).logUnfreezeSuccessful(any(UnfreezeJob.class));

        Stock stock = stockRepository.findByUnitIdAndType(
                unitId, ru.yandex.market.fulfillment.stockstorage.domain.entity.StockType.PREORDER);
        softly
                .assertThat(stock.getAmount())
                .isEqualTo(100000);
        softly
                .assertThat(stock.getFreezeAmount())
                .isEqualTo(90);

        List<StockFreeze> freezes = stockFreezeRepository.findAllByReasonWithoutUnfreezeJobs(
                FreezeReason.of("preorder1", FreezeReasonType.PREORDER));
        softly
                .assertThat(freezes)
                .hasSize(0);

        UnfreezeJob unfreezeJob = unfreezeJobRepository.findById(10011L).get();
        softly.assertThat(unfreezeJob.getStockFreeze().isDeleted())
                .isTrue();
        softly.assertThat(unfreezeJob.getExecuted())
                .isNotNull();

        verify(lgwClient, never()).getStocks(any(), any());
    }

    /**
     * Проверка:
     * Исполнен анфриз на PREORDER сток с изменением доступности стока
     * <p>
     * Результат:
     * Кол-во PREORDER.freeze_amount уменьшилось
     * Синхронизация стоков не была вызвана
     * Фриз помечен как удаленный
     * Джоба помечена как исполненная.
     */
    @Test
    @DatabaseSetup("classpath:database/states/preorder/preorder_stocks_frozen_two_orders_with_unfreeze_complete.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/preorder" +
                    "/preorder_stocks_frozen_two_orders_with_unfreeze_complete_executed.xml",
            assertionMode = NON_STRICT_UNORDERED,
            columnFilters = {BasicColumnsFilter.class})
    public void unfreezeJobWithAvailabilityChangeExecuted() {
        UnitId unitId = new UnitId("sku0", 12L, 1);
        unfreezeJobExecutor.executeNextJob();

        verify(freezeEventAuditService).logUnfreezeSuccessful(any(UnfreezeJob.class));

        Stock stock = stockRepository.findByUnitIdAndType(
                unitId, ru.yandex.market.fulfillment.stockstorage.domain.entity.StockType.PREORDER);
        softly
                .assertThat(stock.getAmount())
                .isEqualTo(100000);
        softly
                .assertThat(stock.getFreezeAmount())
                .isEqualTo(99990);

        List<StockFreeze> freezes = stockFreezeRepository.findAllByReasonWithoutUnfreezeJobs(
                FreezeReason.of("preorder1", FreezeReasonType.PREORDER));
        softly
                .assertThat(freezes)
                .hasSize(0);

        UnfreezeJob unfreezeJob = unfreezeJobRepository.findById(10011L).orElse(null);
        softly.assertThat(unfreezeJob.getExecuted())
                .isNotNull();

        verify(lgwClient, never()).getStocks(any(), any());
    }

    /**
     * Проверка:
     * Запрос на анфриз /preorder с идентификатором фритза отсутствующим в БД.
     * <p>
     * Результат:
     * Код 400
     * Ответ с ошибкой о ненайденном фризе
     */
    @Test
    @DatabaseSetup("classpath:database/states/preorder/preorder_stocks_frozen_two_orders_with_unfreeze.xml")
    @ExpectedDatabase(value = "classpath:database/states/preorder/preorder_stocks_frozen_two_orders_with_unfreeze.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void unfreezeStocksFailedDueToNoFreezesFound() throws Exception {
        String contentAsString = mockMvc.perform(delete(PREORDER + "/preorderNotExist?cancel=true"))
                .andExpect(status().is(StockStorageErrorStatusCode.FREEZE_NOT_FOUND.getCode()))
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains(FreezeNotFoundException.MESSAGE);
    }

    /**
     * Проверка:
     * Запрос на анфриз /preorder с cancel=false. При этом в фризе кол-во больше чем осталось на стоке
     * <p>
     * Результат:
     * Код 400
     * Ответ с ошибкой о недостаточном кол-ве на стоке для анфриза с указанием item и кол-ва
     */
    @Test
    @DatabaseSetup("classpath:database/states/preorder/preorder_stocks_frozen_two_orders_not_enough.xml")
    @ExpectedDatabase(
            value = "classpath:database/states/preorder/preorder_stocks_frozen_two_orders_not_enough.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void unfreezeScheduledWithNotEnoughStock() throws Exception {
        String contentAsString = mockMvc.perform(delete(PREORDER + "/preorder2"))
                .andExpect(status().is5xxServerError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("sku0", "100000", "900000");
    }
}
