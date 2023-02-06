package ru.yandex.autotests.innerpochta.api.settings;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSender;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.api.RestAssuredLoggingFilter.log;
import static ru.yandex.autotests.innerpochta.api.AccountInformationHandler.retryRequestIfNot200;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_EEXP;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_EXP;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_1;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SETTINGS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.HANDLER_DO_SETTINGS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAMS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_ACTUAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LIST;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;


/**
 * Created by mabelpines on 16.04.15.
 */
public class DoSettingsHandler {

    private RequestSpecBuilder reqSpecBuilder;

    private DoSettingsHandler() {
        reqSpecBuilder = new RequestSpecBuilder()
            .addParam(PARAM_MODEL_0, HANDLER_DO_SETTINGS)
            .addParam(PARAM_EXP, EMPTY_STR)
            .addParam(PARAM_EEXP, EMPTY_STR);
    }

    public static DoSettingsHandler doSettingsHandler() {
        return new DoSettingsHandler();
    }

    public DoSettingsHandler withAuth(RestAssuredAuthRule auth) {
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public DoSettingsHandler withList(String list) {
        reqSpecBuilder
            .addParam(SETTINGS_PARAM_LIST, list)
            .addParam(PARAM_MODEL_1, SETTINGS);
        return this;
    }

    public DoSettingsHandler withActual(String actual) {
        reqSpecBuilder.addParam(SETTINGS_PARAM_ACTUAL, actual);
        return this;
    }

    public DoSettingsHandler withParams(Map<String, ?> params) {
        reqSpecBuilder.addParam(SETTINGS_PARAMS, params);
        return this;
    }

    public DoSettingsHandler withParams(String params) {
        reqSpecBuilder.addParam(SETTINGS_PARAMS, params);
        return this;
    }

    public Response callDoSettings() {
        RequestSender spec = given().filter(log())
            .spec(reqSpecBuilder.build())
            .basePath(apiProps().modelsUrl())
            .expect()
            .when();
        return retryRequestIfNot200(spec);
    }

    public RequestSpecBuilder getReqSpecBuilder() {
        return this.reqSpecBuilder;
    }
}



