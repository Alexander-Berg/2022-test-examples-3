package ru.yandex.market.crm.operatorwindow;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.transaction.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jdk.jfr.Description;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import ru.yandex.market.b2bcrm.module.ticket.B2bChatTicket;
import ru.yandex.market.b2bcrm.module.ticket.B2bShopChatTicket;
import ru.yandex.market.b2bcrm.module.ticket.B2bSupplierChatTicket;
import ru.yandex.market.b2bcrm.module.ticket.B2bTicket;
import ru.yandex.market.crm.domain.Phone;
import ru.yandex.market.crm.operatorwindow.domain.Email;
import ru.yandex.market.crm.operatorwindow.domain.customer.Customer;
import ru.yandex.market.crm.operatorwindow.integration.Brands;
import ru.yandex.market.crm.operatorwindow.jmf.entity.BeruChatTicket;
import ru.yandex.market.crm.operatorwindow.jmf.entity.CourierPlatformChatTicket;
import ru.yandex.market.crm.operatorwindow.services.customer.CustomerService;
import ru.yandex.market.crm.operatorwindow.utils.ChatsTestUtils;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.crm.util.Result;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.bcp.exceptions.ValidationException;
import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.jmf.configuration.api.Property;
import ru.yandex.market.jmf.configuration.api.PropertyTypes;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.logic.wf.HasWorkflow;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.chat.Chat;
import ru.yandex.market.jmf.module.chat.ChatBotClient;
import ru.yandex.market.jmf.module.chat.ChatBotResponse;
import ru.yandex.market.jmf.module.chat.ChatClient;
import ru.yandex.market.jmf.module.chat.ChatClientService;
import ru.yandex.market.jmf.module.chat.ChatConversation;
import ru.yandex.market.jmf.module.chat.ChatTicket;
import ru.yandex.market.jmf.module.chat.ChatsService;
import ru.yandex.market.jmf.module.chat.DirectBotChatSettings;
import ru.yandex.market.jmf.module.chat.Employee;
import ru.yandex.market.jmf.module.chat.FeedbackClient;
import ru.yandex.market.jmf.module.chat.InChatComment;
import ru.yandex.market.jmf.module.chat.InChatMessage;
import ru.yandex.market.jmf.module.chat.OutChatComment;
import ru.yandex.market.jmf.module.chat.controller.model.ChatMessageRequest;
import ru.yandex.market.jmf.module.chat.controller.model.CustomFrom;
import ru.yandex.market.jmf.module.chat.controller.model.PlainMessage;
import ru.yandex.market.jmf.module.chat.controller.model.PlainMessageText;
import ru.yandex.market.jmf.module.chat.controller.model.ReplyMarkup;
import ru.yandex.market.jmf.module.chat.controller.model.ServerMessageInfo;
import ru.yandex.market.jmf.module.chat.controller.model.ServiceMessage;
import ru.yandex.market.jmf.module.chat.controller.model.SystemMessage;
import ru.yandex.market.jmf.module.chat.controller.model.TypingMessage;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.comment.InternalComment;
import ru.yandex.market.jmf.module.comment.SendCommentsStatusService;
import ru.yandex.market.jmf.module.comment.test.impl.CommentTestUtils;
import ru.yandex.market.jmf.module.ou.Ou;
import ru.yandex.market.jmf.module.ou.impl.EmployeeTestUtils;
import ru.yandex.market.jmf.module.ou.impl.OuTestUtils;
import ru.yandex.market.jmf.module.ticket.Channel;
import ru.yandex.market.jmf.module.ticket.DistributionService;
import ru.yandex.market.jmf.module.ticket.EmployeeDistributionStatus;
import ru.yandex.market.jmf.module.ticket.OmniChannelSettingsService;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Team;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.TicketCategory;
import ru.yandex.market.jmf.module.ticket.distribution.DistributionUtils;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.security.test.impl.MockSecurityDataService;
import ru.yandex.market.jmf.timings.test.impl.TimerTestUtils;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.ocrm.module.tpl.MarketTplClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.b2bcrm.module.ticket.B2bChatTicket.STATUS_MISSING;
import static ru.yandex.market.b2bcrm.module.ticket.B2bChatTicket.WAIT_ANSWER_FOR_QUESTION;
import static ru.yandex.market.jmf.bcp.operations.SetCurrentEmployeeOperationHandler.AUTHOR_ATTR;
import static ru.yandex.market.jmf.module.ticket.Ticket.STATUS_REGISTERED;

@Transactional
public class ChatsServiceTest extends AbstractModuleOwTest {

    public static final ObjectMapper mapper = new ObjectMapper();
    public static final Property<Boolean> CLOSE_SHOP_CHAT_SUPPORT =
            Property.of("closeShopChatSupport", PropertyTypes.BOOLEAN);
    public static final Property<Boolean> CLOSE_SUPPLIER_CHAT_SUPPORT =
            Property.of("closeSupplierChatSupport", PropertyTypes.BOOLEAN);
    public static final Property<Boolean> ONE_SCRIPT_FOR_B2B_AND_B2C_ON_CREATE_CHAT_TICKET =
            Property.of("oneScriptForB2bAndB2cOnCreateChatTicket", PropertyTypes.BOOLEAN);
    private static final String DEFAULT_CHAT_SERVICE = "beruChat";
    private static final String DEFAULT_CHAT_ID = "default_provider_chat_id";
    private static final String COURIER_PLATFORM_CHAT_ID = "courier_platform_provider_chat_id";
    private static final String COURIER_PLATFORM_CHAT = "courierPlatformChat";
    private static final String B2B_CHAT_ID = "b2b_chat_id";
    private static final String B2B_SUPPLIER_CHAT_ID = "b2b_supplier_chat_id";
    private static final String B2B_SHOP_CHAT_ID = "b2b_shop_chat_id";
    private static final String FIRST_LINE_CHAT_TEAM = "firstLineChat";
    private static final String B2B_CHAT_TEAM = "b2bChat";
    private static final String B2B_SUPPORT_CHAT_TEAM = "b2bSupplierChat";
    private static final String B2B_SHOP_CHAT_TEAM = "b2bShopChat";
    private static final String COURIER_PLATFORM_CHAT_TEAM = "courierPlatformChat";
    private static final String B2B_SHOP_SUPPORT_BRAND = "b2bShopSupport";
    private static final String STATUS_ESCALATED = "escalated";
    private static final String STATUS_REOPENED = "reopened";
    @Inject
    TicketTestUtils ticketTestUtils;
    @Inject
    BcpService bcpService;
    @Inject
    DbService dbService;
    @Inject
    ChatsTestUtils chatsTestUtils;
    @Inject
    ChatsService chatsService;
    @Inject
    OuTestUtils ouTestUtils;
    @Inject
    EmployeeTestUtils employeeTestUtils;
    @Inject
    MarketTplClient marketTplClient;
    @Inject
    MockSecurityDataService mockSecurityDataService;
    @Inject
    DistributionService distributionService;
    @Inject
    DistributionUtils distributionUtils;
    @Inject
    CustomerService customerService;
    @Inject
    ChatClientService chatClientService;
    TicketCategory ticketCategoryBeru;
    TicketCategory ticketCategoryB2bShopSupport;
    TicketCategory ticketCategoryCourierPlatform;
    TicketCategory ticketCategoryB2bSupplierShop;
    Service defaultChatService;
    Service courierPlatformChatService;
    Service b2bChatService;
    Service b2bSupplierChatService;
    Service b2bShopChatService;
    Chat defaultChat;
    Chat courierPlatformChat;
    Chat b2bChat;
    Chat b2bSupplierChat;
    Chat b2bShopChat;
    Team firstLineCourierPlatformChatTeam;
    Team secondLineCourierPlatformChatTeam;
    Team firstLineChatTeam;
    Team b2bChatTeam;
    Employee employee;
    Team b2bSupplierChatTeam;
    Team b2bShopChatTeam;
    Team courierPlatformChatTeam;
    Customer customer;
    Entity partner;
    @Inject
    private ChatBotClient chatBotClient;
    @Inject
    private SendCommentsStatusService sendCommentsStatusService;
    @Inject
    private FeedbackClient feedbackClient;
    @Inject
    private TimerTestUtils timerTestUtils;
    @Inject
    private ConfigurationService configurationService;
    @Inject
    private OmniChannelSettingsService omniChannelSettingsService;
    @Inject
    private CommentTestUtils commentTestUtils;

    private static Stream<Arguments> chatArgumentsForSurveyB2bAndB2cChats() {
        return Stream.of(
                arguments(
                        COURIER_PLATFORM_CHAT_ID,
                        COURIER_PLATFORM_CHAT,
                        COURIER_PLATFORM_CHAT,
                        Brands.COURIER_PLATFORM_SUPPORT,
                        Ticket.STATUS_CLOSED
                ),
                arguments(
                        COURIER_PLATFORM_CHAT_ID,
                        COURIER_PLATFORM_CHAT,
                        COURIER_PLATFORM_CHAT,
                        Brands.COURIER_PLATFORM_SUPPORT,
                        Ticket.STATUS_RESOLVED
                ),
                arguments(
                        DEFAULT_CHAT_ID,
                        DEFAULT_CHAT_SERVICE,
                        FIRST_LINE_CHAT_TEAM,
                        Brands.BERU,
                        Ticket.STATUS_CLOSED
                ),
                arguments(
                        DEFAULT_CHAT_ID,
                        DEFAULT_CHAT_SERVICE,
                        FIRST_LINE_CHAT_TEAM,
                        Brands.BERU,
                        Ticket.STATUS_RESOLVED
                ),
                arguments(
                        B2B_SHOP_CHAT_ID,
                        B2B_SHOP_CHAT_TEAM,
                        B2B_SHOP_CHAT_TEAM,
                        Brands.B2B_SHOP_SUPPORT,
                        Ticket.STATUS_CLOSED
                ),
                arguments(
                        B2B_SHOP_CHAT_ID,
                        B2B_SHOP_CHAT_TEAM,
                        B2B_SHOP_CHAT_TEAM,
                        Brands.B2B_SHOP_SUPPORT,
                        Ticket.STATUS_RESOLVED
                ),
                arguments(
                        B2B_SUPPLIER_CHAT_ID,
                        B2B_SUPPORT_CHAT_TEAM,
                        B2B_SUPPORT_CHAT_TEAM,
                        Brands.B2B_SUPPLIER_SUPPORT,
                        Ticket.STATUS_CLOSED
                ),
                arguments(
                        B2B_SUPPLIER_CHAT_ID,
                        B2B_SUPPORT_CHAT_TEAM,
                        B2B_SUPPORT_CHAT_TEAM,
                        Brands.B2B_SUPPLIER_SUPPORT,
                        Ticket.STATUS_RESOLVED
                )

        );
    }

