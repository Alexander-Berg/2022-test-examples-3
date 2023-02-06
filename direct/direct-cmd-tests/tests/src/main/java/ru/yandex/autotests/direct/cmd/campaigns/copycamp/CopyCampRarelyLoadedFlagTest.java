package ru.yandex.autotests.direct.cmd.campaigns.copycamp;

import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.direct.cmd.util.CampaignHelper.deleteAdGroupMobileContent;
import static ru.yandex.autotests.direct.cmd.util.PerformanceCampaignHelper.runDeleteCampaignScriptAndIgnoreResult;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Features(TestFeatures.Groups.RARELY_LOADED_FLAG)
@Stories(TestFeatures.GROUPS)
@Description("Проверка сброса флага мало показов при копировании кампании")
@Tag(CmdTag.SHOW_CAMP)
@Tag(CmdTag.COPY_CAMP)
@Tag(CampTypeTag.TEXT)
@Tag(ObjectTag.CAMPAIGN)
@Tag(ObjectTag.GROUP)
@RunWith(Parameterized.class)
public class CopyCampRarelyLoadedFlagTest {

    private static final String CLIENT = "at-direct-backend-c";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule;

    @Rule
    public DirectCmdRule cmdRule;

    private Long newCid;
    private CampaignTypeEnum campaignType;

    @Parameterized.Parameters(name = "Проверка сброса флага мало показов при копировании кампании. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE},
                {CampaignTypeEnum.DTO},
                {CampaignTypeEnum.DMO},
        });
    }

    public CopyCampRarelyLoadedFlagTest(CampaignTypeEnum campaignType) {
        this.campaignType = campaignType;
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType).withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Before
    public void before() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).adGroupsSteps()
                .setBsRarelyLoaded(bannersRule.getGroupId(), true);
    }

    @After
    public void after() {
        if (newCid != null) {
            if (campaignType == CampaignTypeEnum.DMO) {
                cmdRule.cmdSteps().campaignSteps().deleteCampaign(CLIENT, newCid);
                runDeleteCampaignScriptAndIgnoreResult(cmdRule, Long.parseLong(User.get(CLIENT).getClientID()), newCid);
            } else {
                if (campaignType == CampaignTypeEnum.MOBILE) {
                    deleteAdGroupMobileContent(newCid, CLIENT);
                }
                cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(CLIENT, newCid);
            }
        }

    }

    @Test
    @Description("Проверка сброса флага мало показов при копировании кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10678")
    public void copyCampRarelyLoadedTest() {
        newCid = cmdRule.cmdSteps().copyCampSteps().copyCampWithinClient(CLIENT, bannersRule.getCampaignId());

        Banner actualGroup = cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(CLIENT, newCid.toString()).getGroups().get(0);

        assertThat("флаг мало показов сбросился", actualGroup.getIsBsRarelyLoaded(),
                equalTo(0));
    }

}
