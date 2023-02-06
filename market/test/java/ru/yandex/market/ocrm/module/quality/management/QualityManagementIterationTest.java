package ru.yandex.market.ocrm.module.quality.management;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.CrmCollections;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.logic.wf.bcp.ValidateWfRequiredAttributesOperationHandler;
import ru.yandex.market.jmf.logic.wf.bcp.WfConstants;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.ticket.Resolution;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Team;
import ru.yandex.market.jmf.module.ticket.test.TestChannels;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.timings.test.impl.TimerTestUtils;
import ru.yandex.market.ocrm.module.quality.management.domain.QualityManagementIteration;
import ru.yandex.market.ocrm.module.quality.management.domain.Ticket;

@Transactional
@SpringJUnitConfig(classes = ModuleQualityManagementTestConfiguration.class)
public class QualityManagementIterationTest {

    private static final Fqn TEST_FQN = Fqn.of("ticket$testQM");

    @Inject
    private TicketTestUtils ticketTestUtils;
    @Inject
    private BcpService bcpService;
    @Inject
    private TimerTestUtils timerTestUtils;
    private TicketTestUtils.TestContext ctx;

    @BeforeEach
    public void setUp() {
        ctx = ticketTestUtils.create();
    }

    @Test
    public void one_iteration() {
        var employee = ctx.employee0;

        Ticket ticket = createTicket(TEST_FQN, Map.of(Ticket.RESPONSIBLE_EMPLOYEE, employee));

        ticket = editTicketStatus(ticket, Ticket.STATUS_PROCESSING);
        QualityManagementIteration qualityManagementIteration = ticket.getQualityManagementIteration();
        Assertions.assertNotNull(qualityManagementIteration);
        Assertions.assertNotNull(qualityManagementIteration.getStartTime());
        Assertions.assertNull(qualityManagementIteration.getEndTime());
        Assertions.assertEquals(qualityManagementIteration.getResponsibleEmployee(), employee);

        ticket = editTicketStatus(ticket, Ticket.STATUS_RESOLVED);
        QualityManagementIteration newQualityManagementIteration = ticket.getQualityManagementIteration();
        Assertions.assertNull(newQualityManagementIteration);
        Assertions.assertNotNull(qualityManagementIteration.getEndTime());
    }

    //https://testpalm2.yandex-team.ru/testcase/ocrm-1034 Создание итерации при обработке обращения
    @Test
    public void one_iteration_longWay() {
        var employee = ctx.employee0;

        Ticket ticket = createTicket(TEST_FQN, Map.of(Ticket.RESPONSIBLE_EMPLOYEE, employee));

        ticket = editTicketStatus(ticket, Ticket.STATUS_PROCESSING);
        QualityManagementIteration qualityManagementIteration = ticket.getQualityManagementIteration();
        ticket = editTicketStatus(ticket, Ticket.STATUS_PENDING);
        QualityManagementIteration qualityManagementIteration2 = ticket.getQualityManagementIteration();
        //Итерация не изменился, т.к. тикет до сих пор в isProcessing
        Assertions.assertEquals(qualityManagementIteration, qualityManagementIteration2);
        ticket = editTicketStatus(ticket, Ticket.STATUS_RESOLVED);
        QualityManagementIteration newQualityManagementIteration = ticket.getQualityManagementIteration();
        Assertions.assertNull(newQualityManagementIteration);
    }

