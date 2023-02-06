package ru.yandex.market.ocrm.module.quality.management;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.attributes.integercondition.IntegerCondition;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.ou.Employee;
import ru.yandex.market.jmf.module.ou.Ou;
import ru.yandex.market.jmf.module.ticket.Channel;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Team;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.TicketCategory;
import ru.yandex.market.jmf.module.ticket.test.TestChannels;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.tx.TxService;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.ocrm.module.order.TicketFirstLine;
import ru.yandex.market.ocrm.module.order.domain.Order;
import ru.yandex.market.ocrm.module.order.test.OrderTestUtils;
import ru.yandex.market.ocrm.module.quality.management.domain.QualityManagementSelection;
import ru.yandex.market.ocrm.module.quality.management.domain.TicketIteration;
import ru.yandex.misc.thread.ThreadUtils;

@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(classes = ModuleQualityManagementTestConfiguration.class)
public class QualityManagementSelectionServiceTest {

    private static final Fqn TEST_FQN = Fqn.of("ticket$testQM");

    @Inject
    private BcpService bcpService;
    @Inject
    private DbService dbService;
    @Inject
    private QualityManagementSelectionService qualityManagementSelectionService;
    @Inject
    private OrderTestUtils orderTestUtils;
    @Inject
    private TicketTestUtils ticketTestUtils;
    @Inject
    private TxService txService;

    private Employee employee1;
    private Employee employee2;
    private Employee responsible;
    private Channel channel1;
    private Channel channel2;
    private Channel channel3;
    private Order order1;
    private Order order2;
    private Ou ou1;
    private Ou ou2;
    private Service service1;
    private Service service2;
    private Service service3;
    private Team team1;
    private Team team2;
    private Team team3;
    private Ticket ticket1;
    private Ticket ticket2;
    private Ticket ticket3;
    private TicketCategory category1;
    private TicketCategory category3;
    private TicketCategory category2;

    @BeforeAll
    public void init() {
        txService.runInNewTx(() -> {
            ou1 = ticketTestUtils.createOu();
            ou2 = ticketTestUtils.createOu();

            employee1 = ticketTestUtils.createEmployee(ou1);
            employee2 = ticketTestUtils.createEmployee(ou2);
            responsible = ticketTestUtils.createEmployee(ou1);

            team1 = ticketTestUtils.createTeam();
            team2 = ticketTestUtils.createTeam();
            team3 = ticketTestUtils.createTeam();

            var brand = ticketTestUtils.createBrand();
            service1 = ticketTestUtils.createService24x7(team1, brand);
            service2 = ticketTestUtils.createService24x7(team2, brand);
            service3 = ticketTestUtils.createService24x7(team3, brand);

            category1 = ticketTestUtils.createTicketCategory(service1.getBrand());
            category2 = ticketTestUtils.createTicketCategory(service2.getBrand());
            category3 = ticketTestUtils.createTicketCategory(service3.getBrand());

            order1 = orderTestUtils.createOrder();
            order2 = orderTestUtils.createOrder();

            channel1 = dbService.getByNaturalId(Channel.FQN, Channel.CODE, Channel.MAIL);
            channel2 = dbService.getByNaturalId(Channel.FQN, Channel.CODE, TestChannels.CH1);
            channel3 = dbService.getByNaturalId(Channel.FQN, Channel.CODE, Channel.CHAT);
        });

        var ticketProps = Map.of(
                Ticket.STATUS, Ticket.STATUS_REGISTERED,
                Ticket.RESPONSIBLE_OU, ou1,
                Ticket.SERVICE, service1,
                Ticket.RESPONSIBLE_TEAM, team1,
                Ticket.CHANNEL, channel1,
                Ticket.CATEGORIES, category1,
                Ticket.RESPONSIBLE_EMPLOYEE, employee1,
                TicketFirstLine.ORDER, order1,
                ru.yandex.market.ocrm.module.csat.Ticket.CSAT_SCORE, 3
        );

        txService.runInNewTx(() -> {
            ticket1 = createTicket(ticketProps);
            createTicket(set(ticketProps, Ticket.SERVICE, service2));
            createTicket(set(ticketProps, Map.of(Ticket.RESPONSIBLE_EMPLOYEE, employee2, Ticket.RESPONSIBLE_OU, ou2)));
            createTicket(set(ticketProps, Ticket.CATEGORIES, category2));
            createTicket(set(ticketProps, Ticket.CHANNEL, channel2));
            createTicket(set(ticketProps, Ticket.RESPONSIBLE_TEAM, team2));
            createTicket(set(ticketProps, TicketFirstLine.ORDER, order2));
            createTicket(set(ticketProps, ru.yandex.market.ocrm.module.csat.Ticket.CSAT_SCORE, 5));
            ticket2 = createTicket(set(ticketProps, Map.of(
                    Ticket.CATEGORIES, category3,
                    Ticket.CHANNEL, channel3,
                    Ticket.RESPONSIBLE_TEAM, team3,
                    Ticket.SERVICE, service3
            )), Ticket.STATUS_PENDING);
            ticketTestUtils.editTicketStatus(ticket2, Ticket.STATUS_PROCESSING);
            ticketTestUtils.editTicketStatus(ticket2, Ticket.STATUS_CLOSED);
            ticket3 = createTicket(ticketProps, Ticket.STATUS_PROCESSING);
        });

        ThreadUtils.sleep(2, TimeUnit.SECONDS);

        txService.runInNewTx(() -> ticketTestUtils.editTicketStatus(ticket3, Ticket.STATUS_CLOSED));

        ThreadUtils.sleep(1, TimeUnit.SECONDS);

        txService.runInNewTx(() -> createTicket(ticketProps));
    }

