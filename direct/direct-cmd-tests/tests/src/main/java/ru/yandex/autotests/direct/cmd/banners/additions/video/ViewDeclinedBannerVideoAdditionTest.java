package ru.yandex.autotests.direct.cmd.banners.additions.video;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.qatools.allure.annotations.TestCaseId;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.data.commons.StatusModerate;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.rules.VideoAdditionCreativeRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersPerformanceStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.ModReasonsType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BannersPerformanceRecord;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.direct.cmd.data.banners.BannerStatusTexts.VIDEO_ADDITION_DECLINED;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;


@Aqua.Test
@Description("Просмотр баннеров с отклоненными видеодополнениями")
@Stories(TestFeatures.Banners.VIDEO_ADDITION)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SHOW_CAMP)
@Tag(CmdTag.GET_AD_GROUP)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.VIDEO_ADDITION)
@Tag(TrunkTag.YES)
@Tag("DIRECT-63700")
public class ViewDeclinedBannerVideoAdditionTest {
    private static final String CLIENT = "at-direct-video-addition-1";
    private static final DirectJooqDbSteps dbSteps = TestEnvironment.newDbSteps(CLIENT);

    @ClassRule
    public static DirectCmdRule classRule = DirectCmdRule.defaultClassRule();

    private VideoAdditionCreativeRule videoAdditionCreativeRule = new VideoAdditionCreativeRule(CLIENT);
    private BannersRule bannerWithVideoRule =
            new TextBannersRule()
                    .withVideoAddition(videoAdditionCreativeRule)
                    .withUlogin(CLIENT);

    @Rule
    public DirectCmdRule classRules = DirectCmdRule.defaultRule()
            .withRules(videoAdditionCreativeRule, bannerWithVideoRule);

    @Before
    public void beforeClass() {
        BsSyncedHelper.makeCampSynced(classRules, bannerWithVideoRule.getCampaignId());

        BannersPerformanceRecord bannersPerformanceRecord =
                dbSteps.bannersPerformanceSteps().getBannersPerformance(bannerWithVideoRule.getBannerId(),
                        videoAdditionCreativeRule.getCreativeId());
        dbSteps.bannersPerformanceSteps().setCreativeStatusModerate(
                bannerWithVideoRule.getCampaignId(),
                bannerWithVideoRule.getGroupId(),
                bannerWithVideoRule.getBannerId(),
                BannersPerformanceStatusmoderate.No
        );
        dbSteps.showDiagSteps().createSomeModReasonsRecord(ModReasonsType.video_addition,
                bannersPerformanceRecord.getBannerCreativeId());
    }

    @Test
    @Description("При просмотре кампании с отклоненным видеодополнением есть предупреждение, флаг declined_show и status_moderate = No")
    @TestCaseId("10941")
    public void testShowCampViewDeclinedVideoAddition() {
        Banner showCampBanner = classRules.cmdSteps().campaignSteps().getShowCamp(
                CLIENT,
                bannerWithVideoRule.getCampaignId().toString()
        ).getGroups().get(0);

        checkBanner(showCampBanner);
    }

    @Test
    @Description("При просмотре группы с отклоненным видеодополнением есть предупреждение, флаг declined_show и status_moderate = No")
    @TestCaseId("10942")
    public void testGetAdGroupViewDeclinedVideoAddition() {
        Banner getAdGroupBanner = classRules.cmdSteps().groupsSteps().getAdGroup(
                CLIENT,
                bannerWithVideoRule.getGroupId()
        ).getBanners().get(0);

        checkBanner(getAdGroupBanner);
    }

    private void checkBanner(Banner banner) {
        assertThat("в статусе есть предупреждение об отклонении видеодополнения", banner.getStatus(),
                equalTo(VIDEO_ADDITION_DECLINED.toString())
        );

        assertThat("выставлен флаг для показа предупреждения", banner.getDeclinedShow(),
                equalTo(1)
        );

        assumeThat("video_resources присутствует", banner.getVideoResources(), notNullValue());
        assertThat("у видеодополнения выставлен статус модерации No",
                banner.getVideoResources().getStatusModerate(),
                equalTo(StatusModerate.NO)
        );
    }
}
