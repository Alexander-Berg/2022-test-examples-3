package ru.yandex.market.logistics.iris.jobs.producers.serialization;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.core.domain.source.Source;
import ru.yandex.market.logistics.iris.jobs.model.BatchGenerationRequestPayload;
import ru.yandex.market.logistics.iris.jobs.model.ContentSyncTasksGenerationPayload;
import ru.yandex.market.logistics.iris.jobs.model.ItemKeysExecutionQueuePayload;
import ru.yandex.market.logistics.iris.jobs.model.QueueType;
import ru.yandex.market.logistics.iris.jobs.producers.BatchGenerationForPartnerBasedTasksProducer;
import ru.yandex.market.logistics.iris.jobs.producers.ContentSyncTasksGenerationProducer;
import ru.yandex.market.logistics.iris.jobs.producers.ItemKeysQueueProducer;
import ru.yandex.market.logistics.iris.model.ItemIdentifierDTO;

public class PayloadsSerializationTest extends AbstractContextualTest {

    private static final String ITEM_KEY_EXEQUTION_QUEUE_PAYLOAD =
            "{\"request_id\":\"1584179640153/334010892e19321d21401d4a6c68132b\"," +
            "\"itemIdentifiers\":[{\"partner_id\":\"614293\",\"partner_sku\":\"NPDP11YKR(2018)-KNVPBLK\"}," +
            "{\"partner_id\":\"556048\",\"partner_sku\":\"550268\"}]," +
            "\"source\":{\"source_id\":\"0\",\"source_type\":\"fake\"}," +
            "\"uuidfields\":[\"fake\",\"0\"]}";

    private static final String PRODUCE_ASYNC_TASKS_PAYLOAD =
            "{\"request_id\":\"1584179640153/334010892e19321d21401d4a6c68132b\"," +
            "\"sources\":[{\"source_id\":\"1\",\"source_type\":\"content\"},{\"source_id\":\"145\"," +
            "\"source_type\":\"warehouse\"},{\"source_id\":\"0\",\"source_type\":\"fake\"}]," +
            "\"type\":\"referenceSync\"," +
            "\"uuidfields\":[\"referenceSync\",\"1\",\"145\",\"0\"]}";

    private static final String CONTENT_SYNC_TASKS_GENERATION_PAYLOAD =
            "{\"request_id\":\"1584179640153/334010892e19321d21401d4a6c68132b\"," +
            "\"queue_type\":\"contentFullSync\"," +
            "\"from_id\":1,\"to_id\":1000001," +
            "\"uuidfields\":[\"contentFullSync\",1,1000001]}";

    @Autowired
    private List<BatchGenerationForPartnerBasedTasksProducer> batchGenerationForPartnerBasedTasksProducers;
    @Autowired
    private List<ItemKeysQueueProducer> itemKeysQueueProducers;
    @Autowired
    private ContentSyncTasksGenerationProducer contentSyncTasksGenerationProducer;

    @Test
    public void testItemKeysExecutionQueuePayloadSerialize() {
        var payload = new ItemKeysExecutionQueuePayload(
                "1584179640153/334010892e19321d21401d4a6c68132b",
                Arrays.asList(
                        new ItemIdentifierDTO("614293", "NPDP11YKR(2018)-KNVPBLK"),
                        new ItemIdentifierDTO("556048", "550268")
                ),
                Source.FAKE_SOURCE
        );
        for (var producer : itemKeysQueueProducers) {
            var actualPayloadString = producer.getPayloadTransformer().fromObject(payload);
            assertions().assertThat(actualPayloadString).isEqualTo(ITEM_KEY_EXEQUTION_QUEUE_PAYLOAD);
        }
    }

    @Test
    public void testProduceAsyncTasksSerialize() {
        var payload = new BatchGenerationRequestPayload(
                "1584179640153/334010892e19321d21401d4a6c68132b",
                QueueType.REFERENCE_SYNC,
                Arrays.asList(
                        Source.CONTENT,
                        Source.MARSCHROUTE,
                        Source.FAKE_SOURCE
                ));
        for (var producer : batchGenerationForPartnerBasedTasksProducers) {
            var actualPayloadString = producer.getPayloadTransformer().fromObject(payload);
            assertions().assertThat(actualPayloadString).isEqualTo(PRODUCE_ASYNC_TASKS_PAYLOAD);
        }
    }

    @Test
    public void testContentSyncTasksGenerationSerialize() {
        var payload = new ContentSyncTasksGenerationPayload(
                "1584179640153/334010892e19321d21401d4a6c68132b",
                QueueType.CONTENT_FULL_SYNC,
                1, 1000001);
        String actualPayloadString = contentSyncTasksGenerationProducer.getPayloadTransformer().fromObject(payload);
        assertions().assertThat(actualPayloadString).isEqualTo(CONTENT_SYNC_TASKS_GENERATION_PAYLOAD);
    }

}
