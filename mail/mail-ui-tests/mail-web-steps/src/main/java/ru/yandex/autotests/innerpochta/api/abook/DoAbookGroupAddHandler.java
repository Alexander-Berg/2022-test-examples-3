package ru.yandex.autotests.innerpochta.api.abook;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Contact;

import java.util.List;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.api.AccountInformationHandler.shouldRecieveNoErrorsInHandlersSpec;
import static ru.yandex.autotests.innerpochta.util.handlers.AbookConstants.ABOOK_GROUP_PARAM_TITLE;
import static ru.yandex.autotests.innerpochta.util.handlers.AbookConstants.ABOOK_PARAM_MCID;
import static ru.yandex.autotests.innerpochta.util.handlers.AbookConstants.HANDLER_DO_ABOOK_GROUP_ADD;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;

/**
 * Created by mabelpines on 15.05.15.
 */
public class DoAbookGroupAddHandler {
    private RequestSpecBuilder reqSpecBuilder;

    private DoAbookGroupAddHandler(){
        reqSpecBuilder = new RequestSpecBuilder()
                .addParam(PARAM_MODEL_0, HANDLER_DO_ABOOK_GROUP_ADD);
    }

    public static DoAbookGroupAddHandler doAbookGroupAddHandler() {
        return new DoAbookGroupAddHandler();
    }

    public DoAbookGroupAddHandler withAuth(RestAssuredAuthRule auth){
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public DoAbookGroupAddHandler withTitle(String title){
        reqSpecBuilder.addParam(ABOOK_GROUP_PARAM_TITLE, title);
        return this;
    }

    public DoAbookGroupAddHandler withContacts(List<Contact> contacts){
        for(Contact contact : contacts) {
            reqSpecBuilder.addParam(ABOOK_PARAM_MCID, contact.getMcid());
        }
        return this;
    }

    public Response callDoAbookGroupAddHandler() {
        Response resp = given().spec(reqSpecBuilder.build())
                .basePath(apiProps().modelsUrl())
                .expect().spec(shouldRecieveNoErrorsInHandlersSpec())
                .when()
                .post();
        return resp;
    }
}
