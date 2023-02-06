package ru.yandex.autotests.direct.cmd.banners.additions.video;

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
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatusbssynced;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BannersPerformanceRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BannersRecord;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Удаление видеодополнения к баннеру")
@Stories(TestFeatures.Banners.VIDEO_ADDITION)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.VIDEO_ADDITION)
@Tag(TrunkTag.YES)
@Tag("DIRECT-63700")
public class DeleteBannerVideoAdditionTest {
    private static final String CLIENT = "at-direct-video-addition-1";
    private static DirectJooqDbSteps dbSteps = TestEnvironment.newDbSteps(CLIENT);

    @ClassRule
    public static DirectCmdRule classRule = DirectCmdRule.defaultClassRule();

    private final VideoAdditionCreativeRule videoAdditionCreativeRule = new VideoAdditionCreativeRule(CLIENT);
    private final BannersRule bannerWithVideoRule =
            new TextBannersRule()
                    .withVideoAddition(videoAdditionCreativeRule)
                    .withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRules = DirectCmdRule.defaultRule()
            .withRules(videoAdditionCreativeRule, bannerWithVideoRule);

    @Before
    public void before() {
        BsSyncedHelper.makeCampSynced(cmdRules, bannerWithVideoRule.getCampaignId());

        bannerWithVideoRule.updateCurrentGroupBy(g -> {
                    g.getBanners().get(0).removeVideoAddition();
                    return g;
                }
        );
    }

    @Test
    @Description("При удалении видеодополнения, видеодополнение удаляется")
    @TestCaseId("10929")
    public void testVideoAdditionDeletedDeleteBannerVideoAddition() {
        BannersPerformanceRecord bannersPerformanceRecord = dbSteps.bannersPerformanceSteps()
                .getBannersPerformance(bannerWithVideoRule.getBannerId(), videoAdditionCreativeRule.getCreativeId());

        assertThat("у баннера удалилось видеодополнение", bannersPerformanceRecord, nullValue());
    }

    @Test
    @Description("При удалении видеодополнения, у баннера не сбрасывается статус модерации, но сбрасывается statusBsSynced")
    @TestCaseId("10928")
    public void testBannerStatusesDeleteBannerVideoAddition() {
        Map<String, Object> bannerRecord =
                dbSteps.bannersSteps().getBanner(bannerWithVideoRule.getBannerId()).intoMap();
        Map<String, Object> bannerWithExpectedStatuses = new BannersRecord()
                .setStatusmoderate(BannersStatusmoderate.Yes)
                .setStatusbssynced(BannersStatusbssynced.No)
                .intoMap();

        assertThat("у баннера не сбросился статус модерации, но сбросился statusBsSynced", bannerRecord,
                beanDiffer(bannerWithExpectedStatuses)
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }
}
