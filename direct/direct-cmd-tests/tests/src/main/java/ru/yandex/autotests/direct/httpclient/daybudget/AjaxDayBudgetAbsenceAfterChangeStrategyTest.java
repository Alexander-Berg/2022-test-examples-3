package ru.yandex.autotests.direct.httpclient.daybudget;

import java.util.Collection;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.CampaignStrategy;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.DayBudget;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.CmdStrategyBeans;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.direct.utils.strategy.data.Strategies;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.direct.httpclient.CocaineSteps.getCsrfTokenFromCocaine;
import static ru.yandex.autotests.direct.utils.strategy.data.Strategies.StrategiesFilters.byCampaignTypes;
import static ru.yandex.autotests.direct.utils.strategy.data.Strategies.StrategiesFilters.byIsAutoOption;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 * Date: 22.09.14
 */

@Aqua.Test
@Description("Проверка отсутствия дневного бюджета при редактировании стратегии существующей кампании")
@Stories(TestFeatures.AjaxSave.AJAX_SAVE_AUTOBUDGET)
@Features(TestFeatures.AJAX_SAVE)
@Tag(TrunkTag.YES)
@Tag(CmdTag.AJAX_SAVE_DAY_BUDGET)
@Tag(OldTag.YES)
@RunWith(Parameterized.class)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class AjaxDayBudgetAbsenceAfterChangeStrategyTest {

    public static String CLIENT = "at-daybudget-c";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    public Strategies strategy;
    @Rule
    public DirectCmdRule cmdRule;
    private TextBannersRule bannersRule;
    private CampaignStrategy ajaxStrategy;

    public AjaxDayBudgetAbsenceAfterChangeStrategyTest(Strategies strategy) {
        this.strategy = strategy;
        ajaxStrategy = CmdStrategyBeans.getStrategyBean(strategy, User.get(CLIENT).getCurrency());
        bannersRule = new TextBannersRule().convertCurrency(true)
                .overrideCampTemplate(new SaveCampRequest().withJsonStrategy(ajaxStrategy))
                .withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule).as(CLIENT);
    }

    @Parameterized.Parameters(name = "Стратегия: {0}")
    public static Collection testData() {
        return Strategies
                .getStrategyListByPredicate(byIsAutoOption(false).and(byCampaignTypes(CampaignTypeEnum.TEXT)))
                .stream()
                .map(ArrayUtils::toArray)
                .collect(Collectors.toList());
    }

    @Test
    @Description("Проверяем отсутсвие дневного бюджета при редактировании стратегии")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10446")
    public void checkAjaxSaveEnabledDayBudget() {
        DayBudget expectedDayBudget = new DayBudget().withShowMode(DayBudget.ShowMode.DEFAULT)
                .withDailyChangeCount("0")
                .withSum("0.00")
                .withStopTime("0000-00-00 00:00:00");

        CSRFToken csrfToken = getCsrfTokenFromCocaine(User.get(CLIENT).getPassportUID());
        cmdRule.oldSteps().strategySteps()
                .saveAndCheckResponseDayBudgetAndStrategy(bannersRule.getCampaignId().toString(), null, ajaxStrategy,
                        csrfToken);
        DayBudget actualDayBudget =
                cmdRule.cmdSteps().campaignSteps()
                        .getEditCamp(bannersRule.getCampaignId(), CLIENT).getCampaign().getDayBudget();
        assertThat("дневной бюджет в апи отсутствует", actualDayBudget,
                BeanDifferMatcher.beanDiffer(expectedDayBudget).useCompareStrategy(
                        DefaultCompareStrategies.onlyExpectedFields())
        );
    }

}
