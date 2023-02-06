package ru.yandex.market.mbo.mdm.tms.executors;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.TestTransaction;

import ru.yandex.market.mbo.mdm.common.masterdata.metrics.models.MdmQueueStatistics;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueueInfoBase;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.SskuToRefreshInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmQueueStatisticsRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MskuToMboQueueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SendReferenceItemQRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SendToErpQueueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SskuToRefreshRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.processed.SendToDatacampQueueProcessedRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.processed.SendToDatacampQueueProcessedRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.to_process.SendToDatacampQueueToProcessRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.to_process.SendToDatacampQueueToProcessRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.service.MdmSolomonPushService;
import ru.yandex.market.mbo.mdm.common.utils.MdmDbWithCleaningTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.MdmProperties;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;


/**
 * @author albina-gima
 * @date 11/23/21
 */
public class MdmQueueStatisticsCounterExecutorTest extends MdmDbWithCleaningTestClass {
    private static final Instant OLD_TS = Instant.now().minus(4, ChronoUnit.MINUTES);
    private static final Instant FRESH_TS = Instant.now().minus(1, ChronoUnit.MINUTES);

    private static final ShopSkuKey KEY_1 = new ShopSkuKey(102, "sku1");
    private static final ShopSkuKey KEY_2 = new ShopSkuKey(206, "sku2");
    private static final ShopSkuKey KEY_3 = new ShopSkuKey(31, "sku3");

    private static final List<String> PERCENTILE_METRICS_NAMES = List.of(
        "mdm.ssku_to_refresh_0.25", "mdm.ssku_to_refresh_0.5", "mdm.ssku_to_refresh_0.7",
        "mdm.ssku_to_refresh_0.9", "mdm.ssku_to_refresh_0.95", "mdm.ssku_to_refresh_0.99"
    );
    @Autowired
    private MdmQueueStatisticsRepository queueStatisticsRepository;
    @Autowired
    private SskuToRefreshRepository sskuQueue;
    @Autowired
    private SendToDatacampQueueProcessedRepository sendToDatacampProccessedQueue;
    @Autowired
    private SendToDatacampQueueToProcessRepository sendToDatacampToProcessQueue;
    @Autowired
    private TransactionHelper transactionHelper;
    @Mock
    private MdmSolomonPushService solomonPushService;

    private StorageKeyValueService keyValueService;

    private MdmQueueStatisticsCounterExecutor executor;

    @Before
    public void before() {
        keyValueService = new StorageKeyValueServiceMock();
        executor = new MdmQueueStatisticsCounterExecutor(
            queueStatisticsRepository,
            keyValueService,
            solomonPushService,
            transactionHelper,
            List.of(Mockito.mock(MskuToRefreshRepository.class),
                Mockito.mock(MskuToMboQueueRepository.class),
                sskuQueue,
                sendToDatacampProccessedQueue,
                sendToDatacampToProcessQueue,
                Mockito.mock(SendReferenceItemQRepository.class),
                Mockito.mock(SendToErpQueueRepository.class))
            );

        keyValueService.putValue(MdmProperties.QUEUE_STATISTICS_JOB_ENABLED, true);
        keyValueService.putValue(MdmProperties.QUEUE_STAT_LAST_TS, OLD_TS);
        keyValueService.putValue(MdmProperties.QUEUE_STATISTICS_THREAD_COUNT, 1);
    }

    @Test
    public void testExecutorRunSuccessfully() {
        // given
        Map<String, Map<Instant, Long>> addedItemsExpectedMap = new HashMap<>();
        Map<String, Map<Instant, Long>> processedItemsExpectedMap = new HashMap<>();

        enqueueItems(addedItemsExpectedMap, processedItemsExpectedMap);

        // when
        execute();
        PERCENTILE_METRICS_NAMES.forEach(metricName -> verify(solomonPushService).push(eq(metricName), anyDouble()));

        // then
        checkExpected(SskuToRefreshRepositoryImpl.TABLE,addedItemsExpectedMap,processedItemsExpectedMap);
        checkExpected(SendToDatacampQueueProcessedRepositoryImpl.TABLE,addedItemsExpectedMap,processedItemsExpectedMap);
        checkExpected(SendToDatacampQueueToProcessRepositoryImpl.TABLE,addedItemsExpectedMap,processedItemsExpectedMap);

        clearSendToDatacampProcessedTable();
        clearSendToDatacampToProcessTable();
    }

