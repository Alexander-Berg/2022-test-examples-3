package ru.yandex.market.jmf.telephony.voximplant.test;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.telephony.voximplant.Employee;
import ru.yandex.market.jmf.telephony.voximplant.VoximplantOutboundCall;
import ru.yandex.market.jmf.telephony.voximplant.controller.VoximplantOutboundCallsController;
import ru.yandex.market.jmf.telephony.voximplant.controller.action.CompletedCallAction;
import ru.yandex.market.jmf.timings.test.impl.ServiceTimeTestUtils;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = VoximplantControllerTestConfiguration.class)
@TestPropertySource(properties = "external.voximplant.enabled=true")
@Transactional
public class OutboundCallResultTest {

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

    private Ticket ticket;
    private VoximplantOutboundCall outboundCall;

    @BeforeEach
    public void setUp() {
        final var team = ticketTestUtils.createTeam();
        ticket = ticketTestUtils.createTicket(
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

        outboundCall = bcpService.create(VoximplantOutboundCall.FQN, Map.of(
                VoximplantOutboundCall.SESSION_ID, Randoms.string(),
                VoximplantOutboundCall.CALLER_ID, "123",
                VoximplantOutboundCall.DESTINATION_ID, Randoms.string(),
                VoximplantOutboundCall.SEND_DATA_TO_VOXIMPLANT_CALL_SESSION_URL, Randoms.string(),
                VoximplantOutboundCall.TICKET, ticket.getGid()
        ));
    }

    @Test
    public void testCompletedCall() {
        OffsetDateTime now = OffsetDateTime.now();
        CompletedCallAction action = new CompletedCallAction(
                CompletedCallAction.EndedBy.OPERATOR,
                null,
                offsetDateTimeToMillis(now.plusSeconds(2)),
                offsetDateTimeToMillis(now.plusSeconds(3)),
                offsetDateTimeToMillis(now.plusSeconds(4)),
                Randoms.longValue(),
                Randoms.longValue(),
                Randoms.url(),
                Randoms.url(),
                null
        );

        controller.executeCallAction(outboundCall.getSessionId(), action);

        Assertions.assertEquals(action.getStartedAt().toEpochSecond(), outboundCall.getStartedAt().toEpochSecond());
        Assertions.assertEquals(action.getConnectedAt().toEpochSecond(), outboundCall.getConnectedAt().toEpochSecond());
        Assertions.assertEquals(action.getEndedAt().toEpochSecond(), outboundCall.getEndedAt().toEpochSecond());
        Assertions.assertEquals(action.getHoldTime(), outboundCall.getHoldTime());
        Assertions.assertEquals(action.getTransferTime(), outboundCall.getTransferTime());
        Assertions.assertEquals(action.getEndedBy().name(), outboundCall.getEndedBy().getCode());
        Assertions.assertEquals(action.getDownloadVoiceLogUrl().toString(), outboundCall.getDownloadVoiceLogUrl());
        Assertions.assertEquals(VoximplantOutboundCall.Statuses.ENDED, outboundCall.getStatus());

        List<Comment> comments = getComments(ticket);
        Assertions.assertEquals(1, comments.size());
    }

    private Long offsetDateTimeToMillis(OffsetDateTime value) {
        return value.toInstant().toEpochMilli();
    }

    private List<Comment> getComments(Ticket ticket) {
        return dbService.<Comment>list(
                Query.of(Comment.FQN)
                        .withFilters(Filters.eq(Comment.ENTITY, ticket))
        )
                .stream()
                .sorted(Comparator.comparing(Comment::getGid))
                .collect(Collectors.toList());
    }
}
