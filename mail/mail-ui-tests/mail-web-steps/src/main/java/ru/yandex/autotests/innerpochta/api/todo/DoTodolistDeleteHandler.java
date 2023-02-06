package ru.yandex.autotests.innerpochta.api.todo;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.handlers.TodoConstants.HANDLER_DO_TODOLIST_DELETE;
import static ru.yandex.autotests.innerpochta.util.handlers.TodoConstants.LIST_EXTERNAL_ID;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;

/**
 * Created by mabelpines on 11.02.16.
 */
public class DoTodolistDeleteHandler {

    private RequestSpecBuilder reqSpecBuilder;

    private DoTodolistDeleteHandler(){
        reqSpecBuilder = new RequestSpecBuilder()
                .addParam(PARAM_MODEL_0, HANDLER_DO_TODOLIST_DELETE);
    }

    public static DoTodolistDeleteHandler doTodolistDeleteHandler() {
        return new DoTodolistDeleteHandler();
    }

    public DoTodolistDeleteHandler withAuth(RestAssuredAuthRule auth){
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public DoTodolistDeleteHandler withExtId(String externalId){
        reqSpecBuilder.addParam(LIST_EXTERNAL_ID, externalId);
        return this;
    }

    public Response callDoTodolistDeleteHandler() {
        Response resp = given().spec(reqSpecBuilder.build())
                .basePath(apiProps().modelsUrl())
                .post();
        return resp;
    }
}
