package ru.yandex.market.ocrm.module.order.arbitrage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.transaction.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jdk.jfr.Description;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import ru.yandex.market.b2bcrm.module.account.Shop;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.entity.test.assertions.EntityCollectionAssert;
import ru.yandex.market.jmf.http.NotFoundException;
import ru.yandex.market.jmf.module.chat.Chat;
import ru.yandex.market.jmf.module.chat.ChatsService;
import ru.yandex.market.jmf.module.chat.CreateChatResult;
import ru.yandex.market.jmf.module.chat.FeedbackClient;
import ru.yandex.market.jmf.module.chat.MessengerClient;
import ru.yandex.market.jmf.module.chat.controller.model.ChatMessage;
import ru.yandex.market.jmf.module.chat.controller.model.ChatMessageRequest;
import ru.yandex.market.jmf.module.chat.controller.model.CustomFrom;
import ru.yandex.market.jmf.module.chat.controller.model.FileInfo;
import ru.yandex.market.jmf.module.chat.controller.model.Gallery;
import ru.yandex.market.jmf.module.chat.controller.model.GalleryItem;
import ru.yandex.market.jmf.module.chat.controller.model.ImageFile;
import ru.yandex.market.jmf.module.chat.controller.model.InlineKeyboardItem;
import ru.yandex.market.jmf.module.chat.controller.model.PlainMessage;
import ru.yandex.market.jmf.module.chat.controller.model.PlainMessageText;
import ru.yandex.market.jmf.module.chat.controller.model.ServerMessageInfo;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.jmf.utils.UrlCreationService;
import ru.yandex.market.ocrm.module.order.TicketFirstLine;
import ru.yandex.market.ocrm.module.order.arbitrage.controller.ConsultationController;
import ru.yandex.market.ocrm.module.order.arbitrage.controller.models.ChatSettings;
import ru.yandex.market.ocrm.module.order.arbitrage.controller.models.ConsultationStatus;
import ru.yandex.market.ocrm.module.order.arbitrage.controller.models.ConversationStatus;
import ru.yandex.market.ocrm.module.order.arbitrage.controller.models.GetEntityConsultationRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(classes = ModuleArbitrageTestConfiguration.class)
public class ConsultationTest {
    private static final String CLIENT_CHAT_ID = "1234";
    private static final String PARTNER_CHAT_ID = "4321";
    private static final String CHAT_USER_ID = "userId";
    private static final String INVITE_HASH = "hash";
    private static final String CHAT_CODE = "beruOrderConsultations";
    private static final String SUMMON_MESSAGE = "Нужна помощь арбитра";
    private static final String SUMMON_YES_MESSAGE = "Саммон";
    private static final String SUMMON_NO_MESSAGE = "Неть";
    private static final String REQUEST_MESSAGE = "Передать ваш вопрос службе";

    private final ConsultationService consultationService;
    private final ConsultationController consultationController;
    private final FeedbackClient feedbackClient;
    private final BcpService bcpService;
    private final TicketTestUtils ticketTestUtils;
    private final EntityStorageService entityStorageService;
    private final UrlCreationService urlCreationService;
    private final ConfigurationService configurationService;
    private final ChatsService chatsService;
    private final MessengerClient messengerClient;

    private Chat chat;
    private TicketFirstLine ticketFirstLine;


    @Autowired
    public ConsultationTest(
            ConsultationService consultationService,
            ConsultationController consultationController,
            FeedbackClient feedbackClient,
            BcpService bcpService,
            TicketTestUtils ticketTestUtils,
            EntityStorageService entityStorageService,
            UrlCreationService urlCreationService,
            ConfigurationService configurationService,
            ChatsService chatsService,
            MessengerClient messengerClient
    ) {
        this.consultationService = consultationService;
        this.consultationController = consultationController;
        this.feedbackClient = feedbackClient;
        this.bcpService = bcpService;
        this.ticketTestUtils = ticketTestUtils;
        this.entityStorageService = entityStorageService;
        this.urlCreationService = urlCreationService;
        this.configurationService = configurationService;
        this.chatsService = chatsService;
        this.messengerClient = messengerClient;
    }

