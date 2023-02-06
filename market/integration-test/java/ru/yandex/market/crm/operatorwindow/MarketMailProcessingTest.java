package ru.yandex.market.crm.operatorwindow;

import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Description;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.operatorwindow.jmf.entity.BeruTicket;
import ru.yandex.market.crm.operatorwindow.jmf.entity.MarketTicket;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.comment.UserComment;
import ru.yandex.market.jmf.module.def.Contact;
import ru.yandex.market.jmf.module.mail.InMailMessage;
import ru.yandex.market.jmf.module.mail.MailBlacklist;
import ru.yandex.market.jmf.module.mail.MailMessage;
import ru.yandex.market.jmf.module.mail.test.impl.MailMessageBuilderService;
import ru.yandex.market.jmf.module.ticket.MailConnection;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.TicketContactInComment;
import ru.yandex.market.jmf.module.ticket.TicketDeduplicationAlgorithm;
import ru.yandex.market.jmf.timings.ServiceTime;
import ru.yandex.market.jmf.utils.Maps;

import static ru.yandex.market.jmf.logic.wf.bcp.WfConstants.SKIP_WF_STATUS_CHANGE_CHECKING_ATTRIBUTE;

@Transactional
public class MarketMailProcessingTest extends AbstractMailProcessingTest {

    private static final String MAIL_CONNECTION = "market";
    private static final String MAIL_CONNECTION_BERU = "beru";

    private Service testService;

    @BeforeEach
    public void prepareData() {
        mailTestUtils.createMailConnection(MAIL_CONNECTION);
        mailTestUtils.createMailConnection(MAIL_CONNECTION_BERU);
        ServiceTime st = serviceTimeTestUtils.createServiceTime24x7();
        ticketTestUtils.setServiceTime24x7("marketQuestion");
        ticketTestUtils.setServiceTime24x7("beruQuestion");
        testService = ticketTestUtils.createService(Map.of(Service.SERVICE_TIME, st));
    }

    @Test
    public void testPartnerEmail() {
        String sender = Randoms.email();
        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder = getMailMessageBuilder(MAIL_CONNECTION);
        processMessage(mailMessageBuilder
                .setFrom(sender)
                .build()
        );
        MarketTicket ticket = getSingleOpenedMarketTicket();

        Contact partner = moduleDefaultTestUtils.createContact(ticket, List.of(Randoms.email()));

        String responseBody = Randoms.string();
        processMessage(mailMessageBuilder
                .newDeduplicationKey()
                .setBody(responseBody)
                .setSubject(mailProcessingTestUtils.createSubjectWithTicketAndPartnerNumbers(ticket, partner))
                .build()
        );

        List<Comment> comments = mailProcessingTestUtils.getComments(ticket);
        Assertions.assertEquals(2, comments.size());

        Assertions.assertEquals(TicketContactInComment.FQN, comments.get(1).getFqn());
        Assertions.assertEquals(responseBody, comments.get(1).getBody());
    }

    @Test
    public void testPartnerEmailOnClosedTicket() {
        String sender = Randoms.email();
        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder = getMailMessageBuilder(MAIL_CONNECTION);
        processMessage(mailMessageBuilder
                .setFrom(sender)
                .build()
        );
        MarketTicket ticket = getSingleOpenedMarketTicket();
        Contact partner = moduleDefaultTestUtils.createContact(ticket, List.of(Randoms.email(), Randoms.email()));
        String clientEmail = Randoms.email();
        bcpService.edit(
                ticket,
                Map.of(
                        Ticket.STATUS, Ticket.STATUS_CLOSED,
                        Ticket.CLIENT_EMAIL, clientEmail,
                        Ticket.SERVICE, testService
                ),
                Maps.of(SKIP_WF_STATUS_CHANGE_CHECKING_ATTRIBUTE, Boolean.TRUE)
        );

        String responseBody = Randoms.string();
        processMessage(mailMessageBuilder
                .newDeduplicationKey()
                .setBody(responseBody)
                .setSubject(mailProcessingTestUtils.createSubjectWithTicketAndPartnerNumbers(ticket, partner))
                .build()
        );
        MarketTicket newTicket = getSingleOpenedMarketTicket();
        mailProcessingTestUtils.assertLinkedRelation(newTicket, ticket);

        List<Comment> comments = mailProcessingTestUtils.getComments(newTicket);
        Assertions.assertEquals(1, comments.size());

        Assertions.assertEquals(TicketContactInComment.FQN, comments.get(0).getFqn());
        Assertions.assertEquals(responseBody, comments.get(0).getBody());

        Contact newContact = ((TicketContactInComment) comments.get(0)).getContact();
        Assertions.assertNotEquals(partner, newContact);
        Assertions.assertEquals(partner.getTitle(), newContact.getTitle());
        Assertions.assertEquals(partner.getEmails(), newContact.getEmails());

        Assertions.assertEquals(testService, newTicket.getService());
        Assertions.assertEquals(clientEmail, newTicket.getClientEmail());
    }

