package ru.yandex.autotests.direct.httpclient.daybudget.autobudgetcmd;

import java.math.RoundingMode;
import java.util.Collection;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runners.Parameterized;

import ru.yandex.autotests.direct.cmd.data.commons.CampaignStrategy;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.CampaignV2;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.DayBudget;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.Money;
import ru.yandex.autotests.direct.utils.strategy.data.Strategies;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.allure.annotations.Description;

import static ru.yandex.autotests.direct.utils.strategy.data.Strategies.StrategiesFilters.byCampaignTypes;
import static ru.yandex.autotests.direct.utils.strategy.data.Strategies.StrategiesFilters.byIsAutoOption;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 * Date: 22.09.14
 */
public abstract class SaveDayBudgetUsingAutobudgetCMDTestBase {
    protected static final String CLIENT = "at-daybudget-c";
    private static final Double BUDGET_SUM = 15.11d;

    protected BannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule).as(CLIENT);
    @Parameterized.Parameter
    public Strategies strategy;

    private Currency currency = User.get(CLIENT).getCurrency();
    protected CampaignStrategy ajaxStrategy;
    protected DayBudget dayBudget;

    @Parameterized.Parameters(name = "Стратегия: {0}")
    public static Collection testData() {
        return Strategies
                .getStrategyListByPredicate(byIsAutoOption(false).and(byCampaignTypes(CampaignTypeEnum.TEXT)))
                .stream()
                .map(ArrayUtils::toArray)
                .collect(Collectors.toList());
    }

    @Before
    public void before() {
        dayBudget = new DayBudget()
                .withSum(Money.valueOf(BUDGET_SUM, Currency.YND_FIXED).convert(currency)
                        .setScale(2, RoundingMode.CEILING).doubleValue().toString())
                .withShowMode(DayBudget.ShowMode.DEFAULT)
                .withSet(true);
    }


    @Description("Проверяем сохранение дневного бюджета в режиме 'стандартный'")
    public void checkAjaxSaveEnabledDayBudget() {
        cmdRule.cmdSteps().strategySteps()
                .saveAutobudget(bannersRule.getCampaignId().toString(), ajaxStrategy, dayBudget);

        CampaignV2 actualCampaign = cmdRule.cmdSteps().campaignSteps()
                .getEditCamp(bannersRule.getCampaignId(), CLIENT).getCampaign();
        CampaignV2 expectedCampaign = new CampaignV2()
                .withDayBudget(dayBudget.withSet(null));

        assertThat("параметры бюджета соответствуют ожиданиям", actualCampaign,
                beanDiffer(expectedCampaign).useCompareStrategy(onlyExpectedFields()));
    }

    @Description("Проверяем сохранение дневного бюджета в режиме 'распределенный'")
    public void checkAjaxSaveEnabledDayBudgetStretched() {
        dayBudget.setShowMode(DayBudget.ShowMode.STRETCHED);
        cmdRule.cmdSteps().strategySteps()
                .saveAutobudget(bannersRule.getCampaignId().toString(), ajaxStrategy, dayBudget);
        CampaignV2 actualCampaign = cmdRule.cmdSteps().campaignSteps()
                .getEditCamp(bannersRule.getCampaignId(), CLIENT).getCampaign();
        CampaignV2 expectedCampaign = new CampaignV2()
                .withDayBudget(dayBudget.withSet(null));

        assertThat("параметры бюджета соответствуют ожиданиям", actualCampaign,
                beanDiffer(expectedCampaign).useCompareStrategy(onlyExpectedFields()));
    }

    @Description("Проверяем сохранение выключенного дневного бюджета")
    public void checkAjaxSaveDisabledDayBudget() {
        dayBudget.setSum("0.00");
        dayBudget.setShowMode(DayBudget.ShowMode.DEFAULT);
        dayBudget.withSet(false);
        cmdRule.cmdSteps().strategySteps()
                .saveAutobudget(bannersRule.getCampaignId().toString(), ajaxStrategy, dayBudget);
        CampaignV2 actualCampaign = cmdRule.cmdSteps().campaignSteps()
                .getEditCamp(bannersRule.getCampaignId(), CLIENT).getCampaign();
        CampaignV2 expectedCampaign = new CampaignV2()
                .withDayBudget(dayBudget.withSet(null));

        assertThat("параметры бюджета соответствуют ожиданиям", actualCampaign,
                beanDiffer(expectedCampaign).useCompareStrategy(onlyExpectedFields()));
    }
}
