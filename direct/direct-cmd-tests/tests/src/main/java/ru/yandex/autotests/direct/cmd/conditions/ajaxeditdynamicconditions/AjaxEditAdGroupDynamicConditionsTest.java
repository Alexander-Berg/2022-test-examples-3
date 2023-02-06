package ru.yandex.autotests.direct.cmd.conditions.ajaxeditdynamicconditions;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.data.commons.group.DynamicCondition;
import ru.yandex.autotests.direct.cmd.data.dynamicconditions.AjaxEditDynamicConditionsRequest;
import ru.yandex.autotests.direct.cmd.data.dynamicconditions.DynamicConditionMap;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.DynamicBannersRule;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Проверки изменения условий ДТО через ajaxEditDynamicConditions")
@Stories(TestFeatures.Conditions.AJAX_EDIT_DYNAMIC_CONDITIONS)
@Features(TestFeatures.CONDITIONS)
@Tag("TESTIRT-8612")
public class AjaxEditAdGroupDynamicConditionsTest {

    private static final String CLIENT = "at-direct-b-bannersmultiedit";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private DynamicBannersRule bannersRule = new DynamicBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);


    private String campaignId;
    private DynamicCondition condition;

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId().toString();
        condition = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, campaignId)
                .getGroups().get(0).getDynamicConditions().get(0);
    }

    @Test
    @Description("Провека возможности удаления последнего условия ДТО")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9608")
    public void deleteLastConditionTest() {
        AjaxEditDynamicConditionsRequest request = new AjaxEditDynamicConditionsRequest()
                .withCondition(bannersRule.getGroupId(),
                        new DynamicConditionMap()
                                .withDeleteIds(singletonList(Long.valueOf(condition.getDynId()))))
                .withCid(campaignId)
                .withUlogin(CLIENT);
        saveDynamicCondition(request);

        ShowCampResponse actualResponse = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, campaignId);
        assertThat("ДТО условие удалилось", actualResponse.getGroups().get(0)
                .getDynamicConditions(), hasSize(0));
    }


    @Test
    @Description("Провека возможности остановки последнего условия ДТО")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9609")
    public void suspendLastConditionTest() {
        condition.setIsSuspended("1");
        AjaxEditDynamicConditionsRequest request = AjaxEditDynamicConditionsRequest.fromDynamicCondition(condition)
                .withCid(campaignId)
                .withUlogin(CLIENT);
        saveDynamicCondition(request);

        ShowCampResponse actualResponse = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, campaignId);
        assertThat("ДТО условие остановилось", actualResponse.getGroups().get(0)
                .getDynamicConditions().get(0).getIsSuspended(), equalTo("1"));
    }

    private void saveDynamicCondition(AjaxEditDynamicConditionsRequest request) {
        CommonResponse response = cmdRule.cmdSteps().ajaxEditDynamicConditionsSteps()
                .postAjaxEditDynamicConditions(request);
        assumeThat("динамическое условие сохранилось", response.getResult(), equalTo("ok"));
    }
}
