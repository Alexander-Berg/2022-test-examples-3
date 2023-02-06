package ru.yandex.autotests.innerpochta.api.folders;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.util.handlers.FoldersConstants.FOLDERS_PARAM_FID;
import static ru.yandex.autotests.innerpochta.util.handlers.FoldersConstants.HANDLER_DO_FOLDER_REMOVE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_EEXP;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_EXP;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;

/**
 * Created by mabelpines on 05.05.15.
 */
public class DoFolderRemoveHandler {

    private RequestSpecBuilder reqSpecBuilder;

    private DoFolderRemoveHandler(){
        reqSpecBuilder = new RequestSpecBuilder()
                .addParam(PARAM_MODEL_0, HANDLER_DO_FOLDER_REMOVE)
                .addParam(PARAM_EXP, EMPTY_STR)
                .addParam(PARAM_EEXP, EMPTY_STR);
    }

    public static DoFolderRemoveHandler doFolderRemoveHandler() {
        return new DoFolderRemoveHandler();
    }

    public DoFolderRemoveHandler withAuth(RestAssuredAuthRule auth){
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public DoFolderRemoveHandler withFid(String fid){
        reqSpecBuilder.addParam(FOLDERS_PARAM_FID, fid);
        return this;
    }

    public Response callDoFolderRemoveHandler() {
        Response resp = given().spec(reqSpecBuilder.build())
                .basePath(apiProps().modelsUrl())
                .post();
        return resp;
    }
}
