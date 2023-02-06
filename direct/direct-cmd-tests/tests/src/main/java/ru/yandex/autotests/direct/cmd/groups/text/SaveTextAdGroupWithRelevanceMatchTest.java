package ru.yandex.autotests.direct.cmd.groups.text;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

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
import ru.yandex.autotests.direct.cmd.steps.groups.GroupHelper;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.updateshowconditions.AjaxUpdateShowConditionsHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static ru.yandex.autotests.direct.cmd.data.groups.GroupsFactory.getDefaultRelevanceMatch;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Проверка сохранения бесфразного таргетинга (relevance_match) в группах")
@Stories(TestFeatures.Groups.BANNER_MULTI_SAVE)
@Features(TestFeatures.GROUPS)
@Tag(ObjectTag.GROUP)
@Tag(TestFeatures.Groups.BANNER_MULTI_SAVE)
@Tag(TestFeatures.GROUPS)
@RunWith(Parameterized.class)
public class SaveTextAdGroupWithRelevanceMatchTest {
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected static String CLIENT = Logins.CLIENT_WITH_RELEVANCE_MATCH;

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

    public SaveTextAdGroupWithRelevanceMatchTest(CampaignTypeEnum campaignTypeEnum) {
        this.bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignTypeEnum)
                .overrideGroupTemplate(new Group().withRelevanceMatch(
                        Collections.singletonList(getDefaultRelevanceMatch())))
                .withUlogin(CLIENT);

        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    private Group initialGroup;

    @Before
    public void before() {
        initialGroup = bannersRule.getCurrentGroup();
        assumeThat("в группе появился безфразный таргетинг", initialGroup.getRelevanceMatch(),
                notNullValue());
        AjaxUpdateShowConditionsObjects showConditions = new AjaxUpdateShowConditionsObjects().withEdited(
                singletonMap(bannersRule.getCurrentGroup().getRelevanceMatch().get(0).getBidId().toString(),
                        new AjaxUpdateShowConditions()
                                .withPrice(("0.78"))));
        AjaxUpdateShowConditionsHelper.updateShowConditions(showConditions, CLIENT, cmdRule, bannersRule);
        initialGroup = bannersRule.getCurrentGroup();
    }

    @Test
    @Description("Создание безфразного таргетинга (cmd = saveAdGroups)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10908")
    public void testAddRelevanceMatchAtSaveAdGroups() {
        RelevanceMatch expectedRelevanceMatch = new RelevanceMatch().withPrice(0.78d);
        assertThat("значения полей нового безфразного таргетинга соответствуют ожидаемым",
                initialGroup.getRelevanceMatch().get(0), beanDiffer(expectedRelevanceMatch)
                        .useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    @Description("Редактирование цены безфразного таргетинга (cmd = saveAdGroups)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10909")
    public void testEditRelevanceMatchPriceAtSaveAdGroups() {
        Double expectedPrice = 0.88d;
        initialGroup.getRelevanceMatch().get(0).withPrice(expectedPrice);
        Group actualGroup = GroupHelper.saveAdGroup(cmdRule, CLIENT, bannersRule.getCampaignId(),
                initialGroup,
                bannersRule.getMediaType());

        assumeThat("в группе есть безфразный таргетинг", actualGroup.getRelevanceMatch(), notNullValue());
        assertThat("цена изменилась", actualGroup.getRelevanceMatch().get(0).getPrice(), equalTo(expectedPrice));
    }

    @Test
    @Description("Удаление безфразного таргетинга (cmd = saveAdGroups)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10910")
    public void testDeleteRelevanceMatchAtSaveAdGroups() {
        Group actualGroup = GroupHelper.saveAdGroup(cmdRule, CLIENT, bannersRule.getCampaignId(),
                initialGroup.withRelevanceMatch(emptyList()),
                bannersRule.getMediaType());
        assertThat("безфразный таргетинг был удален", actualGroup.getRelevanceMatch().size(), equalTo(0));
    }

    @Test
    @Description("Повторное добавление безфразного таргетинга в группу после удаления (cmd = saveAdGroups)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10911")
    public void testReAddingRelevanceMatchAtSaveAdGroups() {
        Long oldBidId = initialGroup.getRelevanceMatch().get(0).getBidId();

        Group groupAfterDeleteRM = GroupHelper.saveAdGroup(cmdRule, CLIENT, bannersRule.getCampaignId(),
                initialGroup.withRelevanceMatch(emptyList()),
                bannersRule.getMediaType());
        assumeThat("безфразный таргетинг был удален", groupAfterDeleteRM.getRelevanceMatch().size(), equalTo(0));

        Group groupAfterReAddRM = GroupHelper.saveAdGroup(cmdRule, CLIENT, bannersRule.getCampaignId(),
                groupAfterDeleteRM.withRelevanceMatch(
                        Collections.singletonList(getDefaultRelevanceMatch())),
                bannersRule.getMediaType());

        assertThat("bid_id остался прежним", groupAfterReAddRM.getRelevanceMatch().get(0).getBidId(),
                equalTo(oldBidId));
    }

}
