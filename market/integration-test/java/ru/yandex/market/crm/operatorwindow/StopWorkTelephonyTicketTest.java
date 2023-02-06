package ru.yandex.market.crm.operatorwindow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.domain.Phone;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.http.HttpResponse;
import ru.yandex.market.jmf.http.HttpStatus;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.ticket.DistributionService;
import ru.yandex.market.jmf.module.ticket.Employee;
import ru.yandex.market.jmf.module.ticket.EmployeeDistributionStatus;
import ru.yandex.market.jmf.module.ticket.Team;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.telephony.voximplant.VoximplantHttpClient;
import ru.yandex.market.jmf.telephony.voximplant.VoximplantInboundCall;
import ru.yandex.market.jmf.telephony.voximplant.controller.models.InboundCallRequest;
import ru.yandex.market.jmf.telephony.voximplant.impl.CreateInboundCallService;
import ru.yandex.market.jmf.tx.TxService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class StopWorkTelephonyTicketTest extends AbstractModuleOwTest {

    private static final Phone PHONE = Phone.fromRaw("+79123456789");

    @Inject
    private BcpService bcpService;
    @Inject
    private TxService txService;
    @Inject
    private DbService dbService;
    @Inject
    private DistributionService distributionService;
    @Inject
    private CreateInboundCallService createInboundCallService;

    @Inject
    private TicketTestUtils ticketTestUtils;

    @Inject
    private VoximplantHttpClient voximplantHttpClient;


    @BeforeEach
    public void setup() {
        txService.runInTx(() -> {
            ticketTestUtils.setServiceTime24x7(Constants.Service.BERU_INCOMING_CALL);
            ticketTestUtils.resetService(Constants.Service.BERU_INCOMING_CALL);
        });

        HttpResponse voximplantResponse = mock(HttpResponse.class);
        when(voximplantHttpClient.executeWithAuth(any())).thenReturn(voximplantResponse);
        when(voximplantResponse.getHttpStatus()).thenReturn(HttpStatus.OK);
        when(voximplantResponse.getBodyAsBytes()).thenReturn("{}".getBytes());
    }

    @Test
    public void telephonyTicketCouldBeStoppedWhileWaitTaken() {
        // настройка системы
        TicketTestUtils.TestContext ctx = ticketTestUtils.createInTx();

        txService.runInTx(() ->
                bcpService.edit(ctx.employee0, Map.of(
                                Employee.SERVICES, Set.of(Constants.Service.BERU_INCOMING_CALL),
                                Employee.TEAMS, Set.of(Team.FIRST_LINE_PHONE),
                                "voximplantLogin", Randoms.string()
                        )
                ));

        setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_WAIT_TICKET);

        // вызов системы
        // при входящем звонке должен создаться тикет, в очереди beruIncomingCall и назначится на оператора
        // employee0 т.к. он обрабатывает эту очередь и находится в ожидании тикета
        final VoximplantInboundCall inboundCall = txService.doInTx(() -> createInboundCall(PHONE));

        // сторожевая проверка: тикет был создан и назначен на оператора
        var t = txService.doInTx(() -> getTicketByPhone(inboundCall.getCallerId()));
        txService.runInTx(() -> {
            EmployeeDistributionStatus employeeStatus = distributionService.getEmployeeStatus(ctx.employee0);
            Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TAKEN, employeeStatus.getStatus());
        });

        // Эмулируем разрыв связи. Сюда попадает случай, когда клиент положил трубку не дождавшись ответа оператора
        txService.runInTx(() -> bcpService.edit(inboundCall, Map.of(
                VoximplantInboundCall.STATUS, "ended",
                VoximplantInboundCall.ENDED_BY, "CLIENT"
        )));

        // проверка утверждений
        // когда оператор не усел взять трубку тикет должен сняться с оператора
        txService.runInTx(() -> distributionService.currentStatus(ctx.employee0));
        txService.runInTx(() -> {
            final var distribution = distributionService.getEmployeeStatus(ctx.employee0);

            Ticket ticket = dbService.get(t.getGid());

            Assertions.assertNull(distribution.getTicket());
            Assertions.assertTrue(distribution.getInactiveTickets().isEmpty());

            Assertions.assertNull(ticket.getResponsibleEmployee());

            Assertions.assertEquals("missed", ticket.getStatus());
            Assertions.assertFalse(ticket.getWaitDistribution());

            Assertions.assertEquals(EmployeeDistributionStatus.STATUS_WAIT_TICKET, distribution.getStatus());
        });
    }

    private EmployeeDistributionStatus setEmployeeStatus(Employee employee, String statusWaitTicket) {
        return txService.doInNewTx(() -> distributionService.setEmployeeStatus(employee, statusWaitTicket));
    }

    private Ticket getTicketByPhone(String phone) {
        Query query = Query.of(Fqn.parse("ticket$beruIncomingCall"))
                .withFilters(Filters.eq(Ticket.CLIENT_PHONE,
                        Phone.fromRaw(phone)));
        final List<Entity> list = dbService.list(query);
        Assertions.assertEquals(1, list.size());
        Ticket ticket = (Ticket) list.get(0);
        ticket.getTitle(); // делаем unlazy
        return ticket;
    }

    private VoximplantInboundCall createInboundCall(Phone phone) {
        Map<String, String> sipHeaders = new HashMap<>();
        final String callerId = phone.getRawOrNormalized();
        InboundCallRequest request = new InboundCallRequest(
                "sessionId_123",
                callerId,
                "destinationId_789",
                "connectWithOperatorUrl_012",
                "sendDataToVoximplant_",
                sipHeaders,
                ""
        );
        return createInboundCallService.createInboundCall(request);
    }
}