    /**
     * Проверяет фактические значения метрик в тесте с ожидаемыми
     * @param tableName - название таблицы
     * @param addedItemsExpectedMap - список ожидаемых значений added для всех таблиц
     * @param processedItemsExpectedMap - список ожидаемых значений processed для всех таблиц
     */
    private void checkExpected(String tableName,Map<String,Map<Instant, Long>> addedItemsExpectedMap, Map<String,Map<Instant, Long>> processedItemsExpectedMap) {
        Map<Instant, MdmQueueStatistics> statByTs = queueStatisticsRepository.findAll().stream()
            .filter(mdmQueueStatistics -> mdmQueueStatistics.getQueueName().equals(tableName))
            .collect(Collectors.toMap(MdmQueueStatistics::getTs, Function.identity()));
        Assertions.assertThat(statByTs).isNotEmpty();

        for (Instant ts : Sets.union(addedItemsExpectedMap.get(tableName).keySet(), statByTs.keySet())) {
            Assertions.assertThat(statByTs.get(ts).getAddedCount())
                .isEqualTo(addedItemsExpectedMap.get(tableName).getOrDefault(ts, 0L));
        }
        for (Instant ts : Sets.union(processedItemsExpectedMap.get(tableName).keySet(), statByTs.keySet())) {
            Assertions.assertThat(statByTs.get(ts).getProcessedCount())
                .isEqualTo(processedItemsExpectedMap.get(tableName).getOrDefault(ts, 0L));
        }
    }

    private void execute() {
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();
        executor.execute();
    }

    /**
     * Создаём stub'ы данных для теста
     * @param addedItemsExpectedMap - список ожидаемых значений метрик added для всех таблиц
     * @param processedItemsExpectedMap - список ожидаемых значений метрик processed для всех таблиц
     */
    private void enqueueItems(Map<String,Map<Instant, Long>> addedItemsExpectedMap, Map<String,Map<Instant, Long>> processedItemsExpectedMap) {
        enqueueSSKUToRefreshItems(addedItemsExpectedMap,processedItemsExpectedMap);
        enqueueSendToDatacampProcessedItems(addedItemsExpectedMap,processedItemsExpectedMap);
        enqueueSendToDatacampToProcessItems(addedItemsExpectedMap,processedItemsExpectedMap);
    }

    private void enqueueSSKUToRefreshItems(Map<String,Map<Instant, Long>> addedItemsExpectedMap, Map<String,Map<Instant, Long>> processedItemsExpectedMap) {
        List<ShopSkuKey> keys = List.of(KEY_1, KEY_2, KEY_3);
        sskuQueue.enqueueAll(keys, MdmEnqueueReason.DEFAULT);

        // play with added ts
        Map<ShopSkuKey, List<SskuToRefreshInfo>> infos = sskuQueue.findAll().stream()
            .collect(Collectors.groupingBy(MdmQueueInfoBase::getEntityKey));
        infos.get(KEY_1).get(0).setAddedTimestamp(OLD_TS);
        infos.get(KEY_2).get(0).setAddedTimestamp(OLD_TS);

        // play with processed ts
        infos.get(KEY_1).get(0).setProcessedTimestamp(FRESH_TS);
        infos.get(KEY_2).get(0).setProcessedTimestamp(FRESH_TS);
        infos.get(KEY_1).get(0).setProcessed(true);
        infos.get(KEY_2).get(0).setProcessed(true);

        // KEY_3 remained unchanged - it was enqueued with now() ts and should not be included in the result stats
        // as we do not calculate any stat for current minute
        sskuQueue.updateBatch(List.of(infos.get(KEY_1).get(0), infos.get(KEY_2).get(0)));
        {
            var addedItems = new HashMap<Instant, Long>();
            addedItems.put(OLD_TS.truncatedTo(ChronoUnit.MINUTES), 2L);
            addedItemsExpectedMap.put(SskuToRefreshRepositoryImpl.TABLE, addedItems);
        }

        {
            var processedItems = new HashMap<Instant,Long>();
            processedItems.put(FRESH_TS.truncatedTo(ChronoUnit.MINUTES), 2L);
            processedItemsExpectedMap.put(SskuToRefreshRepositoryImpl.TABLE, processedItems);
        }

    }

