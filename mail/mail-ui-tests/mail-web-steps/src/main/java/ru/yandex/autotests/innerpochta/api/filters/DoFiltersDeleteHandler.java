package ru.yandex.autotests.innerpochta.api.filters;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_PARAM_ID;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.HANDLER_DO_FILTERS_DELETE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;

/**
 * Created by mabelpines on 31.08.15.
 */
public class DoFiltersDeleteHandler {
    private RequestSpecBuilder reqSpecBuilder;

    private DoFiltersDeleteHandler(){
        reqSpecBuilder = new RequestSpecBuilder()
                .addParam(PARAM_MODEL_0, HANDLER_DO_FILTERS_DELETE);
    }

    public static DoFiltersDeleteHandler doFiltersDeleteHandler() { return new DoFiltersDeleteHandler(); }

    public DoFiltersDeleteHandler withAuth(RestAssuredAuthRule auth){
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public DoFiltersDeleteHandler withId(String id){
        reqSpecBuilder.addParam(FILTERS_PARAM_ID, id);
        return this;
    }

    public Response callFiltersDeleteHandler() {
        return given().spec(reqSpecBuilder.build())
                .basePath(apiProps().modelsUrl())
                .post();
    }
}
