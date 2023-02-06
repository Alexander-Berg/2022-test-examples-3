package ru.yandex.market.ocrm.module.order.arbitrage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.transaction.Transactional;

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
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import ru.yandex.market.b2bcrm.module.account.Shop;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.HasYymmId;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.entity.test.assertions.EntityCollectionAssert;
import ru.yandex.market.jmf.module.chat.Chat;
import ru.yandex.market.jmf.module.chat.ChatsService;
import ru.yandex.market.jmf.module.chat.CreateChatResult;
import ru.yandex.market.jmf.module.chat.FeedbackClient;
import ru.yandex.market.jmf.module.chat.MessengerClient;
import ru.yandex.market.jmf.module.chat.controller.model.ChatMessage;
import ru.yandex.market.jmf.module.chat.controller.model.ChatMessageRequest;
import ru.yandex.market.jmf.module.chat.controller.model.Client;
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
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.trigger.impl.TriggerServiceImpl;
import ru.yandex.market.jmf.utils.LinkUtils;
import ru.yandex.market.jmf.utils.UrlCreationService;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.ocrm.module.common.Customer;
import ru.yandex.market.ocrm.module.order.arbitrage.controller.ArbitrageController;
import ru.yandex.market.ocrm.module.order.arbitrage.controller.models.ConsultationStatus;
import ru.yandex.market.ocrm.module.order.arbitrage.controller.models.ConversationStatus;
import ru.yandex.market.ocrm.module.order.arbitrage.controller.models.GetConsultationRequest;
import ru.yandex.market.ocrm.module.order.domain.Order;
import ru.yandex.market.ocrm.module.order.domain.OrderStatus;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(classes = ModuleOrderArbitrageTestConfiguration.class)
public class OrderConsultationTest {

    private static final String CLIENT_CHAT_ID = "1234";
    private static final String PARTNER_CHAT_ID = "4321";
    private static final String CHAT_USER_ID = "userId";
    private static final String INVITE_HASH = "hash";
    private final OrderConsultationService orderConsultationService;
    private final ArbitrageController arbitrageController;
    private final FeedbackClient feedbackClient;
    private final MessengerClient messengerClient;
    private final BcpService bcpService;
    private final TicketTestUtils ticketTestUtils;
    private final EntityStorageService entityStorageService;
    private final ChatsService chatsService;
    private final TriggerServiceImpl triggerService;
    private final UrlCreationService urlCreationService;
    private final ConfigurationService configurationService;
    private final MbiApiClient mbiApiClient;

    private Service service;
    private Chat chat;
    private Customer customer;
    private Shop shop;
    private Order order;

