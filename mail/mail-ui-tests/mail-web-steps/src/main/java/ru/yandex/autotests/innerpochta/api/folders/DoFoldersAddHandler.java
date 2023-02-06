package ru.yandex.autotests.innerpochta.api.folders;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.api.AccountInformationHandler.shouldRecieveNoErrorsInHandlersSpec;
import static ru.yandex.autotests.innerpochta.util.handlers.FoldersConstants.FOLDERS_ADD_PARAM_FOLDER_NAME;
import static ru.yandex.autotests.innerpochta.util.handlers.FoldersConstants.HANDLER_DO_FOLDERS_ADD;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_EEXP;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_EXP;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;

/**
 * Created by mabelpines on 05.05.15.
 */
public class DoFoldersAddHandler {

    private RequestSpecBuilder reqSpecBuilder;

    private DoFoldersAddHandler(){
        reqSpecBuilder = new RequestSpecBuilder()
                .addParam(PARAM_MODEL_0, HANDLER_DO_FOLDERS_ADD)
                .addParam(PARAM_EXP, EMPTY_STR)
                .addParam(PARAM_EEXP, EMPTY_STR);
    }

    public static DoFoldersAddHandler doFoldersAddHandler() { return new DoFoldersAddHandler(); }

    public DoFoldersAddHandler withAuth(RestAssuredAuthRule auth){
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public DoFoldersAddHandler withFolderName(String folderName){
        reqSpecBuilder.addParam(FOLDERS_ADD_PARAM_FOLDER_NAME, folderName);
        return this;
    }

    public DoFoldersAddHandler withParam(String paramName, String paramVal){
        reqSpecBuilder.addParam(paramName, paramVal);
        return this;
    }

    public Response callDoFoldersAddHandler() {
       return given().spec(reqSpecBuilder.build())
            .basePath(apiProps().modelsUrl())
            .expect().spec(shouldRecieveNoErrorsInHandlersSpec())
            .when()
            .post();
    }
}
