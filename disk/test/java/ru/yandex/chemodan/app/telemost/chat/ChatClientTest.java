package ru.yandex.chemodan.app.telemost.chat;

import java.util.UUID;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.TelemostBaseContextTest;
import ru.yandex.chemodan.app.telemost.chat.model.Chat;
import ru.yandex.chemodan.app.telemost.chat.model.ChatHistory;
import ru.yandex.chemodan.app.telemost.chat.model.ChatRole;
import ru.yandex.chemodan.app.telemost.services.model.ChatType;
import ru.yandex.misc.test.Assert;

public class ChatClientTest extends TelemostBaseContextTest {

    @Autowired
    private ChatClient chatClient;

    private final String uid1 = "4021169636";
    private final String uid2 = "4041475966";

    @Test
    public void testRequestUser() {
        MapF<String, UUID> users = chatClient.getUsers(Cf.list(uid1));
        Assert.sizeIs(1, users);
        Assert.isTrue(users.getO(uid1).isPresent());
    }

    @Test
    public void testGetUserUid() {
        Option<UUID> userO = chatClient.getUser(uid1);

        Assert.isTrue(userO.isPresent());

        Option<String> uidO = chatClient.getUserUid(userO.get());

        Assert.isTrue(uidO.isPresent());
        Assert.equals(uidO.get(), uid1);
    }

    @Test
    public void testCreateChat() {
        // Create chat with one user
        UUID chatId = UUID.randomUUID();
        MapF<String, UUID> users = chatClient.getUsers(Cf.list(uid1));

        Option<Chat> chatO = chatClient.createChat(
                chatId,
                ChatType.CONFERENCE,
                "Test chat",
                "Test chat",
                Option.empty(),
                users.values().toList(),
                users.values().toList()
        );

        Assert.isTrue(chatO.isPresent());

        String[] chatIdElements = chatO.get().getChatPath().split("/");
        Assert.equals(chatIdElements[chatIdElements.length - 1], chatId.toString());
        Assert.equals(chatO.get().getMembers().size(), 1);
        Assert.equals(chatO.get().getMembers().get(0), users.getTs(uid1));
    }

    @Test
    public void testGetChat() {
        UUID chatId = UUID.randomUUID();
        MapF<String, UUID> users = chatClient.getUsers(Cf.list(uid1));

        Option<Chat> chatO = chatClient.getChats(chatId, ChatType.CONFERENCE);

        Assert.isTrue(!chatO.isPresent());

        chatO = chatClient.createChat(
                chatId,
                ChatType.CONFERENCE,
                "Test chat",
                "Test chat",
                Option.empty(),
                users.values().toList(),
                users.values().toList()
        );

        Assert.isTrue(chatO.isPresent());

        String[] chatIdElements = chatO.get().getChatPath().split("/");
        Assert.equals(chatIdElements[chatIdElements.length - 1], chatId.toString());
        Assert.equals(chatO.get().getMembers().size(), 1);
        Assert.equals(chatO.get().getMembers().get(0), users.getTs(uid1));
    }

    @Test
    public void testUpdateMembers() {
        // Create chat with one user
        UUID chatId = UUID.randomUUID();
        MapF<String, UUID> users = chatClient.getUsers(Cf.list(uid1));

        Option<Chat> chatO = chatClient.createChat(
                chatId,
                ChatType.CONFERENCE,
                "Test chat",
                "Test chat",
                Option.empty(),
                users.values().toList(),
                users.values().toList()
        );

        Assert.isTrue(chatO.isPresent());

        // Add second user to chat
        MapF<String, UUID> addingUsers = chatClient.getUsers(Cf.list(uid2));

        chatO = chatClient.updateMembers(chatId, ChatType.CONFERENCE, addingUsers.values().toList(), Cf.list(),
                ChatRole.MEMBER);

        Assert.isTrue(chatO.isPresent());
        Assert.equals(chatO.get().getMembers().size(), 2);
    }

    @Test
    public void testPush() {
        // Create chat with one user
        UUID chatId = UUID.randomUUID();
        MapF<String, UUID> users = chatClient.getUsers(Cf.list(uid1));

        Option<Chat> chatO = chatClient.createChat(
                chatId,
                ChatType.CONFERENCE,
                "Test chat",
                "Test chat",
                Option.empty(),
                users.values().toList(),
                users.values().toList()
        );

        Assert.isTrue(chatO.isPresent());

        // Push message
        Assert.isTrue(chatClient.push(chatId, ChatType.CONFERENCE, users.getO(uid1).get(), "Test message at user"));

        // Get chat history
        Option<ChatHistory> chatHistoryO = chatClient.history(chatId, ChatType.CONFERENCE, users.getO(uid1).get(),
                Option.empty());

        Assert.isTrue(chatHistoryO.isPresent());
    }

    @Test
    public void testHistory() {
        // Create chat with one user
        UUID chatId = UUID.randomUUID();
        MapF<String, UUID> users = chatClient.getUsers(Cf.list(uid1));

        Option<Chat> chatO = chatClient.createChat(
                chatId,
                ChatType.CONFERENCE,
                "Test chat",
                "Test chat",
                Option.empty(),
                users.values().toList(),
                users.values().toList()
        );

        Assert.isTrue(chatO.isPresent());

        // Get chat history
        Option<ChatHistory> chatHistoryO = chatClient.history(chatId, ChatType.CONFERENCE, users.getO(uid1).get(),
                Option.empty());

        Assert.isTrue(!chatHistoryO.isPresent()); // without system messages
    }

}
