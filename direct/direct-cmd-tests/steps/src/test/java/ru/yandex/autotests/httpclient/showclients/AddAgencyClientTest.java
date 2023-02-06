package ru.yandex.autotests.httpclient.showclients;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.autotests.direct.httpclient.UserSteps;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class AddAgencyClientTest {
    String clientLogin = "at-sc-deposit3";
    String agencyLogin = "at-agency-deposit1";

    UserSteps user;

    @Before
    public void setup() {
        user = new UserSteps(DirectTestRunProperties.getInstance());
        user.onPassport().authoriseAs("at-direct-super", "at-tester4");
    }

    @Test
    @Ignore
    public void canAddClientToAgency() {
        DirectResponse result = user.agencySteps().addClientToAgency(agencyLogin, clientLogin);
        assertThat("Не удалось добавить клиента агентству",
                result.getStatusLine().getStatusCode(), equalTo(HttpStatus.SC_MOVED_TEMPORARILY));
    }
}