    private void clearSendToDatacampToProcessTable() {
        if (sendToDatacampToProcessQueue.countItems()> 0) {
            var founded = sendToDatacampToProcessQueue.findAll().
                stream().filter(item -> item.getShopSku().contains("a-unit-test")).collect(Collectors.toList());
            for (var item:founded) {
                sendToDatacampToProcessQueue.delete(item);
            }
        }
    }

    private void enqueueSendToDatacampToProcessItems(Map<String,Map<Instant, Long>> addedItemsExpectedMap, Map<String,Map<Instant, Long>> processedItemsExpectedMap) {
        clearSendToDatacampToProcessTable();
        sendToDatacampToProcessQueue.insert(generate(OLD_TS,FRESH_TS));
        sendToDatacampToProcessQueue.insert(generate(OLD_TS,FRESH_TS));
        sendToDatacampToProcessQueue.insert(generate(OLD_TS,OLD_TS.minus(10,ChronoUnit.MINUTES)));
        {
            var addedItems = new HashMap<Instant, Long>();
            addedItems.put(OLD_TS.truncatedTo(ChronoUnit.MINUTES), 3L);
            addedItemsExpectedMap.put(SendToDatacampQueueToProcessRepositoryImpl.TABLE, addedItems);
        }

        {
            var processedItems = new HashMap<Instant,Long>();
            processedItems.put(FRESH_TS.truncatedTo(ChronoUnit.MINUTES), 0L);
            processedItemsExpectedMap.put(SendToDatacampQueueToProcessRepositoryImpl.TABLE, processedItems);
        }
        return;
    }

    private void clearSendToDatacampProcessedTable() {
        if (sendToDatacampProccessedQueue.countItems()> 0) {
            var founded = sendToDatacampProccessedQueue.findAll().
                stream().filter(item -> item.getShopSku().contains("a-unit-test")).collect(Collectors.toList());
            for (var item:founded) {
                sendToDatacampProccessedQueue.delete(item);
            }
        }
    }

    private void enqueueSendToDatacampProcessedItems(Map<String,Map<Instant, Long>> addedItemsExpectedMap, Map<String,Map<Instant, Long>> processedItemsExpectedMap) {
        clearSendToDatacampProcessedTable();
        sendToDatacampProccessedQueue.insert(generate(OLD_TS,FRESH_TS));
        sendToDatacampProccessedQueue.insert(generate(OLD_TS,FRESH_TS));
        sendToDatacampProccessedQueue.insert(generate(OLD_TS,OLD_TS.minus(10,ChronoUnit.MINUTES)));
        {
            var addedItems = new HashMap<Instant, Long>();
            addedItems.put(OLD_TS.truncatedTo(ChronoUnit.MINUTES), 3L);
            addedItemsExpectedMap.put(SendToDatacampQueueProcessedRepositoryImpl.TABLE, addedItems);
        }

        {
            var processedItems = new HashMap<Instant,Long>();
            processedItems.put(FRESH_TS.truncatedTo(ChronoUnit.MINUTES), 2L);
            processedItemsExpectedMap.put(SendToDatacampQueueProcessedRepositoryImpl.TABLE, processedItems);
        }
        return;
    }

    /**
     * Генератор минимально необходимой SskuToRefresh для стаба
     * @param added - значение для added_timestamp
     * @param processed - значение для processed_timestamp
     * @return сгенерированную тестовую SskuToRefresh
     */
    private SskuToRefreshInfo generate(Instant added, Instant processed) {
        var newSskuToRefresh = new SskuToRefreshInfo();
        newSskuToRefresh.setShopSku("a-unit-test"+Math.random());
        newSskuToRefresh.setSupplierId((int)Math.round(1000*Math.random()));
        newSskuToRefresh.setProcessed(true);
        newSskuToRefresh.setAddedTimestamp(added);
        newSskuToRefresh.setProcessedTimestamp(processed);

        return newSskuToRefresh;
    }
}
