package ru.yandex.autotests.direct.cmd.campaigns.copycamp;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditions;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsObjects;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.interest.RelevanceMatch;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.updateshowconditions.AjaxUpdateShowConditionsHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static ru.yandex.autotests.direct.cmd.data.groups.GroupsFactory.getDefaultRelevanceMatch;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Проверка копирования кампании с группой с бесфразным таргетингом (relevance_match)")
@Stories(TestFeatures.Campaigns.COPY_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(ObjectTag.GROUP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(TestFeatures.Campaigns.COPY_CAMP)
@Tag(TestFeatures.CAMPAIGNS)
@RunWith(Parameterized.class)
public class CopyCampWithRelevanceMatchTest {
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private static final String CLIENT = Logins.CLIENT_WITH_RELEVANCE_MATCH;
    private RelevanceMatch relevanceMatch = getDefaultRelevanceMatch().withIsSuspended("0");

    @Parameterized.Parameters(name = "Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE},
        });
    }

    private BannersRule bannersRule;
    private Long newCid;

    @Rule
    public DirectCmdRule cmdRule;

    @After
    public void deleteCampaignCopy() {
        if (newCid != null) {
            cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(CLIENT, newCid);
        }
    }

    public CopyCampWithRelevanceMatchTest(CampaignTypeEnum campaignTypeEnum) {
        this.bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignTypeEnum)
                .overrideGroupTemplate(new Group().withRelevanceMatch(
                        Collections.singletonList(getDefaultRelevanceMatch())))
                .withUlogin(CLIENT);

        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Before
    public void before() {
        assumeThat("в группе появился безфразный таргетинг",
                bannersRule.getCurrentGroup().getRelevanceMatch(),
                notNullValue());
        assumeThat("в группе появился безфразный таргетинг",
                bannersRule.getCurrentGroup().getRelevanceMatch(),
                not(empty()));
        AjaxUpdateShowConditionsObjects showConditions = new AjaxUpdateShowConditionsObjects().withEdited(
                singletonMap(bannersRule.getCurrentGroup().getRelevanceMatch().get(0).getBidId().toString(),
                        new AjaxUpdateShowConditions().withPrice(relevanceMatch.getPrice().toString())));
        AjaxUpdateShowConditionsHelper.updateShowConditions(showConditions, CLIENT, cmdRule, bannersRule);
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10900")
    public void copyCamp() {
        newCid = cmdRule.cmdSteps().copyCampSteps().copyCampWithinClient(CLIENT, bannersRule.getCampaignId());
        Group currentGroup = cmdRule.cmdSteps().groupsSteps().getGroups(CLIENT, newCid).get(0);
        assumeThat("в скопированной группе появился безфразный таргетинг",
                currentGroup.getRelevanceMatch(),
                not(empty()));
        assertThat("у скопированной кампании включен бф",
                currentGroup.getRelevanceMatch().get(0),
                beanDiffer(relevanceMatch.withBidId(null).withIsSuspended(""))
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

}
