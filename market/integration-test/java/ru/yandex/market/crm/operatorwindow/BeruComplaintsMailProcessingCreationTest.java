package ru.yandex.market.crm.operatorwindow;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jdk.jfr.Description;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.operatorwindow.jmf.entity.BeruComplaintsServiceRule;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.ticket.Brand;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.TicketCategory;
import ru.yandex.market.jmf.module.ticket.TicketTag;
import ru.yandex.market.jmf.script.ScriptContextVariablesService;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.ocrm.module.complaints.BeruComplaintsTicket;
import ru.yandex.market.ocrm.module.order.domain.Order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Transactional
public class BeruComplaintsMailProcessingCreationTest extends AbstractBeruComplaintsMailProcessingTest {

    private static final String VIP_MARKER = "vip";
    private static final String THREATENS_COURT_MARKER = "threatensCourt";
    private static final String YANDEX_TEAM_MARKER = "yandexTeam";

    private static final String TEST_SUBJECT_1 = Randoms.string();
    private static final String TEST_SUBJECT_2 = Randoms.string();
    private static final String TEST_CATEGORY_1 = "beruComplaintsTest1";
    private static final String TEST_CATEGORY_2 = "beruComplaintsTest2";
    private static final String TEST_CATEGORY_3 = "beruComplaintsTest3";
    private static final String TEST_CATEGORY_4 = "beruComplaintsTest4";
    private static final String TEST_CATEGORY_TITLE_1 = Randoms.string();
    private static final String TEST_CATEGORY_TITLE_2 = Randoms.string();
    private static final String TEST_CATEGORY_TITLE_3 = Randoms.string();
    private static final String TEST_CATEGORY_TITLE_4 = Randoms.string();
    private static final String TEST_SERVICE_1 = Randoms.string();
    private static final String TEST_SERVICE_2 = Randoms.string();
    private static final String DEFAULT_SERVICE = "beruComplaintsIncoming";
    private static final String UNMARKED_SERVICE = "beruComplaintsUnmarked";
    private static final String GR_CREDITS_SERVICE = "gr_credits";
    private static final String MARKET_COMPLAINTS_CLAIMS_SERVICE = "marketComplaintsClaims";
    private static final String VIP_SERVICE = "beruComplaintsVip";
    private static final Long DEFAULT_SERVICE_PRIORITY = 40L;
    private static final Long MAX_SERVICE_PRIORITY = 99L; // может поменяться, сейчас - 99
    private static final Long VIP_SERVICE_PRIORITY = 70L;
    private static final Long TEST_DEFAULT_PRIORITY = 11L;
    private static final Long HIGH_PRIORITY = 80L;

    private static String createBodyByCategory(String parentCategory, String childCategory) {
        return String.format("Родительская категория:%n%s%n%nДочерняя категория:%n%s", parentCategory, childCategory);
    }

    private static String createSubject(String part) {
        return String.format("%s%s%s", Randoms.string(), part, Randoms.string());
    }

    @Test
    @Description("""
            Проверка тест-кейса https://testpalm2.yandex-team.ru/testcase/ocrm-757
            """)
    public void unmarkedService() {
        doTest(
                BERU_COMPLAINTS_MAIL_CONNECTION,
                Randoms.string(),                   // subject
                Randoms.string(),                   // body
                Randoms.email(),                    // sender
                UNMARKED_SERVICE,                   // expectedService
                DEFAULT_SERVICE_PRIORITY,           // expectedPriority
                Set.of(),                           // expectedMarkers
                Set.of(),                           // expectedCategories
                null                               // expectedOrderNumber
        );
    }

    @Test
    public void testService2() {
        doTest(
                BERU_COMPLAINTS_MAIL_CONNECTION,
                createSubject(TEST_SUBJECT_1),      // subject
                createBodyByCategory(TEST_CATEGORY_TITLE_2, ""),        // body
                Randoms.email(),                    // sender
                TEST_SERVICE_2,                     // expectedService
                TEST_DEFAULT_PRIORITY,              // expectedPriority
                Set.of(),                           // expectedMarkers
                Set.of(TEST_CATEGORY_2),            // expectedCategories
                null                               // expectedOrderNumber
        );
    }

