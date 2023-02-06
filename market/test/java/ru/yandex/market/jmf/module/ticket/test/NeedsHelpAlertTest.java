package ru.yandex.market.jmf.module.ticket.test;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.opentest4j.AssertionFailedError;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.HasGid;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.ou.Ou;
import ru.yandex.market.jmf.module.ou.impl.OuTestUtils;
import ru.yandex.market.jmf.module.ticket.Employee;
import ru.yandex.market.jmf.module.ticket.EmployeeDistributionStatus;
import ru.yandex.market.jmf.module.ticket.NeedsHelpAlert;
import ru.yandex.market.jmf.module.ticket.NeedsHelpService;
import ru.yandex.market.jmf.module.ticket.OuCallCenter;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Team;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.impl.NeedHelpData;
import ru.yandex.market.jmf.module.ticket.operations.SendNeedsHelpAlertNotificationOperationHandler;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.module.xiva.XivaPersonalClient;
import ru.yandex.market.jmf.module.xiva.request.SendRequest;
import ru.yandex.market.jmf.module.xiva.request.SendRequestParams;
import ru.yandex.market.jmf.module.xiva.request.XivaRequestUtils;
import ru.yandex.market.jmf.security.SecurityDataService;
import ru.yandex.market.jmf.security.test.impl.MockSecurityDataService;
import ru.yandex.market.jmf.timings.test.impl.ServiceTimeTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.jmf.module.ticket.operations.SendNeedsHelpAlertNotificationOperationHandler.OPERATOR_TAG_NAME;
import static ru.yandex.market.jmf.module.ticket.operations.SendNeedsHelpAlertNotificationOperationHandler.SUPERVISOR_TAG_NAME;
import static ru.yandex.market.jmf.module.ticket.operations.SendNeedsHelpAlertNotificationOperationHandler.XIVA_NEEDS_HELP_ALERT_ENABLED;

@Transactional
@SpringJUnitConfig(classes = ModuleTicketTestConfiguration.class)
public class NeedsHelpAlertTest {

    @Inject
    private TicketTestUtils ticketTestUtils;

    @Inject
    private OuTestUtils ouTestUtils;

    @Inject
    private ServiceTimeTestUtils serviceTimeTestUtils;

    @Inject
    private BcpService bcpService;

    @Inject
    private XivaPersonalClient xivaPersonalClient;

    @Inject
    private NeedsHelpService needsHelpService;

    @Inject
    private SecurityDataService securityDataService;

    @Inject
    private ConfigurationService configurationService;

    private Ticket ticket;
    private Employee supervisor1, supervisor2, supervisor3, supervisor4;
    private Employee employee1, employee2;

    @BeforeEach
    public void setUp() {
        configurationService.setValue(XIVA_NEEDS_HELP_ALERT_ENABLED.key(), true);
        ticket = createTicket();

        Ou ou1 = ouTestUtils.createOu(OuCallCenter.FQN_CALL_CENTER);
        Ou ou2 = ouTestUtils.createOu(OuCallCenter.FQN_CALL_CENTER, ou1);

        Ou ou3 = ouTestUtils.createOu(OuCallCenter.FQN_CALL_CENTER);
        Ou ou4 = ouTestUtils.createOu(OuCallCenter.FQN_CALL_CENTER, ou3);

        supervisor1 = createEmployee(ou1);
        supervisor2 = createEmployee(ou1);
        supervisor3 = createEmployee(ou1);
        supervisor4 = createEmployee(ou1);

        setOuSupervisors(ou1, supervisor1);
        setOuSupervisors(ou2, supervisor2, supervisor3);
        setOuSupervisors(ou3, supervisor1);
        setOuSupervisors(ou4, supervisor4);

        employee1 = createEmployee(ou2);
        employee2 = createEmployee(ou4);
    }

    @AfterEach
    public void tearDown() {
        Mockito.reset(xivaPersonalClient);
    }

