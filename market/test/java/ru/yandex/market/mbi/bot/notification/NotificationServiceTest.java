package ru.yandex.market.mbi.bot.notification;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.google.common.net.UrlEscapers;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbi.bot.FunctionalTest;
import ru.yandex.market.mbi.bot.notification.service.NotificationService;
import ru.yandex.market.notification.telegram.bot.model.address.TelegramIdAddress;
import ru.yandex.market.notification.telegram.bot.model.dto.SendMessageResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.mbi.bot.tg.TelegramTestUtils.create400Response;
import static ru.yandex.market.mbi.bot.tg.TelegramTestUtils.create429Response;
import static ru.yandex.market.mbi.bot.tg.TelegramTestUtils.create500Response;
import static ru.yandex.market.mbi.bot.tg.TelegramTestUtils.createOkResponse;

public class NotificationServiceTest extends FunctionalTest {

    private static final String BOT_1_API_URL = "/bottoken/sendMessage";
    private static final String BOT_2_API_URL = "/botbot_2_token/sendMessage";

    private static final long validChatId = 100500L;
    private static final long invalidChatId = 9000L;

    private static final String BOT_1 = "bot_1";
    private static final TelegramIdAddress VALID_BOT_1_CHAT_ID = TelegramIdAddress.create(BOT_1, validChatId);
    private static final TelegramIdAddress INVALID_BOT_1_CHAT_ID = TelegramIdAddress.create(BOT_1, invalidChatId);

    private static final String BOT_2 = "bot_2";
    private static final TelegramIdAddress VALID_BOT_2_CHAT_ID = TelegramIdAddress.create(BOT_2, validChatId);
    private static final TelegramIdAddress INVALID_BOT_2_CHAT_ID = TelegramIdAddress.create(BOT_2, invalidChatId);

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private WireMockServer tgApiMock;

    @Test
    public void testSendMessageToValidChat() {
        String messageToSend = "Notification text";
        configureTgApiMock(BOT_1_API_URL, messageToSend, true, ok(createOkResponse()), false);
        configureTgApiMock(BOT_1_API_URL, messageToSend, false, ok(create400Response()), false);

        SendMessageResponse response = notificationService.sendMessage(
                Collections.singletonList(VALID_BOT_1_CHAT_ID),
                messageToSend,
                null
        );

        assertEquals(1, response.getSuccessful().size());
        assertEquals(0, response.getFailed().size());
        assertEquals(validChatId, (long) response.getSuccessful().get(0).getTelegramId());
    }

    @Test
    public void testSendMessageWithKeyboardToValidChat() {
        String messageToSend = "Notification text";
        configureTgApiMock(BOT_1_API_URL, messageToSend, true, ok(createOkResponse()), true);
        configureTgApiMock(BOT_1_API_URL, messageToSend, false, ok(create400Response()), true);

        SendMessageResponse response = notificationService.sendMessage(
                Collections.singletonList(VALID_BOT_1_CHAT_ID),
                messageToSend,
                new InlineKeyboardMarkup(
                        new InlineKeyboardButton("A").callbackData("data"),
                        new InlineKeyboardButton("B").url("http://ya.ru").callbackData(null),
                        new InlineKeyboardButton("C").url("http://ya.ru").callbackData("data")
                )
        );

        assertEquals(1, response.getSuccessful().size());
        assertEquals(0, response.getFailed().size());
        assertEquals(validChatId, (long) response.getSuccessful().get(0).getTelegramId());
    }

    @Test
    public void testSendMessageWithKeyboardToValidChatAnd429Response() {
        String messageToSend = "Notification too many requests text";
        configureTgApiMock(BOT_1_API_URL, messageToSend, true, ok(create429Response()), true);
        configureTgApiMock(BOT_1_API_URL, messageToSend, false, aResponse().withStatus(429), true);

        SendMessageResponse response = notificationService.sendMessage(
                Collections.singletonList(VALID_BOT_1_CHAT_ID),
                messageToSend,
                new InlineKeyboardMarkup(
                        new InlineKeyboardButton("A").callbackData("data"),
                        new InlineKeyboardButton("B").url("http://ya.ru").callbackData(null),
                        new InlineKeyboardButton("C").url("http://ya.ru").callbackData("data")
                )
        );

        assertEquals(0, response.getSuccessful().size());
        assertEquals(1, response.getFailed().size());
        assertEquals(validChatId, (long) response.getFailed().get(0).getTelegramId());
    }

