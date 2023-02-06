package ru.yandex.autotests.direct.cmd.banners.additions.callouts.roles;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.banners.additions.callouts.CalloutsTestHelper;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/*
* todo javadoc
*/
@Aqua.Test
@Description("Сохранение уточнений на клиента под разными ролями")
@Stories(TestFeatures.Banners.BANNERS_CALLOUTS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_BANNERS_ADDITIONS)
@Tag(CmdTag.GET_BANNERS_ADDITIONS)
@Tag(TrunkTag.YES)
public class SaveAndGetCalloutsByDifferentRolesTest extends CalloutsByDifferentRolesTestBase {

    @Override
    protected String getSvcClient() {
        return "at-direct-banners-callout-svc4";
    }

    @Override
    protected String getUlogin() {
        return "at-direct-banners-callouts-24";
    }

    @Override
    protected void check() {
        CalloutsTestHelper helper = new CalloutsTestHelper(ulogin, cmdRule.cmdSteps(), "");
        helper.clearCalloutsForClient();

        cmdRule.cmdSteps().bannersAdditionsSteps().saveBannersCalloutsSafe(ulogin, expectedCallout);

        List<String> actual = cmdRule.cmdSteps().bannersAdditionsSteps().getCalloutsList(ulogin);
        assertThat("у клиента присутствуют дополнения", actual, containsInAnyOrder(expectedCallout));
    }

    @Override
    protected void checkForManager() {
        check();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9131")
    public void calloutsByAgencyForSvcClient() {
        super.calloutsByAgencyForSvcClient();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9132")
    public void calloutsByManagerForSvcClient() {
        super.calloutsByManagerForSvcClient();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9133")
    public void calloutsByManagerForClient() {
        super.calloutsByManagerForClient();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9134")
    public void calloutsByClient() {
        super.calloutsByClient();
    }
}
