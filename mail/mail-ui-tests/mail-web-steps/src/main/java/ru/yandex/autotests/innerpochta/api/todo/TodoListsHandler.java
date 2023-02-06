package ru.yandex.autotests.innerpochta.api.todo;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.api.AccountInformationHandler.shouldRecieveNoErrorsInHandlersSpec;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.handlers.TodoConstants.HANDLER_TODO_LISTS;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;

/**
 * Created by mabelpines on 11.02.16.
 */
public class TodoListsHandler {

    private RequestSpecBuilder reqSpecBuilder;

    private TodoListsHandler(){
        reqSpecBuilder = new RequestSpecBuilder()
                .addParam(PARAM_MODEL_0, HANDLER_TODO_LISTS);
    }

    public static TodoListsHandler todoListsHandler() {
        return new TodoListsHandler();
    }


    public TodoListsHandler withAuth(RestAssuredAuthRule auth){
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public Response callTodoListsHandler() {
        Response resp = given().spec(reqSpecBuilder.build())
                .basePath(apiProps().modelsUrl())
                .expect().spec(shouldRecieveNoErrorsInHandlersSpec())
                .when()
                .post();
        return resp;
    }
}
