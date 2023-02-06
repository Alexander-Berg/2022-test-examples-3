package ru.yandex.market.crm.operatorwindow;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.google.common.collect.Maps;
import jdk.jfr.Description;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.b2bcrm.module.ticket.B2bTelephony;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.crm.domain.Phone;
import ru.yandex.market.crm.operatorwindow.dao.OwPersistedPropertyId;
import ru.yandex.market.crm.operatorwindow.domain.Email;
import ru.yandex.market.crm.operatorwindow.jmf.TicketFirstLine;
import ru.yandex.market.crm.operatorwindow.jmf.entity.BeruIncomingCallTicket;
import ru.yandex.market.crm.operatorwindow.jmf.entity.BeruOutgoingCallTicket;
import ru.yandex.market.crm.operatorwindow.jmf.entity.MarketIncomingCallTicket;
import ru.yandex.market.crm.operatorwindow.utils.VipTestUtil;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.catalog.items.CatalogItem;
import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.automation.test.utils.AutomationRuleTestUtils;
import ru.yandex.market.jmf.module.notification.Notification;
import ru.yandex.market.jmf.module.ou.Ou;
import ru.yandex.market.jmf.module.ou.impl.OuTestUtils;
import ru.yandex.market.jmf.module.relation.LinkedRelation;
import ru.yandex.market.jmf.module.ticket.DistributionService;
import ru.yandex.market.jmf.module.ticket.EmployeeDistributionStatus;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Team;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.test.TestChannels;
import ru.yandex.market.jmf.module.ticket.test.TicketTestConstants;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.telephony.voximplant.Channel;
import ru.yandex.market.jmf.telephony.voximplant.Employee;
import ru.yandex.market.jmf.telephony.voximplant.VoximplantInboundCall;
import ru.yandex.market.jmf.telephony.voximplant.controller.models.InboundCallRequest;
import ru.yandex.market.jmf.telephony.voximplant.impl.CreateInboundCallService;
import ru.yandex.market.jmf.telephony.voximplant.test.utils.VoximplantCallTestUtils;
import ru.yandex.market.jmf.timings.ServiceTime;
import ru.yandex.market.jmf.timings.test.impl.ServiceTimeTestUtils;
import ru.yandex.market.ocrm.module.complaints.BeruComplaintsTicket;
import ru.yandex.market.ocrm.module.order.domain.Order;
import ru.yandex.market.ocrm.module.order.test.OrderTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
public class CreateTelephonyTicketTest extends AbstractModuleOwTest {

    private static final Phone PHONE = Phone.fromRaw("+79123456789");
    private static final Phone MARKET_PHONE = Phone.fromRaw("+74952305030");
    private static final Phone B2B_MARKET_PHONE = Phone.fromRaw("+74999384362");
    private static final Email VIP_EMAIL = new Email("vip@example.com");
    private static final String DESTINATION_ID = "destinationId_789";
    @Inject
    ServiceTimeTestUtils serviceTimeTestUtils;
    @Inject
    private CreateInboundCallService createInboundCallService;
    @Inject
    private DbService dbService;
    @Inject
    private ConfigurationService configurationService;
    @Inject
    private TicketTestUtils ticketTestUtils;

    @Inject
    private OrderTestUtils orderTestUtils;

    @Inject
    private VipTestUtil vipTestUtil;

    @Inject
    private BcpService bcpService;

    @Inject
    private VoximplantCallTestUtils voximplantCallTestUtils;

    @Inject
    private DistributionService distributionService;

    @Inject
    private AutomationRuleTestUtils automationRuleTestUtils;

    @Inject
    private OuTestUtils ouTestUtils;

    @BeforeEach
    public void setup() {
        ouTestUtils.createOu();

        ticketTestUtils.setServiceTime24x7(Constants.Service.BERU_INCOMING_CALL);
        ticketTestUtils.setServiceTime24x7(Constants.Service.BERU_VIP_INCOMING_CALL);
        ticketTestUtils.setServiceTime24x7(Constants.Service.BERU_COMPLAINTS_CALLS);
        ticketTestUtils.setServiceTime24x7(Constants.Service.MARKET_B2B_INCOMING_CALL);
        ticketTestUtils.setServiceTime24x7(Constants.Service.MARKET_DSBS_INCOMING_CALL);

        Entity st = dbService.getByNaturalId(ServiceTime.FQN, CatalogItem.CODE, "b2b_9_21");
        serviceTimeTestUtils.createPeriod(st, "monday", "09:00", "21:00");
    }


