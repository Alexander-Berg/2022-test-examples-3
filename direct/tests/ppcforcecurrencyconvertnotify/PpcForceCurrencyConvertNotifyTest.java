package ru.yandex.autotests.directintapi.tests.ppcforcecurrencyconvertnotify;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.direct.utils.model.RegionIDValues;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.directapi.common.api45mng.APIPort_PortType;
import ru.yandex.autotests.directapi.common.api45mng.CreateNewSubclientResponse;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.logic.ppc.ClientsToForceMulticurrencyTeaser;
import ru.yandex.autotests.directapi.logic.ppc.ForceCurrencyConvert;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.ClientStepsHelper;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.beandiffer.matchvariation.DefaultMatchVariation;
import ru.yandex.autotests.irt.testutils.beandiffer.matchvariation.MatchVariation;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanDiffer;

/**
 * Created by pavryabov on 30.05.15.
 * https://st.yandex-team.ru/TESTIRT-5459
 */
@Aqua.Test()
@Features(FeatureNames.NOT_REGRESSION_YET)
@Description("Проверка скрипта ppcForceCurrencyConvertNotify.pl")
@Issue("https://st.yandex-team.ru/DIRECT-40916")
public class PpcForceCurrencyConvertNotifyTest {
    //DIRECT-42618

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104).wsdl(APIPort_PortType.class);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    private static DarkSideSteps darkSideSteps = new DarkSideSteps();
    private static String manager = Logins.LOGIN_MNGR;
    private String login;
    private Integer clientID;
    private Integer shard;
    private static final String PATTERN = "yyyy'-'MM'-'dd HH':'mm':'ss";


    @Before
    public void createClient() {
        api.as(manager);
        ClientStepsHelper clientStepsHelper = new ClientStepsHelper(api.userSteps.clientSteps());
        CreateNewSubclientResponse createNewSubclientResponse =
                clientStepsHelper.createServicedClient("intapi-servClient-", Logins.LOGIN_MNGR);
        login = createNewSubclientResponse.getLogin();
        clientID = createNewSubclientResponse.getClientID();
        shard = darkSideSteps.getClientFakeSteps().getUserShard(clientID);
        api.userSteps.clientFakeSteps().setForceCurrencyConvertAcceptedAt(login, DateTime.now().toString(PATTERN));
        ForceCurrencyConvert forceCurrencyConvert =
                darkSideSteps.getDBSteps().getForceCurrencyConvertSteps().getForceCurrencyConvert(clientID, shard);
        assumeThat("клиент есть в force_currency_convert", forceCurrencyConvert, notNullValue());
        assumeThat("клиент есть в force_currency_convert, но без даты конвертации",
                forceCurrencyConvert.getConvertDate(), nullValue());
    }

    @Test
    public void runPpcForceCurrencyConvertNotifyForUkrainianClient() {
        //создание плательщика
        api.userSteps.balanceSteps().createHryvnasPerson(clientID);
        //совершение оплаты
        api.as(Logins.LOGIN_SUPER, login).userSteps.addActiveCampaign(login, 1);
        //получение НДС
        darkSideSteps.getRunScriptSteps().runBalanceGetClientNDSDiscountSchedule(shard, login);
        //получение списка стран и валют
        darkSideSteps.getRunScriptSteps().runPpcFetchClientMulticurrencyTeaserData(shard, login);
        //разрешение на конвертацию
        darkSideSteps.getDBSteps().getClientsToForceMulticurrencyTeaserSteps().saveClientsToForceMulticurrencyTeaser(
                new ClientsToForceMulticurrencyTeaser(Long.valueOf(clientID), false), shard
        );
        darkSideSteps.getRunScriptSteps().runPpcForceCurrencyConvertNotify(shard, login);
        ForceCurrencyConvert forceCurrencyConvert =
                darkSideSteps.getDBSteps().getForceCurrencyConvertSteps().getForceCurrencyConvert(clientID, shard);
        ForceCurrencyConvert expectedForceConvertCurrencyClient = new ForceCurrencyConvert(
                Long.valueOf(clientID), "", "", Currency.UAH.toString(), RegionIDValues.UKRAINE.getId().longValue()
        );
        MatchVariation variation =
                new DefaultMatchVariation().forFields("convertDate", "acceptedAt").useMatcher(notNullValue());
        assertThat("клиенту добавилась дата конвертации, страна и валюта", forceCurrencyConvert,
                beanDiffer(expectedForceConvertCurrencyClient).withVariation(variation));
    }
}
