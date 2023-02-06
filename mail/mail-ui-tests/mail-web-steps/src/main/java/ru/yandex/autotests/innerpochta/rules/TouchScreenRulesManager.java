package ru.yandex.autotests.innerpochta.rules;

import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.rules.enviroment.EnvironmentInfoRule;

import static org.junit.rules.RuleChain.outerRule;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.cal.rules.ChangeTzSetting.changeTzSetting;
import static ru.yandex.autotests.innerpochta.rules.FailScreenRule.failScreenRule;
import static ru.yandex.autotests.innerpochta.rules.FilterRunRule.filterRunRule;
import static ru.yandex.autotests.innerpochta.rules.RetryRule.baseRetry;
import static ru.yandex.autotests.innerpochta.touch.rules.TurnOffPromoAndAdvertRule.turnOffPromoAndAdvert;

/**
 * @author vasily-k
 */
public class TouchScreenRulesManager {

    private WatchRule testWatcher = new WatchRule();
    private RetryRule retry = baseRetry();
    private AccLockRule lock = AccLockRule.use().className();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private TouchWebDriverRule webDriverProd = (TouchWebDriverRule) new TouchWebDriverRule().withRetry(retry);
    private TouchWebDriverRule webDriverTest = (TouchWebDriverRule) new TouchWebDriverRule().withRetry(retry);
    private InitStepsRule stepsProd = new InitStepsRule(webDriverProd, auth);
    private InitStepsRule stepsTest = new InitStepsRule(webDriverTest, auth);
    private EnvironmentInfoRule environmentInfoRule = new EnvironmentInfoRule();
    private FailScreenRule failScreenProd = failScreenRule(webDriverProd);
    private FailScreenRule failScreenTest = failScreenRule(webDriverTest);
    private TestRule ignoreRule = new ConditionalIgnoreRule();
    private TestRule filterRule = filterRunRule();

    public static TouchScreenRulesManager touchScreenRulesManager() {
        return new TouchScreenRulesManager();
    }

    public TouchScreenRulesManager withLock(AccLockRule lock) {
        this.lock = lock;
        auth = RestAssuredAuthRule.auth(lock);
        stepsProd = new InitStepsRule(webDriverProd, auth);
        stepsTest = new InitStepsRule(webDriverTest, auth);
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

    public RuleChain createTouchRuleChain() {
        return outerRule(retry)
            .around(ignoreRule)
            .around(filterRule)
            .around(testWatcher)
            .around(lock)
            .around(auth)
            .around(webDriverProd)
            .around(stepsProd)
            .around(webDriverTest)
            .around(stepsTest)
            .around(environmentInfoRule)
            .around(failScreenProd)
            .around(failScreenTest)
            .around(turnOffPromoAndAdvert(() -> stepsProd.user()))
            .around(turnOffPromoAndAdvert(() -> stepsProd.user()))
            .around(clearAcc(() -> stepsProd.user()));
    }

    public RuleChain createCalendarTouchRuleChain() {
        return outerRule(retry)
            .around(ignoreRule)
            .around(testWatcher)
            .around(lock)
            .around(auth)
            .around(webDriverProd)
            .around(stepsProd)
            .around(webDriverTest)
            .around(stepsTest)
            .around(environmentInfoRule)
            .around(failScreenProd)
            .around(failScreenTest)
            .around(changeTzSetting(() -> stepsProd.user()));
    }
}
