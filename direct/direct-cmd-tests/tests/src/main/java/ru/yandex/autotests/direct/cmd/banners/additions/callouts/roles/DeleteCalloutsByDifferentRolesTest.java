package ru.yandex.autotests.direct.cmd.banners.additions.callouts.roles;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.banners.additions.callouts.CalloutsTestHelper;
import ru.yandex.autotests.direct.cmd.data.banners.additions.DeleteBannersAdditionsRequest;
import ru.yandex.autotests.direct.cmd.data.banners.additions.DeleteBannersAdditionsResponse;
import ru.yandex.autotests.direct.cmd.data.banners.additions.GetBannersAdditionsResponse;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Callout;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.direct.utils.matchers.BeanEqualsAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

/*
* todo javadoc
*/
@Aqua.Test
@Description("Удаление уточнений клиента под разными ролями")
@Stories(TestFeatures.Banners.BANNERS_CALLOUTS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.DELETE_BANNERS_ADDITIONS)
@Tag(TrunkTag.YES)
public class DeleteCalloutsByDifferentRolesTest extends CalloutsByDifferentRolesTestBase {

    @Override
    protected String getSvcClient() {
        return "at-direct-banners-callout-svc3";
    }

    @Override
    protected String getUlogin() {
        return "at-direct-banners-callouts-23";
    }

    @Override
    protected void check() {
        CalloutsTestHelper helper = new CalloutsTestHelper(ulogin, cmdRule.cmdSteps(), "");
        helper.clearCalloutsForClient();

        GetBannersAdditionsResponse saveResponse = cmdRule.cmdSteps()
                .bannersAdditionsSteps().saveBannersCallouts(ulogin, expectedCallout);

        assumeThat("Уточнения сохранились", saveResponse.getCallouts(), hasSize(1));

        assumeThat("Уточнения сохранились", saveResponse.getCallouts().get(0).getAdditionsItemId(), greaterThan(0L));

        List<Callout> callouts = saveResponse.getCallouts();
        DeleteBannersAdditionsResponse response = cmdRule.cmdSteps().bannersAdditionsSteps()
                .deleteClientCallouts(
                        DeleteBannersAdditionsRequest.defaultCalloutsRequest(
                                ulogin, callouts.toArray(new Callout[callouts.size()])));

        assertThat("Удаление прошло успешно", response.getSuccess(), equalTo("1"));
    }

    @Override
    protected void checkForManager() {
        check();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9125")
    public void calloutsByAgencyForSvcClient() {
        super.calloutsByAgencyForSvcClient();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9126")
    public void calloutsByManagerForSvcClient() {
        super.calloutsByManagerForSvcClient();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9127")
    public void calloutsByManagerForClient() {
        super.calloutsByManagerForClient();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9128")
    public void calloutsByClient() {
        super.calloutsByClient();
    }
}
