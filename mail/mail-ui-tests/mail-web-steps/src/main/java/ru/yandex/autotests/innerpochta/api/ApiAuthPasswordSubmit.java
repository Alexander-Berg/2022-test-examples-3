package ru.yandex.autotests.innerpochta.api;

import com.google.common.base.Joiner;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.Cookies;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import ru.yandex.autotests.passport.api.common.data.YandexDomain;
import ru.yandex.autotests.passport.api.core.cookie.SessionIdCookie;
import ru.yandex.autotests.passport.api.core.objects.UserWithProps;
import ru.yandex.autotests.passport.api.core.steps.CookieSteps;
import ru.yandex.autotests.passport.api.core.utilitydata.PassportUris;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.passport.api.common.Properties.props;
import static ru.yandex.autotests.passport.api.common.Utils.getRandomDigits;
import static ru.yandex.autotests.passport.api.common.Utils.getRandomString;
import static ru.yandex.autotests.passport.api.common.Utils.getRandomUserIpForRandomCountry;
import static ru.yandex.autotests.passport.api.core.api.CommonApiSettings.executeRequestWithFluentReqSpecAndRespSpec;
import static ru.yandex.autotests.passport.api.core.api.CommonApiSettings.specBuilder;
import static ru.yandex.autotests.passport.api.core.api.JSONResponses.respSpecJS;
import static ru.yandex.autotests.passport.api.core.api.JSONResponses.shouldBe200Ok;
import static ru.yandex.autotests.passport.api.core.api.track.ApiTrack.getTrackIdFromApiResponse;
import static ru.yandex.autotests.passport.api.core.cookie.YandexCookies.parseCookies;
import static ru.yandex.autotests.passport.api.core.cookie.YandexCookies.sessionIdFrom;
import static ru.yandex.autotests.passport.api.core.objects.UserWithProps.userWithLoginAndPassword;
import static ru.yandex.autotests.passport.api.core.steps.CaptchaSteps.passCaptchaForTrackId;
import static ru.yandex.autotests.innerpochta.tvm.TvmTicketsProvider.ticketsProvider;
import static ru.yandex.autotests.passport.api.core.utilitydata.TermsAndConstants.EMPTY_COOKIES;
import static ru.yandex.autotests.passport.api.core.utilitydata.TermsAndConstants.HEADER_X_YA_SERVICE_TICKET;
import static ru.yandex.autotests.passport.api.core.utilitydata.TermsAndConstants.HEADER_YA_CLIENT_COOKIE;
import static ru.yandex.autotests.passport.api.core.utilitydata.TermsAndConstants.HEADER_YA_CLIENT_HOST;
import static ru.yandex.autotests.passport.api.core.utilitydata.TermsAndConstants.HEADER_YA_CLIENT_USER_AGENT;
import static ru.yandex.autotests.passport.api.core.utilitydata.TermsAndConstants.HEADER_YA_CONSUMER_CLIENT_IP;
import static ru.yandex.autotests.passport.api.core.utilitydata.TermsAndConstants.HEADER_YA_CONSUMER_CLIENT_SCHEME;
import static ru.yandex.autotests.passport.api.core.utilitydata.TermsAndConstants.MAX_NUMBER_OF_USERS_IN_SESSION_ID;
import static ru.yandex.autotests.passport.api.core.utilitydata.TermsAndConstants.RequestType.POST;
import static ru.yandex.autotests.passport.api.core.utilitydata.TermsAndConstants.STR_CONSUMER;
import static ru.yandex.autotests.passport.api.core.utilitydata.TermsAndConstants.STR_ERROR;
import static ru.yandex.autotests.passport.api.core.utilitydata.TermsAndConstants.STR_ERRORS;
import static ru.yandex.autotests.passport.api.core.utilitydata.TermsAndConstants.STR_LOGIN;
import static ru.yandex.autotests.passport.api.core.utilitydata.TermsAndConstants.STR_PASSWORD;
import static ru.yandex.autotests.passport.api.core.utilitydata.TermsAndConstants.STR_RETPATH;
import static ru.yandex.autotests.passport.api.core.utilitydata.TermsAndConstants.STR_STATUS;
import static ru.yandex.autotests.passport.api.core.utilitydata.TermsAndConstants.STR_TRACK_ID;
import static ru.yandex.autotests.passport.api.core.utilitydata.TermsAndConstants.USER_HOST;
import static ru.yandex.autotests.passport.api.core.utilitydata.TermsAndConstants.USER_USER_AGENT;
import static ru.yandex.autotests.passport.api.core.utilitydata.TermsAndConstants.USER_YA_IP;
import static ru.yandex.autotests.passport.api.core.utilitydata.TermsAndConstants.YANDEX_SESSION_ID_NAME;
import static ru.yandex.autotests.passport.api.core.utilitydata.Users.getBallastOrdinaryUser;

