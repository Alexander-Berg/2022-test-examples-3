package ru.yandex.market.crm.operatorwindow;

import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.transaction.Transactional;

import org.assertj.core.api.Condition;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import ru.yandex.market.b2bcrm.module.account.Shop;
import ru.yandex.market.crm.operatorwindow.jmf.entity.BeruConsultationChatTicket;
import ru.yandex.market.crm.operatorwindow.services.customer.CustomerService;
import ru.yandex.market.crm.util.Exceptions;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.HasYymmId;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.entity.test.assertions.EntityCollectionAssert;
import ru.yandex.market.jmf.logic.def.AttachmentsService;
import ru.yandex.market.jmf.module.chat.Chat;
import ru.yandex.market.jmf.module.chat.ChatsService;
import ru.yandex.market.jmf.module.chat.CreateChatResult;
import ru.yandex.market.jmf.module.chat.FeedbackClient;
import ru.yandex.market.jmf.module.chat.Ticket;
import ru.yandex.market.jmf.module.chat.controller.model.ChatMessageRequest;
import ru.yandex.market.jmf.module.chat.controller.model.Client;
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
import ru.yandex.market.jmf.module.ou.Ou;
import ru.yandex.market.jmf.module.ou.impl.EmployeeTestUtils;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.security.test.impl.MockSecurityDataService;
import ru.yandex.market.jmf.timings.test.impl.TimerTestUtils;
import ru.yandex.market.jmf.trigger.impl.TriggerServiceImpl;
import ru.yandex.market.jmf.utils.UrlCreationService;
import ru.yandex.market.ocrm.module.common.Customer;
import ru.yandex.market.ocrm.module.order.arbitrage.Consultation;
import ru.yandex.market.ocrm.module.order.arbitrage.OrderConsultationService;
import ru.yandex.market.ocrm.module.order.domain.Order;
import ru.yandex.market.ocrm.module.order.domain.OrderStatus;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class BeruOrderConsultationTicketTest extends AbstractModuleOwTest {
    private static final String CLIENT_CHAT_ID = "1234";
    private static final String PARTNER_CHAT_ID = "4321";
    private static final String CHAT_USER_ID = "userId";
    private static final String INVITE_HASH = "hash";

    private final OrderConsultationService orderConsultationService;
    private final ChatsService chatsService;
    private final BcpService bcpService;
    private final TicketTestUtils ticketTestUtils;
    private final FeedbackClient feedbackClient;
    private final EntityStorageService entityStorageService;
    private final TriggerServiceImpl triggerService;
    private final CustomerService customerService;
    private final AttachmentsService attachmentsService;
    private final UrlCreationService urlCreationService;
    private final TimerTestUtils timerTestUtils;
    private final MockSecurityDataService securityDataService;
    private final EmployeeTestUtils employeeTestUtils;

    private Service service;
    private Chat chat;
    private Customer customer;
    private Shop shop;
    private Order order;

    @Autowired
    public BeruOrderConsultationTicketTest(
            OrderConsultationService orderConsultationService,
            ChatsService chatsService,
            BcpService bcpService,
            TicketTestUtils ticketTestUtils,
            FeedbackClient feedbackClient,
            EntityStorageService entityStorageService,
            TriggerServiceImpl triggerService,
            CustomerService customerService,
            AttachmentsService attachmentsService,
            UrlCreationService urlCreationService,
            TimerTestUtils timerTestUtils,
            MockSecurityDataService securityDataService,
            EmployeeTestUtils employeeTestUtils
    ) {
        this.orderConsultationService = orderConsultationService;
        this.chatsService = chatsService;
        this.bcpService = bcpService;
        this.ticketTestUtils = ticketTestUtils;
        this.feedbackClient = feedbackClient;
        this.entityStorageService = entityStorageService;
        this.triggerService = triggerService;
        this.customerService = customerService;
        this.attachmentsService = attachmentsService;
        this.urlCreationService = urlCreationService;
        this.timerTestUtils = timerTestUtils;
        this.securityDataService = securityDataService;
        this.employeeTestUtils = employeeTestUtils;
    }

    private static String payloadId() {
        return UUID.randomUUID().toString();
    }

    @BeforeEach
    public void setUp() {
        service = ticketTestUtils.createService();

        chat = entityStorageService.getByNaturalId(Chat.class, "beruOrderConsultations");

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
                Order.BUYER_UID, customer.getUid()
        ));

        when(feedbackClient.createNonPublicGroupChat(
                eq(chat),
                any(String.class),
                any(String.class),
                eq(new Client(customer.getUid(), customer.getTitle()))
        )).thenReturn(new CreateChatResult(CLIENT_CHAT_ID, null));

        when(feedbackClient.createPublicGroupChat(any(Chat.class), any(String.class), any(String.class)))
                .thenReturn(new CreateChatResult(PARTNER_CHAT_ID, INVITE_HASH));

        when(customerService.getCustomerByUid(customer.getUid()))
                .thenReturn(new ru.yandex.market.crm.operatorwindow.domain.customer.Customer(customer));
    }

    @NotNull
    private URL urlOf(String value) {
        return Exceptions.sneakyRethrow(() -> new URL(value));
    }

    @AfterEach
    public void resetMocks() {
        reset(feedbackClient, customerService, urlCreationService);
    }

    /**
     * @see <a href="https://testpalm2.yandex-team.ru/testcase/ocrm-1248">testcase/ocrm-1248</a>
     */
    @Test
    @Transactional
    public void testThatOnlyOneTicketIsCreatedPerConsultation() {
        var consultation = orderConsultationService.createConsultation(order, chat);

        clientSendMessageAndImmediatelyClearInvocations(PlainMessage.forText(CLIENT_CHAT_ID, payloadId(),
                new PlainMessageText("test text", null)));

        var tickets = entityStorageService.list(Query.of(BeruConsultationChatTicket.FQN));
        EntityCollectionAssert.assertThat(tickets)
                .hasSize(1)
                .allHasAttributes(BeruConsultationChatTicket.CHAT_CONVERSATION, consultation.getClientConversation())
                .allHasAttributes(BeruConsultationChatTicket.STATUS, BeruConsultationChatTicket.STATUS_WAITING_PARTNER);

        clientSendMessageAndImmediatelyClearInvocations(PlainMessage.forText(CLIENT_CHAT_ID, payloadId(),
                new PlainMessageText("test text", null)));

        tickets = entityStorageService.list(Query.of(BeruConsultationChatTicket.FQN));
        EntityCollectionAssert.assertThat(tickets)
                .hasSize(1)
                .allHasAttributes(BeruConsultationChatTicket.CHAT_CONVERSATION, consultation.getClientConversation())
                .allHasAttributes(BeruConsultationChatTicket.STATUS, BeruConsultationChatTicket.STATUS_WAITING_PARTNER);

    }

    /**
     * @see <a href="https://testpalm2.yandex-team.ru/testcase/ocrm-1249">testcase/ocrm-1249</a>
     */
    @Test
    @Transactional
    public void testThatTicketIsCreatedWhenFirstMessageWithAttachment() {
        var consultation = orderConsultationService.createConsultation(order, chat);

        var image = new ImageFile(
                new FileInfo(123, "123", "name", 123, CLIENT_CHAT_ID),
                40,
                50,
                "https://ya.ru"
        );
        var gallery = new Gallery(List.of(new GalleryItem(image)), "gallery");
        clientSendMessageAndImmediatelyClearInvocations(PlainMessage.forGallery(CLIENT_CHAT_ID, payloadId(), gallery));

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

    /**
     * @see <a href="https://testpalm2.yandex-team.ru/testcase/ocrm-1250">testcase/ocrm-1250</a>
     */
    @Test
    @Transactional
    public void testThatTicketIsCreatedWhenFirstMessageIsAttachmentOnly() {
        var consultation = orderConsultationService.createConsultation(order, chat);

        var image = new MiscFile(
                new FileInfo(123, "123", "name", 123, CLIENT_CHAT_ID),
                "https://ya.ru"
        );
        clientSendMessageAndImmediatelyClearInvocations(PlainMessage.forFile(CLIENT_CHAT_ID, payloadId(), image));

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

    /**
     * Если консультация в обращении пробыла 30 дней в статусе {@link Consultation.Statuses#ARBITRAGE_REQUESTED},
     * то консультация должна завершиться, а обращение - разрешиться ({@link Ticket#STATUS_RESOLVED})
     */
    @Test
    @Transactional
    public void testTicketClosedAfter30DaysInArbitrageRequested() {
        var consultation = orderConsultationService.createConsultation(order, chat);
        clientSendMessageAndImmediatelyClearInvocations(PlainMessage.forText(CLIENT_CHAT_ID, payloadId(),
                new PlainMessageText("test text", null)));

        bcpService.edit(consultation, Map.of(
                Consultation.STATUS, Consultation.Statuses.ARBITRAGE_REQUESTED
        ));
        var ticket = entityStorageService.<Ticket>list(Query.of(BeruConsultationChatTicket.FQN)).get(0);
        var ou = entityStorageService.list(Query.of(Ou.FQN)).get(0);
        securityDataService.setCurrentEmployee(
                employeeTestUtils.createEmployee(Randoms.string(), ou)
        );

        Assertions.assertEquals(Consultation.Statuses.ARBITRAGE_REQUESTED, consultation.getStatus());
        timerTestUtils.simulateTimerExpiration(ticket.getGid(), "idleTimer");
        Assertions.assertEquals(Consultation.Statuses.FINISHED, consultation.getStatus());
        Assertions.assertEquals(Ticket.STATUS_RESOLVED, ticket.getStatus());
    }

    private void clientSendMessageAndImmediatelyClearInvocations(PlainMessage message) {
        var clientMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                message,
                new CustomFrom("", "")
        );
        chatsService.receiveMessage(chat, clientMessageRequest);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);
        clearInvocations(feedbackClient);
    }
}
