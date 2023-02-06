package ru.yandex.market.b2bcrm.module.ticket.test;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

import ru.yandex.market.b2bcrm.module.account.B2bContact;
import ru.yandex.market.b2bcrm.module.account.Shop;
import ru.yandex.market.b2bcrm.module.account.Supplier;
import ru.yandex.market.b2bcrm.module.ticket.B2bTicketRoutingRule;
import ru.yandex.market.b2bcrm.module.ticket.test.utils.B2bTicketTestUtils;
import ru.yandex.market.b2bcrm.module.utils.AccountModuleTestUtils;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.GidService;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.comment.test.impl.CommentTestUtils;
import ru.yandex.market.jmf.module.def.Contact;
import ru.yandex.market.jmf.module.def.test.impl.ModuleDefaultTestUtils;
import ru.yandex.market.jmf.module.mail.InMailMessage;
import ru.yandex.market.jmf.module.mail.impl.MailProcessingService;
import ru.yandex.market.jmf.module.mail.test.impl.MailMessageBuilderService;
import ru.yandex.market.jmf.module.mail.test.impl.MailTestUtils;
import ru.yandex.market.jmf.module.ou.impl.EmployeeTestUtils;
import ru.yandex.market.jmf.module.ou.impl.OuTestUtils;
import ru.yandex.market.jmf.module.relation.LinkedRelation;
import ru.yandex.market.jmf.module.ticket.Brand;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.TicketCategory;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.timings.ServiceTime;
import ru.yandex.market.jmf.timings.test.impl.ServiceTimeTestUtils;
import ru.yandex.market.ocrm.module.order.test.OrderTestUtils;

public class AbstractB2bMailProcessingCreationTest {

    @Inject
    protected BcpService bcpService;

    @Inject
    protected DbService dbService;

    @Inject
    protected MailTestUtils mailTestUtils;

    @Inject
    protected GidService gidService;

    @Inject
    protected ConfigurationService configurationService;

    @Inject
    protected ServiceTimeTestUtils serviceTimeTestUtils;

    @Inject
    protected TicketTestUtils ticketTestUtils;

    @Inject
    protected CommentTestUtils commentTestUtils;

    @Inject
    protected EmployeeTestUtils employeeTestUtils;

    @Inject
    protected B2bTicketTestUtils b2bTicketTestUtils;

    @Inject
    protected AccountModuleTestUtils accountModuleTestUtils;

    @Inject
    protected OuTestUtils ouTestUtils;

    @Inject
    protected ModuleDefaultTestUtils moduleDefaultTestUtils;

    @Inject
    protected MailMessageBuilderService mailMessageBuilderService;

    @Inject
    protected MailProcessingService mailProcessingService;

    @Inject
    protected OrderTestUtils orderTestUtils;

    protected Shop shop, shop2, shop3, shop4, shop5, shop6;
    protected Supplier supplier, supplier2, supplier3, topSupplier;
    protected B2bTicketRoutingRule b2bTicketRoutingRuleByHeaderAndMail, b2bTicketRoutingRuleByOnlyMail,
            b2bTicketArchivedRoutingRuleByOnlyMail, b2bTicketRoutingRuleByOnlyHeader,
            b2bTicketRoutingRuleByOnlyWrongMail, b2bTicketRoutingRuleByHeaderAndMailCaseInsensitive;
    protected Service defaultService, topGMVService;
    protected B2bContact contact, contact2, contact3, contact4, contact5;

    protected Ticket getSingleOpenedMarketTicket(Fqn fqn) {
        return ticketTestUtils.getSingleOpenedTicket(fqn);
    }

    protected void processMessage(InMailMessage mailMessage) {
        mailProcessingService.processInMessage(mailMessage);
    }

    public String createSubjectWithTicketNumber(Ticket ticket) {
        return Randoms.string() + ", № " + ticket.getId();
    }

    public String createSubjectWithTicketAndContactNumbers(Ticket ticket, Contact partner) {
        return Randoms.string() + ", № B2BCRM-" + ticket.getId() + "-P" + gidService.parse(partner.getGid()).getId();
    }

    public List<Comment> getComments(Ticket ticket) {
        return commentTestUtils.getComments(ticket)
                .stream()
                .sorted(Comparator.comparing(Comment::getGid))
                .collect(Collectors.toList());
    }

    public void assertLinkedRelation(Ticket source, Ticket target) {
        assertLinkedRelationCount(source, target, 1);
    }

    private void assertLinkedRelationCount(Ticket source, Ticket target, int count) {
        long actualCount = dbService.count(Query.of(LinkedRelation.FQN).withFilters(
                Filters.eq(LinkedRelation.SOURCE, source),
                Filters.eq(LinkedRelation.TARGET, target)
        ));
        Assertions.assertEquals(count, actualCount);
    }

