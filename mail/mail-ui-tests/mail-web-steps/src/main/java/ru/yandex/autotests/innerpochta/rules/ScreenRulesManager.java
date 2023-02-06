package ru.yandex.autotests.innerpochta.rules;

import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.rules.enviroment.EnvironmentInfoRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;

import static org.junit.rules.RuleChain.outerRule;
import static ru.yandex.autotests.innerpochta.cal.rules.ChangeTzSetting.changeTzSetting;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.FailScreenRule.failScreenRule;
import static ru.yandex.autotests.innerpochta.rules.FilterRunRule.filterRunRule;
import static ru.yandex.autotests.innerpochta.rules.OffSettingsRule.offSettings;
import static ru.yandex.autotests.innerpochta.rules.RetryRule.baseRetry;

/**
 * @author vasily-k
 */
public class ScreenRulesManager {

    private RetryRule retry = baseRetry();
    private WatchRule testWatcher = new WatchRule();
    private AccLockRule lock = AccLockRule.use().className();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private WebDriverRule webDriverProd = new WebDriverRule().withRetry(retry);
    private WebDriverRule webDriverTest = new WebDriverRule().withRetry(retry);
    private InitStepsRule stepsProd = new InitStepsRule(webDriverProd, auth);
    private InitStepsRule stepsTest = new InitStepsRule(webDriverTest, auth);
    private EnvironmentInfoRule environmentInfoRule = new EnvironmentInfoRule();
    private FailScreenRule failScreenProd = failScreenRule(webDriverProd);
    private FailScreenRule failScreenTest = failScreenRule(webDriverTest);
    private TestRule semaphoreRule = SemaphoreRule.semaphoreRule().enableSemaphore();
    private ThemeSetupRule themeSetup = ThemeSetupRule.themeSetupRule(stepsProd);
    private TestRule ignoreRule = new ConditionalIgnoreRule();
    private TestRule filterRule = filterRunRule();

    public static ScreenRulesManager screenRulesManager() {
        return new ScreenRulesManager();
    }

    public ScreenRulesManager withLock(AccLockRule lock) {
        this.lock = lock;
        auth = RestAssuredAuthRule.auth(lock);
        stepsProd = new InitStepsRule(webDriverProd, auth);
        stepsTest = new InitStepsRule(webDriverTest, auth);
        return this;
    }

    public ScreenRulesManager withAuth(RestAssuredAuthRule auth) {
        this.auth = auth;
        stepsProd = new InitStepsRule(webDriverProd, auth);
        stepsTest = new InitStepsRule(webDriverTest, auth);
        return this;
    }

    public ScreenRulesManager withRetry(RetryRule retry) {
        this.retry = retry;
        return this;
    }

    public ScreenRulesManager withWebDriverTest(WebDriverRule webDriverTest) {
        this.webDriverTest = webDriverTest;
        stepsTest = new InitStepsRule(webDriverTest, auth);
        failScreenTest = failScreenRule(webDriverTest);
        return this;
    }

    public AccLockRule getLock() {
        return lock;
    }

    public RestAssuredAuthRule getAuth() {
        return auth;
    }

    public InitStepsRule getStepsProd() {
        return stepsProd;
    }

    public InitStepsRule getStepsTest() {
        return stepsTest;
    }

    public RetryRule getRetryRule() {
        return retry;
    }

    public RuleChain createRuleChain() {
        RuleChain chain = outerRule(new LogConfigRule())
            .around(retry)
            .around(ignoreRule)
            .around(filterRule)
            .around(testWatcher)
            .around(semaphoreRule);
        if (lock != null) {
            chain = chain.around(lock)
                .around(auth)
                .around(clearAcc(() -> stepsProd.user()))
                .around(offSettings(stepsProd))
                .around(themeSetup);
        }
        return chain.around(webDriverProd)
            .around(stepsProd)
            .around(webDriverTest)
            .around(stepsTest)
            .around(environmentInfoRule)
            .around(failScreenProd)
            .around(failScreenTest);
    }

    public RuleChain createCalendarRuleChain() {
        RuleChain chain = outerRule(new LogConfigRule())
            .around(retry)
            .around(ignoreRule)
            .around(filterRule)
            .around(testWatcher);
        if (lock != null) {
            chain = chain.around(lock)
                .around(auth)
                .around(changeTzSetting(() -> stepsProd.user()));
        }
        return chain.around(webDriverProd)
            .around(stepsProd)
            .around(webDriverTest)
            .around(stepsTest)
            .around(environmentInfoRule)
            .around(failScreenProd)
            .around(failScreenTest);
    }
}
