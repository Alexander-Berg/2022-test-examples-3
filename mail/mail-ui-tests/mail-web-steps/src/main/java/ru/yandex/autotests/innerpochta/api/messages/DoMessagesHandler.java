package ru.yandex.autotests.innerpochta.api.messages;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;

import java.util.List;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.util.handlers.FoldersConstants.FOLDERS_PARAM_FID;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_EEXP;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_EXP;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.HANDLER_DO_MESSAGES;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.MESSAGES_PARAM_ACTION;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.MESSAGES_PARAM_IDS;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.MESSAGES_PARAM_MOVEFILE;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.MESSAGES_PARAM_TAB;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;

/**
 * Created by mabelpines on 22.05.15.
 */
public class DoMessagesHandler {
    private RequestSpecBuilder reqSpecBuilder;

    private DoMessagesHandler() {
        reqSpecBuilder = new RequestSpecBuilder()
            .addParam(PARAM_MODEL_0, HANDLER_DO_MESSAGES)
            .addParam(PARAM_EXP, EMPTY_STR)
            .addParam(PARAM_EEXP, EMPTY_STR);
    }

    public static DoMessagesHandler doMessagesHandler() {
        return new DoMessagesHandler();
    }

    public DoMessagesHandler withAuth(RestAssuredAuthRule auth) {
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public DoMessagesHandler withAction(String action) {
        reqSpecBuilder.addParam(MESSAGES_PARAM_ACTION, action);
        return this;
    }

    public DoMessagesHandler withMessages(List<Message> messages) {
        for (Message message : messages) {
            reqSpecBuilder.addParam(MESSAGES_PARAM_IDS, message.getMid());
        }
        return this;
    }

    public DoMessagesHandler withIds(String ids) {
        reqSpecBuilder.addParam(MESSAGES_PARAM_IDS, ids);
        return this;
    }

    public DoMessagesHandler withFid(String fid) {
        reqSpecBuilder.addParam(FOLDERS_PARAM_FID, fid);
        return this;
    }

    public DoMessagesHandler withMoveFile(String movefileId) {
        reqSpecBuilder.addParam(MESSAGES_PARAM_MOVEFILE, movefileId);
        return this;
    }

    public DoMessagesHandler withTab(String tab) {
        reqSpecBuilder.addParam(MESSAGES_PARAM_TAB, tab);
        return this;
    }

    public Response callDoMessagesHandler() {
        Response resp = given().spec(reqSpecBuilder.build())
            .basePath(apiProps().modelsUrl())
            .post();
        return resp;
    }
}
