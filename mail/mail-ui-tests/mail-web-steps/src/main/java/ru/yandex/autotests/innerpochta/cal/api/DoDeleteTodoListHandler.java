package ru.yandex.autotests.innerpochta.cal.api;

import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.steps.beans.bodyReq.BodyReq;
import ru.yandex.autotests.innerpochta.steps.beans.bodyReq.Model;
import ru.yandex.autotests.innerpochta.steps.beans.bodyReq.Params;
import ru.yandex.autotests.innerpochta.steps.beans.calAccount.CalAccount;

import java.util.Collections;
import java.util.function.Function;

import static ru.yandex.autotests.innerpochta.cal.api.CalApiConfig.apiConfig;
import static ru.yandex.autotests.innerpochta.cal.api.InfoHandler.infoHandler;
import static ru.yandex.autotests.innerpochta.cal.util.handlers.SettingsConsts.HANDLER_DO_DELETE_TODO_LIST;

/**
 * @author cosmopanda
 */
public class DoDeleteTodoListHandler {

    private BodyReq deleteTodoListBody;
    private RestAssuredAuthRule filter;
    private CalAccount accInfo;

    private DoDeleteTodoListHandler() {
    }

    public static DoDeleteTodoListHandler deleteTodoList() {
        return new DoDeleteTodoListHandler();
    }

    public DoDeleteTodoListHandler withAuth(RestAssuredAuthRule auth) {
        accInfo = infoHandler().withFilter(auth).callInfoHandler();
        filter = auth;
        return this;
    }

    public DoDeleteTodoListHandler withListID(String listID) {
        deleteTodoListBody = new BodyReq().withModels(Collections.singletonList(
            new Model()
                .withName(HANDLER_DO_DELETE_TODO_LIST)
                .withParams(new Params().withListId(listID))
            ));
        return this;
    }

    public Response callDeleteTodoList() {
        return apiConfig()
            .dodeletetodolist()
            .withReq(req -> req.addFilter(filter).setBody(deleteTodoListBody))
            .withContentTypeHeader("application/json")
            .withXyandexmayauidHeader(accInfo.getUid())
            .withXyandexmayackeyHeader(accInfo.getCkey())
            .post(Function.identity());
    }
}
