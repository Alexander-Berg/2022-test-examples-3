package ru.yandex.autotests.direct.cmd.banners.additions.callouts.roles;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.banners.additions.callouts.CalloutsTestHelper;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Callout;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.groups.GroupsFactory;
import ru.yandex.autotests.direct.cmd.data.redirect.LocationParam;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Сохранение уточнений на клиента под разными ролями")
@Stories(TestFeatures.Banners.BANNERS_CALLOUTS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(TrunkTag.YES)
public class SaveTextBannersCalloutsByDifferentRolesTest extends CalloutsByDifferentRolesTestBase {

    @Override
    protected String getSvcClient() {
        return "at-direct-banners-callouts-svc";
    }

    @Override
    protected String getUlogin() {
        return "at-direct-banners-callouts-22";
    }

    private void clearCallouts() {
        CalloutsTestHelper helper = new CalloutsTestHelper(ulogin, cmdRule.cmdSteps(), "");
        helper.clearCalloutsForClient();
    }

    private void saveGroupAndCheck(Long expectedCid) {
        Group group = GroupsFactory.getDefaultTextGroup();
        group.getBanners().get(0).withCallouts(new Callout().withCalloutText(expectedCallout));
        GroupsParameters request = GroupsParameters.forNewCamp(ulogin, expectedCid, group);

        Long actualCid = cmdRule.cmdSteps().groupsSteps()
                .postSaveTextAdGroups(request).getLocationParamAsLong(LocationParam.CID);

        assertThat("у клиента присутствуют дополнения", actualCid, equalTo(expectedCid));
    }

    @Override
    protected void check() {
        clearCallouts();
        Long expectedCid = cmdRule.cmdSteps().campaignSteps().saveNewDefaultTextCampaign(ulogin);
        saveGroupAndCheck(expectedCid);
    }

    @Override
    protected void checkForManager() {
        clearCallouts();
        Long expectedCid = cmdRule.cmdSteps().campaignSteps().saveNewDefaultTextCampaign(Logins.AGENCY, ulogin);
        saveGroupAndCheck(expectedCid);
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9139")
    public void calloutsByAgencyForSvcClient() {
        super.calloutsByAgencyForSvcClient();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9140")
    public void calloutsByManagerForSvcClient() {
        super.calloutsByManagerForSvcClient();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9141")
    public void calloutsByManagerForClient() {
        super.calloutsByManagerForClient();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9142")
    public void calloutsByClient() {
        super.calloutsByClient();
    }
}
