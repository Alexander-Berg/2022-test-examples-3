package ru.yandex.autotests.innerpochta.api.todo;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.api.AccountInformationHandler.shouldRecieveNoErrorsInHandlersSpec;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.handlers.TodoConstants.HANDLER_DO_TODOLIST_CREATE;
import static ru.yandex.autotests.innerpochta.util.handlers.TodoConstants.LIST_TITLE;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;


/**
 * Created by mabelpines on 11.02.16.
 */
public class DoTodolistCreateHandler {

    private RequestSpecBuilder reqSpecBuilder;

    private DoTodolistCreateHandler(){
        reqSpecBuilder = new RequestSpecBuilder()
                .addParam(PARAM_MODEL_0, HANDLER_DO_TODOLIST_CREATE);
    }

    public static DoTodolistCreateHandler doTodolistCreateHandler() { return new DoTodolistCreateHandler(); }

    public DoTodolistCreateHandler withAuth(RestAssuredAuthRule auth){
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public DoTodolistCreateHandler withTitle(String title) {
        reqSpecBuilder.addParam(LIST_TITLE, title);
        return this;
    }

    public Response callDoTodolistCreateHandler() {
        Response resp = given().spec(reqSpecBuilder.build())
                .basePath(apiProps().modelsUrl())
                .expect().spec(shouldRecieveNoErrorsInHandlersSpec())
                .when()
                .post();
        return resp;
    }
}
