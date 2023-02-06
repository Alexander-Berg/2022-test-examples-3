package ru.yandex.autotests.direct.cmd.banners.additions.callouts.roles;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;

import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.qatools.allure.annotations.Description;

public abstract class CalloutsByDifferentRolesTestBase {
    @ClassRule
    public static DirectCmdRule defaultClassRuleChain = DirectCmdRule.defaultClassRule();
    public String ulogin;
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    protected String expectedCallout = RandomUtils.getString(10);
    private Long cid;

    protected abstract String getSvcClient();

    protected abstract String getUlogin();

    @Description("Дополнения под агентством сервисируемому клиенту")
    public void calloutsByAgencyForSvcClient() {
        ulogin = getSvcClient();
        cmdRule.cmdSteps().authSteps().authenticate(User.get(Logins.AGENCY));
        check();
    }

    @Description("Дополнения под менеджером сервисируемому клиенту")
    public void calloutsByManagerForSvcClient() {
        ulogin = getSvcClient();
        cmdRule.cmdSteps().authSteps().authenticate(User.get(Logins.MANAGER));
        checkForManager();
    }

    @Description("Дополнения под менеджером клиенту с кампанией, созданной менеджером")
    public void calloutsByManagerForClient() {
        ulogin = getUlogin();
        cmdRule.cmdSteps().authSteps().authenticate(User.get(Logins.MANAGER));
        cmdRule.getApiStepsRule().as(Logins.MANAGER);
        cid = cmdRule.cmdSteps().topLevelSteps().createDefaultTextCampaign(ulogin).getCampaignId();
        check();
    }

    @Description("Дополнения под менеджером клиентус кампанией, созданной менеджером")
    public void calloutsByClient() {
        ulogin = getUlogin();
        cmdRule.cmdSteps().authSteps().authenticate(User.get(ulogin));
        check();
    }

    protected abstract void check();

    protected abstract void checkForManager();

    @After
    public void after() {
        if (cid != null) {
            cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(ulogin, cid);
        }
    }

}
