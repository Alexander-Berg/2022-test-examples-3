package ru.yandex.autotests.innerpochta.api.labels;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_EEXP;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_EXP;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.HANDLER_DO_LABELS_DELETE;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_LIDS;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;


/**
 * Created by mabelpines on 06.05.15.
 */
public class DoLabelsDeleteHandler {

    private RequestSpecBuilder reqSpecBuilder;

    private DoLabelsDeleteHandler(){
        reqSpecBuilder = new RequestSpecBuilder()
                .addParam(PARAM_MODEL_0, HANDLER_DO_LABELS_DELETE)
                .addParam(PARAM_EXP, EMPTY_STR)
                .addParam(PARAM_EEXP, EMPTY_STR);
    }

    public static DoLabelsDeleteHandler doLabelsDeleteHandler() {
        return new DoLabelsDeleteHandler();
    }

    public DoLabelsDeleteHandler withAuth(RestAssuredAuthRule auth){
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public DoLabelsDeleteHandler withLids(String lids){
        reqSpecBuilder.addParam(LABELS_PARAM_LIDS, lids);
        return this;
    }

    public Response calldoLabelsDeleteHandler() {
        Response resp = given().spec(reqSpecBuilder.build())
                .basePath(apiProps().modelsUrl())
                .post();
        return resp;
    }
}
