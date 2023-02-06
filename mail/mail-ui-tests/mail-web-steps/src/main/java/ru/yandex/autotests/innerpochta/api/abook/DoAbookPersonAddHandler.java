package ru.yandex.autotests.innerpochta.api.abook;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.api.RestAssuredLoggingFilter.log;
import static ru.yandex.autotests.innerpochta.api.AccountInformationHandler.shouldRecieveNoErrorsInHandlersSpec;
import static ru.yandex.autotests.innerpochta.util.handlers.AbookConstants.ABOOK_PERSON_DESCR;
import static ru.yandex.autotests.innerpochta.util.handlers.AbookConstants.ABOOK_PERSON_FIRST_NAME;
import static ru.yandex.autotests.innerpochta.util.handlers.AbookConstants.ABOOK_PERSON_LAST_NAME;
import static ru.yandex.autotests.innerpochta.util.handlers.AbookConstants.ABOOK_PERSON_MAIL_ADDR;
import static ru.yandex.autotests.innerpochta.util.handlers.AbookConstants.ABOOK_PERSON_MIDDLE_NAME;
import static ru.yandex.autotests.innerpochta.util.handlers.AbookConstants.ABOOK_PERSON_TEL_LIST;
import static ru.yandex.autotests.innerpochta.util.handlers.AbookConstants.HANDLER_DO_ABOOK_PERSON_ADD;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;

/**
 * Created by mabelpines on 04.09.15.
 */
public class DoAbookPersonAddHandler {
    private RequestSpecBuilder reqSpecBuilder;

    private DoAbookPersonAddHandler() {
        reqSpecBuilder = new RequestSpecBuilder()
            .addParam(PARAM_MODEL_0, HANDLER_DO_ABOOK_PERSON_ADD);
    }

    public static DoAbookPersonAddHandler doAbookPersonAddHandler() {
        return new DoAbookPersonAddHandler();
    }

    public DoAbookPersonAddHandler withAuth(RestAssuredAuthRule auth) {
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public DoAbookPersonAddHandler withDescr(String description) {
        reqSpecBuilder.addParam(ABOOK_PERSON_DESCR, description);
        return this;
    }

    public DoAbookPersonAddHandler withFirstName(String firstName) {
        reqSpecBuilder.addParam(ABOOK_PERSON_FIRST_NAME, firstName);
        return this;
    }

    public DoAbookPersonAddHandler withLastName(String lastName) {
        reqSpecBuilder.addParam(ABOOK_PERSON_LAST_NAME, lastName);
        return this;
    }

    public DoAbookPersonAddHandler withEmail(String email) {
        reqSpecBuilder.addParam(ABOOK_PERSON_MAIL_ADDR, email);
        return this;
    }

    public DoAbookPersonAddHandler withMiddleName(String middleName) {
        reqSpecBuilder.addParam(ABOOK_PERSON_MIDDLE_NAME, middleName);
        return this;
    }

    public DoAbookPersonAddHandler withTelList(String telList) {
        reqSpecBuilder.addParam(ABOOK_PERSON_TEL_LIST, telList);
        return this;
    }

    public Response callDoAbookPersonAddHandler() {
        Response resp = given().spec(reqSpecBuilder.build())
            .filter(log())
            .basePath(apiProps().modelsUrl())
            .expect().spec(shouldRecieveNoErrorsInHandlersSpec())
            .when()
            .post();
        return resp;
    }
}
