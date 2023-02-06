package ru.yandex.autotests.directintapi.tests.autobudgetpricesset;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;

import com.yandex.direct.api.v5.keywords.KeywordGetItem;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.Money;
import ru.yandex.autotests.direct.utils.money.MoneyCurrency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.common.api45mng.APIPort_PortType;
import ru.yandex.autotests.directapi.common.api45mng.CreateNewSubclientResponse;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.autobudget.AutobudgetPricesSetRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.autobudget.AutobudgetPricesSetResponse;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.BannerPhraseFakeInfo;
import ru.yandex.autotests.directapi.darkside.steps.ConvertationLockSteps;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.model.api5.campaigns.TextCampaignNetworkStrategyAddMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.TextCampaignSearchStrategyAddMap;
import ru.yandex.autotests.directapi.model.clients.ConvertType;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.ClientStepsHelper;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Authors: omaz, xy6er
 * Date: 21.01.15
 * https://st.yandex-team.ru/TESTIRT-3980
 */

@Aqua.Test(title = "AutobudgetPrices.set - блокировка при конвертации клиента")
@Tag(TagDictionary.NEVER_RUN)
@Features(FeatureNames.NOT_REGRESSION_YET)
@RunWith(Parameterized.class)
public class AutobudgetPricesSetConvertationLockTest {
    protected LogSteps log = LogSteps.getLogger(this.getClass());
    private static final String ERROR_MESSAGE = "currency converting soon";
    private static DarkSideSteps darkSideSteps;
    private static ClientStepsHelper clientStepsHelper;

    private Money price;
    private AutobudgetPricesSetRequest request;
    private Long pid;
    private Long keywordId;

    @Parameterized.Parameter
    public String convertType;

    private String login;

    @ClassRule
    public static ApiSteps api = new ApiSteps().wsdl(APIPort_PortType.class).as(Logins.LOGIN_MNGR);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();


