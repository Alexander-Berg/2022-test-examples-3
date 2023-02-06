package ru.yandex.autotests.innerpochta.api.filters;

import io.restassured.builder.RequestSpecBuilder;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.api.AccountInformationHandler.shouldRecieveNoErrorsInHandlersSpec;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_PARAM_EMAIL;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.HANDLER_DO_FILTERS_WHITELIST_REMOVE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;

/**
 * @author mariya-murm
 */

public class DoFiltersWhiteListRemoveHandler {
    private RequestSpecBuilder reqSpecBuilder;

    private DoFiltersWhiteListRemoveHandler() {
        reqSpecBuilder = new RequestSpecBuilder()
            .addParam(PARAM_MODEL_0, HANDLER_DO_FILTERS_WHITELIST_REMOVE);
    }

    public static DoFiltersWhiteListRemoveHandler doFiltersWhiteListRemove() {
        return new DoFiltersWhiteListRemoveHandler();
    }

    public DoFiltersWhiteListRemoveHandler withAuth(RestAssuredAuthRule auth) {
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public DoFiltersWhiteListRemoveHandler withEmail(String email) {
        reqSpecBuilder.addParam(FILTERS_PARAM_EMAIL, email);
        return this;

    }

    public void callDoFiltersWhiteListRemove() {
        given().spec(reqSpecBuilder.build())
            .basePath(apiProps().modelsUrl())
            .expect().spec(shouldRecieveNoErrorsInHandlersSpec())
            .when()
            .post();
    }
}