    @BeforeEach
    void setUp() throws IOException {
        configurationService.setValue("useNewConsultationApi", true);

        Service service = ticketTestUtils.createService();
        ticketFirstLine = ticketTestUtils.createTicket(TicketFirstLine.FQN, Maps.of(
                Ticket.CHANNEL, "mail"
        ));
        chat = bcpService.create(Chat.FQN, Map.of(
                Chat.CODE, CHAT_CODE,
                Chat.CHAT_ID, "chatId",
                Chat.SERVICE, service,
                Chat.TITLE, "chat",
                Chat.CHAT_USER_ID, CHAT_USER_ID
        ));
        var properties = new HashMap<String, Object>() {{
            put(ChatConsultationSetting.CHAT, chat);
            put(ChatConsultationSetting.RELATED_METACLASS, ticketFirstLine.getMetaclass());
            put(ChatConsultationSetting.CLIENT_CHAT_TITLE, "Test Client title");
            put(ChatConsultationSetting.CLIENT_CHAT_DESCRIPTION, "Test Client description");
            put(ChatConsultationSetting.PARTNER_CHAT_TITLE, "Test Partner title");
            put(ChatConsultationSetting.PARTNER_CHAT_DESCRIPTION, "Test Partner description");
            put(ChatConsultationSetting.PRIVATE_CLIENT_CHAT, false);
            put(ChatConsultationSetting.PRIVATE_PARTNER_CHAT, false);
            put(ChatConsultationSetting.SERVICE, service);
            put(ChatConsultationSetting.SUMMON_SUPPORT_TEXT, SUMMON_MESSAGE);
            put(ChatConsultationSetting.SUMMON_SUPPORT_YES_TEXT, SUMMON_YES_MESSAGE);
            put(ChatConsultationSetting.SUMMON_SUPPORT_NO_TEXT, SUMMON_NO_MESSAGE);
            put(ChatConsultationSetting.ARBITRAGE_REQUEST_TEXT, REQUEST_MESSAGE);
            put(ChatConsultationSetting.TICKET_METACLASS, "ticket$testConsultationChat");
            put(ChatConsultationSetting.CLIENT_TITLE, "Клиент-тест");
            put(ChatConsultationSetting.PARTNER_TITLE, "Кто-то");
            put(ChatConsultationSetting.PARTNER_TEXT_ABOUT_START_ARBITRAGE, "tst1");
            put(ChatConsultationSetting.CLIENT_TEXT_ABOUT_START_ARBITRAGE, "tst2");
            put(ChatConsultationSetting.CLIENT_AVATAR, "/market-lilucrm/1686480/ba5d2de9-08d4-4772-ad1c-d2512ba809b9");
            put(ChatConsultationSetting.PARTNER_AVATAR, "/market-lilucrm/1683509/74fa2f4d-1240-45e3-8060-58fa435713f1");
        }};
        bcpService.create(ChatConsultationSetting.FQN, properties);

        when(feedbackClient.createPublicGroupChat(any(Chat.class), any(String.class), any(String.class)))
                .thenReturn(
                        new CreateChatResult(CLIENT_CHAT_ID, INVITE_HASH),
                        new CreateChatResult(PARTNER_CHAT_ID, INVITE_HASH)
                );

        var inputStream = new ByteArrayInputStream("YEAH!".getBytes(StandardCharsets.UTF_8));
        var urlConnection = mock(URLConnection.class);
        when(urlConnection.getContentType()).thenReturn(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        when(urlConnection.getInputStream()).thenReturn(inputStream);

        var url = mock(URL.class);
        when(url.getPath()).thenReturn("/");
        when(url.openConnection()).thenReturn(urlConnection);
        when(url.openStream()).thenReturn(inputStream);

        when(urlCreationService.create(any(String.class))).thenReturn(url);
    }

    @AfterEach
    public void tearDown() {
        reset(feedbackClient, messengerClient);
    }

    @Test
    @Transactional
    @Description("""
                Проверяем, что если дернуть ручку создания или получения консультации, если консультация уже существует, то
                получим существующую консультацию
            """)
    public void testThatPostToArbitrageCreateOrGetReturnsExistingConsultation() throws InterruptedException,
            TimeoutException, IOException {
        String json = "{ \"field11\" : \"value1\" } ";
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(json);
        var request = new GetEntityConsultationRequest(jsonNode, CHAT_CODE, null, null);

        consultationController.getOrCreateConsultation(ticketFirstLine.getGid(), request);
        consultationController.getOrCreateConsultation(ticketFirstLine.getGid(), request);

        EntityCollectionAssert.assertThat(entityStorageService.list(
                        Query.of(Consultation.FQN)
                                .withFilters(Filters.eq(Consultation.RELATED_ENTITY, ticketFirstLine.getGid()))
                ))
                .size()
                .isEqualTo(1);
    }

    @Test
    @Transactional
    @Description("Проверяем, что созданные консультации доступны в ручке получения")
    public void testThatCreatedConsultationIsVisible() {
        var request = new GetEntityConsultationRequest(JsonNodeFactory.instance.objectNode(), CHAT_CODE, null, null);

        consultationService.createConsultation(ticketFirstLine, chat, request);

        var partnerConsultation = consultationController.getEntityConsultation(ticketFirstLine.getGid());
        Assertions.assertEquals(ConversationStatus.NONE, partnerConsultation.getConversationStatus());
        Assertions.assertEquals(ConsultationStatus.DIRECT_CONVERSATION, partnerConsultation.getStatus());
    }

    @Test
    @Transactional
    @Description("""
            Проверяем, что дернув ручку создания или получения консультации,
            если консультации нет, получаем вновь созданную консультацию
            """)
    public void testThatPostToArbitrageCreateOrGetCreatesNewConsultation() throws InterruptedException,
            TimeoutException {
        var request = new GetEntityConsultationRequest(JsonNodeFactory.instance.objectNode(),
                CHAT_CODE, null, null);
        var consultation = consultationController.getOrCreateConsultation(ticketFirstLine.getGid(), request);

        Assertions.assertEquals(ticketFirstLine.getGid(), consultation.getRelatedEntityGid());
        Assertions.assertEquals(ConversationStatus.NONE, consultation.getConversationStatus());
        Assertions.assertEquals(ConsultationStatus.DIRECT_CONVERSATION, consultation.getConsultationStatus());
        Assertions.assertEquals(CLIENT_CHAT_ID, consultation.getChatId());
    }

    @Test
    @Transactional
    @Description("""
            Проверяем, что пользователи чатов не имеют прав на редактирование списка участников чата, на то, чтобы выйти
            из чата. А у партнера еще и нет права писать в чат до первого сообщения клиента
            """)
    public void testThatUsersCouldNotInviteOtherUsersAndCouldNotLeaveFromCreatedChats() {
        var request = new GetEntityConsultationRequest(JsonNodeFactory.instance.objectNode(),
                CHAT_CODE, null, null);
        var consultation = consultationService.createConsultation(ticketFirstLine, chat, request);

        var clientConversationMemberRights = consultation.getClientConversation().getMetadata().getMemberRights();
        Assertions.assertEquals(false, clientConversationMemberRights.getLeave());
        Assertions.assertEquals(false, clientConversationMemberRights.getAddUsers());
        Assertions.assertEquals(false, clientConversationMemberRights.getRemoveUsers());

        var partnerConversationMemberRights = consultation.getPartnerConversation().getMetadata().getMemberRights();
        Assertions.assertEquals(false, partnerConversationMemberRights.getLeave());
        Assertions.assertEquals(false, partnerConversationMemberRights.getAddUsers());
        Assertions.assertEquals(false, partnerConversationMemberRights.getRemoveUsers());
        Assertions.assertEquals(false, partnerConversationMemberRights.getWrite());

        var clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(CLIENT_CHAT_ID, payloadId(),
                        new PlainMessageText("test text", null)),
                new CustomFrom("", "")
        );
        chatsService.receiveMessage(chat, clientMessageRequest);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);
        consultation = consultationService.getConsultation(ticketFirstLine);

