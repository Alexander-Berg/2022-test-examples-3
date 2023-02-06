package ru.yandex.market.mbo.mdm.common.masterdata.repository.queue;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.apache.commons.lang3.mutable.MutableInt;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.metrics.models.MdmQueueStatistics;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueueInfoBase;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.BatchProcessingProperties.BatchProcessingPropertiesBuilder.constantBatchProperties;
import static ru.yandex.market.mboc.common.utils.MdmProperties.OLD_MDM_QUEUE_DATA_HISTORY_IN_HOURS;

/**
 * @author dmserebr
 * @date 04/03/2020
 */
@SuppressWarnings("checkstyle:magicnumber")
public abstract class MdmQueueRepositoryTestBase<T extends MdmQueueInfoBase<K>, K>
    extends MdmBaseDbTestClass {
    private static final Instant ACTUAL_TS = Instant.parse("2007-12-03T10:15:30.00Z");
    private static final Instant OLD_TS = ACTUAL_TS.minus(4, ChronoUnit.MINUTES);
    private static final Instant FRESH_TS = ACTUAL_TS.minus(3, ChronoUnit.MINUTES);
    private static final Instant ALMOST_ACTUAL_TS = ACTUAL_TS.minus(1, ChronoUnit.MINUTES);

    protected EnhancedRandom random;

    protected abstract MdmQueueBaseRepository<T, K> getRepository();

    protected abstract K getRandomKey();

    protected abstract T createEmptyInfo();

    @Autowired
    private StorageKeyValueService storageKeyValueService;

    @Before
    public void setup() {
        random = TestDataUtils.defaultRandom(1280);
    }

    @Test
    public void testSimpleInsert() {
        var key = getRandomKey();
        var reason = getRandomReason();

        getRepository().enqueue(key, reason);

        var result = getRepository().getUnprocessedBatch(1).get(0);
        assertThat(result.getEntityKey()).isEqualTo(key);
        assertThat(result.getOnlyReasons()).containsExactlyInAnyOrder(reason);
    }

    @Test
    public void testSimpleInsertWithSeveralReasons() {
        var key = getRandomKey();
        var reason1 = getRandomReason();
        var reason2 = getRandomReason();
        var reason3 = getRandomReason();

        getRepository().enqueue(key, reason1);
        getRepository().enqueue(key, reason2);
        getRepository().enqueue(key, reason3);

        Set<MdmEnqueueReason> expectedReasons = new LinkedHashSet<>();
        expectedReasons.add(reason1);
        expectedReasons.add(reason2);
        expectedReasons.add(reason3);

        var result = getRepository().getUnprocessedBatch(1).get(0);
        assertThat(result.getEntityKey()).isEqualTo(key);
        assertThat(result.getOnlyReasons()).containsExactlyInAnyOrderElementsOf(expectedReasons);
    }

    @Test
    public void testMultipleInsertAndGetFromQueue() {
        List<SingularQueueItem<K>> queueKeys = new ArrayList<>();
        for (int i = 0; i < 30; ++i) {
            var queueItem = new SingularQueueItem<>(getRandomKey(), getRandomReason());
            queueKeys.add(queueItem);
        }

        queueKeys.forEach(q -> getRepository().enqueue(q.getKey(), q.getReason()));
        List<Long> ids = getRepository().findAll()
            .stream()
            .map(T::getId)
            .sorted()
            .collect(Collectors.toList());

        var result = getRepository().getUnprocessedBatch(1).stream()
            .map(info -> new SingularQueueItem<>(
                info.getEntityKey(),
                info.getOnlyReasons().stream().findFirst().get()))
            .collect(Collectors.toList());
        assertThat(result).isEqualTo(List.of(queueKeys.get(0)));

        // if not marked as processed yet, should return the same
        var result2 = getRepository().getUnprocessedBatch(1).stream()
            .map(info -> new SingularQueueItem<>(
                info.getEntityKey(),
                info.getOnlyReasons().stream().findFirst().get()))
            .collect(Collectors.toList());
        assertThat(result2).isEqualTo(List.of(queueKeys.get(0)));

        getRepository().markProcessed(List.of(ids.get(0)));

        // if marked as processed, should take next value
        var result3 = getRepository().getUnprocessedBatch(1).stream()
            .map(info -> new SingularQueueItem<>(
                info.getEntityKey(),
                info.getOnlyReasons().stream().findFirst().get()))
            .collect(Collectors.toList());
        assertThat(result3).isEqualTo(List.of(queueKeys.get(1)));

        getRepository().markProcessed(List.of(ids.get(1)));

        // try to get many items
        var result4 = getRepository().getUnprocessedBatch(20).stream()
            .map(info -> new SingularQueueItem<>(
                info.getEntityKey(),
                info.getOnlyReasons().stream().findFirst().get()))
            .collect(Collectors.toList());

        assertThat(result4)
            .containsExactlyInAnyOrderElementsOf(queueKeys.stream().skip(2).limit(20).collect(Collectors.toList()));
    }

    @Test
    public void testTryToGetMoreThanExists() {
        List<SingularQueueItem<K>> queueKeys = new ArrayList<>();
        for (int i = 0; i < 30; ++i) {
            var queueItem = new SingularQueueItem<>(getRandomKey(), getRandomReason());
            queueKeys.add(queueItem);
        }
        queueKeys.forEach(q -> getRepository().enqueue(q.getKey(), q.getReason()));

        // try to get many items
        var result = getRepository().getUnprocessedBatch(50);
        assertThat(result).containsExactlyInAnyOrderElementsOf(result);

        var foundKeys = result.stream()
            .map(info -> new SingularQueueItem<>(
                info.getEntityKey(),
                info.getOnlyReasons().stream().findFirst().get()))
            .collect(Collectors.toList());
        assertThat(foundKeys).hasSize(30);
        assertThat(foundKeys).containsExactlyInAnyOrderElementsOf(foundKeys);

        // check that the queue becomes empty after marked as processed
        var foundIds = result.stream().map(MdmQueueInfoBase::getId).collect(Collectors.toList());
        getRepository().markProcessed(foundIds);

        var emptyResult = getRepository().getUnprocessedBatch(10);
        assertThat(emptyResult).isEmpty();
    }

    @Test
    public void testBatchPolling() {
        // Нагенерим повторяющихся ключей с разными ризонами.
        K entityKey1 = getRandomKey();
        K entityKey2 = getRandomKey();
        K entityKey3 = getRandomKey();
        assertThat(entityKey1).isNotEqualTo(entityKey2);
        assertThat(entityKey2).isNotEqualTo(entityKey3);
        assertThat(entityKey3).isNotEqualTo(entityKey1);

        List<SingularQueueItem<K>> queueKeys = new ArrayList<>();
        for (int i = 0; i < 30; ++i) {
            var queueKey = new SingularQueueItem<>(randomFrom(entityKey1, entityKey2, entityKey3), getRandomReason());
            queueKeys.add(queueKey);
        }
        queueKeys.forEach(qk -> getRepository().enqueue(qk.getKey(), qk.getReason()));

        MutableInt batchCount = new MutableInt(0);
        List<K> processedKeys = new ArrayList<>();
        getRepository().processUniqueEntitiesInBatches(
            constantBatchProperties(3).build(),
            infos -> {
                List<K> entityKeys = infos.stream().map(MdmQueueInfoBase::getEntityKey).collect(Collectors.toList());
                batchCount.increment();
                processedKeys.addAll(entityKeys);
                return true;
            });

        assertThat(batchCount.intValue()).isEqualTo(1);
        assertThat(processedKeys).containsExactlyInAnyOrder(entityKey1, entityKey2, entityKey3);
        assertThat(processedKeys).hasSize(3);
        assertThat(getRepository().getUnprocessedBatch(Integer.MAX_VALUE)).isEmpty();
    }

    @Test
    public void testBatchPollingRollbacksIfExceptionIsThrown() {
        // Нагенерим повторяющихся ключей с разными ризонами.
        K entityKey1 = getRandomKey();
        K entityKey2 = getRandomKey();
        K entityKey3 = getRandomKey();
        assertThat(entityKey1).isNotEqualTo(entityKey2);
        assertThat(entityKey2).isNotEqualTo(entityKey3);
        assertThat(entityKey3).isNotEqualTo(entityKey1);

        List<SingularQueueItem<K>> queueKeys = new ArrayList<>();
        for (int i = 0; i < 30; ++i) {
            var queueKey = new SingularQueueItem<>(randomFrom(entityKey1, entityKey2, entityKey3), getRandomReason());
            queueKeys.add(queueKey);
        }
        queueKeys.forEach(qk -> getRepository().enqueue(qk.getKey(), qk.getReason()));

        MutableInt batchCount = new MutableInt(0);
        try {
            getRepository().processUniqueEntitiesInBatches(
                constantBatchProperties(3).build(),
                infos -> {
                    batchCount.increment();
                    throw new RuntimeException("boom!");
                });
            fail("Should fail with exception");
        } catch (Exception any) {
            assertThat(batchCount.intValue()).isEqualTo(1);
            assertThat(getRepository().getUnprocessedBatch(Integer.MAX_VALUE)).hasSize(3);
        }

    }

    @Test
    public void testBatchPollingRollbacksIfFailIsReturned() {
        // Нагенерим повторяющихся ключей с разными ризонами.
        K entityKey1 = getRandomKey();
        K entityKey2 = getRandomKey();
        K entityKey3 = getRandomKey();
        assertThat(entityKey1).isNotEqualTo(entityKey2);
        assertThat(entityKey2).isNotEqualTo(entityKey3);
        assertThat(entityKey3).isNotEqualTo(entityKey1);

        List<SingularQueueItem<K>> queueKeys = new ArrayList<>();
        for (int i = 0; i < 30; ++i) {
            var queueKey = new SingularQueueItem<>(randomFrom(entityKey1, entityKey2, entityKey3), getRandomReason());
            queueKeys.add(queueKey);
        }
        queueKeys.forEach(qk -> getRepository().enqueue(qk.getKey(), qk.getReason()));

        MutableInt batchCount = new MutableInt(0);
        getRepository().processUniqueEntitiesInBatches(
            constantBatchProperties(3).build(),
            infos -> {
                batchCount.increment();
                return false; // <-- падение
            });
        assertThat(batchCount.intValue()).isEqualTo(1);
        assertThat(getRepository().getUnprocessedBatch(Integer.MAX_VALUE)).hasSize(3);
    }

    @Test
    public void testProcessedItemsDeletedAfterBatchPolling() {
        // Test special mode when processed items are deleted instead of marking as proccessed.
        K entityKey1 = getRandomKey();
        K entityKey2 = getRandomKey();
        K entityKey3 = getRandomKey();

        List<SingularQueueItem<K>> queueKeys = new ArrayList<>();
        for (int i = 0; i < 30; ++i) {
            var queueKey = new SingularQueueItem<>(randomFrom(entityKey1, entityKey2, entityKey3), getRandomReason());
            queueKeys.add(queueKey);
        }
        queueKeys.forEach(qk -> getRepository().enqueue(qk.getKey(), qk.getReason()));

        getRepository().processUniqueEntitiesInBatches(
            constantBatchProperties(3).deleteProcessed(true).build(),
            infos -> true);

        // all records are deleted after processing
        assertThat(getRepository().findAll()).isEmpty();
    }

    @Test
    public void testMaximumWaitingTimeInQueueSeconds() {
        var key = getRandomKey();
        var reason = getRandomReason();
        getRepository().enqueue(key, reason);

        var item = getRepository().getUnprocessedBatch(1).get(0);
        Instant addTime = Instant.now();
        item.setAddedTimestamp(addTime);
        getRepository().insert(item);

        var result = getRepository().maximumWaitingTimeInQueueSeconds();
        assertThat(result).isGreaterThanOrEqualTo(Duration.between(addTime, Instant.now()).getSeconds());
    }

    @Test
    public void whenAllItemInQueueProcessedMaximumWaitingTimeInQueueSecondsIsZero() {
        var key = getRandomKey();
        var reason = getRandomReason();
        getRepository().enqueue(key, reason);

        var item = getRepository().getUnprocessedBatch(1).get(0);
        item.setProcessed(true);
        getRepository().update(item);

        var result = getRepository().maximumWaitingTimeInQueueSeconds();
        assertThat(result).isEqualTo(0);
    }

    protected MdmEnqueueReason getRandomReason() {
        var enumValues = MdmEnqueueReason.values();
        return enumValues[random.nextInt(enumValues.length)];
    }

    @Test
    public void testDeletingOldData() {
        // not store history after cleaning
        storageKeyValueService.putValue(OLD_MDM_QUEUE_DATA_HISTORY_IN_HOURS, -1);

        var key1 = getRandomKey();
        var reason1 = getRandomReason();
        var key2 = getRandomKey();
        var reason2 = getRandomReason();
        Assertions.assertThat(key1).isNotEqualTo(key2);
        getRepository().enqueue(key1, reason1);
        getRepository().enqueue(key2, reason2);
        Assertions.assertThat(getRepository().totalCount()).isEqualTo(2);
        Assertions.assertThat(getRepository().getUnprocessedItemsCount()).isEqualTo(2);

        getRepository().processUniqueEntitiesInBatches(
            BatchProcessingProperties.BatchProcessingPropertiesBuilder
                .constantBatchProperties(1)
                .deleteProcessed(false)
                .usePriorities(false)
                .build(),
            list -> true
        );
        Assertions.assertThat(getRepository().totalCount()).isEqualTo(2);
        Assertions.assertThat(getRepository().getUnprocessedItemsCount()).isEqualTo(0);

        var key3 = getRandomKey();
        var reason3 = getRandomReason();
        Assertions.assertThat(key1).isNotEqualTo(key3);
        Assertions.assertThat(key2).isNotEqualTo(key3);
        getRepository().enqueue(key3, reason3);
        Assertions.assertThat(getRepository().totalCount()).isEqualTo(3);
        Assertions.assertThat(getRepository().getUnprocessedItemsCount()).isEqualTo(1);

        getRepository().clearOldData();
        Assertions.assertThat(getRepository().totalCount()).isEqualTo(1);
        Assertions.assertThat(getRepository().findAll().iterator().next().getEntityKey()).isEqualTo(key3);
    }

    @Test
    public void testCountAddedAndProcessedStatistics() {
        // given
        Map<Instant, Long> addedItemsExpectedMap = new HashMap<>();
        Map<Instant, Long> processedItemsExpectedMap = new HashMap<>();
        enqueueItems(addedItemsExpectedMap, processedItemsExpectedMap);

        // when
        Map<Instant, MdmQueueStatistics> resultStat =
            getRepository().countAddedAndProcessedStatistics(OLD_TS, ACTUAL_TS).stream()
                .collect(Collectors.toMap(MdmQueueStatistics::getTs, Function.identity()));

        // then
        Assertions.assertThat(resultStat.size()).isEqualTo(4); // ACTUAL_TS - OLD_TS

        for (Instant ts = OLD_TS.truncatedTo(ChronoUnit.MINUTES);
             ts.isBefore(ACTUAL_TS.truncatedTo(ChronoUnit.MINUTES));
             ts = ts.plus(1, ChronoUnit.MINUTES)) {
            Assertions.assertThat(resultStat.get(ts).getAddedCount())
                .isEqualTo(addedItemsExpectedMap.getOrDefault(ts, 0L));
            Assertions.assertThat(resultStat.get(ts).getProcessedCount())
                .isEqualTo(processedItemsExpectedMap.getOrDefault(ts, 0L));
        }
    }

    @Test
    public void testBatchWeightRestrictions() {
        K entityKey1 = getRandomKey();
        K entityKey2 = getRandomKey();
        K entityKey3 = getRandomKey();
        K entityKey4 = getRandomKey();
        K entityKey5 = getRandomKey();

        List<SingularQueueItem<K>> queueKeys = new ArrayList<>();
        queueKeys.add(new SingularQueueItem<>(entityKey1, getRandomReason()));
        queueKeys.add(new SingularQueueItem<>(entityKey2, getRandomReason()));
        queueKeys.add(new SingularQueueItem<>(entityKey3, getRandomReason()));
        queueKeys.add(new SingularQueueItem<>(entityKey4, getRandomReason()));
        queueKeys.add(new SingularQueueItem<>(entityKey5, getRandomReason()));

        queueKeys.forEach(qk -> getRepository().enqueue(qk.getKey(), qk.getReason()));

        MutableInt batchCount = new MutableInt(0);
        List<K> processedKeys = new ArrayList<>();
        getRepository().processUniqueEntitiesInBatches(
            constantBatchProperties(100).maxBatchWeight(() -> 1).build(),
            infos -> {
                List<K> entityKeys = infos.stream().map(MdmQueueInfoBase::getEntityKey).collect(Collectors.toList());
                batchCount.increment();
                processedKeys.addAll(entityKeys);
                return true;
            });

        assertThat(batchCount.intValue()).isEqualTo(5);
        assertThat(processedKeys).containsExactlyInAnyOrder(entityKey1, entityKey2, entityKey3, entityKey4, entityKey5);
        assertThat(processedKeys).hasSize(5);
        assertThat(getRepository().getUnprocessedBatch(Integer.MAX_VALUE)).isEmpty();
    }

    private void enqueueItems(Map<Instant, Long> addedItemsExpectedMap, Map<Instant, Long> processedItemsExpectedMap) {
        var reason = getRandomReason();
        var key1 = getRandomKey();
        var key2 = getRandomKey();
        var key3 = getRandomKey();
        var key4 = getRandomKey();
        var key5 = getRandomKey();
        getRepository().enqueue(key1, reason);
        getRepository().enqueue(key2, reason);
        getRepository().enqueue(key3, reason);
        getRepository().enqueue(key4, reason);
        getRepository().enqueue(key5, reason);

        // play with added ts
        Map<K, List<T>> infos = getRepository().findAll().stream()
            .collect(Collectors.groupingBy(MdmQueueInfoBase::getEntityKey));
        infos.get(key1).get(0).setAddedTimestamp(OLD_TS);
        infos.get(key2).get(0).setAddedTimestamp(OLD_TS);
        infos.get(key3).get(0).setAddedTimestamp(FRESH_TS);
        infos.get(key4).get(0).setAddedTimestamp(FRESH_TS);

        // play with processed ts
        infos.get(key1).get(0).setProcessedTimestamp(FRESH_TS);
        infos.get(key2).get(0).setProcessedTimestamp(FRESH_TS);
        infos.get(key3).get(0).setProcessedTimestamp(ALMOST_ACTUAL_TS);
        infos.get(key4).get(0).setProcessedTimestamp(ALMOST_ACTUAL_TS);
        infos.get(key1).get(0).setProcessed(true);
        infos.get(key2).get(0).setProcessed(true);
        infos.get(key3).get(0).setProcessed(true);
        infos.get(key4).get(0).setProcessed(true);

        // key5 remained unchanged - it was enqueued with now() ts and should not be included in the result stats
        // as we do not calculate any stat for current minute
        getRepository().updateBatch(List.of(infos.get(key1).get(0), infos.get(key2).get(0),
            infos.get(key3).get(0), infos.get(key4).get(0)));

        addedItemsExpectedMap.put(OLD_TS.truncatedTo(ChronoUnit.MINUTES), 2L);
        addedItemsExpectedMap.put(FRESH_TS.truncatedTo(ChronoUnit.MINUTES), 2L);

        processedItemsExpectedMap.put(FRESH_TS.truncatedTo(ChronoUnit.MINUTES), 2L);
        processedItemsExpectedMap.put(ALMOST_ACTUAL_TS.truncatedTo(ChronoUnit.MINUTES), 2L);
    }

    @Test
    public void whenDeduplicateInfosKeepAllUniqueReasons() {
        // given
        K key = getRandomKey();

        List<MdmEnqueueReason> existingReasons = List.of(
            MdmEnqueueReason.CHANGED_BY_MDM_OPERATOR,
            MdmEnqueueReason.DEVELOPER_TOOL,
            MdmEnqueueReason.CHANGED_CATEGORY_SETTING
        );
        List<T> existingInfos = IntStream.range(0, 3)
            .mapToObj(i -> createInfo(key, existingReasons.get(i)))
            .collect(Collectors.toList());
        getRepository().insertOrUpdateAll(existingInfos);

        MdmEnqueueReason newReason = MdmEnqueueReason.CHANGED_BY_MBO_OPERATOR;

        List<MdmEnqueueReason> expectedReasons = new ArrayList<>(existingReasons);
        expectedReasons.add(newReason);

        //when
        getRepository().enqueue(key, newReason);

        //then
        assertThat(getRepository().totalCount()).isEqualTo(3); // все те же 3 info
        List<T> unprocessed = getRepository().getUnprocessedBatch(100);
        assertThat(unprocessed).hasSize(1); // но два info стали processed + discarded
        // а оставшаяся вобрала все из 3 существующих + 1 новой
        T info = unprocessed.iterator().next();
        assertThat(info.getEntityKey()).isEqualTo(key);
        assertThat(info.getOnlyReasons()).containsExactlyInAnyOrderElementsOf(expectedReasons);
    }

    protected final T createInfo(K key, MdmEnqueueReason reason) {
        T info = createEmptyInfo();
        info.setEntityKey(key);
        info.addRefreshReason(reason);
        return info;
    }

    @SafeVarargs
    protected final <X> X randomFrom(X... ts) {
        return ts[random.nextInt(ts.length)];
    }

    private static class SingularQueueItem<EntityKey> {
        private final EntityKey key;
        private final MdmEnqueueReason reason;

        SingularQueueItem(EntityKey key, MdmEnqueueReason reason) {
            this.key = key;
            this.reason = reason;
        }

        public EntityKey getKey() {
            return key;
        }

        public MdmEnqueueReason getReason() {
            return reason;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SingularQueueItem<?> that = (SingularQueueItem<?>) o;
            return Objects.equals(key, that.key) &&
                reason == that.reason;
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, reason);
        }
    }
}
