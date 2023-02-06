package ru.yandex.market.b2bcrm.module.ticket.test;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.b2bcrm.module.ticket.B2bTicket;
import ru.yandex.market.b2bcrm.module.ticket.test.config.B2bTicketTests;
import ru.yandex.market.b2bcrm.module.ticket.test.utils.AutoresponseTestUtils;
import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.mail.InMailMessage;
import ru.yandex.market.jmf.module.mail.impl.MailProcessingService;
import ru.yandex.market.jmf.module.mail.test.impl.MailMessageBuilderService;
import ru.yandex.market.jmf.module.mail.test.impl.MailTestUtils;
import ru.yandex.market.jmf.module.ticket.Autoresponse;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.trigger.TriggerService;

@B2bTicketTests
public class AutoresponseAndSignatureTest {

    @Inject
    AutoresponseTestUtils autoresponseTestUtils;

    @Inject
    protected ConfigurationService configurationService;

    @Inject
    protected MailMessageBuilderService mailMessageBuilderService;

    @Inject
    private TriggerService triggerService;

    @Inject
    protected TicketTestUtils ticketTestUtils;

    @Inject
    protected MailTestUtils mailTestUtils;

    @Inject
    protected MailProcessingService mailProcessingService;

    protected static final String MAIL_CONNECTION = "b2b";

    private AutoresponseTestUtils.AutoresponseTestContext context;

    @BeforeEach
    public void setUp() {
        mailTestUtils.createMailConnection(MAIL_CONNECTION);
        context = autoresponseTestUtils.createContext();
    }

    /**
     * Автоматизированы кейсы
     * - Если поле "Подпись в письмах" задано в очереди, то в поле emailSignature будет она
     */
    @Test
    public void testSignatureOverride() {
        configurationService.setValue("defaultRoutingMailServiceForB2bTicket", context.service0);
        B2bTicket ticket = getB2bTicket();

        Assertions.assertEquals(
                ticket.getService().getEmailSignature(), "signOverride",
                "Подпись должна быть взята из очереди (она задана в emailSignatureOverride)");
    }

    /**
     * Автоматизированы кейсы
     * - Если поле "Подпись в письмах" не задано в очереди, то в поле emailSignature будет подпись бренда
     */
    @Test
    public void testSignatureBrand() {
        configurationService.setValue("defaultRoutingMailServiceForB2bTicket", context.service1);
        B2bTicket ticket = getB2bTicket();

        Assertions.assertEquals(ticket.getService().getEmailSignature(),
                context.brand1.getEmailSignature(), "Подпись должна быть взята из ");
    }

    /**
     * Автоматизированы кейсы
     * - Если при создании автоответа указана очередь и не ее бренд - при сохранении принудительно выставляется бренд
     * выбранной очереди.
     */
    @Test
    public void testCreateAutoresponse() {
        Autoresponse response = autoresponseTestUtils.createAutoresponse(context.brand0, context.service1, "create");
        Assertions.assertEquals(response.getBrand().getGid(),
                context.service1.getBrand().getGid(), "Бренд должен был сменится на бренд очереди");
    }

    /**
     * Автоматизированы кейсы
     * - Если настроены автооответы только на бренд и на бренд+очередь - при поступлении письма в данную очередь
     * используется автоответ бренд+очередь.
     * - Если в справочнике задан формат ответа оператора для всего бренда и для бренда+очереди - при ответе на
     * обращение из данной очереди используется формат ответа оператора по бренд+очередь.
     */
    @Test
    public void testSelectAutoresponseForBrandAndService() {
        configurationService.setValue("defaultRoutingMailServiceForB2bTicket", context.service0);
        B2bTicket ticket = getB2bTicket();

        Autoresponse createResponse = autoresponseTestUtils.getAutoresponseForTicket(ticket, "create");
        Autoresponse commentResponse = autoresponseTestUtils.getAutoresponseForTicket(ticket, "createPublicComment");

        //Проверяем, что выберутся ответы по связке Бренд+Очередь+Ивент (когда есть и записи Бренд+Ивент)
        Assertions.assertEquals(
                createResponse.getGid(), context.responseBrandServiceCreate0.getGid(),
                "Ожидаемый ответ на создание тикета совпадает");
        Assertions.assertEquals(
                commentResponse.getGid(), context.responseBrandServiceCreateComment0.getGid(), "Ожидаемый ответ на " +
                        "создание комментария в тикете совпадает");
    }

    /**
     * Автоматизированы кейсы
     * - Если в справочнике задан формат ответа оператора для всего бренда и не настроен для конкретной очереди - при
     * ответе на обращение из данной очереди используется он (формат ответа оператора всего бренда).
     */
    @Test
    public void testSelectAutoresponseForOnlyBrand() {
        configurationService.setValue("defaultRoutingMailServiceForB2bTicket", context.service0);

        String email = "maria-nehved@yandex-team.ru";
        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder = getMessageBuilder().setFrom(email);
        processMessage(mailMessageBuilder.build());

        B2bTicket ticket = (B2bTicket) getSingleOpenedTicket(B2bTicket.FQN);

        //Проверяем, что выберутся ответы по связке Бренд+Ивент (связка Бренд+Очередь+Ивент в архиве)
        autoresponseTestUtils.archiveAutoresponse(context.responseBrandServiceCreate0);
        autoresponseTestUtils.archiveAutoresponse(context.responseBrandServiceCreateComment0);

        Autoresponse createResponse = autoresponseTestUtils.getAutoresponseForTicket(ticket, "create");
        Autoresponse commentResponse = autoresponseTestUtils.getAutoresponseForTicket(ticket, "createPublicComment");

        Assertions.assertEquals(
                createResponse.getGid(), context.responseBrandCreate0.getGid(),
                "Ожидаемый ответ на создание тикета совпадает");
        Assertions.assertEquals(
                commentResponse.getGid(), context.responseBrandCreateComment0.getGid(), "Ожидаемый ответ на создание " +
                        "комментария в тикете совпадает");
    }

    private B2bTicket getB2bTicket() {
        String email = "test@yandex.ru";
        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder =
                getMessageBuilder().setFrom(email);
        processMessage(mailMessageBuilder.build());

        return (B2bTicket) getSingleOpenedTicket(B2bTicket.FQN);
    }

    //ToDo: нужно сделать тестовые тулзы для b2b-тикетов. Методы ниже унести туда
    private MailMessageBuilderService.MailMessageBuilder getMessageBuilder() {
        return mailMessageBuilderService.getMailMessageBuilder(MAIL_CONNECTION);
    }

    protected void processMessage(InMailMessage mailMessage) {
        mailProcessingService.processInMessage(mailMessage);
    }

    private Ticket getSingleOpenedTicket(Fqn fqn) {
        return ticketTestUtils.getSingleOpenedTicket(fqn);
    }

}
