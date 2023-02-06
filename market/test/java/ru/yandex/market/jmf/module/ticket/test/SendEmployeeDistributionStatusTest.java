package ru.yandex.market.jmf.module.ticket.test;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.ou.Ou;
import ru.yandex.market.jmf.module.ou.impl.OuTestUtils;
import ru.yandex.market.jmf.module.ticket.DistributionService;
import ru.yandex.market.jmf.module.ticket.Employee;
import ru.yandex.market.jmf.module.ticket.EmployeeDistributionStatus;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Team;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.distribution.DistributionUtils;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.module.xiva.XivaPersonalClient;
import ru.yandex.market.jmf.module.xiva.request.SendRequest;
import ru.yandex.market.jmf.module.xiva.request.SendRequestParams;
import ru.yandex.market.jmf.module.xiva.request.XivaRequestUtils;
import ru.yandex.market.jmf.security.SecurityDataService;
import ru.yandex.market.jmf.security.test.impl.MockSecurityDataService;
import ru.yandex.market.jmf.timings.test.impl.ServiceTimeTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.jmf.module.ticket.impl.SendEmployeeDistributionStatusService.STATUS_EVENT_NAME;
import static ru.yandex.market.jmf.module.ticket.impl.SendEmployeeDistributionStatusService.STATUS_TAG_NAME;
import static ru.yandex.market.jmf.module.ticket.impl.SendEmployeeDistributionStatusService.WAIT_TAKEN_STATUS_EVENT_NAME;
import static ru.yandex.market.jmf.module.ticket.impl.SendEmployeeDistributionStatusService.WAIT_TAKEN_STATUS_TAG_NAME;
import static ru.yandex.market.jmf.module.ticket.operations.distribution.InitProcessEmployeeDistributionStatusOperationHandler.DISABLE_PLAY_MODE_IF_PROCESSING_TICKET_EXISTS;
import static ru.yandex.market.jmf.module.ticket.operations.distribution.SendEmployeeDistributionStatusNotificationOperationHandler.XIVA_TICKET_DISTRIBUTION_ENABLED;

@Transactional
@SpringJUnitConfig(classes = ModuleTicketTestConfiguration.class)
public class SendEmployeeDistributionStatusTest {

    @Inject
    private TicketTestUtils ticketTestUtils;

    @Inject
    private OuTestUtils ouTestUtils;

    @Inject
    private ServiceTimeTestUtils serviceTimeTestUtils;

    @Inject
    private BcpService bcpService;

    @Inject
    private DistributionService distributionService;

    @Inject
    private XivaPersonalClient xivaPersonalClient;

    @Inject
    private ConfigurationService configurationService;

    @Inject
    private SecurityDataService securityDataService;

    private Team team;
    private Service service;

    @BeforeEach
    public void setUp() {
        configurationService.setValue(XIVA_TICKET_DISTRIBUTION_ENABLED.key(), true);
        configurationService.setValue(DISABLE_PLAY_MODE_IF_PROCESSING_TICKET_EXISTS.key(), true);
        team = ticketTestUtils.createTeam();
        Entity brand = ticketTestUtils.createBrand();
        service = ticketTestUtils.createService(
                Map.of(
                        Service.SERVICE_TIME, serviceTimeTestUtils.createServiceTime24x7(),
                        Service.RESPONSIBLE_TEAM, team,
                        Service.BRAND, brand));
    }

    @AfterEach
    public void tearDown() {
        Mockito.reset(xivaPersonalClient);
        ((MockSecurityDataService) securityDataService).reset();
    }

    @Test
    public void testFindTicketForEmployee() {
        final long employeeUid = Randoms.longValue();
        Employee employee = createEmployee(employeeUid);
        Ticket ticket = createTicket();
        doStart(employee);

        assertEmployeeDistributionStatus(employee, ticket, EmployeeDistributionStatus.STATUS_PROCESSING);
        assertXivaSendRequest(new SendRequestMatcher(
                STATUS_TAG_NAME,
                employeeUid,
                true,
                EmployeeDistributionStatus.STATUS_PROCESSING,
                ticket
        ));
    }

    @Test
    public void testFindEmployeeForTicket() {
        final long employeeUid = Randoms.longValue();
        Employee employee = createEmployee(employeeUid);
        doStart(employee);
        assertXivaSendRequest(new SendRequestMatcher(
                STATUS_TAG_NAME,
                employeeUid,
                false,
                EmployeeDistributionStatus.STATUS_WAIT_TICKET,
                null
        ));
        Ticket ticket = createTicket();

        assertEmployeeDistributionStatus(employee, ticket, EmployeeDistributionStatus.STATUS_WAIT_TAKEN);
        assertXivaSendRequest(new SendRequestMatcher(
                WAIT_TAKEN_STATUS_TAG_NAME,
                employeeUid,
                true,
                EmployeeDistributionStatus.STATUS_WAIT_TAKEN,
                ticket
        ));
    }