    @Test
    public void testPartnerEmailOnClosedTicketAnotherType() {
        String sender = Randoms.email();
        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder = getMailMessageBuilder(MAIL_CONNECTION);
        processMessage(mailMessageBuilder
                .setFrom(sender)
                .build()
        );
        MarketTicket ticket = getSingleOpenedMarketTicket();
        Contact partner = moduleDefaultTestUtils.createContact(ticket, List.of(Randoms.email(), Randoms.email()));
        String clientEmail = Randoms.email();
        bcpService.edit(
                ticket,
                Map.of(
                        Ticket.STATUS, Ticket.STATUS_CLOSED,
                        Ticket.CLIENT_EMAIL, clientEmail,
                        Ticket.SERVICE, testService
                ),
                Maps.of(SKIP_WF_STATUS_CHANGE_CHECKING_ATTRIBUTE, Boolean.TRUE)
        );

        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder2 = getMailMessageBuilder(MAIL_CONNECTION_BERU);
        String responseBody = Randoms.string();

        processMessage(mailMessageBuilder2
                .setFrom(sender)
                .newDeduplicationKey()
                .setBody(responseBody)
                .setSubject(mailProcessingTestUtils.createSubjectWithTicketAndPartnerNumbers(ticket, partner))
                .build()
        );
        BeruTicket newTicket = ticketTestUtils.getSingleOpenedTicket(BeruTicket.FQN);
        mailProcessingTestUtils.assertLinkedRelation(newTicket, ticket);

        List<Comment> comments = mailProcessingTestUtils.getComments(newTicket);
        Assertions.assertEquals(1, comments.size());

        Assertions.assertEquals(TicketContactInComment.FQN, comments.get(0).getFqn());
        Assertions.assertEquals(responseBody, comments.get(0).getBody());

        Contact newContact = ((TicketContactInComment) comments.get(0)).getContact();
        Assertions.assertNotEquals(partner, newContact);
        Assertions.assertEquals(partner.getTitle(), newContact.getTitle());
        Assertions.assertEquals(partner.getEmails(), newContact.getEmails());

        Assertions.assertNotEquals(testService, newTicket.getService());
        Assertions.assertEquals(clientEmail, newTicket.getClientEmail());
    }

    @Test
    public void testClientEmailOnClosedTicket() {
        String sender = Randoms.email();
        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder = getMailMessageBuilder(MAIL_CONNECTION);
        processMessage(mailMessageBuilder
                .setFrom(sender)
                .build()
        );
        MarketTicket ticket = getSingleOpenedMarketTicket();
        mailProcessingTestUtils.changeStatus(ticket, Ticket.STATUS_CLOSED);

        String responseBody = Randoms.string();
        processMessage(mailMessageBuilder
                .newDeduplicationKey()
                .setBody(responseBody)
                .setFrom(sender)
                .setSubject(mailProcessingTestUtils.createSubjectWithTicketNumber(ticket))
                .build()
        );
        MarketTicket newTicket = getSingleOpenedMarketTicket();
        mailProcessingTestUtils.assertLinkedRelation(newTicket, ticket);

        List<Comment> comments = mailProcessingTestUtils.getComments(newTicket);
        Assertions.assertEquals(1, comments.size());

        Assertions.assertEquals(UserComment.FQN, comments.get(0).getFqn());
        Assertions.assertEquals(responseBody, comments.get(0).getBody());
    }

    @Test
    public void testPartnerEmailOnClosedTicketAndNewExists() {
        String sender = Randoms.email();
        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder = getMailMessageBuilder(MAIL_CONNECTION);
        processMessage(mailMessageBuilder
                .setFrom(sender)
                .build()
        );
        MarketTicket ticket = getSingleOpenedMarketTicket();
        Contact partner = moduleDefaultTestUtils.createContact(ticket, List.of(Randoms.email(), Randoms.email()));
        mailProcessingTestUtils.changeStatus(ticket, Ticket.STATUS_CLOSED);

        String clientResponseBody = Randoms.string();
        processMessage(mailMessageBuilder
                .newDeduplicationKey()
                .setBody(clientResponseBody)
                .setFrom(sender)
                .setSubject(mailProcessingTestUtils.createSubjectWithTicketNumber(ticket))
                .build()
        );

        String partnerResponseBody = Randoms.string();
        processMessage(mailMessageBuilder
                .newDeduplicationKey()
                .setBody(partnerResponseBody)
                .setSubject(mailProcessingTestUtils.createSubjectWithTicketAndPartnerNumbers(ticket, partner))
                .build()
        );

        MarketTicket newTicket = getSingleOpenedMarketTicket();
        mailProcessingTestUtils.assertLinkedRelation(newTicket, ticket);

        List<Comment> comments = mailProcessingTestUtils.getComments(newTicket);
        Assertions.assertEquals(2, comments.size());

        Assertions.assertEquals(TicketContactInComment.FQN, comments.get(1).getFqn());
        Assertions.assertEquals(partnerResponseBody, comments.get(1).getBody());

        Contact newContact = ((TicketContactInComment) comments.get(1)).getContact();
        Assertions.assertNotEquals(partner, newContact);
        Assertions.assertEquals(partner.getTitle(), newContact.getTitle());
        Assertions.assertEquals(partner.getEmails(), newContact.getEmails());
    }

