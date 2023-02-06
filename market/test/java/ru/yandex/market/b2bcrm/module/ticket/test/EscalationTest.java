package ru.yandex.market.b2bcrm.module.ticket.test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.b2bcrm.module.ticket.B2bTicket;
import ru.yandex.market.b2bcrm.module.ticket.test.config.B2bTicketTests;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.entity.query.SortingOrder;
import ru.yandex.market.jmf.entity.test.assertions.EntityCollectionAssert;
import ru.yandex.market.jmf.module.def.Contact;
import ru.yandex.market.jmf.module.mail.InMailMessage;
import ru.yandex.market.jmf.module.mail.test.impl.MailMessageBuilderService.MailMessageBuilder;
import ru.yandex.market.jmf.module.ticket.Brand;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Team;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.TicketCategory;
import ru.yandex.market.jmf.timings.ServiceTime;

import static org.assertj.core.api.Assertions.assertThat;

@B2bTicketTests
public class EscalationTest extends AbstractB2bMailProcessingCreationTest {

    protected static final String MAIL_CONNECTION = "b2b";
    protected static final String ESCALATION_SERVICE = "escalationService";
    protected static final String COUNT_PARTNER_MESSAGE_FOR_ESCALATION = "partnerMessagesCountForEscalation";
    protected static final String TEAM_NAME = "firstLineMail";

    private Service escalationService;
    private Service marketApiService;
    private TicketCategory category;

    @BeforeEach
    public void prepareData() {
        mailTestUtils.createMailConnection(MAIL_CONNECTION);

        Team team0 = ticketTestUtils.createTeam(TEAM_NAME);
        ServiceTime serviceTime24x7 = serviceTimeTestUtils.createServiceTime24x7();
        Brand brand = ticketTestUtils.createBrand();

        category = bcpService.create(TicketCategory.FQN, Map.of(
                "brand", brand.getGid(),
                "code", "test",
                "title", "test"
        ));

        escalationService = ticketTestUtils.createService(team0, serviceTime24x7, brand, Optional.of(
                "marketApiAffiliateEscalation"));
        marketApiService = ticketTestUtils.createService(team0, serviceTime24x7, brand, Optional.of(
                "marketApiAffiliate"), Map.of(ESCALATION_SERVICE, escalationService.getId(),
                COUNT_PARTNER_MESSAGE_FOR_ESCALATION, 3));
        configurationService.setValue("defaultRoutingMailServiceForB2bTicket", marketApiService);
    }

    @Test
    public void escalate() {
        MailMessageBuilder mailMessageBuilder = getMessageBuilder().setFrom("test3@ya.ru");
        B2bTicket ticket = createTicketForEscalation(mailMessageBuilder);
        Contact contact = moduleDefaultTestUtils.createContact(ticket, List.of(Randoms.email()));
        mailMessageBuilder.setSubject(createSubjectWithTicketAndContactNumbers(ticket, contact));

        List<B2bTicket> ticketsToCheck = List.of(ticket);
        checkNoEscalation(mailMessageBuilder, ticketsToCheck);
        checkEscalationCounterIncremented(mailMessageBuilder, ticketsToCheck);
        checkEscalated(mailMessageBuilder, ticketsToCheck);
    }

    @Test
    public void escalateMultipleTicketsByEmailReference() {
        MailMessageBuilder mailMessageBuilder = getMessageBuilder().setFrom("test3@ya.ru");

        List<B2bTicket> tickets = Stream.generate(() -> createTicketForEscalation(mailMessageBuilder.newDeduplicationKey()))
                .limit(3)
                .collect(Collectors.toList());

        String emailReferences = tickets.stream()
                .map(Ticket::getReplyMessage)
                .collect(Collectors.joining(" "));
        mailMessageBuilder.setReferences(emailReferences);

        checkNoEscalation(mailMessageBuilder, tickets);
        checkEscalationCounterIncremented(mailMessageBuilder, tickets);
        checkEscalated(mailMessageBuilder, tickets);
    }

    private B2bTicket createTicketForEscalation(MailMessageBuilder mailMessageBuilder) {
        processMessage(mailMessageBuilder.build());
        Query query = Query.of(B2bTicket.FQN)
                .withSortingOrder(SortingOrder.desc(Ticket.CREATION_TIME))
                .withLimit(1);
        B2bTicket ticket = dbService.<B2bTicket>list(query).get(0);

        bcpService.edit(ticket.getGid(), Map.of(B2bTicket.CATEGORIES, List.of(category.getGid())));
        assertThat(ticket.getEscalationCounter())
                .withFailMessage("Первое письмо не учитывается в счетчике (= 0)")
                .isEqualTo(0L);
        return ticket;
    }

    private void checkNoEscalation(MailMessageBuilder mailMessageBuilder, Collection<B2bTicket> tickets) {
        //Переводим тикет в статус Ожидаем ответ от клиента по запросу смежника
        for (B2bTicket ticket : tickets) {
            ticketTestUtils.editTicketStatus(ticket, B2bTicket.STATUS_CLOSED_WAIT_CLIENT_CONTIGUOUS);
        }
        //Проверяем, что письмо в обращение в этом статусе не увеличивает счетчик эскалации
        sendAndProcessMessage(mailMessageBuilder);
        EntityCollectionAssert.assertThat(tickets)
                .withFailMessage("Очередь не изменилась на очередь эскалации")
                .allHasAttributes(Ticket.SERVICE, marketApiService.getGid())

                .withFailMessage("Счетчик эскалации остался 0")
                .allHasAttributes(B2bTicket.ESCALATION_COUNTER, 0L);
    }

    private void checkEscalationCounterIncremented(MailMessageBuilder mailMessageBuilder,
                                                   Collection<B2bTicket> tickets) {
        //Переводим тикет в статус, учитывающийся в счетчике эскалации
        for (B2bTicket ticket : tickets) {
            ticketTestUtils.editTicketStatus(ticket, B2bTicket.STATUS_CLOSED_WAIT_CLIENT);
        }
        //Отправляем письма и проверяем счетчик и очередь
        sendAndProcessMessage(mailMessageBuilder);
        sendAndProcessMessage(mailMessageBuilder);
        EntityCollectionAssert.assertThat(tickets)
                .withFailMessage("Очередь не изменилась на очередь эскалации")
                .allHasAttributes(Ticket.SERVICE, marketApiService.getGid())

                .withFailMessage("Счетчик эскалации стал 2")
                .allHasAttributes(B2bTicket.ESCALATION_COUNTER, 2L);
    }

    private void checkEscalated(MailMessageBuilder mailMessageBuilder, Collection<B2bTicket> tickets) {
        //Отправляем письмо и проверяем, что эскалация сработала: счетчик сбросился + очередь сменилась
        sendAndProcessMessage(mailMessageBuilder);
        EntityCollectionAssert.assertThat(tickets)
                .withFailMessage("Очередь изменилась на очередь эскалации")
                .allHasAttributes(Ticket.SERVICE, escalationService.getGid())

                .withFailMessage("Счетчик эскалации стал 0")
                .allHasAttributes(B2bTicket.ESCALATION_COUNTER, 0L);
    }

    private void sendAndProcessMessage(MailMessageBuilder mailMessageBuilder) {
        String responseBody = Randoms.string();

        mailMessageBuilder
                .newDeduplicationKey()
                .setBody(responseBody);

        InMailMessage message = mailMessageBuilder.build();
        processMessage(message);
    }

    private MailMessageBuilder getMessageBuilder() {
        return mailMessageBuilderService.getMailMessageBuilder(MAIL_CONNECTION);
    }
}
