package ru.yandex.autotests.innerpochta.akita;

import com.jayway.restassured.builder.ResponseSpecBuilder;
import com.jayway.restassured.response.Cookies;
import com.jayway.restassured.specification.ResponseSpecification;
import org.apache.http.HttpStatus;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runners.model.MultipleFailureException;
import ru.yandex.autotests.innerpochta.wmi.core.akita.auth.ApiAuth;
import ru.yandex.autotests.innerpochta.wmi.core.akita.checkcookies.ApiCheckCookies;
import ru.yandex.autotests.innerpochta.wmi.core.akita.ninjaauth.ApiNinjaAuth;
import ru.yandex.autotests.innerpochta.wmi.core.exceptions.RetryException;
import ru.yandex.autotests.innerpochta.wmi.core.rules.*;
import ru.yandex.autotests.innerpochta.wmi.core.rules.acclock.AccLockRule;
import ru.yandex.autotests.lib.junit.rules.retry.RetryRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.innerpochta.wmi.core.api.WmiApis.apiAkita;
import static ru.yandex.autotests.innerpochta.wmi.core.api.WmiApis.apiAkitaOAuth;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.xOriginalHost;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.IgnoreRule.newIgnoreRule;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.IgnoreSshTestRule.newIgnoreSshTestRule;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.WriteAllureParamsRule.writeParamsForAllure;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.XRequestIdRule.xRequestIdRule;

public class AkitaBaseTest {
    public static AccLockRule lock = new AccLockRule();

    public static HttpClientManagerRule authClient = HttpClientManagerRule.auth().withAnnotation().lock(lock);

    @ClassRule
    public static RuleChain chainAuth = RuleChain.outerRule(lock).around(authClient);

    @ClassRule
    public static IgnoreRule beforeTestClass = newIgnoreRule();

    @Rule
    public IgnoreRule beforeTest = newIgnoreRule();

    @ClassRule
    public static IgnoreSshTestRule beforeSshTestClass = newIgnoreSshTestRule();

    @Rule
    public IgnoreSshTestRule beforeSshTest = newIgnoreSshTestRule();

    @Rule
    public TestRule chainRule = RuleChain
            .outerRule(new LogConfigRule())
            .around(new UpdateHCFieldRule(authClient, this, "hc"));

    @Rule
    public WriteAllureParamsRule writeAllureParamsRule = writeParamsForAllure();

    @Rule
    public XRequestIdRule setXRequestId = xRequestIdRule();

    @Rule
    public RetryRule retryRule = RetryRule.retry().ifException(RetryException.class)
            .or()
            .ifException(MultipleFailureException.class)
            .or()
            //эксперимент
            .ifException(AssertionError.class)
            .every(1, TimeUnit.SECONDS).times(1);

    static ResponseSpecification noMailbox() {
        return new ResponseSpecBuilder()
                .expectStatusCode(HttpStatus.SC_OK)
                .expectBody("error.code", is(2003))
                .build();
    }

    static ResponseSpecification internalError() {
        return new ResponseSpecBuilder()
                .expectStatusCode(HttpStatus.SC_OK)
                .expectBody("error.code", is(2005))
                .build();
    }

    static ResponseSpecification noAuth() {
        return new ResponseSpecBuilder()
                .expectStatusCode(HttpStatus.SC_OK)
                .expectBody("error.code", is(2001))
                .expectBody("error.message", equalTo("authentication required"))
                .build();
    }

    public static ResponseSpecification okAuth() {
        return new ResponseSpecBuilder()
                .expectStatusCode(HttpStatus.SC_OK)
                .expectBody("account_information", notNullValue())
                .build();
    }

    public static ResponseSpecification frozenAuth() {
        return new ResponseSpecBuilder()
                .expectStatusCode(HttpStatus.SC_OK)
                .expectBody("account_information", notNullValue())
                .expectBody("account_information.userId", notNullValue())
                .expectBody("account_information.login", notNullValue())
                .expectBody("account_information.firstName", notNullValue())
                .expectBody("account_information.avatar", notNullValue())
                .expectBody("account_information.haveYaplus", notNullValue())
                .expectBody("error.code", is(2020))
                .expectBody("error.reason", equalTo("the user is inactive (frozen or archived)"))
                .build();
    }

    public static ResponseSpecification ok200() {
        return new ResponseSpecBuilder()
                .expectStatusCode(HttpStatus.SC_OK)
                .build();
    }

    public static ResponseSpecification badRequest400() {
        return new ResponseSpecBuilder()
            .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
            .expectBody("error.code", is(2001))
            .expectBody("error.reason", equalTo("authentication required"))
            .build();
    }

    private Map<String, String> cookies() {
        return authClient.authHC().getCookieStore().getCookies()
                .stream()
                .collect(Collectors.toMap(Cookie::getName, Cookie::getValue));
    }

    ApiAuth auth(Cookies c) {
        return apiAkita(c).auth()
                .withXoriginalhostHeader(xOriginalHost());
    }

    ApiAuth auth() {
        return apiAkita(cookies()).auth()
                .withXoriginalhostHeader(xOriginalHost());
    }

    ApiAuth oauth() {
        return apiAkitaOAuth(authClient.getToken()).auth()
                .withXoriginalhostHeader(xOriginalHost());
    }

    ApiNinjaAuth ninjaAuth() {
        return apiAkita(cookies()).ninjaAuth()
                .withXoriginalhostHeader(xOriginalHost());
    }

    ApiCheckCookies checkCookies() {
        return apiAkita(cookies()).checkCookies()
            .withXoriginalhostHeader(xOriginalHost());
    }

    ApiCheckCookies checkCookies(Cookies c) {
        return apiAkita(c).checkCookies()
            .withXoriginalhostHeader(xOriginalHost());
    }

    protected DefaultHttpClient hc;
}
