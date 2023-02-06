package ru.yandex.autotests.innerpochta.api.filters;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.api.RestAssuredLoggingFilter.log;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_HANDLER_QUERY_PARAM_1;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_HANDLER_QUERY_PARAM_2;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_HANDLER_QUERY_PARAM_1_VAL;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_HANDLER_QUERY_PARAM_2_VAL;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.CONTENT_TYPE;

/**
 * @author a-zoshchuk
 */
public class UnsubscribeFuritaFiltersHandler {
    private RequestSpecBuilder reqSpecBuilder;

    private UnsubscribeFuritaFiltersHandler(){
        reqSpecBuilder = new RequestSpecBuilder();
    }

    public static UnsubscribeFuritaFiltersHandler unsubscribeFuritaFiltersHandler() {
        return new UnsubscribeFuritaFiltersHandler();
    }

    public UnsubscribeFuritaFiltersHandler withAuth(RestAssuredAuthRule auth){
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpecWithQueryParam());
        return this;
    }

    public Response callUnsubscribeFuritaFiltersHandler() {
        return given().spec(reqSpecBuilder.build())
            .filter(log())
            .basePath(apiProps().newsletterFiltersUrl())
            .contentType(CONTENT_TYPE)
            .queryParam(FILTERS_HANDLER_QUERY_PARAM_1, FILTERS_HANDLER_QUERY_PARAM_1_VAL)
            .queryParam(FILTERS_HANDLER_QUERY_PARAM_2, FILTERS_HANDLER_QUERY_PARAM_2_VAL)
            .post();
    }
}
