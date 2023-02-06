package ru.yandex.autotests.innerpochta.imap.rules.base;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import ru.yandex.qatools.allure.Allure;
import ru.yandex.qatools.allure.events.AddParameterEvent;

import static ru.yandex.autotests.innerpochta.imap.config.ImapProperties.props;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 13.05.14
 * Time: 22:11
 * <p/>
 * https://github.com/allure-framework/allure-core#parameters
 */
public class WriteAllureParamsRule extends TestWatcher {

    public static WriteAllureParamsRule writeParamsForAllure() {
        return new WriteAllureParamsRule();
    }

    @Override
    protected void starting(Description description) {
        Allure.LIFECYCLE.fire(new AddParameterEvent("HOST", props().getHost()));
        Allure.LIFECYCLE.fire(new AddParameterEvent("PORT", String.valueOf(props().getPort())));
        Allure.LIFECYCLE.fire(new AddParameterEvent("TYPE", String.valueOf(props().getConnectionType())));
//        Allure.LIFECYCLE.fire(new AddParameterEvent("USER:", String.valueOf(props().getPort())));
//        Allure.LIFECYCLE.fire(new AddParameterEvent("PASS:", String.valueOf(props().getPort())));
    }
}
