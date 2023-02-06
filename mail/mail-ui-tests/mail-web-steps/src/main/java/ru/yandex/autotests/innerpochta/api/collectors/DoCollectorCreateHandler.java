package ru.yandex.autotests.innerpochta.api.collectors;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.util.handlers.CollectorsConstants.COLLECTORS_PARAM_COPY_FOLDERS;
import static ru.yandex.autotests.innerpochta.util.handlers.CollectorsConstants.COLLECTORS_PARAM_DEFAULT_PORT;
import static ru.yandex.autotests.innerpochta.util.handlers.CollectorsConstants.COLLECTORS_PARAM_EMAIL;
import static ru.yandex.autotests.innerpochta.util.handlers.CollectorsConstants.COLLECTORS_PARAM_LOGIN;
import static ru.yandex.autotests.innerpochta.util.handlers.CollectorsConstants.COLLECTORS_PARAM_NO_DELETE_MSG;
import static ru.yandex.autotests.innerpochta.util.handlers.CollectorsConstants.COLLECTORS_PARAM_PASSWORD;
import static ru.yandex.autotests.innerpochta.util.handlers.CollectorsConstants.COLLECTORS_PARAM_PORT;
import static ru.yandex.autotests.innerpochta.util.handlers.CollectorsConstants.COLLECTORS_PARAM_PROTOCOL;
import static ru.yandex.autotests.innerpochta.util.handlers.CollectorsConstants.COLLECTORS_PARAM_PROTOCOL_IMAP;
import static ru.yandex.autotests.innerpochta.util.handlers.CollectorsConstants.COLLECTORS_PARAM_SERVER;
import static ru.yandex.autotests.innerpochta.util.handlers.CollectorsConstants.COLLECTORS_PARAM_USE_SSL;
import static ru.yandex.autotests.innerpochta.util.handlers.CollectorsConstants.HANDLER_DO_COLLECTOR_CREATE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;

/**
 * Created by mabelpines on 14.05.15.
 */
public class DoCollectorCreateHandler {
    private RequestSpecBuilder reqSpecBuilder;
    private String server;

    private DoCollectorCreateHandler(){
        reqSpecBuilder = new RequestSpecBuilder()
                .addParam(PARAM_MODEL_0, HANDLER_DO_COLLECTOR_CREATE);
    }

    public static DoCollectorCreateHandler doCollectorCreateHandler() { return new DoCollectorCreateHandler(); }

    public DoCollectorCreateHandler withAuth(RestAssuredAuthRule auth){
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public DoCollectorCreateHandler withCopyFoldersParam(String status){
        reqSpecBuilder.addParam(COLLECTORS_PARAM_COPY_FOLDERS, status);
        return this;
    }

    public DoCollectorCreateHandler withEmailAsLogin(String email){
        reqSpecBuilder.addParam(COLLECTORS_PARAM_EMAIL, email)
                .addParam(COLLECTORS_PARAM_LOGIN, email);
        return this;
    }

    public DoCollectorCreateHandler withNoDeleteMsgParam(String status){
        reqSpecBuilder.addParam(COLLECTORS_PARAM_NO_DELETE_MSG, status);
        return this;
    }

    public DoCollectorCreateHandler withPassword(String password){
        reqSpecBuilder.addParam(COLLECTORS_PARAM_PASSWORD, password);
        return this;
    }

    public DoCollectorCreateHandler withDefaultPort(){
        reqSpecBuilder.addParam(COLLECTORS_PARAM_PORT, COLLECTORS_PARAM_DEFAULT_PORT);
        return this;
    }

    public DoCollectorCreateHandler withImapProtocol(){
        reqSpecBuilder.addParam(COLLECTORS_PARAM_PROTOCOL, COLLECTORS_PARAM_PROTOCOL_IMAP);
        return this;
    }

    public DoCollectorCreateHandler withServer(String server){
        reqSpecBuilder.addParam(COLLECTORS_PARAM_SERVER, server);
        return this;
    }

    public DoCollectorCreateHandler useSSL(String status){
        reqSpecBuilder.addParam(COLLECTORS_PARAM_USE_SSL, status);
        return this;
    }

    public Response callDoCollectorCreateHandler() {
        return given().spec(reqSpecBuilder.build())
                .basePath(apiProps().modelsUrl())
//                .expect().spec(shouldRecieveNoErrorsInHandlersSpec())
                .post();
    }
}
