package ru.yandex.market.mbo.mdm.common.masterdata.repository.queue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueueInfoBase;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.BatchProcessingProperties.BatchProcessingPropertiesBuilder.constantBatchProperties;

public abstract class MdmPriorityQueueRepositoryTestBase<T extends MdmQueueInfoBase<K>, K>
    extends MdmQueueRepositoryTestBase<T, K> {

    @Test
    public void testSimpleInsert() {
        var key = getRandomKey();
        var reason = getRandomReason();
        int priority = 123456789;

        getRepository().enqueue(key, reason, priority);

        var result = getRepository().getUnprocessedBatch(1).get(0);
        assertThat(result.getEntityKey()).isEqualTo(key);
        assertThat(result.getOnlyReasons()).containsExactlyInAnyOrder(reason);
        assertThat(result.getPriority()).isEqualTo(priority);
    }

    @Test
    public void testSimpleInsertWithSeveralPriorities() {
        var key = getRandomKey();

        var reason1 = getRandomReason();
        var reason2 = getRandomReason();
        var reason3 = getRandomReason();

        int priority1 = -1;
        int priority2 = 25;
        int priority3 = 100500;

        getRepository().enqueue(key, reason1, priority1);
        getRepository().enqueue(key, reason2, priority2);
        getRepository().enqueue(key, reason3, priority3);

        Set<MdmEnqueueReason> expectedReasons = new LinkedHashSet<>();
        expectedReasons.add(reason1);
        expectedReasons.add(reason2);
        expectedReasons.add(reason3);

        var result = getRepository().getUnprocessedBatch(1).get(0);
        assertThat(result.getEntityKey()).isEqualTo(key);
        assertThat(result.getOnlyReasons()).containsExactlyInAnyOrderElementsOf(expectedReasons);
        assertThat(result.getPriority()).isEqualTo(IntStream.of(priority1, priority2, priority3).max().getAsInt());
    }

    @Test
    public void testPollingWithPriorities() {
        // Нагенерим повторяющихся ключей с разными ризонами.
        K entityKey1 = getRandomKey();
        K entityKey2 = getRandomKey();
        K entityKey3 = getRandomKey();
        assertThat(entityKey1).isNotEqualTo(entityKey2);
        assertThat(entityKey2).isNotEqualTo(entityKey3);
        assertThat(entityKey3).isNotEqualTo(entityKey1);

        getRepository().enqueue(entityKey1, MdmEnqueueReason.DEVELOPER_TOOL, 1213);
        getRepository().enqueue(entityKey2, MdmEnqueueReason.DEVELOPER_TOOL, 121323);
        getRepository().enqueue(entityKey3, MdmEnqueueReason.DEVELOPER_TOOL, 44565);

        MutableInt batchCount = new MutableInt(0);
        List<K> processedKeys = new ArrayList<>();
        getRepository().processUniqueEntitiesInBatches(
            constantBatchProperties(1).usePriorities(true).build(),
            infos -> {
                List<K> entityKeys = infos.stream().map(MdmQueueInfoBase::getEntityKey).collect(Collectors.toList());
                batchCount.increment();
                processedKeys.addAll(entityKeys);
                return true;
            });

        assertThat(batchCount.intValue()).isEqualTo(3);
        assertThat(processedKeys).hasSize(3);
        assertThat(processedKeys).containsExactly(entityKey2, entityKey3, entityKey1);
        assertThat(getRepository().getUnprocessedBatch(Integer.MAX_VALUE)).isEmpty();
    }

    @Test
    public void testSkippingFailed() {
        // Нагенерим повторяющихся ключей с разными ризонами.
        K entityKey1 = getRandomKey();
        K entityKey2 = getRandomKey();
        K entityKey3 = getRandomKey();
        assertThat(entityKey1).isNotEqualTo(entityKey2);
        assertThat(entityKey2).isNotEqualTo(entityKey3);
        assertThat(entityKey3).isNotEqualTo(entityKey1);

        getRepository().enqueue(entityKey1, MdmEnqueueReason.DEVELOPER_TOOL);
        getRepository().enqueue(entityKey2, MdmEnqueueReason.DEVELOPER_TOOL);
        getRepository().enqueue(entityKey3, MdmEnqueueReason.DEVELOPER_TOOL);

        MutableInt batchCount = new MutableInt(0);
        List<K> processedKeys = new ArrayList<>();

        BatchProcessingProperties properties =
            constantBatchProperties(1)
                .requeueFailed(true)
                .build();

        getRepository().processUniqueEntitiesInBatchesConfirmable(properties, infos -> {
            List<K> entityKeys = infos.stream().map(MdmQueueInfoBase::getEntityKey).collect(Collectors.toList());
            batchCount.increment();
            processedKeys.addAll(entityKeys);
            if (batchCount.intValue() < 3) {
                return ProcessingResult.failedResults(entityKeys);
            } else {
                return ProcessingResult.successfulResults(entityKeys);
            }
        });

        assertThat(batchCount.intValue()).isEqualTo(5);
        assertThat(processedKeys).hasSize(5);
        assertThat(processedKeys).containsExactly(entityKey1, entityKey2, entityKey3, entityKey1, entityKey2);
        assertThat(getRepository().getUnprocessedBatch(Integer.MAX_VALUE)).hasSize(0);
    }

    @Test
    public void testDeduplicateOnEmptyDoesNothing() {
        if (queueDoesNotSupportDeduplication()) {
            return;
        }
        Map<?, Integer> stats = getRepository().deduplicate();
        assertThat(stats).isEmpty();
    }

    @Test
    public void testDeduplicateOnSingularNonProcessedDoesNothing() {
        if (queueDoesNotSupportDeduplication()) {
            return;
        }
        var key1 = getRandomKey();
        var key2 = getRandomKey();
        var key3 = getRandomKey();

        getRepository().enqueue(key1, getRandomReason(), 1);
        getRepository().enqueue(key2, getRandomReason(), 1);
        getRepository().enqueue(key3, getRandomReason(), 1);

        Map<?, Integer> stats = getRepository().deduplicate();
        assertThat(stats).isEmpty();
        assertThat(getRepository().findAll()).hasSize(3);
    }

    @Test
    public void testDeduplicateOnSingularProcessedDoesNothing() {
        if (queueDoesNotSupportDeduplication()) {
            return;
        }
        var key1 = getRandomKey();
        var key2 = getRandomKey();
        var key3 = getRandomKey();

        getRepository().enqueue(key1, getRandomReason(), 1);
        getRepository().enqueue(key2, getRandomReason(), 1);
        getRepository().enqueue(key3, getRandomReason(), 1);

        markProcessedAll();

        Map<?, Integer> stats = getRepository().deduplicate();
        assertThat(stats).isEmpty();
        assertThat(getRepository().findAll()).hasSize(3);
    }

    @Test
    public void testDeduplicateOnProcessedAndUnprocessedDoesNothing() {
        if (queueDoesNotSupportDeduplication()) {
            return;
        }
        var key = getRandomKey();
        getRepository().enqueue(key, getRandomReason(), 1);

        markProcessedAll();
        getRepository().enqueue(key, getRandomReason(), 1);

        // в очереди:
        // id=1, <key>, processed=true, reason={...}
        // id=2, <key>, processed=false, reason={...}

        Map<?, Integer> stats = getRepository().deduplicate();
        assertThat(stats).isEmpty();
        assertThat(getRepository().findAll()).hasSize(2);
    }

    @Test
    public void testDeduplicateOnSeveralStackedProcessedDoesNothing() {
        if (queueDoesNotSupportDeduplication()) {
            return;
        }
        var key = getRandomKey();
        getRepository().enqueue(key, getRandomReason(), 1);

        markProcessedAll();
        getRepository().enqueue(key, getRandomReason(), 1);
        markProcessedAll();

        // в очереди:
        // id=1, <key>, processed=true, reason={...}
        // id=2, <key>, processed=true, reason={...}

        Map<?, Integer> stats = getRepository().deduplicate();
        assertThat(stats).isEmpty();
        assertThat(getRepository().findAll()).hasSize(2);
    }

    @Test
    public void testDeduplicateOnSeveralStackedUnprocessedRemovesDupes() {
        if (queueDoesNotSupportDeduplication()) {
            return;
        }
        var key = getRandomKey();
        getRepository().enqueue(key, getRandomReason(), 1);

        markProcessedAll();
        getRepository().enqueue(key, getRandomReason(), 1);
        dupeLastRecord();
        dupeLastRecord();

        // в очереди:
        // id=1, <key>, processed=true, reason={...}
        // id=2, <key>, processed=false, reason={...}
        // id=3, <key>, processed=false, reason={...}
        // id=4, <key>, processed=false, reason={...}

        Map<K, Integer> stats = getRepository().deduplicate();
        assertThat(stats).containsOnlyKeys(key);
        assertThat(stats.get(key)).isEqualTo(2);
        assertThat(getRepository().findAll()).hasSize(2);
        assertThat(lastRecord().isProcessed()).isFalse();
    }

    @Test
    public void testDeduplicateOnSeveralShuffledUnprocessedRemovesDupes() {
        if (queueDoesNotSupportDeduplication()) {
            return;
        }
        var key = getRandomKey();
        getRepository().enqueue(key, getRandomReason(), 1);
        dupeLastRecord();
        var last = lastRecord();
        last.setProcessed(true);
        getRepository().insert(last);
        last.setProcessed(false);
        getRepository().insert(last);

        // в очереди:
        // id=1, <key>, processed=false, reason={...}
        // id=2, <key>, processed=false, reason={...}
        // id=3, <key>, processed=true, reason={...}
        // id=4, <key>, processed=false, reason={...}

        Map<K, Integer> stats = getRepository().deduplicate();
        assertThat(stats).containsOnlyKeys(key);
        assertThat(stats.get(key)).isEqualTo(2);
        assertThat(getRepository().findAll()).hasSize(2);
        assertThat(lastRecord().isProcessed()).isFalse();
    }

    private void markProcessedAll() {
        getRepository().markProcessed(getRepository().findAll().stream().map(MdmQueueInfoBase::getId)
            .collect(Collectors.toList()));
    }

    private void dupeLastRecord() {
        getRepository().insert(lastRecord());
    }

    private T lastRecord() {
        return getRepository().findAll().stream().max(Comparator.comparing(MdmQueueInfoBase::getId)).get();
    }

    private boolean queueDoesNotSupportDeduplication() {
        try {
            getRepository().deduplicate();
            return false;
        } catch (UnsupportedOperationException ignored) {
            return true;
        }
    }
}
