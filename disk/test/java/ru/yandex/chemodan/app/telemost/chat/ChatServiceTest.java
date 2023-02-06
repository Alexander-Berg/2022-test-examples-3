package ru.yandex.chemodan.app.telemost.chat;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.TelemostBaseContextTest;
import ru.yandex.chemodan.app.telemost.chat.model.Chat;
import ru.yandex.chemodan.app.telemost.chat.model.ChatHistory;
import ru.yandex.chemodan.app.telemost.repository.dao.ConferenceStateDao;
import ru.yandex.chemodan.app.telemost.repository.dao.ConferenceUserDao;
import ru.yandex.chemodan.app.telemost.repository.model.ConferenceStateDto;
import ru.yandex.chemodan.app.telemost.repository.model.UserRole;
import ru.yandex.chemodan.app.telemost.services.BroadcastService;
import ru.yandex.chemodan.app.telemost.services.ChatService;
import ru.yandex.chemodan.app.telemost.services.model.ChatType;
import ru.yandex.chemodan.app.telemost.services.model.Conference;
import ru.yandex.chemodan.app.telemost.services.model.PassportOrYaTeamUid;
import ru.yandex.chemodan.app.telemost.tools.ConferenceHelper;
import ru.yandex.chemodan.app.telemost.web.v2.model.BroadcastInitData;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

public class ChatServiceTest extends TelemostBaseContextTest {

    private static final boolean USE_STUB = true;

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ConferenceStateDao conferenceStateDao;

    @Autowired
    private ConferenceHelper conferenceHelper;

    @Autowired
    private ConferenceUserDao conferenceUserDao;

    @Autowired
    private BroadcastService broadcastService;

    private final UUID anonymousUser1 = UUID.fromString("5e9d41fc-aebf-802c-e2ab-4b5fb4026542");
    private final UUID anonymousUser2 = UUID.fromString("60ac597f-f889-831c-4e3b-b3f43f260cbb");
    private final PassportOrYaTeamUid TEST_USER = PassportOrYaTeamUid.passportUid(new PassportUid(11144));
    private String SYSTEM_MESSAGE = "Чат сохранится в Яндекс.Мессенджере, если вы авторизованы на Яндексе.";

    @Before
    public void setUp() {
        if (USE_STUB) {
            ((ChatClientStub) chatClient).reset();
        }
        userService.addUserIfNotExists(TEST_USER);
    }

    @Test
    public void testCreateChatWithAnonymousAndWithOwner() {
        // Create conference
        Conference conference = conferenceHelper.createConference(TEST_OWNER);

        // Create chat
        Option<Chat> chatO = chatService.createChat(conference.getConferenceId(), ChatType.CONFERENCE,
                anonymousUser1, Option.empty(), Option.empty());

        Assert.isTrue(chatO.isPresent());
        Assert.equals(1, chatO.get().getMembers().size());
        Assert.equals(1, chatO.get().getSubscribers().size());

        // Join anonymous
        chatO = chatService.joinToChatIfExisting(conference.getConferenceId(), ChatType.CONFERENCE, anonymousUser2,
                Option.empty(),
                Option.empty());

        Assert.isTrue(chatO.isPresent());
        Assert.equals(chatO.get().getMembers().size(), 1);
        Assert.equals(chatO.get().getSubscribers().size(), 2);
    }

    @Test
    public void testCreateChatWithNoConferenceInCalendar() {
        // Create conference
        Conference conference = conferenceHelper.toConference(conferenceHelper.conferenceDefaultBuilder()
                .eventId(Option.of("some_non_existing"))
        );

        // Create chat
        Option<Chat> chatO = chatService.createChat(conference.getConferenceId(), ChatType.CONFERENCE,
                anonymousUser1, Option.empty(), Option.empty());
        //should not fail with ConferenceNotFoundTelemostException from ru.yandex.chemodan.app.telemost.services
        // .CalendarService.getCalendarEventData
        Assert.isTrue(chatO.isPresent());

        UUID chatUser = chatClient.getUser(TEST_OWNER.asString()).get();

        //assert this one doesn't fail with ConferenceNotFoundTelemostException
        //and that history is not available in this case
        Option<ChatHistory> chatHistoryO = chatService.getChatHistory(conference.getConferenceId(), ChatType.CONFERENCE,
                chatUser, Option.of(TEST_OWNER), Option.empty(), Option.empty());
        Assert.equals(Option.empty(), chatHistoryO);

        //assert this one doesn't fail with ConferenceNotFoundTelemostException
        chatService.joinToChatIfExisting(conference.getConferenceId(), ChatType.CONFERENCE, anonymousUser2,
                Option.empty(), Option.empty());
    }

    @Test
    public void addAllAdminToChatWhenCreate() {
        Conference conference = conferenceHelper.createConference(TEST_OWNER);
        conferenceHelper.connect(conference, TEST_USER);
        conferenceUserDao.upsert(conference.getDbId(), TEST_USER, UserRole.ADMIN);
        Option<UUID> owner = chatClient.getUser(TEST_OWNER.asString());
        Option<Chat> chatO = chatService.createChat(conference.getConferenceId(), ChatType.CONFERENCE, owner.get(),
                Option.of(TEST_OWNER), Option.empty());
        Assert.isTrue(chatO.isPresent());
        Assert.equals(2, chatO.get().getMembers().size());
        Assert.equals(0, chatO.get().getSubscribers().size());
    }

