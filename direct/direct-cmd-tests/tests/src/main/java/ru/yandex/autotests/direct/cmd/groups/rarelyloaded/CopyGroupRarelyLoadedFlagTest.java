package ru.yandex.autotests.direct.cmd.groups.rarelyloaded;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.irt.testutils.allure.AssumptionException;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Features(TestFeatures.Groups.RARELY_LOADED_FLAG)
@Stories(TestFeatures.GROUPS)
@Description("Проверка сброса флага мало показов при копировании группы")
@Tag(CmdTag.SHOW_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(ObjectTag.GROUP)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class CopyGroupRarelyLoadedFlagTest {

    private static final String CLIENT = "at-direct-backend-c";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule;

    @Rule
    public DirectCmdRule cmdRule;

    @Parameterized.Parameters(name = "Проверка сброса флага мало показов при копировании группы. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE},
                {CampaignTypeEnum.DTO},
                {CampaignTypeEnum.DMO},
        });
    }

    public CopyGroupRarelyLoadedFlagTest(CampaignTypeEnum campaignType) {
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType).withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Before
    public void before() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).adGroupsSteps()
                .setBsRarelyLoaded(bannersRule.getGroupId(), true);
    }

    @Test
    @Description("Проверка сброса флага мало показов при копировании группы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10685")
    public void copyGroupRarelyLoadedTest() {
        copyGroup();
        Banner actualGroup = cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(CLIENT, bannersRule.getCampaignId().toString()).getGroups()
                .stream().filter(g -> !g.getAdGroupId().equals(bannersRule.getGroupId()))
                .findFirst()
                .orElseThrow(() -> new AssumptionException("Ожидалось что в кампании есть вторая группа"));

        assertThat("флаг мало показов сбросился", actualGroup.getIsBsRarelyLoaded(),
                equalTo(0));
    }

    private void copyGroup() {
        Group group = bannersRule.getGroup()
                .withAdGroupID(bannersRule.getGroupId().toString())
                .withCampaignID(bannersRule.getCampaignId().toString());
        group.getBanners().forEach(b -> b.withCid(bannersRule.getCampaignId()));
        GroupsParameters groupRequest = GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(), group);
        groupRequest.setIsGroupsCopyAction("1");
        groupRequest.setNewGroup("0");
        bannersRule.saveGroup(groupRequest);
    }

}
