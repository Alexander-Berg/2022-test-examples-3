package ru.yandex.market.logistics.iris.jobs.producers;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.core.domain.source.Source;
import ru.yandex.market.logistics.iris.core.domain.source.SourceType;
import ru.yandex.market.logistics.iris.jobs.consumers.BaseQueueConsumer;
import ru.yandex.market.logistics.iris.jobs.consumers.BatchGenerationForNotPartnerBasedTasksConsumer;
import ru.yandex.market.logistics.iris.jobs.model.ExecutionQueueItemPayload;
import ru.yandex.market.logistics.iris.jobs.model.QueueType;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.Mockito.doReturn;

public class ProducerExecutorTest extends AbstractContextualTest {

    @Autowired
    private ProducerExecutor executor;

    @Autowired
    private BatchGenerationForPartnerBasedTasksProducer batchGenerationForPartnerBasedTasksProducer;
    @Autowired
    private BatchGenerationForNotPartnerBasedTasksProducer batchGenerationForNotPartnerBasedTasksProducer;
    @Autowired
    private ContentFullSyncQueueProducer contentFullSyncQueueProducer;
    @Autowired
    private ReferenceItemPageableQueueProducer referenceItemPageableQueueProducer;
    @Autowired
    private BatchGenerationForNotPartnerBasedTasksConsumer consumerNotForPartner;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Before
    public void before() {
        doReturn("1584179640153/334010892e19321d21401d4a6c68132b")
                .when(batchGenerationForPartnerBasedTasksProducer).getRequestId();
        doReturn("1584179640153/334010892e19321d21401d4a6c68132b")
                .when(batchGenerationForNotPartnerBasedTasksProducer).getRequestId();
        doReturn("1584179640153/334010892e19321d21401d4a6c68132b")
                .when(contentFullSyncQueueProducer).getRequestId();
        doReturn("1584179640153/334010892e19321d21401d4a6c68132b")
                .when(referenceItemPageableQueueProducer).getRequestId();
    }

    @After
    public void reset() {
        Mockito.reset(batchGenerationForPartnerBasedTasksProducer, batchGenerationForNotPartnerBasedTasksProducer,
                contentFullSyncQueueProducer, referenceItemPageableQueueProducer);
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/producers/producer_executor_async.xml")
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            value = "classpath:fixtures/expected/producers/producer_executor_async.xml")
    public void executeAsync() {
        executor.execute(QueueType.CONTENT_FULL_SYNC, List.of(Source.CONTENT, Source.CONTENT));
        executor.execute(QueueType.REFERENCE_SYNC, List.of(Source.MARSCHROUTE, Source.MARSCHROUTE,
                new Source("171", SourceType.WAREHOUSE)));
    }


    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/producers/producer_executor_sync_with_async_content_batches.xml")
    public void executeSyncWithAsyncContentBatchesGeneration() {

        executor.execute(QueueType.CONTENT_FULL_SYNC, Collections.singletonList(Source.CONTENT));
        executor.execute(QueueType.CONTENT_FULL_SYNC, Collections.singletonList(Source.CONTENT));

        consumePayloads(getPayloads(), consumerNotForPartner);

        boolean isPresent = getColumns(2).stream()
                .anyMatch(s -> s.equals("CONTENT_SYNC_TASKS_GENERATION"));

        Assert.assertTrue("Was not found after consume",
                isPresent);
    }

    private <T extends ExecutionQueueItemPayload> void consumePayloads(List<String> payloads,
                                                                       BaseQueueConsumer<T> consumer) {
        payloads.stream()
                .map(getPayloadObject(consumer))
                .map(payload -> new Task<>(new QueueShardId("meh"),
                        payload,
                        1,
                        ZonedDateTime.now(),
                        "mehInfo",
                        null))
                .forEach(consumer::execute);
    }

    @NotNull
    private <T extends ExecutionQueueItemPayload> Function<String, T> getPayloadObject(BaseQueueConsumer<T> consumer) {
        return payload -> consumer.getPayloadTransformer().toObject(payload);
    }

    @NotNull
    private List<String> getColumns(int column) {
        return jdbcTemplate.query(
                "select * from queue_tasks", (rs, num) -> rs.getString(column)
        );
    }

    @NotNull
    private List<String> getPayloads() {
        return getColumns(3);
    }
}
