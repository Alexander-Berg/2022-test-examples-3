package ru.yandex.autotests.innerpochta.api.abook;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.api.AccountInformationHandler.shouldRecieveNoErrorsInHandlersSpec;
import static ru.yandex.autotests.innerpochta.util.handlers.AbookConstants.HANDLER_ABOOK_CONTACTS;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_SHARED_0;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_1;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;

/**
 * @author mabelpines
 */
public class AbookContactsHandler {
    private RequestSpecBuilder reqSpecBuilder;

    private AbookContactsHandler() {
        reqSpecBuilder = new RequestSpecBuilder()
            .addParam(PARAM_MODEL_0, HANDLER_ABOOK_CONTACTS);
    }

    public static AbookContactsHandler abookContactsHandler() {
        return new AbookContactsHandler();
    }

    public AbookContactsHandler withAuth(RestAssuredAuthRule auth) {
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public AbookContactsHandler withSharedContacts() {
        reqSpecBuilder.addParam(PARAM_SHARED_0, STATUS_1);
        return this;
    }

    public Response callAbookContactsHandler() {
        Response resp = given().spec(reqSpecBuilder.build())
            .basePath(apiProps().modelsUrl())
            .expect().spec(shouldRecieveNoErrorsInHandlersSpec())
            .when()
            .post();
        return resp;
    }
}
