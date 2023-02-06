package ru.yandex.autotests.innerpochta.api.filters;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.api.AccountInformationHandler.shouldRecieveNoErrorsInHandlersSpec;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_PARAM_EMAIL;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.HANDLER_DO_FILTERS_BLACKLIST_REMOVE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;

/**
 * Created by mabelpines on 12.10.15.
 */
@SuppressWarnings("UnusedReturnValue")
public class DoFiltersBlackListRemoveHandler {
    private RequestSpecBuilder reqSpecBuilder;

    private DoFiltersBlackListRemoveHandler() {
        reqSpecBuilder = new RequestSpecBuilder()
            .addParam(PARAM_MODEL_0, HANDLER_DO_FILTERS_BLACKLIST_REMOVE);
    }

    public static DoFiltersBlackListRemoveHandler doFiltersBlackListRemove() {
        return new DoFiltersBlackListRemoveHandler();
    }

    public DoFiltersBlackListRemoveHandler withAuth(RestAssuredAuthRule auth) {
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public DoFiltersBlackListRemoveHandler withEmail(String email) {
        reqSpecBuilder.addParam(FILTERS_PARAM_EMAIL, email);
        return this;

    }

    public Response callDoFiltersBlackListRemove() {
        return given().spec(reqSpecBuilder.build())
            .basePath(apiProps().modelsUrl())
            .expect().spec(shouldRecieveNoErrorsInHandlersSpec())
            .when()
            .post();
    }
}
