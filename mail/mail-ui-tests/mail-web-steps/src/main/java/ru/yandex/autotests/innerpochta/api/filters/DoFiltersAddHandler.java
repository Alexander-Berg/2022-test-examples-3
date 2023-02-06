package ru.yandex.autotests.innerpochta.api.filters;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.util.Utils;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.api.AccountInformationHandler.shouldRecieveNoErrorsInHandlersSpec;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_CLICKER;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_DEFAULT_NAME;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_FIELD1;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_FIELD2;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_FIELD3;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_LETTER;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_LOGIC;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_NAME;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.HANDLER_DO_FILTERS_ADD;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;


/**
 * Created by mabelpines on 12.05.15.
 */
public class DoFiltersAddHandler {
    private RequestSpecBuilder reqSpecBuilder;

    private DoFiltersAddHandler(){
        reqSpecBuilder = new RequestSpecBuilder()
                .addParam(PARAM_MODEL_0, HANDLER_DO_FILTERS_ADD);
    }

    public static DoFiltersAddHandler doFiltersAddHandler() { return new DoFiltersAddHandler(); }

    public DoFiltersAddHandler withAuth(RestAssuredAuthRule auth){
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public DoFiltersAddHandler withAttachment(String containsAttachment){
        reqSpecBuilder.addParam(FILTERS_ADD_PARAM_ATTACHMENT, containsAttachment);
        return this;
    }

    public DoFiltersAddHandler withClicker(String clicker){
        reqSpecBuilder.addParam(FILTERS_ADD_PARAM_CLICKER, clicker);
        return this;
    }

    public DoFiltersAddHandler withField1Params(String param1, String param2){
        reqSpecBuilder.addParam(FILTERS_ADD_PARAM_FIELD1, param1)
                .addParam(FILTERS_ADD_PARAM_FIELD1, param2);
        return this;
    }

    public DoFiltersAddHandler withField2Params(String param1, String param2){
        reqSpecBuilder.addParam(FILTERS_ADD_PARAM_FIELD2, param1)
                .addParam(FILTERS_ADD_PARAM_FIELD2, param2);
        return this;
    }

    public DoFiltersAddHandler withField3Params(String param1, String param2){
        reqSpecBuilder.addParam(FILTERS_ADD_PARAM_FIELD3, param1)
                .addParam(FILTERS_ADD_PARAM_FIELD3, param2);
        return this;
    }

    public DoFiltersAddHandler withLetter(String letterParam){
        reqSpecBuilder.addParam(FILTERS_ADD_PARAM_LETTER, letterParam);
        return this;
    }

    public DoFiltersAddHandler withLogicParam(String logicParam){
        reqSpecBuilder.addParam(FILTERS_ADD_PARAM_LOGIC, logicParam);
        return this;
    }

    public DoFiltersAddHandler withMove(String moveType, String elementId){
        reqSpecBuilder.addParam(moveType, elementId);
        return this;
    }

    public DoFiltersAddHandler withRandomName(){
        reqSpecBuilder.addParam(FILTERS_ADD_PARAM_NAME, Utils.getRandomName());
        return this;
    }

    public DoFiltersAddHandler withDefaultName(){
        reqSpecBuilder.addParam(FILTERS_ADD_PARAM_NAME, FILTERS_ADD_PARAM_DEFAULT_NAME);
        return this;
    }

    public DoFiltersAddHandler withStop() {
        reqSpecBuilder.addParam("stop.0", "");
        return this;
    }

    public Response callDoFiltersAddHandler() {
        return  given().spec(reqSpecBuilder.build())
            .basePath(apiProps().modelsUrl())
            .expect().spec(shouldRecieveNoErrorsInHandlersSpec())
            .when()
            .post();
    }
}
