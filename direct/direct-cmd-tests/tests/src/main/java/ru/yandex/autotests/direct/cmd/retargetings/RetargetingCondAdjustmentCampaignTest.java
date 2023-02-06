package ru.yandex.autotests.direct.cmd.retargetings;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.RetargetingMultiplier;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Aqua.Test
@Description("Проверка ретаргетинга, связанного с кампанией через корректировку ставок на кампанию")
@Stories(TestFeatures.Retargeting.SHOW_RETARGETING_CONDITIONS)
@Features(TestFeatures.RETARGETING)
@Tag(CmdTag.AJAX_DELETE_RETARGETING_COND)
@Tag(CmdTag.SHOW_RETARGETING_COND)
@Tag(ObjectTag.RETAGRETING)
@Tag(CampTypeTag.TEXT)
public class RetargetingCondAdjustmentCampaignTest extends RetargetingCondAdjustmentTestBase {

    public RetargetingCondAdjustmentCampaignTest() {
        TestEnvironment.newDbSteps().useShardForLogin(getClient()).retargetingConditionSteps()
                .deleteUnusedRetargetingsConditions(Long.valueOf(User.get(getClient()).getClientID()));
        retargetingId = cmdRule.apiSteps().retargetingSteps()
                .addRandomRetargetingCondition(getClient()).longValue();
        bannersRule.overrideCampTemplate(new SaveCampRequest()
                .withHierarchicalMultipliers(new HierarchicalMultipliers()
                        .withRetargetingMultiplier(RetargetingMultiplier
                                .getDefaultRetargetingMultiplier(retargetingId.toString(), "150"))));
        this.cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Override
    protected String getClient() {
        return "at-direct-back-ret13";
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9932")
    public void checkGetUsedCampaignsTest() {
        super.checkGetUsedCampaignsTest();
    }
}
