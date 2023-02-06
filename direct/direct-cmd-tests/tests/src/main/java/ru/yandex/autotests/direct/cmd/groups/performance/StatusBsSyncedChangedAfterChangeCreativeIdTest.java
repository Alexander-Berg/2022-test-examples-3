package ru.yandex.autotests.direct.cmd.groups.performance;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatusbssynced;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Aqua.Test
@ru.yandex.qatools.allure.annotations.Description("Проверка сброса statusBsSynced баннера при изменении креатива в баннере")
@Stories(TestFeatures.Banners.STATUS_BS_SYNCED)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_PERFORMANCE_AD_GROUPS)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.PERFORMANCE)
public class StatusBsSyncedChangedAfterChangeCreativeIdTest {
    private static final String CLIENT = "at-direct-bssync-banners1";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private PerformanceBannersRule bannersRule = new PerformanceBannersRule().withUlogin(CLIENT);
    private Long creativeId;

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule()
            .withRules(new TestWatcher() {
                           @Override
                           protected void finished(Description description) {
                               TestEnvironment.newDbSteps().useShardForLogin(CLIENT);
                               TestEnvironment.newDbSteps().bannersPerformanceSteps()
                                       .deleteBannersPerformanceRecord(creativeId);
                               TestEnvironment.newDbSteps().perfCreativesSteps().deletePerfCreatives(creativeId);
                           }
                       },
                    bannersRule);

    @Before
    public void before() {
        BsSyncedHelper.moderateCamp(cmdRule, bannersRule.getCampaignId());
        BsSyncedHelper.syncCamp(cmdRule, bannersRule.getCampaignId());
        creativeId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).perfCreativesSteps()
                .saveDefaultPerfCreative(User.get(CLIENT).getClientID());
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9852")
    public void changingCreativeInBannerShouldResetBsSyncedStatus() {
        Group group = bannersRule.getCurrentGroup();
        cmdRule.cmdSteps().groupsSteps().prepareGroupForUpdate(group, CampaignTypeEnum.DMO);
        group.getBanners().get(0).getCreativeBanner().withCreativeId(creativeId);
        bannersRule.saveGroup(GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(), group));
        BsSyncedHelper.checkBannerBsSynced(CLIENT, bannersRule.getBannerId(), BannersStatusbssynced.No);
    }

}
