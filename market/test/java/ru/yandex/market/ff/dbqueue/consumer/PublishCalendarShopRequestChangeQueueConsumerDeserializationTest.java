package ru.yandex.market.ff.dbqueue.consumer;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.model.dbqueue.PublishCalendaringChangePayload;
import ru.yandex.market.ff.model.dto.CalendaringChangeDTO;
import ru.yandex.market.ff.service.enums.LogbrokerTopic;

public class PublishCalendarShopRequestChangeQueueConsumerDeserializationTest
        extends IntegrationTestWithDbQueueConsumers {
    private static final String GIVEN_PAYLOAD_STRING =
            "{\"oldMeta\":{\"ffwfId\":1,\"status\":\"IN_PROGRESS\"}," +
                    "\"newMeta\":{\"ffwfId\":1," +
                    "\"status\":\"PROCESSED\"}," +
                    "\"updatedTime\":\"2018-01-01T10:10:10\"," +
                    "\"topic\":\"CALENDARING_META_INFO_CHANGE_EVENTS\",\"source\":\"FFWF\"," +
                    "\"externalId\":1, \"bookingId\":2}";

    @Autowired
    private PublishCalendarShopRequestChangeQueueConsumer consumer;

    @Test
    void testDeserializationWorks() {
        CalendaringChangeDTO oldMeta = new CalendaringChangeDTO();
        oldMeta.setFfwfId(1);
        oldMeta.setStatus(RequestStatus.IN_PROGRESS.name());
        CalendaringChangeDTO newMeta = new CalendaringChangeDTO();
        newMeta.setFfwfId(1);
        newMeta.setStatus(RequestStatus.PROCESSED.name());

        var expected = new PublishCalendaringChangePayload(
                1L,
                oldMeta,
                newMeta,
                LocalDateTime.of(2018, 1, 1, 10, 10, 10),
                LogbrokerTopic.CALENDARING_META_INFO_CHANGE_EVENTS,
                "FFWF"
        );

        var actual = consumer.getPayloadTransformer().toObject(GIVEN_PAYLOAD_STRING);

        assertions.assertThat(actual).isEqualToComparingFieldByFieldRecursively(expected);
    }
}