/**
 * Usage:
 * User: angryBird
 * Date: 05.05.14 Time: 23:59
 */
public class ApiAuthPasswordSubmit {

    private RequestSpecBuilder reqSpecB;
    private Map<String, String> headersForReqSpecB = new HashMap<>();

    public ApiAuthPasswordSubmit() {
        this.reqSpecB = specBuilder();
    }

    public ApiAuthPasswordSubmit withDefaults() {
        headersForReqSpecB.put(HEADER_YA_CONSUMER_CLIENT_IP, USER_YA_IP);
        headersForReqSpecB.put(HEADER_YA_CLIENT_HOST, USER_HOST);
        headersForReqSpecB.put(HEADER_YA_CLIENT_USER_AGENT, USER_USER_AGENT);

        reqSpecB.addQueryParam(STR_CONSUMER, props().getPassportConsumer());
        return this;
    }

    public ApiAuthPasswordSubmit withAllDefaults() {
        headersForReqSpecB.put(HEADER_YA_CONSUMER_CLIENT_IP, USER_YA_IP);
        headersForReqSpecB.put(HEADER_YA_CLIENT_HOST, USER_HOST);
        headersForReqSpecB.put(HEADER_YA_CLIENT_USER_AGENT, USER_USER_AGENT);
        headersForReqSpecB.put(HEADER_YA_CONSUMER_CLIENT_SCHEME, "https");
        headersForReqSpecB.put(HEADER_YA_CLIENT_COOKIE, ";");

        reqSpecB.addQueryParam(STR_CONSUMER, props().getPassportConsumer());
        return this;
    }

    public ApiAuthPasswordSubmit withConsumer(String consumer) {
        reqSpecB.addQueryParam(STR_CONSUMER, consumer);
        return this;
    }

    public ApiAuthPasswordSubmit withScheme(String scheme) {
        headersForReqSpecB.put(HEADER_YA_CONSUMER_CLIENT_SCHEME, scheme);
        return this;
    }

    public ApiAuthPasswordSubmit withLogin(String login) {
        reqSpecB.addParam(STR_LOGIN, login);
        return this;
    }

    public ApiAuthPasswordSubmit withPassword(String password) {
        reqSpecB.addParam(STR_PASSWORD, password);
        return this;
    }

    public ApiAuthPasswordSubmit withService(String service) {
        reqSpecB.addParam("service", service);
        return this;
    }

    public ApiAuthPasswordSubmit withUser(UserWithProps user) {
        reqSpecB.addParam(STR_LOGIN, user.getLogin())
                .addParam(STR_PASSWORD, user.getAuthPasswd());
        return this;
    }

    public ApiAuthPasswordSubmit withRetpath(String retpath) {
        reqSpecB.addParam(STR_RETPATH, retpath);
        return this;
    }

    public ApiAuthPasswordSubmit withTrackId(String trackId) {
        reqSpecB.addParam(STR_TRACK_ID, trackId);
        return this;
    }

    public ApiAuthPasswordSubmit withUserAgent(String userAgent) {
        headersForReqSpecB.put(HEADER_YA_CLIENT_USER_AGENT, userAgent);
        return this;
    }

    public ApiAuthPasswordSubmit withYaConsumerClientIp(String yaConsumerClientIp) {
        headersForReqSpecB.put(HEADER_YA_CONSUMER_CLIENT_IP, yaConsumerClientIp);
        return this;
    }

