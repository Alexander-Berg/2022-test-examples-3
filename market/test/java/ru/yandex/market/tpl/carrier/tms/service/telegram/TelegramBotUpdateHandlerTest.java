package ru.yandex.market.tpl.carrier.tms.service.telegram;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.GetUpdates;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.GetUpdatesResponse;
import com.pengrad.telegrambot.response.SendResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.rating.RatingHelper;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.telegram.TelegramChat;
import ru.yandex.market.tpl.carrier.core.domain.telegram.TelegramChatRepository;
import ru.yandex.market.tpl.carrier.tms.TmsIntTest;
import ru.yandex.market.tpl.common.util.DateTimeUtil;


@TmsIntTest
class TelegramBotUpdateHandlerTest {

    private final long UNKNOWN_CHAT_ID = 1L;
    private final long CHAT_ID_WITH_MAPPING = 2L;

    private final List<String> PROCESS_UPDATES_RESPONSE_MESSAGE_LOGGER = new ArrayList<>();;

    @Value("${tpl.telegram.infobot.name:unknown}")
    private String botUsername;

    @Autowired
    private TelegramBotUpdateHandler telegramBotUpdateHandler;
    @Autowired
    private TelegramChatRepository chatRepository;
    @Autowired
    private TestUserHelper testUserHelper;
    @Autowired
    private RatingHelper ratingHelper;

    @Value("${tpl.telegram.infobot.auth.url}")
    private String telegramBotAuthUrl;
    @Value("${tpl.telegram.infobot.auth.param-name}")
    private String telegramBotAuthParamName;
    @Value("${tpl.telegram.infobot.auth.token-length}")
    private int telegramBotAuthTokenLength;

    private String telegramBotAuthUrlTemplate;
    private Company company;

    @Autowired
    private TelegramBot telegramBot;
    @Autowired
    private TestableClock clock;

    @BeforeEach
    private void setUp() {
        clock.setFixed(ZonedDateTime.of(1990, 1, 1, 10, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID).toInstant(), DateTimeUtil.DEFAULT_ZONE_ID);

        telegramBotAuthUrlTemplate = String.format("%s\\?%s=", telegramBotAuthUrl, telegramBotAuthParamName);
    }

    @Test
    void shouldStartUpdatesListener() {
        telegramBotMockBeanSetUp(List.of());

        Assertions.assertTrue(telegramBotUpdateHandler.isStarted());
        telegramBotUpdateHandler.stop();
        Assertions.assertFalse(telegramBotUpdateHandler.isStarted());
        telegramBotUpdateHandler.start();
        Assertions.assertTrue(telegramBotUpdateHandler.isStarted());
    }

