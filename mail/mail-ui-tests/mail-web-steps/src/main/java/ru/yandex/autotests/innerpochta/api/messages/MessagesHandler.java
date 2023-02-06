package ru.yandex.autotests.innerpochta.api.messages;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.api.RestAssuredLoggingFilter.log;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_EEXP;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_EXP;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.HANDLER_MESSAGES;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.MESSAGES_PARAM_CURRENT_FOLDER;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.MESSAGES_PARAM_CURRENT_LABEL;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.MESSAGES_PARAM_DATE;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.MESSAGES_PARAM_EXTRA_COND;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.MESSAGES_PARAM_GOTO;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.MESSAGES_PARAM_MESSAGES_PER_PAGE;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.MESSAGES_PARAM_SORT_TYPE;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.MESSAGES_PARAM_UNREAD;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.MESSAGES_THREAD_ID;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.MESSAGES_VAL_ALL;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.MESSAGES_VAL_ONLY_NEW;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.MESSAGES_VAL_UNREAD;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;

/**
 * @author mabelpines
 */
public class MessagesHandler {
    private RequestSpecBuilder reqSpecBuilder;

    private MessagesHandler() {
        reqSpecBuilder = new RequestSpecBuilder()
            .addParam(PARAM_MODEL_0, HANDLER_MESSAGES)
            .addParam(PARAM_EXP, EMPTY_STR)
            .addParam(PARAM_EEXP, EMPTY_STR);
    }

    public static MessagesHandler messagesHandler() {
        return new MessagesHandler();
    }

    public MessagesHandler withAuth(RestAssuredAuthRule auth) {
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public MessagesHandler withCurrentFolder(String fid) {
        reqSpecBuilder.addParam(MESSAGES_PARAM_CURRENT_FOLDER, fid)
            .addParam(MESSAGES_PARAM_SORT_TYPE, MESSAGES_PARAM_DATE);
        return this;
    }

    public MessagesHandler withCurrentLid(String lid) {
        reqSpecBuilder.addParam(MESSAGES_PARAM_CURRENT_LABEL, lid)
            .addParam(MESSAGES_PARAM_SORT_TYPE, MESSAGES_PARAM_DATE);
        return this;
    }

    public MessagesHandler withOnlyUnread() {
        reqSpecBuilder.addParam(MESSAGES_PARAM_EXTRA_COND, MESSAGES_VAL_ONLY_NEW)
            .addParam(MESSAGES_PARAM_GOTO, MESSAGES_VAL_ALL)
            .addParam(MESSAGES_PARAM_SORT_TYPE, MESSAGES_PARAM_DATE)
            .addParam(MESSAGES_PARAM_UNREAD, MESSAGES_VAL_UNREAD);
        return this;
    }

    public MessagesHandler withThreadId(String tid) {
        reqSpecBuilder.addParam(MESSAGES_THREAD_ID, tid);
        return this;
    }

    public Response callMessagesHandler() {
        return given().spec(reqSpecBuilder.build())
            .filter(log())
            .basePath(apiProps().modelsUrl())
            .post();
    }

    public MessagesHandler withMessagesPerPage(long messagesPerPage) {
        reqSpecBuilder.addParam(MESSAGES_PARAM_MESSAGES_PER_PAGE, String.valueOf(messagesPerPage));
        return this;
    }
}