    public ApiAuthPasswordSubmit withYaConsumerClientHost(String yaConsumerClientHost) {
        headersForReqSpecB.put(HEADER_YA_CLIENT_HOST, yaConsumerClientHost);
        return this;
    }

    public ApiAuthPasswordSubmit withYaClientCookie(String yaClientCookie) {
        headersForReqSpecB.put(HEADER_YA_CLIENT_COOKIE, yaClientCookie);
        return this;
    }

    public ApiAuthPasswordSubmit withCookies(Cookies cookies) {
        headersForReqSpecB.put(HEADER_YA_CLIENT_COOKIE, Joiner.on(";").join(cookies) + ";");
        return this;
    }

    /**
     * Просто успешная авторизация с проходом капчи, без ожидания какого-то особого ответа
     */
    public Response callAuthPasswordSubmitWithCaptchaPassingAndCheckThatJS200Ok() {
        return withDefaults().withScheme("https")
                .callAuthPasswordSubmitWithCaptchaPassingAndCheckResponse(shouldBe200Ok());
    }

    /**
     * После того, как мы прописали все нужные параметры вызова авторизации,
     * нам требуется вызвать саму авторизацию.
     * В данном методе мы авторизуемся с прохождением капчи, если нам её показали.
     * И без прохождения капчи, если нам её не показали.
     * Для этого мы вызываем авторизацию, проверяя только то, что на выходе
     * у нас JSON. А далее проверяем успешность вызова авторизации, и если
     * требуется капча, то проходим капчу, и вызываем авторизацию ещё раз
     * с треком, в котором пройдена капча.
     * Перед тем, как вернуть ответ, проверяем его на
     * соотвествие с указанной в начале метода спецификацией ответа responseSpecification
     * на тот случай, если возникли ошибки не про капчу.
     * Спецификацию ответа мы задаём в самом методе, а ен передаём, как параметр, т. к.
     * данный методот должен быть максимально удобен в использоваении и качестве инструмента
     * "Просто авторизации".
     * Возможно, есть более надёжное условие по выполнению, которого можно
     * определить, успешна ли авторизация, но я пока придумала только
     * проверку того, что выдаётся ошибка о требовании капчи
     */
    public Response callAuthPasswordSubmitWithCaptchaPassingAndCheckResponse(ResponseSpecification
                                                                                     responseSpecification) {
        reqSpecB.addHeaders(headersForReqSpecB);
        reqSpecB.addHeader(HEADER_X_YA_SERVICE_TICKET, ticketsProvider().getPassportApiTicket());
        Response resp = requestPasswordSubmitWithFluentReqSpecAndRespSpec(reqSpecB.build(), respSpecJS());
        //вынесла сюда, чтобы добавлять один и тот же трек к запросу только один раз
        String trackId = getTrackIdFromApiResponse(resp);
        reqSpecB.addParam(STR_TRACK_ID, trackId);
        int numberOfTries = 0;
        while (resp.jsonPath().get(STR_STATUS).toString().equalsIgnoreCase(STR_ERROR)
                && resp.jsonPath().get(STR_ERRORS).toString().equalsIgnoreCase("[captcha.required]")
                && numberOfTries < 10) {
            numberOfTries++;
            passCaptchaForTrackId(trackId);
            resp = requestPasswordSubmitWithFluentReqSpecAndRespSpec(reqSpecB.build(), respSpecJS());
        }
        return resp.then().spec(responseSpecification).extract().response();
    }

    public Response call(ResponseSpecification responseSpecification) {
        reqSpecB.addHeaders(headersForReqSpecB);
        reqSpecB.addHeader(HEADER_X_YA_SERVICE_TICKET, ticketsProvider().getPassportApiTicket());
        return requestPasswordSubmitWithFluentReqSpecAndRespSpec(reqSpecB.build(), responseSpecification);
    }

    private static Response requestPasswordSubmitWithFluentReqSpecAndRespSpec(RequestSpecification reqSpec,
                                                                              ResponseSpecification respSpec) {
        return executeRequestWithFluentReqSpecAndRespSpec(reqSpec, POST, respSpec, PassportUris.getBundleAuthPasswordSubmitUri());
    }


    public static SessionIdCookie authWithLoginPasswordAndGetSessionId(String login, String password) {
        return sessionIdFrom(authWithLoginPasswordAndGetAllCookies(login, password));
    }