    private static Stream<Arguments> chatArgumentsForCloseChatSupport() {
        return Stream.of(
                arguments(
                        B2B_SHOP_CHAT_ID,
                        B2B_SHOP_CHAT_TEAM,
                        B2bShopChatTicket.FQN,
                        B2B_SHOP_CHAT_TEAM,
                        false,
                        CLOSE_SHOP_CHAT_SUPPORT,
                        false
                ),
                arguments(
                        B2B_SHOP_CHAT_ID,
                        B2B_SHOP_CHAT_TEAM,
                        B2bShopChatTicket.FQN,
                        B2B_SHOP_CHAT_TEAM,
                        false,
                        CLOSE_SHOP_CHAT_SUPPORT,
                        true
                ),
                arguments(
                        B2B_SUPPLIER_CHAT_ID,
                        B2B_SUPPORT_CHAT_TEAM,
                        B2bSupplierChatTicket.FQN,
                        B2B_SUPPORT_CHAT_TEAM,
                        false,
                        CLOSE_SUPPLIER_CHAT_SUPPORT,
                        false
                ),
                arguments(
                        B2B_SUPPLIER_CHAT_ID,
                        B2B_SUPPORT_CHAT_TEAM,
                        B2bSupplierChatTicket.FQN,
                        B2B_SUPPORT_CHAT_TEAM,
                        false,
                        CLOSE_SUPPLIER_CHAT_SUPPORT,
                        true
                ),
                arguments(
                        B2B_SHOP_CHAT_ID,
                        B2B_SHOP_CHAT_TEAM,
                        B2bShopChatTicket.FQN,
                        B2B_SHOP_CHAT_TEAM,
                        true,
                        CLOSE_SHOP_CHAT_SUPPORT,
                        false
                ),
                arguments(
                        B2B_SHOP_CHAT_ID,
                        B2B_SHOP_CHAT_TEAM,
                        B2bShopChatTicket.FQN,
                        B2B_SHOP_CHAT_TEAM,
                        true,
                        CLOSE_SHOP_CHAT_SUPPORT,
                        true
                ),
                arguments(
                        B2B_SUPPLIER_CHAT_ID,
                        B2B_SUPPORT_CHAT_TEAM,
                        B2bSupplierChatTicket.FQN,
                        B2B_SUPPORT_CHAT_TEAM,
                        true,
                        CLOSE_SUPPLIER_CHAT_SUPPORT,
                        false
                ),
                arguments(
                        B2B_SUPPLIER_CHAT_ID,
                        B2B_SUPPORT_CHAT_TEAM,
                        B2bSupplierChatTicket.FQN,
                        B2B_SUPPORT_CHAT_TEAM,
                        true,
                        CLOSE_SUPPLIER_CHAT_SUPPORT,
                        true
                )
        );
    }

    private static Stream<Arguments> chatArgumentsForB2bAndB2cChats() {
        return Stream.of(
                arguments(
                        COURIER_PLATFORM_CHAT_ID,
                        COURIER_PLATFORM_CHAT,
                        COURIER_PLATFORM_CHAT,
                        Brands.COURIER_PLATFORM_SUPPORT,
                        null
                ),
                arguments(
                        DEFAULT_CHAT_ID,
                        DEFAULT_CHAT_SERVICE,
                        FIRST_LINE_CHAT_TEAM,
                        Brands.BERU,
                        null
                ),
                arguments(
                        B2B_SHOP_CHAT_ID,
                        B2B_SHOP_CHAT_TEAM,
                        B2B_SHOP_CHAT_TEAM,
                        Brands.B2B_SHOP_SUPPORT,
                        "account$shop"),
                arguments(
                        B2B_SUPPLIER_CHAT_ID,
                        B2B_SUPPORT_CHAT_TEAM,
                        B2B_SUPPORT_CHAT_TEAM,
                        Brands.B2B_SUPPLIER_SUPPORT,
                        "account$shop")
        );
    }

    private static Stream<Arguments> chatArgumentsForB2cChats() {
        return Stream.of(
                arguments(
                        COURIER_PLATFORM_CHAT_ID,
                        COURIER_PLATFORM_CHAT,
                        COURIER_PLATFORM_CHAT,
                        Brands.COURIER_PLATFORM_SUPPORT
                ),
                arguments(
                        DEFAULT_CHAT_ID,
                        DEFAULT_CHAT_SERVICE,
                        FIRST_LINE_CHAT_TEAM,
                        Brands.BERU
                )
        );
    }


    @BeforeEach
    public void setUp() {
        ticketCategoryBeru = ticketTestUtils.createTicketCategory(ticketTestUtils.createBrand(Brands.BERU));
        ticketCategoryB2bSupplierShop =
                ticketTestUtils.createTicketCategory(ticketTestUtils.createBrand(Brands.B2B_SUPPLIER_SUPPORT));
        ticketCategoryB2bShopSupport =
                ticketTestUtils.createTicketCategory(ticketTestUtils.createBrand(Brands.B2B_SHOP_SUPPORT));
        ticketCategoryCourierPlatform =
                ticketTestUtils.createTicketCategory(ticketTestUtils.createBrand(Brands.COURIER_PLATFORM_SUPPORT));

        firstLineChatTeam = chatsTestUtils.createTeamIfNotExists(FIRST_LINE_CHAT_TEAM);
        firstLineCourierPlatformChatTeam = chatsTestUtils.createTeamIfNotExists(COURIER_PLATFORM_CHAT);
        secondLineCourierPlatformChatTeam = chatsTestUtils.createTeamIfNotExists("courierPlatformSecondLine");

        b2bChatTeam = chatsTestUtils.createTeamIfNotExists(B2B_CHAT_TEAM);
        b2bSupplierChatTeam = chatsTestUtils.createTeamIfNotExists(B2B_SUPPORT_CHAT_TEAM);
        b2bShopChatTeam = chatsTestUtils.createTeamIfNotExists(B2B_SHOP_CHAT_TEAM);

        defaultChatService = chatsTestUtils.createService(DEFAULT_CHAT_SERVICE, firstLineChatTeam, Brands.BERU);
        bcpService.edit(defaultChatService, Map.of(Service.CSAT_ENABLED, true));
        courierPlatformChatService = chatsTestUtils.createService(
                COURIER_PLATFORM_CHAT,
                firstLineCourierPlatformChatTeam,
                Brands.COURIER_PLATFORM_SUPPORT);
        bcpService.edit(courierPlatformChatService, Map.of(Service.CSAT_ENABLED, true));
        b2bChatService = chatsTestUtils.createService(
                B2B_CHAT_TEAM,
                b2bChatTeam,
                B2B_SHOP_SUPPORT_BRAND);
        b2bSupplierChatService = chatsTestUtils.createService(
                B2B_SUPPORT_CHAT_TEAM,
                b2bSupplierChatTeam,
                B2B_SHOP_SUPPORT_BRAND);
        bcpService.edit(b2bSupplierChatService, Map.of(Service.CSAT_ENABLED, true));
        b2bShopChatService = chatsTestUtils.createService(
                B2B_SHOP_CHAT_TEAM,
                b2bShopChatTeam,
                B2B_SHOP_SUPPORT_BRAND);
        bcpService.edit(b2bShopChatService, Map.of(Service.CSAT_ENABLED, true));

        defaultChat = createChat(DEFAULT_CHAT_ID, defaultChatService, BeruChatTicket.FQN);
        courierPlatformChat = createChat(COURIER_PLATFORM_CHAT_ID, courierPlatformChatService,
                CourierPlatformChatTicket.FQN);
        courierPlatformChatTeam = chatsTestUtils.createTeamIfNotExists(COURIER_PLATFORM_CHAT_TEAM);
        b2bChat = createChat(B2B_CHAT_ID, b2bChatService, B2bChatTicket.FQN);
        employee = createEmployee();
        b2bSupplierChat = createChat(B2B_SUPPLIER_CHAT_ID, b2bSupplierChatService, B2bSupplierChatTicket.FQN);

        b2bShopChat = createChat(B2B_SHOP_CHAT_ID, b2bShopChatService, B2bShopChatTicket.FQN);
        customer = createCustomer();

        partner = createAccount(Fqn.of("account$shop"));

        when(omniChannelSettingsService.getPossibleChannels(any(EmployeeDistributionStatus.class)))
                .thenAnswer(inv -> {
                    final List<Channel> channels = dbService.list(Query.of(Channel.FQN)
                            .withFilters(Filters.eq(Channel.ARCHIVED, false)));
                    return channels;
                });
        when(omniChannelSettingsService.getPossibleChannels(any(Ticket.class)))
                .thenAnswer(inv -> {
                    return dbService.list(Query.of(Channel.FQN).withFilters(Filters.eq(Channel.CODE, "chat")));
                });

        configurationService.setValue(ONE_SCRIPT_FOR_B2B_AND_B2C_ON_CREATE_CHAT_TICKET.key(), false);
    }

    @ParameterizedTest
    @MethodSource(value = "chatArgumentsForB2bAndB2cChats")
    @Description("""
            Проверка тест-кейса https://testpalm.yandex-team.ru/testcase/ocrm-775
            https://testpalm.yandex-team.ru/testcase/ocrm-933
            https://testpalm.yandex-team.ru/testcase/ocrm-1233
            Автоматическое снятие обращения с оператора из статуса Pending
            """)
    public void reopenTicketAndAddCommentWhenWaitForOperatorTooLongChat(String chatCode,
                                                                        String serviceCode,
                                                                        String teamCode,
                                                                        String brandCode) {
        Chat chat = createChat(chatCode);
        Team team = chatsTestUtils.createTeamIfNotExists(teamCode);
        Service service = chatsTestUtils.createService(serviceCode, team, brandCode);

        Employee currentEmployee = createEmployee(team, service);
        mockSecurityDataService.setCurrentEmployee(currentEmployee);


        ChatBotResponse response = new ChatBotResponse(true, null, Collections.emptyList());
        Mockito.when(chatBotClient.sendMessage(any())).thenReturn(response);

        String conversationIdFirstTicket = Randoms.string();
        ChatTicket firstTicket = receiveMessage(
                chat,
                "Ничего не работает! Спасите!",
                service,
                team,
                conversationIdFirstTicket);
        String conversationIdSecondTicket = Randoms.string();
        ChatTicket secondTicket = receiveMessage(
                chat,
                "Ничего не работает! Спасите!",
                service,
                team,
                conversationIdSecondTicket);


        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        receivingAChatMessageFromClient(chat, conversationIdFirstTicket, "Нет, я подожду");
        receivingAChatMessageFromClient(chat, conversationIdSecondTicket, "Нет, я подожду");

        distributionService.doStart(currentEmployee);
        distributionService.doBreak(currentEmployee, EmployeeDistributionStatus.STATUS_NOT_READY);
        assertEquals(Ticket.STATUS_PENDING, secondTicket.getStatus());

        timerTestUtils.simulateTimerExpiration(secondTicket.getGid(), "reopenOnPendingTimer");
        TransactionSynchronizationUtils.triggerBeforeCommit(false);
        List<Comment> comments = commentTestUtils.getComments(secondTicket);

        assertEquals(Ticket.STATUS_REOPENED, secondTicket.getStatus());
        assertEquals(3, comments.size());
        assertEquals(InternalComment.FQN, comments.get(comments.size() - 1).getFqn());
        assertEquals("Обращение было автоматически переоткрыто, так как оператор " + currentEmployee.getTitle() +
                " не взял обращение в работу в отведенное время.", comments.get(comments.size() - 1).getBody());
    }

