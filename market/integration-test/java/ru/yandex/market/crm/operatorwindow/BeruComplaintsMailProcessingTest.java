package ru.yandex.market.crm.operatorwindow;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.mail.MessagingException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.entity.query.SortingOrder;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.comment.InternalComment;
import ru.yandex.market.jmf.module.def.Contact;
import ru.yandex.market.jmf.module.mail.test.impl.MailMessageBuilderService;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.TicketContactInComment;
import ru.yandex.market.jmf.utils.html.Htmls;
import ru.yandex.market.ocrm.module.complaints.BeruComplaintsTicket;
import ru.yandex.market.ocrm.module.order.domain.DeliveryService;

@Transactional
public class BeruComplaintsMailProcessingTest extends AbstractBeruComplaintsMailProcessingTest {

    @Inject
    private Htmls htmls;

    @Test
    public void testLargeMail() throws MessagingException {
        processMessage(createMailMessageBuilder("/mail_message/beruComplaintsLargeEmail.eml").build());
        getSingleOpenedBeruComplaintsTicket();
    }

    @Test
    public void testClientMail() {
        String body = Randoms.string();
        String sender = Randoms.email();
        processMessage(createMailMessageBuilder()
                .setBody(body)
                .setFrom(sender)
                .setSubject(Randoms.string() + " - заказ " + DEFAULT_ORDER_ID)
                .build()
        );

        BeruComplaintsTicket ticket = getSingleOpenedBeruComplaintsTicket();
        assertSingleComment(ticket, Comment.FQN_USER, body);
        Assertions.assertNotNull(ticket.getOrder());
        Assertions.assertEquals(DEFAULT_ORDER_ID, ticket.getOrder().getOrderId());
        Assertions.assertEquals(sender, ticket.getClientEmail());
    }

    @Test
    public void testFirstLineMail() {
        String body = Randoms.string();
        processMessage(createMailMessageBuilder()
                .setFrom(FIRST_LINE_SENDER_EMAIL)
                .setBody(body)
                .build()
        );

        BeruComplaintsTicket ticket = getSingleOpenedBeruComplaintsTicket();
        assertSingleComment(ticket, InternalComment.FQN, body);
        Assertions.assertNull(ticket.getClientEmail());
    }

    @Test
    public void testFirstLineMailWithClientEmail() {
        String clientEmail = Randoms.email();
        String body = Randoms.string() + "\nE-mail для связи:\n" + clientEmail + "\n" + Randoms.string();
        processMessage(createMailMessageBuilder()
                .setFrom(FIRST_LINE_SENDER_EMAIL)
                .setBody(body)
                .build()
        );

        BeruComplaintsTicket ticket = getSingleOpenedBeruComplaintsTicket();
        assertSingleComment(ticket, InternalComment.FQN, body);
        Assertions.assertEquals(clientEmail, ticket.getClientEmail());
    }

    @Test
    public void testResponseClientOnClosedTicket() {
        String subject = Randoms.string();
        processMessage(createMailMessageBuilder()
                .setSubject(subject)
                .setBody(Randoms.string() + " заказ " + DEFAULT_ORDER_ID)
                .build()
        );
        BeruComplaintsTicket ticket = getSingleOpenedBeruComplaintsTicket();
        mailProcessingTestUtils.changeStatus(ticket, Ticket.STATUS_CLOSED);

        processMessage(createMailMessageBuilder()
                .newDeduplicationKey()
                .setSubject(mailProcessingTestUtils.createSubjectWithTicketNumber(ticket))
                .build()
        );
        BeruComplaintsTicket newTicket = getSingleOpenedBeruComplaintsTicket();
        mailProcessingTestUtils.assertLinkedRelation(newTicket, ticket);
        Assertions.assertEquals(subject, newTicket.getTitle());
        Assertions.assertNotNull(newTicket.getOrder());
        Assertions.assertEquals(DEFAULT_ORDER_ID, newTicket.getOrder().getOrderId());
    }

    @Test
    public void testReopenTicketOnClientResponse() {
        String sender = Randoms.email();
        processMessage(createMailMessageBuilder().setFrom(sender).build());
        BeruComplaintsTicket ticket = getSingleOpenedBeruComplaintsTicket();
        mailProcessingTestUtils.changeStatus(ticket, BeruComplaintsTicket.STATUS_WAITING_RESPONSE);

        String responseBody = Randoms.string();
        processMessage(createMailMessageBuilder()
                .newDeduplicationKey()
                .setFrom(sender)
                .setBody(responseBody)
                .setSubject(mailProcessingTestUtils.createSubjectWithTicketNumber(ticket))
                .build()
        );
        List<Comment> comments = mailProcessingTestUtils.getComments(ticket);
        Assertions.assertEquals(2, comments.size());

        Assertions.assertEquals(Comment.FQN_USER, comments.get(1).getFqn());
        Assertions.assertEquals(responseBody, comments.get(1).getBody());

        Assertions.assertNotNull(ticket.getResolution());
        Assertions.assertEquals(Constants.Resolution.RESPONSE_FROM_CLIENT, ticket.getResolution().getCode());

        Assertions.assertEquals(Ticket.STATUS_REOPENED, ticket.getStatus());
    }

