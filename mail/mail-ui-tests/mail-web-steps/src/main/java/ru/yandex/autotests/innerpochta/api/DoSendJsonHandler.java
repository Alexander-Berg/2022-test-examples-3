package ru.yandex.autotests.innerpochta.api;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.steps.beans.account.AccountInformation;

import java.net.URI;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.api.RestAssuredLoggingFilter.log;
import static ru.yandex.autotests.innerpochta.api.AccountInformationHandler.shouldHaveOkStatusInRespSpec;
import static ru.yandex.autotests.innerpochta.util.handlers.AccountInfoConstants.ACCINFO_PARAM_CKEY;
import static ru.yandex.autotests.innerpochta.util.handlers.AccountInfoConstants.ACCINFO_PARAM_COMPOSE_CHECK;
import static ru.yandex.autotests.innerpochta.util.handlers.AccountInfoConstants.ACCINFO_PARAM_UID;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_EEXP;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_EXP;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_YES;
import static ru.yandex.autotests.innerpochta.util.handlers.SendJsonConstants.MESSAGES_PARAM_INREPLYTO;
import static ru.yandex.autotests.innerpochta.util.handlers.SendJsonConstants.MESSAGES_PARAM_NOSEND;
import static ru.yandex.autotests.innerpochta.util.handlers.SendJsonConstants.MESSAGES_PARAM_REFERENCES;
import static ru.yandex.autotests.innerpochta.util.handlers.SendJsonConstants.MESSAGES_PARAM_SAVE_SYMBOL;
import static ru.yandex.autotests.innerpochta.util.handlers.SendJsonConstants.MESSAGES_PARAM_SEND;
import static ru.yandex.autotests.innerpochta.util.handlers.SendJsonConstants.MESSAGES_PARAM_TEMPLATES_FID;
import static ru.yandex.autotests.innerpochta.util.handlers.SendJsonConstants.SEND_JSON_HTML_TEXT_TYPE;
import static ru.yandex.autotests.innerpochta.util.handlers.SendJsonConstants.SEND_JSON_PARAM_MESSAGE_TYPE;
import static ru.yandex.autotests.innerpochta.util.handlers.SendJsonConstants.SEND_JSON_PARAM_SEND;
import static ru.yandex.autotests.innerpochta.util.handlers.SendJsonConstants.SEND_JSON_PARAM_SEND_UNDERSCORE;
import static ru.yandex.autotests.innerpochta.util.handlers.SendJsonConstants.SEND_JSON_PARAM_SUBJECT;
import static ru.yandex.autotests.innerpochta.util.handlers.SendJsonConstants.SEND_JSON_PLAIN_TEXT_TYPE;
import static ru.yandex.autotests.innerpochta.util.handlers.SendJsonConstants.MESSAGE_PARAM_ATTACH_IDS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.PARAM_TO;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;

/**
 * Created by mabelpines on 23.04.15.
 */
public class DoSendJsonHandler {

    private RequestSpecBuilder reqSpecBuilder;

    private DoSendJsonHandler() {
        reqSpecBuilder = new RequestSpecBuilder()
            .addParam(SEND_JSON_PARAM_SEND_UNDERSCORE, STATUS_TRUE)
            .addParam(PARAM_EXP, EMPTY_STR)
            .addParam(PARAM_EEXP, EMPTY_STR);
    }

    private DoSendJsonHandler(String handlerParam, String status) {
        reqSpecBuilder = new RequestSpecBuilder()
            .addParam(handlerParam, status);
    }

    public static DoSendJsonHandler doSendJsonHandler() {
        return new DoSendJsonHandler();
    }

    public static DoSendJsonHandler doSendJsonHandler(String handlerParam, String status) {
        return new DoSendJsonHandler(handlerParam, status);
    }