    @Test
    void shouldProcessUpdatesNoCompanyStartCommand() {
        Update update = Mockito.mock(Update.class);
        Message message1 = Mockito.mock(Message.class);
        Chat chat1 = Mockito.mock(Chat.class);
        Mockito.doReturn(Chat.Type.Private).when(chat1).type();
        Mockito.doReturn(UNKNOWN_CHAT_ID).when(chat1).id();
        Mockito.doReturn("Chat Title 1").when(chat1).title();
        Mockito.doReturn("Chat Username 1").when(chat1).username();
        Mockito.doReturn("/start").when(message1).text();
        Mockito.doReturn(new User[0]).when(message1).newChatMembers();
        Mockito.doReturn(chat1).when(message1).chat();
        Mockito.doReturn(1).when(update).updateId();
        Mockito.doReturn(message1).when(update).message();
        telegramBotMockBeanSetUp(List.of(update));

        String expectedMessage = "Чтобы активировать бота перейдите по ссылке  и войдите в аккаунт логиста\n\n" +
                                    "Для справки напишите /help";

        telegramBotUpdateHandler.processNextUpdates();

        Assertions.assertEquals(1, PROCESS_UPDATES_RESPONSE_MESSAGE_LOGGER.size());
        String actualMessage = PROCESS_UPDATES_RESPONSE_MESSAGE_LOGGER.get(0);

        String[] actualMessageParts = actualMessage.split(telegramBotAuthUrlTemplate);
        Assertions.assertEquals(2, actualMessageParts.length);
        Assertions.assertTrue(actualMessageParts[1].length() > telegramBotAuthTokenLength);
        actualMessageParts[1] = actualMessageParts[1].substring(telegramBotAuthTokenLength);
        actualMessage = actualMessageParts[0] + actualMessageParts[1];

        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void shouldProcessUpdatesNoCompanyStatusCommand() {
        Update update = Mockito.mock(Update.class);
        Message message2 = Mockito.mock(Message.class);
        Chat chat2 = Mockito.mock(Chat.class);
        Mockito.doReturn(Chat.Type.group).when(chat2).type();
        Mockito.doReturn(UNKNOWN_CHAT_ID).when(chat2).id();
        Mockito.doReturn("Chat Title 2").when(chat2).title();
        Mockito.doReturn("Chat Username 2").when(chat2).username();
        Mockito.doReturn("/status").when(message2).text();
        Mockito.doReturn(new User[0]).when(message2).newChatMembers();
        Mockito.doReturn(chat2).when(message2).chat();
        Mockito.doReturn(2).when(update).updateId();
        Mockito.doReturn(message2).when(update).message();
        telegramBotMockBeanSetUp(List.of(update));

        String expectedMessage = "Чтобы активировать бота перейдите по ссылке  и войдите в аккаунт логиста\n\n" +
                                    "Для справки напишите /help";

        telegramBotUpdateHandler.processNextUpdates();

        Assertions.assertEquals(1, PROCESS_UPDATES_RESPONSE_MESSAGE_LOGGER.size());
        String actualMessage = PROCESS_UPDATES_RESPONSE_MESSAGE_LOGGER.get(0);

        String[] actualMessageParts = actualMessage.split(telegramBotAuthUrlTemplate);
        Assertions.assertEquals(2, actualMessageParts.length);
        Assertions.assertTrue(actualMessageParts[1].length() > telegramBotAuthTokenLength);
        actualMessageParts[1] = actualMessageParts[1].substring(telegramBotAuthTokenLength);
        actualMessage = actualMessageParts[0] + actualMessageParts[1];

        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void shouldProcessUpdatesNoCompanyHelpCommand() {
        Update update = Mockito.mock(Update.class);
        Message message3 = Mockito.mock(Message.class);
        Chat chat3 = Mockito.mock(Chat.class);
        Mockito.doReturn(Chat.Type.supergroup).when(chat3).type();
        Mockito.doReturn(UNKNOWN_CHAT_ID).when(chat3).id();
        Mockito.doReturn("Chat Title 3").when(chat3).title();
        Mockito.doReturn("Chat Username 3").when(chat3).username();
        Mockito.doReturn("/help").when(message3).text();
        Mockito.doReturn(new User[0]).when(message3).newChatMembers();
        Mockito.doReturn(chat3).when(message3).chat();
        Mockito.doReturn(3).when(update).updateId();
        Mockito.doReturn(message3).when(update).message();
        telegramBotMockBeanSetUp(List.of(update));

        String expectedMessage = "Регулярно в 8:00 бот будет присылать статус по текущим рейсам. " +
                                    "Кроме того справку по текущим рейсам можно получить по команде /status";

        telegramBotUpdateHandler.processNextUpdates();

        Assertions.assertEquals(1, PROCESS_UPDATES_RESPONSE_MESSAGE_LOGGER.size());
        String actualMessage = PROCESS_UPDATES_RESPONSE_MESSAGE_LOGGER.get(0);

        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void shouldProcessUpdatesNoCompanyUnknownCommand() {
        Update update = Mockito.mock(Update.class);
        Message message4 = Mockito.mock(Message.class);
        Chat chat4 = Mockito.mock(Chat.class);
        Mockito.doReturn(Chat.Type.group).when(chat4).type();
        Mockito.doReturn(UNKNOWN_CHAT_ID).when(chat4).id();
        Mockito.doReturn("Chat Title 4").when(chat4).title();
        Mockito.doReturn("Chat Username 4").when(chat4).username();
        Mockito.doReturn("Unknown message 4").when(message4).text();
        Mockito.doReturn(new User[0]).when(message4).newChatMembers();
        Mockito.doReturn(chat4).when(message4).chat();
        Mockito.doReturn(4).when(update).updateId();
        Mockito.doReturn(message4).when(update).message();
        telegramBotMockBeanSetUp(List.of(update));

        telegramBotUpdateHandler.processNextUpdates();

        Assertions.assertEquals(0, PROCESS_UPDATES_RESPONSE_MESSAGE_LOGGER.size());
    }

    @Test
    void shouldProcessUpdatesNoCompanyDropCommand() {
        Update update = Mockito.mock(Update.class);
        Message message3 = Mockito.mock(Message.class);
        Chat chat3 = Mockito.mock(Chat.class);
        Mockito.doReturn(Chat.Type.supergroup).when(chat3).type();
        Mockito.doReturn(UNKNOWN_CHAT_ID).when(chat3).id();
        Mockito.doReturn("Chat Title 3").when(chat3).title();
        Mockito.doReturn("Chat Username 3").when(chat3).username();
        Mockito.doReturn("/drop").when(message3).text();
        Mockito.doReturn(new User[0]).when(message3).newChatMembers();
        Mockito.doReturn(chat3).when(message3).chat();
        Mockito.doReturn(3).when(update).updateId();
        Mockito.doReturn(message3).when(update).message();
        telegramBotMockBeanSetUp(List.of(update));

        String expectedMessage = "Чтобы активировать бота перейдите по ссылке  и войдите в аккаунт логиста\n\n" +
                "Для справки напишите /help";

        telegramBotUpdateHandler.processNextUpdates();

        Assertions.assertEquals(1, PROCESS_UPDATES_RESPONSE_MESSAGE_LOGGER.size());
        String actualMessage = PROCESS_UPDATES_RESPONSE_MESSAGE_LOGGER.get(0);

        String[] actualMessageParts = actualMessage.split(telegramBotAuthUrlTemplate);
        Assertions.assertEquals(2, actualMessageParts.length);
        Assertions.assertTrue(actualMessageParts[1].length() > telegramBotAuthTokenLength);
        actualMessageParts[1] = actualMessageParts[1].substring(telegramBotAuthTokenLength);
        actualMessage = actualMessageParts[0] + actualMessageParts[1];

        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void shouldProcessUpdatesNoCompanyBotAddedToGroupChat() {
        Update update = Mockito.mock(Update.class);
        Message message5 = Mockito.mock(Message.class);
        Chat chat5 = Mockito.mock(Chat.class);
        User user5 = Mockito.mock(User.class);
        Mockito.doReturn(botUsername).when(user5).username();
        Mockito.doReturn(Chat.Type.group).when(chat5).type();
        Mockito.doReturn(UNKNOWN_CHAT_ID).when(chat5).id();
        Mockito.doReturn("Chat Title 5").when(chat5).title();
        Mockito.doReturn("Chat Username 5").when(chat5).username();
        Mockito.doReturn(null).when(message5).text();
        Mockito.doReturn(new User[] {user5}).when(message5).newChatMembers();
        Mockito.doReturn(chat5).when(message5).chat();
        Mockito.doReturn(5).when(update).updateId();
        Mockito.doReturn(message5).when(update).message();
        telegramBotMockBeanSetUp(List.of(update));

        String expectedMessage = "Чтобы активировать бота перейдите по ссылке  и войдите в аккаунт логиста\n\n" +
                "Для справки напишите /help";

        telegramBotUpdateHandler.processNextUpdates();

        Assertions.assertEquals(1, PROCESS_UPDATES_RESPONSE_MESSAGE_LOGGER.size());
        String actualMessage = PROCESS_UPDATES_RESPONSE_MESSAGE_LOGGER.get(0);

        String[] actualMessageParts = actualMessage.split(telegramBotAuthUrlTemplate);
        Assertions.assertEquals(2, actualMessageParts.length);
        Assertions.assertTrue(actualMessageParts[1].length() > telegramBotAuthTokenLength);
        actualMessageParts[1] = actualMessageParts[1].substring(telegramBotAuthTokenLength);
        actualMessage = actualMessageParts[0] + actualMessageParts[1];

        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void shouldProcessUpdatesHasCompanyStartCommand() {
        Update update = Mockito.mock(Update.class);
        Message message6 = Mockito.mock(Message.class);
        Chat chat6 = Mockito.mock(Chat.class);
        Mockito.doReturn(Chat.Type.Private).when(chat6).type();
        Mockito.doReturn(CHAT_ID_WITH_MAPPING).when(chat6).id();
        Mockito.doReturn("Chat Title 6").when(chat6).title();
        Mockito.doReturn("Chat Username 6").when(chat6).username();
        Mockito.doReturn("/start").when(message6).text();
        Mockito.doReturn(new User[0]).when(message6).newChatMembers();
        Mockito.doReturn(chat6).when(message6).chat();
        Mockito.doReturn(6).when(update).updateId();
        Mockito.doReturn(message6).when(update).message();
        telegramBotMockBeanSetUp(List.of(update));

        String expectedMessage = String.format("Бот уже активирован и привязан к %s\n\n" +
                                                    "Для справки напишите /help", company.getName());

        telegramBotUpdateHandler.processNextUpdates();

        Assertions.assertEquals(1, PROCESS_UPDATES_RESPONSE_MESSAGE_LOGGER.size());
        String actualMessage = PROCESS_UPDATES_RESPONSE_MESSAGE_LOGGER.get(0);

        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void shouldProcessUpdatesHasCompanyStatusCommand() {
        Update update = Mockito.mock(Update.class);
        Message message7 = Mockito.mock(Message.class);
        Chat chat7 = Mockito.mock(Chat.class);
        Mockito.doReturn(Chat.Type.Private).when(chat7).type();
        Mockito.doReturn(CHAT_ID_WITH_MAPPING).when(chat7).id();
        Mockito.doReturn("Chat Title 7").when(chat7).title();
        Mockito.doReturn("Chat Username 7").when(chat7).username();
        Mockito.doReturn("/status").when(message7).text();
        Mockito.doReturn(new User[0]).when(message7).newChatMembers();
        Mockito.doReturn(chat7).when(message7).chat();
        Mockito.doReturn(7).when(update).updateId();
        Mockito.doReturn(message7).when(update).message();
        telegramBotMockBeanSetUp(List.of(update));

        String expectedMessage = String.format(
                        "Добрый день, %s!\n" +
                        "1990-01-01 у вас 0 активных рейсов в Яндекс.Маркет\n"+
                        "Ожидает подтверждения - 0\n" +
                        "Не назначен водитель или ТС - 0\n" +
                        "Не использовали приложение - 0\n" +
                        "Выполняется - 0\n" +
                        "Завершено - 0", company.getName());

        telegramBotUpdateHandler.processNextUpdates();

        Assertions.assertEquals(1, PROCESS_UPDATES_RESPONSE_MESSAGE_LOGGER.size());
        String actualMessage = PROCESS_UPDATES_RESPONSE_MESSAGE_LOGGER.get(0);

        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void shouldProcessUpdatesHasCompanyHelpCommand() {
        Update update = Mockito.mock(Update.class);
        Message message8 = Mockito.mock(Message.class);
        Chat chat8 = Mockito.mock(Chat.class);
        Mockito.doReturn(Chat.Type.Private).when(chat8).type();
        Mockito.doReturn(CHAT_ID_WITH_MAPPING).when(chat8).id();
        Mockito.doReturn("Chat Title 8").when(chat8).title();
        Mockito.doReturn("Chat Username 8").when(chat8).username();
        Mockito.doReturn("/help").when(message8).text();
        Mockito.doReturn(new User[0]).when(message8).newChatMembers();
        Mockito.doReturn(chat8).when(message8).chat();
        Mockito.doReturn(8).when(update).updateId();
        Mockito.doReturn(message8).when(update).message();
        telegramBotMockBeanSetUp(List.of(update));

        String expectedMessage = "Регулярно в 8:00 бот будет присылать статус по текущим рейсам. " +
                "Кроме того справку по текущим рейсам можно получить по команде /status";
        telegramBotUpdateHandler.processNextUpdates();

        Assertions.assertEquals(1, PROCESS_UPDATES_RESPONSE_MESSAGE_LOGGER.size());
        String actualMessage = PROCESS_UPDATES_RESPONSE_MESSAGE_LOGGER.get(0);

        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void shouldProcessUpdatesHasCompanyUnknownCommand() {
        Update update = Mockito.mock(Update.class);
        Message message9 = Mockito.mock(Message.class);
        Chat chat9 = Mockito.mock(Chat.class);
        Mockito.doReturn(Chat.Type.Private).when(chat9).type();
        Mockito.doReturn(CHAT_ID_WITH_MAPPING).when(chat9).id();
        Mockito.doReturn("Chat Title 9").when(chat9).title();
        Mockito.doReturn("Chat Username 9").when(chat9).username();
        Mockito.doReturn("Unknown message 9").when(message9).text();
        Mockito.doReturn(new User[0]).when(message9).newChatMembers();
        Mockito.doReturn(chat9).when(message9).chat();
        Mockito.doReturn(9).when(update).updateId();
        Mockito.doReturn(message9).when(update).message();
        telegramBotMockBeanSetUp(List.of(update));

        telegramBotUpdateHandler.processNextUpdates();

        Assertions.assertEquals(0, PROCESS_UPDATES_RESPONSE_MESSAGE_LOGGER.size());
    }

    @Test
    void shouldProcessUpdatesHasCompanyDropCommand() {
        Update update = Mockito.mock(Update.class);
        Message message8 = Mockito.mock(Message.class);
        Chat chat8 = Mockito.mock(Chat.class);
        Mockito.doReturn(Chat.Type.Private).when(chat8).type();
        Mockito.doReturn(CHAT_ID_WITH_MAPPING).when(chat8).id();
        Mockito.doReturn("Chat Title 8").when(chat8).title();
        Mockito.doReturn("Chat Username 8").when(chat8).username();
        Mockito.doReturn("/drop").when(message8).text();
        Mockito.doReturn(new User[0]).when(message8).newChatMembers();
        Mockito.doReturn(chat8).when(message8).chat();
        Mockito.doReturn(8).when(update).updateId();
        Mockito.doReturn(message8).when(update).message();
        telegramBotMockBeanSetUp(List.of(update));

        String expectedMessage = "Компания успешно отвязана. Для привязки новой компании напишите /start";

        telegramBotUpdateHandler.processNextUpdates();

        Assertions.assertEquals(1, PROCESS_UPDATES_RESPONSE_MESSAGE_LOGGER.size());
        String actualMessage = PROCESS_UPDATES_RESPONSE_MESSAGE_LOGGER.get(0);

        Assertions.assertEquals(expectedMessage, actualMessage);

        TelegramChat chat = chatRepository.findById(CHAT_ID_WITH_MAPPING).orElseThrow();
        Assertions.assertNull(chat.getCompany());
    }


    @Test
    void shouldProcessUpdatesHasCompanySummaryCommand() {
        Update update = Mockito.mock(Update.class);
        Message message8 = Mockito.mock(Message.class);
        Chat chat8 = Mockito.mock(Chat.class);
        Mockito.doReturn(Chat.Type.Private).when(chat8).type();
        Mockito.doReturn(CHAT_ID_WITH_MAPPING).when(chat8).id();
        Mockito.doReturn("Chat Title 8").when(chat8).title();
        Mockito.doReturn("Chat Username 8").when(chat8).username();
        Mockito.doReturn("/evening_status").when(message8).text();
        Mockito.doReturn(new User[0]).when(message8).newChatMembers();
        Mockito.doReturn(chat8).when(message8).chat();
        Mockito.doReturn(8).when(update).updateId();
        Mockito.doReturn(message8).when(update).message();
        telegramBotMockBeanSetUp(List.of(update));

        String expectedMessage = String.format(
                 "Добрый вечер, %s!\n" +
                 "1990-01-01 у вас 0 активных рейсов в Яндекс.Маркет\n"+
                 "Ожидает подтверждения - 0\n" +
                 "Не назначен водитель или ТС - 0\n" +
                 "Не использовали приложение - 0\n" +
                 "Выполняется - 0\n" +
                 "Завершено - 0", company.getName());

        telegramBotUpdateHandler.processNextUpdates();

        Assertions.assertEquals(1, PROCESS_UPDATES_RESPONSE_MESSAGE_LOGGER.size());
        String actualMessage = PROCESS_UPDATES_RESPONSE_MESSAGE_LOGGER.get(0);

        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void shouldProcessUpdatesHasCompanyRatingCommand() {

        Update update = Mockito.mock(Update.class);
        Message message8 = Mockito.mock(Message.class);
        Chat chat8 = Mockito.mock(Chat.class);
        Mockito.doReturn(Chat.Type.Private).when(chat8).type();
        Mockito.doReturn(CHAT_ID_WITH_MAPPING).when(chat8).id();
        Mockito.doReturn("Chat Title 18").when(chat8).title();
        Mockito.doReturn("Chat Username 18").when(chat8).username();
        Mockito.doReturn("/rating").when(message8).text();
        Mockito.doReturn(new User[0]).when(message8).newChatMembers();
        Mockito.doReturn(chat8).when(message8).chat();
        Mockito.doReturn(81).when(update).updateId();
        Mockito.doReturn(message8).when(update).message();
        telegramBotMockBeanSetUp(List.of(update));

        ratingHelper.createRatings(company.getId());

        String expectedMessage =
                "Ваш рейтинг:\n" +
                "Подтверждено/отклонено: 48% ↑\n" +
                "Назначены водитель и ТС: 48% ↑\n" +
                "Водитель самостоятельно завершил рейс: 48% ↑";

        telegramBotUpdateHandler.processNextUpdates();

        Assertions.assertEquals(1, PROCESS_UPDATES_RESPONSE_MESSAGE_LOGGER.size());
        String actualMessage = PROCESS_UPDATES_RESPONSE_MESSAGE_LOGGER.get(0);

        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    private void telegramBotMockBeanSetUp(List<Update> updates) {
        company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);

        TelegramChat chat = new TelegramChat();
        chat.setId(CHAT_ID_WITH_MAPPING);
        chat.setTitle("Chat Title 2");
        chat.setCompany(company);
        chatRepository.saveAndFlush(chat);

        GetUpdatesResponse response = Mockito.mock(GetUpdatesResponse.class);

        Mockito.doReturn(updates).when(response).updates();
        Mockito.doNothing().when(telegramBot).execute(Mockito.any(GetUpdates.class), Mockito.any(Callback.class));
        SendResponse sendResponse = Mockito.mock(SendResponse.class);
        Mockito.doReturn(true).when(sendResponse).isOk();
        Mockito.doReturn(sendResponse).when(telegramBot).execute(Mockito.any(GetUpdates.class));
        Mockito.when(telegramBot.execute(Mockito.any())).thenReturn(response);

        Mockito.doReturn("Mocked response").when(response).description();
        Mockito.when(telegramBot.execute(Mockito.any())).then(invocation -> {
            BaseRequest request = invocation.getArgument(0);
            if (request instanceof SendMessage) {
                String text = (String) request.getParameters().get("text");
                PROCESS_UPDATES_RESPONSE_MESSAGE_LOGGER.add(text);
                return null;
            } else if (request instanceof GetUpdates) {
                return response;
            }
            return null;
        });

        Mockito.doNothing().when(telegramBot).setUpdatesListener(Mockito.any(), Mockito.any(GetUpdates.class));
        Mockito.doNothing().when(telegramBot).removeGetUpdatesListener();
    }
}
