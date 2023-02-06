package ru.yandex.market.b2bcrm.module.ticket.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.jfr.Description;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.b2bcrm.module.ticket.B2bLeadTicket;
import ru.yandex.market.b2bcrm.module.ticket.B2bTicket;
import ru.yandex.market.b2bcrm.module.ticket.test.config.B2bTicketTests;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.test.assertions.EntityAssert;
import ru.yandex.market.jmf.entity.test.assertions.EntityCollectionAssert;
import ru.yandex.market.jmf.module.mail.test.impl.MailMessageBuilderService.MailMessageBuilder;
import ru.yandex.market.jmf.module.ou.Employee;
import ru.yandex.market.jmf.module.ou.Ou;
import ru.yandex.market.jmf.module.ticket.MailConnection;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.telephony.voximplant.TelephonyService;
import ru.yandex.market.jmf.utils.Maps;

@B2bTicketTests
public class B2bCdMailProcessingCreationTest extends AbstractB2bMailProcessingCreationTest {

    protected static final String MAIL_CONNECTION_CD = "b2bCd";
    private Employee employee1, employee2, employee3;
    private Entity defaultTelephonyService;

    @BeforeEach
    void setUp() {
        createAccounts();
        mailTestUtils.createMailConnection(MAIL_CONNECTION_CD, Map.of(MailConnection.TICKET_TYPE, B2bLeadTicket.FQN));

        Ou ou = ouTestUtils.createOu();
        employee1 = employeeTestUtils.createEmployee(ou, Map.of("personalEmail", "email1@email.com"));
        employee2 = employeeTestUtils.createEmployee(ou, Map.of("personalEmail", "email2@email.com"));
        employee3 = employeeTestUtils.createEmployee(ou, Map.of("personalEmail", "email3@email.com"));

        defaultTelephonyService = ticketTestUtils.createService24x7(TelephonyService.FQN);
    }


    @Test
    @Description("""
            Должен подставится поставщик по campaign Id, партнер топ по GMV,
            но тикет будет с очередью по умолчанию, поскольку письмо пришло со сборщика b2bCd.
            Специально временно ставится телефонная очередь, чтобы проверить именно эту логику, а не подмену
            нетелефонных очередей для b2bCd
            """)
    public void testCreateTicketWithTopSupplierAndDefaultTelephonyService() {
        configurationService.setValue("routingMailServiceForTopGMVPartners", topGMVService);
        configurationService.setValue("defaultRoutingMailServiceForB2bTicket", defaultTelephonyService);
        Map<String, List<String>> header = Maps.of(
            "x-marketcampaignnumber", List.of("666666")
        );

        processTypicalEmailMessage(header, MAIL_CONNECTION_CD, Randoms.email());
        EntityCollectionAssert.assertThat(ticketTestUtils.getAllActiveTickets(B2bTicket.FQN))
                .hasSize(1)
                .allHasAttributes(
                        B2bTicket.METACLASS, B2bLeadTicket.FQN,
                        B2bTicket.PARTNER, topSupplier.getGid(),
                        B2bTicket.SERVICE, defaultTelephonyService.getGid()
                );
    }

    @Test
    @Description("""
            Должен подставится поставщик по campaign Id, партнер топ по GMV,
            но тикет будет с очередью по умолчанию, поскольку письмо пришло со сборщика b2bCd.
            Очередь по умолчанию нетелефонная, поэтому должна произойти подмена на очередь b2bСDSalesCommon
            """)
    public void testCreateTicketWithTopSupplierAndDefaultService() {
        configurationService.setValue("routingMailServiceForTopGMVPartners", topGMVService);
        Map<String, List<String>> header = Maps.of(
            "x-marketcampaignnumber", List.of("666666")
        );

        processTypicalEmailMessage(header, MAIL_CONNECTION_CD, Randoms.email());

        EntityCollectionAssert.assertThat(ticketTestUtils.getAllActiveTickets(B2bTicket.FQN))
                .hasSize(1)
                .allHasAttributes(
                        B2bTicket.METACLASS, B2bLeadTicket.FQN,
                        B2bTicket.PARTNER, topSupplier.getGid(),
                        B2bTicket.SERVICE, "b2bСDSalesCommon"
                );
    }

    @Test
    public void shouldUseDefaultRoutingMailService() {
        configurationService.setValue("defaultRoutingMailServiceForB2bTicket", defaultTelephonyService);
        processMessage(getMailMessageBuilder().build());
        EntityCollectionAssert.assertThat(ticketTestUtils.<Ticket>getAllActiveTickets(B2bLeadTicket.FQN))
                .hasSize(1)
                .allHasAttributes(
                        Ticket.METACLASS, B2bLeadTicket.FQN,
                        Ticket.SERVICE, defaultTelephonyService.getGid()
                );
    }

