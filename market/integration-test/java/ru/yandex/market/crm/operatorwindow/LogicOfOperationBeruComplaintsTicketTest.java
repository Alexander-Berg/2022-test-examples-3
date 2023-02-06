package ru.yandex.market.crm.operatorwindow;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.transaction.Transactional;

import jdk.jfr.Description;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.crm.operatorwindow.integration.Brands;
import ru.yandex.market.crm.operatorwindow.utils.BeruComplaintsTicketUtils;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.entity.query.SortingOrder;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.comment.PublicComment;
import ru.yandex.market.jmf.module.ou.Ou;
import ru.yandex.market.jmf.module.ou.impl.OuTestUtils;
import ru.yandex.market.jmf.module.ticket.Brand;
import ru.yandex.market.jmf.module.ticket.DistributionService;
import ru.yandex.market.jmf.module.ticket.Employee;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Team;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.TicketCategory;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.security.test.impl.MockSecurityDataService;
import ru.yandex.market.ocrm.module.complaints.BeruComplaintsTicket;
import ru.yandex.market.ocrm.module.order.domain.Order;
import ru.yandex.market.ocrm.module.order.test.OrderTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


@Transactional
public class LogicOfOperationBeruComplaintsTicketTest extends AbstractModuleOwTest {

    @Inject
    private DbService dbService;
    @Inject
    private BcpService bcpService;
    @Inject
    private BeruComplaintsTicketUtils beruComplaintsUtils;
    @Inject
    private OuTestUtils ouTestUtils;
    @Inject
    private MockSecurityDataService mockSecurityDataService;
    @Inject
    private DistributionService distributionService;
    @Inject
    private TicketTestUtils ticketTestUtils;
    @Inject
    private OrderTestUtils orderTestUtils;


    private Service beruComplaintsService;
    private Service beruComplaintsVipService;
    private Team beruComplaintsTeam;
    private Brand beruComplaintsBrand;

    private static Stream<Arguments> attributeForCheckingBouncingRulesWhenCreatingTicketsTest() {
        return Stream.of(
                Arguments.arguments("PREPAID", "YANDEX", false, false),
                Arguments.arguments(null, null, false, true)
        );
    }

    private static Stream<Arguments> attributesForResettingResponsiblePersonWhenChangingQueueTest() {
        return Stream.of(
                Arguments.arguments("beruComplaints", "beruComplaintsUnmarked", "beruComplaintsVip",
                        "beruComplaintsEscalation"),
                Arguments.arguments("beruComplaints", "beruComplaintsUnmarked", "beruComplaintsVip", null)
        );
    }


    @BeforeEach
    void setUp() {
        beruComplaintsTeam = beruComplaintsUtils.createTeamIfNotExists("beruComplaints");
        beruComplaintsService = beruComplaintsUtils.createService("beruComplaintsUnmarked", beruComplaintsTeam,
                Brands.BERU_COMPLAINTS);
        beruComplaintsVipService = beruComplaintsUtils.createService("beruComplaintsVip", beruComplaintsTeam,
                Brands.BERU_COMPLAINTS);
        beruComplaintsBrand = ticketTestUtils.createBrand(Brands.BERU_COMPLAINTS);
    }

    @Test
    @Description("""
            Переоткрытие обращения при решении связного тикета
            Тест-кейс - https://testpalm2.yandex-team.ru/testcase/ocrm-798
            """)
    void rediscoveringATicketWhenSolvingAConnectedTest() {
        Ticket firstBeruComplaintsTicket = beruComplaintsUtils.createTicket(beruComplaintsVipService);
        Ticket secondBeruComplaintsTicket = beruComplaintsUtils.createTicket(beruComplaintsVipService);

        bcpService.edit(secondBeruComplaintsTicket, Map.of("sourceTicket", firstBeruComplaintsTicket));
        ticketTestUtils.editTicketStatus(firstBeruComplaintsTicket, Ticket.STATUS_PROCESSING);
        ticketTestUtils.editTicketStatus(firstBeruComplaintsTicket, Ticket.STATUS_ON_HOLD);

        ticketTestUtils.editTicketStatus(secondBeruComplaintsTicket, Ticket.STATUS_PROCESSING);
        ticketTestUtils.editTicketStatus(secondBeruComplaintsTicket, Ticket.STATUS_RESOLVED);

        assertEquals(Ticket.STATUS_REOPENED, firstBeruComplaintsTicket.getStatus());

    }

