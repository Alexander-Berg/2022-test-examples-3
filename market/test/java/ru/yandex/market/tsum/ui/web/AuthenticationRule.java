package ru.yandex.market.tsum.ui.web;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.yandex.market.tsum.core.auth.TsumUser;
import ru.yandex.market.tsum.ui.auth.TsumAuthentication;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 27.12.2017
 */
public class AuthenticationRule extends TestWatcher {
    private final String login;

    public AuthenticationRule() {
        this("user");
    }

    public AuthenticationRule(String login) {
        this.login = login;
    }

    @Override
    protected void starting(Description description) {
        TsumAuthentication authentication = new TsumAuthentication(null, null, null, null);
        authentication.authOk(new TsumUser(login), null);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
