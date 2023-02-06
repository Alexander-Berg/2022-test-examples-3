package ru.yandex.autotests.direct.cmd.banners.additions.video;

import java.util.Map;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.rules.VideoAdditionCreativeRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersPerformanceStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatusbssynced;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.ModReasonsType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BannersPerformanceRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BannersRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.ModReasonsRecord;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.direct.cmd.data.banners.BannerStatusTexts.VIDEO_ADDITION_WAIT_MODERATION;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Изменение видеодополнения у баннера")
@Stories(TestFeatures.Banners.VIDEO_ADDITION)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.VIDEO_ADDITION)
@Tag(TrunkTag.YES)
@Tag("DIRECT-63700")
public class ChangeBannerVideoAdditionTest {
    private static final String CLIENT = "at-direct-video-addition-1";
    private static final DirectJooqDbSteps dbSteps = TestEnvironment.newDbSteps(CLIENT);

    @ClassRule
    public static DirectCmdRule classRule = DirectCmdRule.defaultClassRule();

    private VideoAdditionCreativeRule oldVideoAdditionRule = new VideoAdditionCreativeRule(CLIENT);
    private BannersRule bannerWithVideoRule =
            new TextBannersRule().withVideoAddition(oldVideoAdditionRule).withUlogin(CLIENT);
    private VideoAdditionCreativeRule newVideoAdditionRule = new VideoAdditionCreativeRule(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule()
            .withRules(oldVideoAdditionRule, bannerWithVideoRule, newVideoAdditionRule);

    @Before
    public void before() {
        BsSyncedHelper.makeCampSynced(cmdRule, bannerWithVideoRule.getCampaignId());

        BannersPerformanceRecord oldBannersPerformanceRecord = dbSteps.bannersPerformanceSteps()
                .getBannersPerformance(bannerWithVideoRule.getBannerId(), oldVideoAdditionRule.getCreativeId());
        dbSteps.bannersPerformanceSteps().setCreativeStatusModerate(
                bannerWithVideoRule.getCampaignId(),
                bannerWithVideoRule.getGroupId(),
                bannerWithVideoRule.getBannerId(),
                BannersPerformanceStatusmoderate.No
        );
        dbSteps.showDiagSteps().createSomeModReasonsRecord(ModReasonsType.video_addition,
                oldBannersPerformanceRecord.getBannerCreativeId());

        bannerWithVideoRule.updateCurrentGroupBy(g -> {
                    g.getBanners().get(0).addDefaultVideoAddition(newVideoAdditionRule.getCreativeId());
                    return g;
                }
        );
    }

    @Test
    @Description("При изменении видеодополнения у баннера пропадает старое видеодополнение, появляется новое со статусом модерации Ready")
    @TestCaseId("10925")
    public void testVideoAdditionChangeBannerVideoAddition() {
        BannersPerformanceRecord oldBannersPerformanceRecord = dbSteps.bannersPerformanceSteps()
                .getBannersPerformance(bannerWithVideoRule.getBannerId(), oldVideoAdditionRule.getCreativeId());
        assertThat("у баннера нет старого видеодополнения", oldBannersPerformanceRecord, nullValue());

        BannersPerformanceRecord newBannersPerformanceRecord = dbSteps.bannersPerformanceSteps()
                .getBannersPerformance(bannerWithVideoRule.getBannerId(), newVideoAdditionRule.getCreativeId());
        assertThat("у баннера появилось новое видеодополнение", newBannersPerformanceRecord, notNullValue());

        assertThat("у нового видео дополнения статус модерации Ready", newBannersPerformanceRecord.getStatusmoderate(),
                equalTo(BannersPerformanceStatusmoderate.Ready));
    }

    @Test
    @Description("При изменении видеодополнения у баннера не сбрасывается статус модерации, но сбрасывается statusBsSynced")
    @TestCaseId("10926")
    public void testBannerStatusChangeBannerVideoAddition() {
        Banner showCampBanner = cmdRule.cmdSteps().campaignSteps().getShowCamp(
                CLIENT,
                bannerWithVideoRule.getCampaignId().toString()
        ).getGroups().get(0);

        assertThat("у баннера в статус \"ожидает модерации\"", showCampBanner.getStatus(),
                equalTo(VIDEO_ADDITION_WAIT_MODERATION.toString()));
    }

    @Test
    @Description("При изменении видеодополнения у баннера не сбрасывается статус модерации, но сбрасывается statusBsSynced")
    @TestCaseId("10926")
    public void testBannerStatusesChangeBannerVideoAddition() {
        Map<String, Object> bannerRecordMap =
                dbSteps.bannersSteps().getBanner(bannerWithVideoRule.getBannerId()).intoMap();
        Map<String, Object> expectedBannerRecordMap = new BannersRecord()
                .setStatusmoderate(BannersStatusmoderate.Yes)
                .setStatusbssynced(BannersStatusbssynced.No)
                .intoMap();

        assertThat("у баннера не сбросился статус модерации", bannerRecordMap,
                beanDiffer(expectedBannerRecordMap).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    @Test
    @Ignore("решили отложить пока")
    @Description("При изменении видеодополнения баннера у нового видеодополнения нет причин отклонения")
    @TestCaseId("10927")
    public void testModReasonsCleanedChangeBannerVideoAddition() {
        BannersPerformanceRecord newBannersPerformanceRecord = dbSteps.bannersPerformanceSteps()
                .getBannersPerformance(bannerWithVideoRule.getBannerId(), newVideoAdditionRule.getCreativeId());
        assumeThat("у баннера найдено новое видеодополнение", newBannersPerformanceRecord, notNullValue());

        ModReasonsRecord modReasonsRecord =
                dbSteps.moderationSteps().getModReasonsRecord(newBannersPerformanceRecord.getBannerCreativeId(),
                        ModReasonsType.video_addition);

        assertThat("у нового видеодополнения нет причин отклонения", modReasonsRecord, nullValue());
    }
}
