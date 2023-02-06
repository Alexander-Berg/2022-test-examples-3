package ru.yandex.market.mbi.bot.tg;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pengrad.telegrambot.model.Update;

public class TelegramTestUtils {

    private static final Gson GSON = new Gson();
    private static final AtomicInteger updateIdSupplier = new AtomicInteger(9000);

    public static String create500Response() {
        JsonObject response = new JsonObject();
        response.addProperty("ok", false);
        response.addProperty("description", "Server Error");
        response.addProperty("error_code", 500);
        return response.toString();
    }

    public static String create429Response() {
        JsonObject response = new JsonObject();
        response.addProperty("ok", false);
        response.addProperty("description", "Too many requests");
        response.addProperty("error_code", 429);
        return response.toString();
    }

    public static String create404Response() {
        JsonObject response = new JsonObject();
        response.addProperty("ok", false);
        response.addProperty("description", "Not found");
        response.addProperty("error_code", 404);
        return response.toString();
    }

    public static String create403Response() {
        JsonObject response = new JsonObject();
        response.addProperty("ok", false);
        response.addProperty("description", "Forbidden");
        response.addProperty("error_code", 403);
        return response.toString();
    }

    public static String create400Response() {
        JsonObject response = new JsonObject();
        response.addProperty("ok", false);
        response.addProperty("description", "Bad request");
        response.addProperty("error_code", 400);
        return response.toString();
    }

    public static String createOkResponse() {
        JsonObject response = new JsonObject();
        response.addProperty("ok", true);
        response.addProperty("description", "ok");
        return response.toString();
    }

    public static JsonObject createUser() {
        return createUser(100500);
    }

    public static JsonObject createUser(long tgId) {
        JsonObject user = new JsonObject();
        user.addProperty("id", tgId);
        user.addProperty("first_name", "First");
        user.addProperty("last_name", "Last");
        user.addProperty("username", "username");
        return user;
    }

    public static JsonObject createChat() {
        return createChat(100500);
    }

    public static JsonObject createChat(long chatId) {
        JsonObject chat = new JsonObject();
        chat.addProperty("id", chatId);
        chat.addProperty("first_name", "First");
        chat.addProperty("last_name", "Last");
        chat.addProperty("username", "username");
        return chat;
    }

    public static JsonObject createCallbackQuery(JsonObject from, String callbackData) {
        JsonObject callbackQuery = new JsonObject();
        callbackQuery.addProperty("id", "12345");
        callbackQuery.addProperty("chat_instance", "54321");
        callbackQuery.add("from", from);
        callbackQuery.addProperty("data", callbackData);
        return callbackQuery;
    }

    public static JsonObject createCallbackQuery(JsonObject from, JsonObject chat, JsonObject message, String callbackData) {
        JsonObject callbackQuery = new JsonObject();
        callbackQuery.addProperty("id", "12345");
        callbackQuery.addProperty("chat_instance", "54321");
        callbackQuery.add("from", from);
        callbackQuery.addProperty("data", callbackData);
        callbackQuery.add("chat", chat);
        callbackQuery.add("message", message);
        return callbackQuery;
    }

    public static JsonObject createMessage(JsonObject from, JsonObject chat, @Nullable String text) {
        JsonObject message = new JsonObject();
        message.addProperty("date", 1441645532);
        message.add("chat", chat);
        message.addProperty("message_id", 1365);
        message.add("from", from);
        message.addProperty("text", text);
        return message;
    }

    public static Update createUpdate(JsonObject callbackQuery) {
        JsonObject update = new JsonObject();
        update.addProperty("update_id", updateIdSupplier.getAndIncrement());
        update.add("callback_query", callbackQuery);
        return GSON.fromJson(update, Update.class);
    }

    public static Update createUpdate(JsonObject message, @Nullable JsonObject callbackQuery) {
        JsonObject update = new JsonObject();
        update.addProperty("update_id", updateIdSupplier.getAndIncrement());
        update.add("callback_query", callbackQuery);
        update.add("message", message);
        return GSON.fromJson(update, Update.class);
    }
}
