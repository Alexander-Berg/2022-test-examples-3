package ru.yandex.autotests.innerpochta.api.folders;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.util.handlers.FoldersConstants.FOLDERS_PARAM_FID;
import static ru.yandex.autotests.innerpochta.util.handlers.FoldersConstants.FOLDERS_SYMBOL;
import static ru.yandex.autotests.innerpochta.util.handlers.FoldersConstants.HANDLER_DO_SET_SYMBOL;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_EEXP;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_EXP;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;

/**
 * @author oleshko
 */
public class DoFolderSetSymbolHandler {

    private RequestSpecBuilder reqSpecBuilder;

    private DoFolderSetSymbolHandler() {
        reqSpecBuilder = new RequestSpecBuilder()
            .addParam(PARAM_MODEL_0, HANDLER_DO_SET_SYMBOL)
            .addParam(PARAM_EXP, EMPTY_STR)
            .addParam(PARAM_EEXP, EMPTY_STR);
    }

    public static DoFolderSetSymbolHandler doFolderSetSymbolHandler() {
        return new DoFolderSetSymbolHandler();
    }

    public DoFolderSetSymbolHandler withAuth(RestAssuredAuthRule auth) {
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public DoFolderSetSymbolHandler withFid(String fid) {
        reqSpecBuilder.addParam(FOLDERS_PARAM_FID, fid);
        return this;
    }

    public DoFolderSetSymbolHandler withSymbol(String fid) {
        reqSpecBuilder.addParam(FOLDERS_SYMBOL, fid);
        return this;
    }

    public Response callFolderSetSymbolHandler() {
        Response resp = given().spec(reqSpecBuilder.build())
            .basePath(apiProps().modelsUrl())
            .post();
        return resp;
    }
}
