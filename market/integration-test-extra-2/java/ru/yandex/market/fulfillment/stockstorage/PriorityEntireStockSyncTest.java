package ru.yandex.market.fulfillment.stockstorage;

import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.jobs.JobWhPair;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.PriorityEntireStockJobQueueSync;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.WarehouseAwareExecutionQueuePayload;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.fullsync.warehouse.strategy.FullSyncJobExecutor;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@DatabaseSetup("classpath:database/states/system_property.xml")
public class PriorityEntireStockSyncTest extends AbstractContextualTest {

    @Autowired
    private PriorityEntireStockJobQueueSync entireStockJobQueueSync;

    @Autowired
    private FulfillmentClient lgwClient;

    @SpyBean
    private FullSyncJobExecutor fullSyncJobExecutor;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        setActiveWarehouses(1);
    }

    @AfterEach
    @Override
    public void resetMocks() {
        super.resetMocks();
        Mockito.reset(lgwClient);
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
    @DatabaseSetup("classpath:database/states/priority_entire_stock_sync/trigger/1.xml")
    @ExpectedDatabase(value = "classpath:database/expected/priority_entire_stock_sync/trigger/1.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void triggerOnJobPairs() {
        Set<JobWhPair> jobWhPairs = Set.of(new JobWhPair(1, "PriorityFullSync").setBatchSize(2));
        entireStockJobQueueSync.trigger(jobWhPairs);
    }

    /**
     * Сценарий для parallelConsume:
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
    @DatabaseSetup("classpath:database/states/priority_entire_stock_sync/consume/1.xml")
    @ExpectedDatabase(value = "classpath:database/expected/priority_entire_stock_sync/consume/1.xml",
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

    private void exceptionByFullSyncJobExecutor() {
        doThrow(new RuntimeException("Exception occurred"))
                .when(fullSyncJobExecutor)
                .consume(getPayload());
    }

    private WarehouseAwareExecutionQueuePayload getPayload() {
        return new WarehouseAwareExecutionQueuePayload(20L, 40L, 20, false, 1);
    }
}