    @Autowired
    public OrderConsultationTest(
            OrderConsultationService orderConsultationService,
            ArbitrageController arbitrageController,
            FeedbackClient feedbackClient,
            MessengerClient messengerClient,
            BcpService bcpService,
            TicketTestUtils ticketTestUtils,
            EntityStorageService entityStorageService,
            ChatsService chatsService,
            TriggerServiceImpl triggerService,
            UrlCreationService urlCreationService,
            ConfigurationService configurationService,
            MbiApiClient mbiApiClient) {
        this.orderConsultationService = orderConsultationService;
        this.arbitrageController = arbitrageController;
        this.feedbackClient = feedbackClient;
        this.messengerClient = messengerClient;
        this.bcpService = bcpService;
        this.ticketTestUtils = ticketTestUtils;
        this.entityStorageService = entityStorageService;
        this.chatsService = chatsService;
        this.triggerService = triggerService;
        this.urlCreationService = urlCreationService;
        this.configurationService = configurationService;
        this.mbiApiClient = mbiApiClient;
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

    private static String payloadId() {
        return UUID.randomUUID().toString();
    }

    @BeforeEach
    void setUp() throws IOException {
        configurationService.setValue("openChatByPartnerEnabled", true);
        configurationService.setValue("checkExternalLinkEnabled", true);

        service = ticketTestUtils.createService();

        chat = bcpService.create(Chat.FQN, Map.of(
                Chat.CODE, "beruOrderConsultations",
                Chat.CHAT_ID, "chatId",
                Chat.SERVICE, service,
                Chat.TITLE, "chat",
                Chat.CHAT_USER_ID, CHAT_USER_ID
        ));

        customer = bcpService.create(Customer.FQN, Map.of(
                Customer.TITLE, "customer",
                Customer.UID, 321
        ));

        shop = bcpService.create(Shop.FQN, Map.of(
                Shop.SHOP_ID, 667,
                Shop.TITLE, "test shop"
        ));

        order = bcpService.create(Order.FQN, Map.of(
                Order.ID, HasYymmId.idOf(OffsetDateTime.now(), "123"),
                Order.NUMBER, 123,
                Order.CUSTOMER, customer,
                Order.CREATION_DATE, OffsetDateTime.now(),
                Order.STATUS, entityStorageService.getByNaturalId(OrderStatus.class, "PROCESSING"),
                Order.DELIVERY_TO_DATE, LocalDate.now(),
                Order.SHOP_ID, shop.getShopId(),
                Order.COLOR, "WHITE"
        ));

        when(feedbackClient.createNonPublicGroupChat(
                eq(chat),
                any(String.class),
                any(String.class),
                eq(new Client(customer.getUid(), customer.getTitle()))
        )).thenReturn(new CreateChatResult(CLIENT_CHAT_ID, null));

        when(feedbackClient.createPublicGroupChat(any(Chat.class), any(String.class), any(String.class)))
                .thenReturn(new CreateChatResult(PARTNER_CHAT_ID, INVITE_HASH));

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

    /**
     * Проверяем, что если дернуть ручку создания или получения консультации, если консультация уже существует, то
     * получим существующую консультацию
     */
    @Test
    @Transactional
    public void testThatPostToArbitrageCreateOrGetReturnsExistingConsultation() throws InterruptedException,
            TimeoutException {
        var consultation1 = arbitrageController.getOrCreateConsultation(
                order.getOrderId(), new GetConsultationRequest(customer.getUid())
        );
        arbitrageController.getOrCreateConsultation(
                order.getOrderId(), new GetConsultationRequest(customer.getUid())
        );
        EntityCollectionAssert.assertThat(entityStorageService.list(
                Query.of(Consultation.FQN)
                        .withFilters(Filters.eq(Consultation.ORDER, order))
        ))
                .size()
                .isEqualTo(1);
    }

    /**
     * Проверяем, что созданные консультации доступны в ручке получения списка консультаций для партнерского интерфейса
     */
    @Test
    @Transactional
    public void testThatCreatedConsultationIsVisibleToPartners() {
        orderConsultationService.createConsultation(order, chat);

        var partnerConsultation = arbitrageController.getPartnerConsultation(shop.getShopId(), order.getOrderId());
        Assertions.assertEquals(123L, partnerConsultation.getOrderNumber());
        Assertions.assertEquals(ConversationStatus.NONE, partnerConsultation.getConversationStatus());
        Assertions.assertEquals(ConsultationStatus.DIRECT_CONVERSATION, partnerConsultation.getStatus());
        Assertions.assertEquals(INVITE_HASH, partnerConsultation.getChatInviteHash());
    }

    @AfterEach
    public void tearDown() {
        reset(feedbackClient, messengerClient);
        configurationService.setValue("openChatByPartnerEnabled", false);
    }

    /**
     * Проверяем, что дернув ручку создания или получения консультации, если консультации нет, получаем вновь
     * созданную консультацию
     */
    @Test
    @Transactional
    public void testThatPostToArbitrageCreateOrGetCreatesNewConsultation() throws InterruptedException,
            TimeoutException {
        var consultation = arbitrageController.getOrCreateConsultation(
                order.getOrderId(), new GetConsultationRequest(customer.getUid())
        );
        Assertions.assertEquals(123L, consultation.getOrderNumber());
        Assertions.assertEquals(ConversationStatus.NONE, consultation.getConversationStatus());
        Assertions.assertEquals(ConsultationStatus.DIRECT_CONVERSATION, consultation.getConsultationStatus());
        Assertions.assertEquals(CLIENT_CHAT_ID, consultation.getChatId());
        Assertions.assertEquals(PARTNER_CHAT_ID, consultation.getPartnerChatId());
    }

    /**
     * Проверяем, что пользователи чатов не имеют прав на редактирование списка участников чата, на то, чтобы выйти
     * из чата. А у партнера еще и нет права писать в чат до первого сообщения клиента
     *
     * @see <a href="https://testpalm2.yandex-team.ru/testcase/ocrm-1318">testcase/ocrm-1318</a>
     */
    @Test
    @Transactional
    public void testThatUsersCouldNotInviteOtherUsersAndCouldNotLeaveFromCreatedChats() {
        var consultation = orderConsultationService.createConsultation(order, chat);

        var clientConversationMemberRights = consultation.getClientConversation().getMetadata().getMemberRights();
        Assertions.assertEquals(false, clientConversationMemberRights.getLeave());
        Assertions.assertEquals(false, clientConversationMemberRights.getAddUsers());
        Assertions.assertEquals(false, clientConversationMemberRights.getRemoveUsers());

        var partnerConversationMemberRights = consultation.getPartnerConversation().getMetadata().getMemberRights();
        Assertions.assertEquals(false, partnerConversationMemberRights.getLeave());
        Assertions.assertEquals(false, partnerConversationMemberRights.getAddUsers());
        Assertions.assertEquals(false, partnerConversationMemberRights.getRemoveUsers());
        Assertions.assertEquals(true, partnerConversationMemberRights.getWrite());

        var clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(CLIENT_CHAT_ID, payloadId(),
                        new PlainMessageText("test text", null)),
                new CustomFrom("", "")
        );
        chatsService.receiveMessage(chat, clientMessageRequest);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);
        consultation = orderConsultationService.getConsultation(order);

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
    /**
     * @see <a href="https://testpalm2.yandex-team.ru/testcase/ocrm-1267">testcase/ocrm-1267</a>
     */
    public void testThatFilesAreSharedWhileResending(String testName, ChatMessage chatMessage) {
        orderConsultationService.createConsultation(order, chat);

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

    /**
     * Проверяем, что сообщения пересылаются во время прямой переписки
     *
     * @see <a href="https://testpalm2.yandex-team.ru/testcase/ocrm-1253">testcase/ocrm-1253</a>
     */
    @Test
    @Transactional
    public void testThatMessagesAreResendWhileConsultationInDirectConversation() {
        orderConsultationService.createConsultation(order, chat);

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
        Assertions.assertEquals("Клиент", chatMessageRequest.getCustomFrom().getDisplayName());
    }

    /**
     * Проверяем, что к сообщениям с внешней ссылкой добавляется предупреждение
     */
    @Test
    @Transactional
    public void testThatMessagesWithExternalLinkAreAppendedWithWarning() {
        orderConsultationService.createConsultation(order, chat);

        var messageText = "test text fishing.site";
        var clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(PARTNER_CHAT_ID, payloadId(),
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
        Assertions.assertEquals(CLIENT_CHAT_ID, plainMessage.getChatId());
        Assertions.assertEquals(messageText + "\n" + LinkUtils.EXTERNAL_LINK_WARNING, plainMessage.getText().getMessageText());
    }

    /**
     * Проверяем, что сообщения не пересылаются, когда арбитраж активен
     * @see <a href="https://testpalm2.yandex-team.ru/testcase/ocrm-1255">testcase/ocrm-1255</a>
     */
    @Test
    @Transactional
    public void testThatMessagesAreNotResendWhileConsultationInArbitrage() {
        var consultation = orderConsultationService.createConsultation(order, chat);

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
        var consultation = orderConsultationService.createConsultation(order, chat);

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

        consultation = orderConsultationService.getConsultation(order);

        Assertions.assertTrue(consultation.getArbitrageWasStarted());

        bcpService.edit(consultation, Map.of(
                Consultation.STATUS, Consultation.Statuses.FINISHED
        ));

        consultation = orderConsultationService.getConsultation(order);

        Assertions.assertTrue(consultation.getArbitrageWasStarted());
    }

    /**
     * Проверяем, что сообщения клиента не пересылаются во время запроса арбитража, а сообщения партнера пересылаются
     *
     * @see <a href="https://testpalm2.yandex-team.ru/testcase/ocrm-1372">testcase/ocrm-1372</a>
     * @see <a href="https://testpalm2.yandex-team.ru/testcase/ocrm-1374">testcase/ocrm-1374</a>
     */
    @Test
    @Transactional
    public void testThatMessagesFromPartnerAreResendButClientMessagesAreNotWhileConsultationInArbitrageRequested() {
        var consultation = orderConsultationService.createConsultation(order, chat);

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
    public void testThatAttachmentFromPartnerAreResendButClientMessagesAreNotWhileConsultationInArbitrageRequested() {
        var consultation = orderConsultationService.createConsultation(order, chat);

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
        var message =  PlainMessage.forGallery(CLIENT_CHAT_ID, payloadId(), gallery);

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
    public void testThatClientCouldSummonArbiter() {
        var consultation = orderConsultationService.createConsultation(order, chat);

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
        Assertions.assertEquals("Нужна помощь Яндекс.Маркета", markupValue);

        clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(CLIENT_CHAT_ID, payloadId(),
                        new PlainMessageText("Нужна помощь Яндекс.Маркета", null)),
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
        Assertions.assertEquals(List.of("Позвать поддержку", "Пока не нужно"), markupValues);
        clearInvocations(feedbackClient);

        clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(CLIENT_CHAT_ID, payloadId(),
                        new PlainMessageText("Позвать поддержку", null)),
                new CustomFrom("", "")
        );
        chatsService.receiveMessage(chat, clientMessageRequest);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        consultation = orderConsultationService.getConsultation(order);

        Assertions.assertEquals(consultation.getStatus(), Consultation.Statuses.ARBITRAGE);
    }

    @Test
    @Transactional
    public void testThatClientCouldCancelArbiterSummon() {
        var consultation = orderConsultationService.createConsultation(order, chat);

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
        Assertions.assertEquals("Нужна помощь Яндекс.Маркета", markupValue);

        clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(CLIENT_CHAT_ID, payloadId(),
                        new PlainMessageText("Нужна помощь Яндекс.Маркета", null)),
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
        Assertions.assertEquals(List.of("Позвать поддержку", "Пока не нужно"), markupValues);
        clearInvocations(feedbackClient);

        clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(CLIENT_CHAT_ID, payloadId(),
                        new PlainMessageText("Пока не нужно", null)),
                new CustomFrom("", "")
        );
        chatsService.receiveMessage(chat, clientMessageRequest);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        consultation = orderConsultationService.getConsultation(order);

        Assertions.assertEquals(consultation.getStatus(), Consultation.Statuses.DIRECT);
    }

    @Test
    @Transactional
    public void testThatClientHaveToSendParticularMessageWhileArbitrageRequested() {
        var consultation = orderConsultationService.createConsultation(order, chat);

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
        Assertions.assertEquals("Нужна помощь Яндекс.Маркета", markupValue);

        clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(CLIENT_CHAT_ID, payloadId(),
                        new PlainMessageText("Нужна помощь Яндекс.Маркета", null)),
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
        Assertions.assertEquals(List.of("Позвать поддержку", "Пока не нужно"), markupValues);
        clearInvocations(feedbackClient);

        clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(CLIENT_CHAT_ID, payloadId(),
                        new PlainMessageText("Ни чо не понял", null)),
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
        Assertions.assertEquals(List.of("Позвать поддержку", "Пока не нужно"), markupValues);
        clearInvocations(feedbackClient);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        consultation = orderConsultationService.getConsultation(order);

        Assertions.assertEquals(consultation.getStatus(), Consultation.Statuses.ARBITRAGE_REQUESTED);
    }

    @Test
    @Transactional
    public void testPartnerInitiatedChat() {
        Consultation consultation = orderConsultationService.createConsultation(order, chat);

        var partnerMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(PARTNER_CHAT_ID, payloadId(),
                        new PlainMessageText("Partner sends text", null)),
                new CustomFrom("", "")
        );

        bcpService.edit(consultation, Map.of(Consultation.STATUS, Consultation.Statuses.NOT_STARTED));

        chatsService.receiveMessage(chat, partnerMessageRequest);

        bcpService.edit(consultation, Map.of(Consultation.STATUS, Consultation.Statuses.DIRECT));

        bcpService.edit(consultation,
                Map.of(Consultation.PARTNER_CONVERSATION_STATUS, ConversationStatus.WAITING_FOR_CLIENT.getCode()));

        var clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(CLIENT_CHAT_ID, payloadId(),
                        new PlainMessageText("Client sends text", null)),
                new CustomFrom("", "")
        );

        chatsService.receiveMessage(chat, clientMessageRequest);

        bcpService.edit(consultation,
                Map.of(Consultation.PARTNER_CONVERSATION_STATUS, ConversationStatus.WAITING_FOR_PARTNER.getCode()));

        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        verify(mbiApiClient, times(1)).sendMessageToShop(eq(667L), eq(1617366066), any());
        verify(mbiApiClient, times(0)).sendMessageToShop(eq(667L), eq(1617096644), any());
    }
}
