package ru.yandex.market.b2bcrm.module.ticket.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jdk.jfr.Description;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.b2bcrm.module.account.Supplier;
import ru.yandex.market.b2bcrm.module.account.Vendor;
import ru.yandex.market.b2bcrm.module.ticket.B2bLeadTicket;
import ru.yandex.market.b2bcrm.module.ticket.B2bTicket;
import ru.yandex.market.b2bcrm.module.ticket.test.config.B2bTicketTests;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.catalog.items.CatalogItem;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.entity.test.assertions.EntityAssert;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.comment.UserComment;
import ru.yandex.market.jmf.module.def.Contact;
import ru.yandex.market.jmf.module.mail.InMailMessage;
import ru.yandex.market.jmf.module.mail.test.impl.MailMessageBuilderService;
import ru.yandex.market.jmf.module.ticket.Brand;
import ru.yandex.market.jmf.module.ticket.MailConnection;
import ru.yandex.market.jmf.module.ticket.Team;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.TicketContactInComment;
import ru.yandex.market.jmf.timings.ServiceTime;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.ocrm.module.order.domain.Order;

import static ru.yandex.market.jmf.entity.test.assertions.EntityCollectionAssert.assertThat;

@B2bTicketTests
public class B2bMailProcessingCreationTest extends AbstractB2bMailProcessingCreationTest {

    protected static final String MAIL_CONNECTION = "b2b";
    protected static final String MAIL_CONNECTION_BLUE = "b2bBlue";
    protected static final String WRONG_MAIL_CONNECTION = "wrong";

    private static Stream<String> attributeForTestCreateEmailWithSupplierIdHeader() {
        return Stream.of(
                "316205",
                null
        );
    }

    @BeforeEach
    public void prepareData() {
        createAccounts();
        createContacts();
        createB2bTicketRoutingRules();

        mailTestUtils.createMailConnection(MAIL_CONNECTION, Map.of(MailConnection.DEFAULT_SERVICE, defaultService));
        mailTestUtils.createMailConnection(MAIL_CONNECTION_BLUE, Map.of(MailConnection.DEFAULT_SERVICE,
                defaultService));
        mailTestUtils.createMailConnection(WRONG_MAIL_CONNECTION);

        Entity st = dbService.getByNaturalId(ServiceTime.FQN, CatalogItem.CODE, "b2b_9_21");
        serviceTimeTestUtils.createPeriod(st, "monday", "09:00", "21:00");

        Team team = ticketTestUtils.createTeam("firstLineMail");
        ServiceTime serviceTime24x7 = serviceTimeTestUtils.createServiceTime24x7();
        Brand brand = ticketTestUtils.createBrand();

        ticketTestUtils.createService(team, serviceTime24x7, brand, Optional.of("marketApiAffiliate"));
    }

    @Test
    public void testCreationBlueTicketWithShopIdHeader() {
        Map<String, List<String>> header = Maps.of(
                "x-market-shopid", List.of("111111")
        );

        processTypicalEmailMessage(header, MAIL_CONNECTION_BLUE, Randoms.email());

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);
        List<Comment> comments = getComments(ticket);

