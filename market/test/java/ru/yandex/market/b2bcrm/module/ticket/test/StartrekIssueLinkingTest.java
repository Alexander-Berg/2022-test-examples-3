package ru.yandex.market.b2bcrm.module.ticket.test;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Description;

import ru.yandex.market.b2bcrm.module.account.Supplier;
import ru.yandex.market.b2bcrm.module.ticket.B2bChatTicket;
import ru.yandex.market.b2bcrm.module.ticket.B2bTicket;
import ru.yandex.market.b2bcrm.module.ticket.B2bTicketPostprocessingService;
import ru.yandex.market.b2bcrm.module.ticket.test.config.B2bTicketTests;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.relation.test.impl.RelationTestUtils;
import ru.yandex.market.jmf.module.startrek.StartrekIssue;
import ru.yandex.market.jmf.module.startrek.test.StartrekTestUtils;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.timings.ServiceTime;
import ru.yandex.market.jmf.timings.test.impl.ServiceTimeTestUtils;
import ru.yandex.market.jmf.utils.Maps;

import static org.assertj.core.api.Assertions.assertThat;

@B2bTicketTests
public class StartrekIssueLinkingTest {

    private static final String DESCRIPTION_FORMAT = "Номер B2B обращения: %d";
    private static final String DESCRIPTION_FORMAT_2 = "Номер B2B обращения:%d";

    @Inject
    private TicketTestUtils ticketTestUtils;

    @Inject
    private StartrekTestUtils startrekTestUtils;

    @Inject
    private RelationTestUtils relationTestUtils;

    @Inject
    private ServiceTimeTestUtils serviceTimeTestUtils;

    @Inject
    private DbService dbService;

    @Inject
    private BcpService bcpService;

    private B2bTicket ticket;
    private B2bChatTicket chatTicket;
    private Service baseService;
    private Service postprocessingService;

    @BeforeEach
    public void prepareData() {
        ServiceTime serviceTime24x7 = serviceTimeTestUtils.createServiceTime24x7();

        postprocessingService = ticketTestUtils.createService(Map.of(Service.SERVICE_TIME, serviceTime24x7));
        baseService = ticketTestUtils.createService(Map.of(Service.SERVICE_TIME, serviceTime24x7));
        bcpService.create(B2bTicketPostprocessingService.FQN, Map.of(
                "code", "testPostprocess",
                "title", "testPostprocess",
                "baseService", baseService,
                "postprocessingService", postprocessingService));

        Supplier supplier = bcpService.create(Fqn.of("account$supplier"), Map.of(
                "title", "Test supplier",
                "campaignId", "10188"
        ));

        ticket = ticketTestUtils.createTicket(B2bTicket.FQN, Collections.emptyMap());
        chatTicket = ticketTestUtils.createTicket(B2bChatTicket.FQN, Map.of(B2bChatTicket.SERVICE, baseService,
                B2bChatTicket.PARTNER, supplier)
        );
    }

    @Test
    @Description("Обычное обращение должно связаться по тексту")
    public void testShouldLinkByDescription() {
        StartrekIssue issue = createStartrekIssue(String.format(DESCRIPTION_FORMAT, ticket.getId()), null);
        assertThat(relationTestUtils.getRelations(ticket, issue)).hasSize(1);
    }

    @Test
    @Description("Обычное обращение должно связаться по тексту")
    public void testShouldLinkByDescription2() {
        StartrekIssue issue = createStartrekIssue(String.format(DESCRIPTION_FORMAT_2, ticket.getId()), null);
        assertThat(relationTestUtils.getRelations(ticket, issue)).hasSize(1);
    }

    @Test
    @Description("Обычное обращение должно связаться по специальному полю в ST")
    public void testShouldLinkByTicketNumbersField() {
        StartrekIssue issue = createStartrekIssue(null, ticket.getId().toString());
        assertThat(relationTestUtils.getRelations(ticket, issue)).hasSize(1);
    }

