package ru.yandex.autotests.innerpochta.api.labels;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.api.AccountInformationHandler.shouldRecieveNoErrorsInHandlersSpec;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_EEXP;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_EXP;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.HANDLER_DO_LABEL;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_IDS;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_LID;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;

/**
 * Created by mabelpines on 10.11.15.
 */
public class DoLabelHandler {
    private RequestSpecBuilder reqSpecBuilder;

    private DoLabelHandler(){
        reqSpecBuilder = new RequestSpecBuilder()
                .addParam(PARAM_MODEL_0, HANDLER_DO_LABEL)
                .addParam(PARAM_EXP, EMPTY_STR)
                .addParam(PARAM_EEXP, EMPTY_STR);
    }

    public static DoLabelHandler doLabelHandler() { return new DoLabelHandler(); }

    public DoLabelHandler withAuth(RestAssuredAuthRule auth){
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public DoLabelHandler withIds (String ids){
        reqSpecBuilder.addParam(LABELS_PARAM_IDS, ids);
        return this;
    }

    public DoLabelHandler withLid (String lid){
        reqSpecBuilder.addParam(LABELS_PARAM_LID, lid);
        return this;
    }

    public Response callDoLabelHandler() {
        Response resp = given().spec(reqSpecBuilder.build())
                .basePath(apiProps().modelsUrl())
                .expect().spec(shouldRecieveNoErrorsInHandlersSpec())
                .when()
                .post();
        return resp;
    }
}
