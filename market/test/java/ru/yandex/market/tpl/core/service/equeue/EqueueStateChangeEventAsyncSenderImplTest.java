package ru.yandex.market.tpl.core.service.equeue;

import java.time.Clock;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.market.logistics.yard.client.dto.event.read.ClientEventDto;
import ru.yandex.market.logistics.yard.client.dto.event.read.YardClientEventPayload;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.service.equeue.model.EqueueStateChangeEventDto;
import ru.yandex.market.tpl.core.service.equeue.model.EqueueStateChangeEventType;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.util.ObjectMappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RequiredArgsConstructor
class EqueueStateChangeEventAsyncSenderImplTest extends TplAbstractTest {

    private final EqueueStateChangeEventAsyncSenderImpl subject;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final AsyncProducer mockedAsyncProducer;
    private final Clock clock;
    private ObjectMapper objectMapper = ObjectMappers.baseObjectMapper();

    @Test
    @SneakyThrows
    void addsDbQueueTask() {
        var scId = 123L;
        var clientId = "test-client";
        var eventType = EqueueStateChangeEventType.ENTERED;
        var event = new EqueueStateChangeEventDto(scId, clientId, eventType);

        subject.send(event);

        dbQueueTestUtil.assertQueueHasSize(QueueType.LOGBROKER_WRITER, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.LOGBROKER_WRITER);

        var bytesCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(mockedAsyncProducer, times(1)).write(bytesCaptor.capture());
        byte[] bytes = bytesCaptor.getValue();

        YardClientEventPayload result = objectMapper.readValue(bytes, YardClientEventPayload.class);

        assertThat(result.getServiceId()).isEqualTo(scId);
        assertThat(result.getEvents().size()).isEqualTo(1);

        ClientEventDto clientEventDto = result.getEvents().get(0);
        assertThat(clientEventDto.getClientId()).isEqualTo(clientId);
        assertThat(clientEventDto.getEventType()).isEqualTo(eventType.toYardEventType());
        assertThat(clientEventDto.getEventDate()).isEqualTo(LocalDateTime.now(clock));
    }

}
