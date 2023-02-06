package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyorder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.YandexAgencyOrdersYaorderstatus;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.YandexAgencyOrdersRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyOrder2JSONRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.MailLogsRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.MailTemplate;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test(title = "NotifyOrder2 - оплата заказа в яндекс.агенства")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.JSON_BALANCE_NOTIFY_ORDER2)
public class NotifyOrder2YandexAgencyTest {
    public static final int YANDEX_AGENCY_PROMO_ORDER_SERVICE_ID = 177;
    public static final String TID = "8878825016765";

    private static final String LOGIN = "at-tester-yapromo";

    private static DarkSideSteps darkSideSteps = new DarkSideSteps();
    private static String MANAGER = Logins.TRANSFER_MANAGER;

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(MANAGER);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    private long clientId;
    private long yaOrderId;

    @Before
    public void init() {
        clientId = Long.parseLong(User.get(LOGIN).getClientID());
        TestEnvironment.newDbSteps().useShardForLogin(LOGIN).yandexAgencyOrdersSteps()
                .setYandexAgencyOrderStatus(YandexAgencyOrdersYaorderstatus.New, clientId);
        yaOrderId = TestEnvironment.newDbSteps().useShardForLogin(LOGIN).yandexAgencyOrdersSteps()
                .getYandexAgencyOrderByClientId(clientId)
                .getYaorderid();
    }

    @Test
    public void notifyOrder2YaOrderMailSendingTest() {
        LocalDateTime now = LocalDateTime.now();
        NotifyOrder2JSONRequest request = new NotifyOrder2JSONRequest()
                .withServiceOrderId(yaOrderId)
                .withTid(TID)
                .withServiceId(YANDEX_AGENCY_PROMO_ORDER_SERVICE_ID);
        darkSideSteps.getBalanceClientNotifyOrderJsonSteps().notifyOrderNoErrors(request);

        MailLogsRequest mailLogsRequest = new MailLogsRequest();
        mailLogsRequest.withTemplateNames(MailTemplate.YA_AGENCY_ORDER_PAID);
        mailLogsRequest.setDateFrom(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        YandexAgencyOrdersRecord actualOrder =
                TestEnvironment.newDbSteps().yandexAgencyOrdersSteps().getYandexAgencyOrderByClientId(clientId);

        assumeThat("Статус соответвует ожиданиям", actualOrder.getYaorderstatus(),
                equalTo(YandexAgencyOrdersYaorderstatus.Paid));

        ZoneId zoneId = ZoneId.systemDefault();
        assertThat("Письмо отправляется", darkSideSteps.getMailSmsFakeSteps().showMailLogs(mailLogsRequest).stream()
                        .anyMatch(
                                mail -> mail.getSubject().contains(String.valueOf(clientId)) &&
                                        mail.getLogtimeUnixtime() >= now.atZone(zoneId).toEpochSecond()),
                is(true));
    }
}