    @Test
    public void testReopenTicketOnWarehouseResponse() {
        processMessage(createMailMessageBuilder().build());
        BeruComplaintsTicket ticket = getSingleOpenedBeruComplaintsTicket();
        mailProcessingTestUtils.changeStatus(ticket, BeruComplaintsTicket.STATUS_WAITING_RESPONSE);

        String responseBody = Randoms.string();
        processMessage(createMailMessageBuilder()
                .newDeduplicationKey()
                .setFrom(WAREHOUSE_SENDER_EMAIL)
                .setBody(responseBody)
                .setSubject(mailProcessingTestUtils.createSubjectWithTicketNumber(ticket))
                .build()
        );
        List<Comment> comments = mailProcessingTestUtils.getComments(ticket);
        Assertions.assertEquals(2, comments.size());

        Assertions.assertEquals(InternalComment.FQN, comments.get(1).getFqn());
        Assertions.assertEquals(responseBody, comments.get(1).getBody());

        Assertions.assertNotNull(ticket.getResolution());
        Assertions.assertEquals(Constants.Resolution.RESPONSE_FROM_WAREHOUSE, ticket.getResolution().getCode());

        Assertions.assertEquals(Ticket.STATUS_REOPENED, ticket.getStatus());
    }

    @Test
    public void testReopenTicketByOrderAndService() {
        String sender = Randoms.email();
        processMessage(createMailMessageBuilder()
                .setFrom(sender)
                .setSubject(Randoms.string() + " - заказ " + DEFAULT_ORDER_ID)
                .build()
        );

        BeruComplaintsTicket ticket = getSingleOpenedBeruComplaintsTicket();
        mailProcessingTestUtils.changeStatus(ticket, BeruComplaintsTicket.STATUS_WAITING_RESPONSE);

        String responseBody = Randoms.string();
        processMessage(createMailMessageBuilder()
                .newDeduplicationKey()
                .setFrom(sender)
                .setSubject(Randoms.string() + " - заказ " + DEFAULT_ORDER_ID)
                .setBody(responseBody)
                .build()
        );
        List<Comment> comments = mailProcessingTestUtils.getComments(ticket);
        Assertions.assertEquals(2, comments.size());

        Assertions.assertEquals(Comment.FQN_USER, comments.get(1).getFqn());
        Assertions.assertEquals(responseBody, comments.get(1).getBody());

        Assertions.assertNotNull(ticket.getResolution());
        Assertions.assertEquals(Constants.Resolution.RESPONSE_FROM_CLIENT, ticket.getResolution().getCode());

        Assertions.assertEquals(Ticket.STATUS_REOPENED, ticket.getStatus());
    }

    @Test
    public void testNoReopeningProcessingTicketOnClientResponse() {
        processMessage(createMailMessageBuilder().build());
        BeruComplaintsTicket ticket = getSingleOpenedBeruComplaintsTicket();
        mailProcessingTestUtils.changeStatus(ticket, BeruComplaintsTicket.STATUS_PROCESSING);

        String responseBody = Randoms.string();
        processMessage(createMailMessageBuilder()
                .newDeduplicationKey()
                .setBody(responseBody)
                .setSubject(mailProcessingTestUtils.createSubjectWithTicketNumber(ticket))
                .build()
        );
        List<Comment> comments = mailProcessingTestUtils.getComments(ticket);
        Assertions.assertEquals(2, comments.size());

        Assertions.assertNull(ticket.getResolution());
        Assertions.assertEquals(Ticket.STATUS_PROCESSING, ticket.getStatus());
    }

