package ru.yandex.market.jmf.module.ticket.test;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.ou.Ou;
import ru.yandex.market.jmf.module.ou.impl.OuTestUtils;
import ru.yandex.market.jmf.module.ticket.DistributionAlgorithm;
import ru.yandex.market.jmf.module.ticket.DistributionService;
import ru.yandex.market.jmf.module.ticket.Employee;
import ru.yandex.market.jmf.module.ticket.EmployeeDistributionStatus;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Team;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.security.test.impl.MockSecurityDataService;
import ru.yandex.market.jmf.timings.test.impl.ServiceTimeTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Transactional
@SpringJUnitConfig(classes = ModuleTicketTestConfiguration.class)
public class SingleResponsibleDistributionAlgorithmTest {

    @Inject
    private TicketTestUtils ticketTestUtils;

    @Inject
    private ServiceTimeTestUtils serviceTimeTestUtils;

    @Inject
    private OuTestUtils ouTestUtils;

    @Inject
    private BcpService bcpService;

    @Inject
    private DistributionService distributionService;

    @Inject
    private MockSecurityDataService mockSecurityDataService;

    private Team team;
    private Service service;
    private Employee employee;
    private EmployeeDistributionStatus employeeDistributionStatus;

    @BeforeEach
    public void setUp() {
        team = ticketTestUtils.createTeam(Map.of(
                Team.DISTRIBUTION_ALGORITHM, DistributionAlgorithm.SINGLE_RESPONSIBLE
        ));
        Entity brand = ticketTestUtils.createBrand();
        service = ticketTestUtils.createService(
                Map.of(
                        Service.SERVICE_TIME, serviceTimeTestUtils.createServiceTime24x7(),
                        Service.RESPONSIBLE_TEAM, team,
                        Service.BRAND, brand));
        employee = createEmployee();
        employeeDistributionStatus = createEmployeeDistributionStatus(employee);
    }

    @AfterEach
    public void tearDown() {
        mockSecurityDataService.reset();
    }

    @Test
    public void testAssignTicketToEmployee() {
        Ticket ticket = createTicket();
        mockSecurityDataService.setInitialEmployee(employee);
        distributionService.doStart(employee);


        assertEquals(employee, ticket.getResponsibleEmployee());
        assertEquals(EmployeeDistributionStatus.STATUS_PROCESSING, employeeDistributionStatus.getStatus());
    }

    @Test
    public void testResolveTicket() {
        Ticket ticket = createTicket();
        mockSecurityDataService.setInitialEmployee(employee);
        distributionService.doStart(employee);
        changeStatus(ticket, Ticket.STATUS_RESOLVED);

        assertEquals(employee, ticket.getResponsibleEmployee());
        assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TICKET, employeeDistributionStatus.getStatus());
    }

    @Test
    public void testReopenTicket() {
        Ticket ticket = createTicket();
        mockSecurityDataService.setInitialEmployee(employee);
        distributionService.doStart(employee);
        changeStatus(ticket, Ticket.STATUS_RESOLVED);
        changeStatus(ticket, Ticket.STATUS_REOPENED);

        assertEquals(employee, ticket.getResponsibleEmployee());
        assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TAKEN, employeeDistributionStatus.getStatus());
    }

    @Test
    public void testAssignTicketToEmployeeWhenProcessOtherTicket() {
        Ticket ticket1 = createTicket();
        mockSecurityDataService.setInitialEmployee(employee);
        distributionService.doStart(employee);

        Ticket ticket2 = createTicket();
        setResponsible(ticket2, employee);

        assertEquals(employee, ticket1.getResponsibleEmployee());
        assertEquals(EmployeeDistributionStatus.STATUS_PROCESSING, employeeDistributionStatus.getStatus());
        assertEquals(employee, ticket2.getResponsibleEmployee());
        assertEquals(Ticket.STATUS_REGISTERED, ticket2.getStatus());
    }

    private void changeStatus(Ticket ticket, String status) {
        bcpService.edit(ticket, Map.of(Ticket.STATUS, status));
    }

    private void setResponsible(Ticket ticket, Employee employee) {
        bcpService.edit(ticket, Map.of(Ticket.RESPONSIBLE_EMPLOYEE, employee));
    }

    private Ticket createTicket() {
        return ticketTestUtils.createTicket(Fqn.of("ticket$test"),
                team,
                service,
                Map.of(Ticket.WAIT_DISTRIBUTION, true)
        );
    }

    private Employee createEmployee() {
        final Ou ou = ouTestUtils.createOu();
        return bcpService.create(Employee.FQN_DEFAULT, Map.of(
                Employee.OU, ou,
                Employee.TITLE, Randoms.string(),
                Employee.UID, Randoms.longValue(),
                Employee.TEAMS, Set.of(team),
                Employee.SERVICES, Set.of(service)
        ));
    }

    private EmployeeDistributionStatus createEmployeeDistributionStatus(Employee employee) {
        return bcpService.create(EmployeeDistributionStatus.FQN, Map.of(
                EmployeeDistributionStatus.EMPLOYEE, employee
        ));
    }
}