    @Test
    public void testOperatorNeedsHelp() {
        setNeedsHelp(employee1, ticket.getGid(), true);
        Set<Alert> expectedAlerts = Set.of(new Alert(employee1, ticket, NeedsHelpAlert.Statuses.ACTIVE));

        var requestsByUid = getRequestsByUid(4);

        checkOperatorAlerts(employee1, expectedAlerts, requestsByUid);
        checkSupervisorAlerts(supervisor1, expectedAlerts, requestsByUid);
        checkSupervisorAlerts(supervisor2, expectedAlerts, requestsByUid);
        checkSupervisorAlerts(supervisor3, expectedAlerts, requestsByUid);
    }

    @Test
    public void testOperatorNeedsHelpWhenAlreadyExistsAlert() {
        setNeedsHelp(employee1, ticket.getGid(), true);
        resetXivaClient();

        setNeedsHelp(employee2, null, true);

        Map<Long, SendRequest> requestsByUid = getRequestsByUid(3);

        checkOperatorAlerts(employee2, Set.of(new Alert(employee2, null, NeedsHelpAlert.Statuses.ACTIVE)),
                requestsByUid);
        checkSupervisorAlerts(supervisor1, Set.of(
                new Alert(employee1, ticket, NeedsHelpAlert.Statuses.ACTIVE),
                new Alert(employee2, null, NeedsHelpAlert.Statuses.ACTIVE)
        ), requestsByUid);
        checkSupervisorAlerts(supervisor4, Set.of(new Alert(employee2, null, NeedsHelpAlert.Statuses.ACTIVE)),
                requestsByUid);
    }

    private ImmutableMap<Long, SendRequest> getRequestsByUid(int wantedNumberOfInvocations) {
        var sendRequestCaptor = ArgumentCaptor.forClass(SendRequest.class);
        verify(xivaPersonalClient, times(wantedNumberOfInvocations)).send(sendRequestCaptor.capture());
        List<SendRequest> requests = sendRequestCaptor.getAllValues();
        return Maps.uniqueIndex(requests, SendRequest::getUid);
    }

    @Test
    public void testSupervisorTakesAlert() {
        NeedsHelpAlert alert = setNeedsHelp(employee1, ticket.getGid(), true);
        resetXivaClient();
        setNeedsHelp(employee2, null, true);
        resetXivaClient();

        assignAlertToSupervisor(alert, supervisor1);

        Map<Long, SendRequest> requestsByUid = getRequestsByUid(4);

        checkOperatorAlerts(employee1, Set.of(new Alert(employee1, ticket, NeedsHelpAlert.Statuses.PROCESSING)),
                requestsByUid);
        checkSupervisorAlerts(supervisor1, Set.of(
                new Alert(employee1, ticket, NeedsHelpAlert.Statuses.PROCESSING),
                new Alert(employee2, null, NeedsHelpAlert.Statuses.ACTIVE)
        ), requestsByUid);
        checkSupervisorAlerts(supervisor2, Set.of(), requestsByUid);
        checkSupervisorAlerts(supervisor3, Set.of(), requestsByUid);
    }

    @Test
    public void testArchiveAlert() {
        setNeedsHelp(employee1, ticket.getGid(), true);
        resetXivaClient();
        setNeedsHelp(employee2, null, true);
        resetXivaClient();

        setNeedsHelp(employee1, null, false);

        Map<Long, SendRequest> requestsByUid = getRequestsByUid(4);

        checkOperatorAlerts(employee1, Set.of(), requestsByUid);
        checkSupervisorAlerts(supervisor1, Set.of(new Alert(employee2, null, NeedsHelpAlert.Statuses.ACTIVE)),
                requestsByUid);
        checkSupervisorAlerts(supervisor2, Set.of(), requestsByUid);
        checkSupervisorAlerts(supervisor3, Set.of(), requestsByUid);
    }

    @Test
    public void testArchiveAlertAfterSupervisorTakes() {
        NeedsHelpAlert alert = setNeedsHelp(employee1, ticket.getGid(), true);
        resetXivaClient();
        setNeedsHelp(employee2, null, true);
        resetXivaClient();

        assignAlertToSupervisor(alert, supervisor1);
        resetXivaClient();

        setNeedsHelp(employee1, null, false);

        Map<Long, SendRequest> requestsByUid = getRequestsByUid(2);

        checkOperatorAlerts(employee1, Set.of(), requestsByUid);
        checkSupervisorAlerts(supervisor1, Set.of(new Alert(employee2, null, NeedsHelpAlert.Statuses.ACTIVE)),
                requestsByUid);
    }

