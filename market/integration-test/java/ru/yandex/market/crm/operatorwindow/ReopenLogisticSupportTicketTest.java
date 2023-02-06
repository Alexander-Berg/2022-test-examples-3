package ru.yandex.market.crm.operatorwindow;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.operatorwindow.jmf.entity.BeruLogisticSupportTicket;
import ru.yandex.market.crm.operatorwindow.jmf.entity.LogisticSupportTicket;
import ru.yandex.market.crm.operatorwindow.jmf.entity.TicketCategory;
import ru.yandex.market.crm.operatorwindow.jmf.entity.TicketCategoryPriority;
import ru.yandex.market.crm.operatorwindow.utils.CategoryTestUtils;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.catalog.items.CatalogItem;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.comment.InternalComment;
import ru.yandex.market.jmf.module.comment.test.impl.CommentTestUtils;
import ru.yandex.market.jmf.module.ou.Ou;
import ru.yandex.market.jmf.module.ou.impl.OuTestUtils;
import ru.yandex.market.jmf.module.ticket.Brand;
import ru.yandex.market.jmf.module.ticket.DistributionAlgorithm;
import ru.yandex.market.jmf.module.ticket.DistributionService;
import ru.yandex.market.jmf.module.ticket.Employee;
import ru.yandex.market.jmf.module.ticket.EmployeeDistributionStatus;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Team;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.distribution.DistributionUtils;
import ru.yandex.market.jmf.module.ticket.test.TestChannels;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.security.test.impl.MockSecurityDataService;
import ru.yandex.market.jmf.timings.ServiceTime;
import ru.yandex.market.jmf.timings.test.impl.ServiceTimeTestUtils;
import ru.yandex.market.jmf.timings.test.impl.TimerTestUtils;
import ru.yandex.market.jmf.utils.Maps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.jmf.logic.wf.HasWorkflow.STATUS;
import static ru.yandex.market.jmf.module.ticket.Ticket.CATEGORIES;
import static ru.yandex.market.jmf.module.ticket.Ticket.CHANNEL;
import static ru.yandex.market.jmf.module.ticket.Ticket.CLIENT_EMAIL;
import static ru.yandex.market.jmf.module.ticket.Ticket.CLIENT_NAME;
import static ru.yandex.market.jmf.module.ticket.Ticket.CLIENT_PHONE;
import static ru.yandex.market.jmf.module.ticket.Ticket.DESCRIPTION;
import static ru.yandex.market.jmf.module.ticket.Ticket.PRIORITY;
import static ru.yandex.market.jmf.module.ticket.Ticket.RESPONSIBLE_TEAM;
import static ru.yandex.market.jmf.module.ticket.Ticket.SERVICE;
import static ru.yandex.market.jmf.module.ticket.Ticket.STATUS_REGISTERED;
import static ru.yandex.market.jmf.module.ticket.Ticket.STATUS_REOPENED;
import static ru.yandex.market.jmf.module.ticket.Ticket.TITLE;

@Transactional
public class ReopenLogisticSupportTicketTest extends AbstractModuleOwTest {

    public static final List<Duration> testPriorityDurations = List.of(
            Duration.ofSeconds(15),
            Duration.ofSeconds(10),
            Duration.ofSeconds(15)
    );

    public static final List<Duration> critPlusPriorityDurations = List.of(
            Duration.ofMinutes(30),
            Duration.ofHours(4),
            Duration.ofHours(2)
    );

    @Inject
    private TimerTestUtils timerTestUtils;
    @Inject
    private TicketTestUtils ticketTestUtils;
    @Inject
    private CommentTestUtils commentTestUtils;
    @Inject
    private DbService dbService;
    @Inject
    private DistributionService distributionService;
    @Inject
    private DistributionUtils distributionUtils;
    @Inject
    private ServiceTimeTestUtils serviceTimeTestUtils;
    @Inject
    private BcpService bcpService;
    @Inject
    private CategoryTestUtils categoryTestUtils;
    @Inject
    private OuTestUtils ouTestUtils;
    @Inject
    private MockSecurityDataService mockSecurityDataService;

    @BeforeEach
    public void setUp() {
        createCategoryPriority("test", 110);
    }

