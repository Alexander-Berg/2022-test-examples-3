package ru.yandex.mail.common.credentials;

import java.util.Map;

import ru.yandex.mail.common.properties.Scopes;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.mail.common.properties.CoreProperties.props;
import static org.hamcrest.MatcherAssert.assertThat;


public class AccountWithScope {
    private Map<Scopes, Account> accs;

    public AccountWithScope(Map<Scopes, Account> accounts) {
        accs = accounts;
    }

    public Account get() {
        Scopes scope = props().scope();

        assertThat("Нет аккаунта для скоупа " + scope.getName(), accs.get(scope), not(nullValue()));

        return accs.get(scope);
    }
}