    @Test
    public void doNotAddNonAdminMemberToChatWhenCreate() {
        Conference conference = conferenceHelper.createConference(TEST_OWNER);
        conferenceHelper.connect(conference, TEST_USER);
        Option<UUID> owner = chatClient.getUser(TEST_OWNER.asString());
        Option<Chat> chatO = chatService.createChat(conference.getConferenceId(), ChatType.CONFERENCE, owner.get(),
                Option.of(TEST_OWNER), Option.empty());

        Assert.isTrue(chatO.isPresent());
        Assert.equals(1, chatO.get().getMembers().size());
        Assert.equals(0, chatO.get().getSubscribers().size());
    }

    @Test
    public void testCreateChatWithAnonymousAndWithoutOwner() {
        // Create conference
        @SuppressWarnings("deprecation")
        Conference conference = conferenceHelper.createConference();

        // Create chat
        Option<Chat> chatO = chatService.createChat(conference.getConferenceId(), ChatType.CONFERENCE, anonymousUser1,
                Option.empty(), Option.empty());

        Assert.isTrue(chatO.isPresent());
        Assert.equals(chatO.get().getMembers().size(), 0);
        Assert.equals(chatO.get().getSubscribers().size(), 1);

        // Join anonymous
        chatO = chatService.joinToChatIfExisting(conference.getConferenceId(), ChatType.CONFERENCE, anonymousUser2,
                Option.empty(), Option.empty());

        Assert.isTrue(chatO.isPresent());
        Assert.equals(chatO.get().getMembers().size(), 0);
        Assert.equals(chatO.get().getSubscribers().size(), 2);
    }

    @Test
    public void testConferenceStateDto() {
        // Create conference
        Conference conference = conferenceHelper.createConference(TEST_OWNER);

        // Create chat
        Option<Chat> chatO = chatService.createChat(conference.getConferenceId(), ChatType.CONFERENCE,
                chatClient.getUser(TEST_OWNER.asString()).get(), Option.of(TEST_OWNER), Option.empty());

        Assert.isTrue(chatO.isPresent());

        Option<ConferenceStateDto> conferenceStateO = conferenceStateDao.findState(conference.getDbId());

        Assert.isTrue(conferenceStateO.isPresent());
        Assert.equals(conferenceStateO.get().getChatPath().get(), chatO.get().getChatPath());
    }

    @Test
    public void testGetExistingChat() {
        // Create conference
        Conference conference = conferenceHelper.createConference(TEST_OWNER);

        // Create chat
        Option<Chat> chatO = chatService.createChat(conference.getConferenceId(), ChatType.CONFERENCE,
                chatClient.getUser(TEST_OWNER.asString()).get(), Option.of(TEST_OWNER), Option.empty());

        Assert.isTrue(chatO.isPresent());

        // Get existing chat
        Option<Chat> chat1 = chatService.createChat(conference.getConferenceId(), ChatType.CONFERENCE,
                chatClient.getUser(TEST_OWNER.asString()).get(), Option.of(TEST_OWNER), Option.empty());

        Assert.isTrue(chat1.isPresent());
    }

    @Test
    public void testGetChatHistory() {
        // Create conference
        Conference conference = conferenceHelper.createConference(TEST_OWNER);

        // Create chat
        UUID chatUser = chatClient.getUser(TEST_OWNER.asString()).get();
        Option<Chat> chatO = chatService.createChat(conference.getConferenceId(), ChatType.CONFERENCE,
                chatUser, Option.of(TEST_OWNER), Option.empty());

        Assert.isTrue(chatO.isPresent());

        // Get chat history
        Option<ChatHistory> chatHistoryO = chatService.getChatHistory(conference.getConferenceId(), ChatType.CONFERENCE,
                chatUser, Option.of(TEST_OWNER), Option.empty(), Option.empty());
        Assert.isTrue(chatHistoryO.isPresent());
        Assert.assertHasSize(1, chatHistoryO.get().getItems());
        Assert.equals(SYSTEM_MESSAGE, chatHistoryO.get().getItems().get(0).getMessage());
    }

    @Test
    public void testCreateBroadcastChatIfNeed() {
        Conference conference = conferenceHelper.createConference(TEST_OWNER);
        Option<String> caption = Option.of("Caption");
        Option<String> description = Option.of("Description");

        broadcastService.createBroadcast(conference.getDbId(), TEST_OWNER.asString(),
                new BroadcastInitData(caption, description));
        Option<PassportOrYaTeamUid> uid = Option.of(PassportOrYaTeamUid.parseUid("666000"));
        chatService.tryCreateBroadcastChatIfNeed(conference.getConferenceDto(), uid, Option.of("fdsa"));
    }
}
