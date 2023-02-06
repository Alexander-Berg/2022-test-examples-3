package ru.yandex.autotests.direct.cmd.groups.text;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

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
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.updateshowconditions.AjaxUpdateShowConditionsHelper;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.PhrasesStatusbssynced;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.autotests.direct.cmd.data.groups.GroupsFactory.getDefaultRelevanceMatch;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка статуса bsSynced у бфт (relevance_match)")
@Stories(TestFeatures.Groups.BANNER_MULTI_SAVE)
@Features(TestFeatures.GROUPS)
@Tag(ObjectTag.GROUP)
@Tag(TestFeatures.Groups.BANNER_MULTI_SAVE)
@Tag(TestFeatures.GROUPS)
@RunWith(Parameterized.class)
public class SaveTextAdGroupsWithRelevanceMatchBsSyncTest {
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected static final String CLIENT = Logins.CLIENT_WITH_RELEVANCE_MATCH;

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

    public SaveTextAdGroupsWithRelevanceMatchBsSyncTest(CampaignTypeEnum campaignTypeEnum) {
        this.bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignTypeEnum)
                .overrideGroupTemplate(new Group().withRelevanceMatch(
                        Collections.singletonList(getDefaultRelevanceMatch())))
                .withUlogin(CLIENT);

        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Test
    @Description("При удалении бфт на странице кампании статус синхронизации группы сбрасывается")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10912")
    public void deleteRelevanceMatchAtAjaxUpdateShowConditions() {
        syncGroupAndRepelevanceMatch();
        makeAllModerate();

        AjaxUpdateShowConditionsObjects showConditions = new AjaxUpdateShowConditionsObjects().withDeleted(
                String.valueOf(bannersRule.getCurrentGroup().getRelevanceMatch().get(0).getBidId())
        );
        AjaxUpdateShowConditionsHelper.updateShowConditions(showConditions, CLIENT, cmdRule, bannersRule);
        Group actualGroup = bannersRule.getCurrentGroup();

        assertThat("Статус синхронизации группы соответсвует ожиданиям",
                actualGroup.getStatusBsSynced(),
                equalTo("No"));
    }

    @Test
    @Description("При изменении бфт на странице кампании статус синхронизации группы сбрасывается")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10916")
    public void changeRelevanceMatchAtAjaxUpdateShowConditions() {
        syncGroupAndRepelevanceMatch();
        makeAllModerate();
        AjaxUpdateShowConditionsObjects showConditions = new AjaxUpdateShowConditionsObjects().withEdited(
                String.valueOf(bannersRule.getCurrentGroup().getRelevanceMatch().get(0).getBidId()),
                new AjaxUpdateShowConditions().withIsSuspended("1")
        );

        AjaxUpdateShowConditionsHelper.updateShowConditions(showConditions, CLIENT, cmdRule, bannersRule);
        Group actualGroup = bannersRule.getCurrentGroup();

        assertThat("Статус синхронизации группы соответсвует ожиданиям",
                actualGroup.getStatusBsSynced(),
                equalTo("No"));
    }

    private void syncGroupAndRepelevanceMatch() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).adGroupsSteps()
                .setPhrasesStatusBsSynced(bannersRule.getGroupId(), PhrasesStatusbssynced.Yes);
    }
    private void makeAllModerate() {
        cmdRule.darkSideSteps().getCampaignFakeSteps().makeCampaignFullyModerated(bannersRule.getCampaignId());
        cmdRule.darkSideSteps().getGroupsFakeSteps().makeGroupFullyModerated(bannersRule.getGroupId());
        cmdRule.darkSideSteps().getBannersFakeSteps().makeBannerFullyModerated(bannersRule.getBannerId());
    }
}
