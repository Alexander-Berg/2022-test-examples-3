package ru.yandex.autotests.direct.cmd.conditions.ajaxeditdynamicconditions;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.conditions.common.AfterLastConditionTestBase;
import ru.yandex.autotests.direct.cmd.data.commons.group.DynamicCondition;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.DynamicBannersRule;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Aqua.Test
@Description("Проверки изменения статусов модерации при удалении последнего условия ДТО через ajaxEditDynamicConditions")
@Stories(TestFeatures.Conditions.AJAX_EDIT_DYNAMIC_CONDITIONS)
@Features(TestFeatures.CONDITIONS)
@Tag("TESTIRT-8612")
public class AfterLastDynamicConditionRemoveTest extends AfterLastConditionTestBase {

    private static final String CLIENT = "at-direct-b-bannersmultiedit";

    public AfterLastDynamicConditionRemoveTest() {
        super(new DynamicBannersRule());
    }

    @Override
    protected String getClient() {
        return CLIENT;
    }

    @Override
    protected void deleteCondition() {
        cmdRule.cmdSteps().ajaxEditDynamicConditionsSteps()
                .dynamicConditionsDeleteWithAssumption(bannersRule.getCampaignId(), bannersRule.getGroupId(),
                        getClient(), getDynId());
    }

    @Override
    protected void suspendCondition() {
        DynamicCondition condition = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, campaignId)
                .getGroups().get(0).getDynamicConditions().get(0);
        condition.setIsSuspended("1");
        cmdRule.cmdSteps().ajaxEditDynamicConditionsSteps()
                .dynamicConditionsChangeWithAssumption(bannersRule.getCampaignId(), bannersRule.getGroupId(), condition,
                        CLIENT);

    }

    private Long getDynId() {
        return Long.valueOf(bannersRule.getCurrentGroup().getDynamicConditions().get(0).getDynId());
    }

    @Override
    protected Group getExpectedGroupStatuses() {
        Group group = super.getExpectedGroupStatuses();
        group.getBanners().get(0).withStatusBsSynced(StatusBsSynced.NO.toString());
        return group;
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10866")
    public void deleteConditionTest() {
        super.deleteConditionTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10867")
    public void suspendConditionTest() {
        super.suspendConditionTest();
    }
}
