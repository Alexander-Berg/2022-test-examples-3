package ru.yandex.autotests.directmonitoring.tests;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.lib.junit.rules.passport.PassportRule;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.allure.webdriver.rules.RetryRule;
import ru.yandex.terra.junit.rules.BottleMessageRule;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * User: buhter
 * Date: 01.04.13
 * Time: 13:18
 */
public abstract class BaseDirectPageAvailabilityTest {

    private static final String CLIENT_LOGIN = "democrat-spb";

    private HttpClient client = new DefaultHttpClient();
    private User availabilityUser;

    @Rule
    public PassportRule passportRule;

    @Rule
    public RuleChain chain = RuleChain.outerRule(new BottleMessageRule()).around(RetryRule
            .retry()
            .every(15, TimeUnit.SECONDS)
            .times(2).ifException(instanceOf(Throwable.class)));

    private static String url;
    private static String host;
    private final int EXPECTED_RESPONSE_CODE = 200;

    public BaseDirectPageAvailabilityTest(String url, String host) {
        this.url = url;
        this.host = host;
    }

    @Test
    @Title("Проверка доступности страницы")
    public void availability() throws Throwable {
        availabilityUser = User.get(CLIENT_LOGIN);

        new PassportRule(client)
                .withLoginPassword(availabilityUser.getLogin(), availabilityUser.getPassword())
                .onHost(new URL(host))
                .login();

        HttpGet request = new HttpGet(url);
        request.addHeader("User-Agent", "Mozilla/5.0");
        try {
            assertThat(
                    String.format("Wrong HTTP response code for %s", url),
                    client.execute(request).getStatusLine().getStatusCode(),
                    is(EXPECTED_RESPONSE_CODE)
            );
        } finally {
            request.releaseConnection();
        }
    }
}
