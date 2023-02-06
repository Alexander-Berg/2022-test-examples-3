package ru.yandex.autotests.direct.cmd.steps.auth;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.CookieStore;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.Asserts;

import ru.yandex.autotests.direct.cmd.steps.base.AuthConfig;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.direct.cmd.steps.base.DirectCmdStepsException;
import ru.yandex.autotests.direct.cmd.steps.base.DirectStepsContext;
import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.qatools.allure.annotations.Step;

import static ru.yandex.autotests.direct.cmd.steps.auth.AuthUtils.getFirstCookie;

public class PassportSteps extends DirectBackEndSteps {

    private static final String COOKIE_SESSION_ID = "Session_id";
    private static final String COOKIE_YANDEX_LOGIN = "yandex_login";

    private CsrfTokenSteps csrfTokenSteps;

    @Override
    protected void init(DirectStepsContext context) {
        super.init(context);
        csrfTokenSteps = getInstance(CsrfTokenSteps.class, context);
    }

    @Step("Логинимся в паспорте {0} {1}")
    public void authoriseAs(String login, String password) {
        getContext().getHttpClientConfig().getCookieStore().clear();
        setupContextCredentials(login, password);
        getRequestExecutor().getHttpClientLite().post(
                createLoginURI(), new PassportAuthBean(login, password));
        assertAuthenticationComplete(login);
        addDoNotShowCaptchaCookie();
        csrfTokenSteps.obtainCsrfToken(extractPassportUid());
    }

    private URI createLoginURI() {
        String passportAuthUrl = DirectTestRunProperties.getInstance().getDirectCmdAuthPassportHost();
        Asserts.notNull(passportAuthUrl, "passport url (from property file)");
        try {
            return new URIBuilder(passportAuthUrl).build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("invalid passport auth URI: " + passportAuthUrl);
        }
    }

    private void setupContextCredentials(String login, String password) {
        AuthConfig authConfig = getContext().getAuthConfig();
        if (authConfig == null) {
            authConfig = new AuthConfig();
            getContext().useAuthConfig(authConfig);
        }
        authConfig.setLogin(login);
        authConfig.setPassword(password);
    }

    private void addDoNotShowCaptchaCookie() {
        if ( DirectTestRunProperties.getInstance().isDirectCmdAuthNoCaptcha()) {
            BasicClientCookie cookie = new BasicClientCookie("do_not_show_captcha", "1");
            getContext().getHttpClientConfig().getCookieStore().addCookie(cookie);
        }
    }

    private void assertAuthenticationComplete(String login) {
        CookieStore cookieStore = getContext().getHttpClientConfig().getCookieStore();
        Cookie yandexLoginCookie = getFirstCookie(cookieStore, COOKIE_YANDEX_LOGIN);
        Asserts.check(yandexLoginCookie != null,
                "Can not log in to passport by \"%s\" (cookie \"%s\" is absent). Check credentials!",
                login, COOKIE_YANDEX_LOGIN);
        Asserts.check(login.equals(yandexLoginCookie.getValue()),
                "Can not log in to passport (exepcted: cookie \"%s\" is set to \"%s\"; but: it is set to \"%s\").",
                COOKIE_YANDEX_LOGIN,
                login,
                yandexLoginCookie.getValue());
    }

    private String extractPassportUid() {
        CookieStore cookieStore = getContext().getHttpClientConfig().getCookieStore();
        Cookie sessionIdCookie = getFirstCookie(cookieStore, COOKIE_SESSION_ID);
        if (sessionIdCookie == null) {
            throw new DirectCmdStepsException(
                    "can not extract passport UID from session-id cookie, cookie does not exists");
        }
        try {
            return extractPassportUidFromSessionId(sessionIdCookie.getValue());
        } catch (Exception e) {
            throw new DirectCmdStepsException(
                    "error in extracting passport UID from session-id cookie: " + sessionIdCookie, e);
        }
    }

    private static String extractPassportUidFromSessionId(String sessionId) {
        String[] parts = sessionId.split("\\|");
        String partWithUid = parts[1];
        return partWithUid.substring(0, partWithUid.indexOf("."));
    }
}
