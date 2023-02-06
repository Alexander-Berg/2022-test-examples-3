package ru.yandex.autotests.direct.cmd.conditions.ajaxupdateshowconditions;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.conditions.common.AfterLastConditionTestBase;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.commons.group.Retargeting;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("ТГО/РМП Проверка статусов после удаления/остановки последнего ретаргетинга через ajaxUpdateShowConditions")
@Stories(TestFeatures.Conditions.AJAX_UPDATE_SHOW_CONDITIONS)
@Features(TestFeatures.CONDITIONS)
@Tag("TESTIRT-8612")
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@Tag(ObjectTag.RETAGRETING)
@RunWith(Parameterized.class)
public class AfterLastRetargetingRemoveTest extends AfterLastConditionTestBase {

    @Parameterized.Parameters(name = "Проверка статусов после удаления последнего ретаргетинга у {0} кампании")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE}
        });
    }

    public AfterLastRetargetingRemoveTest(CampaignTypeEnum campaignType) {
        super(BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType));
        bannersRule.overrideGroupTemplate(new Group()
                .withPhrases(Collections.emptyList())
                .withRetargetings(singletonList(new Retargeting()
                        .withRetCondId(getRetargetingCondition().longValue())
                        .withPriceContext(PRICE_CONTEXT))));
    }

    @Override
    protected String getClient() {
        return "at-direct-back-ret3";
    }

    @Override
    protected void deleteCondition() {
        cmdRule.cmdSteps().ajaxUpdateShowConditionsSteps()
                .deleteRetargetings(bannersRule.getCampaignId(), bannersRule.getGroupId(), getClient(), getRetId());
    }

    @Override
    protected void suspendCondition() {
        cmdRule.cmdSteps().ajaxUpdateShowConditionsSteps()
                .suspendRetargetings(bannersRule.getCampaignId(), bannersRule.getGroupId(), getClient(), getRetId());
        assumeThat("последний ретаргетинг остановлен", cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(getClient(), campaignId)
                .getGroups().get(0).getRetargetings().get(0).getIsSuspended(), equalTo("1"));
    }

    private Integer getRetargetingCondition() {
        TestEnvironment.newDbSteps().useShardForLogin(getClient()).retargetingConditionSteps()
                .deleteUnusedRetargetingsConditions(Long.valueOf(User.get(getClient()).getClientID()));
        return cmdRule.apiSteps().retargetingSteps().addRandomRetargetingCondition(getClient());
    }

    private String getRetId() {
        return cmdRule.cmdSteps().campaignSteps().getShowCamp(getClient(), campaignId)
                .getGroups().get(0).getRetargetings().get(0).getRetId().toString();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10876")
    public void deleteConditionTest() {
        super.deleteConditionTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10877")
    public void suspendConditionTest() {
        super.suspendConditionTest();
    }

    @Override
    protected Group getExpectedGroupStatuses() {
        return super.getExpectedGroupStatuses().withRetargetings(null);
    }
}
