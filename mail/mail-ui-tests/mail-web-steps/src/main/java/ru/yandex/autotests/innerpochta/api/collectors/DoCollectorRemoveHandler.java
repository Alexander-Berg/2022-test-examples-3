package ru.yandex.autotests.innerpochta.api.collectors;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.api.AccountInformationHandler.shouldRecieveNoErrorsInHandlersSpec;
import static ru.yandex.autotests.innerpochta.util.handlers.CollectorsConstants.COLLECTORS_PARAM_EMAIL;
import static ru.yandex.autotests.innerpochta.util.handlers.CollectorsConstants.COLLECTORS_POPID;
import static ru.yandex.autotests.innerpochta.util.handlers.CollectorsConstants.HANDLER_DO_COLLECTOR_REMOVE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;

/**
 * Created by mabelpines on 14.05.15.
 */
public class DoCollectorRemoveHandler {
    private RequestSpecBuilder reqSpecBuilder;

    private DoCollectorRemoveHandler(){
        reqSpecBuilder = new RequestSpecBuilder()
                .addParam(PARAM_MODEL_0, HANDLER_DO_COLLECTOR_REMOVE);
    }

    public static DoCollectorRemoveHandler doCollectorRemoveHandler() {
        return new DoCollectorRemoveHandler();
    }

    public DoCollectorRemoveHandler withAuth(RestAssuredAuthRule auth){
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public DoCollectorRemoveHandler withPopid(String popid){
        reqSpecBuilder.addParam(COLLECTORS_POPID, popid);
        return this;
    }

    public DoCollectorRemoveHandler withEmail(String email){
        reqSpecBuilder.addParam(COLLECTORS_PARAM_EMAIL, email);
        return this;
    }

    public Response callDoCollectorRemoveHandler() {
        Response resp = given().spec(reqSpecBuilder.build())
                .basePath(apiProps().modelsUrl())
                .expect().spec(shouldRecieveNoErrorsInHandlersSpec())
                .when()
                .post();
        return resp;
    }
}
