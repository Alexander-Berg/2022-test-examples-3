package ru.yandex.market.mbi.bot.tg;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pengrad.telegrambot.model.Update;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbi.bot.FunctionalTest;
import ru.yandex.market.mbi.bot.tg.menu.MenuService;
import ru.yandex.market.mbi.bot.tg.model.TgBotAccount;
import ru.yandex.market.mbi.bot.tg.service.TgBotAccountService;
import ru.yandex.market.mbi.bot.tg.service.UpdateProcessor;

import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static ru.yandex.market.mbi.bot.tg.TelegramTestUtils.createCallbackQuery;
import static ru.yandex.market.mbi.bot.tg.TelegramTestUtils.createChat;
import static ru.yandex.market.mbi.bot.tg.TelegramTestUtils.createMessage;
import static ru.yandex.market.mbi.bot.tg.TelegramTestUtils.createOkResponse;
import static ru.yandex.market.mbi.bot.tg.TelegramTestUtils.createUpdate;
import static ru.yandex.market.mbi.bot.tg.TelegramTestUtils.createUser;

/**
 * Тесты для {@link UpdateProcessor}
 */
public class UpdateProcessorTest extends FunctionalTest {

    private static final Gson GSON = new Gson();

    @Autowired
    private WireMockServer mbiApiMock;

    @Autowired
    private WireMockServer tgApiMock;

    @Autowired
    private UpdateProcessor updateProcessor;

    @Autowired
    private TgBotAccountService accountService;

    @Autowired
    private MenuService menuService;

    private TgBotAccount account;

    @BeforeEach
    public void reset() {
        mbiApiMock.resetAll();
        tgApiMock.resetAll();
        account = accountService.getAccount("bot_1");
    }

    @Test
    public void testEmptyCommand() {
        JsonObject from = createUser();
        JsonObject chat = createChat();
        JsonObject message = createMessage(from, chat, null);

        JsonObject update = new JsonObject();
        update.addProperty("update_id", 9000);
        update.add("message", message);

        updateProcessor.processUpdate(account, GSON.fromJson(update, Update.class));
        //Тест выполнен если не вылетело NullpointerException
    }

    @Test
    public void testSendStartCommandAsMessage() {
        mbiApiMock.stubFor(post(urlPathEqualTo("/telegram/sendInvitation"))
                .withRequestBody(equalTo("<telegram-account username=\"username\" tg-id=\"100500\" " +
                        "first-name=\"First\" last-name=\"Last\"/>"))
                .withHeader("Content-Type", equalTo("application/xml"))
                .willReturn(ok()));
        mbiApiMock.stubFor(get(urlPathEqualTo("/telegram/users/100500/")).willReturn(ok(
                "<response>\n" +
                        "  <value>true</value>\n" +
                        "</response>"
                ).withHeader("Content-Type", "application/xml")
        ));
        mbiApiMock.stubFor(patch(urlPathEqualTo("/telegram/users/100500/activate/")).willReturn(ok()));

        JsonObject from = createUser();
        JsonObject chat = createChat();
        JsonObject message = createMessage(from, chat, "/start");

        JsonObject update = new JsonObject();
        update.addProperty("update_id", 9000);
        update.add("message", message);

        updateProcessor.processUpdate(account, GSON.fromJson(update, Update.class));
    }

    @Test
    public void testSendBlankTextData() {
        JsonObject from = createUser();
        Update update = createUpdate(createMessage(from, createChat(), null), null);

        updateProcessor.processUpdate(account, update);
    }

    @Test
    public void testSendUnsubscribeCommandAsCallbackQuery() {
        mbiApiMock.stubFor(post(urlPathEqualTo("/telegram/users/100500/notifications/140/unsubscribe"))
                .willReturn(ok()));

        JsonObject from = createUser();
        JsonObject callbackQuery = createCallbackQuery(from, "/unsubscribe 140 null");

        Update update = createUpdate(createMessage(from, createChat(), "someText"), callbackQuery);

        updateProcessor.processUpdate(account, update);
    }

    @Test
    public void testSendResubscribeCommandAsCallbackQuery() {
        mbiApiMock.stubFor(post(urlPathEqualTo("/telegram/users/100500/notifications/140/resubscribe"))
                .willReturn(ok()));

        JsonObject from = createUser();
        JsonObject callbackQuery = createCallbackQuery(from, "/resubscribe 140 null");
        Update update = createUpdate(callbackQuery);

        updateProcessor.processUpdate(account, update);
    }

