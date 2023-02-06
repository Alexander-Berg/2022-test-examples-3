package ru.yandex.direct.jobs.adfox.messaging;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.jobs.adfox.messaging.ytutils.RawQueueMessage;
import ru.yandex.direct.utils.JsonUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@Disabled("Не работающая фича 'частные сделки'")
class RawMessageConsumerTest {

    private static final String DEAL_CREATE_TYPE = "deal:create";
    private static final ImmutableMap<String, String> EMPTY_PAYLOAD_MAP =
            ImmutableMap.of("@type", "type.googleapis.com/NDirectAdfoxMessaging.AdfoxDealCreatePayload");

    private AdfoxMessageHandler messageHandlerMock;
    private RawMessageConsumer messageConsumer;

    @BeforeEach
    void setUp() {
        messageHandlerMock = mock(AdfoxMessageHandler.class);
        messageConsumer = new RawMessageConsumer(ImmutableMap.of(DEAL_CREATE_TYPE, messageHandlerMock));
    }

    @Test
    void accept_success() throws Exception {
        String message = JsonUtils.toJson(ImmutableMap.of(
                "type", DEAL_CREATE_TYPE,
                "payload", EMPTY_PAYLOAD_MAP));
        messageConsumer.consume(new RawQueueMessage(0L, message));
        verify(messageHandlerMock).handleMessage(any());
    }

    @Test
    void accept_fail_onUnknownType() {
        String unknownType = "unknown_type";
        String message = JsonUtils.toJson(ImmutableMap.of(
                "type", unknownType,
                "payload", EMPTY_PAYLOAD_MAP));

        assertThatThrownBy(() -> messageConsumer.consume(new RawQueueMessage(0L, message)))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Can't find message handler for type " + unknownType);
    }

    @Test
    void accept_fail_onMalformedPayload() {
        String malformedPayloadText = "malformedPayload";
        String message = JsonUtils.toJson(ImmutableMap.of(
                "type", DEAL_CREATE_TYPE,
                "payload", malformedPayloadText));

        assertThatThrownBy(() -> messageConsumer.consume(new RawQueueMessage(0L, message)))
                .isInstanceOf(InvalidProtocolBufferException.class)
                .hasMessageContaining("Expect message object but got: \"" + malformedPayloadText + "\"");
    }

    @Test
    void accept_fail_onHandlerException() throws Exception {
        String simpleExceptionMessage = "Simple exception message";
        doThrow(new Exception(simpleExceptionMessage)).when(messageHandlerMock).handleMessage(any());

        String message = JsonUtils.toJson(ImmutableMap.of(
                "type", DEAL_CREATE_TYPE,
                "payload", EMPTY_PAYLOAD_MAP));

        assertThatThrownBy(() -> messageConsumer.consume(new RawQueueMessage(0L, message)))
                .isInstanceOf(Exception.class)
                .hasMessageContaining(simpleExceptionMessage);
    }

}
