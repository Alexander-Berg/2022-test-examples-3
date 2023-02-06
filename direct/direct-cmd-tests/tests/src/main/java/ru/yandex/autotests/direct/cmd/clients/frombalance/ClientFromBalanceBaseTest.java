package ru.yandex.autotests.direct.cmd.clients.frombalance;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.directapi.model.User;

public abstract class ClientFromBalanceBaseTest {

    protected static final int AGENCY_RUSSIA_CLIENT_ID = 10189002;
    protected static final String AGENCY_RUSSIA = "at-direct-agency-russia-1";
    protected static final int AGENCY_GERMAN_CLIENT_ID = 10189028;
    protected static final String AGENCY_GERMAN = "at-direct-agency-german-1";
    protected static final int AGENCY_BELARUS_CLIENT_ID = 10189047;
    protected static final String AGENCY_BELARUS = "at-direct-agency-belarus-1";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules();
    protected User user;

    protected Integer getRegion(String geo) {
        if (geo == null) {
            return null;
        } else {
            return Integer.parseInt(geo);
        }
    }

    @Before
    public void before() {
    }
}