    @ParameterizedTest
    @ValueSource(strings = {"waitingDeliveryService", "waitingWarehouse"})
    public void reopenTicket__afterWaitingDeliveryService(String waitingStatus) {
        // Создадим сотрудника
        var team = createTeam();
        var service = createService(team);
        var employee = createEmployee(team, service);
        mockSecurityDataService.setCurrentEmployee(employee);
        EmployeeDistributionStatus employeeDistributionStatus = createDistributionStatus(employee);
        distributionService.doStart(employee);

        // Создадим тикет
        var categoryPriority = getCategoryPriority("test");
        var beruCategory = createTicketCategory(Constants.Brand.BERU_LOGISTIC_SUPPORT, categoryPriority);
        var ticket = createDefaultTicket(
                LogisticSupportTicket.FQN,
                List.of(beruCategory),
                team,
                service
        );

        distributionUtils.getTicket();

        // Проверим ответственного
        assertEquals(employee, ticket.getResponsibleEmployee());
        assertEquals(EmployeeDistributionStatus.STATUS_PROCESSING, employeeDistributionStatus.getStatus());

        // Переведем его в необходимый статус "в ожидании ..."
        changeTicketStatus(ticket, waitingStatus);

        // Проверим, что статус тикета стал "в ожидании ..."
        assertEquals(waitingStatus, ticket.getStatus());

        // Проверим ответственного
        assertEquals(employee, ticket.getResponsibleEmployee());
        assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TICKET, employeeDistributionStatus.getStatus());

        // Переведем тикет в статус "переоткрыт"
        changeTicketStatus(ticket, STATUS_REOPENED);

        // Проверим, что статус тикета стал "Переоткрыт"
        assertEquals(Ticket.STATUS_REOPENED, ticket.getStatus());

