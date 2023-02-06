package ru.yandex.autotests.direct.cmd.banners.additions.video;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.qatools.allure.annotations.TestCaseId;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.rules.VideoAdditionCreativeRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersPerformanceStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BannersPerformanceRecord;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Отправка кампании с видеодополнением на модерацию")
@Stories(TestFeatures.Banners.VIDEO_ADDITION)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.ORDER_CAMP)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.VIDEO_ADDITION)
@Tag(TrunkTag.YES)
@Tag("DIRECT-63700")
public class OrderCampVideoAdditionTest {
    private static final String CLIENT = "at-direct-video-addition-1";
    private static DirectJooqDbSteps dbSteps = TestEnvironment.newDbSteps(CLIENT);

    @ClassRule
    public static DirectCmdRule classRule = DirectCmdRule.defaultClassRule();

    private VideoAdditionCreativeRule videoAdditionCreativeRule = new VideoAdditionCreativeRule(CLIENT);
    private BannersRule bannerWithVideoRule = new TextBannersRule()
            .withVideoAddition(videoAdditionCreativeRule)
            .withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule()
            .withRules(videoAdditionCreativeRule, bannerWithVideoRule);

    @Before
    public void before() {
        BannersPerformanceRecord bannersPerformanceRecord = dbSteps.bannersPerformanceSteps()
                .getBannersPerformance(bannerWithVideoRule.getBannerId(), videoAdditionCreativeRule.getCreativeId());

        assumeThat("у видеодополнения статус модерации New до отправки на модерацию",
                bannersPerformanceRecord.getStatusmoderate(),
                equalTo(BannersPerformanceStatusmoderate.New)
        );
    }

    @Test
    @Description("При отправке на модерацию кампании с баннером с видеодополнением, у видеодополнения статус модерации меняется на Ready")
    @TestCaseId("10933")
    public void testVideoAdditionStatusModerateOrderCamp() {
        cmdRule.cmdSteps().campaignSteps().orderCamp(bannerWithVideoRule.getCampaignId(), CLIENT);

        BannersPerformanceRecord bannersPerformanceRecord = dbSteps.bannersPerformanceSteps()
                .getBannersPerformance(bannerWithVideoRule.getBannerId(), videoAdditionCreativeRule.getCreativeId());

        assertThat("у видеодополнения статус модерации изменился на Ready",
                bannersPerformanceRecord.getStatusmoderate(),
                equalTo(BannersPerformanceStatusmoderate.Ready));
    }
}
