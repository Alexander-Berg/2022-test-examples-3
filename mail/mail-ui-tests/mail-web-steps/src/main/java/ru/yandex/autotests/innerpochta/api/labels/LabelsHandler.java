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
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.HANDLER_LABELS;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;

/**
 * Created by mabelpines on 06.05.15.
 */
public class LabelsHandler {

    private RequestSpecBuilder reqSpecBuilder;

    private LabelsHandler(){
        reqSpecBuilder = new RequestSpecBuilder()
                .addParam(PARAM_MODEL_0, HANDLER_LABELS)
                .addParam(PARAM_EXP, EMPTY_STR)
                .addParam(PARAM_EEXP, EMPTY_STR);
    }

    public static LabelsHandler labelsHandler() {
        return new LabelsHandler();
    }

    public LabelsHandler withAuth(RestAssuredAuthRule auth){
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public Response callLabelsHandler() {
        Response resp = given().spec(reqSpecBuilder.build())
                .basePath(apiProps().modelsUrl())
                .expect().spec(shouldRecieveNoErrorsInHandlersSpec())
                .when()
                .post();
        return resp;
    }
}
