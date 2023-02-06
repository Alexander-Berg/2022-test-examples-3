package ru.yandex.autotests.direct.cmd.banners.additions.video.negative;

import java.util.List;
import java.util.Map;

import ru.yandex.qatools.allure.annotations.TestCaseId;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.rules.CreativeBannerRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.VideoAdditionCreativeRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatusbssynced;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.PerfCreativesCreativeType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BannersRecord;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Добавление видеодополнения к image_ad баннеру в текстовой кампании")
@Stories(TestFeatures.Banners.VIDEO_ADDITION)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.VIDEO_ADDITION)
@Tag("DIRECT-63700")
public class SaveVideoAdditionWrongBannerTypeTest {
    protected static final String CLIENT = "at-direct-video-addition-1";
    private static final DirectJooqDbSteps dbSteps = TestEnvironment.newDbSteps(CLIENT);

    @ClassRule
    public static final DirectCmdRule classRule = DirectCmdRule.defaultClassRule();

    private final VideoAdditionCreativeRule videoAdditionCreativeRule = new VideoAdditionCreativeRule(CLIENT);
    private final CreativeBannerRule creativeBannerRule = new CreativeBannerRule(CampaignTypeEnum.TEXT)
            .withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule()
            .withRules(videoAdditionCreativeRule, creativeBannerRule);

    @Before
    public void before() {
        BsSyncedHelper.makeCampSynced(cmdRule, creativeBannerRule.getCampaignId());

        creativeBannerRule.updateCurrentGroupBy(g -> {
                    g.getBanners().get(0).addDefaultVideoAddition(videoAdditionCreativeRule.getCreativeId());
                    return g;
                }
        );
    }

    @Test
    @Description("При добавлении видеодополнения image_ad баннеру статусы баннера не меняются")
    @TestCaseId("10946")
    public void testBannerStatusesSaveVideoAdditionWrongBannerType() {
        Map<String, Object> bannerRecordMap =
                dbSteps.bannersSteps().getBanner(creativeBannerRule.getBannerId()).intoMap();
        Map<String, Object> expectedBannerRecordMap = new BannersRecord()
                .setStatusmoderate(BannersStatusmoderate.Yes)
                .setStatusbssynced(BannersStatusbssynced.Yes)
                .intoMap();

        assertThat("статусы баннера не изменились", bannerRecordMap,
                beanDiffer(expectedBannerRecordMap).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
        );
    }

    @Test
    @Description("При добавлении видеодополнения image_ad баннеру у баннера не появляется видеодополнений")
    @TestCaseId("10947")
    public void testVideoAdditionsCountSaveVideoAdditionWrongBannerType() {
        List<Long> videoAdditionsIds =
                dbSteps.bannersPerformanceSteps().findBannersPerformanceIds(creativeBannerRule.getBannerId(),
                        PerfCreativesCreativeType.video_addition);
        assertThat("у баннера нет видеодополнений", videoAdditionsIds, hasSize(0));
    }
}
