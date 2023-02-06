package ru.yandex.autotests.direct.httpclient.campaigns.editcamp;

import java.util.Collection;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.commons.CampaignStrategy;
import ru.yandex.autotests.direct.cmd.data.editcamp.EditCampResponse;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.CmdStrategyBeans;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.strategy.testdata.StrategyTestData;
import ru.yandex.autotests.direct.utils.strategy.data.Strategies;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directapi.model.campaigns.MetrikaGoals;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Проверка стратегии для контроллера editCamp")
@Stories(TestFeatures.Campaigns.EDIT_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@RunWith(Parameterized.class)
@Tag(TrunkTag.YES)
@Tag(ObjectTag.STRATEGY)
@Tag(CmdTag.EDIT_CAMP)
@Tag(OldTag.YES)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class EditCampStrategyTest {
    public static String CLIENT = Logins.DEFAULT_CLIENT;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule;
    public Strategies strategy;
    private TextBannersRule bannersRule;
    private CampaignStrategy ajaxStrategy;

    public EditCampStrategyTest(Strategies strategy) {
        this.strategy = strategy;
        ajaxStrategy = CmdStrategyBeans.getStrategyBean(strategy, User.get(CLIENT).getCurrency());

        bannersRule = new TextBannersRule()
                .withUlogin(CLIENT);

        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule).as(CLIENT);
    }

    @Parameterized.Parameters(name = "Стратегия: {0}")
    public static Collection testData() {
        return StrategyTestData.getTextCampStrategiesList();
    }

    @Before
    public void setUp() {
        MetrikaGoals goals = new MetrikaGoals();
        int goalId = goals.getNext();
        if (ajaxStrategy.getNet().getGoalId() != null || ajaxStrategy.getSearch().getGoalId() != null) {
            TestEnvironment.newDbSteps().useShardForLogin(CLIENT).campMetrikaGoalsSteps().
                    addOrUpdateMetrikaGoals(bannersRule.getCampaignId(), Long.valueOf(goalId), 50L, 50L);
        }
        if (ajaxStrategy.getNet().getGoalId() != null) {
            ajaxStrategy.getNet().setGoalId(String.valueOf(goalId));
        }
        if (ajaxStrategy.getSearch().getGoalId() != null) {
            ajaxStrategy.getSearch().setGoalId(String.valueOf(goalId));
        }
        SaveCampRequest request = bannersRule.getSaveCampRequest();
        request.setCid(bannersRule.getCampaignId().toString());
        if (ajaxStrategy.getIsSearchStop().equals("1")) {
            request.setBroad_match_flag("0");
        }
        request.setJsonStrategy(ajaxStrategy);
        request.setGeo(Geo.RUSSIA.getGeo());
        cmdRule.cmdSteps().campaignSteps().postSaveCamp(request);

        if ("".equals(ajaxStrategy.getSearch().getBid())) {
            ajaxStrategy.getSearch().setBid(null);
        }
        if ("".equals(ajaxStrategy.getSearch().getAvgBid())) {
            ajaxStrategy.getSearch().setAvgBid(null);
        }

        if ("".equals(ajaxStrategy.getNet().getBid())) {
            ajaxStrategy.getNet().setBid(null);
        }
        if ("".equals(ajaxStrategy.getNet().getAvgBid())) {
            ajaxStrategy.getNet().setAvgBid(null);
        }
    }

    @Test
    @Description("Проверяем стратегию кампании в ответе контроллера editCamp")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10337")
    public void checkCampaignStrategyBlock() {

        EditCampResponse editCampResponse =
                cmdRule.cmdSteps().campaignSteps().getEditCamp(bannersRule.getCampaignId(), CLIENT);


        assertThat("Блок strategy в ответе контроллера совпадает с ожидаемым",
                editCampResponse.getCampaign().getStrategy(),
                beanDiffer(ajaxStrategy));
    }

}
