package ru.yandex.autotests.innerpochta.api.folders;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.util.handlers.FoldersConstants.FOLDERS_CLEAR_PARAM_FID;
import static ru.yandex.autotests.innerpochta.util.handlers.FoldersConstants.FOLDERS_CLEAR_PARAM_METHOD;
import static ru.yandex.autotests.innerpochta.util.handlers.FoldersConstants.FOLDERS_CLEAR_PARAM_OLD_F;
import static ru.yandex.autotests.innerpochta.util.handlers.FoldersConstants.HANDLER_DO_FOLDER_CLEAR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_EEXP;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_EXP;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;

/**
 * Created by mabelpines on 13.05.15.
 */
public class DoFolderClearHandler {
    private RequestSpecBuilder reqSpecBuilder;

    private DoFolderClearHandler(){
        reqSpecBuilder = new RequestSpecBuilder()
                .addParam(PARAM_MODEL_0, HANDLER_DO_FOLDER_CLEAR)
                .addParam(PARAM_EXP, EMPTY_STR)
                .addParam(PARAM_EEXP, EMPTY_STR);
    }

    public static DoFolderClearHandler doFolderClearHandler() { return new DoFolderClearHandler(); }

    public DoFolderClearHandler withAuth(RestAssuredAuthRule auth){
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public DoFolderClearHandler withCfid(String fid){
        reqSpecBuilder.addParam(FOLDERS_CLEAR_PARAM_FID, fid);
        return this;
    }

    public DoFolderClearHandler withMethod(String method){
        reqSpecBuilder.addParam(FOLDERS_CLEAR_PARAM_METHOD, method);
        return this;
    }

    public DoFolderClearHandler withOldF(String oldFParam){
        reqSpecBuilder.addParam(FOLDERS_CLEAR_PARAM_OLD_F, oldFParam);
        return this;
    }

    public Response callDoFolderClearHandler() {
        return given().spec(reqSpecBuilder.build())
                .basePath(apiProps().modelsUrl())
                .post();
    }
}
