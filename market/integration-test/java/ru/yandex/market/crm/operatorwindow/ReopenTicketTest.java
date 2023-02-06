package ru.yandex.market.crm.operatorwindow;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.comment.InternalComment;
import ru.yandex.market.jmf.module.comment.operations.AddCommentOperationHandler;
import ru.yandex.market.jmf.module.ou.Employee;
import ru.yandex.market.jmf.module.ticket.Brand;
import ru.yandex.market.jmf.module.ticket.Resolution;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Team;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.timings.ServiceTime;
import ru.yandex.market.jmf.timings.test.impl.ServiceTimeTestUtils;

@Transactional
public class ReopenTicketTest extends AbstractModuleOwTest {

    private static final Fqn TICKET_FIRST_LINE_FQN = Fqn.of("ticket$firstLine");

    @Inject
    private BcpService bcpService;

    @Inject
    private ServiceTimeTestUtils serviceTimeTestUtils;
    @Inject
    private TicketTestUtils ticketTestUtils;


    private Ticket ticket;

    @BeforeEach
    public void setUp() {
        ServiceTime serviceTime = serviceTimeTestUtils.createServiceTime24x7();
        Brand brand = ticketTestUtils.createBrand();
        Team team = ticketTestUtils.createTeam();
        Service service = createService(serviceTime, brand, team);

        ticket = createTicket(service, team);
    }

    @Test
    public void reopenWithResolution() {
        bcpService.edit(ticket, Map.of(Ticket.STATUS, Ticket.STATUS_REOPENED,
                Ticket.RESOLUTION, Resolution.RESPONSE_FROM_DELIVERY_CLIENT));
        Assertions.assertEquals(Resolution.RESPONSE_FROM_DELIVERY_CLIENT, ticket.getResolution().getCode());
    }

    @Test
    public void autoReopen() {
        bcpService.edit(ticket, Map.of(Ticket.STATUS, Ticket.STATUS_REOPENED));
        Assertions.assertEquals(Resolution.AUTO_REOPENED, ticket.getResolution().getCode());
    }

    @Test
    public void reopenByUser() {
        setCurrentUser(createEmployee());
        bcpService.edit(ticket, Map.of(Ticket.STATUS, Ticket.STATUS_REOPENED));
        Assertions.assertEquals(Resolution.REOPENED_BY_EMPLOYEE, ticket.getResolution().getCode());
    }

    @Test
    public void reopenByUserWhenResolutionIsAlreadySet() {
        setCurrentUser(createEmployee());
        bcpService.edit(ticket, Map.of(Ticket.RESOLUTION, Resolution.AUTO_REOPENED));
        bcpService.edit(ticket, Map.of(Ticket.STATUS, Ticket.STATUS_REOPENED));
        Assertions.assertEquals(Resolution.REOPENED_BY_EMPLOYEE, ticket.getResolution().getCode());
    }

    private void setCurrentUser(Employee employee) {
        securityDataService.setInitialEmployee(employee);
        securityDataService.setCurrentUserProfiles("admin");
        authRunnerService.setCurrentUserSuperUser(true);
    }

    private Service createService(ServiceTime serviceTime, Brand brand, Team team) {
        return ticketTestUtils.createService(
                Map.of(
                        Service.SERVICE_TIME, serviceTime,
                        Service.RESPONSIBLE_TEAM, team,
                        Service.BRAND, brand
                ));
    }

    private Ticket createTicket(Service service, Team team) {
        return ticketTestUtils.createTicket(TICKET_FIRST_LINE_FQN, team, service,
                Map.of(Ticket.STATUS, Ticket.STATUS_RESOLVED,
                        AddCommentOperationHandler.ID, Map.of(
                                Comment.METACLASS, InternalComment.FQN,
                                Comment.BODY, "commentWhichIsRequiredForResolvedStatus")
                ));
    }

    private Employee createEmployee() {
        return ticketTestUtils.createEmployee(ticketTestUtils.createOu());
    }

}