    @Test
    public void two_iterations() {
        var employee = ctx.employee0;

        Ticket ticket = createTicket(TEST_FQN, Map.of(Ticket.RESPONSIBLE_EMPLOYEE, employee));

        ticket = editTicketStatus(ticket, Ticket.STATUS_PROCESSING);
        QualityManagementIteration qualityManagementIteration = ticket.getQualityManagementIteration();
        Assertions.assertNotNull(qualityManagementIteration);

        ticket = editTicketStatus(ticket, Ticket.STATUS_RESOLVED);
        QualityManagementIteration qualityManagementIterationNull = ticket.getQualityManagementIteration();
        Assertions.assertNull(qualityManagementIterationNull);

        ticket = editTicketStatus(ticket, Ticket.STATUS_REOPENED);
        ticket = editTicketStatus(ticket, Ticket.STATUS_PROCESSING, Map.of(Ticket.RESPONSIBLE_EMPLOYEE, employee));
        QualityManagementIteration qualityManagementIteration2 = ticket.getQualityManagementIteration();
        Assertions.assertNotNull(qualityManagementIteration2);
        Assertions.assertNotEquals(qualityManagementIteration.getGid(), qualityManagementIteration2.getGid());

        ticket = editTicketStatus(ticket, Ticket.STATUS_RESOLVED);
        qualityManagementIterationNull = ticket.getQualityManagementIteration();
        Assertions.assertNull(qualityManagementIterationNull);
    }

    @Test
    public void two_iteration_with_reopenedByTimer() {
        var employee = ctx.employee0;

        Ticket ticket = createTicket(TEST_FQN, Map.of(Ticket.RESPONSIBLE_EMPLOYEE, employee));

        ticket = editTicketStatus(ticket, Ticket.STATUS_PROCESSING);
        QualityManagementIteration qualityManagementIteration = ticket.getQualityManagementIteration();
        Assertions.assertNotNull(qualityManagementIteration);

        timerTestUtils.simulateTimerExpiration(ticket.getGid(), "processingBackTimer");
        QualityManagementIteration qualityManagementIterationNull = ticket.getQualityManagementIteration();
        Assertions.assertNull(qualityManagementIterationNull);

        ticket = editTicketStatus(ticket, Ticket.STATUS_PROCESSING, Map.of(Ticket.RESPONSIBLE_EMPLOYEE, employee));
        QualityManagementIteration qualityManagementIteration2 = ticket.getQualityManagementIteration();
        Assertions.assertNotNull(qualityManagementIteration2);
        Assertions.assertNotEquals(qualityManagementIteration.getGid(), qualityManagementIteration2.getGid());

        ticket = editTicketStatus(ticket, Ticket.STATUS_RESOLVED);
        qualityManagementIterationNull = ticket.getQualityManagementIteration();
        Assertions.assertNull(qualityManagementIterationNull);
    }

    private Ticket createTicket(Fqn fqn, Map<String, Object> attributes) {
        final Team team = ticketTestUtils.createTeam();
        final Service service = ticketTestUtils.createService24x7(team);
        ticketTestUtils.createResolution(Resolution.AUTO_REOPENED, service.getBrand());

        return createTicket(fqn,
                team,
                service,
                attributes);
    }

    private Ticket createTicket(Fqn fqn,
                                Team team,
                                Service service,
                                Map<String, Object> additionalAttributes) {
        Map<String, Object> initialAttributes = Map.of(
                Ticket.TITLE, Randoms.string(),
                Ticket.CHANNEL, TestChannels.CH1,
                Ticket.SERVICE, service,
                Ticket.PRIORITY, ticketTestUtils.createPriority(),
                Ticket.RESPONSIBLE_TEAM, team
        );

        final HashMap<String, Object> attributes = new HashMap<>();
        attributes.putAll(initialAttributes);
        attributes.putAll(CrmCollections.nullToEmpty(additionalAttributes));
        return bcpService.create(fqn, attributes);
    }

    private Ticket editTicketStatus(Ticket ticket, String status) {
        return editTicketStatus(ticket, status, Map.of());
    }

    private Ticket editTicketStatus(Ticket ticket, String status, Map<String, Object> additionalAttributes) {
        final HashMap<String, Object> attributes = new HashMap<>();
        attributes.put(Ticket.STATUS, status);
        attributes.putAll(CrmCollections.nullToEmpty(additionalAttributes));

        return bcpService.edit(ticket,
                attributes,
                Map.of(
                        WfConstants.SKIP_WF_STATUS_CHANGE_CHECKING_ATTRIBUTE, true,
                        ValidateWfRequiredAttributesOperationHandler.SKIP_WF_REQUIRED_ATTRIBUTES_VALIDATION, true
                )
        );
    }
}
