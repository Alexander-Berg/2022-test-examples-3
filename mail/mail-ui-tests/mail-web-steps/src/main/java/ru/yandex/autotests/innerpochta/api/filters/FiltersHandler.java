package ru.yandex.autotests.innerpochta.api.filters;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.api.RestAssuredLoggingFilter.log;
import static ru.yandex.autotests.innerpochta.api.AccountInformationHandler.shouldRecieveNoErrorsInHandlersSpec;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.HANDLER_FILTERS;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;

/**
 * Created by mabelpines on 13.05.15.
 */
public class FiltersHandler {
    private RequestSpecBuilder reqSpecBuilder;

    private FiltersHandler(){
        reqSpecBuilder = new RequestSpecBuilder()
                .addParam(PARAM_MODEL_0, HANDLER_FILTERS);
    }

    public static FiltersHandler filtersHandler() {
        return new FiltersHandler();
    }

    public FiltersHandler withAuth(RestAssuredAuthRule auth){
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public Response callFiltersHandler() {
        Response resp = given().spec(reqSpecBuilder.build())
                .filter(log())
                .basePath(apiProps().modelsUrl())
                .expect().spec(shouldRecieveNoErrorsInHandlersSpec())
                .when()
                .post();
        return resp;
    }
}