    @Parameterized.Parameters(name = "тип конвертации: {0}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {ConvertType.COPY},
                {ConvertType.MODIFY}
        };
        return Arrays.asList(data);
    }

    @BeforeClass
    public static void initSteps() {
        darkSideSteps = api.userSteps.getDarkSideSteps();
        clientStepsHelper = new ClientStepsHelper(api.userSteps.clientSteps());
    }

    @Before
    public void init() {
        log.info("Подготовка данных для теста");
        log.info("Создаем фишкового клиента (сервисируемого)");
        CreateNewSubclientResponse clientInfo = clientStepsHelper
                .createServicedClient("intapi-servClient18-", Logins.LOGIN_MNGR);
        login = clientInfo.getLogin();

        log.info("Создаем кампанию с нужными параметрами для нового клиента");
        Long campaignId = api.userSteps.campaignSteps()
                .addDefaultTextCampaignWithStrategies(
                        new TextCampaignSearchStrategyAddMap().defaultWbMaximumClicks(Currency.YND_FIXED),
                        new TextCampaignNetworkStrategyAddMap().defaultNetworkDefault(),
                        login);
        pid = api.userSteps.adGroupsSteps().addDefaultGroup(campaignId, login);
        api.userSteps.adsSteps().addDefaultTextAd(pid, login);
        keywordId = api.userSteps.keywordsSteps().addDefaultKeyword(login, pid);
        api.userSteps.phrasesFakeSteps().setBannerFakePhraseIds(pid);
        BigInteger phraseIDFake = api.userSteps.phrasesFakeSteps().getBannerPhraseParams(keywordId).getPhraseID();

        price = MoneyCurrency.get(Currency.YND_FIXED).getDefaultPriceConstructorAmount();

        request = new AutobudgetPricesSetRequest(
                pid, phraseIDFake,
                price.floatValue(), price.floatValue(),
                0, 1);
    }


    @Test
    public void autobudgetPricesSetConvertationMoreThan15MinutesTest() {
        api.userSteps.clientFakeSteps().convertCurrencyWithDelay(login, Currency.RUB.toString(), convertType,
                ConvertationLockSteps.LOCK_DELAY_MINUTES + 1);
        log.info("Проверяем, что AutobudgetPrices.set проходит, если конвертация больше чем через 15 минут");
        darkSideSteps.getAutobudgetSteps().set(request);

        log.info("Проверяем, что значение price изменилось");
        KeywordGetItem keywordGetItem = api.userSteps.keywordsSteps().keywordsGetById(login, keywordId).get(0);
        assertThat("Не удалось установить верное значение поля Price",
                keywordGetItem.getBid(), equalTo(price.bidLong().longValue()));
    }

    @Test
    public void autobudgetPricesSetConvertationIn15MinutesTest() {
        log.info("Запоминаем значение Price до конвертации");
        // не используем API для чтения ставки, т.к. после начала конвертации API перестанет быть доступным
        // и не получится еще раз его позвать, чтобы свериться заново
        BigDecimal expectedPrice =
                api.userSteps.getDirectJooqDbSteps().useShardForLogin(login).bidsSteps().getBidById(keywordId)
                        .getPrice();

        api.userSteps.clientFakeSteps().convertCurrencyWithDelay(login, Currency.RUB.toString(), convertType,
                ConvertationLockSteps.LOCK_DELAY_MINUTES - 1);
        log.info("Проверяем, что AutobudgetPrices.set не проходит, если конвертация меньше чем через 15 минут");
        darkSideSteps.getAutobudgetSteps().setWithExpectedError(request, 8, ERROR_MESSAGE);

        log.info("Проверяем, что значение price не изменилось");
        BigDecimal actualPrice =
                api.userSteps.getDirectJooqDbSteps().useShardForLogin(login).bidsSteps().getBidById(keywordId)
                        .getPrice();
        assertThat("Значение price изменилось",
                actualPrice, equalTo(expectedPrice));
    }


    @Test
    public void autobudgetPricesSetConvertationTwoClientsTest() {
        log.info("Запоминаем значение Price до конвертации");
        // не используем API для чтения ставки, т.к. после начала конвертации API перестанет быть доступным
        // и не получится еще раз его позвать, чтобы свериться заново
        BigDecimal expectedPrice =
                api.userSteps.getDirectJooqDbSteps().useShardForLogin(login).bidsSteps().getBidById(keywordId)
                        .getPrice();

        log.info("Создаем второго клиента для теста");
        CreateNewSubclientResponse clientInfo = clientStepsHelper
                .createServicedClient("intapi-servClient19-", Logins.LOGIN_MNGR);
        String secondLogin = clientInfo.getLogin();

        log.info("Создаем кампанию с нужными параметрами для нового клиента");
        Long secondCampaignID = api.userSteps.campaignSteps().addDefaultTextCampaignWithStrategies(
                new TextCampaignSearchStrategyAddMap().defaultWbMaximumClicks(Currency.RUB),
                new TextCampaignNetworkStrategyAddMap().defaultNetworkDefault(),
                secondLogin
        );
        Long secondPid = api.userSteps.adGroupsSteps().addDefaultGroup(secondCampaignID, secondLogin);
        api.userSteps.adsSteps().addDefaultTextAd(secondPid, secondLogin);
        Long secondKeywordId = api.userSteps.keywordsSteps().addDefaultKeyword(secondLogin, secondPid);
        api.userSteps.phrasesFakeSteps().setBannerFakePhraseIds(secondPid);
        BannerPhraseFakeInfo[] bannerPhraseFakeInfos = api.userSteps.phrasesFakeSteps().getBannerPhrasesParams(secondPid);
        BigInteger secondPhraseIDFake = bannerPhraseFakeInfos[0].getPhraseID();

        AutobudgetPricesSetRequest secondRequest = new AutobudgetPricesSetRequest(
                secondPid, secondPhraseIDFake,
                price.floatValue(), price.floatValue(),
                0, 1);


        log.info("Ставим в очередь на конвертацию только первого клиента");
        api.userSteps.clientFakeSteps().convertCurrencyWithDelay(login, Currency.RUB.toString(), convertType,
                ConvertationLockSteps.LOCK_DELAY_MINUTES - 1);

        log.info("Вызываем AutobudgetPrices.set для двух фраз. Ожидаем ошибку только для первой фразы.");

        AutobudgetPricesSetResponse expectedError = new AutobudgetPricesSetResponse(
                8,
                ERROR_MESSAGE,
                request.getGroupExportID(),
                request.getPhraseID()
        );
        darkSideSteps.getAutobudgetSteps().setWithExpectedErrors(
                new AutobudgetPricesSetRequest[]{request, secondRequest},
                expectedError
        );

        log.info("Проверяем, что значение price для первого клиента не изменилось");
        BigDecimal actualPrice =
                api.userSteps.getDirectJooqDbSteps().useShardForLogin(login).bidsSteps().getBidById(keywordId)
                        .getPrice();
        assertThat("Значение price изменилось",
                actualPrice, equalTo(expectedPrice));

        log.info("Проверяем, что значение price для второго клиента изменилось");
        KeywordGetItem secondKeywordGetItem =
                api.userSteps.keywordsSteps().keywordsGetById(secondLogin, secondKeywordId).get(0);
        assertThat("Не удалось установить верное значение поля Price",
                secondKeywordGetItem.getBid(), equalTo(price.bidLong().longValue()));
    }
}

