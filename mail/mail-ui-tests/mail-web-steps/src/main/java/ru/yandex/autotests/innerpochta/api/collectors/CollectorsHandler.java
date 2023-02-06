package ru.yandex.autotests.innerpochta.api.collectors;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.api.AccountInformationHandler.shouldRecieveNoErrorsInHandlersSpec;
import static ru.yandex.autotests.innerpochta.util.handlers.CollectorsConstants.COLLECTORS_PARAM_TAB;
import static ru.yandex.autotests.innerpochta.util.handlers.CollectorsConstants.COLLECTOR_PARAM_COLLECTORS;
import static ru.yandex.autotests.innerpochta.util.handlers.CollectorsConstants.HANDLER_COLLECTORS;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;

/**
 * Created by mabelpines on 14.05.15.
 */
public class CollectorsHandler {
    private RequestSpecBuilder reqSpecBuilder;

    private CollectorsHandler(){
        reqSpecBuilder = new RequestSpecBuilder()
                .addParam(PARAM_MODEL_0, HANDLER_COLLECTORS);
    }

    public static CollectorsHandler collectorsHandler() {
        return new CollectorsHandler();
    }

    public CollectorsHandler withAuth(RestAssuredAuthRule auth){
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public CollectorsHandler withCollectrosTab(){
        reqSpecBuilder.addParam(COLLECTORS_PARAM_TAB, COLLECTOR_PARAM_COLLECTORS);
        return this;
    }

    public Response callCollectorsHandler() {
        Response resp = given().spec(reqSpecBuilder.build())
                .basePath(apiProps().modelsUrl())
                .expect().spec(shouldRecieveNoErrorsInHandlersSpec())
                .when()
                .post();
        return resp;
    }
}