    @Test
    public void testClientResponseFromOtherEmail() {
        String sender = Randoms.email();
        String messageId = Randoms.string();
        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder = getMailMessageBuilder(MAIL_CONNECTION);
        processMessage(mailMessageBuilder
                .setFrom(sender)
                .setMessageId(messageId)
                .build()
        );
        MarketTicket ticket = getSingleOpenedMarketTicket();

        String responseBody = Randoms.string();
        String newEmail = Randoms.email();
        processMessage(mailMessageBuilder
                .newDeduplicationKey()
                .setBody(responseBody)
                .setFrom(newEmail)
                .setSubject(mailProcessingTestUtils.createSubjectWithTicketNumber(ticket))
                .setReferences(messageId)
                .build()
        );

        List<Comment> comments = mailProcessingTestUtils.getComments(ticket);
        Assertions.assertEquals(2, comments.size());

        Assertions.assertEquals(UserComment.FQN, comments.get(1).getFqn());
        Assertions.assertEquals(responseBody, comments.get(1).getBody());
        Assertions.assertEquals(newEmail, ((UserComment) comments.get(1)).getUserEmail());
    }

    @Test
    public void testNonClientResponseFromOtherEmail() {
        String sender = Randoms.email();
        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder = getMailMessageBuilder(MAIL_CONNECTION);
        processMessage(mailMessageBuilder
                .setFrom(sender)
                .build()
        );
        MarketTicket ticket = getSingleOpenedMarketTicket();

        processMessage(mailMessageBuilder
                .newDeduplicationKey()
                .setBody(Randoms.string())
                .setFrom(Randoms.email())
                .setSubject(mailProcessingTestUtils.createSubjectWithTicketNumber(ticket))
                .build()
        );

        mailProcessingTestUtils.assertTicketCount(MarketTicket.FQN, 2);
    }

    @Test
    public void testSanitizeTextPlainMailMessage() throws MessagingException {
        String result = "<div style=\"white-space:pre-wrap\">Тема будет содержать текст:  Яндекс.Покупки: изменить \n" +
                "\n" +
                "Ваше имя: 123\n" +
                "\n" +
                "Тип клиента: VIP \n" +
                "\n" +
                "Обращение созданое из формы https://forms.yandex-team.ru/surveys/46294/</div>";
        processMessage(mailMessageBuilderService.getMailMessageBuilder(MAIL_CONNECTION, "/mail_message/textPlainEmail" +
                        ".eml")
                .build()
        );
        MarketTicket ticket = getSingleOpenedMarketTicket();

        List<Comment> comments = mailProcessingTestUtils.getComments(ticket);
        Assertions.assertEquals(1, comments.size());

        Assertions.assertEquals(result, comments.get(0).getBody());
    }

    @Test
    @Description("https://testpalm.yandex-team.ru/testcase/ocrm-1574")
    public void testDefaultConnectionService() {
        Service service = ticketTestUtils.createService(
                Map.of(Service.SERVICE_TIME, serviceTimeTestUtils.createServiceTime24x7())
        );
        mailTestUtils.createMailConnection(
                "connectionWithDefaultService",
                Map.of(MailConnection.DEFAULT_SERVICE, service,
                        MailConnection.USE_DEFAULT_SCRIPT, true,
                        MailConnection.TICKET_TYPE, MarketTicket.FQN,
                        MailConnection.DEDUPLICATION_ALGORITHM, TicketDeduplicationAlgorithm.COMMON_ALGORITHM)
        );
        InMailMessage inMailMessage = getMailMessageBuilder("connectionWithDefaultService")
                .setFrom(Randoms.email())
                .build();

        processMessage(inMailMessage);

        Assertions.assertEquals(service, getSingleOpenedMarketTicket().getService());
    }

    @Test
    @Description("""
            тест-кейс https://testpalm2.yandex-team.ru/testcase/ocrm-443
            """)
    void checkingBlackList() {
        String email = Randoms.email();
        bcpService.create(MailBlacklist.FQN,
                Map.of(
                        MailBlacklist.TITLE, Randoms.string(),
                        MailBlacklist.CODE, email
                )
        );

        var mail = mailMessageBuilderService.getMailMessageBuilder(MAIL_CONNECTION)
                .setFrom(email)
                .setBody(Randoms.string())
                .build();
        mailProcessingService.processInMessage(mail);

        List<MailMessage> mailMessages = dbService.list(Query.of(MailMessage.FQN));
        List<Ticket> tickets = ticketTestUtils.getAllActiveTickets(Ticket.FQN);
        Assertions.assertEquals(1, mailMessages.size());
        Assertions.assertEquals(0, tickets.size());

    }

    private MarketTicket getSingleOpenedMarketTicket() {
        return ticketTestUtils.getSingleOpenedTicket(MarketTicket.FQN);
    }
}
