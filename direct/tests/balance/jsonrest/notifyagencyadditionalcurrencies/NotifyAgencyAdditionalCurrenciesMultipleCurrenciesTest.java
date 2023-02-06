package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyagencyadditionalcurrencies;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.apiclient.errors.AxisError;
import ru.yandex.autotests.directapi.apiclient.errors.AxisErrorDetails;
import ru.yandex.autotests.directapi.apiclient.methods.Method;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyAgencyAdditionalCurrenciesJSONRequest;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.model.clients.CreateNewSubclientRequestMap;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.ClientStepsHelper;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

/**
 * Created by omaz on 28.01.14.
 * https://jira.yandex-team.ru/browse/TESTIRT-1387
 */
@Aqua.Test(title = "NotifyAgencyAdditionalCurrencies - несколько валют")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.JSON_BALANCE_NOTIFY_AGENCY_ADDITIONAL_CURRECIES)
public class NotifyAgencyAdditionalCurrenciesMultipleCurrenciesTest {
    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd");
    private DarkSideSteps darkSideSteps = new DarkSideSteps();
    protected LogSteps log = LogSteps.getLogger(this.getClass());
    private Currency newCurrency = Currency.KZT;
    private Currency newCurrency2 = Currency.EUR;

    @ClassRule
    public static ApiSteps api = new ApiSteps();

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Before
    public void before() {
        api.userSteps.clientFakeSteps().enableToCreateSubClients(
                Logins.LOGIN_AGENCY
        );
        api.as(Logins.LOGIN_AGENCY);
    }

    @Test
    public void notifyAgencyAdditionalCurrenciesMultipleCurrenciesTest() {
        log.info("Обнуляем данные - запрещаем агентству использовать валюты из теста (выставляем старую expireDate)");
        darkSideSteps.getBalanceClientNotifyAgencyAdditionalCurrenciesJsonSteps()
                .notifyAgencyAdditionalCurrenciesNoErrors(
                        new NotifyAgencyAdditionalCurrenciesJSONRequest()
                                .withClientId(Long.valueOf(
                                        api.userSteps.clientFakeSteps().getClientData(Logins.LOGIN_AGENCY).getClientID()))
                                .withAdditionalCurrency(new NotifyAgencyAdditionalCurrenciesJSONRequest.AdditionalCurrency()
                                        .withCurrency(newCurrency.value())
                                        .withExpireDate(DateTime.now().minusDays(1).toString(FORMATTER)))
                                .withAdditionalCurrency(new NotifyAgencyAdditionalCurrenciesJSONRequest.AdditionalCurrency()
                                        .withCurrency(newCurrency2.value())
                                        .withExpireDate(DateTime.now().minusDays(1).toString(FORMATTER)))
                );

        darkSideSteps.getBalanceClientNotifyAgencyAdditionalCurrenciesJsonSteps()
                .notifyAgencyAdditionalCurrenciesNoErrors(
                        new NotifyAgencyAdditionalCurrenciesJSONRequest()
                                .withClientId(Long.valueOf(
                                        api.userSteps.clientFakeSteps().getClientData(Logins.LOGIN_AGENCY).getClientID()))
                                .withAdditionalCurrency(new NotifyAgencyAdditionalCurrenciesJSONRequest.AdditionalCurrency()
                                        .withCurrency(newCurrency.value())
                                        .withExpireDate(DateTime.now().toString(FORMATTER)))
                                .withAdditionalCurrency(new NotifyAgencyAdditionalCurrenciesJSONRequest.AdditionalCurrency()
                                        .withCurrency(newCurrency2.value())
                                        .withExpireDate(DateTime.now().toString(FORMATTER)))
                );

        ClientStepsHelper clientStepsHelper = new ClientStepsHelper(api.userSteps.clientSteps());
        log.info("После нотификации у агентства должна появиться возможность создать субклиента в обеих валютах");
        clientStepsHelper.createNewAgencySubClient("at-intapi-agencySub-", Logins.LOGIN_AGENCY, newCurrency);
        clientStepsHelper.createNewAgencySubClient("at-intapi-agencySub-", Logins.LOGIN_AGENCY, newCurrency2);
    }


    @Test
    public void notifyAgencyAdditionalCurrenciesOldExpireDateTest() {
        log.info("NotifyAgencyAdditionalCurrencies - истекшая дата");
        darkSideSteps.getBalanceClientNotifyAgencyAdditionalCurrenciesJsonSteps()
                .notifyAgencyAdditionalCurrenciesNoErrors(
                        new NotifyAgencyAdditionalCurrenciesJSONRequest()
                                .withClientId(Long.valueOf(
                                        api.userSteps.clientFakeSteps().getClientData(Logins.LOGIN_AGENCY).getClientID()))
                                .withAdditionalCurrency(new NotifyAgencyAdditionalCurrenciesJSONRequest.AdditionalCurrency()
                                        .withCurrency(newCurrency.value())
                                        .withExpireDate(DateTime.now().minusDays(1).toString(FORMATTER)))
                                .withAdditionalCurrency(new NotifyAgencyAdditionalCurrenciesJSONRequest.AdditionalCurrency()
                                        .withCurrency(newCurrency2.value())
                                        .withExpireDate(DateTime.now().toString(FORMATTER)))
                );
        api.userSteps.shouldGetErrorOn(
                "После нотификации со старой датой у агентства не должно быть возможности создать субклиента в новой валюте",
                Method.CREATE_NEW_SUBCLIENT,
                new CreateNewSubclientRequestMap<>(api.type())
                        .withRandomLogin("at-intapi-agencySub-")
                        .withAgencyLogin(Logins.LOGIN_AGENCY)
                        .withCurrency(newCurrency.toString()),
                new AxisError(71, AxisErrorDetails.FIELD_CURRENCY_MUST_CONTAIN_THE_FOLLOWING_VALUE, "EUR, RUB")
        );
    }

}