    private NeedsHelpAlert setNeedsHelp(Employee employee, String ticketGid, boolean needsHelp) {
        NeedsHelpAlert alert = needsHelpService.setNeedsHelp(employee, ticketGid, needsHelp);

        // для проверки, что запрос в xiva отправляется только в конце транзакции
        verify(xivaPersonalClient, never()).send(any());

        // Имитируем срабатывание синхронизации
        TransactionSynchronizationUtils.triggerBeforeCommit(false);
        return alert;
    }

    private void assignAlertToSupervisor(NeedsHelpAlert alert, Employee supervisor) {
        ((MockSecurityDataService) securityDataService).setInitialEmployee(supervisor);
        bcpService.edit(alert, Map.of(
                NeedsHelpAlert.STATUS, NeedsHelpAlert.Statuses.PROCESSING
        ));
        // для проверки, что запрос в xiva отправляется только в конце транзакции
        verify(xivaPersonalClient, never()).send(any());

        // Имитируем срабатывание синхронизации
        TransactionSynchronizationUtils.triggerBeforeCommit(false);
    }

    private void checkOperatorAlerts(Employee operator, Set<Alert> alerts, Map<Long, SendRequest> requestsByUid) {
        assertSentNotification(operator, alerts, OPERATOR_TAG_NAME, requestsByUid.get(operator.getUid()));
    }

    private void assertSentNotification(Employee employee, Set<Alert> alerts, String tagName, SendRequest sendRequest) {
        SendRequestParams params = sendRequest.getParams();
        Object payload = params.getPayload();
        Assertions.assertTrue(payload instanceof XivaRequestUtils.Payload);
        XivaRequestUtils.Payload payloadWithType = (XivaRequestUtils.Payload) payload;
        Assertions.assertEquals(SendNeedsHelpAlertNotificationOperationHandler.EVENT_NAME, payloadWithType.getType());
        Assertions.assertTrue(payloadWithType.getPayload() instanceof NeedHelpData);

        List<Map<String, Object>> requests = ((NeedHelpData) payloadWithType.getPayload()).getRequests();
        Assertions.assertEquals(employee.getUid(), sendRequest.getUid());
        Assertions.assertEquals(List.of(tagName), params.getTags());
        checkAlerts(alerts, requests);
    }

    private void checkAlerts(Set<Alert> alerts, List<Map<String, Object>> requests) {
        Assertions.assertNotNull(requests);
        Assertions.assertEquals(alerts.size(), requests.size());
        alerts.forEach(alert -> checkAlert(alert, requests));
    }

    private void checkAlert(Alert alert, List<Map<String, Object>> requests) {
        AssertionFailedError firstError = new AssertionFailedError("Ни один запрос не совпал с ожидаемым");
        for (Map<String, Object> request : requests) {
            try {
                Assertions.assertEquals(
                        getProperty(request, NeedsHelpAlert.EMPLOYEE, HasGid.GID),
                        alert.getEmployee()
                );
                Assertions.assertEquals(
                        getProperty(request, NeedsHelpAlert.TICKET, HasGid.GID),
                        alert.getTicket()
                );
                Assertions.assertEquals(
                        getProperty(request, NeedsHelpAlert.STATUS, "code"),
                        alert.getStatus()
                );
                return;
            } catch (AssertionFailedError e) {
                firstError.addSuppressed(e);
            }
        }
        throw firstError;
    }

    private String getProperty(Map<String, Object> request, String... keys) {
        Object result = request;
        for (String key : keys) {
            result = ((Map<String, Object>) result).get(key);
            if (null == result) {
                return null;
            }
        }
        return (String) result;
    }

    private void checkSupervisorAlerts(Employee supervisor, Set<Alert> alerts, Map<Long, SendRequest> requestsByUid) {
        assertSentNotification(supervisor, alerts, SUPERVISOR_TAG_NAME, requestsByUid.get(supervisor.getUid()));
    }

    private void checkAlertsCount(int expected) {
        verify(xivaPersonalClient, times(expected)).send(any());
    }