    @ParameterizedTest
    @MethodSource(value = "chatArgumentsForB2bAndB2cChats")
    @Description("""
            Проверка тест-кейса
            https://testpalm.yandex-team.ru/testcase/ocrm-752
            https://testpalm.yandex-team.ru/testcase/ocrm-931
            https://testpalm.yandex-team.ru/testcase/ocrm-1231
            Автоматическое решение чата ЕО, если клиент не отвечает
            """)
    public void checkingSolutionOfTicketWhenWaitingForALongResponseFromClientChat(
            String chatCode,
            String serviceCode,
            String teamCode,
            String brandCode,
            String partnerFQN
    ) {
        Chat chat = createChat(chatCode);
        Team team = chatsTestUtils.createTeamIfNotExists(teamCode);
        Service service = chatsTestUtils.createService(serviceCode, team, brandCode);
        TicketCategory ticketCategory = ticketTestUtils.createTicketCategory(ticketTestUtils.createBrand(brandCode));

        Employee currentEmployee = createEmployee(team, service);
        mockSecurityDataService.setCurrentEmployee(currentEmployee);
        distributionService.doStart(currentEmployee);

        ChatBotResponse response = new ChatBotResponse(true, null, Collections.emptyList());
        Mockito.when(chatBotClient.sendMessage(any())).thenReturn(response);

        String conversationId = Randoms.string();
        ChatTicket ticket = receiveMessage(
                chat,
                "Ничего не работает! Спасите!",
                service,
                team,
                conversationId);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        distributionUtils.getTicket();

        bcpService.edit(ticket, Map.of(Ticket.CATEGORIES, ticketCategory));
        if (partnerFQN != null) {
            Entity partner = createAccount(Fqn.of(partnerFQN));
            bcpService.edit(ticket, Map.of(B2bTicket.PARTNER, partner));
        }
        ticketTestUtils.editTicketStatus(ticket, Ticket.STATUS_WAITING_RESPONSE);

        timerTestUtils.simulateTimerExpiration(ticket.getGid(), "waitingResponseTimer");
        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        assertEquals(Ticket.RESOLVED, ticket.getStatus());

    }

    @Test
    @Description("Проверка отправки сообщения Ищем оператора - https://testpalm.yandex-team.ru/testcase/ocrm-751")
    public void sendMessageAboutSearchingOfAvailableOperatorToChatB2c() {
        ChatBotResponse response = new ChatBotResponse(true, null, Collections.emptyList());
        Mockito.when(chatBotClient.sendMessage(any())).thenReturn(response);

        String conversationId = Randoms.string();
        ChatTicket ticket = receiveMessage(
                defaultChat,
                "Ничего не работает! Спасите!",
                defaultChatService,
                firstLineChatTeam,
                conversationId);

        timerTestUtils.simulateTimerExpiration(ticket.getGid(), "allowanceTakingTimer");

        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        verify(feedbackClient, Mockito.times(1)).pushMessage(any(), any());
    }

    @ParameterizedTest
    @MethodSource(value = "chatArgumentsForB2bAndB2cChats")
    @Description("""
            тест-кейсы:
            https://testpalm.yandex-team.ru/testcase/ocrm-744
            https://testpalm.yandex-team.ru/testcase/ocrm-927
            Проверка работы вывода уведомления печатания сообщения клиентом
            """)
    public void outputOfThePrintNotificationByTheClient(String chatCode,
                                                        String serviceCode,
                                                        String teamCode,
                                                        String brandCode) {
        Chat chat = createChat(chatCode);
        Team team = chatsTestUtils.createTeamIfNotExists(teamCode);
        Service service = chatsTestUtils.createService(serviceCode, team, brandCode);

        ChatBotResponse response = new ChatBotResponse(true, null, Collections.emptyList());
        Mockito.when(chatBotClient.sendMessage(any())).thenReturn(response);

        String conversationId = Randoms.string();
        ChatTicket ticket = receiveMessage(
                chat,
                "Ничего не работает! Спасите!",
                service,
                team,
                conversationId);

        ChatMessageRequest incomingChatMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                new TypingMessage(conversationId, null, null),
                new CustomFrom("avatarId", "displayName")
        );

        chatsService.receiveMessage(chat, incomingChatMessageRequest);