    public static Cookies authWithLoginPasswordAndGetAllCookies(String login, String password) {
        return CookieSteps.forUser(userWithLoginAndPassword(login, password)).authCookies();
    }

    public static Cookies authOnDomainWithLoginPasswordAndGetAllCookies(YandexDomain domain,
                                                                        String login, String password) {
        Response resp = new ApiAuthPasswordSubmit()
                .withDefaults()
                .withYaConsumerClientHost("passport.yandex." + domain.getDomain())
                .withScheme("https")
                .withCookies(EMPTY_COOKIES)
                .withLogin(login)
                .withPassword(password)
                .callAuthPasswordSubmitWithCaptchaPassingAndCheckResponse(shouldBe200Ok());
        return parseCookies(resp);
    }


    public static Cookies callAuthSubmitWithLoginPasswordCookies(String login, String password,
                                                                 Cookies cookies,
                                                                 ResponseSpecification responseSpecification) {
        Response resp = new ApiAuthPasswordSubmit()
                .withDefaults()
                .withScheme("https")
                .withCookies(cookies)
                .withLogin(login)
                .withPassword(password)
                .callAuthPasswordSubmitWithCaptchaPassingAndCheckResponse(responseSpecification);
        return parseCookies(resp);
    }

    public static boolean cookiesHasSlotsForNumberOfUsers(Cookies cookies, int numOfUsersNeedToAdd) {
        if (cookies.hasCookieWithName(YANDEX_SESSION_ID_NAME)) {
            return (sessionIdFrom(cookies).getUsers().size() + numOfUsersNeedToAdd <=
                    MAX_NUMBER_OF_USERS_IN_SESSION_ID);
        } else {
            return numOfUsersNeedToAdd <= MAX_NUMBER_OF_USERS_IN_SESSION_ID;
        }
    }

    /**
     * Метод позволяет дополнить переданные куки
     * нужным количеством балластных пользвоателей.
     * Если куки не пустые, то проверяем, есть ли в них ещё место
     */
    public static Cookies fillingCookiesWithBallastUsers(Cookies cookies,
                                                         int numOfUsersNeedToAdd) {
        assertThat(String.format("Ожидаем, что в куке есть место для %d новых пользователей", numOfUsersNeedToAdd),
                cookiesHasSlotsForNumberOfUsers(cookies, numOfUsersNeedToAdd), is(true));
        for (int i = 1; i <= numOfUsersNeedToAdd; i++) {
            cookies = CookieSteps.forUser(getBallastOrdinaryUser(i)).addToCookies(cookies);
        }
        return cookies;
    }

    /**
     * Данная вспомогательная функция нужна для большого количества тестов
     * на смену дефолта и разлогин
     */
    public static Cookies loginTwoUsers(UserWithProps user1, UserWithProps user2) {
        return CookieSteps.forUser(user1).addToCookies(user2);
    }

    public static Cookies addUsersToCookies(Cookies cookies, UserWithProps... users) {
        for (UserWithProps user : users) {
            cookies = parseCookies(new ApiAuthPasswordSubmit()
                    .withDefaults()
                    .withScheme("https")
                    .withUser(user)
                    .withCookies(cookies)
                    .callAuthPasswordSubmitWithCaptchaPassingAndCheckThatJS200Ok());
        }
        return cookies;
    }

    public static void robotImitationForGettingMicroProfiles(UserWithProps user) {
        int numberOfTries = 0;
        do {
            numberOfTries++;
            new ApiAuthPasswordSubmit()
                    .withConsumer(props().getPassportConsumer())
                    .withYaConsumerClientHost(USER_HOST)
                    .withYaConsumerClientIp(getRandomUserIpForRandomCountry())
                    .withUserAgent(getRandomString(10))
                    .withScheme("https")
                    .withYaClientCookie("yandexuid=" + getRandomDigits(20))
                    .withLogin(user.getLogin())
                    .withPassword(user.getAuthPasswd())
                    .callAuthPasswordSubmitWithCaptchaPassingAndCheckResponse(shouldBe200Ok());
        }
        while ((numberOfTries < 10));
    }
}

