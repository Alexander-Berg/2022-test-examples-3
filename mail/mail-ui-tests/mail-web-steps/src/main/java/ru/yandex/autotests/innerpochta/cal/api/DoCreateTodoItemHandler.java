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
import static ru.yandex.autotests.innerpochta.cal.util.handlers.SettingsConsts.HANDLER_DO_CREATE_TODO_ITEM;

/**
 * @author cosmopanda
 */
public class DoCreateTodoItemHandler {

    private BodyReq createTodoItemBody = new BodyReq();
    private RestAssuredAuthRule filter;
    private CalAccount accInfo;

    private DoCreateTodoItemHandler() {
    }

    public static DoCreateTodoItemHandler createTodoItem() {
        return new DoCreateTodoItemHandler();
    }

    public DoCreateTodoItemHandler withAuth(RestAssuredAuthRule auth) {
        accInfo = infoHandler().withFilter(auth).callInfoHandler();
        filter = auth;
        return this;
    }

    public DoCreateTodoItemHandler withParams(String listId, String title, String dueDate) {
        createTodoItemBody = new BodyReq().withModels(Collections.singletonList(
            new Model()
                .withName(HANDLER_DO_CREATE_TODO_ITEM)
                .withParams(new Params()
                    .withTitle(listId)
                    .withTitle(title)
                    .withDueDate(dueDate)
                )));
        return this;
    }

    public Response callCreateTodoItem() {
        return apiConfig()
            .docreatetodoitem()
            .withReq(req -> req.addFilter(filter).setBody(createTodoItemBody))
            .withContentTypeHeader("application/json")
            .withXyandexmayauidHeader(accInfo.getUid())
            .withXyandexmayackeyHeader(accInfo.getCkey())
            .post(Function.identity());
    }
}
