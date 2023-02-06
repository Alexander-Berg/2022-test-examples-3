package ru.yandex.autotests.directmonitoring.tests;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import ru.yandex.autotests.directweb.steps.UserSteps;
import ru.yandex.qatools.allure.webdriver.rules.RetryRule;
import ru.yandex.qatools.allure.webdriver.rules.WebDriverConfiguration;
import ru.yandex.qatools.allure.webdriver.steps.BaseSteps;
import ru.yandex.terra.junit.rules.BottleMessageRule;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.instanceOf;

/**
 * User: buhter
 * Date: 17.06.13
 * Time: 11:51
 */
public abstract class BaseDirectMonitoringTest {

    protected static final String CLIENT_LOGIN = "democrat-spb";
    private final String CLIENT_PASSWORD = "itisme";

    @Rule
    public WebDriverConfiguration config = new WebDriverConfiguration();

    @Rule
    public RuleChain chain = RuleChain.outerRule(new BottleMessageRule()).around(RetryRule
            .retry()
            .every(15, TimeUnit.SECONDS)
            .times(2).ifException(instanceOf(Throwable.class)));

    protected UserSteps user;

    @Before
    public void beforeTest() {
        user = BaseSteps.getInstance(UserSteps.class, config);
        user.authorizeAs(CLIENT_LOGIN, CLIENT_PASSWORD);

        additionalActions();
    }

    public abstract void additionalActions();

}
