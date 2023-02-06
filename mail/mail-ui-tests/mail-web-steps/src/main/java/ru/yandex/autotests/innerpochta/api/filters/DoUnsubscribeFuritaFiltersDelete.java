package ru.yandex.autotests.innerpochta.api.filters;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import java.util.ArrayList;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.api.RestAssuredLoggingFilter.log;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_HANDLER_QUERY_PARAM_1;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_HANDLER_QUERY_PARAM_2;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_HANDLER_QUERY_PARAM_1_VAL;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_HANDLER_QUERY_PARAM_2_VAL;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.CONTENT_TYPE;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;

/**
 * @author a-zoshchuk
 */
@SuppressWarnings("UnusedReturnValue")
public class DoUnsubscribeFuritaFiltersDelete {
    private RequestSpecBuilder reqSpecBuilder;

    private DoUnsubscribeFuritaFiltersDelete() {
        reqSpecBuilder = new RequestSpecBuilder();
    }

    public static DoUnsubscribeFuritaFiltersDelete doUnsubscribeFuritaFiltersDelete() {
        return new DoUnsubscribeFuritaFiltersDelete();
    }

    public DoUnsubscribeFuritaFiltersDelete withAuth(RestAssuredAuthRule auth) {
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpecWithQueryParam());
        return this;
    }

    public DoUnsubscribeFuritaFiltersDelete withId(ArrayList<String> id, RestAssuredAuthRule auth) {
        reqSpecBuilder.setBody("{\"newsletterFilterIds\":" + id + ",\"_ckey\":\"" + auth.getCkey() + "\"}");
        return this;
    }

    public Response callUnsubscribeFuritaFiltersDelete() {
        return given().spec(reqSpecBuilder.build())
                .filter(log())
                .basePath(apiProps().deleteNewsletterFiltersUrl())
                .contentType(CONTENT_TYPE)
                .queryParam(FILTERS_HANDLER_QUERY_PARAM_1, FILTERS_HANDLER_QUERY_PARAM_1_VAL)
                .queryParam(FILTERS_HANDLER_QUERY_PARAM_2, FILTERS_HANDLER_QUERY_PARAM_2_VAL)
                .post();
    }
}
