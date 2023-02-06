package ru.yandex.market.crm.operatorwindow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import jdk.jfr.Description;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.b2bcrm.module.ticket.B2bOutgoingTicket;
import ru.yandex.market.b2bcrm.module.ticket.B2bTicket;
import ru.yandex.market.crm.operatorwindow.jmf.entity.LogisticSupportTicket;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.logic.wf.bcp.ValidateWfRequiredAttributesOperationHandler;
import ru.yandex.market.jmf.logic.wf.bcp.WfConstants;
import ru.yandex.market.jmf.module.mail.test.impl.MailMessageBuilderService;
import ru.yandex.market.jmf.module.relation.LinkedRelation;
import ru.yandex.market.jmf.module.ticket.Brand;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Team;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.timings.ServiceTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Transactional
public class ReopenB2bTicketForResolveLogisticSupportTicketTest extends AbstractLogisticSupportMailProcessingTest {
    protected static final String MAIL_CONNECTION = "b2b";
    protected static final String TEAM_NAME = "testFirstLineMail";
    @Inject
    protected ConfigurationService configurationService;
    private Service service;

    @BeforeEach
    public void prepare() {
        mailTestUtils.createMailConnection(MAIL_CONNECTION);

        Team team0 = ticketTestUtils.createTeam(TEAM_NAME);
        ServiceTime serviceTime24x7 = serviceTimeTestUtils.createServiceTime24x7();
        Brand brand = ticketTestUtils.createBrand();

        service = ticketTestUtils.createService(team0, serviceTime24x7, brand, Optional.of(
                "marketApiAffiliateEscalation"));
        configurationService.setValue("defaultRoutingMailServiceForB2bTicket", service);
    }

    @Test
    @Description("Проверка для метакласса ticket$b2b")
    public void createRelationBetweenTicketAndCheckReopening() {
        //Создаем b2b обращение
        MailMessageBuilderService.MailMessageBuilder builder =
                mailMessageBuilderService.getMailMessageBuilder(MAIL_CONNECTION);
        builder.newDeduplicationKey()
                .setBody(Randoms.string())
                .setSubject(Randoms.string());

        processMessage(builder.build());
        List<B2bTicket> tickets = dbService.list(Query.of(B2bTicket.FQN));
        B2bTicket b2bTicket = tickets.get(0);

        //Создаем логистическое обращение из письма c номером b2b-обращения в теле
        String responseBody = Randoms.string() + " Номер обращения в Едином Окне: " + b2bTicket.getId();
        beruMailMessageBuilder.newDeduplicationKey()
                .setBody(responseBody)
                .setSubject(Randoms.string());

        processMessage(beruMailMessageBuilder.build());
        LogisticSupportTicket logisticTicket = getSingleLogisticSupportTicket();

        //Проверяем что тикеты связаны
        assertLinkedRelation(b2bTicket, logisticTicket);

        //Проверяем, что тикет b2b переоткроется при решении логистического (если он в статусе для переоткрытия)
        changeTicketStatus(b2bTicket, B2bTicket.STATUS_CLOSED_WAIT_CLIENT);
        changeTicketStatus(logisticTicket, LogisticSupportTicket.STATUS_RESOLVED);

        assertEquals(B2bTicket.STATUS_REOPENED, b2bTicket.getStatus(), "Тикет переоткрылся");
    }


    @Test
    @Description("Проверка для метакласса ticket$b2bOutgoing")
    public void createRelationBetweenLogisticAndOutgoingTicketsAndCheckReopening() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(B2bOutgoingTicket.TITLE, "test outgoing ticket");
        properties.put(B2bOutgoingTicket.SERVICE, service);

        B2bOutgoingTicket b2bTicket = bcpService.create(B2bOutgoingTicket.FQN, properties);

        //Создаем логистическое обращение из письма c номером b2b-обращения в теле
        String responseBody = Randoms.string() + " Номер обращения в Едином Окне: " + b2bTicket.getId();
        beruMailMessageBuilder.newDeduplicationKey()
                .setBody(responseBody)
                .setSubject(Randoms.string());

        processMessage(beruMailMessageBuilder.build());
        LogisticSupportTicket logisticTicket = getSingleLogisticSupportTicket();

        //Проверяем что тикеты связаны
        assertLinkedRelation(b2bTicket, logisticTicket);

        //Проверяем, что тикет b2b переоткроется при решении логистического (если он в статусе для переоткрытия)
        changeTicketStatus(b2bTicket, B2bOutgoingTicket.STATUS_CLOSED_WAIT_CLIENT);
        changeTicketStatus(logisticTicket, LogisticSupportTicket.STATUS_RESOLVED);

        assertEquals(B2bOutgoingTicket.STATUS_REOPENED, b2bTicket.getStatus(), "Тикет переоткрылся");
    }

    private void changeTicketStatus(Ticket ticket, String status) {
        bcpService.edit(ticket.getGid(), Map.of(B2bTicket.STATUS, status),
                Map.of(
                        WfConstants.SKIP_WF_STATUS_CHANGE_CHECKING_ATTRIBUTE, true,
                        ValidateWfRequiredAttributesOperationHandler.SKIP_WF_REQUIRED_ATTRIBUTES_VALIDATION, true
                )
        );
    }

    public void assertLinkedRelation(Ticket source, Ticket target) {
        assertLinkedRelationCount(source, target, 1);
    }

    private void assertLinkedRelationCount(Ticket source, Ticket target, int count) {
        long actualCount = dbService.count(Query.of(LinkedRelation.FQN).withFilters(
                Filters.eq(LinkedRelation.SOURCE, source),
                Filters.eq(LinkedRelation.TARGET, target)
        ));
        assertEquals(count, actualCount);
    }
}
