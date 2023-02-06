package ru.yandex.autotests.direct.cmd.banners.additions.video;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Сохранение баннера с видеодополнением без изменений")
@Stories(TestFeatures.Banners.VIDEO_ADDITION)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.VIDEO_ADDITION)
@Tag(TrunkTag.YES)
@Tag("DIRECT-63700")
public class NoChangeBannerVideoAdditionTest {
    private static final String CLIENT = "at-direct-video-addition-1";
    private static DirectJooqDbSteps dbSteps = TestEnvironment.newDbSteps(CLIENT);

    @ClassRule
    public static DirectCmdRule classRule = DirectCmdRule.defaultClassRule();

    private VideoAdditionCreativeRule oldVideoAdditionRule = new VideoAdditionCreativeRule(CLIENT);
    private BannersRule bannerWithVideoRule =
            new TextBannersRule()
                    .withVideoAddition(oldVideoAdditionRule)
                    .withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule()
            .withRules(oldVideoAdditionRule, bannerWithVideoRule);

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

        bannerWithVideoRule.updateCurrentGroupBy(g -> g);
    }

    @Test
    @Description("При сохранении баннера с видеодополнением без изменений у баннера остается старое видеодополнение"
            + ", сохраняется его статус модерации и причины отклонения")
    @TestCaseId("10931")
    public void testOldVideoAdditionNoChangeBannerVideoAddition() {
        List<BannersPerformanceRecord> additionsList =
                dbSteps.bannersPerformanceSteps().findBannersPerformance(bannerWithVideoRule.getBannerId());
        assertThat("есть только одно видеодополнение", additionsList, hasSize(1));
        Map<String, Object> actualBannersPerformanceMap = additionsList.get(0).intoMap();
        Map<String, Object> expectedBannersPerformanceMap = new BannersPerformanceRecord()
                .setBid(bannerWithVideoRule.getBannerId())
                .setCreativeId(oldVideoAdditionRule.getCreativeId())
                .setStatusmoderate(BannersPerformanceStatusmoderate.No)
                .intoMap();

        assertThat("видеодополнение не изменилось", actualBannersPerformanceMap,
                beanDiffer(expectedBannersPerformanceMap)
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));

        ModReasonsRecord modReasonsRecord =
                dbSteps.moderationSteps().getModReasonsRecord(additionsList.get(0).getBannerCreativeId(),
                        ModReasonsType.video_addition);

        assertThat("у видеодополнения остались причины отклонения", modReasonsRecord, notNullValue());
    }

    @Test
    @Description("При сохранении баннера с видеодополнением без изменений у баннера не меняются статусы")
    @TestCaseId("10932")
    public void testBannerStatusesNoChangeBannerVideoAddition() {
        Map<String, Object> bannerRecordMap =
                dbSteps.bannersSteps().getBanner(bannerWithVideoRule.getBannerId()).intoMap();
        Map<String, Object> expectedBannerRecordMap = new BannersRecord()
                .setStatusmoderate(BannersStatusmoderate.Yes)
                .setStatusbssynced(BannersStatusbssynced.Yes)
                .intoMap();

        assertThat("у баннера не изменились статусы", bannerRecordMap,
                beanDiffer(expectedBannerRecordMap).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }
}