    @Test
    @Description("""
            Проверка тест-кейса https://testpalm2.yandex-team.ru/testcase/ocrm-803
            """)
    public void defaultService() {
        doTest(
                BERU_COMPLAINTS_MAIL_CONNECTION,
                createSubject(TEST_SUBJECT_2),      // subject
                createBodyByCategory(TEST_CATEGORY_TITLE_3, TEST_CATEGORY_TITLE_4), // body
                Randoms.email(),                    // sender
                DEFAULT_SERVICE,                    // expectedService
                DEFAULT_SERVICE_PRIORITY,           // expectedPriority
                Set.of(),                           // expectedMarkers
                Set.of(TEST_CATEGORY_4),            // expectedCategories
                null                               // expectedOrderNumber
        );
    }

    @Test
    @Description("""
            Проверка тест-кейса https://testpalm2.yandex-team.ru/testcase/ocrm-802
            """)
    public void vipService() {
        doTest(
                BERU_COMPLAINTS_MAIL_CONNECTION,
                Randoms.string(),                   // subject
                "test Тип клиента: vip test",       // body
                Randoms.email(),                    // sender
                VIP_SERVICE,                        // expectedService
                VIP_SERVICE_PRIORITY + 25,          // expectedPriority
                Set.of(VIP_MARKER),                 // expectedMarkers
                Set.of(),                           // expectedCategories
                null                               // expectedOrderNumber
        );
    }

    @Test
    @Disabled("FIXME")
    public void vipService_randomBody() {
        doTest(
                BERU_COMPLAINTS_MAIL_CONNECTION,
                Randoms.string(),                   // subject
                Randoms.string(),                   // body
                VIP_EMAIL,                          // sender
                VIP_SERVICE,                        // expectedService
                VIP_SERVICE_PRIORITY + 25,          // expectedPriority
                Set.of(VIP_MARKER),                 // expectedMarkers
                Set.of(),                           // expectedCategories
                null                               // expectedOrderNumber
        );
    }

    @Test
    @Description("""
            проверка тест-кейса - https://testpalm2.yandex-team.ru/testcase/ocrm-802
            """)
    public void unmarkedService_threatens() {
        doTest(
                BERU_COMPLAINTS_MAIL_CONNECTION,
                Randoms.string(),                   // subject
                "угрожает_судом test",              // body
                Randoms.email(),                    // sender
                UNMARKED_SERVICE,                   // expectedService
                VIP_SERVICE_PRIORITY + 15,          // expectedPriority
                Set.of(THREATENS_COURT_MARKER),     // expectedMarkers
                Set.of(),                           // expectedCategories
                null                               // expectedOrderNumber
        );
    }

    @Test
    public void vipService_threatens_withOrder() {
        doTest(
                BERU_COMPLAINTS_MAIL_CONNECTION,
                "заказ " + DEFAULT_ORDER_ID,        // subject
                "угрожает_судом test",              // body
                Randoms.email(),                    // sender
                VIP_SERVICE,                        // expectedService
                VIP_SERVICE_PRIORITY + 15,          // expectedPriority
                Set.of(THREATENS_COURT_MARKER),     // expectedMarkers
                Set.of(),                           // expectedCategories
                DEFAULT_ORDER_ID                   // expectedOrderNumber
        );
    }

    @Test
    @Description("""
            Проверка тест-кейса https://testpalm2.yandex-team.ru/testcase/ocrm-802
            """)
    public void vipService_yandexTeam() {
        doTest(
                BERU_COMPLAINTS_MAIL_CONNECTION,
                Randoms.string(),                   // subject
                Randoms.string(),                   // body
                "vdorogin@yandex-team.ru",          // sender
                VIP_SERVICE,                        // expectedService
                VIP_SERVICE_PRIORITY + 5,           // expectedPriority
                Set.of(YANDEX_TEAM_MARKER),         // expectedMarkers
                Set.of(),                           // expectedCategories
                null                               // expectedOrderNumber
        );
    }

    @Test
    @Description("""
            Проверка тест-кейса https://testpalm2.yandex-team.ru/testcase/ocrm-802
            """)
    public void vipService_yandexTeam_body() {
        doTest(
                BERU_COMPLAINTS_MAIL_CONNECTION,
                Randoms.string(),                   // subject
                "test Тип клиента: Яндексоид test", // body
                Randoms.email(),                    // sender
                VIP_SERVICE,                        // expectedService
                VIP_SERVICE_PRIORITY + 5,           // expectedPriority
                Set.of(YANDEX_TEAM_MARKER),         // expectedMarkers
                Set.of(),                           // expectedCategories
                null                               // expectedOrderNumber
        );
    }

