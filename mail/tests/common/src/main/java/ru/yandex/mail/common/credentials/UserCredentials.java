package ru.yandex.mail.common.credentials;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.specification.RequestSpecification;
import org.junit.rules.TestWatcher;

import ru.yandex.mail.common.properties.Scopes;

import static io.restassured.RestAssured.given;
import static java.util.Arrays.asList;
import static ru.yandex.mail.common.properties.CoreProperties.props;

public class UserCredentials extends TestWatcher {
    private static Map<Account, Cookies> cache = new HashMap<>();

    private Account account;

    private static Cookies login(Account acc, Cookies cookies) {
        if (props().scope() == Scopes.DEVPACK) {
            String s = "";

            if (cookies != null && cookies.get("Session_id") != null) {
                s = cookies.get("Session_id").getValue();
            }

            s += acc.login();

            return new Cookies(new Cookie.Builder("Session_id", s).build(),
                               new Cookie.Builder("sessionid2", s).build());
        } else {
            RequestSpecification spec = given().redirects().follow(true);

            if (cookies != null) {
                spec.cookies(cookies);
            }

            return spec.baseUri(props().passportHost().toString())
                    .queryParam("mode", "auth")
                    .formParam("login", acc.login())
                    .formParam("passwd", acc.password())
                    .log().uri().log().parameters()
                    .post("/passport")
                    .detailedCookies();
        }
    }

    public UserCredentials(AccountWithScope accountWithScope) {
        this.account = accountWithScope.get();
    }

    public Cookies cookies() {
        Cookies cookies = cache.get(account);
        if (cookies == null) {
            cookies = login(account, null);

            cache.put(account, cookies);
        }

        return cookies;
    }

    public static Cookies cookies(Account... accounts) {
        return cookies(asList(accounts));
    }

    public static Cookies cookies(List<Account> accounts) {
        Cookies cookies = new Cookies();

        for (Account acc: accounts) {
            cookies = login(acc, cookies);
        }

        return cookies;
    }

    public Account account() {
        return this.account;
    }

    public UserCredentials withHandler(UserHandler handler) {
        handler.handle(this);
        return this;
    }
}