    @Test
    public void testAcceptOrderCommand() {
        mbiApiMock.stubFor(post(urlEqualTo("/telegram/users/100500/orders/10/accept?campaignId=100&botId=bot_1"))
                .willReturn(ok()));

        JsonObject from = createUser();
        JsonObject callbackQuery = createCallbackQuery(from, "/acceptOrder 100 10");
        Update update = createUpdate(callbackQuery);

        updateProcessor.processUpdate(account, update);
    }

    @Test
    public void testAcceptOrderCommandBadParams() {
        JsonObject from = createUser();
        JsonObject callbackQuery = createCallbackQuery(from, "/acceptOrder 100asd");
        Update update = createUpdate(callbackQuery);

        updateProcessor.processUpdate(account, update);
    }

    @Test
    public void testSendUnknownCommandAsCallbackQuery() {
        JsonObject from = createUser();
        JsonObject callbackQuery = createCallbackQuery(from, "/unknown");
        JsonObject message = createMessage(from, createChat(), "someText");
        Update update = createUpdate(message, callbackQuery);

        updateProcessor.processUpdate(account, update);
    }

    @Test
    public void testSubscriptionsManagement() {
        //language=xml
        String mbiApiResponse = "" +
                "<notification-themes>\n" +
                "    <themes>\n" +
                "        <theme>\n" +
                "            <id>1</id>\n" +
                "            <name>Заказы</name>\n" +
                "            <partner-id>null</partner-id>\n" +
                "        </theme>\n" +
                "        <theme>\n" +
                "            <id>6</id>\n" +
                "            <name>Технические ошибки</name>\n" +
                "            <partner-id>null</partner-id>\n" +
                "        </theme>\n" +
                "        <theme>\n" +
                "            <id>4</id>\n" +
                "            <name>Отзывы</name>\n" +
                "            <partner-id>10</partner-id>\n" +
                "        </theme>\n" +
                "        <theme>\n" +
                "            <id>3</id>\n" +
                "            <name>Оплата</name>\n" +
                "            <partner-id>10</partner-id>\n" +
                "        </theme>\n" +
                "        <theme>\n" +
                "            <id>7</id>\n" +
                "            <name>Управление магазином</name>\n" +
                "            <partner-id>10</partner-id>\n" +
                "        </theme>\n" +
                "    </themes>\n" +
                "</notification-themes>";
        mbiApiMock.stubFor(get(urlEqualTo("/telegram/users/100500/subscriptions/themes/1/notifications"))
                .willReturn(ok(mbiApiResponse).withHeader("Content-Type", "application/xml")));

        tgApiMock.stubFor(post(urlPathEqualTo("/bottoken/sendMessage")).willReturn(ok(createOkResponse())));

        JsonObject from = createUser();
        JsonObject chat = createChat();
        JsonObject message = createMessage(from, chat, "Управление подписками");
        Update update = createUpdate(message, null);

        updateProcessor.processUpdate(account, update);
    }

    @Test
    public void testChooseSubscriptionTheme() {
        //language=xml
        String mbiApiResponse = "" +
                "<notification-themes>\n" +
                "    <notifications-meta>\n" +
                "        <notification-meta id=\"1714360125\" name=\"Уведомление о наступающей дате отгрузки\" partner-id=\"null\" is-active=\"false\"/>\n" +
                "        <notification-meta id=\"1612872409\" name=\"Уведомление поставщика через телеграм о новом заказе\" partner-id=\"10\" is-active=\"true\"/>\n" +
                "        <notification-meta id=\"1606176000\" name=\"Шаблон уведомления о просрочке под утилизацию\" partner-id=\"10\" is-active=\"false\"/>\n" +
                "        <notification-meta id=\"1614852412\" name=\"Покупатель отменил заказ\" partner-id=\"10\" is-active=\"false\"/>\n" +
                "    </notifications-meta>\n" +
                "</notification-themes>";
        mbiApiMock.stubFor(get(urlEqualTo("/telegram/users/100500/subscriptions/themes/1/notifications"))
                .willReturn(ok(mbiApiResponse).withHeader("Content-Type", "application/xml")));

        tgApiMock.stubFor(post(urlPathEqualTo("/bottoken/sendMessage")).willReturn(ok(createOkResponse())));

        JsonObject from = createUser();
        JsonObject chat = createChat();
        JsonObject message = createMessage(from, chat, "Темы подписок");
        JsonObject callbackQuery = createCallbackQuery(from, chat,message, "/manageSubscriptions 1");
        Update update = createUpdate(callbackQuery);

        updateProcessor.processUpdate(account, update);
    }

