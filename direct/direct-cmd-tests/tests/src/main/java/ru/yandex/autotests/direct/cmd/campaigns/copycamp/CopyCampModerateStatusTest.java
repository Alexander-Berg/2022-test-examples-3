package ru.yandex.autotests.direct.cmd.campaigns.copycamp;

import java.util.Arrays;
import java.util.Collection;

import org.hamcrest.core.AnyOf;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.Campaign;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatuspostmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.CampaignsStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.PhrasesStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.PhrasesStatuspostmoderate;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка копирования статусов модерации кампаний контроллером copyCamp")
@Stories(TestFeatures.Campaigns.COPY_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.COPY_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(TestFeatures.Campaigns.COPY_CAMP)
@Tag(TestFeatures.CAMPAIGNS)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@Tag(CampTypeTag.DYNAMIC)
@RunWith(Parameterized.class)
public class CopyCampModerateStatusTest {

    private static final String CLIENT = "at-direct-mod-camp";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule;

    @Rule
    public DirectCmdRule cmdRule;

    private Long newCid;
    private CampaignTypeEnum campaignType;

    @Parameterized.Parameters(name = "Проверка сброса в Ready статуса модерации при копировании кампании. Тип " +
            "кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE},
                {CampaignTypeEnum.DTO},
        });
    }

    public CopyCampModerateStatusTest(CampaignTypeEnum campaignType) {
        this.campaignType = campaignType;
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType).withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Before
    public void before() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).campaignsSteps()
                .setCampaignsStatusModerate(bannersRule.getCampaignId(), CampaignsStatusmoderate.Sent);
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).adGroupsSteps()
                .setPhrasesStatusModerate(bannersRule.getGroupId(), PhrasesStatusmoderate.Sending,
                        PhrasesStatuspostmoderate.Sent);
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersSteps()
                .setBannerStatusModerate(bannersRule.getBannerId(), BannersStatusmoderate.Sending,
                        BannersStatuspostmoderate.Sent);
        newCid = cmdRule.cmdSteps().copyCampSteps()
                .copyCamp(CLIENT, CLIENT, bannersRule.getCampaignId(), "copy_moderate_status");
    }

    @After
    public void after() {
        if (newCid != null) {
            cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(CLIENT, newCid);
        }
    }

    @Test
    @Description("Проверка сброса в Ready статуса модерации на кампании при копировании кампании")
    @TestCaseId("11034")
    public void copyCampCampaign() {
        Campaign actualCampaign = cmdRule.cmdSteps().campaignSteps().getCampaign(CLIENT, newCid);
        AnyOf<String> statusModerateMatcher = anyOf(equalTo(CampaignsStatusmoderate.Ready.toString()),
                equalTo(CampaignsStatusmoderate.Sent.toString()));
        assertThat("статус модерации на кампании сбросился в Ready", actualCampaign.getStatusModerate(),
                statusModerateMatcher);
    }

    @Test
    @Description("Проверка сброса в Ready статуса модерации и постмодерации на группе при копировании кампании")
    @TestCaseId("11035")
    public void copyCampAdGroup() {
        Group actualGroup = cmdRule.cmdSteps().groupsSteps().getGroups(CLIENT, newCid).get(0);
        assertThat("статус модерации сбросился в Ready", actualGroup.getStatusModerate(),
                anyOf(equalTo(PhrasesStatusmoderate.Ready.toString()),
                        equalTo(PhrasesStatusmoderate.Sending.toString())));
        assertThat("статус постмодерации сбросился в Ready", actualGroup.getStatusPostModerate(),
                equalTo(PhrasesStatuspostmoderate.Ready.toString()));
    }

    @Test
    @Description("Проверка сброса в Ready статуса модерации на баннере при копировании кампании")
    @TestCaseId("11033")
    public void copyCampBanner() {
        Banner actualBanner = cmdRule.cmdSteps().groupsSteps().getBanners(CLIENT, newCid).get(0);
        AnyOf<String> statusModerateMatcher = anyOf(equalTo(PhrasesStatusmoderate.Ready.toString()),
                equalTo(PhrasesStatusmoderate.Sending.toString()),
                equalTo(PhrasesStatusmoderate.Sent.toString()));

        assertThat("статус модерации сбросился в Ready", actualBanner.getStatusModerate(),
                statusModerateMatcher);
        assertThat("статус постмодерации сбросился в Ready", actualBanner.getStatusPostModerate(),
                equalTo(BannersStatuspostmoderate.Ready.toString()));
    }
}
