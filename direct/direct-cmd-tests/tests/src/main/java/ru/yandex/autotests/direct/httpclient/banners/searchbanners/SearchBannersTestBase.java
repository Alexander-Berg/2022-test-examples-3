package ru.yandex.autotests.direct.httpclient.banners.searchbanners;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.httpclient.CocaineSteps;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.banners.searchbanners.SearchBannersParameters;
import ru.yandex.autotests.direct.httpclient.util.PropertyLoader;
import ru.yandex.autotests.directapi.model.Logins;
import ru.yandex.autotests.directapi.model.User;

/**
 * Created by shmykov on 16.06.15.
 * TESTIRT-ticketid
 */
public class SearchBannersTestBase {

    protected static final String CLIENT_LOGIN = "at-direct-searchbanners1";

    public BannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT_LOGIN);

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected static CSRFToken csrfToken;
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    protected String userLogin;
    protected SearchBannersParameters requestParams;
    protected DirectResponse response;

    public SearchBannersTestBase(String userLogin) {
        this.userLogin = userLogin;
    }

    @Before
    public void before() {
        csrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(Logins.SUPER_LOGIN).getPassportUID());
        cmdRule.oldSteps().onPassport().authoriseAs(userLogin, User.get(userLogin).getPassword());

        requestParams = new PropertyLoader<>(SearchBannersParameters.class).getHttpBean("defaultSearchBannersParameters");
        requestParams.setTextSearch(bannersRule.getBannerId().toString());
    }
}
