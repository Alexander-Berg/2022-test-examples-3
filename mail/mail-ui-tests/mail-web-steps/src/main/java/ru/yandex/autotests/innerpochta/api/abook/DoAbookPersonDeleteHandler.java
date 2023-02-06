package ru.yandex.autotests.innerpochta.api.abook;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.api.AccountInformationHandler.shouldRecieveNoErrorsInHandlersSpec;
import static ru.yandex.autotests.innerpochta.util.handlers.AbookConstants.ABOOK_PERSON_PARAM_CID;
import static ru.yandex.autotests.innerpochta.util.handlers.AbookConstants.HANDLER_DO_ABOOK_PERSON_DELETE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;

/**
 * Created by mabelpines on 19.05.15.
 */
public class DoAbookPersonDeleteHandler {
    private RequestSpecBuilder reqSpecBuilder;

    private DoAbookPersonDeleteHandler() {
        reqSpecBuilder = new RequestSpecBuilder()
            .addParam(PARAM_MODEL_0, HANDLER_DO_ABOOK_PERSON_DELETE);
    }

    public static DoAbookPersonDeleteHandler doAbookPersonDeleteHandler() {
        return new DoAbookPersonDeleteHandler();
    }

    public DoAbookPersonDeleteHandler withAuth(RestAssuredAuthRule auth) {
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public DoAbookPersonDeleteHandler withCid(String cid) {
        reqSpecBuilder.addParam(ABOOK_PERSON_PARAM_CID, cid);
        return this;
    }

    public Response callDoAbookPersonDeleteHandler() {
        Response resp = given().spec(reqSpecBuilder.build())
            .basePath(apiProps().modelsUrl())
            .expect().spec(shouldRecieveNoErrorsInHandlersSpec())
            .when()
            .post();
        return resp;
    }
}
