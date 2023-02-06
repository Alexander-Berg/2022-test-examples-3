package ru.yandex.mail.tests.akita;

import java.util.concurrent.TimeUnit;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.Cookies;
import io.restassured.specification.ResponseSpecification;
import org.apache.http.HttpStatus;
import org.junit.ClassRule;
import org.junit.Rule;

import ru.yandex.mail.common.rules.RetryRule;
import ru.yandex.mail.tests.akita.generated.ApiAkita;
import ru.yandex.mail.tests.akita.generated.auth.ApiAuth;
import ru.yandex.mail.tests.akita.generated.checkcookies.ApiCheckCookies;
import ru.yandex.mail.tests.akita.generated.ninjaauth.ApiNinjaAuth;
import ru.yandex.mail.common.credentials.AccountWithScope;
import ru.yandex.mail.common.credentials.UserCredentials;
import ru.yandex.mail.common.properties.Scopes;
import ru.yandex.mail.common.rules.BbResponseUploader;
import ru.yandex.mail.common.rules.IgnoreRule;
import ru.yandex.mail.common.rules.WriteAllureParamsRule;
import ru.yandex.mail.common.rules.XRequestIdRule;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.mail.common.properties.CoreProperties.props;
import static ru.yandex.mail.common.rules.IgnoreRule.newIgnoreRule;
import static ru.yandex.mail.common.rules.WriteAllureParamsRule.writeParamsForAllure;
import static ru.yandex.mail.common.rules.XRequestIdRule.xRequestIdRule;

@Features("AKITA")
@Stories("#авторизация")
abstract public class AkitaBaseTest {
    abstract AccountWithScope mainUser();

    static AkitaProperties properties = AkitaProperties.properties();

    UserCredentials authClient = new UserCredentials(mainUser());

    @ClassRule
    public static IgnoreRule beforeTestClass = newIgnoreRule();

    @ClassRule
    public static BbResponseUploader uploader = new BbResponseUploader(Accounts.yplus, Accounts.noMailboxTest,
            Accounts.authTest, Accounts.checkCookiesTest);

    @Rule
    public IgnoreRule beforeTest = newIgnoreRule();

    @Rule
    public WriteAllureParamsRule writeAllureParamsRule = writeParamsForAllure(properties.akitaUri(), props().scope());

    @Rule
    public XRequestIdRule setXRequestId = xRequestIdRule();

    @Rule
    public RetryRule retryRule = RetryRule.retry()
            .ifException(Exception.class)
            .every(1, TimeUnit.SECONDS)
            .times(1);

    static ResponseSpecification noMailbox() {
        return new ResponseSpecBuilder()
                .expectStatusCode(HttpStatus.SC_OK)
                .expectBody("error.code", is(2003))
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

    public static ApiAkita apiAkita(Cookies cookies) {
        return AkitaApi.apiAkita(cookies);
    }

    public static ApiAkita apiAkitaWithoutAuth() {
        return AkitaApi.apiAkitaWithoutAuth();
    }

    static String xOriginalHost() {
        if (props().scope().equals(Scopes.INTRANET_PRODUCTION)) {
            return "mail.yandex-team.ru";
        }

        return "mail.yandex.ru";
    }

    ApiAuth auth(Cookies c) {
        return apiAkita(c).auth()
                .withXoriginalhostHeader(xOriginalHost());
    }

    ApiAuth auth() {
        return apiAkita(authClient.cookies()).auth()
                .withXoriginalhostHeader(xOriginalHost());
    }

    ApiNinjaAuth ninjaAuth() {
        return apiAkita(authClient.cookies()).ninjaAuth()
                .withXoriginalhostHeader(xOriginalHost());
    }

    ApiCheckCookies checkCookies() {
        return apiAkita(authClient.cookies()).checkCookies()
                .withXoriginalhostHeader(xOriginalHost());
    }

    ApiCheckCookies checkCookies(Cookies c) {
        return apiAkita(c).checkCookies()
                .withXoriginalhostHeader(xOriginalHost());
    }
}