        clientConversationMemberRights = consultation.getClientConversation().getMetadata().getMemberRights();
        Assertions.assertEquals(false, clientConversationMemberRights.getLeave());
        Assertions.assertEquals(false, clientConversationMemberRights.getAddUsers());
        Assertions.assertEquals(false, clientConversationMemberRights.getRemoveUsers());

        partnerConversationMemberRights = consultation.getPartnerConversation().getMetadata().getMemberRights();
        Assertions.assertEquals(false, partnerConversationMemberRights.getLeave());
        Assertions.assertEquals(false, partnerConversationMemberRights.getAddUsers());
        Assertions.assertEquals(false, partnerConversationMemberRights.getRemoveUsers());
        Assertions.assertEquals(true, partnerConversationMemberRights.getWrite());
    }

    @Transactional
    @ParameterizedTest(name = "{0}")
    @MethodSource("filesSharingParameters")
    @Description("Отправка вложений при пересылке сообщений")
    public void testThatFilesAreSharedWhileResending(String testName, ChatMessage chatMessage) {
        var request = new GetEntityConsultationRequest(JsonNodeFactory.instance.objectNode(), CHAT_CODE, null, null);

        consultationService.createConsultation(ticketFirstLine, chat, request);

        var clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                chatMessage,
                new CustomFrom("", "")
        );
        chatsService.receiveMessage(chat, clientMessageRequest);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        verify(messengerClient, times(1))
                .shareFile("123", CHAT_USER_ID, CLIENT_CHAT_ID, PARTNER_CHAT_ID);
    }

    @Test
    @Transactional
    @Description("Проверяем, что сообщения пересылаются во время прямой переписки")
    public void testThatMessagesAreResendWhileConsultationInDirectConversation() {
        var request = new GetEntityConsultationRequest(JsonNodeFactory.instance.objectNode(), CHAT_CODE, null, null);

        consultationService.createConsultation(ticketFirstLine, chat, request);

        var messageText = "test text";
        var clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(CLIENT_CHAT_ID, payloadId(),
                        new PlainMessageText(messageText, null)),
                new CustomFrom("", "")
        );
        chatsService.receiveMessage(chat, clientMessageRequest);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        var chatMessageCaptor = ArgumentCaptor.forClass(ChatMessageRequest.class);
        verify(feedbackClient, times(1)).pushMessage(eq(chat), chatMessageCaptor.capture());

        var chatMessageRequest = chatMessageCaptor.getValue();
        var chatMessage = chatMessageRequest.getChatMessage();
        Assertions.assertEquals(PlainMessage.class, chatMessage.getClass());

        PlainMessage plainMessage = (PlainMessage) chatMessage;
        Assertions.assertEquals(PARTNER_CHAT_ID, plainMessage.getChatId());
        Assertions.assertEquals(messageText, plainMessage.getText().getMessageText());
        Assertions.assertEquals("Клиент-тест", chatMessageRequest.getCustomFrom().getDisplayName());
    }

    @Test
    @Transactional
    @Description("Проверяем, что сообщения не пересылаются, когда арбитраж активен")
    public void testThatMessagesAreNotResendWhileConsultationInArbitrage() {
        var request = new GetEntityConsultationRequest(JsonNodeFactory.instance.objectNode(), CHAT_CODE, null, null);

        var consultation = consultationService.createConsultation(ticketFirstLine, chat, request);

        var clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(CLIENT_CHAT_ID, payloadId(),
                        new PlainMessageText("test text", null)),
                new CustomFrom("", "")
        );
        chatsService.receiveMessage(chat, clientMessageRequest);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        verify(feedbackClient, times(1)).pushMessage(any(Chat.class), any(ChatMessageRequest.class));
        clearInvocations(feedbackClient);

        bcpService.edit(consultation, Map.of(
                Consultation.STATUS, Consultation.Statuses.ARBITRAGE_REQUESTED
        ));

        bcpService.edit(consultation, Map.of(
                Consultation.STATUS, Consultation.Statuses.ARBITRAGE
        ));


        clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(CLIENT_CHAT_ID, payloadId(),
                        new PlainMessageText("test text 2", null)),
                new CustomFrom("", "")
        );
        chatsService.receiveMessage(chat, clientMessageRequest);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        verify(feedbackClient, times(0)).pushMessage(any(Chat.class), any(ChatMessageRequest.class));
    }

    @Test
    @Transactional
    public void testThatArbitrageWasStartedIsStoredCorrectly() {
        var request = new GetEntityConsultationRequest(JsonNodeFactory.instance.objectNode(), CHAT_CODE, null, null);

        var consultation = consultationService.createConsultation(ticketFirstLine, chat, request);

        Assertions.assertFalse(consultation.getArbitrageWasStarted());

        var clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(CLIENT_CHAT_ID, payloadId(),
                        new PlainMessageText("test text", null)),
                new CustomFrom("", "")
        );
        chatsService.receiveMessage(chat, clientMessageRequest);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        verify(feedbackClient, times(1)).pushMessage(any(Chat.class), any(ChatMessageRequest.class));
        clearInvocations(feedbackClient);

        bcpService.edit(consultation, Map.of(
                Consultation.STATUS, Consultation.Statuses.ARBITRAGE_REQUESTED
        ));

        bcpService.edit(consultation, Map.of(
                Consultation.STATUS, Consultation.Statuses.ARBITRAGE
        ));

        consultation = consultationService.getConsultation(ticketFirstLine);

        Assertions.assertTrue(consultation.getArbitrageWasStarted());

        bcpService.edit(consultation, Map.of(
                Consultation.STATUS, Consultation.Statuses.FINISHED
        ));

        consultation = consultationService.getConsultation(ticketFirstLine);

        Assertions.assertTrue(consultation.getArbitrageWasStarted());
    }

    @Test
    @Transactional
    @Description("""
            Проверяем, что сообщения клиента не пересылаются во время запроса арбитража,
            а сообщения партнера пересылаются
            """)
    public void testThatMessagesFromPartnerAreResendButClientMessagesAreNotWhileConsultationInArbitrageRequested() {
        var request = new GetEntityConsultationRequest(JsonNodeFactory.instance.objectNode(), CHAT_CODE, null, null);

        var consultation = consultationService.createConsultation(ticketFirstLine, chat, request);

        var clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(CLIENT_CHAT_ID, payloadId(),
                        new PlainMessageText("test text", null)),
                new CustomFrom("", "")
        );
        chatsService.receiveMessage(chat, clientMessageRequest);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        verify(feedbackClient, times(1)).pushMessage(any(Chat.class), any(ChatMessageRequest.class));
        clearInvocations(feedbackClient);

        bcpService.edit(consultation, Map.of(
                Consultation.STATUS, Consultation.Statuses.ARBITRAGE_REQUESTED
        ));

        clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(CLIENT_CHAT_ID, payloadId(),
                        new PlainMessageText("test text 2", null)),
                new CustomFrom("", "")
        );
        chatsService.receiveMessage(chat, clientMessageRequest);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        // Сообщение клиенту: согласись или отмени арбитраж
        var chatMessageCaptor = ArgumentCaptor.forClass(ChatMessageRequest.class);
        verify(feedbackClient, times(1)).pushMessage(any(Chat.class), chatMessageCaptor.capture());

        var chatMessageRequest = chatMessageCaptor.getValue();
        var chatMessage = chatMessageRequest.getChatMessage();
        Assertions.assertEquals(PlainMessage.class, chatMessage.getClass());

        PlainMessage plainMessage = (PlainMessage) chatMessage;
        Assertions.assertEquals(CLIENT_CHAT_ID, plainMessage.getChatId());
        clearInvocations(feedbackClient);

        var partnerMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(PARTNER_CHAT_ID, payloadId(),
                        new PlainMessageText("test text 3", null)),
                new CustomFrom("", "")
        );
        chatsService.receiveMessage(chat, partnerMessageRequest);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        chatMessageCaptor = ArgumentCaptor.forClass(ChatMessageRequest.class);
        verify(feedbackClient, times(1)).pushMessage(any(Chat.class), chatMessageCaptor.capture());

        chatMessageRequest = chatMessageCaptor.getValue();
        chatMessage = chatMessageRequest.getChatMessage();
        Assertions.assertEquals(PlainMessage.class, chatMessage.getClass());

        plainMessage = (PlainMessage) chatMessage;
        Assertions.assertEquals(CLIENT_CHAT_ID, plainMessage.getChatId());
    }

    @Test
    @Transactional
    @Description("""
            Проверяем, что атачи от партнера пересылаются во время запроса арбитража,
            а сообщения клиента не пересылаются
            """)
    public void testThatAttachmentFromPartnerAreResendButClientMessagesAreNotWhileConsultationInArbitrageRequested() {
        var request = new GetEntityConsultationRequest(JsonNodeFactory.instance.objectNode(), CHAT_CODE, null, null);

        var consultation = consultationService.createConsultation(ticketFirstLine, chat, request);

        var clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(CLIENT_CHAT_ID, payloadId(), new PlainMessageText("test text", null)),
                new CustomFrom("", "")
        );
        chatsService.receiveMessage(chat, clientMessageRequest);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        verify(feedbackClient, times(1)).pushMessage(any(Chat.class), any(ChatMessageRequest.class));
        clearInvocations(feedbackClient);

        bcpService.edit(consultation, Map.of(
                Consultation.STATUS, Consultation.Statuses.ARBITRAGE_REQUESTED
        ));

        var image = new ImageFile(
                new FileInfo(123, "123", "name", 123, CLIENT_CHAT_ID),
                40,
                50,
                "https://fasd.as"
        );
        var gallery = new Gallery(List.of(new GalleryItem(image)), "gallery");
        var message = PlainMessage.forGallery(CLIENT_CHAT_ID, payloadId(), gallery);

        clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                message,
                new CustomFrom("", "")
        );
        chatsService.receiveMessage(chat, clientMessageRequest);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        // Сообщение клиенту, мол согласись или отмени арбитраж
        var chatMessageCaptor = ArgumentCaptor.forClass(ChatMessageRequest.class);
        verify(feedbackClient, times(1)).pushMessage(any(Chat.class), chatMessageCaptor.capture());

        var chatMessageRequest = chatMessageCaptor.getValue();
        var chatMessage = chatMessageRequest.getChatMessage();
        Assertions.assertEquals(PlainMessage.class, chatMessage.getClass());

        PlainMessage plainMessage = (PlainMessage) chatMessage;
        Assertions.assertEquals(CLIENT_CHAT_ID, plainMessage.getChatId());
        clearInvocations(feedbackClient);

        var partnerMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forGallery(PARTNER_CHAT_ID, payloadId(), gallery),
                new CustomFrom("", "")
        );
        chatsService.receiveMessage(chat, partnerMessageRequest);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        chatMessageCaptor = ArgumentCaptor.forClass(ChatMessageRequest.class);
        verify(feedbackClient, times(1)).pushMessage(any(Chat.class), chatMessageCaptor.capture());

        chatMessageRequest = chatMessageCaptor.getValue();
        chatMessage = chatMessageRequest.getChatMessage();
        Assertions.assertEquals(PlainMessage.class, chatMessage.getClass());

        plainMessage = (PlainMessage) chatMessage;
        Assertions.assertEquals(CLIENT_CHAT_ID, plainMessage.getChatId());

        Assertions.assertNotNull(plainMessage.getGallery());
    }

    @Test
    @Transactional
    @Description("Проверяем, что не можем получить консультацию по несуществующей сущности")
    public void testCantGetConsultationForNonExistEntity() {
        Exception exception = assertThrows(NotFoundException.class,
                () -> consultationController.getEntityConsultation("ticket$firstLine@444"));

        String expectedMessage = "There is no such entity: ticket$firstLine@444 not found";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    @Transactional
    @Description("Проверяем, что при отсутствии настройки будет брошен exception")
    public void testCantCreateConsultationWithoutSetting() {
        var request = new GetEntityConsultationRequest(JsonNodeFactory.instance.objectNode(), CHAT_CODE, null, null);

        var shop = bcpService.create(Shop.FQN, Map.of(
                Shop.SHOP_ID, 111,
                Shop.TITLE, "test shop"
        ));

        Exception exception = assertThrows(AccessDeniedException.class,
                () -> consultationService.createConsultation(shop, chat, request));

        String expectedMessage = "You can't create consultation for this entity";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    @Transactional
    @Description("Проверяем получение действительно существующих консультаций")
    public void testGetConsultationsForEntities() throws InterruptedException, TimeoutException {
        TicketFirstLine ticketFirstLine2 = ticketTestUtils.createTicket(TicketFirstLine.FQN, Maps.of(
                Ticket.CHANNEL, "mail"
        ));

        var request = new GetEntityConsultationRequest(JsonNodeFactory.instance.objectNode(), CHAT_CODE, null, null);

        consultationController.getOrCreateConsultation(ticketFirstLine.getGid(), request);

        //имитируем другой чат
        when(feedbackClient.createPublicGroupChat(any(Chat.class), any(String.class), any(String.class)))
                .thenReturn(
                        new CreateChatResult("2345", INVITE_HASH),
                        new CreateChatResult("5432", INVITE_HASH)
                );

        consultationController.getOrCreateConsultation(ticketFirstLine2.getGid(), request);

        var result = consultationController.getConsultations(List.of(ticketFirstLine.getGid(),
                ticketFirstLine2.getGid()));
        assertEquals(2, result.size());
    }

    @Test
    @Transactional
    @Description("Проверяем, что не можем создать приватный чат без puid")
    public void testCantCreatePrivateConsultationWithoutPuid() {
        Exception clientException = assertThrows(IllegalArgumentException.class,
                () -> {
                    ChatSettings chatSettings = new ChatSettings(true, null, null, null);
                    var request = new GetEntityConsultationRequest(JsonNodeFactory.instance.objectNode(),
                            CHAT_CODE, chatSettings, null);

                    consultationService.createConsultation(ticketFirstLine, chat, request);
                });

        String expectedMessage = "puids is required because chat mark is private";
        String actualMessage = clientException.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));

        Exception partnerException = assertThrows(IllegalArgumentException.class,
                () -> {
                    ChatSettings partnerChatSettings = new ChatSettings(true, null, null, null);
                    var request = new GetEntityConsultationRequest(JsonNodeFactory.instance.objectNode(), CHAT_CODE,
                            null, partnerChatSettings);

                    consultationService.createConsultation(ticketFirstLine, chat, request);
                });

        actualMessage = partnerException.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    @Transactional
    @Description("Проверяем, что не можем создать приватный чат более чем с 1 puid")
    public void testCantCreatePrivateConsultationWithMoreThanOnePuid() {
        Exception clientException = assertThrows(IllegalArgumentException.class,
                () -> {
                    ChatSettings chatSettings = new ChatSettings(true, null, null, List.of(111L, 222L));
                    var request = new GetEntityConsultationRequest(JsonNodeFactory.instance.objectNode(),
                            CHAT_CODE, chatSettings, null);

                    consultationService.createConsultation(ticketFirstLine, chat, request);
                });

        String expectedMessage = "puids size more than 1 is not supported";
        String actualMessage = clientException.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));

        Exception partnerException = assertThrows(IllegalArgumentException.class,
                () -> {
                    ChatSettings partnerChatSettings = new ChatSettings(true, null, null, List.of(111L, 222L));
                    var request = new GetEntityConsultationRequest(JsonNodeFactory.instance.objectNode(), CHAT_CODE,
                            null, partnerChatSettings);

                    consultationService.createConsultation(ticketFirstLine, chat, request);
                });

        actualMessage = partnerException.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    @Transactional
    @Description("Проверяем вызов арбитража")
    public void testThatClientCouldSummonArbiter() {
        var request = new GetEntityConsultationRequest(JsonNodeFactory.instance.objectNode(), CHAT_CODE, null, null);

        var consultation = consultationService.createConsultation(ticketFirstLine, chat, request);

        // Сообщение от клиента
        var clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(CLIENT_CHAT_ID, payloadId(),
                        new PlainMessageText("client test text", null)),
                new CustomFrom("", "")
        );
        chatsService.receiveMessage(chat, clientMessageRequest);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);
        clearInvocations(feedbackClient);

        //Выставляем флаг доступности арбитража
        bcpService.edit(consultation, Map.of(
                Consultation.ARBITRAGE_AVAILABLE_BY_RELATED_ENTITY, true
        ));

        // Сообщение от партнера
        var partnerMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(PARTNER_CHAT_ID, payloadId(),
                        new PlainMessageText("partner test text", null)),
                new CustomFrom("", "")
        );
        chatsService.receiveMessage(chat, partnerMessageRequest);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        // Сообщение клиенту от партнера с предложением начать арбитраж
        var partnerMessageCaptor = ArgumentCaptor.forClass(ChatMessageRequest.class);
        verify(feedbackClient, times(1)).pushMessage(any(Chat.class), partnerMessageCaptor.capture());
        clearInvocations(feedbackClient);

        var partnerMessage = partnerMessageCaptor.getValue();
        Assertions.assertTrue(partnerMessage.getChatMessage() instanceof PlainMessage);
        PlainMessage plainMessage = (PlainMessage) partnerMessage.getChatMessage();
        Assertions.assertEquals(CLIENT_CHAT_ID, plainMessage.getChatId());
        var markupValue = plainMessage.getText().getReplyMarkup()
                .getItems()
                .get(0)
                .get(0)
                .getText();
        Assertions.assertEquals(SUMMON_MESSAGE, markupValue);

        //Запрос арбитража самим клиентом
        clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(CLIENT_CHAT_ID, payloadId(),
                        new PlainMessageText(SUMMON_MESSAGE, null)),
                new CustomFrom("", "")
        );
        chatsService.receiveMessage(chat, clientMessageRequest);
        TransactionSynchronizationUtils.triggerBeforeCommit(false);
        var messageRequestCaptor = ArgumentCaptor.forClass(ChatMessageRequest.class);
        verify(feedbackClient, times(1)).pushMessage(any(Chat.class), messageRequestCaptor.capture());

        var arbitrageSuggestionMessage = messageRequestCaptor.getAllValues().get(0);

        Assertions.assertTrue(arbitrageSuggestionMessage.getChatMessage() instanceof PlainMessage);
        plainMessage = (PlainMessage) arbitrageSuggestionMessage.getChatMessage();
        Assertions.assertEquals(CLIENT_CHAT_ID, plainMessage.getChatId());
        var markupValues = plainMessage.getText().getReplyMarkup().getItems().get(0)
                .stream()
                .map(InlineKeyboardItem::getText)
                .collect(Collectors.toList());
        Assertions.assertEquals(List.of(SUMMON_YES_MESSAGE, SUMMON_NO_MESSAGE), markupValues);
        clearInvocations(feedbackClient);

        //Подтверждаем арбитраж
        clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(CLIENT_CHAT_ID, payloadId(),
                        new PlainMessageText(SUMMON_YES_MESSAGE, null)),
                new CustomFrom("", "")
        );
        chatsService.receiveMessage(chat, clientMessageRequest);
        TransactionSynchronizationUtils.triggerBeforeCommit(false);
        consultation = consultationService.getConsultation(ticketFirstLine);

        Assertions.assertEquals(consultation.getStatus(), Consultation.Statuses.ARBITRAGE);
    }

    @Test
    @Transactional
    @Description("Проверяем, что клиент может отменить вызов арбитража")
    public void testThatClientCouldCancelArbiterSummon() {
        var request = new GetEntityConsultationRequest(JsonNodeFactory.instance.objectNode(), CHAT_CODE, null, null);

        var consultation = consultationService.createConsultation(ticketFirstLine, chat, request);

        // Сообщение от клиента
        var clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(CLIENT_CHAT_ID, payloadId(),
                        new PlainMessageText("test text", null)),
                new CustomFrom("", "")
        );
        chatsService.receiveMessage(chat, clientMessageRequest);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);
        clearInvocations(feedbackClient);

        bcpService.edit(consultation, Map.of(
                Consultation.ARBITRAGE_AVAILABLE_BY_RELATED_ENTITY, true
        ));

        // Сообщение от партнера
        var partnerMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(PARTNER_CHAT_ID, payloadId(),
                        new PlainMessageText("test text 2", null)),
                new CustomFrom("", "")
        );
        chatsService.receiveMessage(chat, partnerMessageRequest);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        // Сообщение клиенту от партнера с предложением начать арбитраж
        var partnerMessageCaptor = ArgumentCaptor.forClass(ChatMessageRequest.class);
        verify(feedbackClient, times(1)).pushMessage(any(Chat.class), partnerMessageCaptor.capture());
        clearInvocations(feedbackClient);

        var partnerMessage = partnerMessageCaptor.getValue();
        Assertions.assertTrue(partnerMessage.getChatMessage() instanceof PlainMessage);
        PlainMessage plainMessage = (PlainMessage) partnerMessage.getChatMessage();
        Assertions.assertEquals(CLIENT_CHAT_ID, plainMessage.getChatId());
        var markupValue = plainMessage.getText().getReplyMarkup()
                .getItems()
                .get(0)
                .get(0)
                .getText();
        Assertions.assertEquals(SUMMON_MESSAGE, markupValue);

        clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(CLIENT_CHAT_ID, payloadId(),
                        new PlainMessageText(SUMMON_MESSAGE, null)),
                new CustomFrom("", "")
        );
        chatsService.receiveMessage(chat, clientMessageRequest);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        var messageRequestCaptor = ArgumentCaptor.forClass(ChatMessageRequest.class);

        verify(feedbackClient, times(1)).pushMessage(any(Chat.class), messageRequestCaptor.capture());
        var arbitrageSuggestionMessage = messageRequestCaptor.getAllValues().get(0);

        Assertions.assertTrue(arbitrageSuggestionMessage.getChatMessage() instanceof PlainMessage);
        plainMessage = (PlainMessage) arbitrageSuggestionMessage.getChatMessage();
        Assertions.assertEquals(CLIENT_CHAT_ID, plainMessage.getChatId());
        var markupValues = plainMessage.getText().getReplyMarkup().getItems().get(0)
                .stream()
                .map(InlineKeyboardItem::getText)
                .collect(Collectors.toList());
        Assertions.assertEquals(List.of(SUMMON_YES_MESSAGE, SUMMON_NO_MESSAGE), markupValues);
        clearInvocations(feedbackClient);

        clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(CLIENT_CHAT_ID, payloadId(),
                        new PlainMessageText(SUMMON_NO_MESSAGE, null)),
                new CustomFrom("", "")
        );
        chatsService.receiveMessage(chat, clientMessageRequest);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        consultation = consultationService.getConsultation(ticketFirstLine);

        Assertions.assertEquals(consultation.getStatus(), Consultation.Statuses.DIRECT);
    }


    @Test
    @Transactional
    public void testThatClientHaveToSendParticularMessageWhileArbitrageRequested() {
        var request = new GetEntityConsultationRequest(JsonNodeFactory.instance.objectNode(), CHAT_CODE, null, null);
        var consultation = consultationService.createConsultation(ticketFirstLine, chat, request);

        // Сообщение от клиента
        var clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(CLIENT_CHAT_ID, payloadId(),
                        new PlainMessageText("test text", null)),
                new CustomFrom("", "")
        );
        chatsService.receiveMessage(chat, clientMessageRequest);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);
        clearInvocations(feedbackClient);

        bcpService.edit(consultation, Map.of(
                Consultation.ARBITRAGE_AVAILABLE_BY_RELATED_ENTITY, true
        ));

        // Сообщение от партнера
        var partnerMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(PARTNER_CHAT_ID, payloadId(),
                        new PlainMessageText("test text 2", null)),
                new CustomFrom("", "")
        );
        chatsService.receiveMessage(chat, partnerMessageRequest);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        // Сообщение клиенту от партнера с предложением начать арбитраж
        var partnerMessageCaptor = ArgumentCaptor.forClass(ChatMessageRequest.class);
        verify(feedbackClient, times(1)).pushMessage(any(Chat.class), partnerMessageCaptor.capture());
        clearInvocations(feedbackClient);

        var partnerMessage = partnerMessageCaptor.getValue();
        Assertions.assertTrue(partnerMessage.getChatMessage() instanceof PlainMessage);
        PlainMessage plainMessage = (PlainMessage) partnerMessage.getChatMessage();
        Assertions.assertEquals(CLIENT_CHAT_ID, plainMessage.getChatId());
        var markupValue = plainMessage.getText().getReplyMarkup()
                .getItems()
                .get(0)
                .get(0)
                .getText();
        Assertions.assertEquals(SUMMON_MESSAGE, markupValue);

        clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(CLIENT_CHAT_ID, payloadId(),
                        new PlainMessageText(SUMMON_MESSAGE, null)),
                new CustomFrom("", "")
        );
        chatsService.receiveMessage(chat, clientMessageRequest);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        var messageRequestCaptor = ArgumentCaptor.forClass(ChatMessageRequest.class);

        verify(feedbackClient, times(1)).pushMessage(any(Chat.class), messageRequestCaptor.capture());
        var arbitrageSuggestionMessage = messageRequestCaptor.getAllValues().get(0);

        Assertions.assertTrue(arbitrageSuggestionMessage.getChatMessage() instanceof PlainMessage);
        plainMessage = (PlainMessage) arbitrageSuggestionMessage.getChatMessage();
        Assertions.assertEquals(CLIENT_CHAT_ID, plainMessage.getChatId());
        var markupValues = plainMessage.getText().getReplyMarkup().getItems().get(0)
                .stream()
                .map(InlineKeyboardItem::getText)
                .collect(Collectors.toList());
        Assertions.assertEquals(List.of(SUMMON_YES_MESSAGE, SUMMON_NO_MESSAGE), markupValues);
        clearInvocations(feedbackClient);

        clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(CLIENT_CHAT_ID, payloadId(),
                        new PlainMessageText("Что вам от меня нужно?", null)),
                new CustomFrom("", "")
        );
        chatsService.receiveMessage(chat, clientMessageRequest);
        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        messageRequestCaptor = ArgumentCaptor.forClass(ChatMessageRequest.class);

        verify(feedbackClient, times(1)).pushMessage(any(Chat.class), messageRequestCaptor.capture());
        var incorrectChoiceMessage = messageRequestCaptor.getAllValues().get(0);

        Assertions.assertTrue(incorrectChoiceMessage.getChatMessage() instanceof PlainMessage);
        plainMessage = (PlainMessage) incorrectChoiceMessage.getChatMessage();
        Assertions.assertEquals(CLIENT_CHAT_ID, plainMessage.getChatId());
        markupValues = plainMessage.getText().getReplyMarkup().getItems().get(0)
                .stream()
                .map(InlineKeyboardItem::getText)
                .collect(Collectors.toList());
        Assertions.assertEquals(List.of(SUMMON_YES_MESSAGE, SUMMON_NO_MESSAGE), markupValues);
        clearInvocations(feedbackClient);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        consultation = consultationService.getConsultation(ticketFirstLine);

        Assertions.assertEquals(consultation.getStatus(), Consultation.Statuses.ARBITRAGE_REQUESTED);
    }

    private static String payloadId() {
        return UUID.randomUUID().toString();
    }

    private static Stream<Arguments> filesSharingParameters() {
        var image = new ImageFile(
                new FileInfo(123, "123", "name", 123, CLIENT_CHAT_ID),
                40,
                50,
                "https://ya.ru"
        );
        var gallery = new Gallery(List.of(new GalleryItem(image)), "gallery");

        return Stream.of(
                arguments("gallery", PlainMessage.forGallery(CLIENT_CHAT_ID, payloadId(), gallery)),
                arguments("file", PlainMessage.forFile(CLIENT_CHAT_ID, payloadId(), image)),
                arguments("image", PlainMessage.forImage(CLIENT_CHAT_ID, payloadId(), image))
        );
    }
}