    @Test
    @Description("Обычное обращение не должно связаться, если не подходит формат или что-то ещё")
    public void testIssueShouldNotBeLinked() {
        assertThat(relationTestUtils.getRelations(ticket, createStartrekIssue(null, null))).isEmpty();
        assertThat(relationTestUtils.getRelations(ticket, createStartrekIssue(DESCRIPTION_FORMAT, null))).isEmpty();
        assertThat(relationTestUtils.getRelations(ticket, createStartrekIssue(ticket.getId().toString(), null))).isEmpty();
    }

    @Test
    @Description("B2B чат не должен связаться с тикетом ST")
    public void testIssueShouldNotBeLinkedB2bChatTicket() {
        assertThat(relationTestUtils.getRelations(chatTicket, createStartrekIssue(String.format(DESCRIPTION_FORMAT,
                ticket.getId()),
                null))).isEmpty();
        assertThat(relationTestUtils.getRelations(chatTicket, createStartrekIssue(null, ticket.getId().toString()))).isEmpty();
    }

    @Test
    @Description("Если приходит ST тикет с номером b2b чата, " +
            "то должен быть создано новое и созданы транзитивные связи b2bChat -> b2bTicket -> stIssue")
    public void testCreateAndLinkNewB2bTicketForB2bChatTicketIssue() {
        var issueDescription = String.format(DESCRIPTION_FORMAT, chatTicket.getId());
        var issue = createStartrekIssue(issueDescription, null);
        var b2bChatRelations = relationTestUtils.getRelationsFrom(chatTicket);
        assertThat(b2bChatRelations).hasSize(1);

        String target = b2bChatRelations.get(0).getTarget();
        assertThat(target).containsPattern("ticket@\\d+");

        var newB2bTicketEntity = dbService.get(target);
        assertThat(newB2bTicketEntity).isInstanceOf(B2bTicket.class);
        B2bTicket newB2bTicket = (B2bTicket) newB2bTicketEntity;

        assertThat(newB2bTicket.getTitle()).isEqualTo("Ответ на обращение");
        assertThat(newB2bTicket.getChannel().getCode()).isEqualTo("mail");
        assertThat(newB2bTicket.getDescription()).isEqualTo(issueDescription);
        assertThat(newB2bTicket.getClientName()).isEqualTo(chatTicket.getClientName());
        assertThat(newB2bTicket.getClientPhone()).isEqualTo(chatTicket.getClientPhone());
        assertThat(newB2bTicket.getClientEmail()).isEqualTo(chatTicket.getClientEmail());
        assertThat(newB2bTicket.getPartner()).isEqualTo(chatTicket.getPartner());
        assertThat(newB2bTicket.getService()).isEqualTo(postprocessingService);
        assertThat(newB2bTicket.getStatus()).isEqualTo(B2bTicket.STATUS_CLOSED_WAIT_CONTIGUOUS);
        assertThat(relationTestUtils.getRelations(newB2bTicket, issue)).hasSize(1);
    }

    @Test
    @Description("Если приходит ST тикет с номером b2b чата, но связанное обращение уже есть, " +
            "то просто привязываем тикет ST к уже созданному обращению")
    public void testLink2bTicketForB2bChatTicketIssue() {

        var issue = createStartrekIssue(String.format(DESCRIPTION_FORMAT, chatTicket.getId()), null);
        var b2bChatRelations = relationTestUtils.getRelationsFrom(chatTicket);
        assertThat(b2bChatRelations).hasSize(1);

        String target = b2bChatRelations.get(0).getTarget();
        assertThat(target).containsPattern("ticket@\\d+");

        var newB2bTicket = dbService.get(target);
        assertThat(newB2bTicket).isInstanceOf(B2bTicket.class);
        assertThat(newB2bTicket.getAttribute(B2bTicket.TITLE).toString()).hasToString("Ответ на обращение");
        assertThat((Service) newB2bTicket.getAttribute(B2bTicket.SERVICE)).isEqualTo(postprocessingService);
        assertThat(relationTestUtils.getRelations(newB2bTicket, issue)).hasSize(1);
    }

    @Nonnull
    private StartrekIssue createStartrekIssue(String description, String ticketNumbers) {
        return startrekTestUtils.createIssue(Maps.of(
                StartrekIssue.TITLE, "Тикет ST",
                StartrekIssue.DESCRIPTION, description,
                StartrekIssue.TICKET_NUMBERS, ticketNumbers
        ));
    }


}
