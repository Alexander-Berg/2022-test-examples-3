package ru.yandex.market.crm.triggers.services.logbroker.consumers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.market.crm.core.jackson.CustomObjectMapperFactory;
import ru.yandex.market.crm.core.services.logbroker.LogTypesResolver;
import ru.yandex.market.crm.lb.LBInstallation;
import ru.yandex.market.crm.lb.LogIdentifier;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;
import ru.yandex.market.crm.triggers.services.bpm.correlation.MessageSender;
import ru.yandex.market.crm.triggers.services.external.ocrm.domain.chat.ChatMessageRequest;
import ru.yandex.market.crm.triggers.services.external.ocrm.domain.chat.CustomFrom;
import ru.yandex.market.crm.triggers.services.external.ocrm.domain.chat.PlainMessage;
import ru.yandex.market.crm.triggers.services.external.ocrm.domain.chat.PlainMessageText;
import ru.yandex.market.crm.triggers.services.external.ocrm.domain.chat.ServerMessageInfo;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames.ACTION_URL;
import static ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames.CHAT_MESSAGE;
import static ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames.CONVERSATION_ID;
import static ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames.ORDER_ID;
import static ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames.SEEN_MESSAGE_ID;
import static ru.yandex.market.crm.core.services.trigger.MessageTypes.CHAT_MESSAGE_FROM_PARTNER;
import static ru.yandex.market.crm.core.services.trigger.MessageTypes.USER_SEEN_CHAT_MESSAGE_FROM_PARTNER;
import static ru.yandex.market.crm.triggers.services.logbroker.consumers.MessageMatchers.messagesMatcher;

@RunWith(MockitoJUnitRunner.class)
public class ChatEventConsumerTest {

    private static final String CHAT_URL_TEMPLATE =
            "https://renderer-chat-dev.hamster.yandex.ru/chat?config=development&flags=disableNavigation=1#/%s";

    @Mock
    private MessageSender messageSender;

    @Mock
    private LogTypesResolver logTypes;

    private ChatEventConsumer consumer;

    @Before
    public void setUp() {
        when(logTypes.getLogIdentifier("chat.events"))
                .thenReturn(new LogIdentifier("null/null", LBInstallation.LOGBROKER));
        ObjectMapper objectMapper = CustomObjectMapperFactory.INSTANCE.getJsonObjectMapper();
        consumer = new ChatEventConsumer(logTypes, objectMapper, messageSender, "");
    }

    @Test
    public void testUserSeenMessageCreating() {
        Long puid = 12345L;
        String conversationId = "conversation-001";
        String seenMessageId = "seenMessageId-101";
        var jsonMessage = jsonUserSeen(puid, conversationId, seenMessageId);

        var bpmMessage = new UidBpmMessage(
                USER_SEEN_CHAT_MESSAGE_FROM_PARTNER,
                Uid.asPuid(puid),
                Map.of(CONVERSATION_ID, conversationId),
                Map.of(
                        CONVERSATION_ID, conversationId,
                        SEEN_MESSAGE_ID, seenMessageId
                )
        );

        assertMessages(jsonMessage, bpmMessage);
    }

    @Test
    public void testMessageFromPartnerMessageCreating() {
        Long puid = 12345L;
        String conversationId = "conversation-001";
        Long order = 999999L;
        String payloadId = "payload-id-01";
        String messageText = "message text";
        long timestamp = 1650032327L;
        var jsonMessage = jsonMessageFromPartner(puid, conversationId, order, payloadId, messageText, timestamp);

        var chatEvent = new ChatEventConsumer.MessageFromPartnerChatEvent(
                CHAT_MESSAGE_FROM_PARTNER,
                createChatMessageRequest(),
                new ChatEventConsumer.MessageFromPartnerPayload(
                        puid, String.format(CHAT_URL_TEMPLATE, conversationId), conversationId, order
                )
        );
        var bpmMessage = new UidBpmMessage(
                CHAT_MESSAGE_FROM_PARTNER,
                Uid.asPuid(puid),
                Map.of(CONVERSATION_ID, conversationId),
                Map.of(
                        CONVERSATION_ID, conversationId,
                        CHAT_MESSAGE, new ChatEventConsumer.ChatPlainMessage(
                                payloadId,
                                messageText,
                                List.of(),
                                LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault())
                        ),
                        ACTION_URL, String.format(CHAT_URL_TEMPLATE, conversationId),
                        ORDER_ID, order
                )
        );

