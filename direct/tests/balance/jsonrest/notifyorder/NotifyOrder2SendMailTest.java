package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyorder;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.EventlogRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.UsersRecord;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.Money;
import ru.yandex.autotests.direct.utils.money.MoneyCurrency;
import ru.yandex.autotests.direct.utils.money.MoneyFormat;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyOrder2JSONRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.MailLogsRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.MailLogsResponse;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.MailTemplate;
import ru.yandex.autotests.directapi.model.api5.campaigns.CampaignAddItemMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.EmailSettingsMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.NotificationMap;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.autotests.irt.testutils.json.JsonUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static ru.yandex.autotests.directintapi.tests.IntapiConstants.BAYAN_DOMAIN_FORMAT;
import static ru.yandex.autotests.directintapi.tests.IntapiConstants.DIRECT_DOMAIN_FORMAT;
import static ru.yandex.autotests.directintapi.tests.IntapiConstants.NOTIFY_ORDER_CAMP_FINISHED_SUBJECT_TEXT;
import static ru.yandex.autotests.directintapi.tests.IntapiConstants.NOTIFY_ORDER_MONEY_IN_FOR_MCB_SUBJECT_TEXT;
import static ru.yandex.autotests.directintapi.tests.IntapiConstants.NOTIFY_ORDER_MONEY_IN_SUBJECT_TEXT;
import static ru.yandex.autotests.directintapi.tests.IntapiConstants.NOTIFY_ORDER_MONEY_OUT_BLOCKING_SUBJECT_TEXT;
import static ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyorder.SendNotificationHelper.LOCALE;
import static ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyorder.SendNotificationHelper.NOTIFICATION_DATE_TIME_FORMAT;
import static ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyorder.SendNotificationHelper.getSumWithoutVatAsString;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;


/**
 * https://st.yandex-team.ru/TESTIRT-11534
 */
@Issue("https://st.yandex-team.ru/DIRECT-62720")
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.JSON_BALANCE_NOTIFY_ORDER2)
public class NotifyOrder2SendMailTest {

    private static final String LOGIN = Logins.LOGIN_MAIN;
    private static final Currency currency = Currency.RUB;

    private static DirectJooqDbSteps jooqDbSteps;
    private static String fio;
    private static Long clientId;

    private Long cid;
    private LocalDate dateTime;
    private String email;
    private Money sum;
    private String campaignName;
    private MailLogsRequest mailLogsRequest;
    private DefaultCompareStrategy compareStrategy;

    private static Money getSumPayedFromEventLog(EventlogRecord eventLog) {
        Map<String, Object> map = JsonUtils.getObject(eventLog.getParams(), Map.class);
        Currency currency = Currency.valueOf((String) map.get("currency"));
        BigDecimal sumPayed = BigDecimal.valueOf(Double.valueOf((String) map.get("sum_payed")));
        return Money.valueOf(sumPayed, currency);
    }