    /**
     * В базе данных есть некий набор тикетов КК
     * Создаём выборку, в которой в качестве фильтров указываем:
     * 1) границы даты регистрации оцениваемого обращения
     * 2) границы даты обработки оцениваемого обращения
     * 3) колл-центр
     * 4) проект (brnad)
     * 5) две очереди
     * 6) два канала
     * 7) две линии (team)
     * 8) заказ
     * 9) две категории
     * 10) оператор
     * 11) aht < 1 сек
     * 12) csi < 4
     * Убеждаемся, что нашлись именно два этих тикета, потому что такие тикетов только 2
     */
    @Test
    @Transactional
    public void findTicketIterations() {
        Map<String, Object> selectionProps = Maps.of(
                QualityManagementSelection.TITLE, Randoms.string(),
                QualityManagementSelection.TICKET_REGISTRATION_DATE_FROM, ticket1.getRegistrationDate(),
                QualityManagementSelection.TICKET_REGISTRATION_DATE_TO, ticket3.getRegistrationDate().plusSeconds(1),
                QualityManagementSelection.PROCESSING_DATE_FROM, ticket1.getRegistrationDate(),
                QualityManagementSelection.PROCESSING_DATE_TO, ticket3.getRegistrationDate().plusSeconds(1),
                QualityManagementSelection.CALL_CENTER, ou1,
                QualityManagementSelection.PROJECT, service1.getBrand(),
                QualityManagementSelection.SERVICES, Set.of(service1, service3),
                QualityManagementSelection.CHANNELS, Set.of(channel1, channel3),
                QualityManagementSelection.TEAMS, Set.of(team1, team3),
                QualityManagementSelection.ORDER, order1,
                QualityManagementSelection.TICKET_CATEGORY, Set.of(category1, category3),
                QualityManagementSelection.OPERATOR, employee1,
                QualityManagementSelection.AHT, new IntegerCondition(IntegerCondition.Condition.LESS, 1L),
                QualityManagementSelection.CSI, new IntegerCondition(IntegerCondition.Condition.LESS, 4L),
                QualityManagementSelection.RESPONSIBLES, responsible,
                QualityManagementSelection.TICKET_MAX_NUMBER, 10
        );
        var selection = bcpService.<QualityManagementSelection>create(QualityManagementSelection.FQN, selectionProps);
        var iterationToEnd = qualityManagementSelectionService.findTicketIterations(selection);
        var iterations = qualityManagementSelectionService.enrichTicketIterations(iterationToEnd);
        var tickets = iterations.stream().map(TicketIteration::getTicket).collect(Collectors.toList());

        Assertions.assertEquals(2, tickets.size());
        Assertions.assertTrue(tickets.contains(ticket1));
        Assertions.assertTrue(tickets.contains(ticket2));
    }

    /**
     * В базе данных есть некий набор тикетов КК
     * Создаём выборку, в которой в качестве фильтров указываем 2 конкретных тикета
     * Убеждаемся, что нашлись именно два этих тикета
     */
    @Test
    @Transactional
    public void byTickets() {
        var selection = bcpService.<QualityManagementSelection>create(QualityManagementSelection.FQN, Maps.of(
                QualityManagementSelection.TITLE, Randoms.string(),
                QualityManagementSelection.CHANNELS, Set.of(channel1, channel2, channel3),
                QualityManagementSelection.TICKETS, Set.of(ticket1, ticket2),
                QualityManagementSelection.RESPONSIBLES, responsible,
                QualityManagementSelection.TICKET_MAX_NUMBER, 10
        ));
        var iterationToEnd = qualityManagementSelectionService.findTicketIterations(selection);
        var iterations = qualityManagementSelectionService.enrichTicketIterations(iterationToEnd);
        var tickets = iterations.stream().map(TicketIteration::getTicket).collect(Collectors.toList());

        Assertions.assertEquals(2, tickets.size());
        Assertions.assertTrue(tickets.contains(ticket1));
        Assertions.assertTrue(tickets.contains(ticket2));
    }

    private Ticket createTicket(Map<String, Object> properties) {
        return createTicket(properties, null);
    }

    private Ticket createTicket(Map<String, Object> properties, String status) {
        var ticket = ticketTestUtils.createTicket(TEST_FQN, properties);
        ticketTestUtils.editTicketStatus(ticket, Ticket.STATUS_PROCESSING);
        return null != status ?
                ticketTestUtils.editTicketStatus(ticket, status) :
                ticketTestUtils.editTicketStatus(ticket, Ticket.STATUS_CLOSED);
    }

    private Map<String, Object> set(Map<String, Object> map, String key, Object value) {
        return set(map, Maps.of(key, value));
    }

    private Map<String, Object> set(Map<String, Object> source, Map<String, Object> map) {
        var newMap = new HashMap<>(source);
        newMap.putAll(map);
        return newMap;
    }
}