        verify(sendCommentsStatusService, Mockito.times(1)).sendCommentTyping(ticket);
    }

    @Test
    @Description("Проверка создания обращения из чатов курьера - https://testpalm.yandex-team.ru/testcase/ocrm-916")
    public void receiveMessageFromCourierPlatform() {
        String conversationId = UUID.randomUUID().toString();
        ChatTicket ticket = receiveMessage(
                courierPlatformChat,
                "Ничего не работает! Спасите!",
                courierPlatformChatService,
                firstLineCourierPlatformChatTeam,
                conversationId);

        assertThat(ticket.getStatus()).isEqualTo(STATUS_REGISTERED);
        assertThat(ticket.getMetaclass().getFqn()).isEqualTo(CourierPlatformChatTicket.FQN);

        ChatTicket ticketAfterSecondMessage = receiveMessage(
                courierPlatformChat,
                "up",
                courierPlatformChatService,
                firstLineCourierPlatformChatTeam,
                conversationId);

        Assertions.assertEquals(ticket, ticketAfterSecondMessage, "Не должен создаваться новый тикет при новом " +
                "сообщение из того же чата");

        List<Entity> chatComments = getChatCommentsByTicket(ticket);
        Assertions.assertEquals(2, chatComments.size());
    }

    @Test
    @Description("Отправка пушей в приложение курьеров - https://testpalm.yandex-team.ru/testcase/ocrm-1047")
    public void sendPushToCourierPlatformOnNewCommentCourierPlatformChat() {
        Mockito.when(marketTplClient.sendPushNotification(any())).thenReturn(Result.newResult(null));
        Mockito.when(customerService.getCustomerByUid(anyLong())).thenReturn(customer);
        String conversationId = UUID.randomUUID().toString();

        ChatConversation conversation = bcpService.create(ChatConversation.FQN, Map.of(
                ChatConversation.CHAT, courierPlatformChat,
                ChatConversation.CONVERSATION_ID, conversationId,
                ChatConversation.PUID, customer.getUid()

        ));

        ChatTicket ticket = receiveMessage(
                courierPlatformChat,
                "Ничего не работает! Спасите!",
                courierPlatformChatService,
                firstLineCourierPlatformChatTeam,
                conversationId);

        createComment(ticket, employee);

        verify(marketTplClient).sendPushNotification(conversation.getPuid());
    }

    @Test
    public void escalateAndDeescalateTicketInCourierPlatformChat() {
        Employee firstLineCourierEmployee =
                createEmployee(firstLineCourierPlatformChatTeam, courierPlatformChatService);
        mockSecurityDataService.setCurrentEmployee(firstLineCourierEmployee);
        distributionService.doStart(firstLineCourierEmployee);

        ChatTicket ticket = receiveMessage(
                courierPlatformChat,
                "Ничего не работает! Спасите!",
                courierPlatformChatService,
                firstLineCourierPlatformChatTeam,
                UUID.randomUUID().toString());

        distributionUtils.getTicket();

        assertEquals(firstLineCourierEmployee, ticket.getResponsibleEmployee());
        createComment(ticket, firstLineCourierEmployee);

        changeTicketStatus(ticket, STATUS_ESCALATED);
        assertEquals(STATUS_REOPENED, ticket.getStatus());

        Employee secondLineCourierEmployee =
                createEmployee(secondLineCourierPlatformChatTeam, courierPlatformChatService);
        mockSecurityDataService.setCurrentEmployee(secondLineCourierEmployee);
        distributionService.doStart(secondLineCourierEmployee);
        distributionUtils.getTicket();

        assertEquals(secondLineCourierEmployee, ticket.getResponsibleEmployee());
    }

    @Test
    @Description("""
            Получение данных клиента из сервисного сообщения - https://testpalm.yandex-team.ru/testcase/ocrm-1039
            """)
    @Transactional
    public void ticketClientInfoUpdatedByServiceMessage() {
        ChatBotResponse response = new ChatBotResponse(false, null, Collections.emptyList());
        Mockito.when(chatBotClient.sendMessage(any())).thenReturn(response);
        String conversationId = UUID.randomUUID().toString();
        ChatTicket ticket = receiveMessage(courierPlatformChat, "Где мой заказ?!", courierPlatformChatService,
                courierPlatformChatTeam, conversationId);
        assertThat(ticket.getMetaclass().getFqn()).isEqualTo(CourierPlatformChatTicket.FQN);

        Mockito.when(chatClientService.getByUid(anyLong())).thenReturn(new ChatClient(customer.getDisplayName(), null));
        Mockito.when(customerService.getCustomerByUid(anyLong())).thenReturn(customer);

        receiveServiceMessage(courierPlatformChat, ticket.getChatConversation().getConversationId(), null);
        CourierPlatformChatTicket chatTicket = (CourierPlatformChatTicket) getChatTicket(ticket.getChatConversation());

        assertEquals(customer.getEmail().getNormalizedAddress(), chatTicket.getClientEmail());
        assertEquals(customer.getUid(), chatTicket.getClientUid());
        assertEquals(customer.getPhone(), chatTicket.getClientPhone());
        assertEquals(customer.getDisplayName(), chatTicket.getClientName());
    }

    @ParameterizedTest
    @MethodSource(value = "chatArgumentsForB2bAndB2cChats")
    @Description("""
            Тест-кейс
            - https://testpalm.yandex-team.ru/testcase/ocrm-782
            - https://testpalm.yandex-team.ru/testcase/ocrm-934
            Сообщение о присоединении оператора к чату (есть псевдоним)
            """)
    public void sendMessageWhenOperatorWithAnAliasStartProcessingChat(String chatCode,
                                                                      String serviceCode,
                                                                      String teamCode,
                                                                      String brandCode) {
        Chat chat = createChat(chatCode);
        Team team = chatsTestUtils.createTeamIfNotExists(teamCode);
        Service service = chatsTestUtils.createService(serviceCode, team, brandCode);

        Employee currentEmployee = createEmployee(team, service);
        mockSecurityDataService.setCurrentEmployee(currentEmployee);
        bcpService.edit(currentEmployee.getGid(), Map.of(Employee.ALIAS, Randoms.string()));


        ChatBotResponse response = new ChatBotResponse(true, null, Collections.emptyList());
        Mockito.when(chatBotClient.sendMessage(any())).thenReturn(response);

        String conversationId = Randoms.string();
        ChatTicket ticket = receiveMessage(
                chat,
                "Ничего не работает! Спасите!",
                service,
                team,
                conversationId);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        receivingAChatMessageFromClient(chat, conversationId, "Нет, я подожду");

        distributionService.doStart(currentEmployee);
        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        assertEquals(currentEmployee, ticket.getResponsibleEmployee());

        var captor = ArgumentCaptor.forClass(ChatMessageRequest.class);
        int containsMessage = 1;
        if (chatCode.contains("b2b")) {
            containsMessage = 2;
        }
        verify(feedbackClient, Mockito.times(containsMessage)).pushMessage(any(), captor.capture());
        var systemMessageRequest = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertEqualsTextComment(currentEmployee.getAlias(), systemMessageRequest);
    }

    @ParameterizedTest
    @MethodSource(value = "chatArgumentsForB2bAndB2cChats")
    @Description("""
            Тест-кейс - https://testpalm.yandex-team.ru/testcase/ocrm-784
            https://testpalm.yandex-team.ru/testcase/ocrm-936
            Сообщение о присоединении оператора к чату (есть псевдоним)
            """)
    public void sendMessageWhenOperatorWithoutAnAliasStartProcessingChat(String chatCode,
                                                                         String serviceCode,
                                                                         String teamCode,
                                                                         String brandCode) {
        Chat chat = createChat(chatCode);
        Team team = chatsTestUtils.createTeamIfNotExists(teamCode);
        Service service = chatsTestUtils.createService(serviceCode, team, brandCode);

        Employee currentEmployee = createEmployee(team, service);
        mockSecurityDataService.setCurrentEmployee(currentEmployee);

        ChatBotResponse response = new ChatBotResponse(true, null, Collections.emptyList());
        Mockito.when(chatBotClient.sendMessage(any())).thenReturn(response);

        String conversationId = Randoms.string();
        ChatTicket ticket = receiveMessage(
                chat,
                "Ничего не работает! Спасите!",
                service,
                team,
                conversationId);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);
        receivingAChatMessageFromClient(chat, conversationId, "Нет, я подожду");

        distributionService.doStart(currentEmployee);
        TransactionSynchronizationUtils.triggerBeforeCommit(false);


        assertEquals(currentEmployee, ticket.getResponsibleEmployee());
        int countSystemMessage = 1;
        if (chatCode.contains("b2b")) {
            countSystemMessage = 2;
        }

        var captor = ArgumentCaptor.forClass(ChatMessageRequest.class);
        verify(feedbackClient, Mockito.times(countSystemMessage)).pushMessage(any(), captor.capture());
        var systemMessageRequest = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertEqualsTextComment("Оператор", systemMessageRequest);
    }


    @ParameterizedTest
    @MethodSource(value = "chatArgumentsForB2bAndB2cChats")
    @Description("""
            Тест-кейс
            - https://testpalm.yandex-team.ru/testcase/ocrm-783
            - https://testpalm.yandex-team.ru/testcase/ocrm-935
            Сообщение о присоединении оператора к чату (есть псевдоним)
            """)
    public void sendOnlyOneMessageWhenOperatorStartProcessingChat(String chatCode,
                                                                  String serviceCode,
                                                                  String teamCode,
                                                                  String brandCode) {
        Chat chat = createChat(chatCode);
        Team team = chatsTestUtils.createTeamIfNotExists(teamCode);
        Service service = chatsTestUtils.createService(serviceCode, team, brandCode);
        TicketCategory ticketCategory = ticketTestUtils.createTicketCategory(ticketTestUtils.createBrand(brandCode));
        Employee currentEmployee = createEmployee(team, service);
        mockSecurityDataService.setCurrentEmployee(currentEmployee);

        ChatBotResponse response = new ChatBotResponse(true, null, Collections.emptyList());
        Mockito.when(chatBotClient.sendMessage(any())).thenReturn(response);

        String conversationId = Randoms.string();
        ChatTicket ticket = receiveMessage(
                chat,
                "Ничего не работает! Спасите!",
                service,
                team,
                conversationId);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        receivingAChatMessageFromClient(chat, conversationId, "Нет, я подожду");

        distributionService.doStart(currentEmployee);
        distributionService.doBreak(currentEmployee, EmployeeDistributionStatus.STATUS_NOT_READY);
        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        assertEquals(currentEmployee, ticket.getResponsibleEmployee());

        bcpService.edit(ticket, Map.of(Ticket.CATEGORIES, ticketCategory));
        ticketTestUtils.editTicketStatus(ticket, Ticket.STATUS_RESOLVED);

        assertNull(ticket.getResponsibleEmployee());

        ticketTestUtils.editTicketStatus(ticket, Ticket.STATUS_REOPENED);

        distributionService.doStart(currentEmployee);
        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        assertEquals(currentEmployee, ticket.getResponsibleEmployee());

        int countSystemMessage = 1;
        if (chatCode.contains("b2b")) {
            countSystemMessage = 2;
        }

        verify(feedbackClient, Mockito.times(countSystemMessage)).pushMessage(any(), any());

    }

    @ParameterizedTest
    @MethodSource(value = "chatArgumentsForB2bAndB2cChats")
    @Description("Обращение беру чата закрывается через 2 минуты после решения")
    public void closingChatTicketAfterResolved(String chatCode,
                                               String serviceCode,
                                               String teamCode,
                                               String brandCode) {
        Chat chat = createChat(chatCode);
        Team team = chatsTestUtils.createTeamIfNotExists(teamCode);
        Service service = chatsTestUtils.createService(serviceCode, team, brandCode);

        TicketCategory ticketCategory = ticketTestUtils.createTicketCategory(ticketTestUtils.createBrand(brandCode));
        Employee currentEmployee = createEmployee(team, service);
        mockSecurityDataService.setCurrentEmployee(currentEmployee);

        ChatBotResponse response = new ChatBotResponse(true, null, Collections.emptyList());
        Mockito.when(chatBotClient.sendMessage(any())).thenReturn(response);

        ChatTicket ticket = receiveMessage(
                chat,
                "Ничего не работает! Спасите!",
                service,
                team,
                Randoms.string());

        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        distributionService.doStart(currentEmployee);
        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        bcpService.edit(ticket, Map.of(Ticket.CATEGORIES, ticketCategory));
        ticketTestUtils.editTicketStatus(ticket, Ticket.STATUS_RESOLVED);
        assertEquals(Ticket.STATUS_RESOLVED, ticket.getStatus());

        timerTestUtils.simulateTimerExpiration(ticket.getGid(), "allowanceCloseTimer");
        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        assertEquals(Ticket.STATUS_CLOSED, ticket.getStatus());
    }

    @ParameterizedTest
    @MethodSource(value = "chatArgumentsForB2cChats")
    @Description("Тест-кейс - https://testpalm.yandex-team.ru/testcase/ocrm-1094" +
            "Сообщение о присоединении оператора к чату (есть псевдоним)")
    public void sendMessageWhenOperatorWithAnAliasStartProcessingChatV2(String chatCode,
                                                                        String serviceCode,
                                                                        String teamCode,
                                                                        String brandCode) {
        Chat chat = createChat(chatCode);
        Team team = chatsTestUtils.createTeamIfNotExists(teamCode);
        Service service = chatsTestUtils.createService(serviceCode, team, brandCode);

        Employee currentEmployee = createEmployee(team, service);
        mockSecurityDataService.setCurrentEmployee(currentEmployee);
        bcpService.edit(currentEmployee, Map.of(Employee.ALIAS, Randoms.string()));

        ChatBotResponse response = new ChatBotResponse(true, null, Collections.emptyList());
        Mockito.when(chatBotClient.sendMessage(any())).thenReturn(response);

        String conversationIdFirstTicket = Randoms.string();
        ChatTicket firstTicket = receiveMessage(
                chat,
                "Ничего не работает! Спасите!",
                service,
                team,
                conversationIdFirstTicket);
        String conversationIdSecondTicket = Randoms.string();
        ChatTicket secondTicket = receiveMessage(
                chat,
                "Ничего не работает! Спасите!",
                service,
                team,
                conversationIdSecondTicket);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        receivingAChatMessageFromClient(chat, conversationIdFirstTicket, "Нет, я подожду");
        receivingAChatMessageFromClient(chat, conversationIdSecondTicket, "Нет, я подожду");
        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        distributionService.doStart(currentEmployee);

        String conversationIdThirdTicket = Randoms.string();
        ChatTicket thirdTicket = receiveMessage(
                chat,
                "Ничего не работает! Спасите!",
                service,
                team,
                conversationIdThirdTicket);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        receivingAChatMessageFromClient(chat, conversationIdThirdTicket, "Нет, я подожду");

        assertEquals(currentEmployee, firstTicket.getResponsibleEmployee());
        assertEquals(currentEmployee, secondTicket.getResponsibleEmployee());
        assertEquals(currentEmployee, thirdTicket.getResponsibleEmployee());

        var captor = ArgumentCaptor.forClass(ChatMessageRequest.class);
        verify(feedbackClient, Mockito.times(3)).pushMessage(any(), captor.capture());
        var systemMessageRequestList = captor.getAllValues();
        for (var systemMessageRequest : systemMessageRequestList) {
            assertEqualsTextComment(currentEmployee.getAlias(), systemMessageRequest);
        }

    }

    @ParameterizedTest
    @MethodSource(value = "chatArgumentsForB2cChats")
    @Description("""
            Проверка тест-кейса
            - https://testpalm.yandex-team.ru/testcase/ocrm-754
            - https://testpalm.yandex-team.ru/testcase/ocrm-932
            Автоматическое снятие обращения с оператора из статуса waitForOperator
            """)
    public void checkingReopenOfTicketWhenWaitingForALongResponseFromClientChat(String chatCode,
                                                                                String serviceCode,
                                                                                String teamCode,
                                                                                String brandCode) {
        Chat chat = createChat(chatCode);
        Team team = chatsTestUtils.createTeamIfNotExists(teamCode);
        Service service = chatsTestUtils.createService(serviceCode, team, brandCode);
        Employee currentEmployee = createEmployee(team, service);

        mockSecurityDataService.setCurrentEmployee(currentEmployee);
        ChatBotResponse response = new ChatBotResponse(true, null, Collections.emptyList());
        when(chatBotClient.sendMessage(any())).thenReturn(response);

        ChatTicket firstTicket = receiveMessage(
                chat,
                "Ничего не работает! Спасите!",
                service,
                team,
                Randoms.string());

        ChatTicket secondTicket = receiveMessage(
                chat,
                "Ничего не работает! Спасите!",
                service,
                team,
                Randoms.string());

        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        distributionService.doStart(currentEmployee);
        distributionService.doBreak(currentEmployee, EmployeeDistributionStatus.STATUS_NOT_READY);
        assertEquals(Ticket.STATUS_PROCESSING, firstTicket.getStatus());
        ticketTestUtils.editTicketStatus(firstTicket, Ticket.STATUS_WAITING_RESPONSE);
        distributionUtils.getTicket();
        ticketTestUtils.editTicketStatus(firstTicket, ChatTicket.STATUS_WAIT_FOR_OPERATOR);
        timerTestUtils.simulateTimerExpiration(firstTicket.getGid(), "waitForOperatorTimer");
        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        List<Comment> comments = commentTestUtils.getComments(firstTicket);

        assertEquals(Ticket.STATUS_REOPENED, firstTicket.getStatus());
        assertEquals(2, comments.size());
        assertEquals(InternalComment.FQN, comments.get(1).getFqn());
        assertEquals(String.format("Обращение было автоматически переоткрыто, так как оператор %s не ответил клиенту " +
                "за отведенное время.", currentEmployee.getTitle()), comments.get(1).getBody());
    }

    @ParameterizedTest
    @MethodSource(value = "chatArgumentsForB2bAndB2cChats")
    @Description("""
            тест-кейс - https://testpalm.yandex-team.ru/testcase/ocrm-859
            тест-кейс - https://testpalm.yandex-team.ru/testcase/ocrm-925
            Не меняется ответственный обращения при отправке ответа клиенту
            """)
    public void responsiblePersonDoesNotChangeAfterResponseToClient(String chatCode,
                                                                    String serviceCode,
                                                                    String teamCode,
                                                                    String brandCode) {
        Chat chat = createChat(chatCode);
        Team team = chatsTestUtils.createTeamIfNotExists(teamCode);
        Service service = chatsTestUtils.createService(serviceCode, team, brandCode);
        TicketCategory ticketCategory = ticketTestUtils.createTicketCategory(ticketTestUtils.createBrand(brandCode));

        Employee firstEmployee = createEmployee(team, service);
        Employee secondEmployee = createEmployee(team, service);
        mockSecurityDataService.setCurrentEmployee(firstEmployee);


        ChatBotResponse response = new ChatBotResponse(true, null, Collections.emptyList());
        Mockito.when(chatBotClient.sendMessage(any())).thenReturn(response);

        String conversationIdFirstTicket = Randoms.string();
        String conversationIdSecondTicket = Randoms.string();
        ChatTicket firstTicket = receiveMessage(
                chat,
                "Ничего не работает! Спасите!",
                service,
                team,
                conversationIdFirstTicket);
        ChatTicket secondTicket = receiveMessage(
                chat,
                "Ничего не работает! Спасите!",
                service,
                team,
                conversationIdSecondTicket);


        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        receivingAChatMessageFromClient(chat, conversationIdFirstTicket, "Нет, я подожду");
        receivingAChatMessageFromClient(chat, conversationIdSecondTicket, "Нет, я подожду");

        distributionService.doStart(firstEmployee);
        bcpService.edit(firstTicket, Map.of(Ticket.CATEGORIES, ticketCategory));
        ticketTestUtils.editTicketStatus(firstTicket, Ticket.STATUS_WAITING_RESPONSE);
        TransactionSynchronizationUtils.triggerBeforeCommit(false);


        distributionUtils.getTicket();
        ticketTestUtils.editTicketStatus(firstTicket, ChatTicket.STATUS_WAIT_FOR_OPERATOR);
        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        distributionService.doStart(secondEmployee);

        assertEquals(Ticket.STATUS_PROCESSING, secondTicket.getStatus());
        assertEquals(ChatTicket.STATUS_WAIT_FOR_OPERATOR, firstTicket.getStatus());
        assertEquals(firstEmployee, secondTicket.getResponsibleEmployee());
        assertEquals(firstEmployee, firstTicket.getResponsibleEmployee());

    }

    @ParameterizedTest
    @MethodSource(value = "chatArgumentsForSurveyB2bAndB2cChats")
    @Description("""
            Тест-кейс - https://testpalm.yandex-team.ru/testcase/ocrm-1289
            Тест-кейс - https://testpalm.yandex-team.ru/testcase/ocrm-1293
            - https://testpalm.yandex-team.ru/testcase/ocrm-1291
            Отправка опроса в чатах покупок
            """)
    public void sendSurvey(String chatCode,
                           String serviceCode,
                           String teamCode,
                           String brandCode,
                           String statusCode) {
        Chat chat = createChat(chatCode);
        Team team = chatsTestUtils.createTeamIfNotExists(teamCode);
        Service service = chatsTestUtils.createService(serviceCode, team, brandCode);
        TicketCategory ticketCategory = ticketTestUtils.createTicketCategory(ticketTestUtils.createBrand(brandCode));

        Employee currentEmployee = createEmployee(team, service);
        mockSecurityDataService.setCurrentEmployee(currentEmployee);

        ChatBotResponse response = new ChatBotResponse(true, null, Collections.emptyList());
        Mockito.when(chatBotClient.sendMessage(any())).thenReturn(response);

        String conversationId = Randoms.string();
        ChatTicket ticket = receiveMessage(
                chat,
                "Ничего не работает! Спасите!",
                service,
                team,
                conversationId);

        receivingAChatMessageFromClient(chat, conversationId, "Нет, я подожду");
        TransactionSynchronizationUtils.triggerBeforeCommit(false);


        distributionService.doStart(currentEmployee);
        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        bcpService.edit(ticket, Map.of(Ticket.CATEGORIES, ticketCategory));
        ticketTestUtils.editTicketStatus(ticket, Ticket.STATUS_RESOLVED);
        if (statusCode.equals("closed")) {
            ticketTestUtils.editTicketStatus(ticket, statusCode);
        }
        assertEquals(statusCode, ticket.getStatus());

        timerTestUtils.simulateTimerExpiration(ticket.getGid(), "csatTimer");
        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        int countSystemMessage = 2;
        if (chatCode.contains("b2b")) {
            countSystemMessage = 3;
        }

        var captor = ArgumentCaptor.forClass(ChatMessageRequest.class);
        verify(feedbackClient, Mockito.times(countSystemMessage++)).pushMessage(any(), captor.capture());
        var systemMessageRequest = captor.getAllValues().get(captor.getAllValues().size() - 1);
        String firstMessageSent = ((PlainMessage) systemMessageRequest.getChatMessage()).getText().getMessageText();
        var buttons =
                ((PlainMessage) systemMessageRequest.getChatMessage()).getText().getReplyMarkup().getItems().get(0);
        assertEquals("Мы смогли решить ваш вопрос?", firstMessageSent);
        assertEquals("Да", buttons.get(0).getText());
        assertEquals("Нет", buttons.get(1).getText());

        receivingAChatMessageFromClient(chat, conversationId, "Да");
        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        List<Comment> comments = commentTestUtils.getComments(ticket);
        assertEquals("Клиент ответил на первый вопрос.", comments.get(comments.size() - 1).getBody());
        boolean b2bTicketIsClosed = chatCode.contains("b2b") && statusCode.equals("closed");

        verify(feedbackClient, Mockito.times(b2bTicketIsClosed ? (++countSystemMessage) : countSystemMessage++)).pushMessage(any(), captor.capture());
        systemMessageRequest = captor.getAllValues().get(captor.getAllValues().size() - 1);
        String secondMessageSent = ((PlainMessage) systemMessageRequest.getChatMessage()).getText().getMessageText();
        buttons = ((PlainMessage) systemMessageRequest.getChatMessage()).getText().getReplyMarkup().getItems().get(0);

        assertEquals("Насколько вам понравилось с нами общаться?", secondMessageSent);
        for (int i = 0; i < buttons.size(); i++) {
            assertEquals(String.valueOf(i + 1), buttons.get(i).getText());
        }

        receivingAChatMessageFromClient(chat, conversationId, "3");
        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        comments = commentTestUtils.getOrderedComments(ticket);
        assertEquals("Клиент ответил на второй вопрос. Отзыв учтен.", comments.get(comments.size() - 1).getBody());

        verify(feedbackClient, Mockito.times(b2bTicketIsClosed ? (countSystemMessage + 2) : countSystemMessage)).pushMessage(any(), captor.capture());
        systemMessageRequest = captor.getAllValues().get(captor.getAllValues().size() - 1);
        String thirdMessageSent = ((PlainMessage) systemMessageRequest.getChatMessage()).getText().getMessageText();
        assertEquals("Спасибо!", thirdMessageSent);
    }

    @Test
    @Description("""
            Тест-кейс - https://testpalm.yandex-team.ru/testcase/ocrm-1042
            Эскалация чата курьера
            """)
    public void escalatedCourierPlatformChat() {
        Employee currentEmployee = createEmployee(firstLineCourierPlatformChatTeam, courierPlatformChatService);
        mockSecurityDataService.setCurrentEmployee(currentEmployee);

        String conversationId = Randoms.string();
        ChatTicket ticket = receiveMessage(
                courierPlatformChat,
                "Ничего не работает! Спасите!",
                courierPlatformChatService,
                firstLineCourierPlatformChatTeam,
                conversationId);

        distributionService.doStart(currentEmployee);

        ticketTestUtils.editTicketStatus(ticket, ChatTicket.STATUS_ESCALATED);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        assertEquals(Ticket.STATUS_REOPENED, ticket.getStatus());
        assertNull(ticket.getResponsibleEmployee());
        assertEquals("courierPlatformSecondLine", ticket.getResponsibleTeam().getCode());
    }


    //////////////////////////// b2b chats ////////////////////////////

    @Test
    @Description("OCRM-1124 Создание b2b-чата - белые")

    public void сreationB2bShopChat() {
        String conversationId = UUID.randomUUID().toString();
        ChatTicket ticket = receiveMessage(
                b2bShopChat,
                "Помогите решить проблему. С ув. b2b магазин",
                b2bShopChatService,
                b2bShopChatTeam,
                conversationId);

        receivingAChatMessageFromClient(b2bShopChat, conversationId, "Нет, я подожду");


        assertThat(ticket.getStatus()).isEqualTo(STATUS_REOPENED);
        assertThat(ticket.getMetaclass().getFqn()).isEqualTo(B2bShopChatTicket.FQN);
        Assertions.assertEquals(b2bShopChatService, ticket.getService(),
                "Тикет должен был создаться на очередь " + b2bShopChatService);

        ChatTicket ticketAfterSecondMessage = receiveMessage(
                b2bShopChat,
                "up",
                b2bShopChatService,
                b2bShopChatTeam,
                conversationId);

        Assertions.assertEquals(ticket, ticketAfterSecondMessage, "Не должен создаваться новый тикет при новом " +
                "сообщение из того же чата");

        List<Entity> chatComments = getChatCommentsByTicket(ticket);
        Assertions.assertEquals(3, chatComments.size());
    }

    @ParameterizedTest
    @Description("Закрываем поддержку обращений")
    @MethodSource(value = "chatArgumentsForCloseChatSupport")
    public void closeChatSupport(String chatCode, String serviceCode, Fqn ticketType, String teamCode,
                                 boolean oneScriptForB2bAndB2c, Property<Boolean> property, boolean isTicketExist) {
        configurationService.setValue(ONE_SCRIPT_FOR_B2B_AND_B2C_ON_CREATE_CHAT_TICKET.key(), oneScriptForB2bAndB2c);

        String conversationId = UUID.randomUUID().toString();

        Team team = chatsTestUtils.createTeamIfNotExists(teamCode);
        Service service = chatsTestUtils.createService(serviceCode, team, B2B_SHOP_SUPPORT_BRAND);
        Chat chat = createChat(chatCode, service, ticketType);
        // если один скрипт и тикет не должен создаваться, тогда чат должен быть в архиве
        if (oneScriptForB2bAndB2c && !isTicketExist) {
            bcpService.edit(chat, Map.of(HasWorkflow.STATUS, Chat.Statuses.ARCHIVED));
        }

        if (!isTicketExist) {
            //Тикета нет - новый чат, закрываем сразу
            configurationService.setValue(property.key(), true);
        }

        ChatTicket ticket = receiveMessage(
                chat,
                "Помогите решить проблему. С ув. b2b магазин",
                service,
                team,
                conversationId,
                null,
                isTicketExist);

        if (!isTicketExist) {
            assertNull(ticket);
            return;
        }

        //Сюда приходим при условии, что тикет есть и мы должны уметь продолжать существующий чат.
        configurationService.setValue(property.key(), true);

        ChatTicket ticketAfterSecondMessage = receiveMessage(
                chat,
                "up",
                service,
                team,
                conversationId);

        assertThat(ticketAfterSecondMessage).isNotNull();
    }

    @Test
    @Description("Не должен созаться b2b-чат белый потому что время сообщение приходит не во время обслуживани")
    public void сreationB2bShopChatShouldNotWork() {

        setNoWorkingPeriodsForService(b2bShopChatService);

        String conversationId = UUID.randomUUID().toString();
        ChatTicket ticket = receiveMessage(
                b2bShopChat,
                "Помогите решить проблему. С ув. b2b магазин",
                b2bShopChatService,
                b2bShopChatTeam,
                conversationId,
                null,
                false);

        assertNull(ticket);
    }

    @Test

    @Description("Не должен созаться b2b-чат синий потому что время сообщение приходит не во время обслуживани")
    public void сreationB2bSupplierChatShouldNotWork() {

        setNoWorkingPeriodsForService(b2bSupplierChatService);

        String conversationId = UUID.randomUUID().toString();
        ChatTicket ticket = receiveMessage(
                b2bSupplierChat,
                "Помогите решить проблему. С ув. b2b магазин",
                b2bSupplierChatService,
                b2bSupplierChatTeam,
                conversationId,
                null,
                false);

    }

    @Test
    @Description("Ocrm-1125 Создание b2b-чата - синие")
    public void creationB2bSupplierChat() {
        String conversationId = UUID.randomUUID().toString();
        receiveMessage(
                b2bSupplierChat,
                "Помогите решить проблему. С ув. b2b поставщик",
                b2bSupplierChatService,
                b2bSupplierChatTeam,
                conversationId);

        ChatTicket ticket = receiveMessage(
                b2bSupplierChat,
                "Нет, я подожду",
                b2bSupplierChatService,
                b2bSupplierChatTeam,
                conversationId);

        assertThat(ticket.getStatus()).isEqualTo(STATUS_REOPENED);
        assertThat(ticket.getMetaclass().getFqn()).isEqualTo(B2bSupplierChatTicket.FQN);
        Assertions.assertEquals(b2bSupplierChatService,
                ticket.getService(), "Тикет должен был создаться на очередь " + b2bSupplierChatService);

        ChatTicket ticketAfterSecondMessage = receiveMessage(
                b2bSupplierChat,
                "up",
                b2bSupplierChatService,
                b2bSupplierChatTeam,
                conversationId);

        Assertions.assertEquals(ticket, ticketAfterSecondMessage, "Не должен создаваться новый тикет при новом " +
                "сообщение из того же чата");

        List<Entity> chatComments = getChatCommentsByTicket(ticket);
        Assertions.assertEquals(3, chatComments.size());
    }

    @Test

    @Description("Ocrm-1217 Определение партнёра при создании чата , партнер проставляется из сервисного сообщения " +
            "пришедшего после создания тикете")
    public void setPartnerAfterCreationTicket() {
        String conversationId = UUID.randomUUID().toString();
        receiveMessage(
                b2bSupplierChat,
                "Помогите решить проблему. С ув. b2b поставщик",
                b2bSupplierChatService,
                b2bSupplierChatTeam,
                conversationId);

        ChatTicket ticket = receiveMessage(
                b2bSupplierChat,
                "Нет, я подожду",
                b2bSupplierChatService,
                b2bSupplierChatTeam,
                conversationId);

        assertThat(ticket.getStatus()).isEqualTo(STATUS_REOPENED);
        assertThat(ticket.getMetaclass().getFqn()).isEqualTo(B2bSupplierChatTicket.FQN);
        Assertions.assertEquals(b2bSupplierChatService,
                ticket.getService(), "Тикет должен был создаться на очередь " + b2bSupplierChatService);

        ChatTicket ticketAfterSecondMessage = receiveMessage(
                b2bSupplierChat,
                "up",
                b2bSupplierChatService,
                b2bSupplierChatTeam,
                conversationId);

        Assertions.assertEquals(ticket, ticketAfterSecondMessage, "Не должен создаваться новый тикет при новом " +
                "сообщение из того же чата");

        List<Entity> chatComments = getChatCommentsByTicket(ticket);
        Assertions.assertEquals(3, chatComments.size());

        Entity partner = bcpService.create(Fqn.of("account$shop"), Map.of(
                "title", "Test Shop",
                "shopId", "111111",
                "emails", Arrays.asList("test1@ya.ru"),
                "campaignId", "1000661967"
        ));

        Mockito.when(chatClientService.getByUid(anyLong())).thenReturn(new ChatClient(customer.getDisplayName(), null));
        Mockito.when(customerService.getCustomerByUid(anyLong())).thenReturn(customer);

        receiveServiceMessage(defaultChat, ticket.getChatConversation().getConversationId(),
                "https://partner-front--marketpartner-22893.demofslb.market.yandex.ru/supplier/1000661967/onboarding");
        assertEquals(partner, dbService.get(ticket.getGid()).getAttribute("partner"));
    }

    @Test
    @Description("Ocrm-1217 Определение партнёра при создании чата , партнер проставляется из сервисного сообщения " +
            "пришедшего до создания тикете")

    public void setPartnerBeforeCreationTicket() {
        String conversationId = UUID.randomUUID().toString();
        Mockito.when(customerService.getCustomerByUid(anyLong())).thenReturn(customer);
        Mockito.when(chatClientService.getByUid(anyLong())).thenReturn(new ChatClient(customer.getDisplayName(), null));

        Entity partner = bcpService.create(Fqn.of("account$shop"), Map.of(
                "title", "Test Shop",
                "shopId", "111111",
                "emails", Arrays.asList("test1@ya.ru"),
                "campaignId", "1000661967"
        ));

        receiveServiceMessage(b2bSupplierChat, conversationId,
                "https://partner-front--marketpartner-22893.demofslb.market.yandex.ru/supplier/1000661967/onboarding");

        receiveMessage(
                b2bSupplierChat,
                "Помогите решить проблему. С ув. b2b поставщик",
                b2bSupplierChatService,
                b2bSupplierChatTeam,
                conversationId);

        ChatTicket ticket = receiveMessage(
                b2bSupplierChat,
                "Нет, я подожду",
                b2bSupplierChatService,
                b2bSupplierChatTeam,
                conversationId);

        assertThat(ticket.getStatus()).isEqualTo(STATUS_REOPENED);
        assertThat(ticket.getMetaclass().getFqn()).isEqualTo(B2bSupplierChatTicket.FQN);

        List<Entity> chatComments = getChatCommentsByTicket(ticket);
        Assertions.assertEquals(2, chatComments.size());

        assertEquals(partner, dbService.get(ticket.getGid()).getAttribute("partner"));
    }

    @Test
    @Description("Определение партнёра при создании обращения в мобильном приложении для продавцов." +
            " Партнер проставляется из plain сообщения, содержащего context")
    public void setPartnerToTicketFromContext() {
        String conversationId = UUID.randomUUID().toString();
        String campaignId = "1000661967";

        Mockito.when(customerService.getCustomerByUid(anyLong())).thenReturn(customer);
        Mockito.when(chatClientService.getByUid(anyLong())).thenReturn(new ChatClient(customer.getDisplayName(), null));
        Entity partner = bcpService.create(Fqn.of("account$shop"), Map.of(
                "title", "Test Shop",
                "shopId", "111111",
                "emails", Arrays.asList("test1@ya.ru"),
                "campaignId", campaignId
        ));

        ObjectNode context = mapper.createObjectNode()
                .put("user_id", campaignId);
        ChatTicket ticket = receiveMessage(
                b2bSupplierChat,
                "Помогите решить проблему. С ув. b2b поставщик",
                b2bSupplierChatService,
                b2bSupplierChatTeam,
                conversationId,
                context
        );

        assertThat(ticket.getStatus()).isEqualTo(WAIT_ANSWER_FOR_QUESTION);
        assertThat(ticket.getMetaclass().getFqn()).isEqualTo(B2bSupplierChatTicket.FQN);
        Assertions.assertEquals(b2bSupplierChatService,
                ticket.getService(), "Тикет должен был создаться на очередь " + b2bSupplierChatService);

        ChatTicket ticketAfterSecondMessage = receiveMessage(
                b2bSupplierChat,
                "up",
                b2bSupplierChatService,
                b2bSupplierChatTeam,
                conversationId
        );

        Assertions.assertEquals(ticket, ticketAfterSecondMessage, "Не должен создаваться новый тикет при новом " +
                "сообщение из того же чата");

        List<Entity> chatComments = getChatCommentsByTicket(ticket);
        Assertions.assertEquals(2, chatComments.size());

        assertEquals(partner, dbService.get(ticket.getGid()).getAttribute("partner"));
    }

    @Test
    @Description("Ситуация когда в бэклоге лежит 2 тикеты а в конфигурации указано что максимальное количество может " +
            "быть -1, в итоге делаем отбивку. На первое сообщение поставщика пишем вопрос и получив ответ 'Нет' " +
            "заводим тикет")

    public void createSupplierChatTicketIfAnswerNo() {
        //наполним бэклог двумя задачами
        receiveMessage(b2bSupplierChat, "первый тикет в бэклог", b2bSupplierChatService, b2bSupplierChatTeam,
                UUID.randomUUID().toString());
        receiveMessage(b2bSupplierChat, "второй тикет в бэклог", b2bSupplierChatService, b2bSupplierChatTeam,
                UUID.randomUUID().toString());

        String conversationId = UUID.randomUUID().toString();
        ChatTicket chatTicket = receiveMessage(
                b2bSupplierChat,
                "Спасите, помогите",
                b2bSupplierChatService,
                b2bSupplierChatTeam,
                conversationId);
        assertThat(chatTicket.getStatus()).isEqualTo(WAIT_ANSWER_FOR_QUESTION);

        ChatTicket ticketAfterSecondMsg = receiveMessage(
                b2bSupplierChat,
                "Нет, я подожду",
                b2bSupplierChatService,
                b2bSupplierChatTeam,
                conversationId);
        assertThat(ticketAfterSecondMsg.getStatus()).isEqualTo(STATUS_REOPENED);
        assertFalse(ticketAfterSecondMsg.getArchived());

        List<Entity> chatComments = getChatCommentsByTicket(ticketAfterSecondMsg);
        Assertions.assertEquals(2, chatComments.size());
    }

    @Test
    @Description("Ситуация когда в бэклоге лежит 2 тикеты а в конфигурации указано что максимальное количество может " +
            "быть -1, в итоге делаем отбивку. На первое сообщение поставщика пишем вопрос и получив ответ 'Да' " +
            "заводим тикет со статусом missing")

    public void createSupplierChatTicketIfAnswerYes() {
        //наполним бэклог двумя задачами
        receiveMessage(b2bSupplierChat, "первый тикет в бэклог", b2bSupplierChatService, b2bSupplierChatTeam,
                UUID.randomUUID().toString());
        receiveMessage(b2bSupplierChat, "второй тикет в бэклог", b2bSupplierChatService, b2bSupplierChatTeam,
                UUID.randomUUID().toString());

        String conversationId = UUID.randomUUID().toString();
        ChatTicket chatTicket = receiveMessage(
                b2bSupplierChat,
                "Спасите, помогите",
                b2bSupplierChatService,
                b2bSupplierChatTeam,
                conversationId);
        assertThat(chatTicket.getStatus()).isEqualTo(WAIT_ANSWER_FOR_QUESTION);

        ChatTicket ticketAfterSecondMsg = receiveMessage(
                b2bSupplierChat,
                "Да",
                b2bSupplierChatService,
                b2bSupplierChatTeam,
                conversationId);
        assertThat(ticketAfterSecondMsg.getStatus()).isEqualTo(STATUS_MISSING);
        assertTrue(ticketAfterSecondMsg.getArchived());

        List<Entity> chatComments = getChatCommentsByTicket(ticketAfterSecondMsg);
        Assertions.assertEquals(2, chatComments.size());
    }

    @Test
    @Description("Ситуация когда в бэклоге лежит 2 тикеты а в конфигурации указано что максимальное количество может " +
            "быть -1, в итоге делаем отбивку. На первое сообщение белого магазина пишем вопрос и получив ответ 'Нет' " +
            "заводим тикет")

    public void createShopChatTicketIfAnswerNo() {
        //наполним бэклог двумя задачами
        receiveMessage(b2bShopChat, "первый тикет в бэклог", b2bShopChatService, b2bShopChatTeam,
                UUID.randomUUID().toString());
        receiveMessage(b2bShopChat, "второй тикет в бэклог", b2bShopChatService, b2bShopChatTeam,
                UUID.randomUUID().toString());

        String conversationId = UUID.randomUUID().toString();
        ChatTicket chatTicket = receiveMessage(
                b2bShopChat,
                "Спасите, помогите",
                b2bShopChatService,
                b2bShopChatTeam,
                conversationId);
        assertThat(chatTicket.getStatus()).isEqualTo(WAIT_ANSWER_FOR_QUESTION);

        ChatTicket ticketAfterSecondMsg = receiveMessage(
                b2bShopChat,
                "Нет, я подожду",
                b2bShopChatService,
                b2bShopChatTeam,
                conversationId);
        assertThat(ticketAfterSecondMsg.getStatus()).isEqualTo(STATUS_REOPENED);
        assertFalse(ticketAfterSecondMsg.getArchived());

        List<Entity> chatComments = getChatCommentsByTicket(ticketAfterSecondMsg);
        Assertions.assertEquals(2, chatComments.size());
    }

    @Test
    @Description("Ситуация когда в бэклоге лежит 2 тикеты а в конфигурации указано что максимальное количество может " +
            "быть -1, в итоге делаем отбивку. На первое сообщение белого магазина пишем вопрос и получив ответ 'Да' " +
            "заводим тикет со статусом missing")

    public void createShopChatTicketIfAnswerYes() {
        //наполним бэклог двумя задачами
        receiveMessage(b2bShopChat, "первый тикет в бэклог", b2bShopChatService, b2bShopChatTeam,
                UUID.randomUUID().toString());
        receiveMessage(b2bShopChat, "второй тикет в бэклог", b2bShopChatService, b2bShopChatTeam,
                UUID.randomUUID().toString());

        String conversationId = UUID.randomUUID().toString();
        ChatTicket chatTicket = receiveMessage(
                b2bShopChat,
                "Спасите, помогите",
                b2bShopChatService,
                b2bShopChatTeam,
                conversationId);
        assertThat(chatTicket.getStatus()).isEqualTo(WAIT_ANSWER_FOR_QUESTION);

        ChatTicket ticketAfterSecondMsg = receiveMessage(
                b2bShopChat,
                "Да",
                b2bShopChatService,
                b2bShopChatTeam,
                conversationId);
        assertThat(ticketAfterSecondMsg.getStatus()).isEqualTo(STATUS_MISSING);
        assertTrue(ticketAfterSecondMsg.getArchived());

        List<Entity> chatComments = getChatCommentsByTicket(ticketAfterSecondMsg);
        Assertions.assertEquals(2, chatComments.size());
    }

    @Test
    @Description("Создаем тикеты на обращения в синии очереди, так как число уже зарегистрированных тикетов не больше" +
            " максимально допустимого в параметре maxUndistributedTicketNumber")
    public void createB2bSupplierTicket() {

        String conversationId = UUID.randomUUID().toString();
        ChatTicket ticket = receiveMessage(
                b2bSupplierChat,
                "Помогите решить проблему. С ув. синий магазин",
                b2bSupplierChatService,
                b2bSupplierChatTeam,
                conversationId);
        receivingAChatMessageFromClient(b2bSupplierChat, conversationId, "Нет, я подожду");


        assertThat(ticket.getStatus()).isEqualTo(STATUS_REOPENED);
        assertThat(ticket.getMetaclass().getFqn()).isEqualTo(B2bSupplierChatTicket.FQN);
        Assertions.assertEquals(b2bSupplierChatService,
                ticket.getService(), "Тикет должен был создаться на очередь " + b2bSupplierChatService);

        ChatTicket ticketAfterSecondMessage = receiveMessage(
                b2bSupplierChat,
                "up",
                b2bSupplierChatService,
                b2bSupplierChatTeam,
                conversationId);

        Assertions.assertEquals(ticket, ticketAfterSecondMessage, "Не должен создаваться новый тикет при новом " +
                "сообщение из того же чата");

        List<Entity> chatComments = getChatCommentsByTicket(ticket);
        Assertions.assertEquals(3, chatComments.size());
    }

    @Test
    @Description("Ocrm-1294 Отправка опроса в чатах b2b")
    public void sendChatCsatRequestB2bChat() {

        bcpService.edit(b2bShopChatService, Map.of(
                "csatEnabled", true
        ));

        String conversationId = UUID.randomUUID().toString();
        ChatTicket ticket = receiveMessage(
                b2bShopChat,
                "Помогите решить проблему. С ув. b2b магазин",
                b2bShopChatService,
                b2bShopChatTeam,
                conversationId);
        receivingAChatMessageFromClient(b2bShopChat, conversationId, "Нет, я подожду");


        assertThat(ticket.getStatus()).isEqualTo(STATUS_REOPENED);
        assertThat(ticket.getMetaclass().getFqn()).isEqualTo(B2bShopChatTicket.FQN);
        Assertions.assertEquals(b2bShopChatService,
                ticket.getService(), "Тикет должен был создаться на очередь " + b2bShopChatService);

        ChatTicket ticketAfterSecondMessage = receiveMessage(
                b2bShopChat,
                "up",
                b2bShopChatService,
                b2bShopChatTeam,
                conversationId);

        receivingAChatMessageFromClient(b2bShopChat, conversationId, "Нет, я подожду");

        Assertions.assertEquals(ticket, ticketAfterSecondMessage, "Не должен создаваться новый тикет при новом " +
                "сообщение из того же чата");

        List<Entity> chatComments = getChatCommentsByTicket(ticket);
        Assertions.assertEquals(4, chatComments.size());


        bcpService.edit(b2bShopChatService, Map.of(
                "csatEnabled", false
        ));

    }

    @Test
    @Description("Проверка отправки сообщения Ищем оператора у обращений b2b")
    public void sendMessageAboutSearchingOfAvailableOperatorToChatB2B() {
        ChatBotResponse response = new ChatBotResponse(true, null, Collections.emptyList());
        Mockito.when(chatBotClient.sendMessage(any())).thenReturn(response);

        String conversationId = Randoms.string();
        ChatTicket ticket = receiveMessage(
                b2bSupplierChat,
                "Ничего не работает! Спасите!",
                b2bSupplierChatService,
                b2bSupplierChatTeam,
                conversationId);

        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        var chatMessageCaptor = ArgumentCaptor.forClass(ChatMessageRequest.class);
        verify(feedbackClient, times(1)).pushMessage(any(Chat.class), chatMessageCaptor.capture());

        var chatMessageRequest = chatMessageCaptor.getValue();

        var chatMessage = chatMessageRequest.getChatMessage();
        Assertions.assertEquals(PlainMessage.class, chatMessage.getClass());
        Assertions.assertEquals(
                "Мы отвечаем медленнее, чем обычно. Хотите, чтобы вам ответили быстрее?",
                ((PlainMessage) chatMessage).getText().getMessageText()
        );

        reset(feedbackClient);
        timerTestUtils.simulateTimerExpiration(ticket.getGid(), "allowanceTakingTimer");
        TransactionSynchronizationUtils.triggerBeforeCommit(false);

        chatMessageCaptor = ArgumentCaptor.forClass(ChatMessageRequest.class);
        verify(feedbackClient, times(1)).pushMessage(any(Chat.class), chatMessageCaptor.capture());

        chatMessageRequest = chatMessageCaptor.getValue();

        chatMessage = chatMessageRequest.getChatMessage();
        Assertions.assertEquals(SystemMessage.class, chatMessage.getClass());
        Assertions.assertEquals(
                "Мы ищем оператора для вас. Скоро ответим",
                ((SystemMessage) chatMessage).getMessageText()
        );
    }

    /**
     * Перекладывание чата курьеров (нельзя переложить обращение не сменив очередь)
     * https://testpalm2.yandex-team.ru/testcase/ocrm-1395
     */
    @ParameterizedTest(name = "Service was changed = {0}")
    @ValueSource(booleans = {false, true})
    public void serviceHasChangedTest(boolean isServiceChanged) {

        Employee firstLineCourierEmployee =
                createEmployee(firstLineCourierPlatformChatTeam, courierPlatformChatService);
        mockSecurityDataService.setCurrentEmployee(firstLineCourierEmployee);
        distributionService.doStart(firstLineCourierEmployee);

        ChatTicket ticket = receiveMessage(
                courierPlatformChat,
                "Переложи меня если сможешь",
                courierPlatformChatService,
                firstLineCourierPlatformChatTeam,
                UUID.randomUUID().toString());

        distributionUtils.getTicket();
        createComment(ticket, firstLineCourierEmployee);

        if (isServiceChanged) {
            Ticket changedTicket = bcpService.edit(ticket, Map.of(
                    Ticket.STATUS, ru.yandex.market.jmf.module.chat.Ticket.STATUS_SERVICE_HAS_CHANGED,
                    Ticket.SERVICE, DEFAULT_CHAT_SERVICE
            ));
            assertNotEquals(firstLineCourierEmployee, ticket.getResponsibleEmployee());
            assertEquals(Ticket.STATUS_REOPENED, changedTicket.getStatus());
        } else {
            assertThrows(ValidationException.class, () -> bcpService.edit(ticket,
                    Ticket.STATUS, ru.yandex.market.jmf.module.chat.Ticket.STATUS_SERVICE_HAS_CHANGED));
        }
    }

    private List<Entity> getChatCommentsByTicket(ChatTicket ticket) {
        Query query = Query.of(InChatComment.FQN).withFilters(Filters.eq("entity", ticket));
        return dbService.list(query);
    }

    private void receiveServiceMessage(Chat chat, String conversationId, String pageUrl) {
        ObjectNode customPayload = mapper.createObjectNode()
                .put("puid", customer.getUid())
                .put("pageUrl", pageUrl)
                .put("yuid", customer.getUid().toString());

        ChatMessageRequest incomingChatMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                new ServiceMessage(
                        conversationId,
                        false,
                        false,
                        customPayload),
                new CustomFrom("avatarId", "displayName")
        );

        chatsService.receiveMessage(chat, incomingChatMessageRequest);
    }

    private ChatTicket receiveMessage(Chat chat, String message, Service chatService, Team team,
                                      String conversationId) {
        return receiveMessage(chat, message, chatService, team, conversationId, null, true);
    }

    private ChatTicket receiveMessage(Chat chat, String message, Service chatService, Team team,
                                      String conversationId, ObjectNode context) {
        return receiveMessage(chat, message, chatService, team, conversationId, context, true);
    }

    private ChatTicket receiveMessage(Chat chat, String message, Service chatService, Team team,
                                      String conversationId, ObjectNode context, boolean shouldCreateTicket) {
        receivingAChatMessageFromClient(chat, conversationId, message, context);

        return assertThatChatTicketCreated(chatService, team, conversationId, shouldCreateTicket);
    }

    private void receivingAChatMessageFromClient(Chat chat, String conversationId, String message) {
        receivingAChatMessageFromClient(chat, conversationId, message, null);
    }

    private void receivingAChatMessageFromClient(Chat chat, String conversationId, String message, ObjectNode context) {
        String payloadId = UUID.randomUUID().toString();
        ChatMessageRequest incomingChatMessageRequest = new ChatMessageRequest(
                new ServerMessageInfo(Instant.now()),
                new PlainMessage(
                        conversationId,
                        false,
                        false,
                        payloadId,
                        new PlainMessageText(message, new ReplyMarkup(List.of())),
                        null,
                        null,
                        null,
                        null,
                        null,
                        context
                ),
                new CustomFrom("avatarId", "displayName")
        );

        chatsService.receiveMessage(chat, incomingChatMessageRequest);
    }

    private ChatTicket assertThatChatTicketCreated(Service chatService, Team team, String conversationId,
                                                   boolean shouldCreateTicket) {
        ChatConversation conversation =
                dbService.getByNaturalId(ChatConversation.FQN, ChatConversation.CONVERSATION_ID, conversationId);

        assertThat(conversation).isNotNull();

        List<InChatMessage> inMessages = dbService.list(Query.of(InChatMessage.FQN)
                .withFilters(
                        Filters.eq(InChatMessage.CONVERSATION, conversation.getConversationId())
                ));
        assertThat(inMessages).isNotEmpty();

        List<ChatTicket> tickets = dbService.list(Query.of(ChatTicket.FQN)
                .withFilters(
                        Filters.eq(ChatTicket.CHAT_CONVERSATION, conversation)
                )
        );

        if (!shouldCreateTicket) {
            assertThat(tickets).isEmpty();
            return null;
        }

        assertThat(tickets).isNotEmpty();
        ChatTicket ticket = tickets.iterator().next();
        assertThat(ticket.getService().getCode()).isEqualTo(chatService.getCode());
        assertThat(ticket.getService().getBrand().getCode()).isEqualTo(chatService.getBrand().getCode());
        assertThat(ticket.getService().getResponsibleTeam().getCode()).isEqualTo(team.getCode());

        return ticket;
    }

    private Chat createChat(String chatId) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(Chat.TITLE, Randoms.string());
        attributes.put(Chat.CHAT_ID, chatId);
        attributes.put(Chat.CODE, chatId);

        Chat existing = dbService.getByNaturalId(Chat.FQN, Chat.CODE, chatId);
        if (existing != null) {
            if (Chat.Statuses.ARCHIVED.equals(existing.getStatus())) {
                return bcpService.edit(existing, Map.of(HasWorkflow.STATUS, Chat.Statuses.ACTIVE));
            }
            return existing;
        }
        return bcpService.create(Chat.FQN, attributes);
    }

    private Chat createChat(String chatId, Service service, Fqn ticketType) {
        Chat chat = createChat(chatId);

        DirectBotChatSettings existing = dbService.getByNaturalId(DirectBotChatSettings.FQN,
                DirectBotChatSettings.CHAT, chat);
        if (existing != null) {
            return chat;
        }
        bcpService.create(DirectBotChatSettings.FQN, Map.of(
                DirectBotChatSettings.CHAT, chat,
                DirectBotChatSettings.SERVICE, service,
                DirectBotChatSettings.TICKET_TYPE, ticketType
        ));
        return chat;
    }

    private Employee createEmployee() {
        Entity ou = ouTestUtils.createOu();
        return (Employee) employeeTestUtils.createEmployee(ou, Map.of(
                Employee.CHAT_AVATAR_ID, "avatarId"
        ));
    }

    private Employee createEmployee(Team team, Service service) {
        final Ou ou = ouTestUtils.createOu();
        return bcpService.create(ru.yandex.market.jmf.module.ticket.Employee.FQN_DEFAULT, Map.of(
                ru.yandex.market.jmf.module.ticket.Employee.OU, ou,
                ru.yandex.market.jmf.module.ticket.Employee.TITLE, Randoms.string(),
                ru.yandex.market.jmf.module.ticket.Employee.UID, Randoms.longValue(),
                ru.yandex.market.jmf.module.ticket.Employee.TEAMS, Set.of(team),
                ru.yandex.market.jmf.module.ticket.Employee.SERVICES, Set.of(service)
        ));
    }

    private OutChatComment createComment(ChatTicket ticket, Employee employee) {
        Map<String, Object> props = new HashMap<>(Maps.of(
                Comment.ENTITY, ticket,
                Comment.BODY, "comment"
        ));
        Map<String, Object> attr = new HashMap<>(Maps.of(
                AUTHOR_ATTR, employee
        ));
        return bcpService.create(OutChatComment.FQN, props, attr);
    }

    private Customer createCustomer() {
        var customer = new Customer(999999999L);
        customer.setDisplayName("CUSTOMER_DISPLAY_NAME");
        customer.setEmail(new Email("email@yandex.ru"));
        customer.setFirstName("FIRST_NAME");
        customer.setLastName("LAST_NAME");
        customer.setPhone(Phone.fromRaw("+79920137256"));
        customer.setLogin("LOGIN");
        return customer;
    }

    private ChatTicket getChatTicket(ChatConversation conversation) {
        List<ChatTicket> tickets = dbService.list(Query.of(ChatTicket.FQN)
                .withFilters(
                        Filters.eq(ChatTicket.CHAT_CONVERSATION, conversation)
                )
        );
        assertThat(tickets).isNotEmpty();
        return tickets.iterator().next();
    }

    private void changeTicketStatus(Ticket ticket, String status) {
        bcpService.edit(ticket, Map.of(
                Ticket.STATUS, status
        ));
    }

    private void setNoWorkingPeriodsForService(Service service) {
        service.getServiceTime().getPeriods().forEach(period -> {
            if (LocalDateTime.now().getHour() > 12) {
                bcpService.edit(period, Map.of(
                        "startTime", "06:00:00",
                        "endTime", "06:00:01"
                ));
            } else {
                bcpService.edit(period, Map.of(
                        "startTime", "18:00:00",
                        "endTime", "18:00:01"
                ));
            }
        });
    }

    private void assertEqualsTextComment(String employeeAlias, ChatMessageRequest chatMessageRequest) {
        String textComment = ((SystemMessage) chatMessageRequest.getChatMessage()).getMessageText();
        assertEquals(String.format("%s присоединился к чату", employeeAlias), textComment);

    }

    private Entity createAccount(Fqn fqnAccount) {
        return bcpService.create(fqnAccount, Map.of(
                "title", Randoms.string(),
                "shopId", Randoms.intValue(),
                "emails", Arrays.asList("test1@ya.ru"),
                "campaignId", Randoms.intValue()
        ));
    }

}
