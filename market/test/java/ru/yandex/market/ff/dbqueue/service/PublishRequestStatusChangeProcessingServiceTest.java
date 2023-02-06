package ru.yandex.market.ff.dbqueue.service;

import java.time.LocalDateTime;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.model.dbqueue.PublishRequestStatusChangePayload;
import ru.yandex.market.ff.service.enums.LogbrokerTopic;
import ru.yandex.market.logbroker.producer.SimpleAsyncProducer;

class PublishRequestStatusChangeProcessingServiceTest extends IntegrationTest {

    @Autowired
    PublishRequestStatusChangeProcessingService cut;

    @Autowired
    SimpleAsyncProducer mockedSimpleAsyncProducer;

    @DatabaseSetup("classpath:empty.xml")
    public void testLogBrokerSerialization() {
        LocalDateTime time = LocalDateTime.of(2020, 9, 9, 9, 9, 9);
        var payload = new PublishRequestStatusChangePayload(
            1L,
            0,
            false,
            false,
            time,
            time,
            RequestStatus.CREATED,
            RequestStatus.VALIDATED,
            LogbrokerTopic.REQUEST_STATUS_EVENTS
        );
        var expectedPayload = "{" +
                "\"requestStatusChanges\":[{\"requestId\":1," +
                "\"changedAt\":\"2020-09-09T09:09:09\"," +
                "\"receivedChangedAt\":\"2020-09-09T09:09:09\"," +
                "\"oldStatus\":0,\"newStatus\":1}]" +
                "}";

        cut.processPayload(payload);
        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);

        Mockito.verify(mockedSimpleAsyncProducer).write(captor.capture());
        var bytes = captor.getValue();
        String deserializedMessage = new String(bytes);
        assertions.assertThat(expectedPayload).isEqualTo(deserializedMessage);
    }

}