        assertMessages(jsonMessage, bpmMessage);
    }

    @Test
    public void testMessageNotCreatesWithUnknownType() {
        var jsonMessage = jsonMessage("unknown_type", "\"payload\"", "{}");
        assertMessages(jsonMessage);
    }

    private String jsonUserSeen(Long puid, String conversationId, String seenMessageId) {
        String chatUrl = String.format(CHAT_URL_TEMPLATE, conversationId);
        String payload = String.format(
                "{\"puid\":%d,\"chatUrl\":\"%s\",\"conversationId\":\"%s\"}",
                puid, chatUrl, conversationId
        );
        return jsonMessage(USER_SEEN_CHAT_MESSAGE_FROM_PARTNER, payload, userSeenMessage(seenMessageId));
    }

    private String jsonMessageFromPartner(Long puid,
                                          String conversationId,
                                          Long order,
                                          String payloadId,
                                          String messageText,
                                          long timestamp) {
        String chatUrl = String.format(CHAT_URL_TEMPLATE, conversationId);
        String payload = String.format(
                "{\"puid\":%d,\"chatUrl\":\"%s\",\"conversationId\":\"%s\",\"order\":%d}",
                puid, chatUrl, conversationId, order
        );
        return jsonMessage(CHAT_MESSAGE_FROM_PARTNER, payload, plainMessage(payloadId, messageText, timestamp));
    }

    private String jsonMessage(String type, String payload, String chatMessage) {
        return String.format(
                "{\"type\":\"%s\",\"payload\":%s,\"chatMessage\":%s}",
                type, payload, chatMessage
        );
    }

    private String plainMessage(String payloadId, String messageText, long timestamp) {
        return String.format("{\"Message\":{\"Plain\":{\"Text\":{\"MessageText\":\"%s\"},\"ChatId\":\"chat-id\"," +
                        "\"IsRead\":false,\"Context\":null,\"IsSilent\":false,\"PayloadId\":\"%s\"," +
                        "\"ForwardedMessageRefs\":[]}},\"CustomFrom\":{\"AvatarId\":\"\",\"DisplayName\":\"\"}," +
                        "\"ServerMessageInfo\":{\"Timestamp\":%d}}",
                messageText, payloadId, timestamp * 1_000_000
        );
    }

    private String userSeenMessage(String seenMessageId) {
        return String.format("{\"Message\": {\"SeenMarker\": {\"ChatId\": \"chat-id\", \"IsRead\": false, " +
                        "\"IsSilent\": false, \"SeenMessageId\": \"%s\"}}, \"CustomFrom\": {\"AvatarId\": \"\"," +
                        " \"DisplayName\": \"\"}, \"ServerMessageInfo\": {\"Timestamp\": 1646481483644026}}",
                seenMessageId
        );
    }

    private ChatMessageRequest createChatMessageRequest() {
        var message = PlainMessage.forText(
                "conversationId",
                GUID.create().toString(),
                new PlainMessageText("text")
        );
        return new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                message,
                new CustomFrom("", "")
        );
    }

    private void assertMessages(String line, UidBpmMessage... expected) {
        List<ChatEventConsumer.ChatEvent<?>> rows = consumer.transform(line.getBytes());
        assertNotNull(rows);
        consumer.accept(rows);
        verify(messageSender).send(argThat(messagesMatcher(expected)));
    }
}
