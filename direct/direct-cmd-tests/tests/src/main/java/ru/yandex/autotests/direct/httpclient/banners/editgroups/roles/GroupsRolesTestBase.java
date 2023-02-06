package ru.yandex.autotests.direct.httpclient.banners.editgroups.roles;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.GroupsCmdBean;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.direct.httpclient.data.banners.GroupsParameters;
import ru.yandex.autotests.directapi.model.User;

import java.util.Arrays;
import java.util.Collection;

import static java.util.Collections.emptyMap;
import static ru.yandex.autotests.direct.httpclient.CocaineSteps.getCsrfTokenFromCocaine;
import static ru.yandex.autotests.direct.httpclient.util.mappers.groupsapitocmd.apiGroupsSetter.getAdditionalSiteLinks;


/**
 * Created by shmykov on 29.04.15.
 * TESTIRT-4974
 */
@RunWith(Parameterized.class)
public abstract class GroupsRolesTestBase {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected static String SELF_CAMPAIGN = "12645127";
    protected static String SELF_BANNER = "879059721";
    protected static String AG_CAMPAIGN = "12645236";
    protected static String AG_BANNER = "879072887";
    protected static String CLIENT_LOGIN = "at-direct-b-showcampme-client";
    protected static String CLIENT_REPRESENTATIVE = "at-direct-b-showcampme-rep";
    protected static String ANOTHER_CLIENT_LOGIN = "at-backend-banners";
    protected static CSRFToken csrfToken;
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    @Parameterized.Parameter(value = 0)
    public String description;
    @Parameterized.Parameter(value = 1)
    public String userLogin;
    @Parameterized.Parameter(value = 2)
    public String campaignId;
    protected GroupsCmdBean expectedGroups;


    protected CMD cmd;
    protected DirectResponse response;
    protected GroupsParameters requestParams;
    protected String bannerId;

    public GroupsRolesTestBase(CMD cmd) {
        this.cmd = cmd;
    }

    public static Collection<Object[]> allowedRoles() {
        Object[][] data = new Object[][]{
                {"Пользователь", CLIENT_LOGIN, SELF_CAMPAIGN},
                {"Менеджер", Logins.MANAGER, AG_CAMPAIGN},
                {"Агенство", Logins.ADGROUPS_AGENCY, AG_CAMPAIGN},
                {"Супер", Logins.SUPER, SELF_CAMPAIGN},
                {"Представитель", CLIENT_REPRESENTATIVE, SELF_CAMPAIGN},
        };
        return Arrays.asList(data);
    }

    @Before
    public void before() {


        bannerId = campaignId.equals(SELF_CAMPAIGN) ? SELF_BANNER : AG_BANNER;
        cmdRule.apiAggregationSteps().unArchiveCampaign(CLIENT_LOGIN, Long.valueOf(campaignId));
        requestParams = new GroupsParameters();
        requestParams.setUlogin(CLIENT_LOGIN);
        requestParams.setCid(campaignId);
        Long groupId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT_LOGIN).bannersSteps()
                .getBanner(Long.valueOf(bannerId)).getPid();
        requestParams.setAdgroupIds(String.valueOf(groupId));
        expectedGroups =
                cmdRule.oldSteps().groupsSteps().openShowCampMultiEdit(campaignId, groupId.toString(), CLIENT_LOGIN);
        expectedGroups.getGroups().forEach(group -> group.setTags(emptyMap()));
        expectedGroups.getGroups().stream().flatMap(g -> g.getBanners().stream())
                .forEach(b -> b.getSitelinks().addAll(getAdditionalSiteLinks(4 - b.getSitelinks().size())));
        cmdRule.oldSteps().onPassport().authoriseAs(userLogin, User.get(userLogin).getPassword());
        csrfToken = getCsrfTokenFromCocaine(User.get(userLogin).getPassportUID());
        switch (cmd) {
            case SHOW_CAMP_MULTI_EDIT:
                response = cmdRule.oldSteps().groupsSteps().openShowCampMultiEdit(csrfToken, requestParams);
                break;
            case SAVE_TEXT_ADGROUPS:
                expectedGroups.getGroups().get(0).getBanners().get(0).setBannerType("desktop");
                requestParams.setJsonGroups(expectedGroups.toJson());
                response = cmdRule.oldSteps().groupsSteps().saveGroups(csrfToken, requestParams);
                break;
        }
    }
}
