package ru.yandex.autotests.direct.cmd.strategy.savecamp.dbcheck;

import org.junit.Before;
import org.junit.Rule;

import ru.yandex.autotests.direct.cmd.data.commons.CampaignStrategy;
import ru.yandex.autotests.direct.cmd.data.counters.MetrikaCountersData;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampaignsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.MoneyCurrency;
import ru.yandex.qatools.allure.annotations.Description;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

public abstract class SaveCampStrategyDbTestBase {

    private static final String CLIENT = "at-direct-backend-c";
    private static final Currency CURRENCY = Currency.RUB;
    protected static final Double AVG_BID = 3.0d;
    protected static final Double AVG_CPA = 3.0d;
    protected static final Double BID = 3.0d;
    protected static final Double SUM = MoneyCurrency.get(CURRENCY).getMinWeeklyBudgetAmount().doubleValue();

    private CampaignRule campaignRule = getCampaignRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(campaignRule);

    protected Long campaignId;
    protected SaveCampRequest saveCampRequest;

    protected abstract CampaignRule getCampaignRule();

    protected abstract CampaignStrategy getCampaignStrategy();

    protected abstract CampaignsRecord getExpectedCampaignDb();

    protected String getLogin() {
        return CLIENT;
    }

    @Before
    public void before() {
        campaignId = campaignRule.getCampaignId();
        saveCampRequest = getSaveCampRequest().withMobileAppId(null);
        prepareStatistics();
    }

    @Description("Проверяем сохранение стратегии в бд")
    public void checkCampaignStrategyDb() {
        cmdRule.cmdSteps().campaignSteps().postSaveCamp(saveCampRequest);

        CampaignsRecord campaignsRecord =
                TestEnvironment.newDbSteps().useShardForLogin(getLogin()).campaignsSteps().getCampaignById(campaignId);

        assertThat("стратегия правильно сохранилась в базе", campaignsRecord.intoMap(),
                beanDiffer(getExpectedCampaignDb().intoMap()).useCompareStrategy(onlyExpectedFields()));
    }

    private SaveCampRequest getSaveCampRequest() {
        return campaignRule.getSaveCampRequest()
                .withCid(campaignId.toString())
                .withJsonStrategy(getCampaignStrategy());
    }

    protected void prepareStatistics() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).campMetrikaGoalsSteps().
                addOrUpdateMetrikaGoals(campaignId, MetrikaCountersData.DEFAULT_COUNTER.getFirstGoalId(), 50L, 50L);
    }
}
