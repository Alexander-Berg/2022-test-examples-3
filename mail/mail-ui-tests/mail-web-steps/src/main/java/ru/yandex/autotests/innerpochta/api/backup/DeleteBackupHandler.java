package ru.yandex.autotests.innerpochta.api.backup;

import io.restassured.builder.RequestSpecBuilder;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.util.handlers.BackupConstants.HANDLER_DO_BACKUP_DELETE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;
import static ru.yandex.autotests.innerpochta.api.RestAssuredLoggingFilter.log;


/**
 * @author eremin-n-s
 */
public class DeleteBackupHandler {
    private RequestSpecBuilder reqSpecBuilder;

    private DeleteBackupHandler() {
        reqSpecBuilder = new RequestSpecBuilder()
                .addParam(PARAM_MODEL_0, HANDLER_DO_BACKUP_DELETE);
    }

    public static DeleteBackupHandler doDeleteBackupHandler() {
        return new DeleteBackupHandler();
    }

    public DeleteBackupHandler withAuth(RestAssuredAuthRule auth) {
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public void callDeleteBackupHandler() {
        given().spec(reqSpecBuilder.build())
                .filter(log())
                .basePath(apiProps().modelsUrl())
                .post();
    }
}