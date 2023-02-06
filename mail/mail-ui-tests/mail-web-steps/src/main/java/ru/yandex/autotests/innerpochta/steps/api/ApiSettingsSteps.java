package ru.yandex.autotests.innerpochta.steps.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.steps.beans.Sign;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static ru.yandex.autotests.innerpochta.api.settings.DoSettingsHandler.doSettingsHandler;
import static ru.yandex.autotests.innerpochta.api.settings.SettingsHandler.settingsHandler;
import static ru.yandex.autotests.innerpochta.steps.DefaultSteps.sign;
import static ru.yandex.autotests.innerpochta.steps.api.ApiDefaultSteps.getJsonPathConfig;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.LIST_SIGNS;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_TRUE;

/**
 * Created by mabelpines on 16.04.15.
 */
public class ApiSettingsSteps {

    public RestAssuredAuthRule auth;
    private Gson gson = new Gson();

    public ApiSettingsSteps withAuth(RestAssuredAuthRule auth){
        this.auth = auth;
        return this;
    }

    @Step("Вызов api-метода: do-settings. Обновляем количество подписей аккаунта. Новое значение: {0} шт.")
    public ApiSettingsSteps changeSignsAmountTo(int signsAmount){
        String params = buildJsonFromListWithRootEl(getSignsText(signsAmount), LIST_SIGNS);
        doSettingsHandler().withAuth(auth).withActual(STATUS_TRUE).withList(LIST_SIGNS).withParams(params)
                .callDoSettings();
        return this;
    }

    @Step("Вызов api-метода: do-settings. Обновляем подписи аккаунта.")
    public ApiSettingsSteps changeSignsWithTextAndAmount(Sign ... signatures){
        doSettingsHandler().withAuth(auth).withActual(STATUS_TRUE).withList(LIST_SIGNS)
                .withParams(buildJsonFromListWithRootEl(Arrays.asList(signatures), LIST_SIGNS)).callDoSettings();
        return this;
    }


    @Step("Вызов api-метода: do-settings с параметрами: {0}")
    public ApiSettingsSteps callWith(Map<String, String> params){
        doSettingsHandler().withAuth(auth).withParams(params).callDoSettings();
        return this;
    }

    @Step("Вызов api-метода: do-settings с параметрами: list:{0}; {1}")
    public ApiSettingsSteps callWithListAndParams(Map<String, ?> params){
        doSettingsHandler().withAuth(auth).withParams(params).callDoSettings();
        return this;
    }

    @Step("Вызов api-метода: do-settings. “{0}“")
    public ApiSettingsSteps callWithListAndParams(String allureComment, Map<String, ?> params){
        doSettingsHandler().withAuth(auth).withParams(params).callDoSettings();
        return this;
    }


    @Step("Получаем персональные настройки пользователя")
    public String getUserSettings(String attribute) {
        return settingsHandler().withAuth(auth).withList(attribute).withActual(true).callSettigs().then().extract()
                .jsonPath(getJsonPathConfig()).getObject("models[0].data."+ attribute, String.class);
    }

    private List<Sign> getSignsText(int signsAmount){
        List<Sign> signs = new ArrayList<>();
        for (int i=0; i<signsAmount; i++){
            signs.add(sign(Utils.getRandomName()));
        }
        return signs;
    }

    private <T> String buildJsonFromListWithRootEl(List<T> list, String rootElement){
        JsonObject signsJson = new JsonObject();
        signsJson.add(rootElement, gson.toJsonTree(list));
        return signsJson.toString();
    }
}
