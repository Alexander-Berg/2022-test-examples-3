package ru.yandex.autotests.innerpochta.api.filters;

import io.restassured.builder.RequestSpecBuilder;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;
import static ru.yandex.autotests.innerpochta.api.RestAssuredLoggingFilter.log;
import java.lang.String;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_HANDLER_QUERY_PARAM_1;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_HANDLER_QUERY_PARAM_2;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_HANDLER_QUERY_PARAM_1_VAL;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_HANDLER_QUERY_PARAM_2_VAL;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.CONTENT_TYPE;


/**
 * @author oleshko
 */
public class CreateNewsletterFiltersHandler {
    private RequestSpecBuilder reqSpecBuilder;

    private CreateNewsletterFiltersHandler() {
        reqSpecBuilder = new RequestSpecBuilder();
    }

    public static CreateNewsletterFiltersHandler doCreateNewsletterFiltersHadler() {
        return new CreateNewsletterFiltersHandler();
    }

    public CreateNewsletterFiltersHandler withAuth(RestAssuredAuthRule auth) {
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpecWithQueryParam());
        return this;
    }

    public CreateNewsletterFiltersHandler withParam(String params, RestAssuredAuthRule auth) {
        reqSpecBuilder.setBody("{\"newsletterFilters\":" + params + ",\"_ckey\":\"" + auth.getCkey() + "\"}");
        return this;
    }

    public void callCreateNewsletterFiltersHadler() {
       given().spec(reqSpecBuilder.build())
                .filter(log())
                .basePath(apiProps().createNewsletterFiltersUrl())
                .contentType(CONTENT_TYPE)
                .queryParam(FILTERS_HANDLER_QUERY_PARAM_1, FILTERS_HANDLER_QUERY_PARAM_1_VAL)
                .queryParam(FILTERS_HANDLER_QUERY_PARAM_2, FILTERS_HANDLER_QUERY_PARAM_2_VAL)
                .post();
    }
}
