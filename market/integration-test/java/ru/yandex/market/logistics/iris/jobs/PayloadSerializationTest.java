package ru.yandex.market.logistics.iris.jobs;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.configuration.queue.DbQueueConfiguration;
import ru.yandex.market.logistics.iris.configuration.queue.support.JsonSerializablePayloadTransformer;
import ru.yandex.market.logistics.iris.core.domain.source.Source;
import ru.yandex.market.logistics.iris.core.domain.source.SourceType;
import ru.yandex.market.logistics.iris.jobs.model.LoopingQueueItemPayload;
import ru.yandex.market.logistics.iris.jobs.model.QueueType;
import ru.yandex.market.logistics.iris.jobs.model.TraceableExecutionQueuePayload;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

public class PayloadSerializationTest extends AbstractContextualTest {

    private static final String REQUEST_ID = "TestRequestId";

    @Autowired
    @Qualifier(DbQueueConfiguration.DB_QUEUE_OBJECT_MAPPER)
    private ObjectMapper objectMapper;

    @Before
    public void setup() {
        RequestContextHolder.createContext(Optional.of(REQUEST_ID));
    }

    /**
     * TODO:
     * Тест перенес as is, подумать над его необходимостью в целом.
     */
    @Test
    public void LoopingPayloadSerializeDeserialize() {
        JsonSerializablePayloadTransformer<LoopingQueueItemPayload> transformer =
            (JsonSerializablePayloadTransformer<LoopingQueueItemPayload>)
                new JsonSerializablePayloadTransformer(QueueType.REFERENCE_SYNC,
                    LoopingQueueItemPayload.class,
                    objectMapper);
        LoopingQueueItemPayload expected =
            new LoopingQueueItemPayload(REQUEST_ID, 20, new Source("123", SourceType.WAREHOUSE));
        String res = transformer.fromObject(expected);
        LoopingQueueItemPayload deserialized = transformer.toObject(res);
        assertPayloads(expected, deserialized);
    }

    private <T extends TraceableExecutionQueuePayload> void assertPayloads(T expected, T deserialized) {
        assertThat(expected)
            .as("Asserting that serialized and deserialized objects are the same")
            .isEqualToComparingFieldByFieldRecursively(deserialized);

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(deserialized).isEqualToComparingFieldByFieldRecursively(expected);
            softAssertions.assertThat(deserialized.getRequestId()).isNotBlank();
            softAssertions
                .assertThat(deserialized.getRequestId())
                .isEqualTo(RequestContextHolder.getContext().getRequestId());
            softAssertions.assertThat(deserialized.getRequestId()).isEqualTo(expected.getRequestId());
        });
    }
}
