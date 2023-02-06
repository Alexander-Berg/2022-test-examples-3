package ru.yandex.market.fulfillment.stockstorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.google.common.collect.ImmutableList;
import com.mysema.commons.lang.Pair;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.jobs.JobWhPair;
import ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.jobs.SyncJobName;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.EntireStockJobQueueSync;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.WarehouseAwareExecutionQueuePayload;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.fullsync.warehouse.strategy.FullSyncJobExecutor;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ItemStocks;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Stock;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;
import ru.yandex.market.request.trace.RequestContextHolder;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.fulfillment.stockstorage.util.ModelUtil.resourceId;
import static ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime.fromLocalDateTime;

@DatabaseSetup("classpath:database/states/system_property.xml")
public class EntireStockSyncTest extends AbstractContextualTest {

    private static final Partner PARTNER = new Partner(1L);
    private static final UnitId FF_UNIT_1 = new UnitId(null, 1L, "1");
    private static final UnitId FF_UNIT_2 = new UnitId(null, 1L, "2");
    private static final List<Stock> STOCKS = singletonList(
            new Stock(StockType.FIT, 10, fromLocalDateTime(
                    LocalDate.of(1970, 1, 1).atStartOfDay())
            )
    );
    private static final List<Stock> STOCKS_2 = List.of(
            new Stock(StockType.FIT, 15, fromLocalDateTime(
                    LocalDate.of(1970, 1, 1).atStartOfDay())
            ),
            new Stock(StockType.DEFECT, 3, fromLocalDateTime(
                    LocalDate.of(1975, 2, 3).atStartOfDay())
            )
    );
    private static final List<Stock> STOCKS_2_DISAPPEARED = List.of(
            new Stock(StockType.FIT, 0, fromLocalDateTime(
                    LocalDate.of(1970, 1, 1).atStartOfDay())
            ),
            new Stock(StockType.DEFECT, 3, fromLocalDateTime(
                    LocalDate.of(1975, 2, 3).atStartOfDay())
            )
    );

    @Autowired
    private EntireStockJobQueueSync entireStockJobQueueSync;

    @Autowired
    private FulfillmentClient lgwClient;

