package ru.yandex.autotests.innerpochta.api.backup;

import io.restassured.builder.RequestSpecBuilder;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.handlers.BackupConstants.HANDLER_DO_BACKUP_CREATE;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;
import static ru.yandex.autotests.innerpochta.api.RestAssuredLoggingFilter.log;


/**
 * @author eremin-n-s
 */
public class CreateBackupHandler {
    private RequestSpecBuilder reqSpecBuilder;

    private CreateBackupHandler() {
        reqSpecBuilder = new RequestSpecBuilder()
                .addParam(PARAM_MODEL_0, HANDLER_DO_BACKUP_CREATE);
    }

    public static CreateBackupHandler doCreateBackupHandler() {
        return new CreateBackupHandler();
    }

    public CreateBackupHandler withAuth(RestAssuredAuthRule auth) {
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public void callCreateBackupHandler() {
        given().spec(reqSpecBuilder.build())
                .filter(log())
                .basePath(apiProps().modelsUrl())
                .post();
    }
}