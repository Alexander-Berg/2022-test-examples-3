package ru.yandex.autotests.innerpochta.rules;

import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.rules.enviroment.EnvironmentInfoRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;

import static org.junit.rules.RuleChain.outerRule;
import static ru.yandex.autotests.innerpochta.cal.rules.ChangeTzSetting.changeTzSetting;
import static ru.yandex.autotests.innerpochta.rules.FailScreenRule.failScreenRule;
import static ru.yandex.autotests.innerpochta.rules.FilterRunRule.filterRunRule;
import static ru.yandex.autotests.innerpochta.rules.RetryRule.baseRetry;

/**
 * @author vasily-k
 */
public class CalendarRulesManager {

    private RetryRule retry = baseRetry();
    private WatchRule testWatcher = new WatchRule();
    private AccLockRule lock = AccLockRule.use().className();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private WebDriverRule webDriver = new WebDriverRule().withRetry(retry);
    private InitStepsRule steps = new InitStepsRule(webDriver, auth);
    private EnvironmentInfoRule environmentInfoRule = new EnvironmentInfoRule();
    private FailScreenRule failScreenRule = failScreenRule(webDriver);
    private TestRule ignoreRule = new ConditionalIgnoreRule();
    private TestRule filterRule = filterRunRule();

    public static CalendarRulesManager calendarRulesManager() {
        return new CalendarRulesManager();
    }

    public CalendarRulesManager withLock(AccLockRule lock) {
        this.lock = lock;
        auth = RestAssuredAuthRule.auth(lock);
        steps = new InitStepsRule(webDriver, auth);
        return this;
    }

    public AccLockRule getLock() {
        return lock;
    }

    public RestAssuredAuthRule getAuth() {
        return auth;
    }

    public InitStepsRule getSteps() {
        return steps;
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
                .around(changeTzSetting(() -> steps.user()));
        }
        return chain.around(webDriver)
            .around(steps)
            .around(environmentInfoRule)
            .around(failScreenRule);
    }
}