    @Test
    @Description("""
            После решения обращения ответственный за обращение не сбрасывается
            тест-кейс - https://testpalm2.yandex-team.ru/testcase/ocrm-792
            """)
    void afterSolvingTicketPersonResponsibleForTicketIsNotResetTest() {
        Employee currentEmployee = createEmployee(beruComplaintsTeam, Set.of(beruComplaintsVipService));

        Ticket ticket = beruComplaintsUtils.createTicket(beruComplaintsVipService);

        mockSecurityDataService.setCurrentEmployee(currentEmployee);
        distributionService.doStart(currentEmployee);

        assertEquals(BeruComplaintsTicket.STATUS_PROCESSING, ticket.getStatus());

        ticketTestUtils.editTicketStatus(ticket, BeruComplaintsTicket.STATUS_RESOLVED);

        assertEquals(currentEmployee, ticket.getResponsibleEmployee());

    }

    @ParameterizedTest
    @MethodSource(value = "attributesForResettingResponsiblePersonWhenChangingQueueTest")
    @Description("""
            Сбрасывание ответственного при смене очереди
            Тест-кейс
            - https://testpalm2.yandex-team.ru/testcase/ocrm-790
            """)
    void resettingResponsiblePersonWhenChangingQueueTest(
            String teamCode,
            String firstServiceCode,
            String secondServiceCode,
            String escalationServiceCode) {

        Team team = beruComplaintsUtils.createTeamIfNotExists(teamCode);
        Service currentBeruComplaintsService = beruComplaintsUtils.createService(
                firstServiceCode,
                team,
                Brands.BERU_COMPLAINTS);
        Service currentBeruComplaintsVipService = beruComplaintsUtils.createService(
                secondServiceCode,
                team,
                Brands.BERU_COMPLAINTS);


        BeruComplaintsTicket ticket = beruComplaintsUtils.createTicket(currentBeruComplaintsService);
        Employee currentEmployee = createEmployee(team,
                Set.of(
                        currentBeruComplaintsService,
                        currentBeruComplaintsVipService
                )
        );
        mockSecurityDataService.setCurrentEmployee(currentEmployee);

        distributionService.doStart(currentEmployee);

        assertEquals(Ticket.STATUS_PROCESSING, ticket.getStatus());

        bcpService.edit(ticket,
                Map.of(
                        BeruComplaintsTicket.STATUS, BeruComplaintsTicket.STATUS_SERVICE_HAS_CHANGED,
                        BeruComplaintsTicket.SERVICE, escalationServiceCode != null ?
                                beruComplaintsUtils.createService(escalationServiceCode, team, Brands.BERU_COMPLAINTS) :
                                currentBeruComplaintsVipService
                )
        );

        assertEquals(escalationServiceCode == null ? currentEmployee : null, ticket.getResponsibleEmployee());
    }

