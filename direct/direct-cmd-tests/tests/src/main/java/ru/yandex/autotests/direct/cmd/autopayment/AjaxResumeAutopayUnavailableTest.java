package ru.yandex.autotests.direct.cmd.autopayment;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.autopayment.AjaxResumeAutopayRequest;
import ru.yandex.autotests.direct.cmd.data.commons.CommonErrorsResource;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.common.api45.Account;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка невозможности возобновения автоплатежа контроллером ajaxSaveAutopaySettings")
@Stories(TestFeatures.Wallet.AJAX_RESUME_AUTOPAY)
@Features(TestFeatures.WALLET)
@Tag(CmdTag.AJAX_RESUME_AUTOPAY)
@Tag(ObjectTag.WALLET)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class AjaxResumeAutopayUnavailableTest {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    @Parameterized.Parameter(0)
    public String desc;
    @Parameterized.Parameter(1)
    public String user;
    @Parameterized.Parameter(2)
    public String client;

    private String walletId;

    @Parameterized.Parameters(name = "Под {0} ({1}) для {2}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"менеджер", Logins.MANAGER, "at-direct-backend-cl-ap2"},
                {"вешальщик", Logins.PLACER, "at-direct-backend-ap"},
                {"саппорт", Logins.SUPPORT, "at-direct-backend-ap"}
        });
    }

    @Before
    public void before() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(user));
        walletId = String.valueOf(((Account) cmdRule.apiSteps()
                .financeSteps().getAccount(client)).getAccountID());
    }

    @Test
    @Description("Проверка невозможности возобновить автоплатеж")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9028")
    public void checkNoRightsForResumeAutopay() {
        ErrorResponse errorResponse = cmdRule.cmdSteps().autopaySettingsSteps()
                .postAjaxResumeAutopay(new AjaxResumeAutopayRequest()
                        .withWalletCid(walletId)
                        .withUlogin(client));

        assertThat("нет прав для выполнения операции", errorResponse.getError(),
                containsString(CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString()));
    }

}
