package ru.yandex.autotests.directintapi.tests.ppcforcecurrencyconvert;

import java.sql.Date;
import java.time.LocalDateTime;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.ClientsToForceMulticurrencyTeaserRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CurrencyConvertQueueRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.ForceCurrencyConvertRecord;
import ru.yandex.autotests.directapi.common.api45mng.APIPort_PortType;
import ru.yandex.autotests.directapi.common.api45mng.CreateNewSubclientResponse;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.ClientStepsHelper;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

/**
 * Created by pavryabov on 15.05.15.
 * https://st.yandex-team.ru/TESTIRT-5459
 */
@Aqua.Test()
@Features(FeatureNames.NOT_REGRESSION_YET)
@Description("Проверка скрипта ppcForceCurrencyConvert.pl")
@Issue("https://st.yandex-team.ru/DIRECT-40916")
public class PpcForceCurrencyConvertTest {
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
    private static final int HOURS_LIMIT = 23;


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
        //создание плательщика
        api.userSteps.balanceSteps().createByPhPerson(clientID);
        //совершение оплаты
        api.as(Logins.LOGIN_SUPER, login).userSteps.addActiveCampaign(login, 1);
        //получение НДС
        darkSideSteps.getRunScriptSteps().runBalanceGetClientNDSDiscountSchedule(shard, login);
        //получение списка стран и валют
        darkSideSteps.getRunScriptSteps().runPpcFetchClientMulticurrencyTeaserData(shard, login);
        //разрешение на конвертацию
        darkSideSteps.getDirectJooqDbSteps().useShard(shard).clientsToForceMulticurrencyTeaser()
                .createClientsToForceMulticurrencyTeaser(Long.valueOf(clientID), 0);

        darkSideSteps.getRunScriptSteps().runPpcForceCurrencyConvertNotify(shard, login);
        ForceCurrencyConvertRecord forceCurrencyConvertRecord =
                darkSideSteps.getDirectJooqDbSteps().forceCurrencyConvertSteps().getForceCurrencyConvert(Long.valueOf(clientID));
        assumeThat("клиент есть в force_currency_convert", forceCurrencyConvertRecord, notNullValue());
        CurrencyConvertQueueRecord currencyConvertQueueRecord =
                darkSideSteps.getDirectJooqDbSteps().currencyConvertQueueSteps().getCurrencyConvertQueue(Long.valueOf(clientID));
        assumeThat("клиента еще нет в очереди на конвертацию", currencyConvertQueueRecord, nullValue());
    }

    @Test
    public void checkScriptWithConvertDateTheDayAfterTomorrow() {
        darkSideSteps.getDirectJooqDbSteps().forceCurrencyConvertSteps().updateConvertDate(
                clientID, Date.valueOf(LocalDateTime.now().plusDays(2).toLocalDate()));
        darkSideSteps.getRunScriptSteps().runPpcForceCurrencyConvert(shard, login);
        CurrencyConvertQueueRecord currencyConvertQueueRecord =
                darkSideSteps.getDirectJooqDbSteps().currencyConvertQueueSteps().getCurrencyConvertQueue(Long.valueOf(clientID));
        assertThat("клиент не поставился в очередь на конвертацию", currencyConvertQueueRecord, nullValue());
    }

    @Test
    public void checkScriptWithConvertDateTomorrow() {
        darkSideSteps.getDirectJooqDbSteps().forceCurrencyConvertSteps().updateConvertDate(
                clientID, Date.valueOf(LocalDateTime.now().plusDays(1).toLocalDate()));
        darkSideSteps.getRunScriptSteps().runPpcForceCurrencyConvert(shard, login);
        CurrencyConvertQueueRecord currencyConvertQueueRecord =
                darkSideSteps.getDirectJooqDbSteps().currencyConvertQueueSteps().getCurrencyConvertQueue(Long.valueOf(clientID));
        assertThat("клиент поставился в очередь на конвертацию", currencyConvertQueueRecord, notNullValue());
    }
}
