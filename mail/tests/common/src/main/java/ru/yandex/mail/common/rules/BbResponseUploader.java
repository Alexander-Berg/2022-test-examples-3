package ru.yandex.mail.common.rules;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.runner.Description;

import ru.yandex.mail.common.credentials.Account;
import ru.yandex.mail.common.credentials.AccountWithScope;
import ru.yandex.mail.common.credentials.BbResponse;
import ru.yandex.mail.common.properties.Scopes;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.mail.common.properties.CoreProperties.props;


public class BbResponseUploader extends TestWatcherWithExceptions {
    private Map<String, String> accounts;
    private boolean wasCalled = false;

    public BbResponseUploader(AccountWithScope... accs) {
        if (props().scope() == Scopes.DEVPACK) {
            accounts = Arrays.stream(accs)
                    .map(AccountWithScope::get)
                    .collect(Collectors.toMap(Account::login, Account::fakeBbSessionId));
        }
    }

    public BbResponseUploader(Map<String, BbResponse> responses) {
        if (props().scope() == Scopes.DEVPACK) {
            accounts = responses.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, c -> c.getValue().toString()));
        }
    }

    @Override
    protected void starting(Description description) {
        if (props().scope() == Scopes.DEVPACK && !wasCalled) {
            for (Map.Entry<String, String> v : accounts.entrySet()) {
                int statusCode = given()
                        .redirects()
                        .follow(true)
                        .baseUri(props().fakeBbUri())
                        .queryParam("name", v.getKey())
                        .formParam("xml", v.getValue())
                        .log().uri().log().parameters()
                        .post("/save_sessionid")
                        .statusCode();

                assertThat(statusCode, equalTo(200));
            }

            wasCalled = true;
        }
    }
}

