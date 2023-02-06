package ru.yandex.mail.common.rules;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import ru.yandex.mail.common.properties.Scopes;
import ru.yandex.qatools.allure.Allure;
import ru.yandex.qatools.allure.events.AddParameterEvent;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 25.06.15
 * Time: 17:15
 */
public class WriteAllureParamsRule extends TestWatcher {
    private String host;
    private Scopes scope;

    public static WriteAllureParamsRule writeParamsForAllure(String host, Scopes scopes) {
        return new WriteAllureParamsRule(host, scopes);
    }

    public WriteAllureParamsRule(String host, Scopes scope) {
        this.host = host;
        this.scope = scope;
    }

    @Override
    protected void starting(Description description) {
        Allure.LIFECYCLE.fire(new AddParameterEvent("HOST", host));
        Allure.LIFECYCLE.fire(new AddParameterEvent("SCOPE", scope.getName()));
    }
}
