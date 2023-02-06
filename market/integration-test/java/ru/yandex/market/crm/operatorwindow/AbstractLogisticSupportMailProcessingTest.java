package ru.yandex.market.crm.operatorwindow;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.mail.MessagingException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import ru.yandex.market.crm.operatorwindow.jmf.entity.LogisticSupportTicket;
import ru.yandex.market.crm.operatorwindow.utils.mail.LogisticMailBodyBuilder;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.catalog.items.CatalogItem;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.mail.MailConnection;
import ru.yandex.market.jmf.module.mail.test.impl.MailMessageBuilderService;
import ru.yandex.market.jmf.module.ticket.Brand;
import ru.yandex.market.jmf.module.ticket.TicketCategory;
import ru.yandex.market.jmf.script.ScriptContextVariablesService;
import ru.yandex.market.jmf.timings.ServiceTime;
import ru.yandex.market.ocrm.module.order.domain.Order;
import ru.yandex.market.ocrm.module.yadelivery.test.YaDeliveryTestUtils;

public class AbstractLogisticSupportMailProcessingTest extends AbstractMailProcessingTest {

    protected static final String TEST_CATEGORY_CODE = "testCategory";
    protected static final String TEST_CATEGORY_TITLE = "Нарушены сроки доставки";
    protected static final String BERU_LOGISTIC_SUPPORT_MAIL_CONNECTION = "logisticSupport";
    protected static final String YA_DELIVERY_LOGISTIC_SUPPORT_MAIL_CONNECTION = "yaDeliveryLogisticSupport";

    protected MailConnection beruMailConnection;
    protected MailConnection yaDeliveryMailConnection;
    protected MailMessageBuilderService.MailMessageBuilder beruMailMessageBuilder;
    protected MailMessageBuilderService.MailMessageBuilder yaDeliveryMailMessageBuilder;
    @Inject
    protected YaDeliveryTestUtils yaDeliveryTestUtils;

    @BeforeEach
    public void prepareData() throws MessagingException {
        Entity st = dbService.getByNaturalId(ServiceTime.FQN, CatalogItem.CODE, "08_22");
        serviceTimeTestUtils.createPeriod(st, "monday", "08:00", "22:00");

        beruMailConnection = mailTestUtils.createMailConnection(BERU_LOGISTIC_SUPPORT_MAIL_CONNECTION);
        yaDeliveryMailConnection = mailTestUtils.createMailConnection(YA_DELIVERY_LOGISTIC_SUPPORT_MAIL_CONNECTION);

        createDefaultTicketCategory(Constants.Brand.BERU_LOGISTIC_SUPPORT);
        createDefaultTicketCategory(Constants.Brand.YANDEX_DELIVERY_LOGISTIC_SUPPORT);

        orderTestUtils.createOrder(Map.of(Order.NUMBER, LogisticMailBodyBuilder.DEFAULT_ORDER_ID));
        orderTestUtils.createDeliveryService(LogisticMailBodyBuilder.DEFAULT_DELIVERY_SERVICE);
        orderTestUtils.createSortingCenter(LogisticMailBodyBuilder.DEFAULT_SORTING_CENTER);

        beruMailMessageBuilder = createDefaultBeruMailMessageBuilder();
        yaDeliveryMailMessageBuilder = createDefaultYaDeliveryMailMessageBuilder();
    }

    protected MailMessageBuilderService.MailMessageBuilder createDefaultBeruMailMessageBuilder() throws MessagingException {
        return mailMessageBuilderService.getMailMessageBuilder(beruMailConnection.getCode(),
                "/mail_message/logisticSupportEmail.eml");
    }

    protected MailMessageBuilderService.MailMessageBuilder createBeruMailMessageBuilder(String messagePath) throws MessagingException {
        return mailMessageBuilderService.getMailMessageBuilder(beruMailConnection.getCode(), messagePath);
    }

    protected MailMessageBuilderService.MailMessageBuilder createDefaultYaDeliveryMailMessageBuilder() throws MessagingException {
        return mailMessageBuilderService.getMailMessageBuilder(
                yaDeliveryMailConnection.getCode(),
                "/mail_message/logisticSupportEmail.eml"
        );
    }

    protected LogisticSupportTicket getSingleLogisticSupportTicket() {
        List<LogisticSupportTicket> tickets = dbService.list(Query.of(LogisticSupportTicket.FQN));
        Assertions.assertEquals(1, tickets.size());
        return tickets.get(0);
    }

    protected void assertTicketTypeAndService(LogisticSupportTicket ticket, Fqn fqn, String service) {
        Assertions.assertEquals(fqn, ticket.getFqn());
        Assertions.assertEquals(service, ticket.getService().getCode());
    }

    protected void assertTicketTypeAndService(Fqn fqn, String service) {
        LogisticSupportTicket ticket = getSingleLogisticSupportTicket();
        assertTicketTypeAndService(ticket, fqn, service);
    }

    private TicketCategory createDefaultTicketCategory(String brandCode) {
        Brand brand = dbService.getByNaturalId(Brand.FQN, Brand.CODE, brandCode);
        scriptContextVariablesService.addContextVariable(
                ScriptContextVariablesService.ContextVariables.CARD_OBJECT,
                brand
        );
        return bcpService.create(TicketCategory.FQN, Map.of(
                TicketCategory.CODE, TEST_CATEGORY_CODE,
                TicketCategory.TITLE, TEST_CATEGORY_TITLE
        ));
    }

    protected TicketCategory createTicketCategory(String title, String brandCode) {
        Brand brand = dbService.getByNaturalId(Brand.FQN, Brand.CODE, brandCode);
        scriptContextVariablesService.addContextVariable(
                ScriptContextVariablesService.ContextVariables.CARD_OBJECT,
                brand
        );
        return bcpService.create(TicketCategory.FQN, Map.of(
                TicketCategory.CODE, Randoms.string(),
                TicketCategory.TITLE, title
        ));
    }
}
