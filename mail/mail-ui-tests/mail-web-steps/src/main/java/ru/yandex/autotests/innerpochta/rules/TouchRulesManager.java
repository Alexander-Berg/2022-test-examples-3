package ru.yandex.autotests.innerpochta.rules;

import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.rules.enviroment.EnvironmentInfoRule;

import static org.junit.rules.RuleChain.outerRule;
import static ru.yandex.autotests.innerpochta.cal.rules.ChangeTzSetting.changeTzSetting;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.FailScreenRule.failScreenRule;
import static ru.yandex.autotests.innerpochta.rules.FilterRunRule.filterRunRule;
import static ru.yandex.autotests.innerpochta.rules.RetryRule.baseRetry;
import static ru.yandex.autotests.innerpochta.touch.rules.TurnOffPromoAndAdvertRule.turnOffPromoAndAdvert;

/**
 * @author vasily-k
 */
public class TouchRulesManager {

    private WatchRule testWatcher = new WatchRule();
    private RetryRule retry = baseRetry();
    private AccLockRule lock = AccLockRule.use().className();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private TouchWebDriverRule webDriver = (TouchWebDriverRule) new TouchWebDriverRule().withRetry(retry);
    private InitStepsRule steps = new InitStepsRule(webDriver, auth);
    private EnvironmentInfoRule environmentInfoRule = new EnvironmentInfoRule();
    private FailScreenRule failScreenRule = failScreenRule(webDriver);
    private TestRule ignoreRule = new ConditionalIgnoreRule();
    private TestRule filterRule = filterRunRule();

    public static TouchRulesManager touchRulesManager() {
        return new TouchRulesManager();
    }

    public TouchRulesManager withLock(AccLockRule lock) {
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

    public RuleChain createTouchRuleChain() {
        RuleChain chain = outerRule(retry)
            .around(ignoreRule)
            .around(filterRule)
            .around(testWatcher);
        if (lock != null) {
            chain = chain.around(lock)
                .around(auth)
                .around(turnOffPromoAndAdvert(() -> steps.user()))
                .around(clearAcc(() -> steps.user()));
        }
        return chain.around(webDriver)
            .around(steps)
            .around(environmentInfoRule)
            .around(failScreenRule);
    }

    public RuleChain createCalendarTouchRuleChain() {
        RuleChain chain = outerRule(retry)
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