    @Test
    public void createTelephonyTicketForInboundCall() {
        usePhoneVipServices(true);
        final VoximplantInboundCall inboundCall = createInboundCall(PHONE);
        final Ticket ticketByPhone = getTicketByPhone(BeruIncomingCallTicket.FQN, inboundCall.getCallerId());
        Assertions.assertEquals(Constants.Service.BERU_INCOMING_CALL, ticketByPhone.getService().getCode());
    }

    @Test
    @Disabled("FIXME")
    public void detectVipCustomerByLastOrder() {
        usePhoneVipServices(true);

        createOrder(PHONE, VIP_EMAIL);
        vipTestUtil.registerEmailAsVip(VIP_EMAIL);

        final VoximplantInboundCall inboundCall = createInboundCall(PHONE);
        final Ticket ticketByPhone = getTicketByPhone(BeruIncomingCallTicket.FQN, inboundCall.getCallerId());
        Assertions.assertEquals(Constants.Service.BERU_VIP_INCOMING_CALL, ticketByPhone.getService().getCode());
    }

    @Test
    public void disablePhoneVipServices__expectVipServiceIsNotUsed() {
        usePhoneVipServices(false);

        createOrder(PHONE, VIP_EMAIL);
        vipTestUtil.registerEmailAsVip(VIP_EMAIL);

        final VoximplantInboundCall inboundCall = createInboundCall(PHONE);
        final Ticket ticketByPhone = getTicketByPhone(BeruIncomingCallTicket.FQN, inboundCall.getCallerId());
        Assertions.assertEquals(Constants.Service.BERU_INCOMING_CALL, ticketByPhone.getService().getCode());
    }

    @Test
    @Transactional
    @Disabled("FIXME")
    public void detectVipCustomerByIncomingPhoneNumber() {
        usePhoneVipServices(true);

        vipTestUtil.registerPhoneAsVip(PHONE);

        final VoximplantInboundCall inboundCall = createInboundCall(PHONE);
        final Ticket ticketByPhone = getTicketByPhone(BeruIncomingCallTicket.FQN, inboundCall.getCallerId());
        Assertions.assertEquals(Constants.Service.BERU_VIP_INCOMING_CALL, ticketByPhone.getService().getCode());
    }

    @Test
    @Disabled("FIXME")
    public void detectVipCustomerByLastTicket() {
        usePhoneVipServices(true);

        createTicket(PHONE, VIP_EMAIL, Constants.Service.BERU_INCOMING_CALL);
        vipTestUtil.registerEmailAsVip(VIP_EMAIL);

        final VoximplantInboundCall inboundCall = createInboundCall(PHONE);
        final Ticket ticketByPhone = getTicketByPhone(BeruIncomingCallTicket.FQN, inboundCall.getCallerId());
        Assertions.assertEquals(Constants.Service.BERU_VIP_INCOMING_CALL, ticketByPhone.getService().getCode());
    }

    @Test
    public void testTransferToBeruComplaintsWithOrder() {
        VoximplantInboundCall inboundCall = voximplantCallTestUtils.createInboundCall();
        Order order = orderTestUtils.createOrder();
        InboundCallRequest request = new InboundCallRequest(
                Randoms.string(),
                Randoms.string(),
                Constants.Service.BERU_COMPLAINTS_CALLS,
                Randoms.string(),
                Randoms.string(),
                Map.of(
                        "X-EO-ORDER", order.getGid(),
                        "X-EO-TICKET", inboundCall.getTicket().getGid()
                ),
                ""
        );
        VoximplantInboundCall result = createInboundCallService.createInboundCall(request);
        BeruComplaintsTicket ticket = (BeruComplaintsTicket) result.getTicket();
        assertNotNull(ticket);
        assertEquals(Constants.Service.BERU_COMPLAINTS_CALLS, ticket.getService().getCode());
        assertEquals(BeruComplaintsTicket.FQN, ticket.getFqn());
        assertEquals(order, ticket.getOrder());
        assertEquals(Channel.PHONE, ticket.getChannel().getCode());
        assertEquals(ticket.getTitle(), String.format("Входящий звонок (%s), заказ %d",
                inboundCall.getTicket().getClientPhone(), order.getTitle()));
        assertLinkedRelation(ticket, inboundCall.getTicket());
    }

