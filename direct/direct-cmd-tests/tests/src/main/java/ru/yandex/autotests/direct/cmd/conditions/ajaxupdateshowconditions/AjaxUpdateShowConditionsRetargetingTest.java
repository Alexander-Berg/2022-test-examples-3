package ru.yandex.autotests.direct.cmd.conditions.ajaxupdateshowconditions;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditions;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsObjects;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsRequest;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.commons.group.Retargeting;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
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
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка удаления/остановки ретаргетинга через ajaxUpdateShowConditions")
@Stories(TestFeatures.Conditions.AJAX_UPDATE_SHOW_CONDITIONS)
@Features(TestFeatures.CONDITIONS)
@RunWith(Parameterized.class)
@Tag("TESTIRT-8612")
public class AjaxUpdateShowConditionsRetargetingTest {

    private static final String CLIENT = "at-direct-retargeting6";
    private static final String PRICE_CONTEXT = "3";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule;

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    private String campaignId;
    private String retargetingId;
    private AjaxUpdateShowConditionsRequest request;

    public AjaxUpdateShowConditionsRetargetingTest(CampaignTypeEnum campaignTypeEnum) {
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignTypeEnum).withUlogin(CLIENT);
        bannersRule.overrideGroupTemplate(new Group()
                .withPhrases(Collections.emptyList())
                .withRetargetings(singletonList(new Retargeting()
                        .withRetCondId(getRetargetingCondition().longValue())
                        .withPriceContext(PRICE_CONTEXT))));
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Проверка удаления последнего ретаргетинга при отсутствии фраз у {0} кампании")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE}
        });
    }

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId().toString();
        retargetingId = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, campaignId)
                .getGroups().get(0).getRetargetings().get(0).getRetId().toString();
        request = new AjaxUpdateShowConditionsRequest()
                .withCid(campaignId)
                .withUlogin(CLIENT);
    }

    @Test
    @Description("Проверка удаления последнего ретаргетинга при отсутствии фраз")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9660")
    public void deleteLastRetargetingTest() {
        request.withRetargetings(bannersRule.getGroupId().toString(),
                new AjaxUpdateShowConditionsObjects().withDeleted(retargetingId));
        cmdRule.cmdSteps().ajaxUpdateShowConditionsSteps().postAjaxUpdateShowConditions(request);

        ShowCampResponse actualResponse = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, campaignId);
        assertThat("ретаргетинг удалился", actualResponse.getGroups().get(0)
                .getRetargetings(), nullValue());
    }

    @Test
    @Description("Проверка остановки последнего ретаргетинга")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9661")
    public void suspendLastPhraseTest() {
        request.withRetargetings(bannersRule.getGroupId().toString(),
                new AjaxUpdateShowConditionsObjects()
                        .withEdited(retargetingId, new AjaxUpdateShowConditions().withIsSuspended("1")));
        cmdRule.cmdSteps().ajaxUpdateShowConditionsSteps().postAjaxUpdateShowConditions(request);

        Retargeting actualRetargeting = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, campaignId)
                .getGroups().get(0).getRetargetings().get(0);
        assertThat("ретаргетинг остановился", actualRetargeting.getIsSuspended(), equalTo("1"));
    }

    private Integer getRetargetingCondition() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).retargetingConditionSteps()
                .deleteUnusedRetargetingsConditions(Long.valueOf(User.get(CLIENT).getClientID()));
        return cmdRule.apiSteps().retargetingSteps().addRandomRetargetingCondition(CLIENT);
    }
}
