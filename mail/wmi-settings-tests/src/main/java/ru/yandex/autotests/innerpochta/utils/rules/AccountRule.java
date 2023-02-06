package ru.yandex.autotests.innerpochta.utils.rules;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import ru.yandex.autotests.innerpochta.wmi.core.oper.akita.UidAndTvmTicket;

import static ru.yandex.autotests.innerpochta.utils.SettingsProperties.props;
import static ru.yandex.autotests.innerpochta.utils.rules.HttpClientManagerRule.auth;

/**
 * Created by evnikulin on 27.05.19.
 */
public class AccountRule extends TestWatcher {

    private UidAndTvmTicket uidAndTvmTicket;
    private String login;
    private String pwd;

    @Override
    protected void starting(Description description) {
        if (uidAndTvmTicket != null) {
            return;
        }
        with(description.getTestClass());

    }

    public String uid() {
        return uidAndTvmTicket.uid();
    }

    public AccountRule with(Class<?> clazz) {
        login = props().account(clazz).getLogin();
        pwd = props().account(clazz).getPassword();
        fetch();
        return this;

    }

    public AccountRule with(String login_, String pwd_) {
        login = login_;
        pwd = pwd_;
        fetch();
        return this;
    }

    private AccountRule fetch() {
        if ( login == null || pwd == null ) {
            throw new RuntimeException("Login or password not set");
        }
        uidAndTvmTicket = auth().with(login, pwd).login().account();
        return this;
    }
}
