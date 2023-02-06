package ru.yandex.autotests.innerpochta.mbody;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.runners.model.MultipleFailureException;
import ru.yandex.autotests.innerpochta.wmi.core.api.MbodyApi;
import ru.yandex.autotests.innerpochta.wmi.core.exceptions.RetryException;
import ru.yandex.autotests.innerpochta.wmi.core.mbody.ApiMbody;
import ru.yandex.autotests.innerpochta.wmi.core.rules.*;
import ru.yandex.autotests.innerpochta.wmi.core.rules.acclock.AccLockRule;
import ru.yandex.autotests.lib.junit.rules.retry.RetryRule;

import java.util.concurrent.TimeUnit;

import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.IgnoreRule.newIgnoreRule;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.IgnoreSshTestRule.newIgnoreSshTestRule;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.WriteAllureParamsRule.writeParamsForAllure;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.XRequestIdRule.xRequestIdRule;

public abstract class MbodyBaseTest {
    public static AccLockRule lock = new AccLockRule();

    public static HttpClientManagerRule authClient = auth().withAnnotation().lock(lock);

    @ClassRule
    public static RuleChain chainAuth = RuleChain.outerRule(lock).around(authClient);

    @ClassRule
    public static IgnoreRule beforeTestClass = newIgnoreRule();

    @ClassRule
    public static IgnoreSshTestRule beforeSshTestClass = newIgnoreSshTestRule();

    @Rule
    public IgnoreRule beforeTest = newIgnoreRule();

    @Rule
    public UtilsRule utils = new UtilsRule(authClient);

    @Rule
    public WriteAllureParamsRule writeAllureParamsRule = writeParamsForAllure();

    @Rule
    public XRequestIdRule setXRequestId = xRequestIdRule();

    @Rule
    public RetryRule retryRule = RetryRule.retry().ifException(RetryException.class)
            .or()
            .ifException(MultipleFailureException.class)
            .or()
            //эксперимент
            .ifException(AssertionError.class)
            .every(1, TimeUnit.SECONDS).times(1);

    protected ApiMbody apiMbody() {
        return MbodyApi.apiMbody(authClient.account().userTicket());
    }

    protected static String uid() {
        return authClient.account().uid();
    }
}