    @Test
    public void vipService_threatens_body_withOrder() {
        doTest(
                BERU_COMPLAINTS_MAIL_CONNECTION,
                Randoms.string(),                   // subject
                "test Тип клиента: Яндексоид test", // body
                Randoms.email(),                    // sender
                VIP_SERVICE,                        // expectedService
                VIP_SERVICE_PRIORITY + 5,           // expectedPriority
                Set.of(YANDEX_TEAM_MARKER),         // expectedMarkers
                Set.of(),                           // expectedCategories
                null                               // expectedOrderNumber
        );
    }

    @Test
    public void vipService_threatens_body_withOrder_withSubject() {
        doTest(
                BERU_COMPLAINTS_MAIL_CONNECTION,
                "Re: Возврат товара по заказу: " + DEFAULT_ORDER_ID, // subject
                "угрожает_судом test",              // body
                Randoms.email(),                    // sender
                VIP_SERVICE,                        // expectedService
                VIP_SERVICE_PRIORITY + 15,          // expectedPriority
                Set.of(THREATENS_COURT_MARKER),     // expectedMarkers
                Set.of(),                           // expectedCategories
                DEFAULT_ORDER_ID                   // expectedOrderNumber
        );
    }

    @Test
    public void vipService_threatens_body_withOrder_withSubject1() {
        doTest(
                BERU_COMPLAINTS_MAIL_CONNECTION,
                "Re: Заказ - " + DEFAULT_ORDER_ID,  // subject
                "угрожает_судом test",              // body
                Randoms.email(),                    // sender
                VIP_SERVICE,                        // expectedService
                VIP_SERVICE_PRIORITY + 15,          // expectedPriority
                Set.of(THREATENS_COURT_MARKER),     // expectedMarkers
                Set.of(),                           // expectedCategories
                DEFAULT_ORDER_ID                  // expectedOrderNumber
        );
    }

    @Test
    public void vipService_threatens_body_withOrder_withSubject2() {
        doTest(
                BERU_COMPLAINTS_MAIL_CONNECTION,
                "FW: Возврат товара по заказу - " + DEFAULT_ORDER_ID + " 110 684", // subject
                "угрожает_судом test",              // body
                Randoms.email(),                    // sender
                VIP_SERVICE,                        // expectedService
                VIP_SERVICE_PRIORITY + 15,          // expectedPriority
                Set.of(THREATENS_COURT_MARKER),     // expectedMarkers
                Set.of(),                           // expectedCategories
                DEFAULT_ORDER_ID                   // expectedOrderNumber
        );
    }

    @Test
    public void vipService_threatens_body_withOrder_withSubject3() {
        doTest(
                BERU_COMPLAINTS_MAIL_CONNECTION,
                "Re: Заказ Покупки №" + DEFAULT_ORDER_ID, // subject
                "угрожает_судом test",              // body
                Randoms.email(),                    // sender
                VIP_SERVICE,                        // expectedService
                VIP_SERVICE_PRIORITY + 15,          // expectedPriority
                Set.of(THREATENS_COURT_MARKER),     // expectedMarkers
                Set.of(),                           // expectedCategories
                DEFAULT_ORDER_ID                   // expectedOrderNumber
        );
    }

    @Test
    public void vipService_threatens_body_withOrder_withSubject4() {
        doTest(
                BERU_COMPLAINTS_MAIL_CONNECTION,
                "Re[2]: Получена чужая посылка: вместо заказа " + DEFAULT_ORDER_ID + " пришёл 2593855", // subj
                "угрожает_судом test",              // body
                Randoms.email(),                    // sender
                VIP_SERVICE,                        // expectedService
                VIP_SERVICE_PRIORITY + 15,          // expectedPriority
                Set.of(THREATENS_COURT_MARKER),     // expectedMarkers
                Set.of(),                           // expectedCategories
                DEFAULT_ORDER_ID                   // expectedOrderNumber
        );
    }

    @Test
    public void vipService_threatens_body_withOrder_withSubject5() {
        doTest(
                BERU_COMPLAINTS_MAIL_CONNECTION,
                "Re: " + DEFAULT_ORDER_ID,          // subject
                "угрожает_судом test",              // body
                Randoms.email(),                    // sender
                VIP_SERVICE,                        // expectedService
                VIP_SERVICE_PRIORITY + 15,          // expectedPriority
                Set.of(THREATENS_COURT_MARKER),     // expectedMarkers
                Set.of(),                           // expectedCategories
                DEFAULT_ORDER_ID                   // expectedOrderNumber
        );
    }

