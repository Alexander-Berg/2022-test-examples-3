package ru.yandex.autotests.innerpochta.api;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSender;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.api.RestAssuredLoggingFilter.log;
import static ru.yandex.autotests.innerpochta.api.AccountInformationHandler.retryRequestIfNot200;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_EEXP;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_EXP;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.handlers.SearchConstants.HANDLER_DO_CLEAN_SUGGEST;
import static ru.yandex.autotests.innerpochta.util.handlers.SearchConstants.SEARCH_PARAM_ALL;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;


/**
 * @author oleshko
 */
public class DoCleanSuggestHistoryHandler {

    private RequestSpecBuilder reqSpecBuilder;

    private DoCleanSuggestHistoryHandler() {
        reqSpecBuilder = new RequestSpecBuilder()
            .addParam(PARAM_MODEL_0, HANDLER_DO_CLEAN_SUGGEST)
            .addParam(PARAM_EXP, EMPTY_STR)
            .addParam(PARAM_EEXP, EMPTY_STR);
    }

    public static DoCleanSuggestHistoryHandler doCleanSuggestHistoryHandler() {
        return new DoCleanSuggestHistoryHandler();
    }

    public DoCleanSuggestHistoryHandler withAuth(RestAssuredAuthRule auth) {
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public DoCleanSuggestHistoryHandler withAll(String all) {
        reqSpecBuilder.addParam(SEARCH_PARAM_ALL, all);
        return this;
    }

    public Response callDoCleanSuggest() {
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



