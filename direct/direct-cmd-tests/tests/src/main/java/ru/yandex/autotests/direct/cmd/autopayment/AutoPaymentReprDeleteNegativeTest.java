package ru.yandex.autotests.direct.cmd.autopayment;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.representative.RepresentativeErrors;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.autopayment.AutoPaymentHelper;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка невозможности удления представителя, который включил автопополнение")
@Stories(TestFeatures.Representative.DELETE_CL_REP)
@Features(TestFeatures.REPRESENTATIVE)
@Tag(CmdTag.DELETE_CL_REP)
@Tag(ObjectTag.USER)
@Tag(TrunkTag.YES)
public class AutoPaymentReprDeleteNegativeTest {
    private static final String REPRESENTATIVE = "at-direct-backend-rus-os-p2";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();


    @Before
    public void before() {

        Long walletCid = (long) cmdRule.apiSteps().financeSteps()
                .enableAndGetSharedAccount(REPRESENTATIVE);
        AutoPaymentHelper.deleteAutopaymentDB(walletCid, REPRESENTATIVE);
        AutoPaymentHelper.enableAutopaymentDB(walletCid, REPRESENTATIVE);
    }


    @Test
    @Description("Проверка невозможности удаления представителя, который включил автопополнение")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9033")
    public void checkDeleteReprNegativeTest() {
        ErrorResponse errorResponse = cmdRule.cmdSteps().representativeSteps().postDeleteClRepErrorResponse(REPRESENTATIVE);
        assertThat("нельзя удалить представителя", errorResponse.getError(),
                equalTo(RepresentativeErrors.AUTOPAYMENT_REPRESENTATIVE_DELETE.getErrorText()));
    }
}