    @Test
    public void vipService_threatens_body_withOrder_withSubject6() {
        doTest(
                BERU_COMPLAINTS_MAIL_CONNECTION,
                "претензия / № D0004139752 (" + DEFAULT_ORDER_ID + ") (утрата 1-го места из 2-х)", // subject
                "угрожает_судом test",              // body
                Randoms.email(),                    // sender
                VIP_SERVICE,                        // expectedService
                VIP_SERVICE_PRIORITY + 15,          // expectedPriority
                Set.of(THREATENS_COURT_MARKER),     // expectedMarkers
                Set.of(),                           // expectedCategories
                DEFAULT_ORDER_ID                   // expectedOrderNumber
        );
    }

    @Test
    public void vipService_threatens_body_withOrder_withSubject7() {
        doTest(
                BERU_COMPLAINTS_MAIL_CONNECTION,
                "Re: Parcel " + DEFAULT_ORDER_ID + " Yandex.Market", // subject
                "угрожает_судом test",              // body
                Randoms.email(),                    // sender
                VIP_SERVICE,                        // expectedService
                VIP_SERVICE_PRIORITY + 15,          // expectedPriority
                Set.of(THREATENS_COURT_MARKER),     // expectedMarkers
                Set.of(),                           // expectedCategories
                DEFAULT_ORDER_ID                   // expectedOrderNumber
        );
    }

    @Test
    public void unmarkedService_threatens_body_withOrder_withSubject7() {
        doTest(
                BERU_COMPLAINTS_MAIL_CONNECTION,
                "Re: 11223398",                     // subject
                "угрожает_судом test",              // body
                Randoms.email(),                    // sender
                UNMARKED_SERVICE,                   // expectedService
                VIP_SERVICE_PRIORITY + 15,          // expectedPriority
                Set.of(THREATENS_COURT_MARKER),     // expectedMarkers
                Set.of(),                           // expectedCategories
                null                               // expectedOrderNumber
        );
    }

    @Test
    public void marketCredit_mailConnection() {
        doTest(
                MARKET_CREDIT_MAIL_CONNECTION,
                Randoms.string(),                     // subject
                Randoms.string(),              // body
                Randoms.email(),                    // sender
                GR_CREDITS_SERVICE,                   // expectedService
                VIP_SERVICE_PRIORITY,          // expectedPriority
                Set.of(),     // expectedMarkers
                Set.of(),                           // expectedCategories
                null                               // expectedOrderNumber
        );
    }

    @Test
    public void marketComplaints_mailConnection() {
        doTest(
                MARKET_COMPLAINTS_MAIL_CONNECTION,
                Randoms.string(),                     // subject
                Randoms.string(),             // body
                Randoms.email(),                    // sender
                MARKET_COMPLAINTS_CLAIMS_SERVICE,                   // expectedService
                VIP_SERVICE_PRIORITY,          // expectedPriority
                Set.of(),     // expectedMarkers
                Set.of(),                           // expectedCategories
                null                               // expectedOrderNumber
        );
    }

    @Test
    public void vipService_marketCredit_mailConnection() {
        doTest(
                MARKET_CREDIT_MAIL_CONNECTION,
                "Re: Parcel " + DEFAULT_ORDER_ID + " Yandex.Market", // subject
                "угрожает_судом test",              // body
                Randoms.email(),                    // sender
                VIP_SERVICE,                        // expectedService
                VIP_SERVICE_PRIORITY + 15,          // expectedPriority
                Set.of(THREATENS_COURT_MARKER),     // expectedMarkers
                Set.of(),                           // expectedCategories
                DEFAULT_ORDER_ID                   // expectedOrderNumber
        );
    }

    @Test
    public void vipService_exceedPriority() {
        // выставляем повышенный приоритет, что бы превысить максимальный
        // (в скрипте beruComplaints при "повышении" приоритета)
        Entity vipService = dbService.getByNaturalId(Service.FQN, Service.CODE, VIP_SERVICE);
        bcpService.edit(vipService, Map.of(Service.DEFAULT_PRIORITY, HIGH_PRIORITY));

        doTest(
                BERU_COMPLAINTS_MAIL_CONNECTION,
                Randoms.string(),       // subject
                Randoms.string(),       // body
                VIP_EMAIL,              // sender
                VIP_SERVICE,            // expectedService
                MAX_SERVICE_PRIORITY,   // expectedPriority
                Set.of(VIP_MARKER),     // expectedMarkers
                Set.of(),               // expectedCategories
                null // expectedOrderNumber
        );
    }

