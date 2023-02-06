package ru.yandex.market.tpl.core.domain.usershift.publish.queue;

import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.common.db.queue.base.JsonSerializablePayloadTransformer;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;

import static org.assertj.core.api.Assertions.assertThat;

class CollectPublishUserShiftPayloadDeserializationTest {
    @Test
    void correctDeserializationOldPayloads() {
        //when
        var payload = JsonSerializablePayloadTransformer.of(QueueType.COLLECT_PUBLISHED_USER_SHIFTS)
                .toObject("{\"requestId\":\"null/1/1/1/1\",\"shiftId\":1,\"scId\":47819," +
                        "\"processingId\":\"processingId\"," +
                        "\"manual\":false,\"profile\":\"GROUP_FALLBACK_1\",\"entityId\":\"1:47819\"}");

        //then
        assertThat(payload).isNotNull();
        assertThat(payload).isInstanceOf(CollectPublishUserShiftPayload.class);
        assertThat(((CollectPublishUserShiftPayload) payload).getRoutingRequestId()).isNull();
    }
}