    @Test
    public void testReopenTicketOnTicketPartnerResponse() {
        processMessage(createMailMessageBuilder().build());
        BeruComplaintsTicket ticket = getSingleOpenedBeruComplaintsTicket();
        mailProcessingTestUtils.changeStatus(ticket, BeruComplaintsTicket.STATUS_WAITING_RESPONSE);

        List<String> partnerEmails = List.of(createEmailInLowerCase(), createEmailInLowerCase(),
                createEmailInLowerCase(), createEmailInLowerCase(), createEmailInLowerCase(), createEmailInLowerCase());
        Contact partner = moduleDefaultTestUtils.createContact(ticket,
                List.of(partnerEmails.get(0).toUpperCase(), partnerEmails.get(1), partnerEmails.get(2)));

        String responseBody = Randoms.string();
        processMessage(createMailMessageBuilder()
                .newDeduplicationKey()
                .setBody(responseBody)
                .setSubject(mailProcessingTestUtils.createSubjectWithTicketAndPartnerNumbers(ticket, partner))
                .setFrom(partnerEmails.get(3))
                .setToList(BERU_COMPLAINTS_EMAIL, partnerEmails.get(1).toUpperCase(), partnerEmails.get(4))
                .setCcList(BERU_COMPLAINTS_EMAIL, partnerEmails.get(2), partnerEmails.get(5).toUpperCase())
                .build()
        );
        List<Comment> comments = mailProcessingTestUtils.getComments(ticket);
        Assertions.assertEquals(2, comments.size());

        Assertions.assertEquals(TicketContactInComment.FQN, comments.get(1).getFqn());
        Assertions.assertEquals(responseBody, comments.get(1).getBody());

        TicketContactInComment contactInComment = (TicketContactInComment) comments.get(1);
        Assertions.assertNotNull(contactInComment.getContact());
        Assertions.assertEquals(partner, contactInComment.getContact());

        Assertions.assertNotNull(ticket.getResolution());
        Assertions.assertEquals(Constants.Resolution.RESPONSE_FROM_PARTNER, ticket.getResolution().getCode());

        Assertions.assertEquals(Ticket.STATUS_REOPENED, ticket.getStatus());
        Assertions.assertEquals(partnerEmails.size(), partner.getEmails().size());
        Assertions.assertEquals(Set.copyOf(partnerEmails), Set.copyOf(partner.getEmails()));
    }

    @Test
    public void testReopenTicketOnUnknownPartnerResponse() {
        processMessage(createMailMessageBuilder()
                .setSubject(Randoms.string() + " - заказ " + DEFAULT_ORDER_ID)
                .build()
        );

        BeruComplaintsTicket ticket = getSingleOpenedBeruComplaintsTicket();
        mailProcessingTestUtils.changeStatus(ticket, BeruComplaintsTicket.STATUS_WAITING_RESPONSE);

        String sender = Randoms.email();
        String recipient = Randoms.email();
        String recipientCc = Randoms.email();
        String replyTo = Randoms.email();
        String responseBody = Randoms.string();
        processMessage(createMailMessageBuilder()
                .newDeduplicationKey()
                .setFrom(sender)
                .setToList(recipient)
                .setCcList(recipientCc)
                .setReplyToList(replyTo)
                .setSubject(Randoms.string() + " - заказ " + DEFAULT_ORDER_ID)
                .setBody(responseBody)
                .build()
        );
        List<Comment> comments = mailProcessingTestUtils.getComments(ticket);
        Assertions.assertEquals(2, comments.size());

        Assertions.assertEquals(TicketContactInComment.FQN, comments.get(1).getFqn());
        Assertions.assertEquals(responseBody, comments.get(1).getBody());

        TicketContactInComment contactInComment = (TicketContactInComment) comments.get(1);
        Contact partner = contactInComment.getContact();
        Assertions.assertNotNull(partner);
        Assertions.assertEquals(Set.of(sender, recipient, recipientCc, replyTo), Set.copyOf(partner.getEmails()));

        Assertions.assertNotNull(ticket.getResolution());
        Assertions.assertEquals(Constants.Resolution.RESPONSE_FROM_PARTNER, ticket.getResolution().getCode());

        Assertions.assertEquals(Ticket.STATUS_REOPENED, ticket.getStatus());
    }

