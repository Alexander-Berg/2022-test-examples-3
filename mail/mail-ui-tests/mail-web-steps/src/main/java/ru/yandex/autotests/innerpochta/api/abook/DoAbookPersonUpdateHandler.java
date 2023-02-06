package ru.yandex.autotests.innerpochta.api.abook;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.api.RestAssuredLoggingFilter.log;
import static ru.yandex.autotests.innerpochta.api.AccountInformationHandler.shouldRecieveNoErrorsInHandlersSpec;
import static ru.yandex.autotests.innerpochta.util.handlers.AbookConstants.ABOOK_PERSON_DESCR;
import static ru.yandex.autotests.innerpochta.util.handlers.AbookConstants.ABOOK_PERSON_FIRST_NAME;
import static ru.yandex.autotests.innerpochta.util.handlers.AbookConstants.ABOOK_PERSON_ID;
import static ru.yandex.autotests.innerpochta.util.handlers.AbookConstants.ABOOK_PERSON_LAST_NAME;
import static ru.yandex.autotests.innerpochta.util.handlers.AbookConstants.ABOOK_PERSON_MAIL_ADDR;
import static ru.yandex.autotests.innerpochta.util.handlers.AbookConstants.ABOOK_PERSON_MIDDLE_NAME;
import static ru.yandex.autotests.innerpochta.util.handlers.AbookConstants.ABOOK_PERSON_TEL_LIST;
import static ru.yandex.autotests.innerpochta.util.handlers.AbookConstants.HANDLER_DO_ABOOK_PERSON_UPDATE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_SHARED_0;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_1;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;

/**
 * @author a-zoshchuk
 */
public class DoAbookPersonUpdateHandler {
    private RequestSpecBuilder reqSpecBuilder;

    private DoAbookPersonUpdateHandler() {
        reqSpecBuilder = new RequestSpecBuilder()
            .addParam(PARAM_MODEL_0, HANDLER_DO_ABOOK_PERSON_UPDATE);
    }

    public static DoAbookPersonUpdateHandler doAbookPersonUpdateHandler() {
        return new DoAbookPersonUpdateHandler();
    }

    public DoAbookPersonUpdateHandler withAuth(RestAssuredAuthRule auth) {
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public DoAbookPersonUpdateHandler withSharedFlag() {
        reqSpecBuilder.addParam(PARAM_SHARED_0, STATUS_1);
        return this;
    }

    public DoAbookPersonUpdateHandler withPersonID(String cid) {
        reqSpecBuilder.addParam(ABOOK_PERSON_ID, cid);
        return this;
    }

    public DoAbookPersonUpdateHandler withDescr(String description) {
        reqSpecBuilder.addParam(ABOOK_PERSON_DESCR, description);
        return this;
    }

    public DoAbookPersonUpdateHandler withFirstName(String firstName) {
        reqSpecBuilder.addParam(ABOOK_PERSON_FIRST_NAME, firstName);
        return this;
    }

    public DoAbookPersonUpdateHandler withLastName(String lastName) {
        reqSpecBuilder.addParam(ABOOK_PERSON_LAST_NAME, lastName);
        return this;
    }

    public DoAbookPersonUpdateHandler withEmail(String email) {
        reqSpecBuilder.addParam(ABOOK_PERSON_MAIL_ADDR, email);
        return this;
    }

    public DoAbookPersonUpdateHandler withMiddleName(String middleName) {
        reqSpecBuilder.addParam(ABOOK_PERSON_MIDDLE_NAME, middleName);
        return this;
    }

    public DoAbookPersonUpdateHandler withTelList(String telList) {
        reqSpecBuilder.addParam(ABOOK_PERSON_TEL_LIST, telList);
        return this;
    }

    public Response callDoAbookPersonUpdateHandler() {
        Response resp = given().spec(reqSpecBuilder.build())
            .filter(log())
            .basePath(apiProps().modelsUrl())
            .expect().spec(shouldRecieveNoErrorsInHandlersSpec())
            .when()
            .post();
        return resp;
    }
}
