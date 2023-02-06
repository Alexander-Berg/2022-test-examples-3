package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyorder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.WalletCampaignsRecord;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.Money;
import ru.yandex.autotests.direct.utils.money.MoneyCurrency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyOrder2JSONRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.ClientFakeInfo;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.MailLogsRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.MailLogsResponse;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.MailTemplate;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.tests.IntapiConstants;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;

import static java.lang.String.format;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.startsWith;
import static ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyorder.SendNotificationHelper.LOCALE;
import static ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyorder.SendNotificationHelper.NOTIFICATION_DATE_TIME_FORMAT;
import static ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyorder.SendNotificationHelper.getSumWithoutVatAsString;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * https://st.yandex-team.ru/TESTIRT-11534
 */
@Issue("https://st.yandex-team.ru/DIRECT-62720")
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.JSON_BALANCE_NOTIFY_ORDER2)
public class NotifyOrder2SendMailForWalletTest {

    //Не надо использовать в других тестах т.к. в этом тесте сбрасывается сумма на кошельке
    private static final String LOGIN = "at-intapi-wallet10";

    private static String email;
    private static String fio;
    private static Money sum;
    private static Currency currency;
    private static Long walletCid;

    private MailLogsRequest mailLogsRequest;
    private DefaultCompareStrategy compareStrategy;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);

    @BeforeClass
    public static void prepareTestData() {
        currency = Currency.RUB;
        sum = Money.valueOf(RandomUtils.getRandomInteger(100, 1000), currency);

        ClientFakeInfo clientData = api.userSteps.clientFakeSteps().getClientData(LOGIN, new String[]{"email", "fio"});
        fio = clientData.getFio();
        email = clientData.getEmail();
        walletCid = (long) api.userSteps.financeSteps().getAccountID(LOGIN);

        /*
        Добавляем в базу НДС т.к. на ТС по неизвестным причинам НДС у клиента удаляется,
            скорей всего после переналивки базы ТС это действие можно удалить
        Умножаем на 100, чтобы получилось 18
         */
        api.userSteps.clientFakeSteps().setVATRate(LOGIN, MoneyCurrency.get(currency).getVatRate() * 100);
    }

    @Before
    public void beforeSteps() {
        LocalDate dateTime = LocalDate.now();
        compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(BeanFieldPath.newPath("logtimeFormat"))
                .useMatcher(startsWith(dateTime.format(NOTIFICATION_DATE_TIME_FORMAT)))
                .forFields(BeanFieldPath.newPath("logtimeUnixtime")).useMatcher(greaterThan(0L));

        mailLogsRequest = new MailLogsRequest()
                .withEmails(email)
                .withDateFrom(dateTime.toString())
                .withDateTo(dateTime.toString());

        resetWalletSum(walletCid);
    }

    @Step("Обнуляем значение wallet_campaigns.total_sum и campaigns.sum для кошелька: {0}")
    public void resetWalletSum(long walletCid) {
        DirectJooqDbSteps jooqDbSteps = api.userSteps.getDirectJooqDbSteps().useShardForLogin(LOGIN);
        WalletCampaignsRecord walletCampaign = jooqDbSteps.walletCampaignsSteps().getWalletCampaigns(walletCid);
        walletCampaign.setTotalSum(BigDecimal.ZERO);
        jooqDbSteps.walletCampaignsSteps().updateWalletCampaigns(walletCampaign);
        api.userSteps.campaignFakeSteps().setCampaignSum(walletCid, 0f);
    }


    @Test
    public void checkSendMailWithNotifyOrderMoneyInEventForWallet() {
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyOrderJsonSteps()
                .notifyOrderNoErrors(new NotifyOrder2JSONRequest()
                        .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                        .withServiceOrderId(walletCid)
                        .withTimestamp()
                        .withConsumeQty(sum.floatValue())
                        .withTotalConsumeQty(sum.floatValue())
                        .withProductCurrency(currency.toString())
                );

        mailLogsRequest.withTemplateNames(MailTemplate.NOTIFY_ORDER_MONEY_IN);
        List<MailLogsResponse> responseList =
                api.userSteps.getDarkSideSteps().getMailSmsFakeSteps().showMailLogs(mailLogsRequest);
        //Берем последнее письмо
        MailLogsResponse mailLogsResponse = responseList.stream()
                .max(Comparator.comparingLong(MailLogsResponse::getLogtimeUnixtime))
                .orElse(null);

        MailLogsResponse expectedMailLogsResponse = new MailLogsResponse()
                .withEmail(email)
                .withSubject(format(IntapiConstants.NOTIFY_ORDER_MONEY_IN_FOR_WALLET_SUBJECT_TEXT, LOGIN))
                .withTemplateName(MailTemplate.NOTIFY_ORDER_MONEY_IN);
        compareStrategy.forFields(BeanFieldPath.newPath("content"))
                .useMatcher(allOf(containsString(LOGIN),
                        containsString(getSumWithoutVatAsString(sum, currency)),
                        //Проверка, что корректно проставился [% direct_tld %] из шаблона
                        containsString("direct.yandex." + LOCALE)));

        assertThat("получили ожидаемые параметры письма", mailLogsResponse,
                beanDiffer(expectedMailLogsResponse).useCompareStrategy(compareStrategy));
    }
}
