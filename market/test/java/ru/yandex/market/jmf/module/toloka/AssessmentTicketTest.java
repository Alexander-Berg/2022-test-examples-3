package ru.yandex.market.jmf.module.toloka;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.logic.wf.HasWorkflow;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.module.toloka.model.TolokaServer;
import ru.yandex.market.jmf.module.toloka.utils.AssessmentTicketUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
@SpringJUnitConfig(classes = ModuleTolokaTestConfiguration.class)
public class AssessmentTicketTest {

    private static final Fqn ASSESSMENT_TICKET_FQN = Fqn.of("ticket$tolokaAssessmentTest");
    public static final TolokaServer TOLOKA_SERVER_ID = TolokaServer.YANG;

    @Inject
    TicketTestUtils ticketTestUtils;

    @Inject
    BcpService bcpService;

    @Inject
    AssessmentTicketUtils assessmentTicketUtils;

    @Test
    public void testDefaultAssessmentCounter() {
        AssessmentTicket ticket = ticketTestUtils.createTicket(ASSESSMENT_TICKET_FQN, Map.of());
        changeStatus(ticket, AssessmentTicket.STATUS_ASSESSMENT_REQUIRED);
        changeStatus(ticket, AssessmentTicket.STATUS_ASSESSMENT);
        changeStatus(ticket, AssessmentTicket.STATUS_REOPENED);

        assertThrows(RuntimeException.class, () -> changeStatus(ticket, AssessmentTicket.STATUS_ASSESSMENT_REQUIRED));
    }

    @Test
    public void testAssessmentCounter() {
        Service service = createService(2);
        AssessmentTicket ticket = ticketTestUtils.createTicket(ASSESSMENT_TICKET_FQN, Map.of(Ticket.SERVICE, service));
        changeStatus(ticket, AssessmentTicket.STATUS_ASSESSMENT_REQUIRED);
        changeStatus(ticket, AssessmentTicket.STATUS_ASSESSMENT);
        changeStatus(ticket, AssessmentTicket.STATUS_REOPENED);

        assertNotNull(ticket.getAssessmentPool());
        assertNotNull(ticket.getAssessmentTaskId());

        changeStatus(ticket, AssessmentTicket.STATUS_ASSESSMENT_REQUIRED);
        assertNull(ticket.getAssessmentPool());
        assertNull(ticket.getAssessmentTaskId());

        changeStatus(ticket, AssessmentTicket.STATUS_ASSESSMENT);
        changeStatus(ticket, AssessmentTicket.STATUS_REOPENED);

        assertThrows(RuntimeException.class, () -> changeStatus(ticket, AssessmentTicket.STATUS_ASSESSMENT_REQUIRED));
    }

    private void changeStatus(Ticket ticket, String status) {
        bcpService.edit(ticket, Map.of(HasWorkflow.STATUS, status));
        if (AssessmentTicket.STATUS_ASSESSMENT.equals(status)) {
            bcpService.edit(ticket, Map.of(
                    AssessmentTicket.ASSESSMENT_POOL, assessmentTicketUtils.createAssessmentPool(TOLOKA_SERVER_ID),
                    AssessmentTicket.ASSESSMENT_TASK_ID, Randoms.string()
            ));
        }
    }

    private Service createService(long maxAssessmentCount) {
        Service service = ticketTestUtils.createService24x7();
        bcpService.edit(service, Map.of(
                ru.yandex.market.jmf.module.toloka.Service.MAX_ASSESSMENT_COUNT, maxAssessmentCount
        ));
        return service;
    }
}
