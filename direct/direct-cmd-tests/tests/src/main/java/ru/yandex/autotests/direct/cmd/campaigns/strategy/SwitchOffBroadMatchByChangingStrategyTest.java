package ru.yandex.autotests.direct.cmd.campaigns.strategy;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.Campaign;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.CmdStrategyBeans;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.strategy.data.Strategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestStepsEn.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestStepsEn.assumeThat;

@Aqua.Test
@Description("Отключение ДРФ при выставлении стратегий с отключенными показами на поиске")
@Stories(TestFeatures.AjaxSave.AJAX_SAVE_AUTOBUDGET)
@Features(TestFeatures.AJAX_SAVE)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
public class SwitchOffBroadMatchByChangingStrategyTest {
    private static final String CLIENT = Logins.DEFAULT_CLIENT;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT).overrideCampTemplate(getRequest());

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(CLIENT).withRules(bannersRule);

    private SaveCampRequest getRequest() {
        SaveCampRequest saveCampRequest = new SaveCampRequest();
        saveCampRequest.setBroad_match_flag("Yes");
        saveCampRequest.setBroad_match_limit("50");
        saveCampRequest.setJsonStrategy(CmdStrategyBeans
                .getStrategyBean(Strategies.WEEKLY_BUDGET_MAX_CLICKS_DEFAULT)); // любая стратегия с показами на поиска

        return saveCampRequest;
    }

    @Before
    public void before() {
        Campaign camp = cmdRule.cmdSteps().campaignSteps().getCampaign(CLIENT, bannersRule.getCampaignId());
        assumeThat("Стратегия с показами на поиске", camp.getIsSearchStop(), equalTo("0"));
        assumeThat("ДРФ включены", camp.getBroadMatchFlag(), equalTo("1"));
    }

    @Test
    @Description("Отключение ДРФ при выставлении стратегий с отключенными показами на поиске")
    @TestCaseId("11005")
    public void setStrategyWithSearchStop() {
        cmdRule.cmdSteps().strategySteps()
                .saveAutobudget(bannersRule.getCampaignId().toString(),
                        CmdStrategyBeans.getStrategyBean(
                                Strategies.SHOWS_DISABLED_WEEKLY_BUDGET_MAX_CLICKS)); // любая стратегия с без показов на поиска

        Campaign camp = cmdRule.cmdSteps().campaignSteps().getCampaign(CLIENT, bannersRule.getCampaignId());
        assumeThat("Стратегия с отключенными показами на поиске", camp.getIsSearchStop(), equalTo("1"));
        assertThat("ДРФ выключены", camp.getBroadMatchFlag(), equalTo("0"));
    }

    @Test
    @Description("ДРФ не включается обратно при смене со стратегий с отключенными показами на поиске на стратегию с показами на поиске")
    @TestCaseId("11004")
    public void setStrategyWithoutSearchStopAgain() {
        cmdRule.cmdSteps().strategySteps()
                .saveAutobudget(bannersRule.getCampaignId().toString(),
                        CmdStrategyBeans.getStrategyBean(
                                Strategies.SHOWS_DISABLED_WEEKLY_BUDGET_MAX_CLICKS));  // любая стратегия с без показов на поиска

        Campaign camp = cmdRule.cmdSteps().campaignSteps().getCampaign(CLIENT, bannersRule.getCampaignId());
        assumeThat("Стратегия с отключенными показами на поиске", camp.getIsSearchStop(), equalTo("1"));
        assumeThat("ДРФ выключены", camp.getBroadMatchFlag(), equalTo("0"));

        cmdRule.cmdSteps().strategySteps()
                .saveAutobudget(bannersRule.getCampaignId().toString(),
                        CmdStrategyBeans.getStrategyBean(
                                Strategies.WEEKLY_BUDGET_MAX_CLICKS_DEFAULT)); // любая стратегия с показами на поиска

        camp = cmdRule.cmdSteps().campaignSteps().getCampaign(CLIENT, bannersRule.getCampaignId());
        assumeThat("Стратегия с показами на поиске", camp.getIsSearchStop(), equalTo("0"));
        assertThat("ДРФ выключены", camp.getBroadMatchFlag(), equalTo("0"));
    }
}
