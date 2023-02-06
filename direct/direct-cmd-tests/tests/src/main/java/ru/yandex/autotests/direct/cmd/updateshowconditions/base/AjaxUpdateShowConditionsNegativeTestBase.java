package ru.yandex.autotests.direct.cmd.updateshowconditions.base;

import java.util.HashMap;
import java.util.Map;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditions;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsGroup;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsObjects;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsRequest;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsResponse;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.ErrorPhrase;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.PhraseErrorsEnum;
import ru.yandex.autotests.direct.cmd.data.phrases.PhrasesFactory;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singletonList;
import static ru.yandex.autotests.direct.cmd.updateshowconditions.AjaxUpdateShowConditionsHelper.buildRequestWithMaxAvailablePhrases;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;


public abstract class AjaxUpdateShowConditionsNegativeTestBase {
    private static final String ULOGIN = "at-direct-add-plus-phrases";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private String phraseBase = "новая фраза ПЗ ";
    private BannersRule bannersRule = getBannerRule().withUlogin(ULOGIN);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    protected abstract BannersRule getBannerRule();

    @Test
    @Description("Получаем ошибку при попытке добавить в группу больше максимального числа фраз")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10617")
    public void addPhrasesWithoutGroupCopyError() {
        AjaxUpdateShowConditionsRequest request =
                buildRequestWithMaxAvailablePhrases(phraseBase, bannersRule.getGroupId())
                        .withUlogin(ULOGIN);

        AjaxUpdateShowConditionsResponse actualResponse =
                cmdRule.cmdSteps().ajaxUpdateShowConditionsSteps().postAjaxUpdateShowConditions2(request);

        Map<String, AjaxUpdateShowConditionsGroup> expectedGroups = new HashMap<>();
        expectedGroups.put(bannersRule.getGroupId().toString(),
                new AjaxUpdateShowConditionsGroup()
                        .withIsGroupOversized("1")
                        .withPhrasesExceedsLimitQty("1"));

        AjaxUpdateShowConditionsResponse expectedResponse = new AjaxUpdateShowConditionsResponse()
                .withGroups(expectedGroups);

        assertThat("получили ожидаемый ответ", actualResponse, beanDiffer(expectedResponse)
                .useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    @Description("Нельзя добавить фразу более чем из 7ми слов")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10618")
    public void addPhrasesWithMoreThanMaxWordsError() {
        String expectedErrorText = PhraseErrorsEnum.TOO_MANY_WORDS_IN_PHRASE_NEW.getErrorText();
        String phrase = "Социальная сеть Facebook открыла возможность заказывать" +
                " доставку еды и покупать билеты в кино напрямую из приложения";
        AjaxUpdateShowConditions phaseToSend = new AjaxUpdateShowConditions()
                .withPhrase(phrase)
                .withPrice(PhrasesFactory.getDefaultPhrase().getPrice().toString());

        AjaxUpdateShowConditionsRequest request = new AjaxUpdateShowConditionsRequest()
                .withGroupPhrases(bannersRule.getGroupId().toString(),
                        new AjaxUpdateShowConditionsObjects().withAdded(phaseToSend))
                .withUlogin(ULOGIN);

        AjaxUpdateShowConditionsResponse actualResponse =
                cmdRule.cmdSteps().ajaxUpdateShowConditionsSteps().postAjaxUpdateShowConditions2(request);

        expectedErrorText = expectedErrorText + " " + phaseToSend.getPhrase();

        Map<String, AjaxUpdateShowConditionsGroup> expectedGroups = new HashMap<>();
        expectedGroups.put(bannersRule.getGroupId().toString(),
                new AjaxUpdateShowConditionsGroup()
                        .withErrorsByPhrases(singletonList(
                                new ErrorPhrase().withPhrase(phrase).withErrors(singletonList(expectedErrorText)))));

        AjaxUpdateShowConditionsResponse expectedResponse = new AjaxUpdateShowConditionsResponse()
                .withGroups(expectedGroups);

        assertThat("получили ожидаемый ответ", actualResponse, beanDiffer(expectedResponse)
                .useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    @Description("Нельзя добавить фразу в архивную кампанию")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10619")
    public void cannotAddPhraseToArchivedCamp() {
        cmdRule.cmdSteps().campaignSteps().getStopCamp(ULOGIN, bannersRule.getCampaignId());
        cmdRule.cmdSteps().campaignSteps().getCampArc(ULOGIN, bannersRule.getCampaignId());

        AjaxUpdateShowConditions phaseToSend = new AjaxUpdateShowConditions()
                .withPhrase("Новая фраза ПЗ")
                .withPrice(PhrasesFactory.getDefaultPhrase().getPrice().toString())
                .withPriceContext(PhrasesFactory.getDefaultPhrase().getPriceContext().toString());

        AjaxUpdateShowConditionsRequest request = new AjaxUpdateShowConditionsRequest()
                .withGroupPhrases(bannersRule.getGroupId().toString(),
                        new AjaxUpdateShowConditionsObjects().withAdded(phaseToSend))
                .withUlogin(ULOGIN);

        AjaxUpdateShowConditionsResponse actualResponse =
                cmdRule.cmdSteps().ajaxUpdateShowConditionsSteps().postAjaxUpdateShowConditions2(request);

        Map<String, AjaxUpdateShowConditionsGroup> expectedGroups = new HashMap<>();
        expectedGroups.put(bannersRule.getGroupId().toString(),
                new AjaxUpdateShowConditionsGroup()
                        .withErrorsByPhrases(singletonList(
                                new ErrorPhrase().withPhrase("Новая фраза ПЗ")
                                        .withErrors(singletonList("Запрещено изменять заархивированную кампанию")))));

        AjaxUpdateShowConditionsResponse expectedResponse = new AjaxUpdateShowConditionsResponse()
                .withGroups(expectedGroups);

        assertThat("получили ожидаемый ответ", actualResponse, beanDiffer(expectedResponse)
                .useCompareStrategy(onlyExpectedFields()));
    }

}
