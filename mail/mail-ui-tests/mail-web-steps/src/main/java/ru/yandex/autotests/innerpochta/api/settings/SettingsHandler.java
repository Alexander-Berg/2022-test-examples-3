package ru.yandex.autotests.innerpochta.api.settings;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_EEXP;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_EXP;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_1;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SETTINGS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.HANDLER_SETTINGS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAMS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_ACTUAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LIST;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;

/**
 * Created by mabelpines on 24.04.15.
 */
public class SettingsHandler {
    private RequestSpecBuilder reqSpecBuilder;

    private SettingsHandler() {
        reqSpecBuilder = new RequestSpecBuilder()
            .addParam(PARAM_MODEL_0, HANDLER_SETTINGS)
            .addParam(PARAM_EXP, EMPTY_STR)
            .addParam(PARAM_EEXP, EMPTY_STR);
    }

    public static SettingsHandler settingsHandler() {
        return new SettingsHandler();
    }

    public SettingsHandler withAuth(RestAssuredAuthRule auth) {
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public SettingsHandler withParam(String paramName, String paramVal) {
        reqSpecBuilder.addParam(paramName, paramVal);
        return this;
    }

    public SettingsHandler withParamsList(Map<String, String> params) {
        reqSpecBuilder.addParam(SETTINGS_PARAMS, params);
        return this;
    }

    public SettingsHandler withList(String list) {
        reqSpecBuilder
            .addParam(PARAM_MODEL_1, SETTINGS)
            .addParam(SETTINGS_PARAM_LIST, list);
        return this;
    }

    public SettingsHandler withActual(boolean status) {
        reqSpecBuilder.addParam(SETTINGS_PARAM_ACTUAL, status);
        return this;
    }

    public Response callSettigs() {
        return given().spec(reqSpecBuilder.build())
            .basePath(apiProps().modelsUrl())
            .post();
    }
}
