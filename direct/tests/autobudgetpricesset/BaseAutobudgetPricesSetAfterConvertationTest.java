package ru.yandex.autotests.directintapi.tests.autobudgetpricesset;

import java.math.BigInteger;

import org.junit.Before;
import org.junit.ClassRule;

import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.directapi.common.api45mng.APIPort_PortType;
import ru.yandex.autotests.directapi.common.api45mng.CreateNewSubclientResponse;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.autobudget.AutobudgetPricesSetRequest;
import ru.yandex.autotests.directapi.model.api5.campaigns.TextCampaignNetworkStrategyAddMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.TextCampaignSearchStrategyAddMap;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.ClientStepsHelper;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

/**
 * Author: xy6er
 * Date: 21.01.15
 * https://st.yandex-team.ru/TESTIRT-3506
 * https://st.yandex-team.ru/TESTIRT-3980
 */
public class BaseAutobudgetPricesSetAfterConvertationTest {
    protected LogSteps log = LogSteps.getLogger(this.getClass());

    protected Long bannerID;
    protected Long keywordId;
    protected String login;
    protected AutobudgetPricesSetRequest request;

    @ClassRule
    public static ApiSteps api = new ApiSteps().wsdl(APIPort_PortType.class);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Before
    public void init() {
        log.info("Подготовка данных для теста");
        log.info("Создаем фишкового клиента (сервисируемого)");
        ClientStepsHelper clientStepsHelper = new ClientStepsHelper(api.as(Logins.LOGIN_MNGR).userSteps.clientSteps());
        CreateNewSubclientResponse clientInfo = clientStepsHelper
                .createServicedClient("intapi-servClient5-", Logins.LOGIN_MNGR);
        login = clientInfo.getLogin();

        log.info("Создаем кампанию с нужными параметрами для нового клиента");
        Long campaignId = api.userSteps.campaignSteps()
                .addDefaultTextCampaignWithStrategies(
                        new TextCampaignSearchStrategyAddMap().defaultWbMaximumClicks(Currency.YND_FIXED),
                        new TextCampaignNetworkStrategyAddMap().defaultNetworkDefault(),
                        login);
        Long pid = api.userSteps.adGroupsSteps().addDefaultGroup(campaignId, login);
        api.userSteps.adsSteps().addDefaultTextAd(pid, login);
        keywordId = api.userSteps.keywordsSteps().addDefaultKeyword(login, pid);
        api.userSteps.phrasesFakeSteps().setBannerFakePhraseIds(pid);
        BigInteger phraseIDFake = api.userSteps.phrasesFakeSteps().getBannerPhraseParams(keywordId).getPhraseID();

        request = new AutobudgetPricesSetRequest();
        request.setGroupExportID(pid);
        request.setPhraseID(phraseIDFake);
        request.setContextType(1);
    }

}
