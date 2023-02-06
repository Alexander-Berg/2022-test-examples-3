package ru.yandex.market.crm.operatorwindow;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.transaction.Transactional;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import ru.yandex.market.b2bcrm.module.business.process.Bp;
import ru.yandex.market.b2bcrm.module.business.process.BpState;
import ru.yandex.market.b2bcrm.module.business.process.BpStatus;
import ru.yandex.market.crm.operatorwindow.jmf.entity.BeruConsultationChatTicket;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.entity.test.assertions.EntityCollectionAssert;
import ru.yandex.market.jmf.logic.def.AttachmentsService;
import ru.yandex.market.jmf.module.chat.Chat;
import ru.yandex.market.jmf.module.chat.ChatsService;
import ru.yandex.market.jmf.module.chat.CreateChatResult;
import ru.yandex.market.jmf.module.chat.FeedbackClient;
import ru.yandex.market.jmf.module.chat.controller.model.ChatMessageRequest;
import ru.yandex.market.jmf.module.chat.controller.model.CustomFrom;
import ru.yandex.market.jmf.module.chat.controller.model.FileInfo;
import ru.yandex.market.jmf.module.chat.controller.model.Gallery;
import ru.yandex.market.jmf.module.chat.controller.model.GalleryItem;
import ru.yandex.market.jmf.module.chat.controller.model.ImageFile;
import ru.yandex.market.jmf.module.chat.controller.model.MiscFile;
import ru.yandex.market.jmf.module.chat.controller.model.PlainMessage;
import ru.yandex.market.jmf.module.chat.controller.model.PlainMessageText;
import ru.yandex.market.jmf.module.chat.controller.model.ServerMessageInfo;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.jmf.utils.UrlCreationService;
import ru.yandex.market.ocrm.module.order.arbitrage.ChatConsultationSetting;
import ru.yandex.market.ocrm.module.order.arbitrage.ConsultationService;
import ru.yandex.market.ocrm.module.order.arbitrage.controller.models.GetEntityConsultationRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class ConsultationTicketTest extends AbstractModuleOwTest {

    private static final String CLIENT_CHAT_ID = "1234";
    private static final String PARTNER_CHAT_ID = "4321";
    private static final String CHAT_USER_ID = "userId";
    private static final String INVITE_HASH = "hash";
    private static final String CHAT_CODE = "beruOrderConsultations";
    private static final String SUMMON_MESSAGE = "Нужна помощь арбитра";
    private static final String SUMMON_YES_MESSAGE = "Саммон";
    private static final String SUMMON_NO_MESSAGE = "Неть";
    private static final String REQUEST_MESSAGE = "Передать ваш вопрос службе";
    private static final String TEXT1 = "Служба поддержки уже начала разбираться в ситуации и " +
            "ответит здесь в течение суток.";
    private static final String TEXT2 = "Покупатель начал спор по заказу. Для его решения к чату в течение рабочего " +
            "дня подключится арбитр Яндекс.Маркета. Изучение материалов спора и его решение может " +
            "занять до 10 рабочих дней.";

    private final ChatsService chatsService;
    private final BcpService bcpService;
    private final TicketTestUtils ticketTestUtils;
    private final FeedbackClient feedbackClient;
    private final EntityStorageService entityStorageService;
    private final AttachmentsService attachmentsService;
    private final UrlCreationService urlCreationService;
    private final ConsultationService consultationService;
    private final ConfigurationService configurationService;

    private Chat chat;
    private Bp bp;

    @Autowired
    public ConsultationTicketTest(
            ChatsService chatsService,
            BcpService bcpService,
            TicketTestUtils ticketTestUtils,
            FeedbackClient feedbackClient,
            EntityStorageService entityStorageService,
            AttachmentsService attachmentsService,
            UrlCreationService urlCreationService,
            ConsultationService consultationService,
            ConfigurationService configurationService
    ) {
        this.chatsService = chatsService;
        this.bcpService = bcpService;
        this.ticketTestUtils = ticketTestUtils;
        this.feedbackClient = feedbackClient;
        this.entityStorageService = entityStorageService;
        this.attachmentsService = attachmentsService;
        this.urlCreationService = urlCreationService;
        this.consultationService = consultationService;
        this.configurationService = configurationService;
    }

    @BeforeEach
    public void setUp() throws IOException {
        configurationService.setValue("useNewConsultationApi", true);

        Service service = ticketTestUtils.createService24x7();
        var bpStatus = bcpService.create(BpStatus.FQN, Maps.of(BpStatus.CODE, "first", BpStatus.TITLE, "first"));
        BpState bpState = bcpService.create(BpState.FQN, Maps.of(
                BpState.TITLE, "bpState",
                BpState.START_STATUS, bpStatus,
                BpState.NEXT_STATUSES, List.of(bcpService.create(BpStatus.FQN, Maps.of(BpStatus.CODE, "second",
                        BpStatus.TITLE, "second")))
        ));
        bp = bcpService.create(Bp.FQN, Maps.of(
                Bp.CODE, "bp",
                Bp.TITLE, "bp",
                Bp.STATES, List.of(bpState)
        ));

        chat = entityStorageService.getByNaturalId(Chat.class, CHAT_CODE);

        var properties = new HashMap<String, Object>() {{
            put(ChatConsultationSetting.CHAT, chat);
            put(ChatConsultationSetting.RELATED_METACLASS, bp.getMetaclass());
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
            put(ChatConsultationSetting.TICKET_METACLASS, "ticket$beruConsultationChat");
            put(ChatConsultationSetting.CLIENT_TITLE, "Клиент-тест");
            put(ChatConsultationSetting.PARTNER_TITLE, "Кто-то");
            put(ChatConsultationSetting.PARTNER_TEXT_ABOUT_START_ARBITRAGE, TEXT2);
            put(ChatConsultationSetting.CLIENT_TEXT_ABOUT_START_ARBITRAGE, TEXT1);
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
    public void resetMocks() {
        reset(feedbackClient, urlCreationService);
    }


    @Test
    @Transactional
    public void testThatTicketIsCreatedWhenFirstMessageWithAttachment() {
        var request = new GetEntityConsultationRequest(JsonNodeFactory.instance.objectNode(),
                CHAT_CODE, null, null);
        var consultation = consultationService.createConsultation(bp, chat, request);

        var image = new ImageFile(
                new FileInfo(123, "123", "name", 123, CLIENT_CHAT_ID),
                40,
                50,
                "https://ya.ru"
        );
        var gallery = new Gallery(List.of(new GalleryItem(image)), "gallery");
        var clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forGallery(CLIENT_CHAT_ID, payloadId(), gallery),
                new CustomFrom("", "")
        );
        chatsService.receiveMessage(chat, clientMessageRequest);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);
        clearInvocations(feedbackClient);

        var tickets = entityStorageService.list(Query.of(BeruConsultationChatTicket.FQN));
        EntityCollectionAssert.assertThat(tickets)
                .hasSize(1)
                .allHasAttributes(BeruConsultationChatTicket.CHAT_CONVERSATION, consultation.getClientConversation())
                .allHasAttributes(BeruConsultationChatTicket.STATUS, BeruConsultationChatTicket.STATUS_WAITING_PARTNER);

        var comment = entityStorageService.<Comment>list(
                Query.of(Comment.FQN)
                        .withFilters(Filters.eq(Comment.ENTITY, tickets.get(0)))
        );

        EntityCollectionAssert.assertThat(comment)
                .hasSize(1)
                .first()
                .is(new Condition<>(attachmentsService::hasAttachments, "У комментария есть вложение"));
    }

    @Test
    @Transactional
    public void testThatOnlyOneTicketIsCreatedPerConsultation() {
        var request = new GetEntityConsultationRequest(JsonNodeFactory.instance.objectNode(),
                CHAT_CODE, null, null);
        var consultation = consultationService.createConsultation(bp, chat, request);

        var clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(CLIENT_CHAT_ID, payloadId(),
                        new PlainMessageText("test text", null)),
                new CustomFrom("", "")
        );
        chatsService.receiveMessage(chat, clientMessageRequest);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);
        clearInvocations(feedbackClient);

        var tickets = entityStorageService.list(Query.of(BeruConsultationChatTicket.FQN));
        EntityCollectionAssert.assertThat(tickets)
                .hasSize(1)
                .allHasAttributes(BeruConsultationChatTicket.CHAT_CONVERSATION, consultation.getClientConversation())
                .allHasAttributes(BeruConsultationChatTicket.STATUS, BeruConsultationChatTicket.STATUS_WAITING_PARTNER);

        clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forText(CLIENT_CHAT_ID, payloadId(),
                        new PlainMessageText("test text", null)),
                new CustomFrom("", "")
        );
        chatsService.receiveMessage(chat, clientMessageRequest);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);
        clearInvocations(feedbackClient);

        tickets = entityStorageService.list(Query.of(BeruConsultationChatTicket.FQN));
        EntityCollectionAssert.assertThat(tickets)
                .hasSize(1)
                .allHasAttributes(BeruConsultationChatTicket.CHAT_CONVERSATION, consultation.getClientConversation())
                .allHasAttributes(BeruConsultationChatTicket.STATUS, BeruConsultationChatTicket.STATUS_WAITING_PARTNER);
    }

    @Test
    @Transactional
    public void testThatTicketIsCreatedWhenFirstMessageIsAttachmentOnly() {
        var request = new GetEntityConsultationRequest(JsonNodeFactory.instance.objectNode(),
                CHAT_CODE, null, null);
        var consultation = consultationService.createConsultation(bp, chat, request);

        var image = new MiscFile(
                new FileInfo(123, "123", "name", 123, CLIENT_CHAT_ID),
                "https://ya.ru"
        );
        var clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                PlainMessage.forFile(CLIENT_CHAT_ID, payloadId(), image),
                new CustomFrom("", "")
        );
        chatsService.receiveMessage(chat, clientMessageRequest);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);
        clearInvocations(feedbackClient);

        var tickets = entityStorageService.list(Query.of(BeruConsultationChatTicket.FQN));
        EntityCollectionAssert.assertThat(tickets)
                .hasSize(1)
                .allHasAttributes(BeruConsultationChatTicket.CHAT_CONVERSATION, consultation.getClientConversation())
                .allHasAttributes(BeruConsultationChatTicket.STATUS, BeruConsultationChatTicket.STATUS_WAITING_PARTNER);

        var comment = entityStorageService.<Comment>list(
                Query.of(Comment.FQN)
                        .withFilters(Filters.eq(Comment.ENTITY, tickets.get(0)))
        );

        EntityCollectionAssert.assertThat(comment)
                .hasSize(1)
                .first()
                .is(new Condition<>(attachmentsService::hasAttachments, "У комментария есть вложение"));
    }

    private static String payloadId() {
        return UUID.randomUUID().toString();
    }

}
