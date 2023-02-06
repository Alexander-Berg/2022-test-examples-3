package ru.yandex.market.mbi.bot;

import com.google.gson.JsonObject;

public final class IntegrationTestUtils {

    private static final int TG_ID = 139375402;

    private IntegrationTestUtils() {
        throw new UnsupportedOperationException();
    }

    public static JsonObject createUser() {
        return createUser(TG_ID, "First", "Last", "username");
    }

    public static JsonObject createUser(int tgId, String firstName, String lastName, String userName) {
        JsonObject user = new JsonObject();
        user.addProperty("id", tgId);
        user.addProperty("first_name", firstName);
        user.addProperty("last_name", lastName);
        user.addProperty("username", userName);
        return user;
    }

    public static JsonObject createChat() {
        JsonObject chat = new JsonObject();
        chat.addProperty("id", TG_ID);
        chat.addProperty("first_name", "First");
        chat.addProperty("last_name", "Last");
        chat.addProperty("username", "username");
        return chat;
    }

    public static JsonObject createMessage(JsonObject from, JsonObject chat, String text) {
        return createMessage(1, from, chat, text);
    }

    public static JsonObject createMessage(long messageId, JsonObject from, JsonObject chat, String text) {
        JsonObject message = new JsonObject();
        message.add("chat", chat);
        message.addProperty("message_id", messageId);
        message.add("from", from);
        message.addProperty("text", text);
        return message;
    }
}
