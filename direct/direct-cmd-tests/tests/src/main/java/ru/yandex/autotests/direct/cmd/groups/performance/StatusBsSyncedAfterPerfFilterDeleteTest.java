package ru.yandex.autotests.direct.cmd.groups.performance;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.editAdGroupsPerformance.EditAdGroupsPerformanceResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.Collections.singletonList;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.direct.cmd.util.PerformanceCampaignHelper.mapEditGroupResponseToSaveRequest;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Проверка сброса флага statusBsSynced после удаления фильтра перформанс группы")
@Stories(TestFeatures.Groups.SAVE_PERFORMANCE_AD_GROUPS)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_PERFORMANCE_AD_GROUPS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(TrunkTag.YES)
public class StatusBsSyncedAfterPerfFilterDeleteTest {

    protected static final String CLIENT = Logins.DEFAULT_CLIENT;
    private static final String PERFORMANCE_GROUP_TEMPLATE = "cmd.common.request.group.performance.full2";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    public PerformanceBannersRule bannersRule = (PerformanceBannersRule)
            new PerformanceBannersRule().withGroupTemplate(PERFORMANCE_GROUP_TEMPLATE).withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    protected Long campaignId;
    protected Long adGroupId;
    protected Long bannerId;

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
        adGroupId = bannersRule.getGroupId();
        bannerId = bannersRule.getBannerId();
        BsSyncedHelper.moderateCamp(cmdRule, bannersRule.getCampaignId());
        BsSyncedHelper.syncCamp(cmdRule, bannersRule.getCampaignId());
        assumeThat("статус bsSynced установлен", bannersRule.getCurrentGroup().getBanners().get(0).getStatus_bs_synced(),
                equalTo(StatusBsSynced.YES.toString()));
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9851")
    public void bsSyncedAfterFilterDeleteTest() {
        removeFilter();
        EditAdGroupsPerformanceResponse actualResponse = cmdRule.cmdSteps().groupsSteps()
                .getEditAdGroupsPerformance(CLIENT, campaignId.toString(), adGroupId.toString(), bannerId.toString());
        Group group = mapEditGroupResponseToSaveRequest(actualResponse.getCampaign().getPerformanceGroups()).get(0);

        assertThat("статус bsSynced сброшен", group.getBanners().get(0).getStatus_bs_synced(),
                equalTo(StatusBsSynced.NO.toString()));
    }

    private void removeFilter() {
        Group group = bannersRule.getGroup();
        group.setAdGroupID(adGroupId.toString());
        group.getBanners().get(0).setBid(bannerId);
        group.getPerformanceFilters().remove(0);
        cmdRule.cmdSteps().groupsSteps().postSavePerformanceAdGroups(CLIENT, campaignId.toString(), singletonList(group));
    }
}
