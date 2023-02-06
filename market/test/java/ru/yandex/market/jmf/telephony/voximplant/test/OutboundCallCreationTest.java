package ru.yandex.market.jmf.telephony.voximplant.test;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.telephony.voximplant.Employee;
import ru.yandex.market.jmf.telephony.voximplant.VoximplantCall;
import ru.yandex.market.jmf.telephony.voximplant.controller.VoximplantOutboundCallsController;
import ru.yandex.market.jmf.telephony.voximplant.controller.models.OutboundCallRequest;
import ru.yandex.market.jmf.timings.test.impl.ServiceTimeTestUtils;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = VoximplantControllerTestConfiguration.class)
@TestPropertySource("classpath:/vox_test.properties")
@Transactional
public class OutboundCallCreationTest {

    private static final Fqn TICKET_FQN = Fqn.of("ticket$outgoingCall");

    @Inject
    private VoximplantOutboundCallsController controller;

    @Inject
    private TicketTestUtils ticketTestUtils;

    @Inject
    private DbService dbService;
    @Inject
    private BcpService bcpService;
    @Inject
    private ServiceTimeTestUtils serviceTimeTestUtils;

    @Test
    public void testCreate() {
        final var team = ticketTestUtils.createTeam();
        Ticket ticket = ticketTestUtils.createTicket(
                TICKET_FQN,
                team,
                ticketTestUtils.createService(Fqn.of("service$telephony"), Map.of(
                        Service.SERVICE_TIME, serviceTimeTestUtils.createServiceTime24x7()
                )),
                Collections.emptyMap());
        final var ou = ticketTestUtils.createOu();
        final Employee employee = (Employee) ticketTestUtils.createEmployee(ou);
        bcpService.edit(employee, Map.of(Employee.VOX_LOGIN, "123"));
        bcpService.edit(ticket, Map.of(
                Ticket.RESPONSIBLE_EMPLOYEE, employee,
                Ticket.STATUS, Ticket.STATUS_PROCESSING
        ));

        OutboundCallRequest request = new OutboundCallRequest(
                Randoms.string(),
                employee.getVoximplantLogin(),
                Randoms.string(),
                ticket.getGid(),
                Randoms.string()
        );

        Map<String, Object> callDto = controller.outboundCall(request);
        VoximplantCall call = dbService.get((String) callDto.get(Ticket.GID));
        Assertions.assertNotNull(call);
        Assertions.assertNotNull(call.getTicket());
        Assertions.assertEquals(ticket.getGid(), call.getTicket().getGid());
        Assertions.assertEquals(request.getCallerId(), call.getCallerId());
        Assertions.assertEquals(request.getSendDataToVoximplantCallSessionUrl(),
                call.getSendDataToVoximplantCallSessionUrl());
        Assertions.assertEquals(request.getDestinationId(), call.getDestinationId());
        Assertions.assertEquals(request.getVoximplantSessionId(), call.getSessionId());
    }
}
