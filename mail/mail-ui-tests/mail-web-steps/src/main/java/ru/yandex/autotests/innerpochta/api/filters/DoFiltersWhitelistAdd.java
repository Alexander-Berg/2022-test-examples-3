package ru.yandex.autotests.innerpochta.api.filters;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.api.AccountInformationHandler.shouldRecieveNoErrorsInHandlersSpec;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_PARAM_EMAIL;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.HANDLER_DO_FILTERS_WHITELIST_ADD;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;

public class DoFiltersWhitelistAdd {
    private RequestSpecBuilder reqSpecBuilder;

    private DoFiltersWhitelistAdd() {
        reqSpecBuilder = new RequestSpecBuilder()
            .addParam(PARAM_MODEL_0, HANDLER_DO_FILTERS_WHITELIST_ADD);
    }

    public static DoFiltersWhitelistAdd doFiltersWhitelistAdd() { return new DoFiltersWhitelistAdd(); }

    public DoFiltersWhitelistAdd withAuth(RestAssuredAuthRule auth) {
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public DoFiltersWhitelistAdd withEmail(String email) {
        reqSpecBuilder.addParam(FILTERS_PARAM_EMAIL, email);
        return this;

    }

    public Response callDoFiltersWhitelistAdd() {
        return given().spec(reqSpecBuilder.build())
            .basePath(apiProps().modelsUrl())
            .expect().spec(shouldRecieveNoErrorsInHandlersSpec())
            .when()
            .post();
    }
}