    @Test
    public void testSendMessageToInvalidChat() {
        String messageToSend = "Notification text";
        configureTgApiMock(BOT_1_API_URL, messageToSend, true, ok(createOkResponse()), false);
        configureTgApiMock(BOT_1_API_URL, messageToSend, false, ok(create400Response()), false);

        SendMessageResponse response = notificationService.sendMessage(
                Collections.singletonList(INVALID_BOT_1_CHAT_ID),
                messageToSend,
                null
        );

        assertEquals(0, response.getSuccessful().size());
        assertEquals(1, response.getFailed().size());
        assertEquals(invalidChatId, (long) response.getFailed().get(0).getTelegramId());
    }

    @Test
    public void testSendMessageToValidAndInvalidChats() {
        String messageToSend = "Notification text";
        configureTgApiMock(BOT_1_API_URL, messageToSend, true, ok(createOkResponse()), false);
        configureTgApiMock(BOT_1_API_URL, messageToSend, false, ok(create400Response()), false);

        SendMessageResponse response = notificationService.sendMessage(
                Arrays.asList(VALID_BOT_1_CHAT_ID, INVALID_BOT_1_CHAT_ID),
                messageToSend,
                null
        );

        assertEquals(1, response.getSuccessful().size());
        assertEquals(1, response.getFailed().size());
        assertEquals(validChatId, (long) response.getSuccessful().get(0).getTelegramId());
        assertEquals(invalidChatId, (long) response.getFailed().get(0).getTelegramId());
    }

    @Test
    public void testSendMessageToValidWithTgServerError() {
        String messageToSend = "Notification text";
        configureTgApiMock(BOT_1_API_URL, messageToSend, true, ok(create500Response()), false);

        SendMessageResponse response = notificationService.sendMessage(
                Collections.singletonList(VALID_BOT_1_CHAT_ID),
                messageToSend,
                null
        );

        assertEquals(0, response.getSuccessful().size());
        assertEquals(1, response.getFailed().size());
        assertEquals("500: Server Error", response.getFailed().get(0).getError());
    }

    @Test
    void testSendMultipleMessages() {
        String messageToSend = "Notification text";
        configureTgApiMock(BOT_1_API_URL, messageToSend, true, ok(createOkResponse()), false);
        configureTgApiMock(BOT_2_API_URL, messageToSend, true, ok(createOkResponse()), false);

        SendMessageResponse response = notificationService.sendMessage(
                List.of(VALID_BOT_1_CHAT_ID, VALID_BOT_2_CHAT_ID),
                messageToSend,
                null
        );

        assertEquals(2, response.getSuccessful().size());
        assertEquals(0, response.getFailed().size());
    }

    @Test
    void testSendMultipleMessagesWithValidAndInvalidChats() {
        String messageToSend = "Notification text";
        configureTgApiMock(BOT_1_API_URL, messageToSend, true, ok(createOkResponse()), false);
        configureTgApiMock(BOT_2_API_URL, messageToSend, false, ok(create400Response()), false);

        SendMessageResponse response = notificationService.sendMessage(
                List.of(VALID_BOT_1_CHAT_ID, INVALID_BOT_2_CHAT_ID),
                messageToSend,
                null
        );

        assertEquals(1, response.getSuccessful().size());
        assertEquals(validChatId, response.getSuccessful().get(0).getTelegramId());
        assertEquals(BOT_1, response.getSuccessful().get(0).getBotId());

        assertEquals(1, response.getFailed().size());
        assertEquals(invalidChatId, response.getFailed().get(0).getTelegramId());
        assertEquals(BOT_2, response.getFailed().get(0).getBotId());
    }

    private void configureTgApiMock(
            String tgApiUrl,
            String messageToSend,
            boolean isChatValid,
            ResponseDefinitionBuilder response,
            boolean doAddMarkup
    ) {
        tgApiMock.stubFor(post(urlPathEqualTo(tgApiUrl))
                .withRequestBody(matching(requestBody(messageToSend, isChatValid, doAddMarkup)))
                .willReturn(response));

        tgApiMock.stubFor(post(urlPathEqualTo(tgApiUrl))
                .withRequestBody(matching(requestBodyWithKeyboard(messageToSend, isChatValid)))
                .willReturn(response));
    }

    private String requestBody(String messageToSend, boolean isChatValid, boolean doAddMarkup) {
        return "chat_id=" + (isChatValid ? validChatId : invalidChatId) +
                "&text=" + UrlEscapers.urlFragmentEscaper().escape(messageToSend) +
                "&disable_web_page_preview=true" +
                "&parse_mode=Markdown" +
                (doAddMarkup ? "&reply_markup=.*" : "");
    }

    private String requestBodyWithKeyboard(String messageToSend, boolean isChatValid) {
        return "chat_id=" + (isChatValid ? validChatId : invalidChatId) +
                "&text=" + UrlEscapers.urlFragmentEscaper().escape(messageToSend) +
                "&disable_web_page_preview=true" +
                "&parse_mode=Markdown" +
                "&reply_markup=.*";
    }

}
