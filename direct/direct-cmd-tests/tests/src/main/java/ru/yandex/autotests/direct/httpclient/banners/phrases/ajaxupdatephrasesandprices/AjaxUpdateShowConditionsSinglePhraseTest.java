package ru.yandex.autotests.direct.httpclient.banners.phrases.ajaxupdatephrasesandprices;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.phrases.AjaxUpdateShowConditionsBean;
import ru.yandex.autotests.direct.httpclient.data.textresources.phrases.AjaxUpdateTestPhrases;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter.resource;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanEquivalent;

/**
 * Created by shmykov on 27.05.15.
 * TESTIRT-4965
 */
@Aqua.Test
@Description("Проверки контроллера ajaxUpdateShowConditions для одной фразы")
@Stories(TestFeatures.Phrases.AJAX_UPDATE_PHRASES_AND_PRICES)
@Features(TestFeatures.PHRASES)
@Tag(TrunkTag.YES)
public class AjaxUpdateShowConditionsSinglePhraseTest extends AjaxUpdateShowConditionsTestBase {

    public AjaxUpdateShowConditionsSinglePhraseTest() {
        super(new Group());
    }


    @Test
    @Description("Проверка фразы в ответе сервера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10269")
    public void ajaxUpdatePhrasesAndPricesResponseTest() {
        phrasesMap.put(String.valueOf(firstPhraseId), phraseBean);
        groupPhrases.setEdited(phrasesMap);
        jsonPhrases.setGroupPhrases(String.valueOf(adgroupId), groupPhrases);
        response =
                cmdRule.oldSteps().ajaxUpdatePhrasesAndPricesSteps().ajaxUpdateShowConditions(csrfToken, requestParams);

        AjaxUpdateShowConditionsBean actualPhrase = getPhraseFromResponse(response, String.valueOf(firstPhraseId));
        assertThat("фраза в ответе совпадает с ожидаемой", actualPhrase, beanEquivalent(phraseBean));
    }

    @Test
    @Description("Проверка параметров фразы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10268")
    public void phraseParametersTest() {
        phrasesMap.put(String.valueOf(firstPhraseId), phraseBean);
        groupPhrases.setEdited(phrasesMap);
        jsonPhrases.setGroupPhrases(String.valueOf(adgroupId), groupPhrases);
        response =
                cmdRule.oldSteps().ajaxUpdatePhrasesAndPricesSteps().ajaxUpdateShowConditions(csrfToken, requestParams);

        Phrase phrase = bannersRule.getCurrentGroup().getPhrases().get(0);
        AjaxUpdateShowConditionsBean actualPhrase = new AjaxUpdateShowConditionsBean()
                .withPhrase(phrase.getPhrase())
                .withIsSuspended(phrase.getIsSuspended())
                .withPrice(phrase.getPrice().toString());
        assertThat("фраза соответствует ожиданиям", actualPhrase, beanEquivalent(phraseBean));
    }

    @Test
    @Description("Проверка нормализации ненормализованной фразы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10270")
    public void NormalizePhraseTest() {
        phraseBean.setPhrase(resource(AjaxUpdateTestPhrases.NOT_NORMALIZED_PHRASE).toString());
        phrasesMap.put(String.valueOf(firstPhraseId), phraseBean);
        groupPhrases.setEdited(phrasesMap);
        jsonPhrases.setGroupPhrases(String.valueOf(adgroupId), groupPhrases);
        response =
                cmdRule.oldSteps().ajaxUpdatePhrasesAndPricesSteps().ajaxUpdateShowConditions(csrfToken, requestParams);

        AjaxUpdateShowConditionsBean actualPhrase = getPhraseFromResponse(response, String.valueOf(firstPhraseId));
        assertThat("нормализованная фраза в ответе совпадает с ожидаемой", actualPhrase.getNormalizedPhrase(),
                equalTo(resource(AjaxUpdateTestPhrases.NORMALIZED_PHRASE).toString()));
    }

    @Test
    @Description("Проверка склеивания минус слов")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10271")
    public void glueMinusWordsTest() {
        phraseBean.setPhrase(resource(AjaxUpdateTestPhrases.DOUBLE_MINUS_WORDS_PHRASE).toString());
        phrasesMap.put(String.valueOf(firstPhraseId), phraseBean);
        groupPhrases.setEdited(phrasesMap);
        jsonPhrases.setGroupPhrases(String.valueOf(adgroupId), groupPhrases);
        response =
                cmdRule.oldSteps().ajaxUpdatePhrasesAndPricesSteps().ajaxUpdateShowConditions(csrfToken, requestParams);

        AjaxUpdateShowConditionsBean actualPhrase = getPhraseFromResponse(response, String.valueOf(firstPhraseId));
        assertThat("нормализованная фраза в ответе совпадает с ожидаемой", actualPhrase.getPhrase(),
                equalTo(resource(AjaxUpdateTestPhrases.REMOVED_DOUBLES_MINUS_WORDS_PHRASE).toString()));
    }
}
