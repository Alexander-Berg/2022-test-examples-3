package ru.yandex.autotests.direct.cmd.rules;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.autotests.direct.cmd.DirectCmdSteps;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.steps.ApiAggregationSteps;
import ru.yandex.autotests.direct.cmd.util.StageSemaphore;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.steps.UserSteps;
import ru.yandex.autotests.httpclient.lite.core.exceptions.BackEndClientException;
import ru.yandex.autotests.httpclientlite.HttpClientLiteException;
import ru.yandex.junitextensions.rules.retry.RetryRule;

import static ru.yandex.autotests.direct.utils.BaseSteps.getInstance;

/*
* todo javadoc
*/
public class DirectCmdRule implements TestRule {

    public final Logger LOG;
    public DirectTestRunProperties properties;

    public DirectCmdRule(DirectTestRunProperties properties) {
        this.properties = properties;
        LOG = LoggerFactory.getLogger(this.getClass());
    }

    protected DirectCmdRule() {
        this(DirectTestRunProperties.getInstance());
    }

    public static DirectCmdRule defaultClassRule() {
        return new DirectCmdRule()
                .withRules(defaultClassRuleChain())
                .useAuth(false);
    }

    public static DirectCmdRule stepsClassRule() {
        return new DirectCmdRule()
                .withRules(defaultClassRuleChain());
    }

    public static DirectCmdRule defaultRule() {
        return new DirectCmdRule().withRules(defaultRuleChain());
    }

    private static Integer DEFAULT_RETRY_TIMEOUT = 0;

    private static RetryRule retryOnError() {
        return RetryRule
                .retry()
                .times(DirectTestRunProperties.getInstance().getDirectCmdRetryTimes())
                .every(DEFAULT_RETRY_TIMEOUT, TimeUnit.SECONDS)
                .ifException(HttpClientLiteException.class)
                .ifException(BackEndClientException.class);
    }

    private static RuleChain defaultClassRuleChain() {
        DirectTestRunProperties p = DirectTestRunProperties.getInstance();
        TestRule semaphoreRule = StageSemaphore.getSemaphore(p.getDirectHost(), p.getDirectSemaphorePermits());

        RuleChain chain = RuleChain.emptyRuleChain();
        if (semaphoreRule != null) {
            chain = chain.around(semaphoreRule);
        }
        return chain;
    }

    private static RuleChain defaultRuleChain() {
        return RuleChain.emptyRuleChain().around(retryOnError());
    }

    private List<TestRule> rules = new LinkedList<>();

    private String authLogin;

    private Boolean useAuth = true;

    private DirectCmdSteps cmdSteps;

    private ApiStepsRule apiStepsRule;

    private DarkSideSteps darkSideSteps;

    private DirectJooqDbSteps dbSteps;

    public DirectCmdRule as(String authLogin) {
        this.authLogin = authLogin;
        return this;
    }

    public DirectCmdRule withRules(TestRule... rules) {
        Collections.addAll(this.rules, rules);
        return this;
    }

    public DirectCmdRule useAuth(Boolean useAuth) {
        this.useAuth = useAuth;
        return this;
    }

    public ru.yandex.autotests.direct.httpclient.UserSteps oldSteps() {
        return cmdSteps().oldSteps();
    }

    public DirectCmdSteps cmdSteps() {
        if (cmdSteps == null) {
            cmdSteps = new DirectCmdSteps(properties);
        }
        return cmdSteps;
    }

    public UserSteps apiSteps() {
        return getApiStepsRule().userSteps();
    }

    public ApiStepsRule getApiStepsRule() {
        if (apiStepsRule == null) {
            apiStepsRule = new ApiStepsRule(properties);
        }
        return apiStepsRule;
    }

    public DarkSideSteps darkSideSteps() {
        if (darkSideSteps == null) {
            darkSideSteps = new DarkSideSteps(properties);
        }
        return darkSideSteps;
    }

    public DirectJooqDbSteps dbSteps() {
        if (dbSteps == null) {
            dbSteps = new DirectJooqDbSteps();
        }
        return dbSteps;
    }

    public Boolean getUseAuth() {
        return useAuth;
    }

    public String getAuthLogin() {
        if (authLogin == null) {
            authLogin = Logins.SUPER;
        }
        return authLogin;
    }

    private RuleChain buildChain() {
        RuleChain chain = RuleChain.emptyRuleChain();
        chain = chain.around(getApiStepsRule());
        if (getUseAuth()) {
            chain = chain.around(new AuthRule(getAuthLogin()).withDirectCmdSteps(cmdSteps()));
        }
        for (TestRule rule : rules) {
            if (rule instanceof NeedsCmdSteps) {
                ((NeedsCmdSteps) rule).withDirectCmdSteps(cmdSteps());
            }
            chain = chain.around(rule);
        }
        return chain;
    }

    public Logger log() {
        return LOG;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return buildChain().apply(base, description);
    }

    public ApiAggregationSteps apiAggregationSteps() {
        return getInstance(ApiAggregationSteps.class, apiSteps());
    }
}