    private void resetXivaClient() {
        clearInvocations(xivaPersonalClient);
    }

    private Employee createEmployee(Ou ou) {
        Employee employee = bcpService.create(Employee.FQN_DEFAULT, Map.of(
                Employee.OU, ou,
                Employee.TITLE, Randoms.string(),
                Employee.UID, Randoms.longValue()
        ));
        bcpService.create(EmployeeDistributionStatus.FQN, Map.of(
                EmployeeDistributionStatus.EMPLOYEE, employee
        ));
        return employee;
    }

    private void setOuSupervisors(Ou ou, Employee... supervisors) {
        bcpService.edit(ou, Map.of(OuCallCenter.SUPERVISORS, List.of(supervisors)));
    }

    private Ticket createTicket() {
        Team team = ticketTestUtils.createTeam();
        Entity brand = ticketTestUtils.createBrand();
        Service service = ticketTestUtils.createService(
                Map.of(
                        Service.SERVICE_TIME, serviceTimeTestUtils.createServiceTime24x7(),
                        Service.RESPONSIBLE_TEAM, team,
                        Service.BRAND, brand));
        return ticketTestUtils.createTicket(Fqn.of("ticket$test"),
                team,
                service,
                Map.of(Ticket.WAIT_DISTRIBUTION, true)
        );
    }

    private static class SendRequestMatcher implements ArgumentMatcher<SendRequest> {

        private final long uid;
        private final String tagName;
        private final Set<Alert> alerts;

        public SendRequestMatcher(Employee employee, String tagName, Set<Alert> alerts) {
            this.uid = employee.getUid();
            this.tagName = tagName;
            this.alerts = alerts;
        }

        @Override
        public boolean matches(SendRequest argument) {
            if (null == argument) {
                return false;
            }
            SendRequestParams params = argument.getParams();
            Object payload = params.getPayload();
            if (null == payload || !(payload instanceof XivaRequestUtils.Payload)) {
                return false;
            }
            XivaRequestUtils.Payload payloadWithType = (XivaRequestUtils.Payload) payload;
            if (!SendNeedsHelpAlertNotificationOperationHandler.EVENT_NAME.equals(payloadWithType.getType())) {
                return false;
            }
            if (null == payloadWithType.getPayload() || !(payloadWithType.getPayload() instanceof NeedHelpData)) {
                return false;
            }
            List<Map<String, Object>> requests = ((NeedHelpData) payloadWithType.getPayload()).getRequests();
            return argument.getUid() == uid
                    && Objects.equals(params.getTags(), List.of(tagName))
                    && checkAlerts(requests);
        }

        private boolean checkAlerts(List<Map<String, Object>> requests) {
            if (null == requests || alerts.size() != requests.size()) {
                return false;
            }
            return alerts.stream().allMatch(alert -> checkAlert(alert, requests));
        }

        private boolean checkAlert(Alert alert, List<Map<String, Object>> requests) {
            return requests.stream()
                    .anyMatch(request ->
                            Objects.equals(
                                    getProperty(request, NeedsHelpAlert.EMPLOYEE, HasGid.GID),
                                    alert.getEmployee()
                            ) && Objects.equals(
                                    getProperty(request, NeedsHelpAlert.TICKET, HasGid.GID),
                                    alert.getTicket()
                            ) && Objects.equals(
                                    getProperty(request, NeedsHelpAlert.STATUS, "code"),
                                    alert.getStatus()
                            )
                    );
        }

        private String getProperty(Map<String, Object> request, String... keys) {
            Object result = request;
            for (String key : keys) {
                result = ((Map<String, Object>) result).get(key);
                if (null == result) {
                    return null;
                }
            }
            return (String) result;
        }
    }

    private static class Alert {

        private final String employee;
        private final String ticket;
        private final String status;

        public Alert(Employee employee, Ticket ticket, String status) {
            this.employee = null == employee ? null : employee.getGid();
            this.ticket = null == ticket ? null : ticket.getGid();
            this.status = status;
        }

        public String getEmployee() {
            return employee;
        }

        public String getTicket() {
            return ticket;
        }

        public String getStatus() {
            return status;
        }
    }
}
