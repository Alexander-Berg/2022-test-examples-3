package ru.yandex.autotests.direct.cmd.conditions.ajaxupdateshowconditions;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditions;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsObjects;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsRequest;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNull.notNullValue;
import static ru.yandex.autotests.direct.cmd.data.groups.GroupsFactory.getDefaultRelevanceMatch;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Проверка сохранения безфразного таргетинга (relevance_match) в группах используя ajaxUpdateShowConditions")
@Stories(TestFeatures.Conditions.AJAX_UPDATE_SHOW_CONDITIONS)
@Features(TestFeatures.CONDITIONS)
@Tag(TestFeatures.Conditions.AJAX_UPDATE_SHOW_CONDITIONS)
@Tag(TestFeatures.CONDITIONS)
@RunWith(Parameterized.class)
public class AjaxUpdateShowConditionsWithRelevanceMatchTest {
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected static String CLIENT = Logins.CLIENT_WITH_RELEVANCE_MATCH;

    @Parameterized.Parameters(name = "Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE},
        });
    }

    private BannersRule bannersRule;

    @Rule
    public DirectCmdRule cmdRule;

    public AjaxUpdateShowConditionsWithRelevanceMatchTest(CampaignTypeEnum campaignTypeEnum) {
        this.bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignTypeEnum)
                .overrideGroupTemplate(new Group().withRelevanceMatch(
                        Collections.singletonList(getDefaultRelevanceMatch())))
                .withUlogin(CLIENT);

        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }
    private Group initialGroup;

    @Before
    public void before() {
        initialGroup = bannersRule.getCurrentGroup();
        assumeThat("в группе появился безфразный таргетинг", initialGroup.getRelevanceMatch(), notNullValue());
    }

    @Test
    @Description("Редактирование цены для безфразного таргетинга на странице кампании (cmd = ajaxUpdateShowConditions)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10907")
    public void editRelevanceMatchPriceAtAjaxUpdateShowConditions() {
        String expectedPrice = "0.88";

        AjaxUpdateShowConditionsObjects showConditions = new AjaxUpdateShowConditionsObjects().withEdited(
                String.valueOf(initialGroup.getRelevanceMatch().get(0).getBidId()),
                new AjaxUpdateShowConditions().withPrice(expectedPrice)
        );
        Group actualGroup = updateShowConditions(showConditions);

        assumeThat("в группе есть безфразный таргетинг", actualGroup.getRelevanceMatch(), notNullValue());
        assertThat("изменения сохранились", actualGroup.getRelevanceMatch().get(0).getPrice().toString(),
                equalTo(expectedPrice));
    }

    @Test
    @Description("Выключение безфразного таргетинга на странице кампании (cmd = ajaxUpdateShowConditions)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10904")
    public void suspendRelevanceMatchAtAjaxUpdateShowConditions() {
        AjaxUpdateShowConditionsObjects showConditions = new AjaxUpdateShowConditionsObjects().withEdited(
                String.valueOf(initialGroup.getRelevanceMatch().get(0).getBidId()),
                new AjaxUpdateShowConditions().withIsSuspended("1")
        );
        Group actualGroup = updateShowConditions(showConditions);

        assumeThat("в группе есть безфразный таргетинг", actualGroup.getRelevanceMatch(), notNullValue());
        assertThat("безфразный таргетинг выключен", actualGroup.getRelevanceMatch().get(0).getIsSuspended(),
                equalTo("1"));
    }

    @Test
    @Description("Включение безфразного таргетинга на странице кампании после выключения (cmd = ajaxUpdateShowConditions)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10906")
    public void resumeRelevanceMatchAtAjaxUpdateShowConditions() {
        AjaxUpdateShowConditionsObjects showConditions = new AjaxUpdateShowConditionsObjects().withEdited(
                String.valueOf(initialGroup.getRelevanceMatch().get(0).getBidId()),
                new AjaxUpdateShowConditions().withIsSuspended("1")
        );
        Group actualGroup = updateShowConditions(showConditions);

        assumeThat("в группе есть безфразный таргетинг", actualGroup.getRelevanceMatch(), notNullValue());
        assumeThat("безфразный таргетинг выключен", actualGroup.getRelevanceMatch().get(0).getIsSuspended(),
                equalTo("1"));

        showConditions = new AjaxUpdateShowConditionsObjects().withEdited(
                String.valueOf(actualGroup.getRelevanceMatch().get(0).getBidId()),
                new AjaxUpdateShowConditions().withIsSuspended("0")
        );
        actualGroup = updateShowConditions(showConditions);

        assumeThat("в группе есть безфразный таргетинг", actualGroup.getRelevanceMatch(), notNullValue());
        assertThat("безфразный таргетинг включен", actualGroup.getRelevanceMatch().get(0).getIsSuspended(),
                Matchers.either(Matchers.is("")).or(Matchers.is("0")));
    }

    @Test
    @Description("Удаление безфразного таргетинга на странице кампании (cmd = ajaxUpdateShowConditions)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10905")
    public void deleteRelevanceMatchAtAjaxUpdateShowConditions() {
        AjaxUpdateShowConditionsObjects showConditions = new AjaxUpdateShowConditionsObjects().withDeleted(
                String.valueOf(initialGroup.getRelevanceMatch().get(0).getBidId())
        );
        Group actualGroup = updateShowConditions(showConditions);
        assertThat("безфразный таргетинг удален", actualGroup.getRelevanceMatch().size(), equalTo(0));
    }


    private Group updateShowConditions(AjaxUpdateShowConditionsObjects showConditions) {
        AjaxUpdateShowConditionsRequest request = new AjaxUpdateShowConditionsRequest()
                .withCid(String.valueOf(bannersRule.getCampaignId()))
                .withRelevanceMatch(bannersRule.getGroupId(), showConditions)
                .withUlogin(CLIENT);
        ErrorResponse response =
                cmdRule.cmdSteps().ajaxUpdateShowConditionsSteps().postAjaxUpdateShowConditions(request);
        assumeThat("нет ошибок при сохранении условия", response.getError(), nullValue());

        return bannersRule.getCurrentGroup();
    }
}