    @Test
    public void testManageSubscription() {
        mbiApiMock.stubFor(post(urlPathEqualTo("/telegram/users/100500/notifications/1612872409/unsubscribe"))
                .willReturn(ok()));
        //language=xml
        String mbiApiResponse = "" +
                "<notification-themes>\n" +
                "    <notifications-meta>\n" +
                "        <notification-meta id=\"1714360125\" name=\"Уведомление о наступающей дате отгрузки\" partner-id=\"null\" is-active=\"false\"/>\n" +
                "        <notification-meta id=\"1612872409\" name=\"Уведомление поставщика через телеграм о новом заказе\" partner-id=\"10\" is-active=\"true\"/>\n" +
                "        <notification-meta id=\"1606176000\" name=\"Шаблон уведомления о просрочке под утилизацию\" partner-id=\"10\" is-active=\"false\"/>\n" +
                "        <notification-meta id=\"1614852412\" name=\"Покупатель отменил заказ\" partner-id=\"10\" is-active=\"false\"/>\n" +
                "    </notifications-meta>\n" +
                "</notification-themes>";
        mbiApiMock.stubFor(get(urlEqualTo("/telegram/users/100500/subscriptions/themes/1/notifications"))
                .willReturn(ok(mbiApiResponse).withHeader("Content-Type", "application/xml")));
        tgApiMock.stubFor(post(urlPathEqualTo("/bottoken/sendMessage")).willReturn(ok(createOkResponse())));

        JsonObject from = createUser();
        JsonObject chat = createChat();
        JsonObject message = createMessage(from, chat, "Подписки");
        JsonObject callbackQuery = createCallbackQuery(from, chat, message, "/manageSubscription 1 1612872409 10 false");
        Update update = createUpdate(callbackQuery);

        updateProcessor.processUpdate(account, update);
    }

    @Test
    void chatBotMenuButtonShouldSendMessage() {
        tgApiMock.resetAll();

        tgApiMock.stubFor(any(urlPathMatching("/(.*)/setMyCommands")).willReturn(ok(createOkResponse())));
        tgApiMock.stubFor(any(urlPathMatching("/(.*)/sendMessage")).willReturn(ok(createOkResponse())));

        String menuJson = "{" +
                "    \"menu\": [" +
                "        {" +
                "            \"command\": \"/command1\"," +
                "            \"description\": \"Сделайте мне хорошо\"," +
                "            \"scenario\": \"makeMeOk\"" +
                "        }" +
                "    ]," +
                "    \"scenarios\": [" +
                "        {" +
                "            \"scenarioId\": \"makeMeOk\"," +
                "            \"initialPage\": 1," +
                "            \"pages\": [" +
                "                {" +
                "                    \"pageId\": 1," +
                "                    \"text\": \"text 1\"," +
                "                    \"keyboard\": [" +
                "                        [" +
                "                            {" +
                "                                \"caption\": \"button 1\"," +
                "                                \"command\": \"/gotoPage makeMeOk 2\"" +
                "                            }," +
                "                            {" +
                "                                \"caption\": \"button 2\"," +
                "                                \"command\": \"/gotoPage makeMeOk 3\"" +
                "                            }" +
                "                        ]," +
                "                        [" +
                "                            {" +
                "                                \"caption\": \"button 3\"" +
                "                            }" +
                "                        ]" +
                "                    ]" +
                "                }," +
                "                {" +
                "                    \"pageId\": 2," +
                "                    \"text\": \"Вам уже хорошо!\"," +
                "                    \"keyboard\": [" +
                "                        [" +
                "                            {" +
                "                                \"caption\": \"Вернуться назад\"," +
                "                                \"command\": \"/gotoPage makeMeOk 1\"" +
                "                            }" +
                "                        ]" +
                "                    ]" +
                "                }," +
                "                {" +
                "                    \"pageId\": 3," +
                "                    \"text\": \"Сходи поищи [в интернете](https://yandex.ru)\"" +
                "                }" +
                "            ]" +
                "        }" +
                "    ]" +
                "}";
        menuService.initMenu(new ByteArrayInputStream(menuJson.getBytes(StandardCharsets.UTF_8)));

        JsonObject from = createUser();
        JsonObject chat = createChat();
        JsonObject message = createMessage(from, chat, "/command1");
        JsonObject callbackQuery = createCallbackQuery(from, chat, message, "/command1");
        Update update = createUpdate(callbackQuery);

        updateProcessor.processUpdate(account, update);
    }
}