    private void doStart(Employee employee) {
        distributionService.doStart(employee);
        // для проверки, что запрос в xiva отправляется только в конце транзакции
        verify(xivaPersonalClient, never()).send(any());
        TransactionSynchronizationUtils.triggerBeforeCommit(false);
    }

    private Employee createEmployee(long uid) {
        final Ou ou = ouTestUtils.createOu();
        Employee employee = bcpService.create(Employee.FQN_DEFAULT, Map.of(
                Employee.OU, ou,
                Employee.TITLE, Randoms.string(),
                Employee.UID, uid,
                Employee.TEAMS, Set.of(team),
                Employee.SERVICES, Set.of(service)
        ));
        bcpService.create(EmployeeDistributionStatus.FQN, Map.of(
                EmployeeDistributionStatus.EMPLOYEE, employee
        ));
        return employee;
    }

    private Ticket createTicket() {
        Ticket ticket = ticketTestUtils.createTicket(Fqn.of("ticket$test"),
                team,
                service,
                Map.of(Ticket.WAIT_DISTRIBUTION, true)
        );
        // для проверки, что запрос в xiva отправляется только в конце транзакции
        verify(xivaPersonalClient, never()).send(any());
        TransactionSynchronizationUtils.triggerBeforeCommit(false);
        return ticket;
    }

    private void assertEmployeeDistributionStatus(Employee employee, Ticket ticket, String status) {
        EmployeeDistributionStatus distributionStatus = distributionService.getEmployeeStatus(employee);
        Assertions.assertEquals(status, distributionStatus.getStatus());
        Assertions.assertEquals(ticket, distributionStatus.getTicket());
    }

    private void assertXivaSendRequest(SendRequestMatcher sendRequestMatcher) {
        verify(xivaPersonalClient, times(1)).send(argThat(sendRequestMatcher));
        verify(xivaPersonalClient, times(1)).send(any());
        clearInvocations(xivaPersonalClient);
    }

    private static class SendRequestMatcher implements ArgumentMatcher<SendRequest> {

        private final String tag;
        private final long uid;
        private final boolean ticketChanged;
        private final String status;
        private final Ticket ticket;

        private SendRequestMatcher(String tag, long uid, boolean ticketChanged, String status, Ticket ticket) {
            this.tag = tag;
            this.uid = uid;
            this.ticketChanged = ticketChanged;
            this.status = status;
            this.ticket = ticket;
        }

        @Override
        public boolean matches(SendRequest argument) {
            if (null == argument) {
                return false;
            }
            SendRequestParams params = argument.getParams();
            if (!Objects.equals(List.of(tag), params.getTags())) {
                return false;
            }
            Object payload = params.getPayload();
            if (null == payload || !(payload instanceof XivaRequestUtils.Payload)) {
                return false;
            }
            XivaRequestUtils.Payload payloadWithType = (XivaRequestUtils.Payload) payload;
            if (WAIT_TAKEN_STATUS_TAG_NAME.equals(tag)) {
                return WAIT_TAKEN_STATUS_EVENT_NAME.equals(payloadWithType.getType())
                        && null == payloadWithType.getPayload();
            }
            if (!STATUS_EVENT_NAME.equals(payloadWithType.getType())) {
                return false;
            }
            if (null == payloadWithType.getPayload()
                    || !(payloadWithType.getPayload() instanceof DistributionUtils.EmployeeStatus)) {
                return false;
            }
            DistributionUtils.EmployeeStatus employeeStatus =
                    (DistributionUtils.EmployeeStatus) payloadWithType.getPayload();
            return argument.getUid() == uid
                    && employeeStatus.isTicketChanged() == ticketChanged
                    && checkStatus(employeeStatus.getCurrentStatus())
                    && checkTicket(employeeStatus.getTicket());
        }

        private boolean checkStatus(DistributionUtils.StatusName statusName) {
            return null != statusName && Objects.equals(statusName.getCode(), status);
        }

        private boolean checkTicket(DistributionUtils.TicketDescription ticketDescription) {
            if (null == ticket) {
                return null == ticketDescription;
            }
            return null != ticketDescription && Objects.equals(ticketDescription.getGid(), ticket.getGid());
        }
    }
}
