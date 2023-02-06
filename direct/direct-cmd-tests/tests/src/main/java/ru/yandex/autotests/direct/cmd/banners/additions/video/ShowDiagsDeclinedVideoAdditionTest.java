package ru.yandex.autotests.direct.cmd.banners.additions.video;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.qatools.allure.annotations.TestCaseId;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.data.showdiag.ShowDiagResponse;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;


@Aqua.Test
@Description("Просмотр причин отклонения видеодополнения")
@Stories(TestFeatures.Banners.VIDEO_ADDITION)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SHOW_DIAG)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.VIDEO_ADDITION)
@Tag(TrunkTag.YES)
@Tag("DIRECT-63700")
public class ShowDiagsDeclinedVideoAdditionTest {
    private static final String CLIENT = "at-direct-video-addition-1";
    private static final DirectJooqDbSteps dbSteps = TestEnvironment.newDbSteps(CLIENT);

    @ClassRule
    public static final DirectCmdRule classRule = DirectCmdRule.defaultClassRule();

    private VideoAdditionCreativeRule videoAdditionCreativeRule = new VideoAdditionCreativeRule(CLIENT);
    private BannersRule bannerWithVideoRule =
            new TextBannersRule()
                    .withVideoAddition(videoAdditionCreativeRule)
                    .withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule()
            .withRules(videoAdditionCreativeRule, bannerWithVideoRule);

    @Before
    public void before() {
        BsSyncedHelper.makeCampSynced(cmdRule, bannerWithVideoRule.getCampaignId());

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
    @Description("При просмотре причин отклонения у баннера с отклоненным видеодополнением, получаем причины отклонения видеодополнения")
    @TestCaseId("10934")
    public void testShowDiagResponseDeclinedVideoAddition() {
        ShowDiagResponse showDiagResponse =
                cmdRule.cmdSteps().showDiagSteps().getShowDiag(bannerWithVideoRule.getBannerId().toString());

        assertThat("есть причина отклонения видеодополнения", showDiagResponse.getBannerDiags().getVideoAdditionDiags(),
                notNullValue());

        assertThat("найдена 1 причина отклонения", showDiagResponse.getBannerDiags().getVideoAdditionDiags(),
                hasSize(1));
    }
}