    @Test
    public void testTransferToBeruComplaintsWithoutOrder() {
        VoximplantInboundCall inboundCall = voximplantCallTestUtils.createInboundCall();
        InboundCallRequest request = new InboundCallRequest(
                Randoms.string(),
                Randoms.string(),
                Constants.Service.BERU_COMPLAINTS_CALLS,
                Randoms.string(),
                Randoms.string(),
                Map.of(
                        "X-EO-TICKET", inboundCall.getTicket().getGid(),
                        "X-EO-ORDER", ""
                ),
                ""
        );
        VoximplantInboundCall result = createInboundCallService.createInboundCall(request);
        BeruComplaintsTicket ticket = (BeruComplaintsTicket) result.getTicket();
        assertNotNull(ticket);
        assertEquals(Constants.Service.BERU_COMPLAINTS_CALLS, ticket.getService().getCode());
        assertEquals(BeruComplaintsTicket.FQN, ticket.getFqn());
        assertNull(ticket.getOrder());
        assertEquals(Channel.PHONE, ticket.getChannel().getCode());
        assertEquals(ticket.getTitle(), String.format("Входящий звонок (%s), заказ отсутствует",
                inboundCall.getTicket().getClientPhone()));
        assertLinkedRelation(ticket, inboundCall.getTicket());
    }

    @Test
    public void testTransferToB2b() {
        VoximplantInboundCall inboundCall = createInboundCall(B2B_MARKET_PHONE, B2B_MARKET_PHONE.getNormalized());
        InboundCallRequest request = new InboundCallRequest(
                Randoms.string(),
                Randoms.string(),
                Constants.Service.MARKET_B2B_INCOMING_CALL,
                Randoms.string(),
                Randoms.string(),
                Map.of(
                        "X-EO-TICKET", inboundCall.getTicket().getGid(),
                        "X-EO-ORDER", ""
                ),
                ""
        );
        VoximplantInboundCall result = createInboundCallService.createInboundCall(request);
        B2bTelephony ticket = (B2bTelephony) result.getTicket();
        assertNotNull(ticket);
        assertEquals(Constants.Service.MARKET_B2B_INCOMING_CALL, ticket.getService().getCode());
        assertEquals(B2bTelephony.FQN, ticket.getFqn());
        assertEquals(Channel.PHONE, ticket.getChannel().getCode());
        assertEquals(ticket.getTitle(), String.format("Входящий звонок (%s)",
                inboundCall.getTicket().getClientPhone()));
        assertLinkedRelation(ticket, inboundCall.getTicket());
    }

    @Test
    public void createMarketDsbsTelephonyTicketForInboundCall() {
        usePhoneVipServices(false);

        final VoximplantInboundCall inboundCall = createInboundCall(PHONE, MARKET_PHONE.getRawOrNormalized());
        final Ticket ticket = getTicketByPhone(Ticket.FQN, inboundCall.getCallerId());

        assertNotNull(ticket);
        Assertions.assertEquals(MarketIncomingCallTicket.FQN, ticket.getFqn());
        Assertions.assertEquals(Constants.Service.MARKET_DSBS_INCOMING_CALL, ticket.getService().getCode());
        Assertions.assertEquals(Constants.Brand.MARKET, ticket.getBrand().getCode());
    }

    @Test
    public void testTransferToBeruWithoutOrder() {
        VoximplantInboundCall inboundCall = voximplantCallTestUtils.createInboundCall();
        InboundCallRequest request = new InboundCallRequest(
                Randoms.string(),
                Randoms.string(),
                Constants.Service.BERU_INCOMING_CALL,
                Randoms.string(),
                Randoms.string(),
                Map.of(
                        "X-EO-TICKET", inboundCall.getTicket().getGid()
                ),
                ""
        );
        VoximplantInboundCall result = createInboundCallService.createInboundCall(request);
        BeruIncomingCallTicket ticket = (BeruIncomingCallTicket) result.getTicket();
        assertNotNull(ticket);
        assertEquals(Constants.Service.BERU_INCOMING_CALL, ticket.getService().getCode());
        assertEquals(BeruIncomingCallTicket.FQN, ticket.getFqn());
        assertNull(ticket.getOrder());
        assertEquals(Channel.PHONE, ticket.getChannel().getCode());
        assertEquals(inboundCall.getTicket().getClientPhone(), ticket.getClientPhone());
        assertEquals(ticket.getTitle(), String.format("Входящий звонок (%s), заказ отсутствует",
                inboundCall.getTicket().getClientPhone()));
        assertLinkedRelation(ticket, inboundCall.getTicket());
    }

