package ru.yandex.market.b2bcrm.module.ticket.test;


import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.b2bcrm.module.ticket.B2bTicket;
import ru.yandex.market.b2bcrm.module.ticket.test.config.B2bTicketTests;
import ru.yandex.market.jmf.catalog.items.CatalogItem;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.comment.PublicComment;
import ru.yandex.market.jmf.module.mail.test.impl.MailMessageBuilderService;
import ru.yandex.market.jmf.module.ticket.Brand;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Team;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.TicketCategory;
import ru.yandex.market.jmf.timings.ServiceTime;

@B2bTicketTests
public class B2bMailSuiteTicketReopeningTest extends AbstractB2bMailProcessingCreationTest {

    protected static final String MAIL_CONNECTION = "b2b";

    private Team team;
    private Service service;
    private Entity category;
    private Entity shop;

    @BeforeEach
    public void prepareData() {
        mailTestUtils.createMailConnection(MAIL_CONNECTION);
        createB2bTicketRoutingRules();

        Entity st = dbService.getByNaturalId(ServiceTime.FQN, CatalogItem.CODE, "b2b_9_21");
        serviceTimeTestUtils.createPeriod(st, "monday", "09:00", "21:00");

        team = ticketTestUtils.createTeam("firstLineMail");
        ServiceTime serviceTime24x7 = serviceTimeTestUtils.createServiceTime24x7();
        Brand brand = ticketTestUtils.createBrand();

        service = ticketTestUtils.createService(team, serviceTime24x7, brand, Optional.of("marketApiAffiliate"));

        configurationService.setValue("defaultRoutingMailServiceForB2bTicket", defaultService);

        category = bcpService.create(TicketCategory.FQN, Map.of(
                "brand", brand.getGid(),
                "code", "test",
                "title", "test"
        ));

        shop = bcpService.create(Fqn.of("account$shop"), Map.of(
                "title", "Test Shop",
                "shopId", "111111",
                "emails", Collections.singletonList("test1@ya.ru"),
                "campaignId", "21554398"
        ));
    }

    @Test
    public void testReopenTicketBySuiteTicketNumberAndValidSubject() {
        var srcTicket = ticketTestUtils.createTicket(B2bTicket.FQN,
                team,
                service,
                Map.of(B2bTicket.SUITE_TICKET_NUMBER, 123456,
                        B2bTicket.STATUS, B2bTicket.STATUS_CLOSED_DONE,
                        B2bTicket.CATEGORIES, List.of(category),
                        B2bTicket.PARTNER, shop,
                        Ticket.CHANNEL, "mail",
                        "@comment", Map.of(
                                Comment.METACLASS, PublicComment.FQN.toString(),
                                Comment.BODY, "comment"
                        )
                )
        );

        HashMap<String, List<String>> header = new HashMap<>();

        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder =
                mailMessageBuilderService.getMailMessageBuilder(MAIL_CONNECTION)
                        .setSubject("[CaSe:123456]")
                        .setHeader(header);
        processMessage(mailMessageBuilder.build());

        Ticket ticket = dbService.get(srcTicket.getGid());
        List<Comment> comments = getComments(ticket);

        Assertions.assertNotNull(ticket);
        Assertions.assertEquals(B2bTicket.STATUS_REOPENED, ticket.getStatus());
        Assertions.assertEquals(2, comments.size());
    }

    @Test
    public void testDoNotReopenTicketBySuiteTicketNumberAndInvalidSubject() {
        var srcTicket = ticketTestUtils.createTicket(B2bTicket.FQN,
                team,
                service,
                Map.of(B2bTicket.SUITE_TICKET_NUMBER, 123456,
                        B2bTicket.STATUS, B2bTicket.STATUS_CLOSED_DONE,
                        B2bTicket.CATEGORIES, List.of(category),
                        B2bTicket.PARTNER, shop,
                        "@comment", Map.of(
                                Comment.METACLASS, PublicComment.FQN.toString(),
                                Comment.BODY, "comment"
                        )
                )
        );

        HashMap<String, List<String>> header = new HashMap<>();

        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder =
                mailMessageBuilderService.getMailMessageBuilder(MAIL_CONNECTION)
                        .setSubject("[CaSe1:123456]")
                        .setHeader(header);
        processMessage(mailMessageBuilder.build());

        Ticket ticket = dbService.get(srcTicket.getGid());
        List<Comment> comments = getComments(ticket);

        Assertions.assertNotNull(ticket);
        Assertions.assertEquals(B2bTicket.STATUS_CLOSED_DONE, ticket.getStatus());
        Assertions.assertEquals(1, comments.size());
    }

    @Test
    public void testDoNotReopenClosedTicketBySuiteTicketNumberAndValidSubject() {
        var srcTicket = ticketTestUtils.createTicket(B2bTicket.FQN,
                team,
                service,
                Map.of(B2bTicket.SUITE_TICKET_NUMBER, 123456,
                        B2bTicket.STATUS, B2bTicket.STATUS_CLOSED,
                        B2bTicket.CATEGORIES, List.of(category),
                        B2bTicket.PARTNER, shop,
                        "@comment", Map.of(
                                Comment.METACLASS, PublicComment.FQN.toString(),
                                Comment.BODY, "comment"
                        )
                )
        );

        HashMap<String, List<String>> header = new HashMap<>();

        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder =
                mailMessageBuilderService.getMailMessageBuilder(MAIL_CONNECTION)
                        .setSubject("[CaSe:123456]")
                        .setHeader(header);
        processMessage(mailMessageBuilder.build());

        Ticket ticket = dbService.get(srcTicket.getGid());
        List<Comment> comments = getComments(ticket);

        Assertions.assertNotNull(ticket);
        Assertions.assertEquals(B2bTicket.STATUS_CLOSED,
                ticket.getStatus(), "Тикет закрыт в архивное состояние и не должен быть переоткрыт");
        Assertions.assertEquals(1, comments.size());
    }
}