    private static LocalDate getFinishDateFromEventLog(EventlogRecord eventLog) {
        Map<String, String> map = JsonUtils.getObject(eventLog.getParams(), Map.class);
        return LocalDate.parse(map.get("finish_date"));
    }

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);

    @BeforeClass
    public static void initTestData() {
        jooqDbSteps = api.userSteps.getDirectJooqDbSteps().useShardForLogin(LOGIN);
        UsersRecord user = jooqDbSteps.usersSteps().getUser(LOGIN);
        fio = user.getFio();
        clientId = user.getClientid();
    }

    @Before
    public void beforeSteps() {
        dateTime = LocalDate.now();
        email = LOGIN + RandomStringUtils.randomAlphabetic(5) + "@yandex-team.ru";
        sum = Money.valueOf(RandomUtils.getRandomInteger(100, 1000), currency);

        cid = api.userSteps.campaignSteps().addCampaign(new CampaignAddItemMap()
                .defaultCampaignAddItem()
                .withNotification(new NotificationMap()
                        .withEmailSettings(new EmailSettingsMap()
                                .defaultEmailSettings()
                                .withEmail(email)))
                .withDefaultTextCampaign());
        campaignName = api.userSteps.campaignSteps().getCampaign(cid).getName();

        compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(BeanFieldPath.newPath("logtimeFormat"))
                .useMatcher(startsWith(dateTime.format(NOTIFICATION_DATE_TIME_FORMAT)))
                .forFields(BeanFieldPath.newPath("logtimeUnixtime")).useMatcher(greaterThan(0L));

        mailLogsRequest = new MailLogsRequest()
                .withEmails(email)
                .withDateFrom(dateTime.toString())
                .withDateTo(dateTime.toString());
    }

    private void sendNotifyOrder(Money consumeQty) {
        sendNotifyOrder(consumeQty, NotifyOrder2JSONRequest.DIRECT_SERVICE_ID);
    }

    private void sendNotifyOrder(Money consumeQty, int serviceId) {
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyOrderJsonSteps()
                .notifyOrderNoErrors(new NotifyOrder2JSONRequest()
                        .withServiceId(serviceId)
                        .withServiceOrderId(cid)
                        .withTimestamp()
                        .withConsumeQty(consumeQty.floatValue())
                        .withProductCurrency(currency.toString())
                );
    }


    @Test
    public void checkSendMailWithNotifyOrderMoneyInEvent() {
        sendNotifyOrder(sum);

        mailLogsRequest.withTemplateNames(MailTemplate.NOTIFY_ORDER_MONEY_IN);
        List<MailLogsResponse> responseList =
                api.userSteps.getDarkSideSteps().getMailSmsFakeSteps().showMailLogs(mailLogsRequest);
        assumeThat("в ответе получили лог для одного письма", responseList, hasSize(1));

        MailLogsResponse expectedMailLogsResponse = new MailLogsResponse()
                .withEmail(email)
                .withSubject(format(NOTIFY_ORDER_MONEY_IN_SUBJECT_TEXT, cid))
                .withTemplateName(MailTemplate.NOTIFY_ORDER_MONEY_IN);
        compareStrategy.forFields(BeanFieldPath.newPath("content"))
                .useMatcher(allOf(containsString(cid.toString()), containsString(fio),
                        containsString(campaignName), containsString(getSumWithoutVatAsString(sum, currency)),
                        containsString(dateTime.format(NOTIFICATION_DATE_TIME_FORMAT)),
                        //Проверка, что корректно проставился [% direct_tld %] из шаблона
                        containsString(format(DIRECT_DOMAIN_FORMAT, LOCALE))));

        assertThat("получили ожидаемые параметры письма", responseList.get(0),
                beanDiffer(expectedMailLogsResponse).useCompareStrategy(compareStrategy));
    }

    @Test
    public void checkSumPayedFromEventLogWhenSendMailWithNotifyOrderMoneyInEvent() {
        sendNotifyOrder(sum);

        List<EventlogRecord> eventLogList = jooqDbSteps.eventLogSteps().getEventLogsByCid(clientId, cid);
        assumeThat("в таблице eventlog есть нужная запись", eventLogList, hasSize(1));
        assertThat("получили ожидаемое значение sum_payed", getSumPayedFromEventLog(eventLogList.get(0)), equalTo(sum));
    }

    @Test
    public void checkSendMailWithNotifyOrderMoneyInMcbEvent() {
        api.userSteps.campaignFakeSteps().makeCampaignMcbType(cid.intValue());
        sendNotifyOrder(sum, NotifyOrder2JSONRequest.BAYAN_SERVICE_ID);

        mailLogsRequest.withTemplateNames(MailTemplate.NOTIFY_ORDER_MONEY_IN_MCB);
        List<MailLogsResponse> responseList =
                api.userSteps.getDarkSideSteps().getMailSmsFakeSteps().showMailLogs(mailLogsRequest);
        assumeThat("в ответе получили лог для одного письма", responseList, hasSize(1));

        MailLogsResponse expectedMailLogsResponse = new MailLogsResponse()
                .withEmail(email)
                .withSubject(format(NOTIFY_ORDER_MONEY_IN_FOR_MCB_SUBJECT_TEXT, cid))
                .withTemplateName(MailTemplate.NOTIFY_ORDER_MONEY_IN_MCB);
        String sumPayedUnitsRate = format("%s тысяч",
                sum.bigDecimalValue().setScale(3, BigDecimal.ROUND_HALF_DOWN).stripTrailingZeros().toPlainString());
        compareStrategy.forFields(BeanFieldPath.newPath("content"))
                .useMatcher(allOf(containsString(cid.toString()), containsString(fio),
                        containsString(campaignName), containsString(sumPayedUnitsRate),
                        containsString(dateTime.format(NOTIFICATION_DATE_TIME_FORMAT)),
                        //Проверка, что корректно проставился [% direct_tld %] из шаблона
                        containsString(format(BAYAN_DOMAIN_FORMAT, LOCALE))));

        assertThat("получили ожидаемые параметры письма", responseList.get(0),
                beanDiffer(expectedMailLogsResponse).useCompareStrategy(compareStrategy));
    }

    @Test
    public void checkSendMailWithCampFinishedEvent() {
        LocalDate finishDate = LocalDate.now().minusDays(1);
        api.userSteps.getDirectJooqDbSteps().useShardForLogin(LOGIN)
                .campaignsSteps().setFinishTime(cid, Date.valueOf(finishDate));
        sendNotifyOrder(sum);

        mailLogsRequest.withTemplateNames(MailTemplate.CAMP_FINISHED);
        List<MailLogsResponse> responseList =
                api.userSteps.getDarkSideSteps().getMailSmsFakeSteps().showMailLogs(mailLogsRequest);
        assumeThat("в ответе получили лог для одного письма", responseList, hasSize(1));

        String finishDateInMail = finishDate.format(NOTIFICATION_DATE_TIME_FORMAT);
        MailLogsResponse expectedMailLogsResponse = new MailLogsResponse()
                .withEmail(email)
                .withSubject(format(NOTIFY_ORDER_CAMP_FINISHED_SUBJECT_TEXT, cid, finishDateInMail))
                .withTemplateName(MailTemplate.CAMP_FINISHED);
        compareStrategy.forFields(BeanFieldPath.newPath("content"))
                .useMatcher(allOf(containsString(cid.toString()), containsString(fio),
                        containsString(campaignName), containsString(finishDateInMail),
                        //Проверка, что корректно проставился [% direct_tld %] из шаблона
                        containsString(format(DIRECT_DOMAIN_FORMAT, LOCALE))));

        assertThat("получили ожидаемые параметры письма", responseList.get(0),
                beanDiffer(expectedMailLogsResponse).useCompareStrategy(compareStrategy));
    }

    @Test
    public void checkSumPayedAndFinishDateFromEventLogWhenSendMailWithCampFinishedEvent() {
        LocalDate finishDate = LocalDate.now().minusDays(1);
        api.userSteps.getDirectJooqDbSteps().useShardForLogin(LOGIN)
                .campaignsSteps().setFinishTime(cid, Date.valueOf(finishDate));
        sendNotifyOrder(sum);

        List<EventlogRecord> eventLogList = jooqDbSteps.eventLogSteps().getEventLogsByCid(clientId, cid);
        assumeThat("в таблице eventlog есть нужные записи", eventLogList, hasSize(2));
        assertThat("получили ожидаемое значение sum_payed", getSumPayedFromEventLog(eventLogList.get(0)), equalTo(sum));
        assertThat("получили ожидаемое значение finish_date",
                getFinishDateFromEventLog(eventLogList.get(1)), equalTo(finishDate));
    }

    @Test
    public void checkSendMailWithNotifyOrderMoneyOutBlockingEvent() {
        sendNotifyOrder(sum);
        Money newSum = sum.subtract(55);
        sendNotifyOrder(newSum);

        mailLogsRequest.withTemplateNames(MailTemplate.NOTIFY_ORDER_MONEY_OUT_BLOCKING);
        List<MailLogsResponse> responseList =
                api.userSteps.getDarkSideSteps().getMailSmsFakeSteps().showMailLogs(mailLogsRequest);
        assumeThat("в ответе получили лог для одного письма", responseList, hasSize(1));

        MailLogsResponse expectedMailLogsResponse = new MailLogsResponse()
                .withEmail(email)
                .withSubject(format(NOTIFY_ORDER_MONEY_OUT_BLOCKING_SUBJECT_TEXT, cid))
                .withTemplateName(MailTemplate.NOTIFY_ORDER_MONEY_OUT_BLOCKING);
        compareStrategy.forFields(BeanFieldPath.newPath("content"))
                .useMatcher(allOf(containsString(cid.toString()), containsString(fio),
                        containsString(campaignName),
                        containsString(format("%s %s", newSum.stringValue(MoneyFormat.TWO_DIGITS_POINT_SEPARATED),
                                MoneyCurrency.get(currency).getAbbreviation(LOCALE)))));

        assertThat("получили ожидаемые параметры письма", responseList.get(0),
                beanDiffer(expectedMailLogsResponse).useCompareStrategy(compareStrategy));
    }

    @Test
    public void checkSumPayedFromEventLogWhenSendMailWithNotifyOrderMoneyOutBlockingEvent() {
        sendNotifyOrder(sum);
        Money newSum = sum.subtract(53);
        sendNotifyOrder(newSum);

        List<EventlogRecord> eventLogList = jooqDbSteps.eventLogSteps().getEventLogsByCid(clientId, cid);
        assumeThat("в таблице eventlog есть нужная запись", eventLogList, hasSize(1));
        assertThat("получили ожидаемое значение sum_payed", getSumPayedFromEventLog(eventLogList.get(0)), equalTo(sum));
    }
}
