package ru.yandex.autotests.innerpochta.api.folders;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.util.handlers.FoldersConstants.FOLDERS_CLEAR_PARAM_FID;
import static ru.yandex.autotests.innerpochta.util.handlers.FoldersConstants.FOLDERS_CLEAR_PARAM_METHOD;
import static ru.yandex.autotests.innerpochta.util.handlers.FoldersConstants.FOLDERS_CLEAR_PARAM_OLD_F;
import static ru.yandex.autotests.innerpochta.util.handlers.FoldersConstants.FOLDERS_PARAM_FID;
import static ru.yandex.autotests.innerpochta.util.handlers.FoldersConstants.FOLDERS_PARAM_PARENT_ID;
import static ru.yandex.autotests.innerpochta.util.handlers.FoldersConstants.HANDLER_DO_FOLDER_CLEAR;
import static ru.yandex.autotests.innerpochta.util.handlers.FoldersConstants.HANDLER_DO_FOLDER_MOVE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_EEXP;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_EXP;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;

/**
 * @author vasily-k
 */
public class DoFolderMoveHandler {
    private RequestSpecBuilder reqSpecBuilder;

    private DoFolderMoveHandler() {
        reqSpecBuilder = new RequestSpecBuilder()
            .addParam(PARAM_MODEL_0, HANDLER_DO_FOLDER_MOVE)
            .addParam(PARAM_EXP, EMPTY_STR)
            .addParam(PARAM_EEXP, EMPTY_STR);
    }

    public static DoFolderMoveHandler doFolderMoveHandler() { return new DoFolderMoveHandler(); }

    public DoFolderMoveHandler withAuth(RestAssuredAuthRule auth){
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public DoFolderMoveHandler withFid(String fid){
        reqSpecBuilder.addParam(FOLDERS_PARAM_FID, fid);
        return this;
    }

    public DoFolderMoveHandler withParentId(String parentId){
        reqSpecBuilder.addParam(FOLDERS_PARAM_PARENT_ID, parentId);
        return this;
    }

    public Response callDoFolderMoveHandler() {
        return given().spec(reqSpecBuilder.build())
            .basePath(apiProps().modelsUrl())
            .post();
    }
}