    @Test
    public void testReopenTicketOnTicketDeliveryServicePartnerResponse() {
        processMessage(createMailMessageBuilder().build());
        BeruComplaintsTicket ticket = getSingleOpenedBeruComplaintsTicket();
        mailProcessingTestUtils.changeStatus(ticket, BeruComplaintsTicket.STATUS_WAITING_RESPONSE);

        DeliveryService deliveryService = orderTestUtils.createDeliveryService(Randoms.string());
        List<String> deliveryServiceEmails = List.of(Randoms.email(), Randoms.email());
        Contact dsContact = moduleDefaultTestUtils.createContact(deliveryService, deliveryServiceEmails);

        String responseBody = Randoms.string();
        processMessage(createMailMessageBuilder()
                .newDeduplicationKey()
                .setBody(responseBody)
                .setSubject(mailProcessingTestUtils.createSubjectWithTicketAndPartnerNumbers(ticket, dsContact))
                .setFrom(Randoms.email())
                .setToList(BERU_COMPLAINTS_EMAIL, Randoms.email())
                .setCcList(BERU_COMPLAINTS_EMAIL, Randoms.email())
                .build()
        );
        List<Comment> comments = mailProcessingTestUtils.getComments(ticket);
        Assertions.assertEquals(2, comments.size());

        Assertions.assertEquals(TicketContactInComment.FQN, comments.get(1).getFqn());
        Assertions.assertEquals(responseBody, comments.get(1).getBody());

        Assertions.assertNotNull(ticket.getResolution());
        Assertions.assertEquals(Constants.Resolution.RESPONSE_FROM_PARTNER, ticket.getResolution().getCode());

        Assertions.assertEquals(Ticket.STATUS_REOPENED, ticket.getStatus());
        Assertions.assertEquals(deliveryServiceEmails.size(), dsContact.getEmails().size());
        Assertions.assertEquals(Set.copyOf(deliveryServiceEmails), Set.copyOf(dsContact.getEmails()));
    }

    @Test
    public void testResponseTicketPartnerOnClosedTicket() {
        String subject = Randoms.string();
        processMessage(createMailMessageBuilder().setSubject(subject).build());
        BeruComplaintsTicket ticket = getSingleOpenedBeruComplaintsTicket();
        mailProcessingTestUtils.changeStatus(ticket, BeruComplaintsTicket.STATUS_CLOSED);

        List<String> partnerEmails = List.of(Randoms.email(), Randoms.email(), Randoms.email(), Randoms.email(),
                Randoms.email(), Randoms.email());
        Contact partner = moduleDefaultTestUtils.createContact(ticket,
                List.of(partnerEmails.get(0), partnerEmails.get(1), partnerEmails.get(2)));

        String responseBody = Randoms.string();
        processMessage(createMailMessageBuilder()
                .newDeduplicationKey()
                .setBody(responseBody)
                .setSubject(mailProcessingTestUtils.createSubjectWithTicketAndPartnerNumbers(ticket, partner))
                .setFrom(partnerEmails.get(3))
                .setToList(BERU_COMPLAINTS_EMAIL, partnerEmails.get(1), partnerEmails.get(4))
                .setCcList(BERU_COMPLAINTS_EMAIL, partnerEmails.get(2), partnerEmails.get(5))
                .build()
        );

        BeruComplaintsTicket newTicket = getSingleOpenedBeruComplaintsTicket();
        mailProcessingTestUtils.assertLinkedRelation(newTicket, ticket);
        Assertions.assertEquals(subject, newTicket.getTitle());

        List<Comment> comments = mailProcessingTestUtils.getComments(newTicket);
        Assertions.assertEquals(1, comments.size());

        Assertions.assertEquals(TicketContactInComment.FQN, comments.get(0).getFqn());
        Assertions.assertEquals(responseBody, comments.get(0).getBody());

        Assertions.assertNull(newTicket.getResolution());

        Assertions.assertEquals(Ticket.STATUS_REGISTERED, newTicket.getStatus());
        Assertions.assertEquals(partnerEmails.size(), partner.getEmails().size());
        Assertions.assertEquals(Set.copyOf(partnerEmails), Set.copyOf(partner.getEmails()));

        List<Contact> contacts = getContacts(newTicket);
        Assertions.assertEquals(1, contacts.size());
        Assertions.assertEquals(partnerEmails.size(), contacts.get(0).getEmails().size());
        Assertions.assertEquals(Set.copyOf(partnerEmails), Set.copyOf(contacts.get(0).getEmails()));
    }