    @Test
    public void testTransferToBeruWithOrder() {
        VoximplantInboundCall inboundCall = voximplantCallTestUtils.createInboundCall();
        Order order = orderTestUtils.createOrder();
        InboundCallRequest request = new InboundCallRequest(
                Randoms.string(),
                Randoms.string(),
                Constants.Service.BERU_INCOMING_CALL,
                Randoms.string(),
                Randoms.string(),
                Map.of(
                        "X-EO-ORDER", order.getGid(),
                        "X-EO-TICKET", inboundCall.getTicket().getGid()
                ),
                ""
        );
        VoximplantInboundCall result = createInboundCallService.createInboundCall(request);
        BeruIncomingCallTicket ticket = (BeruIncomingCallTicket) result.getTicket();
        assertNotNull(ticket);
        assertEquals(Constants.Service.BERU_INCOMING_CALL, ticket.getService().getCode());
        assertEquals(BeruIncomingCallTicket.FQN, ticket.getFqn());
        assertEquals(order, ticket.getOrder());
        assertEquals(Channel.PHONE, ticket.getChannel().getCode());
        assertEquals(inboundCall.getTicket().getClientPhone(), ticket.getClientPhone());
        assertEquals(ticket.getTitle(), String.format("Входящий звонок (%s), заказ %d",
                inboundCall.getTicket().getClientPhone(), order.getTitle()));
        assertLinkedRelation(ticket, inboundCall.getTicket());
    }

    @Test
    public void testTransferToDsbsWithoutOrder() {
        VoximplantInboundCall inboundCall = voximplantCallTestUtils.createInboundCall();
        InboundCallRequest request = new InboundCallRequest(
                Randoms.string(),
                Randoms.string(),
                Constants.Service.MARKET_DSBS_INCOMING_CALL,
                Randoms.string(),
                Randoms.string(),
                Map.of(
                        "X-EO-TICKET", inboundCall.getTicket().getGid()
                ),
                ""
        );
        VoximplantInboundCall result = createInboundCallService.createInboundCall(request);
        MarketIncomingCallTicket ticket = (MarketIncomingCallTicket) result.getTicket();
        assertNotNull(ticket);
        assertEquals(Constants.Service.MARKET_DSBS_INCOMING_CALL, ticket.getService().getCode());
        assertEquals(MarketIncomingCallTicket.FQN, ticket.getFqn());
        assertNull(ticket.getOrder());
        assertEquals(Channel.PHONE, ticket.getChannel().getCode());
        assertEquals(inboundCall.getTicket().getClientPhone(), ticket.getClientPhone());
        assertEquals(ticket.getTitle(), String.format("Входящий звонок (%s), заказ отсутствует",
                inboundCall.getTicket().getClientPhone()));
        assertLinkedRelation(ticket, inboundCall.getTicket());
    }

    @Test
    public void testTransferToDsbsWithOrder() {
        VoximplantInboundCall inboundCall = voximplantCallTestUtils.createInboundCall();
        Order order = orderTestUtils.createOrder();
        InboundCallRequest request = new InboundCallRequest(
                Randoms.string(),
                Randoms.string(),
                Constants.Service.MARKET_DSBS_INCOMING_CALL,
                Randoms.string(),
                Randoms.string(),
                Map.of(
                        "X-EO-ORDER", order.getGid(),
                        "X-EO-TICKET", inboundCall.getTicket().getGid()
                ),
                ""
        );
        VoximplantInboundCall result = createInboundCallService.createInboundCall(request);
        MarketIncomingCallTicket ticket = (MarketIncomingCallTicket) result.getTicket();
        assertNotNull(ticket);
        assertEquals(Constants.Service.MARKET_DSBS_INCOMING_CALL, ticket.getService().getCode());
        assertEquals(MarketIncomingCallTicket.FQN, ticket.getFqn());
        assertEquals(order, ticket.getOrder());
        assertEquals(Channel.PHONE, ticket.getChannel().getCode());
        assertEquals(inboundCall.getTicket().getClientPhone(), ticket.getClientPhone());
        assertEquals(ticket.getTitle(), String.format("Входящий звонок (%s), заказ %d",
                inboundCall.getTicket().getClientPhone(), order.getTitle()));
        assertLinkedRelation(ticket, inboundCall.getTicket());
    }

