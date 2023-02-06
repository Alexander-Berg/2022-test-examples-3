package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyorder;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.common.api45mng.APIPort_PortType;
import ru.yandex.autotests.directapi.common.api45mng.CreateNewSubclientResponse;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyOrder2JSONRequest;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.ClientStepsHelper;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static ru.yandex.autotests.direct.db.steps.ClientsOptionsSteps.ClientFlagsEnum.CREATE_WITHOUT_WALLET;

/**
 * Created by pashkus on 27.04.16.
 * https://st.yandex-team.ru/TESTIRT-9023
 */
@Issue("https://st.yandex-team.ru/DIRECT-52051")
@Aqua.Test
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.JSON_BALANCE_NOTIFY_ORDER2)
public class NotifyOrder2TotalConsumeQtyForNonWalletCampTest {
    @ClassRule
    public static ApiSteps api = new ApiSteps().wsdl(APIPort_PortType.class);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    public final Float QTY = 100f;
    public static Long cidRub;        //кампания в рублях
    public static Long cidYndFixed;  //кампания в фишках

    /**
     * @ Создаем и кампанию без общего счета
     */
    @BeforeClass
    public static void prepareTestData() {
        String login = Logins.LOGIN_MAIN;
        cidRub = api.as(login).userSteps.campaignSteps().addDefaultTextCampaign();

        ClientStepsHelper clientStepsHelper = new ClientStepsHelper(api.as(Logins.LOGIN_MNGR).userSteps.clientSteps());
        CreateNewSubclientResponse clientInfo = clientStepsHelper.createServicedClient("intapi-servClient25-", Logins.LOGIN_MNGR);
        String loginWithoutWallet = clientInfo.getLogin();
        Integer clientId = clientInfo.getClientID();
        DirectJooqDbSteps jooqDbSteps = api.userSteps.getDirectJooqDbSteps();
        jooqDbSteps.useShardForLogin(login);
        jooqDbSteps.clientsOptionsSteps().setClientFlag(clientId.longValue(), CREATE_WITHOUT_WALLET);
        cidYndFixed = api.userSteps.campaignSteps().addDefaultTextCampaign(loginWithoutWallet);
    }

    @Test
    @Description("(1.5) NotifyOrder2 без TotalConsumeQty для заказа в фишках. " +
            "Ожидается, что запрос не вернет ошибки;")
    public void notifyOrder2WithoutTotalConsumeQtyForYndxFixed() {
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyOrderJsonSteps()
                .notifyOrderNoErrors(new NotifyOrder2JSONRequest()
                        .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                        .withServiceOrderId(cidYndFixed)
                        .withTimestamp()
                        .withConsumeQty(QTY)
                        .withProductCurrency(Currency.YND_FIXED.value())
                );
    }

    @Test
    @Description("(1.6) NotifyOrder2 без TotalConsumeQty для заказа в рублях." +
            "Ожидается, что запрос не вернет ошибки;")
    public void notifyOrder2WithoutTotalConsumeQtyForRub() {
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyOrderJsonSteps()
                .notifyOrderNoErrors(new NotifyOrder2JSONRequest()
                        .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                        .withServiceOrderId(cidRub)
                        .withTimestamp()
                        .withConsumeQty(QTY)
                        .withProductCurrency(Currency.RUB.value())
                );
    }

}
