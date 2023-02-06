package ru.yandex.autotests.innerpochta.util.props;

import ru.yandex.autotests.innerpochta.objstruct.base.misc.Account;
import ru.yandex.qatools.properties.PropertyLoader;
import ru.yandex.qatools.properties.annotations.Property;
import ru.yandex.qatools.properties.annotations.Resource;
import ru.yandex.qatools.properties.annotations.Use;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * User: lanwen
 * Date: 29.08.13
 * Time: 13:27
 */
@Resource.Classpath("accounts.properties")
public class AccountsProperties {

    private static AccountsProperties instance;

    public static AccountsProperties accounts() {
        if (null == instance) {
            instance = new AccountsProperties();
        }
        return instance;
    }

    private AccountsProperties() {
        PropertyLoader.populate(this);
    }

    @Property("custom.user.login")
    private String culogin = "yandex-team-mailproto-003";

    @Property("custom.user.pwd")
    private String cupwd = "simple123456";

    @Property("with.custom.user")
    private boolean withcu = false;

    @Property("accounts.json.file")
    @Use(GroupAccountsConverter.class)
    private Map<String, Map<String, Account>> accounts = new HashMap<String, Map<String, Account>>() {{
        put("default",
                new HashMap<String, Account>() {{
                    put("default", new Account("defaultuser", "testqa"));
                }}
        );
    }};


    public Account account(String group, String name) {
        Account account = accounts.get(group).get(name);
        if (withcu) {
            return new Account(culogin, cupwd);
        }
        return account == null ? new Account("default", "default") : account;
    }

    public List<String> groups() {
        return new ArrayList<>(accounts.keySet());
    }

    public Map<String, Map<String, Account>> getAccounts() { return this.accounts; }
}