    @Test
    @Description("Персональная телефония")
    public void testDistributingTicketWithAdditionalNumber() {
        String additionalNumber = "123";

        Service service = dbService.getByNaturalId(Service.FQN, Service.CODE, "b2bCdTelephony");
        Team team = dbService.getByNaturalId(Team.FQN, Team.CODE, "personalLinePhone");

        Employee employee = createEmployee(additionalNumber, service, team);
        Employee employee1 = createEmployee("124", service, team);

        //уводим сотрудника в оффлайн, чтобы тикеты не распределялись
        setEmployeeStatus(employee, EmployeeDistributionStatus.STATUS_OFFLINE);
        setEmployeeStatus(employee1, EmployeeDistributionStatus.STATUS_OFFLINE);

        //создаем обычные тикеты для массовки
        Ticket ticket0 = createTicket(service, team);
        Ticket ticket1 = createTicket(service, team);

        InboundCallRequest request = new InboundCallRequest(
                Randoms.string(),
                Randoms.string(),
                "b2bCdTelephony",
                Randoms.string(),
                Randoms.string(),
                Map.of(
                        "X-EO-TICKET", "",
                        "X-EO-ORDER", ""
                ),
                additionalNumber
        );

        //Создаем правило автоматизации, которое будет проставлять ответственного
        automationRuleTestUtils.createApprovedEventRule(service, "/automation_rules/personalCall.json", "createTicket",
                Set.of(), Set.of(ouTestUtils.getAnyCreatedOu()));

        //создаем телефонный тикет по входящему звонку
        VoximplantInboundCall result = createInboundCallService.createInboundCall(request);
        B2bTelephony ticket2 = (B2bTelephony) result.getTicket();
        Employee responsibleEmployee = (Employee) ticket2.getResponsibleEmployee();
        assertEquals(responsibleEmployee, employee, "Должен быть назначен сотрудник, с добавочным номером 123");

        //переводим сотрудника в онлайн
        setEmployeeStatus(employee, EmployeeDistributionStatus.STATUS_WAIT_TICKET);
        setEmployeeStatus(employee1, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        EmployeeDistributionStatus employeeStatus = distributionService.getEmployeeStatus(employee);
        String employeeTicketInDistribution = employeeStatus.getTicket().getGid();

        EmployeeDistributionStatus employeeStatus1 = distributionService.getEmployeeStatus(employee1);
        String employeeTicketInDistribution1 = employeeStatus1.getTicket().getGid();
        assertEquals(employeeTicketInDistribution, ticket2.getGid(),
                "У сотрудника в распределении числится тикет по добавочному номеру"
        );
        assertTrue(employeeTicketInDistribution1.equals(ticket0.getGid()) || employeeTicketInDistribution1.equals(ticket1.getGid()));
    }

    @Test
    @Description("Персональная телефония: пропущенный вызов")
    public void testDistributingTicketWithAdditionalNumberMissedCall() {
        String additionalNumber = "123";
        Service service = dbService.getByNaturalId(Service.FQN, Service.CODE, "b2bCdTelephony");
        Team team = dbService.getByNaturalId(Team.FQN, Team.CODE, "personalLinePhone");
        Employee employee = createEmployee(additionalNumber, service, team);
        setEmployeeStatus(employee, EmployeeDistributionStatus.STATUS_OFFLINE);

        InboundCallRequest request = new InboundCallRequest(
                Randoms.string(),
                Randoms.string(),
                "b2bCdTelephony",
                Randoms.string(),
                Randoms.string(),
                Map.of(
                        "X-EO-TICKET", "",
                        "X-EO-ORDER", ""
                ),
                additionalNumber
        );

        automationRuleTestUtils.createApprovedEventRule(service, "/automation_rules/personalCall.json",
                "createTicket", Set.of(), Set.of(ouTestUtils.getAnyCreatedOu()));

        VoximplantInboundCall inboundCall = createInboundCallService.createInboundCall(request);
        bcpService.edit(inboundCall, Map.of(VoximplantInboundCall.STATUS, VoximplantInboundCall.Statuses.ENDED,
                VoximplantInboundCall.ENDED_BY, "CLIENT"));

        B2bTelephony ticket = (B2bTelephony) inboundCall.getTicket();
        assertEquals("missed", ticket.getStatus());
        Employee responsibleEmployee = (Employee) ticket.getResponsibleEmployee();
        List<Entity> notifications = dbService.list(Query.of(Notification.FQN));
        assertEquals(1, notifications.size());
        Notification notification = (Notification) notifications.get(0);
        assertEquals(notification.getEmployee(), responsibleEmployee);
        assertTrue(notification.getMessage().contains("У вас есть пропущенное обращение телефонии: "));
    }

    @NotNull
    private Employee createEmployee(String additionalNumber, Service service, Team team) {
        Ou ou = ticketTestUtils.createOu();

        Map<String, Object> properties = Map.of(
                Employee.TITLE, Randoms.string(),
                Employee.STAFF_LOGIN, Randoms.string(),
                Employee.OU, ou,
                Employee.ADDITIONAL_NUMBER, additionalNumber,
                Employee.VOX_LOGIN, 123L,
                "services", service,
                "teams", team
        );

        return bcpService.create(Employee.FQN_DEFAULT, properties);
    }

    private Ticket createTicket(Service service, Team team) {
        return bcpService.create(TicketTestConstants.TICKET_TEST_FQN, buildTicketAttributes(service, team));
    }

    private Map<String, Object> buildTicketAttributes(Service service, Team team) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("title", Randoms.string());
        attributes.put(Ticket.DESCRIPTION, Randoms.string());
        attributes.put(Ticket.SERVICE, service);
        attributes.put(Ticket.TIME_ZONE, "Europe/Moscow");
        attributes.put(Ticket.PRIORITY, "10");
        attributes.put(Ticket.CHANNEL, TestChannels.CH1);
        attributes.put(Ticket.RESPONSIBLE_TEAM, team);
        attributes.put(Ticket.RESOLUTION_TIME, Duration.ofHours(4));
        return attributes;
    }

