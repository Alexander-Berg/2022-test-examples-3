package ru.yandex.autotests.innerpochta.api.replyLater;

import io.restassured.builder.RequestSpecBuilder;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.api.RestAssuredLoggingFilter.log;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.handlers.ReplyLaterConstants.HANDLER_DO_REPLY_LATER_CREATE;
import static ru.yandex.autotests.innerpochta.util.handlers.ReplyLaterConstants.REPLY_LATER_PARAM_DATE;
import static ru.yandex.autotests.innerpochta.util.handlers.ReplyLaterConstants.REPLY_LATER_PARAM_MID;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;


/**
 * @author oleshko
 */
public class DoReplyLaterCreateHandler {
    private RequestSpecBuilder reqSpecBuilder;

    private DoReplyLaterCreateHandler() {
        reqSpecBuilder = new RequestSpecBuilder()
            .addParam(PARAM_MODEL_0, HANDLER_DO_REPLY_LATER_CREATE);
    }

    public static DoReplyLaterCreateHandler doReplyLaterCreateHandler() {
        return new DoReplyLaterCreateHandler();
    }

    public DoReplyLaterCreateHandler withAuth(RestAssuredAuthRule auth) {
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public DoReplyLaterCreateHandler withMid(String mid) {
        reqSpecBuilder.addParam(REPLY_LATER_PARAM_MID, mid);
        return this;
    }

    public DoReplyLaterCreateHandler withDate(String date) {
        reqSpecBuilder.addParam(REPLY_LATER_PARAM_DATE, date);
        return this;
    }

    public void callDoReplyLaterCreateHandler() {
        given().spec(reqSpecBuilder.build())
            .filter(log())
            .basePath(apiProps().modelsUrl())
            .post();
    }
}
