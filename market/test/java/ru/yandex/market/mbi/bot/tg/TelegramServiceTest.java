package ru.yandex.market.mbi.bot.tg;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbi.bot.FunctionalTest;
import ru.yandex.market.mbi.bot.tg.jpa.repository.LastProcessedUpdateRepository;
import ru.yandex.market.mbi.bot.tg.model.TgBotAccount;
import ru.yandex.market.mbi.bot.tg.service.TelegramService;
import ru.yandex.market.mbi.bot.tg.service.TgBotAccountService;
import ru.yandex.market.mbi.bot.tg.service.mode.pull.TgPullUpdateService;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.mbi.bot.tg.TelegramTestUtils.create403Response;
import static ru.yandex.market.mbi.bot.tg.TelegramTestUtils.create404Response;
import static ru.yandex.market.mbi.bot.tg.TelegramTestUtils.createOkResponse;

class TelegramServiceTest extends FunctionalTest {

    public static final String BOT_1 = "bot_1";

    @Autowired
    private WireMockServer tgApiMock;

    @Autowired
    private WireMockServer mbiApiMock;

    @Autowired
    private TelegramService telegramService;

    @Autowired
    private LastProcessedUpdateRepository lastProcessedUpdateRepository;

    @Autowired
    private TgPullUpdateService pullUpdateService;

    @Autowired
    private TgBotAccountService accountService;

    @BeforeEach
    void reset() {
        mbiApiMock.resetAll();
        tgApiMock.resetAll();
        lastProcessedUpdateRepository.deleteAllInBatch();
    }

    @Test
    void testSendInvitation() {
        tgApiMock.stubFor(post(anyUrl()).willReturn(ok(createMessage("/start", 100500L))));
        mbiApiMock.stubFor(post(urlPathEqualTo("/telegram/sendInvitation"))
                .withRequestBody(equalTo("<telegram-account username=\"username\" tg-id=\"12345\" " +
                        "first-name=\"First\" last-name=\"Last\" bot-id=\"bot_1\"/>"))
                .withHeader("Content-Type", equalTo("application/xml"))
                .willReturn(ok()));
        mbiApiMock.stubFor(post(urlPathEqualTo("/telegram/sendInvitation"))
                .withRequestBody(equalTo("<telegram-account username=\"username\" tg-id=\"12345\" " +
                        "first-name=\"First\" last-name=\"Last\" bot-id=\"bot_2\"/>"))
                .withHeader("Content-Type", equalTo("application/xml"))
                .willReturn(ok()));
        mbiApiMock.stubFor(get(urlPathEqualTo("/telegram/users/12345/")).willReturn(ok(
                "<response>\n" +
                        "  <value>false</value>\n" +
                        "</response>"
                ).withHeader("Content-Type", "application/xml")
        ));
        for (int i = 0; i < 5; ++i) {
            pullUpdateService.pullUpdates();
        }
        mbiApiMock.verify(2, postRequestedFor(urlEqualTo("/telegram/sendInvitation")));
    }

    @Test
    void testReactivateUser() {
        tgApiMock.stubFor(post(anyUrl()).willReturn(ok(createMessage("/start", 100501L))));
        mbiApiMock.stubFor(get(urlPathEqualTo("/telegram/users/12345/")).willReturn(ok(
                "<response>\n" +
                        "  <value>true</value>\n" +
                        "</response>"
                ).withHeader("Content-Type", "application/xml")
        ));
        mbiApiMock.stubFor(patch(urlPathEqualTo("/telegram/users/12345/activate/")).willReturn(ok()));
        pullUpdateService.pullUpdates();
        mbiApiMock.verify(1, patchRequestedFor(urlEqualTo("/telegram/users/12345/activate/?botId=bot_1")));
        mbiApiMock.verify(1, patchRequestedFor(urlEqualTo("/telegram/users/12345/activate/?botId=bot_2")));
    }

    @ParameterizedTest
    @ValueSource(strings = {BOT_1, "bot_2"})
    void testSendMessageShouldDeactivateUserWhen403Response(String botId) {
        var userId = 123L;
        TgBotAccount account = accountService.getAccount(botId);
        tgApiMock.stubFor(post(urlPathEqualTo("/bot" + account.getToken() + "/sendMessage"))
                .willReturn(ok(create403Response())));
        mbiApiMock.stubFor(patch(urlPathEqualTo("/telegram/users/" + userId + "/deactivate/"))
                .willReturn(ok()));
        IOException e = Assertions.assertThrows(
                IOException.class,
                () -> telegramService.sendMessage("message", account, userId, null, ParseMode.Markdown)
        );
        Assertions.assertEquals("Can't send message to chat " + userId +
                " from " + botId + ", because it's blocked or unavailable. " +
                "Telegram account " + userId + " is deactivated.", e.getMessage());
    }

