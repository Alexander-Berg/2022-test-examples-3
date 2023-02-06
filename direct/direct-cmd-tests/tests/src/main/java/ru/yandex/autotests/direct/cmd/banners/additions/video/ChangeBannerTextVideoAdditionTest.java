package ru.yandex.autotests.direct.cmd.banners.additions.video;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.qatools.allure.annotations.TestCaseId;

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
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BannersPerformanceRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Изменение текста у баннера с видеодополнением")
@Stories(TestFeatures.Banners.VIDEO_ADDITION)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.VIDEO_ADDITION)
@Tag(TrunkTag.YES)
@Tag("DIRECT-63700")
public class ChangeBannerTextVideoAdditionTest {
    private static final String CLIENT = "at-direct-video-addition-1";
    private static final String NEW_BODY_TEXT = "новые посудомоечные машины";

    @ClassRule
    public static DirectCmdRule classRule = DirectCmdRule.defaultClassRule();

    private VideoAdditionCreativeRule videoAdditionRule = new VideoAdditionCreativeRule(CLIENT);
    private BannersRule bannerWithVideoRule =
            new TextBannersRule().withVideoAddition(videoAdditionRule).withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule()
            .withRules(videoAdditionRule, bannerWithVideoRule);

    @Before
    public void before() {
        BsSyncedHelper.makeCampSynced(cmdRule, bannerWithVideoRule.getCampaignId());

        bannerWithVideoRule.updateCurrentGroupBy(g -> {
                    g.getBanners().get(0).setBody(NEW_BODY_TEXT);
                    return g;
                }
        );
    }

    @Test
    @Description("При изменении текста баннера у видеодополнения сбрасывается статус модерации")
    @TestCaseId("10924")
    public void testVideoAdditionStatusModerateChangeBannerText() {
        BannersPerformanceRecord bannersPerformanceRecord = TestEnvironment.newDbSteps(CLIENT).bannersPerformanceSteps()
                .getBannersPerformance(bannerWithVideoRule.getBannerId(), videoAdditionRule.getCreativeId());
        assumeThat("у баннера найдено видеодополнение", bannersPerformanceRecord, notNullValue());
        assertThat("у видео дополнения статус модерации Ready", bannersPerformanceRecord.getStatusmoderate(),
                equalTo(BannersPerformanceStatusmoderate.Ready));
    }
}