    @SpyBean
    private FullSyncJobExecutor fullSyncJobExecutor;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        setActiveWarehouses(1);
        when(lmsClient.searchPartners(any()))
                .thenReturn(List.of(PartnerResponse.newBuilder().status(PartnerStatus.ACTIVE).build()));
    }

    @AfterEach
    @Override
    public void resetMocks() {
        super.resetMocks();
        Mockito.reset(lgwClient, lmsClient, fullSyncJobExecutor);
    }

    /**
     * Сценарий #1:
     * В БД находится 5 подходящих СКУ.
     * Должно сгенерироваться 3 записи в очереди
     * <p>
     * from = 0 to = 2
     * from = 2 to = 4
     * from = 4 to = 6
     */
    @Test
    @DatabaseSetup("classpath:database/states/entire_stock_sync/trigger/1.xml")
    @ExpectedDatabase(value = "classpath:database/expected/entire_stock_sync/trigger/1.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void triggerOnFullDatabase() {
        trigger();
    }

    /**
     * Сценарий #2:
     * В БД нет подходящих СКУ и отсутствуют активные warehouse_id.
     * <p>
     * Очередь должна остаться пуста
     */
    @Test
    @DatabaseSetup("classpath:database/states/entire_stock_sync/trigger/2.xml")
    @ExpectedDatabase(value = "classpath:database/expected/entire_stock_sync/trigger/2.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void triggerOnEmptyDatabaseWithoutActiveWarehouses() {
        setActiveWarehouses();
        trigger();
    }


    /**
     * Сценарий #3:
     * Проверяем, что при trigger'е батчи будут созданы только для тех warehouse'ов, которые считаются активными.
     */
    @Test
    @DatabaseSetup("classpath:database/states/entire_stock_sync/trigger/3.xml")
    @ExpectedDatabase(value = "classpath:database/expected/entire_stock_sync/trigger/3.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void triggerOnDisabledWarehouse() {
        trigger();
    }

    /**
     * Сценарий #4:
     * <p>
     * Проверяем, что при consume'е на warehouse'е, который не считается активным - батч будет обработан.
     */
    @Test
    @DatabaseSetup("classpath:database/states/entire_stock_sync/consume/4.xml")
    @ExpectedDatabase(value = "classpath:database/expected/entire_stock_sync/consume/4.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void consumeOnDisabledWarehouse() {
        when(lgwClient.getStocks(anyInt(), anyInt(), any(Partner.class)))
                .thenReturn(singletonList(new ItemStocks(FF_UNIT_1, resourceId("1", "ff1"), STOCKS)));

        setActiveWarehouses();
        entireStockJobQueueSync.consume();

        verify(lgwClient).getStocks(eq(2), eq(0), eq(PARTNER));
        verifyNoMoreInteractions(lgwClient);
    }

    /**
     * Сценарий #4.1 (для parallelConsume):
     * <p>
     * Проверяем, что при consume'е на warehouse'е, который не считается активным - батч будет обработан.
     */
    @Test
    @DatabaseSetup("classpath:database/states/entire_stock_sync/consume/4.xml")
    @ExpectedDatabase(value = "classpath:database/expected/entire_stock_sync/consume/4.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void parallelConsumeOnDisabledWarehouse() {
        when(lgwClient.getStocks(anyInt(), anyInt(), any(Partner.class)))
                .thenReturn(singletonList(new ItemStocks(FF_UNIT_1, resourceId("1", "ff1"), STOCKS)));

        setActiveWarehouses();
        entireStockJobQueueSync.parallelConsume();

        verify(lgwClient).getStocks(eq(2), eq(0), eq(PARTNER));
        verifyNoMoreInteractions(lgwClient);
    }

    /**
     * Сценарий #5:
     * В БД нет подходящих СКУ и присутствует активный warehouse_id = 1,
     * по которому нет настроек в FFInterval.
     * <p>
     * В очереди должна появиться запись c pageSize = 20.
     * from=0 to=20 (Батч пустой БД)
     */
    @Test
    @DatabaseSetup("classpath:database/states/entire_stock_sync/trigger/5.xml")
    @ExpectedDatabase(value = "classpath:database/expected/entire_stock_sync/trigger/5.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void triggerOnEmptyDatabaseWithActiveWarehouses() {
        setActiveWarehouses(1);
        trigger();
    }

    /**
     * Сценарий #6:
     * В БД присутствует батч пустой БД (from=0 to=2 total=0).
     * <p>
     * Должна произойти обработка эквивалентная логике последнего батча
     * (фетчим данные сколько есть со склада).
     */
    @Test
    @DatabaseSetup("classpath:database/states/entire_stock_sync/trigger/6.xml")
    public void consumeEmptyDatabaseBatch() {
        entireStockJobQueueSync.consume();
        Mockito.verify(lgwClient, times(1)).getStocks(eq(20), eq(0), any(Partner.class));
    }

    /**
     * Сценарий #6.1 (для parallelConsume):
     * В БД присутствует батч пустой БД (from=0 to=2 total=0).
     * <p>
     * Должна произойти обработка эквивалентная логике последнего батча
     * (фетчим данные сколько есть со склада).
     */
    @Test
    @DatabaseSetup("classpath:database/states/entire_stock_sync/trigger/6.xml")
    public void parallelConsumeEmptyDatabaseBatch() {
        entireStockJobQueueSync.parallelConsume();
        Mockito.verify(lgwClient, times(1)).getStocks(eq(20), eq(0), any(Partner.class));
    }

    /**
     * Сценарий #7:
     * В БД нету записей в очереди.
     * <p>
     * Не должно произойти обращений к LGW Client'у. c limit = 2, offset = 0;
     */
    @Test
    public void onEmptyDatabaseNoRequestsMade() {
        entireStockJobQueueSync.consume();
        Mockito.verify(lgwClient, Mockito.never()).getStocks(anyInt(), anyInt(), any(Partner.class));
    }

    /**
     * Сценарий #7.1 (для parallelConsume):
     * В БД нету записей в очереди.
     * <p>
     * Не должно произойти обращений к LGW Client'у. c limit = 2, offset = 0;
     */
    @Test
    public void onEmptyDatabaseNoRequestsMadeOnParallelConsume() {
        entireStockJobQueueSync.parallelConsume();
        Mockito.verify(lgwClient, Mockito.never()).getStocks(anyInt(), anyInt(), any(Partner.class));
    }

    /**
     * Сценарий #8:
     * При обработке батча происходит ошибка.
     * В таком случае батч должен быть положен в конец очереди.
     */
    @Test
    @DatabaseSetup("classpath:database/states/entire_stock_sync/consume/8.xml")
    @ExpectedDatabase(value = "classpath:database/expected/entire_stock_sync/consume/8.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void consumeWithException() {
        RequestContextHolder.createContext("0/0");
        Exception expectedException = new RuntimeException("Exception occurred");

        doThrow(expectedException)
                .when(fullSyncJobExecutor)
                .consume(any(WarehouseAwareExecutionQueuePayload.class));

        entireStockJobQueueSync.consume();
    }


    /**
     * Сценарий #8.1 (для parallelConsume):
     * При обработке батча происходит ошибка.
     * В таком случае батч должен быть положен в конец очереди.
     * <p>
     * Параллельное испольнение происходит в несколько потока.
     * Когда первый поток фейлится, то другие потоки могут успеть начать исполнять сфейленный батч.
     * Количеством попыток будет количество обращений к экзекьютору
     */
    @Test
    @DatabaseSetup("classpath:database/states/entire_stock_sync/consume/8-1.xml")
    @ExpectedDatabase(value = "classpath:database/expected/entire_stock_sync/consume/8-1.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void parallelConsumeWithException() {
        Exception expectedException = new RuntimeException("Exception occurred");

        doThrow(expectedException)
                .when(fullSyncJobExecutor)
                .consume(any(WarehouseAwareExecutionQueuePayload.class));

        entireStockJobQueueSync.parallelConsume();

        int numberOfCalls = Mockito.mockingDetails(fullSyncJobExecutor).getInvocations().size();
        List<Pair<Integer, Integer>> expected = ImmutableList.of(Pair.of(numberOfCalls, numberOfCalls));

        List<Pair<Integer, Integer>> actual = getActualIdAndAttempt();
        assertEquals(expected, actual);
    }

    /**
     * Сценарий #9:
     * При обработке батча постоянно происходит ошибка.
     * Пробуем взять джобу в работу 20 раз.
     * Но имеется ограничение на attempt_number (в данном случае 15 раз).
     * <p>
     * Ожидаем, что джоба возьмется в работу 16 раз (0 >= attempt_number <= 15).
     * В остальных вызовах джоба браться не будет.
     */
    @Test
    @DatabaseSetup("classpath:database/states/entire_stock_sync/consume/8.xml")
    @ExpectedDatabase(value = "classpath:database/expected/entire_stock_sync/consume/9.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void consumeWithExceptionMultipleTimes() {
        int attemptCount = 20;
        for (int i = 0; i < attemptCount; ++i) {
            RequestContextHolder.createContext("0/0");
            doThrow(new RuntimeException("Exception #" + (i + 1)))
                    .when(fullSyncJobExecutor)
                    .consume(any(WarehouseAwareExecutionQueuePayload.class));

            entireStockJobQueueSync.consume();
        }
        verify(fullSyncJobExecutor, times(16))
                .consume(any(WarehouseAwareExecutionQueuePayload.class));
    }

    /**
     * Сценарий #9.1 (для parallelConsume):
     * При обработке батча постоянно происходит ошибка.
     * Пробуем взять джобу в работу 20 раз.
     * Но имеется ограничение на attempt_number (в данном случае 15 раз).
     * <p>
     * Ожидаем, что джоба возьмется в работу 16 раз (0 >= attempt_number <= 15).
     * В остальных вызовах джоба браться не будет.
     */
    @Test
    @DatabaseSetup("classpath:database/states/entire_stock_sync/consume/8.xml")
    @ExpectedDatabase(value = "classpath:database/expected/entire_stock_sync/consume/9-1.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void parallelConsumeWithExceptionMultipleTimes() {
        int attemptCount = 20;
        for (int i = 0; i < attemptCount; ++i) {
            RequestContextHolder.createContext("0/0");
            doThrow(new RuntimeException("Exception"))
                    .when(fullSyncJobExecutor)
                    .consume(any(WarehouseAwareExecutionQueuePayload.class));

            entireStockJobQueueSync.parallelConsume();
        }
        verify(fullSyncJobExecutor, times(16))
                .consume(any(WarehouseAwareExecutionQueuePayload.class));
    }

    /**
     * Сценарий #11 (для parallelConsume):
     * В БД 4 записи в очереди.
     * parallelConsume вызывается два раза
     * Для одного payload будет брошено исключение при первом исполнении - "from":20,"to":40
     * Второй раз исполнится без ошибок
     *
     * <p>
     * Ожидаем, что в БД не будет записей
     * <p>
     * При первом исполнении:
     * Два обращения к LGW Client'у для смещений в 0 и 40
     * Для смещения 20 обращений к LGW Client'у быть не должно
     * <p>
     * При втором исполнении:
     * Два обращения к LGW Client'у для смещений в 20 и 60
     */
    @Test
    @DatabaseSetup("classpath:database/states/entire_stock_sync/consume/11.xml")
    @ExpectedDatabase(value = "classpath:database/expected/entire_stock_sync/consume/11.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void consumeParallelBatchTwice() {
        exceptionByFullSyncJobExecutor();

        entireStockJobQueueSync.parallelConsume();

        verify(lgwClient).getStocks(eq(20), eq(0), any(Partner.class));
        verify(lgwClient).getStocks(eq(20), eq(40), any(Partner.class));
        verify(lgwClient, Mockito.never()).getStocks(eq(20), eq(20), any(Partner.class));

        Mockito.reset(fullSyncJobExecutor);

        entireStockJobQueueSync.parallelConsume();

        verify(lgwClient).getStocks(eq(20), eq(20), any(Partner.class));
        verify(lgwClient).getStocks(eq(20), eq(60), any(Partner.class));
    }

    /**
     * Сценарий #12:
     * Ошибка от LGW при запросе стоков на неактивный склад игнорируется
     */
    @Test
    @DatabaseSetup("classpath:database/states/entire_stock_sync/consume/12.xml")
    @ExpectedDatabase(value = "classpath:database/expected/entire_stock_sync/consume/12.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void consumeWithErrorOnInactiveWarehouse() {
        when(lgwClient.getStocks(anyInt(), anyInt(), any())).thenThrow(new RuntimeException());
        mockSearchPartners(List.of(PartnerResponse.newBuilder().status(PartnerStatus.INACTIVE).build()));

        entireStockJobQueueSync.consume();
    }

    @Test
    @DatabaseSetup("classpath:database/states/entire_stock_sync/batch_consume/before-one-item-in-db.xml")
    @ExpectedDatabase(value = "classpath:database/expected/entire_stock_sync/batch_consume/after-one-item-in-db.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void consumeBatchOneItemInDbAndOneMoreItemCreated() {
        when(lgwClient.getStocks(anyInt(), anyInt(), any(Partner.class)))
                .thenReturn(List.of(
                        new ItemStocks(FF_UNIT_1, resourceId("1", "ff1"), STOCKS),
                        new ItemStocks(FF_UNIT_2, resourceId("1", "ff1"), STOCKS_2)
                ));

        setActiveWarehouses();
        entireStockJobQueueSync.consume();

        verify(lgwClient).getStocks(eq(50), eq(0), eq(PARTNER));
        verifyNoMoreInteractions(lgwClient);
    }

    @Test
    @DatabaseSetup("classpath:database/states/entire_stock_sync/batch_consume/before-two-items-in-db.xml")
    @ExpectedDatabase(value = "classpath:database/expected/entire_stock_sync/batch_consume/after-two-items-in-db.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void consumeBatchTwoItemsInDb() {
        when(lgwClient.getStocks(anyInt(), anyInt(), any(Partner.class)))
                .thenReturn(List.of(
                        new ItemStocks(FF_UNIT_1, resourceId("1", "ff1"), STOCKS),
                        new ItemStocks(FF_UNIT_2, resourceId("1", "ff1"), STOCKS_2_DISAPPEARED)
                ));

        setActiveWarehouses();
        entireStockJobQueueSync.consume();

        verify(lgwClient).getStocks(eq(50), eq(0), eq(PARTNER));
        verifyNoMoreInteractions(lgwClient);
    }

    @Test
    @DatabaseSetup("classpath:database/states/entire_stock_sync/batch_consume/before-no-items-in-db.xml")
    @ExpectedDatabase(value = "classpath:database/expected/entire_stock_sync/batch_consume/after-no-items-in-db.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void consumeBatchNoItemsInDb() {
        when(lgwClient.getStocks(anyInt(), anyInt(), any(Partner.class)))
                .thenReturn(List.of(
                        new ItemStocks(FF_UNIT_1, resourceId("1", "ff1"), STOCKS),
                        new ItemStocks(FF_UNIT_2, resourceId("1", "ff1"), STOCKS_2_DISAPPEARED)
                ));

        setActiveWarehouses();
        entireStockJobQueueSync.consume();

        verify(lgwClient).getStocks(eq(50), eq(0), eq(PARTNER));
        verifyNoMoreInteractions(lgwClient);
    }

    @Test
    @DatabaseSetup("classpath:database/states/entire_stock_sync/consume/no_partners.xml")
    @DatabaseSetup("classpath:database/expected/entire_stock_sync/consume/no_partners.xml")
    public void testNoPartnersLinkedError() {
        when(lgwClient.getStocks(anyInt(), anyInt(), any(Partner.class)))
                .thenThrow(new HttpTemplateException(400, "code 9404: No partners linked to warehouse: 55219"));

        setActiveWarehouses();
        entireStockJobQueueSync.consume();

        // по коду 9404 не должно быть повторов в RetryingService
        verify(lgwClient, times(1)).getStocks(eq(450), eq(0), eq(PARTNER));
    }

    private void exceptionByFullSyncJobExecutor() {
        doThrow(new RuntimeException("Exception occurred"))
                .when(fullSyncJobExecutor)
                .consume(getPayload());
    }

    private WarehouseAwareExecutionQueuePayload getPayload() {
        return new WarehouseAwareExecutionQueuePayload(20L, 40L, 20, false, 1);
    }

    private List<Pair<Integer, Integer>> getActualIdAndAttempt() {
        return jdbcTemplate.query(
                "SELECT id, attempt_number FROM execution_queue",
                (rs, rowNum) -> Pair.of(
                        rs.getInt("id"),
                        rs.getInt("attempt_number")
                )
        );
    }

    private void trigger() {
        Set<JobWhPair> jobWhPairs = warehouseSyncService.getSyncJobWHPairs(1).stream()
                .filter(jobWhPair -> StringUtils.equals(jobWhPair.getSyncJobName(), SyncJobName.FULL_SYNC.getValue()))
                .collect(Collectors.toSet());

        entireStockJobQueueSync.trigger(jobWhPairs);
    }
}
