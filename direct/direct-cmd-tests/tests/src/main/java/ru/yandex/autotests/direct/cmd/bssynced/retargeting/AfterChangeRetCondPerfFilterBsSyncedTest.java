package ru.yandex.autotests.direct.cmd.bssynced.retargeting;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingCondition;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.editAdGroupsPerformance.EditAdGroupsPerformanceResponse;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.direct.cmd.util.PerformanceCampaignHelper.mapEditGroupResponseToSaveRequest;

@Aqua.Test
@Description("Проверка сброса статуса bsSynced при изменении условия ретаргетинга ДМО фильтра")
@Stories(TestFeatures.Retargeting.AJAX_SAVE_RETARGETING_CONDITIONS)
@Features(TestFeatures.RETARGETING)
@Tag(CmdTag.AJAX_SAVE_RETARGETING_COND)
@Tag(ObjectTag.RETAGRETING)
@Tag(CampTypeTag.PERFORMANCE)
public class AfterChangeRetCondPerfFilterBsSyncedTest extends AfterChangeRetCondBsSyncedTestBase {
    protected static final String CLIENT = "at-direct-retargeting12";

    public AfterChangeRetCondPerfFilterBsSyncedTest() {
        retCondId = addRetargetingCondition();
        bannersRule = new PerformanceBannersRule().withUlogin(CLIENT);
        bannersRule.getGroup().getPerformanceFilters().get(0).
                withRetargeting(new RetargetingCondition().withRetCondId(retCondId));
        cmdRule.withRules(bannersRule);
    }

    @Override
    protected String getClient() {
        return CLIENT;
    }

    @Test
    @Description("Проверяем сброс статуса bsSynced при изменении условия ретаргетинга привязанного к ДМО фильтру")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9362")
    public void checkAfterChangeRetargetingBSSyncedTest() {
        changeRetargeting();
        EditAdGroupsPerformanceResponse actualResponse = cmdRule.cmdSteps().groupsSteps()
                .getEditAdGroupsPerformance(CLIENT, campaignId.toString(),
                        bannersRule.getGroupId().toString(), bannersRule.getBannerId().toString());
        Group group = mapEditGroupResponseToSaveRequest(actualResponse.getCampaign().getPerformanceGroups()).get(0);

        BsSyncedHelper.checkGroupBsSynced(group, StatusBsSynced.NO);
    }

}