    @Test
    public void testResponseDeliveryServicePartnerOnClosedTicket() {
        String subject = Randoms.string();
        processMessage(createMailMessageBuilder().setSubject(subject).build());
        BeruComplaintsTicket ticket = getSingleOpenedBeruComplaintsTicket();
        mailProcessingTestUtils.changeStatus(ticket, BeruComplaintsTicket.STATUS_CLOSED);

        DeliveryService deliveryService = orderTestUtils.createDeliveryService(Randoms.string());
        List<String> deliveryServiceEmails = List.of(Randoms.email(), Randoms.email());
        Contact dsContact = moduleDefaultTestUtils.createContact(deliveryService, deliveryServiceEmails);

        String responseBody = Randoms.string();
        processMessage(createMailMessageBuilder()
                .newDeduplicationKey()
                .setBody(responseBody)
                .setSubject(mailProcessingTestUtils.createSubjectWithTicketAndPartnerNumbers(ticket, dsContact))
                .setFrom(Randoms.email())
                .setToList(BERU_COMPLAINTS_EMAIL, Randoms.email())
                .setCcList(BERU_COMPLAINTS_EMAIL, Randoms.email())
                .build()
        );

        BeruComplaintsTicket newTicket = getSingleOpenedBeruComplaintsTicket();
        mailProcessingTestUtils.assertLinkedRelation(newTicket, ticket);
        Assertions.assertEquals(subject, newTicket.getTitle());

        List<Comment> comments = mailProcessingTestUtils.getComments(newTicket);
        Assertions.assertEquals(1, comments.size());

        Assertions.assertEquals(TicketContactInComment.FQN, comments.get(0).getFqn());
        Assertions.assertEquals(responseBody, comments.get(0).getBody());

        Assertions.assertNull(newTicket.getResolution());

        Assertions.assertEquals(Ticket.STATUS_REGISTERED, newTicket.getStatus());
        Assertions.assertEquals(deliveryServiceEmails.size(), dsContact.getEmails().size());
        Assertions.assertEquals(Set.copyOf(deliveryServiceEmails), Set.copyOf(dsContact.getEmails()));

        List<Contact> contacts = getContacts(newTicket);
        Assertions.assertEquals(0, contacts.size());
    }

    @Test
    public void testReopenSourceTicket() {
        Ticket sourceTicket = ticketTestUtils.createTicket(
                BeruComplaintsTicket.FQN,
                Map.of(
                        BeruComplaintsTicket.STATUS, BeruComplaintsTicket.STATUS_ON_HOLD,
                        BeruComplaintsTicket.SERVICE, createBeruComplaintsService()
                )
        );

        String emailBody = String.format("Номер исходного обращения: %d", sourceTicket.getId());
        processMessage(createMailMessageBuilder().setBody(emailBody).build());

        List<BeruComplaintsTicket> tickets = getOpenedBeruComplaintsTickets();
        Assertions.assertEquals(2, tickets.size());
        Assertions.assertEquals(sourceTicket, tickets.get(0));
        BeruComplaintsTicket ticket = tickets.get(1);
        Assertions.assertEquals(sourceTicket, ticket.getSourceTicket());

        mailProcessingTestUtils.changeStatus(ticket, BeruComplaintsTicket.STATUS_RESOLVED);
        Assertions.assertEquals(BeruComplaintsTicket.STATUS_REOPENED, sourceTicket.getStatus());

        Assertions.assertNotNull(sourceTicket.getResolution());
        Assertions.assertEquals(Constants.Resolution.RESPONSE_FROM_ADJACENT_DEPARTMENT,
                sourceTicket.getResolution().getCode());
    }

    private List<BeruComplaintsTicket> getOpenedBeruComplaintsTickets() {
        return dbService.list(Query.of(BeruComplaintsTicket.FQN)
                .withFilters(
                        Filters.ne(Ticket.STATUS, Ticket.STATUS_CLOSED)
                )
                .withSortingOrder(SortingOrder.asc(BeruComplaintsTicket.CREATION_TIME))
        );
    }

    private Service createBeruComplaintsService() {
        return ticketTestUtils.createService(Fqn.of("service$telephony"), Map.of(
                Service.SERVICE_TIME, "08_21",
                Service.SUPPORT_TIME, "08_21"
        ));
    }

    private MailMessageBuilderService.MailMessageBuilder createMailMessageBuilder() {
        return mailMessageBuilderService.getMailMessageBuilder(BERU_COMPLAINTS_MAIL_CONNECTION);
    }

    private MailMessageBuilderService.MailMessageBuilder createMailMessageBuilder(String path) throws MessagingException {
        return mailMessageBuilderService.getMailMessageBuilder(BERU_COMPLAINTS_MAIL_CONNECTION, path);
    }

    private String createEmailInLowerCase() {
        return Randoms.email().toLowerCase();
    }

    private List<Contact> getContacts(Ticket ticket) {
        Query q = Query.of(Contact.FQN)
                .withFilters(Filters.eq(Contact.PARENT, ticket.getGid()));
        return dbService.list(q);
    }

    private void assertSingleComment(Ticket ticket, Fqn fqn, String expectedBody) {
        List<Comment> comments = dbService.list(Query.of(Comment.FQN).withFilters(Filters.eq(Comment.ENTITY, ticket)));
        Assertions.assertEquals(1, comments.size());
        Comment comment = comments.get(0);
        Assertions.assertEquals(fqn, comment.getFqn());
        Assertions.assertEquals(htmls.hideQuotes(expectedBody), comment.getBody());
    }
}
