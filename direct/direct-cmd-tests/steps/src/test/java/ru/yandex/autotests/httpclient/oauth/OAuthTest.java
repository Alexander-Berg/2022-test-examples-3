package ru.yandex.autotests.httpclient.oauth;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.autotests.direct.cmd.steps.base.DirectStepsContext;
import ru.yandex.autotests.direct.cmd.steps.oauth.OAuthSteps;
import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.httpclientlite.context.ConnectionContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

public class OAuthTest {

    public static final String DIRECT_API_APP = "ae99016820074f809e5c268e564bebad";

    private String login;

    private String password;
    private OAuthSteps oAuthSteps;

    @Before
    public void setUp() {
        login = "yandex-team4294616478";
        password = "S4L-VEp-8PJ-9Ft";

        DirectStepsContext context = new DirectStepsContext()
                .withProperties(DirectTestRunProperties.getInstance());
        context.useConnectionContext(new ConnectionContext().scheme("https").host("oauth.yandex.ru"));
        oAuthSteps = OAuthSteps.getInstance(OAuthSteps.class, context);
        oAuthSteps.auth(login, password);
    }

    @Test
    public void canGenerateToken() {
        String token = oAuthSteps.getTokenForApp(DIRECT_API_APP);
        System.out.println(token);
        assertThat("Token is empty", token, not(equalTo(null)));
    }
}
