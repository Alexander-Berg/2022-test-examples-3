package ru.yandex.autotests.direct.cmd.conditions.ajaxupdateshowconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import ru.yandex.qatools.allure.annotations.TestCaseId;

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
import ru.yandex.autotests.direct.cmd.data.interest.RelevanceMatch;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assume.assumeThat;

@Aqua.Test
@Description("Проверка добавления бесфразного таргетинга (relevance_match) в группах используя ajaxUpdateShowConditions")
@Stories(TestFeatures.Conditions.AJAX_UPDATE_SHOW_CONDITIONS)
@Features(TestFeatures.CONDITIONS)
@Tag(TestFeatures.Conditions.AJAX_UPDATE_SHOW_CONDITIONS)
@Tag(TestFeatures.CONDITIONS)
@RunWith(Parameterized.class)
public class AjaxUpdateShowConditionsAddRelevanceMatchTest {
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected static final String CLIENT = Logins.CLIENT_WITH_RELEVANCE_MATCH;

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

    public AjaxUpdateShowConditionsAddRelevanceMatchTest(CampaignTypeEnum campaignTypeEnum) {
        this.bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignTypeEnum)
                .overrideGroupTemplate(new Group().withPhrases(new ArrayList<>()))
                .withUlogin(CLIENT);

        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Before
    public void before() {
        Group initialGroup = bannersRule.getCurrentGroup();
        assumeThat("в группе нет бесфразного таргетинга",
                initialGroup.getRelevanceMatch().size(),
                equalTo(0)
        );
    }

    @Test
    @Description("Включение бесфразного таргетинга на странице кампании (cmd = ajaxUpdateShowConditions)")
    @TestCaseId("11022")
    public void addRelevanceMatchPriceAtAjaxUpdateShowConditions() {
        String expectedPrice = "0.88";

        AjaxUpdateShowConditionsObjects showConditions = new AjaxUpdateShowConditionsObjects().withAdded(
                new AjaxUpdateShowConditions().withPrice(expectedPrice)
        );
        ErrorResponse response = updateShowConditions(showConditions);
        assumeThat("нет ошибок при сохранении условия", response.getError(), nullValue());

        Group actualGroup = bannersRule.getCurrentGroup();

        assumeThat("в группе есть ровно один бесфразный таргетинг", actualGroup.getRelevanceMatch().size(), equalTo(1));
        assertThat("изменения сохранились", actualGroup.getRelevanceMatch().get(0).getPrice().toString(),
                equalTo(expectedPrice));
    }

    @Test
    @Description("Автоматический расчет цены для автотаргетинга (cmd = ajaxUpdateShowConditions)")
    @TestCaseId("11023")
    public void addRelevanceMatchPrice_autoPrice_AjaxUpdateShowConditions() {
        String expectedPrice = "4.0";

        Group actualGroup = bannersRule.getCurrentGroup();
        cmdRule.cmdSteps().ajaxUpdateShowConditionsSteps().addTestPhrases(Long.parseLong(actualGroup.getCampaignID()),
                Long.parseLong(actualGroup.getAdGroupID()), CLIENT);

        AjaxUpdateShowConditionsObjects relevanceConditions = new AjaxUpdateShowConditionsObjects().withAdded(
                new AjaxUpdateShowConditions()
        );
        ErrorResponse response = updateShowConditions(relevanceConditions);
        assumeThat("нет ошибок при сохранении условия", response.getError(), nullValue());

        List<RelevanceMatch> actualRelevanceMatch = bannersRule.getCurrentGroup().getRelevanceMatch();

        assumeThat("в группе есть ровно один бесфразный таргетинг", actualRelevanceMatch, hasSize(1));
        assertThat("Автоматический расчет цены выполнен верно",
                actualRelevanceMatch.get(0).getPrice().toString(), equalTo(expectedPrice));
    }

    private ErrorResponse updateShowConditions(AjaxUpdateShowConditionsObjects showConditions) {
        AjaxUpdateShowConditionsRequest request = new AjaxUpdateShowConditionsRequest()
                .withCid(String.valueOf(bannersRule.getCampaignId()))
                .withRelevanceMatch(bannersRule.getGroupId(), showConditions)
                .withUlogin(CLIENT);
        return cmdRule.cmdSteps().ajaxUpdateShowConditionsSteps().postAjaxUpdateShowConditions(request);
    }

}
