package ru.yandex.market.logistics.pechkin.app.telegram;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.telegram.telegrambots.api.objects.Chat;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import ru.yandex.market.logistics.pechkin.app.exception.NotHandledMessageException;
import ru.yandex.market.logistics.pechkin.app.telegram.handlers.ChannelMessageHandler;
import ru.yandex.market.logistics.pechkin.app.telegram.handlers.ChatMessageHandler;
import ru.yandex.market.logistics.pechkin.app.telegram.handlers.GroupMessageHandler;

class PechkinBotTest {

    private PechkinBot bot;

    private ChannelMessageHandler channelMessageHandlerMock;
    private ChatMessageHandler chatMessageHandlerMock;
    private GroupMessageHandler groupMessageHandlerMock;


    @BeforeEach
    void beforeEach() {
        channelMessageHandlerMock = Mockito.mock(ChannelMessageHandler.class);
        groupMessageHandlerMock = Mockito.mock(GroupMessageHandler.class);
        chatMessageHandlerMock = Mockito.mock(ChatMessageHandler.class);


        bot = new PechkinBot("userName", "token", Arrays.asList(
            channelMessageHandlerMock,
            chatMessageHandlerMock,
            groupMessageHandlerMock
        ));
    }

    @Test
    void testUpdateForChannel() throws IllegalAccessException {
        Mockito.when(channelMessageHandlerMock.handle(Mockito.any())).thenReturn(true);
        Mockito.when(channelMessageHandlerMock.canHandle(Mockito.any())).thenCallRealMethod();

        Update updateForChannel = createUpdateForChannel();
        bot.onUpdateReceived(updateForChannel);
        Mockito.verify(channelMessageHandlerMock).canHandle(Mockito.any());
        Mockito.verify(channelMessageHandlerMock).handle(Mockito.any());
    }

    @Test
    void testUpdateForGroup() throws IllegalAccessException {
        Mockito.when(groupMessageHandlerMock.handle(Mockito.any())).thenReturn(true);
        Mockito.when(groupMessageHandlerMock.canHandle(Mockito.any())).thenCallRealMethod();


        Update updateForChannel = createUpdateForGroup();
        bot.onUpdateReceived(updateForChannel);
        Mockito.verify(groupMessageHandlerMock).canHandle(Mockito.any());
        Mockito.verify(groupMessageHandlerMock).handle(Mockito.any());
    }

    @Test
    void testUpdateForChat() throws IllegalAccessException {
        Mockito.when(chatMessageHandlerMock.handle(Mockito.any())).thenReturn(true);
        Mockito.when(chatMessageHandlerMock.canHandle(Mockito.any())).thenCallRealMethod();

        Update updateForChannel = createUpdateForChat();
        bot.onUpdateReceived(updateForChannel);
        Mockito.verify(chatMessageHandlerMock).canHandle(Mockito.any());
        Mockito.verify(chatMessageHandlerMock).handle(Mockito.any());
    }

    @Test
    void testUpdateForNotHandledMessage() {
        Update badUpdate = new Update();
        Assertions.assertThrows(NotHandledMessageException.class, () -> bot.onUpdateReceived(badUpdate));
    }


    private Update createUpdateForChannel() throws IllegalAccessException {
        Chat chat = createChat("title");
        Message message = createMessage(chat);
        return createUpdate(message, "channelPost");
    }

    private Update createUpdateForGroup() throws IllegalAccessException {
        Chat chat = createChat("title");
        Message message = createMessage(chat);
        return createUpdate(message, "message");
    }

    private Update createUpdateForChat() throws IllegalAccessException {
        Chat chat = createChat("userName");
        Message message = createMessage(chat);
        return createUpdate(message, "message");
    }

    private Update createUpdate(Message message, String filedName) throws IllegalAccessException {
        Update update = new Update();
        Map<String, Field> updateFieldMap = getStringFieldMap(update);
        updateFieldMap.get(filedName).set(update, message);
        return update;
    }

    private Chat createChat(String fieldName) throws IllegalAccessException {
        Chat chat = new Chat();
        Map<String, Field> chatFieldMap = getStringFieldMap(chat);
        chatFieldMap.get("id").set(chat, 1L);
        chatFieldMap.get(fieldName).set(chat, fieldName);
        return chat;
    }

    private Message createMessage(Chat chat) throws IllegalAccessException {
        Message message = new Message();
        Map<String, Field> messageFieldMap = getStringFieldMap(message);
        messageFieldMap.get("chat").set(message, chat);
        messageFieldMap.get("date").set(message, 10000);
        return message;
    }

    private Map<String, Field> getStringFieldMap(Object object) {
        return Arrays.stream(object.getClass().getDeclaredFields())
            .peek(field -> field.setAccessible(true))
            .collect(Collectors.toMap(Field::getName, field -> field));
    }
}
