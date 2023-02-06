package ru.yandex.chemodan.app.telemost.chat;

import java.util.Date;
import java.util.UUID;

import lombok.Data;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.chemodan.app.telemost.chat.model.Chat;
import ru.yandex.chemodan.app.telemost.chat.model.ChatHistory;
import ru.yandex.chemodan.app.telemost.chat.model.ChatHistoryItem;
import ru.yandex.chemodan.app.telemost.chat.model.ChatRole;
import ru.yandex.chemodan.app.telemost.services.model.ChatType;

public class ChatClientStub implements ChatClient {
    MapF<UUID, ChatStub> chats = Cf.hashMap();
    MapF<UUID, ChatHistory> histories = Cf.hashMap();
    MapF<String, UUID> users = Cf.hashMap();

    public void reset() {
        chats.clear();
        histories.clear();
        users.clear();
    }

    @Override
    public Option<Chat> createChat(UUID chatId, ChatType chatType, String title, String description, Option<String> avatarId,
                                   ListF<UUID> admins, ListF<UUID> members) {

        ChatStub chat = new ChatStub("0/" + (chatType == ChatType.CONFERENCE ? "22/" : "32/") + chatId.toString(),
                chatType, Cf.toHashSet(members), Cf.toHashSet(admins));
        chats.put(chatId, chat);
        return Option.of(chat.toChat());
    }

    @Override
    public Option<Chat> getChats(UUID chatId, ChatType chatType) {
        return chats.getO(chatId).map(ChatStub::toChat);
    }

    @Override
    public Option<Chat> updateMembers(UUID chatId, ChatType chatType, ListF<UUID> addMembers, ListF<UUID> removeMembers,
                                      ChatRole role) {
        Option<ChatStub> chatO = chats.getO(chatId);
        if(chatO.isEmpty())
            return Option.empty();
        ChatStub chat = chatO.get();
        SetF<UUID> target;
        switch (role) {
            case ADMIN:
                target = chat.getAdmins();
                break;
            case MEMBER:
                target = chat.getMembers();
                break;
            case SUBSCRIBER:
                target = chat.getSubscribers();
                break;
            default: throw new RuntimeException();
        }
        target.addAll(addMembers);
        target.removeAllTs(removeMembers);
        return Option.of(chat.toChat());
    }

    @Override
    public Option<UUID> getUser(String uid) {
        if(uid.startsWith("666")){
            throw new IllegalArgumentException(
                    "to check if errors from chat client are processed correctly for uid " + uid
            );
        }
        return Option.of(users.computeIfAbsent(uid, s -> UUID.randomUUID()));
    }

    @Override
    public MapF<String, UUID> getUsers(ListF<String> uids) {

        MapF<String, UUID> result = Cf.hashMap();
        for (String uid : uids) {
            result.put(uid, getUser(uid).get());
        }
        return result;
    }

    @Override
    public Option<String> getUserUid(UUID user) {
        return users.entries().find(tuple -> tuple.get2().equals(user)).getO(0).map(Tuple2::get1);
    }

    @Override
    public boolean push(UUID chatId, ChatType chatType, UUID user, String message) {
        long now = new Date().getTime() / 1000;
        ChatHistory chatHistory = histories.computeIfAbsent(chatId, id -> new ChatHistory(Cf.arrayList(), now, now));
        chatHistory.getItems().add(new ChatHistoryItem(now, message, user, message));
        return true;
    }

    @Override
    public Option<ChatHistory> history(UUID chatId, ChatType chatType, UUID user, Option<Long> offset) {
        return histories.getO(chatId);
    }

    @Override
    public boolean chatIsEmpty(UUID chatId, ChatType chatType, UUID user) {
        return false;
    }

    @Override
    public void removeChatMembers(UUID chatId, ChatType chatType) {
    }

    @Data
    private static class ChatStub {
        final SetF<UUID> subscribers = Cf.hashSet();
        final String path;
        final ChatType chatType;
        final SetF<UUID> members;
        final SetF<UUID> admins;

        public Chat toChat() {
            return new Chat(path, chatType, members.plus(admins).toList(), subscribers.toList());
        }
    }
}