        Assertions.assertEquals(shop.getGid(), ticket.getPartner().getGid());
        Assertions.assertEquals(1, comments.size());
        Assertions.assertEquals(ticket.getService().getCode(), "defaultService");
        Assertions.assertNull(ticket.getResponsibleEmployee());
    }

    @Test
    public void testShouldNotCreateTicket() {
        processTypicalEmailMessage(Map.of(), WRONG_MAIL_CONNECTION, Randoms.email());

        List<Entity> tickets = dbService.list(Query.of(B2bTicket.FQN));
        Assertions.assertTrue(tickets.isEmpty());
    }

    @Test
    @Description("Есть два магазина один совпадает по shop id второй по campaign Id, найтись должен по shop id")
    public void testCreationTicketWithShopIdHeaderAndMarketCampaignNumber() {
        Map<String, List<String>> header = Maps.of(
                "x-market-shopid", List.of("111111"),
                "x-marketcampaignnumber", List.of("11-1070483")
        );

        processTypicalEmailMessage(header, MAIL_CONNECTION_BLUE, Randoms.email());

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);
        Assertions.assertEquals(shop.getGid(), ticket.getPartner().getGid());
    }

    @Test
    @Description("Должен подставится магазин по campaign Id")
    public void testCreationTicketWithShopMarketCampaignNumber() {
        Map<String, List<String>> header = Maps.of(
                "x-marketcampaignnumber", List.of("=?utf-8?q?=E2=84=96_11-21635249?=")
        );

        processTypicalEmailMessage(header, MAIL_CONNECTION_BLUE, Randoms.email());

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);
        Assertions.assertEquals(shop3.getGid(), ticket.getPartner().getGid());
    }

    @Test
    @Description("Должен подставится магазин по campaign Id, когда заголовок передается с нижним подчеркиванием")
    public void testCreationTicketWithShopMarketCampaignNumberWith_() {
        Map<String, List<String>> header = Maps.of(
                "x-marketcampaignnumber", List.of("96_1000567187")
        );

        processTypicalEmailMessage(header, MAIL_CONNECTION_BLUE, Randoms.email());

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);
        Assertions.assertEquals(shop6.getGid(), ticket.getPartner().getGid());
    }

    @Test
    @Description("Должен подставится поставщик по campaign Id")
    public void testCreationTicketWithSupplierMarketCampaignNumber() {
        Map<String, List<String>> header = Maps.of(
                "x-marketcampaignnumber", List.of("10188")
        );

        extracted(header);

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);
        Assertions.assertEquals(supplier.getGid(), ticket.getPartner().getGid());
    }

    private void extracted(Map<String, List<String>> header) {
        processTypicalEmailMessage(header, MAIL_CONNECTION_BLUE, Randoms.email());
    }

    @Test
    @Description("Должен подставится поставщик по campaign Id и так как партнер топ по GMV тикет будет с очередью для" +
            " топ партнеров")
    public void testCreationTicketWithTopSupplier() {
        configurationService.setValue("routingMailServiceForTopGMVPartners", topGMVService);

        Map<String, List<String>> header = Maps.of(
                "x-marketcampaignnumber", List.of("666666")
        );

        processTypicalEmailMessage(header, MAIL_CONNECTION_BLUE, Randoms.email());

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);
        Assertions.assertEquals(topSupplier.getGid(), ticket.getPartner().getGid());
        Assertions.assertEquals(topGMVService, ticket.getService());
    }

    @Test
    @Description("Партнер должен быть пустым, потому что мы найдем два магазиана с одним campaign id")
    public void testCreationTicketWithSameMarketCampaignNumberShopShouldBeNull() {
        Map<String, List<String>> header = Maps.of(
                "x-marketcampaignnumber", List.of("21534666")
        );

        processTypicalEmailMessage(header, MAIL_CONNECTION_BLUE, Randoms.email());

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);
        Assertions.assertNull(ticket.getPartner());
    }

    @Test
    @Description("Партнер должен быть пустым, потому что мы найдем двух поставщиков с одним campaign id")
    public void testCreationTicketWithSameMarketCampaignNumberSupplierShouldBeNull() {
        Map<String, List<String>> header = Maps.of(
                "x-marketcampaignnumber", List.of("21534655")
        );

        processTypicalEmailMessage(header, MAIL_CONNECTION_BLUE, Randoms.email());

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);
        Assertions.assertNull(ticket.getPartner());
    }

    @Test
    @Description("Партнер должен быть пустым")
    public void testCreationTicketWithMarketCampaignNumberPartner() {
        Map<String, List<String>> header = Maps.of(
                "x-marketcampaignnumber", List.of("434343")
        );

        processTypicalEmailMessage(header, MAIL_CONNECTION_BLUE, Randoms.email());

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);
        Assertions.assertNull(ticket.getPartner());
    }

    @Test
    public void testCreationWithShopIdHeader() {
        Map<String, List<String>> header = Maps.of(
                "x-market-shopid", List.of("111111")
        );

        processTypicalEmailMessage(header, MAIL_CONNECTION, Randoms.email());

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);
        List<Comment> comments = getComments(ticket);

        Assertions.assertEquals(shop.getGid(), ticket.getPartner().getGid());
        Assertions.assertEquals(1, comments.size());
    }

    @Test
    public void testCreationWithWrongShopIdHeader() {
        Map<String, List<String>> header = Maps.of(
                "x-market-shopid", List.of("111111asdaa")
        );

        processTypicalEmailMessage(header, MAIL_CONNECTION, Randoms.email());

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);
        List<Comment> comments = getComments(ticket);

        Assertions.assertNull(ticket.getPartner());
        Assertions.assertEquals(1, comments.size());
    }

    @Test
    @Description("Нашлось правило соответсвующее и заголовкам и почте одновременно")
    public void testCreationWithFoundedRuleByHeaderAndMail() {
        Map<String, List<String>> header = Maps.of(
                "x-otrs-partnerform", List.of("sklick")
        );

        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder = getMessageBuilder()
                .setTo("testreply1@mail.ru")
                .setHeader(header);
        processMessage(mailMessageBuilder.build());

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);

        Assertions.assertEquals(b2bTicketRoutingRuleByHeaderAndMail.getAttribute("service"), ticket.getService());
    }

    @Test
    @Description("Найдено правило по почте и заголовку вне зависимости от регистра")
    public void testCreationWithFoundedRuleByHeaderAndMailCaseInsensitive() {
        Map<String, List<String>> header = Maps.of("x-otrs-partnerform", List.of("sklIck"));

        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder = getMessageBuilder()
                .setTo("testReply1@mail.ru")
                .setHeader(header);
        processMessage(mailMessageBuilder.build());

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);

        Assertions.assertEquals(b2bTicketRoutingRuleByHeaderAndMail.getAttribute("service"), ticket.getService());
    }

    @Test
    @Description("Найдено правило по почте и заголовку вне зависимости от регистра, правило не в нижнем регистре")
    public void testCreationWithFoundedRuleByHeaderAndMailWithRuleCaseInsensitive() {
        Map<String, List<String>> header = Maps.of("x-otrs-partnerform", List.of("abc"));

        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder = getMessageBuilder()
                .setTo("testReply6@mail.ru")
                .setHeader(header);
        processMessage(mailMessageBuilder.build());

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);

        Assertions.assertEquals(b2bTicketRoutingRuleByHeaderAndMailCaseInsensitive.getAttribute("service"),
                ticket.getService());
    }

    @Test
    @Description("Нашлось одно правило соответсвующее заголовку и одно соответствующее почте, должно подстваится " +
            "правило найденное по заголовку")
    public void testCreationWithRuleByHeader() {
        Map<String, List<String>> header = Maps.of(
                "x-otrs-fromfeedback", List.of("https://yandex.ru/support/partnermarket/troubleshooting/offers.html")
        );

        processTypicalEmailMessage(header, MAIL_CONNECTION, "mailfortestingrulebyonlyheader@mail.ru");

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);

        Assertions.assertEquals(b2bTicketRoutingRuleByOnlyHeader.getAttribute("service"), ticket.getService());
    }

    @Test
    @Description("Нашлось одно правило соответсвующее заголовку и одно соответствующее почте, должно подстваится " +
            "правило найденное по заголовку вне зависимости от регистра")
    public void testCreationWithRuleByHeaderCaseInsensitive() {
        Map<String, List<String>> header = Maps.of(
                "x-otrs-fromfeedback", List.of("https://yandex.ru/support/partnermaRket/troubleshooting/offers.html")
        );

        processTypicalEmailMessage(header, MAIL_CONNECTION, "mailfortestingRulebyonlyheader@mail.ru");

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);

        Assertions.assertEquals(b2bTicketRoutingRuleByOnlyHeader.getAttribute("service"), ticket.getService());
    }

    @Test
    @Description("Когда не найдено правило ни по заголовку ни по почте, используется дефолтное правило")
    public void testCreationWithDefaultRoutingMailServiceForB2bTicket() {
        Map<String, List<String>> header = Maps.of(
                "x-otrs-fromfeedback", List.of("https://fake.html")
        );

        processTypicalEmailMessage(header, MAIL_CONNECTION, "fake2@mail.ru");

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);

        Assertions.assertEquals(defaultService, ticket.getService());
    }

    @Test
    @Description("Найдено правило по почте, когда есть заголовки но по ним нет правила")
    public void testCreationWithFoundedRuleByMail() {
        Map<String, List<String>> header = Maps.of(
                "x-otrs-fromfeedback", List.of("https://uselessurl.html")
        );

        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder =
                getMailMessageBuilder(header, MAIL_CONNECTION, Randoms.email())
                        .setTo("testmailforfoundedrule@mail.ru");
        processMessage(mailMessageBuilder.build());

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);

        Assertions.assertEquals(b2bTicketRoutingRuleByOnlyMail.getAttribute("service"), ticket.getService());
    }

    @Test
    @Description("Найдено правило по почте, когда есть заголовки, но по ним нет правила вне зависимости от регистра")
    public void testCreationWithFoundedRuleByMailCaseInsensitive() {
        Map<String, List<String>> header = Maps.of(
                "x-otrs-fromfeedback", List.of("https://uselessurl.html")
        );

        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder =
                getMailMessageBuilder(header, MAIL_CONNECTION, Randoms.email())
                        .setTo("testmaiLFORFOUNDEDRULE@mail.ru");
        processMessage(mailMessageBuilder.build());

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);

        Assertions.assertEquals(b2bTicketRoutingRuleByOnlyMail.getAttribute("service"), ticket.getService());
    }

    @Test
    @Description("Найдено правило по почте, когда заголовки не указаны")
    public void testCreationWithoutHeader() {
        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder =
                getMailMessageBuilder(new HashMap<>(), MAIL_CONNECTION, Randoms.email())
                        .setTo("testmailforfoundedrule@mail.ru");
        processMessage(mailMessageBuilder.build());

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);

        Assertions.assertEquals(b2bTicketRoutingRuleByOnlyMail.getAttribute("service"), ticket.getService());
    }

    @Test
    @Description("Найдено правило по почте, когда заголовки не указаны вне зависимости от регистра")
    public void testCreationWithoutHeaderCaseInsensitive() {
        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder =
                getMailMessageBuilder(new HashMap<>(), MAIL_CONNECTION, Randoms.email())
                        .setTo("testmailForfoundedrule@mail.ru");
        processMessage(mailMessageBuilder.build());

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);

        Assertions.assertEquals(b2bTicketRoutingRuleByOnlyMail.getAttribute("service"), ticket.getService());
    }

    @Test
    @Description("Найдено правило по почте, но оно архивное")
    public void testCreationWithArchivedRule() {
        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder =
                getMailMessageBuilder(new HashMap<>(), MAIL_CONNECTION, Randoms.email())
                        .setTo("testmailforarchivedrule@mail.ru");
        processMessage(mailMessageBuilder.build());

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);

        Assertions.assertEquals(defaultService, ticket.getService());
    }

    @Test
    public void testCreationWithoutHeaders() {
        processTypicalEmailMessage(new HashMap<>(), MAIL_CONNECTION, "test3@ya.1");

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);
        List<Comment> comments = getComments(ticket);

        Assertions.assertEquals(shop.getGid(), ticket.getPartner().getGid());
        Assertions.assertEquals(1, comments.size());
    }

    @Test
    public void testCreationWithoutShopIdHeader() {
        Map<String, List<String>> header = Maps.of(
                "x-market-test", List.of("333333")
        );

        processTypicalEmailMessage(header, MAIL_CONNECTION, "test3@ya.1");

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);
        List<Comment> comments = getComments(ticket);

        Assertions.assertEquals(shop.getGid(), ticket.getPartner().getGid());
        Assertions.assertEquals(1, comments.size());
    }

    private MailMessageBuilderService.MailMessageBuilder getMessageBuilder() {
        return mailMessageBuilderService.getMailMessageBuilder(MAIL_CONNECTION);
    }

    @Test
    public void testPartnerEmail() {
        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder =
                getMailMessageBuilder(new HashMap<>(), MAIL_CONNECTION, Randoms.email());
        processMessage(mailMessageBuilder.build());

        var ticket = getSingleOpenedMarketTicket(B2bTicket.FQN);

        Contact contact = moduleDefaultTestUtils.createContact(ticket, List.of(Randoms.email()));

        String responseBody = Randoms.string();
        mailMessageBuilder
                .newDeduplicationKey()
                .setBody(responseBody)
                .setSubject(createSubjectWithTicketAndContactNumbers(ticket, contact));
        processMessage(mailMessageBuilder.build());

        List<Comment> comments = getComments(ticket);
        Assertions.assertEquals(2, comments.size());

        Assertions.assertEquals(TicketContactInComment.FQN, comments.get(1).getFqn());
        Assertions.assertEquals(responseBody, comments.get(1).getBody());
    }

    @Test
    public void testClientEmailOnClosedTicket() {
        String sender = Randoms.email();
        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder =
                getMailMessageBuilder(new HashMap<>(), MAIL_CONNECTION, sender);
        processMessage(mailMessageBuilder.build());

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);
        ticketTestUtils.editTicketStatus(ticket, Ticket.STATUS_CLOSED);

        String responseBody = Randoms.string();
        mailMessageBuilder
                .newDeduplicationKey()
                .setBody(responseBody)
                .setFrom(sender)
                .setSubject(createSubjectWithTicketNumber(ticket));

        processMessage(mailMessageBuilder.build());
        var newTicket = getSingleOpenedMarketTicket(B2bTicket.FQN);
        assertLinkedRelation(newTicket, ticket);

        List<Comment> comments = getComments(newTicket);
        Assertions.assertEquals(1, comments.size());

        Assertions.assertEquals(UserComment.FQN, comments.get(0).getFqn());
        Assertions.assertEquals(responseBody, comments.get(0).getBody());
    }

    @Test
    public void testIfClientEmailNotEqualsOnClosedTicket() {
        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder =
                getMailMessageBuilder(new HashMap<>(), MAIL_CONNECTION, Randoms.email());
        processMessage(mailMessageBuilder.build());

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);
        ticketTestUtils.editTicketStatus(ticket, Ticket.STATUS_CLOSED);

        String responseBody = Randoms.string();
        mailMessageBuilder
                .newDeduplicationKey()
                .setBody(responseBody)
                .setFrom(Randoms.email())
                .setSubject(createSubjectWithTicketNumber(ticket));

        processMessage(mailMessageBuilder.build());
        var newTicket = getSingleOpenedMarketTicket(B2bTicket.FQN);
        assertLinkedRelation(newTicket, ticket);

        List<Comment> comments = getComments(newTicket);
        Assertions.assertEquals(1, comments.size());

        Assertions.assertEquals(UserComment.FQN, comments.get(0).getFqn());
        Assertions.assertEquals(responseBody, comments.get(0).getBody());
    }

    @ParameterizedTest
    @Description("""
            Разбор письма не падает, если в письме пришел заголовок x-market-supplierid, но не удалось определить партнера
            тест-кейс:
            - https://testpalm2.yandex-team.ru/testcase/ocrm-1061
            - https://testpalm2.yandex-team.ru/testcase/ocrm-1062
            """)
    @MethodSource("attributeForTestCreateEmailWithSupplierIdHeader")
    public void testCreateEmailWithSupplierIdHeader(String supplierId) {
        Map<String, List<String>> header = new HashMap<>();
        Supplier supplier = null;

        if (supplierId != null) {
            supplier = bcpService.create(Fqn.of("account$supplier"), Maps.of(
                    "title", "МультиСвязь",
                    "supplierId", supplierId
            ));
            header = Maps.of(
                    "x-market-supplierid", List.of(supplierId)
            );
        }

        processTypicalEmailMessage(header, MAIL_CONNECTION, "test3@ya.ru");

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);

        Assertions.assertEquals(supplier, ticket.getPartner());
    }

    @Test
    public void testCreateEmailWithVendorIdHeader() {
        Vendor vendor = bcpService.create(Fqn.of("account$vendor"), Map.of(
                "title", "XPERT.RU",
                "vendorId", "402585"
        ));
        Map<String, List<String>> header = Maps.of(
                "x-market-vendorid", List.of("402585")
        );

        processTypicalEmailMessage(header, MAIL_CONNECTION, "test3@ya.ru");

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);

        Assertions.assertEquals(vendor.getGid(), ticket.getPartner().getGid());
    }

    @Test
    public void testLinkPartnerByContact() {
        Map<String, List<String>> header = Maps.of(
                "x-market-vendorid", List.of("402581")
        );

        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder =
                getMailMessageBuilder(header, MAIL_CONNECTION, "test3@ya.1");
        processMessage(mailMessageBuilder.build());

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);
        Assertions.assertEquals(shop.getGid(), ticket.getPartner().getGid());

        ticketTestUtils.editTicketStatus(ticket, Ticket.STATUS_CLOSED);

        mailMessageBuilder
                .newDeduplicationKey()
                .setBody(Randoms.string())
                .setFrom("test2@ya.1")
                .setSubject(createSubjectWithTicketNumber(ticket));

        processMessage(mailMessageBuilder.build());

        ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);
        Assertions.assertNull(ticket.getPartner());
    }

    @Test
    public void testCreationWithoutHeaders2() {
        processTypicalEmailMessage(new HashMap<>(), MAIL_CONNECTION, "test3@ya.3");

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);
        List<Comment> comments = getComments(ticket);

        Assertions.assertEquals(shop3.getGid(), ticket.getPartner().getGid());
        Assertions.assertEquals(1, comments.size());
    }

    @Test
    public void testCreateEmailWithPartnerIdHeader() {
        Supplier supplier = bcpService.create(Fqn.of("account$supplier"), Map.of(
                "title", "МультиСвязь",
                "supplierId", "316205"
        ));

        Map<String, List<String>> header = Maps.of(
                "x-market-partnerid", List.of("316205")
        );

        processTypicalEmailMessage(header, MAIL_CONNECTION, "test3@ya.ru");

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);

        Assertions.assertEquals(supplier.getGid(), ticket.getPartner().getGid());
    }

    @Test
    @Description("""
                Для B2B обращений не должно быть дедупликации по email
                https://testpalm.yandex-team.ru/testcase/ocrm-1361
            """)
    public void testMultipleMessagesNotJoinIntoOneTicketByEmail() {
        int numberOfMessages = 3;
        String clientEmail = Randoms.email();
        Stream.generate(() -> getMessageBuilder().setFrom(clientEmail).build())
                .limit(numberOfMessages)
                .forEach(this::processMessage);
        assertThat(ticketTestUtils.getAllActiveTickets(B2bTicket.FQN))
                .hasSize(numberOfMessages)
                .allHasAttributes(Ticket.CLIENT_EMAIL, clientEmail);
    }

    @Test
    @Description("""
                Если пришло письмо от email из справочника
                'Отправители, обращения от имени которых, содержат приватные данные'
                то будет создано обращение с флагом 'Содержит приватные данные'
                https://testpalm.yandex-team.ru/testcase/ocrm-1367
            """)
    public void testCreateTicketWithPrivateData() {
        String email = Randoms.email();
        bcpService.create(Fqn.of("b2bTicketPrivateDataSenders"), Maps.of(
                CatalogItem.CODE, email,
                CatalogItem.TITLE, Randoms.string()
        ));
        processMessage(getMessageBuilder().setFrom(email).build());
        EntityAssert.assertThat(getSingleOpenedMarketTicket(B2bTicket.FQN))
                .hasAttributes(
                        B2bTicket.CONTAINS_PRIVATE_DATA, true,
                        B2bTicket.CLIENT_EMAIL, email
                );
    }

    @Test
    @Description("""
            Во входящем B2B обращении 'тема письма' равна теме входящего письма
            https://testpalm.yandex-team.ru/testcase/ocrm-1513
            """)
    public void b2bIngoingTicketMailSubjectTest() {
        processMessage(getMessageBuilder().setSubject("Mail subject").build());
        EntityAssert.assertThat(getSingleOpenedMarketTicket(B2bTicket.FQN))
                .hasAttributes(
                        B2bTicket.TITLE, "Mail subject",
                        B2bTicket.MAIL_SUBJECT, "Mail subject"
                );
    }

    @Test
    @Description("""
            Проверка, что невалидный e-mail сохраняется as is в поле "Контактный e-mail"
            Проверка, что в этом случае у тикета есть внутренний комментарий о том, что e-mail не валиден
            https://testpalm.yandex-team.ru/testcase/ocrm-1363
            """)
    public void createTicketWithInvalidEmail() {
        Map<String, List<String>> header = Maps.of(
                "x-market-shopid", List.of("111111")
        );

        String invalidEmail = "анастасия";

        processTypicalEmailMessage(header, MAIL_CONNECTION_BLUE, invalidEmail);

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);

        //Ищем комментарий о том, что email не валиден
        Comment internalComment = getComments(ticket).stream()
                .filter(comment -> comment.getFqn().equals(Fqn.of("comment$internal")))
                .filter(internal -> internal.getTextBody().contains("указан некорректный e-mail"))
                .collect(Collectors.toList())
                .get(0);

        Assertions.assertEquals(invalidEmail, ticket.getClientEmail(), "E-mail должен быть сохранен as is, " +
                "даже если он не валидный");
        Assertions.assertNotNull(internalComment);
    }

    @Test
    @Description("""
            Отключение дедупликации для конкретного адреса
            тест-кейс https://testpalm2.yandex-team.ru/testcase/ocrm-1306
            """)
    void doNotDeduplicateForSTAddress() {
        for (int i = 0; i < 2; i++) {
            InMailMessage message = mailMessageBuilderService.getMailMessageBuilder(MAIL_CONNECTION)
                    .setFrom("st@yandex-team.ru")
                    .build();
            processMessage(message);
        }
        long ticketCount = dbService.count(Query.of(Ticket.FQN));
        Assertions.assertEquals(2, ticketCount);
    }

    @Test
    @Description("""
            Сообщения от ФОС, replyTo = @yandex-team.ru не показываются в ПИ и являются внутренними комментариями
            https://testpalm.yandex-team.ru/testcase/ocrm-1579
            """)
    public void hideTicketInPartnerInterfaceFromRobotFormsReplyToYandexTeam() {
        InMailMessage message = mailMessageBuilderService.getMailMessageBuilder(MAIL_CONNECTION)
                .setFrom("robot-forms@yandex-team.ru")
                .setReplyToList("test@yandex-team.ru")
                .build();
        processMessage(message);

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);
        Assertions.assertTrue(((Boolean) ticket.getAttribute("hideInPartnerInterface")));

        List<Comment> comments = getComments(ticket);
        assertThat(comments)
                .allSatisfy(comment -> TicketContactInComment.class.isAssignableFrom(comment.getClass()));
    }

    @Test
    @Description("""
            Сообщения c replyTo = @yandex-team.ru не показываются в ПИ и являются внутренними комментариями
            https://testpalm.yandex-team.ru/testcase/ocrm-1580
            """)
    public void hideTicketInPartnerInterfaceReplyToYandexTeam() {
        InMailMessage message = mailMessageBuilderService.getMailMessageBuilder(MAIL_CONNECTION)
                .setFrom("test@yandex-team.ru")
                .setReplyToList("test@yandex-team.ru")
                .build();
        processMessage(message);

        B2bTicket ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);
        Assertions.assertTrue(((Boolean) ticket.getAttribute("hideInPartnerInterface")));

        List<Comment> comments = getComments(ticket);
        assertThat(comments)
                .allSatisfy(comment -> TicketContactInComment.class.isAssignableFrom(comment.getClass()));
    }
    @Test
    @Description("""
            Существующее B2B обращение переоткрываем из основных статусов
            """)
    public void reopenExistedTicketWithOrdinaryStatus() {
        B2bTicket ticket = ticketTestUtils.createTicket(B2bTicket.FQN, Map.of(
                B2bTicket.STATUS, B2bTicket.STATUS_CLOSED_WAIT_CONTIGUOUS,
                B2bTicket.CATEGORIES, List.of(createCategory(ticketTestUtils.createBrand())),
                B2bTicket.PARTNER, shop));

        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder =
                getMailMessageBuilder(new HashMap<>(), MAIL_CONNECTION, "test3@ya.1");

        mailMessageBuilder
                .newDeduplicationKey()
                .setBody(Randoms.string())
                .setFrom("test2@ya.1")
                .setSubject(createSubjectWithTicketNumber(ticket));

        processMessage(mailMessageBuilder.build());

        ticket = (B2bTicket) getSingleOpenedMarketTicket(B2bTicket.FQN);
        Assertions.assertEquals(B2bTicket.STATUS_REOPENED, ticket.getStatus());
    }

    @Test
    @Description("""
            Существующие B2BLead обращения должны переоткрываться в том числе из дополнительных статусов
            """)
    public void reopenExistedTicketWithSpecialStatus() {
        B2bLeadTicket ticket = b2bTicketTestUtils.createB2bLead(Map.of(
                B2bTicket.STATUS, B2bTicket.STATUS_RESOLVED,
                B2bTicket.CATEGORIES, List.of(createCategory(ticketTestUtils.createBrand())),
                B2bTicket.PARTNER, shop
        ));

        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder =
                getMailMessageBuilder(new HashMap<>(), MAIL_CONNECTION, "test3@ya.1");

        mailMessageBuilder
                .newDeduplicationKey()
                .setBody(Randoms.string())
                .setFrom("test2@ya.1")
                .setSubject(createSubjectWithTicketNumber(ticket));

        processMessage(mailMessageBuilder.build());

        ticket = (B2bLeadTicket) getSingleOpenedMarketTicket(B2bLeadTicket.FQN);
        Assertions.assertEquals(B2bTicket.STATUS_REOPENED, ticket.getStatus());
    }

    @Test
    @Description("""
            Связывание письма (без номера в теме) с обращением: по заголовку References
            Для тикета созданного из входящего письма
            https://testpalm.yandex-team.ru/testcase/ocrm-1567
            """)
    public void testDeduplicationByReferencesForTicketCreatedViaMail() {
        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder = getMailMessageBuilder(
                new HashMap<>(), MAIL_CONNECTION, "test3@ya.1"
        ).setBody("Первое письмо (создаст тикет)");

        InMailMessage message = mailMessageBuilder.build();
        processMessage(message);

        Ticket ticket = getSingleOpenedMarketTicket(B2bTicket.FQN);

        assertThat(commentTestUtils.getComments(ticket))
                .hasSize(1)
                .allHasAttributes(
                        UserComment.USER_EMAIL, "test3@ya.1",
                        UserComment.BODY, "Первое письмо (создаст тикет)"
                );

        mailMessageBuilder
                .newDeduplicationKey()
                .setReferences("otherMessage1 otherMessage2 " + message.getMessageId())
                .setBody("Второе письмо (прикрепится к созданному тикету)")
                .setSubject("Тема не содержит номер тикета");
        processMessage(mailMessageBuilder.build());

        assertThat(commentTestUtils.getComments(ticket))
                .hasSize(2)
                .anyHasAttributes(
                        UserComment.USER_EMAIL, "test3@ya.1",
                        UserComment.BODY, "Первое письмо (создаст тикет)"
                )
                .anyHasAttributes(
                        UserComment.USER_EMAIL, "test3@ya.1",
                        UserComment.BODY, "Второе письмо (прикрепится к созданному тикету)"
                );
    }

    @Test
    @Description("""
            Связывание письма (без номера в теме) с обращением по заголовку References
            Для тикета созданного вручную или импортом
            https://testpalm.yandex-team.ru/testcase/ocrm-1568
            https://testpalm.yandex-team.ru/testcase/ocrm-1569
            """)
    public void testDeduplicationByReferencesForTicketCreatedManually() {
        B2bTicket ticket = ticketTestUtils.createTicket(B2bTicket.FQN, Map.of());
        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder =
                getMailMessageBuilder(new HashMap<>(), MAIL_CONNECTION, "test3@ya.1")
                        .setReferences("otherMessage1 otherMessage2 " + ticket.getReplyMessage())
                        .setBody("Текст письма")
                        .setSubject("Тема не содержит номер тикета");

        processMessage(mailMessageBuilder.build());

        assertThat(commentTestUtils.getComments(ticket))
                .hasSize(1)
                .allHasAttributes(
                        UserComment.USER_EMAIL, "test3@ya.1",
                        UserComment.BODY, "Текст письма"
                );
    }

    @Test
    public void testGetOrderFromEmail() {
        Long orderId = 11989056L;
        orderTestUtils.createOrder(Map.of(Order.NUMBER, orderId));

        String body = "Номер заказа:\n" +
                "11989056\n" +
                "\n" +
                "Номер телефона клиента:\n" +
                "+7 900 000-00-00\n";
        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder =
                getMailMessageBuilder(new HashMap<>(), MAIL_CONNECTION, Randoms.email());
        processMessage(mailMessageBuilder.setBody(body).build());

        var ticket = (B2bTicket)getSingleOpenedMarketTicket(B2bTicket.FQN);

        Assertions.assertNotNull(ticket.getOrder());
        Assertions.assertEquals(orderId, ticket.getOrder().getOrderId());
    }
    @Test
    @Description("""
            При поступлении письма B2B Лид должен помечаться как непрочитанный
            https://testpalm.yandex-team.ru/testcase/ocrm-1398
            """)
    public void markUnreadExistedTicket() {
        B2bLeadTicket ticket = b2bTicketTestUtils.createB2bLead(Map.of(B2bLeadTicket.IS_READ, true));

        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder = getMailMessageBuilder(
                new HashMap<>(), MAIL_CONNECTION, "test3@ya.1"
        ).setSubject(createSubjectWithTicketNumber(ticket));

        processMessage(mailMessageBuilder.build());

        ticket = (B2bLeadTicket) getSingleOpenedMarketTicket(B2bLeadTicket.FQN);
        Assertions.assertFalse(ticket.isRead());
    }

}
