package ru.yandex.autotests.innerpochta.api.todo;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.api.AccountInformationHandler.shouldRecieveNoErrorsInHandlersSpec;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.handlers.TodoConstants.HANDLER_DO_TODO_SETTINGS;
import static ru.yandex.autotests.innerpochta.util.handlers.TodoConstants.SETTINGS;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;


/**
 * @author marchart
 */
public class DoTodoSettings {

    private RequestSpecBuilder reqSpecBuilder;

    private DoTodoSettings() {
        reqSpecBuilder = new RequestSpecBuilder()
            .addParam(PARAM_MODEL_0, HANDLER_DO_TODO_SETTINGS);
    }

    public static DoTodoSettings doTodoSettings() { return new DoTodoSettings(); }

    public DoTodoSettings withAuth(RestAssuredAuthRule auth) {
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public DoTodoSettings setTodoList() {
        reqSpecBuilder.addParam(SETTINGS, "open=1&state=state%3Dtodo-lists");
        return this;
    }

    public DoTodoSettings hideTodoList() {
        reqSpecBuilder.addParam(SETTINGS, "open=0");
        return this;
    }

    public DoTodoSettings closeTodoList() {
        reqSpecBuilder.addParam(SETTINGS, "open=0&state=state%3Dtodo-lists");
        return this;
    }

    public Response callDoTodoSettings() {
        Response resp = given().spec(reqSpecBuilder.build())
            .basePath(apiProps().modelsUrl())
            .expect().spec(shouldRecieveNoErrorsInHandlersSpec())
            .when()
            .post();
        return resp;
    }
}
