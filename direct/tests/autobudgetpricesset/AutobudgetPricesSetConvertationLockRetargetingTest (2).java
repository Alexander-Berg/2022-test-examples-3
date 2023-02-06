package ru.yandex.autotests.directintapi.tests.autobudgetpricesset;

import java.math.BigInteger;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BidsRetargetingRecord;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.Money;
import ru.yandex.autotests.direct.utils.money.MoneyCurrency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.common.api45mng.APIPort_PortType;
import ru.yandex.autotests.directapi.common.api45mng.CreateNewSubclientResponse;
import ru.yandex.autotests.directapi.common.api45mng.RetargetingGoal;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.autobudget.AutobudgetPricesSetRequest;
import ru.yandex.autotests.directapi.darkside.steps.ConvertationLockSteps;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.model.api5.campaigns.TextCampaignNetworkStrategyAddMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.TextCampaignSearchStrategyAddMap;
import ru.yandex.autotests.directapi.model.campaigns.MetrikaGoals;
import ru.yandex.autotests.directapi.model.clients.ConvertType;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.ClientStepsHelper;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Author: xy6er
 * Date: 21.01.15
 * https://st.yandex-team.ru/TESTIRT-3980
 */

@Aqua.Test(title = "AutobudgetPrices.set - блокировка при конвертации клиента (ретаргетинг)")
@Tag(TagDictionary.NEVER_RUN)
@Features(FeatureNames.NOT_REGRESSION_YET)
public class AutobudgetPricesSetConvertationLockRetargetingTest {
    protected LogSteps log = LogSteps.getLogger(this.getClass());
    private static DarkSideSteps darkSideSteps;

    //цель Метрики для привязки ретаргетинга
    private Integer goaldID = MetrikaGoals.getRandom();
    private Long bannerID;
    private Long keywordId;
    private String login;
    private AutobudgetPricesSetRequest request;
    private Money price;

    @ClassRule
    public static ApiSteps api = new ApiSteps().wsdl(APIPort_PortType.class);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @BeforeClass
    public static void initSteps() {
        darkSideSteps = api.userSteps.getDarkSideSteps();
    }

    @Before
    public void init() {
        log.info("Подготовка данных для теста");
        log.info("Создаем фишкового клиента (сервисируемого)");
        ClientStepsHelper clientStepsHelper = new ClientStepsHelper(api.as(Logins.LOGIN_MNGR).userSteps.clientSteps());
        CreateNewSubclientResponse clientInfo = clientStepsHelper
                .createServicedClient("intapi-servClient17-", Logins.LOGIN_MNGR);
        login = clientInfo.getLogin();
        int shard = api.userSteps.clientFakeSteps().getUserShard(login);

        log.info("Создаем кампанию с ретаргетингом");
        Long campaignId = api.userSteps.campaignSteps().addDefaultTextCampaignWithStrategies(
                new TextCampaignSearchStrategyAddMap().defaultServingOff(),
                new TextCampaignNetworkStrategyAddMap().defaultWbMaximumClicks(Currency.RUB),
                login
        );

        api.userSteps.getDirectJooqDbSteps().useShard(shard).campaignsSteps()
                .addCampMetrikaGoals(campaignId, (long) goaldID, 200L, 0L);

        RetargetingGoal goal = new RetargetingGoal();
        goal.setLogin(login);
        goal.setGoalID(goaldID);
        goal.setName("goal name");
        int[] retargetingConditionIds = api.userSteps.retargetingSteps().addRetargetingConditions(
                api.userSteps.retargetingSteps().generateRandomRetargetingCondition(login, goal)
        );
        Long pid = api.userSteps.adGroupsSteps().addDefaultGroup(campaignId, login);
        bannerID = api.userSteps.adsSteps().addDefaultTextAd(pid, login);
        keywordId = api.userSteps.keywordsSteps().addDefaultKeyword(login, pid);
        api.userSteps.retargetingSteps().addRetargetingToBanner(login, bannerID, retargetingConditionIds[0]);

        //в качестве phraseId для ретаргетинга используется pid из bids_retargeting
        long phraseIDFake = api.userSteps.getDirectJooqDbSteps().bidsRetargetingSteps()
                .getBidsRetargetingByBid(bannerID)
                .get(0)
                .getRetCondId();
        price = MoneyCurrency.get(Currency.YND_FIXED).getDefaultPriceConstructorAmount();

        request = new AutobudgetPricesSetRequest();
        request.setGroupExportID(pid);
        request.setPhraseID(BigInteger.valueOf(phraseIDFake));
        request.setPrice(price.floatValue());
        request.setContextPrice(price.floatValue());
        request.setContextType(2);
    }


    @Test
    public void autobudgetPricesSetConvertationMoreThan15MinutesTest() {
        api.userSteps.clientFakeSteps().convertCurrencyWithDelay(login, Currency.RUB.toString(), ConvertType.MODIFY,
                ConvertationLockSteps.LOCK_DELAY_MINUTES + 1);
        log.info("Проверяем, что AutobudgetPrices.set проходит, если конвертация больше чем через 15 минут");

        request.setCurrency(0);
        darkSideSteps.getAutobudgetSteps().set(request);

        log.info("Проверяем, что значение price изменилось");
        List<BidsRetargetingRecord> bids = api.userSteps.getDirectJooqDbSteps().useShardForLogin(login)
                .bidsRetargetingSteps()
                .getBidsRetargetingByBid(bannerID);
        assertThat("Не удалось установить верное значение поля Price",
                bids.get(0).getPriceContext().floatValue(), equalTo(price.floatValue()));
    }

    @Test
    public void autobudgetPricesSetConvertationIn15MinutesTest() {
        log.info("Запоминаем значение Price до конвертации");
        Long price = api.userSteps.keywordsSteps().keywordsGetById(login, keywordId).get(0).getBid();
        float expectedPrice = Money.valueOf(price).bidShort().floatValue();

        api.userSteps.clientFakeSteps().convertCurrencyWithDelay(login, Currency.RUB.toString(), ConvertType.MODIFY,
                ConvertationLockSteps.LOCK_DELAY_MINUTES - 1);
        log.info("Проверяем, что AutobudgetPrices.set не проходит, если конвертация меньше чем через 15 минут");
        request.setCurrency(0);
        darkSideSteps.getAutobudgetSteps().setWithExpectedError(request, 8, "currency converting soon (retargetings)");

        log.info("Проверяем, что значение price не изменилось");
        List<BidsRetargetingRecord> bids = api.userSteps.getDirectJooqDbSteps().useShardForLogin(login)
                .bidsRetargetingSteps()
                .getBidsRetargetingByBid(bannerID);
        assertThat("Значение price изменилось",
                bids.get(0).getPriceContext().floatValue(), equalTo(expectedPrice));
    }

    @Test
    public void autobudgetPricesSetAfterConvertationTest() {
        api.userSteps.clientFakeSteps().convertCurrency(login, Currency.RUB.toString(), ConvertType.MODIFY);
        log.info("Проверяем, что AutobudgetPrices.set проходит, если конвертация больше чем через 15 минут");
        request.setCurrency(1);
        darkSideSteps.getAutobudgetSteps().set(request);

        log.info("Проверяем, что значение price изменилось");
        List<BidsRetargetingRecord> bids =  api.userSteps.getDirectJooqDbSteps().useShardForLogin(login)
                .bidsRetargetingSteps()
                .getBidsRetargetingByBid(bannerID);
        assertThat("Не удалось установить верное значение поля Price",
                bids.get(0).getPriceContext().floatValue(), equalTo(price.floatValue()));
    }

}

