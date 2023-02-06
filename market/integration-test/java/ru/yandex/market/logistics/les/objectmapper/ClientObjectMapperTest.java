package ru.yandex.market.logistics.les.objectmapper;

import javax.jms.JMSException;

import com.amazon.sqs.javamessaging.message.SQSTextMessage;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.support.converter.MessageConverter;

import ru.yandex.market.logistics.les.AbstractContextualTest;
import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.objectmapper.testmodel.EventMessage;
import ru.yandex.market.logistics.les.objectmapper.testmodel.SimplePayload;
import ru.yandex.market.logistics.les.objectmapper.testmodel.SimplePayloadType;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.logistics.les.objectmapper.UtilsKt.sessionMock;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

public class ClientObjectMapperTest extends AbstractContextualTest {

    @Autowired
    private MessageConverter messageConverter;

    // событие, которое отправляют еще не обновившиеся клиенты леса
    private final Event oldEvent = new Event(
        "some source",
        "some event id",
        1633650600L,
        "some type",
        new SimplePayload(SimplePayloadType.SIMPLE),
        "some description",
        null,
        null,
        null
    );

    // событие, которое отправляют обновившиеся клиенты леса
    private final Event newEvent = new Event(
        "some source",
        "some event id",
        1633650600L,
        "some type",
        new SimplePayload(SimplePayloadType.SIMPLE),
        "some description"
    );

    @Test
    public void testSerializeOldEvent() throws JMSException, JSONException {
        SQSTextMessage message = (SQSTextMessage) messageConverter.toMessage(
            oldEvent,
            sessionMock()
        );
        String expectedJson = extractFileContent("objectmapper/java/old_event.json");
        JSONAssert.assertEquals(expectedJson, message.getText(), true);
    }

    @Test
    public void testDeserializeOldEvent() throws JMSException {
        String extractedContent = extractFileContent("objectmapper/java/old_event.json");
        Event actualEvent = (Event) messageConverter.fromMessage(new EventMessage(extractedContent));
        assertThat(actualEvent).isEqualTo(oldEvent);
    }

    @Test
    public void testDeserializeOldEventWithoutNewNullableFields() throws JMSException {
        String extractedContent = extractFileContent("objectmapper/java/old_event_without_new_nullable_fields.json");
        Event actualEvent = (Event) messageConverter.fromMessage(new EventMessage(extractedContent));
        assertThat(actualEvent).isEqualTo(oldEvent);
    }

    @Test
    public void testSerializeNewEvent() throws JMSException, JSONException {
        SQSTextMessage message = (SQSTextMessage) messageConverter.toMessage(
            newEvent,
            sessionMock()
        );
        String expectedJson = extractFileContent("objectmapper/java/new_event.json");
        JSONAssert.assertEquals(expectedJson, message.getText(), true);
    }

    @Test
    public void testDeserializeNewEvent() throws JMSException {
        String extractedContent = extractFileContent("objectmapper/java/new_event.json");
        Event actualEvent = (Event) messageConverter.fromMessage(new EventMessage(extractedContent));
        assertThat(actualEvent).isEqualTo(newEvent);
    }
}
