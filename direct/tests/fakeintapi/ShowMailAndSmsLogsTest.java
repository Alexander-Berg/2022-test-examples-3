package ru.yandex.autotests.directintapi.tests.fakeintapi;

import java.util.List;

import com.yandex.direct.api.v5.campaigns.SmsEventsEnum;
import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.LocalDate;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyOrder2JSONRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.MailLogsRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.MailLogsResponse;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.MailTemplate;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.SmsLogsRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.SmsLogsResponse;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.model.api5.campaigns.CampaignAddItemMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.EmailSettingsMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.NotificationMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.SmsSettingsMap;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;

import static java.lang.String.format;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

/**
 * User: xy6er
 * https://st.yandex-team.ru/TESTIRT-3617
 */

@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.FAKE_METHODS)
public class ShowMailAndSmsLogsTest {
    private static DarkSideSteps darkSideSteps;
    private static Long cid;
    private static String email;
    private static LocalDate dateTime;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_MAIN);

    @BeforeClass
    public static void initTestData() {
        darkSideSteps = api.userSteps.getDarkSideSteps();
        dateTime = LocalDate.now();

        email = Logins.LOGIN_MAIN + RandomStringUtils.randomAlphabetic(5) + "@yandex-team.ru";

        cid = api.userSteps.campaignSteps().addCampaign(new CampaignAddItemMap()
                .defaultCampaignAddItem()
                .withNotification(new NotificationMap()
                        .withEmailSettings(new EmailSettingsMap()
                                .defaultEmailSettings()
                                .withEmail(email))
                        .withSmsSettings(new SmsSettingsMap()
                                .defaultSmsSettings()
                                .withEvents(SmsEventsEnum.MONEY_IN)))
                .withDefaultTextCampaign());

        darkSideSteps.getBalanceClientNotifyOrderJsonSteps().notifyOrderNoErrors(new NotifyOrder2JSONRequest()
                .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                .withServiceOrderId(cid)
                .withTimestamp()
                .withConsumeQty(100.0f)
                .withProductCurrency(Currency.RUB.toString()));
    }


    @Test
    public void showMailLogsTest() {
        MailLogsRequest mailLogsRequest = new MailLogsRequest()
                .withEmails(email)
                .withDateFrom(dateTime.toString())
                .withDateTo(dateTime.toString())
                .withTemplateNames(MailTemplate.NOTIFY_ORDER_MONEY_IN);
        List<MailLogsResponse> responseList =
                api.userSteps.getDarkSideSteps().getMailSmsFakeSteps().showMailLogs(mailLogsRequest);
        assumeThat("в ответе получили лог для одного письма", responseList, hasSize(1));

        assertThat("Неверный subject письма", responseList.get(0).getSubject(), endsWith("N" + cid));
    }

    @Test
    public void showSmsLogsTest() {
        SmsLogsRequest smsLogsRequest = new SmsLogsRequest()
                .withCids(cid)
                .withDateFrom(dateTime.toString())
                .withDateTo(dateTime.toString());
        List<SmsLogsResponse> responseList = darkSideSteps.getMailSmsFakeSteps().showSmsLogs(smsLogsRequest);
        assumeThat("в ответе получили лог для одного смс", responseList, hasSize(1));

        assertThat("Неверный текст смски", responseList.get(0).getSmsText(), endsWith(format("N%d.", cid)));
    }
}
