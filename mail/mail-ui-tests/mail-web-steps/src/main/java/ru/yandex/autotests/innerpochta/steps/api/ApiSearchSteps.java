package ru.yandex.autotests.innerpochta.steps.api;

import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.qatools.allure.annotations.Step;

import static ru.yandex.autotests.innerpochta.api.DoCleanSuggestHistoryHandler.doCleanSuggestHistoryHandler;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_TRUE;

/**
 * @author oleshko
 */
public class ApiSearchSteps {

    public RestAssuredAuthRule auth;

    public ApiSearchSteps withAuth(RestAssuredAuthRule auth) {
        this.auth = auth;
        return this;
    }

    @Step("Вызов api-метода: do-clean-suggest. Очищаем весь поисковый саджест")
    public ApiSearchSteps cleanSuggestHistory() {
        doCleanSuggestHistoryHandler().withAuth(auth).withAll(STATUS_TRUE).callDoCleanSuggest();
        return this;
    }
}
