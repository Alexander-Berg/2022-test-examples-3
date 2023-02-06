package ru.yandex.autotests.direct.cmd.conditions.ajaxupdateshowconditions;

import java.util.Collections;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.conditions.common.AfterLastConditionTestBase;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.MobileBannersRule;
import ru.yandex.autotests.direct.cmd.steps.retargeting.RetargetingHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.cmd.data.interest.TargetInterestsFactory.defaultInterests;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("ТГО/РМП Проверка статусов после удаления/остановки последнего интереса через ajaxUpdateShowConditions")
@Stories(TestFeatures.Conditions.AJAX_UPDATE_SHOW_CONDITIONS)
@Features(TestFeatures.CONDITIONS)
@Tag("TESTIRT-8612")
@Tag(CampTypeTag.MOBILE)
@Tag(ObjectTag.TARGET_INTERESTS)
public class AfterLastInterestRemoveTest extends AfterLastConditionTestBase {

    private Long categoryId;

    public AfterLastInterestRemoveTest() {
        super(new MobileBannersRule());
        categoryId = RetargetingHelper.getRandomTargetCategoryId();
        bannersRule.overrideGroupTemplate(new Group()
                .withPhrases(Collections.emptyList())
                .withTargetInterests(defaultInterests(categoryId)));
    }

    @Override
    protected String getClient() {
        return "at-direct-back-interest";
    }

    @Override
    protected void deleteCondition() {
        cmdRule.cmdSteps().ajaxUpdateShowConditionsSteps()
                .deleteInterests(bannersRule.getCampaignId(), bannersRule.getGroupId(), getClient(), getInterestId());
    }

    @Override
    protected void suspendCondition() {
        cmdRule.cmdSteps().ajaxUpdateShowConditionsSteps()
                .suspendInterests(bannersRule.getCampaignId(), bannersRule.getGroupId(), getClient(), getInterestId());
        assumeThat("последний интерес остановлен", cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(getClient(), campaignId)
                .getGroups().get(0).getTargetInterests().get(0).getIsSuspended(), equalTo(1));
    }

    private String getInterestId() {
        return cmdRule.cmdSteps().campaignSteps().getShowCamp(getClient(), campaignId)
                .getGroups().get(0).getTargetInterests().get(0).getRetId().toString();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10870")
    public void deleteConditionTest() {
        super.deleteConditionTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10871")
    public void suspendConditionTest() {
        super.suspendConditionTest();
    }
}