    @Test
    public void vipService_marketComplaints_mailConnection() {
        doTest(
                MARKET_COMPLAINTS_MAIL_CONNECTION,
                "Re: Parcel " + DEFAULT_ORDER_ID + " Yandex.Market", // subject
                "угрожает_судом test",              // body
                Randoms.email(),                    // sender
                VIP_SERVICE,                        // expectedService
                VIP_SERVICE_PRIORITY + 15,          // expectedPriority
                Set.of(THREATENS_COURT_MARKER),     // expectedMarkers
                Set.of(),                           // expectedCategories
                DEFAULT_ORDER_ID                   // expectedOrderNumber
        );
    }

    private void doTest(
            String mailConnection,
            String subject,
            String body,
            String sender,
            String expectedService,
            Long expectedPriority,
            Set<String> expectedMarkers,
            Set<String> expectedCategories,
            Long expectedOrderNumber
    ) {
        TicketCategory category1 = createTicketCategory(TEST_CATEGORY_TITLE_1, TEST_CATEGORY_1);
        TicketCategory category2 = createTicketCategory(TEST_CATEGORY_TITLE_2, TEST_CATEGORY_2);
        TicketCategory category3 = createTicketCategory(TEST_CATEGORY_TITLE_3, TEST_CATEGORY_3);
        createTicketCategory(TEST_CATEGORY_TITLE_4, TEST_CATEGORY_4, category3);

        Service service1 = createService(TEST_SERVICE_1);
        Service service2 = createService(TEST_SERVICE_2);
        //createService(GR_CREDITS_SERVICE);
        //createService(MARKET_COMPLAINTS_CLAIMS_SERVICE);

        bcpService.create(BeruComplaintsServiceRule.FQN, Map.of(
                BeruComplaintsServiceRule.CATEGORY, category1,
                BeruComplaintsServiceRule.SERVICE, service1
        ));
        bcpService.create(BeruComplaintsServiceRule.FQN, Map.of(
                BeruComplaintsServiceRule.CATEGORY, category2,
                BeruComplaintsServiceRule.SERVICE, service2
        ));

        processMessage(mailMessageBuilderService.getMailMessageBuilder(mailConnection)
                .setSubject(subject)
                .setBody(body)
                .setFrom(sender)
                .build()
        );

        BeruComplaintsTicket ticket = getSingleOpenedBeruComplaintsTicket();

        assertNotNull(ticket.getService());
        assertEquals(expectedService, ticket.getService().getCode());
        assertEquals(expectedPriority, ticket.getPriorityLevel());
        assertEquals(expectedMarkers, ticket.getTags().stream()
                .map(TicketTag::getCode)
                .collect(Collectors.toSet())
        );
        assertEquals(expectedCategories, ticket.getCategories().stream()
                .map(TicketCategory::getCode)
                .collect(Collectors.toSet())
        );

        assertEquals(expectedOrderNumber, Optional.ofNullable(ticket.getOrder())
                .map(Order::getTitle)
                .orElse(null)
        );
    }

    private Service createService(String code) {
        return ticketTestUtils.createService(Fqn.of("service$telephony"), Map.of(
                Service.DEFAULT_PRIORITY, TEST_DEFAULT_PRIORITY,
                Service.CODE, code,
                Service.SERVICE_TIME, "08_21",
                Service.SUPPORT_TIME, "08_21"
        ));
    }

    private TicketCategory createTicketCategory(String title, String code) {
        return createTicketCategory(title, code, null);
    }

    private TicketCategory createTicketCategory(String title, String code, TicketCategory parent) {
        Brand brand = dbService.getByNaturalId(Brand.FQN, Brand.CODE, Constants.Brand.BERU_COMPLAINTS);
        scriptContextVariablesService.addContextVariable(
                ScriptContextVariablesService.ContextVariables.CARD_OBJECT,
                brand
        );
        return bcpService.create(TicketCategory.FQN, Maps.of(
                TicketCategory.CODE, code,
                TicketCategory.TITLE, title,
                TicketCategory.PARENT, parent
        ));
    }
}