    @Test
    public void shouldCreateTicketForEveryPersonalEmailInMailList() {
        MailMessageBuilder mailMessageBuilder =
                getMailMessageBuilder()
                        .setToList("email1@email.com", "email2@email.com")
                        .setCcList("email3@email.com");
        processMessage(mailMessageBuilder.build());

        EntityCollectionAssert.assertThat(ticketTestUtils.<Ticket>getAllActiveTickets(B2bTicket.FQN))
                .hasSize(3)
                .allHasAttributes(Ticket.METACLASS, B2bLeadTicket.FQN)
                .extracting(ticket -> (Employee) ticket.getResponsibleEmployee())
                .contains(employee1, employee2, employee3);
    }

    @Test
    public void shouldCreateOnlyOneTicketIfNoPersonalEmailInMailList() {
        MailMessageBuilder mailMessageBuilder =
                getMailMessageBuilder()
                        .setToList("notEployeeEmail@email.com", "notEployeeEmail2@email.com")
                        .setCcList("notEployeeEmai32@email.com");
        processMessage(mailMessageBuilder.build());

        EntityCollectionAssert.assertThat(ticketTestUtils.getAllActiveTickets(B2bTicket.FQN))
                .hasSize(1)
                .allHasAttributes(
                        Ticket.METACLASS, B2bLeadTicket.FQN,
                        Ticket.RESPONSIBLE_EMPLOYEE, null
                );
    }

    @Test
    public void shouldCreateNewTicketForNewEmployeesInMailList() {
        MailMessageBuilder mailMessageBuilder = getMailMessageBuilder().setToList("email1@email.com");
        processMessage(mailMessageBuilder.build());
        Ticket openedTicket = getSingleOpenedMarketTicket(B2bTicket.FQN);
        EntityAssert.assertThat(openedTicket)
                .hasAttributes(
                        Ticket.METACLASS, B2bLeadTicket.FQN,
                        Ticket.RESPONSIBLE_EMPLOYEE, employee1.getGid()
                );

        mailMessageBuilder
                .newDeduplicationKey()
                .setCcList("email2@email.com", "email3@email.com")
                .setReferences(openedTicket.getReplyMessage());
        processMessage(mailMessageBuilder.build());
        EntityCollectionAssert.assertThat(ticketTestUtils.<Ticket>getAllActiveTickets(B2bTicket.FQN))
                .hasSize(3)
                .allHasAttributes(Ticket.METACLASS, B2bLeadTicket.FQN)
                .extracting(ticket -> (Employee) ticket.getResponsibleEmployee())
                .contains(employee1, employee2, employee3);
    }

    @Test
    @Description("""
            При поступлении письма: в копию для ответного письма сохраняем всех получателей,
            кроме текущего ответственного
            """)
    public void shouldSetMailCopyTo() {
        MailMessageBuilder mailMessageBuilder = getMailMessageBuilder().setToList("email1@email.com");
        processMessage(mailMessageBuilder.build());
        Ticket openedTicket = getSingleOpenedMarketTicket(B2bTicket.FQN);
        EntityAssert.assertThat(openedTicket)
                .hasAttributes(
                        Ticket.METACLASS, B2bLeadTicket.FQN,
                        Ticket.RESPONSIBLE_EMPLOYEE, employee1.getGid()
                );

        mailMessageBuilder
                .newDeduplicationKey()
                .setCcList("email2@email.com")
                .setReferences(openedTicket.getReplyMessage());
        processMessage(mailMessageBuilder.build());
        EntityCollectionAssert.assertThat(ticketTestUtils.<Ticket>getAllActiveTickets(B2bTicket.FQN))
                .hasSize(2)
                .allHasAttributes(Ticket.METACLASS, B2bLeadTicket.FQN)

                .withFailMessage("Для существующего тикета в копию проставится новый получатель треда")
                .anyHasAttributes(
                        Ticket.RESPONSIBLE_EMPLOYEE, employee1.getGid(),
                        B2bLeadTicket.MAIL_COPY_TO, "email2@email.com"
                )

                .withFailMessage("Для нового тикета в копию проставится уже существующий получатель")
                .anyHasAttributes(
                        Ticket.RESPONSIBLE_EMPLOYEE, employee2.getGid(),
                        B2bLeadTicket.MAIL_COPY_TO, "email1@email.com"
                );
    }

    private MailMessageBuilder getMailMessageBuilder() {
        return getMailMessageBuilder(new HashMap<>(), MAIL_CONNECTION_CD, Randoms.email());
    }

}
