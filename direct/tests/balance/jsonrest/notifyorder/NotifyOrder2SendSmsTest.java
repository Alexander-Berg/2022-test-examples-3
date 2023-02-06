package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyorder;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import com.yandex.direct.api.v5.campaigns.SmsEventsEnum;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.SmsQueueSendStatus;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.Money;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyOrder2JSONRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.SmsLogsRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.SmsLogsResponse;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directapi.model.api5.campaigns.CampaignAddItemMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.NotificationMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.SmsSettingsMap;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.directintapi.tests.IntapiConstants.NOTIFY_ORDER_CAMP_FINISHED_SMS_TEXT;
import static ru.yandex.autotests.directintapi.tests.IntapiConstants.NOTIFY_ORDER_MONEY_IN_SMS_TEXT;
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
public class NotifyOrder2SendSmsTest {

    private static final String LOGIN = Logins.LOGIN_MAIN;

    private static Long uid;
    private static Money sum;
    private static Currency currency;

    private Long cid;
    private SmsLogsRequest smsLogsRequest;
    private SmsLogsResponse expectedSmsLogsResponse;

    @ClassRule
    public static final ApiSteps api = new ApiSteps().as(LOGIN);

    @BeforeClass
    public static void initTestData() {
        currency = Currency.RUB;
        sum = Money.valueOf(RandomUtils.getRandomInteger(100, 1000), currency);

        uid = Long.parseLong(User.get(LOGIN).getPassportUID());
    }

    @Before
    public void beforeSteps() {
        LocalDate dateTime = LocalDate.now();

        cid = api.userSteps.campaignSteps().addCampaign(new CampaignAddItemMap()
                .defaultCampaignAddItem()
                .withNotification(new NotificationMap()
                        .withSmsSettings(new SmsSettingsMap()
                                .defaultSmsSettings()
                                .withEvents(SmsEventsEnum.MONEY_IN, SmsEventsEnum.FINISHED)))
                .withDefaultTextCampaign());

        smsLogsRequest = new SmsLogsRequest()
                .withCids(cid)
                .withDateFrom(dateTime.toString())
                .withDateTo(dateTime.toString());

        expectedSmsLogsResponse = new SmsLogsResponse()
                .withCid(cid)
                .withUid(uid)
                //Отправка смс на тестовых средах отключена, поэтому статус = ожидает отправки
                .withSendStatus(SmsQueueSendStatus.Wait.getLiteral())
                .withSmsTime("");
    }

    private void sendNotifyOrder(Money consumeQty) {
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyOrderJsonSteps()
                .notifyOrderNoErrors(new NotifyOrder2JSONRequest()
                        .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                        .withServiceOrderId(cid)
                        .withTimestamp()
                        .withConsumeQty(consumeQty.floatValue())
                        .withProductCurrency(currency.toString())
                );
    }


    @Test
    public void checkSendSmsWithNotifyOrderMoneyInEvent() {
        sendNotifyOrder(sum);

        List<SmsLogsResponse> responseList =
                api.userSteps.getDarkSideSteps().getMailSmsFakeSteps().showSmsLogs(smsLogsRequest);
        assumeThat("в ответе получили лог для одного sms", responseList, hasSize(1));

        String sumWithoutVat = getSumWithoutVatAsString(sum, currency);
        expectedSmsLogsResponse.withSmsText(format(NOTIFY_ORDER_MONEY_IN_SMS_TEXT, sumWithoutVat, cid));
        assertThat("получили ожидаемые параметры sms", responseList.get(0), beanDiffer(expectedSmsLogsResponse));
    }

    @Test
    public void checkSendSmsWithCampFinishedEvent() {
        LocalDate finishDate = LocalDate.now().minusDays(1);
        api.userSteps.getDirectJooqDbSteps().useShardForLogin(LOGIN)
                .campaignsSteps().setFinishTime(cid, Date.valueOf(finishDate));
        sendNotifyOrder(sum);

        List<SmsLogsResponse> responseList =
                api.userSteps.getDarkSideSteps().getMailSmsFakeSteps().showSmsLogs(smsLogsRequest);
        SmsLogsResponse smsLogsResponse = responseList.stream()
                .filter(s -> s.getSmsText().contains("закончилась"))
                .findFirst()
                .orElse(null);
        assumeThat("в ответе получили лог для нужного sms", smsLogsResponse, notNullValue());

        expectedSmsLogsResponse.withSmsText(
                format(NOTIFY_ORDER_CAMP_FINISHED_SMS_TEXT, cid, finishDate.format(NOTIFICATION_DATE_TIME_FORMAT)));
        assertThat("получили ожидаемые параметры sms", smsLogsResponse, beanDiffer(expectedSmsLogsResponse));
    }
}
