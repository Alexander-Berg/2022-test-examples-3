package ru.yandex.autotests.direct.cmd.groups.text;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.common.CreativeBanner;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.groups.CopyGroupStatusModerateBaseTest;
import ru.yandex.autotests.direct.cmd.rules.CreativeBannerRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersPerformanceStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatuspostmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BannersPerformanceRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BannersRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Статус модерации группы с canvas креативом при копированиии")
@Stories(TestFeatures.Banners.CANVAS_BANNERS_PARAMETERS)
@Features(TestFeatures.GROUPS)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.TEXT)
public class CopyTextGroupWithCanvasCreativeModerationStatusTest extends CopyGroupStatusModerateBaseTest {
    private BannersPerformanceStatusmoderate creativeModerateStatus = BannersPerformanceStatusmoderate.Yes;

    private BannersStatusmoderate bannerModerateStatus = BannersStatusmoderate.Yes;

    private BannersStatuspostmoderate bannerPostModerateStatus = BannersStatuspostmoderate.Yes;

    public CopyTextGroupWithCanvasCreativeModerationStatusTest() {
        bannerRule = new CreativeBannerRule(CampaignTypeEnum.TEXT).withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannerRule);
    }


    public void before() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersSteps().setBannerStatusModerate(
                bannerRule.getBannerId(), BannersStatusmoderate.valueOf(bannerModerateStatus.toString())
        );

        TestEnvironment.newDbSteps().bannersPerformanceSteps().setCreativeStatusModerate(bannerRule.getCampaignId(),
                bannerRule.getGroupId(), bannerRule.getBannerId(), creativeModerateStatus
        );

        TestEnvironment.newDbSteps().bannersSteps().setBannerStatusPostModerate(
                bannerRule.getBannerId(), BannersStatuspostmoderate.valueOf(bannerPostModerateStatus.toString())
        );
    }

    @Override
    protected void checkStatus() {
        ShowCampResponse showCamp = cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(CLIENT, bannerRule.getCampaignId().toString());
        Long copiedBannerId = showCamp.getGroups().get(1).getBid();
        Banner actualBanner = cmdRule.cmdSteps().groupsSteps().getBanner(CLIENT, bannerRule.getCampaignId(), copiedBannerId);
        BannersRecord actualBannerBd = TestEnvironment.newDbSteps().bannersSteps().getBanner(copiedBannerId);
        BannersPerformanceRecord actualCreative = TestEnvironment.newDbSteps().bannersPerformanceSteps()
                .getBannersPerformance(copiedBannerId, ((CreativeBannerRule) bannerRule).getCreativeId());

        assumeThat("статус модерации баннера в бд соответсвует ожиданиям",
                actualBannerBd.getStatusmoderate(),
                beanDiffer(BannersStatusmoderate.New));
        assumeThat("статус модерации баннера в бд соответсвует ожиданиям",
                actualCreative.getStatusmoderate(),
                beanDiffer(BannersPerformanceStatusmoderate.New));

        assertThat(
                "статус модерации баннера соответствует ожиданиям",
                actualBanner.getStatusModerate(),
                beanDiffer(BannersPerformanceStatusmoderate.New.toString())
        );
    }

    @Override
    protected void copyGroup() {
        Group group = bannerRule.getGroup();
        group.setCampaignID(bannerRule.getCampaignId().toString());
        group.getBanners().get(0)
                .withCreativeBanner(new CreativeBanner().withCreativeId(((CreativeBannerRule) bannerRule).getCreativeId()));
        group.getBanners().stream().forEach(b -> b.withCid(bannerRule.getCampaignId()));
        GroupsParameters groupRequest = GroupsParameters.forExistingCamp(CLIENT, bannerRule.getCampaignId(), group);
        groupRequest.setIsGroupsCopyAction("1");
        groupRequest.setNewGroup("0");
        bannerRule.saveGroup(groupRequest);
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9853")
    public void copyGroupTest() {
        super.copyGroupTest();
    }
}
