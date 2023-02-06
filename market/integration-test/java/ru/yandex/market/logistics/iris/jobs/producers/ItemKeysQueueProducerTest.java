package ru.yandex.market.logistics.iris.jobs.producers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.logistics.iris.core.domain.source.Source;
import ru.yandex.market.logistics.iris.jobs.QueueTypeConfigService;
import ru.yandex.market.logistics.iris.jobs.model.ItemKeysExecutionQueuePayload;
import ru.yandex.market.logistics.iris.jobs.model.QueueType;
import ru.yandex.market.logistics.iris.model.ItemIdentifierDTO;
import ru.yandex.market.logistics.iris.model.ItemIdentifierWithSourceDTO;
import ru.yandex.market.logistics.iris.repository.JdbcItemReplicaRepository;
import ru.yandex.market.logistics.iris.service.mbo.sync.ContentFullSyncRequestGenerationService;
import ru.yandex.market.logistics.iris.service.system.SystemPropertyIntegerKey;
import ru.yandex.market.logistics.iris.service.system.SystemPropertyService;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class ItemKeysQueueProducerTest {

    private static final int BATCH_SIZE = 250;

    private SystemPropertyService systemPropertyService;
    private JdbcItemReplicaRepository jdbcItemReplicaRepository;
    private QueueTypeConfigService queueTypeConfigService;

    @Before
    public void init() {
        systemPropertyService = Mockito.mock(SystemPropertyService.class);
        doReturn(1_000_000).when(systemPropertyService)
                .getIntegerProperty(SystemPropertyIntegerKey.LIMIT_FOR_ITEM_KEYS_QUEUE_PRODUCERS);
        jdbcItemReplicaRepository = Mockito.mock(JdbcItemReplicaRepository.class);
        queueTypeConfigService = Mockito.mock(QueueTypeConfigService.class);
        when(queueTypeConfigService.getBatchSize(any())).thenReturn(250);
    }

    @Test
    public void produceTasksSimple() {
        assertProduceTasksCorrect(Arrays.asList(525, 0), 3, 25, 700);
    }

    @Test
    public void produceTasksWhenReturnedExactlyLimit() {
        assertProduceTasksCorrect(Arrays.asList(1_000_000, 0), 400, BATCH_SIZE, 1_500_000);
    }

    private void assertProduceTasksCorrect(List<Integer> countItems, int countTasks, int lastSize, long maxItemId) {
        when(jdbcItemReplicaRepository.getMaxItemId()).thenReturn(maxItemId);
        TestItemKeysQueueProducer queueProducer = createProducer(1, countItems);
        queueProducer.produceTasks(Collections.singletonList(Source.CONTENT));

        List<EnqueueParams<ItemKeysExecutionQueuePayload>> values = queueProducer.enqueueInteractionParams;
        int lastIndex = countTasks - 1;
        for (int i = 0; i < lastIndex; i++) {
            assertCorrectPayload(values.get(i).getPayload(), i * BATCH_SIZE + 1, BATCH_SIZE);
        }
        assertCorrectPayload(values.get(lastIndex).getPayload(), lastIndex * BATCH_SIZE + 1, lastSize);

    }

    private List<ItemIdentifierWithSourceDTO> createIdentifiersWithId(int first, int count) {
        List<ItemIdentifierWithSourceDTO> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int currentIndex = first + i;
            result.add(new ItemIdentifierWithSourceDTO(
                    new ItemIdentifierDTO("partner_id" + currentIndex, "partner_sku" + currentIndex),
                    Source.FAKE_SOURCE
            ));
        }
        return result;
    }

    private List<ItemIdentifierDTO> createIdentifiers(int first, int count) {
        return createIdentifiersWithId(first, count).stream()
                .map(ItemIdentifierWithSourceDTO::getItemIdentifier)
                .collect(Collectors.toList());
    }

    private TestItemKeysQueueProducer createProducer(int first, List<Integer> counts) {
        List<List<ItemIdentifierWithSourceDTO>> allIdentifiers = new ArrayList<>();
        List<ItemIdentifierWithSourceDTO> firstIdentifiers = createIdentifiersWithId(first, counts.get(0));
        allIdentifiers.add(firstIdentifiers);
        for (int i = 1; i < counts.size(); i++) {
            allIdentifiers.add(createIdentifiersWithId(first + counts.get(i - 1), counts.get(i)));
        }
        return new TestItemKeysQueueProducer(allIdentifiers, systemPropertyService, jdbcItemReplicaRepository,
                queueTypeConfigService);
    }

    private void assertCorrectPayload(ItemKeysExecutionQueuePayload payload, int first, int count) {
        List<ItemIdentifierDTO> identifiers = createIdentifiers(first, count);
        Assert.assertEquals(identifiers, payload.getItemIdentifiers());
        Assert.assertEquals(Source.FAKE_SOURCE, payload.getSource());
    }

    private static class TestItemKeysQueueProducer extends ItemKeysQueueProducer {

        private final List<List<ItemIdentifierWithSourceDTO>> results;
        private final List<EnqueueParams<ItemKeysExecutionQueuePayload>> enqueueInteractionParams;
        private final AtomicInteger resultsIndex = new AtomicInteger(0);

        TestItemKeysQueueProducer(List<List<ItemIdentifierWithSourceDTO>> results,
                                  SystemPropertyService systemPropertyService,
                                  JdbcItemReplicaRepository jdbcItemReplicaRepository,
                                  QueueTypeConfigService queueTypeConfigService) {
            super(QueueType.CONTENT_FULL_SYNC,
                    new ContentFullSyncRequestGenerationService(systemPropertyService, jdbcItemReplicaRepository),
                    queueTypeConfigService);
            this.results = results;
            this.enqueueInteractionParams = new ArrayList<>();
        }

        @Override
        protected List<ItemIdentifierWithSourceDTO> getItemIdentifiersWithId(long minId, Collection<Source> sources,
                                                                             int limit) {
            return results.get(resultsIndex.getAndIncrement());
        }

        @Override
        public long enqueue(@Nonnull EnqueueParams<ItemKeysExecutionQueuePayload> param) {
            enqueueInteractionParams.add(param);
            return 1;
        }
    }
}
