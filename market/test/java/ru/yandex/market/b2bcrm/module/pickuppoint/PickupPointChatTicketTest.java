package ru.yandex.market.b2bcrm.module.pickuppoint;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.b2bcrm.module.account.Account;
import ru.yandex.market.b2bcrm.module.account.Shop;
import ru.yandex.market.b2bcrm.module.pickuppoint.config.B2bPickupPointTestConfig;
import ru.yandex.market.b2bcrm.module.pickuppoint.config.B2bPickupPointTests;
import ru.yandex.market.b2bcrm.module.ticket.B2bTicket;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.logic.wf.WfService;
import ru.yandex.market.jmf.module.chat.Chat;
import ru.yandex.market.jmf.module.chat.ChatConversation;
import ru.yandex.market.jmf.module.chat.ModuleChatTestConfiguration;
import ru.yandex.market.jmf.module.ticket.Channel;
import ru.yandex.market.jmf.module.ticket.DistributionService;
import ru.yandex.market.jmf.module.ticket.Employee;
import ru.yandex.market.jmf.module.ticket.EmployeeDistributionStatus;
import ru.yandex.market.jmf.module.ticket.OmniChannelSettingsService;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.TicketCategory;
import ru.yandex.market.jmf.module.ticket.test.TestChannels;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.timings.test.impl.TimerTestUtils;
import ru.yandex.market.jmf.ui.UiUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@B2bPickupPointTests
@SpringJUnitConfig(classes = {
        ModuleChatTestConfiguration.class,
        B2bPickupPointTestConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class PickupPointChatTicketTest {
    private static final String CONVERSATION_ID = Randoms.string();
    private static final String CATEGORY_CODE = Randoms.string();
    private static final Duration DEFER_TIME = Duration.ofHours(1);
    @Inject
    private BcpService bcpService;
    @Inject
    private TicketTestUtils ticketTestUtils;
    @Inject
    private DistributionService distributionService;
    @Inject
    private DbService dbService;
    @Inject
    private OmniChannelSettingsService omniChannelSettingsService;
    @Inject
    private WfService wfService;
    @Inject
    private UiUtils uiUtils;

    @Inject
    private TimerTestUtils timerTestUtils;
    private TicketTestUtils.TestContext ctx;
    private Account partner;
    private TicketCategory category;
    private Service service;
    private Employee employee;

    @BeforeEach
    public void setUp() {
        this.ctx = ticketTestUtils.create();

        var chat = bcpService.create(Chat.FQN, Map.of(
                Chat.CHAT_ID, Randoms.string(),
                Chat.CODE, Randoms.string(),
                Chat.TITLE, Randoms.string()
        ));
        bcpService.create(ChatConversation.FQN, Map.of(
                ChatConversation.CONVERSATION_ID, CONVERSATION_ID,
                ChatConversation.CHAT, chat
        ));

        category = bcpService.create(TicketCategory.FQN, Map.of(
                TicketCategory.BRAND, ctx.brand,
                TicketCategory.CODE, CATEGORY_CODE,
                TicketCategory.TITLE, Randoms.string()
        ));

        this.partner = bcpService.create(PickupPointOwner.FQN, Map.of(
                Shop.TITLE, Randoms.string()
        ));

        this.service = ticketTestUtils.createService(ctx.team0, ctx.serviceTime24x7, ctx.brand, Optional.empty());

        this.employee = ticketTestUtils.createEmployee(ctx.ou, service);

        bcpService.edit(employee, Map.of(Employee.TEAMS, ctx.team0));
    }

    @Test
    public void testTransitionFromProcessingToClosedWaitContiguous() {

        var ch2 = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH2);
        when(omniChannelSettingsService.getPossibleChannels(any(EmployeeDistributionStatus.class)))
                .then(inv -> List.of(ch2));
        when(omniChannelSettingsService.getPossibleChannels(any(Ticket.class)))
                .then(inv -> TestChannels.CH2.equals(inv.getArgument(0, Ticket.class).getChannel().getCode())
                        ? List.of(ch2)
                        : List.of());

        var ticketGid = createTicket().getGid();

        distributionService.setEmployeeStatus(employee, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        distributionService.currentStatus(employee);

        var distribution = distributionService.getEmployeeStatus(employee);

        Ticket ticket = dbService.get(ticketGid);

        Assertions.assertEquals(ticket, distribution.getTicket());
        Assertions.assertEquals(0, distribution.getInactiveTickets().size());
        Assertions.assertEquals(employee, ticket.getResponsibleEmployee());
        Assertions.assertEquals(Ticket.STATUS_PROCESSING, ticket.getStatus());
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_PROCESSING, distribution.getStatus());

        bcpService.edit(ticketGid, Map.of(
                PickupPointChatTicket.STATUS, B2bTicket.STATUS_CLOSED_WAIT_CONTIGUOUS,
                PickupPointChatTicket.PARTNER, partner,
                PickupPointChatTicket.CATEGORIES, List.of(category)
        ));

        distribution = distributionService.getEmployeeStatus(employee);
        ticket = dbService.get(ticketGid);

        Assertions.assertNull(distribution.getTicket());
        Assertions.assertEquals(0, distribution.getInactiveTickets().size());
        Assertions.assertNull(ticket.getResponsibleEmployee());
        Assertions.assertEquals(B2bTicket.STATUS_CLOSED_WAIT_CONTIGUOUS, ticket.getStatus());
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TICKET, distribution.getStatus());
    }

    @Test
    public void testTransitionFromProcessingToDeferred() {

        var ch2 = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH2);
        when(omniChannelSettingsService.getPossibleChannels(any(EmployeeDistributionStatus.class)))
                .then(inv -> List.of(ch2));
        when(omniChannelSettingsService.getPossibleChannels(any(Ticket.class)))
                .then(inv -> TestChannels.CH2.equals(inv.getArgument(0, Ticket.class).getChannel().getCode())
                        ? List.of(ch2)
                        : List.of());

        var ticketGid = createTicket().getGid();

        distributionService.setEmployeeStatus(employee, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        distributionService.currentStatus(employee);

        var distribution = distributionService.getEmployeeStatus(employee);

        Ticket ticket = dbService.get(ticketGid);

        Assertions.assertEquals(ticket, distribution.getTicket());
        Assertions.assertEquals(0, distribution.getInactiveTickets().size());
        Assertions.assertEquals(employee, ticket.getResponsibleEmployee());
        Assertions.assertEquals(Ticket.STATUS_PROCESSING, ticket.getStatus());
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_PROCESSING, distribution.getStatus());

        bcpService.edit(ticketGid, Map.of(
                PickupPointChatTicket.STATUS, PickupPointChatTicket.STATUS_DEFERRED,
                PickupPointChatTicket.PARTNER, partner
        ));

        distribution = distributionService.getEmployeeStatus(employee);
        ticket = dbService.get(ticketGid);

        var wf = wfService.getOrError(PickupPointChatTicket.FQN);
        var from = wf.getStatus(PickupPointChatTicket.STATUS_PROCESSING);
        var to = wf.getStatus(PickupPointChatTicket.STATUS_DEFERRED);

        var transitionAttributes = uiUtils.getTransitionAttributes(ticket, from, to);

        Assertions.assertTrue(transitionAttributes.isEmpty());

        Assertions.assertNull(distribution.getTicket());
        Assertions.assertEquals(0, distribution.getInactiveTickets().size());
        Assertions.assertNull(ticket.getResponsibleEmployee());
        Assertions.assertEquals(PickupPointChatTicket.STATUS_DEFERRED, ticket.getStatus());
        Assertions.assertEquals(DEFER_TIME, ticket.getDeferTime());
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TICKET, distribution.getStatus());

        distributionService.setEmployeeStatus(distribution.getEmployee(), EmployeeDistributionStatus.STATUS_NOT_READY);

        timerTestUtils.simulateTimerExpiration(ticketGid, PickupPointChatTicket.DEFER_BACK_TIMER);

        ticket = dbService.get(ticketGid);
        Assertions.assertEquals(PickupPointChatTicket.STATUS_REOPENED, ticket.getStatus());
    }


    private Ticket createTicket() {
        return bcpService.create(PickupPointChatTicket.FQN, buildTicketAttributes());
    }

    private Map<String, Object> buildTicketAttributes() {
        return Map.of(
                PickupPointChatTicket.TITLE, Randoms.string(),
                PickupPointChatTicket.SERVICE, service,
                PickupPointChatTicket.CHANNEL, TestChannels.CH2,
                PickupPointChatTicket.RESOLUTION_TIME, Duration.ofHours(4),
                PickupPointChatTicket.CHAT_CONVERSATION, CONVERSATION_ID
        );
    }
}