    @Test
    void testSendMessageShouldErrorWhenNotOk() {
        tgApiMock.stubFor(post(urlPathEqualTo("/bottoken/sendMessage"))
                .willReturn(ok(create404Response())));
        UnsupportedOperationException e = Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> telegramService.sendMessage("message", accountService.getAccount(BOT_1), 123L, null,
                        ParseMode.Markdown)
        );
        Assertions.assertEquals("Unsupported error 404: Not found", e.getMessage());
    }

    @Test
    void testSendMessageSplitLongMessagesRunner() throws IOException {
        int[][] params = new int[][]{
                new int[]{1, 1000},
                new int[]{1, 4096},
                new int[]{2, 5000},
                new int[]{3, 8192},
                new int[]{3, 10000},
                new int[]{5, 20000},
        };

        for (int[] param : params) {
            testSendMessageSplitLongMessages(param[0], param[1]);
        }
    }

    private void testSendMessageSplitLongMessages(int expectedRequestCount, int messageLength) throws IOException {
        reset();
        String tooLongMessage = "AB C".repeat(messageLength / 4);
        String sendMessageUrl = "/bottoken/sendMessage";

        tgApiMock.stubFor(post(urlPathEqualTo(sendMessageUrl)).willReturn(ok(createOkResponse())));
        telegramService.sendMessage(
                tooLongMessage,
                accountService.getAccount(BOT_1),
                123L,
                new InlineKeyboardMarkup(new InlineKeyboardButton("button")),
                ParseMode.Markdown
        );

        tgApiMock.verify(expectedRequestCount, postRequestedFor(urlEqualTo(sendMessageUrl)));
        List<LoggedRequest> requestsResult = tgApiMock
                .findAll(postRequestedFor(urlPathEqualTo(sendMessageUrl)));

        // 1. проверим что текст есть и его длинна валидна
        for (LoggedRequest request : requestsResult) {
            String text = extractBodyPart(request, "text");
            assertTrue(text.length() <= 4096, "Text is too long " + text.length());
        }

        // 2. проверим, что последнее сообщение из серии содержит кнопку отписаться.
        assertTrue(extractBodyPart(requestsResult.get(requestsResult.size() - 1), "reply_markup").length() > 0);
    }

    private String extractBodyPart(LoggedRequest request, String key) {
        assertNotNull(request);
        String bodyAsString = request.getBodyAsString();
        assertNotNull(bodyAsString);
        List<NameValuePair> params = URLEncodedUtils.parse(bodyAsString, StandardCharsets.UTF_8);
        for (org.apache.http.NameValuePair param : params) {
            if (key.equals(param.getName())) {
                return param.getValue();
            }
        }

        throw new IllegalStateException(key + " must exist");
    }

    private String createMessage(String message, Long updateId) {
        return String.format("{\n" +
                "    \"ok\": true,\n" +
                "    \"result\": [\n" +
                "        {\n" +
                "            \"update_id\": %d,\n" +
                "            \"message\": {\n" +
                "                \"message_id\": 1164,\n" +
                "                \"from\": {\n" +
                "                    \"id\": 12345,\n" +
                "                    \"is_bot\": false,\n" +
                "                    \"first_name\": \"First\",\n" +
                "                    \"last_name\": \"Last\",\n" +
                "                    \"username\": \"username\",\n" +
                "                    \"language_code\": \"ru\"\n" +
                "                },\n" +
                "                \"chat\": {\n" +
                "                    \"id\": 12345,\n" +
                "                    \"first_name\": \"First\",\n" +
                "                    \"last_name\": \"Last\",\n" +
                "                    \"username\": \"username\",\n" +
                "                    \"type\": \"private\"\n" +
                "                },\n" +
                "                \"date\": 1598207519,\n" +
                "                \"text\": \"%s\",\n" +
                "                \"entities\": [\n" +
                "                    {\n" +
                "                        \"offset\": 0,\n" +
                "                        \"length\": 6,\n" +
                "                        \"type\": \"bot_command\"\n" +
                "                    }\n" +
                "                ]\n" +
                "            }\n" +
                "        }\n" +
                "    ]\n" +
                "}", updateId, message);
    }
}