        // Проверим ответственного
        assertEquals(employee, ticket.getResponsibleEmployee());
        assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TAKEN, employeeDistributionStatus.getStatus());
    }

    @NotNull
    private EmployeeDistributionStatus createDistributionStatus(Employee employee) {
        return bcpService.create(EmployeeDistributionStatus.FQN, Map.of(
                EmployeeDistributionStatus.EMPLOYEE, employee
        ));
    }

    private Service createService(Team team) {
        return ticketTestUtils.createService(Map.of(
                Service.SERVICE_TIME, serviceTimeTestUtils.createServiceTime24x7(),
                Service.RESPONSIBLE_TEAM, team,
                Service.BRAND, ticketTestUtils.createBrand(),
                Service.CODE, "logisticSupport"
        ));
    }

    private Team createTeam() {
        return ticketTestUtils.createTeam(Map.of(
                Team.DISTRIBUTION_ALGORITHM,
                DistributionAlgorithm.SINGLE_RESPONSIBLE
        ));
    }

    @ParameterizedTest
    @ValueSource(strings = {"waitingDeliveryService", "waitingWarehouse"})
    public void reopenTicket__straightScenario(String waitingStatus) {
        // Создадим тикет
        var categoryPriority = getCategoryPriority("test");
        var beruCategory = createTicketCategory(Constants.Brand.BERU_LOGISTIC_SUPPORT, categoryPriority);
        var ticket = createDefaultTicket(
                LogisticSupportTicket.FQN,
                Constants.Brand.BERU_LOGISTIC_SUPPORT,
                List.of(beruCategory)
        );

        // Переведем его в необходимый статус "в ожидании ..."
        changeTicketStatus(ticket, waitingStatus);

        // Проверим, что сработало выставление времени в триггере редактирования тикета
        // и что выбралось первое время из списка (15с)
        assertEquals(testPriorityDurations.get(0), ticket.getReopenTime());

        // Сэмулируем срабатывание таймера переоткрытия тикета
        exceedReopenTimerOnTicket(ticket);

        // Проверим, что статус тикета стал "Переоткрыт"
        assertEquals(Ticket.STATUS_REOPENED, ticket.getStatus());

        // Провреяем, что т.к. поменялся статус, то время для таймера сбросилось
        Assertions.assertNull(ticket.getReopenTime());

        // Добавился один внутренний комментарий
        assertComments(commentTestUtils.getComments(ticket), 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"waitingDeliveryService", "waitingWarehouse"})
    public void reopenTicket__multipleReopens(String waitingStatus) {
        var categoryPriority = getCategoryPriority("test");
        var beruCategory = createTicketCategory(Constants.Brand.BERU_LOGISTIC_SUPPORT, categoryPriority);
        var ticket = createDefaultTicket(
                LogisticSupportTicket.FQN,
                Constants.Brand.BERU_LOGISTIC_SUPPORT,
                List.of(beruCategory)
        );
        final int repeatCount = 4;

        // пройдем обычный сценарий 4 раза
        for (int i = 0; i < repeatCount; i++) {
            changeTicketStatus(ticket, waitingStatus);
            // Проверим, что сработало выставление времени в триггере редактирования тикета
            // и что выбралось i-ое время из списка (15c, 10c, 15c)
            assertEquals(testPriorityDurations.get(i % testPriorityDurations.size()), ticket.getReopenTime());
            // Сэмулируем срабатывание таймера переоткрытия тикета
            exceedReopenTimerOnTicket(ticket);
        }

        assertComments(commentTestUtils.getComments(ticket), repeatCount);
    }

    @ParameterizedTest
    @ValueSource(strings = {"waitingDeliveryService", "waitingWarehouse"})
    public void reopenTicket__onSlaExceed(String waitingStatus) {
        var ticket = createDefaultTicket(LogisticSupportTicket.FQN, Constants.Brand.BERU_LOGISTIC_SUPPORT, List.of());

        changeTicketStatus(ticket, waitingStatus);
        exceedSlaTimerOnTicket(ticket);


        assertEquals(Ticket.STATUS_REOPENED, ticket.getStatus());
        // проверим, что оставили один комментарий
        assertComments(commentTestUtils.getComments(ticket), 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"waitingDeliveryService", "waitingWarehouse"})
    // При отсутствии приотитета у категорий не нужно включать таймер переоткрытия
    public void setReopenTimer__noCategoryPriority(String waitingStatus) {
        var beruCategory = createTicketCategory(Constants.Brand.BERU_LOGISTIC_SUPPORT, null);
        var ticket = createDefaultTicket(
                LogisticSupportTicket.FQN,
                Constants.Brand.BERU_LOGISTIC_SUPPORT,
                List.of(beruCategory)
        );

        changeTicketStatus(ticket, waitingStatus);

        // Проверим, что время переоткрытия не установилось
        assertNull(ticket.getReopenTime());
    }

    @ParameterizedTest
    @ValueSource(strings = {"waitingDeliveryService", "waitingWarehouse"})
    // Если выбраны несколько категорий с разными приоритетами должно выбираться самое приоритетное расписание
    public void setReopenTimer__multipleCategoryWithDifferentPriority(String waitingStatus) {
        var critPriority = getCategoryPriority("critical");
        var critPlusPriority = getCategoryPriority("critical_plus");
        var yaDoCategoryCritPlus = createTicketCategory(
                Constants.Brand.YANDEX_DELIVERY_LOGISTIC_SUPPORT,
                critPlusPriority
        );
        var yaDoCategoryCrit = createTicketCategory(
                Constants.Brand.YANDEX_DELIVERY_LOGISTIC_SUPPORT,
                critPriority
        );
        var ticket = createDefaultTicket(
                LogisticSupportTicket.FQN,
                Constants.Brand.BERU_LOGISTIC_SUPPORT,
                List.of(yaDoCategoryCrit, yaDoCategoryCritPlus)
        );

        changeTicketStatus(ticket, waitingStatus);

        // Проверим, что время переоткрытия установилось в соответствии с приоритетом critical_plus
        assertEquals(critPlusPriorityDurations.get(0), ticket.getReopenTime());
    }

    @ParameterizedTest
    @ValueSource(strings = {"waitingDeliveryService", "waitingWarehouse"})
    // Если выбраны несколько категорий, и одна из них не имеет приоритета - выбирать время из той, что с приоритетом
    public void setReopenTimer__multipleCategoryOneWithoutPriority(String waitingStatus) {
        // Создадим тикет
        var testPriority = getCategoryPriority("test");
        var categoryTest = createTicketCategory(Constants.Brand.BERU_LOGISTIC_SUPPORT, testPriority);
        var categoryWithoutPriority = createTicketCategory(Constants.Brand.BERU_LOGISTIC_SUPPORT, null);
        var ticket = createDefaultTicket(
                BeruLogisticSupportTicket.FQN,
                Constants.Brand.BERU_LOGISTIC_SUPPORT,
                List.of(categoryTest, categoryWithoutPriority)
        );

        changeTicketStatus(ticket, waitingStatus);

        // Проверим, что время переоткрытия установилось в соответствии с приоритетом test
        assertEquals(testPriorityDurations.get(0), ticket.getReopenTime());
    }

    @ParameterizedTest
    @ValueSource(strings = {"waitingDeliveryService", "waitingWarehouse"})
    // При переходе вручную из статуса "В ожидании СД" таймер переоткрытия должен останавливаться
    public void stopReopenTimer__onChangeStatus(String waitingStatus) {
        var testPriority = getCategoryPriority("test");
        var categoryTest = createTicketCategory(Constants.Brand.BERU_LOGISTIC_SUPPORT, testPriority);
        var ticket = createDefaultTicket(
                BeruLogisticSupportTicket.FQN,
                Constants.Brand.BERU_LOGISTIC_SUPPORT,
                List.of(categoryTest)
        );

        changeTicketStatus(ticket, waitingStatus);
        assertEquals(testPriorityDurations.get(0), ticket.getReopenTime());

        // Вручную переносим тикет в статус переоткрыто
        changeTicketStatus(ticket, Ticket.STATUS_REOPENED);

        assertNull(ticket.getReopenTime());

        // прверим, что нет ни одного комментария
        assertComments(commentTestUtils.getComments(ticket), 0);
    }

    private Employee createEmployee(Team team, Service service) {
        final Ou ou = ouTestUtils.createOu();
        return bcpService.create(Employee.FQN_DEFAULT, Map.of(
                Employee.OU, ou,
                Employee.TITLE, Randoms.string(),
                Employee.UID, Randoms.longValue(),
                Employee.TEAMS, Set.of(team),
                Employee.SERVICES, Set.of(service)
        ));
    }

    private void exceedReopenTimerOnTicket(Ticket ticket) {
        timerTestUtils.simulateTimerExpiration(ticket.getGid(), "reopenTimer");
    }

    private void exceedSlaTimerOnTicket(Ticket ticket) {
        timerTestUtils.simulateTimerExpiration(ticket.getGid(), "allowanceTimer");
    }

    private void assertComments(List<Comment> comments, int count) {
        assertEquals(count, comments.size());

        comments.forEach(comment -> {
            assertEquals(InternalComment.FQN, comment.getFqn());
            // И Не пустой
            Assertions.assertNotNull(comment.getBody());
        });
    }

    private void changeTicketStatus(Ticket ticket, String status) {
        bcpService.edit(ticket, Map.of(
                Ticket.STATUS, status
        ));
    }

    private TicketCategoryPriority getCategoryPriority(String code) {
        return categoryTestUtils.getCategoryPriority(code);
    }

    private <T extends LogisticSupportTicket> T createDefaultTicket(
            Fqn fqn, String brandCode,
            List<TicketCategory> categories
    ) {
        var responsibleTeam = ticketTestUtils.createTeam();

        var serviceTime = dbService.getByNaturalId(ServiceTime.FQN, CatalogItem.CODE, "08_22");
        serviceTimeTestUtils.createPeriod(serviceTime, "monday", "08:00", "22:00");

        var brand = dbService.getByNaturalId(Brand.FQN, Brand.CODE, brandCode);

        var service = ticketTestUtils.createService(
                responsibleTeam,
                serviceTime,
                brand,
                Optional.of("logisticSupport")
        );

        Map<String, Object> attributes = Maps.of(
                TITLE, Randoms.string(),
                DESCRIPTION, Randoms.string(),
                CHANNEL, TestChannels.CH1,
                CLIENT_NAME, Randoms.string(),
                CLIENT_EMAIL, Randoms.email(),
                CLIENT_PHONE, Randoms.phoneNumber(),
                SERVICE, service,
                PRIORITY, ticketTestUtils.createPriority(),
                RESPONSIBLE_TEAM, responsibleTeam,
                CATEGORIES, categories,
                STATUS, STATUS_REGISTERED);
        return bcpService.create(fqn, attributes);
    }

    private <T extends LogisticSupportTicket> T createDefaultTicket(
            Fqn fqn,
            List<TicketCategory> categories,
            Team team,
            Service service
    ) {
        var serviceTime = dbService.getByNaturalId(ServiceTime.FQN, CatalogItem.CODE, "08_22");
        serviceTimeTestUtils.createPeriod(serviceTime, "monday", "08:00", "22:00");

        Map<String, Object> attributes = Maps.of(
                TITLE, Randoms.string(),
                DESCRIPTION, Randoms.string(),
                CHANNEL, TestChannels.CH1,
                CLIENT_NAME, Randoms.string(),
                CLIENT_EMAIL, Randoms.email(),
                CLIENT_PHONE, Randoms.phoneNumber(),
                SERVICE, service,
                PRIORITY, ticketTestUtils.createPriority(),
                RESPONSIBLE_TEAM, team,
                CATEGORIES, categories,
                STATUS, STATUS_REGISTERED
        );
        return bcpService.create(fqn, attributes);
    }

    private TicketCategoryPriority createCategoryPriority(String code, int level) {
        return categoryTestUtils.createCategoryPriority(code, level);
    }

    private TicketCategory createTicketCategory(String brandCode, TicketCategoryPriority categoryPriority) {
        return categoryTestUtils.createTicketCategory(brandCode, categoryPriority);
    }
}