    @ParameterizedTest
    @MethodSource(value = "attributeForCheckingBouncingRulesWhenCreatingTicketsTest")
    @Description("""
            Проверка правил отбивок при создании обращений
            тест-кейс https://testpalm2.yandex-team.ru/testcase/ocrm-768
            """)
    void checkingBouncingRulesWhenCreatingTicketsTest(String paymentType,
                                                      String paymentMethod,
                                                      Boolean isDropship,
                                                      Boolean withoutOrder) {
        TicketCategory ticketCategory = ticketTestUtils.createTicketCategory(beruComplaintsBrand);

        Map<String, Object> attributesForTicket = new HashMap<>(Map.of(
                BeruComplaintsTicket.CLIENT_EMAIL, Randoms.email(),
                BeruComplaintsTicket.TITLE, Randoms.string(),
                BeruComplaintsTicket.SERVICE, beruComplaintsService,
                BeruComplaintsTicket.CHANNEL, "mail",
                BeruComplaintsTicket.CATEGORIES, ticketCategory));

        Map<String, Object> attributesForBeruComplaintsReplyRule = new HashMap<>(Map.of(
                "dropship", isDropship,
                "categories", ticketCategory,
                "withoutOrder", withoutOrder,
                "emailBody", Randoms.string()));


        if (paymentMethod != null) {
            Order order = orderTestUtils.createOrder(
                    Map.of(
                            Order.PAYMENT_TYPE, paymentType,
                            Order.PAYMENT_METHOD, paymentMethod)
            );

            attributesForBeruComplaintsReplyRule.putAll(Map.of(
                    "paymentMethod", paymentMethod,
                    "paymentType", paymentType
            ));
            attributesForTicket.put(BeruComplaintsTicket.ORDER, order);

        }

        Entity beruComplaintsReplyRule = bcpService.create(Fqn.of("beruComplaintsReplyRule"),
                attributesForBeruComplaintsReplyRule);

        Ticket ticket = ticketTestUtils.createTicket(Fqn.of("ticket$beruComplaints"), attributesForTicket);

        Comment comment = (Comment) dbService.list(
                Query.of(Fqn.of("comment")).withFilters(
                        Filters.eq("entity", ticket)
                ).withSortingOrder(SortingOrder.desc("creationTime")).withLimit(1)
        ).get(0);
        String expectedEmailBody = beruComplaintsReplyRule.getAttribute("emailBody");

        assertEquals(expectedEmailBody, comment.getBody());
        assertEquals(PublicComment.FQN, comment.getFqn());

    }

    @Test
    @Description("""
            При удалении очереди у пользователя, у всех решенных пользователем
            обращений из этой очереди очищается ответственный
            тест-кейс - https://testpalm2.yandex-team.ru/testcase/ocrm-793
            """)
    void checkingCleaningOfResponsibleForTicketTest() {
        Employee currentEmployee = createEmployee(beruComplaintsTeam, Set.of(beruComplaintsVipService));

        Ticket ticket = beruComplaintsUtils.createTicket(beruComplaintsVipService);

        mockSecurityDataService.setCurrentEmployee(currentEmployee);
        distributionService.doStart(currentEmployee);

        assertEquals(BeruComplaintsTicket.STATUS_PROCESSING, ticket.getStatus());

        ticketTestUtils.editTicketStatus(ticket, BeruComplaintsTicket.STATUS_RESOLVED);
        assertEquals(currentEmployee, ticket.getResponsibleEmployee());

        bcpService.edit(currentEmployee, Map.of(
                "services", Set.of(),
                "personalEmail", "a@a.ru",
                "title", "test"
        ));

        assertNull(ticket.getResponsibleEmployee());
    }

    private Employee createEmployee(Team team, Set<Service> services) {
        final Ou ou = ouTestUtils.createOu();
        return bcpService.create(ru.yandex.market.jmf.module.ticket.Employee.FQN_DEFAULT, Map.of(
                ru.yandex.market.jmf.module.ticket.Employee.OU, ou,
                ru.yandex.market.jmf.module.ticket.Employee.TITLE, Randoms.string(),
                ru.yandex.market.jmf.module.ticket.Employee.UID, Randoms.longValue(),
                ru.yandex.market.jmf.module.ticket.Employee.TEAMS, Set.of(team),
                ru.yandex.market.jmf.module.ticket.Employee.SERVICES, services
        ));
    }


}
