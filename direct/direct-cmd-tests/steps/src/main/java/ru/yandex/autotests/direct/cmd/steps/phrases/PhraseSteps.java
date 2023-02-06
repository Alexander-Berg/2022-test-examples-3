package ru.yandex.autotests.direct.cmd.steps.phrases;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditions;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsObjects;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsRequest;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.data.phrases.AjaxCheckMinusWordsRequest;
import ru.yandex.autotests.direct.cmd.data.phrases.AjaxTestPhrasesRequest;
import ru.yandex.autotests.direct.cmd.data.phrases.AjaxTestPhrasesXmlResponse;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.HashMap;
import java.util.Map;

public class PhraseSteps extends DirectBackEndSteps {
    @Step("Изменяем цену на {0}")
    public String changePriceAjaxUpdateShowCondition(Double price, Long phraseId, Long campaignId, Long groupId, String ulogin) {
        AjaxUpdateShowConditions ajaxUpdateShowConditions = new AjaxUpdateShowConditions();
        ajaxUpdateShowConditions.setPrice(String.valueOf(price));

        Map phrases = new HashMap<String, AjaxUpdateShowConditions>();
        phrases.put(phraseId, ajaxUpdateShowConditions);

        AjaxUpdateShowConditionsObjects ajaxUpdateShowConditionsObjects = new AjaxUpdateShowConditionsObjects();
        ajaxUpdateShowConditionsObjects.setEdited(phrases);

        AjaxUpdateShowConditionsRequest ajaxUpdateShowConditionsRequest = new AjaxUpdateShowConditionsRequest();
        ajaxUpdateShowConditionsRequest.setCid(String.valueOf(campaignId));
        ajaxUpdateShowConditionsRequest.setGroupPhrases(String.valueOf(groupId), ajaxUpdateShowConditionsObjects);
        ajaxUpdateShowConditionsRequest.setUlogin(ulogin);
        return postAjaxUpdateShowCondition(ajaxUpdateShowConditionsRequest);
    }

    @Step("POST cmd=ajaxUpdateShowConditions")
    public String postAjaxUpdateShowCondition(AjaxUpdateShowConditionsRequest request) {
        return post(CMD.AJAX_UPDATE_SHOW_CONDITIONS, request, String.class);
    }

    @Step("POST cmd=ajaxTestPhrases (проверяем фразы)")
    public AjaxTestPhrasesXmlResponse ajaxTestPhrases(AjaxTestPhrasesRequest request) {
        return post(CMD.AJAX_TEST_PHRASES, request, AjaxTestPhrasesXmlResponse.class);
    }

    @Step("Проверяем фразы: {0}")
    public AjaxTestPhrasesXmlResponse ajaxTestPhrases(String phrases) {
        return ajaxTestPhrases(new AjaxTestPhrasesRequest().withPhrases(phrases));
    }

    @Step("POST cmd=ajaxCheckCampMinusWords (валидация минус-слов на странице кампании)")
    public CommonResponse ajaxCheckCampMinusWords(AjaxCheckMinusWordsRequest request) {
        return post(CMD.AJAX_CHECK_CAMP_MINUS_WORDS, request, CommonResponse.class);
    }


    @Step("POST cmd=ajaxCheckBannersMinusWords (валидация минус-слов на странице группы)")
    public CommonResponse ajaxCheckBannersMinusWords(AjaxCheckMinusWordsRequest request) {
        return post(CMD.AJAX_CHECK_BANNERS_MINUS_WORDS, request, CommonResponse.class);
    }

}
