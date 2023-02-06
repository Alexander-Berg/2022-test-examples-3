package ru.yandex.autotests.direct.cmd.banners.additions.video;

import java.util.Map;

import ru.yandex.qatools.allure.annotations.TestCaseId;

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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Добавление видеодополнения к баннеру")
@Stories(TestFeatures.Banners.VIDEO_ADDITION)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.VIDEO_ADDITION)
@Tag(TrunkTag.YES)
@Tag("DIRECT-63700")
public class AddBannerVideoAdditionTest {
    private static final String CLIENT = "at-direct-video-addition-1";
    private static final DirectJooqDbSteps dbSteps = TestEnvironment.newDbSteps(CLIENT);

    @ClassRule
    public static DirectCmdRule classRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannerNoVideoRule = new TextBannersRule().withUlogin(CLIENT);
    private VideoAdditionCreativeRule videoAdditionCreativeRule = new VideoAdditionCreativeRule(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule()
            .withRules(bannerNoVideoRule, videoAdditionCreativeRule);

    @Before
    public void before() {
        BsSyncedHelper.makeCampSynced(cmdRule, bannerNoVideoRule.getCampaignId());

        bannerNoVideoRule.updateCurrentGroupBy(g -> {
                    g.getBanners().get(0).addDefaultVideoAddition(videoAdditionCreativeRule.getCreativeId());
                    return g;
                }
        );
    }

    @Test
    @Description("При добавлении к баннеру видеодополнения у баннера появляется видеодополнение")
    @TestCaseId("10922")
    public void testVideoAdditionAddBannerVideoAddition() {
        BannersPerformanceRecord bannersPerformanceRecord = dbSteps.bannersPerformanceSteps()
                .getBannersPerformance(bannerNoVideoRule.getBannerId(), videoAdditionCreativeRule.getCreativeId());

        assertThat("у баннера найдено видеодополнение", bannersPerformanceRecord, notNullValue());

        assertThat("у видеодополнения статус модерации Ready", bannersPerformanceRecord.getStatusmoderate(),
                equalTo(BannersPerformanceStatusmoderate.Ready));
    }

    @Test
    @Description("При добавлении к баннеру видеодополнения у баннера не изменяется статус модерации, но сбрасывается statusBsSynced")
    @TestCaseId("10923")
    public void testBannerStatusesAddBannerVideoAddition() {
        Map<String, Object> bannerRecordMap =
                dbSteps.bannersSteps().getBanner(bannerNoVideoRule.getBannerId()).intoMap();
        Map<String, Object> expectedBannerRecordMap = new BannersRecord()
                .setStatusmoderate(BannersStatusmoderate.Yes)
                .setStatusbssynced(BannersStatusbssynced.No)
                .intoMap();

        assertThat("у баннера не изменился статус модерации, но сбросился statusBsSynced",
                bannerRecordMap,
                beanDiffer(expectedBannerRecordMap).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }
}