    public void createB2bTicketRoutingRules() {
        ServiceTime serviceTime24x7 = serviceTimeTestUtils.createServiceTime24x7();
        defaultService = createService("defaultService", serviceTime24x7);
        topGMVService = createService("topGMVService", serviceTime24x7);
        b2bTicketRoutingRuleByHeaderAndMail = b2bTicketTestUtils.createRoutingRule(
                createService("code1", serviceTime24x7), "testreply1@mail.ru", "sklick"
        );
        b2bTicketRoutingRuleByOnlyHeader = b2bTicketTestUtils.createRoutingRule(
                "https://yandex.ru/support/partnermarket/troubleshooting/offers.html",
                createService("code2", serviceTime24x7)
        );
        b2bTicketRoutingRuleByOnlyMail = b2bTicketTestUtils.createRoutingRule(
                createService("code3", serviceTime24x7), "testmailforfoundedrule@mail.ru"
        );
        b2bTicketRoutingRuleByOnlyWrongMail = b2bTicketTestUtils.createRoutingRule(
                createService("code4", serviceTime24x7), "mailfortestingrulebyonlyheader@mail.ru"
        );
        b2bTicketArchivedRoutingRuleByOnlyMail = bcpService.edit(
                b2bTicketTestUtils.createRoutingRule(
                        createService("code5", serviceTime24x7), "testmailforarchivedrule@mail.ru"
                ),
                Map.of(B2bTicketRoutingRule.STATUS, "archived")
        );
        b2bTicketRoutingRuleByHeaderAndMailCaseInsensitive = b2bTicketTestUtils.createRoutingRule(
                createService("code6", serviceTime24x7), "TESTrepLY6@MAIL.RU", "AbC"
        );
    }

    protected Service createService(String code, Entity serviceTime) {
        // обычно очередь по умолчанию не является телефонной, поэтому надёжнее проверять на обычных
        return createService(Service.FQN_DEFAULT, code, serviceTime);
    }

    protected Service createService(Fqn fqn, String code, Entity serviceTime) {
        return ticketTestUtils.createService(fqn, Map.of(
                Service.DEFAULT_PRIORITY, 11L,
                Service.CODE, code,
                Service.SERVICE_TIME, serviceTime,
                Service.SUPPORT_TIME, serviceTime
        ));
    }

    public void createAccounts() {
        shop = accountModuleTestUtils.createShop("Test Shop", "111111", "21554398", "test1@ya.ru");
        shop2 = accountModuleTestUtils.createShop("Test Shop", "222222", "1070483", "test3@ya.ru", "test2@ya.ru");
        shop3 = accountModuleTestUtils.createShop("Test Shop", "333333", "21635249", "test3@ya.ru", "test2@ya.ru");
        shop4 = accountModuleTestUtils.createShop("Test Shop", "444", "21534666", "test4@ya.ru", "test4@ya.ru");
        shop5 = accountModuleTestUtils.createShop("Test Shop", "555", "21534666", "test5@ya.ru", "test5@ya.ru");
        shop6 = accountModuleTestUtils.createShop("Test Shop", "666", "1000567187", "test5@ya.ru", "test5@ya.ru");

        supplier = accountModuleTestUtils.createSupplier("Test supplier", "10188", false);
        supplier2 = accountModuleTestUtils.createSupplier("Test supplier2", "21534655", false);
        supplier3 = accountModuleTestUtils.createSupplier("Test supplier3", "21534655", false);
        topSupplier = accountModuleTestUtils.createSupplier("Test TOP supplier", "666666", true);
    }

    public void createContacts() {
        contact = accountModuleTestUtils.createMbiContactForAccount(shop, "SHOP_ADMIN", "Test Shop 1", "test3@ya.1");
        contact2 = accountModuleTestUtils.createMbiContactForAccount(shop2, "SHOP_ADMIN", "Test Shop 2", "test2@ya.1", "bz@ya.1", "bz2@ya.1");
        contact3 = accountModuleTestUtils.createMbiContactForAccount(shop3, "SHOP_ADMIN", "Test Shop 1", "test3@ya.3");
        contact4 = accountModuleTestUtils.createMbiContactForAccount(shop3, "SHOP_ADMIN", "Test Shop 1", "test3@ya.3", "test5@ya.5");
        contact5 = accountModuleTestUtils.createMbiContactForAccount(shop2, "SHOP_ADMIN", "Test Shop 5", "test5@ya.5");
        accountModuleTestUtils.createMbiRelation(shop, contact2, "SHOP_ADMIN");
    }

    @NotNull
    protected TicketCategory createCategory(Brand brand) {
        return ticketTestUtils.createTicketCategory("test", brand);
    }

    protected void processTypicalEmailMessage(Map<String, List<String>> header, String connection, String email) {
        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder =
                getMailMessageBuilder(header, connection, email);
        processMessage(mailMessageBuilder.build());
    }

    @NotNull
    protected MailMessageBuilderService.MailMessageBuilder getMailMessageBuilder(Map<String, List<String>> header,
                                                                                 String mailConnectionBlue,
                                                                                 String email) {
        return mailMessageBuilderService.getMailMessageBuilder(mailConnectionBlue)
                .setFrom(email)
                .setHeader(header);
    }

}