    public DoSendJsonHandler withAuth(RestAssuredAuthRule auth) {
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public DoSendJsonHandler withCookies(Cookies cookies) {
        reqSpecBuilder.addCookies(cookies);
        return this;
    }

    public DoSendJsonHandler withCookie(Cookie cookie) {
        reqSpecBuilder.addCookie(cookie);
        return this;
    }

    public DoSendJsonHandler withAccInfo(AccountInformation accInfo) {
        reqSpecBuilder.addParam(ACCINFO_PARAM_CKEY, accInfo.getCkey())
            .addParam(ACCINFO_PARAM_UID, accInfo.getUid())
            .addParam(ACCINFO_PARAM_COMPOSE_CHECK, accInfo.getComposeCheck());
        return this;
    }

    public DoSendJsonHandler withBaseUri(URI uri) {
        return withBaseUri(uri.toString());
    }

    public DoSendJsonHandler withBaseUri(String uri) {
        reqSpecBuilder.setBaseUri(uri).setRelaxedHTTPSValidation();
        return this;
    }

    public DoSendJsonHandler withMessageBody(String messageBody) {
        reqSpecBuilder.addParam(SEND_JSON_PARAM_SEND, messageBody);
        return this;
    }

    public DoSendJsonHandler withSubject(String subject) {
        reqSpecBuilder.addParam(SEND_JSON_PARAM_SUBJECT, subject);
        return this;
    }

    public DoSendJsonHandler withPlainTextType() {
        reqSpecBuilder.addParam(SEND_JSON_PARAM_MESSAGE_TYPE, SEND_JSON_PLAIN_TEXT_TYPE);
        return this;
    }

    public DoSendJsonHandler withHtmlTextType() {
        reqSpecBuilder.addParam(SEND_JSON_PARAM_MESSAGE_TYPE, SEND_JSON_HTML_TEXT_TYPE);
        return this;
    }

    public DoSendJsonHandler withReceiver(String receiverMail) {
        reqSpecBuilder.addParam(PARAM_TO, receiverMail);
        return this;
    }

    public DoSendJsonHandler withParam(String paramName, String paramVal) {
        reqSpecBuilder.addParam(paramName, paramVal);
        return this;
    }

    public DoSendJsonHandler withCc(String ccEmails) {
        reqSpecBuilder.addParam("cc", ccEmails);
        return this;
    }

    public DoSendJsonHandler withBcc(String bccEmails) {
        reqSpecBuilder.addParam("bcc", bccEmails);
        return this;
    }

    public DoSendJsonHandler withNoSend() {
        reqSpecBuilder.addParam(MESSAGES_PARAM_NOSEND, STATUS_YES);
        return this;
    }

    public DoSendJsonHandler withSaveSymbol(String saveSymbol) {
        reqSpecBuilder.addParam(MESSAGES_PARAM_SAVE_SYMBOL, saveSymbol);
        return this;
    }

    public DoSendJsonHandler withSend(String text) {
        reqSpecBuilder.addParam(MESSAGES_PARAM_SEND, text);
        return this;
    }


    public DoSendJsonHandler withParams(Map<String, String> params) {
        reqSpecBuilder.addParam("params", params);
        return this;
    }

    public DoSendJsonHandler withTemplatesFid(String fid) {
        reqSpecBuilder.addParam(MESSAGES_PARAM_TEMPLATES_FID, fid);
        return this;
    }

    public DoSendJsonHandler withThreadId(int id) {
        String reference = String.format("<%s@sas1-1e9408e1968c.qloud-c.yandex.net>", id);
        reqSpecBuilder
            .addParam(MESSAGES_PARAM_REFERENCES, reference)
            .addParam(MESSAGES_PARAM_INREPLYTO, reference);
        return this;
    }

    public DoSendJsonHandler withThreadToMessage(String id) {
        reqSpecBuilder
            .addParam(MESSAGES_PARAM_REFERENCES, id)
            .addParam(MESSAGES_PARAM_INREPLYTO, id);
        return this;
    }

    public DoSendJsonHandler withAttachId(String id) {
        reqSpecBuilder
            .addParam(MESSAGE_PARAM_ATTACH_IDS, id);
        return this;
    }

    public Response callDoSendJson() {
        return given().spec(reqSpecBuilder.build())
            .filter(log())
            .basePath(apiProps().doSendJsonHandlerUrl())
            .expect().spec(shouldHaveOkStatusInRespSpec())
            .when()
            .post();
    }
}