    private void setEmployeeStatus(Employee employee, String statusWaitTicket) {
        distributionService.setEmployeeStatus(employee, statusWaitTicket);
    }

    private void assertLinkedRelation(Ticket source, Ticket target) {
        boolean exists = dbService.any(Query.of(LinkedRelation.FQN).withFilters(
                Filters.eq(LinkedRelation.SOURCE, source),
                Filters.eq(LinkedRelation.TARGET, target)
        ));
        assertTrue(exists);
    }

    private TicketFirstLine createTicket(Phone phone, Email email, String serviceCode) {
        Map<String, Object> attributes = ru.yandex.market.jmf.utils.Maps.of(
                TicketFirstLine.TITLE, Randoms.string(),
                TicketFirstLine.DESCRIPTION, Randoms.string(),
                TicketFirstLine.CHANNEL, TestChannels.CH1,
                TicketFirstLine.CLIENT_NAME, Randoms.string(),
                TicketFirstLine.CLIENT_EMAIL, email.getAddress(),
                TicketFirstLine.CLIENT_PHONE, phone.getRawOrNormalized(),
                TicketFirstLine.SERVICE, serviceCode
        );

        return bcpService.create(BeruOutgoingCallTicket.FQN, attributes);
    }

    private Order createOrder(Phone phone,
                              Email email) {
        Map<String, Object> attributes = Maps.newHashMap();
        attributes.put(Order.BUYER_PHONE, phone);
        attributes.put(Order.BUYER_EMAIL, email.getAddress());
        attributes.put(Order.BUYER_FIRST_NAME, "Фердинанд");
        attributes.put(Order.BUYER_MIDDLE_NAME, "");
        attributes.put(Order.BUYER_LAST_NAME, "Порше");
        attributes.put(Order.STATUS, OrderStatus.PENDING.name());
        attributes.put(Order.SUB_STATUS, OrderSubstatus.AWAIT_CONFIRMATION.name());

        return orderTestUtils.createOrder(attributes);
    }

    private Ticket getTicketByPhone(Fqn fqn, String phone) {
        Query query = Query.of(fqn)
                .withFilters(Filters.eq(Ticket.CLIENT_PHONE, Phone.fromRaw(phone)));

        final List<Entity> list = dbService.list(query);
        Assertions.assertEquals(1, list.size());

        return (Ticket) list.get(0);
    }

    private VoximplantInboundCall createInboundCall(Phone callerPhone) {
        return createInboundCall(callerPhone, DESTINATION_ID);
    }

    private VoximplantInboundCall createInboundCall(Phone callerPhone, String destination) {
        Map<String, String> sipHeaders = new HashMap<>();
        final String callerId = callerPhone.getRawOrNormalized();
        InboundCallRequest request = new InboundCallRequest(
                "sessionId_123",
                callerId,
                destination,
                "connectWithOperatorUrl_012",
                "sendDataToVoximplant_",
                sipHeaders,
                ""
        );
        return createInboundCallService.createInboundCall(request);
    }

    private void usePhoneVipServices(boolean value) {
        configurationService.setValue(OwPersistedPropertyId.USE_PHONE_VIP_SERVICES.key(), value);
    }

}
