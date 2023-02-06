package ru.yandex.autotests.direct.cmd.campaigns.showcamp;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditions;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsObjects;
import ru.yandex.autotests.direct.cmd.data.campaigns.ShowCampMultiEditResponse;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.interest.RelevanceMatch;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.updateshowconditions.AjaxUpdateShowConditionsHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.direct.cmd.data.groups.GroupsFactory.getDefaultRelevanceMatch;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;


@Aqua.Test
@Description("Проверка наличия безфразного таргетинга на страницах просмотра и редактирования кампании")
@Stories(TestFeatures.Campaigns.SHOW_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
@Tag(CmdTag.SHOW_CAMP)
@Tag(CmdTag.SHOW_CAMP_MULTI_EDIT)
@Tag(CmdTag.GET_AD_GROUP)
@RunWith(Parameterized.class)
public class ShowCampRelevanceMatchTest {

    private static final String CLIENT = Logins.CLIENT_WITH_RELEVANCE_MATCH;

    @Parameterized.Parameters(name = "Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE},
        });
    }

    private BannersRule bannersRule;

    @Rule
    public DirectCmdRule cmdRule;

    public ShowCampRelevanceMatchTest(CampaignTypeEnum campaignTypeEnum) {
        this.bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignTypeEnum)
                .overrideGroupTemplate(new Group().withRelevanceMatch(
                        Collections.singletonList(getDefaultRelevanceMatch())))
                .withUlogin(CLIENT);

        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }
    private RelevanceMatch expectedRelevanceMatch = new RelevanceMatch()
            .withPrice(0.78d).withIsSuspended("");


    @Before
    public void before() {
        AjaxUpdateShowConditionsObjects showConditions = new AjaxUpdateShowConditionsObjects().withEdited(
                singletonMap(bannersRule.getCurrentGroup().getRelevanceMatch().get(0).getBidId().toString(),
                        new AjaxUpdateShowConditions()
                                .withPrice(expectedRelevanceMatch.getPrice().toString())));
        AjaxUpdateShowConditionsHelper.updateShowConditions(showConditions, CLIENT, cmdRule, bannersRule);
    }

    @Test
    @Description("безфразный таргетинг в showCamp")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10902")
    public void showCampRelevanceMatchTest() {
        ShowCampResponse response = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT,
                bannersRule.getCampaignId().toString());
        List<RelevanceMatch> actualRelevanceMatch = response.getGroups().get(0).getRelevanceMatch();

        assumeThat("у баннера есть безфразный таргетинг", actualRelevanceMatch, hasSize(1));
        assertThat("значения полей безфразного таргетинга соответствуют ожидаемым",
                actualRelevanceMatch.get(0),
                beanDiffer(expectedRelevanceMatch).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    @Description("безфразный таргетинг в showCampMultiEdit")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10901")
    public void showCampMultiEditRelevanceMatchTest() {
        ShowCampMultiEditResponse response = cmdRule.cmdSteps().campaignSteps().getShowCampMultiEdit(CLIENT,
                bannersRule.getCampaignId());
        List<RelevanceMatch> actualRelevanceMatch = response.getCampaign().getGroups().get(0).getRelevanceMatch();

        assumeThat("у группы есть безфразный таргетинг", actualRelevanceMatch, hasSize(1));
        assertThat("значения полей безфразного таргетинга соответствуют ожидаемым",
                actualRelevanceMatch.get(0),
                beanDiffer(expectedRelevanceMatch).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    @Description("безфразный таргетинг в getAdGroup")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10903")
    public void getAdGroupRelevanceMatchTest() {
        List<RelevanceMatch> actualRelevanceMatch = cmdRule.cmdSteps().groupsSteps().getAdGroup(CLIENT,
                bannersRule.getGroupId()).getRelevanceMatch();

        assumeThat("у группы есть безфразный таргетинг", actualRelevanceMatch, hasSize(1));
        assertThat("значения полей безфразного таргетинга соответствуют ожидаемым",
                actualRelevanceMatch.get(0),
                beanDiffer(expectedRelevanceMatch).useCompareStrategy(onlyExpectedFields()));
    }
}
