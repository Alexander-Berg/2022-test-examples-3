package ru.yandex.autotests.direct.cmd.bssynced.phrases;


import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditions;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsObjects;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsRequest;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.CmdStrategyBeans;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.direct.utils.strategy.data.Strategies;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Aqua.Test
@Description("Проверка сброса статуса bsSynced группы при изменении фразы на странице просмотра кампании")
@Stories(TestFeatures.Phrases.AJAX_UPDATE_SHOW_CONDITIONS)
@Features(TestFeatures.PHRASES)
@Tag(ObjectTag.PHRASE)
@Tag(CmdTag.AJAX_UPDATE_SHOW_CONDITIONS)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@RunWith(Parameterized.class)
public class StatusBsSyncedAfterFastPhraseChangeTest extends StatusBsSyncedAfterPhraseChangeBaseTest {

    public StatusBsSyncedAfterFastPhraseChangeTest(CampaignTypeEnum campaignType) {
        this.campaignType = campaignType;
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType)
                .withCampStrategy(CmdStrategyBeans.getStrategyBean(Strategies.HIGHEST_POSITION_MAX_COVERAGE))
                .withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Сброс bsSynced группы и фразы после изменения фразы. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE}
        });
    }

    @Test
    @Description("Сброс bsSynced группы при удалении фразы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9342")
    public void checkBsSyncedFastDeletePhrase() {
        Phrase phrase = bannersRule.getCurrentGroup().getPhrases().get(0);
        AjaxUpdateShowConditionsRequest request = new AjaxUpdateShowConditionsRequest()
                .withCid(campaignId.toString())
                .withGroupPhrases(bannersRule.getGroupId().toString(),
                        new AjaxUpdateShowConditionsObjects()
                                .withDeleted(phrase.getId().toString()))
                .withUlogin(CLIENT);

        cmdRule.cmdSteps().phrasesSteps().postAjaxUpdateShowCondition(request);

        BsSyncedHelper.checkGroupBsSynced(bannersRule.getCurrentGroup(), StatusBsSynced.NO);
    }

    @Test
    @Description("Сброс bsSynced группы при изменении текста фразы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9343")
    public void checkBsSyncedFastChangePhraseText() {
        Phrase phrase = bannersRule.getCurrentGroup().getPhrases().get(0);
        AjaxUpdateShowConditionsRequest request = new AjaxUpdateShowConditionsRequest()
                .withCid(campaignId.toString())
                .withGroupPhrases(bannersRule.getGroupId().toString(),
                        new AjaxUpdateShowConditionsObjects()
                                .withEdited(phrase.getId().toString(), new AjaxUpdateShowConditions()
                                        .withPhrase("новый тест")))
                .withUlogin(CLIENT);

        cmdRule.cmdSteps().phrasesSteps().postAjaxUpdateShowCondition(request);

        BsSyncedHelper.checkGroupBsSynced(bannersRule.getCurrentGroup(), StatusBsSynced.NO);
    }

    @Test
    @Description("Сброс bsSynced группы при остановке фразы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9346")
    public void checkBsSyncedSetPhraseStop() {
        Phrase phrase = bannersRule.getCurrentGroup().getPhrases().get(0);
        stopPhrase(phrase.getId().toString());
        BsSyncedHelper.checkGroupBsSynced(bannersRule.getCurrentGroup(), StatusBsSynced.NO);
    }

    @Test
    @Description("Сброс bsSynced группы при запуске фразы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9347")
    public void checkBsSyncedSetPhraseStart() {
        Phrase phrase = bannersRule.getCurrentGroup().getPhrases().get(0);
        stopPhrase(phrase.getId().toString());
        BsSyncedHelper.setGroupBsSynced(cmdRule, bannersRule.getGroupId(), StatusBsSynced.YES);
        startPhrase(phrase.getId().toString());
        BsSyncedHelper.checkGroupBsSynced(bannersRule.getCurrentGroup(), StatusBsSynced.NO);
    }

}
