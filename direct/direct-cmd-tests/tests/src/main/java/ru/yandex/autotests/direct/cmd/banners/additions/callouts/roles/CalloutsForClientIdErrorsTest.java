package ru.yandex.autotests.direct.cmd.banners.additions.callouts.roles;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.additions.BannersAdditionsErrorsResponse;
import ru.yandex.autotests.direct.cmd.data.banners.additions.DeleteBannersAdditionsRequest;
import ru.yandex.autotests.direct.cmd.data.banners.additions.GetBannersAdditionsRequest;
import ru.yandex.autotests.direct.cmd.data.banners.additions.SaveBannersAdditionsRequest;
import ru.yandex.autotests.direct.cmd.data.commons.CommonErrorsResource;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Callout;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
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
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/*
* todo javadoc
*/
@Aqua.Test
@Description("Проверяем, что нет доступа к ручкам про дополениня")
@Stories(TestFeatures.Banners.BANNERS_CALLOUTS)
@Features(TestFeatures.BANNERS)
@Tag(ObjectTag.CALLOUTS)
@RunWith(Parameterized.class)
public class CalloutsForClientIdErrorsTest {
    @ClassRule
    public static DirectCmdRule defaultClassRuleChain = DirectCmdRule.defaultClassRule();
    public String ulogin = "at-direct-banners-callouts-1";
    @Parameterized.Parameter(0)
    public String authLogin;
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    private String callout = "callout1";

    @Parameterized.Parameters(name = "Логин {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"at-direct-banners-callouts-18"},
                {Logins.AGENCY},
                {Logins.MANAGER},
        });
    }

    @Test
    @Description("Не можем сохранять дополнения клиента")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9123")
    public void saveCallouts() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(authLogin));
        BannersAdditionsErrorsResponse error = cmdRule.cmdSteps().bannersAdditionsSteps()
                .saveBannersAdditionsError(SaveBannersAdditionsRequest.defaultCalloutsRequest(ulogin, callout));
        assertThat("получили ошибку доступа", error.getError(),
                containsString(CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString()));
    }

    @Test
    @Description("Не можем получать дополнения клиента")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9122")
    public void getCallouts() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(authLogin));
        ErrorResponse error = cmdRule.cmdSteps().bannersAdditionsSteps()
                .getBannersAdditionsError(GetBannersAdditionsRequest.getDefaultCalloutsRequest(ulogin));
        assertThat("получили ошибку доступа", error.getError(),
                containsString(CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString()));
    }

    @Test
    @Description("Не можем удалять дополнения клиента")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9124")
    public void deleteCallouts() {
        List<Callout> expectedCallots = cmdRule.cmdSteps()
                .bannersAdditionsSteps().getCallouts(ulogin).getCallouts();
        cmdRule.cmdSteps().authSteps().authenticate(User.get(authLogin));
        BannersAdditionsErrorsResponse error = cmdRule.cmdSteps().bannersAdditionsSteps()
                .deleteClientCalloutsError(
                        DeleteBannersAdditionsRequest.defaultCalloutsRequest(ulogin,
                                expectedCallots.toArray(new Callout[expectedCallots.size()])));
        assertThat("получили ошибку доступа", error.getError(),
                containsString(CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString()));
    }
}
