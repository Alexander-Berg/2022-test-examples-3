package ru.yandex.autotests.direct.httpclient.daybudget;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.CampaignV2;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.DayBudget;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Сохранение дневного бюджета с помощью ajaxSaveDayBudget")
@Stories(TestFeatures.AjaxSave.AJAX_SAVE_DAY_BUDGET)
@Features(TestFeatures.AJAX_SAVE)
@Tag(TrunkTag.YES)
@Tag(CmdTag.AJAX_SAVE_DAY_BUDGET)
@Tag(OldTag.YES)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class AjaxSaveDayBudgetUsingDayBudgetCMDTest {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    public static String CLIENT = "at-daybudget-c";

    private TextBannersRule bannersRule = new TextBannersRule().convertCurrency(true).withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule).as(CLIENT);

    private DayBudget dayBudget;

    @Before
    public void before() {
        dayBudget = new DayBudget()
                .withSum("400.00")
                .withShowMode(DayBudget.ShowMode.DEFAULT)
                .withSet(true);
    }

    @Test
    @Description("Проверяем сохранение дневного бюджета в режиме 'стандартный'")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10448")
    public void checkAjaxSaveEnabledDayBudget() {
        cmdRule.cmdSteps().campaignSteps().setDayBudget(bannersRule.getCampaignId(), dayBudget, null);
        CampaignV2 actualCampaign = cmdRule.cmdSteps().campaignSteps()
                .getEditCamp(bannersRule.getCampaignId(), CLIENT).getCampaign();
        CampaignV2 expectedCampaign = new CampaignV2()
                .withDayBudget(dayBudget.withSet(null));

        assertThat("параметры бюджета соответствуют ожиданиям", actualCampaign,
                beanDiffer(expectedCampaign).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    @Description("Проверяем сохранение дневного бюджета в режиме 'распределенный'")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10449")
    public void checkAjaxSaveEnabledDayBudgetStretched() {
        dayBudget.setShowMode(DayBudget.ShowMode.STRETCHED);
        cmdRule.cmdSteps().campaignSteps().setDayBudget(bannersRule.getCampaignId(), dayBudget, null);
        CampaignV2 actualCampaign = cmdRule.cmdSteps().campaignSteps()
                .getEditCamp(bannersRule.getCampaignId(), CLIENT).getCampaign();
        CampaignV2 expectedCampaign = new CampaignV2()
                .withDayBudget(dayBudget.withSet(null));

        assertThat("параметры бюджета соответствуют ожиданиям", actualCampaign,
                beanDiffer(expectedCampaign).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    @Description("Проверяем сохранение выключенного дневного бюджета")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10447")
    public void checkAjaxSaveDisabledDayBudget() {
        cmdRule.cmdSteps().campaignSteps().setDayBudget(bannersRule.getCampaignId(), dayBudget, null);

        dayBudget.withSum(null)
                .withShowMode(null)
                .withSet(false);

        cmdRule.cmdSteps().campaignSteps().setDayBudget(bannersRule.getCampaignId(), dayBudget, null);
        CampaignV2 actualCampaign = cmdRule.cmdSteps().campaignSteps()
                .getEditCamp(bannersRule.getCampaignId(), CLIENT).getCampaign();
        CampaignV2 expectedCampaign = new CampaignV2()
                .withDayBudget(new DayBudget()
                        .withSum("0.00"));

        assertThat("параметры бюджета соответствуют ожиданиям", actualCampaign,
                beanDiffer(expectedCampaign).useCompareStrategy(onlyExpectedFields()));
    }
}
