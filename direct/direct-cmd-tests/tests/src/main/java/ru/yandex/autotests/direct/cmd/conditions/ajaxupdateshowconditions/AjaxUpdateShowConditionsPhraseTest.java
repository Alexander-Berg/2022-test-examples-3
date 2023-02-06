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
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Проверка удаления/остановки фразы через ajaxUpdateShowConditions")
@Stories(TestFeatures.Conditions.AJAX_UPDATE_SHOW_CONDITIONS)
@Features(TestFeatures.CONDITIONS)
@RunWith(Parameterized.class)
@Tag("TESTIRT-8612")
public class AjaxUpdateShowConditionsPhraseTest {

    private static final String CLIENT = "at-direct-b-bannersmultiedit";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule;

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    private String campaignId;
    private AjaxUpdateShowConditionsRequest request;
    private String phraseId;

    public AjaxUpdateShowConditionsPhraseTest(CampaignTypeEnum campaignTypeEnum) {
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignTypeEnum).withUlogin(CLIENT);
        assumeThat("в бине есть всего одна фраза", bannersRule.getGroup().getPhrases(), hasSize(1));
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
        assumeThat("фраза активна", bannersRule.getCurrentGroup().getPhrases().get(0).getSuspended(), equalTo("0"));
        phraseId = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, campaignId)
                .getGroups().get(0).getPhrases().get(0).getId().toString();
        request = new AjaxUpdateShowConditionsRequest()
                .withCid(campaignId)
                .withUlogin(CLIENT);
    }

    @Test
    @Description("Проверка удаление последней фразы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9658")
    public void deleteLastRetargetingTest() {
        request.withGroupPhrases(bannersRule.getGroupId().toString(),
                new AjaxUpdateShowConditionsObjects()
                        .withDeleted(phraseId));
        cmdRule.cmdSteps().ajaxUpdateShowConditionsSteps().postAjaxUpdateShowConditions(request);

        ShowCampResponse actualResponse = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, campaignId);
        assertThat("фраза удалилась", actualResponse.getGroups().get(0)
                .getPhrases(), hasSize(0));
    }

    @Test
    @Description("Проверка остановки последней фразы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9659")
    public void suspendLastPhraseTest() {
        request.withGroupPhrases(bannersRule.getGroupId().toString(),
                new AjaxUpdateShowConditionsObjects()
                        .withEdited(phraseId, new AjaxUpdateShowConditions().withIsSuspended("1")));
        cmdRule.cmdSteps().ajaxUpdateShowConditionsSteps().postAjaxUpdateShowConditions(request);

        Phrase actualPhrase = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, campaignId)
                .getGroups().get(0).getPhrases().get(0);
        assertThat("фраза остановилась", actualPhrase.getSuspended(), equalTo("1"));
    }
}
