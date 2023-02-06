package ru.yandex.market.tsum.telegrambot.bot;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import ru.yandex.market.tsum.clients.staff.StaffApiClient;
import ru.yandex.market.tsum.clients.staff.StaffPerson;
import ru.yandex.market.tsum.core.auth.TelegramChat;
import ru.yandex.market.tsum.core.auth.TelegramChatDao;
import ru.yandex.market.tsum.core.auth.TsumUserDao;
import ru.yandex.market.tsum.telegrambot.TestObjectLoader;
import ru.yandex.market.tsum.telegrambot.bot.handlers.commands.CommandHandler;
import ru.yandex.market.tsum.telegrambot.bot.handlers.commands.CommandHandlerStore;
import ru.yandex.market.tsum.telegrambot.bot.handlers.commands.general.HelpCommandHandler;
import ru.yandex.market.tsum.telegrambot.bot.handlers.events.EventHandler;
import ru.yandex.market.tsum.telegrambot.bot.handlers.events.EventHandlerStore;
import ru.yandex.market.tsum.telegrambot.bot.service.AuthenticationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BotTest {
    private static final String BOT_USERNAME = "test_bot";
    private static final String BOT_TOKEN = "TEST_TOKEN";
    private static final String UPDATE_OBJECT_FILE_NAME = "bot/UpdateObjectWithCommand.json";

    private static final String GROUP_TITLE = "тестовая группа 18";
    private static final String CONCURRENT_GROUP_TITLE_AFTER_RENAME = "тестовая группа 18 - новая";
    private static final long GROUP_ID = -418135728;
    private static final long SUPERGROUP_ID = -1001431592965L;
    private static final long CONCURRENT_GROUP_ID = -410089851;
    private static final String TSUM_URL = "https://tsum.yandex-team.ru/";

    private Update update;
    private StaffApiClient staffApiClient;
    private Bot bot;
    private HelpCommandHandler helpCommandHandler;
    private CommandHandlerStore commandHandlerStore;
    private TsumUserDao tsumUserDao;
    private TelegramChatDao telegramChatDao;
    private ArgumentCaptor<TelegramChat> telegramChatArgumentCaptor;
    private ArgumentCaptor<SendMessage> sendMessageArgumentCaptor;

    @Before
    public void setUpTestObjects() throws Exception {
        update = loadUpdate(UPDATE_OBJECT_FILE_NAME);
        helpCommandHandler = mock(HelpCommandHandler.class);
        ImmutableMap.Builder<String, CommandHandler> mapBuilder = ImmutableMap.builder();
        mapBuilder.put("help", helpCommandHandler);
        commandHandlerStore = new CommandHandlerStore(mapBuilder.build());
        tsumUserDao = mock(TsumUserDao.class, Mockito.RETURNS_DEEP_STUBS);
        staffApiClient = mock(StaffApiClient.class, Mockito.RETURNS_DEEP_STUBS);
        AuthenticationService authenticationService = new AuthenticationService(staffApiClient);
        telegramChatDao = mock(TelegramChatDao.class);
        bot = Mockito.spy(new Bot(
            BOT_USERNAME, BOT_TOKEN, commandHandlerStore, new EventHandlerStore(ImmutableList.<EventHandler>builder().build()),
            telegramChatDao, tsumUserDao, authenticationService, TSUM_URL));
        telegramChatArgumentCaptor = ArgumentCaptor.forClass(TelegramChat.class);
        sendMessageArgumentCaptor = ArgumentCaptor.forClass(SendMessage.class);

        //чтобы не пытаться обратиться в тестах к реальному API телеграма
        doReturn(null).when(bot).execute((BotApiMethod) any());

        doNothing().when(helpCommandHandler).handle(
            any(Bot.class),
            any(Message.class),
            any(StaffPerson.class));
    }

    @Test
    public void onUpdateReceived() throws Exception {
        StaffPerson person = setupStaffPerson("test_user", update.getMessage().getFrom().getUserName());

        bot.onUpdateReceived(update);

        verify(helpCommandHandler, times(1)).handle(any(Bot.class),
            eq(update.getMessage()), eq(person));
    }

    @Test
    public void onUpdateReceived_senderNotInStaff() throws Exception {
        List<StaffPerson> personList = Collections.emptyList();
        when(
            staffApiClient.getPersonsByAccountName(
                StaffPerson.AccountType.TELEGRAM, update.getMessage().getFrom().getUserName()
            )
        ).thenReturn(personList);

        bot.onUpdateReceived(update);

        verify(helpCommandHandler, times(0)).handle(eq(bot), eq(update.getMessage()),
            any(StaffPerson.class));
        verify(bot, times(1)).replyOnMessageAndSetReplyTo(update.getMessage(),
            "Мы знакомы? Я вас не нашел в своем STандартном справочнике.");
    }

    /*
     * Далее с помощью трёх тестов описан сценарий добавления бота в существующую группу и её превращения в супергруппу.
     */

    /**
     * Шаг 1 - добавление бота в существующую группу.
     */
    @Test
    public void addBotToGroup() throws TelegramApiException {
        setupStaffPersonSeminSerg();

        bot.onUpdateReceived(loadUpdate("bot/group_converts_to_supergroup/01_bot_added_to_group.json"));

        verify(telegramChatDao, times(1)).getChatByTitle(GROUP_TITLE);
        verify(telegramChatDao, times(1)).saveChat(telegramChatArgumentCaptor.capture());
        verifyNoMoreInteractions(telegramChatDao);
        TelegramChat telegramChat = telegramChatArgumentCaptor.getValue();
        assertThat(telegramChat).usingRecursiveComparison().isEqualTo(new TelegramChat(GROUP_ID, GROUP_TITLE));
        verify(bot, never()).execute((BotApiMethod) any());
    }

    /**
     * Шаг 2 - приход сообщения с полем migrate_to_chat_id от имени пользователя, который выполнил действие,
     * превратившее группу в супергруппу.
     */
    @Test
    public void migrateToChatId() throws TelegramApiException {
        setupStaffPersonSeminSerg();
        when(telegramChatDao.getChatByTitle(GROUP_TITLE)).thenReturn(new TelegramChat(GROUP_ID, GROUP_TITLE));

        bot.onUpdateReceived(loadUpdate("bot/group_converts_to_supergroup/02_migrate_to_chat_id.json"));

        verify(telegramChatDao, times(1)).getChatByTitle(GROUP_TITLE);
        verify(telegramChatDao, times(1)).replaceChat(eq(GROUP_ID), telegramChatArgumentCaptor.capture());
        verifyNoMoreInteractions(telegramChatDao);
        assertThat(telegramChatArgumentCaptor.getValue()).isEqualToComparingFieldByField(
            new TelegramChat(SUPERGROUP_ID, GROUP_TITLE));
        verify(bot, times(1)).execute(sendMessageArgumentCaptor.capture());
        SendMessage sendMessage = sendMessageArgumentCaptor.getValue();
        assertThat(sendMessage.getChatId()).isEqualTo(String.valueOf(SUPERGROUP_ID));
        assertThat(sendMessage.getText()).isEqualTo("Чат стал супергруппой. Чтобы получать уведомления, необходимо " +
            "обновить настройки нотификаций в ЦУМе. Для этого перейдите по следующей ссылке: " +
            "https://tsum.yandex-team.ru/api/projects/notifications/replaceChatId?from=-418135728&to=-1001431592965");
    }

    /**
     * Шаг 3 - приход сообщения с полем migrate_from_chat_id от имени GroupAnonymousBot, которое уже просто
     * игнорируется, поскольку необходимая обработка была выполнена с использованием сообщения с полем
     * migrate_to_chat_id на шаге 2.
     */
    @Test
    public void migrateFromChatId() throws TelegramApiException {
        setupStaffPersonSeminSerg();
        lenient().when(telegramChatDao.getChatByTitle(GROUP_TITLE)).thenReturn(new TelegramChat(SUPERGROUP_ID,
            GROUP_TITLE));

        bot.onUpdateReceived(loadUpdate("bot/group_converts_to_supergroup/03_migrate_from_chat_id.json"));

        verifyZeroInteractions(telegramChatDao);
        verify(bot, never()).execute((BotApiMethod) any());
    }

    /*
    Окончание сценария превращения группы в супергруппу.
     */

    /*
    Далее с помощью нескольких тестов описывается сценарий создания группы с именем, которое дублирует
    зарегистрированную ранее группу, а потом происходит переименование нового группы и её успешная регистрация.
     */

    /**
     * Шаг 1 - приглашаем бота в группу таким же именем, как у одной из зарегистрированных групп.
     */
    @Test
    public void inviteBotIntoGroupWithConcurrentName() throws TelegramApiException {
        setupStaffPersonSeminSerg();
        when(telegramChatDao.getChatByTitle(GROUP_TITLE)).thenReturn(new TelegramChat(SUPERGROUP_ID, GROUP_TITLE));

        bot.onUpdateReceived(loadUpdate("bot/group_with_concurrent_name/" +
            "01_invite_bot_into_group_with_concurrent_name.json"));

        verify(telegramChatDao, times(1)).getChatByTitle(GROUP_TITLE);
        //таким образом убеждаемся, что не было записи нового чата в базу ни одним из методов
        verifyNoMoreInteractions(telegramChatDao);
        verify(bot, times(1)).execute(sendMessageArgumentCaptor.capture());
        SendMessage sendMessage = sendMessageArgumentCaptor.getValue();
        assertThat(sendMessage.getChatId()).isEqualTo(String.valueOf(CONCURRENT_GROUP_ID));
        assertThat(sendMessage.getText()).isEqualTo("Не получилось добавить чат в базу, так как чат с таким именем " +
            "уже существует. Переименуйте свой чат, чтобы получать нотификации от ЦУМа");
    }

    /**
     * Шаг 2 - переименовываем группу с конкурирующим именем.
     */
    @Test
    public void renameGroupWithConcurrentName() throws TelegramApiException {
        setupStaffPersonSeminSerg();
        lenient().when(telegramChatDao.getChatByTitle(GROUP_TITLE)).thenReturn(
            new TelegramChat(SUPERGROUP_ID, GROUP_TITLE));

        bot.onUpdateReceived(loadUpdate("bot/group_with_concurrent_name/02_rename_group_with_concurrent_name.json"));

        verify(telegramChatDao, times(1)).getChatByTitle(CONCURRENT_GROUP_TITLE_AFTER_RENAME);
        verify(telegramChatDao, times(1)).saveChat(telegramChatArgumentCaptor.capture());
        verifyNoMoreInteractions(telegramChatDao);
        TelegramChat savedChat = telegramChatArgumentCaptor.getValue();
        assertThat(savedChat).isEqualToComparingFieldByField(
            new TelegramChat(CONCURRENT_GROUP_ID, CONCURRENT_GROUP_TITLE_AFTER_RENAME));
        verify(bot, never()).execute((BotApiMethod) any());
    }

    /*
    Окончание сценария переименования конкурирующей группы.
     */

    private void setupStaffPersonSeminSerg() {
        setupStaffPerson("semin-serg", "sergei_semin");
    }

    private StaffPerson setupStaffPerson(String staffName, String telegramName) {
        StaffPerson person = new StaffPerson(staffName, -1, null, null, null, null);
        List<StaffPerson> personList = Collections.singletonList(person);
        when(staffApiClient.getPersonsByAccountName(StaffPerson.AccountType.TELEGRAM, telegramName))
            .thenReturn(personList);
        return person;
    }

    private Update loadUpdate(String resourceName) {
        try {
            return TestObjectLoader.getTestUpdateObject(resourceName, Update.class,
                TestObjectLoader.SerializerType.JACKSON);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
