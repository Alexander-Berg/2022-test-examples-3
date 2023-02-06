package ru.yandex.market.arbiter.test.service;

import java.util.List;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.businesschat.api.client.BusinesschatClientApi;
import ru.yandex.businesschat.api.client.dto.BusinessChatMessageDto;
import ru.yandex.businesschat.api.client.dto.MessageType;
import ru.yandex.businesschat.api.client.dto.OperatorMessageDto;
import ru.yandex.businesschat.api.client.dto.OperatorSenderDto;
import ru.yandex.businesschat.api.client.dto.UserRecipientDto;
import ru.yandex.market.arbiter.api.server.dto.BusinesschatParamsDto;
import ru.yandex.market.arbiter.api.server.dto.ConversationSide;
import ru.yandex.market.arbiter.api.server.dto.CreateConversationRequestDto;
import ru.yandex.market.arbiter.api.server.dto.MessageDto;
import ru.yandex.market.arbiter.api.server.dto.NotificationChannelDto;
import ru.yandex.market.arbiter.api.server.dto.NotificationChannelType;
import ru.yandex.market.arbiter.service.Notifier;
import ru.yandex.market.arbiter.test.BaseUnitTest;
import ru.yandex.market.arbiter.test.util.RandomDataGenerator;
import ru.yandex.market.arbiter.workflow.Workflow;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author moskovkin@yandex-team.ru
 * @since 09.06.2020
 */
public class BusinesschatMessagesNotifierTest extends BaseUnitTest {
    private static final EnhancedRandom RANDOM =
            RandomDataGenerator.dataRandom(BusinesschatMessagesNotifierTest.class).build();

    @Autowired
    private Notifier notifier;

    @Autowired
    private BusinesschatClientApi mockBusinesschatClientApi;

    @Autowired
    private Workflow workflow;

    @BeforeEach
    public void setup() {
        super.setup();
        Mockito.clearInvocations(mockBusinesschatClientApi);
    }

    @Test
    public void testNotifyMessageConsumersSendMessageOnlyOnce() {
        CreateConversationRequestDto createConversationRequest = testCreateConversationRequest();

        Long conversationId = workflow.addConversation(createConversationRequest);
        Long arbiterUid = RANDOM.nextLong();
        MessageDto message = new MessageDto()
                .sender(ConversationSide.ARBITER)
                .recipient(ConversationSide.USER)
                .text("Text for user");

        workflow.arbiterInProgress(arbiterUid, conversationId);
        workflow.arbiterMessageAdd(arbiterUid, conversationId, message);
        notifier.notifyMessageConsumers();
        notifier.notifyMessageConsumers();

        verify(mockBusinesschatClientApi, times(1))
                .pushProviderNameChatIdPostWithHttpInfo(anyString(), anyString(), any());
    }

    @Test
    public void testNotifyMessageConsumersSendMessageToApi() {
        CreateConversationRequestDto createConversationRequest = testCreateConversationRequest();

        Long conversationId = workflow.addConversation(createConversationRequest);
        Long arbiterUid = RANDOM.nextLong();
        MessageDto message = new MessageDto()
                .sender(ConversationSide.ARBITER)
                .recipient(ConversationSide.MERCHANT)
                .text("Text for merchant");

        workflow.arbiterInProgress(arbiterUid, conversationId);
        Long messageId = workflow.arbiterMessageAdd(arbiterUid, conversationId, message);
        notifier.notifyMessageConsumers();

        verify(mockBusinesschatClientApi)
                .pushProviderNameChatIdPostWithHttpInfo("arbiter", "MERCHANT_CHAT_ID",
                        new OperatorMessageDto()
                            .sender(new OperatorSenderDto()
                                    .name("Арбитр")
                            )
                            .message(new BusinessChatMessageDto()
                                    .id(messageId.toString())
                                    .text(message.getText())
                                    .type(MessageType.TEXT)
                            )
                            .recipient(new UserRecipientDto()
                                    .id("MERCHANT_RECIPIENT_ID")
                            )
                );
        verify(mockBusinesschatClientApi, times(1))
                .pushProviderNameChatIdPostWithHttpInfo(anyString(), anyString(), any());
    }

    private CreateConversationRequestDto testCreateConversationRequest() {
        return RANDOM.nextObject(
                CreateConversationRequestDto.class
        ).notificationChannels(List.of(
                new NotificationChannelDto()
                        .conversationSide(ConversationSide.USER)
                        .type(NotificationChannelType.BUSINESSCHAT)
                        .businesschatParams(new BusinesschatParamsDto()
                                .chatId("USER_CHAT_ID")
                                .recipientId("USER_RECIPIENT_ID")
                        ),
                new NotificationChannelDto()
                        .conversationSide(ConversationSide.MERCHANT)
                        .type(NotificationChannelType.BUSINESSCHAT)
                        .businesschatParams(new BusinesschatParamsDto()
                                .chatId("MERCHANT_CHAT_ID")
                                .recipientId("MERCHANT_RECIPIENT_ID")
                        )
                )
        );
    }
}
