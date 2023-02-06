package ru.yandex.autotests.innerpochta.api.abook;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.api.AccountInformationHandler.shouldRecieveNoErrorsInHandlersSpec;
import static ru.yandex.autotests.innerpochta.util.handlers.AbookConstants.ABOOK_GROUP_PARAM_TID;
import static ru.yandex.autotests.innerpochta.util.handlers.AbookConstants.HANDLER_DO_ABOOK_GROUP_REMOVE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;

/**
 * Created by mabelpines on 15.05.15.
 */
public class DoAbookGroupRemoveHandler {
    private RequestSpecBuilder reqSpecBuilder;

    private DoAbookGroupRemoveHandler(){
        reqSpecBuilder = new RequestSpecBuilder()
                .addParam(PARAM_MODEL_0, HANDLER_DO_ABOOK_GROUP_REMOVE);
    }

    public static DoAbookGroupRemoveHandler doAbookGroupRemoveHandler() {
        return new DoAbookGroupRemoveHandler();
    }

    public DoAbookGroupRemoveHandler withAuth(RestAssuredAuthRule auth){
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public DoAbookGroupRemoveHandler withTid(String tid){
        reqSpecBuilder.addParam(ABOOK_GROUP_PARAM_TID, tid);
        return this;
    }

    public Response callDoAbookGroupRemoveHandler() {
        Response resp = given().spec(reqSpecBuilder.build())
                .basePath(apiProps().modelsUrl())
                .expect().spec(shouldRecieveNoErrorsInHandlersSpec())
                .when()
                .post();
        return resp;
    }
}
