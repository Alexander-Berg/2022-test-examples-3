package ru.yandex.autotests.innerpochta.api;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.response.Response;
import io.restassured.specification.RequestSender;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.steps.beans.account.AccountInformation;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.innerpochta.api.RestAssuredLoggingFilter.log;
import static ru.yandex.autotests.innerpochta.steps.api.ApiDefaultSteps.getJsonPathConfig;
import static ru.yandex.autotests.innerpochta.util.handlers.AccountInfoConstants.HANDLER_ACCOUNT_INFO;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_EEXP;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_EXP;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.PARAM_MODEL_0;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;
import static ru.yandex.autotests.innerpochta.util.props.UrlProps.urlProps;
import static ru.yandex.autotests.passport.api.core.tools.log.AllureRestAssuredLoggerFilter.allureLoggerFilter;

/**
 * Created by mabelpines on 16.04.15.
 */
public class AccountInformationHandler {

    private RequestSpecBuilder reqSpecBuilder;

    private AccountInformationHandler() {
        reqSpecBuilder = new RequestSpecBuilder()
            .setBaseUri(urlProps().getBaseUri().toString()).setRelaxedHTTPSValidation()
            .addParam(PARAM_MODEL_0, HANDLER_ACCOUNT_INFO)
            .addParam(PARAM_EXP, EMPTY_STR)
            .addParam(PARAM_EEXP, EMPTY_STR);
    }

    private AccountInformationHandler(String baseUri) {
        reqSpecBuilder = new RequestSpecBuilder()
            .setBaseUri(baseUri).setRelaxedHTTPSValidation()
            .addParam(PARAM_MODEL_0, HANDLER_ACCOUNT_INFO)
            .addParam(PARAM_EXP, EMPTY_STR)
            .addParam(PARAM_EEXP, EMPTY_STR);

    }

    public static AccountInformationHandler accountInformationHandler() {
        return new AccountInformationHandler();
    }

    public static AccountInformationHandler accountInformationHandler(String baseUri) {
        return new AccountInformationHandler(baseUri);
    }

    public AccountInformationHandler withAuth(RestAssuredAuthRule auth) {
        reqSpecBuilder.addFilter(auth);
        return this;
    }

    public AccountInformationHandler withCookies(Cookies cookies) {
        reqSpecBuilder.addCookies(cookies);
        return this;
    }

    public AccountInformationHandler withCookie(Cookie cookie) {
        reqSpecBuilder.addCookie(cookie);
        return this;
    }

    public Response callAccountInformation() {
        RequestSpecification spec = given().spec(reqSpecBuilder.build())
            .filter(log())
            .filter( allureLoggerFilter())
            .basePath(apiProps().modelsUrl())
            .when();
        return retryRequestIfNot200(spec);
    }

    public Response callAccountInformation(String modelsUrl) {
        return given().spec(reqSpecBuilder.build())
            .filter(log())
            .basePath(modelsUrl)
            .expect().spec(shouldRecieveNoErrorsInHandlersSpec())
            .when()
            .post();
    }


    public static AccountInformation getAccInfo(Response resp) {
        return resp.then().extract()
            .jsonPath(getJsonPathConfig()).getObject("models[0].data", AccountInformation.class);
    }

    public static ResponseSpecification shouldRecieveNoErrorsInHandlersSpec() {
        return new ResponseSpecBuilder().expectBody("models.status.toString()", containsString("ok")).build();
    }

    public static ResponseSpecification shouldHaveOkStatusInRespSpec() {
        return new ResponseSpecBuilder().expectBody("status.toString()", containsString("ok")).build();
    }

    public static Response retryRequestIfNot200(RequestSender spec) {
        int retries = 0;
        Response response = spec.post();
        while (!response.body().path("models.status").toString().contains("ok") && retries < 2) {
            System.out.println(response.body().path("models.status").toString());
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            retries++;
            response = spec.post();
        }
        assertTrue(
            "Запрос завершился ошибкой!",
            response.body().path("models.status").toString().contains("ok")
        );
        return response;
    }
}

