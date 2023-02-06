package ru.yandex.autotests.innerpochta.api.backup;

import io.restassured.builder.RequestSpecBuilder;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.handlers.BackupConstants.HANDLER_DO_FOLDER_PURGE_HIDDEN_TRASH;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;
import static ru.yandex.autotests.innerpochta.api.RestAssuredLoggingFilter.log;


/**
 * @author eremin-n-s
 */
public class PurgeHiddenTrashHandler {
    private RequestSpecBuilder reqSpecBuilder;

    private PurgeHiddenTrashHandler() {
        reqSpecBuilder = new RequestSpecBuilder()
                .addParam(PARAM_MODEL_0, HANDLER_DO_FOLDER_PURGE_HIDDEN_TRASH);
    }

    public static PurgeHiddenTrashHandler doPurgeHiddenTrashHandler() {
        return new PurgeHiddenTrashHandler();
    }

    public PurgeHiddenTrashHandler withAuth(RestAssuredAuthRule auth) {
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public void callPurgeHiddenTrashHandler() {
        given().spec(reqSpecBuilder.build())
                .filter(log())
                .basePath(apiProps().modelsUrl())
                .post();
    }
}