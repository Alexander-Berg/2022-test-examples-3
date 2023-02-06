package ru.yandex.market.ff.dbqueue.consumer;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.model.dbqueue.PublishCalendaringChangePayload;
import ru.yandex.market.ff.model.dto.CalendaringChangeDTO;
import ru.yandex.market.ff.service.enums.LogbrokerTopic;

public class PublishCalendarShopRequestChangeQueueConsumerTest extends IntegrationTestWithDbQueueConsumers {

    private static final String GIVEN_PAYLOAD_STRING =
            "{\"externalId\":823600,\"oldMeta\":{\"ffwfId\":0},\"newMeta\":" +
                    "{\"ffwfId\":823600,\"externalRequestId\":\"TMU242771\"," +
                    "\"requestType\":\"MOVEMENT_WITHDRAW\",\"" +
                    "readableRequestType\":\"Изъятие для межскладского перемещения\",\"status\":\"CREATED\"," +
                    "\"itemsCount\":0,\"palletsCount\":1},\"updatedTime\":\"2021-08-03T15:00:00\"," +
                    "\"topic\":\"CALENDARING_META_INFO_CHANGE_EVENTS\",\"source\":\"FFWF\"}";

    @Autowired
    private PublishCalendarShopRequestChangeQueueConsumer consumer;

    @Test
    void testDeserializationWorks() {

        CalendaringChangeDTO oldMeta = new CalendaringChangeDTO();
        oldMeta.setFfwfId(0);
        CalendaringChangeDTO newMeta = new CalendaringChangeDTO();
        newMeta.setFfwfId(823600);
        newMeta.setExternalRequestId("TMU242771");
        newMeta.setRequestType(RequestType.MOVEMENT_WITHDRAW.name());
        newMeta.setReadableRequestType("Изъятие для межскладского перемещения");
        newMeta.setStatus("CREATED");
        newMeta.setItemsCount(0L);
        newMeta.setPalletsCount(1L);

        var expected = new PublishCalendaringChangePayload(
                823600L,
                oldMeta,
                newMeta,
                LocalDateTime.of(2021, 8, 3, 15, 0, 0),
                LogbrokerTopic.CALENDARING_META_INFO_CHANGE_EVENTS,
                "FFWF"
        );


        var actual = consumer.getPayloadTransformer().toObject(GIVEN_PAYLOAD_STRING);

        assertions.assertThat(actual).isEqualTo(expected);
    }

}
