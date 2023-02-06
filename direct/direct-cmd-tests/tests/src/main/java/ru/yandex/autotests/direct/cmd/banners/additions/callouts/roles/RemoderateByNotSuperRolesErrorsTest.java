package ru.yandex.autotests.direct.cmd.banners.additions.callouts.roles;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.banners.additions.callouts.CalloutsTestHelper;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.additions.RemoderateBannersAdditionsRequest;
import ru.yandex.autotests.direct.cmd.data.commons.CommonErrorsResource;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

/*
* todo javadoc
*/
@Aqua.Test
@Description("Перемодерация дополнений недоступна не супер ролям")
@Stories(TestFeatures.Banners.BANNERS_CALLOUTS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.REMODERATE_BANNERS_ADDITIONS)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class RemoderateByNotSuperRolesErrorsTest {

    public final static String SVC_CLIENT = "at-direct-banners-callouts-svc";
    public final static String CLIENT = "at-direct-banners-callouts-20";
    @ClassRule
    public static DirectCmdRule defaultClassRuleChain = DirectCmdRule.defaultClassRule();
    @Parameterized.Parameter(0)
    public String authLogin;
    @Parameterized.Parameter(1)
    public String ulogin;
    @Parameterized.Parameter(2)
    public String forAgency;
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    protected CalloutsTestHelper helper;
    private String callout = "callout1";
    private Long cid;

    @Parameterized.Parameters(name = "Логин {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CLIENT, CLIENT, null},
                {Logins.AGENCY, SVC_CLIENT, Logins.AGENCY},
                {Logins.MANAGER, SVC_CLIENT, Logins.AGENCY},
                {Logins.MANAGER, CLIENT, null}
        });
    }

    @Before
    public void setUp() {
        helper = new CalloutsTestHelper(ulogin, cmdRule.cmdSteps(), "");
        helper.clearCalloutsForClient();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9129")
    public void remoderateCalloutsForTextBanner() {

        cid = cmdRule.cmdSteps().campaignSteps().saveNewDefaultTextCampaign(forAgency, ulogin);

        helper.overrideCid(cid.toString());

        helper.saveCallouts(helper.getRequestFor(helper.newGroupAndSet(callout)));

        remoderateAndCheck();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9130")
    public void remoderateCalloutsForDynamicBanner() {

        cid = cmdRule.cmdSteps().campaignSteps().saveNewDefaultDynamicCampaign(forAgency, ulogin);

        helper.overrideCid(cid.toString());

        helper.saveCalloutsForDynamic(helper.getRequestForDynamic(helper.newDynamicGroupAndSet(callout)));

        remoderateAndCheck();
    }

    private void remoderateAndCheck() {
        List<Banner> groups = cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(ulogin, cid.toString()).getGroups();

        assumeThat("Группа сохранилась", groups, hasSize(1));

        Long adGroupId = cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(ulogin, cid.toString()).getGroups().get(0).getAdGroupId();

        cmdRule.cmdSteps().authSteps().authenticate(User.get(authLogin));

        ErrorResponse response =
                cmdRule.cmdSteps().bannersAdditionsSteps().remoderateClientCalloutsError(
                        new RemoderateBannersAdditionsRequest()
                                .withAdgroupIds(adGroupId)
                                .withCid(cid)
                                .withUlogin(ulogin)
                );

        assertThat("Получили ошибку", response.getError(),
                containsString(CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString()));

    }

}
